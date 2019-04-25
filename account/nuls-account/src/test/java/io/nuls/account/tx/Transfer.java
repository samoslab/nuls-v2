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

package io.nuls.account.tx;

import io.nuls.account.constant.AccountConstant;
import io.nuls.account.constant.RpcConstant;
import io.nuls.account.model.dto.CoinDto;
import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.info.HostInfo;
import io.nuls.rpc.info.NoUse;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.rpc.util.RPCUtil;
import org.junit.Before;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2019/4/22
 */
public class Transfer implements Runnable {

    static String address20 = "tNULSeBaMvEtDfvZuukDf2mVyfGo3DdiN8KLRG";
    static String address21 = "tNULSeBaMnrs6JKrCy6TQdzYJZkMZJDng7QAsD";
    static String address22 = "tNULSeBaMrbMRiFAUeeAt6swb4xVBNyi81YL24";
    static String address23 = "tNULSeBaMu38g1vnJsSZUCwTDU9GsE5TVNUtpD";
    static String address24 = "tNULSeBaMp9wC9PcWEcfesY7YmWrPfeQzkN1xL";
    static String address25 = "tNULSeBaMshNPEnuqiDhMdSA4iNs6LMgjY6tcL";
    static String address26 = "tNULSeBaMoodYW7AqyJrgYdWiJ6nfwfVHHHyXm";
    static String address27 = "tNULSeBaMmTNYqywL5ZSHbyAQ662uE3wibrgD1";
    static String address28 = "tNULSeBaMoNnKitV28JeuUdBaPSR6n1xHfKLj2";
    static String address29 = "tNULSeBaMqywZjfSrKNQKBfuQtVxAHBQ8rB2Zn";

    static int chainId = 2;
    static int assetChainId = 2;
    static int assetId = 1;
    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";

    private String addressFrom;

    private String addressTo;

    public Transfer(String addressFrom, String addressTo) {
        this.addressFrom = addressFrom;
        this.addressTo = addressTo;
    }

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":8887/ws");
    }

    @Override
    public void run() {
        try {
            NulsDigestData hash = null;
            for (int i = 0; i < 120000; i++) {
                hash = transfer(hash);
                System.out.println("count:" + (i + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private NulsDigestData transfer(NulsDigestData hash) throws Exception{
        Map transferMap = CreateTx.createTransferTx(addressFrom, addressTo, new BigInteger("1000000000"));
        Transaction tx = CreateTx.assemblyTransaction((List<CoinDto>) transferMap.get("inputs"),
                (List<CoinDto>) transferMap.get("outputs"), (String) transferMap.get("remark"), hash);
        newTx(tx);
//        LoggerUtil.logger.info("hash:" + hash.getDigestHex());
//        LoggerUtil.logger.info("count:" + (i + 1));
//        LoggerUtil.logger.info("");
//        System.out.println("hash:" + hash.getDigestHex());
        return tx.getHash();
    }


    private Response newTx(Transaction tx)  throws Exception{
        Map<String, Object> params = new HashMap<>(AccountConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, RpcConstant.TX_NEW_VERSION);
        params.put(RpcConstant.TX_CHAIN_ID, chainId);
        params.put(RpcConstant.TX_DATA, RPCUtil.encode(tx.serialize()));
        return ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx_test", params);
    }
}
