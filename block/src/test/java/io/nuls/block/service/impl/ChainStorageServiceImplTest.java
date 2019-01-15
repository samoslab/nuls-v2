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

package io.nuls.block.service.impl;

import io.nuls.base.data.Block;
import io.nuls.base.data.NulsDigestData;
import io.nuls.block.service.BlockStorageService;
import io.nuls.block.service.ChainStorageService;
import io.nuls.block.test.BlockGenerator;
import io.nuls.block.utils.BlockUtil;
import io.nuls.db.manager.RocksDBManager;
import io.nuls.db.service.RocksDBService;
import io.nuls.tools.core.ioc.SpringLiteContext;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.nuls.block.constant.Constant.DATA_PATH;
import static org.junit.Assert.*;

public class ChainStorageServiceImplTest {

    private static ChainStorageService service;
    private static Block block;

    @BeforeClass
    public static void beforeClass() throws Exception {
        SpringLiteContext.init("io.nuls.block");
        RocksDBService.init(DATA_PATH);
        service = SpringLiteContext.getBean(ChainStorageService.class);
        block = BlockGenerator.generate(null);
    }

    @Test
    public void save() {
        NulsDigestData hash = block.getHeader().getHash();
        service.save(1, block);
        Block block1 = service.query(1, hash);
        NulsDigestData hash1 = block1.getHeader().getHash();
        System.out.println(hash);
        assertEquals(hash, hash1);
    }

    @Test
    public void remove() {
        NulsDigestData hash = block.getHeader().getHash();
        service.save(1, block);
        service.remove(1, hash);
        Block block1 = service.query(1, hash);
        assertEquals(null, block1);
    }
}