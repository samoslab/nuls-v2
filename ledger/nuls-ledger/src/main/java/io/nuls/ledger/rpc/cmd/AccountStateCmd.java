package io.nuls.ledger.rpc.cmd;

import io.nuls.ledger.db.Repository;
import io.nuls.ledger.model.AccountState;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.CmdResponse;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by wangkun23 on 2018/11/19.
 */
@Component
public class AccountStateCmd extends BaseCmd {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private Repository repository;

    /**
     * when account module create new account,then create accountState
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "lg_createAccount", version = 1.0, preCompatible = true)
    public CmdResponse createAccount(List params) {
        for (Object param : params) {
            logger.info("param {}", param);
        }
        //TODO.. 验证参数个数和格式
        short chainId = (short) params.get(0);
        String address = (String) params.get(1);
        AccountState state = repository.createAccount(chainId, address.getBytes());
        return success("", state);
    }

    /**
     * get user account balance
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "lg_getBalance", version = 1.0, preCompatible = true)
    public CmdResponse getBalance(List params) {
        for (Object param : params) {
            logger.info("param {}", param);
        }
        //TODO.. 验证参数个数和格式
        short chainId = (short) params.get(0);
        String address = (String) params.get(1);
        long balance = repository.getBalance(address.getBytes());
        return success("", balance);
    }

    /**
     * get user account nonce
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "lg_getNonce", version = 1.0, preCompatible = true)
    public CmdResponse getNonce(List params) {
        for (Object param : params) {
            logger.info("param {}", param);
        }
        //TODO.. 验证参数个数和格式
        String address = (String) params.get(1);
        long nonce = repository.getNonce(address.getBytes());
        return success("", nonce);
    }
}
