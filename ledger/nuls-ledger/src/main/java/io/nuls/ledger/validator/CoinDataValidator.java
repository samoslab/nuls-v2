/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.constant.ValidateEnum;
import io.nuls.ledger.model.AccountBalance;
import io.nuls.ledger.model.TempAccountNonce;
import io.nuls.ledger.model.ValidateResult;
import io.nuls.ledger.model.po.*;
import io.nuls.ledger.service.AccountStateService;
import io.nuls.ledger.service.TransactionService;
import io.nuls.ledger.service.UnconfirmedStateService;
import io.nuls.ledger.storage.Repository;
import io.nuls.ledger.utils.CoinDataUtil;
import io.nuls.ledger.utils.LedgerUtil;
import io.nuls.ledger.utils.LoggerUtil;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.model.BigIntegerUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.nuls.ledger.utils.LoggerUtil.logger;

/**
 * validate Coin Data
 * Created by wangkun23 on 2018/11/22.
 * updatge  by lanjinsheng on 2018/12/28.
 *
 * @author lanjinsheng
 */
@Component
public class CoinDataValidator {
    /**
     * key String:chainId
     * value:Map<key是交易hash  value是欲提交交易>
     */
    private Map<String, Map<String, String>> chainsBatchValidateTxMap = new ConcurrentHashMap<String, Map<String, String>>();


    /**
     * key String:chainId
     * value map :key是账号资产 value是待确认支出列表
     */
    private Map<String, Map<String, List<TempAccountNonce>>> chainsAccountNonceMap = new ConcurrentHashMap<String, Map<String, List<TempAccountNonce>>>();
    /**
     * key String:chainId
     * value map :key是账号资产 value是待确认账户
     */
    private Map<String, Map<String, AccountState>> chainsAccountStateMap = new ConcurrentHashMap<String, Map<String, AccountState>>();

    @Autowired
    private AccountStateService accountStateService;

    @Autowired
    private UnconfirmedStateService unconfirmedStateService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private Repository repository;


    /**
     * 区块提交时进行校验
     *
     * @param addressChainId
     * @param tx
     * @return
     */
    public boolean hadValidateTx(int addressChainId, Transaction tx, Map<String, String> batchValidateTxMap) {
        if (null == batchValidateTxMap || null == batchValidateTxMap.get(tx.getHash().toString())) {
            logger(addressChainId).error("txHash = {} is not exist!", tx.getHash().toString());
            return false;
        }
        return true;
    }

    public Map<String, String> getBatchValidateTxMap(int addressChainId) {
        return chainsBatchValidateTxMap.get(String.valueOf(addressChainId));
    }

    public Map<String, List<TempAccountNonce>> getAccountBalanceValidateMap(int addressChainId) {
        return chainsAccountNonceMap.get(String.valueOf(addressChainId));
    }

    public Map<String, AccountState> getAccountValidateMap(int addressChainId) {
        return chainsAccountStateMap.get(String.valueOf(addressChainId));
    }

    /**
     * 校验nonce的连续性
     *
     * @param txNonce
     * @param fromCoinNonce
     * @return
     */
    public boolean validateAndAddNonces(AccountBalance accountBalance, byte[] txNonce, byte[] fromCoinNonce, BigInteger amount) {
        byte[] preNonce = null;
        if (0 == accountBalance.getNonces().size()) {
            //初次校验，取数据库里的值
            preNonce = accountBalance.getPreAccountState().getNonce();
        } else {
            AmountNonce amountNonce = accountBalance.getNonces().get(accountBalance.getNonces().size() - 1);
            preNonce = amountNonce.getNonce();
        }
        if (LedgerUtil.equalsNonces(preNonce, fromCoinNonce)) {
            AmountNonce amountNonce = new AmountNonce(fromCoinNonce, txNonce, amount);
            accountBalance.getNonces().add(amountNonce);
            return true;
        }
        return false;
    }

