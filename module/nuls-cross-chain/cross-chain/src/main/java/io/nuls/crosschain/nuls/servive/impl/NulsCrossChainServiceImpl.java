package io.nuls.crosschain.nuls.servive.impl;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.crosschain.base.constant.CommandConstant;
import io.nuls.crosschain.base.message.GetCtxStateMessage;
import io.nuls.crosschain.base.service.CrossChainService;
import io.nuls.crosschain.nuls.constant.NulsCrossChainConfig;
import io.nuls.crosschain.nuls.constant.NulsCrossChainConstant;
import io.nuls.crosschain.nuls.model.dto.input.CoinDTO;
import io.nuls.crosschain.nuls.model.po.SendCtxHashPo;
import io.nuls.crosschain.nuls.rpc.call.AccountCall;
import io.nuls.crosschain.nuls.rpc.call.ChainManagerCall;
import io.nuls.crosschain.nuls.rpc.call.NetWorkCall;
import io.nuls.crosschain.nuls.rpc.call.TransactionCall;
import io.nuls.crosschain.nuls.srorage.*;
import io.nuls.crosschain.nuls.utils.TxUtil;
import io.nuls.crosschain.nuls.utils.manager.ChainManager;
import io.nuls.crosschain.nuls.utils.manager.CoinDataManager;
import io.nuls.crosschain.nuls.model.bo.Chain;
import io.nuls.crosschain.nuls.model.dto.input.CrossTxTransferDTO;
import io.nuls.crosschain.nuls.utils.validator.CrossTxValidator;
import io.nuls.core.rpc.util.RPCUtil;
import io.nuls.core.rpc.util.TimeUtils;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Service;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;

import static io.nuls.crosschain.nuls.constant.NulsCrossChainErrorCode.*;
import static io.nuls.crosschain.nuls.constant.NulsCrossChainConstant.*;
import static io.nuls.crosschain.nuls.constant.ParamConstant.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨链模块默认接口实现类
 * @author tag
 * @date 2019/4/9
 */
@Service
public class NulsCrossChainServiceImpl implements CrossChainService {
    @Autowired
    private ChainManager chainManager;

    @Autowired
    private NulsCrossChainConfig config;

    @Autowired
    private CoinDataManager coinDataManager;

    @Autowired
    private CrossTxValidator txValidator;

    @Autowired
    private NewCtxService newCtxService;

    @Autowired
    private CommitedCtxService commitedCtxService;

    @Autowired
    private SendHeightService sendHeightService;

    @Autowired
    private CompletedCtxService completedCtxService;

    @Autowired
    private ConvertFromCtxService convertFromCtxService;

    @Autowired
    private CtxStateService ctxStateService;

