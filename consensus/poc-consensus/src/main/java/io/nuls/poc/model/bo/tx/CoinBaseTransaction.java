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
package io.nuls.poc.model.bo.tx;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.TransactionLogicData;
import io.nuls.base.data.Coin;
import io.nuls.base.data.Na;
import io.nuls.poc.constant.ConsensusConstant;
import io.nuls.tools.exception.NulsException;
import io.nuls.base.data.Transaction;
import java.util.Arrays;

/**
 * @author tag
 * 2018/11/19
 */
public class CoinBaseTransaction extends Transaction {

    public CoinBaseTransaction() {
        this(ConsensusConstant.TX_TYPE_COINBASE);
    }

    @Override
    protected TransactionLogicData parseTxData(NulsByteBuffer byteBuffer) throws NulsException {
        byteBuffer.readBytes(ConsensusConstant.PLACE_HOLDER.length);
        return null;
    }

    protected CoinBaseTransaction(int type) {
        super(type);
    }

    @Override
    public String getInfo(byte[] address) {
        Na to = Na.ZERO;
        for (Coin coin : coinData.getTo()) {
            if (Arrays.equals(address, coin.getAddress()))
            {
                to = to.add(coin.getNa());
            }
        }
        return "+" + to.toText();
    }

    @Override
    public boolean isSystemTx() {
        return true;
    }

    @Override
    public boolean needVerifySignature() {
        return false;
    }
}
