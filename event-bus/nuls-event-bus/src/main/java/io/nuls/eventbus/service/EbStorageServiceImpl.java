package io.nuls.eventbus.service;

import io.nuls.db.service.RocksDBService;
import io.nuls.eventbus.EventBus;
import io.nuls.eventbus.constant.EbConstants;
import io.nuls.eventbus.model.Topic;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.data.CollectionUtils;
import io.nuls.tools.data.ObjectUtils;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.log.Log;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author naveen
 */
@Component
public class EbStorageServiceImpl implements EbStorageService {

    @Override
    public void init() {
        try{
            RocksDBService.init("../../eventbus");
            if(!RocksDBService.existTable(EbConstants.TB_EB_TOPIC)){
                RocksDBService.createTable(EbConstants.TB_EB_TOPIC);
            }
        }catch (Exception e){
            Log.error("RocksDb init failed");
        }
    }

    @Override
    public ConcurrentMap<String,Topic> loadTopics() {
        ConcurrentMap<String,Topic> topicMap = new ConcurrentHashMap();
        try{
            List<byte[]> keys = RocksDBService.keyList(EbConstants.TB_EB_TOPIC);
            if(!keys.isEmpty()){
                Map<byte[],byte[]> map = RocksDBService.multiGet(EbConstants.TB_EB_TOPIC,keys);
                Set<Map.Entry<byte[],byte[]>> entrySet = map.entrySet();
                for(Map.Entry<byte[],byte[]> entry : entrySet){
                    topicMap.put(new String(entry.getKey()),ObjectUtils.bytesToObject(entry.getValue()));
                }
            }
        }catch (Exception e){
           Log.error("Error while loading Topics from DB");
        }
        return topicMap;
    }

    @Override
    public void putTopic(Topic topic) {
        try{
            if(null != topic && StringUtils.isNotBlank(topic.getTopicId())){
                RocksDBService.put(EbConstants.TB_EB_TOPIC,topic.getTopicId().getBytes(),ObjectUtils.objectToBytes(topic));
            }
        }catch (Exception e){
            Log.error("Topic save failed :"+e.getMessage());
        }
    }

    @Override
    public Topic getTopic(byte[] key) {
        return ObjectUtils.bytesToObject(RocksDBService.get(EbConstants.TB_EB_TOPIC,key));
    }
}
