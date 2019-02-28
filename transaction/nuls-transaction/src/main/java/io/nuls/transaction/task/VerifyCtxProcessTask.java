/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.transaction.task;

import io.nuls.base.data.Transaction;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.constant.TxCmd;
import io.nuls.transaction.db.rocksdb.storage.CtxStorageService;
import io.nuls.transaction.db.rocksdb.storage.UnverifiedCtxStorageService;
import io.nuls.transaction.manager.TransactionManager;
import io.nuls.transaction.message.VerifyCrossWithFCMessage;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.CrossTx;
import io.nuls.transaction.model.bo.CrossTxData;
import io.nuls.transaction.model.bo.Node;
import io.nuls.transaction.rpc.call.NetworkCall;
import io.nuls.transaction.utils.TxUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Charlie
 * @date: 2018-12-27
 */
public class VerifyCtxProcessTask implements Runnable {


//    private CtxService ctxService = SpringLiteContext.getBean(CtxService.class);
    private CtxStorageService ctxStorageService = SpringLiteContext.getBean(CtxStorageService.class);
    private UnverifiedCtxStorageService unverifiedCtxStorageService = SpringLiteContext.getBean(UnverifiedCtxStorageService.class);
    private TransactionManager transactionManager = SpringLiteContext.getBean(TransactionManager.class);
    private Chain chain;

    public VerifyCtxProcessTask(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        try {
            doTask(chain);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * 1.基础验证
     * 2.发送跨链验证
     *
     * @param chain
     */
    private void doTask(Chain chain) throws NulsException {
        try {
            int chainId = chain.getChainId();
            List<CrossTx> unprocessedList = unverifiedCtxStorageService.getTxList(chainId);
            List<CrossTx> processedList = new ArrayList<>();
            //Map<String,List<Node>> nodeMap=new HashMap<>();
            for (CrossTx ctx : unprocessedList) {
                Transaction tx = ctx.getTx();
                //交易验证
                if(!transactionManager.verify(chain, tx)){
                    break;
                }
                CrossTxData crossTxData = TxUtil.getInstance(tx.getTxData(), CrossTxData.class);
                VerifyCrossWithFCMessage verifyCrossWithFCMessage = new VerifyCrossWithFCMessage();
                verifyCrossWithFCMessage.setOriginalTxHash(crossTxData.getOriginalTxHash());
                verifyCrossWithFCMessage.setRequestHash(tx.getHash());
                verifyCrossWithFCMessage.setCommand(TxCmd.NW_VERIFY_FC);
                //获取节点组 放CrossChainTx
                if (ctx.getVerifyNodeList() == null || ctx.getVerifyNodeList().size() == 0) {
                    List<Node> nodeList = NetworkCall.getAvailableNodes(ctx.getSenderChainId(), 1, ctx.getSenderNodeId());
                    ctx.setVerifyNodeList(nodeList);
                }
                //发送跨链验证msg，除去发送者节点
                if (ctx.getVerifyNodeList() != null) {
                    for (Node node : ctx.getVerifyNodeList()) {
                        //TODO 是通过广播发送还是点对点发送
                        boolean rs = NetworkCall.sendToNode(ctx.getSenderChainId(), verifyCrossWithFCMessage, node.getId());
                        if (!rs) {
                            break;
                        }
                        processedList.add(ctx);
                    }
                }
            }
            if (processedList != null && processedList.size() > 0) {
                //添加到处理中
                ctxStorageService.putTxs(chainId, processedList);
                //从未处理DB表中清除
                unverifiedCtxStorageService.removeTxList(chainId, processedList);
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
        }
    }
}
