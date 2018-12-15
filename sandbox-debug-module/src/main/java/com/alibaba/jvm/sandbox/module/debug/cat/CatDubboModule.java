package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.BeanTraces;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;

@MetaInfServices(Module.class)
@Information(id = "cat-dubbo", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatDubboModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorDubboOld();
        monitorDubboNewVersion();
    }

    class Event {
        Event(Object url, String host, Transaction transaction) {
            this.url = url;
            this.host = host;
            this.transaction = transaction;
        }

        String host;
        Object url;
        Transaction transaction;
    }

    /**
     * public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
     */
    private void monitorDubboOld() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.alibaba.dubbo.monitor.support.MonitorFilter")
                .onBehavior("invoke")
                .withParameterTypes("com.alibaba.dubbo.rpc.Invoker", "com.alibaba.dubbo.rpc.Invocation")

                .onWatch(new AdviceListener() {
                    @Override
                    public void before(Advice advice) throws Throwable {
                        final Class rpcContextClass = ClassUtils.getClass("com.alibaba.dubbo.rpc.RpcContext");
                        CatDubboModule.this.before(advice, rpcContextClass);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        after(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        after(advice);
                    }
                });
    }

    /**
     * public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
     */
    private void monitorDubboNewVersion() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.dubbo.monitor.support.MonitorFilter")
                .onBehavior("invoke")
                .withParameterTypes("org.apache.dubbo.rpc.Invoker", "org.apache.dubbo.rpc.Invocation")
                .onWatch(new AdviceListener() {
                    @Override
                    public void before(Advice advice) throws Throwable {
                        final Class rpcContextClass = ClassUtils.getClass("org.apache.dubbo.rpc.RpcContext");
                        CatDubboModule.this.before(advice, rpcContextClass);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        after(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        after(advice);
                    }
                });
    }


    private void before(Advice advice, Class rpcContextClass) throws Exception {
        Object invoker = advice.getParameterArray()[0];
        Object invocation = advice.getParameterArray()[1];
        Object rpcContext = MethodUtils.invokeStaticMethod(rpcContextClass, "getContext");
        boolean isConsumer = invokeMethod(rpcContext, "isConsumerSide");
        Object requestURL = invokeMethod(invoker, "getUrl");

        String host = invokeMethod(requestURL, "getHost");
        String path = invokeMethod(requestURL, "getPath");
        String methodName = invokeMethod(invocation, "getMethodName");
        Transaction transaction = Cat.newTransaction(getCatType() + (isConsumer ? "-c-" : "-p-") + host, path + "." + methodName);
        advice.attach(new Event(requestURL, host, transaction));
    }

    private void after(Advice advice) {
        Event event = advice.attachment();
        if (event != null) {
            try {
                Object returnObj = advice.getReturnObj();
                Throwable throwable = advice.getThrowable();
                if (throwable == null) {
                    throwable = invokeMethod(returnObj, "getException");
                }
                if (throwable != null) {
                    String callUrl = invokeMethod(event.url, "toString");
                    Object[] args = invokeMethod(advice.getParameterArray()[1], "getArguments");
                    StringBuilderWriter writer = BeanTraces.printBeanTraceAscii(args);
                    Cat.logEvent(getCatType(), "url", "500", callUrl);
                    Cat.logEvent(getCatType(), "params", "500", writer.toString());
                    Cat.logError(throwable);
                    event.transaction.setStatus(throwable);
                } else {
                    event.transaction.setStatus(Message.SUCCESS);
                }
            } catch (Exception e) {
                event.transaction.setStatus(e);
                Cat.logError(e);
            } finally {
                event.transaction.complete();
            }
        }
    }

    @Override
    String getCatType() {
        return "dubbo";
    }
}
