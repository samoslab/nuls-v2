package io.nuls.block.manager;

import io.nuls.block.thread.BlockSaver;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class BlockSaverManager {

    private static Map<Integer, BlockSaver> map = new HashMap<>(5);

    public static synchronized BlockSaver getBlockSaver(int chainId) {
        BlockSaver saver = map.get(chainId);
        if (null == saver) {
            saver = new BlockSaver(chainId);
            saver.start();
            map.put(chainId, saver);
        }
        return saver;
    }
}
