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

package io.nuls.transaction;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Page;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.info.HostInfo;
import io.nuls.rpc.info.NoUse;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.config.ConfigBean;
import io.nuls.transaction.model.dto.CoinDTO;
import io.nuls.transaction.model.dto.CrossTxTransferDTO;
import io.nuls.transaction.rpc.call.LedgerCall;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.transaction.utils.LoggerUtil.Log;
import static org.junit.Assert.*;

/**
 * @author: Charlie
 * @date: 2019-01-15
 */
public class TestTx {

    @Test
    public void allInOne() throws NulsException {
        Log.debug("0.##########导入创世块中的一个地址##########");
        importPriKey("00c805d2d6d5e06f57fdfb1aff56ef3c2dd15eee88f36fa7d45d368c352ec5ec0d", password);//23 5MR_2CWKhFuoGVraaxL5FYY3RsQLjLDN7jw
        BigInteger balance = LedgerCall.getBalance(chain, AddressTool.getAddress(address23), chainId, assetId);
        Log.debug(address23 + "-----balance:{}", balance);
        List<String> accountList;
        try {
            while (true){
                Log.debug("##################################################");
                String agentAddress = "";
                String packingAddress = "";
                String agentHash = "";
                String depositHash = "";
                {
                    Log.debug("1.##########新建两个地址，一个作为节点地址，一个作为打包地址##########");
                    //新建两个地址
                    Map<String, Object> params = new HashMap<>();
                    params.put(Constants.VERSION_KEY_STR, version);
                    params.put("chainId", chainId);
                    params.put("count", 2);
                    params.put("password", password);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_createAccount", params);
                    assertTrue(response.isSuccess());
                    accountList = (List<String>) ((HashMap) ((HashMap) response.getResponseData()).get("ac_createAccount")).get("list");
                    agentAddress = accountList.get(0);
                    packingAddress = accountList.get(1);
                    Log.debug("agentAddress-{{}}", agentAddress);
                    Log.debug("packingAddress-{{}}", packingAddress);
                }
                {
                    Log.debug("2.##########从创世块地址转账给新创建的地址##########");
                    Map transferMap = new HashMap();
                    transferMap.put("chainId", chainId);
                    transferMap.put("remark", "transfer test");
                    List<CoinDTO> inputs = new ArrayList<>();
                    List<CoinDTO> outputs = new ArrayList<>();
                    CoinDTO inputCoin1 = new CoinDTO();
                    inputCoin1.setAddress(address23);
                    inputCoin1.setPassword(password);
                    inputCoin1.setAssetsChainId(chainId);
                    inputCoin1.setAssetsId(assetId);
                    inputCoin1.setAmount(new BigInteger("25000400000000"));
                    inputs.add(inputCoin1);

                    CoinDTO outputCoin1 = new CoinDTO();
                    outputCoin1.setAddress(agentAddress);
                    outputCoin1.setPassword(password);
                    outputCoin1.setAssetsChainId(chainId);
                    outputCoin1.setAssetsId(assetId);
                    outputCoin1.setAmount(new BigInteger("25000100000000"));
                    outputs.add(outputCoin1);

                    CoinDTO outputCoin2 = new CoinDTO();
                    outputCoin2.setAddress(packingAddress);
                    outputCoin2.setPassword(password);
                    outputCoin2.setAssetsChainId(chainId);
                    outputCoin2.setAssetsId(assetId);
                    outputCoin2.setAmount(new BigInteger("200000000"));
                    outputs.add(outputCoin2);
                    transferMap.put("inputs", inputs);
                    transferMap.put("outputs", outputs);

                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                    assertTrue(response.isSuccess());
                    HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                    Log.debug("transfer hash:{}", result.get("value"));
                    Log.debug("transfer from {} to {}", address23, agentAddress);
                    Log.debug("transfer from {} to {}", address23, packingAddress);
                }

                Thread.sleep(15000);
                {
                    Log.debug("3.##########给新创建的地址设置别名##########");
                    BigInteger agentBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(agentAddress), chainId, assetId);
                    Log.debug(agentAddress + "-----balance:{}", agentBalance);
                    assertEquals(new BigInteger("25000100000000"), agentBalance);
                    BigInteger packingBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(packingAddress), chainId, assetId);
                    Log.debug(packingAddress + "-----balance:{}", packingBalance);
                    assertEquals(new BigInteger("200000000"), packingBalance);
                    {
                        String alias = "test_" + System.currentTimeMillis();
                        Map<String, Object> params = new HashMap<>();
                        params.put(Constants.VERSION_KEY_STR, "1.0");
                        params.put("chainId", chainId);
                        params.put("address", agentAddress);
                        params.put("password", password);
                        params.put("alias", alias);
                        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_setAlias", params);
                        assertTrue(response.isSuccess());
                        HashMap result = (HashMap) ((HashMap) response.getResponseData()).get("ac_setAlias");
                        String txHash = (String) result.get("txHash");
                        Log.debug("agentAddress alias-txHash:{}", txHash);
                    }
                    {
                        String alias = "test_" + System.currentTimeMillis();
                        Map<String, Object> params = new HashMap<>();
                        params.put(Constants.VERSION_KEY_STR, "1.0");
                        params.put("chainId", chainId);
                        params.put("address", packingAddress);
                        params.put("password", password);
                        params.put("alias", alias);
                        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_setAlias", params);
                        assertTrue(response.isSuccess());
                        HashMap result = (HashMap) ((HashMap) response.getResponseData()).get("ac_setAlias");
                        String txHash = (String) result.get("txHash");
                        Log.debug("packingAddress alias-txHash:{}", txHash);
                    }
                    Thread.sleep(12000);
                    Log.debug("4.##########创建节点##########");
                    //创建节点
                    Map agentTxMap = this.createAgentTx(agentAddress, packingAddress);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", agentTxMap);
                    assertTrue(response.isSuccess());
                    Map map = (HashMap) (((HashMap) response.getResponseData()).get("cs_createAgent"));
                    agentHash =  (String)map.get("txHash");
                    Log.debug("createAgent-txHash:{}", agentHash);
                }

                Thread.sleep(12000);
                {
                    Log.debug("5.##########进行委托##########");
                    Map<String, Object> dpParams = new HashMap<>();
                    dpParams.put("chainId", chainId);
                    dpParams.put("address", agentAddress);
                    dpParams.put("password", password);
                    dpParams.put("agentHash", agentHash);
                    dpParams.put("deposit", 200000 * 100000000L);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", dpParams);
                    assertTrue(response.isSuccess());
                    HashMap dpResult = (HashMap) ((HashMap) response.getResponseData()).get("cs_depositToAgent");
                    depositHash = (String) dpResult.get("txHash");
                    Log.debug("deposit-txHash:{}", depositHash);
                }

                Thread.sleep(60000);
                {
                    Log.debug("6.##########取消委托##########");
                    //取消委托
                    Map<String, Object> params = new HashMap<>();
                    params.put("chainId", chainId);
                    params.put("address", agentAddress);
                    params.put("password", password);
                    params.put("txHash", depositHash);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_withdraw", params);
                    assertTrue(response.isSuccess());
                    HashMap dpResult = (HashMap) ((HashMap) response.getResponseData()).get("cs_withdraw");
                    String hash = (String) dpResult.get("txHash");
                    Log.debug("withdraw-txHash:{}", hash);
                }

                Thread.sleep(60000);
                {
                    Log.debug("7.##########再次委托##########");
                    //再次委托
                    Map<String, Object> dpParams = new HashMap<>();
                    dpParams.put("chainId", chainId);
                    dpParams.put("address", agentAddress);
                    dpParams.put("password", password);
                    dpParams.put("agentHash", agentHash);
                    dpParams.put("deposit", 200000 * 100000000L);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", dpParams);
                    assertTrue(response.isSuccess());
                    HashMap dpResult = (HashMap) ((HashMap) response.getResponseData()).get("cs_depositToAgent");
                    depositHash = (String) dpResult.get("txHash");
                    Log.debug("deposit-txHash:{}", depositHash);
                }

                Thread.sleep(60000);
                {
                    Log.debug("8.##########删除节点##########");
                    //停止节点
                    Map<String, Object> txMap = new HashMap();
                    txMap.put("chainId", chainId);
                    txMap.put("address", agentAddress);
                    txMap.put("password", password);
                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", txMap);
                    assertTrue(response.isSuccess());
                    Map result = (HashMap) (((HashMap) response.getResponseData()).get("cs_stopAgent"));
                    String txHash = (String) result.get("txHash");
                    Log.debug("stopAgent-txHash:{}", txHash);
                }

                Thread.sleep(1000);
                {
                    Log.debug("9.##########查询刚才创建的地址余额并返还给创世块地址##########");
                    BigInteger agentBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(agentAddress), chainId, assetId);
                    Log.debug(agentAddress + "-----balance:{}", agentBalance);
                    BigInteger packingBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(packingAddress), chainId, assetId);
                    Log.debug(packingAddress + "-----balance:{}", packingBalance);
                    Map transferMap = new HashMap();
                    transferMap.put("chainId", chainId);
                    transferMap.put("remark", "transfer test");
                    List<CoinDTO> inputs = new ArrayList<>();
                    List<CoinDTO> outputs = new ArrayList<>();
                    CoinDTO inputCoin1 = new CoinDTO();
                    inputCoin1.setAddress(agentAddress);
                    inputCoin1.setPassword(password);
                    inputCoin1.setAssetsChainId(chainId);
                    inputCoin1.setAssetsId(assetId);
                    inputCoin1.setAmount(agentBalance.subtract(new BigInteger("100000000")));
                    inputs.add(inputCoin1);
                    CoinDTO inputCoin2 = new CoinDTO();
                    inputCoin2.setAddress(packingAddress);
                    inputCoin2.setPassword(password);
                    inputCoin2.setAssetsChainId(chainId);
                    inputCoin2.setAssetsId(assetId);
                    inputCoin2.setAmount(packingBalance.subtract(new BigInteger("100000000")));
                    inputs.add(inputCoin2);

