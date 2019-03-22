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
package io.nuls.contract.util;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.model.bo.ContractResult;
import io.nuls.contract.model.bo.ContractTempTransaction;
import io.nuls.contract.model.bo.ContractWrapperTransaction;
import io.nuls.contract.model.po.ContractTokenTransferInfoPo;
import io.nuls.contract.model.tx.CallContractTransaction;
import io.nuls.contract.model.tx.ContractBaseTransaction;
import io.nuls.contract.model.tx.CreateContractTransaction;
import io.nuls.contract.model.tx.DeleteContractTransaction;
import io.nuls.contract.model.txdata.CallContractData;
import io.nuls.contract.model.txdata.ContractData;
import io.nuls.contract.model.txdata.CreateContractData;
import io.nuls.contract.model.txdata.DeleteContractData;
import io.nuls.contract.rpc.call.BlockCall;
import io.nuls.db.service.RocksDBService;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.message.MessageUtil;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.basic.Result;
import io.nuls.tools.basic.VarInt;
import io.nuls.tools.constant.ErrorCode;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.log.Log;
import io.nuls.tools.model.StringUtils;
import io.nuls.tools.parse.JSONUtils;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static io.nuls.contract.constant.ContractConstant.*;
import static io.nuls.tools.model.StringUtils.isBlank;

/**
 * @author: PierreLuo
 * @date: 2018/8/25
 */
public class ContractUtil {

    /**
     * 此长度来源于BlockExtendsData中定长变量的字节总数
     */
    private static final int BLOCK_EXTENDS_DATA_FIX_LENGTH = 21;

    private static final String STRING = "String";

    public static String[][] twoDimensionalArray(Object[] args, String[] types) {
        if (args == null) {
            return null;
        } else {
            int length = args.length;
            String[][] two = new String[length][];
            Object arg;
            for (int i = 0; i < length; i++) {
                arg = args[i];
                if (arg == null) {
                    two[i] = new String[0];
                    continue;
                }
                if (arg instanceof String) {
                    String argStr = (String) arg;
                    // 非String类型参数，若传参是空字符串，则赋值为空一维数组，避免数字类型转化异常 -> 空字符串转化为数字
                    if (types != null && isBlank(argStr) && !STRING.equalsIgnoreCase(types[i])) {
                        two[i] = new String[0];
                    } else {
                        two[i] = new String[]{argStr};
                    }
                } else if (arg.getClass().isArray()) {
                    int len = Array.getLength(arg);
                    String[] result = new String[len];
                    for (int k = 0; k < len; k++) {
                        result[k] = valueOf(Array.get(arg, k));
                    }
                    two[i] = result;
                } else if (arg instanceof ArrayList) {
                    ArrayList resultArg = (ArrayList) arg;
                    int size = resultArg.size();
                    String[] result = new String[size];
                    for (int k = 0; k < size; k++) {
                        result[k] = valueOf(resultArg.get(k));
                    }
                    two[i] = result;
                } else {
                    two[i] = new String[]{valueOf(arg)};
                }
            }
            return two;
        }
    }

    public static byte[] extractContractAddressFromTxData(Transaction tx) {
        if (tx == null) {
            return null;
        }
        int txType = tx.getType();
        if (txType == ContractConstant.TX_TYPE_CREATE_CONTRACT
                || txType == ContractConstant.TX_TYPE_CALL_CONTRACT
                || txType == ContractConstant.TX_TYPE_DELETE_CONTRACT) {
            return extractContractAddressFromTxData(tx.getTxData());
        }
        return null;
    }

    private static byte[] extractContractAddressFromTxData(byte[] txData) {
        if (txData == null) {
            return null;
        }
        int length = txData.length;
        if (length < Address.ADDRESS_LENGTH * 2) {
            return null;
        }
        byte[] contractAddress = new byte[Address.ADDRESS_LENGTH];
        System.arraycopy(txData, Address.ADDRESS_LENGTH, contractAddress, 0, Address.ADDRESS_LENGTH);
        return contractAddress;
    }

