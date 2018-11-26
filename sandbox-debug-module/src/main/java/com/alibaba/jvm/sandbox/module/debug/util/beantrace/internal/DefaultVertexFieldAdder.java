package com.alibaba.jvm.sandbox.module.debug.util.beantrace.internal;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Attribute;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Edge;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;


/**
 * Populates the references and the attributes of the vertex based on the
 * passed field of the object.
 */
public class DefaultVertexFieldAdder implements com.alibaba.jvm.sandbox.module.debug.util.beantrace.VertexFieldAdder {

    private final VertexFactory vertexFactory;

    public DefaultVertexFieldAdder(VertexFactory vertexFactory) {
        this.vertexFactory = vertexFactory;
    }

    @Override
    public void addField(Vertex vertex, String fieldName, Object item) {
        if (item == null) {
            return;
        }

        if (ReflectUtils.isPrimitive(item.getClass())) {
            vertex.getAttributes().add(new Attribute(fieldName, item));
            return;
        }

        vertex.getReferences().add(new Edge(
                fieldName, vertexFactory.create(item)
        ));
    }
}
