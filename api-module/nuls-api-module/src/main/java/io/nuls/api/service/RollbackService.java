package io.nuls.api.service;

import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.ApiErrorCode;
import io.nuls.api.db.*;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.db.*;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.exception.NulsRuntimeException;

import java.math.BigInteger;
import java.util.*;

@Component
public class RollbackService {
    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AliasService aliasService;
    @Autowired
    private DepositService depositService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private PunishService punishService;
    @Autowired
    private RoundManager roundManager;
    @Autowired
    private ContractService contractService;
    @Autowired
    private ChainService chainService;
    @Autowired
    private AccountLedgerService ledgerService;

    //记录每个区块打包交易涉及到的账户的余额变动
    private Map<String, AccountInfo> accountInfoMap = new HashMap<>();
    //记录每个账户的资产变动
    private Map<String, AccountLedgerInfo> accountLedgerInfoMap = new HashMap<>();
    //记录每个区块代理节点的变化
    private List<AgentInfo> agentInfoList = new ArrayList<>();
    //记录每个区块设置别名信息
    private List<AliasInfo> aliasInfoList = new ArrayList<>();
    //记录每个区块委托共识的信息
    private List<DepositInfo> depositInfoList = new ArrayList<>();

    private List<String> punishTxHashList = new ArrayList<>();

    public boolean rollbackBlock(int chainId, long blockHeight) {
        clear();

        BlockInfo blockInfo = queryBlock(chainId, blockHeight);
        if (blockInfo == null) {
            chainService.rollbackComplete(chainId);
            return true;
        }

        findAddProcessAgentOfBlock(chainId, blockInfo);

        processTxs(chainId, blockInfo.getTxList());

        roundManager.rollback(chainId, blockInfo);

        save(chainId, blockInfo);
        return false;
    }

    private void findAddProcessAgentOfBlock(int chainId, BlockInfo blockInfo) {
        BlockHeaderInfo headerInfo = blockInfo.getHeader();
        AgentInfo agentInfo;
        if (headerInfo.isSeedPacked()) {
            //如果是种子节点打包的区块，则创建一个新的AgentInfo对象，临时使用
            //If it is a block packed by the seed node, create a new AgentInfo object for temporary use.
            agentInfo = new AgentInfo();
            agentInfo.setPackingAddress(headerInfo.getPackingAddress());
            agentInfo.setAgentId(headerInfo.getPackingAddress());
            agentInfo.setRewardAddress(agentInfo.getPackingAddress());
            headerInfo.setByAgentInfo(agentInfo);
        } else {
            //根据区块头的打包地址，查询打包节点的节点信息，修改相关统计数据
            //According to the packed address of the block header, query the node information of the packed node, and modify related statistics.
            agentInfo = queryAgentInfo(chainId, headerInfo.getPackingAddress(), 3);
            agentInfo.setTotalPackingCount(agentInfo.getTotalPackingCount() - 1);
            agentInfo.setLastRewardHeight(headerInfo.getHeight() - 1);
            agentInfo.setVersion(headerInfo.getAgentVersion());
            headerInfo.setByAgentInfo(agentInfo);

            if (blockInfo.getTxList() != null && !blockInfo.getTxList().isEmpty()) {
                calcCommissionReward(agentInfo, blockInfo.getTxList().get(0));
            }
        }
    }

    private void calcCommissionReward(AgentInfo agentInfo, TransactionInfo coinBaseTx) {
        List<CoinToInfo> list = coinBaseTx.getCoinTos();
        if (null == list || list.isEmpty()) {
            return;
        }

        BigInteger agentReward = BigInteger.ZERO, otherReward = BigInteger.ZERO;
        for (CoinToInfo output : list) {
            if (output.getAddress().equals(agentInfo.getRewardAddress())) {
                agentReward = agentReward.add(output.getAmount());
            } else {
                otherReward = otherReward.add(output.getAmount());
            }
        }
        agentInfo.setTotalReward(agentInfo.getTotalReward().subtract(agentReward).subtract(otherReward));
        agentInfo.setAgentReward(agentInfo.getAgentReward().subtract(agentReward));
        agentInfo.setCommissionReward(agentInfo.getCommissionReward().subtract(otherReward));
    }

