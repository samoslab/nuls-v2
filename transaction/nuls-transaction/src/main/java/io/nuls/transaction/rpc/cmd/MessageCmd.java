package io.nuls.transaction.rpc.cmd;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.ObjectUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.cache.TxDuplicateRemoval;
import io.nuls.transaction.constant.TxCmd;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.message.*;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.CrossTx;
import io.nuls.transaction.rpc.call.NetworkCall;
import io.nuls.transaction.service.ConfirmedTxService;
import io.nuls.transaction.service.CtxService;
import io.nuls.transaction.service.TxService;
import io.nuls.transaction.storage.rocksdb.CtxStorageService;
import io.nuls.transaction.utils.TxUtil;

import java.util.HashMap;
import java.util.Map;

import static io.nuls.transaction.constant.TxCmd.NW_NEW_MN_TX;
import static io.nuls.transaction.constant.TxCmd.NW_VERIFYR_ESULT;
import static io.nuls.transaction.constant.TxConstant.*;
import static io.nuls.transaction.utils.LoggerUtil.Log;

/**
 * 处理网络协议数据
 *
 * @author: qinyifeng
 * @date: 2018/12/26
 */
@Component
public class MessageCmd extends BaseCmd {
    @Autowired
    private CtxService ctxService;
    @Autowired
    private TxService txService;
    @Autowired
    private ConfirmedTxService confirmedTxService;
    @Autowired
    private CtxStorageService ctxStorageService;
    @Autowired
    private ChainManager chainManager;

