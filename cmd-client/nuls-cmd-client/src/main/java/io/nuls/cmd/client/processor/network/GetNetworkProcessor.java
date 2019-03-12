/*
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
 */

package io.nuls.cmd.client.processor.network;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.api.provider.Result;
import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.account.AccountService;
import io.nuls.api.provider.account.facade.AccountInfo;
import io.nuls.api.provider.network.NetworkProvider;
import io.nuls.api.provider.network.facade.NetworkInfo;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandHelper;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.cmd.client.processor.ErrorCodeConstants;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.model.StringUtils;
import io.nuls.tools.parse.JSONUtils;

/**
 * @author: zhoulijun
 */
@Component
public class GetNetworkProcessor implements CommandProcessor {

    NetworkProvider networkProvider = ServiceManager.get(NetworkProvider.class);

    @Override
    public String getCommand() {
        return "network";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription());
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "network info --get network info \nnetwork nodes --get network nodes";
    }

    @Override
    public boolean argsValidate(String[] args) {
        if (args.length != 2) {
            return false;
        }
        if (!CommandHelper.checkArgsIsNull(args)) {
            return false;
        }
        if (StringUtils.isBlank(args[1])) {
            return false;
        }
        if (!("info".equals(args[1]) || "nodes".equals(args[1]))){
            return false;
        }
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String cmd = args[1];
        Result<?> result;
        if("info".equals(cmd)){
            result = networkProvider.getInfo();
            if (result.isFailed()) {
                return CommandResult.getFailed(result);
            }
            return CommandResult.getSuccess(result);
        }else{
            result = networkProvider.getNodes();
            if (result.isFailed()) {
                return CommandResult.getFailed(result);
            }
            return CommandResult.getResult(CommandResult.dataTransformList(result));
        }

    }
}
