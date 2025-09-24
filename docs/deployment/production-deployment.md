# Production Deployment Guide

This guide provides comprehensive instructions for deploying Wasmtime4j in production environments with enterprise-grade reliability, security, and observability.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Production Configuration](#production-configuration)
- [Deployment Architectures](#deployment-architectures)
- [Container Deployment](#container-deployment)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Monitoring and Observability](#monitoring-and-observability)
- [Security Configuration](#security-configuration)
- [Performance Optimization](#performance-optimization)
- [High Availability](#high-availability)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements

**Minimum Requirements:**
- **CPU**: 2 cores, x86_64 or ARM64
- **Memory**: 2GB RAM (4GB+ recommended)
- **Storage**: 1GB available space
- **Network**: Outbound HTTPS (for dependency downloads)

**Recommended Production Requirements:**
- **CPU**: 4+ cores with SMT/Hyperthreading
- **Memory**: 8GB+ RAM
- **Storage**: 10GB+ SSD storage
- **Network**: Load balancer with health checks

### Software Dependencies

**Java Runtime:**
- **JDK 23+**: Recommended for Panama FFI (optimal performance)
- **JDK 8+**: Minimum supported version (JNI runtime)

**Operating Systems:**
- **Linux**: Ubuntu 20.04+, RHEL 8+, Amazon Linux 2
- **macOS**: 11.0+ (Big Sur), both Intel and Apple Silicon
- **Windows**: Windows 10/Server 2019+ (x64)

**Container Platforms:**
- **Docker**: 20.10+
- **Kubernetes**: 1.21+
- **OpenShift**: 4.8+

## Production Configuration

### JVM Configuration

```bash
# Optimal JVM settings for production
export JAVA_OPTS="
  -server
  -XX:+UseG1GC
  -XX:+UseContainerSupport
  -XX:MaxRAMPercentage=75.0
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -XX:+UseTransparentHugePages
  -XX:+UseLargePages
  -Djava.security.egd=file:/dev/./urandom
  -Dfile.encoding=UTF-8
  -Duser.timezone=UTC
"

# For containerized environments
export JAVA_OPTS="$JAVA_OPTS
  -XX:+ExitOnOutOfMemoryError
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/wasmtime4j/
"

# For Panama FFI (Java 23+)
export JAVA_OPTS="$JAVA_OPTS
  --enable-preview
  --enable-native-access=ALL-UNNAMED
"
```

### Wasmtime4j Configuration

```properties
# Application configuration (application.properties)

# Runtime Selection
wasmtime4j.runtime=auto
wasmtime4j.runtime.fallback=true

# Performance Settings
wasmtime4j.engine.optimization-level=speed
wasmtime4j.engine.debug-info=false
wasmtime4j.engine.profiling=false

# Memory Management
wasmtime4j.memory.pool.enabled=true
wasmtime4j.memory.pool.max-size=256MB
wasmtime4j.memory.pool.cleanup-interval=300

# Module Caching
wasmtime4j.cache.enabled=true
wasmtime4j.cache.size=512MB
wasmtime4j.cache.ttl=3600

# Monitoring
wasmtime4j.metrics.enabled=true
wasmtime4j.metrics.jvm=true
wasmtime4j.metrics.native=true

# Logging
wasmtime4j.logging.level=INFO
wasmtime4j.logging.native=false
```

### Security Configuration

```properties
# Security settings
wasmtime4j.security.sandbox.enabled=true
wasmtime4j.security.wasi.filesystem.enabled=false
wasmtime4j.security.wasi.networking.enabled=false
wasmtime4j.security.module.verification=strict

# Resource Limits
wasmtime4j.limits.memory.max=1GB
wasmtime4j.limits.execution.timeout=30000
wasmtime4j.limits.compilation.timeout=10000
```

## Deployment Architectures

### Single Instance Deployment

```
┌─────────────────┐
│  Load Balancer  │
└─────────┬───────┘
          │
┌─────────▼───────┐
│   Application   │
│   + Wasmtime4j  │
└─────────────────┘
```

**Use Cases:**
- Development/staging environments
- Small to medium workloads
- Proof of concept deployments

**Configuration:**
```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    image: your-app:latest
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Xmx2g -XX:+UseG1GC
    volumes:
      - ./wasm-modules:/app/wasm:ro
    restart: unless-stopped
```

### High Availability Deployment

```
┌─────────────────┐
│  Load Balancer  │
└─────┬───┬───┬───┘
      │   │   │
┌─────▼─┐ │ ┌─▼─────┐
│ App 1 │ │ │ App N │
└───────┘ │ └───────┘
          │
      ┌───▼───┐
      │ App 2 │
      └───────┘
```

**Use Cases:**
- Production environments
- High availability requirements
- Horizontal scaling needs

## Container Deployment

### Docker Image

Build production-optimized image:

```dockerfile
# Dockerfile.production
FROM eclipse-temurin:23-jdk-jammy as builder

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl git build-essential pkg-config libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Install Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"

WORKDIR /build
COPY . .
RUN ./mvnw clean package -P release -DskipTests -q

# Production runtime image
FROM eclipse-temurin:23-jre-jammy

# Create application user
RUN groupadd -r wasmtime4j && useradd -r -g wasmtime4j wasmtime4j

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    ca-certificates tini \
    && rm -rf /var/lib/apt/lists/*

# Copy application
COPY --from=builder /build/target/*.jar /app/
COPY --from=builder /build/wasmtime4j-*/target/*.jar /app/
COPY --from=builder /build/wasmtime4j-native/target/classes/native/ /app/native/

# Create directories
RUN mkdir -p /app/logs /app/wasm /app/config /app/cache \
    && chown -R wasmtime4j:wasmtime4j /app

USER wasmtime4j
WORKDIR /app

# Environment configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV WASMTIME4J_NATIVE_PATH="/app/native"
ENV WASMTIME4J_CACHE_DIR="/app/cache"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD java -cp "/app/*" ai.tegmentum.wasmtime4j.util.HealthCheck

# Use tini for signal handling
ENTRYPOINT ["tini", "--"]
CMD ["java", "-cp", "/app/*", "your.main.Application"]
```

### Build and Deploy

```bash
# Build production image
docker build -f Dockerfile.production -t wasmtime4j-app:production .

# Run with production configuration
docker run -d \
  --name wasmtime4j-prod \
  --restart unless-stopped \
  -p 8080:8080 \
  -v $(pwd)/wasm-modules:/app/wasm:ro \
  -v wasmtime4j-logs:/app/logs \
  -v wasmtime4j-cache:/app/cache \
  -e JAVA_OPTS="-Xmx4g -XX:+UseG1GC" \
  wasmtime4j-app:production
```

## Kubernetes Deployment

### Base Configuration

```yaml
# wasmtime4j-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wasmtime4j
  labels:
    app: wasmtime4j
    version: v1.0.0
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: wasmtime4j
  template:
    metadata:
      labels:
        app: wasmtime4j
        version: v1.0.0
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/metrics"
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
      - name: wasmtime4j
        image: wasmtime4j-app:production
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: JAVA_OPTS
          value: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
        - name: WASMTIME4J_LOG_LEVEL
          valueFrom:
            configMapKeyRef:
              name: wasmtime4j-config
              key: log.level
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2"
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
        startupProbe:
          httpGet:
            path: /health/startup
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
          failureThreshold: 30
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
        - name: wasm-modules
          mountPath: /app/wasm
          readOnly: true
        - name: cache
          mountPath: /app/cache
        - name: logs
          mountPath: /app/logs
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          runAsNonRoot: true
          capabilities:
            drop: [ALL]
      volumes:
      - name: config
        configMap:
          name: wasmtime4j-config
      - name: wasm-modules
        configMap:
          name: wasmtime4j-wasm-modules
      - name: cache
        emptyDir:
          sizeLimit: 1Gi
      - name: logs
        emptyDir:
          sizeLimit: 2Gi
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app: wasmtime4j
              topologyKey: kubernetes.io/hostname
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: wasmtime4j-config
data:
  log.level: "INFO"
  cache.size: "512MB"
  metrics.enabled: "true"
---
apiVersion: v1
kind: Service
metadata:
  name: wasmtime4j
  labels:
    app: wasmtime4j
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
spec:
  selector:
    app: wasmtime4j
  ports:
  - port: 80
    targetPort: 8080
    name: http
```

### Deploy to Kubernetes

```bash
# Deploy application
kubectl apply -f wasmtime4j-deployment.yaml

# Verify deployment
kubectl get deployments wasmtime4j
kubectl get pods -l app=wasmtime4j

# Check logs
kubectl logs -l app=wasmtime4j --tail=100

# Test service
kubectl port-forward service/wasmtime4j 8080:80
curl http://localhost:8080/health
```

## Monitoring and Observability

### Prometheus Metrics

```yaml
# prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
    - job_name: 'wasmtime4j'
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
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

### Grafana Dashboard

Key metrics to monitor:

```json
{
  "dashboard": {
    "title": "Wasmtime4j Production Dashboard",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(wasmtime4j_requests_total[5m])"
          }
        ]
      },
      {
        "title": "Response Time",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, wasmtime4j_request_duration_seconds_bucket)"
          }
        ]
      },
      {
        "title": "WebAssembly Module Compilation Time",
        "targets": [
          {
            "expr": "wasmtime4j_module_compilation_duration_seconds"
          }
        ]
      },
      {
        "title": "Memory Usage",
        "targets": [
          {
            "expr": "wasmtime4j_memory_usage_bytes"
          }
        ]
      }
    ]
  }
}
```

### Alerting Rules

```yaml
# alerting-rules.yaml
groups:
- name: wasmtime4j
  rules:
  - alert: Wasmtime4jHighErrorRate
    expr: rate(wasmtime4j_errors_total[5m]) > 0.1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High error rate in Wasmtime4j"
      description: "Error rate is {{ $value }} errors per second"

  - alert: Wasmtime4jHighMemoryUsage
    expr: wasmtime4j_memory_usage_bytes / wasmtime4j_memory_limit_bytes > 0.9
    for: 10m
    labels:
      severity: critical
    annotations:
      summary: "High memory usage in Wasmtime4j"
      description: "Memory usage is {{ $value | humanizePercentage }} of limit"
