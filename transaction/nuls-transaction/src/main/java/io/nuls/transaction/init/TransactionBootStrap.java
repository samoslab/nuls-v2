package io.nuls.transaction.init;

import io.nuls.db.service.RocksDBService;
import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.server.WsServer;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.ConfigLoader;
import io.nuls.tools.parse.I18nUtils;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.db.rocksdb.storage.LanguageStorageService;
import io.nuls.transaction.scheduler.TransactionScheduler;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: Charlie
 * @date: 2018/11/12
 */
public class TransactionBootStrap {

    public static void main(String[] args) {
        try {
            init(1);
            //TimeService.getInstance().start();
            System.out.println();
        }catch (Exception e){
            Log.error("Transaction startup error!");
            Log.error(e);
        }
    }
    public static void init(int chainId){
        try{
            //初始化系统参数
            initSys();
            //初始化数据库配置文件
            initDB();
            //初始化上下文
            SpringLiteContext.init(TxConstant.CONTEXT_PATH);

            //初始化国际资源文件语言
            initLanguage();
            //加载本地配置参数,并启动本地服务
            sysStart(chainId);
            //启动WebSocket服务,向外提供RPC接口
            initServer();
        }catch (Exception e){
            Log.error(e);
        }
    }

    /**
     * 初始化系统编码
     * */
    public static void initSys(){
        try {
            System.setProperty(TxConstant.SYS_ALLOW_NULL_ARRAY_ELEMENT, "true");
            System.setProperty(TxConstant.SYS_FILE_ENCODING, UTF_8.name());
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, UTF_8);
        }catch (Exception e){
            Log.error(e);
        }
    }

    /**
     * 初始化数据库
     * */
    public static void initDB(){
        try {
            Properties properties = ConfigLoader.loadProperties(TxConstant.DB_CONFIG_NAME);
            String path = properties.getProperty(TxConstant.DB_DATA_PATH,
                    TransactionBootStrap.class.getClassLoader().getResource("").getPath() + "data");
            RocksDBService.init(path);
        }catch (Exception e){
            Log.error(e);
        }
    }

    /**
     * 初始化国际化资源文件语言
     * */
    public static void initLanguage(){
        try {
            LanguageStorageService languageService = SpringLiteContext.getBean(LanguageStorageService.class);
            String languageDB = (String) languageService.getLanguage();
            I18nUtils.loadLanguage("","");
            String language = null == languageDB ? I18nUtils.getLanguage() : languageDB;
            I18nUtils.setLanguage(language);
            if (null == languageDB) {
                languageService.saveLanguage(language);
            }
        }catch (Exception e){
            Log.error(e);
        }
    }

    public static void sysStart(int chainId){
        TransactionScheduler.getInstance().start();
    }

    /**
     * 共识模块启动WebSocket服务，用于其他模块连接共识模块与共识模块交互
     * */
    public static void initServer(){
        try {
            // Start server instance
            WsServer.getInstance(ModuleE.AC)
                    .moduleRoles(new String[]{"1.0"})
                    .moduleVersion("1.0")
                    .dependencies(ModuleE.LG.abbr, "1.0")
                    .scanPackage("io.nuls.transaction.rpc.cmd")
                    .connect("ws://127.0.0.1:8887");

            // Get information from kernel
            CmdDispatcher.syncManager();
        }catch (Exception e){
            Log.error("Transaction startup webSocket server error!");
            e.printStackTrace();
        }
    }



}
