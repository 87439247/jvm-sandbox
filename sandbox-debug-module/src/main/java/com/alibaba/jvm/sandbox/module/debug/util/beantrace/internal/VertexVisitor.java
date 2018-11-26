package com.alibaba.jvm.sandbox.module.debug.util.beantrace.internal;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

public interface VertexVisitor {
    void visit(Vertex vertex);
}
