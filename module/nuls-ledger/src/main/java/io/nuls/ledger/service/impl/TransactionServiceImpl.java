/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Service;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.model.AccountBalance;
import io.nuls.ledger.model.Uncfd2CfdKey;
import io.nuls.ledger.model.ValidateResult;
import io.nuls.ledger.model.po.AccountState;
import io.nuls.ledger.model.po.AccountStateSnapshot;
import io.nuls.ledger.model.po.BlockSnapshotAccounts;
import io.nuls.ledger.model.po.TxUnconfirmed;
import io.nuls.ledger.service.*;
import io.nuls.ledger.service.processor.CommontTransactionProcessor;
import io.nuls.ledger.service.processor.LockedTransactionProcessor;
import io.nuls.ledger.storage.Repository;
import io.nuls.ledger.utils.*;
import io.nuls.ledger.validator.CoinDataValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.nuls.ledger.utils.LoggerUtil.logger;

/**
 * 交易业务处理实现
 *
 * @author lanjinsheng
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    AccountStateService accountStateService;
    @Autowired
    UnconfirmedStateService unconfirmedStateService;
    @Autowired
    CoinDataValidator coinDataValidator;
    @Autowired
    LockedTransactionProcessor lockedTransactionProcessor;
    @Autowired
    CommontTransactionProcessor commontTransactionProcessor;
    @Autowired
    Repository repository;
    @Autowired
    FreezeStateService freezeStateService;
    @Autowired
    ChainAssetsService chainAssetsService;
    /**
     * 缓存一个区块的nonce值
     */
    private Map<String, Integer> ledgerNonce = new HashMap<String, Integer>(5120);
    /**
     * 缓存一个区块的hash值
     */
    private Map<String, Integer> ledgerHash = new HashMap<String, Integer>(5120);

    /**
     * 未确认交易数据处理
     *
     * @param transaction
     */

    @Override
    public ValidateResult unConfirmTxProcess(int addressChainId, Transaction transaction) throws Exception {
        //直接更新未确认交易
        CoinData coinData = CoinDataUtil.parseCoinData(transaction.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接返回
            return ValidateResult.getSuccess();
        }
        /*未确认交易的校验*/
        Map<String, TxUnconfirmed> accountsMap = new ConcurrentHashMap<>(8);
        byte[] txNonce = LedgerUtil.getNonceByTx(transaction);
        ValidateResult validateResult = coinDataValidator.analysisCoinData(addressChainId, transaction, accountsMap, txNonce);
        if (!validateResult.isSuccess()) {
            LoggerUtil.logger(addressChainId).error("validateResult = {}={}", validateResult.getValidateCode(), validateResult.getValidateDesc());
            return validateResult;
        }
        Set keys = accountsMap.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            TxUnconfirmed txUnconfirmed = accountsMap.get(it.next());
            ValidateResult updateResult = unconfirmedStateService.updateUnconfirmedTx(addressChainId, txNonce, txUnconfirmed);
            if (!updateResult.isSuccess()) {
                return updateResult;
            }
        }
        return ValidateResult.getSuccess();
    }

    private boolean confirmBlockTxProcess(int addressChainId, long blockHeight, List<Transaction> txList,
                                          Map<String, AccountBalance> updateAccounts, List<Uncfd2CfdKey> delUncfd2CfdKeys, Map<String, Integer> clearUncfs) throws Exception {
        for (Transaction transaction : txList) {
            byte[] nonce8Bytes = LedgerUtil.getNonceByTx(transaction);
            String txHash = transaction.getHash().toString();
            ledgerHash.put(txHash, 1);
            //从缓存校验交易
            CoinData coinData = CoinDataUtil.parseCoinData(transaction.getCoinData());
            if (null == coinData) {
                //例如黄牌交易，直接返回
                LoggerUtil.logger(addressChainId).info("txHash = {},coinData is null continue.", txHash);
                continue;
            }
            List<CoinFrom> froms = coinData.getFrom();
            for (CoinFrom from : froms) {
                if (LedgerUtil.isNotLocalChainAccount(addressChainId, from.getAddress())) {
                    //非本地网络账户地址,不进行处理
                    logger(addressChainId).info("address={} not localChainAccount", AddressTool.getStringAddressByBytes(from.getAddress()));
                    continue;
                }
                boolean process = false;
                AccountBalance accountBalance = getAccountBalance(addressChainId, from, txHash, blockHeight, updateAccounts);
                if (from.getLocked() == 0) {
                    if (!coinDataValidator.validateAndAddNonces(accountBalance, nonce8Bytes, from.getNonce(), from.getAmount())) {
                        logger(addressChainId).info("nonce1={},nonce2={} validate fail.", accountBalance.getNonces().get(accountBalance.getNonces().size() - 1), LedgerUtil.getNonceEncode(from.getNonce()));
                        return false;
                    }
                    //判断是否存在未确认过程交易，如果存在则进行确认记录，如果不存在，则进行未确认的清空记录
                    String accountkeyStr = LedgerUtil.getAccountAssetStrKey(from);
                    String nonce8Str = LedgerUtil.getNonceEncode(nonce8Bytes);
                    if (unconfirmedStateService.existTxUnconfirmedTx(addressChainId, accountkeyStr, nonce8Str)) {
                        delUncfd2CfdKeys.add(new Uncfd2CfdKey(accountkeyStr, nonce8Str));
                    } else {
                        clearUncfs.put(accountkeyStr, 1);
                    }
                    //非解锁交易处理
                    process = commontTransactionProcessor.processFromCoinData(from, nonce8Bytes, accountBalance.getNowAccountState());
                    ledgerNonce.put(LedgerUtil.getAccountNoncesStringKey(from, nonce8Bytes), 1);
                } else {
                    process = lockedTransactionProcessor.processFromCoinData(from, nonce8Bytes, txHash, accountBalance.getNowAccountState());
                }
                if (!process) {
                    logger(addressChainId).info("address={},txHash = {} processFromCoinData is fail.", addressChainId, transaction.getHash().toString());
                    return false;
                }
            }
            List<CoinTo> tos = coinData.getTo();
            for (CoinTo to : tos) {
                if (LedgerUtil.isNotLocalChainAccount(addressChainId, to.getAddress())) {
                    //非本地网络账户地址,不进行处理
                    logger(addressChainId).info("address={} not localChainAccount", AddressTool.getStringAddressByBytes(to.getAddress()));
                    continue;
                }
                AccountBalance accountBalance = getAccountBalance(addressChainId, to, txHash, blockHeight, updateAccounts);
                if (to.getLockTime() == 0) {
                    //非锁定交易处理
                    commontTransactionProcessor.processToCoinData(to, accountBalance.getNowAccountState());
                } else {
                    //锁定交易处理
                    lockedTransactionProcessor.processToCoinData(to, nonce8Bytes, txHash, accountBalance.getNowAccountState(), transaction.getTime());
                }
            }
        }
        return true;
    }

    /**
     * 已确认区块数据处理
     * 提交交易：1.交易存库（上一个账户状态进行镜像,存储最近x=500区块以内交易） 2.更新账户
     *
     * @param addressChainId
     * @param txList
     * @param blockHeight
     * @return
     */
    @Override
    public boolean confirmBlockProcess(int addressChainId, List<Transaction> txList, long blockHeight) {
        long time1, time2, time7 = 0;
        time1 = System.currentTimeMillis();
        try {
            ledgerNonce.clear();
            ledgerHash.clear();
            LockerUtil.BLOCK_SYNC_LOCKER.lock();
            long currentDbHeight = repository.getBlockHeight(addressChainId);
            if ((blockHeight - currentDbHeight) != 1) {
                //高度不一致，数据出问题了
                logger(addressChainId).error("addressChainId ={},blockHeight={},ledgerBlockHeight={}", addressChainId, blockHeight, currentDbHeight);
                return false;
            }
            //批量交易按交易进行账户的金额处理，再按区块为原子性进行提交,updateAccounts用于账户计算缓存，最后统一处理
            Map<String, AccountBalance> updateAccounts = new HashMap<>(5120);
            //整体区块备份
            BlockSnapshotAccounts blockSnapshotAccounts = new BlockSnapshotAccounts();
            Map<byte[], byte[]> accountStatesMap = new HashMap<>(5120);
            List<Uncfd2CfdKey> delUncfd2CfdKeys = new ArrayList<>();
            Map<String, Integer> clearUncfs = new HashMap<>(64);
            Map<String, List<String>> assetAddressIndex = new HashMap<>();
            try {
                if (!confirmBlockTxProcess(addressChainId, blockHeight, txList, updateAccounts, delUncfd2CfdKeys, clearUncfs)) {
                    return false;
                }
                time2 = System.currentTimeMillis();

                //整体交易的处理
                //更新账本信息
                for (Map.Entry<String, AccountBalance> entry : updateAccounts.entrySet()) {
                    //缓存数据
                    AccountStateSnapshot accountStateSnapshot = new AccountStateSnapshot(entry.getValue().getPreAccountState(), entry.getValue().getNonces());
                    blockSnapshotAccounts.addAccountState(accountStateSnapshot);
                    freezeStateService.recalculateFreeze(entry.getValue().getNowAccountState());
                    entry.getValue().getNowAccountState().setLatestUnFreezeTime(TimeUtil.getCurrentTime());
                    accountStatesMap.put(entry.getKey().getBytes(LedgerConstant.DEFAULT_ENCODING), entry.getValue().getNowAccountState().serialize());
                    String assetIndexKey = entry.getValue().getNowAccountState().getAssetChainId() + "-" + entry.getValue().getNowAccountState().getAssetId();
                    List<String> addressList = null;
                    if (null == assetAddressIndex.get(assetIndexKey)) {
                        addressList = new ArrayList<>();
                        assetAddressIndex.put(assetIndexKey, addressList);
                    } else {
                        addressList = assetAddressIndex.get(assetIndexKey);
                    }
                    addressList.add(entry.getValue().getNowAccountState().getAddress());
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger(addressChainId).error("confirmBlockProcess blockSnapshotAccounts addAccountState error!");
                return false;
            }
            //提交整体数据
            try {
                //备份历史
                repository.saveBlockSnapshot(addressChainId, blockHeight, blockSnapshotAccounts);
                if (accountStatesMap.size() > 0) {
                    repository.batchUpdateAccountState(addressChainId, accountStatesMap);
                }
                chainAssetsService.updateChainAssets(addressChainId, assetAddressIndex);
                repository.saveAccountNonces(addressChainId, ledgerNonce);
                repository.saveAccountHash(addressChainId, ledgerHash);
                for (Map.Entry<String, Integer> entry : clearUncfs.entrySet()) {
                    //进行清空处理
                    unconfirmedStateService.clearAccountUnconfirmed(addressChainId, entry.getKey());
                }
                //删除跃迁的未确认交易
                unconfirmedStateService.batchDeleteUnconfirmedTx(addressChainId, delUncfd2CfdKeys);
            } catch (Exception e) {
                e.printStackTrace();
                //需要回滚数据
                logger(addressChainId).error("confirmBlockProcess  error! go rollBackBlock!");
                LoggerUtil.txRollBackLog(addressChainId).error("confirmBlockProcess  error! go rollBackBlock!addrChainId={},height={}", addressChainId, blockHeight);
                rollBackBlock(addressChainId, blockSnapshotAccounts.getAccounts(), blockHeight);
                return false;
            }
            //完全提交,存储当前高度。
            repository.saveOrUpdateBlockHeight(addressChainId, blockHeight);
            time7 = System.currentTimeMillis();
            LoggerUtil.timeTestLogger(addressChainId).debug("####txs={}==accountSize={}====总时间:{},结构校验解析时间={}",
                    txList.size(), updateAccounts.size(), time7 - time1, time2 - time1);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger(addressChainId).error("confirmBlockProcess error", e);
            return false;
        } finally {
            LockerUtil.BLOCK_SYNC_LOCKER.unlock();
            ledgerNonce.clear();
            ledgerHash.clear();
        }

    }

    private AccountBalance getAccountBalance(int addressChainId, Coin coin, String txHash, long height, Map<String, AccountBalance> updateAccounts) {
        String address = AddressTool.getStringAddressByBytes(coin.getAddress());
        int assetChainId = coin.getAssetsChainId();
        int assetId = coin.getAssetsId();
        String key = LedgerUtil.getKeyStr(address, assetChainId, assetId);
        AccountBalance accountBalance = updateAccounts.get(key);
        if (null == accountBalance) {
            //交易里的账户处理缓存AccountBalance
            AccountState accountState = accountStateService.getAccountStateReCal(address, addressChainId, assetChainId, assetId);
            AccountState orgAccountState = accountState.deepClone();
            accountState.setTxHash(txHash);
            accountState.setHeight(height);
            accountBalance = new AccountBalance(accountState, orgAccountState);
            updateAccounts.put(key, accountBalance);
        }
        accountBalance.getNowAccountState().setTxHash(txHash);
        return accountBalance;
    }

    /**
     * @param addressChainId
     * @param preAccountStates
     * @return
     */
    @Override
    public synchronized boolean rollBackBlock(int addressChainId, List<AccountStateSnapshot> preAccountStates, long blockHeight) {
        try {
            //回滚账号信息
            for (AccountStateSnapshot accountStateSnapshot : preAccountStates) {
                String key = LedgerUtil.getKeyStr(accountStateSnapshot.getAccountState().getAddress(), accountStateSnapshot.getAccountState().getAssetChainId(), accountStateSnapshot.getAccountState().getAssetId());
                accountStateService.rollAccountState(key, accountStateSnapshot);
                logger(addressChainId).debug("rollBack account={},assetChainId={},assetId={}, height={},lastHash= {} ", key, accountStateSnapshot.getAccountState().getAssetChainId(), accountStateSnapshot.getAccountState().getAssetId(),
                        accountStateSnapshot.getAccountState().getHeight(), accountStateSnapshot.getAccountState().getTxHash());
            }
            //回滚备份数据
            repository.delBlockSnapshot(addressChainId, blockHeight);
        } catch (Exception e) {
            logger(addressChainId).error("rollBackBlock error!!", e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * @param addressChainId
     * @return
     */
    @Override
    public boolean rollBackConfirmTxs(int addressChainId, long blockHeight, List<Transaction> txs) {
        try {
            LockerUtil.BLOCK_SYNC_LOCKER.lock();
            long currentDbHeight = repository.getBlockHeight(addressChainId);
            if ((blockHeight - currentDbHeight) == 1) {
                logger(addressChainId).debug("addressChainId ={},blockHeight={},ledgerBlockHeight={}", addressChainId, blockHeight, currentDbHeight);
                return true;
            }
            if (blockHeight != currentDbHeight) {
                //高度不一致，数据出问题了
                logger(addressChainId).error("addressChainId ={},blockHeight={},ledgerBlockHeight={}", addressChainId, blockHeight, currentDbHeight);
                return false;
            }
            BlockSnapshotAccounts blockSnapshotAccounts = repository.getBlockSnapshot(addressChainId, blockHeight);
            //回滚高度
            repository.saveOrUpdateBlockHeight(addressChainId, (blockHeight - 1));
            List<AccountStateSnapshot> preAccountStates = blockSnapshotAccounts.getAccounts();
            for (AccountStateSnapshot accountStateSnapshot : preAccountStates) {
                String key = LedgerUtil.getKeyStr(accountStateSnapshot.getAccountState().getAddress(), accountStateSnapshot.getAccountState().getAssetChainId(), accountStateSnapshot.getAccountState().getAssetId());
                LoggerUtil.txRollBackLog(addressChainId).debug("#####start rollBackConfirmTxs acountKey={},blockHeight={},preHash={}", key, blockHeight, accountStateSnapshot.getAccountState().getTxHash());
                accountStateService.rollAccountState(key, accountStateSnapshot);
                logger(addressChainId).info("rollBack account={},assetChainId={},assetId={}, height={},lastHash= {} ", key, accountStateSnapshot.getAccountState().getAssetChainId(), accountStateSnapshot.getAccountState().getAssetId(),
                        accountStateSnapshot.getAccountState().getHeight(), accountStateSnapshot.getAccountState().getTxHash());
            }
            //删除备份数据
            repository.delBlockSnapshot(addressChainId, blockHeight);
            //回滚nonce缓存信息
            txs.forEach(tx -> {
                //从缓存校验交易
                CoinData coinData = CoinDataUtil.parseCoinData(tx.getCoinData());
                //删除备份的hash
                try {
                    repository.deleteAccountHash(addressChainId, tx.getHash().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (null != coinData) {
                    //更新账户状态
                    String nonce8BytesStr = LedgerUtil.getNonceEncodeByTx(tx);
                    List<CoinFrom> froms = coinData.getFrom();
                    for (CoinFrom from : froms) {
                        if (LedgerUtil.isNotLocalChainAccount(addressChainId, from.getAddress())) {
                            continue;
                        }
                        if (from.getLocked() == 0) {
                            try {
                                //删除备份的花费nonce值。
                                repository.deleteAccountNonces(addressChainId, LedgerUtil.getAccountNoncesStrKey(AddressTool.getStringAddressByBytes(from.getAddress()), from.getAssetsChainId(), from.getAssetsId(), nonce8BytesStr));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            logger(addressChainId).error("rollBackConfirmTxs error!!", e);
            e.printStackTrace();
            repository.saveOrUpdateBlockHeight(addressChainId, blockHeight);
            return false;
        } finally {
            LockerUtil.BLOCK_SYNC_LOCKER.unlock();
        }
        return true;
    }

    @Override
    public boolean rollBackUnconfirmTx(int addressChainId, Transaction transaction) {
        //回滚未确认交易,就是回滚未确认nonce值
        CoinData coinData = CoinDataUtil.parseCoinData(transaction.getCoinData());
        if (null == coinData) {
            //例如黄牌交易，直接返回
            return true;
        }
        List<CoinFrom> froms = coinData.getFrom();
        String txHash = transaction.getHash().toString();
        for (CoinFrom from : froms) {
            if (LedgerUtil.isNotLocalChainAccount(addressChainId, from.getAddress())) {
                //非本地网络账户地址,不进行处理
                continue;
            }
            String address = AddressTool.getStringAddressByBytes(from.getAddress());
            int assetChainId = from.getAssetsChainId();
            int assetId = from.getAssetsId();
            String assetKey = LedgerUtil.getKeyStr(address, assetChainId, assetId);
            return unconfirmedStateService.rollUnconfirmedTx(addressChainId, assetKey, txHash);
        }
        return true;
    }

    @Override
    public boolean hadCommit(int addressChainId, String accountNonceKey) throws Exception {
        if (null != ledgerNonce.get(accountNonceKey)) {
            return true;
        }
        return (repository.existAccountNonce(addressChainId, accountNonceKey));
    }

    @Override
    public boolean hadTxExist(int addressChainId, String hash) throws Exception {
        if (null != ledgerHash.get(hash)) {
            return true;
        }
        return (repository.existAccountHash(addressChainId, hash));
    }
}
