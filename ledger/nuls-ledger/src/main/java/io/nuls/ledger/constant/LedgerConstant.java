/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.constant;

/**
 * Created by wangkun23 on 2018/11/19.
 */
public interface LedgerConstant {

    /**
     * 模块配置文件名称
     * Module configuration file name.
     */
    public static String MODULES_CONFIG_FILE = "modules.ini";

    /**
     * 系统使用的编码方式
     * The encoding used by the nuls system.
     */
    public static String DEFAULT_ENCODING = "UTF-8";

    public static int  UNCONFIRMED_NONCE = 0;
    public static int  CONFIRMED_NONCE = 1;
    /**
     * 普通交易处理
     */
    public static int COMMONT_TX = 1;
    /**
     * 解锁交易处理
     */
    public static int  UNLOCK_TX = 2;

    /**
     * 高度解锁的阈值，大于这个值就是时间
     */
    public static final int MAX_HEIGHT_VALUE = 10000000;
    /**
     * 重新统计锁定的时间
     */
    public static final int TIME_RECALCULATE_FREEZE = 1000;

    public static final int PERMANENT_LOCK = -1;
}
