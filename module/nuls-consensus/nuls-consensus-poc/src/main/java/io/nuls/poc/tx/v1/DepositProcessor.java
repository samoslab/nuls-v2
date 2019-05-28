package io.nuls.poc.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.protocol.TransactionProcessor;
import io.nuls.poc.model.bo.Chain;
import io.nuls.poc.model.bo.tx.txdata.Deposit;
import io.nuls.poc.utils.LoggerUtil;
import io.nuls.poc.utils.manager.ChainManager;
import io.nuls.poc.utils.manager.DepositManager;
import io.nuls.poc.utils.validator.TxValidator;

import java.util.*;

@Component("DepositProcessorV1")
public class DepositProcessor implements TransactionProcessor {
    @Autowired
    private DepositManager depositManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private TxValidator txValidator;

    @Override
    public int getType() {
        return TxType.DEPOSIT;
    }

    @Override
    public List<Transaction> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist.");
            return null;
        }
        List<Transaction> invalidTxList = new ArrayList<>();
        Set<NulsHash> invalidHashSet = txValidator.getInvalidAgentHash(txMap.get(TxType.RED_PUNISH),txMap.get(TxType.CONTRACT_STOP_AGENT),txMap.get(TxType.STOP_AGENT),chain);
        for (Transaction depositTx:txs) {
            try {
                if(!txValidator.validateTx(chain, depositTx)){
                    invalidTxList.add(depositTx);
                    chain.getLogger().info("Delegated transaction verification failed");
                    continue;
                }
                Deposit deposit = new Deposit();
                deposit.parse(depositTx.getTxData(), 0);
                if (invalidHashSet.contains(deposit.getAgentHash())) {
                    invalidTxList.add(depositTx);
                    chain.getLogger().info("Conflict between Intelligent Delegation Transaction and Red Card Transaction or Stop Node Transaction");
                }
            }catch (Exception e){
                chain.getLogger().error(e);
            }
        }
        return invalidTxList;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> commitSuccessList = new ArrayList<>();
        boolean commitResult = true;
        for (Transaction tx:txs) {
            try {
                if(depositManager.depositCommit(tx,blockHeader,chain)){
                    commitSuccessList.add(tx);
                }
            }catch (NulsException e){
                chain.getLogger().error("Failure to deposit transaction submission");
                chain.getLogger().error(e);
                commitResult = false;
            }
        }
        //回滚已提交成功的交易
        if(!commitResult){
            for (Transaction rollbackTx:commitSuccessList) {
                try {
                    depositManager.depositRollBack(rollbackTx, chain);
                }catch (NulsException e){
                    chain.getLogger().error("Failure to deposit transaction rollback");
                    chain.getLogger().error(e);
                }
            }
        }
        return commitResult;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if(chain == null){
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> rollbackSuccessList = new ArrayList<>();
        boolean rollbackResult = true;
        for (Transaction tx:txs) {
            try {
                if(depositManager.depositRollBack(tx,chain)){
                    rollbackSuccessList.add(tx);
                }
            }catch (NulsException e){
                chain.getLogger().error("Failure to deposit transaction rollback");
                chain.getLogger().error(e);
                rollbackResult = false;
            }
        }
        //保存已回滚成功的交易
        if(!rollbackResult){
            for (Transaction commitTx:rollbackSuccessList) {
                try {
                    depositManager.depositCommit(commitTx, blockHeader, chain);
                }catch (NulsException e){
                    chain.getLogger().error("Failure to deposit transaction submission");
                    chain.getLogger().error(e);
                }
            }
        }
        return rollbackResult;
    }
}
