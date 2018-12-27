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

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.parse.SerializeUtils;
import io.nuls.tools.parse.config.ConfigItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChainParameters extends BaseNulsData {

    /**
     * 链名
     */
    private String chainName;
    /**
     * 链ID
     */
    private int chainId;
    /**
     * 区块大小阈值
     */
    private int blockMaxSize;
    /**
     * 网络重置阈值
     */
    private int resetTime;
    /**
     * 分叉链比主链高几个区块就进行链切换
     */
    private int chainSwtichThreshold;
    /**
     * 分叉链、孤儿链区块最大缓存数量
     */
    private int cacheSize;
    /**
     * 接收新区块的范围
     */
    private int heightRange;
    /**
     * 每次回滚区块最大值
     */
    private int maxRollback;
    /**
     * 一致节点比例
     */
    private int consistencyNodePercent;
    /**
     * 系统运行最小节点数
     */
    private int minNodeAmount;
    /**
     * 每次从一个节点下载多少区块
     */
    private int downloadNumber;
    /**
     * 区块头中扩展字段的最大长度
     */
    private int extendMaxSize;
    /**
     * 为阻止恶意节点提前出块,设置此参数
     * 区块时间戳大于当前时间多少就丢弃该区块
     */
    private int validBlockInterval;
    /**
     * 同步区块时最多缓存多少个区块
     */
    private int blockCache;
    /**
     * 系统正常运行时最多缓存多少个从别的节点接收到的小区块
     */
    private int smallBlockCache;
    /**
     * 孤儿链最大年龄
     */
    private int orphanChainMaxAge;

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeString(chainName);
        stream.writeUint16(chainId);
        stream.writeUint16(blockMaxSize);
        stream.writeUint16(resetTime);
        stream.writeUint16(chainSwtichThreshold);
        stream.writeUint16(cacheSize);
        stream.writeUint16(heightRange);
        stream.writeUint16(maxRollback);
        stream.writeUint16(consistencyNodePercent);
        stream.writeUint16(minNodeAmount);
        stream.writeUint16(downloadNumber);
        stream.writeUint16(extendMaxSize);
        stream.writeUint16(validBlockInterval);
        stream.writeUint16(blockCache);
        stream.writeUint16(smallBlockCache);
        stream.writeUint16(orphanChainMaxAge);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.chainName = byteBuffer.readString();
        this.chainId = byteBuffer.readUint16();
        this.blockMaxSize = byteBuffer.readUint16();
        this.resetTime = byteBuffer.readUint16();
        this.chainSwtichThreshold = byteBuffer.readUint16();
        this.cacheSize = byteBuffer.readUint16();
        this.heightRange = byteBuffer.readUint16();
        this.maxRollback = byteBuffer.readUint16();
        this.consistencyNodePercent = byteBuffer.readUint16();
        this.minNodeAmount = byteBuffer.readUint16();
        this.downloadNumber = byteBuffer.readUint16();
        this.extendMaxSize = byteBuffer.readUint16();
        this.validBlockInterval = byteBuffer.readUint16();
        this.blockCache = byteBuffer.readUint16();
        this.smallBlockCache = byteBuffer.readUint16();
    }

    @Override
    public int size() {
        int size = 0;
        size += (15 * SerializeUtils.sizeOfUint16());
        size += SerializeUtils.sizeOfString(chainName);
        return size;
    }

    public void init(List<ConfigItem> list) {
        for (ConfigItem configItem : list) {
            //todo 待完善
        }
    }
}
