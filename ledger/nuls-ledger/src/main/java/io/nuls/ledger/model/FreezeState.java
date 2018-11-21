package io.nuls.ledger.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * account balance lock
 * Created by wangkun23 on 2018/11/21.
 */
@ToString
public class FreezeState implements Serializable {

    /**
     * 交易的hash值
     */
    @Setter
    @Getter
    private String txHash;

    /**
     * 锁定金额
     */
    @Setter
    @Getter
    private long amount;

    /**
     * 锁定时间
     */
    @Setter
    @Getter
    private long lockTime;

    @Setter
    @Getter
    private long createTime;
}
