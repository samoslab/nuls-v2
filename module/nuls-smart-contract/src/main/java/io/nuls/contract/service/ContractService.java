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


import io.nuls.contract.model.bo.ContractResult;
import io.nuls.contract.model.bo.ContractTempTransaction;
import io.nuls.core.basic.Result;

import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2018/11/19
 */
public interface ContractService {

    Result begin(int chainId, long blockHeight, long blockTime, String packingAddress, String preStateRoot);

    Result beforeEnd(int chainId, long blockHeight);

    Result end(int chainId, long blockHeight);

    Result invokeContractOneByOne(int chainId, ContractTempTransaction tx);

    Result commitProcessor(int chainId, List<String> txDataList, String blockHeaderData);

    Result rollbackProcessor(int chainId, List<String> txDataList, String blockHeaderData);

    Result saveContractExecuteResult(int chainId, byte[] hash, ContractResult contractResult);

    Result deleteContractExecuteResult(int chainId, byte[] hash);

    ContractResult getContractExecuteResult(int chainId, byte[] hash);

}
