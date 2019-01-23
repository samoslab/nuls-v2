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
package io.nuls.network.rpc;

import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.server.WsServer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lan
 * @description
 * @date 2018/12/20
 */
public class ProtocolRegisterTest {
    @Test
    public void test1() throws Exception {
        WsServer.getInstance(ModuleE.BL)
                .moduleRoles(new String[]{"1.0", "1.4"})
                .moduleVersion("1.2")
//                .dependencies(ModuleE.LG.abbr, "1.1")
//                .dependencies(ModuleE.BL.abbr, "2.1")
                .scanPackage("io.nuls.network.rpc.internal")
                .connect("ws://127.0.0.1:8887");

        // Get information from kernel
        CmdDispatcher.syncKernel();
        Map<String,Object> map = new HashMap<>();
        List<Map<String,String>> cmds = new ArrayList<>();
        map.put("role","bl");
        Map<String,String> cmd1 = new HashMap<>();
        cmd1.put("protocolCmd","getBlock2");
        cmd1.put("handler","getBlockRequest3");
        Map<String,String> cmd2 = new HashMap<>();
        cmd2.put("protocolCmd","sendBlock3");
        cmd2.put("handler","downLoadBlock3");
        cmds.add(cmd1);
        cmds.add(cmd2);
        map.put("protocolCmds",cmds);
        CmdDispatcher.requestAndResponse(ModuleE.NW.abbr,"nw_protocolRegister",map);
        Thread.sleep(Integer.MAX_VALUE);
    }

}
