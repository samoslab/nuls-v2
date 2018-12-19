/*
 *
 *  * MIT License
 *  * Copyright (c) 2017-2018 nuls.io
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package io.nuls.block.thread;

import io.nuls.base.data.Block;
import io.nuls.base.data.NulsDigestData;
import io.nuls.block.cache.CacheHandler;
import io.nuls.block.constant.CommandConstant;
import io.nuls.block.constant.ConfigConstant;
import io.nuls.block.manager.ConfigManager;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.CompleteMessage;
import io.nuls.block.message.HeightRangeMessage;
import io.nuls.block.model.Node;
import io.nuls.block.service.BlockService;
import io.nuls.block.utils.BlockDownloadUtils;
import io.nuls.block.utils.BlockUtil;
import io.nuls.block.utils.module.NetworkUtil;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.data.DoubleUtils;
import io.nuls.tools.log.Log;
import lombok.AllArgsConstructor;

import java.util.concurrent.*;

/**
 * 区块下载管理器
 *
 * @author captain
 * @version 1.0
 * @date 18-11-9 下午4:25
 */
public class BlockDownloader implements Callable<Boolean> {

    /**
     * 区块下载参数
     */
    private BlockDownloaderParams params;
    private ThreadPoolExecutor executor;
    private BlockingQueue<Future<BlockDownLoadResult>> futures;
    private int chainId;
    private BlockService blockService;

    public BlockDownloader(int chainId, BlockingQueue<Future<BlockDownLoadResult>> futures, ThreadPoolExecutor executor, BlockDownloaderParams params) {
        this.params = params;
        this.executor = executor;
        this.futures = futures;
        this.chainId = chainId;
        this.blockService = SpringLiteContext.getBean(BlockService.class);
    }

    @Override
    public Boolean call() {
        if (!checkLocalBlock()) {
            return false;
        }

        PriorityBlockingQueue<Node> nodes = params.getNodes();
        long netLatestHeight = params.getNetLatestHeight();
        long startHeight = ContextManager.getContext(chainId).getLatestHeight() + 1;
        int maxDowncount = Integer.parseInt(ConfigManager.getValue(chainId, ConfigConstant.DOWNLOAD_NUMBER));
        try {
            long total = netLatestHeight - startHeight + 1;
            long start = System.currentTimeMillis();
            while (startHeight <= netLatestHeight) {
                Node node = nodes.take();
                int size = maxDowncount * 100 / node.getCredit();
                if (startHeight + size > netLatestHeight) {
                    size = (int) (netLatestHeight - startHeight + 1);
                }
                Worker worker = new Worker(startHeight, size, chainId, node);
                Future<BlockDownLoadResult> future = executor.submit(worker);
                futures.offer(future);
                startHeight += size;
            }
            long end = System.currentTimeMillis();
            Log.info("block syn complete, total download:{}, total time:{}, average time:{}", total, end - start, end - start / total);
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
        executor.shutdown();
        return true;
    }

    /**
     * 区块同步前，与网络区块作对比，检查本地区块是否需要回滚
     *
     * @return
     */
    private boolean checkLocalBlock() {
        Block localBlock = blockService.getLatestBlock(chainId);
        long localHeight = localBlock.getHeader().getHeight();
        long netHeight = params.getNetLatestHeight();
        //得到共同高度
        long commonHeight = Math.min(localHeight, netHeight);
        if (checkHashEquality(localBlock)) {
            if (commonHeight < netHeight) {
                //commonHeight区块的hash一致，正常，比远程节点落后，下载区块
                return true;
            }
        } else {
            //需要回滚的场景，要满足可用节点数(10个)>配置，一致可用节点数(6个)占比超80%两个条件
            if (params.getNodes().size() >= Integer.parseInt(ConfigManager.getValue(chainId, ConfigConstant.MIN_NODE_AMOUNT))
                    && DoubleUtils.div(params.getNodes().size(), params.getAvailableNodesCount(), 2) >= Double.parseDouble(ConfigManager.getValue(chainId, ConfigConstant.CONSISTENCY_NODE_PERCENT)) * 100
            ) {
                return checkRollback(localBlock, 0);
            }
        }
        return false;
    }

    private boolean checkRollback(Block localBestBlock, int rollbackCount) {
        //每次最多回滚10个区块，等待下次同步，这样可以避免被恶意节点攻击，大量回滚正常区块。
        if (rollbackCount >= Integer.parseInt(ConfigManager.getValue(chainId, ConfigConstant.MAX_ROLLBACK))) {
            return false;
        }

        blockService.rollbackBlock(chainId, BlockUtil.toBlockHeaderPo(localBestBlock));
        localBestBlock = blockService.getLatestBlock(chainId);
        if (checkHashEquality(localBestBlock)) {
            return true;
        }

        return checkRollback(localBestBlock, rollbackCount + 1);
    }

    /**
     * 根据传入的区块localBlock判断localBlock.hash与网络上同高度的区块hash是否一致
     *
     * @author captain
     * @date 18-11-9 下午6:13
     * @version 1.0
     */
    private boolean checkHashEquality(Block localBlock) {
        NulsDigestData localHash = localBlock.getHeader().getHash();
        long localHeight = localBlock.getHeader().getHeight();
        long netHeight = params.getNetLatestHeight();
        //得到共同高度
        long commonHeight = Math.min(localHeight, netHeight);
        NulsDigestData remoteHash = params.getNetLatestHash();
        //如果双方共同高度<网络高度，要进行hash判断，需要从网络上下载区块，因为params里只有最新的区块hash，没有旧的hash
        if (commonHeight < netHeight) {
            for (Node node : params.getNodes()) {
                Block remoteBlock = BlockDownloadUtils.getBlockByHash(chainId, localHash, node);
                remoteHash = remoteBlock.getHeader().getHash();
                break;
            }
        }
        return localHash.equals(remoteHash);
    }

    /**
     * 区块下载器
     *
     * @author captain
     * @version 1.0
     * @date 18-12-4 下午8:29
     */
    @AllArgsConstructor
    static class Worker implements Callable<BlockDownLoadResult> {

        private long startHeight;
        private int size;
        private int chainId;
        private Node node;

        @Override
        public BlockDownLoadResult call() {
            boolean b = false;
            long endHeight = startHeight + size - 1;
            Log.info("getBlocks:{}->{} ,from:{}, start", startHeight, endHeight, node.getId());
            //组装批量获取区块消息
            HeightRangeMessage message = new HeightRangeMessage(startHeight, endHeight);
            message.setCommand(CommandConstant.GET_BLOCKS_BY_HEIGHT_MESSAGE);
            //计算本次请求hash，用来跟踪本次异步请求
            NulsDigestData messageHash = message.getHash();
            try {
                Future<CompleteMessage> future = CacheHandler.addBatchBlockRequest(chainId, messageHash);

                //发送消息给目标节点
                boolean result = NetworkUtil.sendToNode(chainId, message, node.getId());

                //发送失败清空数据
                if (!result) {
                    CacheHandler.removeRequest(chainId, messageHash);
                    return new BlockDownLoadResult(messageHash, startHeight, size, node, false);
                }

                CompleteMessage completeMessage = future.get(30L, TimeUnit.SECONDS);
                b = completeMessage.isSuccess();
            } catch (Exception e) {
                Log.error(e);
            }
            return new BlockDownLoadResult(messageHash, startHeight, size, node, b);
        }
    }

}
