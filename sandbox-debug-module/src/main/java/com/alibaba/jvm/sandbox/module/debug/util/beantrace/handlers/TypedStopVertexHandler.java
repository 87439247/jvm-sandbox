package com.alibaba.jvm.sandbox.module.debug.util.beantrace.handlers;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.VertexFieldAdder;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

/**
 * This class stops the propagation of the scanning  on the given object type.
 */
class TypedStopVertexHandler<T> extends TypeBasedHandler<T> {

    TypedStopVertexHandler(Class<?> handledType) {
        super(handledType);
    }

    @Override
    protected void typedHandle(Vertex vertex, T subject, VertexFieldAdder vertexFieldAdder) {
        // Intentionally left blank
    }
}
