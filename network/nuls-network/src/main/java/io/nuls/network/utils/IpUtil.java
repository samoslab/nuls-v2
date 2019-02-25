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
package io.nuls.network.utils;

import io.nuls.network.constant.NetworkConstant;

import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.nuls.network.utils.LoggerUtil.Log;

/**
 * @author vivi
 */
public class IpUtil {
    private static final Pattern pattern = Pattern.compile("\\<dd class\\=\"fz24\">(.*?)\\<\\/dd>");

    private static final Set<String> ips = new HashSet<>();

    static {
        List<String> localIPs = getLocalIP();
        for (String ip : localIPs) {
            ips.add(ip);
        }
    }

    public static Set<String> getIps() {
        return ips;
    }

    private static ArrayList<String> getLocalIP() {
        ArrayList<String> iplist = new ArrayList<>();
        boolean loop = false;
        String bindip;
        Enumeration<?> network;
        List<NetworkInterface> netlist = new ArrayList<>();
        try {
            network = NetworkInterface.getNetworkInterfaces();
            while (network.hasMoreElements()) {
                loop = true;
                NetworkInterface ni = (NetworkInterface) network.nextElement();
                if (ni.isLoopback()) {
                    continue;
                }
                netlist.add(0, ni);
                InetAddress ip;
                for (NetworkInterface list : netlist) {
                    if (loop == false) {
                        break;
                    }
                    Enumeration<?> card = list.getInetAddresses();
                    while (card.hasMoreElements()) {
                        while (true) {
                            ip = null;
                            try {
                                ip = (InetAddress) card.nextElement();
                            } catch (Exception e) {

                            }
                            if (ip == null) {
                                break;
                            }
                            if (!ip.isLoopbackAddress()) {
                                if ("127.0.0.1".equalsIgnoreCase(ip.getHostAddress())) {
                                    continue;
                                }
                            }
                            if (ip instanceof Inet6Address) {
                                continue;
                            }
                            if (ip instanceof Inet4Address) {
                                bindip = ip.getHostAddress();
                                boolean addto = true;
                                for (int n = 0; n < iplist.size(); n++) {
                                    if (bindip.equals(iplist.get(n))) {
                                        addto = false;
                                        break;
                                    }
                                }
                                if (addto) {
                                    iplist.add(bindip);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            // skip
            Log.error("Get local IP error: " + e.getMessage());
        }

        return iplist;
    }

    public static boolean judgeLocalIsServer(String localIP, String remoteIP) {
        long local = ipToLong(localIP);
        long remote = ipToLong(remoteIP);
        if (local < remote) {
            return true;
        }
        return false;
    }

    public static long ipToLong(String ipAddress) {
        long result = 0;
        String[] ipAddressInArray = ipAddress.split("\\.");
        for (int i = 3; i >= 0; i--) {
            long ip = Long.parseLong(ipAddressInArray[3 - i]);
            //left shifting 24,16,8,0 and bitwise OR
            //1. 192 << 24
            //1. 168 << 16
            //1. 1   << 8
            //1. 2   << 0
            result |= ip << (i * 8);
        }
        return result;
    }

    public static String getNodeId(InetSocketAddress socketAddress) {
        if (socketAddress == null) {
            return null;
        }
        return socketAddress.getHostString() + NetworkConstant.COLON + socketAddress.getPort();
    }

    /**
     * 判断是否为合法IP * @return the ip
     */
    public static boolean isboolIp(String ipAddress) {
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("119.23.248.143");
        list.add("119.23.212.239");
        list.add("39.108.181.47");
        list.add("185.7�\u000B��<\u0011��");
        list.add("39.104.136.6");
        list.add("47.107.45.135");
        list.add("51.15.63.174");
        list.add("39.108.167.178");
        list.add("47.106.149.140");
        list.add("139.159.147.21");
        list.add("122.114.0.96");
        list.add("127.0.0.1");
        list.add("192.168.1.1");
        list.add("256.2.3.4");
        list.add("0.0.0.0");
        list.add("0.0.0");

        for (String str : list) {
            System.out.println(isboolIp(str));
        }
    }
}