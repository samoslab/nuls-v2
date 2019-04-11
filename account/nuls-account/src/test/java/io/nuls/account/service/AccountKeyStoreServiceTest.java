package io.nuls.account.service;

import io.nuls.account.AccountBootstrap;
import io.nuls.account.config.NulsConfig;
import io.nuls.account.model.bo.Account;
import io.nuls.db.service.RocksDBService;
import io.nuls.tools.core.inteceptor.ModularServiceMethodInterceptor;
import io.nuls.tools.core.ioc.SpringLiteContext;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author: qinyifeng
 * @description:
 * @date: 2018/11/20
 */
public class AccountKeyStoreServiceTest {

    protected static AccountService accountService;
    protected static AccountKeyStoreService accountKeyStoreService;
    protected int chainId = 2;
    protected String password = "a2678";

    @BeforeClass
    public static void beforeTest() {
        //初始化配置
        SpringLiteContext.init("io.nuls.account", new ModularServiceMethodInterceptor());
        AccountBootstrap accountBootstrap = SpringLiteContext.getBean(AccountBootstrap.class);
        //初始化配置
        accountBootstrap.initCfg();
        //读取配置文件，数据存储根目录，初始化打开该目录下所有表连接并放入缓存
        RocksDBService.init(NulsConfig.DATA_PATH);
        //启动时间同步线程
        TimeService.getInstance().start();
        accountService = SpringLiteContext.getBean(AccountService.class);
        accountKeyStoreService = SpringLiteContext.getBean(AccountKeyStoreService.class);
    }

    @Test
    public void backupAccountToKeyStoreTest() {
        //Create password accounts
        List<Account> accountList = accountService.createAccount(chainId, 1, password);
        String address = accountList.get(0).getAddress().getBase58();
        //测试不指定备份路径
        String pathDir = "";
        //备份账户keystore  backup account keyStore
        String path = accountKeyStoreService.backupAccountToKeyStore(pathDir, chainId, address, password);
        assertNotNull(path);

        //测试指定非windows备份路径
        pathDir = "测试1/back/up";
        //备份账户keystore  backup account keyStore
        path = accountKeyStoreService.backupAccountToKeyStore(pathDir, chainId, address, password);
        assertNotNull(path);

        //测试指定windows备份路径
        pathDir = "D:\\workspace\\github\\nuls_2.0\\测试2\\back\\up";
        //Create an unencrypted account for test
        accountList = accountService.createAccount(chainId, 1, null);
        address = accountList.get(0).getAddress().getBase58();
        //备份账户keystore  backup account keyStore
        path = accountKeyStoreService.backupAccountToKeyStore(pathDir, chainId, address, password);
        assertNotNull(path);

    }

}
