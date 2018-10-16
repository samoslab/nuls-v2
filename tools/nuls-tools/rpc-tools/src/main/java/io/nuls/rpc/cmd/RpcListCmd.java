/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2018 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *  *
 *
 */

package io.nuls.rpc.cmd;

import com.alibaba.fastjson.JSON;
import io.nuls.rpc.RpcInfo;
import io.nuls.rpc.pojo.Rpc;

import java.util.HashMap;
import java.util.Map;

/**
 * @author tangyi
 * @date 2018/10/16
 * @description
 */
public class RpcListCmd extends BaseCmd {

    /**
     * cmd命令的具体实现方法
     *
     * @param param param说明：
     *              不需要参数
     * @return String
     */
    @Override
    public String execRpc(Object param) {
        Map<String, Rpc> rpcMap = new HashMap<>(16);
        rpcMap.putAll(RpcInfo.localInterfaceMap);
        rpcMap.putAll(RpcInfo.remoteInterfaceMap);
        return JSON.toJSONString(rpcMap);
    }
}
