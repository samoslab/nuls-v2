package io.nuls.poc.pbft.manager;

import io.nuls.poc.pbft.BlockVoter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class BlockVoterManager {

    private static final Map<Integer, BlockVoter> voterMap = new HashMap<>();

    public static BlockVoter getVoter(int chainId) {
        BlockVoter voter = voterMap.get(chainId);
        if (null == voter) {
            voter = new BlockVoter(chainId);
            voterMap.put(chainId, voter);
        }
        return voter;
    }
}
