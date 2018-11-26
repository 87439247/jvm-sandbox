package com.alibaba.jvm.sandbox.module.debug.util.beantrace.internal;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

public interface VertexFactory {

    Vertex create(Object subject);

}
