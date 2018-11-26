package com.alibaba.jvm.sandbox.module.debug.util.beantrace;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

public interface VertexFieldAdder {
    void addField(Vertex vertex, String fieldName, Object item);
}
