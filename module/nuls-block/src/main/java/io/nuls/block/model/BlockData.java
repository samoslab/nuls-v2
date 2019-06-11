package io.nuls.block.model;

import io.nuls.base.data.Block;

import java.util.List;

/**
 * @author Niels
 */
public class BlockData {

    public BlockData(Block block, List contractList) {
        this.block = block;
        this.contractList = contractList;
    }

    private Block block;
    private List contractList;

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public List getContractList() {
        return contractList;
    }

    public void setContractList(List contractList) {
        this.contractList = contractList;
    }
}
