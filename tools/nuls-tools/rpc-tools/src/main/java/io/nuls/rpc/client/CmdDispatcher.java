package io.nuls.rpc.client;

import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.message.*;
import io.nuls.rpc.server.CmdHandler;
import io.nuls.rpc.server.ServerRuntime;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.tools.thread.TimeService;

import java.io.IOException;
import java.util.Map;

/**
 * Command dispatcher
 * All commands should be invoked through this class
 *
 * @author tangyi
 * @date 2018/11/5
 * @description
 */
public class CmdDispatcher {

    /**
     * 与核心模块（Manager）握手
     * Shake hands with the core module (Manager)
     */
    public static boolean handshakeKernel() throws Exception {
        Message message = CmdHandler.basicMessage(Constants.nextSequence(), MessageType.NegotiateConnection);
        message.setMessageData(CmdHandler.defaultNegotiateConnection());

        WsClient wsClient = ClientRuntime.getWsClient(Constants.kernelUrl);
        if (wsClient == null) {
            throw new Exception("Kernel not available");
        }
        Log.info("NegotiateConnection:" + JSONUtils.obj2json(message));
        wsClient.send(JSONUtils.obj2json(message));

        /*
        是否收到正确的握手确认
        Whether received the correct handshake confirmation?
         */
        return getNegotiateConnectionResponse();
    }

    /**
     * 同步本地模块与核心模块（Manager）
     * 1. 发送本地信息给Manager
     * 2. 获取本地所依赖的角色的连接信息
     * Synchronize Local Module and Core Module (Manager)
     * 1. Send local information to Manager
     * 2. Get connection information for locally dependent roles
     */
    public static void syncKernel() throws Exception {
        String messageId = Constants.nextSequence();
        Message message = CmdHandler.basicMessage(messageId, MessageType.Request);
        Request request = ClientRuntime.defaultRequest();
        request.getRequestMethods().put("registerAPI", ServerRuntime.local);
        message.setMessageData(request);

        WsClient wsClient = ClientRuntime.getWsClient(Constants.kernelUrl);
        if (wsClient == null) {
            throw new Exception("Kernel not available");
        }
        wsClient.send(JSONUtils.obj2json(message));

        Response response = getResponse(messageId);
        Log.info("APIMethods from kernel:" + JSONUtils.obj2json(response));
        Map responseData = (Map) response.getResponseData();
        Map methodMap = (Map) responseData.get("registerAPI");
        Map dependMap = (Map) methodMap.get("Dependencies");
        for (Object key : dependMap.keySet()) {
            ClientRuntime.roleMap.put(key.toString(), (Map) dependMap.get(key));
        }
    }


    /**
     * 发送Request，并等待Response，如果等待超过1分钟，则抛出超时异常
     * Send Request and wait for Response, and throw a timeout exception if the waiting time more than one minute
     */
    public static Response requestAndResponse(String role, String cmd, Map params) throws Exception {
        String messageId = request(role, cmd, params, Constants.booleanString(false), "0");
        return getResponse(messageId);
    }

    /**
     * 发送Request，并根据返回结果自动调用本地方法
     * 返回值为messageId，用以取消订阅
     * Send the Request and automatically call the local method based on the return result
     * The return value is messageId, used to unsubscribe
     */
    public static String requestAndInvoke(String role, String cmd, Map params, String subscriptionPeriod, Class clazz, String invokeMethod) throws Exception {
        if (Integer.parseInt(subscriptionPeriod) <= 0) {
            throw new Exception("subscriptionPeriod must great than 0");
        }
        String messageId = request(role, cmd, params, Constants.booleanString(false), subscriptionPeriod);
        ClientRuntime.INVOKE_MAP.put(messageId, new Object[]{clazz, invokeMethod});
        return messageId;
    }

    /**
     * 与requestAndInvoke类似，但是发送之后必须接收到一个Ack作为确认
     * Similar to requestAndInvoke, but after sending, an Ack must be received as an acknowledgement
     */
    public static String requestAndInvokeWithAck(String role, String cmd, Map params, String subscriptionPeriod, Class clazz, String invokeMethod) throws Exception {
        String messageId = request(role, cmd, params, Constants.booleanString(true), subscriptionPeriod);
        ClientRuntime.INVOKE_MAP.put(messageId, new Object[]{clazz, invokeMethod});
        return getAck(messageId) ? messageId : null;
    }

    /**
     * 发送Request
     * Send Request
     */
    private static String request(String role, String cmd, Map params, String ack, String subscriptionPeriod) throws Exception {
        String messageId = Constants.nextSequence();
        Message message = CmdHandler.basicMessage(messageId, MessageType.Request);
        Request request = ClientRuntime.defaultRequest();
        request.setRequestAck(ack);
        request.setSubscriptionPeriod(subscriptionPeriod);
        request.getRequestMethods().put(cmd, params);
        message.setMessageData(request);

        /*
        从roleMap获取命令需发送到的地址
        Get the url from roleMap which the command needs to be sent to
         */
        String url = ClientRuntime.getRemoteUri(role);
        if (url == null) {
            return "-1";
        }
        WsClient wsClient = ClientRuntime.getWsClient(url);
        Log.info("SendRequest to " + wsClient.getRemoteSocketAddress().getHostString() + ":" + wsClient.getRemoteSocketAddress().getPort() + "->" + JSONUtils.obj2json(message));
        wsClient.send(JSONUtils.obj2json(message));

        if (Integer.parseInt(subscriptionPeriod) > 0) {
            /*
            如果是需要重复发送的消息（订阅消息），记录messageId与客户端的对应关系，用于取消订阅
            If it is a message (subscription message) that needs to be sent repeatedly, record the relationship between the messageId and the WsClient
             */
            ClientRuntime.msgIdKeyWsClientMap.put(messageId, wsClient);
        }

        return messageId;
    }


