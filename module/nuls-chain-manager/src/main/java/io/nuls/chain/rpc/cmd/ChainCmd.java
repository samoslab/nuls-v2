package io.nuls.chain.rpc.cmd;


import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.chain.config.NulsChainConfig;
import io.nuls.chain.info.CmErrorCode;
import io.nuls.chain.info.CmRuntimeInfo;
import io.nuls.chain.info.RpcConstants;
import io.nuls.chain.model.dto.AccountBalance;
import io.nuls.chain.model.dto.AssetDto;
import io.nuls.chain.model.dto.ChainDto;
import io.nuls.chain.model.dto.RegChainDto;
import io.nuls.chain.model.po.Asset;
import io.nuls.chain.model.po.BlockChain;
import io.nuls.chain.model.tx.RegisterChainAndAssetTransaction;
import io.nuls.chain.rpc.call.RpcService;
import io.nuls.chain.service.AssetService;
import io.nuls.chain.service.ChainService;
import io.nuls.chain.util.LoggerUtil;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.util.NulsDateUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tangyi
 * @date 2018/11/7
 * @description
 */
@Component
public class ChainCmd extends BaseChainCmd {

    @Autowired
    private ChainService chainService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private RpcService rpcService;
    @Autowired
    NulsChainConfig nulsChainConfig;


