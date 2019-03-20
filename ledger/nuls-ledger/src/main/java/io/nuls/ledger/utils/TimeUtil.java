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
package io.nuls.ledger.utils;

import io.nuls.ledger.rpc.call.TimeRpcService;
import io.nuls.ledger.rpc.call.impl.TimeRpcServiceImpl;
import io.nuls.tools.core.ioc.SpringLiteContext;

/**
 * @author lan
 * @description
 * @date 2019/01/07
 **/
public class TimeUtil {
    static long latestGetTime = System.currentTimeMillis();
    static long offset = 0;

    public static long getCurrentTime() {
        long now = System.currentTimeMillis();
        if (now - latestGetTime > 30000) {
            TimeRpcService timeRpcService = SpringLiteContext.getBean(TimeRpcServiceImpl.class);
            offset = timeRpcService.getTime() - System.currentTimeMillis();
        }
        latestGetTime = now;
        return (System.currentTimeMillis() + offset);
    }
}
