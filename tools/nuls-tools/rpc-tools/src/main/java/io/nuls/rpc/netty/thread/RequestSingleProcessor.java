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
package io.nuls.rpc.netty.thread;

import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.message.Request;
import io.nuls.rpc.netty.channel.ConnectData;
import io.nuls.rpc.netty.processor.RequestMessageProcessor;
import io.nuls.tools.log.Log;

/**
 * 处理客户端消息的线程
 * Threads handling client messages
 *
 * @author tag
 * @date 2019/2/25
 */
public class RequestSingleProcessor implements Runnable {
    private ConnectData connectData;

    public RequestSingleProcessor(ConnectData connectData){
        this.connectData = connectData;
    }

    /**
     * 发送只响应一次的消息
     * Send a message that responds only once
     */
    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (connectData.isConnected()) {
            try {
                /*
                获取队列中的第一个对象
                Get the first item of the queue
                 */
                if(!connectData.getRequestSingleQueue().isEmpty()){
                    Object[] objects = connectData.getRequestSingleQueue().poll();
                    if(objects != null && objects.length == 2){
                        String messageId = (String) objects[0];
                        Request request = (Request) objects[1];
                        /*
                        Request，调用本地方法
                        If it is Request, call the local method
                        */
                        RequestMessageProcessor.callCommandsWithPeriod(connectData.getChannel(), request.getRequestMethods(), messageId);
                    }
                }
                Thread.sleep(Constants.INTERVAL_TIMEMILLIS);
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }
}
