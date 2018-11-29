package io.nuls.chain.test;

import io.nuls.chain.ChainBootstrap;
import io.nuls.chain.model.dto.Asset;
import io.nuls.tools.thread.TimeService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * @author tangyi
 * @date 2018/11/12
 * @description
 */
public class AssetCmdTest {
    @Test
    public void asset() throws Exception {
//        System.out.println(CmdDispatcher.call("asset", new Object[]{1542092573248L}));
//        System.out.println(CmdDispatcher.call("asset", new Object[]{1542092632850L}));
    }

    @Test
    public void assetReg() throws Exception {
//        System.out.println(
//                CmdDispatcher.call(
//                        "assetReg",
//                        new Object[]{(short) 867, "G", "Gold", 200000, 21000000, 8, true}));
    }

    @Test
    public void assetRegValidator() throws Exception {
//        Asset asset = build();
//        asset.setAssetId(1542092573248L);
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setSymbol("showmethemoney");
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setSymbol("￥");
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setName("abnclasjflajsdfljasldfjalsiiwrpqwiefakvcnaskdfjlj");
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setDepositNuls(1);
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setInitNumber(10000 - 1);
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setInitNumber(100000000 + 1);
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setDecimalPlaces((short) 3);
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = build();
//        asset.setDecimalPlaces((short) 9);
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
//
//        asset = new Asset();
//        asset.setAssetId(1542092573248L);
//        asset.setSymbol("￥");
//        System.out.println(CmdDispatcher.call("assetRegValidator", new Object[]{asset}));
    }

    private Asset build() {
        Asset asset = new Asset();
        asset.setChainId(867);
        asset.setAssetId(3);
        asset.setSymbol("HH");
        asset.setName("HHHHHH");
        asset.setDepositNuls(200000);
        asset.setInitNumber("95565");
        asset.setDecimalPlaces((short) 8);
        asset.setAvailable(true);
        asset.setCreateTime(TimeService.currentTimeMillis());
        return asset;
    }

    @Test
    public void assetRegCommit() throws Exception {
        Asset asset = new Asset();
        asset.setChainId((short) 867);
        asset.setAssetId(23);
        asset.setSymbol("B");
        asset.setName("bts");
        asset.setDepositNuls(200000);
        asset.setInitNumber("32232");
        asset.setDecimalPlaces((short) 8);
        asset.setAvailable(true);
        asset.setCreateTime(TimeService.currentTimeMillis());
//        System.out.println(CmdDispatcher.call("assetRegCommit", new Object[]{asset}));
    }

    @Test
    public void assetRegRollback() throws Exception {

    }

    @Test
    public void assetEnable() throws Exception {
//        System.out.println(CmdDispatcher.call("assetEnable", new Object[]{1542092573248L}));
//        System.out.println(CmdDispatcher.call("asset", new Object[]{1542092573248L}));
    }

    @Test
    public void assetDisable() throws Exception {
//        System.out.println(CmdDispatcher.call("assetDisable", new Object[]{1542092573248L}));
//        System.out.println(CmdDispatcher.call("asset", new Object[]{1542092573248L}));
    }

    @Test
    public void assetDisableValidator() throws Exception {
//        System.out.println(CmdDispatcher.call("assetDisableValidator", new Object[]{1, 1542092573248L}));
//        System.out.println(CmdDispatcher.call("assetDisableValidator", new Object[]{867, 1542092573248L}));
    }
    @Test
    public void assetStringTest(){
        BigDecimal b = new BigDecimal("999999999999999999999999999999999999999999");
        System.out.println(b.toString());
    }
}
