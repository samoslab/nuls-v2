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

import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.manager.NodeGroupManager;
import io.nuls.network.manager.handler.base.BaseMessageHandler;
import io.nuls.network.model.NetworkEventResult;
import io.nuls.network.model.Node;
import io.nuls.network.model.NodeGroup;
import io.nuls.network.model.message.base.BaseMessage;
import io.nuls.tools.log.Log;

/**
 * @program: nuls2.0
 * @description: bye message handler
 * @author: lan
 * @create: 2018/11/13
 **/
public class ByeMessageHandler extends BaseMessageHandler {
    private static ByeMessageHandler instance = new ByeMessageHandler();

    private ByeMessageHandler() {

    }

    public static ByeMessageHandler getInstance() {
        return instance;
    }

    @Override
    public NetworkEventResult recieve(BaseMessage message, String nodeKey, boolean isServer) {
        long magicNumber = message.getHeader().getMagicNumber();
        NodeGroup nodeGroup = NodeGroupManager.getInstance().getNodeGroupByMagic(magicNumber);
        if(null == nodeGroup){
            Log.error("(magicNumber="+magicNumber+")group is not exist!");
            return new NetworkEventResult(false, null);
        }
        Node node = nodeGroup.getConnectNode(nodeKey);
        if(null == node){
            Log.error("(nodeKey="+nodeKey+")node is not exist!");
            return new NetworkEventResult(false, null);
        }

        nodeGroup.removePeerNode(node,true,false);
        if(Node.OUT == node.getType()) {
            nodeGroup.addFailConnect(node.getId(), NetworkConstant.CONNECT_FAIL_LOCK_MINUTE);
        }
        return  new NetworkEventResult(true, null);
    }
}
