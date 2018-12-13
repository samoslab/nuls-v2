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

package io.nuls.block.thread;

import io.nuls.base.data.NulsDigestData;
import io.nuls.block.config.ConfigLoader;
import io.nuls.block.model.Node;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static io.nuls.block.constant.Constant.CHAIN_ID;
import static io.nuls.block.constant.Constant.MODULES_CONFIG_FILE;

public class BlockSynchronizerTest {

    @Before
    public void setUp() throws Exception {
        ConfigLoader.load(MODULES_CONFIG_FILE);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void statistics() {
        //测试一致节点比例，一致节点超过80%
        List<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            Node node = new Node();
            node.setId("group1-" + i);
            node.setHeight(888L);
            node.setHash(NulsDigestData.calcDigestData("888".getBytes()));
            nodeList.add(node);
        }
        for (int i = 0; i < 20; i++) {
            Node node = new Node();
            node.setId("group2-" + i);
            node.setHeight(888L);
            node.setHash(NulsDigestData.calcDigestData("sss".getBytes()));
            nodeList.add(node);
        }
        BlockDownloaderParams params = BlockSynchronizer.getInstance().statistics(nodeList, CHAIN_ID);
        Assert.assertTrue(params.getNodes().size() == 80);

        //测试一致节点比例，一致节点低于80%
        nodeList.clear();
        for (int i = 0; i < 79; i++) {
            Node node = new Node();
            node.setId("group1-" + i);
            node.setHeight(888L);
            node.setHash(NulsDigestData.calcDigestData("888".getBytes()));
            nodeList.add(node);
        }
        for (int i = 0; i < 21; i++) {
            Node node = new Node();
            node.setId("group2-" + i);
            node.setHeight(888L);
            node.setHash(NulsDigestData.calcDigestData("sss".getBytes()));
            nodeList.add(node);
        }
        params = BlockSynchronizer.getInstance().statistics(nodeList, CHAIN_ID);
        Assert.assertTrue(params.getNodes().size() == 0);

        //测试一致节点hash与高度是否正确
        nodeList.clear();
        for (int i = 0; i < 88; i++) {
            Node node = new Node();
            node.setId("group1-" + i);
            node.setHeight(888L);
            node.setHash(NulsDigestData.calcDigestData("888".getBytes()));
            nodeList.add(node);
        }
        for (int i = 0; i < 6; i++) {
            Node node = new Node();
            node.setId("group2-" + i);
            node.setHeight(888L);
            node.setHash(NulsDigestData.calcDigestData("sss".getBytes()));
            nodeList.add(node);
        }
        for (int i = 0; i < 6; i++) {
            Node node = new Node();
            node.setId("group2-" + i);
            node.setHeight(666L);
            node.setHash(NulsDigestData.calcDigestData("666".getBytes()));
            nodeList.add(node);
        }
        params = BlockSynchronizer.getInstance().statistics(nodeList, CHAIN_ID);
        Assert.assertTrue(params.getNodes().get(0).getHeight() == 888 && params.getNodes().get(0).getHash().equals(NulsDigestData.calcDigestData("888".getBytes())));
    }

}