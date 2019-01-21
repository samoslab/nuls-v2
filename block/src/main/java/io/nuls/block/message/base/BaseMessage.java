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

package io.nuls.block.message.base;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsDigestData;
import io.nuls.tools.constant.ToolsConstant;
import io.nuls.tools.exception.NulsException;

import java.io.IOException;

import static io.nuls.block.utils.LoggerUtil.commonLog;

/**
 * 通用消息体
 *
 * @author captain
 * @version 1.0
 * @date 18-11-20 上午10:44
 */
public abstract class BaseMessage extends BaseNulsData {

    private transient NulsDigestData hash;

    @Override
    public int size() {
        return 4;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(ToolsConstant.PLACE_HOLDER);

    }

    @Override
    public void parse(NulsByteBuffer buffer) throws NulsException {
        buffer.readBytes(4);
    }

    public NulsDigestData getHash() {
        if (hash == null) {
            try {
                this.hash = NulsDigestData.calcDigestData(this.serialize());
            } catch (IOException e) {
                e.printStackTrace();
                commonLog.error(e);
            }
        }
        return hash;
    }

}
