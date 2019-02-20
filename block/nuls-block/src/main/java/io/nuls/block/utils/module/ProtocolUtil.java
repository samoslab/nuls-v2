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

package io.nuls.block.utils.module;

import io.nuls.base.data.Block;
import io.nuls.base.data.BlockHeader;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.ChainContext;
import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.log.logback.NulsLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用协议升级模块接口的工具类
 *
 * @author captain
 * @version 1.0
 * @date 18-11-9 上午10:43
 */
public class ProtocolUtil {

    public static boolean meaasge(int chainId, BlockHeader blockHeader) {
        return false;
    }

    /**
     * 回滚区块时通知协议升级模块
     *
     * @param chainId
     * @return
     */
    public static boolean rollbackNotice(int chainId, BlockHeader blockHeader) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger commonLog = context.getCommonLog();
        try {
            Map<String, Object> params = new HashMap<>(3);
//            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            params.put("blockHeader", HexUtil.encode(blockHeader.serialize()));
            Response response = CmdDispatcher.requestAndResponse(ModuleE.PU.abbr, "rollbackBlock", params);
            if (response.isSuccess()) {
                context.setVersion((short) 0);
            }
            return response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            commonLog.error(e);
            return false;
        }
    }

    /**
     * 新增区块时通知协议升级模块
     *
     * @param chainId
     * @return
     */
    public static boolean saveNotice(int chainId, BlockHeader blockHeader) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger commonLog = context.getCommonLog();
        try {
            Map<String, Object> params = new HashMap<>(3);
//            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            params.put("blockHeader", HexUtil.encode(blockHeader.serialize()));
            Response response = CmdDispatcher.requestAndResponse(ModuleE.PU.abbr, "saveBlock", params);
            if (response.isSuccess()) {
                context.setVersion((short) 0);
            }
            return response.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            commonLog.error(e);
            return false;
        }
    }

}
