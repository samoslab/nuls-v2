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
import io.nuls.base.data.Transaction;
import io.nuls.ledger.db.Repository;
import io.nuls.ledger.model.AccountState;
import io.nuls.ledger.model.FreezeLockTimeState;
import io.nuls.ledger.model.TempAccountState;
import io.nuls.ledger.model.ValidateResult;
import io.nuls.ledger.service.AccountStateService;
import io.nuls.ledger.utils.CoinDataUtils;
import io.nuls.ledger.utils.LedgerUtils;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.ByteUtils;
import io.nuls.tools.data.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * validate Coin Data
 * Created by wangkun23 on 2018/11/22.
 * updatge  by lanjinsheng on 2018/12/28.
 */
@Component
public class CoinDataValidator {
    final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * key是交易hash  value是欲提交交易
     */
    private Map<String,Transaction> batchValidateTxMap = new ConcurrentHashMap<>();
    /**
     * key是账号资产 value是待确认支出列表
     */
    private Map<String,List<TempAccountState>> accountBalanceValidateTxMap = new ConcurrentHashMap<>();

    private static final int VALIDATE_SUCCESS_CODE  = 1;
    private static final String VALIDATE_SUCCESS_DESC = "success";
    private static final int VALIDATE_ORPHAN_CODE = 2;
    private static final String VALIDATE_ORPHAN_DESC = "address {},nonce {} is orphan transaction";
    private static final int VALIDATE_DOUBLE_EXPENSES_CODE = 3;
    private static final String VALIDATE_DOUBLE_EXPENSES_DESC = "address {},nonce {} is double expenses";
    private static final int VALIDATE_FAIL_CODE = 4;
    private static final String VALIDATE_FAIL_DESC = "address {},nonce {} validate fail";


    @Autowired
    private AccountStateService accountStateService;
    @Autowired
    private Repository repository;

    public boolean hadValidateTx(Transaction tx){
        //TODO:hash 值校验

        if(null == batchValidateTxMap.get(tx.getHash().toString())){
            return false;
        }
        return true;
    }
    /**
     * 开始批量校验
     */
    public void beginBatchPerTxValidate(){

        batchValidateTxMap.clear();
        accountBalanceValidateTxMap.clear();
        //清空缓存
        repository.clearBatchValidateTx();
    }

