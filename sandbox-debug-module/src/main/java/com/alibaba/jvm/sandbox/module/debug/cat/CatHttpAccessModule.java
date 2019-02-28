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
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
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
    public void watch(final Map<String, String> param, final PrintWriter writer) throws IOException {
        try {
            String enableDetail = getParameter(param, "enableDetail");
            if (enableDetail != null) {
                booleanEnableDetail = enableDetail.equals("yes") || enableDetail.equals("true");
                writer.print("enable http detail log ok");
            } else {
                booleanEnableDetail = false;
                writer.print("disabled http detail log [yes is enable]");
            }
        } catch (Exception e) {
            writer.print("error");
            stLogger.error("设置http详细信息错误", e);
        }
    }

    private static Set<String> excludeSuffix = new HashSet<>();

    static {
        excludeSuffix.add(".ico");
        excludeSuffix.add(".jpg");
        excludeSuffix.add(".txt");
        excludeSuffix.add(".jpeg");
        excludeSuffix.add(".png");
        excludeSuffix.add(".gif");
        excludeSuffix.add(".js");
        excludeSuffix.add(".css");
        excludeSuffix.add(".woff");
        excludeSuffix.add(".woff2");
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
                            for (String prefix : excludeSuffix) {
                                if (uri.endsWith(prefix)) {
                                    return true;
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    final String SEP = "-----------------------\n";

                    /**
                     * 记录异常请求的url详细信息
                     * @param req http servlet request
                     * @param responseCode http status code
                     * @throws NoSuchMethodException
                     * @throws IllegalAccessException
                     * @throws InvocationTargetException
                     */
                    private void logAbnormalRequestInfo(Object req, int responseCode) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
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
                                            logAbnormalRequestInfo(req, status);
                                        }
                                        t.setStatus(Transaction.SUCCESS);
                                    } else if (status >= 500) {
                                        logAbnormalRequestInfo(req, status);
                                        t.setStatus(status + "");
                                    } else { //301 302 400 404 +
                                        logAbnormalRequestInfo(req, status);
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
