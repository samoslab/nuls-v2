/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.test.cmd;

import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.info.NoUse;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangkun23 on 2018/11/19.
 */
public class CmdTest {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Before
    public void before() throws Exception {
        NoUse.mockModule();
        //CmdDispatcher.syncKernel("ws://127.0.0.1:8887");
    }

    @Test
    public void lg_getBalance() throws Exception {
        double version = 1.0;
        // Build params map
        Map<String, Object> params = new HashMap<>();
        // Version information ("1.1" or 1.1 is both available)
        params.put(Constants.VERSION_KEY_STR, "1.0");
        params.put("chainId", 8096);
        params.put("address", "123");
        Response response = CmdDispatcher.requestAndResponse(ModuleE.LG.abbr, "lg_getBalance", params);
        logger.info("response {}", response);
    }
    @Test
    public void getBalance() throws Exception {
        double version = 1.0;
        // Build params map
        Map<String, Object> params = new HashMap<>();
        // Version information ("1.1" or 1.1 is both available)
        params.put("chainId", 8096);
        params.put("address", "256");
        params.put("assetId", 222);
        Response response = CmdDispatcher.requestAndResponse(ModuleE.LG.abbr, "getBalanceNonce", params);
        logger.info("response {}", response);
    }
}
