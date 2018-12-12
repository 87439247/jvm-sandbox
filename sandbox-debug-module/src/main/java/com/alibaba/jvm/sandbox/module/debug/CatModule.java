package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.dianping.cat.Cat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatModule implements Module, LoadCompleted {

    public static final String CAT_DOMAIN;

    static Logger internalLogger = LoggerFactory.getLogger(CatModule.class);

    static {
        internalLogger.error("env size=" + System.getenv().size());
        for (String envKey : System.getenv().keySet()) {
            internalLogger.error("env key=" + envKey + ";value=" + System.getenv(envKey));
        }
        internalLogger.error("prop size=" + System.getProperties().size());
        for (Object propertyKey : System.getProperties().keySet()) {
            internalLogger.error("prop key=" + propertyKey + ";value=" + System.getProperty((String) propertyKey));
        }
        final String domainKey = "catdomain";
        CAT_DOMAIN = getConfigFromEnv(domainKey, "catdemo");
        Cat.initializeByDomainForce(CAT_DOMAIN);
    }

    protected Logger stLogger = LoggerFactory.getLogger(this.getClass());


    public static String getConfigFromEnv(String key, String def) {
        String value = System.getenv(key);
        if (StringUtils.isBlank(value)) {
            value = System.getProperty(key, def);
        }
        return value;
    }

    abstract String getCatType();
}
