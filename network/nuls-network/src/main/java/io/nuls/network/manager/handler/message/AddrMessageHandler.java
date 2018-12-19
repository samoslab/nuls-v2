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

package io.nuls.network.manager.handler.message;

import io.nuls.network.constant.NetworkErrorCode;
import io.nuls.network.manager.MessageFactory;
import io.nuls.network.manager.MessageManager;
import io.nuls.network.manager.NodeGroupManager;
import io.nuls.network.manager.StorageManager;
import io.nuls.network.manager.handler.base.BaseMessageHandler;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.dto.IpAddress;
import io.nuls.network.model.message.AddrMessage;
import io.nuls.network.model.message.base.BaseMessage;
import io.nuls.network.model.po.NodePo;
import java.util.ArrayList;
import java.util.List;

import static io.nuls.network.utils.LoggerUtil.Log;


/**
 * address message handler
 * @author lan
 * @date 2018/11/02
 *
 */
public class AddrMessageHandler extends BaseMessageHandler {

    private static AddrMessageHandler instance = new AddrMessageHandler();

    private AddrMessageHandler() {

    }

    public static AddrMessageHandler getInstance() {
        return instance;
    }

    /**
     *
     *@description  接收消息处理
     * Receive message processing
     * @param message   address message
     * @param nodeKey      peer node key
     * @param isServer client=false or server=true
     * @return NetworkEventResult
     */
    @Override
    public NetworkEventResult recieve(BaseMessage message, String nodeKey,boolean isServer) {
        NodeGroup nodeGroup=NodeGroupManager.getInstance().getNodeGroupByMagic(message.getHeader().getMagicNumber());
        Log.info("============================nodeKey="+nodeKey);
        Node node =nodeGroup .getConnectNodeMap().get(nodeKey);
        Log.debug("AddrMessageHandler Recieve:"+(isServer?"Server":"Client")+":"+node.getIp()+":"+node.getRemotePort()+"==CMD=" +message.getHeader().getCommandStr());
        AddrMessage addrMessage=(AddrMessage)message;
        /*
         *空消息错误返回
         *Empty message error return
         */
        if(null == addrMessage.getMsgBody()){
            Log.error("rec error addr message.");
            return NetworkEventResult.getResultFail(NetworkErrorCode.NET_MESSAGE_ERROR);
        }
        List<IpAddress> ipAddressList=addrMessage.getMsgBody().getIpAddressList();
        /*
         * 判断地址是否本地已经拥有，如果拥有不转发，PEER是跨链网络也不转发
         * Determine whether the address is already owned locally. If it does not forward, PEER is not a cross-chain network.
         */
        List<NodePo> addNodes=new ArrayList<>();
        List<IpAddress> addAddressList=new ArrayList<>();
        if(node.isCrossConnect()){
            /*
             * 跨链，只存储，不转发
             * Cross-chain, only store, not forward
             */
            for(IpAddress ipAddress:ipAddressList){
                String ip=ipAddress.getIp().getHostAddress();
                int port=ipAddress.getPort();
                Log.info("======ip:"+ip+":"+ipAddress.getPort());
                String nodeId=ip+":"+port;
                /*
                 * 不在已有的列表中，增加存储
                 *Not in the existing list, increase storage
                 */
                if(!nodeGroup.existCrossGroupList(nodeId)){
                    //add storage node
                    Node crossNode=new Node(ip,port,Node.OUT,true);
                    nodeGroup.addDisConnetNode(crossNode,false);
                    addNodes.add((NodePo)crossNode.parseToPo());
                }
            }
        }else{
            /*
             *  自有网络，存储，转发
             *  Own network, storage, forwarding
             */
            for(IpAddress ipAddress:ipAddressList){
                String ip=ipAddress.getIp().getHostAddress();
                int port=ipAddress.getPort();
                Log.info("======ip:"+ip+":"+ipAddress.getPort());
                String nodeId=ip+":"+port;
                if(!nodeGroup.existSelfGroupList(nodeId)){
                    //add storage node
                    Node selfNode=new Node(ip,port,Node.OUT,false);
                    nodeGroup.addDisConnetNode(selfNode,false);
                    IpAddress addIpAddress=new IpAddress(ip,port);
                    addAddressList.add(addIpAddress);
                    addNodes.add((NodePo)selfNode.parseToPo());
                }
            }

            if(addAddressList.size()>0){
                /*
                *向自有网络广播
                * Broadcast to own network
                */
                AddrMessage addrMessagebroadCast=MessageFactory.getInstance().buildAddrMessage(addAddressList,nodeGroup.getMagicNumber());
                Log.info("SEND addrMessagebroadCast:"+addrMessagebroadCast.getMsgBody().size());
                MessageManager.getInstance().broadcastAddrToAllNode(addrMessagebroadCast,node,true);

            }
        }
        /*
         *存储节点信息
         * Broadcast to own network
         */
        if (addNodes.size() > 0) {
            StorageManager.getInstance().saveNodes(addNodes,nodeGroup.getChainId());
        }

        return NetworkEventResult.getResultSuccess();
    }

    /**
     *
     * AddrMessageHandler sending a message
     * @param message   address message
     * @param node      peer info
     * @param isServer client=false or server=true
     * @param asyn  default true
     * @return NetworkEventResult
     */
    @Override
    public NetworkEventResult send(BaseMessage message, Node node, boolean isServer, boolean asyn) {
        Log.debug("AddrMessageHandler Send:"+(isServer?"Server":"Client")+":"+node.getIp()+":"+node.getRemotePort()+"==CMD=" +message.getHeader().getCommandStr());
        return super.send(message,node,isServer,asyn);
    }
}
