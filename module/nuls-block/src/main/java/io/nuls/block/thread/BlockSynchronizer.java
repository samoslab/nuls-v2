/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.thread;

import com.google.common.collect.Lists;
import io.nuls.base.data.Block;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.po.BlockHeaderPo;
import io.nuls.block.constant.LocalBlockStateEnum;
import io.nuls.block.constant.StatusEnum;
import io.nuls.block.manager.BlockChainManager;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.ChainContext;
import io.nuls.block.model.ChainParameters;
import io.nuls.block.model.Node;
import io.nuls.block.rpc.call.ConsensusUtil;
import io.nuls.block.rpc.call.NetworkUtil;
import io.nuls.block.rpc.call.TransactionUtil;
import io.nuls.block.service.BlockService;
import io.nuls.block.storage.BlockStorageService;
import io.nuls.block.utils.BlockUtil;
import io.nuls.block.utils.ChainGenerator;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

import static io.nuls.block.BlockBootstrap.blockConfig;
import static io.nuls.block.constant.Constant.CONSENSUS_WORKING;
import static io.nuls.block.constant.Constant.NODE_COMPARATOR;
import static io.nuls.block.constant.LocalBlockStateEnum.*;

/**
 * 区块同步主线程,管理多条链的区块同步
 *
 * @author captain
 * @version 1.0
 * @date 18-11-8 下午5:49
 */
public class BlockSynchronizer implements Runnable {

    private static Map<Integer, BlockSynchronizer> synMap = new HashMap<>();

    private int chainId;

    private boolean running;

    /**
     * 区块同步过程中缓存的区块字节数
     */
    private AtomicInteger cachedBlockSize = new AtomicInteger(0);

    private static boolean firstStart = true;
    /**
     * 保存多条链的区块同步状态
     */
    private BlockService blockService;

    BlockSynchronizer(int chainId) {
        this.chainId = chainId;
        this.running = false;
        this.blockService = SpringLiteContext.getBean(BlockService.class);
    }

    public static void syn(int chainId) {
        ChainContext context = ContextManager.getContext(chainId);
        NulsLogger commonLog = context.getCommonLog();
        BlockSynchronizer blockSynchronizer = synMap.computeIfAbsent(chainId, BlockSynchronizer::new);
        if (!blockSynchronizer.isRunning()) {
            commonLog.info("blockSynchronizer run......");
            ThreadUtils.createAndRunThread("block-synchronizer", blockSynchronizer);
        } else {
            commonLog.info("blockSynchronizer already running......");
        }
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        setRunning(true);
        ChainContext context = ContextManager.getContext(chainId);
        context.setStatus(StatusEnum.SYNCHRONIZING);
        int synSleepInterval = context.getParameters().getSynSleepInterval();
        NulsLogger commonLog = context.getCommonLog();
        try {
            BlockStorageService blockStorageService = SpringLiteContext.getBean(BlockStorageService.class);
            long latestHeight = blockStorageService.queryLatestHeight(chainId);
            BlockHeaderPo blockHeader = blockStorageService.query(chainId, latestHeight);
            //如果上一次同步时保存区块报错,有可能本地的最新区块头数据是不准确的,需要进行验证
            if (!blockHeader.isComplete()) {
                commonLog.info("clean incomplete block between block-syn, incomplete block generated by last failed block-syn");
                if (!TransactionUtil.rollback(chainId, blockHeader)) {
                    commonLog.error("TransactionUtil rollback error when clean incomplete block ");
                    System.exit(1);
                }
                if (!blockStorageService.remove(chainId, latestHeight)) {
                    commonLog.error("blockStorageService remove error when clean incomplete block ");
                    System.exit(1);
                }
                latestHeight = latestHeight - 1;
                if (!blockStorageService.setLatestHeight(chainId, latestHeight)) {
                    commonLog.error("blockStorageService setLatestHeight error when clean incomplete block ");
                    System.exit(1);
                }
                //latestHeight已经维护成功,上面的步骤保证了latestHeight这个高度的区块数据在本地是完整的,但是区块数据的内容并不一定是正确的,所以要继续验证latestBlock
                Block block = blockService.getBlock(chainId, latestHeight);
                //本地区块维护成功
                context.setLatestBlock(block);
                BlockChainManager.setMasterChain(chainId, ChainGenerator.generateMasterChain(chainId, block, blockService));
            }
            //系统启动后自动回滚区块,回滚数量testAutoRollbackAmount写在配置文件中
            if (firstStart) {
                firstStart = false;
                int testAutoRollbackAmount = blockConfig.getTestAutoRollbackAmount();
                if (testAutoRollbackAmount > 0) {
                    if (latestHeight < testAutoRollbackAmount) {
                        testAutoRollbackAmount = (int) (latestHeight);
                    }
                    for (int i = 0; i < testAutoRollbackAmount; i++) {
                        boolean b = blockService.rollbackBlock(chainId, latestHeight--, true);
                        if (!b || latestHeight == 0) {
                            break;
                        }
                    }
                }
            }
            waitUntilNetworkStable();
            while (!synchronize()) {
                Thread.sleep(synSleepInterval);
            }
        } catch (Exception e) {
            commonLog.error("", e);
        } finally {
            setRunning(false);
        }
    }

