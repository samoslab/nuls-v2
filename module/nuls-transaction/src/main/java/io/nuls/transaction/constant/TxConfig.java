package io.nuls.transaction.constant;

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.basic.VersionChangeInvoker;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.transaction.model.bo.config.ConfigBean;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Transaction module setting
 * @author: Charlie
 * @date: 2019/03/14
 */
@Component
@Configuration(domain = ModuleE.Constant.TRANSACTION)
public class TxConfig extends ConfigBean implements ModuleConfig {
    /**
     * ROCK DB 数据库文件存储路径
     */
    private String dataPath;
    /** 交易模块数据根目录*/
    private String txDataRoot;
    /** 模块code*/
    private String moduleCode;
    /** 主链链ID*/
    private int mainChainId;
    /** 主链主资产ID*/
    private int mainAssetId;
    /** 编码*/
    private String encoding;
    /** 未确认交易过期毫秒数-30分钟 */
    private long unconfirmedTxExpireMs;


    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getTxDataRoot() {
        return dataPath + File.separator + txDataRoot;
    }

    public void setTxDataRoot(String txDataRoot) {
        this.txDataRoot = txDataRoot;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public int getMainChainId() {
        return mainChainId;
    }

    public void setMainChainId(int mainChainId) {
        this.mainChainId = mainChainId;
    }

    public int getMainAssetId() {
        return mainAssetId;
    }

    public void setMainAssetId(int mainAssetId) {
        this.mainAssetId = mainAssetId;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public long getUnconfirmedTxExpireMs() {
        return unconfirmedTxExpireMs;
    }

    public void setUnconfirmedTxExpireMs(long unconfirmedTxExpireMs) {
        this.unconfirmedTxExpireMs = unconfirmedTxExpireMs;
    }

    @Override
    public VersionChangeInvoker getVersionChangeInvoker() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?> aClass = Class.forName("io.nuls.transaction.constant.TxVersionChangeInvoker");
        return (VersionChangeInvoker) aClass.getDeclaredConstructor().newInstance();
    }
}
