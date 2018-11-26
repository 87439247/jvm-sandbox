package com.alibaba.jvm.sandbox.module.debug.util.beantrace;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.handlers.VertexHandler;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.renderers.GraphRenderer;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.renderers.GraphRenderers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents the bean trace algorithm options.
 */
public class TraceConfiguration {

    /**
     * Default max depth to stop when scanning objects.
     */
    public static final Integer DEFAULT_MAX_DEPTH = 10;
    /**
     * The graph renderer to use.
     */
    private GraphRenderer graphRenderer = GraphRenderers.newAscii();
    /**
     * custom vertexHandler
     */
    private List<VertexHandler> customHandlers = new LinkedList<VertexHandler>();
    /**
     * max depth
     */
    private Integer maxDepth = DEFAULT_MAX_DEPTH;
    /**
     * excluded types
     */
    private Collection<Class> excludedTypes = new LinkedList<Class>();

    public GraphRenderer getGraphRenderer() {
        return graphRenderer;
    }

    public void setGraphRenderer(GraphRenderer graphRenderer) {
        this.graphRenderer = graphRenderer;
    }

    public List<VertexHandler> getCustomHandlers() {
        return customHandlers;
    }

    public void setCustomHandlers(List<VertexHandler> customHandlers) {
        this.customHandlers = customHandlers;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Collection<Class> getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(Collection<Class> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final TraceConfiguration configuration = BeanTraces.newDefaultConfiguration();

        Builder() {
        }

        public Builder withExclusion(Class<?> excludedType) {
            configuration.getExcludedTypes().add(excludedType);
            return this;
        }

        public Builder withMaxDepth(Integer maxDepth) {
            configuration.setMaxDepth(maxDepth);
            return this;
        }

        public TraceConfiguration build() {
            return configuration;
        }
    }
}
