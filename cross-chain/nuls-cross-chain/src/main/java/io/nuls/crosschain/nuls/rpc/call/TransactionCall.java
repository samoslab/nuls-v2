package io.nuls.crosschain.nuls.rpc.call;

import io.nuls.crosschain.base.model.dto.ModuleTxRegisterDTO;
import io.nuls.crosschain.nuls.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.nuls.model.bo.Chain;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;
import java.util.HashMap;
import java.util.Map;
/**
 * 与交易模块交互类
 * Interaction class with transaction module
 * @author tag
 * 2019/4/10
 */
public class TransactionCall {
    /**
     * 交易注册
     */
    @SuppressWarnings("unchecked")
    public static boolean registerTx(ModuleTxRegisterDTO moduleTxRegisterDTO) {
        try {
            Map<String, Object> params = new HashMap(4);
            params.put("chainId", moduleTxRegisterDTO.getChainId());
            params.put("list", moduleTxRegisterDTO.getList());
            params.put("moduleCode", moduleTxRegisterDTO.getModuleCode());
            params.put("moduleValidator", moduleTxRegisterDTO.getModuleValidator());
            params.put("commit", moduleTxRegisterDTO.getCommit());
            params.put("rollback", moduleTxRegisterDTO.getRollback());
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_register", params);
            if (!cmdResp.isSuccess()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    /**
     * 将新创建的交易发送给交易管理模块
     * The newly created transaction is sent to the transaction management module
     *
     * @param chain chain info
     * @param tx transaction hex
     */
    @SuppressWarnings("unchecked")
    public static boolean sendTx(Chain chain, String tx) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put("chainId", chain.getConfig().getChainId());
        params.put("tx", tx);
        try {
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_newTx", params);
            if (!cmdResp.isSuccess()) {
                chain.getRpcLogger().error("Transaction failed to send!");
                throw new NulsException(NulsCrossChainErrorCode.FAILED);
            }
            return true;
        }catch (NulsException e){
            throw e;
        }catch (Exception e) {
            chain.getRpcLogger().error(e);
            return false;
        }
    }
}
