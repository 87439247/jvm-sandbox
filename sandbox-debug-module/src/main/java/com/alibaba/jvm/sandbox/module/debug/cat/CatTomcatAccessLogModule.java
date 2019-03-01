package com.alibaba.jvm.sandbox.module.debug.cat;


import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import static com.alibaba.jvm.sandbox.api.ProcessController.returnImmediately;
import static com.alibaba.jvm.sandbox.module.debug.util.CatFinishUtil.finish;

@MetaInfServices(Module.class)
@Information(id = "cat-tomcat-access", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatTomcatAccessLogModule extends CatLogModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorTomcatLog();
    }


    /**
     * executeMethod(HostConfiguration hostconfig, HttpMethod method, HttpState state)
     */
    private void monitorTomcatLog() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.catalina.valves.AccessLogValve")
                .onBehavior("log")
                .withParameterTypes("java.io.CharArrayWriter")

                .onWatch(new AdviceListener() {
                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object param0 = advice.getParameterArray()[0];
                        if (param0 != null) {
                            String logMessage = param0.toString();
                            if (mayBeJsonObject(logMessage)) {
                                offerTomcatLog(logMessage);
                                returnImmediately(null);
                            }
                        }
                    }
                });
    }

    /**
     * 可能是json格式的,目前只支持json格式
     *
     * @param string 待校验的字符串
     * @return 是否是json
     */
    private boolean mayBeJsonObject(String string) {
        return string != null
                && (string.startsWith("{") && string.endsWith("}"));
    }


    @Override
    String getCatType() {
        return "tomcat-log";
    }
}