package io.nuls.transaction.service;

import io.nuls.base.data.NulsDigestData;
import io.nuls.transaction.model.bo.CrossChainTx;

import java.util.List;

/**
 * 验证过程中的跨链交易
 * Cross-chain transaction in verification
 *
 * @author: qinyifeng
 * @date: 2018/12/19
 */
public interface CrossChainTxService {

    /**
     * 新增或修改跨链交易数据
     *
     * @param chainId
     * @param ctx
     * @return
     */
    boolean putTx(int chainId, CrossChainTx ctx);

    /**
     * 删除跨链交易
     *
     * @param chainId
     * @param hash
     * @return
     */
    boolean removeTx(int chainId, NulsDigestData hash);

    /**
     * 根据交易哈希查询跨链交易
     *
     * @param chainId
     * @param hash
     * @return
     */
    CrossChainTx getTx(int chainId, NulsDigestData hash);

    /**
     * 查询指定链下所有跨链交易
     * Query all cross-chain transactions in the specified chain
     *
     * @param chainId
     * @return
     */
    List<CrossChainTx> getTxList(int chainId);

}
