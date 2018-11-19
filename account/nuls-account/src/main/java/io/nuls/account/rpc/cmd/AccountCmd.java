package io.nuls.account.rpc.cmd;

import io.nuls.account.constant.AccountConstant;
import io.nuls.account.constant.AccountErrorCode;
import io.nuls.account.model.bo.Account;
import io.nuls.account.model.bo.AccountKeyStore;
import io.nuls.account.model.dto.AccountKeyStoreDto;
import io.nuls.account.model.dto.AccountOfflineDto;
import io.nuls.account.model.dto.SimpleAccountDto;
import io.nuls.account.service.AccountKeyStoreService;
import io.nuls.account.service.AccountService;
import io.nuls.account.util.AccountTool;
import io.nuls.base.data.Page;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.CmdResponse;
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
     * Create a specified number of accounts
     *
     * @param params [chainId,count,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_createAccount", version = 1.0, preCompatible = true)
    public CmdResponse createAccount(List params) {
        Log.debug("ac_createAccount start");
        Map<String, List<String>> map = new HashMap<>();
        List<String> list = new ArrayList<>();
        try {
            // check parameters
            if (params.size() != 3 || params.get(0) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //创建账户个数
            Integer count = params.get(1) != null ? (Integer) params.get(1) : 0;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;
            //创建账户
            List<Account> accountList = accountService.createAccount(chainId, count, password);
            if (accountList != null) {
                accountList.forEach(account -> list.add(account.getAddress().toString()));
                map.put("list", list);
            }
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_createAccount end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 创建离线账户, 该账户不保存到数据库, 并将直接返回账户的所有信息
     * Create an offline account, which is not saved to the database and will directly return all information to the account.
     *
     * @param params [chainId,count,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_createOfflineAccount", version = 1.0, preCompatible = true)
    public CmdResponse createOfflineAccount(List params) {
        Log.debug("ac_createOfflineAccount start");
        Map<String, List<AccountOfflineDto>> map = new HashMap<>();
        try {
            // check parameters size
            if (params.size() != 3 || params.get(0) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //创建账户个数
            Integer count = params.get(1) != null ? (Integer) params.get(1) : 0;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;

            //Check parameter is correct.
            if (count <= 0 || count > AccountTool.CREATE_MAX_SIZE) {
                throw new NulsRuntimeException(AccountErrorCode.PARAMETER_ERROR);
            }
            if (StringUtils.isNotBlank(password) && !FormatValidUtils.validPassword(password)) {
                throw new NulsRuntimeException(AccountErrorCode.PASSWORD_FORMAT_WRONG);
            }

            List<AccountOfflineDto> accounts = new ArrayList<>();
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
            map.put("list", accounts);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        } catch (NulsException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_createOfflineAccount end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 根据地址获取账户
     * Get account according to address
     *
     * @param params [chainId,address]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAccountByAddress", version = 1.0, preCompatible = true)
    public CmdResponse getAccountByAddress(List params) {
        Log.debug("ac_getAccountByAddress start");
        Account account;
        try {
            // check parameters
            if (params.size() != 2 || params.get(0) == null || params.get(1) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //根据地址查询账户
            account = accountService.getAccount(chainId, address);
            if (null == account) {
                return success(AccountConstant.SUCCESS_MSG, null);
            }
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_getAccountByAddress end");
        return success(AccountConstant.SUCCESS_MSG, new SimpleAccountDto(account));
    }

    /**
     * 获取所有账户集合,并放入缓存
     * Query all account collections and put them in cache.
     *
     * @param params []
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAccountList", version = 1.0, preCompatible = true)
    public CmdResponse getAccountList(List params) {
        Log.debug("ac_getAccountList start");
        Map<String, List<SimpleAccountDto>> map = new HashMap<>();
        List<SimpleAccountDto> simpleAccountList = new ArrayList<>();
        try {
            //query all accounts
            List<Account> accountList = accountService.getAccountList();
            if (null == accountList) {
                return success(AccountConstant.SUCCESS_MSG, null);
            }
            accountList.forEach(account -> simpleAccountList.add(new SimpleAccountDto((account))));
            map.put("list", simpleAccountList);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_getAccountList end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 分页查询账户地址列表
     * Paging query account address list
     *
     * @param params [chainId,pageNumber,pageSize]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAddressList", version = 1.0, preCompatible = true)
    public CmdResponse getAddressList(List params) {
        Log.debug("ac_getAddressList start");
        Page<String> resultPage;
        try {
            // check parameters size
            if (params.size() != 3 || params.get(0) == null || params.get(1) == null || params.get(2) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //页码
            Integer pageNumber = params.get(1) != null ? (Integer) params.get(1) : 0;
            //每页显示数量
            Integer pageSize = params.get(2) != null ? (Integer) params.get(2) : 0;

            if (chainId <= 0 || pageNumber < 1 || pageSize < 1) {
                throw new NulsRuntimeException(AccountErrorCode.PARAMETER_ERROR);
            }

            //query all accounts
            List<Account> accountList = accountService.getAccountList();
            if (null == accountList) {
                return success(AccountConstant.SUCCESS_MSG, null);
            }
            //根据分页参数返回账户地址列表 Returns the account address list according to paging parameters
            Page<String> page = new Page<>(pageNumber, pageSize);
            page.setTotal(accountList.size());
            int start = (pageNumber - 1) * pageSize;
            if (start >= accountList.size()) {
                return success(AccountConstant.SUCCESS_MSG, page);
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
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_getAddressList end");
        return success(AccountConstant.SUCCESS_MSG, resultPage);
    }

    /**
     * 移除账户
     * Remove specified account
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_removeAccount", version = 1.0, preCompatible = true)
    public CmdResponse removeAccount(List params) {
        Log.debug("ac_removeAccount start");
        boolean result;
        try {
            // check parameters
            if (params.size() != 3 || params.get(0) == null || params.get(1) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;

            //Remove specified account
            result = accountService.removeAccount(chainId, address, password);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_removeAccount end");

        Map<String, Boolean> map = new HashMap<>();
        map.put("value", result);
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 根据地址查询账户私匙,只返回加密账户私钥，未加密账户不返回
     * Inquire the account's private key according to the address.
     * Only returns the private key of the encrypted account, and the unencrypted account does not return.
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getPriKeyByAddress", version = 1.0, preCompatible = true)
    public CmdResponse getPriKeyByAddress(List params) {
        Log.debug("ac_getPriKeyByAddress start");
        String unencryptedPrivateKey;
        try {
            // check parameters
            if (params.size() != 3 || params.get(0) == null || params.get(1) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;

            //Get the account private key
            unencryptedPrivateKey = accountService.getPrivateKey(chainId, address, password);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_getPriKeyByAddress end");

        Map<String, String> map = new HashMap<>();
        map.put("priKey", unencryptedPrivateKey);
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 获取所有本地账户账户私钥，必须保证所有账户密码一致
     * 如果本地账户中的密码不一致，将返回错误信息
     * Get the all local private keys
     * If the password in the local account is different, the error message will be returned.
     *
     * @param params [chainId,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_getAllPriKey", version = 1.0, preCompatible = true)
    public CmdResponse getAllPriKey(List params) {
        Log.debug("ac_getAllPriKey start");
        Map<String, List<String>> map = new HashMap<>();
        List<String> privateKeyList = new ArrayList<>();
        try {
            // check parameters
            if (params.size() != 2 || params.get(0) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户密码
            String password = params.get(1) != null ? (String) params.get(1) : null;

            //Get the account private key
            privateKeyList = accountService.getAllPrivateKey(chainId, password);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_getAllPriKey end");
        map.put("list", privateKeyList);
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 为账户设置备注
     * Set remark for accounts
     *
     * @param params [chainId,address,remark]
     * @return
     */
    @CmdAnnotation(cmd = "ac_setRemark", version = 1.0, preCompatible = true)
    public CmdResponse setRemark(List params) {
        Log.debug("ac_setRemark start");
        Map<String, Boolean> map = new HashMap<>(1);
        boolean result;
        try {
            // check parameters
            if (params.size() != 3 || params.get(0) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户备注
            String remark = params.get(2) != null ? (String) params.get(2) : null;

            //Get the account private key
            result = accountService.setRemark(chainId, address, remark);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_setRemark end");
        map.put("value", result);
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 根据私钥导入账户
     * Import accounts by private key
     *
     * @param params [chainId,priKey,password,overwrite]
     * @return
     */
    @CmdAnnotation(cmd = "ac_importAccountByPriKey", version = 1.0, preCompatible = true)
    public CmdResponse importAccountByPriKey(List params) {
        Log.debug("ac_importAccountByPriKey start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 4 || params.get(0) == null || params.get(1) == null || params.get(3) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户私钥
            String priKey = params.get(1) != null ? (String) params.get(1) : null;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;
            //账户存在时是否覆盖
            Boolean overwrite = params.get(3) != null ? (Boolean) params.get(3) : null;
            //导入账户
            Account account = accountService.importAccount(chainId, priKey, password, overwrite);
            map.put("address", account.getAddress().toString());
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        } catch (NulsException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_importAccountByPriKey end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }


    /**
     * 根据AccountKeyStore导入账户
     * Import accounts according to AccountKeyStore
     *
     * @param params [chainId,keyStore,password,overwrite]
     * @return
     */
    @CmdAnnotation(cmd = "ac_importAccountByKeystore", version = 1.0, preCompatible = true)
    public CmdResponse importAccountByKeystore(List params) {
        Log.debug("ac_importAccountByKeystore start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 4 || params.get(0) == null || params.get(1) == null || params.get(3) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //keyStore HEX编码
            String keyStore = params.get(1) != null ? (String) params.get(1) : null;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;
            //账户存在时是否覆盖
            Boolean overwrite = params.get(3) != null ? (Boolean) params.get(3) : null;

            AccountKeyStoreDto accountKeyStoreDto;
            try {
                accountKeyStoreDto = JSONUtils.json2pojo(new String(HexUtil.decode(keyStore)), AccountKeyStoreDto.class);
            } catch (IOException e) {
                throw new NulsRuntimeException(AccountErrorCode.ACCOUNTKEYSTORE_FILE_DAMAGED);
            }

            //导入账户
            Account account = keyStoreService.importAccountFormKeyStore(accountKeyStoreDto.toAccountKeyStore(), chainId, password, overwrite);
            map.put("address", account.getAddress().toString());
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        } catch (NulsException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_importAccountByKeystore end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 账户备份，导出AccountKeyStore字符串
     *
     * @param params [chainId,address,password,path]
     * @return
     */
    @CmdAnnotation(cmd = "ac_exportAccountKeyStore", version = 1.0, preCompatible = true)
    public CmdResponse exportAccountKeyStore(List params) {
        Log.debug("ac_exportAccountKeyStore start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 4 || params.get(0) == null || params.get(1) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;
            //文件备份地址
            String filePath = params.get(3) != null ? (String) params.get(3) : null;
            //export account to keystore
            AccountKeyStore accountKeyStore = keyStoreService.exportAccountToKeyStore(chainId, address, password);
            //backup keystore files
            String backupFileName = AccountTool.backUpKeyStore(filePath, new AccountKeyStoreDto(accountKeyStore));
            map.put("path", backupFileName);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }

        Log.debug("ac_exportAccountKeyStore end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 设置账户密码
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_setPassword", version = 1.0, preCompatible = true)
    public CmdResponse setPassword(List params) {
        Log.debug("ac_setPassword start");
        Map<String, Boolean> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 3 || params.get(0) == null || params.get(1) == null || params.get(2) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;

            //set account password
            boolean result = accountService.setPassword(chainId, address, password);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }

        Log.debug("ac_setPassword end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 设置离线账户密码
     *
     * @param params [chainId,address,priKey,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_setOfflineAccountPassword", version = 1.0, preCompatible = true)
    public CmdResponse setOfflineAccountPassword(List params) {
        Log.debug("ac_setOfflineAccountPassword start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 4 || params.get(0) == null || params.get(1) == null || params.get(2) == null || params.get(3) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户私钥
            String priKey = params.get(2) != null ? (String) params.get(2) : null;
            //账户密码
            String password = params.get(3) != null ? (String) params.get(3) : null;

            //set account password
            String encryptedPriKey = accountService.setOfflineAccountPassword(chainId, address, priKey, password);
            map.put("encryptedPriKey", encryptedPriKey);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_setOfflineAccountPassword end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 根据原密码修改账户密码
     *
     * @param params [chainId,address,password,newPassword]
     * @return
     */
    @CmdAnnotation(cmd = "ac_updatePassword", version = 1.0, preCompatible = true)
    public CmdResponse updatePassword(List params) {
        Log.debug("ac_updatePassword start");
        Map<String, Boolean> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 4 || params.get(0) == null || params.get(1) == null || params.get(2) == null || params.get(3) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户旧密码
            String password = params.get(2) != null ? (String) params.get(2) : null;
            //账户新密码
            String newPassword = params.get(3) != null ? (String) params.get(3) : null;

            //change account password
            boolean result = accountService.changePassword(chainId, address, password, newPassword);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_updatePassword end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * [离线钱包修改密码] 根据原密码修改账户密码
     *
     * @param params [chainId,address,priKey,password,newPassword]
     * @return
     */
    @CmdAnnotation(cmd = "ac_updateOfflineAccountPassword", version = 1.0, preCompatible = true)
    public CmdResponse updateOfflineAccountPassword(List params) {
        Log.debug("ac_updateOfflineAccountPassword start");
        Map<String, String> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 5 || params.get(0) == null || params.get(1) == null || params.get(2) == null || params.get(3) == null || params.get(4) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户私钥
            String priKey = params.get(2) != null ? (String) params.get(2) : null;
            //账户旧密码
            String password = params.get(3) != null ? (String) params.get(3) : null;
            //账户新密码
            String newPassword = params.get(4) != null ? (String) params.get(4) : null;

            //set account password
            String encryptedPriKey = accountService.changeOfflinePassword(chainId, address, priKey, password, newPassword);
            map.put("encryptedPriKey", encryptedPriKey);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_updateOfflineAccountPassword end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 根据账户地址获取账户是否加密
     * Whether the account is encrypted according to the account address.
     *
     * @param params [chainId,address]
     * @return
     */
    @CmdAnnotation(cmd = "ac_isEncrypted", version = 1.0, preCompatible = true)
    public CmdResponse isEncrypted(List params) {
        Log.debug("ac_isEncrypted start");
        Map<String, Boolean> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 2 || params.get(0) == null || params.get(1) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }
            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户是否加密
            boolean result = accountService.isEncrypted(chainId, address);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_isEncrypted end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

    /**
     * 验证账户密码是否正确
     * verify that the account password is correct
     *
     * @param params [chainId,address,password]
     * @return
     */
    @CmdAnnotation(cmd = "ac_validationPassword", version = 1.0, preCompatible = true)
    public CmdResponse validationPassword(List params) {
        Log.debug("ac_validationPassword start");
        Map<String, Boolean> map = new HashMap<>(1);
        try {
            // check parameters
            if (params.size() != 3 || params.get(0) == null || params.get(1) == null || params.get(2) == null) {
                throw new NulsRuntimeException(AccountErrorCode.NULL_PARAMETER);
            }

            // parse params
            //链ID
            short chainId = 0;
            chainId += (Integer) params.get(0);
            //账户地址
            String address = params.get(1) != null ? (String) params.get(1) : null;
            //账户密码
            String password = params.get(2) != null ? (String) params.get(2) : null;

            //check the account is exist
            Account account = accountService.getAccount(chainId, address);
            if (null == account) {
                throw new NulsRuntimeException(AccountErrorCode.ACCOUNT_NOT_EXIST);
            }
            //verify that the account password is correct
            boolean result = account.validatePassword(password);
            map.put("value", result);
        } catch (NulsRuntimeException e) {
            return failed(e.getErrorCode(), null);
        }
        Log.debug("ac_validationPassword end");
        return success(AccountConstant.SUCCESS_MSG, map);
    }

}
