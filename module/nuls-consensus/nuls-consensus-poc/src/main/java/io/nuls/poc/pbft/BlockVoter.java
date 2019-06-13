package io.nuls.poc.pbft;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Block;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.Sha256Hash;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.poc.constant.ConsensusErrorCode;
import io.nuls.poc.constant.PocMessageType;
import io.nuls.poc.model.bo.Chain;
import io.nuls.poc.model.bo.round.MeetingMember;
import io.nuls.poc.model.bo.round.MeetingRound;
import io.nuls.poc.pbft.cache.VoteCenter;
import io.nuls.poc.pbft.message.VoteMessage;
import io.nuls.poc.pbft.model.PbftData;
import io.nuls.poc.pbft.model.VoteResultItem;
import io.nuls.poc.pbft.model.VoteRound;
import io.nuls.poc.rpc.call.CallMethodUtils;
import io.nuls.poc.utils.LoggerUtil;
import io.nuls.poc.utils.manager.RoundManager;

import java.io.IOException;

/**
 * @author Niels
 */
public class BlockVoter implements Runnable {

    private final int chainId;
    private final Chain chain;
    private final RoundManager roundManager;
    private VoteCenter cache;
    private VoteRound lastRound;

    private VoteRound nowRound;
    private BlockHeader lastHeader;

    public BlockVoter(Chain chain) {
        this.chainId = chain.getConfig().getChainId();
        this.chain = chain;
        this.roundManager = SpringLiteContext.getBean(RoundManager.class);
        this.cache = new VoteCenter(chainId);
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
        this.lastHeader = chain.getNewestHeader();

    }

    public ErrorCode recieveBlock(Block block) {
        ErrorCode code = ConsensusErrorCode.WAIT_BLOCK_VERIFY;
        long height = block.getHeader().getHeight();
        NulsHash hash = block.getHeader().getHash();
        PbftData pbftData = cache.addVote1(height, 1, hash, block.getHeader().getPackingAddress(chainId), block.getHeader().getTime());

        MeetingRound pocRound = this.getCurrentRound();
        int totalCount = pocRound.getMemberCount();
        if (totalCount == 1) {
            code = ConsensusErrorCode.SUCCESS;
            return code;
        }
        //判断自己是否需要签名，如果需要就直接进行
        MeetingMember self = pocRound.getMyMember();
        if (null != self && !pbftData.hasVoted1(self.getAgent().getPackingAddress())) {
            VoteMessage message = new VoteMessage();
            message.setHeight(height);
            message.setRound(1);
            message.setStep((byte) 0);
            if (block.getHeader().getTime() + 10 < NulsDateUtils.getCurrentTimeSeconds()) {
                message.setHash(null);
            } else {
                message.setHash(hash);
            }
            this.signAndBroadcast(self, message);
            cache.addVote1(height, 1, hash, self.getAgent().getPackingAddress(), block.getHeader().getTime());
        }
        VoteResultItem result = pbftData.getVote1LargestItem();
        if (result.getCount() > 0.66 * totalCount && !pbftData.hasVoted2(self.getAgent().getPackingAddress())) {
            VoteMessage message = new VoteMessage();
            message.setHeight(height);
            message.setRound(1);
            message.setStep((byte) 1);
            message.setHash(result.getHash());
            this.signAndBroadcast(self, message);
            cache.addVote2(height, 1, hash, self.getAgent().getPackingAddress(), block.getHeader().getTime());
        }
        return code;
    }

    private void signAndBroadcast(MeetingMember self, VoteMessage message) {
        //签名
        byte[] sign = new byte[0];
        try {
            sign = CallMethodUtils.signature(chain, AddressTool.getStringAddressByBytes(self.getAgent().getPackingAddress()), Sha256Hash.hash(message.serializeForDigest()));
        } catch (NulsException e) {
            LoggerUtil.commonLog.error(e);
        } catch (IOException e) {
            LoggerUtil.commonLog.error(e);
        }
        message.setSign(sign);

        CallMethodUtils.broadcastMsg(chainId, message, PocMessageType.VOTE_MESSAGE);
    }


    private MeetingRound getCurrentRound() {
        return this.roundManager.getCurrentRound(this.chain);
    }
}
