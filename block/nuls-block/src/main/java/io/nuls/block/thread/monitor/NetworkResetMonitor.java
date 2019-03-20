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

package io.nuls.block.thread.monitor;

import io.nuls.block.constant.RunningStatusEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.ChainContext;
import io.nuls.block.model.ChainParameters;
import io.nuls.block.rpc.call.NetworkUtil;
import io.nuls.tools.log.logback.NulsLogger;

/**
 * 区块高度监控器
 * 每隔固定时间间隔启动
 * 如果发现区块高度没更新,通知网络模块重置可用节点,并重启区块同步线程
 *
 * @author captain
 * @version 1.0
 * @date 18-11-14 下午3:53
 */
public class NetworkResetMonitor implements Runnable {

    private static final NetworkResetMonitor INSTANCE = new NetworkResetMonitor();

    private NetworkResetMonitor() {
    }

    public static NetworkResetMonitor getInstance() {
        return INSTANCE;
    }

    @Override
    public void run() {
        for (Integer chainId : ContextManager.chainIds) {
            ChainContext context = ContextManager.getContext(chainId);
            NulsLogger commonLog = context.getCommonLog();
            try {
                //判断该链的运行状态,只有正常运行时才会有区块高度监控
                RunningStatusEnum status = context.getStatus();
                if (!status.equals(RunningStatusEnum.RUNNING)) {
                    commonLog.info("skip process, status is " + status + ", chainId-" + chainId);
                    return;
                }
                ChainParameters parameters = context.getParameters();
                int reset = parameters.getResetTime();
                long time = context.getLatestBlock().getHeader().getTime();
                //如果(当前时间戳-最新区块时间戳)>重置网络阈值,通知网络模块重置可用节点
                long currentTime = NetworkUtil.currentTime();
                commonLog.debug("chainId-" + chainId + ",currentTime-" + currentTime + ",blockTime-" + time + ",diffrence-" + (currentTime - time));
                if (currentTime - time > reset) {
                    commonLog.info("chainId-" + chainId + ",NetworkReset!");
                    NetworkUtil.resetNetwork(chainId);
                }
            } catch (Exception e) {
                e.printStackTrace();
                commonLog.error("chainId-" + chainId + ",NetworkReset error!");
            }
        }

    }

}
