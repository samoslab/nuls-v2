/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
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

package io.nuls.block.thread;

import io.nuls.base.data.Block;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.service.BlockService;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.log.logback.NulsLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 * 消费共享队列中的区块
 *
 * @author captain
 * @version 1.0
 * @date 18-11-8 下午5:45
 */
public class BlockConsumer implements Callable<Boolean> {

    /**
     * 区块下载参数
     */
    private BlockDownloaderParams params;
    private int chainId;
    private BlockingQueue<Block> queue;
    private BlockService blockService;
    /**
     * 是否继续本次下载，中途发生异常置为false
     */
    private boolean flag;

    public BlockConsumer(int chainId, BlockingQueue<Block> queue, BlockDownloaderParams params, boolean flag) {
        this.params = params;
        this.chainId = chainId;
        this.queue = queue;
        this.flag = flag;
        this.blockService = SpringLiteContext.getBean(BlockService.class);
    }

    @Override
    public Boolean call() {
        long netLatestHeight = params.getNetLatestHeight();
        long startHeight = params.getLocalLatestHeight() + 1;
        NulsLogger commonLog = ContextManager.getContext(chainId).getCommonLog();
        Block block;
        commonLog.info("BlockConsumer start work");
        try {
            while (startHeight <= netLatestHeight && flag) {
                block = queue.take();
                boolean saveBlock = blockService.saveBlock(chainId, block, true);
                if (!saveBlock) {
                    commonLog.error("error occur when save syn blocks");
                    flag = false;
                    return false;
                }
                startHeight++;
            }
            commonLog.info("BlockConsumer stop work normally");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            commonLog.error("BlockConsumer stop work abnormally");
            flag = false;
            return false;
        }
    }

}
