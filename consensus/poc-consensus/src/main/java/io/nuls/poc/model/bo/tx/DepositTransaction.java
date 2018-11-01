/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2018 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */
package io.nuls.poc.model.bo.tx;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.poc.model.bo.tx.txdata.Deposit;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.exception.NulsRuntimeException;

/**
 * @author Niels
 */
public class DepositTransaction extends Transaction<Deposit> {

    public DepositTransaction() {
        super(3);
    }

    @Override
    protected Deposit parseTxData(NulsByteBuffer byteBuffer) throws NulsException {
        return byteBuffer.readNulsData(new Deposit());
    }

    @Override
    public String getInfo(byte[] address) {
        return "lock "+ getTxData().getDeposit().toText();
    }

    @Override
    public DepositTransaction clone() {
        DepositTransaction tx = new DepositTransaction();
        try {
            tx.parse(serialize(), 0);
        } catch (Exception e) {
            throw new NulsRuntimeException(e);
        }
        tx.setBlockHeight(blockHeight);
        tx.setStatus(status);
        tx.setHash(hash);
        tx.setSize(size);

        Deposit deposit = tx.getTxData();
        deposit.setBlockHeight(txData.getBlockHeight());
        deposit.setDelHeight(txData.getDelHeight());
        deposit.setTime(txData.getTime());
        deposit.setTxHash(txData.getTxHash());
        deposit.setStatus(txData.getStatus());

        return tx;
    }
}
