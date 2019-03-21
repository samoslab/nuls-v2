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
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.logback.NulsLogger;
import io.nuls.tools.model.DateUtils;
import io.nuls.tools.model.StringUtils;
import io.nuls.transaction.constant.TxConfig;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.TxManager;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.po.TransactionPO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static io.nuls.transaction.utils.LoggerUtil.Log;

/**
 * @author: Charlie
 * @date: 2018-12-05
 */
public class TxUtil {

    private static TxConfig txConfig = SpringLiteContext.getBean(TxConfig.class);

    public static CoinData getCoinData(Transaction tx) throws NulsException {
        if (null == tx) {
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
        if (null == txBytes || txBytes.length == 0) {
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
        if (StringUtils.isBlank(hex)) {
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
        return getTransaction(HexUtil.decode(hex));
    }

    public static <T> T getInstance(byte[] bytes, Class<? extends BaseNulsData> clazz) throws NulsException {
        if (null == bytes || bytes.length == 0) {
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
        try {
            BaseNulsData baseNulsData = clazz.getDeclaredConstructor().newInstance();
            baseNulsData.parse(new NulsByteBuffer(bytes));
            return (T) baseNulsData;
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            Log.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_ERROR);
        }
    }

    public static <T> T getInstance(String hex, Class<? extends BaseNulsData> clazz) throws NulsException {
        if (StringUtils.isBlank(hex)) {
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
        return getInstance(HexUtil.decode(hex), clazz);
    }


    public static boolean isNulsAsset(Coin coin) {
        return isNulsAsset(coin.getAssetsChainId(), coin.getAssetsId());
    }

    public static boolean isNulsAsset(int chainId, int assetId) {

        if (chainId == txConfig.getMainChainId()
                && assetId == txConfig.getMainAssetId()) {
            return true;
        }
        return false;
    }

    public static boolean isChainAssetExist(Chain chain, Coin coin) {
        if (chain.getConfig().getChainId() == coin.getAssetsChainId() &&
                chain.getConfig().getAssetId() == coin.getAssetsId()) {
            return true;
        }
        return false;
    }

    public static List<TransactionPO> tx2PO(Chain chain, Transaction tx) throws NulsException {
        List<TransactionPO> list = new ArrayList<>();
        if (null == tx.getCoinData()) {
            return list;
        }
        CoinData coinData = tx.getCoinDataInstance();
        if (tx.getType() == TxConstant.TX_TYPE_YELLOW_PUNISH) {
            YellowPunishData punishData = new YellowPunishData();
            punishData.parse(tx.getTxData(), 0);
            for (byte[] address : punishData.getAddressList()) {
                TransactionPO transactionPO = new TransactionPO();
                transactionPO.setAddress(AddressTool.getStringAddressByBytes(address));
                transactionPO.setAssetChainId(chain.getConfig().getChainId());
                transactionPO.setAssetId(chain.getConfig().getAssetId());
                transactionPO.setAmount(BigInteger.ZERO);
                transactionPO.setHash(tx.getHash().getDigestHex());
                transactionPO.setType(tx.getType());
                transactionPO.setState(5);
                transactionPO.setTime(tx.getTime());
                list.add(transactionPO);
            }
        } else if (tx.getType() == TxConstant.TX_TYPE_RED_PUNISH) {
            RedPunishData punishData = new RedPunishData();
            punishData.parse(tx.getTxData(), 0);
            TransactionPO transactionPO = new TransactionPO();
            transactionPO.setAddress(AddressTool.getStringAddressByBytes(punishData.getAddress()));
            transactionPO.setAssetChainId(chain.getConfig().getChainId());
            transactionPO.setAssetId(chain.getConfig().getAssetId());
            transactionPO.setAmount(BigInteger.ZERO);
            transactionPO.setHash(tx.getHash().getDigestHex());
            transactionPO.setType(tx.getType());
            transactionPO.setState(3);
            transactionPO.setTime(tx.getTime());
            list.add(transactionPO);

        } else if (TxManager.isUnSystemSmartContract(chain, tx.getType())) {
            TransactionPO transactionPO = new TransactionPO();
            transactionPO.setAddress(extractContractAddress(tx.getTxData()));
            transactionPO.setAssetChainId(chain.getConfig().getChainId());
            transactionPO.setAssetId(chain.getConfig().getAssetId());
            transactionPO.setAmount(BigInteger.ZERO);
            transactionPO.setHash(tx.getHash().getDigestHex());
            transactionPO.setType(tx.getType());
            transactionPO.setState(4);
            transactionPO.setTime(tx.getTime());
            list.add(transactionPO);
        } else {
            if (coinData.getFrom() != null
                    && tx.getType() != TxConstant.TX_TYPE_COINBASE
                    && tx.getType() != TxConstant.TX_TYPE_REGISTER_AGENT
                    && tx.getType() != TxConstant.TX_TYPE_JOIN_CONSENSUS
                    && tx.getType() != TxConstant.TX_TYPE_CANCEL_DEPOSIT
                    && tx.getType() != TxConstant.TX_TYPE_STOP_AGENT) {
                TransactionPO transactionPO = null;
                for (CoinFrom coinFrom : coinData.getFrom()) {
                    transactionPO = new TransactionPO();
                    transactionPO.setAddress(AddressTool.getStringAddressByBytes(coinFrom.getAddress()));
                    transactionPO.setHash(tx.getHash().getDigestHex());
                    transactionPO.setType(tx.getType());
                    transactionPO.setAssetChainId(coinFrom.getAssetsChainId());
                    transactionPO.setAssetId(coinFrom.getAssetsId());
                    transactionPO.setAmount(coinFrom.getAmount());
                    // 0普通交易，(-1:按时间解锁, 1:按高度解锁)解锁金额交易（退出共识，退出委托）
                    byte locked = coinFrom.getLocked();
                    int state = 0;
                    if (locked == -1 || locked == 1) {
                        //解锁金额交易
                        break;
                    }
                    transactionPO.setState(state);
                    transactionPO.setTime(tx.getTime());
                    list.add(transactionPO);
                }
            }
            if (coinData.getTo() != null) {
                TransactionPO transactionPO = null;
                for (CoinTo coinTo : coinData.getTo()) {
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
                    if (lockTime != 0) {
                        state = 2;
                    }
                    transactionPO.setState(state);
                    transactionPO.setTime(tx.getTime());
                    list.add(transactionPO);
                }
            }
        }
        return list;
    }

    /**
     * 从智能合约TxData中获取地址
     *
     * @param txData
     * @return
     */
    public static String extractContractAddress(byte[] txData) {
        if (txData == null) {
            return null;
        }
        int length = txData.length;
        if (length < Address.ADDRESS_LENGTH * 2) {
            return null;
        }
        byte[] contractAddress = new byte[Address.ADDRESS_LENGTH];
        System.arraycopy(txData, Address.ADDRESS_LENGTH, contractAddress, 0, Address.ADDRESS_LENGTH);
        return AddressTool.getStringAddressByBytes(contractAddress);
    }

    /**
     * 获取跨链交易tx中froms里面地址的链id
     *
     * @param tx
     * @return
     */
    public static int getCrossTxFromsOriginChainId(Transaction tx) throws NulsException {
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData.getFrom() || coinData.getFrom().size() == 0) {
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        return AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());

    }

    /**
     * 获取跨链交易tx中tos里面地址的链id
     *
     * @param tx
     * @return
     */
    public static int getCrossTxTosOriginChainId(Transaction tx) throws NulsException {
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData.getTo() || coinData.getTo().size() == 0) {
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        return AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());

    }

    public static boolean isLegalContractAddress(byte[] addressBytes, Chain chain) {
        if (addressBytes == null) {
            return false;
        }
        return AddressTool.validContractAddress(addressBytes, chain.getChainId());
    }

    /**
     * 根据上一个交易hash获取下一个合法的nonce
     *
     * @param hash
     * @return
     */
    public static byte[] getNonceByPreHash(NulsDigestData hash) {
        byte[] out = new byte[8];
        byte[] in = hash.getDigestBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        String nonce8BytesStr = HexUtil.encode(out);
        return HexUtil.decode(nonce8BytesStr);
    }

    public static void txInformationDebugPrint(Chain chain, Transaction tx, NulsLogger nulsLogger) {
        if(tx.getType() == 1) return;
        nulsLogger.debug("");
        nulsLogger.debug("**************************************************");
        nulsLogger.debug("Transaction information");
        nulsLogger.debug("type: {}", tx.getType());
        nulsLogger.debug("txHash: {}", tx.getHash().getDigestHex());
        nulsLogger.debug("time: {}", DateUtils.timeStamp2DateStr(tx.getTime()));
        nulsLogger.debug("size: {}B,  -{}KB, -{}MB",
                String.valueOf(tx.getSize()), String.valueOf(tx.getSize() / 1024), String.valueOf(tx.getSize() / 1024 / 1024));
        byte[] remark = tx.getRemark();
        nulsLogger.debug("remark: {}", remark == null ? "" : HexUtil.encode(tx.getRemark()));
        CoinData coinData = null;
        try {
            if (tx.getCoinData() != null) {
                coinData = tx.getCoinDataInstance();
            }
        } catch (NulsException e) {
            e.printStackTrace();
        }
        if (coinData != null) {
            nulsLogger.debug("coinData:");
            List<CoinFrom> coinFromList = coinData.getFrom();
            if (coinFromList == null) {
                nulsLogger.debug("\tcoinFrom: null");
            } else if (coinFromList.size() == 0) {
                nulsLogger.debug("\tcoinFrom: size 0");
            } else {
                nulsLogger.debug("\tcoinFrom: ");
                for (int i = 0; i < coinFromList.size(); i++) {
                    CoinFrom coinFrom = coinFromList.get(i);
                    nulsLogger.debug("\tFROM_{}:", i);
                    nulsLogger.debug("\taddress: {}", AddressTool.getStringAddressByBytes(coinFrom.getAddress()));
                    nulsLogger.debug("\tamount: {}", coinFrom.getAmount());
                    nulsLogger.debug("\tassetChainId: [{}]", coinFrom.getAssetsChainId());
                    nulsLogger.debug("\tassetId: [{}]", coinFrom.getAssetsId());
                    nulsLogger.debug("\tnonce: {}", HexUtil.encode(coinFrom.getNonce()));
                    nulsLogger.debug("\tlocked(0普通交易，-1解锁金额交易（退出共识，退出委托)): [{}]", coinFrom.getLocked());
                    nulsLogger.debug("");
                }
            }

            List<CoinTo> coinToList = coinData.getTo();
            if (coinToList == null) {
                nulsLogger.debug("\tcoinTo: null");
            } else if (coinToList.size() == 0) {
                nulsLogger.debug("\tcoinTo: size 0");
            } else {
                nulsLogger.debug("\tcoinTo: ");
                for (int i = 0; i < coinToList.size(); i++) {
                    CoinTo coinTo = coinToList.get(i);
                    nulsLogger.debug("\tTO_{}:", i);
                    nulsLogger.debug("\taddress: {}", AddressTool.getStringAddressByBytes(coinTo.getAddress()));
                    nulsLogger.debug("\tamount: {}", coinTo.getAmount());
                    nulsLogger.debug("\tassetChainId: [{}]", coinTo.getAssetsChainId());
                    nulsLogger.debug("\tassetId: [{}]", coinTo.getAssetsId());
                    nulsLogger.debug("\tlocked(解锁高度或解锁时间，-1为永久锁定): [{}]", coinTo.getLockTime());
                    nulsLogger.debug("");
                }
            }

        } else {
            nulsLogger.debug("coinData: null");
        }
        nulsLogger.debug("**************************************************");
        nulsLogger.debug("");
    }
}
