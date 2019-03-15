package io.nuls.transaction.task;

import io.nuls.base.data.Transaction;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.VerifyTxResult;
import io.nuls.transaction.model.po.TransactionConfirmedPO;
import io.nuls.transaction.rpc.call.LedgerCall;
import io.nuls.transaction.rpc.call.NetworkCall;
import io.nuls.transaction.service.TxService;
import io.nuls.transaction.storage.h2.TransactionH2Service;
import io.nuls.transaction.storage.rocksdb.UnconfirmedTxStorageService;
import io.nuls.transaction.storage.rocksdb.UnverifiedTxStorageService;
import io.nuls.transaction.utils.TransactionTimeComparator;
import io.nuls.transaction.utils.TxUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.nuls.transaction.utils.LoggerUtil.Log;

/**
 * @author: Charlie
 * @date: 2018/11/28
 */
public class VerifyTxProcessTask implements Runnable {

    private PackablePool packablePool = SpringLiteContext.getBean(PackablePool.class);

    private UnverifiedTxStorageService unverifiedTxStorageService = SpringLiteContext.getBean(UnverifiedTxStorageService.class);
    private TxService txService = SpringLiteContext.getBean(TxService.class);
    private UnconfirmedTxStorageService unconfirmedTxStorageService = SpringLiteContext.getBean(UnconfirmedTxStorageService.class);
    private TransactionH2Service transactionH2Service = SpringLiteContext.getBean(TransactionH2Service.class);

    private TransactionTimeComparator txComparator = SpringLiteContext.getBean(TransactionTimeComparator.class);
    private List<Transaction> orphanTxList = new ArrayList<>();

    private Chain chain;

    public VerifyTxProcessTask(Chain chain){
        this.chain = chain;
    }


    @Override
    public void run() {
        try {
            doTask(chain);
        } catch (Exception e) {
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).error(e);
        }
        try {
            //处理孤儿交易
            doOrphanTxTask(chain);
        } catch (Exception e) {
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).error(e);
        }
    }

    private void doTask(Chain chain){
        if (packablePool.getPoolSize(chain) >= chain.getConfig().getTxUnverifiedQueueSize()) {
            return;
        }

        Transaction tx = null;
        while ((tx = unverifiedTxStorageService.pollTx(chain)) != null && orphanTxList.size() < chain.getConfig().getOrphanContainerSize()) {
            processTx(chain, tx, false);
        }
    }

    private boolean processTx(Chain chain, Transaction tx, boolean isOrphanTx){
        try {
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("Process tx start......");
            TxUtil.txInformationDebugPrint(chain, tx, chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS));
            int chainId = chain.getChainId();
            boolean rs = txService.verify(chain, tx);
            //todo 跨链交易单独处理, 是否需要进行跨链验证？
            //只会有本地创建的跨链交易才会进入这里, 其他链广播到跨链交易, 由其他逻辑处理
            if (!rs) {
                return false;
            }
            //获取一笔交易
            TransactionConfirmedPO existTx = txService.getTransaction(chain, tx.getHash());
            if(null != existTx){
                return isOrphanTx;
            }
            VerifyTxResult verifyTxResult = LedgerCall.verifyCoinData(chain, tx, false);
            if(verifyTxResult.success()){
                chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS)
                        .debug("Task-Packaging 判断交易是否进入待打包队列,节点是否是打包节点: {}", chain.getPackaging().get());
                if(chain.getPackaging().get()) {
                    //当节点是出块节点时, 才将交易放入待打包队列
                    packablePool.add(chain, tx, false);
                }
                //保存到rocksdb
                unconfirmedTxStorageService.putTx(chainId, tx);
                //保存到h2数据库
                transactionH2Service.saveTxs(TxUtil.tx2PO(chain,tx));
                //调账本记录未确认交易
                LedgerCall.commitUnconfirmedTx(chain, tx.hex());
                //广播交易hash
                NetworkCall.broadcastTxHash(chain.getChainId(),tx.getHash());
                chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("Process tx success..................");
                chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("");
                return true;
            }
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("");
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("-- verifyCoinData fail ----------");
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug(
                    "coinData not success - code: {}, - reason:{}, type:{} - txhash:{}",
                    verifyTxResult.getCode(),  verifyTxResult.getDesc(), tx.getType(), tx.getHash().getDigestHex());
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("---------------------------------");
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("Process tx fail..................");
            chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("");
            if(verifyTxResult.getCode() == VerifyTxResult.ORPHAN && !isOrphanTx){
                processOrphanTx(tx);
            }else if(isOrphanTx){
                long currentTimeMillis = NetworkCall.getCurrentTimeMillis();
                return tx.getTime() < (currentTimeMillis - chain.getConfig().getOrphanTtl());
            }
        } catch (Exception e) {
            Log.error(e);
            e.printStackTrace();
        }
        return false;
    }

    private void doOrphanTxTask(Chain chain) throws NulsException{
        try {
            //时间排序TransactionTimeComparator
            orphanTxList.sort(txComparator);
            Iterator<Transaction> it = orphanTxList.iterator();
            while (it.hasNext()) {
                Transaction tx = it.next();
                boolean success = processTx(chain, tx, true);
                if (success) {
                    it.remove();
                    chain.getLoggerMap().get(TxConstant.LOG_NEW_TX_PROCESS).debug("*** Debug *** [VerifyTxProcessTask - OrphanTx] " +
                            "OrphanTx remove - type:{} - txhash:{}, -orphanTxList size:{}", tx.getType(), tx.getHash().getDigestHex(), orphanTxList.size());
                }
            }
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    private void processOrphanTx(Transaction tx) throws NulsException {
        orphanTxList.add(tx);
    }

}