    /**
     * 等待网络稳定
     * 每隔5秒请求一次getAvailableNodes,连续5次节点数大于minNodeAmount就认为网络稳定
     *
     * @return
     */
    private void waitUntilNetworkStable() throws InterruptedException {
        ChainContext context = ContextManager.getContext(chainId);
        ChainParameters parameters = context.getParameters();
        int waitNetworkInterval = parameters.getWaitNetworkInterval();
        int minNodeAmount = parameters.getMinNodeAmount();
        NulsLogger commonLog = context.getCommonLog();
        List<Node> availableNodes;
        int nodeAmount;
        int count = 0;
        while (true) {
            availableNodes = NetworkUtil.getAvailableNodes(chainId);
            nodeAmount = availableNodes.size();
            if (nodeAmount >= minNodeAmount) {
                count++;
            } else {
                count = 0;
            }
            commonLog.info("minNodeAmount = " + minNodeAmount + ", current nodes amount=" + nodeAmount + ", count = " + count + ", wait Until Network Stable......");
            if (count >= 6) {
                return;
            }
            Thread.sleep(waitNetworkInterval);
        }
    }

    private boolean synchronize() throws Exception {
        NulsLogger commonLog = ContextManager.getContext(chainId).getCommonLog();
        //1.调用网络模块接口获取当前chainId网络的可用节点
        List<Node> availableNodes = NetworkUtil.getAvailableNodes(chainId);
        //2.判断可用节点数是否满足最小配置
        ChainContext context = ContextManager.getContext(chainId);
        ChainParameters parameters = context.getParameters();
        int minNodeAmount = parameters.getMinNodeAmount();
        if (minNodeAmount == 0 && availableNodes.isEmpty()) {
            commonLog.info("skip block syn, because minNodeAmount is set to 0, minNodeAmount should't set to 0 otherwise you want run local node without connect with network");
            context.setStatus(StatusEnum.RUNNING);
            ConsensusUtil.notice(chainId, CONSENSUS_WORKING);
            return true;
        }
        //3.统计网络中可用节点的一致区块高度、区块hash
        BlockDownloaderParams params = statistics(availableNodes, context);
        int size = params.getNodes().size();
        //网络上没有可用的一致节点,就是节点高度都不一致,或者一致的节点比例不够
        if (size == 0) {
            commonLog.warn("chain-" + chainId + ", no consistent nodes, availableNodes-" + availableNodes);
            return false;
        }
        //网络上所有节点高度都是0,说明是该链第一次运行
        if (params.getNetLatestHeight() == 0 && size == availableNodes.size()) {
            commonLog.info("chain-" + chainId + ", first start");
            context.setStatus(StatusEnum.RUNNING);
            ConsensusUtil.notice(chainId, CONSENSUS_WORKING);
            return true;
        }
        //检查本地区块状态
        LocalBlockStateEnum stateEnum = checkLocalBlock(chainId, params);
        if (stateEnum.equals(CONSISTENT)) {
            commonLog.info("chain-" + chainId + ", local blocks is newest");
            context.setStatus(StatusEnum.RUNNING);
            ConsensusUtil.notice(chainId, CONSENSUS_WORKING);
            return true;
        }
        if (stateEnum.equals(UNCERTAINTY)) {
            commonLog.warn("chain-" + chainId + ", The number of rolled back blocks exceeded the configured value");
            NetworkUtil.resetNetwork(chainId);
            waitUntilNetworkStable();
            return false;
        }
        if (stateEnum.equals(CONFLICT)) {
            commonLog.error("chain-" + chainId + ", The local GenesisBlock differ from network");
            System.exit(1);
        }
        PriorityBlockingQueue<Node> nodes = params.getNodes();
        int nodeCount = nodes.size();
        ThreadPoolExecutor executor = ThreadUtils.createThreadPool(nodeCount * 4, 0, new NulsThreadFactory("worker-" + chainId));
        BlockingQueue<Block> queue = new LinkedBlockingQueue<>();
        BlockingQueue<Future<BlockDownLoadResult>> futures = new LinkedBlockingQueue<>();
        long netLatestHeight = params.getNetLatestHeight();
        long startHeight = params.getLocalLatestHeight() + 1;
        long total = netLatestHeight - startHeight + 1;
        long start = System.currentTimeMillis();
        //5.开启区块下载器BlockDownloader
        BlockDownloader downloader = new BlockDownloader(chainId, futures, executor, params, queue, cachedBlockSize);
        Future<Boolean> downloadFutrue = ThreadUtils.asynExecuteCallable(downloader);
        //6.开启区块收集线程BlockCollector,收集BlockDownloader下载的区块
        BlockCollector collector = new BlockCollector(chainId, futures, executor, params, queue, cachedBlockSize);
        ThreadUtils.createAndRunThread("block-collector-" + chainId, collector);
        //7.开启区块消费线程BlockConsumer,与上面的BlockDownloader共用一个队列blockQueue
        BlockConsumer consumer = new BlockConsumer(chainId, queue, params, cachedBlockSize);
        Future<Boolean> consumerFuture = ThreadUtils.asynExecuteCallable(consumer);
        Boolean downResult = downloadFutrue.get();
        Boolean storageResult = consumerFuture.get();
        boolean success = downResult != null && downResult && storageResult != null && storageResult;
        long end = System.currentTimeMillis();
        executor.shutdownNow();
        if (success) {
            commonLog.info("block syn complete, total download:" + total + ", total time:" + (end - start) + ", average time:" + (end - start) / total);
            if (checkIsNewest(context)) {
                //要测试分叉链切换或者孤儿链,放开下面语句,概率会加大
//                if (true) {
                commonLog.info("block syn complete successfully, current height-" + params.getNetLatestHeight());
                System.gc();
                context.setStatus(StatusEnum.RUNNING);
                ConsensusUtil.notice(chainId, CONSENSUS_WORKING);
                return true;
            } else {
                commonLog.warn("block syn complete but is not newest");
            }
        } else {
            commonLog.error("block syn fail, downResult:" + downResult + ", storageResult:" + storageResult);
            context.setDoSyn(true);
        }
        return false;
    }

