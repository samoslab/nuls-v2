package io.nuls.transaction.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.NulsDigestData;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.SerializeUtils;
import io.nuls.transaction.message.base.BaseMessage;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

/**
 * 向链内其他节点广播本节点对于某个跨链交易的验证结果
 *
 * @author: qinyifeng
 * @date: 2018/12/18
 */
public class BroadcastCrossNodeRsMessage extends BaseMessage {

    /**
     * 转换NULS主网协议后交易hash
     */
    @Getter
    @Setter
    private NulsDigestData requestHash;

    /**
     * 验证的节点对交易的签名
     */
    @Getter
    @Setter
    private P2PHKSignature signature;

    /**
     * 节点地址
     */
    @Getter
    @Setter
    private String packingAddress;

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(requestHash);
        size += SerializeUtils.sizeOfNulsData(signature);
        size += SerializeUtils.sizeOfString(packingAddress);
        return size;
    }

    @Override
    public void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(requestHash);
        stream.writeNulsData(signature);
        stream.writeString(packingAddress);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.requestHash = byteBuffer.readHash();
        this.signature = byteBuffer.readNulsData(new P2PHKSignature());
        this.packingAddress = byteBuffer.readString();
    }
}