    /**
     * 开始批量校验
     */
    public boolean beginBatchPerTxValidate(int chainId) {
        Map<String, String> batchValidateTxMap = getBatchValidateTxMap(chainId);
        if (null == batchValidateTxMap) {
            batchValidateTxMap = new ConcurrentHashMap<>(1024);
            chainsBatchValidateTxMap.put(String.valueOf(chainId), batchValidateTxMap);
        }
        Map<String, List<TempAccountNonce>> accountBalanceValidateTxMap = getAccountBalanceValidateMap(chainId);
        if (null == accountBalanceValidateTxMap) {
            accountBalanceValidateTxMap = new ConcurrentHashMap<>(1024);
            chainsAccountNonceMap.put(String.valueOf(chainId), accountBalanceValidateTxMap);
        }
        Map<String, AccountState> accountStateMap = getAccountValidateMap(chainId);
        if (null == accountStateMap) {
            accountStateMap = new ConcurrentHashMap<>(1024);
            chainsAccountStateMap.put(String.valueOf(chainId), accountStateMap);
        }

        batchValidateTxMap.clear();
        accountBalanceValidateTxMap.clear();
        accountStateMap.clear();
        return true;

    }


    /**
     * 开始批量校验,整个区块校验，场景：接收到的外部的区块包
     */
    public boolean blockValidate(int chainId, long height, List<Transaction> txs) {
        LoggerUtil.logger(chainId).debug("peer blocksValidate chainId={},height={},txsNumber={}", chainId, height, txs.size());
        Map<String, String> batchValidateTxMap = new HashMap(1024);
        Map<String, List<TempAccountNonce>> accountValidateTxMap = new HashMap<>(1024);
        Map<String, AccountState> accountStateMap = new HashMap<>(1024);
        for (Transaction tx : txs) {
            ValidateResult validateResult = blockTxsValidate(chainId, tx, batchValidateTxMap, accountValidateTxMap, accountStateMap);
            if (ValidateEnum.SUCCESS_CODE.getValue() != validateResult.getValidateCode()) {
                LoggerUtil.logger(chainId).error("code={},msg={}", validateResult.getValidateCode(), validateResult.getValidateCode());
                return false;
            }
        }
        //遍历余额判断
        for (Map.Entry<String, AccountState> entry : accountStateMap.entrySet()) {
            //缓存数据
            if (BigIntegerUtils.isLessThan(entry.getValue().getAvailableAmount(), BigInteger.ZERO)) {
                //余额不足
                logger(chainId).info("{}=={}=={}==balance is not enough", entry.getValue().getAddress(), entry.getValue().getAssetChainId(), entry.getValue().getAssetId());
                return false;
            }
        }
        return true;

    }


    /**
     * 批量逐笔校验
     * 批量校验 非解锁交易，余额校验与coindata校验一致,从数据库获取金额校验。
     * nonce校验与coindata不一样，是从批量累计中获取，进行批量连贯性校验。
     * 解锁交易的验证与coidate一致。
     * <p>
     * 批量校验的过程中所有错误按恶意双花来进行处理，
     * 返回VALIDATE_DOUBLE_EXPENSES_CODE
     *
     * @param chainId
     * @param tx
     * @return ValidateResult
     */
    public ValidateResult bathValidatePerTx(int chainId, Transaction tx) {
        Map<String, String> batchValidateTxMap = getBatchValidateTxMap(chainId);
        Map<String, List<TempAccountNonce>> accountBalanceValidateTxMap = getAccountBalanceValidateMap(chainId);
        return confirmedTxValidate(chainId, tx, batchValidateTxMap, accountBalanceValidateTxMap);
    }

    /**
     * 未确认交易数据处理
     *
     * @param transaction
     */

    public ValidateResult verifyCoinData(int addressChainId, Transaction transaction) throws Exception {
        //直接更新未确认交易
        CoinData coinData = CoinDataUtil.parseCoinData(transaction.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接返回
            return ValidateResult.getSuccess();
        }
        /*未确认交易的校验*/
        ValidateResult validateResult = validateCoinData(addressChainId, transaction);
        if (!validateResult.isSuccess()) {
            LoggerUtil.logger(addressChainId).error("validateResult = {}={}", validateResult.getValidateCode(), validateResult.getValidateDesc());
            return validateResult;
        }
        return ValidateResult.getSuccess();
    }

