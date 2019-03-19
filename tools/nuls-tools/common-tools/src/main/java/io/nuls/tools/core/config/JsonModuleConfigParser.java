package io.nuls.tools.core.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.tools.io.IoUtils;
import io.nuls.tools.parse.JSONUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-13 17:51
 * @Description: 功能描述
 */
public class JsonModuleConfigParser implements ModuleConfigParser {
    @Override
    public String fileSuffix() {
        return "json";
    }

    @Override
    public Map<String, ConfigurationLoader.ConfigItem> parse(String configFile,InputStream inputStream) {
        try {
            String configJson = IoUtils.readRealPath(inputStream);
            Map<String,Object> data = JSONUtils.json2map(configJson);
            Map<String,ConfigurationLoader.ConfigItem> res = new HashMap<>(data.size());
            data.entrySet().forEach(entry->{
                try {
                    if(ConfigSetting.isPrimitive(entry.getValue().getClass())){
                        res.put(entry.getKey(),new ConfigurationLoader.ConfigItem(configFile,String.valueOf(entry.getValue())));
                    }else{
                        res.put(entry.getKey(),new ConfigurationLoader.ConfigItem(configFile,JSONUtils.obj2json(entry.getValue())));
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("json配置文件解析错误："+entry.getKey());
                }
            });
            return res;
        } catch (Exception e) {
            throw new RuntimeException("json配置文件解析错误");
        }
    }
}
