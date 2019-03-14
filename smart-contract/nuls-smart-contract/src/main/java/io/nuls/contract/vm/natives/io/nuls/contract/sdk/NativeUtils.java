/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
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
package io.nuls.contract.vm.natives.io.nuls.contract.sdk;

import io.nuls.contract.sdk.Event;
import io.nuls.contract.vm.*;
import io.nuls.contract.vm.code.ClassCode;
import io.nuls.contract.vm.code.FieldCode;
import io.nuls.contract.vm.code.MethodCode;
import io.nuls.contract.vm.code.VariableType;
import io.nuls.contract.vm.exception.ErrorException;
import io.nuls.contract.vm.natives.NativeMethod;
import io.nuls.contract.vm.util.Constants;
import io.nuls.contract.vm.util.JsonUtils;
import io.nuls.contract.vm.util.Utils;
import io.nuls.tools.crypto.Sha3Hash;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.contract.vm.natives.NativeMethod.NOT_SUPPORT_NATIVE;
import static io.nuls.contract.vm.natives.NativeMethod.SUPPORT_NATIVE;
import static io.nuls.contract.vm.util.Utils.hashMapInitialCapacity;

public class NativeUtils {

    public static final String TYPE = "io/nuls/contract/sdk/Utils";

    public static Result nativeRun(MethodCode methodCode, MethodArgs methodArgs, Frame frame, boolean check) {
        switch (methodCode.fullName) {
            case revert:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return revert(methodCode, methodArgs, frame);
                }
            case emit:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return emit(methodCode, methodArgs, frame);
                }
            case sha3:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return sha3(methodCode, methodArgs, frame);
                }
            case sha3Bytes:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return sha3Bytes(methodCode, methodArgs, frame);
                }
            case verifySignatureData:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return verifySignatureData(methodCode, methodArgs, frame);
                }
            case getRandomSeedByCount:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return getRandomSeedByCount(methodCode, methodArgs, frame);
                }
            case getRandomSeedByHeight:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return getRandomSeedByHeight(methodCode, methodArgs, frame);
                }
            case getRandomSeedListByCount:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return getRandomSeedListByCount(methodCode, methodArgs, frame);
                }
            case getRandomSeedListByHeight:
                if (check) {
                    return SUPPORT_NATIVE;
                } else {
                    return getRandomSeedListByHeight(methodCode, methodArgs, frame);
                }
            default:
                if (check) {
                    return NOT_SUPPORT_NATIVE;
                } else {
                    frame.nonsupportMethod(methodCode);
                    return null;
                }
        }
    }

    public static final String revert = TYPE + "." + "revert" + "(Ljava/lang/String;)V";

    /**
     * native
     *
     * @see Utils#revert(String)
     */
    private static Result revert(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = (ObjectRef) methodArgs.invokeArgs[0];
        String errorMessage = null;
        if (objectRef != null) {
            errorMessage = frame.heap.runToString(objectRef);
        }
        throw new ErrorException(errorMessage, frame.vm.getGasUsed(), null);
    }

    public static final String emit = TYPE + "." + "emit" + "(Lio/nuls/contract/sdk/Event;)V";

    /**
     * native
     *
     * @see Utils#emit(Event)
     */
    private static Result emit(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        ObjectRef objectRef = (ObjectRef) methodArgs.invokeArgs[0];
        //String str = frame.heap.runToString(objectRef);
        ClassCode classCode = frame.methodArea.loadClass(objectRef.getVariableType().getType());
        Map<String, Object> jsonMap = toJson(objectRef, frame);
        EventJson eventJson = new EventJson();
        eventJson.setContractAddress(frame.vm.getProgramInvoke().getAddress());
        eventJson.setBlockNumber(frame.vm.getProgramInvoke().getNumber() + 1);
        eventJson.setEvent(classCode.simpleName);
        eventJson.setPayload(jsonMap);
        String json = JsonUtils.toJson(eventJson);
        frame.vm.getEvents().add(json);
        Result result = NativeMethod.result(methodCode, null, frame);
        return result;
    }

    private static Map<String, Object> toJson(ObjectRef objectRef, Frame frame) {
        if (objectRef == null) {
            return null;
        }

        Map<String, FieldCode> fields = frame.methodArea.allFields(objectRef.getVariableType().getType());
        Map<String, Object> map = frame.heap.getFields(objectRef);
        Map<String, Object> jsonMap = new LinkedHashMap<>(hashMapInitialCapacity(map.size()));
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            FieldCode fieldCode = fields.get(name);
            if (fieldCode != null && !fieldCode.isSynthetic) {
                Object value = entry.getValue();
                jsonMap.put(name, toJson(fieldCode, value, frame));
            }
        }
        return jsonMap;
    }

    private static Object toJson(FieldCode fieldCode, Object value, Frame frame) {
        VariableType variableType = fieldCode.variableType;
        if (value == null) {
            return null;
        } else if (variableType.isPrimitive()) {
            return variableType.getPrimitiveValue(value);
        } else if (variableType.isArray()) {
            ObjectRef ref = (ObjectRef) value;
            if (variableType.isPrimitiveType() && variableType.getDimensions() == 1) {
                return frame.heap.getObject(ref);
            } else {
                int length = ref.getDimensions()[0];
                Object[] array = new Object[length];
                for (int i = 0; i < length; i++) {
                    Object item = frame.heap.getArray(ref, i);
                    if (item != null) {
                        ObjectRef itemRef = (ObjectRef) item;
                        item = frame.heap.runToString(itemRef);
                    }
                    array[i] = item;
                }
                return array;
            }
        } else {
            ObjectRef ref = (ObjectRef) value;
            return frame.heap.runToString(ref);
        }
    }

    static class EventJson {

        private String contractAddress;

        private long blockNumber;

        private String event;

        private Map<String, Object> payload;

        public String getContractAddress() {
            return contractAddress;
        }

        public void setContractAddress(String contractAddress) {
            this.contractAddress = contractAddress;
        }

        public long getBlockNumber() {
            return blockNumber;
        }

        public void setBlockNumber(long blockNumber) {
            this.blockNumber = blockNumber;
        }

        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(Map<String, Object> payload) {
            this.payload = payload;
        }

    }

    public static final String sha3 = TYPE + "." + "sha3" + "(Ljava/lang/String;)Ljava/lang/String;";

    /**
     * native
     *
     * @see Utils#sha3(String)
     */
    private static Result sha3(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        frame.vm.addGasUsed(GasCost.SHA3);
        ObjectRef objectRef = (ObjectRef) methodArgs.invokeArgs[0];
        ObjectRef ref = null;
        if (objectRef != null) {
            String src = frame.heap.runToString(objectRef);
            String sha3 = Sha3Hash.sha3(src);
            ref = frame.heap.newString(sha3);
        }
        Result result = NativeMethod.result(methodCode, ref, frame);
        return result;
    }

    public static final String sha3Bytes = TYPE + "." + "sha3" + "([B)Ljava/lang/String;";

    /**
     * native
     *
     * @see Utils#sha3(byte[])
     */
    private static Result sha3Bytes(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        frame.vm.addGasUsed(GasCost.SHA3);
        ObjectRef objectRef = (ObjectRef) methodArgs.invokeArgs[0];
        ObjectRef ref = null;
        if (objectRef != null) {
            byte[] bytes = (byte[]) frame.heap.getObject(objectRef);
            String sha3 = Sha3Hash.sha3(bytes);
            ref = frame.heap.newString(sha3);
        }
        Result result = NativeMethod.result(methodCode, ref, frame);
        return result;
    }

    public static final String verifySignatureData = TYPE + "." + "verifySignatureData" + "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z";

    private static Result verifySignatureData(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        frame.vm.addGasUsed(GasCost.VERIFY_SIGNATURE);
        ObjectRef dataRef = (ObjectRef) methodArgs.invokeArgs[0];
        ObjectRef signatureRef = (ObjectRef) methodArgs.invokeArgs[1];
        ObjectRef pubKeyRef = (ObjectRef) methodArgs.invokeArgs[2];
        String data = frame.heap.runToString(dataRef);
        String signature = frame.heap.runToString(signatureRef);
        String pubKey = frame.heap.runToString(pubKeyRef);

        boolean verify = false;
        do {
            if (data == null || signature == null || pubKey == null) {
                break;
            }
            try {
                verify = Utils.verify(data, signature, pubKey);
            } catch (Exception e) {
                verify = false;
            }
        } while (false);

        Result result = NativeMethod.result(methodCode, verify, frame);
        return result;
    }

    public static final String getRandomSeedByCount = TYPE + "." + "getRandomSeed" + "(JILjava/lang/String;)Ljava/math/BigInteger;";

    private static Result getRandomSeedByCount(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        frame.setAddGas(false);
        frame.vm.addGasUsed(GasCost.RANDOM_COUNT_SEED);
        long endHeight = (long) methodArgs.invokeArgs[0];
        int count = (int) methodArgs.invokeArgs[1];
        ObjectRef algorithmRef = (ObjectRef) methodArgs.invokeArgs[2];
        String algorithm = frame.heap.runToString(algorithmRef);

        String seed = frame.vm.getRandomSeed(endHeight, count, algorithm);
        if (StringUtils.isBlank(seed)) {
            seed = "0";
        }
        ObjectRef objectRef = frame.heap.newBigInteger(seed);

        Result result = NativeMethod.result(methodCode, objectRef, frame);
        frame.setAddGas(true);
        return result;
    }

    public static final String getRandomSeedByHeight = TYPE + "." + "getRandomSeed" + "(JJLjava/lang/String;)Ljava/math/BigInteger;";

    private static Result getRandomSeedByHeight(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        frame.setAddGas(false);
        frame.vm.addGasUsed(GasCost.RANDOM_HEIGHT_SEED);
        long startHeight = (long) methodArgs.invokeArgs[0];
        long endHeight = (long) methodArgs.invokeArgs[1];
        ObjectRef algorithmRef = (ObjectRef) methodArgs.invokeArgs[2];
        String algorithm = frame.heap.runToString(algorithmRef);

        String seed = frame.vm.getRandomSeed(startHeight, endHeight, algorithm);
        if (StringUtils.isBlank(seed)) {
            seed = "0";
        }
        ObjectRef objectRef = frame.heap.newBigInteger(seed);

        Result result = NativeMethod.result(methodCode, objectRef, frame);
        frame.setAddGas(true);
        return result;
    }

    public static final String getRandomSeedListByCount = TYPE + "." + "getRandomSeedList" + "(JI)Ljava/util/List;";

    private static Result getRandomSeedListByCount(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        frame.setAddGas(false);
        frame.vm.addGasUsed(GasCost.RANDOM_COUNT_SEED);
        long endHeight = (long) methodArgs.invokeArgs[0];
        int count = (int) methodArgs.invokeArgs[1];

        List<byte[]> seeds = frame.vm.getRandomSeedList(endHeight, count);
        int i = seeds.size() - 1;
        if(i > 0) {
            frame.vm.addGasUsed(GasCost.RANDOM_COUNT_SEED * i);
        }

        ObjectRef objectRef = newBigIntegerArrayList(frame, seeds);

        Result result = NativeMethod.result(methodCode, objectRef, frame);
        frame.setAddGas(true);
        return result;
    }

    private static ObjectRef newBigIntegerArrayList(Frame frame, List<byte[]> seeds) {
        ObjectRef objectRef = frame.heap.newArrayList();

        MethodCode arrayListAddMethodCode = frame.vm.methodArea.loadMethod(VariableType.ARRAYLIST_TYPE.getType(), Constants.ARRAYLIST_ADD_METHOD_NAME, Constants.ARRAYLIST_ADD_METHOD_DESC);
        for (byte[] seed : seeds) {
            frame.vm.run(arrayListAddMethodCode, new Object[]{objectRef, frame.heap.newBigInteger(seed)}, false);
        }
        return objectRef;
    }

    public static final String getRandomSeedListByHeight = TYPE + "." + "getRandomSeedList" + "(JJ)Ljava/util/List;";

    private static Result getRandomSeedListByHeight(MethodCode methodCode, MethodArgs methodArgs, Frame frame) {
        frame.setAddGas(false);
        frame.vm.addGasUsed(GasCost.RANDOM_HEIGHT_SEED);
        long startHeight = (long) methodArgs.invokeArgs[0];
        long endHeight = (long) methodArgs.invokeArgs[1];

        List<byte[]> seeds = frame.vm.getRandomSeedList(startHeight, endHeight);
        int i = seeds.size() - 1;
        if(i > 0) {
            frame.vm.addGasUsed(GasCost.RANDOM_HEIGHT_SEED * i);
        }

        ObjectRef objectRef = newBigIntegerArrayList(frame, seeds);

        Result result = NativeMethod.result(methodCode, objectRef, frame);
        frame.setAddGas(true);
        return result;
    }
}
