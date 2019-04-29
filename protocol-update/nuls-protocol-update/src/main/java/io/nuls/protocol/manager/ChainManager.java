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
package io.nuls.protocol.manager;

import io.nuls.db.service.RocksDBService;
import io.nuls.protocol.constant.Constant;
import io.nuls.protocol.service.ProtocolService;
import io.nuls.protocol.utils.ConfigLoader;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.log.logback.NulsLogger;

import java.util.List;

/**
 * 链管理类,负责各条链的初始化,运行,启动,参数维护等
 * Chain management class, responsible for the initialization, operation, start-up, parameter maintenance of each chain, etc.
 *
 * @author: PierreLuo
 * @date: 2019-02-26
 */
@Component
public class ChainManager {

    @Autowired
    private ProtocolService protocolService;

    /**
     * 初始化并启动链
     * Initialize and start the chain
     */
    public void runChain() throws Exception {
        //加载配置
        ConfigLoader.load();
        List<Integer> chainIds = ContextManager.chainIds;
        for (Integer chainId : chainIds) {
            initTable(chainId);
            //服务初始化
            protocolService.init(chainId);
        }
    }

    /**
     * 停止一条链
     * Delete a chain
     *
     * @param chainId 链ID/chain id
     */
    public void stopChain(int chainId) {

    }

    /**
     * 初始化链相关表
     * Initialization chain correlation table
     *
     * @param chainId
     */
    private void initTable(int chainId) {
        NulsLogger commonLog = ContextManager.getContext(chainId).getCommonLog();
        try {
            RocksDBService.createTable(Constant.STATISTICS + chainId);
        } catch (Exception e) {
            commonLog.error(e);
        }
    }

}
