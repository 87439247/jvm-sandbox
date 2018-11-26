package com.alibaba.jvm.sandbox.module.debug.util.beantrace.handlers;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.VertexFieldAdder;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Attribute;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

import java.net.URL;

class URLTypeHandler extends TypeBasedHandler<URL> {

    URLTypeHandler() {
        super(URL.class);
    }

    @Override
    protected void typedHandle(Vertex vertex, URL subject, VertexFieldAdder vertexFieldAdder) {
        vertex.getAttributes().add(new Attribute<>("url", subject.toString()));
    }
}
