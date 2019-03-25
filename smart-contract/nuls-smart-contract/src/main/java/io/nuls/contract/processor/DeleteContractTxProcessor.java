/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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
package io.nuls.contract.processor;


import io.nuls.base.data.BlockHeader;
import io.nuls.contract.helper.ContractHelper;
import io.nuls.contract.model.bo.ContractResult;
import io.nuls.contract.model.bo.ContractWrapperTransaction;
import io.nuls.contract.service.ContractService;
import io.nuls.tools.basic.Result;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;

import static io.nuls.contract.util.ContractUtil.getSuccess;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/8
 */
@Component
public class DeleteContractTxProcessor {

    @Autowired
    private ContractService contractService;
    @Autowired
    private ContractHelper contractHelper;

    public Result onCommit(int chainId, ContractWrapperTransaction tx) {
        BlockHeader blockHeader = contractHelper.getBatchInfoCurrentBlockHeader(chainId);
        byte[] stateRoot = blockHeader.getStateRoot();
        long blockHeight = blockHeader.getHeight();
        ContractResult contractResult = tx.getContractResult();
        contractResult.setStateRoot(stateRoot);
        contractResult.setBlockHeight(blockHeight);
        contractService.saveContractExecuteResult(chainId, tx.getHash(), contractResult);
        return getSuccess();
    }

    public Result onRollback(int chainId, ContractWrapperTransaction tx) {
        contractService.deleteContractExecuteResult(chainId, tx.getHash());
        return getSuccess();
    }

}
