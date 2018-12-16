package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import java.util.Map;

import static com.alibaba.jvm.sandbox.module.debug.util.CatFinishUtil.finish;
import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;
import static com.alibaba.jvm.sandbox.module.debug.util.UrlUtils.rebuildPath;

@MetaInfServices(Module.class)
@Information(id = "cat-http-client3", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatHttpClient3Module extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorUrlConnection();
    }


    /**
     * executeMethod(HostConfiguration hostconfig, HttpMethod method, HttpState state)
     */
    private void monitorUrlConnection() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.commons.httpclient.HttpClient")
                .includeBootstrap()
                .onBehavior("executeMethod")
                .withParameterTypes("org.apache.commons.httpclient.HostConfiguration","org.apache.commons.httpclient.HttpMethod","org.apache.commons.httpclient.HttpState")

                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object httpMethod = advice.getParameterArray()[1];
                        Object uri = invokeMethod(httpMethod, "getURI");
                        String host = invokeMethod(uri,"getHost");
                        String path = invokeMethod(uri,"getPath");
                        Transaction transaction = Cat.newTransaction(getCatType() + "-" + host, rebuildPath(path));
                        advice.attach(transaction);
                        CatContext context = new CatContext();
                        Cat.logRemoteCallClient(context, CatModule.CAT_DOMAIN);
                        for (Map.Entry<String, String> entry : context.properties.entrySet()) {
                            invokeMethod(httpMethod, "setHeader", entry.getKey(), entry.getValue());
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


                });
    }


    @Override
    String getCatType() {
        return "HttpClient";
    }
}
