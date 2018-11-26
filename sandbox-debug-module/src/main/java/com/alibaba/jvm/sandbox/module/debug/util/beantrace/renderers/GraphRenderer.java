package com.alibaba.jvm.sandbox.module.debug.util.beantrace.renderers;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

public interface GraphRenderer {
    void render(Vertex subject);
}
