package io.nuls.poc.pbft.cache;

import io.nuls.base.data.NulsHash;
import io.nuls.poc.pbft.model.VoteData;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class VoteCache {

    private final int chainId;

    private Map<String, VoteData> map = new HashMap<>();

    public VoteCache(int chainId){
        this.chainId = chainId;
    }

    public void addVote(long height, int round, NulsHash hash,String address){
        VoteData data = new VoteData();

    }

}
