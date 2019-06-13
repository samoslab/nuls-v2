package io.nuls.poc.pbft.model;

import io.nuls.base.data.NulsHash;

/**
 * @author Niels
 */
public class VoteResultItem {

    private NulsHash hash;

    private int count;

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
