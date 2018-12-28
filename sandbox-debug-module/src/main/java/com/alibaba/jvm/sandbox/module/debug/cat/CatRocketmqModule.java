package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import org.apache.commons.lang3.EnumUtils;
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
                .onBehavior("sendSelectImpl")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object message = advice.getParameterArray()[0];
                        String topic = invokeMethod(message, "getTopic");
                        Transaction t = Cat.newTransaction(getCatType() + "-P", topic);
                        advice.attach(t);
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
                .onClass("org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently")
                .includeSubClasses()
                .onBehavior("consumeMessage")
                .onClass("org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly")
                .includeSubClasses()
                .onBehavior("consumeMessage")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object msg = invokeMethod(advice.getParameterArray()[0], "get", 0);
                        if (msg != null) {
                            String msgTopic = invokeMethod(msg, "getTopic");
                            Transaction transaction = Cat.newTransaction(getCatType() + "-CONSUME", msgTopic);
                            advice.attach(transaction);
                        }
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
                        Transaction transaction = advice.attachment();
                        if (transaction == null) {
                            return;
                        }
                        try {
                            Enum consumeSuccess = getSuccess(advice.getReturnObj().getClass());
                            Object msg = invokeMethod(advice.getParameterArray()[0], "get", 0);
                            if (msg != null) {
                                String msgId = invokeMethod(msg, "getMsgId");
                                String msgTopic = invokeMethod(msg, "getTopic");
                                //记录metric
                                int size = invokeMethod(advice.getParameterArray()[0], "size");
                                Cat.logMetricForCount("rmq-consumer-" + msgTopic, size);

                                if (advice.isThrows()) { // when error
                                    //log error
                                    Cat.logError("consume-error(id=" + msgId + ",topic=" + msgTopic + ")", advice.getThrowable());
                                    // set transaction status
                                    transaction.setStatus(advice.getThrowable());
                                } else if (consumeSuccess != null && !consumeSuccess.equals(advice.getReturnObj())) { // when consume fail
                                    Cat.logError("consume-error(id=" + msgId + ",topic=" + msgTopic + ")", null);
                                    // set transaction status
                                    transaction.setStatus("500");
                                } else { // consume success
                                    // set transaction status
                                    transaction.setStatus(Message.SUCCESS);
                                }
                            }
                        } catch (Exception e) {
                            stLogger.error("error", e);
                            Cat.logError(e);
                        } finally {
                            transaction.complete();
                        }
                    }
                });
    }

    private Enum success_orderly = null;

    private Enum success_concurrently = null;

    private Enum getSuccess(Class consumeStatusClass) {
        if (consumeStatusClass.getSimpleName().equals("ConsumeConcurrentlyStatus")) {
            if (success_concurrently == null) {
                success_concurrently = EnumUtils.getEnum(consumeStatusClass, "CONSUME_SUCCESS");
            }
            return success_concurrently;
        } else {
            if (success_orderly == null) {
                success_orderly = EnumUtils.getEnum(consumeStatusClass, "SUCCESS");
            }
        }
        return null;
    }

    @Override
    String getCatType() {
        return "ROCKETMQ";
    }
}
