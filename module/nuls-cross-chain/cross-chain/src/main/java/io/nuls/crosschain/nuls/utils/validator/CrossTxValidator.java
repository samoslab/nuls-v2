package io.nuls.crosschain.nuls.utils.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.crosschain.nuls.constant.NulsCrossChainConfig;
import io.nuls.crosschain.nuls.constant.NulsCrossChainConstant;
import io.nuls.crosschain.nuls.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.nuls.model.bo.Chain;
import io.nuls.crosschain.nuls.rpc.call.ChainManagerCall;
import io.nuls.crosschain.nuls.srorage.ConvertFromCtxService;
import io.nuls.crosschain.nuls.srorage.NewCtxService;
import io.nuls.crosschain.nuls.utils.CommonUtil;
import io.nuls.crosschain.nuls.utils.TxUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.nuls.crosschain.nuls.constant.NulsCrossChainErrorCode.PAYEE_AND_PAYER_IS_THE_SAME_CHAIN;

/**
 * 跨链交易验证工具类
 * Transaction Verification Tool Class
 *
 * @author tag
 * 2019/4/15
 */
@Component
public class CrossTxValidator {
    @Autowired
    private NulsCrossChainConfig config;

    @Autowired
    private NewCtxService newCtxService;

    @Autowired
    private ConvertFromCtxService convertFromCtxService;

    /**
     * 验证交易
     * Verifying transactions
     *
     * @param chain 链ID/chain id
     * @param tx    交易/transaction info
     * @return boolean
     */
    public boolean validateTx(Chain chain, Transaction tx) throws NulsException, IOException {
        //判断这笔跨链交易是否属于本链
        CoinData coinData = tx.getCoinDataInstance();
        if (!coinDataValid(chain, coinData, tx.size())) {
            return false;
        }
        //如果本链为发起链且本链不为主链,则需要生成主网协议的跨链交易验证并验证签名
        int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
        if (!config.isMainNet() && chain.getChainId() == fromChainId) {
            NulsDigestData mainTxHash = convertFromCtxService.get(tx.getHash(), chain.getChainId());
            Transaction mainTx;
            if (mainTxHash == null) {
                mainTx = TxUtil.friendConvertToMain(chain, tx, null, NulsCrossChainConstant.TX_TYPE_CROSS_CHAIN);
                if (SignatureUtil.validateTransactionSignture(mainTx)) {
                    convertFromCtxService.save(tx.getHash(), mainTx.getHash(), chain.getChainId());
                    newCtxService.save(mainTx.getHash(), mainTx, chain.getChainId());
                }
            } else {
                mainTx = newCtxService.get(mainTxHash, chain.getChainId());
                if (!SignatureUtil.validateTransactionSignture(mainTx)) {
                    chain.getRpcLogger().error("签名验证失败");
                    throw new NulsException(NulsCrossChainErrorCode.SIGNATURE_ERROR);
                }
            }
        } else if (config.isMainNet() && chain.getChainId() != fromChainId) {
            //如果本链为中转链（即本链是主网且不是接收链）如果本链为主链且该跨链交易发起链不为主链，则需要验证发起链转出资产是否足够
            return ChainManagerCall.verifyCtxAsset(fromChainId, tx);
        }
        return true;
    }


    public boolean coinDataValid(Chain chain, CoinData coinData, int txSize)throws NulsException{
        return coinDataValid(chain,coinData,txSize,true);
    }

    /**
     * CoinData基础验证
     * CoinData basic validate
     *
     * @param chain
     * @param coinData
     * @param txSize
     */
    public boolean coinDataValid(Chain chain, CoinData coinData, int txSize, boolean isLocalCtx) throws NulsException {
        List<CoinFrom> coinFromList = coinData.getFrom();
        List<CoinTo> coinToList = coinData.getTo();
        if (coinFromList == null || coinFromList.isEmpty()
                || coinToList == null || coinToList.isEmpty()) {
            chain.getRpcLogger().error("转出方或转入方为空");
            throw new NulsException(NulsCrossChainErrorCode.COINFROM_NOT_FOUND);
        }
        int fromChainId = 0;
        int toChainId = 0;
        //跨链交易的from中地址必须是同一条链的地址，to中的地址必须是一条链地址
        for (CoinFrom coinFrom : coinFromList) {
            if (fromChainId == 0) {
                fromChainId = AddressTool.getChainIdByAddress(coinFrom.getAddress());
            }
            if (AddressTool.getChainIdByAddress(coinFrom.getAddress()) != fromChainId) {
                chain.getRpcLogger().error("跨链交易转出方存在多条链账户");
                throw new NulsException(NulsCrossChainErrorCode.CROSS_TX_PAYER_CHAIN_NOT_SAME);
            }
        }
        for (CoinTo coinTo : coinToList) {
            if (toChainId == 0) {
                toChainId = AddressTool.getChainIdByAddress(coinTo.getAddress());
            }
            if (AddressTool.getChainIdByAddress(coinTo.getAddress()) != toChainId) {
                chain.getRpcLogger().error("跨链交易转入方存在多条链账户");
                throw new NulsException(NulsCrossChainErrorCode.CROSS_TX_PAYEE_CHAIN_NOT_SAME);
            }
        }
        //from和to不能是同一个地址
        if (fromChainId == toChainId) {
            chain.getRpcLogger().error("跨链交易转出方和转入方是同一条链账户");
            throw new NulsException(PAYEE_AND_PAYER_IS_THE_SAME_CHAIN);
        }
        //查询这条跨链交易是否与本链相关
        int chainId = chain.getChainId();
        if (fromChainId != chainId && toChainId != chainId && !config.isMainNet()) {
            chain.getRpcLogger().error("该跨链交易不是本链跨链交易");
            throw new NulsException(NulsCrossChainErrorCode.NOT_BELONG_TO_CURRENT_CHAIN);
        }
        //如果本链不为发起链，验证CoinData中的主网主资产是否足够支付手续费
        if (chain.getChainId() != fromChainId || !isLocalCtx) {
            BigInteger feeTotalFrom = BigInteger.ZERO;
            for (CoinFrom coinFrom : coinFromList) {
                if (CommonUtil.isNulsAsset(coinFrom)) {
                    feeTotalFrom = feeTotalFrom.add(coinFrom.getAmount());
                }
            }
            BigInteger feeTotalTo = BigInteger.ZERO;
            for (CoinTo coinTo : coinToList) {
                if (CommonUtil.isNulsAsset(coinTo)) {
                    feeTotalTo = feeTotalTo.add(coinTo.getAmount());
                }
            }
            //本交易预计收取的手续费
            BigInteger targetFee = TransactionFeeCalculator.getCrossTxFee(txSize);
            //交易中已收取的手续费
            BigInteger actualFee = feeTotalFrom.subtract(feeTotalTo);
            if(BigIntegerUtils.isLessThan(actualFee, targetFee)){
                chain.getRpcLogger().error("手续费不足");
                throw new NulsException(NulsCrossChainErrorCode.INSUFFICIENT_FEE);
            }
        }
        return true;
    }
}
