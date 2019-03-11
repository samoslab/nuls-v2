/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.contract.rpc.call;

import io.nuls.contract.rpc.CallHelper;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.ModuleE;
import io.nuls.tools.exception.NulsException;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author: PierreLuo
 * @date: 2019-02-27
 */
public class AccountCall {

    public static String createContractAddress(int chainId) throws NulsException {
        try {
            Map<String, Object> params = new HashMap<>(4);
            params.put("chainId", chainId);
            //TODO pierre account模块获取合约地址的命令
            Map resultMap = (Map) CallHelper.request(ModuleE.AC.abbr, "contractAddress", params);
            return (String) resultMap.get("height");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

}
