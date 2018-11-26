package com.alibaba.jvm.sandbox.module.debug.util.beantrace.internal;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;

import java.util.Collection;
import java.util.LinkedList;

public class CollectorVisitor implements VertexVisitor {

    private final Collection<Vertex> result = new LinkedList<Vertex>();

    @Override
    public void visit(Vertex vertex) {
        result.add(vertex);
    }

    public Collection<Vertex> getResult() {
        return result;
    }
}
