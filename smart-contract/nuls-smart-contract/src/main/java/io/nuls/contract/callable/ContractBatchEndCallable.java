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
package io.nuls.contract.callable;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.contract.helper.ContractHelper;
import io.nuls.contract.model.bo.*;
import io.nuls.contract.model.dto.ContractPackageDto;
import io.nuls.contract.model.tx.ContractReturnGasTransaction;
import io.nuls.contract.model.txdata.ContractData;
import io.nuls.contract.service.ResultAnalyzer;
import io.nuls.contract.service.ResultHanlder;
import io.nuls.contract.util.Log;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.model.ByteArrayWrapper;
import io.nuls.tools.model.LongUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static io.nuls.contract.constant.ContractConstant.TX_TYPE_DELETE_CONTRACT;


/**
 * @author: PierreLuo
 * @date: 2019-03-26
 */
public class ContractBatchEndCallable implements Callable<ContractPackageDto> {

    private int chainId;
    private long blockHeight;
    private ContractHelper contractHelper;
    private ResultAnalyzer resultAnalyzer;
    private ResultHanlder resultHanlder;

    public ContractBatchEndCallable(int chainId, long blockHeight) {
        this.chainId = chainId;
        this.blockHeight = blockHeight;
        this.contractHelper = SpringLiteContext.getBean(ContractHelper.class);
        this.resultAnalyzer = SpringLiteContext.getBean(ResultAnalyzer.class);
        this.resultHanlder = SpringLiteContext.getBean(ResultHanlder.class);
    }

    @Override
    public ContractPackageDto call() {
        try {
            BatchInfo batchInfo = contractHelper.getChain(chainId).getBatchInfo();
            BlockHeader currentBlockHeader = batchInfo.getCurrentBlockHeader();
            long blockTime = currentBlockHeader.getTime();

            LinkedHashMap<String, ContractContainer> contractContainerMap = batchInfo.getContractContainerMap();
            Collection<ContractContainer> containerList = contractContainerMap.values();
            CallerResult callerResult = new CallerResult();
            List<CallableResult> resultList = callerResult.getCallableResultList();
            for (ContractContainer container : containerList) {
                container.loadFutureList();
                resultList.add(container.getCallableResult());
            }

            ProgramExecutor batchExecutor = batchInfo.getBatchExecutor();
            String preStateRoot = batchInfo.getPreStateRoot();
            // 合约执行结果归类
            AnalyzerResult analyzerResult = resultAnalyzer.analysis(callerResult.getCallableResultList());
            // 重新执行冲突合约，处理失败合约的金额退还
            List<ContractResult> contractResultList = resultHanlder.handleAnalyzerResult(chainId, batchExecutor, analyzerResult, preStateRoot);
            // 归集合约内部转账交易
            List<Transaction> resultTxList = new ArrayList<>();
            for (ContractResult contractResult : contractResultList) {
                Log.info("=======contractResult 地址 is {}, 排序时间 is {}", AddressTool.getStringAddressByBytes(contractResult.getContractAddress()), contractResult.getTxTime());
                resultTxList.addAll(contractResult.getContractTransferList());
            }
            // 生成退还剩余Gas的交易
            ContractReturnGasTransaction contractReturnGasTx = makeReturnGasTx(chainId, contractResultList, blockTime, contractHelper);
            if (contractReturnGasTx != null) {
                resultTxList.add(contractReturnGasTx);
            }

            ContractPackageDto dto = new ContractPackageDto(null, resultTxList);
            dto.makeContractResultMap(contractResultList);
            batchInfo.setContractPackageDto(dto);

            return dto;
        } catch (IOException e) {
            Log.error(e);
            return null;
        } catch (InterruptedException e) {
            Log.error(e);
            return null;
        } catch (ExecutionException e) {
            Log.error(e);
            return null;
        }
    }

    private static ContractReturnGasTransaction makeReturnGasTx(int chainId, List<ContractResult> resultList, long time, ContractHelper contractHelper) throws IOException {
        int assetsId = contractHelper.getChain(chainId).getConfig().getAssetsId();
        ContractWrapperTransaction wrapperTx;
        ContractData contractData;
        Map<ByteArrayWrapper, BigInteger> returnMap = new HashMap<>();
        for (ContractResult contractResult : resultList) {
            wrapperTx = contractResult.getTx();
            // 终止合约不消耗Gas，跳过
            if (wrapperTx.getType() == TX_TYPE_DELETE_CONTRACT) {
                continue;
            }
            contractData = wrapperTx.getContractData();
            long realGasUsed = contractResult.getGasUsed();
            long txGasUsed = contractData.getGasLimit();
            long returnGas;

            BigInteger returnValue;
            if (txGasUsed > realGasUsed) {
                returnGas = txGasUsed - realGasUsed;
                returnValue = BigInteger.valueOf(LongUtils.mul(returnGas, contractData.getPrice()));

                ByteArrayWrapper sender = new ByteArrayWrapper(contractData.getSender());
                BigInteger senderValue = returnMap.get(sender);
                if (senderValue == null) {
                    senderValue = returnValue;
                } else {
                    senderValue = senderValue.add(returnValue);
                }
                returnMap.put(sender, senderValue);
            }
        }
        if (!returnMap.isEmpty()) {
            CoinData coinData = new CoinData();
            List<CoinTo> toList = coinData.getTo();
            Set<Map.Entry<ByteArrayWrapper, BigInteger>> entries = returnMap.entrySet();
            CoinTo returnCoin;
            for (Map.Entry<ByteArrayWrapper, BigInteger> entry : entries) {
                returnCoin = new CoinTo(entry.getKey().getBytes(), chainId, assetsId, entry.getValue(), 0L);
                toList.add(returnCoin);
            }
            ContractReturnGasTransaction tx = new ContractReturnGasTransaction();
            tx.setTime(time);
            tx.setCoinData(coinData.serialize());
            tx.setHash(NulsDigestData.calcDigestData(tx.serializeForHash()));
            return tx;
        }

        return null;
    }

}
