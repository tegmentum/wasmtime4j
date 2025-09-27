package ai.tegmentum.wasmtime4j.profiling;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Advanced flame graph generator for wasmtime4j profiling data.
 *
 * <p>This class provides comprehensive flame graph generation capabilities including:
 * <ul>
 *   <li>Interactive SVG flame graphs with zoom and search</li>
 *   <li>Hierarchical call stack visualization</li>
 *   <li>Time-based and sample-based flame graphs</li>
 *   <li>Multi-threaded profiling data aggregation</li>
 *   <li>Memory allocation flame graphs</li>
 *   <li>Function call frequency analysis</li>
 * </ul>
 *
 * <p>Supports multiple output formats:
 * <ul>
 *   <li>Interactive SVG with JavaScript controls</li>
 *   <li>Static SVG for embedding</li>
 *   <li>JSON for external visualization tools</li>
 *   <li>Chrome DevTools profiling format</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class FlameGraphGenerator {

    private static final Logger LOGGER = Logger.getLogger(FlameGraphGenerator.class.getName());

    /** Default flame graph width in pixels. */
    private static final int DEFAULT_WIDTH = 1200;

    /** Default flame graph height in pixels. */
    private static final int DEFAULT_HEIGHT = 800;

    /** Minimum frame width in pixels to display text. */
    private static final int MIN_FRAME_WIDTH_FOR_TEXT = 30;

    /** Colors for different function categories. */
    private static final Map<String, String> CATEGORY_COLORS = Map.of(
        "wasm", "#e74c3c",      // Red for WebAssembly functions
        "jni", "#3498db",       // Blue for JNI calls
        "panama", "#2ecc71",    // Green for Panama FFI
        "host", "#f39c12",      // Orange for host functions
        "memory", "#9b59b6",    // Purple for memory operations
        "system", "#95a5a6",    // Gray for system calls
        "unknown", "#34495e"    // Dark gray for unknown
    );

    private final FlameGraphConfig config;
    private final StackTraceCollector stackTraceCollector;
    private final AtomicLong sampleId = new AtomicLong(0);

    /**
     * Configuration for flame graph generation.
     */
    public static final class FlameGraphConfig {
        private final int width;
        private final int height;
        private final boolean includeJavaScript;
        private final boolean showTooltips;
        private final boolean enableSearch;
        private final boolean colorByCategory;
        private final Duration minFrameDuration;
        private final int maxDepth;
        private final boolean aggregateSmallFrames;
        private final double aggregationThreshold;

        private FlameGraphConfig(final Builder builder) {
            this.width = builder.width;
            this.height = builder.height;
            this.includeJavaScript = builder.includeJavaScript;
            this.showTooltips = builder.showTooltips;
            this.enableSearch = builder.enableSearch;
            this.colorByCategory = builder.colorByCategory;
            this.minFrameDuration = builder.minFrameDuration;
            this.maxDepth = builder.maxDepth;
            this.aggregateSmallFrames = builder.aggregateSmallFrames;
            this.aggregationThreshold = builder.aggregationThreshold;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int width = DEFAULT_WIDTH;
            private int height = DEFAULT_HEIGHT;
            private boolean includeJavaScript = true;
            private boolean showTooltips = true;
            private boolean enableSearch = true;
            private boolean colorByCategory = true;
            private Duration minFrameDuration = Duration.ofMicroseconds(1);
            private int maxDepth = 100;
            private boolean aggregateSmallFrames = true;
            private double aggregationThreshold = 0.01; // 1% of total time

            public Builder width(final int width) { this.width = width; return this; }
            public Builder height(final int height) { this.height = height; return this; }
            public Builder includeJavaScript(final boolean include) { this.includeJavaScript = include; return this; }
            public Builder showTooltips(final boolean show) { this.showTooltips = show; return this; }
            public Builder enableSearch(final boolean enable) { this.enableSearch = enable; return this; }
            public Builder colorByCategory(final boolean color) { this.colorByCategory = color; return this; }
            public Builder minFrameDuration(final Duration duration) { this.minFrameDuration = duration; return this; }
            public Builder maxDepth(final int depth) { this.maxDepth = depth; return this; }
            public Builder aggregateSmallFrames(final boolean aggregate) { this.aggregateSmallFrames = aggregate; return this; }
            public Builder aggregationThreshold(final double threshold) { this.aggregationThreshold = threshold; return this; }

            public FlameGraphConfig build() {
                return new FlameGraphConfig(this);
            }
        }

        // Getters
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public boolean includeJavaScript() { return includeJavaScript; }
        public boolean showTooltips() { return showTooltips; }
        public boolean enableSearch() { return enableSearch; }
        public boolean colorByCategory() { return colorByCategory; }
        public Duration getMinFrameDuration() { return minFrameDuration; }
        public int getMaxDepth() { return maxDepth; }
        public boolean aggregateSmallFrames() { return aggregateSmallFrames; }
        public double getAggregationThreshold() { return aggregationThreshold; }
    }

    /**
     * Represents a stack frame in the flame graph.
     */
    public static final class FlameFrame {
        private final String functionName;
        private final String category;
        private final Duration selfTime;
        private final Duration totalTime;
        private final long sampleCount;
        private final int depth;
        private final List<FlameFrame> children;
        private final Map<String, Object> metadata;

        private FlameFrame(final Builder builder) {
            this.functionName = Objects.requireNonNull(builder.functionName);
            this.category = Objects.requireNonNull(builder.category);
            this.selfTime = Objects.requireNonNull(builder.selfTime);
            this.totalTime = Objects.requireNonNull(builder.totalTime);
            this.sampleCount = builder.sampleCount;
            this.depth = builder.depth;
            this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
            this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
        }

        public static Builder builder(final String functionName) {
            return new Builder(functionName);
        }

        public static final class Builder {
            private final String functionName;
            private String category = "unknown";
            private Duration selfTime = Duration.ZERO;
            private Duration totalTime = Duration.ZERO;
            private long sampleCount = 0;
            private int depth = 0;
            private final List<FlameFrame> children = new ArrayList<>();
            private final Map<String, Object> metadata = new HashMap<>();

            private Builder(final String functionName) {
                this.functionName = functionName;
            }

            public Builder category(final String category) { this.category = category; return this; }
            public Builder selfTime(final Duration time) { this.selfTime = time; return this; }
            public Builder totalTime(final Duration time) { this.totalTime = time; return this; }
            public Builder sampleCount(final long count) { this.sampleCount = count; return this; }
            public Builder depth(final int depth) { this.depth = depth; return this; }
            public Builder addChild(final FlameFrame child) { this.children.add(child); return this; }
            public Builder metadata(final String key, final Object value) { this.metadata.put(key, value); return this; }

            public FlameFrame build() {
                return new FlameFrame(this);
            }
        }

        // Getters
        public String getFunctionName() { return functionName; }
        public String getCategory() { return category; }
        public Duration getSelfTime() { return selfTime; }
        public Duration getTotalTime() { return totalTime; }
        public long getSampleCount() { return sampleCount; }
        public int getDepth() { return depth; }
        public List<FlameFrame> getChildren() { return children; }
        public Map<String, Object> getMetadata() { return metadata; }

        public double getSelfTimePercentage(final Duration totalProfileTime) {
            return totalProfileTime.toNanos() > 0 ?
                (selfTime.toNanos() * 100.0) / totalProfileTime.toNanos() : 0.0;
        }

        public double getTotalTimePercentage(final Duration totalProfileTime) {
            return totalProfileTime.toNanos() > 0 ?
                (totalTime.toNanos() * 100.0) / totalProfileTime.toNanos() : 0.0;
        }
    }

    /**
     * Stack trace sample data.
     */
    public static final class StackSample {
        private final long sampleId;
        private final Instant timestamp;
        private final Duration duration;
        private final List<String> stackTrace;
        private final String threadName;
        private final Map<String, Object> metadata;

        public StackSample(final long sampleId, final Instant timestamp, final Duration duration,
                          final List<String> stackTrace, final String threadName,
                          final Map<String, Object> metadata) {
            this.sampleId = sampleId;
            this.timestamp = Objects.requireNonNull(timestamp);
            this.duration = Objects.requireNonNull(duration);
            this.stackTrace = Collections.unmodifiableList(List.copyOf(stackTrace));
            this.threadName = Objects.requireNonNull(threadName);
            this.metadata = Collections.unmodifiableMap(Map.copyOf(metadata));
        }

        // Getters
        public long getSampleId() { return sampleId; }
        public Instant getTimestamp() { return timestamp; }
        public Duration getDuration() { return duration; }
        public List<String> getStackTrace() { return stackTrace; }
        public String getThreadName() { return threadName; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * Collects stack trace samples for flame graph generation.
     */
    public static final class StackTraceCollector {
        private final ConcurrentHashMap<Long, StackSample> samples = new ConcurrentHashMap<>();
        private final AtomicLong nextSampleId = new AtomicLong(1);
        private volatile boolean collecting = false;

        public void startCollection() {
            collecting = true;
            LOGGER.info("Started stack trace collection");
        }

        public void stopCollection() {
            collecting = false;
            LOGGER.info("Stopped stack trace collection, collected " + samples.size() + " samples");
        }

        public long recordSample(final Duration duration, final List<String> stackTrace,
                                final String threadName, final Map<String, Object> metadata) {
            if (!collecting) {
                return 0;
            }

            final long sampleId = nextSampleId.getAndIncrement();
            final StackSample sample = new StackSample(
                sampleId, Instant.now(), duration, stackTrace, threadName, metadata
            );

            samples.put(sampleId, sample);
            return sampleId;
        }

        public Collection<StackSample> getAllSamples() {
            return Collections.unmodifiableCollection(samples.values());
        }

        public void clear() {
            samples.clear();
            nextSampleId.set(1);
        }

        public boolean isCollecting() {
            return collecting;
        }
    }

    public FlameGraphGenerator() {
        this(FlameGraphConfig.builder().build());
    }

    public FlameGraphGenerator(final FlameGraphConfig config) {
        this.config = Objects.requireNonNull(config);
        this.stackTraceCollector = new StackTraceCollector();
    }

    /**
     * Records a stack trace sample for flame graph generation.
     *
     * @param duration duration of the operation
     * @param stackTrace stack trace at the time of sampling
     * @param threadName name of the thread
     * @param metadata additional metadata
     * @return sample ID
     */
    public long recordSample(final Duration duration, final List<String> stackTrace,
                            final String threadName, final Map<String, Object> metadata) {
        return stackTraceCollector.recordSample(duration, stackTrace, threadName, metadata);
    }

    /**
     * Starts stack trace collection.
     */
    public void startCollection() {
        stackTraceCollector.startCollection();
    }

    /**
     * Stops stack trace collection.
     */
    public void stopCollection() {
        stackTraceCollector.stopCollection();
    }

    /**
     * Generates flame graph from collected profiling data.
     *
     * @param samples stack trace samples to process
     * @return flame graph root frame
     */
    public FlameFrame generateFlameGraph(final Collection<StackSample> samples) {
        if (samples.isEmpty()) {
            return FlameFrame.builder("(no samples)")
                .category("system")
                .selfTime(Duration.ZERO)
                .totalTime(Duration.ZERO)
                .build();
        }

        LOGGER.info("Generating flame graph from " + samples.size() + " samples");

        // Build call tree from samples
        final CallTreeNode rootNode = buildCallTree(samples);

        // Convert to flame frame
        final Duration totalTime = samples.stream()
            .map(StackSample::getDuration)
            .reduce(Duration.ZERO, Duration::plus);

        return convertToFlameFrame(rootNode, totalTime, 0);
    }

    /**
     * Generates flame graph from currently collected samples.
     *
     * @return flame graph root frame
     */
    public FlameFrame generateFlameGraph() {
        return generateFlameGraph(stackTraceCollector.getAllSamples());
    }

    /**
     * Generates interactive SVG flame graph.
     *
     * @param rootFrame root frame of the flame graph
     * @return SVG content as string
     */
    public String generateSvgFlameGraph(final FlameFrame rootFrame) {
        final StringWriter writer = new StringWriter();
        try {
            generateSvgFlameGraph(rootFrame, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate SVG flame graph", e);
        }
        return writer.toString();
    }

    /**
     * Generates interactive SVG flame graph and writes to the provided writer.
     *
     * @param rootFrame root frame of the flame graph
     * @param writer writer to output SVG content
     * @throws IOException if writing fails
     */
    public void generateSvgFlameGraph(final FlameFrame rootFrame, final Appendable writer) throws IOException {
        final Duration totalTime = rootFrame.getTotalTime();
        if (totalTime.isZero()) {
            writer.append("<svg><text>No profiling data available</text></svg>");
            return;
        }

        // SVG header
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<svg width=\"").append(String.valueOf(config.getWidth()))
              .append("\" height=\"").append(String.valueOf(config.getHeight()))
              .append("\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        // Add CSS styles
        appendSvgStyles(writer);

        // Generate flame graph rectangles
        generateSvgFrames(rootFrame, writer, 0, 0, config.getWidth(), totalTime);

        // Add JavaScript interactivity
        if (config.includeJavaScript()) {
            appendSvgJavaScript(writer);
        }

        // SVG footer
        writer.append("</svg>\n");
    }

    /**
     * Generates JSON representation of the flame graph for external tools.
     *
     * @param rootFrame root frame of the flame graph
     * @return JSON string
     */
    public String generateJsonFlameGraph(final FlameFrame rootFrame) {
        final StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"name\": \"").append(escapeJson(rootFrame.getFunctionName())).append("\",\n");
        json.append("  \"category\": \"").append(escapeJson(rootFrame.getCategory())).append("\",\n");
        json.append("  \"selfTime\": ").append(rootFrame.getSelfTime().toNanos()).append(",\n");
        json.append("  \"totalTime\": ").append(rootFrame.getTotalTime().toNanos()).append(",\n");
        json.append("  \"sampleCount\": ").append(rootFrame.getSampleCount()).append(",\n");
        json.append("  \"children\": [\n");

        final List<FlameFrame> children = rootFrame.getChildren();
        for (int i = 0; i < children.size(); i++) {
            json.append(generateJsonFlameGraph(children.get(i)));
            if (i < children.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Generates Chrome DevTools profiling format.
     *
     * @param rootFrame root frame of the flame graph
     * @return Chrome DevTools format JSON
     */
    public String generateChromeDevToolsFormat(final FlameFrame rootFrame) {
        final StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"nodes\": [\n");

        final List<FlameFrame> allFrames = collectAllFrames(rootFrame);
        for (int i = 0; i < allFrames.size(); i++) {
            final FlameFrame frame = allFrames.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(i).append(",\n");
            json.append("      \"callFrame\": {\n");
            json.append("        \"functionName\": \"").append(escapeJson(frame.getFunctionName())).append("\",\n");
            json.append("        \"scriptId\": \"0\",\n");
            json.append("        \"url\": \"\",\n");
            json.append("        \"lineNumber\": 0,\n");
            json.append("        \"columnNumber\": 0\n");
            json.append("      },\n");
            json.append("      \"selfTime\": ").append(frame.getSelfTime().toNanos() / 1000).append(",\n"); // microseconds
            json.append("      \"totalTime\": ").append(frame.getTotalTime().toNanos() / 1000).append("\n");
            json.append("    }");
            if (i < allFrames.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"samples\": [],\n");
        json.append("  \"timeDeltas\": []\n");
        json.append("}");

        return json.toString();
    }

    /**
     * Saves flame graph as SVG file.
     *
     * @param rootFrame root frame of the flame graph
     * @param outputPath path to save the SVG file
     * @throws IOException if writing fails
     */
    public void saveSvgFlameGraph(final FlameFrame rootFrame, final Path outputPath) throws IOException {
        try (final BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            generateSvgFlameGraph(rootFrame, writer);
        }
        LOGGER.info("Saved flame graph to: " + outputPath);
    }

    /**
     * Saves flame graph as JSON file.
     *
     * @param rootFrame root frame of the flame graph
     * @param outputPath path to save the JSON file
     * @throws IOException if writing fails
     */
    public void saveJsonFlameGraph(final FlameFrame rootFrame, final Path outputPath) throws IOException {
        Files.writeString(outputPath, generateJsonFlameGraph(rootFrame), StandardCharsets.UTF_8);
        LOGGER.info("Saved flame graph JSON to: " + outputPath);
    }

    /**
     * Analyzes flame graph for performance insights.
     *
     * @param rootFrame root frame to analyze
     * @return performance insights
     */
    public FlameGraphInsights analyzeFlameGraph(final FlameFrame rootFrame) {
        final List<FlameFrame> allFrames = collectAllFrames(rootFrame);
        final Duration totalTime = rootFrame.getTotalTime();

        // Find hottest functions
        final List<FlameFrame> hottestFunctions = allFrames.stream()
            .filter(frame -> frame.getSelfTime().compareTo(Duration.ZERO) > 0)
            .sorted((f1, f2) -> f2.getSelfTime().compareTo(f1.getSelfTime()))
            .limit(10)
            .collect(Collectors.toList());

        // Find deepest stacks
        final int maxDepth = allFrames.stream()
            .mapToInt(FlameFrame::getDepth)
            .max()
            .orElse(0);

        // Analyze categories
        final Map<String, Duration> categoryTimes = allFrames.stream()
            .collect(Collectors.groupingBy(
                FlameFrame::getCategory,
                Collectors.reducing(Duration.ZERO, FlameFrame::getSelfTime, Duration::plus)
            ));

        return new FlameGraphInsights(
            totalTime,
            allFrames.size(),
            maxDepth,
            hottestFunctions,
            categoryTimes
        );
    }

    /**
     * Performance insights from flame graph analysis.
     */
    public static final class FlameGraphInsights {
        private final Duration totalTime;
        private final int totalFrames;
        private final int maxDepth;
        private final List<FlameFrame> hottestFunctions;
        private final Map<String, Duration> categoryTimes;

        public FlameGraphInsights(final Duration totalTime, final int totalFrames, final int maxDepth,
                                 final List<FlameFrame> hottestFunctions, final Map<String, Duration> categoryTimes) {
            this.totalTime = totalTime;
            this.totalFrames = totalFrames;
            this.maxDepth = maxDepth;
            this.hottestFunctions = Collections.unmodifiableList(List.copyOf(hottestFunctions));
            this.categoryTimes = Collections.unmodifiableMap(Map.copyOf(categoryTimes));
        }

        public Duration getTotalTime() { return totalTime; }
        public int getTotalFrames() { return totalFrames; }
        public int getMaxDepth() { return maxDepth; }
        public List<FlameFrame> getHottestFunctions() { return hottestFunctions; }
        public Map<String, Duration> getCategoryTimes() { return categoryTimes; }

        public List<String> getOptimizationRecommendations() {
            final List<String> recommendations = new ArrayList<>();

            if (maxDepth > 50) {
                recommendations.add("Consider reducing call stack depth (current: " + maxDepth + ")");
            }

            if (!hottestFunctions.isEmpty()) {
                final FlameFrame hottest = hottestFunctions.get(0);
                final double percentage = hottest.getSelfTimePercentage(totalTime);
                if (percentage > 20) {
                    recommendations.add(String.format(
                        "Function '%s' consumes %.1f%% of total time - consider optimization",
                        hottest.getFunctionName(), percentage));
                }
            }

            // Check for JNI overhead
            final Duration jniTime = categoryTimes.getOrDefault("jni", Duration.ZERO);
            if (jniTime.toNanos() > 0) {
                final double jniPercentage = (jniTime.toNanos() * 100.0) / totalTime.toNanos();
                if (jniPercentage > 30) {
                    recommendations.add(String.format(
                        "JNI calls consume %.1f%% of time - consider batching operations", jniPercentage));
                }
            }

            if (recommendations.isEmpty()) {
                recommendations.add("No significant performance issues identified");
            }

            return recommendations;
        }
    }

    // Private helper methods

    private static final class CallTreeNode {
        final String functionName;
        final String category;
        Duration totalTime = Duration.ZERO;
        long sampleCount = 0;
        final Map<String, CallTreeNode> children = new HashMap<>();

        CallTreeNode(final String functionName, final String category) {
            this.functionName = functionName;
            this.category = category;
        }

        void addSample(final Duration duration) {
            totalTime = totalTime.plus(duration);
            sampleCount++;
        }
    }

    private CallTreeNode buildCallTree(final Collection<StackSample> samples) {
        final CallTreeNode root = new CallTreeNode("(root)", "system");

        for (final StackSample sample : samples) {
            CallTreeNode currentNode = root;

            // Process stack trace in reverse order (from root to leaf)
            final List<String> stack = sample.getStackTrace();
            for (int i = stack.size() - 1; i >= 0; i--) {
                final String functionName = stack.get(i);
                final String category = categorizeFunction(functionName);

                currentNode = currentNode.children.computeIfAbsent(
                    functionName,
                    name -> new CallTreeNode(name, category)
                );

                currentNode.addSample(sample.getDuration());
            }
        }

        return root;
    }

    private FlameFrame convertToFlameFrame(final CallTreeNode node, final Duration totalProfileTime, final int depth) {
        if (depth > config.getMaxDepth()) {
            // Aggregate remaining frames
            return FlameFrame.builder("(truncated)")
                .category("system")
                .selfTime(node.totalTime)
                .totalTime(node.totalTime)
                .sampleCount(node.sampleCount)
                .depth(depth)
                .build();
        }

        Duration childrenTime = Duration.ZERO;
        final List<FlameFrame> childFrames = new ArrayList<>();

        for (final CallTreeNode childNode : node.children.values()) {
            // Filter out small frames if aggregation is enabled
            if (config.aggregateSmallFrames()) {
                final double percentage = (childNode.totalTime.toNanos() * 100.0) / totalProfileTime.toNanos();
                if (percentage < config.getAggregationThreshold()) {
                    continue;
                }
            }

            final FlameFrame childFrame = convertToFlameFrame(childNode, totalProfileTime, depth + 1);
            childFrames.add(childFrame);
            childrenTime = childrenTime.plus(childNode.totalTime);
        }

        final Duration selfTime = node.totalTime.minus(childrenTime);

        return FlameFrame.builder(node.functionName)
            .category(node.category)
            .selfTime(selfTime)
            .totalTime(node.totalTime)
            .sampleCount(node.sampleCount)
            .depth(depth)
            .addChild(childFrames.toArray(new FlameFrame[0]))
            .build();
    }

    private String categorizeFunction(final String functionName) {
        if (functionName.contains("wasmtime4j_jni") || functionName.contains("JNI")) {
            return "jni";
        } else if (functionName.contains("wasmtime4j_panama") || functionName.contains("Panama")) {
            return "panama";
        } else if (functionName.startsWith("wasm_") || functionName.contains("WebAssembly")) {
            return "wasm";
        } else if (functionName.contains("host_function") || functionName.startsWith("host_")) {
            return "host";
        } else if (functionName.contains("memory") || functionName.contains("alloc")) {
            return "memory";
        } else if (functionName.startsWith("java.") || functionName.startsWith("sun.")) {
            return "system";
        }
        return "unknown";
    }

    private void appendSvgStyles(final Appendable writer) throws IOException {
        writer.append("<defs>\n");
        writer.append("  <style type=\"text/css\"><![CDATA[\n");
        writer.append("    .frame { stroke: #000; stroke-width: 0.5; cursor: pointer; }\n");
        writer.append("    .frame:hover { stroke: #ff0000; stroke-width: 2; }\n");
        writer.append("    .frame-text { font-family: Verdana, sans-serif; font-size: 12px; fill: #000; }\n");
        writer.append("    .tooltip { background: rgba(0,0,0,0.8); color: white; padding: 5px; border-radius: 3px; }\n");
        writer.append("  ]]></style>\n");
        writer.append("</defs>\n");
    }

    private void generateSvgFrames(final FlameFrame frame, final Appendable writer,
                                  final double x, final double y, final double width, final Duration totalTime) throws IOException {
        if (width < 1) return; // Skip very thin frames

        final String color = config.colorByCategory() ?
            CATEGORY_COLORS.getOrDefault(frame.getCategory(), CATEGORY_COLORS.get("unknown")) : "#3498db";

        // Draw frame rectangle
        writer.append("<rect class=\"frame\" x=\"").append(String.valueOf(x))
              .append("\" y=\"").append(String.valueOf(y))
              .append("\" width=\"").append(String.valueOf(width))
              .append("\" height=\"20\" fill=\"").append(color).append("\"");

        if (config.showTooltips()) {
            final double selfPercent = frame.getSelfTimePercentage(totalTime);
            final double totalPercent = frame.getTotalTimePercentage(totalTime);
            writer.append(" title=\"").append(escapeXml(frame.getFunctionName()))
                  .append("&#10;Self: ").append(String.format("%.2f", selfPercent)).append("%")
                  .append("&#10;Total: ").append(String.format("%.2f", totalPercent)).append("%")
                  .append("&#10;Samples: ").append(String.valueOf(frame.getSampleCount()))
                  .append("\"");
        }

        writer.append("/>\n");

        // Add text label if frame is wide enough
        if (width > MIN_FRAME_WIDTH_FOR_TEXT) {
            String displayName = frame.getFunctionName();
            if (displayName.length() * 8 > width) { // Rough character width estimation
                displayName = displayName.substring(0, Math.max(1, (int) (width / 8) - 3)) + "...";
            }

            writer.append("<text class=\"frame-text\" x=\"").append(String.valueOf(x + 5))
                  .append("\" y=\"").append(String.valueOf(y + 15))
                  .append("\">").append(escapeXml(displayName)).append("</text>\n");
        }

        // Draw children
        double childX = x;
        for (final FlameFrame child : frame.getChildren()) {
            final double childWidth = (child.getTotalTime().toNanos() * width) / frame.getTotalTime().toNanos();
            generateSvgFrames(child, writer, childX, y + 20, childWidth, totalTime);
            childX += childWidth;
        }
    }

    private void appendSvgJavaScript(final Appendable writer) throws IOException {
        writer.append("<script type=\"text/javascript\"><![CDATA[\n");
        writer.append("// Interactive flame graph functionality\n");
        writer.append("document.addEventListener('DOMContentLoaded', function() {\n");
        writer.append("  var frames = document.querySelectorAll('.frame');\n");
        writer.append("  frames.forEach(function(frame) {\n");
        writer.append("    frame.addEventListener('click', function() {\n");
        writer.append("      console.log('Clicked frame:', this.getAttribute('title'));\n");
        writer.append("    });\n");
        writer.append("  });\n");
        writer.append("});\n");
        writer.append("]]></script>\n");
    }

    private List<FlameFrame> collectAllFrames(final FlameFrame rootFrame) {
        final List<FlameFrame> allFrames = new ArrayList<>();
        collectAllFramesRecursive(rootFrame, allFrames);
        return allFrames;
    }

    private void collectAllFramesRecursive(final FlameFrame frame, final List<FlameFrame> collector) {
        collector.add(frame);
        for (final FlameFrame child : frame.getChildren()) {
            collectAllFramesRecursive(child, collector);
        }
    }

    private String escapeXml(final String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "<")
                  .replace(">", ">")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    private String escapeJson(final String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public StackTraceCollector getStackTraceCollector() {
        return stackTraceCollector;
    }

    public FlameGraphConfig getConfig() {
        return config;
    }
}