    /**
     * 检查本地区块是否同步到最新高度,如果不是最新高度,变更同步状态为BlockSynStatusEnum.WAITING,等待下次同步
     *
     * @param context
     * @return
     * @throws Exception
     */
    private boolean checkIsNewest(ChainContext context) {
        BlockDownloaderParams newestParams = statistics(NetworkUtil.getAvailableNodes(chainId), context);
        return newestParams.getNetLatestHeight() <= context.getLatestBlock().getHeader().getHeight();
    }

    /**
     * 统计网络中可用节点的一致区块高度、区块hash,构造下载参数
     *
     * @param
     * @param context
     * @return
     * @date 18-11-8 下午4:55
     */
    BlockDownloaderParams statistics(List<Node> availableNodes, ChainContext context) {
        BlockDownloaderParams params = new BlockDownloaderParams();
        params.setAvailableNodesCount(availableNodes.size());
        PriorityBlockingQueue<Node> nodeQueue = new PriorityBlockingQueue<>(availableNodes.size(), NODE_COMPARATOR);
        params.setNodes(nodeQueue);
        //每个节点的(最新HASH+最新高度)是key
        String key = "";
        int count = 0;
        //一个以key为主键记录持有该key的节点列表
        Map<String, List<Node>> nodeMap = new HashMap<>(availableNodes.size());
        //一个以key为主键统计次数
        Map<String, Integer> countMap = new HashMap<>(availableNodes.size());
        for (Node node : availableNodes) {
            String tempKey = node.getHash().getDigestHex() + node.getHeight();
            if (countMap.containsKey(tempKey)) {
                //tempKey已存在,统计次数加1
                countMap.put(tempKey, countMap.get(tempKey) + 1);
            } else {
                //tempKey不存在,初始化统计次数
                countMap.put(tempKey, 1);
            }

            if (nodeMap.containsKey(tempKey)) {
                //tempKey已存在,添加到持有节点列表中
                List<Node> nodes = nodeMap.get(tempKey);
                nodes.add(node);
            } else {
                //tempKey不存在,新增持有节点列表
                nodeMap.put(tempKey, Lists.newArrayList(node));
            }
        }
        //最终统计出出现频率最大的key,就获取到当前可信的最新高度与最新hash,以及可信的节点列表
        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            Integer value = entry.getValue();
            if (value > count) {
                count = value;
                key = entry.getKey();
            }
        }
        ChainParameters parameters = context.getParameters();
        double div = DoubleUtils.div(count, availableNodes.size(), 2);
        if (div * 100 < parameters.getConsistencyNodePercent()) {
            return params;
        }
        List<Node> nodeList = nodeMap.get(key);
        nodeQueue.addAll(nodeList);
        params.setList(nodeList);
        Node node = nodeQueue.peek();
        params.setNetLatestHash(node.getHash());
        params.setNetLatestHeight(node.getHeight());

