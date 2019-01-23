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
package io.nuls.ledger.db;

import io.nuls.ledger.model.po.AccountState;

public interface Repository {


    /**
     * put accountState to rocksdb
     *
     * @param key
     * @param accountState
     */
    void createAccountState(byte[] key, AccountState accountState);

    /**
     * update accountState to rocksdb
     * @param key
     * @param preAccountState
     * @param nowAccountState
     */
    void updateAccountStateAndSnapshot(String key,AccountState preAccountState,AccountState nowAccountState);

    /**
     *
     * @param chainId
     * @param key
     * @return AccountState
     */
    AccountState getAccountState(int chainId,byte[] key);

    /**
     *
     * @param key
     * @param nowAccountState
     */
    void updateAccountState(byte[] key,AccountState nowAccountState);

    /**
     *
     * @param chainId
     * @param key
     * @return AccountState
     */
    AccountState getSnapshotAccountState(int chainId,byte[] key);

    /**
     *
     * @param chainId
     * @param key
     */
    void delSnapshotAccountState(int chainId,byte[] key);

    /**
     *
     * @param chainId
     * @return
     */
      long  getBlockHeight(int chainId);
}
