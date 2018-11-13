package io.nuls.chain.storage;

import io.nuls.base.data.chain.ChainAsset;

public interface ChainAssetStorage {

    /**
     * Get specific asset values of a specific chain
     *
     * @param key chainId-assetId
     * @return ChainAsset object
     */
    ChainAsset load(String key);

    /**
     * Save specific asset values of a specific chain
     *
     * @param key        chainId-assetId
     * @param chainAsset ChainAsset object
     * @return true/false
     */
    boolean save(String key, ChainAsset chainAsset);
}
