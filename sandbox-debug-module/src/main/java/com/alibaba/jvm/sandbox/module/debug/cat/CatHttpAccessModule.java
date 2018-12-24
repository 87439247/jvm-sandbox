package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.http.Http;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    private boolean booleanEnableDetail = false;

    @Http("/enable-detail")
    public void watch(final HttpServletRequest req,
                      final HttpServletResponse resp) throws IOException {
        try {
            String enableDetail = req.getParameter("enableDetail");
            if (enableDetail != null) {
                booleanEnableDetail = enableDetail.equals("yes") || enableDetail.equals("true");
            }
        } catch (Exception e) {
            return;
        }
    }


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

                    final String SEP = "-----------------------\n";

                    private void logRequestInfo(Object req, int responseCode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
                        StringBuilder builder = new StringBuilder("\r\n");
                        Enumeration headerNames = invokeMethod(req, "getHeaderNames");
                        while (headerNames.hasMoreElements()) {
                            String headerName = (String) headerNames.nextElement();
                            builder.append("\r\n").append("Header Name - ").append(headerName).append(", Value - ").append((String) invokeMethod(req, "getHeader", headerName));
                        }
                        builder.append(SEP);

                        Enumeration params = invokeMethod(req, "getParameterNames");
                        while (params.hasMoreElements()) {
                            String paramName = (String) params.nextElement();
                            builder.append("\r\n").append("Parameter Name - ").append(paramName).append(", Value - ").append((String) invokeMethod(req, "getParameter", paramName));
                        }
                        builder.append("\n");

                        //##########request info ################
                        builder.append("\n>>>>>>request\n")
                                .append((String) invokeMethod(req, "getMethod")).append(' ')
                                .append((String) invokeMethod(req, "getScheme")).append(' ')
                                .append((String) invokeMethod(req, "getRequestURI"));

                        // record event
                        Cat.logEvent(getCatType(), "CODE-" + responseCode, Message.SUCCESS, builder.toString());
                    }


                    @Override
                    public void before(Advice advice) throws Throwable {
                        final Object req = advice.getParameterArray()[0];
                        String uri = invokeMethod(req, "getRequestURI");
                        if (excludeURI(uri))
                            return;


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

                        advice.attach(t);
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
                        Transaction t = advice.attachment();
                        if (t != null) {
                            Object req = advice.getParameterArray()[0];
                            Object response = advice.getParameterArray()[1];
                            try {
                                int status = invokeMethod(response, "getStatus");

                                if (advice.getThrowable() != null) {
                                    t.setStatus(advice.getThrowable());
                                } else {
                                    if (status == 200) {
                                        if (booleanEnableDetail) {
                                            logRequestInfo(req, status);
                                        }
                                        t.setStatus(Transaction.SUCCESS);
                                    } else if (status >= 500) {
                                        logRequestInfo(req, status);
                                        t.setStatus(status + "");
                                    } else { //301 302 400 404 +
                                        logRequestInfo(req, status);
                                        t.setStatus(Transaction.SUCCESS);
                                    }
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


}
