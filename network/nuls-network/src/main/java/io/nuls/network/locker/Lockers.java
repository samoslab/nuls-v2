package io.nuls.network.locker;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @description  全局锁,用于同步代码片段
 *                 Lockers
 * @author  lan
 * @date 2018/11/13
 **/
public class Lockers {
    /**
     * 用于peer发起连接代码片段锁定。
     * Used for peer-initiated connection code fragment locking
     */
    public final static Lock NODE_LAUNCH_CONNECT_LOCK = new ReentrantLock();
    /**
     * 用于已建立的peer连接 增加与移除的锁定。
     * Used for established peer connections to establish and disconnect locks.
     */
    public final static Lock NODE_ESTABLISH_CONNECT_LOCK = new ReentrantLock();
}
