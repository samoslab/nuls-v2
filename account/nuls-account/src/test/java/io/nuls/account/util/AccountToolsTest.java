package io.nuls.account.util;

import io.nuls.tools.crypto.ECKey;
import io.nuls.tools.exception.NulsException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Niels
 */
public class AccountToolsTest {

    @Test
    public void createAddress() {
        for (int i = 0; i < 1; i++) {
            ECKey ecKey = new ECKey();
            String address = AccountTools.createAddress((short) 261, (byte) 1, ecKey);
            System.out.println(address);
        }
        System.out.println("====================================");
        for (int i = 0; i < 1; i++) {
            ECKey ecKey = new ECKey();
            String address = AccountTools.createAddress((short) 261, (byte) 2, ecKey);
            System.out.println(address);
        }
        System.out.println("====================================");
        for (int i = 0; i < 1; i++) {
            ECKey ecKey = new ECKey();
            String address = AccountTools.createAddress((short) 261, (byte) 3, ecKey);
            System.out.println(address);
        }
        assertTrue(true);
    }

    @Test
    public void createAddress2() throws NulsException {
        for (int i = 0; i < 100; i++) {
            ECKey ecKey = new ECKey();
            String address = AccountTool.createAccount((short) 261).getAddress().toString();
            System.out.println(address);
        }
        System.out.println("====================================");
        assertTrue(true);
    }
}