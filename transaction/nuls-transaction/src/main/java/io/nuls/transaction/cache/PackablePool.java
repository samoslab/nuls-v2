package io.nuls.transaction.cache;

import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.tools.core.annotation.Component;
import io.nuls.transaction.model.bo.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 交易已完成交易管理模块的校验(打包的时候从这里取), 包括孤儿交易
 *
 * @author: Charlie
 * @date: 2018/11/13
 */
@Component
public class PackablePool {

    public boolean addInFirst(Chain chain, Transaction tx, boolean isOrphan) {
        try {
            if (tx == null) {
                return false;
            }
            //check Repeatability
            if (isOrphan) {
                NulsDigestData hash = tx.getHash();
                chain.getOrphanContainer().put(hash, tx);
            } else {
                chain.getTxQueue().addFirst(tx);
            }
            return true;
        } finally {
        }
    }

    public boolean add(Chain chain, Transaction tx, boolean isOrphan) {
        try {
            if (tx == null) {
                return false;
            }
            //check Repeatability
            if (isOrphan) {
                NulsDigestData hash = tx.getHash();
                chain.getOrphanContainer().put(hash, tx);
            } else {
                chain.getTxQueue().offer(tx);
            }
            return true;
        } finally {
        }
    }

    /**
     * Get a TxContainer, the first TxContainer received, removed from the memory pool after acquisition
     * <p>
     * 获取一笔交易，最先存入的交易，获取之后从内存池中移除
     *
     * @return TxContainer
     */
    public Transaction get(Chain chain) {
        return chain.getTxQueue().poll();
    }

    public List<Transaction> getAll(Chain chain) {
        List<Transaction> txs = new ArrayList<>();
        Iterator<Transaction> it = chain.getTxQueue().iterator();
        while (it.hasNext()) {
            txs.add(it.next());
        }
        return txs;
    }

    public boolean exist(Chain chain, Transaction tx, boolean isOrphan) {
        if (isOrphan) {
            return chain.getOrphanContainer().containsKey(tx.getHash());
        } else {
            return chain.getTxQueue().contains(tx);
        }
    }

    public int getPoolSize(Chain chain) {
        return chain.getTxQueue().size();
    }

    public List<Transaction> getAllOrphan(Chain chain) {
        return new ArrayList<>(chain.getOrphanContainer().values());
    }

    public void removeOrphan(Chain chain, NulsDigestData hash) {
        chain.getOrphanContainer().remove(hash);
    }

    public boolean existOrphan(Chain chain, NulsDigestData hash) {
        return chain.getOrphanContainer().containsKey(hash);
    }

    public void clear(Chain chain) {
        try {
            chain.getTxQueue().clear();
            chain.getOrphanContainer().clear();
        } finally {
        }
    }

    public int getOrphanPoolSize(Chain chain) {
        return chain.getOrphanContainer().size();
    }

}
