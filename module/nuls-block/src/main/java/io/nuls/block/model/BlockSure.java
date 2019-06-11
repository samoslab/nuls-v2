package io.nuls.block.model;

import io.nuls.base.data.NulsHash;

/**
 * @author Niels
 */
public class BlockSure {

    private NulsHash hash;
    private long height;

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }
}
