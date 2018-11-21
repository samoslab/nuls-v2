/*
 * MIT License
 * Copyright (c) 2017-2018 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.thread;

import io.nuls.tools.log.Log;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * @author: Niels Wang
 */
public class ShutdownHook extends Thread {

    @Override
    public void run() {
        String root = this.getClass().getClassLoader().getResource("").getPath();
        try {
            root = URLDecoder.decode(root,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.error(e);
        }
//        String version = SyncVersionRunner.getInstance().getNewestVersion();
//        String newDirPath = root + "/temp/" + version;
//        File tempDir = new File(newDirPath);
//        if (tempDir.exists()) {
//            Log.error(1 + "");
//            FileUtil.deleteFolder(root + "/bin");
//            Log.error(2 + "");
//            FileUtil.deleteFolder(root + "/conf");
//            Log.error(3 + "");
//            FileUtil.deleteFolder(root + "/libs");
//            Log.error(4 + "");
//            FileUtil.decompress(newDirPath + "/bin.zip", newDirPath);
//            FileUtil.copyFolder(new File(newDirPath + "/bin"), new File(root + "/bin"));
//            Log.error(5 + "");
//            FileUtil.decompress(newDirPath + "/conf.zip", newDirPath);
//            String os = System.getProperty("os.name").toUpperCase();
//            FileUtil.copyFolder(new File(newDirPath + "/conf"), new File(root + "/conf"));
//            Log.error(6 + "");
//            FileUtil.copyFolder(new File(newDirPath + "/libs"), new File(root + "/libs"));

//        }
        String os = System.getProperty("os.name").toUpperCase();
        if (os.startsWith("WINDOWS")) {
            try {
                Runtime.getRuntime().exec("NULS-Wallet.exe");
            } catch (IOException e) {
                Log.error(e);
            }
        } else if (os.startsWith("MAC")) {
            try {
                Runtime.getRuntime().exec("open -a NULSWallet");
            } catch (IOException e) {
                Log.error(e);
            }
        } else {
            try {
                Runtime.getRuntime().exec("sh start.sh", null, new File(root + "/bin"));
            } catch (IOException e) {
                Log.error(e);
            }
        }

    }
}
