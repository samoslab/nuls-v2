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

package io.nuls.test;

import io.nuls.rpc.cmd.CmdDispatcher;
import io.nuls.rpc.info.HostInfo;
import io.nuls.rpc.server.WsServer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tangyi
 * @date 2018/10/30
 * @description
 */
public class WsM2 {
    @Test
    public void test() throws Exception {

        WsServer s = new WsServer(HostInfo.randomPort());

        List<String> depends = new ArrayList<>();
        depends.add("m1");

        s.init("wangkun", depends, null);
        s.start();

        CmdDispatcher.syncLocalToKernel("ws://127.0.0.1:8887");

        System.out.println(CmdDispatcher.call("cmd1", null, 1.0));

        System.out.println(CmdDispatcher.call("cmd2", null, 1.0));

        System.out.println(CmdDispatcher.call("cmd1", null, 1.0));

        Thread.sleep(Integer.MAX_VALUE);
    }
}
