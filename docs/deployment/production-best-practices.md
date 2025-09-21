# Production Deployment Best Practices

This guide provides comprehensive best practices for deploying Wasmtime4j applications in production environments, covering deployment strategies, infrastructure requirements, operational considerations, and troubleshooting.

## Table of Contents
- [Deployment Strategy](#deployment-strategy)
- [Infrastructure Requirements](#infrastructure-requirements)
- [Configuration Management](#configuration-management)
- [Zero-Downtime Deployment](#zero-downtime-deployment)
- [Environment Preparation](#environment-preparation)
- [Service Discovery and Load Balancing](#service-discovery-and-load-balancing)
- [Data Management](#data-management)
- [Backup and Recovery](#backup-and-recovery)
- [Operational Procedures](#operational-procedures)
- [Troubleshooting Guide](#troubleshooting-guide)

## Deployment Strategy

### Blue-Green Deployment

Blue-green deployment is the recommended strategy for Wasmtime4j applications to ensure zero downtime and quick rollback capability.

```yaml
# blue-green-deployment.yml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: wasmtime4j-app
spec:
  replicas: 6
  strategy:
    blueGreen:
      activeService: wasmtime4j-active
      previewService: wasmtime4j-preview
      autoPromotionEnabled: false
      scaleDownDelaySeconds: 30
      prePromotionAnalysis:
        templates:
        - templateName: wasmtime4j-health-check
        args:
        - name: service-name
          value: wasmtime4j-preview
      postPromotionAnalysis:
        templates:
        - templateName: wasmtime4j-performance-test
        args:
        - name: service-name
          value: wasmtime4j-active
  selector:
    matchLabels:
      app: wasmtime4j-app
  template:
    metadata:
      labels:
        app: wasmtime4j-app
    spec:
      containers:
      - name: app
        image: wasmtime4j-app:{{.Values.image.tag}}
        ports:
        - containerPort: 8080
        env:
        - name: DEPLOYMENT_COLOR
          value: "{{.Values.deployment.color}}"
        - name: WASMTIME4J_RUNTIME
          value: "jni"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          successThreshold: 2
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
```

### Canary Deployment

For high-risk deployments, use canary deployment with gradual traffic shifting:

```yaml
# canary-deployment.yml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: wasmtime4j-app-canary
spec:
  replicas: 10
  strategy:
    canary:
      canaryService: wasmtime4j-canary
      stableService: wasmtime4j-stable
      trafficRouting:
        istio:
          virtualService:
            name: wasmtime4j-vsvc
      steps:
      - setWeight: 5
      - pause: {duration: 2m}
      - setWeight: 10
      - pause: {duration: 5m}
      - setWeight: 20
      - pause: {duration: 10m}
      - setWeight: 50
      - pause: {duration: 15m}
      - setWeight: 100
      analysis:
        templates:
        - templateName: wasmtime4j-error-rate
        - templateName: wasmtime4j-latency
        args:
        - name: canary-hash
          valueFrom:
            podTemplateHashValue: Latest
```

## Infrastructure Requirements

### Minimum Hardware Requirements

| Component | Minimum | Recommended | Notes |
|-----------|---------|-------------|-------|
| CPU | 2 cores | 4+ cores | WebAssembly compilation is CPU-intensive |
| Memory | 4 GB | 8+ GB | JVM heap + direct memory + native memory |
| Storage | 20 GB | 50+ GB | Application + logs + temporary files |
| Network | 1 Gbps | 10 Gbps | High throughput for WebAssembly module loading |

### Platform-Specific Considerations

#### Linux (Recommended)

```bash
# System requirements
- Kernel: 4.18+ (for optimal container support)
- glibc: 2.28+ (for native library compatibility)
- systemd: 239+ (for service management)

# Recommended distributions
- Ubuntu 20.04+ LTS
- RHEL/CentOS 8+
- Amazon Linux 2

# System optimization
echo 'net.core.somaxconn = 65535' >> /etc/sysctl.conf
echo 'vm.max_map_count = 262144' >> /etc/sysctl.conf
echo 'fs.file-max = 1000000' >> /etc/sysctl.conf
sysctl -p
```

#### Container Orchestration

```yaml
# Kubernetes node requirements
apiVersion: v1
kind: Node
metadata:
  name: wasmtime4j-node
  labels:
    workload: wasmtime4j
    instance-type: compute-optimized
spec:
  capacity:
    cpu: "16"
    memory: "32Gi"
    ephemeral-storage: "100Gi"
  allocatable:
    cpu: "15.5"
    memory: "30Gi"
    ephemeral-storage: "90Gi"
  taints:
  - key: workload
    value: wasmtime4j
    effect: NoSchedule
```

### Network Architecture

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    │   (AWS ALB/     │
                    │   NGINX/HAProxy)│
                    └─────────┬───────┘
                             │
                    ┌────────▼────────┐
                    │   API Gateway   │
                    │   (Kong/Istio/  │
                    │   Ambassador)   │
                    └─────────┬───────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼──────┐ ┌─────▼──────┐ ┌────▼──────┐
    │ Wasmtime4j App │ │Wasmtime4j  │ │Wasmtime4j │
    │   Instance 1   │ │ Instance 2 │ │Instance 3 │
    └─────────┬──────┘ └─────┬──────┘ └────┬──────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼────────┐
                    │  Shared Services│
                    │  - Database     │
                    │  - Cache        │
                    │  - Message Queue│
                    └─────────────────┘
```

## Configuration Management

### Environment-Specific Configuration

```java
// Production configuration class
@Configuration
@Profile("production")
public class ProductionConfiguration {

    @Bean
    @Primary
    public WasmRuntime productionWasmRuntime() throws WasmException {
        return WasmRuntimeFactory.create(
            RuntimeConfig.builder()
                .runtimeType(RuntimeType.JNI)  // Predictable runtime
                .optimizationLevel(OptimizationLevel.SPEED)
                .memoryLimit(512L * 1024 * 1024)  // 512MB limit
                .executionTimeout(Duration.ofSeconds(30))
                .enableDebug(false)  // Disable debug in production
                .enableProfiling(false)  // Disable profiling overhead
                .build()
        );
    }

    @Bean
    public EngineConfig productionEngineConfig() {
        return EngineConfig.builder()
            .maxMemory(1024L * 1024 * 1024)  // 1GB max
            .maxModules(1000)  // Cache up to 1000 modules
            .maxInstances(500)  // Max 500 concurrent instances
            .compilationStrategy(CompilationStrategy.EAGER)  // Pre-compile for speed
            .build();
    }

    @Bean
    public WasiConfig productionWasiConfig() {
        return WasiConfig.builder()
            .preopenDirectory("/app/data", "/data", WasiDirectoryAccess.READ_ONLY)
            .preopenDirectory("/tmp/app-work", "/tmp", WasiDirectoryAccess.READ_WRITE)
            .resourceLimits(WasiResourceLimits.builder()
                .maxMemory(256L * 1024 * 1024)  // 256MB WASI limit
                .maxOpenFiles(100)
                .maxNetworkConnections(50)
                .maxExecutionTime(Duration.ofSeconds(15))
                .build())
            .build();
    }
}
```

### Externalized Configuration

```yaml
# application-production.yml
wasmtime4j:
  runtime:
    type: jni
    optimization-level: speed
    memory-limit: 536870912  # 512MB
    execution-timeout: PT30S
    debug: false
    profiling: false

  engine:
    max-memory: 1073741824  # 1GB
    max-modules: 1000
    max-instances: 500
    compilation-strategy: eager

  wasi:
    directories:
      - host: "/app/data"
        guest: "/data"
        access: read-only
      - host: "/tmp/app-work"
        guest: "/tmp"
        access: read-write
    resource-limits:
      max-memory: 268435456  # 256MB
      max-open-files: 100
      max-network-connections: 50
      max-execution-time: PT15S

# JVM tuning for production
java:
  opts: >
    -server
    -Xms2g -Xmx4g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+UnlockExperimentalVMOptions
    -XX:+UseStringDeduplication
    -XX:+UseCompressedOops
    -XX:MaxDirectMemorySize=1g
    -Djava.security.egd=file:/dev/./urandom

# Monitoring and observability
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: always
      show-components: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### Secret Management

```yaml
# Using Kubernetes secrets
apiVersion: v1
kind: Secret
metadata:
  name: wasmtime4j-secrets
type: Opaque
stringData:
  database-password: ${DATABASE_PASSWORD}
  api-key: ${API_KEY}
  signing-key: ${WASM_MODULE_SIGNING_KEY}
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: wasmtime4j-config
data:
  application.yml: |
    spring:
      datasource:
        password: ${DATABASE_PASSWORD}
    wasmtime4j:
      security:
        module-signing-key: ${WASM_MODULE_SIGNING_KEY}
      api:
        external-service-key: ${API_KEY}
```

## Zero-Downtime Deployment

### Graceful Shutdown Implementation

```java
@Component
public class GracefulShutdownManager {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownManager.class);
    private final WasmRuntime runtime;
    private final List<WasmExecution> activeExecutions = new CopyOnWriteArrayList<>();
    private volatile boolean shutdownInitiated = false;

    @PreDestroy
    public void initiateGracefulShutdown() {
        log.info("Initiating graceful shutdown");
        shutdownInitiated = true;

        // Stop accepting new requests
        stopAcceptingNewRequests();

        // Wait for active executions to complete
        waitForActiveExecutions();

        // Cleanup WebAssembly runtime
        cleanupRuntime();

        log.info("Graceful shutdown completed");
    }

    private void stopAcceptingNewRequests() {
        // Mark service as unhealthy
        HealthIndicatorRegistry.setGlobalHealth(Health.down().build());

        // Wait for load balancer to detect health change
        try {
            Thread.sleep(5000);  // Grace period
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void waitForActiveExecutions() {
        long startTime = System.currentTimeMillis();
        long timeout = 30000;  // 30 seconds

        while (!activeExecutions.isEmpty() &&
               (System.currentTimeMillis() - startTime) < timeout) {

            log.info("Waiting for {} active executions to complete",
                     activeExecutions.size());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!activeExecutions.isEmpty()) {
            log.warn("Forcing shutdown with {} active executions",
                     activeExecutions.size());
            // Force terminate remaining executions
            activeExecutions.forEach(WasmExecution::forceTerminate);
        }
    }

    private void cleanupRuntime() {
        try {
            if (runtime != null && runtime.isValid()) {
                runtime.close();
            }
        } catch (Exception e) {
            log.error("Error during runtime cleanup", e);
        }
    }

    public boolean isShuttingDown() {
        return shutdownInitiated;
    }

    public void registerExecution(WasmExecution execution) {
        if (!shutdownInitiated) {
            activeExecutions.add(execution);
        } else {
            throw new IllegalStateException("Service is shutting down");
        }
    }

    public void unregisterExecution(WasmExecution execution) {
        activeExecutions.remove(execution);
    }
}
```

### Health Check Configuration

```java
@Component
public class ProductionHealthCheck implements HealthIndicator {

    private final WasmRuntime runtime;
    private final GracefulShutdownManager shutdownManager;
    private final DatabaseHealthIndicator databaseHealth;

    @Override
    public Health health() {
        if (shutdownManager.isShuttingDown()) {
            return Health.down()
                .withDetail("status", "Shutting down")
                .withDetail("message", "Service is preparing for shutdown")
                .build();
        }

        Health.Builder builder = new Health.Builder();

        // Check WebAssembly runtime
        Health wasmHealth = checkWasmRuntime();
        builder.withDetail("webassembly", wasmHealth.getDetails());

        // Check database connectivity
        Health dbHealth = databaseHealth.health();
        builder.withDetail("database", dbHealth.getDetails());

        // Check external dependencies
        Health externalHealth = checkExternalDependencies();
        builder.withDetail("external", externalHealth.getDetails());

        // Overall status
        if (wasmHealth.getStatus() == Status.UP &&
            dbHealth.getStatus() == Status.UP &&
            externalHealth.getStatus() == Status.UP) {
            return builder.up().build();
        } else if (wasmHealth.getStatus() == Status.DOWN) {
            return builder.down()
                .withDetail("critical", "WebAssembly runtime unavailable")
                .build();
        } else {
            return builder.outOfService().build();
        }
    }

    private Health checkWasmRuntime() {
        try {
            if (!runtime.isValid()) {
                return Health.down()
                    .withDetail("error", "Runtime invalid")
                    .build();
            }

            // Test basic functionality
            Engine engine = runtime.createEngine();
            long startTime = System.nanoTime();

            // Load a simple test module
            byte[] testModule = getTestModule();
            Module module = runtime.compileModule(engine, testModule);
            Instance instance = runtime.instantiate(module);

            // Execute test function
            WasmFunction testFunc = instance.getFunction("health_check");
            WasmValue[] result = testFunc.call();

            long executionTime = System.nanoTime() - startTime;

            return Health.up()
                .withDetail("runtime-type", runtime.getRuntimeInfo().getRuntimeType())
                .withDetail("execution-time-ns", executionTime)
                .withDetail("test-result", result[0].asI32())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}
```

## Environment Preparation

### System Prerequisites

```bash
#!/bin/bash
# production-setup.sh

set -euo pipefail

echo "=== Wasmtime4j Production Environment Setup ==="

# Update system packages
apt-get update && apt-get upgrade -y

# Install required packages
apt-get install -y \
    openjdk-23-jdk \
    curl \
    wget \
    unzip \
    ca-certificates \
    supervisor \
    rsyslog \
    logrotate \
    htop \
    iotop \
    nethogs

# Create application user
useradd -r -s /bin/false -d /opt/wasmtime4j wasmtime4j

# Create directory structure
mkdir -p /opt/wasmtime4j/{app,logs,data,temp,config}
mkdir -p /opt/wasmtime4j/wasm-modules/{trusted,sandboxed}

# Set permissions
chown -R wasmtime4j:wasmtime4j /opt/wasmtime4j
chmod 750 /opt/wasmtime4j/app
chmod 755 /opt/wasmtime4j/data
chmod 1777 /opt/wasmtime4j/temp  # Sticky bit for temp

# Configure system limits
cat << 'EOF' > /etc/security/limits.d/wasmtime4j.conf
wasmtime4j soft nofile 65536
wasmtime4j hard nofile 65536
wasmtime4j soft nproc 32768
wasmtime4j hard nproc 32768
wasmtime4j soft memlock unlimited
wasmtime4j hard memlock unlimited
EOF

# Configure systemd service
cat << 'EOF' > /etc/systemd/system/wasmtime4j.service
[Unit]
Description=Wasmtime4j Application
After=network.target
Wants=network.target

[Service]
Type=simple
User=wasmtime4j
Group=wasmtime4j
WorkingDirectory=/opt/wasmtime4j/app
Environment="JAVA_HOME=/usr/lib/jvm/java-23-openjdk-amd64"
Environment="WASMTIME4J_HOME=/opt/wasmtime4j"
Environment="WASMTIME4J_RUNTIME=jni"
ExecStart=/usr/bin/java \
    -server \
    -Xms2g -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseStringDeduplication \
    -Djava.awt.headless=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=production \
    -jar wasmtime4j-app.jar
ExecStop=/bin/kill -TERM $MAINPID
TimeoutStopSec=30
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=wasmtime4j

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/wasmtime4j/logs /opt/wasmtime4j/temp /opt/wasmtime4j/data

[Install]
WantedBy=multi-user.target
EOF

# Configure log rotation
cat << 'EOF' > /etc/logrotate.d/wasmtime4j
/opt/wasmtime4j/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 wasmtime4j wasmtime4j
    postrotate
        systemctl reload wasmtime4j || true
    endscript
}
EOF

# Optimize kernel parameters
cat << 'EOF' >> /etc/sysctl.conf
# Wasmtime4j optimizations
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 5000
net.ipv4.tcp_max_syn_backlog = 8192
vm.max_map_count = 262144
vm.swappiness = 1
fs.file-max = 1000000
EOF

sysctl -p

echo "=== Setup completed successfully ==="
```

### Java Environment Configuration

```bash
# Java environment setup
export JAVA_HOME=/usr/lib/jvm/java-23-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

# Production JVM settings
export JAVA_OPTS="
    -server
    -Xms2g -Xmx4g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+UnlockExperimentalVMOptions
    -XX:+UseStringDeduplication
    -XX:+UseCompressedOops
    -XX:MaxDirectMemorySize=1g
    -XX:+UseTransparentHugePages
    -Djava.awt.headless=true
    -Djava.security.egd=file:/dev/./urandom
    -Dspring.profiles.active=production
    -Dwasmtime4j.runtime=jni
    -Dwasmtime4j.debug=false
    -Dwasmtime4j.native.extract=/tmp
"

# For Panama FFI (Java 23+)
if [[ $($JAVA_HOME/bin/java -version 2>&1 | grep -q "23") ]]; then
    export JAVA_OPTS="$JAVA_OPTS
        --enable-preview
        --enable-native-access=ALL-UNNAMED
        -Djdk.foreign.restricted=permit
    "
fi
```

## Service Discovery and Load Balancing

### Load Balancer Configuration

```nginx
# nginx.conf for Wasmtime4j
upstream wasmtime4j_backend {
    least_conn;
    keepalive 32;

    server wasmtime4j-app-1:8080 max_fails=3 fail_timeout=30s;
    server wasmtime4j-app-2:8080 max_fails=3 fail_timeout=30s;
    server wasmtime4j-app-3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name wasmtime4j.example.com;

    # Health check endpoint
    location /health {
        access_log off;
        proxy_pass http://wasmtime4j_backend/actuator/health;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 5s;
        proxy_send_timeout 10s;
        proxy_read_timeout 10s;
    }

    # Application endpoints
    location / {
        proxy_pass http://wasmtime4j_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts for WebAssembly execution
        proxy_connect_timeout 10s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # Buffering
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;

        # Connection keep-alive
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }

    # WebAssembly module upload (if supported)
    location /api/modules {
        client_max_body_size 10M;  # Limit module size
        proxy_pass http://wasmtime4j_backend;
        proxy_request_buffering off;
        proxy_read_timeout 300s;  # Allow time for module compilation
    }
}
```

### Service Discovery with Consul

```json
{
  "service": {
    "name": "wasmtime4j-app",
    "id": "wasmtime4j-app-1",
    "port": 8080,
    "address": "10.0.1.100",
    "tags": [
      "wasmtime4j",
      "webassembly",
      "production"
    ],
    "meta": {
      "version": "1.0.0",
      "runtime": "jni",
      "region": "us-east-1"
    },
    "check": {
      "http": "http://10.0.1.100:8080/actuator/health",
      "interval": "10s",
      "timeout": "5s",
      "deregister_critical_service_after": "30s"
    }
  }
}
```

## Data Management

### WebAssembly Module Management

```java
@Service
public class ProductionModuleManager {

    private static final Logger log = LoggerFactory.getLogger(ProductionModuleManager.class);
    private final ModuleRepository moduleRepository;
    private final ModuleValidator moduleValidator;
    private final ModuleCache moduleCache;

    @Value("${wasmtime4j.modules.trusted-path:/opt/wasmtime4j/wasm-modules/trusted}")
    private String trustedModulesPath;

    @Value("${wasmtime4j.modules.sandboxed-path:/opt/wasmtime4j/wasm-modules/sandboxed}")
    private String sandboxedModulesPath;

    @PostConstruct
    public void initializeModules() {
        // Preload trusted modules on startup
        loadTrustedModules();

        // Verify module integrity
        validateModuleIntegrity();

        // Warm up module cache
        warmUpCache();
    }

    private void loadTrustedModules() {
        try {
            Path trustedPath = Paths.get(trustedModulesPath);
            if (!Files.exists(trustedPath)) {
                Files.createDirectories(trustedPath);
                return;
            }

            Files.walk(trustedPath)
                .filter(path -> path.toString().endsWith(".wasm"))
                .forEach(this::loadTrustedModule);

        } catch (IOException e) {
            log.error("Failed to load trusted modules", e);
            throw new IllegalStateException("Cannot initialize trusted modules", e);
        }
    }

    private void loadTrustedModule(Path modulePath) {
        try {
            String moduleId = modulePath.getFileName().toString();
            byte[] moduleBytes = Files.readAllBytes(modulePath);

            // Verify module signature
            if (!moduleValidator.verifySignature(moduleBytes)) {
                log.error("Module signature verification failed: {}", moduleId);
                return;
            }

            // Compile and cache module
            Module module = compileModule(moduleBytes);
            moduleCache.put(moduleId, module);

            log.info("Loaded trusted module: {}", moduleId);

        } catch (Exception e) {
            log.error("Failed to load module: {}", modulePath, e);
        }
    }

    public Module getModule(String moduleId, ModuleTrust trust) {
        // Check cache first
        Module cached = moduleCache.get(moduleId);
        if (cached != null) {
            return cached;
        }

        // Load from storage
        ModuleInfo info = moduleRepository.findByIdAndTrust(moduleId, trust);
        if (info == null) {
            throw new ModuleNotFoundException("Module not found: " + moduleId);
        }

        // Validate and compile
        byte[] moduleBytes = loadModuleBytes(info);
        validateModule(moduleBytes, trust);

        Module module = compileModule(moduleBytes);
        moduleCache.put(moduleId, module);

        return module;
    }

    private void validateModule(byte[] moduleBytes, ModuleTrust trust) {
        // Basic validation
        ValidationResult result = moduleValidator.validate(moduleBytes);
        if (!result.isValid()) {
            throw new InvalidModuleException("Module validation failed: " +
                                           result.getErrors());
        }

        // Trust-level specific validation
        switch (trust) {
            case TRUSTED:
                if (!moduleValidator.verifySignature(moduleBytes)) {
                    throw new SecurityException("Trusted module signature invalid");
                }
                break;

            case SANDBOXED:
                if (!moduleValidator.verifySandboxConstraints(moduleBytes)) {
                    throw new SecurityException("Module violates sandbox constraints");
                }
                break;
        }
    }
}
```

### Configuration Data Management

```java
@Configuration
public class ProductionDataConfiguration {

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getProperty("spring.datasource.url"));
        config.setUsername(env.getProperty("spring.datasource.username"));
        config.setPassword(env.getProperty("spring.datasource.password"));

        // Production connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);      // 5 minutes
        config.setMaxLifetime(1800000);     // 30 minutes
        config.setConnectionTimeout(20000); // 20 seconds
        config.setValidationTimeout(5000);  // 5 seconds
        config.setLeakDetectionThreshold(60000); // 1 minute

        // Health check
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());

        // JSON serialization for cached objects
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.setKeySerializer(new StringRedisSerializer());

        // Module cache configuration
        template.opsForValue().getOperations().expire(
            "module:*", Duration.ofHours(24));

        return template;
    }
}
```

## Backup and Recovery

### Automated Backup Strategy

```bash
#!/bin/bash
# backup-wasmtime4j.sh

set -euo pipefail

BACKUP_DIR="/opt/backups/wasmtime4j"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="$BACKUP_DIR/$TIMESTAMP"

# Create backup directory
mkdir -p "$BACKUP_PATH"

echo "Starting Wasmtime4j backup: $TIMESTAMP"

# Backup application configuration
echo "Backing up configuration..."
tar -czf "$BACKUP_PATH/config.tar.gz" \
    /opt/wasmtime4j/config \
    /etc/systemd/system/wasmtime4j.service

# Backup WebAssembly modules
echo "Backing up WebAssembly modules..."
tar -czf "$BACKUP_PATH/modules.tar.gz" \
    /opt/wasmtime4j/wasm-modules

# Backup application data
echo "Backing up application data..."
tar -czf "$BACKUP_PATH/data.tar.gz" \
    /opt/wasmtime4j/data

# Backup database (if applicable)
if command -v pg_dump >/dev/null 2>&1; then
    echo "Backing up database..."
    pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME | \
        gzip > "$BACKUP_PATH/database.sql.gz"
fi

# Backup logs (last 7 days)
echo "Backing up recent logs..."
find /opt/wasmtime4j/logs -name "*.log*" -mtime -7 | \
    tar -czf "$BACKUP_PATH/logs.tar.gz" -T -

# Create backup manifest
cat > "$BACKUP_PATH/manifest.txt" << EOF
Backup Date: $TIMESTAMP
Application Version: $(java -jar /opt/wasmtime4j/app/wasmtime4j-app.jar --version 2>/dev/null || echo "unknown")
System Information: $(uname -a)
Files:
$(ls -la "$BACKUP_PATH")
EOF

# Cleanup old backups (keep 30 days)
find "$BACKUP_DIR" -type d -mtime +30 -exec rm -rf {} +

echo "Backup completed successfully: $BACKUP_PATH"

# Upload to remote storage (optional)
if [[ "${REMOTE_BACKUP_ENABLED:-false}" == "true" ]]; then
    echo "Uploading to remote storage..."
    aws s3 sync "$BACKUP_PATH" "s3://$BACKUP_BUCKET/wasmtime4j/$TIMESTAMP/"
fi
```

### Disaster Recovery Plan

```yaml
# disaster-recovery-playbook.yml
---
- name: Wasmtime4j Disaster Recovery
  hosts: wasmtime4j_servers
  become: yes
  vars:
    backup_source: "s3://backup-bucket/wasmtime4j/latest/"
    recovery_path: "/opt/wasmtime4j-recovery"

  tasks:
  - name: Stop existing service
    systemd:
      name: wasmtime4j
      state: stopped
    ignore_errors: yes

  - name: Create recovery directory
    file:
      path: "{{ recovery_path }}"
      state: directory
      owner: wasmtime4j
      group: wasmtime4j

  - name: Download backup from S3
    amazon.aws.aws_s3:
      bucket: backup-bucket
      object: wasmtime4j/latest/
      dest: "{{ recovery_path }}"
      mode: get
      recursive: yes

  - name: Extract configuration
    unarchive:
      src: "{{ recovery_path }}/config.tar.gz"
      dest: /
      remote_src: yes

  - name: Extract modules
    unarchive:
      src: "{{ recovery_path }}/modules.tar.gz"
      dest: /
      remote_src: yes

  - name: Extract application data
    unarchive:
      src: "{{ recovery_path }}/data.tar.gz"
      dest: /
      remote_src: yes

  - name: Restore database
    shell: |
      gunzip -c {{ recovery_path }}/database.sql.gz | \
      psql -h {{ db_host }} -U {{ db_user }} -d {{ db_name }}
    when: db_restore_enabled | default(false)

  - name: Set permissions
    file:
      path: /opt/wasmtime4j
      owner: wasmtime4j
      group: wasmtime4j
      recurse: yes

  - name: Start service
    systemd:
      name: wasmtime4j
      state: started
      enabled: yes

  - name: Wait for service health
    uri:
      url: "http://localhost:8080/actuator/health"
      method: GET
      timeout: 10
    retries: 30
    delay: 10
```

## Operational Procedures

### Startup Procedures

```bash
#!/bin/bash
# startup-wasmtime4j.sh

set -euo pipefail

echo "=== Wasmtime4j Startup Procedure ==="

# Pre-startup checks
echo "Performing pre-startup checks..."

# Check system resources
check_system_resources() {
    # Memory check
    AVAILABLE_MEMORY=$(free -m | awk 'NR==2{printf "%.0f", $7}')
    if [[ $AVAILABLE_MEMORY -lt 1024 ]]; then
        echo "ERROR: Insufficient memory. Available: ${AVAILABLE_MEMORY}MB, Required: 1024MB"
        exit 1
    fi

    # Disk space check
    AVAILABLE_DISK=$(df /opt/wasmtime4j | awk 'NR==2{print $4}')
    if [[ $AVAILABLE_DISK -lt 1048576 ]]; then  # 1GB in KB
        echo "ERROR: Insufficient disk space. Available: ${AVAILABLE_DISK}KB, Required: 1GB"
        exit 1
    fi

    # Port availability check
    if netstat -ln | grep -q ":8080 "; then
        echo "ERROR: Port 8080 is already in use"
        exit 1
    fi

    echo "System resources check: PASSED"
}

# Check dependencies
check_dependencies() {
    # Java availability
    if ! command -v java >/dev/null 2>&1; then
        echo "ERROR: Java not found"
        exit 1
    fi

    # Java version check
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [[ $JAVA_VERSION -lt 8 ]]; then
        echo "ERROR: Java 8+ required, found: $JAVA_VERSION"
        exit 1
    fi

    # Application JAR check
    if [[ ! -f "/opt/wasmtime4j/app/wasmtime4j-app.jar" ]]; then
        echo "ERROR: Application JAR not found"
        exit 1
    fi

    echo "Dependencies check: PASSED"
}

# Validate configuration
validate_configuration() {
    # Configuration file check
    if [[ ! -f "/opt/wasmtime4j/config/application.yml" ]]; then
        echo "ERROR: Configuration file not found"
        exit 1
    fi

    # Environment variables check
    REQUIRED_VARS=("WASMTIME4J_RUNTIME" "SPRING_PROFILES_ACTIVE")
    for var in "${REQUIRED_VARS[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            echo "ERROR: Required environment variable not set: $var"
            exit 1
        fi
    done

    echo "Configuration validation: PASSED"
}

# Module validation
validate_modules() {
    local modules_dir="/opt/wasmtime4j/wasm-modules"

    if [[ ! -d "$modules_dir" ]]; then
        echo "WARNING: WebAssembly modules directory not found, creating..."
        mkdir -p "$modules_dir"/{trusted,sandboxed}
        chown -R wasmtime4j:wasmtime4j "$modules_dir"
    fi

    # Check for trusted modules
    local trusted_count=$(find "$modules_dir/trusted" -name "*.wasm" 2>/dev/null | wc -l)
    echo "Found $trusted_count trusted WebAssembly modules"

    echo "Module validation: PASSED"
}

# Run all checks
check_system_resources
check_dependencies
validate_configuration
validate_modules

# Start the service
echo "Starting Wasmtime4j service..."
systemctl start wasmtime4j

# Wait for service to become ready
echo "Waiting for service to become ready..."
timeout=60
counter=0

while [[ $counter -lt $timeout ]]; do
    if curl -f -s http://localhost:8080/actuator/health/readiness >/dev/null 2>&1; then
        echo "Service is ready!"
        break
    fi

    echo "Waiting... ($counter/$timeout)"
    sleep 1
    ((counter++))
done

if [[ $counter -eq $timeout ]]; then
    echo "ERROR: Service failed to become ready within $timeout seconds"
    systemctl status wasmtime4j
    exit 1
fi

# Post-startup verification
echo "Performing post-startup verification..."

# Health check
health_status=$(curl -s http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
if [[ "$health_status" != "UP" ]]; then
    echo "ERROR: Health check failed. Status: $health_status"
    exit 1
fi

# Module loading test
echo "Testing module loading..."
module_test=$(curl -s -X POST http://localhost:8080/api/test/module-loading || echo "FAILED")
if [[ "$module_test" == "FAILED" ]]; then
    echo "WARNING: Module loading test failed"
fi

echo "=== Startup completed successfully ==="
```

### Shutdown Procedures

```bash
#!/bin/bash
# shutdown-wasmtime4j.sh

set -euo pipefail

echo "=== Wasmtime4j Shutdown Procedure ==="

# Graceful shutdown with timeout
SHUTDOWN_TIMEOUT=60
START_TIME=$(date +%s)

# Signal graceful shutdown
echo "Initiating graceful shutdown..."
systemctl stop wasmtime4j &
STOP_PID=$!

# Monitor shutdown progress
while kill -0 $STOP_PID 2>/dev/null; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))

    if [[ $ELAPSED -ge $SHUTDOWN_TIMEOUT ]]; then
        echo "TIMEOUT: Graceful shutdown exceeded ${SHUTDOWN_TIMEOUT}s, forcing stop..."
        kill -KILL $STOP_PID 2>/dev/null || true
        systemctl kill wasmtime4j
        break
    fi

    echo "Shutdown in progress... (${ELAPSED}s/${SHUTDOWN_TIMEOUT}s)"
    sleep 2
done

# Verify shutdown
if systemctl is-active wasmtime4j >/dev/null 2>&1; then
    echo "ERROR: Service still running after shutdown attempt"
    exit 1
fi

# Cleanup temporary files
echo "Cleaning up temporary files..."
find /opt/wasmtime4j/temp -type f -mtime +1 -delete 2>/dev/null || true

echo "=== Shutdown completed successfully ==="
```

## Troubleshooting Guide

### Common Issues and Solutions

#### 1. Service Won't Start

**Symptoms:**
- Service fails to start
- Health checks return DOWN status
- JVM crash on startup

**Diagnosis:**
```bash
# Check service status
systemctl status wasmtime4j

# Check logs
journalctl -u wasmtime4j -f

# Check Java process
ps aux | grep wasmtime4j

# Check port availability
netstat -ln | grep 8080
```

**Solutions:**
```bash
# Fix memory issues
echo 'vm.overcommit_memory = 1' >> /etc/sysctl.conf
sysctl -p

# Fix file descriptor limits
echo 'wasmtime4j soft nofile 65536' >> /etc/security/limits.conf
echo 'wasmtime4j hard nofile 65536' >> /etc/security/limits.conf

# Fix permission issues
chown -R wasmtime4j:wasmtime4j /opt/wasmtime4j
chmod +x /opt/wasmtime4j/app/wasmtime4j-app.jar
```

#### 2. High Memory Usage

**Symptoms:**
- OutOfMemoryError exceptions
- High heap usage metrics
- System becomes unresponsive

**Diagnosis:**
```bash
# Check JVM memory usage
jstat -gc <pid>

# Generate heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.system_properties
jmap -dump:format=b,file=heap.hprof <pid>

# Check system memory
free -h
cat /proc/meminfo
```

**Solutions:**
```java
// Tune JVM settings
JAVA_OPTS="
    -Xms4g -Xmx4g
    -XX:MaxDirectMemorySize=2g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=100
    -XX:G1HeapRegionSize=16m
"

// Configure WebAssembly memory limits
wasmtime4j:
  engine:
    max-memory: 1073741824  # 1GB
  wasi:
    resource-limits:
      max-memory: 536870912  # 512MB
```

#### 3. Module Loading Failures

**Symptoms:**
- ModuleCompilationException
- Invalid module errors
- Signature verification failures

**Diagnosis:**
```bash
# Check module files
file /opt/wasmtime4j/wasm-modules/trusted/*.wasm
hexdump -C module.wasm | head

# Validate module structure
wasm-objdump -h module.wasm
wasm-validate module.wasm
```

**Solutions:**
```bash
# Recompile modules with correct flags
rustc --target wasm32-wasi -O module.rs -o module.wasm

# Update module signatures
openssl dgst -sha256 -sign private.key module.wasm > module.sig

# Reset module cache
rm -rf /opt/wasmtime4j/cache/*
systemctl restart wasmtime4j
```

#### 4. Performance Issues

**Symptoms:**
- High execution times
- CPU usage spikes
- Timeout errors

**Diagnosis:**
```bash
# Profile JVM performance
java -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=profile.jfr

# Monitor system performance
top -p $(pidof java)
iostat -x 1
nethogs

# Check WebAssembly execution times
curl -s http://localhost:8080/actuator/metrics/wasm.execution.time
```

**Solutions:**
```java
// Optimize engine configuration
EngineConfig config = EngineConfig.builder()
    .optimizationLevel(OptimizationLevel.SPEED)
    .compilationStrategy(CompilationStrategy.EAGER)
    .enableProfiling(false)  // Disable in production
    .build();

// Configure thread pools
@Bean
public TaskExecutor wasmTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
    executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("wasm-");
    return executor;
}
```

This production deployment guide provides comprehensive procedures and best practices for successfully deploying and operating Wasmtime4j applications in production environments.