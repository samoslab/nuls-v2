package io.nuls.poc.rpc.cmd;

import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.poc.model.bo.config.ConfigBean;
import io.nuls.poc.model.bo.round.MeetingRound;
import io.nuls.poc.model.dto.output.AccountConsensusInfoDTO;
import io.nuls.poc.model.dto.output.WholeNetConsensusInfoDTO;
import io.nuls.poc.service.ChainService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 共识链相关接口
 *
 * @author tag
 * 2018/11/7
 */
@Component
public class ChainCmd extends BaseCmd {
    @Autowired
    private ChainService service;

//    /**
//     * 共识模块交易提交
//     * */
//    @CmdAnnotation(cmd = BaseConstant.TX_COMMIT, version = 1.0, description = "withdraw deposit agent transaction validate 1.0")
//    @Parameter(parameterName = ConsensusConstant.PARAM_CHAIN_ID, parameterType = "int")
//    @Parameter(parameterName = ConsensusConstant.PARAM_BLOCK_HEADER_HEX, parameterType = "String")
//    @Parameter(parameterName = ConsensusConstant.PARAM_TX_HEX_LIST, parameterType = "List<String>")
//    public Response commit(Map<String,Object> params){
//        Result result = service.commitCmd(params);
//        if(result.isFailed()){
//            return failed(result.getErrorCode());
//        }
//        return success(result.getData());
//    }

//    /**
//     * 共识模块交易回滚
//     * */
//    @CmdAnnotation(cmd = BaseConstant.TX_ROLLBACK, version = 1.0, description = "withdraw deposit agent transaction validate 1.0")
//    @Parameter(parameterName = "chainId", parameterType = "int")
//    @Parameter(parameterName = ConsensusConstant.PARAM_BLOCK_HEADER_HEX, parameterType = "String")
//    @Parameter(parameterName = "txHexList", parameterType = "List<String>")
//    public Response rollback(Map<String,Object> params){
//        Result result = service.rollbackCmd(params);
//        if(result.isFailed()){
//            return failed(result.getErrorCode());
//        }
//        return success(result.getData());
//    }
//
//    /**
//     * 批量验证共识模块交易
//     * */
//    @CmdAnnotation(cmd = BaseConstant.TX_VALIDATOR, version = 1.0, description = "batch Verification Consensus Module Transaction 1.0")
//    @Parameter(parameterName = "chainId", parameterType = "int")
//    @Parameter(parameterName = "tx", parameterType = "String")
//    public Response batchValid(Map<String,Object> params){
//        Result result = service.batchValid(params);
//        if(result.isFailed()){
//            return failed(result.getErrorCode());
//        }
//        return success(result.getData());
//    }

    /**
     * 区块分叉记录
     */
    @CmdAnnotation(cmd = "cs_addEvidenceRecord", version = 1.0, description = "add evidence record 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @Parameter(parameterName = "blockHeader", parameterType = "String", parameterDes = "分叉区块头一")
    @Parameter(parameterName = "evidenceHeader", parameterType = "String", parameterDes = "分叉区块头二")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value",valueType = Boolean.class, description = "处理结果")
    }))
    public Response addEvidenceRecord(Map<String, Object> params) {
        Result result = service.addEvidenceRecord(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 双花交易记录
     */
    @CmdAnnotation(cmd = "cs_doubleSpendRecord", version = 1.0, description = "double spend transaction record 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @Parameter(parameterName = "block", parameterType = "String", parameterDes = "区块信息")
    @Parameter(parameterName = "tx", parameterType = "String",parameterDes = "分叉交易")
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value",valueType = Boolean.class, description = "处理结果")
    }))
    public Response doubleSpendRecord(Map<String, Object> params) {
        Result result = service.doubleSpendRecord(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询惩罚列表
     */
    @CmdAnnotation(cmd = "cs_getPublishList", version = 1.0, description = "query punish list 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @Parameter(parameterName = "address", parameterType = "String", parameterDes = "地址")
    @Parameter(parameterName = "type", parameterType = "int", parameterDes = "惩罚类型")
    @ResponseData(name = "返回值", description = "返回一个Map对象，包含两个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "redPunish",valueType = List.class, valueElement = String.class,  description = "获得的红牌列表"),
            @Key(name = "yellowPunish", valueType = List.class, valueElement = String.class, description = "获得的黄牌惩罚列表")
    }))
    public Response getPublishList(Map<String, Object> params) {
        Result result = service.getPublishList(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询全网共识信息
     */
    @CmdAnnotation(cmd = "cs_getWholeInfo", version = 1.0, description = "query the consensus information of the whole network 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @ResponseData(name = "返回值", responseType = @TypeDescriptor(value = WholeNetConsensusInfoDTO.class))
    public Response getWholeInfo(Map<String, Object> params) {
        Result result = service.getWholeInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询指定账户的共识信息
     */
    @CmdAnnotation(cmd = "cs_getInfo", version = 1.0, description = "query consensus information for specified accounts 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @Parameter(parameterName = "address", parameterType = "String", parameterDes = "账户地址")
    @ResponseData(name = "返回值", responseType = @TypeDescriptor(value = AccountConsensusInfoDTO.class))
    public Response getInfo(Map<String, Object> params) {
        Result result = service.getInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取当前轮次信息
     */
    @CmdAnnotation(cmd = "cs_getRoundInfo", version = 1.0, description = "get current round information 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @ResponseData(name = "返回值", responseType = @TypeDescriptor(value = MeetingRound.class))
    public Response getCurrentRoundInfo(Map<String, Object> params) {
        Result result = service.getCurrentRoundInfo(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 查询指定区块所在轮次
     */
    @CmdAnnotation(cmd = "cs_getRoundMemberList", version = 1.0, description = "get current round information 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @Parameter(parameterName = "extend", parameterType = "String", parameterDes = "区块头扩展信息")
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "packAddressList",valueType = List.class, valueElement = String.class,  description = "当前伦次出块地址列表")
    }))
    public Response getRoundMemberList(Map<String, Object> params) {
        Result result = service.getRoundMemberList(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 获取共模块识配置信息
     */
    @CmdAnnotation(cmd = "cs_getConsensusConfig", version = 1.0, description = "get consensus config")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "链id")
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "seedNodes", description = "种子节点列表"),
            @Key(name = "inflationAmount",valueType = Integer.class, description = "委托金额最大值"),
            @Key(name = "agentAssetId",valueType = Integer.class, description = "共识资产ID"),
            @Key(name = "agentChainId",valueType = Integer.class, description = "共识资产链ID"),
            @Key(name = "awardAssetId",valueType = Integer.class, description = "奖励资产ID（共识奖励为本链资产）"),
    }))
    @SuppressWarnings("unchecked")
    public Response getConsensusConfig(Map<String, Object> params) {
        Result<ConfigBean> result = service.getConsensusConfig(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 停止一条子链
     */
   /* @CmdAnnotation(cmd = "cs_stopChain", version = 1.0, description = "stop a chain 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int")
    public Response stopChain(Map<String, Object> params) {
        Result result = service.stopChain(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }*/

    /**
     * 运行一条子链
     */
/*    @CmdAnnotation(cmd = "cs_runChain", version = 1.0, description = "Running a sub chain 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int")
    public Response runChain(Map<String, Object> params) {
        Result result = service.runChain(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }*/

    /**
     * 启动主链
     */
/*    @CmdAnnotation(cmd = "cs_runMainChain", version = 1.0, description = "run main chain 1.0")
    @Parameter(parameterName = "chainId", parameterType = "int")
    public Response runMainChain(Map<String, Object> params) {
        Result result = service.runMainChain(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }*/
}
