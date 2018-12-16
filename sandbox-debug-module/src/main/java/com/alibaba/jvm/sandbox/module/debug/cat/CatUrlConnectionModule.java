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
import sun.net.www.MessageHeader;
import sun.net.www.protocol.http.HttpURLConnection;

import javax.annotation.Resource;

import java.net.URL;
import java.util.Map;

import static com.alibaba.jvm.sandbox.module.debug.util.CatFinishUtil.finish;
import static com.alibaba.jvm.sandbox.module.debug.util.FieldUtils.invokeField;
import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;
import static com.alibaba.jvm.sandbox.module.debug.util.UrlUtils.rebuildPath;

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
     * com.sun.net.ssl.internal.www.protocol.https.HttpsURLConnectionOldImpl
     * sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection
     * sun.plugin.net.protocol.jar.CachedJarURLConnection
     * com.sun.net.ssl.internal.www.protocol.https.DelegateHttpsURLConnection
     * com.sun.deploy.net.protocol.javascript.JavaScriptURLConnection
     * com.sun.jnlp.JNLPCachedJarURLConnection
     * sun.net.www.protocol.mailto.MailToURLConnection
     * com.sun.webkit.network.DirectoryURLConnection
     * com.sun.webkit.network.data.DataURLConnection
     * java.net.HttpURLConnection
     * com.sun.net.ssl.HttpsURLConnection
     * sun.net.www.protocol.https.HttpsURLConnectionImpl
     * com.sun.webkit.network.about.AboutURLConnection
     * sun.net.www.protocol.jar.JarURLConnection
     * sun.net.www.URLConnection
     * sun.net.www.protocol.file.FileURLConnection
     * javax.net.ssl.HttpsURLConnection
     * com.sun.deploy.net.protocol.chrome.ChromeURLConnection
     * sun.net.www.protocol.http.HttpURLConnection
     * sun.net.www.protocol.ftp.FtpURLConnection
     * java.net.JarURLConnection
     * sun.net.www.protocol.https.DelegateHttpsURLConnection
     * com.sun.deploy.net.protocol.about.AboutURLConnection
     */
    private void monitorUrlConnection() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("sun.net.www.http.HttpClient")
                .includeBootstrap()
                .onBehavior("parseHTTP")

                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        HttpURLConnection urlConnection = (HttpURLConnection) advice.getParameterArray()[2];
                        URL url = urlConnection.getURL();
                        Transaction transaction = Cat.newTransaction(getCatType() + "-" + url.getHost(), rebuildPath(url.getPath()));
                        advice.attach(transaction);
                        MessageHeader headerMessage = (MessageHeader) advice.getParameterArray()[0];
                        CatContext context = new CatContext();
                        Cat.logRemoteCallClient(context, CatModule.CAT_DOMAIN);
                        for (Map.Entry<String, String> entry : context.properties.entrySet()) {
                            headerMessage.add(entry.getKey(), entry.getValue());
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
