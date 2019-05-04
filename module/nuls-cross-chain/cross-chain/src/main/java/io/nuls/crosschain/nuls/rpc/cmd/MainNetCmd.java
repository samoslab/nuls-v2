package io.nuls.crosschain.nuls.rpc.cmd;

import io.nuls.crosschain.nuls.servive.MainNetService;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.util.Map;

/**
 * 主网跨链模块特有方法
 * @author tag
 * @date 2019/4/23
 */
@Component
public class MainNetCmd extends BaseCmd {
    @Autowired
    private MainNetService service;
    /**
     * 友链向主网链管理模块注册跨链信息,链管理模块通知跨链模块
     * */
    @CmdAnnotation(cmd = "registerCrossChain", version = 1.0, description = "register Cross Chain")
    @Parameter(parameterName = "chainId", parameterType = "int")
    public Response registerCrossChain(Map<String,Object> params){
        Result result = service.registerCrossChain(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 友链向主网连管理模块注销跨链信息，连管理模块通知跨链模块
     * */
    @CmdAnnotation(cmd = "cancelCrossChain", version = 1.0, description = "cancel Cross Chain")
    @Parameter(parameterName = "chainId", parameterType = "int")
    public Response cancelCrossChain(Map<String,Object> params){
        Result result = service.cancelCrossChain(params);
        if(result.isFailed()){
            return failed(result.getErrorCode());
        }
        return success(result.getData());

    }
}