    /**
     * 打包单笔校验
     *
     * @param chainId
     * @param txHash
     * @param nonce8Bytes
     * @param coinFroms
     * @param accountValidateTxMap
     * @param accountStateMap
     * @param balanceValidateMap
     * @return
     */
    private ValidateResult analysisFromCoinPerTx(int chainId, String txHash, byte[] nonce8Bytes, List<CoinFrom> coinFroms, Map<String, List<TempAccountNonce>> accountValidateTxMap, Map<String, AccountState> accountStateMap, Map<String, AccountState> balanceValidateMap) {
        for (CoinFrom coinFrom : coinFroms) {
            if (LedgerUtil.isNotLocalChainAccount(chainId, coinFrom.getAddress())) {
                //非本地网络账户地址,不进行处理
                continue;
            }
            String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
            String assetKey = LedgerUtil.getKeyStr(address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            AccountState accountState = accountStateMap.get(assetKey);
            if (null == accountState) {
                accountState = accountStateService.getAccountStateReCal(AddressTool.getStringAddressByBytes(coinFrom.getAddress()), chainId, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                accountStateMap.put(assetKey, accountState);
            }
            accountState.addTotalFromAmount(coinFrom.getAmount());
            balanceValidateMap.put(assetKey, accountState);
            //判断是否是解锁操作
            if (coinFrom.getLocked() == 0) {
                ValidateResult validateResult = isValidateCommonTxBatch(accountState, coinFrom, nonce8Bytes, accountValidateTxMap);
                if (ValidateEnum.SUCCESS_CODE.getValue() != validateResult.getValidateCode()) {
                    return validateResult;
                }
            } else {
                //解锁交易，需要从from 里去获取需要的高度数据或时间数据，进行校验
                //解锁交易只需要从已确认的数据中去获取数据进行校验
                if (!isValidateFreezeTx(coinFrom.getLocked(), accountState, coinFrom.getAmount(), coinFrom.getNonce())) {
                    return ValidateResult.getResult(ValidateEnum.DOUBLE_EXPENSES_CODE, new String[]{accountState.getAddress(), LedgerUtil.getNonceEncode(coinFrom.getNonce()), "validate fail"});
                }
            }
        }
        return ValidateResult.getSuccess();
    }

    /**
     * 只是对金额进行处理
     * 打包单笔校验
     *
     * @param chainId
     * @param coinTos
     * @param accountStateMap
     * @return
     */
    private ValidateResult analysisToCoinPerTx(int chainId, List<CoinTo> coinTos, Map<String, AccountState> accountStateMap) {
        for (CoinTo coinTo : coinTos) {
            if (LedgerUtil.isNotLocalChainAccount(chainId, coinTo.getAddress())) {
                //非本地网络账户地址,不进行处理
                continue;
            }
            //判断是否是解锁操作
            if (coinTo.getLockTime() == 0) {
                String address = AddressTool.getStringAddressByBytes(coinTo.getAddress());
                String assetKey = LedgerUtil.getKeyStr(address, coinTo.getAssetsChainId(), coinTo.getAssetsId());
                AccountState accountState = accountStateMap.get(assetKey);
                if (null == accountState) {
                    accountState = accountStateService.getAccountStateReCal(AddressTool.getStringAddressByBytes(coinTo.getAddress()), chainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
                    accountStateMap.put(assetKey, accountState);
                }
                accountState.addTotalToAmount(coinTo.getAmount());
            }
        }
        return ValidateResult.getSuccess();
    }

    /**
     * @param chainId
     * @param tx
     * @param batchValidateTxMap
     * @return
     */
    public ValidateResult confirmedTxValidate(int chainId, Transaction tx, Map<String, String> batchValidateTxMap, Map<String, List<TempAccountNonce>> accountValidateTxMap) {
        Map<String, AccountState> accountStateMap = getAccountValidateMap(chainId);
        Map<String, AccountState> balanceValidateMap = new HashMap<>(64);
        //先校验，再逐笔放入缓存
        //交易的 hash值如果已存在，返回false，交易的from coin nonce 如果不连续，则存在双花。
        String txHash = tx.getHash().toString();
        if (null == batchValidateTxMap || null != batchValidateTxMap.get(txHash)) {
            logger(chainId).error("{} tx exist!", txHash);
            return ValidateResult.getResult(ValidateEnum.TX_EXIST_CODE, new String[]{"--", txHash});
        }
        //判断是否已经在打包或已完成的交易
        try {
            if (transactionService.hadTxExist(chainId, txHash)) {
                return ValidateResult.getResult(ValidateEnum.TX_EXIST_CODE, new String[]{"--", txHash});
            }
        } catch (Exception e) {
            LoggerUtil.logger(chainId).error(e);
            return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{"--", txHash, "unknown error"});
        }
        CoinData coinData = CoinDataUtil.parseCoinData(tx.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接返回
            batchValidateTxMap.put(tx.getHash().toString(), tx.getHash().toString());
            return ValidateResult.getSuccess();
        }
        List<CoinFrom> coinFroms = coinData.getFrom();
        List<CoinTo> coinTos = coinData.getTo();

        byte[] nonce8Bytes = LedgerUtil.getNonceByTx(tx);
        ValidateResult validateResult = analysisFromCoinPerTx(chainId, txHash, nonce8Bytes, coinFroms, accountValidateTxMap, accountStateMap, balanceValidateMap);
        if (!validateResult.isSuccess()) {
            return validateResult;
        }
        ValidateResult toCoinValidateResult = analysisToCoinPerTx(chainId, coinTos, accountStateMap);
        if (!toCoinValidateResult.isSuccess()) {
            return validateResult;
        }
        //遍历余额判断
        for (Map.Entry<String, AccountState> entry : balanceValidateMap.entrySet()) {
            //缓存数据
            if (BigIntegerUtils.isLessThan(entry.getValue().getAvailableAmount(), BigInteger.ZERO)) {
                //余额不足
                logger(chainId).info("{}=={}=={}==balance is not enough", entry.getValue().getAddress(), entry.getValue().getAssetChainId(), entry.getValue().getAssetId());
                return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{entry.getValue().getAddress(), "--", "balance is not enough"});
            }
        }
        batchValidateTxMap.put(txHash, txHash);
        return ValidateResult.getSuccess();
    }

    /**
     * 进行普通交易的coindata 校验，未确认校验的提交校验
     *
     * @param accountState
     * @param address
     * @param fromAmount
     * @param fromNonce
     * @return
     */
    private ValidateResult validateCommonCoinData(AccountState accountState, String address, BigInteger fromAmount, byte[] fromNonce, boolean containUncomfirmedAmount) {
        AccountStateUnconfirmed accountStateUnconfirmed = null;
        if (containUncomfirmedAmount) {
            accountStateUnconfirmed = unconfirmedStateService.getUnconfirmedInfo(accountState);
        } else {
            accountStateUnconfirmed = unconfirmedStateService.getUnconfirmedJustNonce(accountState);
        }
        byte[] preNonce = null;
        BigInteger amount = BigInteger.ZERO;
        if (null == accountStateUnconfirmed) {
            //新建
            preNonce = accountState.getNonce();
            amount = accountState.getAvailableAmount();
        } else {
            preNonce = accountStateUnconfirmed.getNonce();
            amount = accountState.getAvailableAmount().subtract(accountStateUnconfirmed.getAmount());
        }
        String fromNonceStr = LedgerUtil.getNonceEncode(fromNonce);
        if (BigIntegerUtils.isLessThan(amount, fromAmount)) {
            logger(accountState.getAddressChainId()).info("balance is not enough");
            return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, fromNonceStr, "balance is not enough"});
        }
        //直接连接上未确认nonce了
        if (LedgerUtil.equalsNonces(fromNonce, preNonce)) {
            return ValidateResult.getSuccess();
        }

