package io.nuls.transaction.rpc.call;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.data.Transaction;
import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.constant.ErrorCode;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.TxRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用其他模块跟交易相关的接口
 *
 * @author: qinyifeng
 * @date: 2018/12/05
 */
public class TransactionCall {

    /**
     * 调用其他模块接口
     * Call other module interfaces
     */
    public static Object request(String moduleCode, String cmd, Map params) throws NulsException {
        try {
            params.put(Constants.VERSION_KEY_STR, "1.0");
            Response cmdResp = CmdDispatcher.requestAndResponse(moduleCode, cmd, params);
            Map resData = (Map)cmdResp.getResponseData();
//            try {
//                Log.debug("moduleCode:{}, -cmd:{}, -txProcess -rs: {}",moduleCode, cmd, JSONUtils.obj2json(resData));
//            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//            }
            if (!cmdResp.isSuccess()) {
                String errorMsg = null;
                if(null == resData){
                    cmdResp.getResponseComment();
                    errorMsg = String.format("Remote call fail. ResponseComment: %s ", cmdResp.getResponseComment());
                }else {
                    Map map = (Map) resData.get(cmd);
                    errorMsg = String.format("Remote call fail. msg: %s - code: %s - module: %s - interface: %s \n- params: %s ",
                            map.get("msg"), map.get("code"), moduleCode, cmd, JSONUtils.obj2PrettyJson(params));
                }
                //throw new NulsException(TxErrorCode.CALLING_REMOTE_INTERFACE_FAILED);
                throw new Exception(errorMsg);
            }
            if (null == resData) {
                return null;
            }
            return resData.get(cmd);
        } catch (Exception e) {
            Log.debug("cmd: {}", cmd);
            throw new NulsException(e);
        }
    }


    /**
     * 调用各交易验证器
     * @param chain
     * @param txRegister 交易注册信息
     * @param txHex
     * @return
     * @throws NulsException
     */
    public static boolean txValidatorProcess(Chain chain, TxRegister txRegister, String txHex) throws NulsException {

        if(StringUtils.isBlank(txRegister.getValidator())){
            //交易没有注册验证器cmd的交易,包括系统交易,则直接返回true
            return true;
        }
        //调用单个交易验证器
        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
        params.put("chainId", chain.getChainId());
        params.put("txHex", txHex);
        Map result = (Map) TransactionCall.request(txRegister.getModuleCode(), txRegister.getValidator(), params);
        try {
            chain.getLogger().debug("moduleCode:{}, -cmd:{}, -txProcess -rs: {}", txRegister.getModuleCode(), txRegister.getValidator(), JSONUtils.obj2json(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return (Boolean) result.get("value");
    }

    /**
     * 调用交易的 commit 或者 rollback
     * @param chain
     * @param cmd
     * @param moduleCode
     * @param tx
     * @return
     */
    public static boolean txProcess(Chain chain, String cmd, String moduleCode, Transaction tx, String blockHeaderDigest) throws NulsException {
        try {
            if(tx.getType() == TxConstant.TX_TYPE_COINBASE || StringUtils.isBlank(cmd)){
                //coinbase 没有提交回滚接口, 或者没有注册commit或者rollback接口的交易, 直接返回true
                return true;
            }
            //调用单个交易验证器
            Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
            params.put("chainId", chain.getChainId());
            params.put("txHex", tx.hex());
            params.put("blockHeaderDigest", blockHeaderDigest);
            Map result = (Map) TransactionCall.request(moduleCode, cmd, params);
            chain.getLogger().debug("moduleCode:{}, -cmd:{}, -txProcess -rs: {}",moduleCode, cmd, JSONUtils.obj2json(result));
            return (Boolean) result.get("value");
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 批量调用模块交易统一验证器
     * Batch call module transaction integrate validator
     *
     * @param chain
     * @param map
     * @return
     */
    public static boolean txsModuleValidators(Chain chain, Map<TxRegister, List<String>> map) throws NulsException {
        //调用交易模块统一验证器 批量
        boolean rs = true;
        for (Map.Entry<TxRegister, List<String>> entry : map.entrySet()) {
            List<String> list = txModuleValidator(chain, entry.getKey().getModuleValidator(), entry.getKey().getModuleCode(), entry.getValue());
            if (list.size() > 0) {
                rs = false;
                break;
            }
        }
        return rs;
    }

    /**
     * 单个模块交易统一验证器
     * Single module transaction integrate validator
     *
     * @param moduleValidator
     * @param txHexList
     * @return 返回未通过验证的交易hash / return unverified transaction hash
     */
    public static List<String> txModuleValidator(Chain chain, String moduleValidator, String moduleCode, List<String> txHexList) throws NulsException {

        //调用交易模块统一验证器
        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
        params.put("chainId", chain.getChainId());
        params.put("txHexList", txHexList);
        Map result = (Map) TransactionCall.request(moduleCode, moduleValidator, params);
        return (List<String>) result.get("list");
    }

}
