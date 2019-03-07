/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
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
 *
 */
package io.nuls.api.model.po.db;

import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Data
public class BlockHeaderInfo {

    private String hash;

    private long height;

    private String preHash;

    private String merkleHash;

    private long createTime;

    private String agentHash;

    private String agentId;

    private String packingAddress;

    private String agentAlias;

    private int txCount;

    private long roundIndex;

    private BigInteger totalFee;

    private BigInteger reward;

    private int size;

    private int packingIndexOfRound;

    private String scriptSign;

    private List<String> txHashList;

    private boolean isSeedPacked;

    private long roundStartTime;

    private int agentVersion;

    public void setByAgentInfo(AgentInfo agentInfo) {
        this.agentHash = agentInfo.getTxHash();
        this.agentId = agentInfo.getAgentId();
        this.agentAlias = agentInfo.getAgentAlias();
    }

}
