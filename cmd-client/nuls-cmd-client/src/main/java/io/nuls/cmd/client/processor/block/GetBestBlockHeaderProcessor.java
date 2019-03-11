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

package io.nuls.cmd.client.processor.block;


import io.nuls.api.provider.Result;
import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.block.BlockService;
import io.nuls.api.provider.block.facade.GetBlockHeaderByLastHeightReq;
import io.nuls.base.data.BlockHeader;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.tools.core.annotation.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Charlie
 */
@Component
@Slf4j
public class GetBestBlockHeaderProcessor implements CommandProcessor {

    BlockService blockService = ServiceManager.get(BlockService.class);

    @Override
    public String getCommand() {
        return "getbestblockheader";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription());
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "getbestblockheader --get the best block header";
    }

    @Override
    public boolean argsValidate(String[] args) {
        int length = args.length;
        if(length > 1) {
            return false;
        }
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        Result<BlockHeader> result = blockService.getBlockHeaderByLastHeight(new GetBlockHeaderByLastHeightReq());
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}
