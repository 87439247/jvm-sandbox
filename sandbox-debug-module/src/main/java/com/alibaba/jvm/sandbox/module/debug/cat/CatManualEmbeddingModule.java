package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.dianping.cat.Cat;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

@MetaInfServices(Module.class)
@Information(id = "cat-manual-embedding", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatManualEmbeddingModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorManualEmbedding();
    }


    /**
     * public static void logMetricForCount(String name)
     * <p>
     * public static void logMetricForCount(String name, int quantity)
     */
    private void monitorManualEmbedding() {

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.hexun.**.Cat")
                .includeBootstrap()
                .onBehavior("logMetricForCount")
                .withParameterTypes("java.lang.String")

                .onWatch(new AdviceListener() {
                    @Override
                    public void before(Advice advice) throws Throwable {
                        int count = 1;
                        if (advice.getParameterArray().length >= 2) {
                            count = (int) advice.getParameterArray()[1];
                        }
                        Cat.logMetricForCount((String) advice.getParameterArray()[0], count);
                    }
                });

        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.hexun.**.Cat")
                .includeBootstrap()
                .onBehavior("logMetricForDuration")
                .withParameterTypes("java.lang.String", long.class.getName())

                .onWatch(new AdviceListener() {
                    @Override
                    public void before(Advice advice) throws Throwable {
                        Cat.logMetricForDuration((String) advice.getParameterArray()[0], (int) advice.getParameterArray()[1]);
                    }
                });
    }

    @Override
    String getCatType() {
        return "HttpClient";
    }
}
