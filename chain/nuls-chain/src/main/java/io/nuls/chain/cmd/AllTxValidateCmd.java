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
package io.nuls.chain.cmd;

import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.Parameter;
import io.nuls.rpc.model.message.Response;

import java.util.Map;

/**
 * @program: nuls2.0
 * @description: moduleValidateCmd
 * @author: lan
 * @create: 2018/11/22
 **/
public class AllTxValidateCmd extends BaseCmd {
    /**
     * chainModuleTxValidate
     * 重连网络
     */
    @CmdAnnotation(cmd = "cm_chainModuleTxValidate", version = 1.0,
            description = "chainModuleTxValidate")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]", parameterValidRegExp = "")
    @Parameter(parameterName = "txHexs", parameterType = "array")
    public Response chainModuleTxValidate(Map params){
        //1获取交易类型
        //2进入不同验证器里处理
        //3封装失败交易返回
        return null;
    }
}
