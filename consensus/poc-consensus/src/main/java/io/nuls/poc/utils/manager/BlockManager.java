package io.nuls.poc.utils.manager;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.poc.constant.ConsensusConstant;
import io.nuls.poc.model.bo.Chain;
import io.nuls.poc.utils.compare.BlockHeaderComparator;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.log.Log;

import java.util.*;

/**
 * 链区块管理类
 * Chain Block Management Class
 *
 * @author tag
 * 2018/12/20
 */
@Component
public class BlockManager {
    @Autowired
    private RoundManager roundManager;
    /**
     * 初始化链区块头数据，缓存指定数量的区块头
     * Initialize chain block header entity to cache a specified number of block headers
     *
     * @param chain chain info
     */
    @SuppressWarnings("unchecked")
    public void loadBlockHeader(Chain chain) throws Exception {
        Map params = new HashMap(ConsensusConstant.INIT_CAPACITY);
        params.put("chainId", chain.getConfig().getChainId());
        params.put("round", ConsensusConstant.INIT_BLOCK_HEADER_COUNT);
        Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.BL.abbr, "getLatestRoundBlockHeaders", params);
        Map<String, Object> resultMap;
        List<String> blockHeaderHexs = new ArrayList<>();
        if(cmdResp.isSuccess()){
            resultMap = (Map<String, Object>) cmdResp.getResponseData();
            blockHeaderHexs = (List<String>) resultMap.get("getLatestRoundBlockHeaders");
        }
        while(!cmdResp.isSuccess() && blockHeaderHexs.size() == 0){
            cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.BL.abbr, "getLatestRoundBlockHeaders", params);
            if(cmdResp.isSuccess()){
                resultMap = (Map<String, Object>) cmdResp.getResponseData();
                blockHeaderHexs = (List<String>) resultMap.get("getLatestRoundBlockHeaders");
                break;
            }
            Log.info("---------------------------区块加载失败！");
            Thread.sleep(1000);
        }
        List<BlockHeader> blockHeaders = new ArrayList<>();
        for (String blockHeaderHex : blockHeaderHexs) {
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.parse(HexUtil.decode(blockHeaderHex), 0);
            blockHeaders.add(blockHeader);
        }
        Collections.sort(blockHeaders, new BlockHeaderComparator());
        chain.setBlockHeaderList(blockHeaders);
        chain.setNewestHeader(blockHeaders.get(blockHeaders.size() - 1));
        Log.info("---------------------------区块加载成功！");
    }

    /**
     * 收到最新区块头，更新链区块缓存数据
     * Receive the latest block header, update the chain block cache entity
     *
     * @param chain       chain info
     * @param blockHeader block header
     */
    public void addNewBlock(Chain chain, BlockHeader blockHeader) {
        /*
        如果新增区块有轮次变化，则删除最小轮次区块
         */
        BlockHeader newestHeader = chain.getNewestHeader();
        BlockExtendsData newestExtendsData = new BlockExtendsData(newestHeader.getExtend());
        BlockExtendsData receiveExtendsData = new BlockExtendsData(blockHeader.getExtend());
        long receiveRoundIndex = receiveExtendsData.getRoundIndex();
        BlockExtendsData lastExtendsData = new BlockExtendsData(chain.getBlockHeaderList().get(0).getExtend());
        long lastRoundIndex = lastExtendsData.getRoundIndex();
        if(receiveRoundIndex > newestExtendsData.getRoundIndex() && (receiveRoundIndex - ConsensusConstant.INIT_BLOCK_HEADER_COUNT > lastRoundIndex)){
            Iterator<BlockHeader> iterator = chain.getBlockHeaderList().iterator();
            while (iterator.hasNext()){
                lastExtendsData = new BlockExtendsData(iterator.next().getExtend());
                if(lastExtendsData.getRoundIndex() == lastRoundIndex){
                    iterator.remove();
                }else if(lastExtendsData.getRoundIndex() > lastRoundIndex){
                    break;
                }
            }
        }
        chain.getBlockHeaderList().add(blockHeader);
        chain.setNewestHeader(blockHeader);
        chain.getLoggerMap().get(ConsensusConstant.BASIC_LOGGER_NAME).info("新区块保存成功，新区块高度为：" + blockHeader.getHeight() + ",本地最新区块高度为：" + chain.getNewestHeader().getHeight());
    }

    /**
     * 链分叉，区块回滚
     * Chain bifurcation, block rollback
     *
     * @param chain  chain info
     * @param height block height
     */
    public void chainRollBack(Chain chain, int height) {
        List<BlockHeader> headerList = chain.getBlockHeaderList();
        Collections.sort(headerList, new BlockHeaderComparator());
        for (int index = headerList.size() - 1; index >= 0; index--) {
            if (headerList.get(index).getHeight() >= height) {
                headerList.remove(index);
            } else {
                break;
            }
        }
        chain.setBlockHeaderList(headerList);
        chain.setNewestHeader(headerList.get(headerList.size() - 1));
        roundManager.rollBackRound(chain,height);
        chain.getLoggerMap().get(ConsensusConstant.BASIC_LOGGER_NAME).info("区块回滚成功，回滚到的高度为：" + height + ",本地最新区块高度为：" + chain.getNewestHeader().getHeight());
    }
}
