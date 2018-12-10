package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.dianping.cat.Cat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatModule implements Module, LoadCompleted {
    static {
        final String domainKey = "catdomain";
        String catDomain = System.getenv(domainKey);
        if (StringUtils.isBlank(catDomain)) {
            catDomain = System.getProperty(domainKey, "cat111");
        }
        Cat.initializeByDomainForce(catDomain);
    }

    protected Logger stLogger = LoggerFactory.getLogger(this.getClass());

    abstract String getCatType();
}
