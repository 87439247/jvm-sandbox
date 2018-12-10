package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.dianping.cat.Cat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatModule implements Module, LoadCompleted {
    static {
        Logger logger = LoggerFactory.getLogger(CatModule.class);
        logger.error("env size=" +  System.getenv().size());
        for (String envKey : System.getenv().keySet()) {
            logger.error("env key=" + envKey + ";value=" + System.getenv(envKey));
        }
        logger.error("prop size=" +  System.getProperties().size());
        for (Object propertyKey : System.getProperties().keySet()) {
            logger.error("prop key=" + propertyKey + ";value=" + System.getProperty((String) propertyKey));
        }
        final String domainKey = "catdomain";
        String catDomain = System.getenv(domainKey);
        if (StringUtils.isBlank(catDomain)) {
            catDomain = System.getProperty(domainKey, "catdemo");
        }
        Cat.initializeByDomainForce(catDomain);
    }

    protected Logger stLogger = LoggerFactory.getLogger(this.getClass());

    abstract String getCatType();
}
