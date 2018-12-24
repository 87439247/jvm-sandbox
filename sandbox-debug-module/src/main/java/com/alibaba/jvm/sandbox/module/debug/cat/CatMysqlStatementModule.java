package com.alibaba.jvm.sandbox.module.debug.cat;

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

@MetaInfServices(Module.class)
@Information(id = "cat-mysql-statement", version = "0.0.1", author = "yuanyue@staff.hexun.com")
public class CatMysqlStatementModule extends CatModule {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorMysqlPreparedStatement();
    }


    /**
     * return named("execute")
     * .or(named("executeQuery"))
     * .or(named("executeUpdate"))
     * .or(named("executeLargeUpdate"))
     * .or(named("executeBatchInternal"))
     * .or(named("executeUpdateInternal"))
     * .or(named("executeQuery"))
     * .or(named("executeBatch"));
     */
    private void monitorMysqlPreparedStatement() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("com.mysql.jdbc.StatementImpl")
                .onBehavior("execute")
                .onBehavior("executeQuery")
                .onBehavior("executeUpdate")
                .onBehavior("executeLargeUpdate")
                .onBehavior("executeBatchInternal")
                .onBehavior("executeUpdateInternal")
                .onBehavior("executeBatch")

                .onClass("com.mysql.cj.jdbc.StatementImpl")
                .onBehavior("execute")
                .onBehavior("executeQuery")
                .onBehavior("executeUpdate")
                .onBehavior("executeLargeUpdate")
                .onBehavior("executeBatchInternal")
                .onBehavior("executeUpdateInternal")
                .onBehavior("executeBatch")

                .onWatch(new AdviceListener() {

                    @Override
                    public void before(Advice advice) throws Throwable {
                        Transaction t = Cat.newTransaction(getCatType(), "SQL");
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
                        if (t != null) {
                            try {
                                if (advice.getThrowable() != null) {
                                    t.setStatus(advice.getThrowable());
                                    String sql = advice.getTarget().toString();
                                    Cat.logEvent(getCatType(), getCatType(), "500", sql);
                                    Cat.logError(advice.getThrowable());
                                } else {
                                    String sql = advice.getTarget().toString();
                                    Cat.logEvent(getCatType(), getCatType(), Transaction.SUCCESS, sql);
                                    t.setStatus(Message.SUCCESS);
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

    @Override
    String getCatType() {
        return "SQL";
    }

}
