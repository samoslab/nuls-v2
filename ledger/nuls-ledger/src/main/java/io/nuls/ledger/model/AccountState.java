package io.nuls.ledger.model;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.tools.data.LongUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.SerializeUtils;
import lombok.*;

import java.io.IOException;

/**
 * Created by wangkun23 on 2018/11/19.
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccountState extends BaseNulsData {

    @Setter
    @Getter
    private short chaiId;

    @Setter
    @Getter
    private long nonce;

    @Setter
    @Getter
    private long balance;

    /**
     * 账户总金额
     */
    private long totalAmount;

    /**
     * 账户冻结的资产
     */
    @Setter
    @Getter
    private FreezeState freezeState;


    public AccountState(short chaiId, long nonce, long balance) {
        this.chaiId = chaiId;
        this.nonce = nonce;
        this.balance = balance;
        this.freezeState = new FreezeState();
    }

    /**
     * 获取账户总金额
     *
     * @return
     */
    public long getTotalAmount() {
        return LongUtils.add(balance, freezeState.getTotal());
    }

    public AccountState withNonce(long nonce) {
        return new AccountState(chaiId, nonce, balance);
    }

    public AccountState withIncrementedNonce() {
        return new AccountState(chaiId, nonce + 1, balance);
    }

    public AccountState withBalanceIncrement(long value) {
        return new AccountState(chaiId, nonce, balance + value);
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeShort(chaiId);
        stream.writeUint32(nonce);
        stream.writeUint32(balance);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chaiId = byteBuffer.readShort();
        this.nonce = byteBuffer.readUint32();
        this.balance = byteBuffer.readUint32();
    }

    @Override
    public int size() {
        int size = 0;
        //chainId
        size += 2;
        size += SerializeUtils.sizeOfInt32();
        size += SerializeUtils.sizeOfInt32();
        return size;
    }
}
