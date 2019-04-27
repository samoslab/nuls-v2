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
package io.nuls.network.task;

import io.netty.channel.socket.SocketChannel;
import io.nuls.network.cfg.NetworkConfig;
import io.nuls.network.manager.NodeGroupManager;
import io.nuls.network.manager.TimeManager;
import io.nuls.network.manager.handler.MessageHandlerFactory;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.dto.ProtocolRoleHandler;
import io.nuls.network.netty.container.NodesContainer;
import io.nuls.network.utils.LoggerUtil;
import io.nuls.tools.core.ioc.SpringLiteContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Group event monitor
 * 测试 定时打印连接信息
 *
 * @author lan
 * @create 2018/11/14
 */
public class NwInfosPrintTask implements Runnable {
    @Override
    public void run() {
        printlnPeer();
    }

    private void printlnProtocolMap() {
        Collection<Map<String, ProtocolRoleHandler>> values = MessageHandlerFactory.getInstance().getProtocolRoleHandlerMap().values();
        LoggerUtil.logger().debug("protocolRoleHandler ==================");
        StringBuilder stringBuilder = new StringBuilder();
        for (Map<String, ProtocolRoleHandler> map : values) {
            Collection<ProtocolRoleHandler> list = map.values();
            for (ProtocolRoleHandler protocolRoleHandler : list) {
                stringBuilder.append("{role:").append(protocolRoleHandler.getRole()).append(",cmd:").append(protocolRoleHandler.getHandler()).append("}");

            }
        }
        LoggerUtil.logger().debug("protocolRoleHandler={}", stringBuilder.toString());

    }

    private void printlnPeer() {
        NodeGroupManager nodeGroupManager = NodeGroupManager.getInstance();
        List<NodeGroup> nodeGroupList = nodeGroupManager.getNodeGroups();
        NetworkConfig networkConfig = SpringLiteContext.getBean(NetworkConfig.class);
        if(networkConfig.isMoonNode()){
            for (NodeGroup nodeGroup : nodeGroupList) {
                if(nodeGroup.isMoonCrossGroup()){
                    printCross(nodeGroup);
                }else{
                    printLocalNet(nodeGroup);
                }
            }
        }else{
            for (NodeGroup nodeGroup : nodeGroupList) {
                printLocalNet(nodeGroup);
                printCross(nodeGroup);
            }
        }

    }

    private void printCross(NodeGroup nodeGroup){
        NodesContainer crossNodesContainer = nodeGroup.getCrossNodeContainer();
        Collection<Node> d1 = crossNodesContainer.getConnectedNodes().values();
        Collection<Node> d2 = crossNodesContainer.getCanConnectNodes().values();
        Collection<Node> d3 = crossNodesContainer.getDisconnectNodes().values();
        Collection<Node> d4 = crossNodesContainer.getUncheckNodes().values();
        Collection<Node> d5 = crossNodesContainer.getFailNodes().values();
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("BEGIN @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("(跨链网络)begin printlnPeer :CrossConnectNodes-网络时间time = {},offset={}", TimeManager.currentTimeMillis(), TimeManager.netTimeOffset);
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("######################################################################");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("@@@@@@@@@@@ chainId={},magicNumber={},crossNetStatus(跨链)={}",
                nodeGroup.getChainId(), nodeGroup.getMagicNumber(), nodeGroup.getCrossStatus());
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(connected)已连接信息**********");
        for (Node n : d1) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("**connected:{},info:blockHash={},blockHeight={},channelId={}", n.getId(), n.getBlockHash(), n.getBlockHeight(), n.getChannel().id());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(canConnect)可连接信息**********");
        for (Node n : d2) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****canConnect:{},info:blockHash={},blockHeight={}", n.getId(), n.getBlockHash(), n.getBlockHeight());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(disConnect)断开连接信息**********");
        for (Node n : d3) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****disConnect:{},info:blockHash={},blockHeight={}", n.getId(), n.getBlockHash(), n.getBlockHeight());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(uncheck)待检测连接信息**********");
        for (Node n : d4) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****uncheck:{},info:blockHash={},blockHeight={}", n.getId(), n.getBlockHash(), n.getBlockHeight());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(failed)失败连接信息**********");
        for (Node n : d5) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****failed:{},FailCount = {},info:blockHash={},blockHeight={}", n.getId(), n.getFailCount(), n.getBlockHash(), n.getBlockHeight());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("@@@@@@@@@@@ @@@@@@@@@@@ @@@@@@@@@@@ @@@@@@@@@@@ end==============");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("#####################################################################");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("");

    }

    private void printLocalNet(NodeGroup nodeGroup){
        NodesContainer localNodesContainer = nodeGroup.getLocalNetNodeContainer();
        Collection<Node> c1 = localNodesContainer.getConnectedNodes().values();
        Collection<Node> c2 = localNodesContainer.getCanConnectNodes().values();
        Collection<Node> c3 = localNodesContainer.getDisconnectNodes().values();
        Collection<Node> c4 = localNodesContainer.getUncheckNodes().values();
        Collection<Node> c5 = localNodesContainer.getFailNodes().values();
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("BEGIN @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("(自有网络)begin printlnPeer :SelfConnectNodes-网络时间time = {},offset={}", TimeManager.currentTimeMillis(), TimeManager.netTimeOffset);
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("######################################################################");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("@@@@@@@@@@@ chainId={},magicNumber={},localNetStatus(本地网络)={}",
                nodeGroup.getChainId(), nodeGroup.getMagicNumber(), nodeGroup.getLocalStatus());
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(connected)已连接信息**********");
        for (Node n : c1) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("**connected:{},info:blockHash={},blockHeight={},channelId={},connStatus={}",
                    n.getId(), n.getBlockHash(), n.getBlockHeight(), n.getChannel().id().asLongText(),n.getConnectStatus());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(canConnect)可连接信息**********");
        for (Node n : c2) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("**:{},info:blockHash={},blockHeight={},nodeStatus={}",
                    n.getId(), n.getBlockHash(), n.getBlockHeight(),n.getStatus());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(disConnect)断开连接信息**********");
        for (Node n : c3) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("**{},info:blockHash={},blockHeight={}", n.getId(), n.getBlockHash(), n.getBlockHeight());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(uncheck)待检测连接信息**********");
        for (Node n : c4) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("**{},info:blockHash={},blockHeight={}", n.getId(), n.getBlockHash(), n.getBlockHeight());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("*****(failed)失败连接信息**********");
        for (Node n : c5) {
            LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("**{},FailCount = {},info:blockHash={},blockHeight={}", n.getId(), n.getFailCount(), n.getBlockHash(), n.getBlockHeight());
        }
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("======================================================================");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("@@@@@@@@@@@ @@@@@@@@@@@ @@@@@@@@@@@ @@@@@@@@@@@ end==============");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("#####################################################################");
        LoggerUtil.nwInfosLogger(nodeGroup.getChainId()).info("");
    }
}
