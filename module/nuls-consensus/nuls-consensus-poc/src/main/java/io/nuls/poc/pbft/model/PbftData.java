package io.nuls.poc.pbft.model;

import java.util.List;

/**
 * @author Niels
 */
public class PbftData {

    private long height;

    private int round;

    private long startTime;

    private long endTime;

    private List<VoteData> voteResultList;


}
