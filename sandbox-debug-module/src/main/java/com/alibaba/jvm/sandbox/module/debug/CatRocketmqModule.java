package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.BeanTraces;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;

@MetaInfServices(Module.class)
@Information(id = "cat-rocketmq", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatRocketmqModule implements Module, LoadCompleted {

    private final Logger smLogger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorRocketmqProducerContext();
        monitorRocketmqConsumerContext();
    }

    private void monitorRocketmqProducerContext() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl").includeSubClasses()
                .onBehavior("sendDefaultImpl")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {

                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        finish(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        finish(advice);
                        if (advice.isThrows()) {
                            Cat.logError(advice.getThrowable());
                        }
                    }

                    private void finish(Advice advice) {
                        try {
                            Object message = advice.getParameterArray()[0];
                            String topic = invokeMethod(message, "getTopic");
                            String tags = invokeMethod(message, "getTags");
                            Cat.logMetricForCount("rmq-produce-" + topic + "-" + tags);
                        } catch (Exception e) {
                            //black hole
                        }
                    }
                });
    }

    private void monitorRocketmqConsumerContext() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly").includeSubClasses()
                .onBehavior("consumeMessage")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {

                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        finish(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        finish(advice);
                        if (advice.isThrows()) {
                            Cat.logError(advice.getThrowable());
                        }
                    }

                    private void finish(Advice advice) {
                        try {
                            Object target = advice.getTarget();
                        } catch (Exception e) {
                            //black hole
                        }
                    }
                });
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
