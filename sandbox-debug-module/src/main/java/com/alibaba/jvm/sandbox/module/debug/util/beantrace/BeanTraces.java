package com.alibaba.jvm.sandbox.module.debug.util.beantrace;

import com.alibaba.jvm.sandbox.module.debug.util.beantrace.internal.Container;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.model.Vertex;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.renderers.AsciiRenderer;
import com.alibaba.jvm.sandbox.module.debug.util.beantrace.renderers.GraphRenderer;
import org.apache.commons.io.output.StringBuilderWriter;

/**
 * <p>Bean Trace enables printing  object relationships in a graphic clear way.
 * Following examples show the various usage of this library.</p>
 * <p/>
 * <h2>Basic printing</h2>
 * <pre>
 *
 * List&lt;String&gt; subject = new LinkedList&lt;&gt;();
 * subject.add("one");
 * subject.add("two");
 * BeanTraces.printBeanTrace(subject);
 *
 * </pre>
 * Output:
 * <pre>
 * LinkedList
 *    |-- 1 : two
 *    `-- 0 : one
 * </pre>
 * <p/>
 * <h2>Limiting max depth</h2>
 * <pre>
 *
 * List&lt;Object&gt; subject = Arrays.asList(Arrays.asList(Arrays.asList()));
 * TraceConfiguration config = BeanTraces.newDefaultConfiguration();
 * config.setMaxDepth(2);
 * BeanTraces.printBeanTrace(subject, config);
 *
 * </pre>
 * Output:
 * <pre>
 * ArrayList
 *   `-- 0 : ArrayList
 *      `-- ... : ...
 * </pre>
 * <p/>
 * <h2>Type based exclusion</h2>
 * <p>You can avoid to scan some objects based on their types</p>
 * <p/>
 * <pre>
 * List&lt;Object&gt; subject = Arrays.asList(
 *      ImmutableMap.of("some key", Collections.emptyList()),
 *      Arrays.asList("element")
 * );
 * TraceConfiguration config = BeanTraces.newDefaultConfiguration();
 * config.setExcludedTypes(Arrays.asList(Map.class));
 * BeanTraces.printBeanTrace(subject, config);
 *
 * </pre>
 * Output:
 * <pre>
 * ArrayList
 *    |-- 0 : SingletonImmutableBiMap
 *    `-- 1 : ArrayList
 *       `-- 0 : element
 * </pre>
 * <p/>
 * <h2>Use builder for configuring traces</h2>
 * <pre>
 *
 * List&lt;Object&gt; subject = Arrays.asList(Arrays.asList(Arrays.asList()));
 * TraceConfiguration config = BeanTraces.builder()
 *      .withMaxDepth(2)
 *      .withExclusion(Map.class)
 *      .build();
 * BeanTraces.printBeanTrace(subject, config);
 *
 * </pre>
 */
public class BeanTraces {

    private BeanTraces() {
    }

    /**
     * Print an object trace with the default configuration.
     *
     * @param subject The object to print
     * @see #newDefaultConfiguration()
     */
    public static void printBeanTrace(Object subject) {
        printBeanTrace(subject, newDefaultConfiguration());
    }

    /**
     * Prints an object trace and dumps it to the given renderer.
     *
     * @param subject       The object to print
     * @param graphRenderer The renderer to use
     * @see com.alibaba.jvm.sandbox.module.debug.util.beantrace.renderers.GraphRenderers
     */
    public static void printBeanTrace(Object subject, GraphRenderer graphRenderer) {
        final TraceConfiguration traceConfiguration = newDefaultConfiguration();
        traceConfiguration.setGraphRenderer(graphRenderer);
        printBeanTrace(subject, traceConfiguration);
    }

    /**
     * Prints an object trace with the given configuration.
     *
     * @param subject            The object to print
     * @param traceConfiguration The algorithm configuration
     */
    public static void printBeanTrace(Object subject, TraceConfiguration traceConfiguration) {
        final Container container = Container.make(traceConfiguration);
        final Vertex vertex = container.getVertexFactory().create(subject);
        container.getGraphRenderer().render(vertex);
    }

    /**
     * Create a new configuration object with default values.
     *
     * @return the configuration.
     */
    public static TraceConfiguration newDefaultConfiguration() {
        return new TraceConfiguration();
    }

    /**
     * get default writer instead of system.out
     *
     * @return writer
     */
    public static StringBuilderWriter getStringBuilderWriter() {
        return new StringBuilderWriter();
    }

    /**
     * print bean with string builder writer
     *
     * @param subject target
     * @return string builder writer
     */
    public static StringBuilderWriter printBeanTraceAscii(Object subject, StringBuilderWriter sbw) {
        TraceConfiguration traceConfiguration = newDefaultConfiguration();
        AsciiRenderer.Config config = new AsciiRenderer.Config();
        config.setOutput(sbw);
        AsciiRenderer asciiRenderer = new AsciiRenderer(config);
        traceConfiguration.setGraphRenderer(asciiRenderer);
        printBeanTrace(subject, asciiRenderer);
        return sbw;
    }

    /**
     * printBeanTraceAscii
     *
     * @param subject subject
     * @return StringBuilderWriter
     */
    public static StringBuilderWriter printBeanTraceAscii(Object subject) {
        return printBeanTraceAscii(subject, getStringBuilderWriter());
    }


}
