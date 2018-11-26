package com.alibaba.jvm.sandbox.module.debug.util.beantrace.model;

/**
 * A link from a vertex to another.
 *
 * @see com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex
 */
public class Edge {
    private String name;
    private Vertex to;

    public Edge(String name, Vertex to) {
        this.name = name;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public Vertex getTo() {
        return to;
    }
}
