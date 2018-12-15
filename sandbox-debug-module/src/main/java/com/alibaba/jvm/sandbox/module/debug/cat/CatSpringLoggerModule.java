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

import static com.alibaba.jvm.sandbox.module.debug.util.CatFinishUtil.finish;

/**
 * Spring容器的调试日志
 */
@MetaInfServices(Module.class)
@Information(id = "cat-spring-logger", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatSpringLoggerModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        buildingSpringRestController();
        buildingSpringBean();
    }

    private void buildingSpringRestController() {
        new EventWatchBuilder(moduleEventWatcher)
                .onAnyClass()
                .hasAnnotationTypes("org.springframework.web.bind.annotation.RestController")
                .onAnyBehavior()
                .hasAnnotationTypes("org.springframework.web.bind.annotation.RequestMapping")
                .onWatch(listener);
    }

    private void buildingSpringBean() {
        new EventWatchBuilder(moduleEventWatcher)
                .onAnyClass()
                .hasAnnotationTypes("org.springframework.stereotype.Service")
                .hasAnnotationTypes("org.springframework.stereotype.Component")
                .hasAnnotationTypes("org.springframework.stereotype.Repository")
                .hasAnnotationTypes("org.springframework.stereotype.Controller")
                .onAnyBehavior()
                .onWatch(listener);
    }

    private AdviceListener listener = new AdviceListener() {

        @Override
        public void before(Advice advice) {
            String methodName = advice.getBehavior().getName();
            if ("<init>".equals(methodName)) {
                methodName = "constructor";
            }
            Transaction t = Cat.newTransaction(getCatType(), advice.getTarget().getClass().getName() + "." + methodName);
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

    };

    @Override
    String getCatType() {
        return "Service";
    }
}
