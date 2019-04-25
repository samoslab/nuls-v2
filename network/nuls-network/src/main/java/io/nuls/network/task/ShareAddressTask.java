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

import io.nuls.network.cfg.NetworkConfig;
import io.nuls.network.manager.ConnectionManager;
import io.nuls.network.manager.MessageManager;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.dto.IpAddress;
import io.nuls.network.utils.LoggerUtil;
import io.nuls.tools.core.ioc.SpringLiteContext;

import java.util.*;

public class ShareAddressTask implements Runnable {

    private final NetworkConfig networkConfig = SpringLiteContext.getBean(NetworkConfig.class);
    private NodeGroup nodeGroup = null;
    private boolean isCross = false;

    private ConnectionManager connectionManager = ConnectionManager.getInstance();

    public ShareAddressTask(NodeGroup nodeGroup,boolean isCross) {
        this.nodeGroup = nodeGroup;
        this.isCross = isCross;
    }

    @Override
    public void run() {
        if (isCross) {
            doCrossNet();
        } else {
            doLocalNet();
        }

    }

    private void doLocalNet() {
        //getMoreNodes
        MessageManager.getInstance().sendGetAddrMessage(nodeGroup, false, true);
        //shareMyServer
        String externalIp = getMyExtranetIp();
        if (externalIp == null) {
            return;
        }
        networkConfig.getLocalIps().add(externalIp);
        /*自有网络的连接分享*/
        if (!nodeGroup.isMoonCrossGroup()) {
            LoggerUtil.logger().info("share self ip  is {}", externalIp);
            Node myNode = new Node(nodeGroup.getMagicNumber(), externalIp, networkConfig.getPort(), Node.OUT, false);
            myNode.setConnectedListener(() -> {
                myNode.getChannel().close();
                doShare(externalIp, nodeGroup.getLocalNetNodeContainer().getConnectedNodes().values(), networkConfig.getPort());
            });
            myNode.setDisconnectListener(() -> myNode.setChannel(null));
            connectionManager.connection(myNode);
        }
    }

    private void doCrossNet() {
        //getMoreNodes
        MessageManager.getInstance().sendGetAddrMessage(nodeGroup, true, true);
        //shareMyServer
        String externalIp = getMyExtranetIp();
        if (externalIp == null) {
            return;
        }
        networkConfig.getLocalIps().add(externalIp);
        if (nodeGroup.isCrossActive()) {
            //开启了跨链业务
            Node crossNode = new Node(nodeGroup.getMagicNumber(), externalIp, networkConfig.getCrossPort(), Node.OUT, true);
            crossNode.setConnectedListener(() -> {
                crossNode.getChannel().close();
                doShare(externalIp, nodeGroup.getCrossNodeContainer().getConnectedNodes().values(), networkConfig.getCrossPort());
            });
            connectionManager.connection(crossNode);
        }
    }

    private String getMyExtranetIp() {
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(nodeGroup.getLocalNetNodeContainer().getConnectedNodes().values());
        nodes.addAll(nodeGroup.getCrossNodeContainer().getConnectedNodes().values());
        return getMostSameIp(nodes);
    }

    private String getMostSameIp(Collection<Node> nodes) {

        Map<String, Integer> ipMaps = new HashMap<>();

        for (Node node : nodes) {
            String ip = node.getExternalIp();
            if (ip == null) {
                continue;
            }
            Integer count = ipMaps.get(ip);
            if (count == null) {
                ipMaps.put(ip, 1);
            } else {
                ipMaps.put(ip, count + 1);
            }
        }

        int maxCount = 0;
        String ip = null;
        for (Map.Entry<String, Integer> entry : ipMaps.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                ip = entry.getKey();
            }
        }

        return ip;
    }

    private void doShare(String externalIp, Collection<Node> nodes, int port) {
        IpAddress ipAddress = new IpAddress(externalIp, port);
        MessageManager.getInstance().broadcastSelfAddrToAllNode(nodes, ipAddress, true);
    }


}
