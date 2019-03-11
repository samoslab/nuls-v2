package io.nuls.api.analysis;

import io.nuls.api.ApiContext;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.CommandConstant;
import io.nuls.api.model.po.db.AccountInfo;
import io.nuls.api.model.po.db.AgentInfo;
import io.nuls.api.model.po.db.BlockInfo;
import io.nuls.api.model.po.db.TransactionInfo;
import io.nuls.api.rpc.RpcCall;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Block;
import io.nuls.base.data.Transaction;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.ModuleE;
import io.nuls.tools.basic.Result;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.exception.NulsException;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class WalletRpcHandler {

    public static BlockInfo getBlockInfo(int chainID, long height) {
        Map<String, Object> params = new HashMap<>(ApiConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, ApiContext.VERSION);
        params.put("chainId", chainID);
        params.put("height", height);
        try {
            String blockHex = (String) RpcCall.request(ModuleE.BL.abbr, CommandConstant.GET_BLOCK_BY_HEIGHT, params);
            if (null == blockHex) {
                return null;
            }
            byte[] bytes = HexUtil.decode(blockHex);
            Block block = new Block();
            block.parse(new NulsByteBuffer(bytes));
            return AnalysisHandler.toBlockInfo(block, chainID);
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BlockInfo getBlockInfo(int chainID, String hash) {
        Map<String, Object> params = new HashMap<>(ApiConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, ApiContext.VERSION);
        params.put("chainId", chainID);
        params.put("hash", hash);
        try {
            String blockHex = (String) RpcCall.request(ModuleE.BL.abbr, CommandConstant.GET_BLOCK_BY_HASH, params);
            byte[] bytes = HexUtil.decode(blockHex);
            Block block = new Block();
            block.parse(new NulsByteBuffer(bytes));
            return AnalysisHandler.toBlockInfo(block, chainID);
        } catch (NulsException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static AccountInfo getAccountBalance(int chainId, String address, int assetChainId, int assetId) {
        Map<String, Object> params = new HashMap<>(ApiConstant.INIT_CAPACITY_8);
        params.put(Constants.VERSION_KEY_STR, ApiContext.VERSION);
        params.put("chainId", chainId);
        params.put("address", address);
        params.put("assetChainId", assetChainId);
        params.put("assetId", assetId);
        try {
            Map map = (Map) RpcCall.request(ModuleE.LG.abbr, CommandConstant.GET_BALANCE, params);
            AccountInfo accountInfo = new AccountInfo();
            accountInfo.setTotalBalance(new BigInteger(map.get("total").toString()));
            accountInfo.setBalance(new BigInteger(map.get("available").toString()));
            accountInfo.setTimeLock(new BigInteger(map.get("timeHeightLocked").toString()));
            accountInfo.setConsensusLock(new BigInteger(map.get("permanentLocked").toString()));

            return accountInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TransactionInfo getTx(int chainId, String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, ApiContext.VERSION);
        params.put("chainId", chainId);
        params.put("txHash", hash);
        Map map = (Map) RpcCall.request(ModuleE.TX.abbr, CommandConstant.GET_TX, params);
        String txHex = (String) map.get("txHex");
        Transaction tx = Transaction.getInstance(txHex);
        TransactionInfo txInfo = AnalysisHandler.toTransaction(tx);
        return txInfo;
    }

    public static Result<AgentInfo> getAgentInfo(int chainId, String hash) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constants.VERSION_KEY_STR, ApiContext.VERSION);
        params.put("chainId", chainId);
        params.put("agentHash", hash);
        try {
            Map map = (Map) RpcCall.request(ModuleE.CS.abbr, CommandConstant.GET_AGENT, params);

        } catch (NulsException e) {
            e.printStackTrace();
        }
        return null;
    }
}