    /**
     * 取消订阅
     * Unsubscribe
     */
    public static void unsubscribe(String messageId) throws Exception {
        Message message = CmdHandler.basicMessage(Constants.nextSequence(), MessageType.Unsubscribe);
        Unsubscribe unsubscribe = new Unsubscribe();
        unsubscribe.setUnsubscribeMethods(new String[]{messageId});
        message.setMessageData(unsubscribe);

        /*
        根据messageId获取WsClient，发送取消订阅命令，然后移除本地信息
        Get the WsClient according to messageId, send the unsubscribe command, and then remove the local information
         */
        WsClient wsClient = ClientRuntime.msgIdKeyWsClientMap.get(messageId);
        if (wsClient != null) {
            wsClient.send(JSONUtils.obj2json(message));
            ClientRuntime.INVOKE_MAP.remove(messageId);
        }
    }

    /**
     * 是否握手成功
     * Whether shake hands successfully?
     */
    private static boolean getNegotiateConnectionResponse() throws InterruptedException, IOException {

        long timeMillis = System.currentTimeMillis();
        while (System.currentTimeMillis() - timeMillis <= Constants.TIMEOUT_TIMEMILLIS) {
            /*
            获取队列中的第一个对象，如果是空，舍弃
            Get the first item of the queue, If it is an empty object, discard
             */
            Message message = ClientRuntime.firstItemInServerResponseQueue();
            if (message == null) {
                continue;
            }

            /*
            消息类型应该是NegotiateConnectionResponse，如果不是，放回队列等待其他线程处理
            Message type should be "NegotiateConnectionResponse". If not, add back to the queue and wait for other threads to process
             */
            if (MessageType.NegotiateConnectionResponse.name().equals(message.getMessageType())) {
                Log.info("NegotiateConnectionResponse:" + JSONUtils.obj2json(message));
                return true;
            } else {
                ClientRuntime.SERVER_MESSAGE_QUEUE.add(message);
            }

            Thread.sleep(Constants.INTERVAL_TIMEMILLIS);
        }

        /*
        Timeout Error
         */
        return false;
    }

    /**
     * 根据messageId获取Response
     * Get response by messageId
     */
    private static Response getResponse(String messageId) throws InterruptedException, IOException {
        if (Integer.parseInt(messageId) < 0) {
            return ServerRuntime.newResponse(messageId, Constants.booleanString(false), Constants.CMD_NOT_FOUND);
        }

        long timeMillis = System.currentTimeMillis();
        while (System.currentTimeMillis() - timeMillis <= Constants.TIMEOUT_TIMEMILLIS) {
            /*
            获取队列中的第一个对象，如果是空，舍弃
            Get the first item of the queue, If it is an empty object, discard
             */
            Message message = ClientRuntime.firstItemInServerResponseQueue();
            if (message == null) {
                continue;
            }

            /*
            消息类型应该是Response，如果不是，放回队列等待其他线程处理
            Message type should be "Response". If not, add back to the queue and wait for other threads to process
             */
            if (!MessageType.Response.name().equals(message.getMessageType())) {
                ClientRuntime.SERVER_MESSAGE_QUEUE.add(message);
                continue;
            }

            Response response = JSONUtils.map2pojo((Map) message.getMessageData(), Response.class);
            if (response.getRequestId().equals(messageId)) {
                /*
                messageId匹配，说明就是需要的结果，返回
                If messageId is the same, then the response is needed
                 */
                Log.info("Response:" + JSONUtils.obj2json(message));
                return response;
            } else {
                /*
                messageId不匹配，放回队列等待其他线程处理
                Add back to the queue and wait for other threads to process
                 */
                ClientRuntime.SERVER_MESSAGE_QUEUE.add(message);
            }

            Thread.sleep(Constants.INTERVAL_TIMEMILLIS);
        }

        /*
        Timeout Error
         */
        return ServerRuntime.newResponse(messageId, Constants.booleanString(false), Constants.RESPONSE_TIMEOUT);
    }

    /**
     * 获取收到Request的确认
     * Get confirmation of receipt(Ack) of Request
     */
    private static boolean getAck(String messageId) throws InterruptedException, IOException {
        if (Integer.parseInt(messageId) < 0) {
            return false;
        }

        long timeMillis = TimeService.currentTimeMillis();
        while (TimeService.currentTimeMillis() - timeMillis <= Constants.TIMEOUT_TIMEMILLIS) {
            /*
            获取队列中的第一个对象，如果是空，舍弃
            Get the first item of the queue, If it is an empty object, discard
             */
            Message message = ClientRuntime.firstItemInServerResponseQueue();
            if (message == null) {
                continue;
            }

            /*
            消息类型应该是Ack，如果不是，放回队列等待其他线程处理
            Message type should be "Ack". If not, add back to the queue and wait for other threads to process
             */
            if (!MessageType.Ack.name().equals(message.getMessageType())) {
                ClientRuntime.SERVER_MESSAGE_QUEUE.add(message);
                continue;
            }

            Ack ack = JSONUtils.map2pojo((Map) message.getMessageData(), Ack.class);
            if (ack.getRequestId().equals(messageId)) {
                /*
                messageId匹配，说明就是需要的结果，返回
                If messageId is the same, then the ack is needed
                 */
                Log.info("Ack:" + JSONUtils.obj2json(ack));
                return true;
            } else {
                /*
                messageId不匹配，放回队列等待其他线程处理
                Add back to the queue and wait for other threads to process
                 */
                ClientRuntime.SERVER_MESSAGE_QUEUE.add(message);
            }

            Thread.sleep(Constants.INTERVAL_TIMEMILLIS);
        }

        /*
        Timeout Error
         */
        return false;
    }

}
