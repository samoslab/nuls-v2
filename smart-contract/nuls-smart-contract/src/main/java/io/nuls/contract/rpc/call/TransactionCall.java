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

import io.nuls.base.data.Transaction;
import io.nuls.contract.rpc.CallHelper;
import io.nuls.rpc.model.ModuleE;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.model.StringUtils;
import org.spongycastle.util.encoders.Hex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2019-02-27
 */
public class TransactionCall {

    public static boolean registerTx(int chainId, String moduleCode, String moduleValidator, String commit, String rollback, List list) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put("chainId", chainId);
        params.put("moduleCode", moduleCode);
        params.put("moduleValidator", moduleValidator);
        params.put("commit", commit);
        params.put("rollback", rollback);
        params.put("list", list);
        try {
            Map<String, Boolean> registerResult = (Map<String, Boolean>) CallHelper.request(ModuleE.TX.abbr, "tx_register", params);
            return registerResult.get("value");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    public static boolean newTx(int chainId, String txHex) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put("chainId", chainId);
        params.put("txHex", txHex);
        try {
            Map<String, Boolean> registerResult = (Map<String, Boolean>) CallHelper.request(ModuleE.TX.abbr, "tx_newTx", params);
            return registerResult.get("value");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    public static Transaction getConfirmedTx(int chainId, String txHash) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put("chainId", chainId);
        params.put("txHash", txHash);
        try {
            Map result = (Map) CallHelper.request(ModuleE.TX.abbr, "tx_getConfirmedTx", params);
            String txHex = (String) result.get("txHex");
            if (StringUtils.isBlank(txHex)) {
                return null;
            }
            Transaction tx = new Transaction();
            tx.parse(Hex.decode(txHex), 0);
            return tx;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }


}
