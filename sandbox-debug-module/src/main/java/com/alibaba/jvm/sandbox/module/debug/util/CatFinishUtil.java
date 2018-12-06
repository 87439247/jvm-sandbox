package com.alibaba.jvm.sandbox.module.debug.util;

import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;

public class CatFinishUtil {
    /**
     * cat finish common
     *
     * @param advice advice
     */
    public static void finish(Advice advice) {
        Transaction transaction = advice.attachment();
        if (transaction != null) {
            try {
                if (advice.getThrowable() != null) {
                    transaction.setStatus(advice.getThrowable());
                    Cat.logError(advice.getThrowable());
                } else {
                    transaction.setStatus(Message.SUCCESS);
                }
            } catch (Exception e) {
                transaction.setStatus(e);
                Cat.logError(e);
            } finally {
                transaction.complete();
            }
        }
    }
}