```

## Security Configuration

### Network Security

```yaml
# network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: wasmtime4j-netpol
spec:
  podSelector:
    matchLabels:
      app: wasmtime4j
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-system
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to: []
    ports:
    - protocol: TCP
      port: 443  # HTTPS only
```

### Pod Security

```yaml
# pod-security-policy.yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: wasmtime4j-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

## Performance Optimization

### JVM Tuning

```bash
# Large heap applications (8GB+)
export JAVA_OPTS="
  -Xms4g -Xmx8g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:G1HeapRegionSize=16m
  -XX:+UseStringDeduplication
"

# High throughput applications
export JAVA_OPTS="
  -XX:+UseParallelGC
  -XX:+UseParallelOldGC
  -XX:ParallelGCThreads=8
  -XX:+AggressiveOpts
"

# Low latency applications
export JAVA_OPTS="
  -XX:+UseZGC
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseTransparentHugePages
"
```

### Wasmtime4j Optimization

```properties
# High performance configuration
wasmtime4j.engine.optimization-level=speed
wasmtime4j.engine.parallel-compilation=true
wasmtime4j.memory.pool.enabled=true
wasmtime4j.memory.pool.prealloc=256MB
wasmtime4j.cache.compilation.enabled=true
wasmtime4j.cache.compilation.size=1GB
```

