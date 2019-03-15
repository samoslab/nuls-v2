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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.data.Transaction;
import io.nuls.rpc.model.ModuleE;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.TxRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nuls.transaction.utils.LoggerUtil.Log;

/**
 * 交易管理类，存储管理交易注册的基本信息
 *
 * @author: Charlie
 * @date: 2018/11/22
 */
public class TxManager {

    public static String getModuleCode(Chain chain, int type){
        return chain.getTxRegisterMap().get(type).getModuleCode();
    }

    /**
     * 注册交易
     *
     * @param txRegister 注册交易请求数据封装
     * @return boolean
     */
    public static boolean register(Chain chain, TxRegister txRegister) {
        boolean rs = false;
        if (!chain.getTxRegisterMap().containsKey(txRegister.getTxType())) {
            chain.getTxRegisterMap().put(txRegister.getTxType(), txRegister);

            try {
                Log.debug("\nTxRegisterMap = " + JSONUtils.obj2PrettyJson(chain.getTxRegisterMap()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
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
    public static TxRegister getTxRegister(Chain chain, int type) {
        return chain.getTxRegisterMap().get(type);
    }

    /**
     * 根据交易类型返回交易类型是否存在
     *
     * @param type
     * @return
     */
    public static boolean contain(Chain chain, int type) {
        return chain.getTxRegisterMap().containsKey(type);
    }

    /**
     * 返回系统交易类型
     */
    public static List<Integer> getSysTypes(Chain chain) {
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
    public static boolean isSystemTx(Chain chain, Transaction tx) {
        TxRegister txRegister = chain.getTxRegisterMap().get(tx.getType());
        return txRegister.getSystemTx();
    }

    /**
     * 是否是智能合约交易
     * @param txType
     * @return
     */
    public static boolean isSmartContract(Chain chain, int txType){
        if(ModuleE.SC.abbr.equals(getModuleCode(chain, txType))){
            return true;
        }
        return false;
    }
}
