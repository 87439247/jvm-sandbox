package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import java.net.URL;

import static com.alibaba.jvm.sandbox.module.debug.util.CatFinishUtil.finish;
import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;

@MetaInfServices(Module.class)
@Information(id = "cat-dubbo", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatDubboModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorDubbo();
    }


    /**
     *
     */
    private void monitorDubbo() {
        try {
            new EventWatchBuilder(moduleEventWatcher)
                    .onClass("org.apache.dubbo.monitor.support.MonitorFilter")
                    .onBehavior("invoke")

                    .onWatch(new AdviceListener() {

                        final Class rpcContextClass = ClassUtils.getClass("com.alibaba.dubbo.rpc.RpcContext");

                        @Override
                        public void before(Advice advice) throws Throwable {
                            Object invoker = advice.getParameterArray()[0];
                            Object rpcContext = MethodUtils.invokeStaticMethod(rpcContextClass, "getContext");
                            boolean isConsumer = invokeMethod(rpcContext, "isConsumerSide");
                            URL requestURL = invokeMethod(invoker, "getUrl");

                            final String hostName = requestURL.getHost();
                            Transaction transaction;
                            if (isConsumer) {
                                transaction = Cat.newTransaction(getCatType() + "-c-" + hostName, requestURL.getPath());
                            } else {
                                transaction = Cat.newTransaction(getCatType() + "-p-" + hostName, requestURL.getPath());
                            }
                            advice.attach(transaction);
                        }

                        @Override
                        public void afterReturning(Advice advice) {
                            finish(advice);
                        }

                        @Override
                        public void afterThrowing(Advice advice) {
                            finish(advice);
                        }
                    });
        } catch (ClassNotFoundException e) {
            stLogger.error("class not found ", e);
        }
    }

    @Override
    String getCatType() {
        return "dubbo";
    }
}
