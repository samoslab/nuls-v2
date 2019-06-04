/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.contract.rpc.call;

import io.nuls.contract.enums.LedgerUnConfirmedTxStatus;
import io.nuls.contract.model.bo.Chain;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.exception.NulsException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2019-02-26
 */
public class LedgerCall {

    public static Map<String, Object> getBalanceAndNonce(Chain chain, String address) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put("assetChainId", chain.getConfig().getChainId());
        params.put("address", address);
        params.put("assetId", chain.getConfig().getAssetsId());
        try {
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getBalanceNonce", params);
            if (!callResp.isSuccess()) {
                return null;
            }
            return (HashMap) ((HashMap) callResp.getResponseData()).get("getBalanceNonce");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    public static Map<String, Object> getBalance(Chain chain, String address) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put("assetChainId", chain.getConfig().getChainId());
        params.put("address", address);
        params.put("assetId", chain.getConfig().getAssetsId());
        try {
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getBalance", params);
            if (!callResp.isSuccess()) {
                return null;
            }
            return (HashMap) ((HashMap) callResp.getResponseData()).get("getBalance");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    public static Map<String, Object> getNonce(Chain chain, String address) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put("assetChainId", chain.getConfig().getChainId());
        params.put("address", address);
        params.put("assetId", chain.getConfig().getAssetsId());
        try {
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "getNonce", params);
            if (!callResp.isSuccess()) {
                return null;
            }
            return (HashMap) ((HashMap) callResp.getResponseData()).get("getNonce");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }


    /**
     * 提交未确认交易给账本
     */
    public static int commitUnconfirmedTx(int chainId, String txData) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(4);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("tx", txData);
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "commitUnconfirmedTx", params);
            if (!callResp.isSuccess()) {
                return LedgerUnConfirmedTxStatus.OTHER.status();
            }
            HashMap result = (HashMap) ((HashMap) callResp.getResponseData()).get("commitUnconfirmedTx");
            return (int) result.get("validateCode");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 调用账本回滚未确认的交易
     */
    public static boolean rollBackUnconfirmTx(int chainId, String txData) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(4);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("tx", txData);
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, "rollBackUnconfirmTx", params);
            if (!callResp.isSuccess()) {
                return false;
            }
            HashMap result = (HashMap) ((HashMap) callResp.getResponseData()).get("rollBackUnconfirmTx");
            return (int) result.get("value") == 1;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }
}