    @CmdAnnotation(cmd = RpcConstants.CMD_CHAIN, version = 1.0,
            description = "查看链信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1-65535]", parameterDes = "资产链Id,取值区间[1-65535]"),
    })
    @ResponseData(description = "返回链信息", responseType = @TypeDescriptor(value = RegChainDto.class))
    public Response chain(Map params) {
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            BlockChain blockChain = chainService.getChain(chainId);
            if (blockChain == null) {
                return failed(CmErrorCode.ERROR_CHAIN_NOT_FOUND);
            } else {
                RegChainDto regChainDto = new RegChainDto();
                regChainDto.buildRegChainDto(blockChain);
                regChainDto.setSeeds(rpcService.getCrossChainSeeds());
                return success(regChainDto);
            }
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
            return failed(CmErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = RpcConstants.CMD_CHAIN_REG, version = 1.0,
            description = "链注册")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1-65535]", parameterDes = "资产链Id,取值区间[1-65535]"),
            @Parameter(parameterName = "chainName", parameterType = "String", parameterDes = "链名称"),
            @Parameter(parameterName = "addressType", parameterType = "int", parameterDes = "1 使用NULS框架构建的链 生态内，2生态外"),
            @Parameter(parameterName = "magicNumber", parameterType = "long", parameterDes = "网络魔法参数"),
            @Parameter(parameterName = "minAvailableNodeNum", parameterType = "int", parameterDes = "最小连接数"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterValidRange = "[1-65535]", parameterDes = "资产Id,取值区间[1-65535]"),
            @Parameter(parameterName = "symbol", parameterType = "String", parameterDes = "资产符号"),
            @Parameter(parameterName = "assetName", parameterType = "String", parameterDes = "资产名称"),
            @Parameter(parameterName = "initNumber", parameterType = "String", parameterDes = "资产初始值"),
            @Parameter(parameterName = "decimalPlaces", parameterType = "short", parameterDes = "资产小数点位数"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "创建交易的账户地址"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "账户密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象",
            responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                    @Key(name = "txHash", valueType = String.class, description = "交易hash值")
            })
    )
    public Response chainReg(Map params) {
        /* 发送到交易模块 (Send to transaction module) */
        Map<String, String> rtMap = new HashMap<>(1);
        try {
            /*判断链与资产是否已经存在*/
            /* 组装BlockChain (BlockChain object) */
            BlockChain blockChain = new BlockChain();
            blockChain.map2pojo(params);

            /* 组装Asset (Asset object) */
            /* 取消int assetId = seqService.createAssetId(blockChain.getChainId());*/
            Asset asset = new Asset();
            asset.map2pojo(params);
            asset.setChainId(blockChain.getChainId());
            asset.setDepositNuls(new BigInteger(nulsChainConfig.getAssetDepositNuls()));
            int rateToPercent = new BigDecimal(nulsChainConfig.getAssetDepositNulsDestroyRate()).multiply(BigDecimal.valueOf(100)).intValue();
            asset.setDestroyNuls(new BigInteger(nulsChainConfig.getAssetDepositNuls()).multiply(BigInteger.valueOf(rateToPercent)).divide(BigInteger.valueOf(100)));
            asset.setAvailable(true);
            BlockChain dbChain = chainService.getChain(blockChain.getChainId());
            if (null != dbChain) {
                return failed(CmErrorCode.ERROR_CHAIN_ID_EXIST);
            }
            if (chainService.hadExistMagicNumber(blockChain.getMagicNumber())) {
                return failed(CmErrorCode.ERROR_MAGIC_NUMBER_EXIST);
            }
            if (chainService.hadExistChainName(blockChain.getChainName())) {
                return failed(CmErrorCode.ERROR_CHAIN_NAME_EXIST);
            }
            /* 组装交易发送 (Send transaction) */
            Transaction tx = new RegisterChainAndAssetTransaction();
            tx.setTxData(blockChain.parseToTransaction(asset));
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            AccountBalance accountBalance = new AccountBalance(null, null);
            ErrorCode ldErrorCode = rpcService.getCoinData(String.valueOf(params.get("address")), accountBalance);
            if (null != ldErrorCode) {
                return failed(ldErrorCode);
            }
            CoinData coinData = super.getRegCoinData(asset, CmRuntimeInfo.getMainIntChainId(),
                    CmRuntimeInfo.getMainIntAssetId(), tx.size(),
                    accountBalance);
            tx.setCoinData(coinData.serialize());

            /* 判断签名是否正确 (Determine if the signature is correct),取主网的chainid进行签名 */
            ErrorCode acErrorCode = rpcService.transactionSignature(CmRuntimeInfo.getMainIntChainId(), (String) params.get("address"), (String) params.get("password"), tx);
            if (null != acErrorCode) {
                return failed(acErrorCode);
            }

            rtMap.put("txHash", tx.getHash().toHex());
            ErrorCode txErrorCode = rpcService.newTx(tx);
            if (null != txErrorCode) {
                return failed(txErrorCode);
            }
        } catch (NulsRuntimeException e) {
            LoggerUtil.logger().error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
            return failed(CmErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = RpcConstants.CMD_GET_CROSS_CHAIN_INFOS, version = 1.0,
            description = "获取跨链注册资产信息")

    @ResponseData(name = "返回值", description = "返回一个Map对象",
            responseType = @TypeDescriptor(value = Map.class, collectionElement = List.class, mapKeys = {
                    @Key(name = "chainInfos", valueType = List.class, valueElement = ChainDto.class, description = "资产信息列表")
            })
    )
    public Response getCrossChainInfos(Map params) {
        List<Map<String, Object>> chainInfos = new ArrayList<>();
        Map<String, Object> rtMap = new HashMap<>();
        List<ChainDto> rtChainList = new ArrayList<>();
        try {
            List<BlockChain> blockChains = chainService.getBlockList();
            for (BlockChain blockChain : blockChains) {
                ChainDto chainInfo = new ChainDto();
                chainInfo.setChainId(blockChain.getChainId());
                chainInfo.setChainName(blockChain.getChainName());
                chainInfo.setMinAvailableNodeNum(blockChain.getMinAvailableNodeNum());
                List<Asset> assets = assetService.getAssets(blockChain.getSelfAssetKeyList());
                List<AssetDto> rtAssetList = new ArrayList<>();
                for (Asset asset : assets) {
                    AssetDto assetDto = new AssetDto();
                    assetDto.setAssetId(asset.getAssetId());
                    assetDto.setSymbol(asset.getSymbol());
                    assetDto.setAssetName(asset.getAssetName());
                    assetDto.setUsable(asset.isAvailable());
                    assetDto.setDecimalPlaces(asset.getDecimalPlaces());
                    rtAssetList.add(assetDto);
                }
                chainInfo.setAssetInfoList(rtAssetList);
                rtChainList.add(chainInfo);
            }
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
        }
        rtMap.put("chainInfos", rtChainList);
        return success(rtMap);
    }
}
