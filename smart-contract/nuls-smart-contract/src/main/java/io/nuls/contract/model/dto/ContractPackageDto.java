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
package io.nuls.contract.model.dto;

import io.nuls.base.data.Transaction;
import io.nuls.contract.model.bo.ContractResult;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2019-03-06
 */
@Getter
public class ContractPackageDto {
    @Setter
    private byte[] stateRoot;
    @Setter
    private List<Transaction> resultTxList;
    private Map<String, ContractResult> contractResultMap;

    public ContractPackageDto(byte[] stateRoot, List<Transaction> resultTxList) {
        this.stateRoot = stateRoot;
        this.resultTxList = resultTxList;
    }

    public void makeContractResultMap(List<ContractResult> contractResultList) {
        this.contractResultMap = contractResultList.stream().collect(Collectors.toMap(c -> c.getTx().getTxHex(), Function.identity(), (key1, key2) -> key2, LinkedHashMap::new));
    }
}
