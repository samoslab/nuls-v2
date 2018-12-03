package io.nuls.poc.model.bo.config;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * 共识模块配置类
 * @author tag
 * 2018/11/7
 * */
public class ConfigBean implements Serializable {
    /**
     * 打包间隔时间
     * */
    private long packing_interval;
    /**
     * 区块大小
     * */
    private int block_size;
    /**
     * 出块最小金额
     * */
    private BigInteger packing_amount;
    /**
     * 奖励金锁定块数
     * */
    private int coinbase_unlock_height;
    /**
     * 获得红牌保证金锁定时间
     * */
    private long redPublish_lockTime;
    /**
     * 注销节点保证金锁定时间
     * */
    private long stopAgent_lockTime;
    /**
     * 佣金比例的最小值
     * */
    private double commissionRate_min;
    /**
     * 佣金比例的最大值
     * */
    private double commissionRate_max;
    /**
     * 创建节点的保证金最小值
     * */
    private BigInteger deposit_min;
    /**
     * 创建节点的保证金最大值
     * */
    private BigInteger deposit_max;
    /**
     * 节点出块委托金额最小值
     * */
    private BigInteger commission_min;
    /**
     * 节点委托金额最大值
     * */
    private BigInteger commission_max;

    //委托最小金额
    private BigInteger entruster_deposit_min;

    //节点最多能被多少人委托
    private int deposit_number_max;
    /**
     * 种子节点
     **/
    private String seedNodes;

    //资产ID
    private int assetsId;

    //链ID
    private int chainId;

    public long getPacking_interval() {
        return packing_interval;
    }

    public void setPacking_interval(long packing_interval) {
        this.packing_interval = packing_interval;
    }

    public int getBlock_size() {
        return block_size;
    }

    public void setBlock_size(int block_size) {
        this.block_size = block_size;
    }

    public int getCoinbase_unlock_height() {
        return coinbase_unlock_height;
    }

    public void setCoinbase_unlock_height(int coinbase_unlock_height) {
        this.coinbase_unlock_height = coinbase_unlock_height;
    }
    public long getRedPublish_lockTime() {
        return redPublish_lockTime;
    }

    public void setRedPublish_lockTime(long redPublish_lockTime) {
        this.redPublish_lockTime = redPublish_lockTime;
    }

    public long getStopAgent_lockTime() {
        return stopAgent_lockTime;
    }

    public void setStopAgent_lockTime(long stopAgent_lockTime) {
        this.stopAgent_lockTime = stopAgent_lockTime;
    }

    public double getCommissionRate_min() {
        return commissionRate_min;
    }

    public void setCommissionRate_min(double commissionRate_min) {
        this.commissionRate_min = commissionRate_min;
    }

    public double getCommissionRate_max() {
        return commissionRate_max;
    }

    public void setCommissionRate_max(double commissionRate_max) {
        this.commissionRate_max = commissionRate_max;
    }

    public String getSeedNodes() {
        return seedNodes;
    }

    public void setSeedNodes(String seedNodes) {
        this.seedNodes = seedNodes;
    }

    public int getDeposit_number_max() {
        return deposit_number_max;
    }

    public void setDeposit_number_max(int deposit_number_max) {
        this.deposit_number_max = deposit_number_max;
    }

    public BigInteger getPacking_amount() {
        return packing_amount;
    }

    public void setPacking_amount(BigInteger packing_amount) {
        this.packing_amount = packing_amount;
    }

    public BigInteger getDeposit_min() {
        return deposit_min;
    }

    public void setDeposit_min(BigInteger deposit_min) {
        this.deposit_min = deposit_min;
    }

    public BigInteger getDeposit_max() {
        return deposit_max;
    }

    public void setDeposit_max(BigInteger deposit_max) {
        this.deposit_max = deposit_max;
    }

    public BigInteger getCommission_min() {
        return commission_min;
    }

    public void setCommission_min(BigInteger commission_min) {
        this.commission_min = commission_min;
    }

    public BigInteger getCommission_max() {
        return commission_max;
    }

    public void setCommission_max(BigInteger commission_max) {
        this.commission_max = commission_max;
    }

    public BigInteger getEntruster_deposit_min() {
        return entruster_deposit_min;
    }

    public void setEntruster_deposit_min(BigInteger entruster_deposit_min) {
        this.entruster_deposit_min = entruster_deposit_min;
    }

    public int getAssetsId() {
        return assetsId;
    }

    public void setAssetsId(int assetsId) {
        this.assetsId = assetsId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }
}
