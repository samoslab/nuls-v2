package io.nuls.poc.pbft;

/**
 * @author Niels
 */
public class BlockVoter implements Runnable {

    private final int chainId;

    public BlockVoter(int chainId) {
        this.chainId = chainId;
        new Thread(this).start();
    }

    @Override
    public void run() {

    }
}
