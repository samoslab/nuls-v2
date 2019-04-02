package io.nuls.test.cases.transcation;

import io.nuls.api.provider.Result;
import io.nuls.api.provider.transaction.facade.GetConfirmedTxByHashReq;
import io.nuls.api.provider.transaction.facade.TransactionData;
import io.nuls.base.constant.TxStatusEnum;
import io.nuls.test.cases.TestFailException;
import io.nuls.tools.core.annotation.Component;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-21 10:46
 * @Description: 功能描述
 */
@Component
public class GetTxInfoCase extends BaseTranscationCase<TransactionData,String> {

    @Override
    public String title() {
        return "通过hash获取交易信息";
    }

    @Override
    public TransactionData doTest(String param, int depth) throws TestFailException {
        Result<TransactionData> result = transferService.getSimpleTxDataByHash(new GetConfirmedTxByHashReq(param));
        checkResultStatus(result);
        check(result.getData().getStatus().equals(TxStatusEnum.CONFIRMED),"交易状态不符合预期，未确认");
        return result.getData();
    }
}
