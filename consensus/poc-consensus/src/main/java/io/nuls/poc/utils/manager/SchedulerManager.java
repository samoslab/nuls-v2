package io.nuls.poc.utils.manager;

import io.nuls.poc.model.bo.config.ConfigBean;
import io.nuls.poc.utils.thread.ConsensusProcessTask;
import io.nuls.poc.utils.thread.process.ConsensusProcess;
import io.nuls.tools.thread.ThreadUtils;
import io.nuls.tools.thread.commom.NulsThreadFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务管理器
 * @author tag
 * 2018/11/9
 * */
public class SchedulerManager {

    /**
     * 存储链与任务管理器的对应关系
     * */
    public static Map<Integer, ScheduledThreadPoolExecutor> scheduleMap = new HashMap<>();

    /**
     * 创建多条链的任务
     * @param chainMap  多条链的配置
     * */
    public static void createChainSchefuler(Map<Integer,ConfigBean> chainMap){
        for (Map.Entry<Integer,ConfigBean> entry:chainMap.entrySet()) {
            createChainScheduler(entry.getKey());
        }
    }

    /**
     * 创建一条链的任务
     * @param chain_id 链ID
     * */
    public static void createChainScheduler(int chain_id){
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadUtils.createScheduledThreadPool(3,new NulsThreadFactory("consensus"+chain_id));
        //创建链相关的任务
        ConsensusProcess consensusProcess = new ConsensusProcess();
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new ConsensusProcessTask(chain_id,consensusProcess),1000L,100L, TimeUnit.MILLISECONDS);
        scheduleMap.put(chain_id,scheduledThreadPoolExecutor);
    }
}
