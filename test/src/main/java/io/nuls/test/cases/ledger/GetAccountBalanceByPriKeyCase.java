package io.nuls.test.cases.ledger;

import io.nuls.api.provider.Result;
import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.ledger.LedgerProvider;
import io.nuls.api.provider.ledger.facade.AccountBalanceInfo;
import io.nuls.api.provider.ledger.facade.GetBalanceReq;
import io.nuls.test.Config;
import io.nuls.test.cases.TestCaseIntf;
import io.nuls.test.cases.TestFailException;
import io.nuls.test.cases.account.ImportAccountByPriKeyCase;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-21 11:25
 * @Description: 功能描述
 */
@Component
public class GetAccountBalanceByPriKeyCase implements TestCaseIntf<AccountBalanceInfo,String> {

    LedgerProvider ledgerProvider = ServiceManager.get(LedgerProvider.class);

    @Autowired
    Config config;

    @Autowired
    ImportAccountByPriKeyCase importAccountByPriKeyCase;


    @Override
    public String title() {
        return "通过私钥查询余额";
    }

    @Override
    public AccountBalanceInfo doTest(String priKey, int depth) throws TestFailException {
        String address = importAccountByPriKeyCase.check(priKey,depth);
        Result<AccountBalanceInfo> result = ledgerProvider.getBalance(new GetBalanceReq(config.getAssetsId(),config.getChainId(),address));
        checkResultStatus(result);
        return result.getData();
    }
}