    private void processTxs(int chainId, List<TransactionInfo> txs) {
        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
            if (tx.getType() == ApiConstant.TX_TYPE_COINBASE) {
                processCoinBaseTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_TRANSFER) {
                processTransferTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_ALIAS) {
                processAliasTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_REGISTER_AGENT) {
                processCreateAgentTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_JOIN_CONSENSUS) {
                processDepositTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_CANCEL_DEPOSIT) {
                processCancelDepositTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_STOP_AGENT) {
                processStopAgentTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_YELLOW_PUNISH) {
                processYellowPunishTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_RED_PUNISH) {
                processRedPunishTx(chainId, tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_CREATE_CONTRACT) {
                //                processCreateContract(tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_CALL_CONTRACT) {
                //                processCallContract(tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_DELETE_CONTRACT) {
                //                processDeleteContract(tx);
            } else if (tx.getType() == ApiConstant.TX_TYPE_CONTRACT_TRANSFER) {
                //                processContractTransfer(tx);
            }
        }
    }

    private void processCoinBaseTx(int chainId, TransactionInfo tx) {
        if (tx.getCoinTos() == null || tx.getCoinTos().isEmpty()) {
            return;
        }
        Set<String> addressSet = new HashSet<>();

        for (CoinToInfo output : tx.getCoinTos()) {
            addressSet.add(output.getAddress());
            calcBalance(chainId, output);
        }

        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        }
    }

    private void processTransferTx(int chainId, TransactionInfo tx) {
        Set<String> addressSet = new HashSet<>();

        if (tx.getCoinFroms() != null) {
            for (CoinFromInfo input : tx.getCoinFroms()) {
                addressSet.add(input.getAddress());
                calcBalance(chainId, input);
            }
        }
        if (tx.getCoinTos() != null) {
            for (CoinToInfo output : tx.getCoinTos()) {
                addressSet.add(output.getAddress());
                calcBalance(chainId, output);
            }
        }
        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        }
    }

    private void processAliasTx(int chainId, TransactionInfo tx) {
        Set<String> addressSet = new HashSet<>();

        if (tx.getCoinFroms() != null) {
            for (CoinFromInfo input : tx.getCoinFroms()) {
                addressSet.add(input.getAddress());
                calcBalance(chainId, input);
            }
        }
        if (tx.getCoinTos() != null) {
            for (CoinToInfo output : tx.getCoinTos()) {
                addressSet.add(output.getAddress());
                calcBalance(chainId, output);
            }
        }
        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        }

        AliasInfo aliasInfo = aliasService.getAliasByAddress(chainId, tx.getCoinFroms().get(0).getAddress());
        if (aliasInfo != null) {
            AccountInfo accountInfo = queryAccountInfo(chainId, aliasInfo.getAddress());
            accountInfo.setAlias(null);
            aliasInfoList.add(aliasInfo);
        }
    }

    private void processCreateAgentTx(int chainId, TransactionInfo tx) {
        CoinFromInfo input = tx.getCoinFroms().get(0);
        AccountInfo accountInfo = queryAccountInfo(chainId, input.getAddress());
        calcBalance(chainId, accountInfo, tx.getFee(), input);

        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        //查找到代理节点，设置isNew = true，最后做存储的时候删除
        AgentInfo agentInfo = queryAgentInfo(chainId, tx.getHash(), 1);
        agentInfo.setNew(true);
    }

    private void processDepositTx(int chainId, TransactionInfo tx) {
        CoinFromInfo input = tx.getCoinFroms().get(0);

        AccountInfo accountInfo = queryAccountInfo(chainId, input.getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        calcBalance(chainId, accountInfo, tx.getFee(), input);

        //查找到委托记录，设置isNew = true，最后做存储的时候删除
        DepositInfo depositInfo = depositService.getDepositInfoByKey(chainId, tx.getHash() + accountInfo.getAddress());
        depositInfo.setNew(true);
        depositInfoList.add(depositInfo);
        AgentInfo agentInfo = queryAgentInfo(chainId, depositInfo.getAgentHash(), 1);
        agentInfo.setTotalDeposit(agentInfo.getTotalDeposit().subtract(depositInfo.getAmount()));
        agentInfo.setNew(false);
        if (agentInfo.getTotalDeposit().compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("data error: agent[" + agentInfo.getTxHash() + "] totalDeposit < 0");
        }
    }

    private void processCancelDepositTx(int chainId, TransactionInfo tx) {
        CoinFromInfo input = tx.getCoinFroms().get(0);

        AccountInfo accountInfo = queryAccountInfo(chainId, input.getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut().subtract(tx.getFee()));
        accountInfo.setTotalBalance(accountInfo.getTotalBalance().add(tx.getFee()));

        //查询取消委托记录，再根据deleteHash反向查到委托记录
        DepositInfo cancelInfo = depositService.getDepositInfoByHash(chainId, tx.getHash());
        DepositInfo depositInfo = depositService.getDepositInfoByKey(chainId, cancelInfo.getDeleteKey());
        depositInfo.setDeleteKey(null);
        depositInfo.setDeleteHeight(0);
        cancelInfo.setNew(true);
        depositInfoList.add(depositInfo);
        depositInfoList.add(cancelInfo);

        AgentInfo agentInfo = queryAgentInfo(chainId, depositInfo.getAgentHash(), 1);
        agentInfo.setTotalDeposit(agentInfo.getTotalDeposit().add(depositInfo.getAmount()));
        agentInfo.setNew(false);
    }

    private void processStopAgentTx(int chainId, TransactionInfo tx) {
        for (int i = 0; i < tx.getCoinTos().size(); i++) {
            CoinToInfo output = tx.getCoinTos().get(i);
            AccountInfo accountInfo = queryAccountInfo(chainId, output.getAddress());
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
            if (i == 0) {
                accountInfo.setTotalBalance(accountInfo.getTotalBalance().add(tx.getFee()));
            }
        }

        AgentInfo agentInfo = queryAgentInfo(chainId, tx.getHash(), 4);
        agentInfo.setDeleteHash(null);
        agentInfo.setDeleteHeight(0);
        agentInfo.setStatus(1);
        agentInfo.setNew(false);
        //根据交易hash查询所有取消委托的记录
        List<DepositInfo> depositInfos = depositService.getDepositListByHash(chainId, tx.getHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo cancelDeposit : depositInfos) {
                //需要删除的数据
                cancelDeposit.setNew(true);

                DepositInfo depositInfo = depositService.getDepositInfoByKey(chainId, cancelDeposit.getDeleteKey());
                depositInfo.setDeleteHeight(0);
                depositInfo.setDeleteKey(null);

                depositInfoList.add(cancelDeposit);
                depositInfoList.add(depositInfo);

                agentInfo.setTotalDeposit(agentInfo.getTotalDeposit().add(depositInfo.getAmount()));
            }
        }
    }

    private void processYellowPunishTx(int chainId, TransactionInfo tx) {
        List<TxDataInfo> logList = punishService.getYellowPunishLog(chainId, tx.getHash());
        Set<String> addressSet = new HashSet<>();
        for (TxDataInfo txData : logList) {
            PunishLogInfo punishLog = (PunishLogInfo) txData;
            addressSet.add(punishLog.getAddress());
        }

        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(chainId, address);
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        }

        punishTxHashList.add(tx.getHash());
    }

    private void processRedPunishTx(int chainId, TransactionInfo tx) {
        punishTxHashList.add(tx.getHash());
        for (int i = 0; i < tx.getCoinTos().size(); i++) {
            CoinToInfo output = tx.getCoinTos().get(i);
            AccountInfo accountInfo = queryAccountInfo(chainId, output.getAddress());
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        }
        PunishLogInfo redPunish = punishService.getRedPunishLog(chainId, tx.getHash());
        //根据红牌找到被惩罚的节点
        AgentInfo agentInfo = queryAgentInfo(chainId, redPunish.getAddress(), 2);
        agentInfo.setDeleteHash(null);
        agentInfo.setDeleteHeight(0);
        agentInfo.setStatus(1);
        agentInfo.setNew(false);
        //根据交易hash查询所有取消委托的记录
        List<DepositInfo> depositInfos = depositService.getDepositListByHash(chainId, tx.getHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo cancelDeposit : depositInfos) {
                cancelDeposit.setNew(true);

                DepositInfo depositInfo = depositService.getDepositInfoByKey(chainId, cancelDeposit.getDeleteKey());
                depositInfo.setDeleteHeight(0);
                depositInfo.setDeleteKey(null);

                depositInfoList.add(cancelDeposit);
                depositInfoList.add(depositInfo);

                agentInfo.setTotalDeposit(agentInfo.getTotalDeposit().add(depositInfo.getAmount()));
            }
        }
    }

    private void save(int chainId, BlockInfo blockInfo) {
        SyncInfo syncInfo = chainService.getSyncInfo(chainId);
        if (blockInfo.getHeader().getHeight() != syncInfo.getBestHeight()) {
            throw new NulsRuntimeException(ApiErrorCode.DATA_ERROR);
        }
        if (syncInfo.isFinish()) {
            accountService.saveAccounts(chainId, accountInfoMap);
            syncInfo.setStep(30);
            chainService.updateStep(syncInfo);
        }

        if (syncInfo.getStep() == 30) {
            ledgerService.saveLedgerList(chainId, accountLedgerInfoMap);
            syncInfo.setStep(20);
            chainService.updateStep(syncInfo);
        }

        if (syncInfo.getStep() == 20) {
            agentService.saveAgentList(chainId, agentInfoList);
            syncInfo.setStep(10);
            chainService.updateStep(syncInfo);
        }

        depositService.rollbackDepoist(chainId, depositInfoList);
        punishService.rollbackPunishLog(chainId, punishTxHashList, blockInfo.getHeader().getHeight());
        aliasService.rollbackAliasList(chainId, aliasInfoList);
        transactionService.rollbackTxRelationList(chainId, blockInfo.getHeader().getTxHashList());
        transactionService.rollbackTx(chainId, blockInfo.getHeader().getTxHashList());
        blockService.deleteBlockHeader(chainId, blockInfo.getHeader().getHeight());

        syncInfo.setStep(100);
        syncInfo.setBestHeight(blockInfo.getHeader().getHeight() - 1);
        chainService.updateStep(syncInfo);
    }

    private AccountLedgerInfo calcBalance(int chainId, CoinToInfo output) {
        ChainInfo chainInfo = CacheManager.getChainInfo(chainId);
        if (output.getChainId() == chainInfo.getChainId() && output.getAssetsId() == chainInfo.getDefaultAsset().getAssetId()) {
            AccountInfo accountInfo = queryAccountInfo(chainId, output.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn().add(output.getAmount()));
            accountInfo.setTotalBalance(accountInfo.getTotalBalance().add(output.getAmount()));
        }

        AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, output.getAddress(), output.getChainId(), output.getAssetsId());
        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().add(output.getAmount()));
        return ledgerInfo;
    }

    private AccountLedgerInfo calcBalance(int chainId, CoinFromInfo input) {
        ChainInfo chainInfo = CacheManager.getChainInfo(chainId);
        if (input.getChainId() == chainInfo.getChainId() && input.getAssetsId() == chainInfo.getDefaultAsset().getAssetId()) {
            AccountInfo accountInfo = queryAccountInfo(chainId, input.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn().subtract(input.getAmount()));
            accountInfo.setTotalBalance(accountInfo.getTotalBalance().subtract(input.getAmount()));
            if (accountInfo.getTotalBalance().compareTo(BigInteger.ZERO) < 0) {
                throw new NulsRuntimeException(ApiErrorCode.DATA_ERROR, "account[" + accountInfo.getAddress() + "] totalBalance < 0");
            }
        }
        AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, input.getAddress(), input.getChainId(), input.getAssetsId());
        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().subtract(input.getAmount()));
        if (ledgerInfo.getTotalBalance().compareTo(BigInteger.ZERO) < 0) {
            throw new NulsRuntimeException(ApiErrorCode.DATA_ERROR, "accountLedger[" + ledgerInfo.getAddress() + "_" + ledgerInfo.getChainId() + "_" + ledgerInfo.getAssetId() + "] totalBalance < 0");
        }
        return ledgerInfo;
    }

    private AccountLedgerInfo calcBalance(int chainId, AccountInfo accountInfo, BigInteger fee, CoinFromInfo input) {
        accountInfo.setTotalOut(accountInfo.getTotalOut().subtract(fee));
        accountInfo.setTotalBalance(accountInfo.getTotalBalance().add(fee));

        AccountLedgerInfo ledgerInfo = queryLedgerInfo(chainId, input.getAddress(), input.getChainId(), input.getAssetsId());
        ledgerInfo.setTotalBalance(ledgerInfo.getTotalBalance().add(fee));

        return ledgerInfo;
    }


    private BlockInfo queryBlock(int chainId, long blockHeight) {
        BlockHeaderInfo headerInfo = blockService.getBlockHeader(chainId, blockHeight);
        if (headerInfo == null) {
            return null;
        }
        BlockInfo blockInfo = new BlockInfo();
        blockInfo.setHeader(headerInfo);
        List<TransactionInfo> txList = new ArrayList<>();
        for (int i = 0; i < headerInfo.getTxHashList().size(); i++) {
            TransactionInfo tx = transactionService.getTx(chainId, headerInfo.getTxHashList().get(i));
            if (tx != null) {
                txList.add(tx);
            }
        }
        blockInfo.setTxList(txList);
        return blockInfo;
    }

    private AccountInfo queryAccountInfo(int chainId, String address) {
        AccountInfo accountInfo = accountInfoMap.get(address);
        if (accountInfo == null) {
            accountInfo = accountService.getAccountInfo(chainId, address);
            if (accountInfo == null) {
                accountInfo = new AccountInfo(address);
            }
            accountInfoMap.put(address, accountInfo);
        }
        return accountInfo;
    }

    private AccountLedgerInfo queryLedgerInfo(int defaultChainId, String address, int chainId, int assetId) {
        String key = address + chainId + assetId;
        AccountLedgerInfo ledgerInfo = accountLedgerInfoMap.get(key);
        if (ledgerInfo == null) {
            ledgerInfo = ledgerService.getAccountLedgerInfo(defaultChainId, key);
            if (ledgerInfo == null) {
                ledgerInfo = new AccountLedgerInfo(address, chainId, assetId);
            }
            accountLedgerInfoMap.put(key, ledgerInfo);
        }
        return ledgerInfo;
    }

    private AgentInfo queryAgentInfo(int chainId, String key, int type) {
        AgentInfo agentInfo;
        for (int i = 0; i < agentInfoList.size(); i++) {
            agentInfo = agentInfoList.get(i);

            if (type == 1 && agentInfo.getTxHash().equals(key)) {
                return agentInfo;
            } else if (type == 2 && agentInfo.getAgentAddress().equals(key)) {
                return agentInfo;
            } else if (type == 3 && agentInfo.getPackingAddress().equals(key)) {
                return agentInfo;
            }
        }
        if (type == 1) {
            agentInfo = agentService.getAgentByHash(chainId, key);
        } else if (type == 2) {
            agentInfo = agentService.getAgentByAgentAddress(chainId, key);
        } else {
            agentInfo = agentService.getAgentByPackingAddress(chainId, key);
        }
        if (agentInfo != null) {
            agentInfoList.add(agentInfo);
        }
        return agentInfo;
    }

    private void clear() {
        accountInfoMap.clear();
        accountLedgerInfoMap.clear();
        agentInfoList.clear();
        aliasInfoList.clear();
        depositInfoList.clear();
        punishTxHashList.clear();
    }
}
