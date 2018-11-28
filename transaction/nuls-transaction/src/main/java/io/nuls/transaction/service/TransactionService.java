package io.nuls.transaction.service;

import io.nuls.base.data.Transaction;
import io.nuls.transaction.model.bo.TxRegister;

/**
 * @author: Charlie
 * @date: 2018/11/22
 */
public interface TransactionService {

    boolean register(TxRegister txRegister);

    void newTx(Transaction transaction);
}
