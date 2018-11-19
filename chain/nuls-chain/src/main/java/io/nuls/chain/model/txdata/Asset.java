package io.nuls.chain.model.txdata;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.basic.TransactionLogicData;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.SerializeUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tangyi
 * @date 2018/11/6
 * @description
 */
public class Asset extends TransactionLogicData {
    private short chainId;
    private long assetId;
    private String symbol;
    private String name;
    private int depositNuls;
    private long initNumber;
    private short decimalPlaces;
    private boolean available;
    private long createTime;
    private long lastUpdateTime;
    private byte[] address;

    public short getChainId() {
        return chainId;
    }

    public void setChainId(short chainId) {
        this.chainId = chainId;
    }

    public long getAssetId() {
        return assetId;
    }

    public void setAssetId(long assetId) {
        this.assetId = assetId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDepositNuls() {
        return depositNuls;
    }

    public void setDepositNuls(int depositNuls) {
        this.depositNuls = depositNuls;
    }

    public long getInitNumber() {
        return initNumber;
    }

    public void setInitNumber(long initNumber) {
        this.initNumber = initNumber;
    }

    public short getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(short decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeShort(chainId);
        stream.writeUint48(assetId);
        stream.writeString(symbol);
        stream.writeString(name);
        stream.writeUint32(depositNuls);
        stream.writeInt64(initNumber);
        stream.writeShort(decimalPlaces);
        stream.writeBoolean(available);
        stream.writeUint48(createTime);
        stream.writeUint48(lastUpdateTime);
        stream.writeBytesWithLength(address);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chainId = byteBuffer.readShort();
        this.assetId = byteBuffer.readUint48();
        this.symbol = byteBuffer.readString();
        this.name = byteBuffer.readString();
        this.depositNuls = byteBuffer.readInt32();
        this.initNumber = byteBuffer.readInt64();
        this.decimalPlaces = byteBuffer.readShort();
        this.available = byteBuffer.readBoolean();
        this.createTime = byteBuffer.readUint48();
        this.lastUpdateTime = byteBuffer.readUint48();
        this.address=byteBuffer.readByLengthByte();
    }

    @Override
    public int size() {
        int size = 0;
        // chainId
        size += SerializeUtils.sizeOfInt16();
        // assetId
        size += SerializeUtils.sizeOfUint48();
        size += SerializeUtils.sizeOfString(symbol);
        size += SerializeUtils.sizeOfString(name);
        // depositNuls
        size += SerializeUtils.sizeOfInt32();
        // initNumber
        size += SerializeUtils.sizeOfInt64();
        // decimalPlaces
        size += SerializeUtils.sizeOfInt16();
        size += SerializeUtils.sizeOfBoolean();
        // createTime
        size += SerializeUtils.sizeOfUint48();
        // lastUpdateTime
        size += SerializeUtils.sizeOfUint48();
        size+=SerializeUtils.sizeOfBytes(address);

        return size;
    }

    @Override
    public Set<byte[]> getAddresses() {
        Set<byte[]> addressSet = new HashSet<>();
        addressSet.add(this.address);
        return addressSet;
    }
}
