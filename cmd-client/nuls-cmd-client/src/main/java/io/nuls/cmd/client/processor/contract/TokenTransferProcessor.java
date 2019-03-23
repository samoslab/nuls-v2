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

package io.nuls.cmd.client.processor.contract;

import io.nuls.api.provider.Result;
import io.nuls.api.provider.contract.facade.GetContractInfoReq;
import io.nuls.api.provider.contract.facade.TokenTransferReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandHelper;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.tools.model.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/9/22
 */
public class TokenTransferProcessor extends ContractBaseProcessor {


    private TokenTransferReq paramsData = new TokenTransferReq();

    @Override
    public String getCommand() {
        return "tokentransfer";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<address>           source address - Required")
                .newLine("\t<toaddress>         receiving address - Required")
                .newLine("\t<contractAddress>   contract address    -Required")
                .newLine("\t<gasLimit>          gas limit    -Required")
                .newLine("\t<price>             price (Unit: Na/Gas)    -Required")
                .newLine("\t<amount>            amount, you can have up to [decimals of the contract] valid digits after the decimal point - Required")
                .newLine("\t[remark]            remark -not required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "tokentransfer <address> <toAddress> <contractAddress> <gasLimit> <price> <amount> [remark] --token transfer";
    }

    @Override
    public boolean argsValidate(String[] args) {
        boolean result;
        do {
            int length = args.length;
            if (length != 7 && length != 8) {
                result = false;
                break;
            }
            if (!CommandHelper.checkArgsIsNull(args)) {
                result = false;
                break;
            }

            // gasLimit
            if (!StringUtils.isNumeric(args[4])) {
                result = false;
                break;
            }
            // price
            if (!StringUtils.isNumeric(args[5])) {
                result = false;
                break;
            }
            // amount
            if (!StringUtils.isNumberGtZero(args[6])) {
                result = false;
                break;
            }
            TokenTransferReq form = getTokenTransferForm(args);
            if(null == form){
                result = false;
                break;
            }
            paramsData = form;
            result = StringUtils.isNotBlank(form.getToAddress());
            if (!result) {
                break;
            }
            result = true;
        } while (false);
        return result;
    }

    private TokenTransferReq getTokenTransferForm(String[] args) {
        TokenTransferReq transfer = null;
        try {
            transfer = new TokenTransferReq();
            transfer.setAddress(args[1].trim());
            transfer.setToAddress(args[2].trim());
            transfer.setContractAddress(args[3].trim());
            transfer.setGasLimit(Long.valueOf(args[4].trim()));
            transfer.setPrice(Long.valueOf(args[5].trim()));
            transfer.setAmount(args[6].trim());
            if(args.length == 8) {
                transfer.setRemark(args[7].trim());
            }
            return transfer;
        } catch (Exception e) {
            e.fillInStackTrace();
            return null;
        }
    }

    @Override
    public CommandResult execute(String[] args) {
        TokenTransferReq form = paramsData;
        if (null == form) {
            form = getTokenTransferForm(args);
        }
        String address = form.getAddress();
        String password = CommandHelper.getPwd("Please enter your account password");
        String contractAddress = form.getContractAddress();
        String url = "/contract/" + contractAddress;
        Result<Map> contract = contractProvider.getContractInfo(new GetContractInfoReq(contractAddress));
        if (contract.isFailed()) {
            return CommandResult.getFailed(contract);
        }
        Boolean isNrc20 = (Boolean) contract.getData().get("isNrc20");
        if(!isNrc20) {
            return CommandResult.getFailed("Non-NRC20 contract, can not transfer token.");
        }
        Integer decimals = (Integer) contract.getData().get("decimals");
        BigDecimal amountBigD = new BigDecimal(form.getAmount()).multiply(BigDecimal.TEN.pow(decimals));
        try {
            BigInteger amountBigI = amountBigD.toBigIntegerExact();
            form.setAmount(amountBigI.toString());
        } catch(Exception e) {
            return CommandResult.getFailed("Illegal amount, you can have up to " + decimals + " valid digits after the decimal point.");
        }
        form.setPassword(password);
        Result<String> result = contractProvider.tokenTransfer(form);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getResult(result);
    }
}
