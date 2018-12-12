package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;


/**
 * logback
 *
 * @author yuanyue@staff.hexun.com
 */
@MetaInfServices(Module.class)
@Information(id = "cat-logback", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatLogbackModule extends CatLogModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        buildingLogbackLogger();
    }

    /*
     * 拦截ch.qos.logback.classic.Logger.callAppenders
     * public static final int ERROR_INT = 40000;
     * public static final int WARN_INT = 30000;
     * public static final int INFO_INT = 20000;
     * public static final int DEBUG_INT = 10000;
     */
    private void buildingLogbackLogger() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("ch.qos.logback.classic.Logger")
                .onBehavior("callAppenders")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) {
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        try {
                            int errorLevel = 40000;
                            Object event = advice.getParameterArray()[0];
                            int level = invokeMethod(invokeMethod(event, "getLevel"), "toInt");
                            long timeStamp = invokeMethod(event, "getTimeStamp");
                            String msg = invokeMethod(event, "getFormattedMessage");
                            String loggerName = invokeMethod(event, "getLoggerName");
                            String threadName = invokeMethod(event, "getThreadName");
                            if (level < errorLevel) {
                                offer(timeStamp, msg, level, loggerName, threadName, null);
                            } else {
                                Throwable throwable = null;
                                Object throwProxy = invokeMethod(event, "getThrowableProxy");
                                if (throwProxy != null) {
                                    throwable = invokeMethod(throwProxy, "getThrowable");
                                }
                                offer(timeStamp, msg, level, loggerName, threadName, throwable);
                                Cat.logError("[ERROR] " + msg, throwable);
                            }
                        } catch (Exception ex) {
                            //黑洞
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        // do nothing
                    }
                });
    }


    @Override
    String getCatType() {
        return null;
    }
}