    public static ContractWrapperTransaction parseContractTransaction(ContractTempTransaction tx) throws NulsException {
        ContractWrapperTransaction contractTransaction = null;
        ContractData contractData = null;
        boolean isContractTx = true;
        switch (tx.getType()) {
            case TX_TYPE_CREATE_CONTRACT:
                CreateContractData create = new CreateContractData();
                create.parse(tx.getTxData(), 0);
                contractData = create;
                break;
            case TX_TYPE_CALL_CONTRACT:
                CallContractData call = new CallContractData();
                call.parse(tx.getTxData(), 0);
                contractData = call;
                break;
            case TX_TYPE_DELETE_CONTRACT:
                DeleteContractData delete = new DeleteContractData();
                delete.parse(tx.getTxData(), 0);
                contractData = delete;
                break;
            default:
                isContractTx = false;
                break;
        }
        if (isContractTx) {
            contractTransaction = new ContractWrapperTransaction(tx, tx.getTxHex(), contractData);
        }
        return contractTransaction;
    }

    public static String[][] twoDimensionalArray(Object[] args) {
        return twoDimensionalArray(args, null);
    }

    public static boolean isLegalContractAddress(int chainId, byte[] addressBytes) {
        if (addressBytes == null) {
            return false;
        }
        return AddressTool.validContractAddress(addressBytes, chainId);
    }

    public static String valueOf(Object obj) {
        return (obj == null) ? null : obj.toString();
    }

    public static ContractTokenTransferInfoPo convertJsonToTokenTransferInfoPo(int chainId, String event) {
        if (isBlank(event)) {
            return null;
        }
        ContractTokenTransferInfoPo po;
        try {
            Map<String, Object> eventMap = JSONUtils.json2map(event);
            String eventName = (String) eventMap.get(CONTRACT_EVENT);
            String contractAddress = (String) eventMap.get(CONTRACT_EVENT_ADDRESS);
            po = new ContractTokenTransferInfoPo();
            po.setContractAddress(contractAddress);
            if (NRC20_EVENT_TRANSFER.equals(eventName)) {
                Map<String, Object> data = (Map<String, Object>) eventMap.get(CONTRACT_EVENT_DATA);
                Collection<Object> values = data.values();
                int i = 0;
                String transferEventdata;
                byte[] addressBytes;
                for (Object object : values) {
                    transferEventdata = (String) object;
                    if (i == 0 || i == 1) {
                        if (AddressTool.validAddress(chainId, transferEventdata)) {
                            addressBytes = AddressTool.getAddress(transferEventdata);
                            if (i == 0) {
                                po.setFrom(addressBytes);
                            } else {
                                po.setTo(addressBytes);
                            }
                        }
                    }
                    if (i == 2) {
                        po.setValue(isBlank(transferEventdata) ? BigInteger.ZERO : new BigInteger(transferEventdata));
                        break;
                    }
                    i++;
                }
                return po;
            }
            return null;
        } catch (Exception e) {
            Log.error(e);
            return null;
        }
    }

    public static boolean isContractTransaction(Transaction tx) {
        if (tx == null) {
            return false;
        }
        int txType = tx.getType();
        if (txType == ContractConstant.TX_TYPE_CREATE_CONTRACT
                || txType == ContractConstant.TX_TYPE_CALL_CONTRACT
                || txType == ContractConstant.TX_TYPE_DELETE_CONTRACT
                || txType == ContractConstant.TX_TYPE_CONTRACT_TRANSFER
                || txType == ContractConstant.TX_TYPE_CONTRACT_RETURN_GAS) {
            return true;
        }
        return false;
    }

    public static boolean isGasCostContractTransaction(Transaction tx) {
        if (tx == null) {
            return false;
        }
        int txType = tx.getType();
        if (txType == ContractConstant.TX_TYPE_CREATE_CONTRACT
                || txType == ContractConstant.TX_TYPE_CALL_CONTRACT) {
            return true;
        }
        return false;
    }

