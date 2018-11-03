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
package io.nuls.network.manager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.network.constant.NetworkErrorCode;
import io.nuls.network.manager.handler.base.BaseMeesageHandlerInf;
import io.nuls.network.manager.handler.NetworkMessageHandlerFactory;
import io.nuls.network.model.BroadcastResult;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.NodeGroupConnector;
import io.nuls.network.model.dto.IpAddress;
import io.nuls.network.model.message.*;

import io.nuls.network.model.message.base.BaseMessage;
import io.nuls.network.model.message.base.MessageHeader;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 消息管理器，用于收发消息
 * message manager：  send and rec msg
 * @author lan
 * @date 2018/11/01
 *
 */
public class MessageManager extends BaseManager{

    private static MessageManager instance = new MessageManager();
    public static MessageManager getInstance(){
        return instance;
    }
    public void  sendToNode(BaseMessage message, Node node, boolean aysn) {
        //向节点发送消息
        broadcastToANode(message,node,aysn);

    }

    public  BaseMessage getMessageInstance(String command) {
        Class<? extends BaseMessage> msgClass  = MessageFactory.getMessage(command);
        if (null == msgClass) {
            return null;
        }
        try {
            BaseMessage  message = msgClass.getDeclaredConstructor().newInstance();
            return message;
        } catch (InstantiationException e) {
            Log.error(e);

        } catch (IllegalAccessException e) {
            Log.error(e);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean validate(BaseMessage data){
        if (data.getHeader() == null) {
            Log.error("NET_MESSAGE_ERROR");
            return false;
        }
        if(data.getHeader().getPayloadLength()==0 && data.getMsgBody() == null){
            //ok
            return true;
        } else if (data.getHeader().getPayloadLength() != data.getMsgBody().size()) {
            Log.error("NET_MESSAGE_LENGTH_ERROR");
            return false;
        }

         if(!data.isCheckSumValid()){
                Log.error("NET_MESSAGE_CHECKSUM_ERROR");
                return false;
            }
        return true;
    }
    public void receiveMessage(ByteBuf buffer,String nodeKey,boolean isServer) throws NulsException {
        //统一接收消息处理
        try {
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            NulsByteBuffer byteBuffer = new NulsByteBuffer(bytes);
            while (!byteBuffer.isFinished()) {
                MessageHeader header = byteBuffer.readNulsData(new MessageHeader());
                Log.debug((isServer?"Server":"Client")+":----receive message-- magicNumber:"+ header.getMagicNumber()+"==CMD:"+header.getCommandStr());
                byteBuffer.setCursor(byteBuffer.getCursor() - header.size());
                BaseMessage message=MessageManager.getInstance().getMessageInstance(header.getCommandStr());
                BaseMeesageHandlerInf handler=NetworkMessageHandlerFactory.getInstance().getHandler(message);
                message = byteBuffer.readNulsData(message);
               if(!validate(message)){
                     return;
               }else{
                   handler.recieve(message,nodeKey,isServer);
               }
               }

        } catch (Exception e) {
            e.printStackTrace();
            throw new NulsException(NetworkErrorCode.DATA_ERROR, e);
        } finally {
            buffer.clear();
        }
    }

    public BroadcastResult broadcastSelfAddrToAllNode(boolean asyn) {
        if(LocalInfoManager.getInstance().isAddrBroadcast()){
            return new BroadcastResult(true, NetworkErrorCode.SUCCESS);
        }
        List<Node> connectNodes= ConnectionManager.getInstance().getCacheAllNodeList();
        for(Node connectNode:connectNodes){
            List<NodeGroupConnector> nodeGroupConnectors=connectNode.getNodeGroupConnectors();
            for(NodeGroupConnector nodeGroupConnector:nodeGroupConnectors){
                if(Node.HANDSHAKE == nodeGroupConnector.getStatus()){
                    List<IpAddress> addressesList=new ArrayList<>();
                    addressesList.add(LocalInfoManager.getInstance().getExternalAddress());
                    AddrMessage addrMessage= MessageFactory.getInstance().buildAddrMessage(addressesList,nodeGroupConnector.getMagicNumber());
                    this.sendToNode(addrMessage,connectNode,asyn);
                }

            }
        }

        if(connectNodes.size() > 0) {
            //已经广播
            LocalInfoManager.getInstance().setAddrBroadcast(true);
        }
        return new BroadcastResult(true, NetworkErrorCode.SUCCESS);
    }
    public BroadcastResult broadcastAddrToAllNode(BaseMessage addrMessage, Node excludeNode,boolean asyn) {
         NodeGroup nodeGroup=NodeGroupManager.getInstance().getNodeGroupByMagic(addrMessage.getHeader().getMagicNumber());
        Collection<Node> connectNodes=nodeGroup.getConnectNodes();
        if(null != connectNodes && connectNodes.size()>0){
            for(Node connectNode:connectNodes){
                if(connectNode.getId().equals(excludeNode.getId())){
                    continue;
                }
                this.sendToNode(addrMessage,connectNode,asyn);
            }
        }
        return new BroadcastResult(true, NetworkErrorCode.SUCCESS);
    }

    public BroadcastResult broadcastToANode(BaseMessage message, Node node, boolean asyn) {
//        NetworkParam
        NodeGroupConnector nodeGroupConnector= node.getNodeGroupConnector(message.getHeader().getMagicNumber());
        if (Node.HANDSHAKE !=nodeGroupConnector.getStatus()) {
            return new BroadcastResult(false, NetworkErrorCode.NET_NODE_DEAD);
        }
        if (node.getChannel() == null || !node.getChannel().isActive()) {
            return new BroadcastResult(false, NetworkErrorCode.NET_NODE_MISS_CHANNEL);
        }
        try {
            MessageHeader header = message.getHeader();
            BaseNulsData body = message.getMsgBody();
            header.setPayloadLength(body.size());
            ChannelFuture future = node.getChannel().writeAndFlush(Unpooled.wrappedBuffer(message.serialize()));
            if (!asyn) {
                future.await();
                boolean success = future.isSuccess();
                if (!success) {
                    return new BroadcastResult(false, NetworkErrorCode.NET_BROADCAST_FAIL);
                }
            }
        } catch (Exception e) {
            Log.error(e);
            return new BroadcastResult(false, NetworkErrorCode.NET_MESSAGE_ERROR);
        }
        return new BroadcastResult(true, NetworkErrorCode.SUCCESS);
    }

    @Override
    public void init() {
        MessageFactory.putMessage(VersionMessage.class);
        MessageFactory.putMessage(VerackMessage.class);
        MessageFactory.putMessage(GetAddrMessage.class);
        MessageFactory.putMessage(AddrMessage.class);
    }

    @Override
    public void start() {

    }
}
