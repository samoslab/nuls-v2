package io.nuls.api.task;


import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.db.AssetInfo;
import io.nuls.core.basic.Result;

import java.util.HashMap;
import java.util.Map;

public class QueryChainInfoTask implements Runnable {

    private int chainId;


    public QueryChainInfoTask(int chainId) {
        this.chainId = chainId;
    }

    @Override
    public void run() {
        Map<String, AssetInfo> map;
        if (ApiContext.isRunCrossChain) {
            Result<Map<String, AssetInfo>> result = WalletRpcHandler.getRegisteredChainInfoList();
            map = result.getData();
            CacheManager.setAssetInfoMap(map);
        } else {
            map = new HashMap<>();
            ApiCache apiCache = CacheManager.getCache(chainId);
            map.put(apiCache.getChainInfo().getDefaultAsset().getKey(), apiCache.getChainInfo().getDefaultAsset());
        }
    }
}
