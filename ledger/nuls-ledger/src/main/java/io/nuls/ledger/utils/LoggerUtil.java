/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.ledger.utils;

import ch.qos.logback.classic.Level;
import io.nuls.tools.log.logback.LoggerBuilder;
import io.nuls.tools.log.logback.NulsLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lan
 * @description
 * @date 2018/12/17
 **/
public class LoggerUtil {
   public static NulsLogger timeTest = LoggerBuilder.getLogger("./ld", "timeTest", Level.ALL);
    /**
     * 日志
     */
    private static Map<String, NulsLogger> loggerMap = new HashMap<>();
    public static String logLevel = "DEBUG";
    private static final String LOGGER_KEY1 = "ld";
    private static final String LOGGER_KEY2 = "ld_tx";
    private static final String LOGGER_KEY3 = "ld_txRb";
    private static final String LOGGER_KEY4 = "ld_txUncfRb";
    private static final String LOGGER_KEY5 = "ld_txAmount";

    private static NulsLogger defaultLogger = null;

    public static NulsLogger logger() {
        if (null == defaultLogger) {
            defaultLogger = LoggerBuilder.getLogger("./ld", "ld", Level.valueOf(logLevel));
        }
        return defaultLogger;
    }

    public static NulsLogger logger(int chainId) {
        if (null == loggerMap.get(LOGGER_KEY1 + chainId)) {
            createLogger(chainId);
        }
        return loggerMap.get(LOGGER_KEY1 + chainId);
    }

    public static NulsLogger txCommitLog(int chainId) {
        if (null == loggerMap.get(LOGGER_KEY2 + chainId)) {
            createLogger(chainId);
        }
        return loggerMap.get(LOGGER_KEY2 + chainId);
    }

    public static NulsLogger txRollBackLog(int chainId) {
        if (null == loggerMap.get(LOGGER_KEY3 + chainId)) {
            createLogger(chainId);
        }
        return loggerMap.get(LOGGER_KEY3 + chainId);
    }

    public static NulsLogger txUnconfirmedRollBackLog(int chainId) {
        if (null == loggerMap.get(LOGGER_KEY4 + chainId)) {
            createLogger(chainId);
        }
        return loggerMap.get(LOGGER_KEY4 + chainId);
    }

    public static NulsLogger txAmount(int chainId) {
        if (null == loggerMap.get(LOGGER_KEY5 + chainId)) {
            createLogger(chainId);
        }
        return loggerMap.get(LOGGER_KEY5 + chainId);
    }

    public static void createLogger(int chainId) {
        String folderName = "./ld/chain-" + chainId;
        loggerMap.put(LOGGER_KEY1 + chainId, LoggerBuilder.getLogger(folderName, "ld", Level.valueOf(logLevel)));
        loggerMap.put(LOGGER_KEY2 + chainId, LoggerBuilder.getLogger(folderName, "tx", Level.valueOf(logLevel)));
        loggerMap.put(LOGGER_KEY3 + chainId, LoggerBuilder.getLogger(folderName, "txRb", Level.valueOf(logLevel)));
        loggerMap.put(LOGGER_KEY4 + chainId, LoggerBuilder.getLogger(folderName, "txUncfRb", Level.valueOf(logLevel)));
        loggerMap.put(LOGGER_KEY5 + chainId, LoggerBuilder.getLogger(folderName, "txAmount", Level.valueOf(logLevel)));
    }

}
