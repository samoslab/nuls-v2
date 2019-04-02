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
package io.nuls.ledger.rpc.cmd;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.model.StringUtils;

import java.util.List;

import static io.nuls.ledger.utils.LoggerUtil.logger;

/**
 * @author lan
 * @description
 * @date 2019/03/13
 **/
public class BaseLedgerCmd extends BaseCmd {
    Response parseTxs(List<String> txHexList, List<Transaction> txList,int chainId) {
        for (String txHex : txHexList) {
            if (StringUtils.isBlank(txHex)) {
                return failed("txHex is blank");
            }
            byte[] txStream = HexUtil.decode(txHex);
            Transaction tx = new Transaction();
            try {
                tx.parse(new NulsByteBuffer(txStream));
                txList.add(tx);
            } catch (NulsException e) {
                logger(chainId).error("transaction parse error", e);
                return failed("transaction parse error");
            }
        }
        return success();
    }

    Transaction parseTxs(String txHex,int chainId) {
        if (StringUtils.isBlank(txHex)) {
            return null;
        }
        byte[] txStream = HexUtil.decode(txHex);
        Transaction tx = new Transaction();
        try {
            tx.parse(new NulsByteBuffer(txStream));
        } catch (NulsException e) {
            logger(chainId).error("transaction parse error", e);
            return null;
        }
        return tx;
    }

}
