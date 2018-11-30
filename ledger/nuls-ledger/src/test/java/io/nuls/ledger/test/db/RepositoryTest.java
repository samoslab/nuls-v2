package io.nuls.ledger.test.db;

import io.nuls.db.service.RocksDBService;
import io.nuls.ledger.config.AppConfig;
import io.nuls.ledger.db.DataBaseArea;
import io.nuls.ledger.db.Repository;
import io.nuls.ledger.db.RepositoryImpl;
import io.nuls.ledger.model.AccountState;
import io.nuls.tools.log.Log;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangkun23 on 2018/11/21.
 */
public class RepositoryTest {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private Repository repository;

    @Before
    public void before() {
        try {
            AppConfig.loadModuleConfig();
            RocksDBService.init(AppConfig.moduleConfig.getDatabaseDir());
            if (!RocksDBService.existTable(DataBaseArea.TB_LEDGER_ACCOUNT)) {
                RocksDBService.createTable(DataBaseArea.TB_LEDGER_ACCOUNT);
            }
        } catch (Exception e) {
            Log.error(e);
        }
        repository = new RepositoryImpl();
    }
}
