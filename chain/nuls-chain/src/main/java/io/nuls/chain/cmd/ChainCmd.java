package io.nuls.chain.cmd;

import io.nuls.base.data.chain.Asset;
import io.nuls.base.data.chain.Chain;
import io.nuls.chain.service.AssetService;
import io.nuls.chain.service.ChainService;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.CmdResponse;
import io.nuls.tools.constant.ErrorCode;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.tools.thread.TimeService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tangyi
 * @date 2018/11/7
 * @description
 */
@Component
public class ChainCmd extends BaseCmd {

    @Autowired
    private ChainService chainService;

    @Autowired
    private AssetService assetService;

    @CmdAnnotation(cmd = "chain", version = 1.0, preCompatible = true)
    public CmdResponse chain(List params) {
        try {
            Chain chain = chainService.getChain(Short.valueOf(params.get(0).toString()));
            List<Asset> assetList = assetService.getAssetListByChain(Short.valueOf(params.get(0).toString()));
            chain.setAssetList(assetList);
            return success("chain", chain);
        } catch (Exception e) {
            Log.error(e);
            return failed(ErrorCode.init("-100"), e.getMessage());
        }
    }

    @CmdAnnotation(cmd = "chainReg", version = 1.0, preCompatible = true)
    public CmdResponse chainReg(List params) {
        try {
            Chain chain = new Chain();
            chain.setChainId(Short.valueOf(params.get(0).toString()));
            chain.setName((String) params.get(1));
            chain.setAddressType((String) params.get(2));
            chain.setMagicNumber(Integer.valueOf(params.get(3).toString()));
            chain.setSupportInflowAsset((Boolean) params.get(4));
            chain.setMinAvailableNodeNum(Integer.valueOf(params.get(5).toString()));
            chain.setSingleNodeMinConnectionNum(Integer.valueOf(params.get(6).toString()));
            chain.setTxConfirmedBlockNum(Integer.valueOf(params.get(7).toString()));
            chain.setSeedList(new ArrayList<>());
            chain.setCreateTime(TimeService.currentTimeMillis());

            // TODO
            return success("sent newTx", null);
        } catch (Exception e) {
            Log.error(e);
            return failed(ErrorCode.init("-100"), e.getMessage());
        }
    }

    @CmdAnnotation(cmd = "chainRegValidator", version = 1.0, preCompatible = true)
    public CmdResponse chainRegValidator(List params) {
        try {
            Chain chain = JSONUtils.json2pojo(JSONUtils.obj2json(params.get(0)), Chain.class);
            if (chain.getChainId() < 0) {
                return failed(ErrorCode.init("-10002"));
            }
            if (chainService.getChain(chain.getChainId()) != null) {
                return failed(ErrorCode.init("-10001"));
            }

            return success();
        } catch (Exception e) {
            Log.error(e);
            return failed(ErrorCode.init("-100"), e.getMessage());
        }
    }

    @CmdAnnotation(cmd = "chainRegCommit", version = 1.0, preCompatible = true)
    public CmdResponse chainRegCommit(List params) {
        try {
            Chain chain = JSONUtils.json2pojo(JSONUtils.obj2json(params.get(0)), Chain.class);

            chainService.saveChain(chain);

            return success("chainRegCommit", null);
        } catch (Exception e) {
            Log.error(e);
            return failed(ErrorCode.init("-100"), e.getMessage());
        }
    }

    @CmdAnnotation(cmd = "chainRegRollback", version = 1.0, preCompatible = true)
    public CmdResponse chainRegRollback(List params) {
        try {

            return success("chainRegRollback", null);
        } catch (Exception e) {
            Log.error(e);
            return failed(ErrorCode.init("-100"), e.getMessage());
        }
    }
}
