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
package io.nuls.ledger.rpc.cmd;


import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.ledger.constant.LedgerConstant;
import io.nuls.ledger.constant.LedgerErrorCode;
import io.nuls.ledger.service.ChainAssetsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author lan
 * @description 查询链资产接口
 * @date 2019/02/14
 **/
@Component
public class ChainAssetCmd extends BaseLedgerCmd {
    @Autowired
    ChainAssetsService chainAssetsService;

    @CmdAnnotation(cmd = "getAssetsById",
            version = 1.0,
            description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "assetIds", parameterType = "String")
    public Response getAssetsById(Map params) {
        List<Map<String, Object>> rtAssetList = new ArrayList<>();
        Integer addressChainId = (Integer) params.get("chainId");
        Integer assetChainId = addressChainId;
        if (!chainHanlder(addressChainId)) {
            return failed(LedgerErrorCode.CHAIN_INIT_FAIL);
        }
        String assetIds = params.get("assetIds").toString();
        String[] assetIdList = assetIds.split(LedgerConstant.COMMA);
        for (String assetIdStr : assetIdList) {
            Map<String, Object> map = chainAssetsService.getAssetByChainAssetId(addressChainId, assetChainId, Integer.valueOf(assetIdStr));
            rtAssetList.add(map);
        }
        Map<String, Object> rtMap = new HashMap<>();
        rtMap.put("assets", rtAssetList);
        return success(rtMap);
    }
}
