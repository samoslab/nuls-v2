package io.nuls.poc.model.bo;

/**
 * 交易手续费返回结果类
 * @author tag
 * */
public class ChargeResultData {
    private String fee;
    private int chainId;

    public ChargeResultData(String fee, int chainId) {
        this.fee = fee;
        this.chainId = chainId;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }
}
