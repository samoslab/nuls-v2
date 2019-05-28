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
package io.nuls.chain.util;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.chain.model.po.Asset;
import io.nuls.chain.model.po.BlockChain;
import io.nuls.chain.model.tx.txdata.TxChain;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.util.NulsDateUtils;

/**
 * @author lan
 * @description
 * @date 2019/02/20
 **/
public class TxUtil {
    public static Asset buildAssetWithTxChain(Transaction tx) {
        try {
            TxChain txChain = new TxChain();
            txChain.parse(tx.getTxData(), 0);
            Asset asset = new Asset(txChain);
            asset.setTxHash(tx.getHash().toHex());
            return asset;
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    public static BlockChain buildChainWithTxData(Transaction tx, boolean isDelete) {
        try {
            TxChain txChain = new TxChain();
            txChain.parse(tx.getTxData(), 0);
            BlockChain blockChain = new BlockChain(txChain);
            if (isDelete) {
                blockChain.setDelTxHash(tx.getHash().toHex());
                blockChain.setDelAddress(txChain.getDefaultAsset().getAddress());
                blockChain.setDelAssetId(txChain.getDefaultAsset().getAssetId());
            } else {
                blockChain.setRegTxHash(tx.getHash().toHex());
                blockChain.setRegAddress(txChain.getDefaultAsset().getAddress());
                blockChain.setRegAssetId(txChain.getDefaultAsset().getAssetId());
            }
            blockChain.setCreateTime(NulsDateUtils.getCurrentTimeSeconds());
            return blockChain;
        } catch (Exception e) {
            LoggerUtil.logger().error("buildChainWithTxData error:{}", e);
            return null;
        }
    }

    public static byte[] getNonceByTxHash(String txHash) {
        byte[] out = new byte[8];
        byte[] in = HexUtil.decode(txHash);
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        return out;
    }

    public static Transaction buildTxData(String txHex) {
        try {
            if (StringUtils.isBlank(txHex)) {
                return null;
            }
            byte[] txStream = RPCUtil.decode(txHex);
            Transaction tx = new Transaction();
            tx.parse(new NulsByteBuffer(txStream));
            return tx;
        } catch (Exception e) {
            LoggerUtil.logger().error("transaction parse error:{}", e);
            return null;
        }
    }
}