## High Availability

### Load Balancing

```yaml
# haproxy.cfg
global
    daemon
    maxconn 4096

defaults
    mode http
    timeout connect 5000ms
    timeout client 50000ms
    timeout server 50000ms

frontend wasmtime4j_frontend
    bind *:80
    default_backend wasmtime4j_backend

backend wasmtime4j_backend
    balance roundrobin
    option httpchk GET /health/ready
    server app1 app1:8080 check
    server app2 app2:8080 check
    server app3 app3:8080 check
```

### Circuit Breaker Pattern

```java
@Component
public class WasmExecutionService {

    private final CircuitBreaker circuitBreaker;

    public WasmExecutionService() {
        this.circuitBreaker = CircuitBreaker.ofDefaults("wasmExecution");
        circuitBreaker.getEventPublisher().onStateTransition(
            event -> logger.info("Circuit breaker state transition: {}", event)
        );
    }

    public WasmValue[] executeFunction(String functionName, WasmValue... args) {
        return circuitBreaker.executeSupplier(() -> {
            return wasmFunction.call(args);
        });
    }
}
```

## Troubleshooting

### Common Production Issues

1. **Out of Memory Errors**
   ```bash
   # Increase heap size
   -Xmx8g
   # Enable heap dumps
   -XX:+HeapDumpOnOutOfMemoryError
   -XX:HeapDumpPath=/var/log/wasmtime4j/
   ```

2. **Native Library Loading Failures**
   ```bash
   # Check library path
   ls -la /app/native/
   # Verify permissions
   chmod +x /app/native/libwasmtime4j.so
   ```

3. **Performance Degradation**
   ```bash
   # Enable GC logging
   -Xloggc:/var/log/wasmtime4j/gc.log
   -XX:+PrintGCDetails
   -XX:+PrintGCTimeStamps
   ```

### Health Checks

```java
@RestController
public class HealthController {

    @Autowired
    private WasmRuntime wasmRuntime;

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> health = new HashMap<>();
        try {
            // Test WebAssembly execution
            RuntimeInfo info = wasmRuntime.getRuntimeInfo();
            health.put("status", "UP");
            health.put("runtime", info.getRuntimeType());
            health.put("version", info.getVersion());
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}
```

This production deployment guide provides comprehensive coverage for deploying Wasmtime4j in enterprise environments with proper security, monitoring, and operational considerations.