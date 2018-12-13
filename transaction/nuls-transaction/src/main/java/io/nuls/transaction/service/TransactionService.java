package io.nuls.transaction.service;

import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.tools.basic.Result;
import io.nuls.tools.crypto.ECKey;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.model.bo.TxRegister;
import io.nuls.transaction.model.bo.TxWrapper;
import io.nuls.transaction.model.dto.AccountSignDTO;
import io.nuls.transaction.model.dto.BlockHeaderDigestDTO;
import io.nuls.transaction.model.dto.CoinDTO;

import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2018/11/22
 */
public interface TransactionService {

    /**
     * 注册交易
     * Register transaction
     *
     * @param txRegister
     * @return Result
     */
    boolean register(TxRegister txRegister);

    /**
     * 收到一个新的交易
     * Received a new transaction
     *
     * @param transaction
     * @return Result
     */
    boolean newTx(int chainId, Transaction transaction) throws NulsException;

    /**
     * 获取一笔交易
     * get a transaction
     *
     * @param hash
     * @return Result
     */
    Transaction getTransaction(NulsDigestData hash) throws NulsException;


    /**
     * 创建不包含多签地址跨链交易，支持多普通地址
     * Create a cross-chain transaction
     *
     * @param currentChainId 当前链的id Current chainId
     * @param listFrom 交易的转出者数据 payer coins
     * @param listTo 交易的接收者数据 payee  coins
     * @param remark 交易备注 remark
     * @return Result
     */
    String createCrossTransaction(int currentChainId, List<CoinDTO> listFrom, List<CoinDTO> listTo, String remark) throws NulsException;

    /**
     * 创建跨链多签签名地址交易
     * 所有froms中有且仅有一个地址，并且只能是多签地址，但可以包含多个资产(from)
     *
     * @param currentChainId 当前链的id Current chainId
     * @param listFrom 交易的转出者数据 payer coins
     * @param listTo 交易的接收者数据 payee  coins
     * @param remark 交易备注 remark
     * @return Result
     */
    Map<String, String> createCrossMultiTransaction(int currentChainId, List<CoinDTO> listFrom, List<CoinDTO> listTo, String remark, AccountSignDTO accountSignDTO) throws NulsException;

    Map<String, String> createCrossMultiTransaction(int currentChainId, List<CoinDTO> listFrom, List<CoinDTO> listTo, String remark) throws NulsException;

    /**
     * 对多签交易进行签名的数据组装
     * @param address 执行签名的账户地址
     * @param password 账户密码
     * @param tx 待签名的交易数据
     * @return
     */
    Map<String, String> signMultiTransaction(String address, String password, String tx) throws NulsException;

    /**
     * 处理多签交易的签名 追加签名
     * @param txWrapper 交易数据 tx
     * @param ecKey 签名者的eckey
     * @return Result
     */
    Map<String, String> txMultiSignProcess(TxWrapper txWrapper, ECKey ecKey) throws NulsException;


    /**
     * 处理多签交易的签名，第一次签名可以先组装多签账户的基础数据，到签名数据中
     * @param txWrapper 交易数据 tx
     * @param ecKey 签名者的 eckey数据
     * @param multiSignTxSignature 新的签名数据  sign data
     * @return Result
     */
    Map<String, String> txMultiSignProcess(TxWrapper txWrapper, ECKey ecKey, MultiSignTxSignature multiSignTxSignature) throws NulsException;



    boolean crossTransactionValidator(int chainId, Transaction transaction) throws NulsException;
    boolean crossTransactionCommit(int chainId, Transaction transaction, BlockHeaderDigestDTO blockHeader) throws NulsException;
    boolean crossTransactionRollback(int chainId, Transaction transaction, BlockHeaderDigestDTO blockHeader) throws NulsException;

}
