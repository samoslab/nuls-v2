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
package io.nuls.contract.service;


import io.nuls.base.data.NulsDigestData;
import io.nuls.contract.model.bo.ContractResult;
import io.nuls.contract.model.bo.ContractTempTransaction;
import io.nuls.tools.basic.Result;

import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2018/11/19
 */
public interface ContractService {

    Result begin(int chainId, long blockHeight, long blockTime, String packingAddress, String preStateRoot);

    Result end(int chainId, long blockHeight);

    Result invokeContractOneByOne(int chainId, ContractTempTransaction tx);

    Result commitProcessor(int chainId, List<String> txHexList, String blockHeaderHex);

    Result rollbackProcessor(int chainId, List<String> txHexList, String blockHeaderHex);

    /**
     * 保存合约执行结果
     *
     * @param hash
     * @param contractResult
     * @return
     */
    Result saveContractExecuteResult(int chainId, NulsDigestData hash, ContractResult contractResult);

    /**
     * 删除合约执行结果
     *
     * @param hash
     * @return
     */
    Result deleteContractExecuteResult(int chainId, NulsDigestData hash);

    /**
     * 获取合约执行结果
     *
     * @param hash
     * @return
     */
    ContractResult getContractExecuteResult(int chainId, NulsDigestData hash);

}
