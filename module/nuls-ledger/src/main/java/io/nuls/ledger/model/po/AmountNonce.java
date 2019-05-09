/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.ledger.constant.LedgerConstant;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author lanjinsheng
 * @date 2018/11/19
 */

public class AmountNonce extends BaseNulsData {
    private byte[] fromNonce = LedgerConstant.INIT_NONCE_BYTE;
    private byte[] nonce = LedgerConstant.INIT_NONCE_BYTE;
    private BigInteger amount = BigInteger.ZERO;


    public AmountNonce() {
        super();
    }

    public AmountNonce(byte[] fromNonce,byte[] nonce, BigInteger amount) {
        this.fromNonce = fromNonce;
        this.nonce = nonce;
        this.amount = amount;
    }


    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(fromNonce);
        stream.write(nonce);
        stream.writeBigInteger(amount);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.fromNonce = byteBuffer.readBytes(8);
        this.nonce = byteBuffer.readBytes(8);
        this.amount = byteBuffer.readBigInteger();
    }

    @Override
    public int size() {
        int size = 0;
        size += fromNonce.length;
        size += nonce.length;
        size += SerializeUtils.sizeOfBigInteger();
        return size;
    }
    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public byte[] getFromNonce() {
        return fromNonce;
    }

    public void setFromNonce(byte[] fromNonce) {
        this.fromNonce = fromNonce;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }
}
