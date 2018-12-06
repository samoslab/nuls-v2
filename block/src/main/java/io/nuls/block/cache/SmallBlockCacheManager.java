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

package io.nuls.block.cache;

import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.SmallBlock;
import io.nuls.tools.cache.LimitHashMap;

/**
 * 缓存收到的SmallBlock
 *
 * @author captain
 * @version 1.0
 * @date 18-12-6 上午10:49
 */
public class SmallBlockCacheManager {

    private static final SmallBlockCacheManager INSTANCE = new SmallBlockCacheManager();

    private LimitHashMap<NulsDigestData, SmallBlock> smallBlockCacheMap = new LimitHashMap<>(100);

    private SmallBlockCacheManager() {

    }

    public static SmallBlockCacheManager getInstance() {
        return INSTANCE;
    }

    /**
     * 将一个SmallBlock放入内存中，若不主动删除，则在缓存存满或者存在时间超过1000秒时，自动清理
     * Store a SmallBlock in memory, cache it full or exist for over 1000 seconds, and clean it automatically.
     *
     * @param smallBlock 要放入内存中的对象
     */
    public void cacheSmallBlock(SmallBlock smallBlock) {
        smallBlockCacheMap.put(smallBlock.getHeader().getHash(), smallBlock);
    }

    public void cacheSmallBlockRequest(NulsDigestData blockHash) {
        smallBlockCacheMap.put(blockHash, null);
    }

    /**
     * 根据区块hash获取完整的SmallBlock
     * get SmallBlock by block header digest data
     *
     * @param blockHash
     * @return
     */
    public SmallBlock getSmallBlockByHash(NulsDigestData blockHash) {
        return smallBlockCacheMap.get(blockHash);
    }

    public boolean containsSmallBlock(NulsDigestData blockHash) {
        return smallBlockCacheMap.get(blockHash) != null;
    }

}
