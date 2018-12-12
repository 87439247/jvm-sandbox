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
 * log4j v1版本 拦截 事件处理
 *
 * @author yuanyue@staff.hexun.com
 */
@MetaInfServices(Module.class)
@Information(id = "cat-log4j-v1", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatLog4jV1Module extends CatLogModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        buildingLog4jCategory();
    }


    /*
     * 拦截HttpServlet的服务请求入口
     */
    private void buildingLog4jCategory() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.log4j.Category")
                .onBehavior("callAppenders")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) {
                        // do nothing
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        try {
                            final int errorLevel = 40000;
                            Object event = advice.getParameterArray()[0];
                            int level = invokeMethod(invokeMethod(event, "getLevel"), "toInt");
                            if (level >= errorLevel) {
                                Throwable throwable = null;
                                Object throwProxy = invokeMethod(event, "getThrowableInformation");
                                if (throwProxy != null) {
                                    throwable = invokeMethod(throwProxy, "getThrowable");
                                }
                                String msg = invokeMethod(event, "getRenderedMessage");
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
