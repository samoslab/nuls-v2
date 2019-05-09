package io.nuls.contract.helper;

import io.nuls.base.basic.AddressTool;
import io.nuls.contract.util.Log;
import io.nuls.contract.vm.Frame;

public class ContractNewTxFromOtherModuleHandlerMock extends ContractNewTxFromOtherModuleHandler{

    @Override
    public void updateNonceAndVmBalance(int chainId, byte[] contractAddressBytes, String txHash, String txStr, Frame frame) {
        Log.info("chainId: {}, contractAddress: {}, txHash: {}, txStr: {}", chainId, AddressTool.getStringAddressByBytes(contractAddressBytes), txHash, txStr);
    }
}