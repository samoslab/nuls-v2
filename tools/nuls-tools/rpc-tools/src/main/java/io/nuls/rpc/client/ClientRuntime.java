package io.nuls.rpc.client;

import io.nuls.rpc.info.Constants;
import io.nuls.tools.log.Log;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author tangyi
 * @date 2018/11/23
 * @description
 */
public class ClientRuntime {

    public static final String ROLE_CM = "cm";

    /**
     * remote module information
     * key: module name/code
     */
    public static ConcurrentMap<String, Map> roleMap = new ConcurrentHashMap<>();

    /**
     * The response of the cmd invoked through RPC
     */
    public static final List<Map> CALLED_VALUE_QUEUE = Collections.synchronizedList(new ArrayList<>());

    /**
     * WsClient object that communicates with other modules
     * key: uri(ex: ws://127.0.0.1:8887)
     * value: WsClient
     */
    private static ConcurrentMap<String, WsClient> wsClientMap = new ConcurrentHashMap<>();

    /**
     * Get the WsClient object through the url
     */
    public static WsClient getWsClient(String uri) throws Exception {

        if (!wsClientMap.containsKey(uri)) {
            WsClient wsClient = new WsClient(uri);
            wsClient.connect();
            Thread.sleep(1000);
            if (wsClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
                wsClientMap.put(uri, wsClient);
            } else {
                Log.info("Failed to connect " + uri);
            }
        }
        return wsClientMap.get(uri);
    }

    /**
     * WsClient object that communicates with other modules
     * Used to unsubscribe
     * key: messageId
     * value: WsClient
     */
    public static ConcurrentMap<String, WsClient> msgIdKeyWsClientMap = new ConcurrentHashMap<>();

    /**
     * Get the url of the module that provides the cmd through the cmd
     * The resulting url may not be unique, returning all found
     */
    public static String getRemoteUri(String role) {
//        for (Map role : roleMap.values()) {
//            for (CmdDetail cmdDetail : moduleInfo.getRegisterApi().getApiMethods()) {
//                if (cmdDetail.getMethodName().equals(cmd)) {
//                    String address = (String) moduleInfo.getRegisterApi().getConnectionInformation().get(Constants.KEY_IP);
//                    int port = Integer.parseInt(moduleInfo.getRegisterApi().getConnectionInformation().get(Constants.KEY_PORT));
//                    return "ws://" + address + ":" + port;
//                }
//            }
//        }
//        return null;
        Map map = roleMap.get(role);

        return map != null
                ? "ws://" + map.get(Constants.KEY_IP) + ":" + map.get(Constants.KEY_PORT)
                : null;

    }
}
