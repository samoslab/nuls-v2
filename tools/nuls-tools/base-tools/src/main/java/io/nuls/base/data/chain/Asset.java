package io.nuls.base.data.chain;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.SerializeUtils;

import java.io.IOException;

/**
 * @author tangyi
 * @date 2018/11/6
 * @description
 */
public class Asset extends BaseNulsData {
    private short chainId;
    private short assetId;
    private String symbol;
    private String name;
    private int depositNuls;
    private long initCirculation;
    private short decimalPlaces;
    private boolean available;

    public short getChainId() {
        return chainId;
    }

    public void setChainId(short chainId) {
        this.chainId = chainId;
    }

    public short getAssetId() {
        return assetId;
    }

    public void setAssetId(short assetId) {
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

    public long getInitCirculation() {
        return initCirculation;
    }

    public void setInitCirculation(long initCirculation) {
        this.initCirculation = initCirculation;
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

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeShort(chainId);
        stream.writeShort(assetId);
        stream.writeString(symbol);
        stream.writeString(name);
        stream.writeUint32(depositNuls);
        stream.writeInt64(initCirculation);
        stream.writeShort(decimalPlaces);
        stream.writeBoolean(available);

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chainId = byteBuffer.readShort();
        this.assetId = byteBuffer.readShort();
        this.symbol = byteBuffer.readString();
        this.name = byteBuffer.readString();
        this.depositNuls = byteBuffer.readInt32();
        this.initCirculation = byteBuffer.readInt64();
        this.decimalPlaces = byteBuffer.readShort();
        this.available = byteBuffer.readBoolean();
    }

    @Override
    public int size() {
        int size = 0;
        // chainId
        size += SerializeUtils.sizeOfInt16();
        // assetId
        size += SerializeUtils.sizeOfInt16();
        size += SerializeUtils.sizeOfString(symbol);
        size += SerializeUtils.sizeOfString(name);
        // depositNuls
        size += SerializeUtils.sizeOfInt32();
        // initCirculation
        size += SerializeUtils.sizeOfInt64();
        // decimalPlaces
        size += SerializeUtils.sizeOfInt16();
        size += SerializeUtils.sizeOfBoolean(available);

        return size;
    }
}
