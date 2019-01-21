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

package io.nuls.block.test;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.block.constant.BlockErrorCode;
import io.nuls.block.utils.module.NetworkUtil;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;

import java.util.Map;

import static io.nuls.block.utils.LoggerUtil.messageLog;
import static io.nuls.block.utils.LoggerUtil.sendLog;

/**
 * 处理收到的{@link TestMessage},用测试消息收发的稳定性
 *
 * @author captain
 * @version 1.0
 * @date 18-11-14 下午4:23
 */
@Component
public class TestMessageHandler extends BaseCmd {

    @CmdAnnotation(cmd = "test", version = 1.0, scope = Constants.PUBLIC, description = "")
    public Response process(Map map) {
        Integer chainId = Integer.parseInt(map.get("chainId").toString());
        String nodeId = map.get("nodeId").toString();
        TestMessage message = new TestMessage();

        byte[] decode = HexUtil.decode(map.get("messageBody").toString());
        message.parse(new NulsByteBuffer(decode));

        if (message == null || nodeId == null) {
            return failed(BlockErrorCode.PARAMETER_ERROR);
        }

        int index = message.getIndex();
        messageLog.info("recieve TestMessage from node-" + nodeId + ", chainId:" + chainId + ", index:" + index);
        try {
            Thread.sleep(1000L);
            boolean b = NetworkUtil.sendToNode(chainId, new TestMessage(index + 1), nodeId, "test");
            sendLog.info("send index:" + message.getIndex() + " to node-" + nodeId + ", chainId:" + chainId + ", success:" + b);
        } catch (Exception e) {
            return failed(BlockErrorCode.PARAMETER_ERROR);
        }
        return success();
    }

}
