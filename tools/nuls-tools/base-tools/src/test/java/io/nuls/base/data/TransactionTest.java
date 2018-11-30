package io.nuls.base.data;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.tools.thread.TimeService;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: Charlie
 * @date: 2018/11/30
 */
public class TransactionTest {

    @Test
    public void serialization() throws Exception{
        Transaction tx = new Transaction();
        tx.setType(10);
        tx.setTime(TimeService.currentTimeMillis());
        tx.setBlockHeight(100);
        String remark = "试一试";
        tx.setRemark(StringUtils.bytes(remark));
        CoinData coinData = new CoinData();
        CoinFrom coinFrom = new CoinFrom();

        coinFrom.setAddress(AddressTool.getAddress("WSqyJxB1B83MJaAGYoJDnfqZxNc7o3930"));
        coinFrom.setAmount(new BigInteger("999"));
        coinFrom.setAssetsChainId(1);
        coinFrom.setAssetsId(2);
        coinFrom.setLocked((byte)0);
        coinFrom.setNonce("ABCDEFG".getBytes());

        System.out.println(JSONUtils.obj2json(coinFrom));

        CoinFrom cf1 = new CoinFrom();
        cf1.parse(new NulsByteBuffer(coinFrom.serialize()));
        System.out.println(JSONUtils.obj2json(cf1));

        coinData.addFrom(coinFrom);

        CoinTo coinTo = new CoinTo();
        coinTo.setAddress(AddressTool.getAddress("WSqyJxB1B83MJaAGYoJDnfqZxNc7o3930"));
        coinTo.setAmount(new BigInteger("999"));
        coinTo.setAssetsChainId(1);
        coinTo.setAssetsId(2);
        coinTo.setLockTime(TimeService.currentTimeMillis());
        coinData.addTo(coinTo);

        try {
            tx.setCoinData(coinData.serialize());
            //String hex = HexUtil.encode(tx.serialize());
            String hex = tx.hex();
            System.out.println(hex);
            Transaction transaction = Transaction.getInstance(hex);
            Assert.assertTrue(Arrays.equals(tx.getCoinData(), transaction.getCoinData()));

           /* CoinData cd = new CoinData();
            cd.parse(new NulsByteBuffer(transaction.getCoinData()));

            CoinFrom cf= cd.getFrom().get(0);
            System.out.println(JSONUtils.obj2json(cf));*/
            System.out.println(JSONUtils.obj2json(transaction));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
