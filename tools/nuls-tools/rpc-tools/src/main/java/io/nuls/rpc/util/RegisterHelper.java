package io.nuls.rpc.util;

import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.rpc.netty.channel.manager.ConnectManager;
import io.nuls.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.tools.log.Log;
import io.nuls.tools.protocol.*;

import java.util.*;
import java.util.stream.Collectors;

public class RegisterHelper {

    /**
     * 向交易模块注册交易
     * Register transactions with the transaction module
     */
    public static boolean registerTx(int chainId, Protocol protocol) {
        try {
            List<TxRegisterDetail> txRegisterDetailList = new ArrayList<>();
            List<TxDefine> allowTxs = protocol.getAllowTx();
            for (TxDefine config : allowTxs) {
                TxRegisterDetail detail = new TxRegisterDetail();
                detail.setValidator(config.getValidate());
                detail.setCommit(config.getCommit());
                detail.setRollback(config.getRollback());
                detail.setSystemTx(config.isSystemTx());
                detail.setTxType(config.getType());
                detail.setUnlockTx(config.isUnlockTx());
                detail.setVerifySignature(config.isVerifySignature());
                txRegisterDetailList.add(detail);
            }

            //向交易管理模块注册交易
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            params.put("moduleCode", ConnectManager.LOCAL.getAbbreviation());
            params.put("moduleValidator", protocol.getModuleValidator());
            params.put("moduleCommit", protocol.getModuleCommit());
            params.put("moduleRollback", protocol.getModuleRollback());
            params.put("list", txRegisterDetailList);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_register", params);
            if (!cmdResp.isSuccess()) {
                Log.error("chain ：" + chainId + " Failure of transaction registration,errorMsg: " + cmdResp.getResponseComment());
                return false;
            }
        } catch (Exception e) {
            Log.error("", e);
        }
        return true;
    }

    /**
     * 向协议升级模块注册多版本协议配置
     * Register transactions with the transaction module
     */
    public static boolean registerProtocol(int chainId) {
        if (!ModuleHelper.isSupportProtocolUpdate()) {
            return true;
        }
        try {
            Collection<Protocol> protocols = ProtocolGroupManager.getProtocols(chainId);
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put("chainId", chainId);
            List<Protocol> list = new ArrayList<>(protocols);
            params.put("list", list);
            params.put("moduleCode", ConnectManager.LOCAL.getAbbreviation());

            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.PU.abbr, "registerTx", params);
            if (!cmdResp.isSuccess()) {
                Log.error("chain ：" + chainId + " Failure of transaction registration,errorMsg: " + cmdResp.getResponseComment());
                return false;
            }
        } catch (Exception e) {
            Log.error("", e);
        }
        return true;
    }

    /**
     * 向网络模块注册消息
     *
     * @return
     */
    public static void registerMsg(Protocol protocol) {
        try {
            Map<String, Object> map = new HashMap<>(2);
            List<Map<String, String>> cmds = new ArrayList<>();
            map.put("role", ConnectManager.LOCAL.getAbbreviation());
            List<String> collect = protocol.getAllowMsg().stream().map(MessageDefine::getProtocolCmd).collect(Collectors.toList());
            for (String s : collect) {
                String[] split = s.split(",");
                for (String s1 : split) {
                    Map<String, String> cmd = new HashMap<>(2);
                    cmd.put("protocolCmd", s1);
                    cmd.put("handler", s1);
                    cmds.add(cmd);
                }
            }
            map.put("protocolCmds", cmds);
            ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_protocolRegister", map);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error("registerMsg fail");
        }
    }

}
