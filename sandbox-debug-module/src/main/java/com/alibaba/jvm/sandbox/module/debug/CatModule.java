package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.dianping.cat.Cat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CatModule implements Module, LoadCompleted {
    static {
        Cat.initializeByDomainForce("cat111");
    }

    protected Logger stLogger = LoggerFactory.getLogger(this.getClass());

}
