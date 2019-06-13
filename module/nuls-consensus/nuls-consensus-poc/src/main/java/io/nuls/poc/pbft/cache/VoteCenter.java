package io.nuls.poc.pbft.cache;

import io.nuls.base.data.NulsHash;
import io.nuls.poc.pbft.model.PbftData;
import io.nuls.poc.pbft.model.VoteData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class VoteCenter {

    private final int chainId;

    private Map<String, PbftData> map = new HashMap<>();

    public VoteCenter(int chainId) {
        this.chainId = chainId;
    }

    public PbftData addVote1(long height, int round, NulsHash hash, byte[] address, long startTime) {

        PbftData pbftData = getPbftData(height, round, startTime);
        VoteData data = new VoteData();
        data.setAddress(address);
        data.setHash(hash);
        data.setHeight(height);
        data.setRound(round);
        pbftData.addVote1Result(data);
        return pbftData;
    }
    public PbftData addVote2(long height, int round, NulsHash hash, byte[] address, long startTime) {

        PbftData pbftData = getPbftData(height, round, startTime);
        VoteData data = new VoteData();
        data.setAddress(address);
        data.setHash(hash);
        data.setHeight(height);
        data.setRound(round);
        pbftData.addVote2Result(data);
        return pbftData;
    }

    private PbftData getPbftData(long height, int round, long startTime) {
        String key = height + "_" + round;
        PbftData data = map.get(key);
        if (null == data) {
            data = new PbftData();
            data.setHeight(height);
            data.setRound(round);
            data.setStartTime(startTime);
            data.setEndTime(startTime + 10);
            map.put(key, data);
        }
        return data;
    }

}
