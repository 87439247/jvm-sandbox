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
@Information(id = "cat-httpclient", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatHttpClientModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorAbstractHttpClient();
    }


    /**
     * protected final CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException
     */
    private void monitorAbstractHttpClient() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.http.impl.client.AbstractHttpClient").includeSubClasses()
                .onBehavior("doExecute")
                .onClass("org.apache.http.impl.client.DefaultRequestDirector")
                .onBehavior("execute")
                .onClass("org.apache.http.impl.client.InternalHttpClient")
                .onBehavior("doExecute")
                .onClass("org.apache.http.impl.client.MinimalHttpClient")
                .onBehavior("doExecute")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        String hostName = invokeMethod(advice.getParameterArray()[0], "getHostName");
                        String uri = invokeMethod(invokeMethod(advice.getParameterArray()[1], "getRequestLine"), "getUri");

                        Transaction t0 = Cat.newTransaction(getCatType() + hostName, uri);
                        advice.attach(t0);
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
                        Transaction t0 = advice.attachment();
                        try {
                            if (advice.getThrowable() != null) {
                                t0.setStatus(advice.getThrowable());
                                Cat.logError(advice.getThrowable());
                            } else {
                                t0.setStatus(Message.SUCCESS);
                            }
                        } catch (Exception e) {
                            t0.setStatus(e);
                            Cat.logError(e);
                        } finally {
                            t0.complete();
                        }
                    }
                });
    }


    @Override
    String getCatType() {
        return "HttpClient";
    }
}
