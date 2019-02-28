package com.alibaba.jvm.sandbox.module.debug.cat;

import com.alibaba.fastjson.JSON;
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

import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;

@MetaInfServices(Module.class)
@Information(id = "cat-elastic-job", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatElasticJobModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorElasticJobContext();
    }


    private void monitorElasticJobContext() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.dangdang.ddframe.job.executor.AbstractElasticJobExecutor").includeSubClasses()
                .onBehavior("process")
                .withParameterTypes("com.dangdang.ddframe.job.api.ShardingContext")
                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        String jobName = invokeMethod(advice.getParameterArray()[0], "getJobName");
                        String shardingParameter = invokeMethod(advice.getParameterArray()[0], "getShardingParameter");
                        Transaction t = Cat.newTransaction(getCatType(), jobName + "-" + shardingParameter);
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
                            Cat.logEvent(getCatType(), t.getName(), Message.SUCCESS, JSON.toJSONString(advice.getParameterArray()[0]));
                            StringBuilder metricName = new StringBuilder(getCatType()).append('-').append((String) invokeMethod(advice.getParameterArray()[0], "getJobName"));
                            Cat.logMetricForCount(metricName.toString());
                            if (advice.getThrowable() != null) {
                                t.setStatus(advice.getThrowable());
                                Cat.logError(advice.getThrowable());
                            } else {
                                t.setStatus(Message.SUCCESS);
                            }
                        } catch (Exception e) {
                            t.setStatus(e);
                            Cat.logError(e);
                        } finally {
                            t.complete();
                        }
                    }
                });
    }


    @Override
    String getCatType() {
        return "JOB";
    }
}
