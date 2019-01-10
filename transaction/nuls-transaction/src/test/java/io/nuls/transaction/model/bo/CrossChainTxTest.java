package io.nuls.transaction.model.bo;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.tools.exception.NulsException;
import io.nuls.transaction.TestConstant;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class CrossChainTxTest {

    private CrossChainTx obj ;
    byte[] bytes = null;

    @Before
    public void setUp() throws Exception {
        obj = new CrossChainTx();
        obj.setState(3);
        obj.setSenderChainId(324);
        obj.setSenderNodeId("23");
        obj.setVerifyNodeList(Arrays.asList(
                TestConstant.getNode1(),
                TestConstant.getNode2()
        ));
        obj.setCtxVerifyResultList(Arrays.asList(
                TestConstant.getCrossTxVerifyResult1(),
                TestConstant.getCrossTxVerifyResult1()
        ));
        obj.setSignRsList(Arrays.asList(
                TestConstant.getCrossTxSignResult1(),
                TestConstant.getCrossTxSignResult2()
        ));
        obj.setTx(TestConstant.getTransaction1());
    }

    @Test
    public void serializeToStream() {
        try {
            bytes = obj.serialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void parse() {
        if (null == bytes) {
            serializeToStream();
        }
        CrossChainTx obj2 = new CrossChainTx();
        try {
            obj2.parse(new NulsByteBuffer(bytes));
            assertTrue(obj.getHeight()==obj2.getHeight());
            assertEquals(obj.size(), obj2.size());
            assertEquals(obj.getState(), obj2.getState());
            assertTrue(obj.getSenderNodeId().equals(obj2.getSenderNodeId()));
            assertTrue(obj.getSenderChainId() == obj2.getSenderChainId());
            assertTrue(Arrays.equals(obj.getTx().serialize(), obj2.getTx().serialize()));
            TestConstant.equals(obj.getSignRsList(), obj2.getSignRsList());
            TestConstant.equals(obj.getVerifyNodeList(), obj2.getVerifyNodeList());
            TestConstant.equals(obj.getCtxVerifyResultList(), obj2.getCtxVerifyResultList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}