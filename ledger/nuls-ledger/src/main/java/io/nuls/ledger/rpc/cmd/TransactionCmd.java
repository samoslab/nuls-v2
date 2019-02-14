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

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.ledger.service.TransactionService;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.ledger.utils.LoggerUtil.logger;

/**
 * 未确认交易提交，提交失败直接返回错误信息
 * Created by wangkun23 on 2018/11/20.
 */
@Component
public class TransactionCmd extends BaseCmd {

    @Autowired
    private TransactionService transactionService;


    Response parseTxs(List<String> txHexList, List<Transaction> txList){
        for (String txHex : txHexList) {
            if (StringUtils.isBlank(txHex)) {
                return failed("txHex is blank");
            }
            byte[] txStream = HexUtil.decode(txHex);
            Transaction tx = new Transaction();
            try {
                tx.parse(new NulsByteBuffer(txStream));
                txList.add(tx);
            } catch (NulsException e) {
                logger.error("transaction parse error", e);
                return failed("transaction parse error");
            }
        }
        return success();
    }
    /**
     * 未确认交易提交
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "commitTx",
            version = 1.0, scope = "private", minEvent = 0, minPeriod = 0,
            description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHexList", parameterType = "List")
    @Parameter(parameterName = "blockHeight", parameterType = "long")
    @Parameter(parameterName = "isConfirmTx", parameterType = "boolean")
    public Response commitTx(Map params) {
        Map<String, Object> rtData = new HashMap<>();
        Integer chainId = (Integer) params.get("chainId");
        List<String> txHexList = (List) params.get("txHexList");
        boolean isConfirmTx = Boolean.valueOf(params.get("isConfirmTx").toString());
        if (null == txHexList || 0 == txHexList.size()) {
            return failed("txHexList is blank");
        }
        int value = 0;
        List<Transaction> txList = new ArrayList<>();
        Response  parseResponse =  parseTxs(txHexList,txList);
        if(!parseResponse.isSuccess()){
            return parseResponse;
        }
         if (isConfirmTx) {
                 long blockHeight =  Long.valueOf(params.get("blockHeight").toString());
                if (transactionService.confirmBlockProcess(chainId, txList,blockHeight)) {
                    value = 1;
                } else {
                    value = 0;
                }
         } else {
                //未确认交易按单笔来提交
                if(txList.size() != 1){
                    value = 0;
                }else{
                    if (transactionService.unConfirmTxProcess(chainId, txList.get(0))) {
                        value = 1;
                    } else {
                        value = 0;
                    }
                }
         }
        rtData.put("value", value);
        return success(rtData);
    }

    /**
     * 逐笔回滚交易
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "rollBackConfirmTx",
            version = 1.0, scope = "private", minEvent = 0, minPeriod = 0,
            description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txHexList", parameterType = "List")
    @Parameter(parameterName = "blockHeight", parameterType = "long")
    @Parameter(parameterName = "isConfirmTx", parameterType = "boolean")
    public Response rollBackConfirmTx(Map params) {
        Map<String, Object> rtData = new HashMap<>();
        int value = 0;
        try {
            Integer chainId = (Integer) params.get("chainId");
            List<String> txHexList = (List) params.get("txHexList");
            boolean isConfirmTx = Boolean.valueOf(params.get("isConfirmTx").toString());
            if (null == txHexList || 0 == txHexList.size()) {
                return failed("txHexList is blank");
            }
            List<Transaction> txList = new ArrayList<>();
            Response  parseResponse =  parseTxs(txHexList,txList);
            if(!parseResponse.isSuccess()){
                return parseResponse;
            }
            if (isConfirmTx) {
                long blockHeight =  Long.valueOf(params.get("blockHeight").toString());
                if (transactionService.rollBackConfirmTxs(chainId, blockHeight)) {
                    value = 1;
                } else {
                    value = 0;
                }
            } else {
                //未确认交易按单笔来提交
                if(txList.size() != 1){
                    value = 0;
                }else{
                    if (transactionService.rollBackUnconfirmTx(chainId, txList.get(0))) {
                        value = 1;
                    } else {
                        value = 0;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        rtData.put("value", value);
        return success(rtData);
    }
}
