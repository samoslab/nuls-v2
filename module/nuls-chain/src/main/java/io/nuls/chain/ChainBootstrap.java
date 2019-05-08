package io.nuls.chain;

import io.nuls.base.basic.AddressTool;
import io.nuls.chain.config.NulsChainConfig;
import io.nuls.chain.info.CmConstants;
import io.nuls.chain.info.CmRuntimeInfo;
import io.nuls.chain.rpc.call.RpcService;
import io.nuls.chain.rpc.call.impl.RpcServiceImpl;
import io.nuls.chain.service.CacheDataService;
import io.nuls.chain.service.ChainService;
import io.nuls.chain.storage.InitDB;
import io.nuls.chain.storage.impl.*;
import io.nuls.chain.util.LoggerUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.TimeUtils;

/**
 * 链管理模块启动类
 * Main class of BlockChain module
 *
 * @author tangyi
 * @date 2018/11/7
 */
@Component
public class ChainBootstrap extends RpcModule {
    @Autowired
    private NulsChainConfig nulsChainConfig;

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"ws://" + HostInfo.getLocalIP() + ":8887/ws"};
        }
        NulsRpcModuleBootstrap.run("io.nuls", args);
    }


    /**
     * 读取resources/module.ini，初始化配置
     * Read resources/module.ini to initialize the configuration
     *
     * @throws Exception Any error will throw an exception
     */
    private void initCfg() throws Exception {
        CmRuntimeInfo.nulsAssetId = nulsChainConfig.getMainAssetId();
        CmRuntimeInfo.nulsChainId = nulsChainConfig.getMainChainId();
        CmConstants.BLACK_HOLE_ADDRESS = AddressTool.getAddress(nulsChainConfig.getBlackHoleAddress());
        LoggerUtil.defaultLogInit(nulsChainConfig.getLogLevel());
    }

    /**
     * 如果数据库中有相同的配置，则以数据库为准
     * If the database has the same configuration, use the database entity
     *
     * @throws Exception Any error will throw an exception
     */
    private void initWithDatabase() throws Exception {
        /* 打开数据库连接 (Open database connection) */
        RocksDBService.init(nulsChainConfig.getDataPath() + CmConstants.MODULE_DB_PATH);
        InitDB assetStorage = SpringLiteContext.getBean(AssetStorageImpl.class);
        assetStorage.initTableName();
        LoggerUtil.logger().info("assetStorage.init complete.....");
        InitDB blockHeightStorage = SpringLiteContext.getBean(BlockHeightStorageImpl.class);
        blockHeightStorage.initTableName();
        LoggerUtil.logger().info("blockHeightStorage.init complete.....");
        InitDB cacheDatasStorage = SpringLiteContext.getBean(CacheDatasStorageImpl.class);
        cacheDatasStorage.initTableName();
        LoggerUtil.logger().info("cacheDatasStorage.init complete.....");
        InitDB chainAssetStorage = SpringLiteContext.getBean(ChainAssetStorageImpl.class);
        chainAssetStorage.initTableName();
        LoggerUtil.logger().info("chainAssetStorage.init complete.....");
        InitDB chainStorage = SpringLiteContext.getBean(ChainStorageImpl.class);
        chainStorage.initTableName();
        LoggerUtil.logger().info("chainStorage.init complete.....");
    }


    /**
     * 把Nuls2.0主网信息存入数据库中
     * Store the Nuls2.0 main network information into the database
     *
     * @throws Exception Any error will throw an exception
     */
    private void initMainChain() throws Exception {
        SpringLiteContext.getBean(ChainService.class).initMainChain();
    }

    private void initChainDatas() throws Exception {
        SpringLiteContext.getBean(CacheDataService.class).initBlockDatas();
    }

    private void regTxRpc() throws Exception {
        RpcService rpcService = SpringLiteContext.getBean(RpcServiceImpl.class);
        boolean regResult = false;
        while (!regResult) {
            regResult = rpcService.regTx();
            LoggerUtil.logger().info("regTx fail,continue  regTx....");
            Thread.sleep(3000);
        }
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{new Module(ModuleE.NW.abbr, "1.0"),
                new Module(ModuleE.TX.abbr, "1.0"),
                new Module(ModuleE.LG.abbr, "1.0")};
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.CM.abbr, "1.0");
    }

    /**
     * 初始化模块信息，比如初始化RockDB等，在此处初始化后，可在其他bean的afterPropertiesSet中使用
     */
    @Override
    public void init() {
        super.init();
        try {
            /* Read resources/module.ini to initialize the configuration */
            initCfg();
            LoggerUtil.logger().info("initCfg complete.....");
            /*storage info*/
            initWithDatabase();
            LoggerUtil.logger().info("initWithDatabase complete.....");
            /* 把Nuls2.0主网信息存入数据库中 (Store the Nuls2.0 main network information into the database) */
            initMainChain();
            LoggerUtil.logger().info("initMainChain complete.....");
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
            LoggerUtil.logger().error("初始化异常退出....");
            System.exit(-1);
        }
    }

    @Override
    public boolean doStart() {
        try {
            /* 进行数据库数据初始化（避免异常关闭造成的事务不一致） */
            initChainDatas();
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
            LoggerUtil.logger().error("启动异常退出....");
            System.exit(-1);

        }
        return true;
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        try {
            /*注册交易处理器*/
            regTxRpc();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            LoggerUtil.logger().error(e);
        }
        TimeUtils.getInstance().start();
        return RpcModuleState.Running;
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        return RpcModuleState.Ready;
    }
}