    /**
     * 接收链内广播的新交易hash
     * receive new transaction hash
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_NEW_HASH, version = 1.0, description = "receive new transaction hash")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response newHash(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            chain = chainManager.getChain(chainId);
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析广播交易hash消息
            BroadcastTxMessage message = new BroadcastTxMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [newHash], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            NulsDigestData hash = message.getRequestHash();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [newHash] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, hash.getDigestHex());
            //交易缓存中是否已存在该交易hash
            boolean consains = TxDuplicateRemoval.mightContain(hash);
            if (consains) {
                return success();
            }
            //如果交易hash不存在，则添加到缓存中
            TxDuplicateRemoval.insert(hash);
            //去该节点查询完整交易
            GetTxMessage getTxMessage = new GetTxMessage();
            getTxMessage.setCommand(TxCmd.NW_ASK_TX);
            getTxMessage.setRequestHash(hash);
            result = NetworkCall.sendToNode(chainId, getTxMessage, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * 获取完整链内交易数据,包含还未开始跨链处理的跨链交易
     * get complete transaction data
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_ASK_TX, version = 1.0, description = "get complete transaction data")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response askTx(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析获取完整交易消息
            GetTxMessage message = new GetTxMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [askTx], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            NulsDigestData txHash = message.getRequestHash();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [askTx] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, txHash.getDigestHex());
            Transaction tx = txService.getTransaction(chain, txHash);
            if (tx == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }
            result = NetworkCall.sendTxToNode(chainId, nodeId, tx);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * 接收链内其他节点的新的完整交易
     * receive new transactions from other nodes
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_RECEIVE_TX, version = 1.0, description = "receive new transactions from other nodes")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response receiveTx(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            String nodeId = params.get(KEY_NODE_ID).toString();
            chain = chainManager.getChain(chainId);
            //解析新的交易消息
            TransactionMessage message = new TransactionMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [receiveTx], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            Transaction transaction = message.getTx();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [receiveTx] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, transaction.getHash().getDigestHex());
            TxUtil.txInformationDebugPrint(chain, transaction, chain.getLoggerMap().get(TxConstant.LOG_TX));
            //交易缓存中是否已存在该交易hash
            boolean consains = TxDuplicateRemoval.mightContain(transaction.getHash());
            if (!consains) {
                //添加到交易缓存中
                TxDuplicateRemoval.insert(transaction.getHash());
            }
            //将交易放入待验证本地交易队列中
            txService.newTx(chainManager.getChain(chainId), transaction);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", true);
        return success(map);
    }

    /**
     * (主网,友链都要处理)接收广播的新跨链交易hash
     * receive new cross transaction hash
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_NEW_CROSS_HASH, version = 1.0, description = "receive new cross transaction hash")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response newCrossHash(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            chain = chainManager.getChain(chainId);
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析广播跨链交易hash消息
            BroadcastTxMessage message = new BroadcastTxMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [newCrossHash], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            NulsDigestData hash = message.getRequestHash();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [newCrossHash] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, hash.getDigestHex());
            //交易缓存中是否已存在该交易hash
            boolean consains = TxDuplicateRemoval.mightContain(hash);
            if (consains) {
                return success();
            }
            //如果交易hash不存在，则添加到缓存中
            TxDuplicateRemoval.insert(hash);
            //去该节点查询完整跨链交易
            GetTxMessage getTxMessage = new GetTxMessage();
            getTxMessage.setCommand(TxCmd.NW_ASK_CROSS_TX_M_FC);
            getTxMessage.setRequestHash(hash);
            result = NetworkCall.sendToNode(chainId, getTxMessage, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * 主网向友链索取完整跨链交易
     * The main network asks for complete cross-chain transactions from the Friends Chain
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_ASK_CROSS_TX_M_FC, version = 1.0, description = "The main network asks for complete cross-chain transactions from the Friends Chain")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response askCrossTxM2Fc(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析获取完整跨链交易消息
            GetTxMessage message = new GetTxMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [askCrossTxM2Fc], message is null from node-{}, chainId:{}", nodeId, chainId);
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            //查询已确认跨链交易
            NulsDigestData txHash = message.getRequestHash();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [askCrossTxM2Fc] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, txHash.getDigestHex());
            Transaction tx = confirmedTxService.getConfirmedTransaction(chain, txHash);
            if (tx == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }
            //交易是否被确认超过指定高度
            if (chain.getBestBlockHeight() < (tx.getBlockHeight() + TxConstant.CTX_EFFECT_THRESHOLD)) {
                throw new NulsException(TxErrorCode.TX_NOT_EFFECTIVE_HEIGHT);
            }
            //发送跨链交易到指定节点
            CrossTxMessage crossTxMessage = new CrossTxMessage();
            crossTxMessage.setCommand(NW_NEW_MN_TX);
            crossTxMessage.setTx(tx);
            result = NetworkCall.sendToNode(chainId, crossTxMessage, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * 主网链内节点索取完整跨链交易
     * Complete Cross-Chain Transaction Required by Nodes in the Main Network Chain
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_ASK_CROSS_TX_M_M, version = 1.0, description = "Complete Cross-Chain Transaction Required by Nodes in the Main Network Chain")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response askCrossTxM2M(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析获取完整跨链交易消息
            GetTxMessage message = new GetTxMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [askCrossTxM2M], message is null from node-{}, chainId:{}", nodeId, chainId);
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            NulsDigestData txHash = message.getRequestHash();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [askCrossTxM2M] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, txHash.getDigestHex());
            //查询处理中的跨链交易
            CrossTx ctx = ctxStorageService.getTx(chain.getChainId(), message.getRequestHash());
            if (ctx == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }
            Transaction tx = ctx.getTx();
            if (tx == null) {
                //查询已确认跨链交易
                tx = confirmedTxService.getConfirmedTransaction(chain, txHash);
                if (tx == null) {
                    throw new NulsException(TxErrorCode.TX_NOT_EXIST);
                }
            }
            //发送跨链交易到指定节点
            CrossTxMessage crossTxMessage = new CrossTxMessage();
            crossTxMessage.setCommand(NW_NEW_MN_TX);
            crossTxMessage.setTx(tx);
            result = NetworkCall.sendToNode(chainId, crossTxMessage, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * 友链向主网索取完整跨链交易
     * Friend Chain Requests Complete Cross-Chain Transactions from Main Network
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_ASK_CROSS_TX_FC_M, version = 1.0, description = "Friend Chain Requests Complete Cross-Chain Transactions from Main Network")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response askCrossTxFc2M(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析获取完整跨链交易消息
            GetTxMessage message = new GetTxMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [askCrossTxFc2M], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            //查询已确认跨链交易
            NulsDigestData txHash = message.getRequestHash();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [askCrossTxFc2M] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, txHash.getDigestHex());

            Transaction tx = confirmedTxService.getConfirmedTransaction(chain, txHash);
            if (tx == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }
            //交易是否被确认超过指定高度
            if (chain.getBestBlockHeight() < (tx.getBlockHeight() + TxConstant.CTX_EFFECT_THRESHOLD)) {
                throw new NulsException(TxErrorCode.TX_NOT_EFFECTIVE_HEIGHT);
            }
            //发送跨链交易到指定节点
            CrossTxMessage crossTxMessage = new CrossTxMessage();
            crossTxMessage.setCommand(NW_NEW_MN_TX);
            crossTxMessage.setTx(tx);
            result = NetworkCall.sendToNode(chainId, crossTxMessage, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * 接收友链节点发送的新的完整跨链交易
     * receive new cross transactions from other nodes
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_NEW_MN_TX, version = 1.0, description = "receive new cross transactions from other nodes")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response newMnTx(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            chain = chainManager.getChain(chainId);
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析新的跨链交易消息
            CrossTxMessage message = new CrossTxMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [newMnTx], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            Transaction transaction = message.getTx();
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [newMnTx] message from node-{}, chainId:{}, hash:{}", nodeId, chainId, transaction.getHash().getDigestHex());
            TxUtil.txInformationDebugPrint(chain, transaction, chain.getLoggerMap().get(TxConstant.LOG_TX));

            //交易缓存中是否已存在该交易hash
            boolean consains = TxDuplicateRemoval.mightContain(transaction.getHash());
            if (!consains) {
                //添加到交易缓存中
                TxDuplicateRemoval.insert(transaction.getHash());
            }
            //保存未验证跨链交易
            ctxService.newCrossTx(chain, nodeId, transaction);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", true);
        return success(map);
    }

    /**
     * (友链处理)主网节点向友链节点验证跨链交易
     * verification of cross-chain transactions from home network node to friend chain node
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_VERIFY_FC, version = 1.0, description = "verification of cross-chain transactions from home network node to friend chain node")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response verifyFc(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析跨链交易验证消息
            VerifyCrossWithFCMessage message = new VerifyCrossWithFCMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [verifyFc], message is null from node-{}, chainId:{}", nodeId, chainId);
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            //解析原始交易hash
            byte[] origTxHashByte = message.getOriginalTxHash();
            NulsDigestData originalTxHash = NulsDigestData.fromDigestHex(HexUtil.encode(origTxHashByte));
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [verifyFc] message from node-{}, chainId:{}, originalTxHash:{}", nodeId, chainId, originalTxHash.getDigestHex());
            //查询已确认跨链交易
            Transaction tx = confirmedTxService.getConfirmedTransaction(chainManager.getChain(chainId), originalTxHash);
            if (tx == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }
            //验证该交易所在的区块是否被确认超过指定高度
            if (chain.getBestBlockHeight() < (tx.getBlockHeight() + TxConstant.CTX_EFFECT_THRESHOLD)) {
                throw new NulsException(TxErrorCode.TX_NOT_EFFECTIVE_HEIGHT);
            }

            //TODO 将atx交易进行协议转换生成新的Anode2_atx_trans，再验证接收到的atx_trans_hash与Anode2_atx_trans_hash一致

            //发送跨链交易验证结果到指定节点
            VerifyCrossResultMessage verifyResultMessage = new VerifyCrossResultMessage();
            verifyResultMessage.setCommand(NW_VERIFYR_ESULT);
            verifyResultMessage.setRequestHash(message.getRequestHash());
            verifyResultMessage.setHeight(tx.getBlockHeight());
            result = NetworkCall.sendToNode(chainId, verifyResultMessage, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * (主网和友链都要处理)节点接收其他链节点发送的跨链验证结果
     * home network node receive cross-chain verify results sent by friend chain node
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_VERIFYR_ESULT, version = 1.0, description = "home network node receive cross-chain verify results sent by friend chain node")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response verifyResult(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            chain = chainManager.getChain(chainId);
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析跨链交易验证结果消息
            VerifyCrossResultMessage message = new VerifyCrossResultMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [verifyResult], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [verifyResult] message from node-{}, chainId:{}, txHash:{}", nodeId, chainId, message.getRequestHash().getDigestHex());
            //处理跨链节点验证结果
            ctxService.ctxResultProcess(chain, message, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", true);
        return success(map);
    }

    /**
     * 友链节点向主网节点验证跨链交易
     * verification of cross-chain transactions from friend chain node to home network node
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_VERIFY_MN, version = 1.0, description = "verification of cross-chain transactions from friend chain node to home network node")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response verifyMn(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            Integer chainId = Integer.parseInt(params.get(KEY_CHAIN_ID).toString());
            String nodeId = params.get(KEY_NODE_ID).toString();
            //解析跨链交易验证消息
            VerifyCrossWithFCMessage message = new VerifyCrossWithFCMessage();
            byte[] decode = HexUtil.decode(params.get(KEY_MESSAGE_BODY).toString());
            message.parse(new NulsByteBuffer(decode));
            if (message == null) {
                chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                        "recieve [verifyMn], message is null from node-{}, chainId:{}", nodeId, chainId);
                return failed(TxErrorCode.PARAMETER_ERROR);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            //解析原始交易hash
            byte[] origTxHashByte = message.getOriginalTxHash();
            NulsDigestData originalTxHash = NulsDigestData.fromDigestHex(HexUtil.encode(origTxHashByte));
            chain.getLoggerMap().get(TxConstant.LOG_TX).debug(
                    "recieve [verifyMn] message from node-{}, chainId:{}, txHash:{}", nodeId, chainId, originalTxHash.getDigestHex());
            //查询已确认跨链交易
            Transaction tx = confirmedTxService.getConfirmedTransaction(chainManager.getChain(chainId), originalTxHash);
            if (tx == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }
            //验证该交易所在的区块是否被确认超过指定高度
            if (chain.getBestBlockHeight() < (tx.getBlockHeight() + TxConstant.CTX_EFFECT_THRESHOLD)) {
                throw new NulsException(TxErrorCode.TX_NOT_EFFECTIVE_HEIGHT);
            }

            //TODO 将atx交易进行协议转换生成新的Anode2_atx_trans，再验证接收到的atx_trans_hash与Anode2_atx_trans_hash一致

            //发送跨链交易验证结果到指定节点
            VerifyCrossResultMessage verifyResultMessage = new VerifyCrossResultMessage();
            verifyResultMessage.setCommand(NW_VERIFYR_ESULT);
            verifyResultMessage.setRequestHash(message.getRequestHash());
            verifyResultMessage.setHeight(tx.getBlockHeight());
            result = NetworkCall.sendToNode(chainId, verifyResultMessage, nodeId);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        map.put("value", result);
        return success(map);
    }

    /**
     * 接收链内其他节点广播的跨链验证结果, 并保存.
     * 1.如果接收者是主网 当一个交易的签名者超过共识节点总数的80%，则通过
     * 2.如果接受者是友链 如果交易的签名者是友链最近x块的出块者
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = TxCmd.NW_CROSS_NODE_RS, version = 1.0, description = "friend chain node receive cross-chain verify results sent by home network node")
    @Parameter(parameterName = KEY_CHAIN_ID, parameterType = "int")
    @Parameter(parameterName = KEY_NODE_ID, parameterType = "String")
    public Response crossNodeRs(Map params) {
        Map<String, Boolean> map = new HashMap<>();
        boolean result;
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get(KEY_CHAIN_ID), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get(KEY_NODE_ID), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get(KEY_MESSAGE_BODY), TxErrorCode.PARAMETER_ERROR.getMsg());

            chain = chainManager.getChain((int) params.get(KEY_CHAIN_ID));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }

            String nodeId = (String) params.get(KEY_NODE_ID);
            //解析验证结果消息
            BroadcastCrossNodeRsMessage message = new BroadcastCrossNodeRsMessage();
            byte[] decode = HexUtil.decode((String) params.get(KEY_MESSAGE_BODY));
            message.parse(new NulsByteBuffer(decode));
            //查询处理中的跨链交易
 /*           CrossTx ctx = ctxStorageService.getTx(chain.getChainId(), message.getRequestHash());

            if (ctx == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }

            //获取跨链交易验证结果
            List<CrossTxVerifyResult> verifyResultList = ctx.getCtxVerifyResultList();
            if (verifyResultList == null) {
                verifyResultList = new ArrayList<>();
            }
            //添加新的跨链验证结果
//            CrossTxVerifyResult verifyResult = new CrossTxVerifyResult();
//            verifyResult.setChainId(chainId);
//            verifyResult.setNodeId(nodeId);
//            verifyResult.setHeight(message.getHeight());
//            verifyResultList.add(verifyResult);
//            ctx.setCtxVerifyResultList(verifyResultList);
//            ctx.setState(TxConstant.CTX_VERIFY_RESULT_2);
            //保存跨链交易验证结果
            result = ctxStorageService.putTx(chainId, ctx);*/
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        //map.put("value", result);
        return success(map);
    }


    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            Log.error(e);
        } else {
            chain.getLoggerMap().get(TxConstant.LOG_TX).error(e);
        }
    }
}
