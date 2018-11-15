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

package io.nuls.network.manager.handler;

import io.nuls.network.constant.NetworkParam;
import io.nuls.network.manager.*;
import io.nuls.network.manager.handler.base.BaseMessageHandler;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.NodeGroupConnector;
import io.nuls.network.model.message.base.BaseMessage;
import io.nuls.network.model.message.VerackMessage;
import io.nuls.network.model.message.VersionMessage;
import io.nuls.network.model.message.body.VerackMessageBody;
import io.nuls.network.model.message.body.VersionMessageBody;
import io.nuls.tools.log.Log;

/**
 * version message handler
 * @author lan
 * @date 2018/10/20
 */
public class VersionMessageHandler extends BaseMessageHandler {

    private static VersionMessageHandler instance = new VersionMessageHandler();
    NodeGroupManager nodeGroupManager=NodeGroupManager.getInstance();
    private VersionMessageHandler() {

    }

    public static VersionMessageHandler getInstance() {
        return instance;
    }
    LocalInfoManager   localInfoManager =LocalInfoManager.getInstance();

    /**
     *  server recieve handler
     * @param message
     * @param nodeKey
     */
    public void serverRecieveHandler(BaseMessage message, String nodeKey){
        VersionMessageBody versionBody=(VersionMessageBody)message.getMsgBody();
        Node node =ConnectionManager.getInstance().getNodeByCache(nodeKey,Node.IN);
        Log.debug("VersionMessageHandler Recieve:"+"Server"+":"+node.getIp()+":"+node.getRemotePort()+"==CMD=" +message.getHeader().getCommandStr());
        NodeGroup nodeGroup=nodeGroupManager.getNodeGroupByMagic(message.getHeader().getMagicNumber());
        String peerIp = versionBody.getAddrYou().getIp().getHostAddress();
        int peerPort = versionBody.getAddrYou().getPort();
        localInfoManager.updateExternalAddress(peerIp,peerPort);
        int maxIn=0;
        if(node.isCrossConnect()){
            maxIn=nodeGroup.getMaxCrossIn();
        }else{
            maxIn=nodeGroup.getMaxIn();
        }
        /**
         *会存在情况如：种子节点 启动 无法扫描到自己的IP 只有 到握手时候才能知道自己外网IP，发现是自连。
         */
        //远程地址与自己地址相同 或者 连接满额处理
        if(LocalInfoManager.getInstance().isSelfIp(node.getIp()) || ConnectionManager.getInstance().isPeerConnectExceedMaxIn(node.getIp(),nodeGroup.getMagicNumber(),maxIn)){
            if(node.getNodeGroupConnectors().size() == 0){
                node.getChannel().close();
                node.setCanConnect(true);
                return;
            }else{
                //client 回复过载消息--reply over maxIn
                VerackMessage verackMessage=MessageFactory.getInstance().buildVerackMessage(node,message.getHeader().getMagicNumber(), VerackMessageBody.VER_CONNECT_MAX);
                MessageManager.getInstance().sendToNode(verackMessage,node,true);
                return;
            }
        }else{
            //add maxIn count
            ConnectionManager.getInstance().addGroupMaxInIp(node,nodeGroup.getMagicNumber());
        }
        //服务端首次知道channel的网络属性，进行channel归属
        node.addGroupConnector(message.getHeader().getMagicNumber());
        NodeGroupConnector nodeGroupConnector=node.getNodeGroupConnector(message.getHeader().getMagicNumber());
        //node加入到Group的未连接中
        nodeGroupConnector.setStatus(Node.CONNECTING);
        nodeGroup.addDisConnetNode(node,true);
        //存储需要的信息
        node.setVersionProtocolInfos(message.getHeader().getMagicNumber(),versionBody.getProtocolVersion(),versionBody.getBlockHeight(),versionBody.getBlockHash());
        node.setRemoteCrossPort(versionBody.getPortMeCross());
        //回复version
        VersionMessage   versionMessage = MessageFactory.getInstance().buildVersionMessage(node,message.getHeader().getMagicNumber());
        send(versionMessage, node, true,true);
    }

    /**
     * client recieve handler
     * @param message
     * @param nodeKey
     */
    public void clientRecieveHandler(BaseMessage message, String nodeKey){
        VersionMessageBody versionBody=(VersionMessageBody)message.getMsgBody();
        localInfoManager.updateExternalAddress(versionBody.getAddrYou().getIp().getHostAddress(),NetworkParam.getInstance().getPort());
        Node node = nodeGroupManager.getNodeGroupByMagic(message.getHeader().getMagicNumber()).getDisConnectNodeMap().get(nodeKey);
        Log.debug("VersionMessageHandler Recieve:Client"+":"+node.getIp()+":"+node.getRemotePort()+"==CMD=" +message.getHeader().getCommandStr());
        NodeGroupConnector nodeGroupConnector=node.getNodeGroupConnector(message.getHeader().getMagicNumber());
        nodeGroupConnector.setStatus(Node.HANDSHAKE);
        //node加入到Group的连接中
        NodeGroup nodeGroup=nodeGroupManager.getNodeGroupByMagic(message.getHeader().getMagicNumber());
        nodeGroup.addConnetNode(node,true);
        //存储需要的信息
        node.setVersionProtocolInfos(message.getHeader().getMagicNumber(),versionBody.getProtocolVersion(),versionBody.getBlockHeight(),versionBody.getBlockHash());
        node.setRemoteCrossPort(versionBody.getPortMeCross());
        //client:接收到server端消息，进行verack答复
        VerackMessage verackMessage=MessageFactory.getInstance().buildVerackMessage(node,message.getHeader().getMagicNumber(), VerackMessageBody.VER_SUCCESS);
        //从已连接池中获取node节点
        node = nodeGroupManager.getNodeGroupByMagic(message.getHeader().getMagicNumber()).getConnectNodeMap().get(nodeKey);
        MessageManager.getInstance().sendToNode(verackMessage,node,true);
        //自我连接
        ConnectionManager.getInstance().selfConnection();
    }
    @Override
    public NetworkEventResult recieve(BaseMessage message, String nodeKey,boolean isServer) {
        if(isServer){
            serverRecieveHandler(message,nodeKey);
        }else{
            clientRecieveHandler(message, nodeKey);
        }
        return null;
    }

    @Override
    public NetworkEventResult send(BaseMessage message, Node node, boolean isServer, boolean asyn) {
        Log.debug("VersionMessageHandler send:"+(isServer?"Server":"Client")+":"+node.getIp()+":"+node.getRemotePort()+"==CMD=" +message.getHeader().getCommandStr());
        //VERSION 协议的发送中，节点所属的网络业务状态是连接中
        NodeGroupConnector nodeGroupConnector=node.getNodeGroupConnector(message.getHeader().getMagicNumber());
        nodeGroupConnector.setStatus(Node.CONNECTING);
        return super.send(message,node,isServer,asyn);
    }
}
