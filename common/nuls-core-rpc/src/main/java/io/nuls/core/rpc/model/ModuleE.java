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
package io.nuls.core.rpc.model;

import java.util.Arrays;

/**
 * Module information
 *
 * @author tangyi
 */
public enum ModuleE {
    /**
     * prefix + name
     */
    KE("ke", Constant.KERNEL, "nuls.io"),
    CM("cm", Constant.CHAIN, "nuls.io"),
    AC("ac", Constant.ACCOUNT, "nuls.io"),
    NW("nw", Constant.NETWORK, "nuls.io"),
    CS("cs", Constant.CONSENSUS, "nuls.io"),
    BL("bl", Constant.BLOCK, "nuls.io"),
    LG("lg", Constant.LEDGER, "nuls.io"),
    TX("tx", Constant.TRANSACTION, "nuls.io"),
    EB("eb", Constant.EVENT_BUS, "nuls.io"),
    PU("pu", Constant.PROTOCOL, "nuls.io"),
    CC("cc", Constant.CROSS_CHAIN, "nuls.io"),
    SC("sc", Constant.SMART_CONTRACT, "nuls.io"),
    AP("ap", Constant.API_MODULE, "nuls.io");


    public static class Constant {

        public static final String KERNEL = "kernel";

        public static final String CHAIN = "nuls_chain";

        public static final String ACCOUNT = "account";

        public static final String NETWORK = "network";

        public static final String CONSENSUS = "consensus";

        public static final String BLOCK = "block";

        public static final String LEDGER = "ledger";

        public static final String TRANSACTION = "transaction";

        public static final String EVENT_BUS = "eventbus";

        public static final String PROTOCOL = "protocol";

        public static final String CROSS_CHAIN = "crosschain";

        public static final String SMART_CONTRACT = "smart_contract";

        public static final String API_MODULE = "api_module";
    }

    public final String abbr;
    public final String name;
    public final String domain;

    ModuleE(String abbr, String name, String domain) {
        this.abbr = abbr;
        this.name = name.toLowerCase();
        this.domain = domain;
    }

    public static ModuleE valueOfAbbr(String abbr) {
        return Arrays.stream(ModuleE.values()).filter(m -> m.abbr.equals(abbr)).findFirst().orElseThrow(() -> new IllegalArgumentException("can not found abbr of " + abbr));
    }

    public static boolean hasOfAbbr(String abbr){
        return Arrays.stream(ModuleE.values()).anyMatch(m -> m.abbr.equals(abbr));
    }

    @Override
    public String toString() {
        return domain + "/" + name + "/" + abbr;
    }
}