    /**
     * 批量逐笔校验
     * @param tx
     * @return boolean
     */
    public boolean bathValidatePerTx(Transaction tx){
        //TODO:交易Hash值校验

        //先校验，再逐笔放入缓存
        //交易的 hash值如果已存在，返回false，交易的from coin nonce 如果不连续，则存在双花。
        if(null != batchValidateTxMap.get(tx.getHash().toString())){
            return false;
        }

        CoinData coinData =  CoinDataUtils.parseCoinData(tx.getCoinData());
        List<CoinFrom> coinFroms = coinData.getFrom();
        byte [] nonce8Bytes = ByteUtils.copyOf(tx.getHash().getDigestBytes(), 8);
        String nonce8BytesStr = HexUtil.encode(nonce8Bytes);
        for(CoinFrom coinFrom:coinFroms) {
            String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
            String assetKey = LedgerUtils.getKey(address,coinFrom.getAssetsChainId(),coinFrom.getAssetsId());
            String nonce =HexUtil.encode(coinFrom.getNonce());
            //判断是否是解锁from
            if(coinFrom.getLocked() == 0 ) {
                List<TempAccountState> list = accountBalanceValidateTxMap.get(assetKey);
                if (null == list) {
                    //从头开始处理
                    AccountState accountState = accountStateService.getAccountState(address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                    if (!accountState.getNonce().equalsIgnoreCase(nonce)) {
                        logger.info("{}=={}=={}==nonce is error!{}!={}", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId(),accountState.getNonce(),nonce);
                        return false;
                    }
                    //余额判断
                    if (accountState.getAvailableAmount().compareTo(coinFrom.getAmount()) == -1) {
                        logger.info("{}=={}=={}==balance is not enough", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                        return false;
                    }
                    list = new ArrayList<>();
                    BigInteger balance = accountState.getAvailableAmount().subtract(coinFrom.getAmount());
                    list.add(new TempAccountState(assetKey, nonce, nonce8BytesStr,balance));
                    accountBalanceValidateTxMap.put(assetKey, list);
                } else {
                    TempAccountState tempAccountState = list.get(list.size()-1);
                    if(!tempAccountState.getNextNonce().equalsIgnoreCase(nonce)){
                        logger.info("{}=={}=={}==nonce is error!{}!={}", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId(),tempAccountState.getNextNonce(),nonce);
                        return false;
                    }
                    //余额判断
                    if (tempAccountState.getBalance().compareTo(coinFrom.getAmount()) == -1) {
                        logger.info("{}=={}=={}==balance is not enough", address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
                        return false;
                    }
                    list.add(new TempAccountState(assetKey, nonce, nonce8BytesStr,tempAccountState.getBalance().subtract(coinFrom.getAmount())));
                }
            }else{
                //TODO:解锁交易的from 校验
            }
        }
        batchValidateTxMap.put(tx.getHash().toString(),tx);
        repository.putBatchValidateTx(tx.getHash().getDigestBytes(),tx);
        return true;
    }

    /**
     * 进行coinData值的校验
     * 未进行全文的nonce检索,所以不排除双花被当成孤儿交易返回。
     * @param tx
     * @return
     */
    public ValidateResult validateCoinData(Transaction tx) {
        CoinData coinData =  CoinDataUtils.parseCoinData(tx.getCoinData());
        byte [] nonce8Bytes = ByteUtils.copyOf(tx.getHash().getDigestBytes(), 8);
        String nonce8BytesStr =  HexUtil.encode(nonce8Bytes);
        /*
         * 先校验nonce值是否正常
         */
        List<CoinFrom> coinFroms = coinData.getFrom();
        for(CoinFrom coinFrom:coinFroms){
            String address = AddressTool.getStringAddressByBytes(coinFrom.getAddress());
            String nonce = HexUtil.encode(coinFrom.getNonce());
            AccountState accountState = accountStateService.getAccountState(address, coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            if(accountState == null){
                ValidateResult validateResult = new ValidateResult(VALIDATE_FAIL_CODE,String.format(VALIDATE_FAIL_DESC,address,nonce));
                return validateResult;
            }
            //初始花费交易,nonce为 0;已兼容处理。

            //普通交易
            if(coinFrom.getLocked() == 0){
                //余额判断
                if(accountState.getAvailableAmount().compareTo(coinFrom.getAmount())== -1 ){
                    logger.info("{}=={}=={}==balance is not enough",address,coinFrom.getAssetsChainId(),coinFrom.getAssetsId());
                    ValidateResult validateResult = new ValidateResult(VALIDATE_FAIL_CODE,String.format(VALIDATE_FAIL_DESC,address,nonce));
                    return validateResult;
                }
                //存在未确认交易
                if(StringUtils.isNotBlank(accountState.getUnconfirmedNonce())){
                    if(!accountState.getUnconfirmedNonce().equalsIgnoreCase(nonce)){
                        //如果存在未确认交易，而又与账户确认的nonce状态一致，则双花
                        if(accountState.getNonce().equalsIgnoreCase(nonce)){
                            ValidateResult validateResult = new ValidateResult(VALIDATE_DOUBLE_EXPENSES_CODE,String.format(VALIDATE_DOUBLE_EXPENSES_DESC,address,nonce));
                            return validateResult;
                        }else{
                            //孤儿交易了
                            ValidateResult validateResult = new ValidateResult(VALIDATE_ORPHAN_CODE,String.format(VALIDATE_ORPHAN_DESC,address,nonce));
                        }
                    }
                }else{
                    //不存在未确认交易
                    if(!accountState.getNonce().equalsIgnoreCase(nonce)){
                        //提交的nonce 不等于 已确认的账本 nonce，则可能是孤儿交易
                        ValidateResult validateResult = new ValidateResult(VALIDATE_ORPHAN_CODE,String.format(VALIDATE_ORPHAN_DESC,address,nonce));
                        return validateResult;
                    }
                }


            }else{
                boolean  inValid = true;
                //解锁交易，校验是否存在该笔交易
                //获取交易号
                  List<FreezeLockTimeState> list =  accountState.getFreezeState().getFreezeLockTimeStates();
                  for(FreezeLockTimeState freezeLockTimeState:list){
                      if(freezeLockTimeState.getNonce().equalsIgnoreCase(nonce) && freezeLockTimeState.getAmount().compareTo(coinFrom.getAmount())== 0){
                          //找到交易
                          inValid = false;
                          break;
                      }
                  }
                  if(inValid){
                      //未找到冻结的交易
                      ValidateResult validateResult = new ValidateResult(VALIDATE_FAIL_CODE,String.format(VALIDATE_FAIL_DESC,address,nonce));
                      return validateResult;
                  }
            }
        }
        ValidateResult validateResult = new ValidateResult(VALIDATE_SUCCESS_CODE,VALIDATE_SUCCESS_DESC);
        return validateResult;
    }

}
