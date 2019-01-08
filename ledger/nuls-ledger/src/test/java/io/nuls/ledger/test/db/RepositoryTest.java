/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.test.db;

import io.nuls.db.service.RocksDBService;
import io.nuls.ledger.config.AppConfig;
import io.nuls.ledger.db.DataBaseArea;
import io.nuls.ledger.db.Repository;
import io.nuls.ledger.db.RepositoryImpl;
import io.nuls.tools.log.Log;
import org.junit.Before;
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
            RocksDBService.init(AppConfig.loadModuleConfig().getDatabaseDir());
            if (!RocksDBService.existTable(DataBaseArea.TB_LEDGER_ACCOUNT)) {
                RocksDBService.createTable(DataBaseArea.TB_LEDGER_ACCOUNT);
            }
        } catch (Exception e) {
            Log.error(e);
        }
        repository = new RepositoryImpl();
    }
}
