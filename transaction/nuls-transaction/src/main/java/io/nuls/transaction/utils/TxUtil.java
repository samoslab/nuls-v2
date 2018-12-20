/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.transaction.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.CrossTxData;
import io.nuls.transaction.model.po.TransactionPO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2018-12-05
 */
public class TxUtil {


    public static CrossTxData getCrossTxData(Transaction tx) throws NulsException{
        if(null == tx){
            throw new NulsException(TxErrorCode.TX_NOT_EXIST);
        }
        CrossTxData crossTxData = new CrossTxData();
        try {
            crossTxData.parse(new NulsByteBuffer(tx.getTxData()));
            return crossTxData;
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_ERROR);
        }
    }

    public static CoinData getCoinData(Transaction tx) throws NulsException {
        if(null == tx){
            throw new NulsException(TxErrorCode.TX_NOT_EXIST);
        }
        try {
            return tx.getCoinDataInstance();
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_COINDATA_ERROR);
        }
    }

    public static Transaction getTransaction(byte[] txBytes) throws NulsException {
        if(null == txBytes || txBytes.length == 0){
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
        try {
            return Transaction.getInstance(txBytes);
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_TX_ERROR);
        }
    }

    public static Transaction getTransaction(String hex) throws NulsException {
        if(StringUtils.isBlank(hex)){
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
       return getTransaction(HexUtil.decode(hex));
    }

    public static MultiSigAccount getMultiSigAccount(String hex) throws NulsException{
        if(StringUtils.isBlank(hex)){
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
        MultiSigAccount multiSigAccount = new MultiSigAccount();
        try {
            multiSigAccount.parse(new NulsByteBuffer(HexUtil.decode(hex)));
            return multiSigAccount;
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_ERROR);
        }
    }

    public static boolean isNulsAsset(Coin coin) {
        return isNulsAsset(coin.getAssetsChainId(), coin.getAssetsId());
    }

    public static boolean isNulsAsset(int chainId, int assetId) {
        if (chainId == TxConstant.NULS_CHAINID
                && assetId == TxConstant.NULS_CHAIN_ASSETID) {
            return true;
        }
        return false;
    }

    public static boolean isChainAssetExist(Chain chain, Coin coin) {
        if(chain.getConfig().getChainId() == coin.getAssetsChainId() &&
                chain.getConfig().getAssetsId() == coin.getAssetsId()){
            return true;
        }
        return false;
    }

    public static boolean assetExist(int chainId, int assetId) {
        //todo 查资产是否存在
        return true;
    }

    public static byte[] getNonce(byte[] address, int chainId, int assetId) throws NulsException {
        //todo 查nonce
        byte[] nonce = new byte[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
        return nonce;
    }

    public static BigInteger getBalance(byte[] address, int chainId, int assetId) throws NulsException {
        //todo 查余额;
        return new BigInteger("10000");
    }

    public static String getPrikey(String address, String password) throws NulsException {
        //todo 查私钥;
        int chainId = AddressTool.getChainIdByAddress(address);
        return "";
    }

    public static MultiSigAccount getMultiSigAccount(byte[] multiSignAddress) throws NulsException {
        String address = AddressTool.getStringAddressByBytes(multiSignAddress);
        //todo 获取多签账户
        return new MultiSigAccount();
    }


    public static boolean verifyCoinData(Chain chain, String txHex){
        //todo 验证CoinData
        /*try {

            return true;
        } catch (NulsException e){
            chain.getLogger().info(e.getErrorCode().getMsg(), e.fillInStackTrace());
            return false;
        }*/
        return true;
    }
    public static boolean verifyCoinData(Chain chain, Transaction tx){
        //todo 验证CoinData
        try {
            return verifyCoinData(chain, tx.hex());
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    public static boolean txValidator(Chain chain, String cmd, String txHex){
        //todo 调用交易验证器
        return true;
    }

    public static boolean txsModuleValidators(Chain chain, Map<String, List<String>> map) {
        //todo 调用交易模块统一验证器 批量
        boolean rs = true;
        for(Map.Entry<String, List<String>> entry : map.entrySet()){
            List<String> list = txModuleValidator(chain, entry.getKey(), entry.getValue());
            if(list.size() > 0){
                rs = false;
                break;
            }
        }
        return rs;
    }

    /**
     * 统一验证返回被干掉的交易hash
     * @param moduleValidator
     * @param txHexList
     * @return
     */
    public static List<String> txModuleValidator(Chain chain, String moduleValidator, List<String> txHexList) {
        //todo 调用交易模块统一验证器
        return new ArrayList<>();
    }


    public static List<TransactionPO> tx2PO(Transaction tx) throws NulsException{
        List<TransactionPO> list = new ArrayList<>();
        if(null == tx.getCoinData()){
            return list;
        }
        CoinData coinData = tx.getCoinDataInstance();
        if(coinData.getFrom() != null){
            TransactionPO transactionPO = null;
            for(CoinFrom coinFrom : coinData.getFrom()){
                transactionPO = new TransactionPO();
                transactionPO.setAddress(AddressTool.getStringAddressByBytes(coinFrom.getAddress()));
                transactionPO.setHash(tx.getHash().getDigestHex());
                transactionPO.setType(tx.getType());
                transactionPO.setAssetChainId(coinFrom.getAssetsChainId());
                transactionPO.setAssetId(coinFrom.getAssetsId());
                transactionPO.setAmount(coinFrom.getAmount());
                // 0普通交易，-1解锁金额交易（退出共识，退出委托）
                byte locked = coinFrom.getLocked();
                int state = 0;
                if(locked == -1){
                    state = 3;
                }
                transactionPO.setState(state);
                list.add(transactionPO);
            }
        }
        if(coinData.getTo() != null){
            TransactionPO transactionPO = null;
            for(CoinTo coinTo : coinData.getTo()){
                transactionPO = new TransactionPO();
                transactionPO.setAddress(AddressTool.getStringAddressByBytes(coinTo.getAddress()));
                transactionPO.setAssetChainId(coinTo.getAssetsChainId());
                transactionPO.setAssetId(coinTo.getAssetsId());
                transactionPO.setAmount(coinTo.getAmount());
                transactionPO.setHash(tx.getHash().getDigestHex());
                transactionPO.setType(tx.getType());
                // 解锁高度或解锁时间，-1为永久锁定
                Long lockTime = coinTo.getLockTime();
                int state = 1;
                if(lockTime != 0){
                    state = 2;
                }
                transactionPO.setState(state);
                list.add(transactionPO);
            }
        }
        return list;
    }


}
