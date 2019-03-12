package io.nuls.transaction.service;

import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.po.TransactionConfirmedPO;

import java.util.List;

/**
 * 已确认交易的服务接口
 * @author: Charlie
 * @date: 2018/11/30
 */
public interface ConfirmedTxService {

    /**
     * Get a confirmed transaction
     *
     * 获取一笔已打包进区块并且确认的交易
     * @param chain
     * @param hash
     * @return TransactionConfirmedPO
     */
    TransactionConfirmedPO getConfirmedTransaction(Chain chain, NulsDigestData hash);

    /**
     * 保存已确认交易
     * Save confirmed transactions
     *
     * @param chain
     * @param transaction
     * @return Result
     */
    /*boolean saveTx(Chain chain, Transaction transaction);*/


    /**
     * 保存创世块的交易
     * @param chain
     * @param txhexList
     * @param blockHeaderHex
     * @return
     * @throws NulsException
     */
    boolean saveGengsisTxList(Chain chain, List<Transaction> txhexList, String blockHeaderHex) throws NulsException;

    /**
     * 保存区块中已确认交易
     * @param chain
     * @param txHashList
     * @param blockHeaderHex
     * @return
     */
    boolean saveTxList(Chain chain, List<NulsDigestData> txHashList, String blockHeaderHex) throws NulsException;



    /**
     * 批量回滚已确认交易
     * @param chain
     * @param txHashList
     * @param blockHeaderHex
     * @return
     */
    boolean rollbackTxList(Chain chain, List<NulsDigestData> txHashList, String blockHeaderHex) throws NulsException;

    /**
     * 根据最新区块高度扫描是否有需要处理的跨链交易,如果有则进行跨链发送
     * @param chain 链
     * @param blockHeight 最新区块高度
     * @throws NulsException
     */
    void processEffectCrossTx(Chain chain, long blockHeight) throws NulsException;
}