        // a read-only method
        // upgrade from optimistic read to read lock
        StampedLock lock = context.getLock();
        long stamp = lock.tryOptimisticRead();
        try {
            for (; ; stamp = lock.readLock()) {
                if (stamp == 0L) {
                    continue;
                }
                // possibly racy reads
                params.setLocalLatestHeight(context.getLatestHeight());
                params.setLocalLatestHash(context.getLatestBlock().getHeader().getHash());
                if (!lock.validate(stamp)) {
                    continue;
                }
                return params;
            }
        } finally {
            if (StampedLock.isReadLockStamp(stamp)) {
                lock.unlockRead(stamp);
            }
        }
    }

    /**
     * 区块同步前,与网络区块作对比,检查本地区块是否需要回滚
     *
     * @param chainId 链Id/chain id
     * @param params
     * @return
     */
    private LocalBlockStateEnum checkLocalBlock(int chainId, BlockDownloaderParams params) {
        long localHeight = params.getLocalLatestHeight();
        long netHeight = params.getNetLatestHeight();
        //得到共同高度
        long commonHeight = Math.min(localHeight, netHeight);
        if (checkHashEquality(params)) {
            //commonHeight区块的hash一致,正常,比远程节点落后,下载区块
            if (commonHeight < netHeight) {
                return INCONSISTENT;
            } else {
                return CONSISTENT;
            }
        } else {
            //需要回滚的场景,要满足可用节点数(10个)>配置,一致可用节点数(6个)占比超80%两个条件
            ChainParameters parameters = ContextManager.getContext(chainId).getParameters();
            if (params.getNodes().size() >= parameters.getMinNodeAmount()
                    && params.getAvailableNodesCount() >= params.getNodes().size() * parameters.getConsistencyNodePercent() / 100
            ) {
                return checkRollback(0, params);
            }
        }
        return INCONSISTENT;
    }

    private LocalBlockStateEnum checkRollback(int rollbackCount, BlockDownloaderParams params) {
        //每次最多回滚maxRollback个区块,等待下次同步,这样可以避免被恶意节点攻击,大量回滚正常区块.
        ChainParameters parameters = ContextManager.getContext(chainId).getParameters();
        if (params.getLocalLatestHeight() == 0) {
            return CONFLICT;
        }
        if (rollbackCount >= parameters.getMaxRollback()) {
            return UNCERTAINTY;
        }
        blockService.rollbackBlock(chainId, params.getLocalLatestHeight(), true);
        BlockHeader latestBlockHeader = blockService.getLatestBlockHeader(chainId);
        params.setLocalLatestHeight(latestBlockHeader.getHeight());
        params.setLocalLatestHash(latestBlockHeader.getHash());
        if (checkHashEquality(params)) {
            return INCONSISTENT;
        }
        return checkRollback(rollbackCount + 1, params);
    }

    /**
     * 根据传入的区块localBlock判断localBlock.hash与网络上同高度的区块hash是否一致
     *
     * @author captain
     * @date 18-11-9 下午6:13
     * @version 1.0
     */
    private boolean checkHashEquality(BlockDownloaderParams params) {
        NulsDigestData localHash = params.getLocalLatestHash();
        long localHeight = params.getLocalLatestHeight();
        long netHeight = params.getNetLatestHeight();
        NulsDigestData netHash = params.getNetLatestHash();
        //得到共同高度
        long commonHeight = Math.min(localHeight, netHeight);
        //如果双方共同高度<网络高度,要进行hash判断,需要从网络上下载区块,因为params里只有最新的区块hash,没有旧的hash
        if (commonHeight < netHeight) {
            for (Node node : params.getNodes()) {
                Block remoteBlock = BlockUtil.downloadBlockByHash(chainId, localHash, node.getId());
                if (remoteBlock != null) {
                    netHash = remoteBlock.getHeader().getHash();
                    return localHash.equals(netHash);
                }
            }
            //如果从网络上下载区块失败,返回false
            return false;
        }
        if (commonHeight < localHeight) {
            localHash = blockService.getBlockHash(chainId, commonHeight);
        }
        return localHash.equals(netHash);
    }
}
