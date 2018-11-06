/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2018 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *  *
 *
 */

package io.nuls.rpc.info;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Random;

/**
 * @author tangyi
 * @date 2018/10/17
 * @description
 */
public class HostInfo {

    /**
     * 根据网卡获得IP地址
     * 兼容Windows和Linux
     *
     * @return ip
     */
    public static String getIpAdd() {
        String ip = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                String name = networkInterface.getName();
                if (!name.contains("docker") && !name.contains("lo")) {
                    for (Enumeration<InetAddress> enumIpAddress = networkInterface.getInetAddresses(); enumIpAddress.hasMoreElements(); ) {
                        //获得IP
                        InetAddress inetAddress = enumIpAddress.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            String ipAddress = inetAddress.getHostAddress();
                            if (!ipAddress.contains("::") && !ipAddress.contains("0:0:") && !ipAddress.contains("fe80")) {
                                if (!"127.0.0.1".equals(ip) && ipAddress.length() <= 16) {
                                    ip = ipAddress;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ip;
    }

    public static String getIpAddLocally() {
        return "127.0.0.1";
    }

    public static int randomPort() {
        int min = 10000;
        int max = 20000;
        Random random = new Random();
        int port = random.nextInt(max) % (max - min + 1) + min;

        if (isLocalPortUsing(port)) {
            return randomPort();
        } else {
            return port;
        }
    }

    /**
     * 测试本机端口是否被使用
     *
     * @return boolean
     */
    private static boolean isLocalPortUsing(int port) {
        try {
            //建立一个Socket连接
            InetAddress address = InetAddress.getByName("127.0.0.1");
            new Socket(address, port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
