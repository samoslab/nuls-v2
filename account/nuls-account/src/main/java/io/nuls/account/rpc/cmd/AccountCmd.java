package io.nuls.account.rpc.cmd;

import io.nuls.account.constant.AccountConstant;
import io.nuls.account.constant.AccountErrorCode;
import io.nuls.account.model.bo.Account;
import io.nuls.account.model.dto.AccountKeyStoreDto;
import io.nuls.account.model.dto.AccountOfflineDto;
import io.nuls.account.model.dto.SimpleAccountDto;
import io.nuls.account.service.AccountKeyStoreService;
import io.nuls.account.service.AccountService;
import io.nuls.account.util.AccountTool;
import io.nuls.base.data.Page;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.FormatValidUtils;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: qinyifeng
 * @description:
 * @date: 2018/11/5
 */
@Component
public class AccountCmd extends BaseCmd {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountKeyStoreService keyStoreService;

    /**
     * 创建指定个数的账户
     * create a specified number of accounts
     *
     * @param params [chainId,count,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_createAccount", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "create a specified number of accounts")
    public Object createAccount(Map params) {
        Log.debug("ac_createAccount start");
        Map<String, List<String>> map = new HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            // check parameters
            if (params == null || params.get("chainId") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }

            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //创建账户个数
            Integer count = params.get("count") != null ? (Integer) params.get("count") : 0;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;
            //创建账户
            List<Account> accountList = accountService.createAccount(chainId, count, password);
            if (accountList != null) {
                accountList.forEach(account -> list.add(account.getAddress().toString()));
                //map.put("list", list);
            }
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_createAccount end");
        return success(list);
    }

    /**
     * 创建离线账户, 该账户不保存到数据库, 并将直接返回账户的所有信息
     * create an offline account, which is not saved to the database and will directly return all information to the account.
     *
     * @param params [chainId,count,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_createOfflineAccount", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "create an offline account")
    public Object createOfflineAccount(Map params) {
        Log.debug("ac_createOfflineAccount start");
        Map<String, List<AccountOfflineDto>> map = new HashMap<>();
        List<AccountOfflineDto> accounts = new ArrayList<>();
        try {
            // check parameters size
            if (params == null || params.get("chainId") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //创建账户个数
            Integer count = params.get("count") != null ? (Integer) params.get("count") : 0;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;

            //Check parameter is correct.
            if (count <= 0 || count > AccountTool.CREATE_MAX_SIZE) {
                throw new NulsRuntimeException(AccountErrorCode.PARAMETER_ERROR);
            }
            if (StringUtils.isNotBlank(password) && !FormatValidUtils.validPassword(password)) {
                throw new NulsRuntimeException(AccountErrorCode.PASSWORD_FORMAT_WRONG);
            }

            try {
                for (int i = 0; i < count; i++) {
                    Account account = AccountTool.createAccount(chainId);
                    if (StringUtils.isNotBlank(password)) {
                        account.encrypt(password);
                    }
                    accounts.add(new AccountOfflineDto(account));
                }
            } catch (NulsException e) {
                throw e;
            }
            //map.put("list", accounts);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_createOfflineAccount end");
        return success(accounts);
    }

    /**
     * 根据地址获取账户
     * get account according to address
     *
     * @param params [chainId,address]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAccountByAddress", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "get account according to address")
    public Object getAccountByAddress(Map params) {
        Log.debug("ac_getAccountByAddress start");
        Account account;
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //根据地址查询账户
            account = accountService.getAccount(chainId, address);
            if (null == account) {
                return success(null);
            }
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_getAccountByAddress end");
        return success(new SimpleAccountDto(account));
    }

    /**
     * 获取所有账户集合,并放入缓存
     * query all account collections and put them in cache
     *
     * @param params []
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAccountList", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "query all account collections and put them in cache")
    public Object getAccountList(Map params) {
        Log.debug("ac_getAccountList start");
        Map<String, List<SimpleAccountDto>> map = new HashMap<>();
        List<SimpleAccountDto> simpleAccountList = new ArrayList<>();
        try {
            //query all accounts
            List<Account> accountList = accountService.getAccountList();
            if (null == accountList) {
                return success(null);
            }
            accountList.forEach(account -> simpleAccountList.add(new SimpleAccountDto((account))));
            //map.put("list", simpleAccountList);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_getAccountList end");
        return success(simpleAccountList);
    }

    /**
     * 分页查询账户地址列表
     * paging query account address list
     *
     * @param params [chainId,pageNumber,pageSize]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAddressList", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "paging query account address list")
    public Object getAddressList(Map params) {
        Log.debug("ac_getAddressList start");
        Page<String> resultPage;
        try {
            // check parameters size
            if (params == null || params.get("chainId") == null || params.get("pageNumber") == null || params.get("pageSize") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //页码
            Integer pageNumber = params.get("pageNumber") != null ? (Integer) params.get("pageNumber") : 0;
            //每页显示数量
            Integer pageSize = params.get("pageSize") != null ? (Integer) params.get("pageSize") : 0;

            if (chainId <= 0 || pageNumber < 1 || pageSize < 1) {
                throw new NulsRuntimeException(AccountErrorCode.PARAMETER_ERROR);
            }

            //query all accounts
            List<Account> accountList = accountService.getAccountList();
            if (null == accountList) {
                return success(null);
            }
            //根据分页参数返回账户地址列表 Returns the account address list according to paging parameters
            Page<String> page = new Page<>(pageNumber, pageSize);
            page.setTotal(accountList.size());
            int start = (pageNumber - 1) * pageSize;
            if (start >= accountList.size()) {
                return success(page);
            }
            int end = pageNumber * pageSize;
            if (end > accountList.size()) {
                end = accountList.size();
            }
            accountList = accountList.subList(start, end);
            resultPage = new Page<>(page);
            List<String> addressList = new ArrayList<>();
            //只返回账户地址 Only return to account address
            accountList.forEach(account -> addressList.add(account.getAddress().getBase58()));
            resultPage.setList(addressList);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_getAddressList end");
        return success(resultPage);
    }

    /**
     * 移除指定账户
     * remove specified account
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_removeAccount", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "remove specified account")
    public Object removeAccount(Map params) {
        Log.debug("ac_removeAccount start");
        boolean result;
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;

            //Remove specified account
            result = accountService.removeAccount(chainId, address, password);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_removeAccount end");
        Map<String, Boolean> map = new HashMap<>();
        map.put("value", result);
        return success(map);
    }

    /**
     * 根据地址查询账户私匙,只返回加密账户私钥，未加密账户不返回
     * inquire the account's private key according to the address.
     * only returns the private key of the encrypted account, and the unencrypted account does not return.
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getPriKeyByAddress", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "inquire the account's private key according to the address")
    public Object getPriKeyByAddress(Map params) {
        Log.debug("ac_getPriKeyByAddress start");
        String unencryptedPrivateKey;
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;

            //Get the account private key
            unencryptedPrivateKey = accountService.getPrivateKey(chainId, address, password);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_getPriKeyByAddress end");
        Map<String, String> map = new HashMap<>();
        map.put("priKey", unencryptedPrivateKey);
        return success(map);
    }

    /**
     * 获取所有本地账户账户私钥，必须保证所有账户密码一致
     * 如果本地账户中的密码不一致，将返回错误信息
     * get the all local private keys
     * if the password in the local account is different, the error message will be returned.
     *
     * @param params [chainId,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAllPriKey", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "get the all local private keys")
    public Object getAllPriKey(Map params) {
        Log.debug("ac_getAllPriKey start");
        Map<String, List<String>> map = new HashMap<>();
        List<String> privateKeyList = new ArrayList<>();
        try {
            // check parameters
            if (params == null || params.get("chainId") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;

            //Get the account private key
            privateKeyList = accountService.getAllPrivateKey(chainId, password);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_getAllPriKey end");
        return success(privateKeyList);
    }

    /**
     * 为账户设置备注
     * set remark for accounts
     *
     * @param params [chainId,address,remark]
     * @return
     */
    @CmdAnnotation(cmd = "ac_setRemark", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "set remark for accounts")
    @Parameter(parameterName = "chainId", parameterType = "short", parameterValidRange = "", parameterValidRegExp = "")
    @Parameter(parameterName = "address", parameterType = "String", parameterValidRange = "", parameterValidRegExp = "")
    @Parameter(parameterName = "remark", parameterType = "String", parameterValidRange = "", parameterValidRegExp = "")
    public Object setRemark(Map params) {
        Log.debug("ac_setRemark start");
        Map<String, Boolean> map = new HashMap<>(1);
        boolean result;
        try {
            // check parameters
            if (params == null || params.get("chainId") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户备注
            String remark = params.get("remark") != null ? (String) params.get("remark") : null;

            //Get the account private key
            result = accountService.setRemark(chainId, address, remark);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_setRemark end");
        return success(map);
    }

    /**
     * 根据私钥导入账户
     * import accounts by private key
     *
     * @param params [chainId,priKey,password,overwrite]
     * @return
     */
    @CmdAnnotation(cmd = "ac_importAccountByPriKey", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "import accounts by private key")
    public Object importAccountByPriKey(Map params) {
        Log.debug("ac_importAccountByPriKey start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("priKey") == null || params.get("overwrite") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户私钥
            String priKey = params.get("priKey") != null ? (String) params.get("priKey") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;
            //账户存在时是否覆盖
            Boolean overwrite = params.get("overwrite") != null ? (Boolean) params.get("overwrite") : null;
            //导入账户
            Account account = accountService.importAccountByPrikey(chainId, priKey, password, overwrite);
            map.put("address", account.getAddress().toString());
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_importAccountByPriKey end");
        return success(map);
    }


    /**
     * 根据AccountKeyStore导入账户
     * import accounts by AccountKeyStore
     *
     * @param params [chainId,keyStore,password,overwrite]
     * @return
     */
    @CmdAnnotation(cmd = "ac_importAccountByKeystore", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "import accounts by AccountKeyStore")
    public Object importAccountByKeystore(Map params) {
        Log.debug("ac_importAccountByKeystore start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("keyStore") == null || params.get("overwrite") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //keyStore HEX编码
            String keyStore = params.get("keyStore") != null ? (String) params.get("keyStore") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;
            //账户存在时是否覆盖
            Boolean overwrite = params.get("overwrite") != null ? (Boolean) params.get("overwrite") : null;

            AccountKeyStoreDto accountKeyStoreDto;
            try {
                accountKeyStoreDto = JSONUtils.json2pojo(new String(HexUtil.decode(keyStore)), AccountKeyStoreDto.class);
            } catch (IOException e) {
                throw new NulsRuntimeException(AccountErrorCode.ACCOUNTKEYSTORE_FILE_DAMAGED);
            }

            //导入账户
            Account account = accountService.importAccountByKeyStore(accountKeyStoreDto.toAccountKeyStore(), chainId, password, overwrite);
            map.put("address", account.getAddress().toString());
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_importAccountByKeystore end");
        return success(map);
    }

    /**
     * 账户备份，导出AccountKeyStore字符串
     * export account KeyStore
     *
     * @param params [chainId,address,password,path]
     * @return
     */
    @CmdAnnotation(cmd = "ac_exportAccountKeyStore", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "export account KeyStore")
    public Object exportAccountKeyStore(Map params) {
        Log.debug("ac_exportAccountKeyStore start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;
            //文件备份地址
            String filePath = params.get("filePath") != null ? (String) params.get("filePath") : null;
            //backup account to keystore
            String backupFileName = keyStoreService.backupAccountToKeyStore(filePath, chainId, address, password);
            map.put("path", backupFileName);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }

        Log.debug("ac_exportAccountKeyStore end");
        return success(map);
    }

    /**
     * 设置账户密码
     * set account password
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_setPassword", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "set account password")
    public Object setPassword(Map params) {
        Log.debug("ac_setPassword start");
        Map<String, Boolean> map = new HashMap<>(1);
        boolean result;
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null || params.get("password") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;

            //set account password
            result = accountService.setPassword(chainId, address, password);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }

        Log.debug("ac_setPassword end");
        return success(map);
    }

    /**
     * 设置离线账户密码
     * set offline account password
     *
     * @param params [chainId,address,priKey,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_setOfflineAccountPassword", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "set offline account password")
    public Object setOfflineAccountPassword(Map params) {
        Log.debug("ac_setOfflineAccountPassword start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null || params.get("priKey") == null || params.get("password") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户私钥
            String priKey = params.get("priKey") != null ? (String) params.get("priKey") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;

            //set account password
            String encryptedPriKey = accountService.setOfflineAccountPassword(chainId, address, priKey, password);
            map.put("encryptedPriKey", encryptedPriKey);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_setOfflineAccountPassword end");
        return success(map);
    }

    /**
     * 根据原密码修改账户密码
     * modify the account password by the original password
     *
     * @param params [chainId,address,password,newPassword]
     * @return
     */
    @CmdAnnotation(cmd = "ac_updatePassword", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "modify the account password by the original password")
    public Object updatePassword(Map params) {
        Log.debug("ac_updatePassword start");
        Map<String, Boolean> map = new HashMap<>(1);
        boolean result;
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null || params.get("password") == null || params.get("newPassword") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户旧密码
            String password = params.get("password") != null ? (String) params.get("password") : null;
            //账户新密码
            String newPassword = params.get("newPassword") != null ? (String) params.get("newPassword") : null;

            //change account password
            result = accountService.changePassword(chainId, address, password, newPassword);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_updatePassword end");
        return success(map);
    }

    /**
     * [离线账户修改密码] 根据原密码修改账户密码
     * offline account change password
     *
     * @param params [chainId,address,priKey,password,newPassword]
     * @return
     */
    @CmdAnnotation(cmd = "ac_updateOfflineAccountPassword", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "offline account change password")
    public Object updateOfflineAccountPassword(Map params) {
        Log.debug("ac_updateOfflineAccountPassword start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null || params.get("priKey") == null || params.get("password") == null || params.get("newPassword") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户私钥
            String priKey = params.get("priKey") != null ? (String) params.get("priKey") : null;
            //账户旧密码
            String password = params.get("password") != null ? (String) params.get("password") : null;
            //账户新密码
            String newPassword = params.get("newPassword") != null ? (String) params.get("newPassword") : null;

            //set account password
            String encryptedPriKey = accountService.changeOfflinePassword(chainId, address, priKey, password, newPassword);
            map.put("encryptedPriKey", encryptedPriKey);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_updateOfflineAccountPassword end");
        return success( map);
    }

    /**
     * 根据账户地址获取账户是否加密
     * whether the account is encrypted by the account address
     *
     * @param params [chainId,address]
     * @return
     */
    @CmdAnnotation(cmd = "ac_isEncrypted", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "whether the account is encrypted by the account address")
    public Object isEncrypted(Map params) {
        Log.debug("ac_isEncrypted start");
        Map<String, Boolean> map = new HashMap<>(1);
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户是否加密
            boolean result = accountService.isEncrypted(chainId, address);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_isEncrypted end");
        return success(map);
    }

    /**
     * 验证账户密码是否正确
     * verify that the account password is correct
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_validationPassword", version = 1.0, scope = "private", minEvent = 0, minPeriod = 0, description = "verify that the account password is correct")
    public Object validationPassword(Map params) {
        Log.debug("ac_validationPassword start");
        Map<String, Boolean> map = new HashMap<>(1);
        try {
            // check parameters
            if (params == null || params.get("chainId") == null || params.get("address") == null || params.get("password") == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }

            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get("chainId");
            //账户地址
            String address = params.get("address") != null ? (String) params.get("address") : null;
            //账户密码
            String password = params.get("password") != null ? (String) params.get("password") : null;

            //check the account is exist
            Account account = accountService.getAccount(chainId, address);
            if (null == account) {
                throw new NulsRuntimeException(AccountErrorCode.ACCOUNT_NOT_EXIST);
            }
            //verify that the account password is correct
            boolean result = account.validatePassword(password);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode());
        }
        Log.debug("ac_validationPassword end");
        return success(map);
    }

}
