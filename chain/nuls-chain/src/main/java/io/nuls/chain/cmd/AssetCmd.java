package io.nuls.chain.cmd;

import io.nuls.base.basic.AddressTool;
import io.nuls.chain.info.CmConstants;
import io.nuls.chain.model.dto.Asset;
import io.nuls.chain.model.tx.AssetDisableTransaction;
import io.nuls.chain.model.tx.AssetRegTransaction;
import io.nuls.chain.service.AssetService;
import io.nuls.chain.service.ChainService;
import io.nuls.chain.service.RpcService;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.data.ByteUtils;
import io.nuls.tools.thread.TimeService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author tangyi
 * @date 2018/11/9
 * @description
 */
@Component
public class AssetCmd extends BaseCmd {

    @Autowired
    private AssetService assetService;
    @Autowired
    private ChainService chainService;
    @Autowired
    private RpcService rpcService;

    @CmdAnnotation(cmd = "cm_asset", version = 1.0,
            description = "asset")
    public Response asset(List params) {
        Asset asset = assetService.getAsset(Long.valueOf(params.get(0).toString()));
        return success(asset);
    }

    /**
     * 资产注册
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "cm_assetReg", version = 1.0,
            description = "assetReg")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "symbol", parameterType = "array")
    @Parameter(parameterName = "name", parameterType = "String")
    @Parameter(parameterName = "initNumber", parameterType = "long", parameterValidRange = "[1,4294967295]", parameterValidRegExp = "")
    @Parameter(parameterName = "decimalPlaces", parameterType = "short", parameterValidRange = "[1,128]", parameterValidRegExp = "")
    @Parameter(parameterName = "address", parameterType = "String")
    public Response assetReg(Map params) {
        Asset asset = new Asset();
        asset.setChainId(Integer.valueOf(params.get("chainId").toString()));
        asset.setAssetId(TimeService.currentTimeMillis());
        asset.setSymbol((String) params.get("symbol"));
        asset.setName((String) params.get("name"));
        asset.setDepositNuls(Integer.valueOf(CmConstants.PARAM_MAP.get(CmConstants.ASSET_DEPOSITNULS)));
        asset.setInitNumber(Long.valueOf(params.get("initNumber").toString()));
        asset.setDecimalPlaces(Short.valueOf(params.get("decimalPlaces").toString()));
        asset.setAvailable(true);
        asset.setCreateTime(TimeService.currentTimeMillis());
        asset.setAddress(AddressTool.getAddress(String.valueOf(params.get("address"))));
        if(assetService.assetExist(asset))
        {
            return failed("A10005");
        }
        // 组装交易发送
        AssetRegTransaction assetRegTransaction = new AssetRegTransaction();
        try {
            assetRegTransaction.setTxData(asset.parseToTransaction());
            return failed("parseToTransaction fail");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //TODO:coindata 未封装
        boolean rpcReslt = rpcService.newTx(assetRegTransaction);
        if(rpcReslt) {
            return success("sent asset newTx");
        }else{
            return failed("sent asset fail");
        }
    }

    @CmdAnnotation(cmd = "cm_assetDisable", version = 1.0,
            description = "assetDisable")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "assetId", parameterType = "int", parameterValidRange = "[1,4294967295]", parameterValidRegExp = "")
    @Parameter(parameterName = "address", parameterType = "String")
    public Response assetDisable(Map params) {
        long assetId = Long.valueOf(params.get("assetId").toString());
        byte []address = AddressTool.getAddress(params.get("address").toString());
        //身份的校验，账户地址的校验
        Asset asset = assetService.getAsset(assetId);
        if (asset == null) {
            return failed("A10014");
        }
        if(!ByteUtils.arrayEquals(asset.getAddress(),address)){
            return failed("A10014");
        }
        AssetDisableTransaction assetDisableTransaction = new AssetDisableTransaction();
        try {
            assetDisableTransaction.setTxData(asset.parseToTransaction());
        } catch (IOException e) {
            e.printStackTrace();
            return failed("parseToTransaction fail");
        }
        //TODO:coindata 未封装
        boolean rpcReslt = rpcService.newTx(assetDisableTransaction);
        if(rpcReslt) {
            return success(asset);
        }else{
            return failed("sent tx fail");
        }
    }

}
