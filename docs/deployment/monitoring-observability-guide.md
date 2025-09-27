# Monitoring and Observability Guide

This guide provides comprehensive monitoring and observability strategies for Wasmtime4j applications in production, covering metrics collection, logging, distributed tracing, alerting, and performance analysis.

## Table of Contents
- [Monitoring Architecture](#monitoring-architecture)
- [Metrics Collection](#metrics-collection)
- [Logging Strategy](#logging-strategy)
- [Distributed Tracing](#distributed-tracing)
- [Health Monitoring](#health-monitoring)
- [Alerting and Notifications](#alerting-and-notifications)
- [Performance Monitoring](#performance-monitoring)
- [Dashboards and Visualization](#dashboards-and-visualization)
- [Troubleshooting and Debugging](#troubleshooting-and-debugging)

## Monitoring Architecture

### Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  Wasmtime4j Application                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Metrics   │  │   Logging   │  │   Tracing   │        │
│  │ (Micrometer)│  │    (SLF4J)  │  │(OpenTelemetry)│        │
│  └─────┬───────┘  └─────┬───────┘  └─────┬───────┘        │
└────────┼──────────────────┼──────────────────┼──────────────┘
         │                  │                  │
         ▼                  ▼                  ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Prometheus  │    │   ELK/EFK   │    │   Jaeger    │
│   (Metrics) │    │  (Logging)  │    │  (Tracing)  │
└─────┬───────┘    └─────┬───────┘    └─────┬───────┘
      │                  │                  │
      ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    Grafana Dashboard                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │ Application │  │   System    │  │  Security   │        │
│  │  Metrics    │  │  Metrics    │  │   Events    │        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────┐
│ AlertManager│
│ (Alerting)  │
└─────────────┘
```

### Components Integration

```yaml
# monitoring-stack.yml
version: '3.8'
services:
  wasmtime4j-app:
    image: wasmtime4j:latest
    ports:
      - "8080:8080"    # Application
      - "8081:8081"    # Metrics
    environment:
      - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus,info
      - MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
      - OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger:14268/api/traces

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/var/lib/grafana/dashboards
      - ./grafana/provisioning:/etc/grafana/provisioning

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.15.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data

  logstash:
    image: docker.elastic.co/logstash/logstash:7.15.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:7.15.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI
      - "14268:14268"  # HTTP
    environment:
      - COLLECTOR_ZIPKIN_HTTP_PORT=9411

volumes:
  prometheus_data:
  grafana_data:
  elasticsearch_data:
```

## Metrics Collection

### Application Metrics Configuration

```java
@Configuration
@EnableMetrics
public class MetricsConfiguration {

    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Bean
    public WasmMetrics wasmMetrics(MeterRegistry meterRegistry) {
        return new WasmMetrics(meterRegistry);
    }

    @Component
    public static class WasmMetrics {

        private final MeterRegistry meterRegistry;
        private final Timer executionTimer;
        private final Timer compilationTimer;
        private final Counter errorCounter;
        private final Counter moduleLoadCounter;
        private final Gauge activeInstancesGauge;
        private final Gauge memoryUsageGauge;
        private final DistributionSummary moduleSizeDistribution;

        public WasmMetrics(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;

            // Execution metrics
            this.executionTimer = Timer.builder("wasm.execution.time")
                .description("WebAssembly function execution time")
                .tag("runtime", "wasmtime4j")
                .register(meterRegistry);

            this.compilationTimer = Timer.builder("wasm.compilation.time")
                .description("WebAssembly module compilation time")
                .register(meterRegistry);

            // Error tracking
            this.errorCounter = Counter.builder("wasm.errors")
                .description("WebAssembly execution errors")
                .register(meterRegistry);

            // Module metrics
            this.moduleLoadCounter = Counter.builder("wasm.modules.loaded")
                .description("Number of WebAssembly modules loaded")
                .register(meterRegistry);

            // Resource usage
            this.activeInstancesGauge = Gauge.builder("wasm.instances.active")
                .description("Number of active WebAssembly instances")
                .register(meterRegistry, this, WasmMetrics::getActiveInstances);

            this.memoryUsageGauge = Gauge.builder("wasm.memory.usage.bytes")
                .description("WebAssembly memory usage in bytes")
                .register(meterRegistry, this, WasmMetrics::getMemoryUsage);

            // Distribution metrics
            this.moduleSizeDistribution = DistributionSummary.builder("wasm.module.size.bytes")
                .description("Distribution of WebAssembly module sizes")
                .register(meterRegistry);
        }

        public Timer.Sample startExecution(String moduleId, String functionName) {
            return Timer.start(meterRegistry)
                .tag("module", moduleId)
                .tag("function", functionName);
        }

        public void recordExecution(Timer.Sample sample, String moduleId, String functionName,
                                  ExecutionOutcome outcome) {
            sample.stop(Timer.builder("wasm.function.execution.time")
                .tag("module", moduleId)
                .tag("function", functionName)
                .tag("outcome", outcome.name())
                .register(meterRegistry));
        }

        public void recordCompilation(String moduleId, Duration duration, boolean success) {
            compilationTimer.record(duration);

            Counter.builder("wasm.compilation.total")
                .tag("module", moduleId)
                .tag("success", String.valueOf(success))
                .register(meterRegistry)
                .increment();
        }

        public void recordError(String moduleId, String errorType, String errorCode) {
            errorCounter.increment(
                Tags.of(
                    "module", moduleId,
                    "error_type", errorType,
                    "error_code", errorCode
                )
            );
        }

        public void recordModuleLoad(String moduleId, long sizeBytes, ModuleTrust trustLevel) {
            moduleLoadCounter.increment(
                Tags.of(
                    "module", moduleId,
                    "trust_level", trustLevel.name()
                )
            );

            moduleSizeDistribution.record(sizeBytes);
        }

        // Gauge value providers
        private double getActiveInstances() {
            return wasmRuntimeManager.getActiveInstanceCount();
        }

        private double getMemoryUsage() {
            return wasmRuntimeManager.getTotalMemoryUsage();
        }
    }
}
```

### Custom Metrics Implementation

```java
@Component
public class AdvancedWasmMetrics {

    private final MeterRegistry meterRegistry;
    private final LongTaskTimer longRunningExecutions;
    private final Map<String, AtomicLong> moduleExecutionCounts = new ConcurrentHashMap<>();

    public AdvancedWasmMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Long-running executions
        this.longRunningExecutions = LongTaskTimer.builder("wasm.execution.long_running")
            .description("Long-running WebAssembly executions")
            .register(meterRegistry);

        // Module popularity
        Gauge.builder("wasm.module.execution.count")
            .description("Total executions per module")
            .tag("module", "dynamic")
            .register(meterRegistry, this, metrics ->
                moduleExecutionCounts.values().stream()
                    .mapToLong(AtomicLong::get)
                    .sum());

        // JVM metrics integration
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new FileDescriptorMetrics().bindTo(meterRegistry);
    }

    @EventListener
    public void handleExecutionStart(WasmExecutionStartEvent event) {
        // Track module popularity
        moduleExecutionCounts.computeIfAbsent(event.getModuleId(), k -> new AtomicLong(0))
                           .incrementAndGet();

        // Start long-running execution tracking if needed
        if (event.getExpectedDuration().compareTo(Duration.ofSeconds(5)) > 0) {
            LongTaskTimer.Sample sample = longRunningExecutions.start();
            event.setLongTaskSample(sample);
        }

        // Custom business metrics
        recordBusinessMetric(event);
    }

    @EventListener
    public void handleExecutionComplete(WasmExecutionCompleteEvent event) {
        // Stop long-running tracking
        if (event.getLongTaskSample() != null) {
            event.getLongTaskSample().stop();
        }

        // Record execution outcome metrics
        recordExecutionOutcome(event);

        // Performance analysis
        analyzePerformance(event);
    }

    private void recordBusinessMetric(WasmExecutionStartEvent event) {
        // Record business-specific metrics
        Counter.builder("wasm.business.operations")
            .tag("operation_type", event.getOperationType())
            .tag("customer_tier", event.getCustomerTier())
            .tag("region", event.getRegion())
            .register(meterRegistry)
            .increment();
    }

    private void recordExecutionOutcome(WasmExecutionCompleteEvent event) {
        Timer.builder("wasm.execution.duration")
            .tag("module", event.getModuleId())
            .tag("function", event.getFunctionName())
            .tag("outcome", event.getOutcome().name())
            .tag("runtime", event.getRuntimeType().name())
            .register(meterRegistry)
            .record(event.getDuration());

        // Resource consumption metrics
        Gauge.builder("wasm.execution.memory.peak")
            .tag("module", event.getModuleId())
            .register(meterRegistry, event, e -> e.getPeakMemoryUsage());

        Gauge.builder("wasm.execution.cpu.time")
            .tag("module", event.getModuleId())
            .register(meterRegistry, event, e -> e.getCpuTime().toMillis());
    }

    private void analyzePerformance(WasmExecutionCompleteEvent event) {
        Duration duration = event.getDuration();

        // Slow execution detection
        if (duration.compareTo(Duration.ofSeconds(10)) > 0) {
            Counter.builder("wasm.execution.slow")
                .tag("module", event.getModuleId())
                .tag("function", event.getFunctionName())
                .register(meterRegistry)
                .increment();
        }

        // Performance percentiles
        DistributionSummary.builder("wasm.execution.performance.score")
            .description("Performance score based on execution time vs expected")
            .tag("module", event.getModuleId())
            .register(meterRegistry)
            .record(calculatePerformanceScore(event));
    }

    private double calculatePerformanceScore(WasmExecutionCompleteEvent event) {
        Duration actual = event.getDuration();
        Duration expected = event.getExpectedDuration();

        if (expected.isZero()) {
            return 1.0;  // No baseline, assume good
        }

        double ratio = (double) actual.toMillis() / expected.toMillis();
        return Math.max(0, 2.0 - ratio);  // Score from 0-2, 1.0 = meeting expectations
    }
}
```

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "wasmtime4j_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # Wasmtime4j application
  - job_name: 'wasmtime4j'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['wasmtime4j-app:8081']
    scrape_interval: 10s
    scrape_timeout: 5s

  # JVM metrics
  - job_name: 'wasmtime4j-jvm'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['wasmtime4j-app:8081']
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'jvm_.*'
        action: keep

  # System metrics
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  # Kubernetes pods (if applicable)
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
```

### Recording Rules

```yaml
# wasmtime4j_rules.yml
groups:
  - name: wasmtime4j.rules
    rules:
      # Execution rate
      - record: wasm:execution_rate
        expr: rate(wasm_function_execution_time_count[5m])

      # Error rate
      - record: wasm:error_rate
        expr: rate(wasm_errors_total[5m])

      # Average execution time
      - record: wasm:execution_time_avg
        expr: rate(wasm_function_execution_time_sum[5m]) / rate(wasm_function_execution_time_count[5m])

      # P95 execution time
      - record: wasm:execution_time_p95
        expr: histogram_quantile(0.95, rate(wasm_function_execution_time_bucket[5m]))

      # Memory utilization
      - record: wasm:memory_utilization
        expr: wasm_memory_usage_bytes / wasm_memory_limit_bytes

      # Module popularity
      - record: wasm:module_popularity
        expr: increase(wasm_modules_loaded_total[1h])

      # System health score
      - record: wasm:health_score
        expr: |
          (
            (1 - wasm:error_rate / wasm:execution_rate) * 0.4 +
            (1 - wasm:memory_utilization) * 0.3 +
            (wasm:execution_time_avg < 1000) * 0.3
          )
```

## Logging Strategy

### Structured Logging Configuration

```java
@Configuration
public class LoggingConfiguration {

    @Bean
    public Logger structuredLogger() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // JSON encoder for structured logs
        JsonEncoder jsonEncoder = new JsonEncoder();
        jsonEncoder.setContext(context);
        jsonEncoder.start();

        // Console appender with JSON format
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setEncoder(jsonEncoder);
        consoleAppender.start();

        // File appender for application logs
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setFile("/opt/wasmtime4j/logs/application.log");
        fileAppender.setEncoder(jsonEncoder);
        fileAppender.start();

        // Security log appender
        FileAppender<ILoggingEvent> securityAppender = new FileAppender<>();
        securityAppender.setContext(context);
        securityAppender.setFile("/opt/wasmtime4j/logs/security.log");
        securityAppender.setEncoder(jsonEncoder);
        securityAppender.start();

        // Configure root logger
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);
        rootLogger.addAppender(fileAppender);
        rootLogger.setLevel(Level.INFO);

        // Configure security logger
        ch.qos.logback.classic.Logger securityLogger = context.getLogger("SECURITY");
        securityLogger.addAppender(securityAppender);
        securityLogger.setLevel(Level.INFO);
        securityLogger.setAdditive(false);

        return rootLogger;
    }
}
```

### Comprehensive Logging Service

```java
@Component
public class WasmLoggingService {

    private static final Logger log = LoggerFactory.getLogger(WasmLoggingService.class);
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");
    private static final Logger performanceLog = LoggerFactory.getLogger("PERFORMANCE");
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final ObjectMapper objectMapper;
    private final TraceContext traceContext;

    public void logExecutionStart(String moduleId, String functionName,
                                Map<String, Object> parameters) {
        Map<String, Object> logData = Map.of(
            "event", "execution_start",
            "timestamp", Instant.now().toString(),
            "module_id", moduleId,
            "function_name", functionName,
            "parameter_count", parameters.size(),
            "trace_id", traceContext.getTraceId(),
            "span_id", traceContext.getSpanId(),
            "user_id", getCurrentUserId(),
            "session_id", getCurrentSessionId()
        );

        log.info("WebAssembly execution started: {}", toJson(logData));
    }

    public void logExecutionComplete(String moduleId, String functionName,
                                   Duration executionTime, ExecutionOutcome outcome,
                                   long memoryUsed, Map<String, Object> metadata) {
        Map<String, Object> logData = Map.of(
            "event", "execution_complete",
            "timestamp", Instant.now().toString(),
            "module_id", moduleId,
            "function_name", functionName,
            "execution_time_ms", executionTime.toMillis(),
            "outcome", outcome.name(),
            "memory_used_bytes", memoryUsed,
            "trace_id", traceContext.getTraceId(),
            "span_id", traceContext.getSpanId(),
            "metadata", metadata
        );

        log.info("WebAssembly execution completed: {}", toJson(logData));

        // Performance logging for slow executions
        if (executionTime.compareTo(Duration.ofSeconds(5)) > 0) {
            logSlowExecution(moduleId, functionName, executionTime, metadata);
        }
    }

    public void logError(String moduleId, String functionName, Exception error,
                        Map<String, Object> context) {
        Map<String, Object> logData = Map.of(
            "event", "execution_error",
            "timestamp", Instant.now().toString(),
            "module_id", moduleId,
            "function_name", functionName,
            "error_type", error.getClass().getSimpleName(),
            "error_message", error.getMessage(),
            "trace_id", traceContext.getTraceId(),
            "span_id", traceContext.getSpanId(),
            "context", context,
            "stack_trace", getStackTrace(error)
        );

        log.error("WebAssembly execution error: {}", toJson(logData));
    }

    public void logSecurityEvent(SecurityEventType eventType, String description,
                                Map<String, Object> details) {
        Map<String, Object> logData = Map.of(
            "event", "security_event",
            "timestamp", Instant.now().toString(),
            "event_type", eventType.name(),
            "description", description,
            "details", details,
            "source_ip", getCurrentSourceIp(),
            "user_agent", getCurrentUserAgent(),
            "user_id", getCurrentUserId(),
            "session_id", getCurrentSessionId(),
            "trace_id", traceContext.getTraceId()
        );

        securityLog.warn("Security event: {}", toJson(logData));
    }

    public void logPerformanceMetrics(String moduleId, PerformanceMetrics metrics) {
        Map<String, Object> logData = Map.of(
            "event", "performance_metrics",
            "timestamp", Instant.now().toString(),
            "module_id", moduleId,
            "metrics", Map.of(
                "compilation_time_ms", metrics.getCompilationTime().toMillis(),
                "instantiation_time_ms", metrics.getInstantiationTime().toMillis(),
                "execution_time_ms", metrics.getExecutionTime().toMillis(),
                "memory_usage_bytes", metrics.getMemoryUsage(),
                "cpu_time_ms", metrics.getCpuTime().toMillis(),
                "gc_count", metrics.getGcCount(),
                "gc_time_ms", metrics.getGcTime().toMillis()
            ),
            "trace_id", traceContext.getTraceId()
        );

        performanceLog.info("Performance metrics: {}", toJson(logData));
    }

    public void logAuditEvent(AuditEventType eventType, String resource,
                             String action, AuditOutcome outcome,
                             Map<String, Object> details) {
        Map<String, Object> logData = Map.of(
            "event", "audit_event",
            "timestamp", Instant.now().toString(),
            "event_type", eventType.name(),
            "resource", resource,
            "action", action,
            "outcome", outcome.name(),
            "user_id", getCurrentUserId(),
            "source_ip", getCurrentSourceIp(),
            "session_id", getCurrentSessionId(),
            "details", details,
            "trace_id", traceContext.getTraceId()
        );

        auditLog.info("Audit event: {}", toJson(logData));
    }

    private void logSlowExecution(String moduleId, String functionName,
                                Duration executionTime, Map<String, Object> metadata) {
        Map<String, Object> logData = Map.of(
            "event", "slow_execution",
            "timestamp", Instant.now().toString(),
            "module_id", moduleId,
            "function_name", functionName,
            "execution_time_ms", executionTime.toMillis(),
            "metadata", metadata,
            "trace_id", traceContext.getTraceId(),
            "recommendations", generatePerformanceRecommendations(executionTime, metadata)
        );

        performanceLog.warn("Slow WebAssembly execution: {}", toJson(logData));
    }

    private List<String> generatePerformanceRecommendations(Duration executionTime,
                                                           Map<String, Object> metadata) {
        List<String> recommendations = new ArrayList<>();

        if (executionTime.compareTo(Duration.ofSeconds(10)) > 0) {
            recommendations.add("Consider breaking large operations into smaller chunks");
        }

        Long memoryUsed = (Long) metadata.get("memory_used_bytes");
        if (memoryUsed != null && memoryUsed > 100 * 1024 * 1024) {  // 100MB
            recommendations.add("High memory usage detected, consider memory optimization");
        }

        return recommendations;
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return object.toString();
        }
    }

    private String getStackTrace(Exception error) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }
}
```

### Logstash Configuration

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
  file {
    path => "/opt/wasmtime4j/logs/*.log"
    start_position => "beginning"
    codec => "json"
    type => "wasmtime4j"
  }
}

filter {
  if [type] == "wasmtime4j" {
    # Parse timestamp
    date {
      match => [ "timestamp", "ISO8601" ]
    }

    # Extract trace information
    if [trace_id] {
      mutate {
        add_field => { "trace_url" => "http://jaeger:16686/trace/%{trace_id}" }
      }
    }

    # Categorize events
    if [event] == "execution_start" or [event] == "execution_complete" {
      mutate { add_tag => ["execution"] }
    }

    if [event] == "execution_error" {
      mutate { add_tag => ["error"] }
    }

    if [event] == "security_event" {
      mutate { add_tag => ["security"] }
    }

    if [event] == "performance_metrics" {
      mutate { add_tag => ["performance"] }
    }

    # Extract performance metrics
    if [execution_time_ms] {
      if [execution_time_ms] > 5000 {
        mutate { add_tag => ["slow_execution"] }
      }
    }

    # GeoIP for source IPs
    if [source_ip] {
      geoip {
        source => "source_ip"
        target => "geoip"
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "wasmtime4j-%{+YYYY.MM.dd}"
  }

  # Send critical events to separate index
  if "security" in [tags] or "error" in [tags] {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "wasmtime4j-alerts-%{+YYYY.MM.dd}"
    }
  }
}
```

## Distributed Tracing

### OpenTelemetry Configuration

```java
@Configuration
@EnableAutoConfiguration
public class TracingConfiguration {

    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(TracerSdkProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(
                    JaegerGrpcSpanExporter.builder()
                        .setEndpoint("http://jaeger:14250")
                        .build())
                    .build())
                .setResource(Resource.getDefault()
                    .merge(Resource.of(ResourceAttributes.SERVICE_NAME, "wasmtime4j"))
                    .merge(Resource.of(ResourceAttributes.SERVICE_VERSION, "1.0.0")))
                .build())
            .buildAndRegisterGlobal();
    }

    @Bean
    public WasmTracingService wasmTracingService(OpenTelemetry openTelemetry) {
        return new WasmTracingService(openTelemetry.getTracer("wasmtime4j"));
    }
}
```

### Tracing Service Implementation

```java
@Component
public class WasmTracingService {

    private final Tracer tracer;
    private final Map<String, Span> activeSpans = new ConcurrentHashMap<>();

    public WasmTracingService(Tracer tracer) {
        this.tracer = tracer;
    }

    public Span startExecutionSpan(String moduleId, String functionName,
                                  Map<String, Object> parameters) {
        Span span = tracer.spanBuilder("wasm.execution")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("wasm.module.id", moduleId)
            .setAttribute("wasm.function.name", functionName)
            .setAttribute("wasm.parameter.count", parameters.size())
            .setAttribute("wasm.runtime", getRuntimeType())
            .startSpan();

        // Add parameters as attributes (with size limits)
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = "wasm.param." + entry.getKey();
            String value = truncateValue(entry.getValue().toString(), 256);
            span.setAttribute(key, value);
        }

        activeSpans.put(span.getSpanContext().getSpanId(), span);
        return span;
    }

    public Span startCompilationSpan(String moduleId, long moduleSize) {
        return tracer.spanBuilder("wasm.compilation")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("wasm.module.id", moduleId)
            .setAttribute("wasm.module.size.bytes", moduleSize)
            .setAttribute("wasm.runtime", getRuntimeType())
            .startSpan();
    }

    public Span startInstantiationSpan(String moduleId) {
        return tracer.spanBuilder("wasm.instantiation")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("wasm.module.id", moduleId)
            .setAttribute("wasm.runtime", getRuntimeType())
            .startSpan();
    }

    public void recordExecutionMetrics(Span span, ExecutionMetrics metrics) {
        span.setAttribute("wasm.execution.time.ms", metrics.getExecutionTime().toMillis());
        span.setAttribute("wasm.memory.used.bytes", metrics.getMemoryUsed());
        span.setAttribute("wasm.memory.peak.bytes", metrics.getPeakMemoryUsed());
        span.setAttribute("wasm.cpu.time.ms", metrics.getCpuTime().toMillis());
        span.setAttribute("wasm.gc.count", metrics.getGcCount());
        span.setAttribute("wasm.gc.time.ms", metrics.getGcTime().toMillis());
    }

    public void recordError(Span span, Exception error) {
        span.setStatus(StatusCode.ERROR, error.getMessage());
        span.recordException(error);
        span.setAttribute("wasm.error.type", error.getClass().getSimpleName());

        if (error instanceof WasmException) {
            WasmException wasmError = (WasmException) error;
            span.setAttribute("wasm.error.code", wasmError.getErrorCode());
            span.setAttribute("wasm.error.category", wasmError.getCategory());
        }
    }

    public void finishSpan(Span span, ExecutionOutcome outcome) {
        span.setAttribute("wasm.execution.outcome", outcome.name());

        if (outcome == ExecutionOutcome.SUCCESS) {
            span.setStatus(StatusCode.OK);
        } else {
            span.setStatus(StatusCode.ERROR, "Execution failed: " + outcome);
        }

        span.end();
        activeSpans.remove(span.getSpanContext().getSpanId());
    }

    public void addCustomEvent(Span span, String eventName, Map<String, Object> attributes) {
        AttributesBuilder builder = Attributes.builder();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getValue() instanceof String) {
                builder.put(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Long) {
                builder.put(entry.getKey(), (Long) entry.getValue());
            } else if (entry.getValue() instanceof Double) {
                builder.put(entry.getKey(), (Double) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                builder.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }

        span.addEvent(eventName, builder.build());
    }

    @EventListener
    public void handleExecutionStart(WasmExecutionStartEvent event) {
        Span span = startExecutionSpan(event.getModuleId(), event.getFunctionName(),
                                     event.getParameters());
        event.setTraceSpan(span);
    }

    @EventListener
    public void handleExecutionComplete(WasmExecutionCompleteEvent event) {
        Span span = event.getTraceSpan();
        if (span != null) {
            recordExecutionMetrics(span, event.getMetrics());
            finishSpan(span, event.getOutcome());
        }
    }

    private String getRuntimeType() {
        return wasmRuntimeManager.getRuntimeType().name();
    }

    private String truncateValue(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
```

## Health Monitoring

### Comprehensive Health Checks

```java
@Component
public class WasmHealthIndicator implements HealthIndicator {

    private final WasmRuntime runtime;
    private final ModuleManager moduleManager;
    private final MetricsCollector metricsCollector;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            // Runtime health
            HealthStatus runtimeHealth = checkRuntimeHealth();
            builder.withDetail("runtime", runtimeHealth);

            // Module system health
            HealthStatus moduleHealth = checkModuleHealth();
            builder.withDetail("modules", moduleHealth);

            // Performance health
            HealthStatus performanceHealth = checkPerformanceHealth();
            builder.withDetail("performance", performanceHealth);

            // Resource health
            HealthStatus resourceHealth = checkResourceHealth();
            builder.withDetail("resources", resourceHealth);

            // Security health
            HealthStatus securityHealth = checkSecurityHealth();
            builder.withDetail("security", securityHealth);

            // Overall status
            if (allHealthy(runtimeHealth, moduleHealth, performanceHealth,
                          resourceHealth, securityHealth)) {
                return builder.up().build();
            } else if (anyCritical(runtimeHealth, moduleHealth, performanceHealth,
                                  resourceHealth, securityHealth)) {
                return builder.down().build();
            } else {
                return builder.status("DEGRADED").build();
            }

        } catch (Exception e) {
            return builder.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }

    private HealthStatus checkRuntimeHealth() {
        if (!runtime.isValid()) {
            return HealthStatus.critical("Runtime is invalid");
        }

        try {
            // Test basic functionality
            Engine engine = runtime.createEngine();
            byte[] testModule = createHealthCheckModule();
            Module module = runtime.compileModule(engine, testModule);
            Instance instance = runtime.instantiate(module);

            // Execute health check function
            WasmFunction healthCheck = instance.getFunction("health_check");
            long startTime = System.nanoTime();
            WasmValue[] result = healthCheck.call();
            long duration = System.nanoTime() - startTime;

            if (result[0].asI32() != 42) {
                return HealthStatus.unhealthy("Health check function returned incorrect value");
            }

            if (duration > Duration.ofMillis(100).toNanos()) {
                return HealthStatus.degraded("Health check took too long: " +
                                           Duration.ofNanos(duration).toMillis() + "ms");
            }

            return HealthStatus.healthy("Runtime operational")
                .withDetail("test_duration_ms", Duration.ofNanos(duration).toMillis())
                .withDetail("runtime_type", runtime.getRuntimeInfo().getRuntimeType());

        } catch (Exception e) {
            return HealthStatus.critical("Runtime test failed: " + e.getMessage());
        }
    }

    private HealthStatus checkModuleHealth() {
        ModuleSystemStatus status = moduleManager.getSystemStatus();

        if (status.getTotalModules() == 0) {
            return HealthStatus.degraded("No modules loaded");
        }

        if (status.getFailedModules() > 0) {
            double failureRate = (double) status.getFailedModules() / status.getTotalModules();
            if (failureRate > 0.1) {  // 10% failure rate
                return HealthStatus.unhealthy("High module failure rate: " +
                                            (failureRate * 100) + "%");
            }
        }

        return HealthStatus.healthy("Module system operational")
            .withDetail("total_modules", status.getTotalModules())
            .withDetail("loaded_modules", status.getLoadedModules())
            .withDetail("failed_modules", status.getFailedModules())
            .withDetail("cache_hit_rate", status.getCacheHitRate());
    }

    private HealthStatus checkPerformanceHealth() {
        PerformanceMetrics metrics = metricsCollector.getRecentMetrics(Duration.ofMinutes(5));

        // Check average execution time
        if (metrics.getAverageExecutionTime().compareTo(Duration.ofSeconds(5)) > 0) {
            return HealthStatus.degraded("High average execution time: " +
                                       metrics.getAverageExecutionTime().toMillis() + "ms");
        }

        // Check error rate
        double errorRate = metrics.getErrorRate();
        if (errorRate > 0.05) {  // 5% error rate
            return HealthStatus.unhealthy("High error rate: " + (errorRate * 100) + "%");
        }

        // Check throughput
        if (metrics.getThroughput() < 10) {  // Less than 10 ops/sec
            return HealthStatus.degraded("Low throughput: " + metrics.getThroughput() + " ops/sec");
        }

        return HealthStatus.healthy("Performance within acceptable limits")
            .withDetail("avg_execution_time_ms", metrics.getAverageExecutionTime().toMillis())
            .withDetail("error_rate", errorRate)
            .withDetail("throughput_ops_per_sec", metrics.getThroughput());
    }

    private HealthStatus checkResourceHealth() {
        ResourceUsage usage = resourceMonitor.getCurrentUsage();

        // Memory check
        double memoryUsage = usage.getMemoryUsagePercentage();
        if (memoryUsage > 0.9) {  // 90%
            return HealthStatus.critical("Critical memory usage: " + (memoryUsage * 100) + "%");
        } else if (memoryUsage > 0.8) {  // 80%
            return HealthStatus.degraded("High memory usage: " + (memoryUsage * 100) + "%");
        }

        // CPU check
        double cpuUsage = usage.getCpuUsagePercentage();
        if (cpuUsage > 0.9) {  // 90%
            return HealthStatus.degraded("High CPU usage: " + (cpuUsage * 100) + "%");
        }

        // Disk check
        double diskUsage = usage.getDiskUsagePercentage();
        if (diskUsage > 0.9) {  // 90%
            return HealthStatus.degraded("High disk usage: " + (diskUsage * 100) + "%");
        }

        return HealthStatus.healthy("Resource usage within limits")
            .withDetail("memory_usage_percent", memoryUsage * 100)
            .withDetail("cpu_usage_percent", cpuUsage * 100)
            .withDetail("disk_usage_percent", diskUsage * 100);
    }

    private HealthStatus checkSecurityHealth() {
        SecurityStatus status = securityMonitor.getSecurityStatus();

        if (status.hasCriticalThreats()) {
            return HealthStatus.critical("Critical security threats detected");
        }

        if (status.hasActiveIncidents()) {
            return HealthStatus.degraded("Active security incidents: " +
                                       status.getActiveIncidentCount());
        }

        return HealthStatus.healthy("No security issues detected")
            .withDetail("threat_level", status.getThreatLevel())
            .withDetail("last_scan", status.getLastScanTime())
            .withDetail("blocked_attempts", status.getBlockedAttempts());
    }

    private static class HealthStatus {
        private final Status status;
        private final String message;
        private final Map<String, Object> details;

        private HealthStatus(Status status, String message) {
            this.status = status;
            this.message = message;
            this.details = new HashMap<>();
        }

        public static HealthStatus healthy(String message) {
            return new HealthStatus(Status.HEALTHY, message);
        }

        public static HealthStatus degraded(String message) {
            return new HealthStatus(Status.DEGRADED, message);
        }

        public static HealthStatus unhealthy(String message) {
            return new HealthStatus(Status.UNHEALTHY, message);
        }

        public static HealthStatus critical(String message) {
            return new HealthStatus(Status.CRITICAL, message);
        }

        public HealthStatus withDetail(String key, Object value) {
            details.put(key, value);
            return this;
        }

        public enum Status {
            HEALTHY, DEGRADED, UNHEALTHY, CRITICAL
        }
    }
}
```

## Alerting and Notifications

### Alert Rules Configuration

```yaml
# wasmtime4j_alerts.yml
groups:
  - name: wasmtime4j.alerts
    rules:
      # High error rate
      - alert: WasmHighErrorRate
        expr: wasm:error_rate > 0.05
        for: 2m
        labels:
          severity: warning
          service: wasmtime4j
        annotations:
          summary: "High WebAssembly error rate"
          description: "Error rate is {{ $value | humanizePercentage }} for the last 2 minutes"

      # Slow execution times
      - alert: WasmSlowExecution
        expr: wasm:execution_time_p95 > 5000
        for: 5m
        labels:
          severity: warning
          service: wasmtime4j
        annotations:
          summary: "Slow WebAssembly execution times"
          description: "95th percentile execution time is {{ $value }}ms"

      # Memory usage critical
      - alert: WasmMemoryCritical
        expr: wasm:memory_utilization > 0.9
        for: 1m
        labels:
          severity: critical
          service: wasmtime4j
        annotations:
          summary: "Critical WebAssembly memory usage"
          description: "Memory utilization is {{ $value | humanizePercentage }}"

      # Service down
      - alert: WasmServiceDown
        expr: up{job="wasmtime4j"} == 0
        for: 1m
        labels:
          severity: critical
          service: wasmtime4j
        annotations:
          summary: "WebAssembly service is down"
          description: "Wasmtime4j service has been down for more than 1 minute"

      # Security incidents
      - alert: WasmSecurityIncident
        expr: increase(security_events_total{severity="critical"}[5m]) > 0
        for: 0m
        labels:
          severity: critical
          service: wasmtime4j
          team: security
        annotations:
          summary: "Critical security incident detected"
          description: "{{ $value }} critical security events in the last 5 minutes"

      # Module loading failures
      - alert: WasmModuleLoadFailure
        expr: increase(wasm_module_load_failures_total[10m]) > 5
        for: 2m
        labels:
          severity: warning
          service: wasmtime4j
        annotations:
          summary: "Multiple WebAssembly module load failures"
          description: "{{ $value }} module load failures in the last 10 minutes"
```

### AlertManager Configuration

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'wasmtime4j-alerts@company.com'

templates:
  - '/etc/alertmanager/templates/*.tmpl'

route:
  group_by: ['alertname', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
    - match:
        team: security
      receiver: 'security-team'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://slack-webhook:8080/alerts'

  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@company.com'
        subject: 'CRITICAL: {{ .GroupLabels.service }} Alert'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
    webhook_configs:
      - url: 'http://pagerduty-webhook:8080/critical'
        send_resolved: true

  - name: 'security-team'
    email_configs:
      - to: 'security@company.com'
        subject: 'SECURITY: {{ .GroupLabels.service }} Security Event'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/...'
        channel: '#security-alerts'
        title: 'Security Alert: {{ .GroupLabels.service }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

This comprehensive monitoring and observability guide provides the foundation for understanding and maintaining Wasmtime4j applications in production environments, enabling proactive issue detection and resolution.