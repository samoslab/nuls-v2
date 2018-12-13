/*
 * MIT License
 * Copyright (c) 2017-2018 nuls.io
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
package io.nuls.block.utils.module;

import io.nuls.base.data.NulsDigestData;
import io.nuls.block.constant.CommandConstant;
import io.nuls.block.message.BaseMessage;
import io.nuls.block.message.CompleteMessage;
import io.nuls.block.model.Node;
import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用网络模块接口的工具
 * @author captain
 * @date 18-11-9 下午3:48
 * @version 1.0
 */
public class NetworkUtil {

    static {
        /*
         * 从kernel获取所有接口列表（实际使用中不需要每次都调用这句话，同步一次即可）
         */
        try {
            CmdDispatcher.syncKernel();
        } catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * todo 待完善
     * 获取可用节点
     * @date 18-11-9 下午3:49
     * @param
     * @return
     */
    public static List<Node> getAvailableNodes(int chainId) {
        try {
            Map<String, Object> params = new HashMap<>(6);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            params.put("state", 1);
            params.put("isCross", 0);
            params.put("startPage", 0);
            params.put("pageSize", 0);

            Response response = CmdDispatcher.requestAndResponse(ModuleE.NW.abbr, "nw_getNodes", params);
            Map responseData = (Map) response.getResponseData();
            List list = (List) responseData.get("nw_getNodes");
            List nodes = new ArrayList();
            for (Object o : list) {
                Map map = (Map) o;
                Node node = new Node();
                node.setId((String) map.get("nodeId"));
                node.setHeight(Long.parseLong(map.get("blockHeight").toString()));
//            node.setHash(NulsDigestData.fromDigestHex((String) map.get("blockHash")));
                nodes.add(node);
            }
            return nodes;
        } catch (Exception e) {
            Log.error(e);
            return List.of();
        }
    }

    /**
     * 重置网络节点
     * @date 18-11-9 下午3:49
     * @param
     * @return
     */
    public static void resetNetwork(int chainId){
        try {
            Map<String, Object> params = new HashMap<>(2);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);

            CmdDispatcher.requestAndResponse(ModuleE.NW.abbr, "nw_reconnect", params);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * 给网络上节点广播消息
     * @param chainId
     * @param message
     * @param excludeNodes      排除的节点
     * @return
     */
    public static boolean broadcast(int chainId, BaseMessage message, String excludeNodes){
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            params.put("excludeNodes", excludeNodes);
            params.put("messageBody", HexUtil.byteToHex(message.serialize()));
            params.put("command", message.getCommand());

            return CmdDispatcher.requestAndResponse(ModuleE.NW.abbr, "nw_broadcast", params).isSuccess();
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    /**
     * 给指定节点发送消息
     * @param chainId
     * @param message
     * @param nodeId
     * @return
     */
    public static boolean sendToNode(int chainId, BaseMessage message, String nodeId){
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            params.put("nodes", nodeId);
            params.put("messageBody", HexUtil.byteToHex(message.serialize()));
            params.put("command", message.getCommand());

            return CmdDispatcher.requestAndResponse(ModuleE.NW.abbr, "nw_sendPeersMsg", params).isSuccess();
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    /**
     * 给网络上节点广播消息
     * @param chainId
     * @param message
     * @return
     */
    public static boolean broadcast(int chainId, BaseMessage message){
        return broadcast(chainId, message, null);
    }

    public static void sendFail(int chainId, NulsDigestData hash, String nodeId) {
        CompleteMessage message = new CompleteMessage();
        message.setRequestHash(hash);
        message.setSuccess(false);
        message.setCommand(CommandConstant.COMPLETE_MESSAGE);
        boolean result = sendToNode(chainId, message, nodeId);
        if (!result) {
            Log.warn("send fail message failed:{}, hash:{}", nodeId, hash);
        }
    }

    public static void sendSuccess(int chainId, NulsDigestData hash, String nodeId) {
        CompleteMessage message = new CompleteMessage();
        message.setRequestHash(hash);
        message.setSuccess(true);
        message.setCommand(CommandConstant.COMPLETE_MESSAGE);
        boolean result = sendToNode(chainId, message, nodeId);
        if (!result) {
            Log.warn("send success message failed:{}, hash:{}", nodeId, hash);
        }
    }

    public static void setHashAndHeight(int chainId, NulsDigestData hash, long height, String nodeId) {
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            params.put("nodeId", nodeId);
            params.put("blockHeight", height);
            params.put("blockHash", hash.toString());

            CmdDispatcher.requestAndResponse(ModuleE.NW.abbr, "nw_updateNodeInfo", params);
        } catch (Exception e) {
            Log.error(e);
        }
    }

}
