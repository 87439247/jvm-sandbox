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

import java.net.URI;
import java.net.URL;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;

@MetaInfServices(Module.class)
@Information(id = "cat-url-connection", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatUrlConnectionModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorUrlConnection();
    }


    /**
     * protected final CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException
     */
    private void monitorUrlConnection() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("java.net.URLConnection")
                .includeBootstrap()
                .includeSubClasses()
                .onBehavior("getInputStream")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object urlConnection = advice.getTarget();
                        Object url = invokeMethod(urlConnection, "getURI");
                        String host = invokeMethod(url, "getHost");
                        String path = invokeMethod(url, "getPath");
                        Transaction transaction = Cat.newTransaction(getCatType() + "-" + host, path);
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


    @Override
    String getCatType() {
        return "HttpClient";
    }
}
