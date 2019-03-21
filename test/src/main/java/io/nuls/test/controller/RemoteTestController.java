package io.nuls.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.test.cases.TestCaseIntf;
import io.nuls.test.cases.TestFailException;
import io.nuls.test.cases.account.GetAccountByAddressCase;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.core.config.ConfigSetting;
import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.parse.JSONUtils;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-20 11:08
 * @Description: 功能描述
 */
@Path("/remote/")
@Component
public class RemoteTestController {


    static Map<Class, Function<String, Object>> transfer = new HashMap<>();

    static {
        transfer.put(Integer.class, Integer::parseInt);
        transfer.put(int.class, Integer::parseInt);
        transfer.put(Long.class, Long::parseLong);
        transfer.put(long.class, Long::parseLong);
        transfer.put(Float.class, Float::parseFloat);
        transfer.put(float.class, Float::parseFloat);
        transfer.put(Double.class, Double::parseDouble);
        transfer.put(double.class, Double::parseDouble);
        transfer.put(Character.class, str -> str.charAt(0));
        transfer.put(char.class, str -> str.charAt(0));
        transfer.put(Short.class, Short::parseShort);
        transfer.put(short.class, Short::parseShort);
        transfer.put(Boolean.class, Boolean::parseBoolean);
        transfer.put(boolean.class, Boolean::parseBoolean);
        transfer.put(Byte.class, Byte::parseByte);
        transfer.put(byte.class, Byte::parseByte);
        transfer.put(String.class, str -> str);

    }

    @Path("/call")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public <T> RemoteResult doTest(RemoteCaseReq req) {
        Class<? extends TestCaseIntf> caseClass = req.getCaseClass();
        Type type = caseClass.getGenericSuperclass();
        if(!(type instanceof ParameterizedType)){
            type = caseClass.getGenericInterfaces()[0];
        }
        Type[] params = ((ParameterizedType) type).getActualTypeArguments();
        Class<T> reponseClass = (Class) params[1];
        TestCaseIntf tc = SpringLiteContext.getBean(caseClass);
        try {
            Object param;
            if(ConfigSetting.isPrimitive(reponseClass)){
                param = transfer.get(reponseClass).apply(req.getParam());
            }else{
                param = req.getParam() == null ? null : JSONUtils.json2pojo(req.getParam(), reponseClass);
            }
            return new RemoteResult(tc.check(param, 0));
        } catch (TestFailException e) {
            return new RemoteResult("10002",e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return new RemoteResult("10002","系统异常："+e.getMessage());
        }
    }

}