    public static boolean isLockContract(int chainId, long blockHeight) throws NulsException {
        if (blockHeight > 0) {
            long bestBlockHeight = BlockCall.getLatestHeight(chainId);
            long confirmCount = bestBlockHeight - blockHeight;
            if (confirmCount < 7) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLockContract(long lastestHeight, long blockHeight) throws NulsException {
        if (blockHeight > 0) {
            long confirmCount = lastestHeight - blockHeight;
            if (confirmCount < 7) {
                return true;
            }
        }
        return false;
    }

    public static byte[] getStateRoot(BlockHeader blockHeader) {
        if (blockHeader == null || blockHeader.getExtend() == null) {
            return null;
        }
        byte[] stateRoot = blockHeader.getStateRoot();
        if (stateRoot != null && stateRoot.length > 0) {
            return stateRoot;
        }
        try {
            byte[] extend = blockHeader.getExtend();
            if (extend.length > BLOCK_EXTENDS_DATA_FIX_LENGTH) {
                VarInt varInt = new VarInt(extend, BLOCK_EXTENDS_DATA_FIX_LENGTH);
                int lengthFieldSize = varInt.getOriginalSizeInBytes();
                int stateRootlength = (int) varInt.value;
                stateRoot = new byte[stateRootlength];
                System.arraycopy(extend, BLOCK_EXTENDS_DATA_FIX_LENGTH + lengthFieldSize, stateRoot, 0, stateRootlength);
                blockHeader.setStateRoot(stateRoot);
                return stateRoot;
            }
        } catch (Exception e) {
            Log.error("parse stateRoot error.", e);
        }
        return null;
    }

    public static String bigInteger2String(BigInteger bigInteger) {
        if (bigInteger == null) {
            return null;
        }
        return bigInteger.toString();
    }

    public static String simplifyErrorMsg(String errorMsg) {
        String resultMsg = "contract error - ";
        if (isBlank(errorMsg)) {
            return resultMsg;
        }
        if (errorMsg.contains("Exception:")) {
            String[] msgs = errorMsg.split("Exception:", 2);
            return resultMsg + msgs[1].trim();
        }
        return resultMsg + errorMsg;
    }

    public static Result checkVmResultAndReturn(String errorMessage, Result defaultResult) {
        if (isBlank(errorMessage)) {
            return defaultResult;
        }
        if (isNotEnoughGasError(errorMessage)) {
            return Result.getFailed(ContractErrorCode.CONTRACT_GAS_LIMIT);
        }
        return defaultResult;
    }

    private static boolean isNotEnoughGasError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        if (errorMessage.contains(NOT_ENOUGH_GAS)) {
            return true;
        }
        return false;
    }

    public static boolean isNotEnoughGasError(ContractResult contractResult) {
        if(contractResult.isSuccess()) {
            return false;
        }
        return isNotEnoughGasError(contractResult.getErrorMessage());
    }

    public static boolean isTerminatedContract(int status) {
        return ContractConstant.STOP == status;
    }

    public static boolean isTransferMethod(String method) {
        return (ContractConstant.NRC20_METHOD_TRANSFER.equals(method)
                || ContractConstant.NRC20_METHOD_TRANSFER_FROM.equals(method));
    }

    public static String argToString(String[][] args) {
        if (args == null) {
            return "";
        }
        String result = "";
        for (String[] a : args) {
            result += Arrays.toString(a) + "| ";
        }
        return result;
    }

    public static boolean checkPrice(long price) {
        if (price < ContractConstant.CONTRACT_MINIMUM_PRICE) {
            return false;
        }
        return true;
    }

    public static void createTable(String name) {
        if (!RocksDBService.existTable(name)) {
            try {
                RocksDBService.createTable(name);
            } catch (Exception e) {
                Log.error(e);
                throw new NulsRuntimeException(ContractErrorCode.CONTRACT_OTHER_ERROR);
            }
        }
    }

    public static boolean isLegalContractAddress(byte[] addressBytes) {
        if (addressBytes == null) {
            return false;
        }
        return true;
    }

    public static boolean isLegalContractAddress(String address) {
        return true;
    }


    public static void put(Map<String, Set<ContractResult>> map, String contractAddress, ContractResult result) {
        Set<ContractResult> resultSet = map.get(contractAddress);
        if (resultSet == null) {
            resultSet = new HashSet<>();
            map.put(contractAddress, resultSet);
        }
        resultSet.add(result);
    }

    public static void putAll(Map<String, Set<ContractResult>> map, Map<String, Set<ContractResult>> collectAddress) {
        Set<Map.Entry<String, Set<ContractResult>>> entries = collectAddress.entrySet();
        for (Map.Entry<String, Set<ContractResult>> entry : entries) {
            String contractAddress = entry.getKey();
            Set<ContractResult> contractResultSet = entry.getValue();

            Set<ContractResult> resultSet = map.get(contractAddress);
            if (resultSet == null) {
                resultSet = new HashSet<>();
                map.put(contractAddress, resultSet);
            }
            resultSet.addAll(contractResultSet);
        }
    }

    public static void putAll(Map<String, Set<ContractResult>> map, ContractResult contractResult) {
        Set<String> addressSet = collectAddress(contractResult);
        for (String address : addressSet) {
            put(map, address, contractResult);
        }
    }

    public static Set<String> collectAddress(ContractResult result) {
        Set<String> set = new HashSet<>();
        set.add(AddressTool.getStringAddressByBytes(result.getContractAddress()));
        Set<String> innerCallSet = result.getContractAddressInnerCallSet();
        if (innerCallSet != null) {
            set.addAll(innerCallSet);
        }

        result.getTransfers().stream().forEach(transfer -> {
            if (ContractUtil.isLegalContractAddress(transfer.getFrom())) {
                set.add(AddressTool.getStringAddressByBytes(transfer.getFrom()));
            }
            if (ContractUtil.isLegalContractAddress(transfer.getTo())) {
                set.add(AddressTool.getStringAddressByBytes(transfer.getTo()));
            }
        });
        return set;
    }

    /**
     * @param needReCallList
     * @return 去掉重复的交易，并按照时间降序排列
     */
    public static List<ContractResult> deduplicationAndOrder(List<ContractResult> contractResultList) {
        return contractResultList.stream().collect(Collectors.toSet()).stream()
                .collect(Collectors.toList()).stream().sorted(CompareTx.getInstance()).collect(Collectors.toList());
    }

    /**
     * @param list
     * @return 收集合约执行中所有出现过的合约地址，包括内部调用合约，合约转账
     */
    public static Map<String, Set<ContractResult>> collectAddressMap(List<ContractResult> contractResultList) {
        Map<String, Set<ContractResult>> map = new HashMap<>();
        for (ContractResult result : contractResultList) {
            put(map, AddressTool.getStringAddressByBytes(result.getContractAddress()), result);
            result.getContractAddressInnerCallSet().stream().forEach(inner -> put(map, inner, result));

            result.getTransfers().stream().forEach(transfer -> {
                if (ContractUtil.isLegalContractAddress(transfer.getFrom())) {
                    put(map, AddressTool.getStringAddressByBytes(transfer.getFrom()), result);
                }
                if (ContractUtil.isLegalContractAddress(transfer.getTo())) {
                    put(map, AddressTool.getStringAddressByBytes(transfer.getTo()), result);
                }
            });
        }
        return map;
    }

    public static void makeContractResult(ContractWrapperTransaction tx, ContractResult contractResult) {
        contractResult.setTx(tx);
        contractResult.setTxTime(tx.getTime());
        contractResult.setHash(tx.getHash().toString());
    }

    public static Result getSuccess() {
        return Result.getSuccess(ContractErrorCode.SUCCESS);
    }

    public static Result getFailed() {
        return Result.getFailed(ContractErrorCode.FAILED);
    }

    public static String asString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] asBytes(String string) {
        return Base64.getDecoder().decode(string);
    }

    public static BigInteger minus(BigInteger a, BigInteger b) {
        BigInteger result = a.subtract(b);
        if (result.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("Negative number detected.");
        }
        return result;
    }

    public static ContractBaseTransaction convertContractTx(Transaction tx) {
        ContractBaseTransaction resultTx = null;
        switch (tx.getType()) {
            case TX_TYPE_CREATE_CONTRACT:
                resultTx = new CreateContractTransaction();
                resultTx.copyTx(tx);
                break;
            case TX_TYPE_CALL_CONTRACT:
                resultTx = new CallContractTransaction();
                resultTx.copyTx(tx);
                break;
            case TX_TYPE_DELETE_CONTRACT:
                resultTx = new DeleteContractTransaction();
                resultTx.copyTx(tx);
                break;
            default:
                break;
        }
        return resultTx;
    }

    public static Response wrapperFailed(Result result) {
        ErrorCode errorCode = result.getErrorCode();
        String msg = result.getMsg();
        if (StringUtils.isBlank(msg)) {
            msg = errorCode.getMsg();
        }
        Response response = MessageUtil.newResponse("", Constants.BOOLEAN_FALSE, msg);
        response.setResponseData(errorCode);
        return response;
    }
}
