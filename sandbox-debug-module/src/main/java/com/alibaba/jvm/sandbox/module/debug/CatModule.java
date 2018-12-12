package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.module.debug.util.IpUtils;
import com.dianping.cat.Cat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatModule implements Module, LoadCompleted {

    public static final String CAT_DOMAIN;

    static Logger internalLogger = LoggerFactory.getLogger(CatModule.class);

    static {
        StringBuilder envBuider = new StringBuilder();
        envBuider.append("\nenv size=").append(System.getenv().size());
        for (String envKey : System.getenv().keySet()) {
            envBuider.append("\nenv key=").append(envKey).append(";value=").append(System.getenv(envKey));
        }
        envBuider.append("\nprop size=").append(System.getProperties().size());
        for (Object propertyKey : System.getProperties().keySet()) {
            envBuider.append("\nprop key=").append(propertyKey).append(";value=").append(System.getProperty((String) propertyKey));
        }
        envBuider.append("\nIP=").append(IpUtils.getIp());
        envBuider.append("\nHOSTNAME=").append(IpUtils.getHostName());
        internalLogger.error(envBuider.toString());
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
