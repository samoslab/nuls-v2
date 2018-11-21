package io.nuls.chain.storage;


import io.nuls.chain.model.dto.Chain;

/**
 * @author tangyi
 * @date 2018/11/8
 * @description
 */
public interface ChainStorage {

    /**
     * Save chain
     *
     * @param key   The key
     * @param chain Chain object that needs to be saved
     * @return true/false
     */
    boolean save(int key, Chain chain);

    /**
     * Find chain based on key
     *
     * @param key The key
     * @return Chain object
     */
    Chain load(int key);
}
