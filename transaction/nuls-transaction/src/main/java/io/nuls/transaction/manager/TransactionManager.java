/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.transaction.manager;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.tools.basic.Result;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.data.BigIntegerUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.TxRegister;
import io.nuls.transaction.rpc.call.ChainCall;
import io.nuls.transaction.rpc.call.TransactionCall;
import io.nuls.transaction.service.TransactionService;
import io.nuls.transaction.utils.TxUtil;

import java.math.BigInteger;
import java.util.*;

/**
 * 交易管理类，存储管理交易注册的基本信息
 *
 * @author: Charlie
 * @date: 2018/11/22
 */
@Service
public class TransactionManager {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ChainManager chainManager;

    public TransactionManager() {
    }

    /**
     * 注册交易
     *
     * @param txRegister 注册交易请求数据封装
     * @return boolean
     */
    public boolean register(Chain chain, TxRegister txRegister) {
        boolean rs = false;
        if (!chain.getTxRegisterMap().containsKey(txRegister.getTxType())) {
            chain.getTxRegisterMap().put(txRegister.getTxType(), txRegister);
            rs = true;
        }
        return rs;
    }

    /**
     * 获取交易的注册对象
     *
     * @param type
     * @return
     */
    public TxRegister getTxRegister(Chain chain, int type) {
        return chain.getTxRegisterMap().get(type);
    }

    /**
     * 根据交易类型返回交易类型是否存在
     *
     * @param type
     * @return
     */
    public boolean contain(Chain chain, int type) {
        return chain.getTxRegisterMap().containsKey(type);
    }

    /**
     * 返回系统交易类型
     */
    public List<Integer> getSysTypes(Chain chain) {
        List<Integer> list = new ArrayList<>();
        for (Map.Entry<Integer, TxRegister> map : chain.getTxRegisterMap().entrySet()) {
            if (map.getValue().getSystemTx()) {
                list.add(map.getKey());
            }
        }
        return list;
    }

    /**
     * 判断交易是系统交易
     *
     * @param tx
     * @return
     */
    public boolean isSystemTx(Chain chain, Transaction tx) {
        TxRegister txRegister = chain.getTxRegisterMap().get(tx.getType());
        return txRegister.getSystemTx();
    }

    /**
     * 验证交易
     *
     * @param chain
     * @param tx
     * @return
     */
    public boolean verify(Chain chain, Transaction tx) {
        try {
            baseTxValidate(chain, tx);
            //如果是跨链交易直接调模块内部验证器接口，不走cmd命令
            if (tx.getType() == TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER) {
                transactionService.crossTransactionValidator(chain, tx);
            }
            TxRegister txRegister = this.getTxRegister(chain, tx.getType());
            //调验证器
            return TransactionCall.txProcess(chain, txRegister.getValidator(), txRegister.getModuleCode(), tx.hex());
        } catch (NulsException e) {
            chain.getLogger().error(e.getErrorCode().getMsg(), e.fillInStackTrace());
            return false;
        } catch (Exception e) {
            chain.getLogger().error(TxErrorCode.IO_ERROR.getMsg());
            return false;
        }

    }

