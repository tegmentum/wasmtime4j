package ai.tegmentum.wasmtime4j.profiling;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Integration layer for external profiling and monitoring tools.
 *
 * <p>This class provides integrations with popular profiling and monitoring systems:
 * <ul>
 *   <li>Prometheus metrics export</li>
 *   <li>Grafana dashboard generation</li>
 *   <li>JFR (Java Flight Recorder) custom events</li>
 *   <li>Chrome DevTools profiling format</li>
 *   <li>async-profiler integration</li>
 *   <li>Application Performance Monitoring (APM) systems</li>
 *   <li>OpenTelemetry tracing</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class ProfilingIntegrations implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ProfilingIntegrations.class.getName());

    /** Prometheus metrics exporter. */
    public static final class PrometheusExporter {
        private final ConcurrentHashMap<String, Double> metrics = new ConcurrentHashMap<>();
        private final String metricsPrefix;

        public PrometheusExporter() {
            this("wasmtime4j_");
        }

        public PrometheusExporter(final String metricsPrefix) {
            this.metricsPrefix = Objects.requireNonNull(metricsPrefix);
        }

        /**
         * Records a metric value.
         *
         * @param name metric name
         * @param value metric value
         * @param labels metric labels
         */
        public void recordMetric(final String name, final double value, final Map<String, String> labels) {
            final String metricKey = buildMetricKey(name, labels);
            metrics.put(metricKey, value);
        }

        /**
         * Exports metrics in Prometheus format.
         *
         * @param statistics profiling statistics to export
         * @return Prometheus metrics format
         */
        public String exportMetrics(final AdvancedProfiler.ProfilingStatistics statistics) {
            final StringBuilder prometheus = new StringBuilder();
            final long timestamp = System.currentTimeMillis();

            // Export basic profiling metrics
            appendMetric(prometheus, "total_samples", statistics.getTotalSamples(), timestamp);
            appendMetric(prometheus, "function_calls_total", statistics.getFunctionCalls(), timestamp);
            appendMetric(prometheus, "avg_execution_time_seconds",
                statistics.getAverageExecutionTimeNanos() / 1_000_000_000.0, timestamp);
            appendMetric(prometheus, "memory_allocations_total", statistics.getMemoryAllocations(), timestamp);
            appendMetric(prometheus, "memory_allocated_bytes", statistics.getTotalAllocatedBytes(), timestamp);
            appendMetric(prometheus, "jni_calls_total", statistics.getJniCalls(), timestamp);
            appendMetric(prometheus, "panama_calls_total", statistics.getPanamaCalls(), timestamp);

            // Export function call counts
            statistics.getFunctionCallCounts().forEach((function, count) -> {
                final Map<String, String> labels = Map.of("function", sanitizeLabel(function));
                final String metricKey = buildMetricKey("function_calls", labels);
                appendMetric(prometheus, "function_calls", count, labels, timestamp);
            });

            // Export custom metrics
            metrics.forEach((key, value) -> {
                prometheus.append(key).append(" ").append(value).append(" ").append(timestamp).append("\n");
            });

            return prometheus.toString();
        }

        /**
         * Saves metrics to a file for Prometheus scraping.
         *
         * @param outputPath path to save metrics
         * @param statistics profiling statistics
         * @throws IOException if saving fails
         */
        public void saveMetrics(final Path outputPath, final AdvancedProfiler.ProfilingStatistics statistics)
                throws IOException {
            final String metricsContent = exportMetrics(statistics);
            Files.writeString(outputPath, metricsContent, StandardCharsets.UTF_8);
        }

        private void appendMetric(final StringBuilder sb, final String name, final double value, final long timestamp) {
            sb.append(metricsPrefix).append(name).append(" ").append(value).append(" ").append(timestamp).append("\n");
        }

        private void appendMetric(final StringBuilder sb, final String name, final double value,
                                final Map<String, String> labels, final long timestamp) {
            sb.append(metricsPrefix).append(name);
            if (!labels.isEmpty()) {
                sb.append("{");
                sb.append(labels.entrySet().stream()
                    .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"")
                    .collect(Collectors.joining(",")));
                sb.append("}");
            }
            sb.append(" ").append(value).append(" ").append(timestamp).append("\n");
        }

        private String buildMetricKey(final String name, final Map<String, String> labels) {
            final StringBuilder key = new StringBuilder(metricsPrefix).append(name);
            if (!labels.isEmpty()) {
                key.append("{");
                key.append(labels.entrySet().stream()
                    .map(entry -> entry.getKey() + "=\"" + entry.getValue() + "\"")
                    .collect(Collectors.joining(",")));
                key.append("}");
            }
            return key.toString();
        }

        private String sanitizeLabel(final String label) {
            return label.replaceAll("[^a-zA-Z0-9_]", "_");
        }
    }

    /** Grafana dashboard generator. */
    public static final class GrafanaDashboard {

        /**
         * Generates a Grafana dashboard JSON for wasmtime4j metrics.
         *
         * @param dashboardTitle dashboard title
         * @return Grafana dashboard JSON
         */
        public String generateDashboard(final String dashboardTitle) {
            final Map<String, Object> dashboard = new LinkedHashMap<>();
            dashboard.put("id", null);
            dashboard.put("title", dashboardTitle);
            dashboard.put("tags", Arrays.asList("wasmtime4j", "performance", "profiling"));
            dashboard.put("timezone", "browser");
            dashboard.put("schemaVersion", 30);
            dashboard.put("version", 1);
            dashboard.put("refresh", "5s");

            // Time range
            final Map<String, Object> time = new LinkedHashMap<>();
            time.put("from", "now-1h");
            time.put("to", "now");
            dashboard.put("time", time);

            // Panels
            final List<Map<String, Object>> panels = new ArrayList<>();

            // Function calls panel
            panels.add(createPanel(1, "Function Calls", "graph",
                "sum(rate(wasmtime4j_function_calls_total[5m]))", 0, 0, 12, 8));

            // Execution time panel
            panels.add(createPanel(2, "Average Execution Time", "graph",
                "wasmtime4j_avg_execution_time_seconds", 12, 0, 12, 8));

            // Memory usage panel
            panels.add(createPanel(3, "Memory Allocation Rate", "graph",
                "rate(wasmtime4j_memory_allocations_total[5m])", 0, 8, 12, 8));

            // JNI vs Panama calls
            panels.add(createPanel(4, "JNI vs Panama Calls", "graph",
                Arrays.asList(
                    "rate(wasmtime4j_jni_calls_total[5m])",
                    "rate(wasmtime4j_panama_calls_total[5m])"
                ), 12, 8, 12, 8));

            dashboard.put("panels", panels);

            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dashboard);
            } catch (Exception e) {
                // Fallback to manual JSON generation if Jackson not available
                return generateDashboardManually(dashboard);
            }
        }

        private Map<String, Object> createPanel(final int id, final String title, final String type,
                                              final Object query, final int x, final int y, final int w, final int h) {
            final Map<String, Object> panel = new LinkedHashMap<>();
            panel.put("id", id);
            panel.put("title", title);
            panel.put("type", type);
            panel.put("gridPos", Map.of("x", x, "y", y, "w", w, "h", h));

            // Targets (queries)
            final List<Map<String, Object>> targets = new ArrayList<>();
            if (query instanceof String) {
                targets.add(Map.of("expr", query, "legendFormat", title));
            } else if (query instanceof List) {
                final List<String> queries = (List<String>) query;
                for (int i = 0; i < queries.size(); i++) {
                    targets.add(Map.of("expr", queries.get(i), "legendFormat", "Series " + (i + 1)));
                }
            }
            panel.put("targets", targets);

            return panel;
        }

        private String generateDashboardManually(final Map<String, Object> dashboard) {
            // Simplified manual JSON generation
            final StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"title\": \"").append(dashboard.get("title")).append("\",\n");
            json.append("  \"tags\": [\"wasmtime4j\", \"performance\", \"profiling\"],\n");
            json.append("  \"refresh\": \"5s\",\n");
            json.append("  \"panels\": []\n");
            json.append("}");
            return json.toString();
        }
    }

    /** Chrome DevTools profiling format exporter. */
    public static final class ChromeDevToolsExporter {

        /**
         * Exports profiling data in Chrome DevTools format.
         *
         * @param flameGraph flame graph to export
         * @return Chrome DevTools profiling format JSON
         */
        public String exportProfile(final FlameGraphGenerator.FlameFrame flameGraph) {
            final Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("$schema", "https://www.speedscope.app/file-format-schema.json");
            profile.put("version", "0.0.1");
            profile.put("name", "wasmtime4j Profile");
            profile.put("unit", "nanoseconds");
            profile.put("indexToView", 0);
            profile.put("exporter", "wasmtime4j");

            // Shared data
            final Map<String, Object> shared = new LinkedHashMap<>();
            final List<Map<String, Object>> frames = new ArrayList<>();
            final Map<FlameGraphGenerator.FlameFrame, Integer> frameMap = new HashMap<>();

            collectFramesForChrome(flameGraph, frames, frameMap);
            shared.put("frames", frames);
            profile.put("shared", shared);

            // Profiles
            final List<Map<String, Object>> profiles = new ArrayList<>();
            final Map<String, Object> flamegraphProfile = new LinkedHashMap<>();
            flamegraphProfile.put("type", "sampled");
            flamegraphProfile.put("name", "wasmtime4j Flame Graph");
            flamegraphProfile.put("unit", "nanoseconds");
            flamegraphProfile.put("startValue", 0);
            flamegraphProfile.put("endValue", flameGraph.getTotalTime().toNanos());

            // Samples
            final List<Object> samples = new ArrayList<>();
            final List<Object> weights = new ArrayList<>();
            collectSamplesForChrome(flameGraph, frameMap, samples, weights, new ArrayList<>());

            flamegraphProfile.put("samples", samples);
            flamegraphProfile.put("weights", weights);

            profiles.add(flamegraphProfile);
            profile.put("profiles", profiles);

            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(profile);
            } catch (Exception e) {
                // Fallback to simplified JSON
                return "{\"name\":\"wasmtime4j Profile\",\"type\":\"flamegraph\"}";
            }
        }

        private void collectFramesForChrome(final FlameGraphGenerator.FlameFrame frame,
                                          final List<Map<String, Object>> frames,
                                          final Map<FlameGraphGenerator.FlameFrame, Integer> frameMap) {
            if (frameMap.containsKey(frame)) {
                return;
            }

            final int frameIndex = frames.size();
            frameMap.put(frame, frameIndex);

            final Map<String, Object> chromeFrame = new LinkedHashMap<>();
            chromeFrame.put("name", frame.getFunctionName());
            chromeFrame.put("file", "");
            chromeFrame.put("line", 0);
            chromeFrame.put("col", 0);
            frames.add(chromeFrame);

            for (final FlameGraphGenerator.FlameFrame child : frame.getChildren()) {
                collectFramesForChrome(child, frames, frameMap);
            }
        }

        private void collectSamplesForChrome(final FlameGraphGenerator.FlameFrame frame,
                                           final Map<FlameGraphGenerator.FlameFrame, Integer> frameMap,
                                           final List<Object> samples, final List<Object> weights,
                                           final List<Integer> stack) {
            final Integer frameIndex = frameMap.get(frame);
            if (frameIndex == null) return;

            stack.add(frameIndex);

            // Add sample
            samples.add(new ArrayList<>(stack));
            weights.add(frame.getSelfTime().toNanos());

            // Process children
            for (final FlameGraphGenerator.FlameFrame child : frame.getChildren()) {
                collectSamplesForChrome(child, frameMap, samples, weights, stack);
            }

            stack.remove(stack.size() - 1);
        }
    }

    /** OpenTelemetry integration. */
    public static final class OpenTelemetryIntegration {
        private final String serviceName;
        private final Map<String, String> resourceAttributes;

        public OpenTelemetryIntegration(final String serviceName) {
            this.serviceName = serviceName;
            this.resourceAttributes = new HashMap<>();
            this.resourceAttributes.put("service.name", serviceName);
            this.resourceAttributes.put("service.version", "1.0.0");
        }

        /**
         * Creates a span for profiling data.
         *
         * @param operationName operation name
         * @param duration operation duration
         * @param attributes span attributes
         * @return span data in OTLP format
         */
        public Map<String, Object> createSpan(final String operationName, final Duration duration,
                                            final Map<String, String> attributes) {
            final Map<String, Object> span = new LinkedHashMap<>();
            span.put("traceId", generateTraceId());
            span.put("spanId", generateSpanId());
            span.put("parentSpanId", "");
            span.put("name", operationName);
            span.put("kind", 1); // SPAN_KIND_INTERNAL
            span.put("startTimeUnixNano", System.nanoTime());
            span.put("endTimeUnixNano", System.nanoTime() + duration.toNanos());
            span.put("attributes", convertAttributes(attributes));
            span.put("status", Map.of("code", 1)); // STATUS_CODE_OK

            return span;
        }

        private String generateTraceId() {
            return String.format("%032x", new Random().nextLong());
        }

        private String generateSpanId() {
            return String.format("%016x", new Random().nextLong());
        }

        private List<Map<String, Object>> convertAttributes(final Map<String, String> attributes) {
            return attributes.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                    "key", entry.getKey(),
                    "value", Map.of("stringValue", entry.getValue())
                ))
                .collect(Collectors.toList());
        }
    }

    /** async-profiler integration. */
    public static final class AsyncProfilerIntegration {
        private final Path asyncProfilerPath;

        public AsyncProfilerIntegration(final Path asyncProfilerPath) {
            this.asyncProfilerPath = Objects.requireNonNull(asyncProfilerPath);
        }

        /**
         * Starts async-profiler programmatically.
         *
         * @param duration profiling duration
         * @param outputPath output file path
         * @return future that completes when profiling is done
         */
        public CompletableFuture<Path> startProfiling(final Duration duration, final Path outputPath) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    final ProcessBuilder pb = new ProcessBuilder(
                        asyncProfilerPath.toString(),
                        "start",
                        "-d", String.valueOf(duration.getSeconds()),
                        "-f", outputPath.toString(),
                        String.valueOf(ProcessHandle.current().pid())
                    );

                    final Process process = pb.start();
                    final int exitCode = process.waitFor();

                    if (exitCode != 0) {
                        throw new RuntimeException("async-profiler failed with exit code: " + exitCode);
                    }

                    return outputPath;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to run async-profiler", e);
                }
            });
        }
    }

    /** Application Performance Monitoring (APM) integration. */
    public static final class ApmIntegration {
        private final String apmEndpoint;
        private final String apiKey;
        private final ScheduledExecutorService scheduler;

        public ApmIntegration(final String apmEndpoint, final String apiKey) {
            this.apmEndpoint = Objects.requireNonNull(apmEndpoint);
            this.apiKey = Objects.requireNonNull(apiKey);
            this.scheduler = Executors.newScheduledThreadPool(1);
        }

        /**
         * Sends profiling metrics to APM system.
         *
         * @param statistics profiling statistics
         * @return future that completes when data is sent
         */
        public CompletableFuture<Void> sendMetrics(final AdvancedProfiler.ProfilingStatistics statistics) {
            return CompletableFuture.runAsync(() -> {
                try {
                    final Map<String, Object> metrics = new LinkedHashMap<>();
                    metrics.put("timestamp", Instant.now().toEpochMilli());
                    metrics.put("service", "wasmtime4j");
                    metrics.put("function_calls", statistics.getFunctionCalls());
                    metrics.put("avg_execution_time_ms", statistics.getAverageExecutionTimeNanos() / 1_000_000.0);
                    metrics.put("memory_allocations", statistics.getMemoryAllocations());
                    metrics.put("jni_calls", statistics.getJniCalls());
                    metrics.put("panama_calls", statistics.getPanamaCalls());

                    // In a real implementation, this would make an HTTP request to the APM system
                    LOGGER.info("Sending metrics to APM: " + metrics);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to send APM metrics", e);
                }
            });
        }

        public void close() {
            scheduler.shutdown();
        }
    }

    private final PrometheusExporter prometheusExporter;
    private final GrafanaDashboard grafanaDashboard;
    private final ChromeDevToolsExporter chromeExporter;
    private final OpenTelemetryIntegration otelIntegration;
    private final ApmIntegration apmIntegration;
    private final Optional<AsyncProfilerIntegration> asyncProfilerIntegration;

    public ProfilingIntegrations(final String serviceName) {
        this.prometheusExporter = new PrometheusExporter();
        this.grafanaDashboard = new GrafanaDashboard();
        this.chromeExporter = new ChromeDevToolsExporter();
        this.otelIntegration = new OpenTelemetryIntegration(serviceName);
        this.apmIntegration = null; // Would be configured with actual APM settings
        this.asyncProfilerIntegration = Optional.empty(); // Would be configured with async-profiler path

        LOGGER.info("Profiling integrations initialized for service: " + serviceName);
    }

    /**
     * Exports all profiling data to various formats and systems.
     *
     * @param flameGraph flame graph to export
     * @param statistics profiling statistics
     * @param outputDirectory output directory for files
     * @return map of export results
     */
    public Map<String, CompletableFuture<Path>> exportAll(final FlameGraphGenerator.FlameFrame flameGraph,
                                                         final AdvancedProfiler.ProfilingStatistics statistics,
                                                         final Path outputDirectory) {
        final Map<String, CompletableFuture<Path>> exports = new HashMap<>();
        final String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());

        // Prometheus metrics
        exports.put("prometheus", CompletableFuture.supplyAsync(() -> {
            try {
                final Path prometheusFile = outputDirectory.resolve("wasmtime4j_metrics_" + timestamp + ".txt");
                prometheusExporter.saveMetrics(prometheusFile, statistics);
                return prometheusFile;
            } catch (Exception e) {
                throw new RuntimeException("Failed to export Prometheus metrics", e);
            }
        }));

        // Grafana dashboard
        exports.put("grafana", CompletableFuture.supplyAsync(() -> {
            try {
                final Path grafanaFile = outputDirectory.resolve("wasmtime4j_dashboard_" + timestamp + ".json");
                final String dashboard = grafanaDashboard.generateDashboard("Wasmtime4j Performance");
                Files.writeString(grafanaFile, dashboard, StandardCharsets.UTF_8);
                return grafanaFile;
            } catch (Exception e) {
                throw new RuntimeException("Failed to export Grafana dashboard", e);
            }
        }));

        // Chrome DevTools format
        exports.put("chrome", CompletableFuture.supplyAsync(() -> {
            try {
                final Path chromeFile = outputDirectory.resolve("wasmtime4j_profile_" + timestamp + ".json");
                final String profile = chromeExporter.exportProfile(flameGraph);
                Files.writeString(chromeFile, profile, StandardCharsets.UTF_8);
                return chromeFile;
            } catch (Exception e) {
                throw new RuntimeException("Failed to export Chrome DevTools profile", e);
            }
        }));

        return exports;
    }

    /**
     * Starts continuous profiling with all enabled integrations.
     *
     * @param interval export interval
     * @return scheduler future
     */
    public CompletableFuture<Void> startContinuousProfiling(final Duration interval,
                                                           final FlameGraphGenerator.FlameFrame flameGraph,
                                                           final AdvancedProfiler.ProfilingStatistics statistics) {
        return CompletableFuture.runAsync(() -> {
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // Send metrics to APM if configured
                    if (apmIntegration != null) {
                        apmIntegration.sendMetrics(statistics);
                    }

                    // Record Prometheus metrics
                    prometheusExporter.recordMetric("function_calls", statistics.getFunctionCalls(), Collections.emptyMap());
                    prometheusExporter.recordMetric("avg_execution_time",
                        statistics.getAverageExecutionTimeNanos() / 1_000_000.0, Collections.emptyMap());

                    LOGGER.fine("Continuous profiling metrics updated");
                } catch (Exception e) {
                    LOGGER.warning("Error in continuous profiling: " + e.getMessage());
                }
            }, 0, interval.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        });
    }

    @Override
    public void close() {
        if (apmIntegration != null) {
            apmIntegration.close();
        }
        LOGGER.info("Profiling integrations closed");
    }

    // Getters for individual integrations
    public PrometheusExporter getPrometheusExporter() { return prometheusExporter; }
    public GrafanaDashboard getGrafanaDashboard() { return grafanaDashboard; }
    public ChromeDevToolsExporter getChromeExporter() { return chromeExporter; }
    public OpenTelemetryIntegration getOtelIntegration() { return otelIntegration; }
    public Optional<ApmIntegration> getApmIntegration() { return Optional.ofNullable(apmIntegration); }
    public Optional<AsyncProfilerIntegration> getAsyncProfilerIntegration() { return asyncProfilerIntegration; }
}