package com.alibaba.jvm.sandbox.module.debug.util.beantrace.internal;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.VertexFieldAdder;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.handlers.VertexHandler;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Attribute;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Edge;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class DefaultVertexFactory implements VertexFactory {

    private final Map<Object, Vertex> visitedMap = new IdentityHashMap<Object, Vertex>();
    private final VertexHandler vertexHandler;
    private final VertexFieldAdder vertexFieldAdder;

    public DefaultVertexFactory(VertexHandler vertexHandler, VertexFieldAdder vertexFieldAdder) {
        this.vertexHandler = vertexHandler;
        this.vertexFieldAdder = vertexFieldAdder;
    }

    @Override
    public Vertex create(Object subject) {
        if (visitedMap.containsKey(subject)) {
            return visitedMap.get(subject);
        }

        final Set<Edge> references = new HashSet<Edge>();
        final Set<Attribute> attributes = new HashSet<Attribute>();
        final Vertex ret = new Vertex(
                subject.getClass(),
                System.identityHashCode(subject) + "",
                references,
                attributes
        );

        visitedMap.put(subject, ret);

        vertexHandler.handle(ret, subject, vertexFieldAdder);

        return ret;
    }

}
