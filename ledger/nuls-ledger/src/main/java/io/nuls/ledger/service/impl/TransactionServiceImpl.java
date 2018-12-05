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
package io.nuls.ledger.service.impl;

import io.nuls.base.data.Transaction;
import io.nuls.ledger.constant.TransactionType;
import io.nuls.ledger.service.TransactionService;
import io.nuls.ledger.service.processor.AccountAliasProcessor;
import io.nuls.ledger.service.processor.CoinBaseProcessor;
import io.nuls.ledger.service.processor.TransferProcessor;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangkun23 on 2018/11/28.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    CoinBaseProcessor coinBaseProcessor;

    @Autowired
    TransferProcessor transferProcessor;

    @Autowired
    AccountAliasProcessor accountAliasProcessor;

    /**
     * 已确认交易数据处理
     *
     * @param transaction
     */
    @Override
    public void txProcess(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        TransactionType txType = TransactionType.valueOf(transaction.getType());
        //先判断对应的交易类型
        switch (txType) {
            case TX_TYPE_COINBASE:
                //TX_TYPE_COIN_BASE 直接累加账户余额
                coinBaseProcessor.process(transaction);
                break;
            case TX_TYPE_TRANSFER:
                //TX_TYPE_TRANSFER 减去发送者账户余额，增加to方的余额
                transferProcessor.process(transaction);
                break;
            case TX_TYPE_ACCOUNT_ALIAS:
                accountAliasProcessor.process(transaction);
                break;
            default:
                logger.info("tx type incorrect: {}", transaction.getType());
        }

        //TX_TYPE_ACCOUNT_ALIAS  //减去发送者账户余额 Burned
        //TX_TYPE_REGISTER_AGENT
        //TX_TYPE_STOP_AGENT
        //TX_TYPE_JOIN_CONSENSUS
        //TX_TYPE_CANCEL_DEPOSIT

        //TX_TYPE_YELLOW_PUNISH
        //TX_TYPE_RED_PUNISH
    }
}
