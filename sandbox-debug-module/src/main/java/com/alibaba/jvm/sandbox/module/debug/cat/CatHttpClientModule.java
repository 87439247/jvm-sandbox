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
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import java.util.Map;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;
import static com.alibaba.jvm.sandbox.module.debug.util.UrlUtils.rebuildPath;

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
                .onClass("org.apache.http.impl.client.AbstractHttpClient")
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
                        Object httpRequest = advice.getParameterArray()[1];
                        String uri = invokeMethod(invokeMethod(httpRequest, "getRequestLine"), "getUri");
                        uri = rebuildPath(uri);
                        Transaction transaction = Cat.newTransaction(getCatType() + "-" + hostName, uri);
                        advice.attach(transaction);
                        CatContext context = new CatContext();
                        Cat.logRemoteCallClient(context, CatModule.CAT_DOMAIN);
                        for (Map.Entry<String, String> entry : context.properties.entrySet()) {
                            invokeMethod(httpRequest, "setHeader", entry.getKey(), entry.getValue());
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
                        try {
                            if (advice.getThrowable() != null) {
                                transaction.setStatus(advice.getThrowable());
                                Cat.logError(advice.getThrowable());
                            } else {
                                Object target = advice.getReturnObj();// getStatusLine
                                if (target != null) {
                                    Object statusLine = invokeMethod(target, "getStatusLine");
                                    if (statusLine != null) {
                                        int httpCode = invokeMethod(statusLine, "getStatusCode");
                                        if (httpCode != 200) {
                                            Cat.logError(transaction.getName() + ":" + httpCode, null);
                                        }
                                    }
                                }
                                transaction.setStatus(Message.SUCCESS);
                            }
                        } catch (Exception e) {
                            transaction.setStatus(e);
                            Cat.logError(e);
                        } finally {
                            transaction.complete();
                        }
                    }
                });
    }


    @Override
    String getCatType() {
        return "HttpClient";
    }
}
