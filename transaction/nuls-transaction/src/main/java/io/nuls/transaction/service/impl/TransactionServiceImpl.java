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
package io.nuls.transaction.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.tools.basic.Result;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.crypto.ECKey;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.BigIntegerUtils;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.CrossTxData;
import io.nuls.transaction.model.bo.TxRegister;
import io.nuls.transaction.model.bo.TxWrapper;
import io.nuls.transaction.model.dto.BlockHeaderDigestDTO;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.service.TransactionService;
import io.nuls.transaction.utils.TransactionManager;
import io.nuls.transaction.utils.TxUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * @author: Charlie
 * @date: 2018/11/22
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    private TransactionManager transactionManager = TransactionManager.getInstance();

    @Override
    public Result newTx(int chainId, Transaction tx) {
        /**
         * 1.基础数据库校验
         * 2.放入队列
         */
       /* Result result = transactionManager.BaseTxValidate(tx);
        if(result.isFailed()){
            return result;
        }*/
        TxWrapper txWrapper = new TxWrapper(chainId, tx);

        return Result.getSuccess(TxErrorCode.SUCCESS);
    }

    @Override
    public Result register(TxRegister txRegister) {
        boolean rs = transactionManager.register(txRegister);
        return Result.getSuccess(TxErrorCode.SUCCESS).setData(rs);
    }

    @Override
    public Result getTransaction(NulsDigestData hash){
        return Result.getSuccess(TxErrorCode.SUCCESS);
    }

    @Override
    public Result createCrossTransaction(int currentChainId, List<CoinDTO> listFrom, List<CoinDTO> listTo, String remark) {
        Transaction tx = new Transaction(TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER);
        CrossTxData txData = new CrossTxData();
        txData.setChainId(TxConstant.NUlS_CHAINID);
        tx.setRemark(StringUtils.bytes(remark));
        try {
            tx.setTxData(txData.serialize());
            List<CoinFrom> coinFromList = assemblyCoinFrom(currentChainId, listFrom);
            List<CoinTo> coinToList = assemblyCoinTo(listTo);
            CoinData coinData = getCoinData(coinFromList, coinToList, tx.size() + getSignatureSize(coinFromList));
            tx.setTxData(coinData.serialize());
            tx.setHash(NulsDigestData.calcDigestData(tx.serializeForHash()));
            List<ECKey> signEcKeys = new ArrayList<>();
            for(CoinDTO coinDTO : listFrom) {
                String priKey = TxUtil.getPrikey(coinDTO.getAddress(), coinDTO.getPassword());
                ECKey ecKey = ECKey.fromPrivate(new BigInteger(ECKey.SIGNUM, HexUtil.decode(priKey)));
                signEcKeys.add(ecKey);
            }
            SignatureUtil.createTransactionSignture(tx, signEcKeys);
            this.newTx(currentChainId, tx);
            return Result.getSuccess(TxErrorCode.SUCCESS);
        }catch (IOException e) {
            e.printStackTrace();
            return Result.getFailed(TxErrorCode.SERIALIZE_ERROR);
        } catch (NulsException e) {
            e.printStackTrace();
            return Result.getFailed(e.getErrorCode());
        }
    }

    /**
     * 通过coinfrom计算签名数据的size
     * 如果coinfrom有重复地址则只计算一次；如果有多签地址，只计算m个地址的size
     * @param coinFroms
     * @return
     */
    private int getSignatureSize(List<CoinFrom> coinFroms){
        int size = 0;
        Set<String> signAddress = new HashSet<>();
        for (CoinFrom coinFrom : coinFroms) {
            byte[] address = coinFrom.getAddress();
            //判断是否是多签地址
            if(AddressTool.isMultiSignAddress(address)){
                //todo 获取多重签名地址的m，最小签名数量
                int signNumber = TxUtil.getMofMultiSignAddress(address);
                size += signNumber * P2PHKSignature.SERIALIZE_LENGTH;
            }
            signAddress.add(AddressTool.getStringAddressByBytes(address));
        }
        size += signAddress.size() * P2PHKSignature.SERIALIZE_LENGTH;
        return size;
    }

    /**
     * assembly coinFrom
     * @param listFrom Initiator set coinFrom
     * @return List<CoinFrom>
     * @throws NulsException
     */
    private List<CoinFrom> assemblyCoinFrom(int currentChainId, List<CoinDTO> listFrom) throws NulsException {
        List<CoinFrom> coinFroms = new ArrayList<>();
        for(CoinDTO coinDTO : listFrom){
            String addr = coinDTO.getAddress();
            if(!AddressTool.validAddress(currentChainId, addr)){
                //转账交易转出地址必须是本链地址
                throw new NulsException(TxErrorCode.ADDRESS_IS_NOT_THE_CURRENT_CHAIN);
            }
            byte[] address = AddressTool.getAddress(addr);
            CoinFrom coinFrom = new CoinFrom();
            coinFrom.setAddress(address);
            coinFrom.setLocked(TxConstant.CORSS_TX_LOCKED);
            int assetChainId = coinDTO.getAssetsChainId();
            int assetId = coinDTO.getAssetsId();
            if(!TxUtil.assetExist(assetChainId, assetId)){
                //资产不存在 chainId assetId
                throw new NulsException(TxErrorCode.ASSET_NOT_EXIST);
            }
            coinFrom.setAssetsChainId(assetChainId);
            coinFrom.setAssetsId(assetId);
            //检查对应资产余额 是否足够
            BigInteger amount = coinDTO.getAmount();
            BigInteger balance = TxUtil.getBalance(address, assetChainId, assetId);
            if(BigIntegerUtils.isLessThan(balance, amount)){
                throw new NulsException(TxErrorCode.INSUFFICIENT_BALANCE);
            }
            coinFrom.setAmount(amount);
            coinFrom.setNonce(TxUtil.getNonce(address, assetChainId, assetId));
            coinFroms.add(coinFrom);
        }
        return coinFroms;
    }

    /**
     * assembly coinTo
     * @param listTo Initiator set coinTo
     * @return List<CoinTo>
     * @throws NulsException
     */
    private List<CoinTo> assemblyCoinTo(List<CoinDTO> listTo) throws NulsException{
        List<CoinTo> coinTos = new ArrayList<>();
        for(CoinDTO coinDTO : listTo){
            byte[] address = AddressTool.getAddress(coinDTO.getAddress());
            CoinTo coinTo = new CoinTo();
            coinTo.setAddress(address);
            int chainId = coinDTO.getAssetsChainId();
            int assetId = coinDTO.getAssetsId();
            coinTo.setAmount(coinDTO.getAmount());
            if(!TxUtil.assetExist(chainId, assetId)){
                //资产不存在 chainId assetId
                throw new NulsException(TxErrorCode.ASSET_NOT_EXIST);
            }
            coinTo.setAssetsChainId(chainId);
            coinTo.setAssetsId(assetId);
            coinTos.add(coinTo);
        }
        return coinTos;
    }


    /**
     * assembly coinData
     * @param listFrom
     * @param listTo
     * @param txSize
     * @return
     * @throws NulsException
     */
    private CoinData getCoinData(List<CoinFrom> listFrom, List<CoinTo> listTo, int txSize) throws NulsException{
        BigInteger feeTotalFrom = BigInteger.ZERO;
        for(CoinFrom coinFrom : listFrom){
            txSize += coinFrom.size();
            if(TxUtil.isNulsAsset(coinFrom)){
                feeTotalFrom = feeTotalFrom.add(coinFrom.getAmount());
            }
        }
        BigInteger feeTotalTo = BigInteger.ZERO;
        for(CoinTo coinTo : listTo){
            txSize += coinTo.size();
            if(TxUtil.isNulsAsset(coinTo)){
                feeTotalTo = feeTotalTo.add(coinTo.getAmount());
            }
        }
        //本交易预计收取的手续费
        BigInteger targetFee = TransactionFeeCalculator.getCrossTxFee(txSize);
        //实际收取的手续费, 可能自己已经组装完成
        BigInteger actualFee = feeTotalFrom.subtract(feeTotalTo);
        if(BigIntegerUtils.isLessThan(actualFee, BigInteger.ZERO)){
            //所有from中账户的nuls余额总和小于to的总和，不够支付手续费
            throw new NulsException(TxErrorCode.INSUFFICIENT_FEE);
        }else if(BigIntegerUtils.isLessThan(actualFee, targetFee)) {
            //只从资产为nuls的coinfrom中收取手续费
            actualFee = getFeeDirect(listFrom, targetFee, actualFee);
            if (BigIntegerUtils.isLessThan(actualFee, targetFee)) {
                //如果没收到足够的手续费，则从CoinFrom中资产不是nuls的coin账户中查找nuls余额，并组装新的coinfrom来收取手续费
                if (!getFeeIndirect(listFrom, txSize, targetFee, actualFee)) {
                    //所有from中账户的nuls余额总和都不够支付手续费
                    throw new NulsException(TxErrorCode.INSUFFICIENT_FEE);
                }
            }
        }
        CoinData coinData = new CoinData();
        coinData.setFrom(listFrom);
        coinData.setTo(listTo);
        return coinData;
    }

    /**
     * Only collect fees from CoinFrom's coins whose assets are nuls, and return the actual amount charged.
     * 只从CoinFrom中资产为nuls的coin中收取手续费，返回实际收取的数额
     *
     * @param listFrom  All coins transferred out 转出的所有coin
     * @param targetFee The amount of the fee that needs to be charged 需要收取的手续费数额
     * @param actualFee Actual amount charged 实际收取的数额
     * @return BigInteger The amount of the fee actually charged 实际收取的手续费数额
     * @throws NulsException
     */
    private BigInteger getFeeDirect(List<CoinFrom> listFrom, BigInteger targetFee, BigInteger actualFee) throws NulsException {
        for(CoinFrom coinFrom : listFrom){
            if(TxUtil.isNulsAsset(coinFrom)){
                BigInteger mainAsset = TxUtil.getBalance(coinFrom.getAddress(), TxConstant.NUlS_CHAINID, TxConstant.NUlS_CHAIN_ASSETID);
                //当前还差的手续费
                BigInteger current = targetFee.subtract(actualFee);
                //如果余额大于等于目标手续费，则直接收取全额手续费
                if(BigIntegerUtils.isEqualOrGreaterThan(mainAsset, current)) {
                    coinFrom.setAmount(coinFrom.getAmount().add(current));
                    actualFee = actualFee.add(current);
                    break;
                }else if(BigIntegerUtils.isGreaterThan(mainAsset, BigInteger.ZERO)){
                    coinFrom.setAmount(coinFrom.getAmount().add(mainAsset));
                    actualFee = actualFee.add(mainAsset);
                    continue;
                }
            }
        }
        return actualFee;
    }

    /**
     * 从CoinFrom中资产不为nuls的coin中收取nuls手续费，返回是否收取完成
     * Only collect the nuls fee from the coin in CoinFrom whose assets are not nuls, and return whether the charge is completed.
     *
     * @param listFrom All coins transferred out 转出的所有coin
     * @param txSize Current transaction size
     * @param targetFee Estimated fee
     * @param actualFee actual Fee
     * @return boolean
     * @throws NulsException
     */
    private boolean getFeeIndirect(List<CoinFrom> listFrom, int txSize, BigInteger targetFee, BigInteger actualFee) throws NulsException {
        ListIterator<CoinFrom> iterator = listFrom.listIterator();
        while (iterator.hasNext()){
            CoinFrom coinFrom = iterator.next();
            if(!TxUtil.isNulsAsset(coinFrom)){
                BigInteger mainAsset = TxUtil.getBalance(coinFrom.getAddress(), TxConstant.NUlS_CHAINID, TxConstant.NUlS_CHAIN_ASSETID);
                if(BigIntegerUtils.isEqualOrLessThan(mainAsset, BigInteger.ZERO)){
                    continue;
                }
                CoinFrom feeCoinFrom = new CoinFrom();
                byte[] address = coinFrom.getAddress();
                feeCoinFrom.setAddress(address);
                feeCoinFrom.setNonce(TxUtil.getNonce(address, TxConstant.NUlS_CHAINID, TxConstant.NUlS_CHAIN_ASSETID));
                txSize += feeCoinFrom.size();
                //新增coinfrom，重新计算本交易预计收取的手续费
                targetFee = TransactionFeeCalculator.getCrossTxFee(txSize);
                //当前还差的手续费
                BigInteger current = targetFee.subtract(actualFee);
                //此账户可以支付的手续费
                BigInteger fee = BigIntegerUtils.isEqualOrGreaterThan(mainAsset, current) ? current : mainAsset;

                feeCoinFrom.setLocked(TxConstant.CORSS_TX_LOCKED);
                feeCoinFrom.setAssetsChainId(TxConstant.NUlS_CHAINID);
                feeCoinFrom.setAssetsId(TxConstant.NUlS_CHAIN_ASSETID);
                feeCoinFrom.setAmount(fee);

                iterator.add(feeCoinFrom);
                actualFee = actualFee.add(fee);
                if(BigIntegerUtils.isEqualOrGreaterThan(actualFee, targetFee)){
                    break;
                }
            }
        }
        //最终的实际收取数额大于等于预计收取数额，则可以正确组装CoinData
        if(BigIntegerUtils.isEqualOrGreaterThan(actualFee, targetFee)){
            return true;
        }
        return false;
    }


    /**
     * 跨链交易验证器
     * 交易类型为跨链交易
     * 地址和签名一一对应
     * from里面的资产是否存在，是否可以进行跨链交易
     * 必须包含NULS资产的from
     * @param chainId
     * @param tx
     * @return Result
     */
    @Override
    public Result crossTransactionValidator(int chainId, Transaction tx) {
        Result result = transactionManager.baseTxValidate(chainId, tx);
        if(result.isFailed()){
            return result;
        }
        if(null == tx.getCoinData() || tx.getCoinData().length == 0){
            return Result.getFailed(TxErrorCode.COINDATA_NOT_FOUND);
        }
        try {
            CoinData coinData = tx.getCoinDataInstance();
            Result resultCoinFrom = validateCoinFrom(chainId, coinData.getFrom());
            if(resultCoinFrom.isFailed()){
                return resultCoinFrom;
            }
        } catch (NulsException e) {
            e.printStackTrace();
            return Result.getFailed(TxErrorCode.DESERIALIZE_ERROR);
        }
        return Result.getSuccess(TxErrorCode.SUCCESS);
    }

    /**
     * 验证跨链交易的付款方数据
     * @param chainId
     * @param listFrom
     * @return
     */
    private Result validateCoinFrom(int chainId, List<CoinFrom> listFrom){
        if(null == listFrom || listFrom.size() == 0){
            return Result.getFailed(TxErrorCode.COINFROM_NOT_FOUND);
        }
        boolean hasNulsFrom = false;
        for(CoinFrom coinFrom : listFrom){
            //是否有nuls(手续费)
            if(TxUtil.isNulsAsset(coinFrom)){
                hasNulsFrom = true;
            }
        }
        if(!hasNulsFrom){
            return Result.getFailed(TxErrorCode.INSUFFICIENT_FEE);
        }
        return Result.getSuccess(TxErrorCode.SUCCESS);
    }


    @Override
    public Result crossTransactionCommit(int chainId, Transaction tx, BlockHeaderDigestDTO blockHeader) {
        return Result.getSuccess(TxErrorCode.SUCCESS);
    }

    @Override
    public Result crossTransactionRollback(int chainId, Transaction tx, BlockHeaderDigestDTO blockHeader) {
        return Result.getSuccess(TxErrorCode.SUCCESS);
    }
}
