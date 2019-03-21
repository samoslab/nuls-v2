package io.nuls.test.cases.transcation;

import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.transaction.TransferService;
import io.nuls.test.Config;
import io.nuls.test.cases.TestCaseIntf;
import io.nuls.tools.core.annotation.Autowired;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-21 10:13
 * @Description: 功能描述
 */
public abstract class BaseTranscationCase<T,P> implements TestCaseIntf<T,P> {

    /**
     * 10NULS
     */
    public static final BigInteger TRANSFER_AMOUNT = BigInteger.valueOf(1000000000L);

    public static final String REMARK = "test_remark";

    TransferService transferService = ServiceManager.get(TransferService.class);

    @Autowired Config config;

}
