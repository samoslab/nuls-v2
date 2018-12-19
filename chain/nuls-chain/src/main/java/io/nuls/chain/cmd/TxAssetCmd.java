package io.nuls.chain.cmd;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.chain.info.CmConstants;
import io.nuls.chain.info.CmErrorCode;
import io.nuls.chain.info.CmRuntimeInfo;
import io.nuls.chain.model.dto.Asset;
import io.nuls.chain.model.dto.BlockChain;
import io.nuls.chain.model.dto.ChainAsset;
import io.nuls.chain.model.dto.CoinDataAssets;
import io.nuls.chain.model.tx.AddAssetToChainTransaction;
import io.nuls.chain.service.AssetService;
import io.nuls.chain.service.ChainService;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.BigIntegerUtils;
import io.nuls.tools.data.ByteUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author lan
 * @date 2018/11/21
 */
@Component
public class TxAssetCmd extends BaseChainCmd {

    @Autowired
    private AssetService assetService;
    @Autowired
    private ChainService chainService;


    @CmdAnnotation(cmd = "cm_assetRegValidator", version = 1.0, description = "assetRegValidator")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "txHex", parameterType = "String")
    @Parameter(parameterName = "secondaryData", parameterType = "String")
    public Response assetRegValidator(Map params) {
        try {
            String txHex = String.valueOf(params.get("txHex"));
            Asset asset = buildAssetWithTxChain(txHex, new AddAssetToChainTransaction());

            if (assetService.assetExist(asset)) {
                return failed(CmErrorCode.ERROR_ASSET_ID_EXIST);
            }
            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(CmErrorCode.Err10002);
        }
    }


    @CmdAnnotation(cmd = "cm_assetRegCommit", version = 1.0, description = "assetRegCommit")
    @Parameter(parameterName = "txHex", parameterType = "String")
    @Parameter(parameterName = "secondaryData", parameterType = "String")
    public Response assetRegCommit(Map params) {
        try {
            Response cmdResponse = assetRegValidator(params);
            if (cmdResponse.isSuccess()) {
                return cmdResponse;
            }
            String txHex = String.valueOf(params.get("txHex"));
            Asset asset = buildAssetWithTxChain(txHex, new AddAssetToChainTransaction());

            assetService.registerAsset(asset);
        } catch (Exception e) {
            Log.error(e);
            return failed(CmErrorCode.Err10002);
        }

        return success();
    }


    @CmdAnnotation(cmd = "cm_assetRegRollback", version = 1.0, description = "assetRegRollback")
    @Parameter(parameterName = "txHex", parameterType = "String")
    @Parameter(parameterName = "secondaryData", parameterType = "String")
    public Response assetRegRollback(Map params) {
        try {
            String txHex = String.valueOf(params.get("txHex"));
            Asset asset = buildAssetWithTxChain(txHex, new AddAssetToChainTransaction());
            assetService.registerAssetRollback(asset);
            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    /**
     * 资产注销校验
     */
    @CmdAnnotation(cmd = "cm_assetDisableValidator", version = 1.0, description = "assetDisableValidator")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "txHex", parameterType = "String")
    @Parameter(parameterName = "secondaryData", parameterType = "String")
    public Response assetDisableValidator(Map params) {
        String txHex = String.valueOf(params.get("txHex"));
        Asset asset = buildAssetWithTxChain(txHex, new AddAssetToChainTransaction());
        try {
            Asset dbAsset = assetService.getAsset(CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId()));

            if (!ByteUtils.arrayEquals(asset.getAddress(), dbAsset.getAddress())) {
                return failed(CmErrorCode.ERROR_ADDRESS_ERROR);
            }
            if (!asset.getTxHash().equalsIgnoreCase(dbAsset.getTxHash())) {
                return failed(CmErrorCode.A10017);
            }
            if (asset.getChainId() != dbAsset.getChainId()) {
                return failed(CmErrorCode.ERROR_CHAIN_ASSET_NOT_MATCH);
            }
            ChainAsset chainAsset = assetService.getChainAsset(asset.getChainId(), dbAsset);
            BigDecimal initAsset = new BigDecimal(chainAsset.getInitNumber());
            BigDecimal inAsset = new BigDecimal(chainAsset.getInNumber());
            BigDecimal outAsset = new BigDecimal(chainAsset.getOutNumber());
            BigDecimal currentNumber = initAsset.add(inAsset).subtract(outAsset);
            double actual = currentNumber.divide(initAsset, 8, RoundingMode.HALF_DOWN).doubleValue();
            double config = Double.parseDouble(CmConstants.PARAM_MAP.get(CmConstants.ASSET_RECOVERY_RATE));
            if (actual < config) {
                return failed(CmErrorCode.ERROR_ASSET_RECOVERY_RATE);
            }
            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    /**
     * 资产注销提交
     */
    @CmdAnnotation(cmd = "cm_assetDisableCommit", version = 1.0,
            description = "assetDisableCommit")
    @Parameter(parameterName = "txHex", parameterType = "String")
    @Parameter(parameterName = "secondaryData", parameterType = "String")
    public Response assetDisableCommit(Map params) {
        String txHex = String.valueOf(params.get("txHex"));
        Asset asset = buildAssetWithTxChain(txHex, new AddAssetToChainTransaction());
        try {
            assetService.setStatus(CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId()), false);
            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    /**
     * 链注销回滚
     */
    @CmdAnnotation(cmd = "cm_assetDisableRollback", version = 1.0, description = "assetDisableRollback")
    @Parameter(parameterName = "txHex", parameterType = "String")
    @Parameter(parameterName = "secondaryData", parameterType = "String")
    public Response assetDisableRollback(Map params) {
        String txHex = String.valueOf(params.get("txHex"));
        Asset asset = buildAssetWithTxChain(txHex, new AddAssetToChainTransaction());
        try {
            /*判断资产是否可用*/
            Asset dbAsset = assetService.getAsset(CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId()));
            if (null == dbAsset || dbAsset.isAvailable()) {
                return failed(CmErrorCode.ERROR_ASSET_NOT_EXIST);
            }
            if (!dbAsset.getTxHash().equalsIgnoreCase(asset.getTxHash())) {
                return failed(CmErrorCode.ERROR_ASSET_NOT_EXIST);
            }
            assetService.setStatus(CmRuntimeInfo.getAssetKey(asset.getChainId(), asset.getAssetId()), true);
            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    private List<CoinDataAssets> getChainAssetList(String coinDataHex) throws NulsException {
        List<CoinDataAssets> list = new ArrayList<>();

        int fromChainId = 0;
        int toChainId = 0;
        Map<String, BigInteger> fromAssetMap = new HashMap<>(1);
        Map<String, BigInteger> toAssetMap = new HashMap<>(1);

        // 打造CoinData
        byte[] coinDataByte = HexUtil.hexToByte(coinDataHex);
        CoinData coinData = new CoinData();
        coinData.parse(coinDataByte, 0);

        // 从CoinData中取出from的资产信息，放入Map中（同类型相加）
        List<CoinFrom> listFrom = coinData.getFrom();
        for (CoinFrom coinFrom : listFrom) {
            fromChainId = AddressTool.getChainIdByAddress(coinFrom.getAddress());
            String assetKey = CmRuntimeInfo.getAssetKey(coinFrom.getAssetsChainId(), coinFrom.getAssetsId());
            BigInteger amount = coinFrom.getAmount();
            BigInteger current = fromAssetMap.get(assetKey);
            if (current != null) {
                amount = amount.add(current);
            }
            fromAssetMap.put(assetKey, amount);
        }

        // 从CoinData中取出to的资产信息，放入Map中（同类型相加）
        List<CoinTo> listTo = coinData.getTo();
        for (CoinTo coinTo : listTo) {
            toChainId = AddressTool.getChainIdByAddress(coinTo.getAddress());
            int assetChainId = coinTo.getAssetsChainId();
            int assetId = coinTo.getAssetsId();
            String assetKey = CmRuntimeInfo.getAssetKey(assetChainId, assetId);
            BigInteger amount = coinTo.getAmount();
            BigInteger current = toAssetMap.get(assetKey);
            if (current != null) {
                amount = amount.add(current);
            }
            toAssetMap.put(assetKey, amount);
        }

        CoinDataAssets fromCoinDataAssets = new CoinDataAssets();
        fromCoinDataAssets.setChainId(fromChainId);
        fromCoinDataAssets.setAssetsMap(fromAssetMap);
        list.add(fromCoinDataAssets);
        CoinDataAssets toCoinDataAssets = new CoinDataAssets();
        toCoinDataAssets.setChainId(toChainId);
        toCoinDataAssets.setAssetsMap(toAssetMap);
        list.add(toCoinDataAssets);
        return list;

    }

    private Response assetCirculateValidator(int fromChainId, int toChainId, Map<String, BigInteger> fromAssetMap, Map<String, BigInteger> toAssetMap) {
        try {
            BlockChain fromChain = chainService.getChain(fromChainId);
            BlockChain toChain = chainService.getChain(toChainId);
            if (fromChainId == toChainId) {
                Log.error("fromChain ==  toChain is not cross tx" + fromChain);
                return failed("fromChain ==  toChain is not cross tx");
            }
            if (fromChainId != 0 && fromChain.isDelete()) {
                Log.info("fromChain is delete,chainId=" + fromChain.getChainId());
                return failed("fromChain is delete");
            }
            if (toChainId != 0 && toChain.isDelete()) {
                Log.info("toChain is delete,chainId=" + fromChain.getChainId());
                return failed("toChain is delete");
            }
            //获取链内 资产 状态是否正常
            Set<String> toAssets = toAssetMap.keySet();
            for (Object toAsset : toAssets) {
                String assetKey = toAsset.toString();
                Asset asset = assetService.getAsset(assetKey);
                if (null == asset || !asset.isAvailable()) {
                    return failed("asset is not exsit");
                }
            }
            //校验from 资产是否足够
            Set<String> fromAssets = fromAssetMap.keySet();
            for (Object fromAsset : fromAssets) {
                String assetKey = fromAsset.toString();
                Asset asset = assetService.getAsset(assetKey);
                if (null == asset || !asset.isAvailable()) {
                    return failed("asset is not exsit");
                }
                ChainAsset chainAsset = assetService.getChainAsset(fromChainId, asset);
                BigDecimal currentAsset = new BigDecimal(chainAsset.getInitNumber()).add(new BigDecimal(chainAsset.getInNumber())).subtract(new BigDecimal(chainAsset.getOutNumber()));
                if (currentAsset.subtract(new BigDecimal(fromAssetMap.get(assetKey))).doubleValue() < 0) {
                    return failed("asset is not enough");
                }
            }
            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    /**
     * 跨链流通校验
     */
    @CmdAnnotation(cmd = "cm_assetCirculateValidator", version = 1.0, description = "assetCirculateValidator")
    @Parameter(parameterName = "coinDatas", parameterType = "String")
    public Response assetCirculateValidator(Map params) {
        //提取 从哪条链 转 哪条链，是否是跨链，链 手续费共多少？
        try {
            String coinDataHex = String.valueOf(params.get("coinDatas"));
            List<CoinDataAssets> list = getChainAssetList(coinDataHex);
            CoinDataAssets fromCoinDataAssets = list.get(0);
            CoinDataAssets toCoinDataAssets = list.get(1);
            int fromChainId = fromCoinDataAssets.getChainId();
            int toChainId = toCoinDataAssets.getChainId();
            Map<String, BigInteger> fromAssetMap = fromCoinDataAssets.getAssetsMap();
            Map<String, BigInteger> toAssetMap = toCoinDataAssets.getAssetsMap();
            return assetCirculateValidator(fromChainId, toChainId, fromAssetMap, toAssetMap);
        } catch (NulsException e) {
            e.printStackTrace();
        }
        return failed(CmErrorCode.Err10002);
    }

    /**
     * 跨链流通提交
     */
    @CmdAnnotation(cmd = "cm_assetCirculateCommit", version = 1.0, description = "assetCirculateCommit")
    @Parameter(parameterName = "coinDatas", parameterType = "String")
    public Response assetCirculateCommit(Map params) {
        //A链转B链资产X，数量N ;A链X资产减少N, B链 X资产 增加N。
        try {
            String coinDataHex = String.valueOf(params.get("coinDatas"));
            List<CoinDataAssets> list = getChainAssetList(coinDataHex);
            CoinDataAssets fromCoinDataAssets = list.get(0);
            CoinDataAssets toCoinDataAssets = list.get(1);
            int fromChainId = fromCoinDataAssets.getChainId();
            int toChainId = toCoinDataAssets.getChainId();
            Map<String, BigInteger> fromAssetMap = fromCoinDataAssets.getAssetsMap();
            Map<String, BigInteger> toAssetMap = toCoinDataAssets.getAssetsMap();
            Response response = assetCirculateValidator(fromChainId, toChainId, fromAssetMap, toAssetMap);
            if (!response.isSuccess()) {
                return response;
            }
            //from 的处理
            Set<String> assetKeys = fromAssetMap.keySet();
            for (String assetKey : assetKeys) {
                ChainAsset fromChainAsset = assetService.getChainAsset(fromChainId, assetKey);
                BigDecimal currentAsset = new BigDecimal(fromChainAsset.getOutNumber()).add(new BigDecimal(fromAssetMap.get(assetKey)));
                fromChainAsset.setOutNumber(BigIntegerUtils.stringToBigInteger(currentAsset.toString()));
                assetService.saveOrUpdateChainAsset(fromChainId, fromChainAsset);
            }
            if (!isMainChain(toChainId)) {
                //toChainId == nuls chain  需要进行跨外链的 手续费在coinBase里增加。

                //提取toChainId的 手续费资产，如果存将手续费放入外链给的回执，也取消 外链手续费增加。
                String mainAssetKey = CmRuntimeInfo.getMainAsset();
                BigInteger allFromMainAmount = fromAssetMap.get(mainAssetKey);
                BigInteger allToMainAmount = toAssetMap.get(mainAssetKey);
                BigInteger feeAmount = allFromMainAmount.subtract(allToMainAmount)
                        .multiply(new BigInteger("4").divide(new BigInteger("10")));
                if (null != toAssetMap.get(mainAssetKey)) {
                    feeAmount = feeAmount.add(toAssetMap.get(mainAssetKey));
                }
                toAssetMap.put(mainAssetKey, feeAmount);
            }
            //to 的处理
            Set<String> toAssetKeys = toAssetMap.keySet();
            for (String toAssetKey : toAssetKeys) {
                ChainAsset toChainAsset = assetService.getChainAsset(toChainId, toAssetKey);
                if (null == toChainAsset) {
                    //链下加资产，资产下增加链
                    BlockChain toChain = chainService.getChain(toChainId);
                    Asset asset = assetService.getAsset(CmRuntimeInfo.getMainAsset());
                    toChain.addCirculateAssetId(CmRuntimeInfo.getMainAsset());
                    asset.addChainId(toChainId);
                    chainService.updateChain(toChain);
                    assetService.updateAsset(asset);
                    //更新资产
                    toChainAsset = new ChainAsset();
                    toChainAsset.setChainId(asset.getChainId());
                    toChainAsset.setAssetId(asset.getAssetId());
                    toChainAsset.setInNumber(toAssetMap.get(toAssetKey));
                } else {
                    BigDecimal inAsset = new BigDecimal(toChainAsset.getInNumber());
                    BigDecimal inNumberBigDec = new BigDecimal(toAssetMap.get(toAssetKey)).add(inAsset);
                    toChainAsset.setInNumber(BigIntegerUtils.stringToBigInteger(inNumberBigDec.toString()));
                }
                assetService.saveOrUpdateChainAsset(toChainId, toChainAsset);
            }
            return failed(CmErrorCode.Err10002);
        } catch (Exception e) {
            Log.error(e);
            return failed(e.getMessage());
        }
    }

    /**
     * 跨链流通回滚
     */
    @CmdAnnotation(cmd = "cm_assetCirculateRollBack", version = 1.0, description = "assetCirculateRollBack")
    @Parameter(parameterName = "coinDatas", parameterType = "String")
    public Response assetCirculateRollBack(Map params) {
        //交易回滚，from的加，to的减
        //TODO:
        return success();
    }
}
