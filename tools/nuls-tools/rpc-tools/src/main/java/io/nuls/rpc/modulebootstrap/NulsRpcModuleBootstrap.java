package io.nuls.rpc.modulebootstrap;

import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.log.Log;
import io.nuls.tools.thread.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: zhoulijun
 * @Time: 2019-02-28 14:27
 * @Description: 功能描述
 */
@Slf4j
public class NulsRpcModuleBootstrap {

    private static final String DEFAULT_SCAN_PACKAGE = "io.nuls" ;

    public static void main(String[] args) {
        NulsRpcModuleBootstrap.run(args);
    }

    public static void run(String[] args) {
        run(DEFAULT_SCAN_PACKAGE,args);
    }

    public static void run(String scanPackage,String[] args){
        SpringLiteContext.init(scanPackage,"io.nuls.rpc.modulebootstrap");
        RpcModule module;
        try {
            module = SpringLiteContext.getBean(RpcModule.class);
        }catch(NulsRuntimeException e){
            Log.error("未找到到RpcModule的实现类");
            return ;
        }
        ThreadUtils.createAndRunThread(module.moduleInfo().getName()+"-thread",()->{
            BufferedReader is_reader = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                try {
                    String cmd = is_reader.readLine();
                    if(cmd == null)break;
                    switch (cmd){
                        case "f":
                            System.out.println("模块的追随者：");
                            module.getFollowerList().entrySet().forEach(System.out::println);
                            break;
                        case "d":
                            System.out.println("依赖的模块列表");
                            Arrays.stream(module.getDependencies()).forEach(d->{
                                System.out.println(d.name + " is ready : " + module.isDependencieReady(d));
                            });
                            break;
                        case "s":
                            System.out.println("当前状态："+module.getState());
                            break;
                        default:
                            System.out.println("错误的输入,请输入f,d,s");
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        log.info("MODULE INFO : {}:{}",module.moduleInfo().name,module.moduleInfo().version);
        log.info("MODULE DEPENDENCIES:");
        Arrays.stream(module.getDependencies()).forEach(d->log.info("====>{}:{}",d.name,d.version));
        module.run(scanPackage,"ws://" + args[0]);
    }

}
