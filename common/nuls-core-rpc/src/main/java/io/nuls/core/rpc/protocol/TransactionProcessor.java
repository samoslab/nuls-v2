package io.nuls.core.rpc.protocol;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;

import java.util.List;

/**
 * 交易处理器
 *
 * @author captain
 * @version 1.0
 * @date 2019/5/23 17:43
 */
public interface TransactionProcessor {

    /**
     * 获取该交易器绑定的交易类型,参见{@link TxType}
     *
     * @return
     */
    int getType();

    /**
     * 验证接口
     *
     * @param chainId       链Id
     * @param txs           类型为{@link #getType()}的所有交易集合
     * @param allTxs        本模块注册的所有交易集合,可用于不同类型交易间的冲突检测
     * @param blockHeader   区块头
     * @return 未通过验证的交易,需要丢弃
     */
    List<Transaction> validate(int chainId, List<Transaction> txs, List<Transaction> allTxs, BlockHeader blockHeader);

    /**
     * 提交接口
     *
     * @param chainId       链Id
     * @param txs           类型为{@link #getType()}的所有交易集合
     * @param blockHeader   区块头
     * @return 是否提交成功
     */
    boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader);

    /**
     * 回滚接口
     *
     * @param chainId       链Id
     * @param txs           类型为{@link #getType()}的所有交易集合
     * @param blockHeader   区块头
     * @return 是否回滚成功
     */
    boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader);

}
