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

package io.nuls.cmd.client;

import io.nuls.api.provider.Result;
import io.nuls.tools.constant.ErrorCode;
import io.nuls.tools.model.StringUtils;
import io.nuls.tools.parse.JSONUtils;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Charlie
 */
public class CommandHelper {

    public static boolean checkArgsIsNull(String... args) {
        for (String arg : args) {
            if (arg == null || arg.trim().length() == 0) {
                return false;
            }
        }
        return true;
    }

//    //
////    /**
////     * 获取用户的新密码 必填
////     * @return
////     */
//    public static String getNewPwd() {
//        System.out.print("Please enter the new password(8-20 characters, the combination of letters and numbers).\nEnter your new password:");
//        ConsoleReader reader = null;
//        try {
//            reader = new ConsoleReader();
//            String pwd = null;
//            do {
//                pwd = reader.readLine('*');
//                if (!StringUtils.validPassword(pwd)) {
//                    System.out.print("The password is invalid, (8-20 characters, the combination of letters and numbers) .\nReenter the new password: ");
//                }
//            } while (!StringUtils.validPassword(pwd));
//            return pwd;
//        } catch (IOException e) {
//            return null;
//        } finally {
//            try {
//                if (!reader.delete()) {
//                    reader.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }


    //    /**
//     * 确认新密码
//     * @param newPwd
//     */
    public static void confirmPwd(String newPwd) {
        System.out.print("Please confirm new password:");
        ConsoleReader reader = null;
        try {
            reader = new ConsoleReader();
            String confirmed = null;
            do {
                confirmed = reader.readLine('*');
                if (!newPwd.equals(confirmed)) {
                    System.out.print("Password confirmation doesn't match the password.\nConfirm new password: ");
                }
            } while (!newPwd.equals(confirmed));
        } catch (IOException e) {

        } finally {
            try {
                if (!reader.delete()) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //
//    /**
//     * 得到用户输入的密码,必须输入
//     * 提示信息为默认
//     * @return
//     */
    public static String getPwd() {
        return getPwd(null);
    }

    //    /**
//     * 得到用户输入的密码,必须输入
//     * @param prompt 提示信息
//     * @return
//     */
    public static String getPwd(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            prompt = "Please enter the password.\nEnter your password:";
        }
        System.out.print(prompt);
        ConsoleReader reader = null;
        try {
            reader = new ConsoleReader();
            String npwd = null;
            do {
                npwd = reader.readLine('*');
                if ("".equals(npwd)) {
                    System.out.print("The password is required.\nEnter your password:");
                }
            } while ("".equals(npwd));
            return npwd;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (!reader.delete()) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //
    /**
     * 得到用户输入的密码,允许不输入
     * @param prompt
     * @return
     */
    public static String getPwdOptional(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            prompt = "Please enter the password (password is between 8 and 20 inclusive of numbers and letters), " +
                    "If you do not want to set a password, return directly.\nEnter your password:";
        }
        System.out.print(prompt);
        ConsoleReader reader = null;
        try {
            reader = new ConsoleReader();
            String npwd = null;
            do {
                npwd = reader.readLine('*');
                if (!"".equals(npwd) && !validPassword(npwd)) {
                    System.out.print("Password invalid, password is between 8 and 20 inclusive of numbers and letters.\nEnter your password:");
                }
            } while (!"".equals(npwd) && !validPassword(npwd));
            return npwd;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (!reader.delete()) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *  Check the difficulty of the password
     *  length between 8 and 20, the combination of characters and numbers
     *
     * @return boolean
     */
    public static boolean validPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return false;
        }
        if (password.length() < 8 || password.length() > 20) {
            return false;
        }
        if (password.matches("(.*)[a-zA-z](.*)")
                && password.matches("(.*)\\d+(.*)")
                && !password.matches("(.*)\\s+(.*)")
                && !password.matches("(.*)[\u4e00-\u9fa5\u3000]+(.*)")) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 得到用户输入的密码,允许不输入
     * 提示信息为默认
     *
     * @return
     */
    public static String getPwdOptional() {
        return getPwdOptional(null);
    }




    public static String txTypeExplain(Integer type) {
        if (null == type) {
            return null;
        }
        switch (type) {
            case 1:
                return "coinbase";
            case 2:
                return "transfer";
            case 3:
                return "account_alias";
            case 4:
                return "register_agent";
            case 5:
                return "join_consensus";
            case 6:
                return "cancel_deposit";
            case 7:
                return "yellow_punish";
            case 8:
                return "red_punish";
            case 9:
                return "stop_agent";
            default:
                return type.toString();
        }
    }

    public static String consensusExplain(Integer status) {
        if (null == status) {
            return null;
        }
        switch (status) {
            case 0:
                return "unconsensus";
            case 1:
                return "consensus";

            default:
                return status.toString();
        }
    }

    public static String statusConfirmExplain(Integer status) {
        if (null == status) {
            return null;
        }
        switch (status) {
            case 0:
                return "confirm";
            case 1:
                return "unConfirm";

            default:
                return status.toString();
        }
    }


    //    /**
//     * 根据账户获取密码
//     * 1.如果账户有密码, 则让用户输入密码
//     * 2.如果账户没有设置密码, 直接返回
//     *
//     * @param address
//     * @param restFul
//     * @return RpcClientResult
//     */
//    public static Result<String> getPassword(String address) {
//        return getPassword(address, null);
//    }

    //    /**
//     * 根据账户获取密码
//     * 1.如果账户有密码, 则让用户输入密码
//     * 2.如果账户没有设置密码, 直接返回
//     *
//     * @param address
//     * @param restFul
//     * @param prompt 自定义提示
//     * @return RpcClientResult
//     */
//    public static Result getPassword(String address,  String prompt) {
//        if (StringUtils.isBlank(address)) {
//            return Result.fail(30002,ErrorCode.init("30002").getMsg());
//        }
//
//        RpcClientResult result = restFul.get("/account/encrypted/" + address, null);
//        if (result.isSuccess()) {
//            RpcClientResult rpcClientResult = new RpcClientResult();
//            rpcClientResult.setSuccess(true);
//            if (result.dataToBooleanValue()) {
//                String pwd = getPwd(prompt);
//                rpcClientResult.setData(pwd);
//            }
//            return rpcClientResult;
//        }
//        return result;
//
//    }


    public static String getArgsJson() {
        String prompt = "Please enter the arguments according to the arguments structure(eg. \"a\",2,[\"c\",4],\"\",\"e\" or \"'a',2,['c',4],'','e'\")," +
                "\nIf this method has no arguments(Refer to the command named \"getcontractinfo\" for the arguments structure of the method.), return directly.\nEnter the arguments:";
        System.out.print(prompt);
        ConsoleReader reader = null;
        try {
            reader = new ConsoleReader();
            String args = reader.readLine();
            if(StringUtils.isNotBlank(args)) {
                args = "[" + args + "]";
            }
            return args;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (!reader.delete()) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Object[] parseArgsJson(String argsJson) {
        if(StringUtils.isBlank(argsJson)) {
            return new Object[0];
        }
        try {
            List<Object> list = JSONUtils.json2pojo(argsJson, ArrayList.class);
            return list.toArray();
        } catch (Exception e) {
            e.fillInStackTrace();
            return null;
        }
    }
//
//    public static RpcClientResult getContractCallArgsJson() {
//        RpcClientResult rpcClientResult = new RpcClientResult();
//        rpcClientResult.setSuccess(true);
//        try {
//            Object[] argsObj;
//            // 再次交互输入构造参数
//            String argsJson = getArgsJson();
//            argsObj = parseArgsJson(argsJson);
//            rpcClientResult.setData(argsObj);
//        } catch (Exception e) {
//            e.printStackTrace();
//            rpcClientResult.setSuccess(false);
//        }
//        return rpcClientResult;
//    }
//
//
//    public static String tokenRecovery(String amount, Integer decimals) {
//        if(StringUtils.isBlank(amount) || decimals == null) {
//            return null;
//        }
//        return new BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals)).toPlainString();
//    }
}
