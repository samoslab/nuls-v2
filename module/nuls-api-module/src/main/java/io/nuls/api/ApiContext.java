/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Niels
 */
public class ApiContext {

    public static int mainChainId;

    public static int mainAssetId;

    public static String mainSymbol;

    public static int defaultChainId;

    public static int defaultAssetId;

    public static String defaultChainName;

    public static String defaultSymbol;

    public static int defaultDecimals;

    public static int agentChainId;

    public static int agentAssetId;

    public static int awardAssetId;

    public static String databaseUrl;

    public static int databasePort;

    public static String listenerIp;

    public static int rpcPort;

    public static String logLevel;

    public static String VERSION = "1.0";

    public static int maxAliveConnect;

    public static int maxWaitTime;

    public static int connectTimeOut;

    public static boolean isRunSmartContract;

    public static boolean isRunCrossChain;

    public static boolean isReady;
    //开发者节点地址
    public static Set<String> DEVELOPER_NODE_ADDRESS = new HashSet<>();
    //大使节点地址
    public static Set<String> AMBASSADOR_NODE_ADDRESS = new HashSet<>();
    //映射地址
    public static Set<String> MAPPING_ADDRESS = new HashSet<>();
    //商务地址
    public static String BUSINESS_ADDRESS1 = "SAMOdGs2w6pNmDeo9Bme5Mfy37vqJ5j6Nxxmq";
    public static String BUSINESS_ADDRESS2 = "SAMOdGs35Lsq76zSMcpKMzDgoiaYhY5BdZ84b";
    public static String BUSINESS_ADDRESS3 = "SAMOdGs31GWbXMPnJKyb6CNyjiQxsVvM6yd28";
    public static String BUSINESS_ADDRESS4 = "SAMOdGs31d1aVc7s4qoAWV4JBgucLzbntjAUJ";
    //团队地址
    public static String TEAM_ADDRESS = "SAMOdGs2xhZmdnBdjCfeakK9hCSC2PserCXiq";
    //社区地址
    public static String COMMUNITY_ADDRESS1 = "SAMOdGs34vvwpZTCisGNZim6ovs36xjsPCTQS";
    public static String COMMUNITY_ADDRESS2 = "SAMOdGs2z8DcxyStsMtmK5gRWrcLz2YR3LNAk";
    //销毁地址
    public static String DESTROY_ADDRESS = "";

    public static String COMMUNITY_ADDRESS = "";
    //销毁地址公钥
    public static byte[] blackHolePublicKey;

}
