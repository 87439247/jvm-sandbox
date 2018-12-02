package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;


/**
 * 基于HTTP-SERVLET(v2.4)规范的HTTP访问日志
 *
 * @author luanjia@taobao.com
 */
@MetaInfServices(Module.class)
@Information(id = "cat-logback", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatLogbackModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        buildingLogbackLogger();
    }


    /*
     * 拦截HttpServlet的服务请求入口
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
                            final int errorLevel = 40000;
                            Object event = advice.getParameterArray()[0];
                            int level = invokeMethod(invokeMethod(event, "getLevel"), "toInt");
                            if (level >= errorLevel) {
                                Throwable throwable = null;
                                Object throwProxy = invokeMethod(event, "getThrowableProxy");
                                if (throwProxy != null) {
                                    throwable = invokeMethod(throwProxy, "getThrowable");
                                }
                                String msg = invokeMethod(event, "getFormattedMessage");
                                Cat.logError("[ERROR] " + msg, throwable);
                            }
                        } catch (Exception ex) {
                            //黑洞
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        // no
                    }
                });
    }

    @Override
    String getCatType() {
        return null;
    }
}
