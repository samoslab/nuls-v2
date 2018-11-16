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

import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.nuls.network.constant.ManagerStatusEnum;
import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.constant.NetworkParam;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;

import java.util.Collection;
import java.util.List;

/**
 * 节点管理
 * node  manager
 * @author lan
 * @date 2018/11/01
 *
 */
public class NodeManager extends  BaseManager{
    private static NodeManager instance = new NodeManager();
    StorageManager storageManager = StorageManager.getInstance();
    LocalInfoManager localInfoManager = LocalInfoManager.getInstance();
    private ManagerStatusEnum status=ManagerStatusEnum.UNINITIALIZED;
    private NodeManager(){

    }

    public static NodeManager getInstance(){
        return instance;
    }
    public  boolean  isRunning(){
        return  instance.status==ManagerStatusEnum.RUNNING;
    }

    @Override
    public void init() {
        status=ManagerStatusEnum.INITIALIZED;
        Collection<NodeGroup> nodeGroups=NodeGroupManager.getInstance().getNodeGroupCollection();
        for(NodeGroup nodeGroup:nodeGroups){
            if(nodeGroup.isSelf()){
                //自有网络组，增加种子节点的加载，跨链网络组，则无此步骤
                loadSeedsNode(nodeGroup.getMagicNumber());
            }
            //数据库获取node
            List<Node> nodes=storageManager.getNodesByChainId(nodeGroup.getChainId());
            for(Node node:nodes){
                nodeGroup.addDisConnetNode(node,false);
            }
        }
    }

    @Override
    public void start() {

        status=ManagerStatusEnum.RUNNING;
    }


    /**
     * 加载种子节点
     * @param magicNumber
     */
    public void loadSeedsNode(long magicNumber){
        NetworkParam networkParam=NetworkParam.getInstance();
        List<String> list=networkParam.getSeedIpList();
        NodeGroup nodeGroup= NodeGroupManager.getInstance().getNodeGroupByMagic(networkParam.getPacketMagic());
        for(String seed:list){
            String []peer=seed.split(NetworkConstant.COLON);
            if(localInfoManager.isSelfIp(peer[0])){
                continue;
            }
            Node node=new Node(peer[0],Integer.valueOf(peer[1]),Node.OUT,false);
            nodeGroup.addDisConnetNode(node,false);
        }
    }
    public boolean isPeerSingleGroup(Channel channel){
        SocketChannel socketChannel = (SocketChannel) channel;
        String remoteIP = socketChannel.remoteAddress().getHostString();
        int port = socketChannel.remoteAddress().getPort();
        String nodeId = remoteIP+NetworkConstant.COLON+port;
        Node node = ConnectionManager.getInstance().getNodeByCache(nodeId);
        if(null != node){
            if(null != node.getNodeGroupConnectors() && node.getNodeGroupConnectors().size() > 1){
                return false;
            }
        }
        return true;
    }
}
