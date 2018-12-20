package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import com.dianping.cat.CatConstants;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.util.UrlParser;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;


/**
 * 基于HTTP-SERVLET(v2.4)规范的HTTP访问日志
 *
 * @author yuanyue@staff.hexun.com
 */
@MetaInfServices(Module.class)
@Information(id = "cat-http-access", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatHttpAccessModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;


    private static Set<String> excludeUrls = new HashSet<>();

    private static Set<String> excludePrefixes = new HashSet<>();

    static {
        excludeUrls.add("favicon.ico");
        excludePrefixes.add(".jpg");
        excludePrefixes.add(".jpeg");
        excludePrefixes.add(".png");
        excludePrefixes.add(".gif");
        excludePrefixes.add(".js");
        excludePrefixes.add(".css");
        excludePrefixes.add(".woff");
        excludePrefixes.add(".woff2");
    }

    @Override
    String getCatType() {
        return CatConstants.TYPE_URL;
    }


    /**
     * HTTP接入信息
     */
    class HttpAccess {
        Transaction transaction;
        long beginTimestamp;
    }


    @Override
    public void loadCompleted() {
        buildingHttpServletService();
    }

    /*
     * 拦截HttpServlet的服务请求入口  com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher class org.apache.catalina.servlets.DefaultServlet
     */
    private void buildingHttpServletService() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet")
                .onBehavior("service")
                .withParameterTypes(
                        "javax.servlet.http.HttpServletRequest",
                        "javax.servlet.http.HttpServletResponse"
                )


                .onClass("org.springframework.web.context.support.HttpRequestHandlerServlet")
                .onBehavior("service")
                .withParameterTypes(
                        "javax.servlet.http.HttpServletRequest",
                        "javax.servlet.http.HttpServletResponse"
                )


                .onClass("org.springframework.web.servlet.HttpServletBean").includeSubClasses()
                .onBehavior("service")
                .withParameterTypes(
                        "javax.servlet.http.HttpServletRequest",
                        "javax.servlet.http.HttpServletResponse"
                )
                .onWatch(new AdviceListener() {

                    /**
                     * 排除的uri
                     * @param uri uri
                     * @return 是否是排除的
                     */
                    private boolean excludeURI(String uri) {
                        try {
                            boolean exclude = excludeUrls.contains(uri);

                            if (!exclude) {
                                for (String prefix : excludePrefixes) {
                                    if (uri.endsWith(prefix)) {
                                        exclude = true;
                                        break;
                                    }
                                }
                            }
                            return exclude;
                        } catch (Exception e) {
                            return false;
                        }
                    }


                    private void logRequestInfo(Object req, int responseCode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
                        StringBuilder sb = new StringBuilder();

                        //######## header ##############
                        String clientIp = invokeMethod(req, "getHeader", "x-forwarded-for");
                        String remoteAddr = invokeMethod(req, "getRemoteAddr");
                        if (clientIp == null) {
                            clientIp = invokeMethod(req, "getHeader", "X-Forwarded-For");
                        }
                        if (clientIp == null) {
                            clientIp = remoteAddr;
                        }
                        sb.append(">>>>>>header\nIPS=").append(clientIp)
                                .append("\nVirtualIP=").append(remoteAddr)
                                .append("\nServer=").append((String) invokeMethod(req, "getServerName"))
                                .append("\nReferer=").append((String) invokeMethod(req, "getHeader", "referer"))
                                .append("\nAgent=").append((String) invokeMethod(req, "getHeader", "user-agent"));

                        //##########request info ################
                        sb.append("\n>>>>>>request\n")
                                .append((String) invokeMethod(req, "getMethod")).append(' ')
                                .append((String) invokeMethod(req, "getScheme")).append(' ')
                                .append((String) invokeMethod(req, "getRequestURI"));

                        String qs = invokeMethod(req, "getQueryString");

                        if (qs != null) {
                            sb.append('?').append(qs);
                        }

                        // record event
                        Cat.logEvent(getCatType(), "CODE-" + responseCode, Message.SUCCESS, sb.toString());
                    }


                    @Override
                    public void before(Advice advice) throws Throwable {
                        final Object req = advice.getParameterArray()[0];
                        String uri = invokeMethod(req, "getRequestURI");
                        if (excludeURI(uri))
                            return;


                        HttpAccess ha = new HttpAccess();
                        Transaction t = Cat.newTransaction(getCatType(), UrlParser.format(uri));
                        //###########cross ################
                        Enumeration<String> headerNames = invokeMethod(req, "getHeaderNames");
                        CatContext context = new CatContext();
                        while (headerNames.hasMoreElements()) {
                            String key = headerNames.nextElement();
                            String value = invokeMethod(req, "getHeader", key);
                            context.addProperty(key, value);
                        }
                        Cat.logRemoteCallServer(context);
                        //#################################


                        ha.transaction = t;
                        ha.beginTimestamp = System.currentTimeMillis();

                        advice.attach(ha);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        finishing(advice);
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        finishing(advice);
                    }

                    /**
                     * 判断是否请求对称结束
                     *
                     * @param advice 通知
                     */
                    private void finishing(Advice advice) {
                        HttpAccess ha = advice.attachment();
                        final Object req = advice.getParameterArray()[0];
                        final Object response = advice.getParameterArray()[1];
                        try {
                            int status = invokeMethod(response, "getStatus");
                            logRequestInfo(req, status);
                            if (advice.getThrowable() != null) {
                                ha.transaction.setStatus(advice.getThrowable());
                            } else {
                                if (status == 200) {
                                    ha.transaction.setStatus(Transaction.SUCCESS);
                                } else if (status >= 500) {
                                    ha.transaction.setStatus(status + "");
                                } else { //301 302 400 404 +
                                    ha.transaction.setStatus(Transaction.SUCCESS);
                                }
                            }
                        } catch (Exception e) {
                            ha.transaction.setStatus(e);
                            Cat.logError(e);
                        } finally {
                            ha.transaction.complete();
                        }
                    }

                });
    }


}
