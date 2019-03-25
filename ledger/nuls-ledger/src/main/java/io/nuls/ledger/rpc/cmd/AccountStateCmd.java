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
package io.nuls.ledger.rpc.cmd;

import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.model.po.AccountState;
import io.nuls.ledger.model.po.FreezeHeightState;
import io.nuls.ledger.model.po.FreezeLockTimeState;
import io.nuls.ledger.model.po.UnconfirmedNonce;
import io.nuls.ledger.service.AccountStateService;
import io.nuls.ledger.utils.LoggerUtil;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.model.StringUtils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于获取账户余额及账户nonce值
 * Created by wangkun23 on 2018/11/19.
 */
@Component
public class AccountStateCmd extends BaseCmd {


    @Autowired
    private AccountStateService accountStateService;

    /**
     * 获取账户资产余额
     * get user account balance
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "getBalance",
            version = 1.0,
            description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "assetChainId", parameterType = "int")
    @Parameter(parameterName = "address", parameterType = "String")
    @Parameter(parameterName = "assetId", parameterType = "int")
    public Response getBalance(Map params) {
        Integer chainId = (Integer) params.get("chainId");
        Integer assetChainId = (Integer) params.get("assetChainId");
        String address = (String) params.get("address");
        Integer assetId = (Integer) params.get("assetId");
        LoggerUtil.logger(chainId).debug("chainId={},assetChainId={},address={},assetId={}", chainId, assetChainId, address, assetId);
        AccountState accountState = accountStateService.getAccountState(address, chainId, assetChainId, assetId);
        Map<String, Object> rtMap = new HashMap<>();
        rtMap.put("freeze", accountState.getFreezeTotal());
        rtMap.put("total", accountState.getTotalAmount());
        rtMap.put("available", accountState.getAvailableAmount());
        BigInteger permanentLocked = BigInteger.ZERO;
        BigInteger timeHeightLocked = BigInteger.ZERO;;
        for(FreezeLockTimeState freezeLockTimeState:accountState.getFreezeLockTimeStates()){
            if(LedgerConstant.PERMANENT_LOCK == freezeLockTimeState.getLockTime()){
                permanentLocked= permanentLocked.add(freezeLockTimeState.getAmount());
            }else{
                timeHeightLocked= timeHeightLocked.add(freezeLockTimeState.getAmount());
            }
        }
        for(FreezeHeightState freezeHeightState:accountState.getFreezeHeightStates()){
            timeHeightLocked= timeHeightLocked.add(freezeHeightState.getAmount());
        }
        rtMap.put("permanentLocked",permanentLocked);
        rtMap.put("timeHeightLocked", timeHeightLocked);
        Response response = success(rtMap);
        LoggerUtil.logger(chainId).debug("response={}",response);
        return  response;
    }

    /**
     * 获取账户nonce值
     * get user account nonce
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "getNonce",
            version = 1.0, scope = "private", minEvent = 0, minPeriod = 0,
            description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "assetChainId", parameterType = "int")
    @Parameter(parameterName = "address", parameterType = "String")
    @Parameter(parameterName = "assetId", parameterType = "int")
    public Response getNonce(Map params) {
        Integer chainId = (Integer) params.get("chainId");
        Integer assetChainId = (Integer) params.get("assetChainId");
        String address = (String) params.get("address");
        Integer assetId = (Integer) params.get("assetId");
        AccountState accountState = accountStateService.getAccountState(address, chainId, assetChainId, assetId);
        Map<String, Object> rtMap = new HashMap<>();
        String unconfirmedNonce = accountState.getLatestUnconfirmedNonce();
        if (StringUtils.isNotBlank(unconfirmedNonce)) {
            rtMap.put("nonce", unconfirmedNonce);
            rtMap.put("nonceType", LedgerConstant.UNCONFIRMED_NONCE);
        } else {
            rtMap.put("nonce", accountState.getNonce());
            rtMap.put("nonceType", LedgerConstant.CONFIRMED_NONCE);
        }

        return success(rtMap);
    }

    @CmdAnnotation(cmd = "getBalanceNonce",
            version = 1.0, scope = "private", minEvent = 0, minPeriod = 0,
            description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "assetChainId", parameterType = "int")
    @Parameter(parameterName = "address", parameterType = "String")
    @Parameter(parameterName = "assetId", parameterType = "int")
    public Response getBalanceNonce(Map params) {
        Integer chainId = (Integer) params.get("chainId");
        Integer assetChainId = (Integer) params.get("assetChainId");
        String address = (String) params.get("address");
        Integer assetId = (Integer) params.get("assetId");
        LoggerUtil.logger(chainId).debug("chainId={},assetChainId={},address={},assetId={}", chainId, assetChainId, address, assetId);
        AccountState accountState = accountStateService.getAccountState(address, chainId, assetChainId, assetId);
        Map<String, Object> rtMap = new HashMap<>();
        List<UnconfirmedNonce> unconfirmedNonces = accountState.getUnconfirmedNonces();
        if (unconfirmedNonces.size() > 0) {
            rtMap.put("nonce", accountState.getLatestUnconfirmedNonce());
            rtMap.put("nonceType", LedgerConstant.UNCONFIRMED_NONCE);
        } else {
            rtMap.put("nonce", accountState.getNonce());
            rtMap.put("nonceType", LedgerConstant.CONFIRMED_NONCE);
        }
        if (accountState.getUnconfirmedAmounts().size() > 0) {
            rtMap.put("available", accountState.getAvailableAmount().add(accountState.getUnconfirmedAmount()));
            rtMap.put("freeze", accountState.getFreezeTotal().add(accountState.getUnconfirmedFreezeAmount()));
            LoggerUtil.logger(chainId).debug("UnconfirmedAmounts=true");

        } else {
            rtMap.put("available", accountState.getAvailableAmount());
            rtMap.put("freeze", accountState.getFreezeTotal());
        }

        Response response =  success(rtMap);
        LoggerUtil.logger(chainId).debug("response={}", response);
        return response;
    }

}
