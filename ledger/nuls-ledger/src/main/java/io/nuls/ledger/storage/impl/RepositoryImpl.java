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
package io.nuls.ledger.storage.impl;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.db.model.Entry;
import io.nuls.db.service.RocksDBService;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.model.ChainHeight;
import io.nuls.ledger.model.po.AccountState;
import io.nuls.ledger.model.po.BlockSnapshotAccounts;
import io.nuls.ledger.storage.DataBaseArea;
import io.nuls.ledger.storage.Repository;
import io.nuls.ledger.utils.LoggerUtil;
import io.nuls.tools.basic.InitializingBean;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.model.ByteUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.ledger.utils.LoggerUtil.logger;

/**
 * Created by wangkun23 on 2018/11/19.
 * @author lanjinsheng
 */
@Service
public class RepositoryImpl implements Repository, InitializingBean {

    public RepositoryImpl() {

    }

    /**
     * create accountState to rocksdb
     *
     * @param key
     * @param accountState
     */
    @Override
    public void createAccountState(byte[] key, AccountState accountState) {
        try {
            RocksDBService.put(getLedgerAccountTableName(accountState.getAddressChainId()), key, accountState.serialize());
        } catch (Exception e) {
            logger(accountState.getAddressChainId()).error("createAccountState serialize error.", e);
        }
    }

    /**
     * update accountState to rocksdb
     *
     * @param key
     * @param nowAccountState
     */
    @Override
    public void updateAccountState(byte[] key, AccountState nowAccountState) throws Exception {
        //update account
        LoggerUtil.logger(nowAccountState.getAddressChainId()).debug("updateAccountState hash={},nonce={}", nowAccountState.getTxHash(), nowAccountState.getNonce());
        LoggerUtil.logger(nowAccountState.getAddressChainId()).debug("updateAccountState address={},addressChainId={},assetChainId={},assetId={},getAvailableAmount={}," +
                        "getFreezeTotal={}",
                nowAccountState.getAddress(), nowAccountState.getAddressChainId(), nowAccountState.getAssetChainId(), nowAccountState.getAssetId(),
                nowAccountState.getAvailableAmount(), nowAccountState.getFreezeTotal());
        RocksDBService.put(getLedgerAccountTableName(nowAccountState.getAddressChainId()), key, nowAccountState.serialize());
    }
    @Override
    public void  batchUpdateAccountState(int addressChainId, Map<byte[],byte[]> accountStateMap) throws Exception {
        //update account
        RocksDBService.batchPut(getLedgerAccountTableName(addressChainId), accountStateMap);
    }


    @Override
    public void delBlockSnapshot(int chainId, long height) throws Exception {
        RocksDBService.delete(getBlockSnapshotTableName(chainId), ByteUtils.longToBytes(height));
    }

    @Override
    public void saveBlockSnapshot(int chainId, long height, BlockSnapshotAccounts blockSnapshotAccounts) throws Exception {
        RocksDBService.put(getBlockSnapshotTableName(chainId), ByteUtils.longToBytes(height), blockSnapshotAccounts.serialize());

    }

    @Override
    public BlockSnapshotAccounts getBlockSnapshot(int chainId, long height) {
        byte[] stream = RocksDBService.get(getBlockSnapshotTableName(chainId), ByteUtils.longToBytes(height));
        if (stream == null) {
            return null;
        }
        BlockSnapshotAccounts blockSnapshotAccounts = new BlockSnapshotAccounts();
        try {
            blockSnapshotAccounts.parse(new NulsByteBuffer(stream));
        } catch (NulsException e) {
            logger(chainId).error("getAccountState serialize error.", e);
        }
        return blockSnapshotAccounts;
    }


    /**
     * get accountState from rocksdb
     *
     * @param key
     * @return
     */
    @Override
    public AccountState getAccountState(int chainId, byte[] key) {
        byte[] stream = RocksDBService.get(getLedgerAccountTableName(chainId), key);
        if (stream == null) {
            return null;
        }
        AccountState accountState = new AccountState();
        try {
            accountState.parse(new NulsByteBuffer(stream));
        } catch (NulsException e) {
            logger(chainId).error("getAccountState serialize error.", e);
        }
        return accountState;
    }

    @Override
    public long getBlockHeight(int chainId) {
        byte[] stream = RocksDBService.get(getChainsHeightTableName(), ByteUtils.intToBytes(chainId));
        if (stream == null) {
            return -1;
        }
        try {
            long height = ByteUtils.byteToLong(stream);
            return height;
        } catch (Exception e) {
            logger(chainId).error("getBlockHeight serialize error.", e);
        }
        return -1;
    }

    @Override
    public void saveOrUpdateBlockHeight(int chainId, long height) {
        try {
            RocksDBService.put(getChainsHeightTableName(), ByteUtils.intToBytes(chainId), ByteUtils.longToBytes(height));
        } catch (Exception e) {
            logger(chainId).error("saveBlockHeight serialize error.", e);
        }

    }

