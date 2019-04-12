package io.nuls.account.rpc.cmd;

import io.nuls.account.constant.AccountConstant;
import io.nuls.account.constant.AccountErrorCode;
import io.nuls.account.constant.RpcParameterNameConstant;
import io.nuls.account.util.LoggerUtil;
import io.nuls.account.util.TxUtil;
import io.nuls.account.util.validator.TxValidator;
import io.nuls.base.data.Transaction;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.constant.TxType;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.protocol.ResisterTx;
import io.nuls.tools.protocol.TransactionProcessor;
import io.nuls.tools.protocol.TxMethodType;
import io.nuls.tools.protocol.TxProperty;

import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionHandler extends BaseCmd {

    @Autowired
    private TxValidator txValidator;

    /**
     * 转账交易验证
     */
    @CmdAnnotation(cmd = "ac_transferTxValidate", version = 1.0, description = "create transfer transaction validate 1.0")
    @ResisterTx(txType = TxProperty.TRANSFER, methodType = TxMethodType.VALID, methodName = "ac_transferTxValidate")
    @Parameter(parameterName = RpcParameterNameConstant.CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = RpcParameterNameConstant.TX, parameterType = "String")
    @TransactionProcessor(txType = TxType.TRANSFER, methodType = TxMethodType.VALID)
    public Response transferTxValidate(Map<String, Object> params) {
        Map<String, Boolean> resultMap = new HashMap<>(AccountConstant.INIT_CAPACITY_2);
        boolean result;
        try {
            if (params.get(RpcParameterNameConstant.CHAIN_ID) == null || params.get(RpcParameterNameConstant.TX) == null) {
                LoggerUtil.logger.warn("ac_transferTxValidate params is null");
                throw new NulsException(AccountErrorCode.PARAMETER_ERROR);
            }
            int chainId = (Integer) params.get(RpcParameterNameConstant.CHAIN_ID);
            String tx = (String) params.get(RpcParameterNameConstant.TX);
            if (chainId <= 0) {
                throw new NulsException(AccountErrorCode.PARAMETER_ERROR);
            }
            Transaction transaction = TxUtil.getInstanceRpcStr(tx, Transaction.class);
            result = txValidator.validateTx(chainId, transaction);
        } catch (NulsException e) {
            LoggerUtil.logger.warn("", e);
            result = false;
        } catch (Exception e) {
            LoggerUtil.logger.error("", e);
            result = false;
        }
        resultMap.put("value", result);
        return success(resultMap);
    }

}