                    CoinDTO outputCoin1 = new CoinDTO();
                    outputCoin1.setAddress(address23);
                    outputCoin1.setPassword(password);
                    outputCoin1.setAssetsChainId(chainId);
                    outputCoin1.setAssetsId(assetId);
                    outputCoin1.setAmount(agentBalance.add(packingBalance).subtract(new BigInteger("200000000")));
                    outputs.add(outputCoin1);
                    transferMap.put("inputs", inputs);
                    transferMap.put("outputs", outputs);

                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                    HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                    Log.debug("ac_transfer hash:{}", result.get("value"));
                    assertTrue(response.isSuccess());
                }
                Thread.sleep(12000);
                BigInteger agentBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(agentAddress), chainId, assetId);
                Log.debug(agentAddress + "---balance:{}", agentBalance);
                BigInteger packingBalance = LedgerCall.getBalance(chain, AddressTool.getAddress(packingAddress), chainId, assetId);
                Log.debug(packingAddress + "---balance:{}", packingBalance);
                Log.debug("##################################################");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void batchTransfer() throws Exception {
        importPriKey("00c805d2d6d5e06f57fdfb1aff56ef3c2dd15eee88f36fa7d45d368c352ec5ec0d", password);//23 5MR_2CWKhFuoGVraaxL5FYY3RsQLjLDN7jw
        int count = 5;
        List<String> accountList;
        {
            //新建100个地址
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, version);
            params.put("chainId", chainId);
            params.put("count", count);
            params.put("password", password);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_createAccount", params);
            assertTrue(response.isSuccess());
            accountList = (List<String>) ((HashMap) ((HashMap) response.getResponseData()).get("ac_createAccount")).get("list");
            assertEquals(count, accountList.size());
        }
        {
            //给这100个地址转账
            for (String account : accountList) {
                Map transferMap = new HashMap();
                transferMap.put("chainId", chainId);
                transferMap.put("remark", "transfer test");
                List<CoinDTO> inputs = new ArrayList<>();
                List<CoinDTO> outputs = new ArrayList<>();
                CoinDTO inputCoin1 = new CoinDTO();
                inputCoin1.setAddress(address23);
                inputCoin1.setPassword(password);
                inputCoin1.setAssetsChainId(chainId);
                inputCoin1.setAssetsId(assetId);
                inputCoin1.setAmount(new BigInteger("500000000000"));
                inputs.add(inputCoin1);

                CoinDTO outputCoin1 = new CoinDTO();
                outputCoin1.setAddress(account);
                outputCoin1.setPassword(password);
                outputCoin1.setAssetsChainId(chainId);
                outputCoin1.setAssetsId(assetId);
                outputCoin1.setAmount(new BigInteger("500000000000"));
                outputs.add(outputCoin1);
                transferMap.put("inputs", inputs);
                transferMap.put("outputs", outputs);

                Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                assertTrue(response.isSuccess());
                HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                Log.debug("transfer from {} to {}, hash:{}", address23, account, result.get("value"));
                Thread.sleep(5000);
            }
        }
        Thread.sleep(15000);
        {
            //100个地址之间互相转账
            while (true) {
                for (int i = 0; i < count; i++) {
                    String from = accountList.get(i % count);
                    String to = accountList.get((i + 1) % count);

                    Map transferMap = new HashMap();
                    transferMap.put("chainId", chainId);
                    transferMap.put("remark", "transfer test");
                    List<CoinDTO> inputs = new ArrayList<>();
                    List<CoinDTO> outputs = new ArrayList<>();
                    CoinDTO inputCoin1 = new CoinDTO();
                    inputCoin1.setAddress(from);
                    inputCoin1.setPassword(password);
                    inputCoin1.setAssetsChainId(chainId);
                    inputCoin1.setAssetsId(assetId);
                    inputCoin1.setAmount(new BigInteger("100000000"));
                    inputs.add(inputCoin1);

                    CoinDTO outputCoin1 = new CoinDTO();
                    outputCoin1.setAddress(to);
                    outputCoin1.setPassword(password);
                    outputCoin1.setAssetsChainId(chainId);
                    outputCoin1.setAssetsId(assetId);
                    outputCoin1.setAmount(new BigInteger("100000000"));
                    outputs.add(outputCoin1);
                    transferMap.put("inputs", inputs);
                    transferMap.put("outputs", outputs);

                    Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
                    assertTrue(response.isSuccess());
                    HashMap result = (HashMap) (((HashMap) response.getResponseData()).get("ac_transfer"));
                    Log.debug("transfer from {} to {}, hash:{}", from, to, result.get("value"));
                }
                Thread.sleep(15000);
            }
        }
    }

    static String address20 = "5MR_2CWWTDXc32s9Wd1guNQzPztFgkyVEsz";
    static String address21 = "5MR_2CbdqKcZktcxntG14VuQDy8YHhc6ZqW";
    static String address22 = "5MR_2Cj9tfgQpdeF7nDy5wyaGG6MZ35H3rA";
    static String address23 = "5MR_2CWKhFuoGVraaxL5FYY3RsQLjLDN7jw";
    static String address24 = "5MR_2CgwCFRoJ8KX37xNqjjR7ttYuJsg8rk";
    static String address25 = "5MR_2CjZkQsN7EnEPcaLgNrMrp6wpPGN6xo";
    static String address26 = "5MR_2CeG11nRqx7nGNeh8hTXADibqfSYeNu";
    static String address27 = "5MR_2CVCFWH7o8AmrTBPLkdg2dYH1UCUJiM";
    static String address28 = "5MR_2CfUsasd33vQV3HqGw6M3JwVsuVxJ7r";
    static String address29 = "5MR_2CVuGjQ3CYVkhFszxfSt6sodg1gDHYF";

    private Chain chain;
    static int chainId = 12345;
    static int assetChainId = 12345;
    static int assetId = 1;
    static String version = "1.0";

    static String password = "nuls123456";//"nuls123456";

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":8887/ws");
        chain = new Chain();
        chain.setConfig(new ConfigBean(chainId, assetId, 1024*1024,500,20000L,20000,60000L));
    }

    @Test
    public void newCtx() throws Exception {
        createTransfer();
//        createCtxTransfer();
    }

    @Test
    public void multiThreadCreateTx() {
        for (int i = 0; i < 1; i++) {
            Thread thread = new Thread(new CreateTxThread(), "MR" + i);
            thread.start();
        }
        try {
            while (true) {
                Thread.sleep(1000000L);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void importPriKeyTest() {
//        importPriKey("7e304e60c4e29c15382f76c0bb097bda28a1950b78871b6b7eb2bb4cc4ddeb49", password);//种子出块地址 5MR_2Cb86fpFbuY4Lici8MJStNxDFYH6kRB
//        importPriKey("70e871a2e637b4182dfbedc53e164182d266821f4824ab1a3a73055e9f252f98", password);//种子出块地址

//        importPriKey("00c299b105e2f9b260d7811d5cb94c713cc324e55831cb15a18454f7382f0a5f6e", password);//20 5MR_2CWWTDXc32s9Wd1guNQzPztFgkyVEsz
//        importPriKey("00c4a6b90d3f4eb7b50bc85fd0e99ccb717e148b4fde7462e14c590445e589588c", password);//21 5MR_2CbdqKcZktcxntG14VuQDy8YHhc6ZqW
//        importPriKey("009ad5018ed1fc162c5320b9ae496984dd10227086ad86ea954a209597ff9b7d3a", password);//22 5MR_2Cj9tfgQpdeF7nDy5wyaGG6MZ35H3rA
//        importPriKey("00c805d2d6d5e06f57fdfb1aff56ef3c2dd15eee88f36fa7d45d368c352ec5ec0d", null);//23 5MR_2CWKhFuoGVraaxL5FYY3RsQLjLDN7jw
//        importPriKey("00c77707b21eece6c1ce0b8add04db79dc846f36830effe5c5ae2aced00097fafb", null);//24 5MR_2CgwCFRoJ8KX37xNqjjR7ttYuJsg8rk
//        importPriKey("00def3b0f4bfad2a6abb5f6957829e752a1a30806edc35e98016425d578fdc4e77", password);//25 5MR_2CjZkQsN7EnEPcaLgNrMrp6wpPGN6xo
//        importPriKey("1c2b9fd4417c1aad8ae9f24c982ff294eb50a6462b873b79a879e805a9990346", password);//26 5MR_2CeG11nRqx7nGNeh8hTXADibqfSYeNu
//        importPriKey("00c98ecfd3777745270cacb9afba17ef0284769a83ff2adb4106b8a0baaec9452c", password);//27 5MR_2CVCFWH7o8AmrTBPLkdg2dYH1UCUJiM
//        importPriKey("23848d45b4b34aca8ff24b00949a25a2c9175faf283675128e189eee8b085942", password);//28 5MR_2CfUsasd33vQV3HqGw6M3JwVsuVxJ7r
//        importPriKey("009560d5ed6587822b7aee6f318f50b312c281e4f330b6990396881c6d3f870bc1", password);//29 5MR_2CVuGjQ3CYVkhFszxfSt6sodg1gDHYF
//
//        importPriKey("00fffd585ed08dddf0d034236aa1ea85abd2e4e69981617ee477adf6cdcf50f4d5", password);//打包地址 5MR_2Ch8CCnLwoLWFZ45pFEZSmo1C1pkPFA
        importPriKey("008da295e53ad4aab4a5b4c20c95bf0732a7ab10d61e4acc788f5a50eef2f51f58", password);//种子出块地址 5MR_2CVzGriCSBRf9KCFqPW4mq26uEK5Vig
    }

    @Test
    public void createAgentTx() throws Exception {
        //组装创建节点交易
        Map agentTxMap = this.createAgentTx(address23, address24);
        //调用接口
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_createAgent", agentTxMap);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get("cs_createAgent"));
        Assert.assertTrue(null != result);
        String hash =  (String)result.get("txHash");
        Log.debug("createAgent-txHash:{}", hash);
    }


    /**
     * 委托节点
     */
    @Test
    public void depositToAgent() throws Exception {
        //组装委托节点交易
        String agentHash = "00201b42bc483d07e50a0f4904700c4f6d74610609b2aa371f4a4033c5479ba4208a";
        Map<String, Object> dpParams = new HashMap<>();
        dpParams.put("chainId", chainId);
        dpParams.put("address", address23);
        dpParams.put("password", password);
        dpParams.put("agentHash", agentHash);
        dpParams.put("deposit", 200000 * 100000000L);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_depositToAgent", dpParams);
        HashMap dpResult = (HashMap) ((HashMap) dpResp.getResponseData()).get("cs_depositToAgent");
        String txHash = (String) dpResult.get("txHash");
        Log.debug("deposit-txHash:{}", txHash);
    }

    /**
     * 取消委托
     *
     * @throws Exception
     */
    @Test
    public void withdraw() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("address", "5MR_2Cb5vRQRzG52A9qWg9G7seXvUWtbjMi");
        params.put("password", password);
        params.put("txHash", "0020b8a42eb4c70196189e607e9434fe09b595d5753711f21819113f40d64a1c82c1");
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_withdraw", params);
        HashMap dpResult = (HashMap) ((HashMap) cmdResp.getResponseData()).get("cs_withdraw");
        String hash = (String) dpResult.get("txHash");
        Log.debug("withdraw-txHash:{}", hash);
    }

    @Test
    public void stopAgentTx() throws Exception {
        Map<String, Object> txMap = new HashMap();
        txMap.put("chainId", chainId);
        txMap.put("address", address23);
        txMap.put("password", "nuls123456");
        //调用接口
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.CS.abbr, "cs_stopAgent", txMap);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get("cs_stopAgent"));
        String txHash = (String) result.get("txHash");
        Log.debug("stopAgent-txHash:{}", txHash);
    }

    /**
     * 设置别名
     * @throws Exception
     */
    @Test
    public void alias() throws Exception {
        String alias = "charlie" + System.currentTimeMillis();
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", chainId);
        params.put("address", address25);
        params.put("password", password);
        params.put("alias", alias);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_setAlias", params);
        System.out.println("ac_setAlias result:" + JSONUtils.obj2json(cmdResp));
        assertNotNull(cmdResp);
        HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_setAlias");
        assertNotNull(result);
        String txHash = (String) result.get("txHash");
        assertNotNull(txHash);
        Log.debug("alias-txHash{}", txHash);
    }

    /**
     * 查交易
     */
    @Test
    public void getTxRecord() throws Exception{
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("address", address27);
        params.put("assetChainId", null);
        params.put("assetId", null);
        params.put("type", null);
        params.put("pageSize", null);
        params.put("pageNumber", null);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getTxs", params);
        Page record = (Page) dpResp.getResponseData();
        Log.debug("Page<TransactionPO>:{}", JSONUtils.obj2PrettyJson(record));
    }

    /**
     * 导入keystore
     */
    @Test
    public void importAccountByKeystoreFile(){
        String address = importAccountByKeystoreFile("C:/Users/Administrator/Desktop/2.0测试配置和内容/种子节点地址/5MR_2Ch2F7jfohNqwwdmMmF3ESQYK2R9bt8.keystore");
        Log.info("address:{}", address);
    }

    /**
     *  删除账户
     */
    @Test
    public void removeAccountTest() throws Exception {
        removeAccount("5MR_2Cb86fpFbuY4Lici8MJStNxDFYH6kRB", password);
        removeAccount(address26, password);
    }

    private void createTransfer() throws Exception {
        Map transferMap = this.createTransferTx();
        //调用接口
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_transfer", transferMap);
        HashMap result = (HashMap) (((HashMap) cmdResp.getResponseData()).get("ac_transfer"));
        Assert.assertTrue(null != result);
        Log.debug("{}", result.get("value"));
    }


    private void createCtxTransfer() throws Exception {
        CrossTxTransferDTO ctxTransfer = new CrossTxTransferDTO(chain.getChainId(),
                createFromCoinDTOList(), createToCoinDTOList(), "this is cross-chain transaction");
        //调接口
        String json = JSONUtils.obj2json(ctxTransfer);
        Map<String, Object> params = JSONUtils.json2map(json);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_createCtx", params);
        Assert.assertTrue(null != response.getResponseData());
        Map map = (HashMap) ((HashMap) response.getResponseData()).get("tx_createCtx");
        Assert.assertTrue(null != map);
        Log.debug("{}", map.get("value"));
    }

    public static String importAccountByKeystoreFile(String filePath) {
        String address = null;
        try {
            File file = new File(filePath);
            byte[] bytes = copyToByteArray(file);
            String keyStoreStr = new String(bytes,"UTF-8");

            //AccountKeyStoreDto accountKeyStoreDto = JSONUtils.json2pojo(new String(HexUtil.decode(keyStoreHexStr)), AccountKeyStoreDto.class);

            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, version);
            params.put("chainId", chainId);
            params.put("keyStore", HexUtil.encode(bytes));
            params.put("password", password);
            params.put("overwrite", true);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByKeystore", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByKeystore");
            address = (String) result.get("address");
            //assertEquals(accountList.get(0), address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    public static byte[] copyToByteArray(File in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        InputStream input = null;
        try {
            input = new FileInputStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            int byteCount = 0;
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = input.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return out.toByteArray();
        } finally {
            try {
                input.close();
            } catch (Exception e) {
            }
        }
    }

    public void importPriKey(String priKey, String pwd){
        try {
            //账户已存在则覆盖 If the account exists, it covers.
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);

            params.put("priKey", priKey);
            params.put("password", pwd);
            params.put("overwrite", true);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_importAccountByPriKey", params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get("ac_importAccountByPriKey");
            String address = (String) result.get("address");
            Log.debug("importPriKey success! address-{}", address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeAccount(String address, String password) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", chainId);
        params.put("address", address);
        params.put("password", password);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, "ac_removeAccount", params);
        Log.debug("{}", JSONUtils.obj2json(cmdResp.getResponseData()));
    }

    @Test
    public void getBalance() throws Exception {

        BigInteger balance = LedgerCall.getBalance(chain, AddressTool.getAddress("5MR_2CkYEhXKCmUWTEsWRTnaWgYE8kJdfd5"), assetChainId, assetId);
        System.out.println(JSONUtils.obj2PrettyJson(balance));

        BigInteger balance2 = LedgerCall.getBalance(chain, AddressTool.getAddress(address21), assetChainId, assetId);
        System.out.println(JSONUtils.obj2PrettyJson(balance2));
        BigInteger balance3 = LedgerCall.getBalance(chain, AddressTool.getAddress(address22), assetChainId, assetId);
        System.out.println(JSONUtils.obj2PrettyJson(balance3));
        BigInteger balance4 = LedgerCall.getBalance(chain, AddressTool.getAddress(address24), assetChainId, assetId);
        System.out.println(JSONUtils.obj2PrettyJson(balance4));
        BigInteger balance5 = LedgerCall.getBalance(chain, AddressTool.getAddress(address25), assetChainId, assetId);
        System.out.println(JSONUtils.obj2PrettyJson(balance5));
    }


    //    @Test
    public void packableTxs() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        long endTime = System.currentTimeMillis() + 10000L;
        System.out.println("endTime: " + endTime);
        params.put("endTimestamp", endTime);
        params.put("maxTxDataSize", 2 * 1024 * 1024L);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_packableTxs", params);
        Assert.assertTrue(null != response.getResponseData());
        Map map = (HashMap) ((HashMap) response.getResponseData()).get("tx_packableTxs");
        Assert.assertTrue(null != map);
        List<String> list = (List) map.get("list");
        Log.debug("packableTxs:");
        for (String s : list) {
            Log.debug(s);
        }
    }

    private List<CoinDTO> createFromCoinDTOList() {
        CoinDTO coinDTO = new CoinDTO();
        coinDTO.setAssetsId(assetId);
        coinDTO.setAssetsChainId(assetChainId);
        coinDTO.setAddress(address25);
        coinDTO.setAmount(new BigInteger("200000000"));
        coinDTO.setPassword(password);

        CoinDTO coinDTO2 = new CoinDTO();
        coinDTO2.setAssetsId(assetId);
        coinDTO2.setAssetsChainId(assetChainId);
        coinDTO2.setAddress(address26);
        coinDTO2.setAmount(new BigInteger("100000000"));
        coinDTO2.setPassword(password);
        List<CoinDTO> listFrom = new ArrayList<>();
        listFrom.add(coinDTO);
        listFrom.add(coinDTO2);
        return listFrom;
    }

    private List<CoinDTO> createToCoinDTOList() {
        CoinDTO coinDTO = new CoinDTO();
        coinDTO.setAssetsId(assetId);
        coinDTO.setAssetsChainId(23);
        coinDTO.setAddress("2kX_2Ceu7cSmeV9ZSJaqqwwF27Jr6n7omyw");
        coinDTO.setAmount(new BigInteger("200000000"));

        CoinDTO coinDTO2 = new CoinDTO();
        coinDTO2.setAssetsId(assetId);
        coinDTO2.setAssetsChainId(23);
        coinDTO2.setAddress("2kX_2CfowdpKsNtwCiTGM4L8PhQrF1b4Rki");
        coinDTO2.setAmount(new BigInteger("100000000"));
        List<CoinDTO> listTO = new ArrayList<>();
        listTO.add(coinDTO);
        listTO.add(coinDTO2);
        return listTO;
    }


    /**
     * 创建普通转账交易
     *
     * @return
     */
    private Map createTransferTx() {
        Map transferMap = new HashMap();
        transferMap.put("chainId", chainId);
        transferMap.put("remark", "transfer test");
        List<CoinDTO> inputs = new ArrayList<>();
        List<CoinDTO> outputs = new ArrayList<>();
        CoinDTO inputCoin1 = new CoinDTO();
        inputCoin1.setAddress(address26);
        inputCoin1.setPassword(password);
        inputCoin1.setAssetsChainId(chainId);
        inputCoin1.setAssetsId(assetId);
        inputCoin1.setAmount(new BigInteger("10000000"));
        inputs.add(inputCoin1);

        CoinDTO outputCoin1 = new CoinDTO();
        outputCoin1.setAddress(address27);
        outputCoin1.setPassword(password);
        outputCoin1.setAssetsChainId(chainId);
        outputCoin1.setAssetsId(assetId);
        outputCoin1.setAmount(new BigInteger("10000000"));
        outputs.add(outputCoin1);

        transferMap.put("inputs", inputs);
        transferMap.put("outputs", outputs);
        return transferMap;
    }

    /**
     * 创建节点
     */
    public Map createAgentTx(String agentAddr, String packingAddr) {
        Map<String, Object> params = new HashMap<>();
        params.put("agentAddress", agentAddr);
        params.put("chainId", chainId);
        params.put("deposit", 20000 * 100000000L);
        params.put("commissionRate", 10);
        params.put("packingAddress", packingAddr);
        params.put("password", password);
        params.put("rewardAddress", agentAddr);
        return params;
    }
}
