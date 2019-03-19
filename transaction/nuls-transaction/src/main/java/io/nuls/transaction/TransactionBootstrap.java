/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.transaction;

import io.nuls.db.service.RocksDBService;
import io.nuls.db.util.DBUtils;
import io.nuls.h2.utils.MybatisDbHelper;
import io.nuls.rpc.info.HostInfo;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.modulebootstrap.Module;
import io.nuls.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.rpc.modulebootstrap.RpcModule;
import io.nuls.rpc.modulebootstrap.RpcModuleState;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.ConfigLoader;
import io.nuls.tools.parse.I18nUtils;
import io.nuls.transaction.constant.TxConfig;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxDBConstant;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.rpc.call.BlockCall;
import io.nuls.transaction.rpc.call.NetworkCall;
import io.nuls.transaction.storage.h2.TransactionH2Service;
import io.nuls.transaction.storage.rocksdb.LanguageStorageService;
import io.nuls.transaction.utils.DBUtil;
import io.nuls.transaction.utils.LoggerUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: Charlie
 * @date: 2019/3/4
 */
@Component
public class TransactionBootstrap extends RpcModule {

    @Autowired
    TxConfig txConfig;

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"ws://" + HostInfo.getLocalIP() + ":8887/ws"};
        }
        NulsRpcModuleBootstrap.run("io.nuls", args);
    }

    @Override
    public void init() {
        try {
            //初始化系统参数
            initSys();
            //初始化数据库配置文件
            initDB();

        } catch (Exception e) {
            Log.error("Transaction init error!");
            Log.error(e);
        }
    }

    @Override
    public boolean doStart() {
        //初始化国际资源文件语言
        try {
            initLanguage();
            initH2Table();
            SpringLiteContext.getBean(ChainManager.class).runChain();
            Log.info("Transaction Ready...");
            return true;
        } catch (Exception e) {
            Log.error("Transaction init error!");
            Log.error(e);
            return false;
        }
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        Log.info("Transaction onDependenciesReady");
        try {
            NetworkCall.registerProtocol();
            subscriptionBlockHeight();
            Log.info("Transaction Running...");
            return RpcModuleState.Running;
        } catch (Exception e) {
            LoggerUtil.Log.error(e);
            return RpcModuleState.Ready;
        }

    }

    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        return RpcModuleState.Ready;
    }

    @Override
    public Module[] getDependencies() {
        return new Module[]{
                new Module(ModuleE.NW.abbr, TxConstant.RPC_VERSION),
                new Module(ModuleE.LG.abbr, TxConstant.RPC_VERSION),
                new Module(ModuleE.BL.abbr, TxConstant.RPC_VERSION)
        };
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.TX.abbr, TxConstant.RPC_VERSION);
    }

    @Override
    public String getRpcCmdPackage() {
        return TxConstant.TX_CMD_PATH;
    }

    /**
     * 初始化系统编码
     */
    private void initSys() {
        try {
            System.setProperty(TxConstant.SYS_ALLOW_NULL_ARRAY_ELEMENT, "true");
            System.setProperty(TxConstant.SYS_FILE_ENCODING, UTF_8.name());
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, UTF_8);
        } catch (Exception e) {
            LoggerUtil.Log.error(e);
        }
    }

    public void initDB() {
        try {
            //数据文件存储地址
            RocksDBService.init(txConfig.getDataPath() + txConfig.getTxDataRoot());
            //模块配置表
            DBUtil.createTable(TxDBConstant.DB_MODULE_CONGIF);
            //语言表
            DBUtil.createTable(TxDBConstant.DB_TX_LANGUAGE);

            //todo 单个节点跑多链的时候 h2是否需要通过chain来区分数据库(如何分？)，待确认！！
            String resource = "mybatis/mybatis-config.xml";
            Properties prop =  ConfigLoader.loadProperties(TxConstant.DB_CONFIG_NAME);
            String currentPath = DBUtils.genAbsolutePath(txConfig.getDataPath() + txConfig.getTxDataRoot());
            LoggerUtil.Log.debug("#########################:" + currentPath);
            prop.setProperty("url", "jdbc:h2:file:" + currentPath + "/h2/nuls;LOG=2;DB_CLOSE_DELAY=-1;TRACE_LEVEL_SYSTEM_OUT=1;DATABASE_TO_UPPER=FALSE;MV_STORE=false;COMPRESS=true;MAX_COMPACT_TIME=5000");
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader(resource), "druid",prop);
            MybatisDbHelper.setSqlSessionFactory(sqlSessionFactory);
        } catch (Exception e) {
            LoggerUtil.Log.error(e);
        }
    }

    /**
     * 初始化国际化资源文件语言
     */
    public void initLanguage() {
        try {
            LanguageStorageService languageService = SpringLiteContext.getBean(LanguageStorageService.class);
            String languageDB = languageService.getLanguage();
            I18nUtils.loadLanguage(TransactionBootstrap.class, "languages", txConfig.getLanguage());
            String language = null == languageDB ? I18nUtils.getLanguage() : languageDB;
            I18nUtils.setLanguage(language);
            if (null == languageDB) {
                languageService.saveLanguage(language);
            }
        } catch (Exception e) {
            LoggerUtil.Log.error(e);
        }
    }

    /**
     * 创建H2的表, 如果存在则不会创建
     */
    private void initH2Table() {
        TransactionH2Service ts = SpringLiteContext.getBean(TransactionH2Service.class);
        ts.createTxTablesIfNotExists(TxConstant.H2_TX_TABLE_NAME_PREFIX,
                TxConstant.H2_TX_TABLE_INDEX_NAME_PREFIX,
                TxConstant.H2_TX_TABLE_UNIQUE_NAME_PREFIX,
                txConfig.getH2TxTableNumber());
    }

    /**
     * 订阅最新区块高度
     */
    private void subscriptionBlockHeight() {
        try {
            ChainManager chainManager = SpringLiteContext.getBean(ChainManager.class);
            for (Map.Entry<Integer, Chain> entry : chainManager.getChainMap().entrySet()) {
                Chain chain = entry.getValue();
                //订阅Block模块接口
                BlockCall.subscriptionNewBlockHeight(chain);
            }
        } catch (NulsException e) {
            throw new RuntimeException(e);
        }
    }
}
