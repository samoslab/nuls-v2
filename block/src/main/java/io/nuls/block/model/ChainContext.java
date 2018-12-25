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

package io.nuls.block.model;

import ch.qos.logback.classic.Logger;
import io.nuls.base.data.Block;
import io.nuls.block.constant.RunningStatusEnum;
import io.nuls.block.model.Chain;
import io.nuls.tools.log.logback.LoggerBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 每个链ID对应一个{@link ChainContext},维护一些链运行期间的信息,并负责链的初始化、启动、停止、销毁操作
 *
 * @author captain
 * @version 1.0
 * @date 18-11-20 上午10:46
 */
@Data
@NoArgsConstructor
public class ChainContext {
    /**
     * 代表该链的运行状态
     */
    private RunningStatusEnum status;

    /**
     * 链ID
     */
    private int chainId;

    /**
     * 该链的系统交易类型
     */
    private List<Integer> systemTransactionType;

    /**
     * 最新区快
     */
    private Block latestBlock;

    /**
     * 创世区块
     */
    private Block genesisBlock;

    /**
     * 主链
     */
    private Chain masterChain;

    /**
     * 分叉链集合
     */
    private List<Chain> forkChains;

    public void setGenesisBlock(Block block) {
        this.genesisBlock = block;
    }

    public synchronized void setStatus(RunningStatusEnum status) {
        this.status = status;
    }

    public long getLatestHeight() {
        return latestBlock.getHeader().getHeight();
    }

    public void init() {

    }

    public void start() {

    }

    public void stop() {

    }

    public void destroy() {

    }
}