package io.nuls.rpc.server;

import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.message.Message;
import io.nuls.rpc.model.message.MessageType;
import io.nuls.rpc.model.message.Request;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.Map;

/**
 * @author tangyi
 * @date 2018/11/7
 * @description
 */
public class ServerProcessor implements Runnable {
    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {

        while (ServerRuntime.REQUEST_QUEUE.size() > 0) {

            Object[] objects = null;
            /*
            Get the first item of the queue.
            First in, first out
             */
            synchronized (ServerRuntime.REQUEST_QUEUE) {
                if (ServerRuntime.REQUEST_QUEUE.size() > 0) {
                    objects = ServerRuntime.REQUEST_QUEUE.get(0);
                    ServerRuntime.REQUEST_QUEUE.remove(0);
                }
            }

            try {
                if (objects == null) {
                    continue;
                }

                WebSocket webSocket = (WebSocket) objects[0];
                String msg = (String) objects[1];

                Message message;
                try {
                    message = JSONUtils.json2pojo(msg, Message.class);
                } catch (IOException e) {
                    Log.error(e);
                    continue;
                }

                MessageType messageType = MessageType.valueOf(message.getMessageType());
                switch (messageType) {
                    case NegotiateConnection:
                        CmdHandler.negotiateConnectionResponse(webSocket);
                        break;
                    case Request:
                        Request request = JSONUtils.map2pojo((Map) message.getMessageData(), Request.class);
                        if (Constants.booleanString(true).equals(request.getRequestAck())) {
                            CmdHandler.ack(webSocket, message.getMessageId());
                        }
                        if (CmdHandler.response(webSocket, message)) {
                            synchronized (ServerRuntime.REQUEST_QUEUE) {
                                /*
                                Whether an Ack needs to be sent or not, it is set to false after execution once.
                                That is to say, send Ack only once at most.
                                 */
                                request.setRequestAck(Constants.booleanString(false));
                                message.setMessageData(request);
                                ServerRuntime.REQUEST_QUEUE.add(new Object[]{webSocket, JSONUtils.obj2json(message)});
                            }
                        }
                        break;
                    case Unsubscribe:
                        CmdHandler.unsubscribe(webSocket, message);
                        break;
                    default:
                        break;
                }

                Thread.sleep(Constants.INTERVAL_TIMEMILLIS);
            } catch (Exception e) {
                Log.error(e);
            }
        }

    }
}
