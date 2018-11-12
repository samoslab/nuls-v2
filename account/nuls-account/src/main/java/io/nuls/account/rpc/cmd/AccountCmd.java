package io.nuls.account.rpc.cmd;

import io.nuls.account.constant.AccountConstant;
import io.nuls.account.constant.AccountErrorCode;
import io.nuls.account.model.bo.Account;
import io.nuls.account.model.dto.AccountOfflineDto;
import io.nuls.account.model.dto.SimpleAccountDto;
import io.nuls.account.service.AccountService;
import io.nuls.account.util.AccountTool;
import io.nuls.base.data.Page;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.CmdResponse;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.log.Log;

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

    /*
     * CmdAnnotation注解包含
     * 1. 调用的命令
     * 2. 调用的命令的版本
     * 3. 调用的命令是否兼容前一个版本
     *
     * 返回的结果包含：
     * 1. 内置编码
     * 2. 真正调用的版本号
     * 3. 返回的文本
     * 4. 返回的对象，由接口自己约定
     */

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
            if (params.get(0) == null || params.size() != 3) {
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
                for (Account account : accountList) {
                    list.add(account.getAddress().toString());
                }
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
            if (params.get(0) == null || params.size() != 3) {
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
            if (StringUtils.isNotBlank(password) && !AccountTool.validPassword(password)) {
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
            if (params.get(0) == null || params.size() != 2) {
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
        //Map<String, List<String>> map = new HashMap<>();
        //List<String> returnAccountList = new ArrayList<>();
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
            if (params.get(0) == null || params.size() != 3) {
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

}
