package io.nuls.rpc.modulebootstrap;

import io.nuls.tools.core.ioc.SpringLiteContext;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.tools.log.Log;
import io.nuls.tools.thread.ThreadUtils;

import java.io.*;
import java.util.Arrays;

/**
 * @Author: zhoulijun
 * @Time: 2019-02-28 14:27
 * @Description: 功能描述
 */
public class NulsRpcModuleBootstrap {

    private static final String DEFAULT_SCAN_PACKAGE = "io.nuls";

    private static boolean printLogoed = false;

    public static void main(String[] args) {
        NulsRpcModuleBootstrap.run(args);
    }

    public static void run(String[] args) {
        run(DEFAULT_SCAN_PACKAGE, args);
    }

    public static void run(String scanPackage, String[] args) {
        printLogo("/logo");
        Log.info("RUN MODULE:{}",System.getProperty("app.name"));
        SpringLiteContext.init(scanPackage, "io.nuls.rpc.modulebootstrap", "io.nuls.rpc.cmd");
        RpcModule module;
        try {
            module = SpringLiteContext.getBean(RpcModule.class);
            module.setMainArgs(args);
        } catch (NulsRuntimeException e) {
            Log.error("加载RpcModule的实现类失败");
            return;
        }

        String debug = "1";
        if (args.length > 1) {
            debug = args[1];
        }
        if ("1".equals(debug)) {
            ThreadUtils.createAndRunThread(module.moduleInfo().getName() + "-thread", () -> {
                BufferedReader is_reader = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    try {
                        String cmd = is_reader.readLine();
                        if (cmd == null) {
                            break;
                        }
                        switch (cmd) {
                            case "f":
                                System.out.println("模块的追随者：");
                                module.getFollowerList().entrySet().forEach(System.out::println);
                                break;
                            case "d":
                                System.out.println("依赖的模块列表");
                                module.getDependentces().forEach(d -> {
                                    System.out.println(d.name + " is ready : " + module.isDependencieReady(d));
                                });
                                break;
                            case "s":
                                System.out.println("当前状态：" + module.getState());
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
        }

//        System.out.println("MODULE INFO : {}:{}", module.moduleInfo().name, module.moduleInfo().version);
//        Log.info("MODULE DEPENDENCIES:");
//        Arrays.stream(module.declareDependent()).forEach(d -> Log.info("====>{}:{}", d.name, d.version));
        Log.info("main param list：");
        Arrays.stream(args).forEach(Log::info);
        module.run(scanPackage, args[0]);
    }

    public static void printLogo(String logoFile) {
        if(printLogoed) {
            return ;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Class.forName(NulsRpcModuleBootstrap.class.getName()).getResourceAsStream(logoFile)))){
            String line = reader.readLine();
            while(line != null){
                System.out.println(line);
                line = reader.readLine();
            }
            System.out.println("Module:" + System.getProperty("app.name"));
            System.out.println();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        printLogoed = true;
    }

}
