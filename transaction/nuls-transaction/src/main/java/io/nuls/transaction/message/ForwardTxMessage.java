package io.nuls.transaction.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.NulsDigestData;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.SerializeUtils;
import io.nuls.transaction.message.base.BaseMessage;

import java.io.IOException;

/**
 * 处理完来自其他节点的交易时，再转发出去
 * 转发来自其他节点的交易
 * 转发交易hash
 * @author: Charlie
 * @date: 2019/04/17
 */
public class ForwardTxMessage extends BaseMessage {
    /**
     * 交易hash
     */
    private NulsDigestData hash;

    @Override
    public NulsDigestData getHash() {
        return hash;
    }

    @Override
    public void setHash(NulsDigestData hash) {
        this.hash = hash;
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(hash);
        return size;
    }

    @Override
    public void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(hash);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.hash = byteBuffer.readHash();
    }
}