    /**
     * 交易基础验证
     * 基础字段
     * 交易size
     * 交易类型
     * 交易签名
     * * from的地址必须全部是发起链(本链or相同链）地址
     * from里面的资产是否存在
     * to里面的地址必须是相同链的地址
     * 交易手续费
     *
     * @param chain
     * @param tx
     * @return Result
     */
    private boolean baseTxValidate(Chain chain, Transaction tx) throws NulsException {

        if (null == tx) {
            throw new NulsException(TxErrorCode.TX_NOT_EXIST);
        }
        if (tx.getHash() == null || tx.getHash().size() == 0 || tx.getHash().size() > TxConstant.TX_HASH_DIGEST_BYTE_MAX_LEN) {
            throw new NulsException(TxErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        if (!contain(chain, tx.getType())) {
            throw new NulsException(TxErrorCode.TX_NOT_EFFECTIVE);
        }
        if (tx.getTime() == 0L) {
            throw new NulsException(TxErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        if (tx.size() > TxConstant.TX_MAX_SIZE) {
            throw new NulsException(TxErrorCode.TX_SIZE_TOO_LARGE);
        }
        //todo 确认验证签名正确性
        if (!SignatureUtil.validateTransactionSignture(tx)) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
        //如果有coinData, 则进行验证,有一些交易没有coinData数据
        if (null != tx.getCoinData() && tx.getCoinData().length > 0) {
            //coinData基础验证以及手续费 (from中所有的nuls资产-to中所有nuls资产)
            CoinData coinData = TxUtil.getCoinData(tx);
            if (!validateCoinFromBase(chain, tx.getType(), coinData.getFrom())) {
                return false;
            }
            if (!validateCoinToBase(coinData.getTo())) {
                return false;
            }
            if (!validateFee(chain, tx.getType(), tx.size(), coinData)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证交易的付款方数据
     * 1.from中地址对应的链id是否是发起链id
     * 2.验证资产是否存在
     *
     * @param chain
     * @param listFrom
     * @return Result
     */
    public boolean validateCoinFromBase(Chain chain, int type, List<CoinFrom> listFrom) throws NulsException {
        //coinBase交易没有from
        if (type == TxConstant.TX_TYPE_COINBASE) {
            throw new NulsException(TxErrorCode.SUCCESS);
        }
        if (null == listFrom || listFrom.size() == 0) {
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        int chainId = chain.getConfig().getChainId();
        Set<String> uniqueCoin = new HashSet<>();
        for (CoinFrom coinFrom : listFrom) {
            byte[] addrBytes = coinFrom.getAddress();
            int addrChainId = AddressTool.getChainIdByAddress(addrBytes);
            int assetsChainId =  coinFrom.getAssetsChainId();
            int assetsId =  coinFrom.getAssetsId();

            //如果不是跨链交易，from中地址对应的链id必须发起链id，跨链交易在验证器中验证
            if (type != TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER) {
                if (chainId != addrChainId) {
                    throw new NulsException(TxErrorCode.CHAINID_ERROR);
                }

            }
            //当交易不是转账以及跨链转账时，from的资产必须是该链主资产。(转账以及跨链交易，在验证器中验证资产)
            if (type != TxConstant.TX_TYPE_TRANSFER && type != TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER) {
                if (chain.getConfig().getAssetsId() != assetsId) {
                    throw new NulsException(TxErrorCode.ASSETID_ERROR);
                }
            }

            if (chainId == TxConstant.NULS_CHAINID) {
                //如果chainId是主网则通过连管理验证资产是否存在
                //todo
               /* if (!ChainCall.verifyAssetExist(assetsChainId, assetsId)) {
                    throw new NulsException(TxErrorCode.ASSET_NOT_EXIST);
                }*/
            }/*else{
               if(chain.getConfig().getAssetsId() != coinFrom.getAssetsId()){
                   //todo 普通交易如果资产不是该链资产，还需通过主网验证 是否需要？
               }
            }*/
            //验证账户地址,资产链id,资产id的组合唯一性
            boolean rs = uniqueCoin.add(AddressTool.getStringAddressByBytes(addrBytes) + "-" + assetsChainId + "-" + assetsId);
            if(!rs){
                throw new NulsException(TxErrorCode.COINFROM_HAS_DUPLICATE_COIN);
            }
            /* 1.没有进行链内转账交易的资产合法性验证(因为可能出现链外资产)，
               2.跨链交易(非主网发起)from地址与发起链匹配的验证，需各验证器进行验证
             */
        }
        return true;
    }

    /**
     * 验证交易的收款方数据(coinTo是不是属于同一条链)
     * 1.收款方所有地址是不是属于同一条链
     *
     * @param listTo
     * @return Result
     */
    public boolean validateCoinToBase(List<CoinTo> listTo) throws NulsException {
        if (null == listTo || listTo.size() == 0) {
            throw new NulsException(TxErrorCode.COINTO_NOT_FOUND);
        }
        //验证收款方是不是属于同一条链
        Integer addressChainId = null;
        Set<String> uniqueCoin = new HashSet<>();
        for (CoinTo coinTo : listTo) {
            int chainId = AddressTool.getChainIdByAddress(coinTo.getAddress());
            int assetsChainId =  coinTo.getAssetsChainId();
            int assetsId =  coinTo.getAssetsId();
            if (null == addressChainId) {
                addressChainId = chainId;
                continue;
            } else if (addressChainId != chainId) {
                throw new NulsException(TxErrorCode.CROSS_TX_PAYER_CHAINID_MISMATCH);
            }
            //验证账户地址,资产链id,资产id的组合唯一性
            boolean rs = uniqueCoin.add(AddressTool.getStringAddressByBytes(coinTo.getAddress()) + "-" + assetsChainId + "-" + assetsId);
            if(!rs){
                throw new NulsException(TxErrorCode.COINFROM_HAS_DUPLICATE_COIN);
            }
        }
        return true;
    }

    /**
     * 验证交易手续费是否正确
     *
     * @param chain    链id
     * @param type     tx type
     * @param txSize   tx size
     * @param coinData
     * @return Result
     */
    private boolean validateFee(Chain chain, int type, int txSize, CoinData coinData) throws NulsException {
        if (type == TxConstant.TX_TYPE_RED_PUNISH) {
            //红牌惩罚没有手续费
            return true;
        }
        //int chainId = chain.getConfig().getChainId();
        BigInteger feeFrom = BigInteger.ZERO;
        for (CoinFrom coinFrom : coinData.getFrom()) {
            feeFrom = feeFrom.add(accrueFee(type, chain, coinFrom));
        }
        BigInteger feeTo = BigInteger.ZERO;
        for (CoinTo coinTo : coinData.getTo()) {
            feeFrom = feeFrom.add(accrueFee(type, chain, coinTo));
        }
        //交易中实际的手续费
        BigInteger fee = feeFrom.subtract(feeTo);
        if (BigIntegerUtils.isEqualOrLessThan(fee, BigInteger.ZERO)) {
            Result.getFailed(TxErrorCode.INSUFFICIENT_FEE);
        }
        //根据交易大小重新计算手续费，用来验证实际手续费
        BigInteger targetFee;
        if (type == TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER) {
            targetFee = TransactionFeeCalculator.getCrossTxFee(txSize);
        } else {
            targetFee = TransactionFeeCalculator.getNormalTxFee(txSize);
        }
        if (BigIntegerUtils.isLessThan(fee, targetFee)) {
            Result.getFailed(TxErrorCode.INSUFFICIENT_FEE);
        }
        return true;
    }

    /**
     * 累积计算当前coinfrom中可用于计算手续费的资产
     *
     * @param type  tx type
     * @param chain chain id
     * @param coin  coinfrom
     * @return BigInteger
     */
    private BigInteger accrueFee(int type, Chain chain, Coin coin) {
        BigInteger fee = BigInteger.ZERO;
        if (type == TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER) {
            //为跨链交易时，只算nuls
            if (TxUtil.isNulsAsset(coin)) {
                fee = fee.add(coin.getAmount());
            }
        } else {
            //不为跨链交易时，只算发起链的主资产
            if (TxUtil.isChainAssetExist(chain, coin)) {
                fee = fee.add(coin.getAmount());
            }
        }
        return fee;
    }

}
