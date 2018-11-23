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

package io.nuls.rpc.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.info.RuntimeInfo;
import io.nuls.rpc.model.CmdDetail;
import io.nuls.rpc.model.message.*;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.data.DateUtils;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.tools.thread.TimeService;
import org.java_websocket.WebSocket;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Call the correct method based on request information
 *
 * @author tangyi
 * @date 2018/10/30
 * @description
 */
public class CmdHandler {

    /**
     * For Request
     * Call local cmd.
     * 1. If the interface is injected via @Autowired, the injected object is used
     * 2. If the interface has no special annotations, construct a new object by reflection
     */
    private static Response invoke(String invokeClass, String invokeMethod, Map params) throws Exception {

        Class clz = Class.forName(invokeClass);
        @SuppressWarnings("unchecked") Method method = clz.getDeclaredMethod(invokeMethod, Map.class);

        BaseCmd cmd;
        if (SpringLiteContext.getBeanByClass(invokeClass) == null) {
            @SuppressWarnings("unchecked") Constructor constructor = clz.getConstructor();
            cmd = (BaseCmd) constructor.newInstance();
        } else {
            cmd = (BaseCmd) SpringLiteContext.getBeanByClass(invokeClass);
        }

        return (Response) method.invoke(cmd, params);
    }


    /**
     * Build basic message object
     */
    public static Message basicMessage(int messageId, MessageType messageType) {
        Message message = new Message();
        message.setMessageId(messageId);
        message.setMessageType(messageType.name());
        message.setTimestamp(TimeService.currentTimeMillis());
        message.setTimezone(DateUtils.getTimeZone());
        return message;
    }

    /**
     * For NegotiateConnection
     * Default NegotiateConnection object
     */
    public static NegotiateConnection defaultNegotiateConnection() {
        NegotiateConnection negotiateConnection = new NegotiateConnection();
        negotiateConnection.setCompressionAlgorithm("zlib");
        negotiateConnection.setCompressionRate(0);
        return negotiateConnection;
    }

    /**
     * For NegotiateConnectionResponse
     * Send NegotiateConnectionResponse
     */
    public static void negotiateConnectionResponse(WebSocket webSocket) throws JsonProcessingException {
        NegotiateConnectionResponse negotiateConnectionResponse = new NegotiateConnectionResponse();
        negotiateConnectionResponse.setNegotiationStatus(0);
        negotiateConnectionResponse.setNegotiationComment("Incompatible protocol version");

        Message rspMsg = basicMessage(RuntimeInfo.nextSequence(), MessageType.NegotiateConnectionResponse);
        rspMsg.setMessageData(negotiateConnectionResponse);
        webSocket.send(JSONUtils.obj2json(rspMsg));
    }

    /**
     * For Response
     */
    public static boolean response(WebSocket webSocket, Message message) throws Exception {
        int messageId = message.getMessageId();
        Request request = JSONUtils.map2pojo((Map) message.getMessageData(), Request.class);
        Map requestMethods = request.getRequestMethods();

        int subscriptionPeriod = request.getSubscriptionPeriod();

        boolean addBack = false;
        for (Object method : requestMethods.keySet()) {

            /*
            subscriptionPeriod > 0, means send response every time.
            subscriptionPeriod <= 0, means send response only once.
             */
            String key = webSocket.toString() + messageId + method;
            if (subscriptionPeriod > 0) {
                addBack = true;
                if (RuntimeInfo.cmdInvokeTime.containsKey(key)) {
                    if (RuntimeInfo.cmdInvokeTime.get(key) == Constants.UNSUBSCRIBE_TIMEMILLIS) {
                        RuntimeInfo.cmdInvokeTime.remove(key);
                        Log.info("Remove: " + key);
                        return false;
                    }

                    if (TimeService.currentTimeMillis() - RuntimeInfo.cmdInvokeTime.get(key) < subscriptionPeriod * 1000) {
                        continue;
                    }
                }
            }

            RuntimeInfo.cmdInvokeTime.put(key, TimeService.currentTimeMillis());

            long startTimemillis = TimeService.currentTimeMillis();

            Map params = (Map) requestMethods.get(method);
            CmdDetail cmdDetail = params == null || params.get(Constants.VERSION_KEY_STR) == null
                    ? RuntimeInfo.getLocalInvokeCmd((String) method)
                    : RuntimeInfo.getLocalInvokeCmd((String) method, Double.parseDouble(params.get(Constants.VERSION_KEY_STR).toString()));

            Response response = cmdDetail == null
                    ? defaultResponse(messageId, Constants.RESPONSE_STATUS_FAILED,
                    Constants.CMD_NOT_FOUND + ":" + method + "," + (params != null ? params.get(Constants.VERSION_KEY_STR) : ""))
                    : CmdHandler.invoke(cmdDetail.getInvokeClass(), cmdDetail.getInvokeMethod(), params);
            response.setResponseProcessingTime(TimeService.currentTimeMillis() - startTimemillis);
            response.setRequestId(messageId);

            Message rspMessage = basicMessage(RuntimeInfo.nextSequence(), MessageType.Response);
            rspMessage.setMessageData(response);
            Log.info("webSocket.send: " + JSONUtils.obj2json(rspMessage));
            try {
                webSocket.send(JSONUtils.obj2json(rspMessage));
            } catch (Exception e) {
                Log.error("Socket disconnect, remove!");
                addBack = false;
            }
        }
        return addBack;
    }

    /**
     * For Unsubscribe
     */
    public static void unsubscribe(WebSocket webSocket, Message message) throws Exception {
        Unsubscribe unsubscribe = JSONUtils.map2pojo((Map) message.getMessageData(), Unsubscribe.class);
        for (String str : unsubscribe.getUnsubscribeMethods()) {
            String key = webSocket.toString() + str;
            RuntimeInfo.cmdInvokeTime.put(key, Constants.UNSUBSCRIBE_TIMEMILLIS);
        }
    }

    public static Request defaultRequest() {
        Request request = new Request();
        request.setRequestAck(0);
        request.setSubscriptionEventCounter(0);
        request.setSubscriptionPeriod(0);
        request.setSubscriptionRange("");
        request.setResponseMaxSize(0);
        request.setRequestMethods(new HashMap<>(16));
        return request;
    }

    public static Response defaultResponse(int requestId, int status, String comment) {
        Response response = new Response();
        response.setRequestId(requestId);
        response.setResponseStatus(status);
        response.setResponseComment(comment);
        response.setResponseMaxSize(0);
        return response;
    }
}
