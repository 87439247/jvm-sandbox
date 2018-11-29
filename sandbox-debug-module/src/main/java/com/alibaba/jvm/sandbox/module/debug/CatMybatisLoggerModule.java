package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.BeanTraces;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.HashMap;

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;

/**
 * 基于JDBC的SQL日志
 *
 * @author luanjia@taobao.com
 */
@MetaInfServices(Module.class)
@Information(id = "cat-mybatis-logger", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatMybatisLoggerModule implements Module, LoadCompleted {

    private final Logger smLogger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorMybatisMappedStatement();
    }

    // 监控org.apache.ibatis.executor.ReuseExecutor的所有实现类
    private void monitorMybatisMappedStatement() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.ibatis.executor.ReuseExecutor").includeSubClasses()
                .onBehavior("doQuery*")
                /**/.withParameterTypes("org.apache.ibatis.mapping.MappedStatement", "java.lang.Object")
                .onBehavior("doUpdate")
                /**/.withParameterTypes("org.apache.ibatis.mapping.MappedStatement", "java.lang.Object")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object mappedStatement = advice.getParameterArray()[0];
                        String id = invokeMethod(mappedStatement, "getId");
                        Transaction t = Cat.newTransaction("MYBATIS", id);
                        advice.attach(t);
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
                        Transaction t = advice.attachment();
                        try {
                            if (advice.getThrowable() != null) {
                                t.setStatus(advice.getThrowable());
                                Object param = advice.getParameterArray()[1];
                                HashMap map = (HashMap) FieldUtils.getField(advice.getTarget().getClass(), "statementMap", true).get(advice.getTarget());
                                Cat.logEvent("MYBATIS", "SQL", "500", BeanTraces.printBeanTraceAscii(map.keySet().toArray()).toString());
                                Cat.logEvent("MYBATIS", "SQL.PARAM", "500", BeanTraces.printBeanTraceAscii(param).toString());
                                Cat.logError(advice.getThrowable());
                            } else {
                                t.setStatus(Message.SUCCESS);
                            }
                        } catch (Throwable e) {
                            t.setStatus(e);
                            Cat.logError(e);
                        } finally {
                            t.complete();
                        }
                    }
                });
    }

}
