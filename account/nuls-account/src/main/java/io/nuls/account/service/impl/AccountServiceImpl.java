/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.account.service.impl;

import io.nuls.account.constant.AccountErrorCode;
import io.nuls.account.model.bo.Account;
import io.nuls.account.model.po.AccountPo;
import io.nuls.account.service.AccountCacheService;
import io.nuls.account.service.AccountService;
import io.nuls.account.storage.AccountStorageService;
import io.nuls.account.util.AccountTool;
import io.nuls.base.basic.AddressTool;
import io.nuls.tools.basic.InitializingBean;
import io.nuls.tools.basic.Result;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: qinyifeng
 */
@Service
public class AccountServiceImpl implements AccountService, InitializingBean {

    private Lock locker = new ReentrantLock();

    @Autowired
    private AccountStorageService accountStorageService;

    private AccountCacheService accountCacheService = AccountCacheService.getInstance();

    @Override
    public void afterPropertiesSet() {
        //Initialize local account data to cache
        getAccountList();
    }

    @Override
    public List<Account> createAccount(short chainId, int count, String password) {
        //check params
        if (chainId <= 0 || count <= 0 || count > AccountTool.CREATE_MAX_SIZE) {
            throw new NulsRuntimeException(AccountErrorCode.PARAMETER_ERROR);
        }
        if (StringUtils.isNotBlank(password) && !AccountTool.validPassword(password)) {
            throw new NulsRuntimeException(AccountErrorCode.PASSWORD_FORMAT_WRONG);
        }
        locker.lock();
        List<Account> accounts = new ArrayList<>();
        try {
            List<AccountPo> accountPos = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                //create account
                Account account = AccountTool.createAccount(chainId);
                if (StringUtils.isNotBlank(password)) {
                    account.encrypt(password);
                }
                accounts.add(account);
                AccountPo po = new AccountPo(account);
                accountPos.add(po);
            }
            //Saving account data in batches
            boolean result = accountStorageService.saveAccountList(accountPos);
            if (result) {
                //If saved successfully, put the account in local cache.
                for (Account account : accounts) {
                    accountCacheService.localAccountMaps.put(account.getAddress().getBase58(), account);
                }
                //TODO
                //Sending account creation events

            }
        } catch (Exception e) {
            Log.error(e);
            throw new NulsRuntimeException(AccountErrorCode.FAILED);
        } finally {
            locker.unlock();
        }
        return accounts;
    }

    @Override
    public Account getAccount(short chainId, String address) {
        //check params
        if (!AddressTool.validAddress(chainId, address)) {
            throw new NulsRuntimeException(AccountErrorCode.ADDRESS_ERROR);
        }
        //check the account is exist
        Account account = getAccountByAddress(chainId, address);
        return account;
    }

    @Override
    public List<Account> getAccountList() {
        List<Account> list = new ArrayList<>();
        //If local account data is loaded into the cache
        if (accountCacheService.localAccountMaps.size() > 0) {
            Collection<Account> values = accountCacheService.localAccountMaps.values();
            Iterator<Account> iterator = values.iterator();
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
        } else {
            //Query all accounts list
            List<AccountPo> poList = accountStorageService.getAccountList();
            Set<String> addressList = new HashSet<>();
            if (null == poList || poList.isEmpty()) {
                return list;
            }
            for (AccountPo po : poList) {
                Account account = po.toAccount();
                list.add(account);
                addressList.add(account.getAddress().getBase58());
            }
            //put the account in local cache.
            for (Account account : list) {
                accountCacheService.localAccountMaps.put(account.getAddress().getBase58(), account);
            }
        }
        Collections.sort(list, (Account o1, Account o2) -> (o2.getCreateTime().compareTo(o1.getCreateTime())));
        return list;
    }

    /**
     * 根据账户地址字符串,获取账户对象(内部调用)
     * Get account object based on account address string
     *
     * @return Account
     */
    private Account getAccountByAddress(short chainId, String address) {
        //check params
        if (!AddressTool.validAddress(chainId, address)) {
            return null;
        }
        // If the account is not yet cached, all local accounts are queried and cached
        if (accountCacheService.localAccountMaps == null || accountCacheService.localAccountMaps.size() == 0) {
            getAccountList();
        }
        return accountCacheService.localAccountMaps.get(address);
    }

    /**
     * set the password for exist account
     *
     * @param chainId
     * @param address
     * @param password
     * @return true or false
     * @auther EdwardChan
     * <p>
     * Nov.10th 2018
     */
    @Override
    public boolean setPassword(short chainId, String address, String password) {
        //check if the account is legal
        if (!AddressTool.validAddress(chainId, address)) {
            Log.debug("the address is illegal,chainId:{},address:{}", chainId, address);
            return false;
        }
        if (password == null) {
            Log.debug("the password should't be null,chainId:{},address:{}", chainId, address);
            return false;
        }
        //check if the account is exist
        Account account = getAccountByAddress(chainId, address);
        if (account == null) {
            Log.debug("the account isn't exist,chainId:{},address:{}", chainId, address);
            return false;
        }
        //check if the account has encrypt
        if (account.isEncrypted()) {
            Log.debug("the account has encrypted,chainId:{},address:{}", chainId, address);
            return false;
        }
        //encrypt the account
        try {
            account.encrypt(password);
        } catch (Exception e) {
            Log.error("encrypt the account occur exception,chainId:{},address:{}", chainId, address, e);
        }
        //save the account
        AccountPo po = new AccountPo(account);
        boolean result = accountStorageService.saveAccount(po);
        if (!result) {
            Log.debug("save the account failed,chainId:{},address:{}", chainId, address);
        }
        return result;
    }

    /**
     * check if the account is encrypted
     *
     * @param chainId
     * @param address
     * @return true or false
     * @auther EdwardChan
     * <p>
     * Nov.10th 2018
     */
    @Override
    public boolean isEncrypted(short chainId, String address) {
        //check if the account is legal
        if (!AddressTool.validAddress(chainId, address)) {
            Log.debug("the address is illegal,chainId:{},address:{}", chainId, address);
            return false;
        }
        //check if the account is exist
        Account account = getAccountByAddress(chainId, address);
        if (account == null) {
            Log.debug("the account isn't exist,chainId:{},address:{}", chainId, address);
            return false;
        }
        boolean result = account.isEncrypted();
        Log.debug("the account is Encrypted:{},chainId:{},address:{}", result, chainId, address);
        return result;
    }

    @Override
    public boolean removeAccount(short chainId, String address, String password) {
        //check params
        if (!AddressTool.validAddress(chainId, address)) {
            throw new NulsRuntimeException(AccountErrorCode.ADDRESS_ERROR);
        }
        //Check whether the account exists
        Account account = getAccountByAddress(chainId, address);
        if (account == null) {
            throw new NulsRuntimeException(AccountErrorCode.ACCOUNT_NOT_EXIST);
        }
        //The account is encrypted, verify password
        if (account.isEncrypted()) {
            if (!account.validatePassword(password)) {
                throw new NulsRuntimeException(AccountErrorCode.PASSWORD_IS_WRONG);
            }
        }
        //Delete the account from the database
        boolean result = accountStorageService.removeAccount(account.getAddress());
        //Delete the account from the cache
        accountCacheService.localAccountMaps.remove(account.getAddress().getBase58());

        //TODO
        //Sending account remove events
        return result;
    }
}
