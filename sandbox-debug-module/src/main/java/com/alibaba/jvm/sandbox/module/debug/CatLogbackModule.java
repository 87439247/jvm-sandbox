package com.alibaba.jvm.sandbox.module.debug;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import com.dianping.cat.logback.CatLogbackAppender;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;


/**
 * 基于HTTP-SERVLET(v2.4)规范的HTTP访问日志
 *
 * @author luanjia@taobao.com
 */
@MetaInfServices(Module.class)
@Information(id = "cat-logback", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatLogbackModule implements Module, LoadCompleted {

    private final Logger stLogger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
//        buildingHttpStatusFillBack();
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
                            Object event = advice.getParameterArray()[0];
                            int level = invokeMethod(invokeMethod(event, "getLevel"), "toInt");
                            if (level > 20000) {
                                Throwable throwable;
                                Object throwProxy = invokeMethod(event, "getThrowableProxy");
                                if (throwProxy != null) {
                                    throwable = invokeMethod(throwProxy, "getThrowable");
                                } else {
                                    throwable = new UnknownError("biz-error");
                                }
                                String msg = invokeMethod(event, "getFormattedMessage");
                                Cat.logError(msg, throwable);
                            }
                        } catch (Exception ex) {
                            //黑洞
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {

                    }
                });
    }

    static {
        Cat.initializeByDomainForce("cat111");
    }

    /*
     * 泛型转换方法调用
     * 底层使用apache common实现
     */
    private static <T> T invokeMethod(final Object object,
                                      final String methodName,
                                      final Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return (T) MethodUtils.invokeMethod(object, methodName, args);
    }

}
