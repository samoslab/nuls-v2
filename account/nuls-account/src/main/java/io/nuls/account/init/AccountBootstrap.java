package io.nuls.account.init;

import io.nuls.account.config.NulsConfig;
import io.nuls.account.constant.AccountConstant;
import io.nuls.account.constant.AccountParam;
import io.nuls.account.rpc.call.TransactionCmdCall;
import io.nuls.db.service.RocksDBService;
import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.server.WsServer;
import io.nuls.tools.core.inteceptor.ModularServiceMethodInterceptor;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.ConfigLoader;
import io.nuls.tools.parse.I18nUtils;
import io.nuls.tools.thread.TimeService;

import java.io.IOException;

/**
 * @author: qinyifeng
 * @date: 2018/10/15
 */
public class AccountBootstrap {
    public static void main(String[] args) {
        Log.info("Account Bootstrap start...");

        try {
            //初始化配置
            initCfg();
            //读取配置文件，数据存储根目录，初始化打开该目录下所有表连接并放入缓存
            RocksDBService.init(AccountParam.getInstance().getDataPath());
            //springLite容器初始化
            SpringLiteContext.init("io.nuls.account", new ModularServiceMethodInterceptor());
            //启动时间同步线程
            TimeService.getInstance().start();
            //启动账户模块服务
            initServer();
            //注册账户交易
            TransactionCmdCall.register();
        } catch (Exception e) {
            Log.error("Account Bootstrap failed", e);
            System.exit(-1);
        }
    }

    public static void initCfg() {
        try {
            NulsConfig.MODULES_CONFIG = ConfigLoader.loadIni(NulsConfig.MODULES_CONFIG_FILE);

            AccountParam accountParam = AccountParam.getInstance();
            //set data save path
            accountParam.setDataPath(NulsConfig.MODULES_CONFIG.getCfgValue(AccountConstant.DB_SECTION, AccountConstant.DB_DATA_PATH, null));

            try {
                //set system encoding
                NulsConfig.DEFAULT_ENCODING = NulsConfig.MODULES_CONFIG.getCfgValue(AccountConstant.CFG_SYSTEM_SECTION, AccountConstant.CFG_SYSTEM_DEFAULT_ENCODING);
                //set system language
                String language = NulsConfig.MODULES_CONFIG.getCfgValue(AccountConstant.CFG_SYSTEM_SECTION, AccountConstant.CFG_SYSTEM_LANGUAGE);
                I18nUtils.loadLanguage("languages", language);
                I18nUtils.setLanguage(language);
                //ACCOUNTKEYSTORE_FOLDER_NAME
                NulsConfig.DEFAULT_ENCODING = NulsConfig.MODULES_CONFIG.getCfgValue(AccountConstant.CFG_SYSTEM_SECTION, AccountConstant.CFG_SYSTEM_TKEYSTORE_FOLDER);
                String keystoreFolder = NulsConfig.MODULES_CONFIG.getCfgValue(AccountConstant.CFG_SYSTEM_SECTION, AccountConstant.CFG_SYSTEM_TKEYSTORE_FOLDER);
                if (StringUtils.isNotBlank(keystoreFolder)) {
                    NulsConfig.ACCOUNTKEYSTORE_FOLDER_NAME = keystoreFolder;
                }
                NulsConfig.KERNEL_MODULE_URL = NulsConfig.MODULES_CONFIG.getCfgValue(AccountConstant.CFG_SYSTEM_SECTION, AccountConstant.KERNEL_MODULE_URL);
            } catch (Exception e) {
                Log.error(e);
            }
        } catch (IOException e) {
            Log.error("Account Bootstrap initCfg failed", e);
            throw new RuntimeException("Account Bootstrap initCfg failed");
        }
    }


    /**
     * 初始化websocket服务器，供其他模块调用本模块接口
     *
     * @throws Exception
     */
    public static void initServer() {

        try {
            // Start server instance
            WsServer.getInstance(ModuleE.AC)
                    .moduleRoles(new String[]{"1.0"})
                    .moduleVersion("1.0")
                    .dependencies(ModuleE.LG.abbr, "1.0")
                    .scanPackage("io.nuls.account.rpc.cmd")
                    .connect(NulsConfig.KERNEL_MODULE_URL);

            // Get information from kernel
            CmdDispatcher.syncKernel();

        } catch (Exception e) {
            Log.error("Account initServer failed", e);
        }
    }
}
