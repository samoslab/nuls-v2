package io.nuls.test;

import io.nuls.api.provider.Result;
import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.account.AccountService;
import io.nuls.api.provider.account.facade.ImportAccountByPrivateKeyReq;
import io.nuls.api.provider.network.NetworkProvider;
import io.nuls.api.provider.network.facade.NetworkInfo;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.modulebootstrap.Module;
import io.nuls.rpc.modulebootstrap.RpcModule;
import io.nuls.rpc.modulebootstrap.RpcModuleState;
import io.nuls.test.cases.Constants;
import io.nuls.test.cases.TestCase;
import io.nuls.test.cases.TestCaseIntf;
import io.nuls.test.cases.TestFailException;
import io.nuls.test.controller.RpcServerManager;
import io.nuls.test.utils.LoggerUtil;
import io.nuls.test.utils.RestFulUtils;
import io.nuls.test.utils.Utils;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.log.logback.NulsLogger;
import io.nuls.tools.model.DateUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @Author: zhoulijun
 * @Time: 2019-03-18 15:05
 * @Description: 功能描述
 */
@Component
public class TestModule extends RpcModule {

    static NulsLogger log = LoggerUtil.logger;

    AccountService accountService = ServiceManager.get(AccountService.class);

    @Autowired Config config;


    NetworkProvider networkProvider = ServiceManager.get(NetworkProvider.class);

    @Override
    public Module[] getDependencies() {
        return new Module[]{
//                new Module(ModuleE.NW.abbr,"1.0"),
//                new Module(ModuleE.BL.abbr,"1.0"),
//                new Module(ModuleE.SC.abbr,"1.0"),
//                new Module(ModuleE.AC.abbr,"1.0"),
//                new Module(ModuleE.TX.abbr,"1.0"),
//                new Module(ModuleE.LG.abbr,"1.0"),
//                new Module(ModuleE.CS.abbr,"1.0")
        };
    }

    @Override
    public Module moduleInfo() {
        return new Module("test","1.0");
    }

    @Override
    public boolean doStart()
    {
        log.info("等待依赖模块准备就绪");
        return true;
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        log.info("do running");
        RpcServerManager.getInstance().startServer("0.0.0.0",9999);
        if(config.getNodeType().equals("master")){
            Result<String> result = accountService.importAccountByPrivateKey(new ImportAccountByPrivateKeyReq(Constants.PASSWORD,config.getTestSeedAccount(),true));
            config.setSeedAddress(result.getData());
            Result<NetworkInfo> networkInfo = networkProvider.getInfo();
            Utils.success("=".repeat(100));
            Utils.success("网络环境");
            Utils.success("localBestHeight:"+networkInfo.getData().getLocalBestHeight());
            Utils.success("netBestHeight:"+networkInfo.getData().getNetBestHeight());
            Utils.success("timeOffset:"+networkInfo.getData().getTimeOffset());
            Utils.success("inCount:"+networkInfo.getData().getInCount());
            Utils.success("outCount:"+networkInfo.getData().getOutCount());
            Utils.success("nodes:" + networkProvider.getNodes().getList().toString());
            Utils.success("Time:" + DateUtils.timeStamp2DateStr(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss"));
            Utils.success("packetMagic:"+config.getPacketMagic());
            Utils.success("=".repeat(100));
            if(networkProvider.getNodes().getList().isEmpty()){
                log.error("网络节点数量为0，不能进行测试");
                System.exit(0);
            }
            System.out.println();
            System.out.println();
            AtomicBoolean isSuccess = new AtomicBoolean(true);
            try {
                List<TestCaseIntf> testList = SpringLiteContext.getBeanList(TestCaseIntf.class);
                testList.forEach(tester->{
                    TestCase testCase = tester.getClass().getAnnotation(TestCase.class);
                    if(testCase == null){
                        return ;
                    }
                    try {
                        Utils.successDoubleLine("开始测试"+tester.title() + "   " + tester.getClass());
                        tester.check(null,0);
                    } catch (TestFailException e) {
                        Utils.failLine( "【" + tester.title() + "】 测试失败 :" + e.getMessage());
                        isSuccess.set(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(isSuccess.get()){
                Utils.successLine(" TEST DONE ");
            }else{
                Utils.failLine(" TEST FAIL ");
            }
            System.exit(0);
        }
        return RpcModuleState.Running;
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module dependenciesModule) {
        return RpcModuleState.Ready;
    }

    @Override
    public void init() {
        super.init();
        RestFulUtils.getInstance().setServerUri("http://127.0.0.1:9999/api/");
    }
}
