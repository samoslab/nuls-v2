/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.ledger.service.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Service;
import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.model.ChainHeight;
import io.nuls.ledger.model.po.AccountStateSnapshot;
import io.nuls.ledger.model.po.BlockSnapshotAccounts;
import io.nuls.ledger.model.po.BlockSnapshotTxs;
import io.nuls.ledger.service.AccountStateService;
import io.nuls.ledger.service.BlockDataService;
import io.nuls.ledger.service.ChainAssetsService;
import io.nuls.ledger.storage.LgBlockSyncRepository;
import io.nuls.ledger.storage.Repository;
import io.nuls.ledger.utils.CoinDataUtil;
import io.nuls.ledger.utils.LedgerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lan
 * @description
 * @date 2019/02/14
 **/
@Service
public class BlockDataServiceImpl implements BlockDataService {
    @Autowired
    private Repository repository;
    @Autowired
    LgBlockSyncRepository lgBlockSyncRepository;
    @Autowired
    private AccountStateService accountStateService;
    @Autowired
    private ChainAssetsService chainAssetsService;

    @Override
    public void initBlockDatas() throws Exception {
        //获取确认高度
        List<ChainHeight> list = getChainsBlockHeight();
        if (null != list) {
            Log.info("chainList size = {}", list.size());
            for (ChainHeight chainHeight : list) {
                Log.info("begin chain ledger checked..chainId = {},chainHeight={}", chainHeight.getChainId(), chainHeight.getBlockHeight());
                BlockSnapshotAccounts blockSnapshotAccounts = repository.getBlockSnapshot(chainHeight.getChainId(), chainHeight.getBlockHeight() + 1);
                if (null != blockSnapshotAccounts) {
                    List<AccountStateSnapshot> preAccountStates = blockSnapshotAccounts.getAccounts();
                    //回滚高度
                    accountStateService.rollAccountState(chainHeight.getChainId(), preAccountStates);
                }
                Log.info("end chain ledger checked..chainId = {},chainHeight={}", chainHeight.getChainId(), chainHeight.getBlockHeight());
                Log.info("begin block sync info checked..chainId = {}", chainHeight.getChainId());
                long currenHeight = lgBlockSyncRepository.getSyncBlockHeight(chainHeight.getChainId());
                if (currenHeight > 0) {
                    BlockSnapshotTxs blockSnapshotTxs = lgBlockSyncRepository.getBlockSnapshotTxs(chainHeight.getChainId(), currenHeight + 1);
                    if (null != blockSnapshotTxs) {
                        rollBackBlockDatas(chainHeight.getChainId(), currenHeight + 1);
                    }
                }
            }
        }
    }

    @Override
    public List<ChainHeight> getChainsBlockHeight() throws Exception {
        return repository.getChainsBlockHeight();
    }

    private void dealAssetAddressIndex(Map<String, List<String>> assetAddressIndex, int chainId, int assetId, byte[] address) {
        String assetIndexKey = chainId + "-" + assetId;
        List<String> addressList = null;
        if (null == assetAddressIndex.get(assetIndexKey)) {
            addressList = new ArrayList<>();
            assetAddressIndex.put(assetIndexKey, addressList);
        } else {
            addressList = assetAddressIndex.get(assetIndexKey);
        }
        addressList.add(AddressTool.getStringAddressByBytes(address));
    }

    @Override
    public void syncBlockDatas(int addressChainId, long height, Block block) throws Exception {
        Map<byte[], byte[]> saveHashMap = new HashMap<>(5120);
        Map<String, Integer> ledgerNonce = new HashMap<String, Integer>(5120);
        Map<String, List<String>> assetAddressIndex = new HashMap<>();
        BlockSnapshotTxs blockSnapshotTxs = new BlockSnapshotTxs();
        blockSnapshotTxs.setBlockHash(block.getHeader().getHash().toHex());
        //解析区块
        List<Transaction> txList = block.getTxs();
        for (Transaction transaction : txList) {
            byte[] nonce8Bytes = LedgerUtil.getNonceByTx(transaction);
            String txHash = transaction.getHash().toHex();
            blockSnapshotTxs.addHash(txHash);
            saveHashMap.put(ByteUtils.toBytes(txHash, LedgerConstant.DEFAULT_ENCODING), ByteUtils.intToBytes(1));
            //从缓存校验交易
            CoinData coinData = CoinDataUtil.parseCoinData(transaction.getCoinData());
            if (null == coinData) {
                continue;
            }
            List<CoinFrom> froms = coinData.getFrom();
            for (CoinFrom from : froms) {
                if (LedgerUtil.isNotLocalChainAccount(addressChainId, from.getAddress())) {
                    //非本地网络账户地址,不进行处理
                    continue;
                }
                dealAssetAddressIndex(assetAddressIndex, from.getAssetsChainId(), from.getAssetsId(), from.getAddress());
                if (from.getLocked() == 0) {
                    String addressNonce = LedgerUtil.getAccountNoncesStringKey(from, nonce8Bytes);
                    ledgerNonce.put(addressNonce, 1);
                    blockSnapshotTxs.addNonce(addressNonce);
                } else {

                }
            }
            List<CoinTo> tos = coinData.getTo();
            for (CoinTo to : tos) {
                if (LedgerUtil.isNotLocalChainAccount(addressChainId, to.getAddress())) {
                    //非本地网络账户地址,不进行处理
                    continue;
                }
                dealAssetAddressIndex(assetAddressIndex, to.getAssetsChainId(), to.getAssetsId(), to.getAddress());
            }
        }
        //存储备份信息
        lgBlockSyncRepository.bakBlockInfosByHeight(addressChainId, height, blockSnapshotTxs);
        //存储height - hash
        lgBlockSyncRepository.saveBlockHashByHeight(addressChainId, height, block.getHeader().getHash().toHex());
        chainAssetsService.updateChainAssets(addressChainId, assetAddressIndex);
        lgBlockSyncRepository.saveAccountNonces(addressChainId, ledgerNonce);
        lgBlockSyncRepository.saveAccountHash(addressChainId, saveHashMap);
        //存储当前height: chainId-height
        lgBlockSyncRepository.saveOrUpdateSyncBlockHeight(addressChainId, height);
        //删除height-100的缓存
        if (height > LedgerConstant.CACHE_NONCE_INFO_BLOCK) {
            lgBlockSyncRepository.delBlockSnapshotTxs(addressChainId, (height - LedgerConstant.CACHE_NONCE_INFO_BLOCK));
        }
    }

    @Override
    public void rollBackBlockDatas(int chainId, long height) throws Exception {
        BlockSnapshotTxs blockSnapshotTxs = lgBlockSyncRepository.getBlockSnapshotTxs(chainId, height);
        if (null != blockSnapshotTxs) {
            lgBlockSyncRepository.delBlockHash(chainId, height);
            lgBlockSyncRepository.batchDeleteAccountHash(chainId, blockSnapshotTxs.getTxHashList());
            lgBlockSyncRepository.batchDeleteAccountNonces(chainId, blockSnapshotTxs.getAddressNonceList());
        }
        lgBlockSyncRepository.saveOrUpdateSyncBlockHeight(chainId, height - 1);
        lgBlockSyncRepository.delBlockSnapshotTxs(chainId, height);
    }

    @Override
    public String getBlockHashByHeight(int chainId, long height) throws Exception {
        return lgBlockSyncRepository.getBlockHash(chainId, height);
    }

    @Override
    public long currentSyncHeight(int chainId) throws Exception {
        return lgBlockSyncRepository.getSyncBlockHeight(chainId);
    }
}
