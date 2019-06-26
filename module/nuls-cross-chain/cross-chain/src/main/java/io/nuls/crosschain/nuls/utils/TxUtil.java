package io.nuls.crosschain.nuls.utils;

import io.nuls.base.data.Coin;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.crosschain.nuls.constant.NulsCrossChainConfig;
import io.nuls.crosschain.nuls.constant.NulsCrossChainConstant;
import io.nuls.crosschain.nuls.model.bo.Chain;
import io.nuls.crosschain.nuls.rpc.call.AccountCall;
import io.nuls.crosschain.nuls.utils.manager.CoinDataManager;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * 交易工具类
 * Transaction Tool Class
 *
 * @author tag
 * 2019/4/15
 */
public class TxUtil {
    private static NulsCrossChainConfig config = SpringLiteContext.getBean(NulsCrossChainConfig.class);

    /**
     * 友链协议跨链交易转主网协议跨链交易
     * Friendly Chain Protocol Cross-Chain Transaction to Main Network Protocol Cross-Chain Transaction
     */
    public static Transaction friendConvertToMain(Chain chain, Transaction friendCtx, Map<String, String> signedAddressMap, int ctxType) throws NulsException, IOException {
        Transaction mainCtx = new Transaction(ctxType);
        mainCtx.setRemark(friendCtx.getRemark());
        mainCtx.setTime(friendCtx.getTime());
        mainCtx.setTxData(friendCtx.getHash().getBytes());
        //还原并重新结算CoinData
        CoinData realCoinData = friendCtx.getCoinDataInstance();
        restoreCoinData(realCoinData);
        mainCtx.setCoinData(realCoinData.serialize());

        //如果是新建跨链交易则直接用账户信息签名，否则从原始签名中获取签名
        TransactionSignature transactionSignature = new TransactionSignature();
        List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
        if (signedAddressMap != null && !signedAddressMap.isEmpty()) {
            for (Map.Entry<String, String> entry : signedAddressMap.entrySet()) {
                P2PHKSignature p2PHKSignature = AccountCall.signDigest(entry.getKey(), entry.getValue(), mainCtx.getHash().getBytes());
                p2PHKSignatures.add(p2PHKSignature);
            }
        } else {
            TransactionSignature originalSignature = new TransactionSignature();
            originalSignature.parse(friendCtx.getTransactionSignature(), 0);
            Set<String> pubKeySet = new HashSet<>();
            for (P2PHKSignature signature : originalSignature.getP2PHKSignatures()) {
                if (!pubKeySet.add(HexUtil.encode(signature.getPublicKey()))) {
                    p2PHKSignatures.add(signature);
                }
            }
        }
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        mainCtx.setTransactionSignature(transactionSignature.serialize());
        chain.getLogger().debug("本链协议跨链交易转主网协议跨链交易完成!");
        return mainCtx;
    }

    /**
     * 主网协议跨链交易转友链协议跨链交易
     * Main Network Protocol Cross-Chain Transaction Transfer Chain Protocol Cross-Chain Transaction
     */
    public static Transaction mainConvertToFriend(Transaction mainCtx, int ctxType) throws IOException {
        Transaction friendCtx = new Transaction(ctxType);
        friendCtx.setRemark(mainCtx.getRemark());
        friendCtx.setTime(mainCtx.getTime());
        friendCtx.setTxData(mainCtx.getHash().getBytes());
        friendCtx.setCoinData(mainCtx.getCoinData());
        return friendCtx;
    }

    /**
     * 还原本链协议CoinData
     * Restore the Chain Protocol CoinData
     * */
    private static void restoreCoinData(CoinData coinData){
        //资产与手续费 key:assetChainId_assetId   value:from中该资产 - to中该资产总额
        Map<String, BigInteger> assetMap = new HashMap<>(NulsCrossChainConstant.INIT_CAPACITY_16);
        String key;
        String mainKey = config.getMainChainId() +"_"+ config.getMainAssetId();
        for (Coin coin:coinData.getFrom()) {
            key = coin.getAssetsChainId()+"_"+coin.getAssetsId();
            if(assetMap.containsKey(key)){
                BigInteger amount = assetMap.get(key).add(coin.getAmount());
                assetMap.put(key, amount);
            }else{
                assetMap.put(key, coin.getAmount());
            }
        }
        for (Coin coin:coinData.getTo()) {
            key = coin.getAssetsChainId()+"_"+coin.getAssetsId();
            BigInteger amount = assetMap.get(key).subtract(coin.getAmount());
            assetMap.put(key, amount);
        }
        for (Map.Entry<String, BigInteger> entry:assetMap.entrySet()) {
            String entryKey = entry.getKey();
            if(entryKey.equals(mainKey)){
                continue;
            }
            BigInteger entryValue = entry.getValue();
            Iterator<CoinFrom> it = coinData.getFrom().iterator();
            while (it.hasNext()){
                Coin coin = it.next();
                key = coin.getAssetsChainId()+"_"+coin.getAssetsId();
                if(entryKey.equals(key)){
                    if(coin.getAmount().compareTo(entryValue) > 0){
                        coin.setAmount(coin.getAmount().subtract(entryValue));
                        break;
                    }else{
                        it.remove();
                        entryValue = entryValue.subtract(coin.getAmount());
                    }
                }
            }
        }
    }
}