        try {
            //数据库已经不为初始值了，则这笔交易可以认为双花
            if(LedgerUtil.equalsNonces(fromNonce,LedgerConstant.INIT_NONCE_BYTE)){
                logger(accountState.getAddressChainId()).info("DOUBLE_EXPENSES_CODE address={},fromNonceStr={}", address, fromNonceStr);
                return ValidateResult.getResult(ValidateEnum.DOUBLE_EXPENSES_CODE, new String[]{address, fromNonceStr});
            }
            //上面没连接上，但是fromNonce又存储过，则双花了
            if (transactionService.hadCommit(accountState.getAddressChainId(), LedgerUtil.getAccountNoncesStrKey(address, accountState.getAssetChainId(), accountState.getAssetId(), fromNonceStr))) {
                logger(accountState.getAddressChainId()).info("DOUBLE_EXPENSES_CODE address={},fromNonceStr={}", address, fromNonceStr);
                return ValidateResult.getResult(ValidateEnum.DOUBLE_EXPENSES_CODE, new String[]{address, fromNonceStr});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //孤儿交易了，这笔交易不清楚状况，是孤儿
        return ValidateResult.getResult(ValidateEnum.ORPHAN_CODE, new String[]{address, fromNonceStr, LedgerUtil.getNonceEncode(preNonce)});
    }

    /**
     * 进行普通交易的批量校验
     * 与单笔交易校验不同的是，批量校验要校验批量池中的nonce连续性
     *
     * @param accountState
     * @param coinFrom
     * @param txNonce
     * @return
     */
    private ValidateResult isValidateCommonTxBatch(AccountState accountState, CoinFrom coinFrom, byte[] txNonce,
                                                   Map<String, List<TempAccountNonce>> accountValidateTxMap) {
        int chainId = accountState.getAddressChainId();
        String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
        String assetKey = LedgerUtil.getKeyStr(address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
        String fromCoinNonceStr = LedgerUtil.getNonceEncode(coinFrom.getNonce());
        if (LedgerUtil.equalsNonces(coinFrom.getNonce(), txNonce)) {
            //nonce 重复了
            logger(chainId).info("{}=={}=={}== nonce is repeat", AddressTool.getStringAddressByBytes(coinFrom.getAddress()), coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, LedgerUtil.getNonceEncode(coinFrom.getNonce()), "nonce repeat"});
        }
        //不是解锁操作
        //从批量校验池中获取缓存交易
        List<TempAccountNonce> list = accountValidateTxMap.get(assetKey);
        if (null == list) {
            //从头开始处理
            if (!LedgerUtil.equalsNonces(accountState.getNonce(), coinFrom.getNonce())) {
                logger(chainId).error("批量校验失败(BatchValidate failed)： isValidateCommonTxBatch {}=={}=={}==nonce is error!dbNonce:{}!=fromNonce:{}", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId(), LedgerUtil.getNonceEncode(accountState.getNonce()), fromCoinNonceStr);
                //判断是否fromNonce是否已经存储了,如果存储了，则这笔是异常交易双花
                try {
                    if (transactionService.hadCommit(chainId, LedgerUtil.getAccountNoncesStringKey(coinFrom, coinFrom.getNonce()))) {
                        return ValidateResult.getResult(ValidateEnum.DOUBLE_EXPENSES_CODE, new String[]{address, LedgerUtil.getNonceEncode(coinFrom.getNonce())});
                    } else {
                        if(LedgerUtil.equalsNonces(coinFrom.getNonce(),LedgerConstant.INIT_NONCE_BYTE)){
                            return ValidateResult.getResult(ValidateEnum.DOUBLE_EXPENSES_CODE, new String[]{address, LedgerUtil.getNonceEncode(coinFrom.getNonce())});
                        }
                        return ValidateResult.getResult(ValidateEnum.ORPHAN_CODE, new String[]{address, fromCoinNonceStr, LedgerUtil.getNonceEncode(accountState.getNonce())});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    LoggerUtil.logger(chainId).error(e);
                    return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, fromCoinNonceStr, "validate Exception"});
                }
            }
            list = new ArrayList<>();
            list.add(new TempAccountNonce(assetKey, coinFrom.getNonce(), txNonce));
            accountValidateTxMap.put(assetKey, list);
        } else {
            //从已有的缓存数据中获取对象进行操作,nonce必须连贯
            TempAccountNonce tempAccountState = list.get(list.size() - 1);
            if (!LedgerUtil.equalsNonces(tempAccountState.getNextNonce(), coinFrom.getNonce())) {
                logger(chainId).info("isValidateCommonTxBatch {}=={}=={}==nonce is error!tempNonce:{}!=fromNonce:{}", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId(), tempAccountState.getNextNonce(), fromCoinNonceStr);
                return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, fromCoinNonceStr, "last pool nonce=" + tempAccountState.getNextNonce()});
            }
            list.add(new TempAccountNonce(assetKey, coinFrom.getNonce(), txNonce));
        }
        return ValidateResult.getSuccess();
    }

    private ValidateResult analysisFromCoinBlokTx(int chainId, String txHash, byte[] txNonce, List<CoinFrom> coinFroms, Map<String, List<TempAccountNonce>> accountValidateTxMap, Map<String, AccountState> accountStateMap) {
        for (CoinFrom coinFrom : coinFroms) {
            if (LedgerUtil.isNotLocalChainAccount(chainId, coinFrom.getAddress())) {
                //非本地网络账户地址,不进行处理
                continue;
            }
            String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
            String assetKey = LedgerUtil.getKeyStr(address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            AccountState accountState = accountStateMap.get(assetKey);
            if (null == accountState) {
                accountState = accountStateService.getAccountStateReCal(AddressTool.getStringAddressByBytes(coinFrom.getAddress()), chainId, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                accountStateMap.put(assetKey, accountState);
            }

            //判断是否是解锁操作
            if (coinFrom.getLocked() == 0) {
                //不是解锁操作
                //从批量校验池中获取缓存交易
                List<TempAccountNonce> list = accountValidateTxMap.get(assetKey);
                String fromCoinNonce = LedgerUtil.getNonceEncode(coinFrom.getNonce());
                //余额累计
                accountState.addTotalFromAmount(coinFrom.getAmount());
                if (LedgerUtil.equalsNonces(coinFrom.getNonce(), txNonce)) {
                    //nonce 重复了
                    logger(chainId).info("{}=={}=={}== nonce is repeat", AddressTool.getStringAddressByBytes(coinFrom.getAddress()), coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                    return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, fromCoinNonce, "nonce repeat"});
                }
                if (null == list) {
                    //从头开始处理
                    if (!LedgerUtil.equalsNonces(accountState.getNonce(), coinFrom.getNonce())) {
                        logger(chainId).error("校验失败(isBlockValidateCommonTx failed)：{}=={}=={}==nonce is error!dbNonce:{}!=fromNonce:{}", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId(), accountState.getNonce(), fromCoinNonce);
                        //判断是否fromNonce是否已经存储了,如果存储了，则这笔是异常交易双花
                        return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, fromCoinNonce, "dbNonce=" + accountState.getNonce()});
                    }
                    list = new ArrayList<>();
                    list.add(new TempAccountNonce(assetKey, coinFrom.getNonce(), txNonce));
                    accountValidateTxMap.put(assetKey, list);
                } else {
                    //从已有的缓存数据中获取对象进行操作,nonce必须连贯
                    TempAccountNonce tempAccountState = list.get(list.size() - 1);
                    if (!LedgerUtil.equalsNonces(tempAccountState.getNextNonce(), coinFrom.getNonce())) {
                        logger(chainId).info("isValidateCommonTxBatch {}=={}=={}==nonce is error!tempNonce:{}!=fromNonce:{}", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId(), tempAccountState.getNextNonce(), fromCoinNonce);
                        return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, fromCoinNonce, "last pool nonce=" + tempAccountState.getNextNonce()});
                    }
                    list.add(new TempAccountNonce(assetKey, coinFrom.getNonce(), txNonce));
                }
            } else {
                //解锁交易，需要从from 里去获取需要的高度数据或时间数据，进行校验
                //解锁交易只需要从已确认的数据中去获取数据进行校验
                if (!isValidateFreezeTx(coinFrom.getLocked(), accountState, coinFrom.getAmount(), coinFrom.getNonce())) {
                    return ValidateResult.getResult(ValidateEnum.DOUBLE_EXPENSES_CODE, new String[]{accountState.getAddress(), LedgerUtil.getNonceEncode(coinFrom.getNonce()), "validate fail"});
                }
            }
        }
        return ValidateResult.getSuccess();
    }

    public ValidateResult blockTxsValidate(int chainId, Transaction tx, Map<String, String> batchValidateTxMap, Map<String, List<TempAccountNonce>> accountValidateTxMap, Map<String, AccountState> accountStateMap) {
        //先校验，再逐笔放入缓存
        //交易的 hash值如果已存在，返回false，交易的from coin nonce 如果不连续，则存在双花。
        String txHash = tx.getHash().toString();
        if (null == batchValidateTxMap || null != batchValidateTxMap.get(txHash)) {
            logger(chainId).error("{} tx exist!", txHash);
            return ValidateResult.getResult(ValidateEnum.TX_EXIST_CODE, new String[]{"--", txHash});
        }
        try {
            if (transactionService.hadTxExist(chainId, txHash)) {
                logger(chainId).error("{} tx exist!", txHash);
                return ValidateResult.getResult(ValidateEnum.TX_EXIST_CODE, new String[]{"--", txHash});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CoinData coinData = CoinDataUtil.parseCoinData(tx.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接返回
            batchValidateTxMap.put(txHash, txHash);
            return ValidateResult.getSuccess();
        }
        List<CoinFrom> coinFroms = coinData.getFrom();
        List<CoinTo> coinTos = coinData.getTo();
        byte[] txNonce = LedgerUtil.getNonceByTx(tx);
        ValidateResult fromCoinsValidateResult = analysisFromCoinBlokTx(chainId, txHash, txNonce, coinFroms, accountValidateTxMap, accountStateMap);
        if (!fromCoinsValidateResult.isSuccess()) {
            return fromCoinsValidateResult;
        }
        ValidateResult toCoinValidateResult = analysisToCoinPerTx(chainId, coinTos, accountStateMap);
        if (!toCoinValidateResult.isSuccess()) {
            return toCoinValidateResult;
        }
        batchValidateTxMap.put(txHash, txHash);
        return ValidateResult.getSuccess();
    }

    /**
     * 进行解锁交易的校验
     *
     * @param locked
     * @param accountState
     * @param fromAmount
     * @param fromNonce
     * @return
     */
    private boolean isValidateFreezeTx(byte locked, AccountState accountState, BigInteger fromAmount, byte[] fromNonce) {
        boolean isValidate = false;
        int chainId = accountState.getAddressChainId();
        //解锁交易，校验是否存在该笔交易
        if (locked == -1) {
            //时间解锁
            List<FreezeLockTimeState> list = accountState.getFreezeLockTimeStates();
            for (FreezeLockTimeState freezeLockTimeState : list) {
                LoggerUtil.logger(chainId).debug("UnlockedValidate-time: address={},assetChainId={},assetId={},nonceFrom={},nonceDb={},amountFrom={},amountDb={}",
                        accountState.getAddress(), accountState.getAssetChainId(), accountState.getAssetId(), fromNonce, freezeLockTimeState.getNonce(), fromAmount, freezeLockTimeState.getAmount());
                if (LedgerUtil.equalsNonces(freezeLockTimeState.getNonce(), fromNonce) && freezeLockTimeState.getAmount().compareTo(fromAmount) == 0) {
                    //找到交易
                    isValidate = true;
                    break;
                }
            }
        } else if (locked == 1) {
            //高度解锁
            List<FreezeHeightState> list = accountState.getFreezeHeightStates();
            for (FreezeHeightState freezeHeightState : list) {
                LoggerUtil.logger(chainId).debug("UnlockedValidate-height: address={},assetChainId={},assetId={},nonceFrom={},nonceDb={},amountFrom={},amountDb={}",
                        accountState.getAddress(), accountState.getAssetChainId(), accountState.getAssetId(), fromNonce, freezeHeightState.getNonce(), fromAmount, freezeHeightState.getAmount());
                if (LedgerUtil.equalsNonces(freezeHeightState.getNonce(), fromNonce) && freezeHeightState.getAmount().compareTo(fromAmount) == 0) {
                    //找到交易
                    isValidate = true;
                    break;
                }
            }
        }
        LoggerUtil.logger(chainId).debug("isValidateFreezeTx: address={},assetChainId={},assetId={},isValidate={}",
                accountState.getAddress(), accountState.getAssetChainId(), accountState.getAssetId(), isValidate);
        return isValidate;
    }


    /**
     * 进行coinData值的校验,在本地交易产生时候进行的校验
     * 即只有未确认交易的校验使用到
     *
     * @param addressChainId
     * @param tx
     * @return
     */
    public ValidateResult validateCoinData(int addressChainId, Transaction tx) throws Exception {
        if (transactionService.hadCommit(addressChainId, tx.getHash().toString())) {
            return ValidateResult.getResult(ValidateEnum.TX_EXIST_CODE, new String[]{"--", tx.getHash().toString()});
        }
        CoinData coinData = CoinDataUtil.parseCoinData(tx.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接返回
            return ValidateResult.getSuccess();
        }
        /*
         * 先校验nonce值是否正常
         */
        List<CoinFrom> coinFroms = coinData.getFrom();
        for (CoinFrom coinFrom : coinFroms) {
            if (LedgerUtil.isNotLocalChainAccount(addressChainId, coinFrom.getAddress())) {
                //非本地网络账户地址,不进行处理
                continue;
            }
            String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
            AccountState accountState = accountStateService.getAccountStateReCal(address, addressChainId, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            //普通交易
            if (coinFrom.getLocked() == 0) {
                return validateCommonCoinData(accountState, address, coinFrom.getAmount(), coinFrom.getNonce(),true);
            } else {
                if (!isValidateFreezeTx(coinFrom.getLocked(), accountState, coinFrom.getAmount(), coinFrom.getNonce())) {
                    //确认交易未找到冻结的交易
                    return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, LedgerUtil.getNonceEncode(coinFrom.getNonce()), "freeze tx is not exist"});
                }
            }
        }
        return ValidateResult.getSuccess();
    }

    public ValidateResult analysisCoinData(int addressChainId, Transaction tx, Map<String, TxUnconfirmed> accountsMap, byte[] txNonce) throws Exception {
        if (transactionService.hadCommit(addressChainId, tx.getHash().toString())) {
            return ValidateResult.getResult(ValidateEnum.TX_EXIST_CODE, new String[]{"--", tx.getHash().toString()});
        }
        CoinData coinData = CoinDataUtil.parseCoinData(tx.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接返回
            return ValidateResult.getSuccess();
        }
        /*
         * 先校验nonce值是否正常
         */
        List<CoinFrom> coinFroms = coinData.getFrom();

        for (CoinFrom coinFrom : coinFroms) {
            if (LedgerUtil.isNotLocalChainAccount(addressChainId, coinFrom.getAddress())) {
                //非本地网络账户地址,不进行处理
                continue;
            }
            String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
            int assetChainId = coinFrom.getAssetsChainId();
            int assetId = coinFrom.getAssetsId();
            String accountKey = LedgerUtil.getKeyStr(address, assetChainId, assetId);
            AccountState accountState = accountStateService.getAccountStateReCal(address, addressChainId, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            //普通交易
            if (coinFrom.getLocked() == 0) {
                ValidateResult validateResult = validateCommonCoinData(accountState, address, coinFrom.getAmount(), coinFrom.getNonce(),false);
                if (validateResult.isSuccess()) {
                    CoinDataUtil.calTxFromAmount(addressChainId, accountsMap, coinFrom, txNonce, accountKey);
                } else {
                    return validateResult;
                }
            } else {
                if (!isValidateFreezeTx(coinFrom.getLocked(), accountState, coinFrom.getAmount(), coinFrom.getNonce())) {
                    //确认交易未找到冻结的交易
                    return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{address, LedgerUtil.getNonceEncode(coinFrom.getNonce()), "freeze tx is not exist"});
                }
            }
        }
        return ValidateResult.getSuccess();
    }

    /**
     * 批量打包单笔交易回滚处理
     */
    public boolean rollbackTxValidateStatus(int chainId, Transaction tx) {
        Map<String, List<TempAccountNonce>> accountBalanceValidateTxMap = getAccountBalanceValidateMap(chainId);
        String txHash = tx.getHash().toString();
        if (null == chainsBatchValidateTxMap.get(txHash)) {
            logger(chainId).info("{} tx not exist!", txHash);
            return true;
        }
        CoinData coinData = CoinDataUtil.parseCoinData(tx.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接移除返回.
            chainsBatchValidateTxMap.remove(txHash);
            return true;
        }
        List<CoinFrom> coinFroms = coinData.getFrom();
        byte[] nonce8Bytes = LedgerUtil.getNonceByTx(tx);
        for (CoinFrom coinFrom : coinFroms) {
            if (LedgerUtil.isNotLocalChainAccount(chainId, coinFrom.getAddress())) {
                //非本地网络账户地址,不进行处理
                continue;
            }
            //判断是否是解锁操作
            if (coinFrom.getLocked() == 0) {
                String assetKey = LedgerUtil.getKeyStr(AddressTool.getStringAddressByBytes(coinFrom.getAddress()), coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                //回滚accountBalanceValidateTxMap缓存数据
                List<TempAccountNonce> list = accountBalanceValidateTxMap.get(assetKey);
                if (null == list) {
                    continue;
                } else {
                    TempAccountNonce tempAccountState = list.get(list.size() - 1);
                    if (LedgerUtil.equalsNonces(tempAccountState.getNextNonce(), nonce8Bytes)) {
                        list.remove(list.size() - 1);
                    }
                }
            } else {
                //解锁交易,暂无缓存记录
            }
        }
        chainsBatchValidateTxMap.remove(tx.getHash().toString());
        return true;
    }
}
