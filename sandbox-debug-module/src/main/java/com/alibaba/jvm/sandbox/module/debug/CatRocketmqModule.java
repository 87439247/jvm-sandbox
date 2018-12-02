package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;

@MetaInfServices(Module.class)
@Information(id = "cat-rocketmq", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatRocketmqModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorRocketmqProducerContext();
        monitorRocketmqConsumerContext();
    }

    private void monitorRocketmqProducerContext() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl")
                .includeSubClasses()
                .onBehavior("sendDefaultImpl")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object message = advice.getParameterArray()[0];
                        String topic = invokeMethod(message, "getTopic");
                        Transaction t = Cat.newTransaction(getCatType() + "-P", topic);
                        advice.attach(t, topic);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        finish(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        finish(advice);
                    }

                    private void finish(Advice advice) {
                        Transaction t = advice.attachment();
                        if (t != null) {
                            try {
                                Cat.logMetricForCount(getCatType() + "-P-" + t.getName());
                                if (advice.getThrowable() != null) {
                                    t.setStatus(advice.getThrowable());
                                    Cat.logError(advice.getThrowable());
                                } else {
                                    t.setStatus(Message.SUCCESS);
                                }
                            } catch (Exception e) {
                                t.setStatus(e);
                                Cat.logError(e);
                            } finally {
                                t.complete();
                            }
                        }
                    }
                });
    }

    private void monitorRocketmqConsumerContext() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently").includeSubClasses()
                .onBehavior("consumeMessage")
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
                            Class returnClass = advice.getReturnObj().getClass();
                            Object msg = invokeMethod(advice.getParameterArray()[0], "get", 0);
                            Enum consumeSuccess = Enum.valueOf(returnClass, "CONSUME_SUCCESS");
                            String msgId = invokeMethod(msg, "getMsgId");
                            String msgTopic = invokeMethod(msg, "getTopic");
                            if (!consumeSuccess.equals(advice.getReturnObj())) {
                                Cat.logError("consume-error(id=" + msgId + ",topic=" + msgTopic + ")", null);
                            } else {
                                int size = invokeMethod(advice.getParameterArray()[0], "size");
                                Cat.logEvent(getCatType() + "-" + msgTopic, "consumed[" + msgId + "]");
                                Cat.logMetricForCount("rmq-consumer-" + msgTopic, size);
                            }
                        } catch (Exception e) {
                            //black hole
                        }
                    }
                });
    }

    @Override
    String getCatType() {
        return "ROCKETMQ";
    }
}
