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
import io.netty.util.internal.ConcurrentSet;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

import java.util.concurrent.ConcurrentMap;

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
                        uri = rebuildPath(uri);
                        Transaction transaction = Cat.newTransaction(getCatType() + "-" + hostName, uri);
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

    private static ConcurrentSet<Character> words = new ConcurrentSet<>();

    private static ConcurrentSet<Character> numbers = new ConcurrentSet<>();

    static {
        String word = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/?&=_";
        String number = "0123456789";
        for (char w : word.toCharArray()) {
            words.add(w);
        }
        for (char n : number.toCharArray()) {
            numbers.add(n);
        }
    }

    private static String rebuildPath(String uri) {
        int start = uri.indexOf("://");
        if (start >= 0) {
            int pathStart = uri.indexOf("/", start + 3);
            uri = uri.substring(pathStart);
        }
        StringBuilder builder = new StringBuilder();
        boolean preIsNum = false;
        final String num = "(NUM)";
        for (char c : uri.toCharArray()) {

            if (words.contains(c)) {
                if (preIsNum) {
                    preIsNum = false;
                    builder.append(num);
                }
                builder.append(c);
            } else if (numbers.contains(c)) {
                preIsNum = true;
            } else {
                if (preIsNum) {
                    preIsNum = false;
                    builder.append(num);
                }
                final char sep = 'I';
                builder.append(sep);
            }
        }
        if (preIsNum) {
            builder.append(num);
        }
        return builder.toString();
    }


    @Override
    String getCatType() {
        return "HttpClient";
    }
}
