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
import java.util.Map;

import static com.alibaba.jvm.sandbox.module.debug.util.CatFinishUtil.finish;
import static com.alibaba.jvm.sandbox.module.debug.util.MethodUtils.invokeMethod;
import static com.alibaba.jvm.sandbox.module.debug.util.UrlUtils.rebuildPath;

@MetaInfServices(Module.class)
@Information(id = "cat-mysql-prepared-statement", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatMysqlPreparedStatementModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorMysqlPreparedStatement();
    }


    /**
     * named("execute")
     * .or(named("executeQuery"))
     * .or(named("executeUpdate"))
     * .or(named("executeLargeUpdate"));
     */
    private void monitorMysqlPreparedStatement() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.mysql.jdbc.PreparedStatement")
                .onBehavior("execute")
                .onBehavior("executeQuery")
                .onBehavior("executeUpdate")
                .onBehavior("executeLargeUpdate")

                .onClass("com.mysql.cj.jdbc.PreparedStatement")
                .onBehavior("execute")
                .onBehavior("executeQuery")
                .onBehavior("executeUpdate")
                .onBehavior("executeLargeUpdate")

                .onClass("com.mysql.jdbc.JDBC42PreparedStatement")
                .onBehavior("execute")
                .onBehavior("executeQuery")
                .onBehavior("executeUpdate")
                .onBehavior("executeLargeUpdate")

                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Object httpMethod = advice.getParameterArray()[1];

                    }

                    @Override
                    public void afterReturning(Advice advice) {


                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        finish(advice);
                    }


                });
    }

    @Override
    String getCatType() {
        return "SQL";
    }

}