    @Override
    public List<ChainHeight> getChainsBlockHeight() {
        List<Entry<byte[], byte[]>> list = RocksDBService.entryList(getChainsHeightTableName());
        List<ChainHeight> rtList = new ArrayList<>();
        if (null == list || 0 == list.size()) {
            return null;
        }
        for (Entry<byte[], byte[]> entry : list) {
            ChainHeight chainHeight = new ChainHeight();
            chainHeight.setChainId(ByteUtils.bytesToInt(entry.getKey()));
            chainHeight.setBlockHeight(ByteUtils.byteToLong(entry.getValue()));
            rtList.add(chainHeight);
        }
        return rtList;
    }


    String getLedgerAccountTableName(int chainId) {
        return DataBaseArea.TB_LEDGER_ACCOUNT + chainId;
    }

    String getBlockSnapshotTableName(int chainId) {
        return DataBaseArea.TB_LEDGER_ACCOUNT_BLOCK_SNAPSHOT + chainId;
    }

    public String getChainsHeightTableName() {
        return DataBaseArea.TB_LEDGER_BLOCK_HEIGHT;
    }

    String getBlockTableName(int chainId) {
        return DataBaseArea.TB_LEDGER_BLOCKS + chainId;
    }

    String getLedgerNonceTableName(int chainId) {
        return DataBaseArea.TB_LEDGER_NONCES + chainId;
    }
    String getLedgerHashTableName(int chainId) {
        return DataBaseArea.TB_LEDGER_HASH + chainId;
    }
    /**
     * 初始化数据库
     */
    public void initChainDb(int addressChainId) {
        try {
            if (!RocksDBService.existTable(getLedgerAccountTableName(addressChainId))) {
                RocksDBService.createTable(getLedgerAccountTableName(addressChainId));
            }
            if (!RocksDBService.existTable(getBlockSnapshotTableName(addressChainId))) {
                RocksDBService.createTable(getBlockSnapshotTableName(addressChainId));
            }
            if (!RocksDBService.existTable(getBlockTableName(addressChainId))) {
                RocksDBService.createTable(getBlockTableName(addressChainId));
            }
            if (!RocksDBService.existTable(getLedgerNonceTableName(addressChainId))) {
                RocksDBService.createTable(getLedgerNonceTableName(addressChainId));
            }
        } catch (Exception e) {
            logger(addressChainId).error(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws NulsException {

    }

    @Override
    public void initTableName() throws NulsException {
        try {
            if (!RocksDBService.existTable(getChainsHeightTableName())) {
                RocksDBService.createTable(getChainsHeightTableName());
            } else {
                LoggerUtil.logger().info("table {} exist.", getChainsHeightTableName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new NulsException(e);
        }
    }

    @Override
    public void saveAccountNonces(int chainId, Map<String, Integer> noncesMap) throws Exception {
        String table = getLedgerNonceTableName(chainId);
        if (!RocksDBService.existTable(table)) {
            RocksDBService.createTable(table);
        }
        Map<byte[], byte[]> saveMap = new HashMap<>(1024);
        for (Map.Entry<String, Integer> m : noncesMap.entrySet()) {
            saveMap.put(ByteUtils.toBytes(m.getKey(), LedgerConstant.DEFAULT_ENCODING), ByteUtils.intToBytes(m.getValue()));
        }
        if (saveMap.size() > 0) {
            RocksDBService.batchPut(table, saveMap);
        }
    }

    @Override
    public void deleteAccountNonces(int chainId, String accountNonceKey) throws Exception {
        RocksDBService.delete(getLedgerNonceTableName(chainId), ByteUtils.toBytes(accountNonceKey, LedgerConstant.DEFAULT_ENCODING));
    }

    @Override
    public boolean existAccountNonce(int chainId, String accountNonceKey) throws Exception {
        return (null != RocksDBService.get(getLedgerNonceTableName(chainId), ByteUtils.toBytes(accountNonceKey, LedgerConstant.DEFAULT_ENCODING)));
    }


    @Override
    public void saveAccountHash(int chainId, Map<String, Integer> hashMap) throws Exception {
        String table = getLedgerHashTableName(chainId);
        if (!RocksDBService.existTable(table)) {
            RocksDBService.createTable(table);
        }
        Map<byte[], byte[]> saveMap = new HashMap<>(1024);
        for (Map.Entry<String, Integer> m : hashMap.entrySet()) {
            saveMap.put(ByteUtils.toBytes(m.getKey(), LedgerConstant.DEFAULT_ENCODING), ByteUtils.intToBytes(m.getValue()));
        }
        if (saveMap.size() > 0) {
            RocksDBService.batchPut(table, saveMap);
        }
    }

    @Override
    public void deleteAccountHash(int chainId, String hash) throws Exception {
        RocksDBService.delete(getLedgerHashTableName(chainId), ByteUtils.toBytes(hash, LedgerConstant.DEFAULT_ENCODING));
    }

    @Override
    public boolean existAccountHash(int chainId, String hash) throws Exception {
        return (null != RocksDBService.get(getLedgerHashTableName(chainId), ByteUtils.toBytes(hash, LedgerConstant.DEFAULT_ENCODING)));
    }
}
