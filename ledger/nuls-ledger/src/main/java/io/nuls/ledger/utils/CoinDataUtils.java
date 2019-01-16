package io.nuls.ledger.utils;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.CoinData;
import io.nuls.tools.exception.NulsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangkun23 on 2018/12/5.
 */
public class CoinDataUtils {
    static final Logger logger = LoggerFactory.getLogger(CoinDataUtils.class);

    /**
     * parseCoinData
     *
     * @param stream
     * @return
     */
    public static CoinData parseCoinData(byte[] stream) {
        if(null == stream){
            return null;
        }
        CoinData coinData = new CoinData();
        try {
            coinData.parse(new NulsByteBuffer(stream));
        } catch (NulsException e) {
            logger.error("coinData parse error", e);
        }
        return coinData;
    }
}
