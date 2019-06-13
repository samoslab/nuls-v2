package io.nuls.poc.pbft;

import io.nuls.base.data.Block;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.poc.model.bo.Chain;
import io.nuls.poc.model.bo.round.MeetingRound;
import io.nuls.poc.pbft.model.VoteRound;
import io.nuls.poc.rpc.call.CallMethodUtils;
import io.nuls.poc.utils.LoggerUtil;
import io.nuls.poc.utils.manager.RoundManager;

/**
 * @author Niels
 */
public class BlockVoter implements Runnable {

    private final int chainId;
    private final Chain chain;
    private final RoundManager roundManager;

    private VoteRound lastRound;

    private VoteRound nowRound;

    public BlockVoter(Chain chain) {
        this.chainId = chain.getConfig().getChainId();
        this.chain = chain;
        this.roundManager = SpringLiteContext.getBean(RoundManager.class);
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                doit();
                Thread.sleep(1000L);
            } catch (Exception e) {
                LoggerUtil.commonLog.error(e);
            }
        }
    }

    private void doit() {
        long now = NulsDateUtils.getCurrentTimeSeconds();
        chain.getNewestHeader().getTime();

    }

    public void recieveBlock(Block block) {


//        CallMethodUtils.signature();
    }


    private MeetingRound getCurrentRound() {
        //todo

        return new MeetingRound();
    }
}
