package com.alibaba.jvm.sandbox.module.debug.util.beantrace.handlers;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.VertexFieldAdder;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

abstract class TypeBasedHandler<T> implements VertexHandler {

    private final Class<?> handledType;

    protected TypeBasedHandler(Class<?> handledType) {
        this.handledType = handledType;
    }

    @Override
    public boolean canHandle(Object subject) {
        return handledType.isAssignableFrom(subject.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(Vertex vertex, Object subject, VertexFieldAdder vertexFieldAdder) {
        typedHandle(vertex, (T) subject, vertexFieldAdder);
    }

    protected abstract void typedHandle(Vertex vertex, T subject, VertexFieldAdder vertexFieldAdder);
}
