package io.nuls.eventbus.rpc.processor;


import io.nuls.eventbus.EventBus;
import io.nuls.eventbus.constant.EbConstants;
import io.nuls.eventbus.runtime.EventBusRuntime;
import io.nuls.rpc.client.CmdDispatcher;
import io.nuls.rpc.client.runtime.ClientRuntime;
import io.nuls.rpc.info.Constants;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.log.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author naveen
 */
public class ClientSyncProcessor implements Runnable {

    @Override
    public void run() {
        while (true){
            try{
                Object[] objects = EventBusRuntime.firstObjArrInClientSyncQueue();
                if(null == objects){
                    Thread.sleep(200L);
                }else{
                    String moduleAbbr = (String)objects[0];
                    String cmd = (String)objects[1];
                    Log.info("Sync process started for Subscriber :"+moduleAbbr +" for the operation:"+cmd);
                    ConcurrentMap<String,String> connectionInfoMap = new ConcurrentHashMap<>(2);
                    //TODO get module connection info from manager module, send request to retrieve
                    Map<String,Object> params = new HashMap<>(1);
                    params.put(EbConstants.CMD_PARAM_ROLE,moduleAbbr);
                    Response response = CmdDispatcher.requestAndResponse(ModuleE.KE.abbr,"roleInfo",params);
                    if(!response.isSuccess()){
                        Log.error("Couldn't get connection info from kernel for the role:"+moduleAbbr);
                        continue;
                    }
                    Object data = response.getResponseData();
                    // TODO parse this response to get connection info
                    //added for test
                    connectionInfoMap.put(Constants.KEY_IP,"127.0.0.1");
                    connectionInfoMap.put(Constants.KEY_PORT,"8871");
                    switch (cmd){
                        case EbConstants.SUBSCRIBE:
                            ClientRuntime.ROLE_MAP.put(moduleAbbr,connectionInfoMap);
                            ClientRuntime.getWsClient(ClientRuntime.getRemoteUri(moduleAbbr));
                            break;
                        case EbConstants.UNSUBSCRIBE:
                            Set<String> roles = EventBus.getInstance().getAllSubscribers();
                            if(roles != null && !roles.contains(moduleAbbr)){
                                ClientRuntime.WS_CLIENT_MAP.remove(ClientRuntime.getRemoteUri(moduleAbbr));
                                ClientRuntime.ROLE_MAP.remove(moduleAbbr);
                            }
                            break;
                         default:
                    }
                    Thread.sleep(200L);
                }
            }catch (Exception e){
                Log.error(e);
            }
        }
    }
}
