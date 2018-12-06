/*
 * MIT License
 * Copyright (c) 2017-2018 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.rpc;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.block.cache.SmallBlockCacheManager;
import io.nuls.block.constant.BlockErrorCode;
import io.nuls.block.message.TxGroupMessage;
import io.nuls.block.service.BlockService;
import io.nuls.block.utils.BlockUtil;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.block.constant.CommandConstant.TXGROUP_MESSAGE;

/**
 * 处理收到的{@link TxGroupMessage}
 *
 * @author captain
 * @version 1.0
 * @date 18-11-14 下午4:23
 */
@Component
public class TxGroupHandler extends BaseCmd {

    @Autowired
    private BlockService blockService;
    private SmallBlockCacheManager smallBlockCacheManager = SmallBlockCacheManager.getInstance();

    @CmdAnnotation(cmd = TXGROUP_MESSAGE, version = 1.0, scope = Constants.PUBLIC, description = "")
    public Response process(List<Object> params) {
        Integer chainId = Integer.parseInt(params.get(0).toString());
        String nodeId = params.get(1).toString();
        TxGroupMessage message = new TxGroupMessage();

        byte[] decode = HexUtil.decode(params.get(2).toString());
        try {
            message.parse(new NulsByteBuffer(decode));
        } catch (NulsException e) {
            Log.warn(e.getMessage());
            return failed(BlockErrorCode.PARAMETER_ERROR);
        }

        if (message == null || nodeId == null) {
            return failed(BlockErrorCode.PARAMETER_ERROR);
        }

        List<Transaction> transactions = message.getTransactions();
        if (null == transactions || transactions.size() == 0) {
            Log.warn("recieved a null txGroup form " + nodeId);
            return failed(BlockErrorCode.PARAMETER_ERROR);
        }

        SmallBlock smallBlock = smallBlockCacheManager.getSmallBlockByHash(message.getBlockHash());
        if (null == smallBlock) {
            return failed(BlockErrorCode.PARAMETER_ERROR);
        }

        BlockHeader header = smallBlock.getHeader();
        NulsDigestData headerHash = header.getHash();
        Map<NulsDigestData, Transaction> txMap = new HashMap<>((int) header.getTxCount());
        for (Transaction tx : smallBlock.getSubTxList()) {
            txMap.put(tx.getHash(), tx);
        }
        for (Transaction tx : transactions) {
            txMap.put(tx.getHash(), tx);
        }

        Block block = BlockUtil.assemblyBlock(header, txMap, smallBlock.getTxHashList());
        if (blockService.saveBlock(chainId, block)) {
            smallBlockCacheManager.cacheSmallBlock(smallBlock);
            blockService.forwardBlock(chainId, headerHash, nodeId);
        } else {
            Log.error("save fail! chainId:{}, height:{}, hash:{}", chainId, header.getHeight(), headerHash);
        }
        return success();
    }

}
