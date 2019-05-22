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
package io.nuls.base.data;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.crypto.UnsafeByteArrayOutputStream;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.parse.HashUtil;
import io.nuls.core.parse.SerializeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;

/**
 * @author vivi
 */
public class BlockHeader extends BaseNulsData {

    /**
     * 区块头排序器
     */
    public static final Comparator<BlockHeader> BLOCK_HEADER_COMPARATOR = Comparator.comparingLong(BlockHeader::getHeight);

    private transient byte[] hash;
    private byte[] preHash;
    private byte[] merkleHash;
    private long time;
    private long height;
    private int txCount;
    private BlockSignature blockSignature;
    private byte[] extend;
    /**
     * pierre add 智能合约世界状态根
     */
    private transient byte[] stateRoot;

    private transient int size;
    private transient byte[] packingAddress;

    public BlockHeader() {
    }

    private synchronized void calcHash() {
        if (null != this.hash) {
            return;
        }
        try {
            hash = HashUtil.calcHash(serializeWithoutSign());
        } catch (Exception e) {
            throw new NulsRuntimeException(e);
        }
    }

    @Override
    public int size() {
        int size = 0;
        size += HashUtil.HASH_LENGTH;               //preHash
        size += HashUtil.HASH_LENGTH;               //merkleHash
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfBytes(extend);
        size += SerializeUtils.sizeOfNulsData(blockSignature);
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(preHash);
        stream.write(merkleHash);
        stream.writeUint32(time);
        stream.writeUint32(height);
        stream.writeUint32(txCount);
        stream.writeBytesWithLength(extend);
        stream.writeNulsData(blockSignature);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.preHash = byteBuffer.readBytes(HashUtil.HASH_LENGTH);
        this.merkleHash = byteBuffer.readBytes(HashUtil.HASH_LENGTH);
        this.time = byteBuffer.readUint32();
        this.height = byteBuffer.readUint32();
        this.txCount = byteBuffer.readInt32();
        this.extend = byteBuffer.readByLengthByte();
        this.blockSignature = byteBuffer.readNulsData(new BlockSignature());
    }

    public byte[] serializeWithoutSign() {
        ByteArrayOutputStream bos = null;
        try {
            int size = size() - SerializeUtils.sizeOfNulsData(blockSignature);
            bos = new UnsafeByteArrayOutputStream(size);
            NulsOutputStreamBuffer buffer = new NulsOutputStreamBuffer(bos);
            buffer.write(preHash);
            buffer.write(merkleHash);
            buffer.writeUint32(time);
            buffer.writeUint32(height);
            buffer.writeUint32(txCount);
            buffer.writeBytesWithLength(extend);
            byte[] bytes = bos.toByteArray();
            if (bytes.length != size) {
                throw new RuntimeException();
            }
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            }
        }
    }

    public byte[] getHash() {
        if (null == hash) {
            calcHash();
        }
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public byte[] getPreHash() {
        return preHash;
    }

    public void setPreHash(byte[] preHash) {
        this.preHash = preHash;
    }

    public byte[] getMerkleHash() {
        return merkleHash;
    }

    public void setMerkleHash(byte[] merkleHash) {
        this.merkleHash = merkleHash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public int getTxCount() {
        return txCount;
    }

    public void setTxCount(int txCount) {
        this.txCount = txCount;
    }

    public BlockSignature getBlockSignature() {
        return blockSignature;
    }

    public void setBlockSignature(BlockSignature scriptSign) {
        this.blockSignature = scriptSign;
    }

    public byte[] getPackingAddress(int chainID) {
        if (this.blockSignature != null && this.packingAddress == null) {
            this.packingAddress = AddressTool.getAddress(blockSignature.getPublicKey(), chainID);
        }
        return packingAddress;
    }


    public byte[] getExtend() {
        return extend;
    }

    public void setExtend(byte[] extend) {
        this.extend = extend;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setPackingAddress(byte[] packingAddress) {
        this.packingAddress = packingAddress;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    @Override
    public String toString() {
        return "BlockHeader{" +
                "hash=" + HashUtil.toHex(hash) +
                ", preHash=" + HashUtil.toHex(preHash) +
                ", merkleHash=" + HashUtil.toHex(merkleHash) +
                ", time=" + time +
                ", height=" + height +
                ", txCount=" + txCount +
                ", blockSignature=" + blockSignature +
                //", extend=" + Arrays.toString(extend) +
                ", size=" + size +
                ", packingAddress=" + (packingAddress == null ? packingAddress : AddressTool.getStringAddressByBytes(packingAddress)) +
                '}';
    }
}