    @Override
    @SuppressWarnings("unchecked")
    public Result createCrossTx(Map<String, Object> params) {
        if(params == null){
            return Result.getFailed(PARAMETER_ERROR);
        }
        CrossTxTransferDTO crossTxTransferDTO = JSONUtils.map2pojo(params, CrossTxTransferDTO.class);
        int chainId = crossTxTransferDTO.getChainId();
        if (chainId <= CHAIN_ID_MIN) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        Transaction tx = new Transaction(config.getCrossCtxType());
        try {
            tx.setRemark(StringUtils.bytes(crossTxTransferDTO.getRemark()));
            tx.setTime(TimeUtils.getCurrentTimeMillis());
            List<CoinFrom> coinFromList = coinDataManager.assemblyCoinFrom(chain, crossTxTransferDTO.getListFrom(), false);
            List<CoinTo> coinToList = coinDataManager.assemblyCoinTo(crossTxTransferDTO.getListTo(),chain);
            coinDataManager.verifyCoin(coinFromList, coinToList,chain);
            int txSize = tx.size();
            //如果为主链跨链交易中只存在原始跨链交易签名，如果不为主链，跨链交易签名列表中会包含主网协议跨链交易的签名列表
            if(config.isMainNet()){
                txSize += coinDataManager.getSignatureSize(coinFromList);
            }else{
                txSize += coinDataManager.getSignatureSize(coinFromList) * 2;
            }
            CoinData coinData = coinDataManager.getCoinData(chain, coinFromList, coinToList, txSize, true);
            tx.setCoinData(coinData.serialize());
            tx.setHash(NulsDigestData.calcDigestData(tx.serializeForHash()));
            //签名
            TransactionSignature transactionSignature = new TransactionSignature();
            List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
            List<String> signedAddressList = new ArrayList<>();
            Map<String,String> signedAddressMap = new HashMap<>(INIT_CAPACITY_8);
            for (CoinDTO coinDTO : crossTxTransferDTO.getListFrom()) {
                if(!signedAddressList.contains(coinDTO.getAddress())){
                    P2PHKSignature p2PHKSignature = AccountCall.signDigest(coinDTO.getAddress(), coinDTO.getPassword(), tx.getHash().getDigestBytes());
                    p2PHKSignatures.add(p2PHKSignature);
                    signedAddressList.add(coinDTO.getAddress());
                    signedAddressMap.put(coinDTO.getAddress(), coinDTO.getPassword());
                }
            }
            if(!txValidator.coinDataValid(chain, coinData, tx.size())){
                chain.getRpcLogger().error("跨链交易CoinData验证失败！");
                return Result.getFailed(COINDATA_VERIFY_FAIL);
            }
            //判断本链是友链还是主网，如果是友链则需要生成对应的主网协议跨链交易，如果为主网则直接将跨链交易发送给交易模块处理
            if(!config.isMainNet()){
                Transaction mainCtx = TxUtil.friendConvertToMain(chain,tx,signedAddressMap,TX_TYPE_CROSS_CHAIN);
                TransactionSignature mTransactionSignature = new TransactionSignature();
                mTransactionSignature.parse(mainCtx.getTransactionSignature(),0);
                p2PHKSignatures.addAll(mTransactionSignature.getP2PHKSignatures());
                if(!txValidator.coinDataValid(chain, mainCtx.getCoinDataInstance(), mainCtx.size(),false)){
                    chain.getRpcLogger().error("生成的主网协议跨链交易CoinData验证失败！");
                    return Result.getFailed(COINDATA_VERIFY_FAIL);
                }
                //保存mtx
                convertFromCtxService.save(tx.getHash(), mainCtx.getHash(), chainId);
                newCtxService.save(mainCtx.getHash(), mainCtx, chainId);
            }
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
            tx.setTransactionSignature(transactionSignature.serialize());
            //如果本链为主网，则创建的交易就是主网协议交易，直接保存
            if(config.isMainNet()){
                newCtxService.save(tx.getHash(), tx, chainId);
            }
            if(!TransactionCall.sendTx(chain, RPCUtil.encode(tx.serialize()))){
                chain.getRpcLogger().error("跨链交易发送交易模块失败");
                throw new NulsException(INTERFACE_CALL_FAILED);
            }
            Map<String, Object> result = new HashMap<>(2);
            result.put(TX_HASH, tx.getHash().getDigestHex());
            return Result.getSuccess(SUCCESS).setData(result);
        }catch (NulsException e){
            chain.getRpcLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }catch (IOException e) {
            Log.error(e);
            return Result.getFailed(SERIALIZE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result validCrossTx(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        String txStr = (String) params.get(TX);
        try {
            Transaction transaction = new Transaction();
            transaction.parse(RPCUtil.decode(txStr), 0);
            if(!txValidator.validateTx(chain, transaction)){
                chain.getRpcLogger().error("跨链交易验证失败");
                return Result.getFailed(TX_DATA_VALIDATION_ERROR);
            }
            Map<String, Object> validResult = new HashMap<>(2);
            validResult.put(VALUE, true);
            return Result.getSuccess(SUCCESS).setData(validResult);
        }catch (NulsException e) {
            chain.getRpcLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }catch (IOException e) {
            Log.error(e);
            return Result.getFailed(SERIALIZE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result commitCrossTx(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX_LIST) == null || params.get(PARAM_BLOCK_HEADER) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        List<String> txStrList = (List<String>)params.get(TX_LIST);
        String headerStr = (String) params.get(PARAM_BLOCK_HEADER);
        Map<String, Object> result = new HashMap<>(2);
        try {
            Map<NulsDigestData,Transaction> waitSendMap = new HashMap<>(INIT_CAPACITY_16);
            Map<NulsDigestData,Transaction> finishedMap = new HashMap<>(INIT_CAPACITY_16);
            List<NulsDigestData> hashList = new ArrayList<>();
            for (String txStr : txStrList) {
                Transaction ctx = new Transaction();
                ctx.parse(RPCUtil.decode(txStr),0);
                CoinData coinData = ctx.getCoinDataInstance();
                int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
                int toChainId = AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());
                //如果本链为发起链且本链不为主链，则需要找到主链协议跨链交易对应的主网协议跨链交易hash,然后找到对应的主网协议跨链交易
                NulsDigestData realCtxHash = ctx.getHash();
                if(chainId == fromChainId && chainId != config.getMainChainId()){
                    realCtxHash = convertFromCtxService.get(ctx.getHash(), chainId);
                    ctx = newCtxService.get(realCtxHash, chainId);
                }
                //如果本链为接收链则表示该跨链交易不需再广播给其他链，所以直接放入处理完成表中，否则放入已提交待广播表中
                if(chainId == toChainId){
                    if(!completedCtxService.save(realCtxHash, ctx, chainId) || !newCtxService.delete(realCtxHash, chainId)){
                        rollbackCtx(waitSendMap, finishedMap, chainId);
                        return Result.getFailed(TX_COMMIT_FAIL);
                    }
                    finishedMap.put(realCtxHash, ctx);
                }else{
                    hashList.add(realCtxHash);
                    //如果保存失败，则需要回滚已保存交易，直接返回
                    if(!commitedCtxService.save(realCtxHash, ctx, chainId) || !newCtxService.delete(realCtxHash, chainId)){
                        rollbackCtx(waitSendMap, finishedMap, chainId);
                        return Result.getFailed(TX_COMMIT_FAIL);
                    }
                    waitSendMap.put(realCtxHash, ctx);
                }
            }
            if(!hashList.isEmpty()){
                BlockHeader blockHeader = new BlockHeader();
                blockHeader.parse(RPCUtil.decode(headerStr), 0);
                //跨链交易被打包的高度
                long sendHeight = blockHeader.getHeight() + chain.getConfig().getSendHeight();
                SendCtxHashPo sendCtxHashPo = new SendCtxHashPo(hashList);
                if(!sendHeightService.save(sendHeight,sendCtxHashPo,chainId)){
                    rollbackCtx(waitSendMap, finishedMap, chainId);
                    return Result.getFailed(TX_COMMIT_FAIL);
                }
            }
            //如果本链为主网通知跨链管理模块发起链与接收链资产变更
            if(config.isMainNet()){
                ChainManagerCall.ctxAssetCirculateCommit(chainId,txStrList, headerStr);
            }
            result.put(VALUE ,true);
            return Result.getSuccess(SUCCESS).setData(result);
        }catch (NulsException e){
            chain.getRpcLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result rollbackCrossTx(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX_LIST) == null || params.get(PARAM_BLOCK_HEADER) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        List<String> txStrList = (List<String>)params.get(TX_LIST);
        String headerStr = (String) params.get(PARAM_BLOCK_HEADER);
        Map<String, Object> result = new HashMap<>(2);
        try {
            Map<NulsDigestData,Transaction> waitSendMap = new HashMap<>(INIT_CAPACITY_16);
            Map<NulsDigestData,Transaction> finishedMap = new HashMap<>(INIT_CAPACITY_16);
            for (String txStr : txStrList) {
                Transaction ctx = new Transaction();
                ctx.parse(RPCUtil.decode(txStr),0);
                CoinData coinData = ctx.getCoinDataInstance();
                int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
                int toChainId = AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());
                //如果本链为发起链且本链不为主链，则需要找到主链协议跨链交易对应的主网协议跨链交易hash,然后找到对应的主网协议跨链交易
                NulsDigestData realCtxHash = ctx.getHash();
                if(chainId == fromChainId && chainId != config.getMainChainId()){
                    realCtxHash = convertFromCtxService.get(ctx.getHash(), chainId);
                    ctx = commitedCtxService.get(realCtxHash, chainId);
                }
                if(chainId == toChainId){
                    if(completedCtxService.delete(realCtxHash, chainId) || newCtxService.save(realCtxHash, ctx, chainId)){
                        commitCtx(waitSendMap, finishedMap, chainId);
                        return Result.getFailed(TX_ROLLBACK_FAIL);
                    }
                    finishedMap.put(realCtxHash, ctx);
                }else{
                    if(commitedCtxService.delete(realCtxHash, chainId) || newCtxService.save(realCtxHash, ctx, chainId)){
                        commitCtx(waitSendMap, finishedMap, chainId);
                        return Result.getFailed(TX_ROLLBACK_FAIL);
                    }
                    waitSendMap.put(realCtxHash, ctx);
                }
            }
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.parse(RPCUtil.decode(headerStr), 0);
            //需要被清理的跨链交易高度
            long sendHeight = blockHeader.getHeight() + chain.getConfig().getSendHeight();
            if(!sendHeightService.delete(sendHeight, chainId)){
                return Result.getFailed(TX_ROLLBACK_FAIL);
            }
            //如果为主网通知跨链管理模块发起链与接收链资产变更
            if (config.isMainNet()){
                ChainManagerCall.ctxAssetCirculateRollback(chainId,txStrList, headerStr);
            }
            result.put(VALUE ,true);
            return Result.getSuccess(SUCCESS).setData(result);
        }catch (NulsException e){
            chain.getRpcLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result crossTxBatchValid(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX_LIST) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        List<String> txStrList = (List<String>)params.get(TX_LIST);
        List<String> txHashList = new ArrayList<>();
        for (String txStr:txStrList) {
            Transaction ctx = new Transaction();
            try {
                ctx.parse(RPCUtil.decode(txStr),0);
                if(!txValidator.validateTx(chain, ctx)){
                    txHashList.add(ctx.getHash().getDigestHex());
                }
            }catch (NulsException e){
                chain.getRpcLogger().error(e);
                txHashList.add(ctx.getHash().getDigestHex());
            }catch (IOException e) {
                Log.error(e);
                txHashList.add(ctx.getHash().getDigestHex());
            }
        }
        Map<String, Object> result = new HashMap<>(2);
        result.put(LIST,txHashList);
        return Result.getSuccess(SUCCESS).setData(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getCrossTxState(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX_HASH) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        String hashStr = (String)params.get(TX_HASH);
        Map<String, Object> result = new HashMap<>(2);
        byte[] hashBytes = HexUtil.decode(hashStr);
        //查看本交易是否已经存在查询处理成功记录，如果有直接返回，否则需向主网节点验证
        if(ctxStateService.get(hashBytes, chainId)){
            result.put(VALUE, true);
            return Result.getSuccess(SUCCESS).setData(result);
        }
        NulsDigestData requestHash = new NulsDigestData();
        try {
            requestHash.parse(hashBytes,0);
        }catch (Exception e){
            chain.getMessageLog().error(e);
            return Result.getFailed(DATA_PARSE_ERROR);
        }
        GetCtxStateMessage message = new GetCtxStateMessage();
        message.setRequestHash(requestHash);
        int linkedChainId = chainId;
        try {
            if(config.isMainNet()){
                Transaction ctx = completedCtxService.get(requestHash, chainId);
                if(ctx == null ){
                    result.put(VALUE, false);
                    return Result.getSuccess(SUCCESS).setData(result);
                }
                CoinData coinData = ctx.getCoinDataInstance();
                linkedChainId = AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());
            }
            NetWorkCall.broadcast(linkedChainId, message, CommandConstant.GET_CTX_STATE_MESSAGE,true);
            if(!chain.getCtxStateMap().containsKey(requestHash)){
                chain.getCtxStateMap().put(requestHash, new ArrayList<>());
            }
            result.put(VALUE, statisticsCtxState(chain,linkedChainId,requestHash));
            return Result.getSuccess(SUCCESS).setData(result);
        }catch (NulsException e){
            return Result.getFailed(e.getErrorCode());
        }
    }

    private void commitCtx(Map<NulsDigestData,Transaction> waitSendMap,Map<NulsDigestData,Transaction> finishedMap,int chainId){
        for (Map.Entry<NulsDigestData,Transaction> entry:waitSendMap.entrySet()) {
            newCtxService.delete(entry.getKey(), chainId);
            commitedCtxService.save(entry.getKey(), entry.getValue(), chainId);
        }
        for (Map.Entry<NulsDigestData,Transaction> entry:finishedMap.entrySet()) {
            newCtxService.delete(entry.getKey(), chainId);
            completedCtxService.save(entry.getKey(), entry.getValue(), chainId);
        }
    }

    private void rollbackCtx(Map<NulsDigestData,Transaction> waitSendMap,Map<NulsDigestData,Transaction> finishedMap,int chainId){
        for (Map.Entry<NulsDigestData,Transaction> entry:waitSendMap.entrySet()) {
            commitedCtxService.delete(entry.getKey(), chainId);
            newCtxService.save(entry.getKey(), entry.getValue(), chainId);
        }
        for (Map.Entry<NulsDigestData,Transaction> entry:finishedMap.entrySet()) {
            completedCtxService.delete(entry.getKey(), chainId);
            newCtxService.save(entry.getKey(), entry.getValue(), chainId);
        }
    }

    private boolean statisticsCtxState(Chain chain,int fromChainId,NulsDigestData requestHash){
        try {
            int linkedNode = NetWorkCall.getAvailableNodeAmount(fromChainId, true);
            int needSuccessCount = linkedNode*chain.getConfig().getByzantineRatio()/ NulsCrossChainConstant.MAGIC_NUM_100;
            int tryCount = 0;
            boolean statisticsResult = false;
            while (tryCount <= NulsCrossChainConstant.BYZANTINE_TRY_COUNT){
                if(chain.getCtxStateMap().get(requestHash).size() < needSuccessCount){
                    Thread.sleep(2000);
                    tryCount++;
                    continue;
                }
                statisticsResult = chain.statisticsCtxState(requestHash, needSuccessCount);
                if(statisticsResult || chain.getCtxStateMap().get(requestHash).size() >= linkedNode){
                    break;
                }
                Thread.sleep(2000);
                tryCount++;
            }
            return statisticsResult;
        }catch (Exception e){
            chain.getMessageLog().error(e);
            return false;
        }finally {
            chain.getCtxStateMap().remove(requestHash);
        }
    }
}
