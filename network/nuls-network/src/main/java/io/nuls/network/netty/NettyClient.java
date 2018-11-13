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

package io.nuls.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.nuls.network.manager.handler.ClientChannelHandler;
import io.nuls.network.model.Node;
import io.nuls.tools.log.Log;
import io.nuls.tools.thread.TimeService;

import static io.nuls.network.constant.NetworkConstant.CONNETCI_TIME_OUT;

/**
 * NettyClient
 * @author lan
 * @date 2018/11/01
 *
 */
public class NettyClient {

    public static EventLoopGroup worker = new NioEventLoopGroup();

    Bootstrap boot;

    private SocketChannel socketChannel;

    private Node node;

//    private NodeManager nodeManager = NodeManager.getInstance();

    public NettyClient(Node node) {
        this.node = node;
        boot = new Bootstrap();

        AttributeKey<Node> key = null;
        synchronized (NettyClient.class) {
            if (AttributeKey.exists("node")) {
                key = AttributeKey.valueOf("node");
            } else {
                key = AttributeKey.newInstance("node");
            }
        }
        boot.attr(key, node);
        boot.group(worker)
                .channel(NioSocketChannel.class)
//                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.TCP_NODELAY, true)            //Send messages immediately
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_SNDBUF, 128 * 1024)
                .option(ChannelOption.SO_RCVBUF, 128 * 1024)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNETCI_TIME_OUT)
                .handler(new NulsChannelInitializer<>(new ClientChannelHandler()));
    }

    public void start() {
        try {
            ChannelFuture future = boot.connect(node.getIp(), node.getRemotePort()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        socketChannel = (SocketChannel) future.channel();
                    } else {
                        Log.error("Client connect to host error: " + future.cause() + ", remove node: " + node.getId());
                        node.setLastFailTime(TimeService.currentTimeMillis());
                        node.setFailCount(node.getFailCount()+1);
                        if(node.getChannel()!=null && node.getChannel().isActive()){
                            node.getChannel().close();
                        }
                        node.disConnectNodeChannel();
                        node.setCanConnect(true);
                    }
                }
            });
            future.channel().closeFuture().awaitUninterruptibly();
        } catch (Exception e) {
            //maybe time out or refused or something
            if (socketChannel != null) {
                socketChannel.close();
            }
            Log.error("Client start exception:" + e.getMessage() + ", remove node: " + node.getId());
//            nodeManager.removeNode(node.getId());
        }
    }

}
