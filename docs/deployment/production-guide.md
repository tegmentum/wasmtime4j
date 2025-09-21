# Production Deployment Guide for Wasmtime4j

This comprehensive guide covers deploying Wasmtime4j applications to production environments, including configuration, monitoring, scaling, and operational best practices.

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Production Configuration](#production-configuration)
3. [Platform-Specific Deployment](#platform-specific-deployment)
4. [Container Deployment](#container-deployment)
5. [Cloud Deployment](#cloud-deployment)
6. [Performance Optimization](#performance-optimization)
7. [Monitoring and Observability](#monitoring-and-observability)
8. [Security Hardening](#security-hardening)
9. [Scaling Strategies](#scaling-strategies)
10. [Troubleshooting](#troubleshooting)
11. [Maintenance and Updates](#maintenance-and-updates)

## Pre-Deployment Checklist

### Code Readiness
- [ ] All tests pass (unit, integration, performance)
- [ ] Code review completed
- [ ] Security scan completed
- [ ] Performance benchmarks meet requirements
- [ ] Error handling tested thoroughly
- [ ] Resource cleanup verified (no memory leaks)

### Configuration
- [ ] Production configuration files prepared
- [ ] Environment-specific settings configured
- [ ] Secrets management configured
- [ ] Logging configuration optimized
- [ ] Monitoring configuration enabled

### Infrastructure
- [ ] Production environment provisioned
- [ ] Network security configured
- [ ] Load balancer configured (if applicable)
- [ ] Database connections tested
- [ ] Backup and recovery procedures in place

### Dependencies
- [ ] All dependencies verified and scanned
- [ ] Native libraries included for target platforms
- [ ] Version compatibility verified
- [ ] License compliance checked

## Production Configuration

### Engine Configuration

Create a production-optimized engine configuration:

```java
public class ProductionConfig {
    public static EngineConfig createProductionEngine() {
        return new EngineConfig()
            .optimizationLevel(OptimizationLevel.SPEED)
            .parallelCompilation(true)
            .consumeFuel(true)
            .maxFuel(10_000_000) // Prevent infinite loops
            .debugInfo(false) // Disable debug info for performance
            .detailedDiagnostics(false)
            .simdSupport(true) // Enable SIMD if available
            .bulkMemoryOperations(true)
            .multiThreading(false) // Disable for deterministic behavior
            .precompileModules(true) // Pre-compile for faster startup
            .enableModuleCaching(true); // Cache compiled modules
    }

    public static ResourcePool<Engine> createEnginePool() {
        PoolConfiguration poolConfig = PoolConfiguration.builder()
            .initialSize(Runtime.getRuntime().availableProcessors())
            .maxSize(Runtime.getRuntime().availableProcessors() * 2)
            .maxIdleTime(Duration.ofMinutes(10))
            .validationQuery(Engine::isValid)
            .evictionPolicy(EvictionPolicy.LRU)
            .build();

        return ResourcePool.<Engine>builder()
            .configuration(poolConfig)
            .factory(() -> {
                WasmRuntime runtime = WasmRuntimeFactory.create();
                return runtime.createEngine(createProductionEngine());
            })
            .build();
    }
}
```

### Security Configuration

Implement comprehensive security policies:

```java
public class ProductionSecurity {
    public static SecurityPolicy createProductionSecurityPolicy() {
        return SecurityPolicy.builder()
            .allowMemoryAccess(MemoryAccess.READ_WRITE)
            .setResourceLimits(ResourceLimits.builder()
                .maxMemory(128 * 1024 * 1024) // 128MB per instance
                .maxFuel(10_000_000)
                .maxStackDepth(1000)
                .maxExecutionTime(Duration.ofSeconds(30))
                .build())
            .setRateLimits(RateLimits.builder()
                .maxRequestsPerSecond(1000)
                .maxConcurrentRequests(100)
                .build())
            .enableAuditLogging(true)
            .enforcementLevel(EnforcementLevel.STRICT)
            .build();
    }

    public static Sandbox createProductionSandbox() {
        return Sandbox.builder()
            .withPolicy(createProductionSecurityPolicy())
            .withAuditLogging(true)
            .withIntrusionDetection(IntrusionDetectionConfig.builder()
                .enableAnomalyDetection(true)
                .suspiciousPatternThreshold(10)
                .enableRealTimeBlocking(true)
                .build())
            .build();
    }
}
```

### Application Configuration

```properties
# application-production.properties

# Wasmtime4j Configuration
wasmtime4j.runtime.preferredImplementation=auto
wasmtime4j.engine.optimizationLevel=SPEED
wasmtime4j.engine.parallelCompilation=true
wasmtime4j.engine.consumeFuel=true
wasmtime4j.engine.maxFuel=10000000
wasmtime4j.engine.debugInfo=false

# Resource Pool Configuration
wasmtime4j.pool.initialSize=4
wasmtime4j.pool.maxSize=16
wasmtime4j.pool.maxIdleTime=PT10M
wasmtime4j.pool.validationInterval=PT5M

# Security Configuration
wasmtime4j.security.maxMemoryPerInstance=134217728
wasmtime4j.security.maxExecutionTime=PT30S
wasmtime4j.security.enableAuditLogging=true
wasmtime4j.security.enforcementLevel=STRICT

# Performance Configuration
wasmtime4j.performance.enableProfiling=false
wasmtime4j.performance.enableMetrics=true
wasmtime4j.performance.metricsInterval=PT1M

# Logging Configuration
wasmtime4j.logging.level=INFO
wasmtime4j.logging.enableStructuredLogging=true
wasmtime4j.logging.includeStackTraces=false
```

## Platform-Specific Deployment

### Linux Deployment

#### System Requirements

```bash
# Minimum system requirements
# - Linux kernel 3.10+
# - glibc 2.17+
# - Java 8+ (Java 23+ recommended for Panama)

# Install required packages (Ubuntu/Debian)
sudo apt update
sudo apt install -y libc6-dev

# Install required packages (CentOS/RHEL)
sudo yum install -y glibc-devel

# Verify system compatibility
java -version
ldd --version
```

#### Service Configuration

Create a systemd service:

```ini
# /etc/systemd/system/wasmtime4j-app.service
[Unit]
Description=Wasmtime4j Application
After=network.target

[Service]
Type=simple
User=wasmtime4j
Group=wasmtime4j
WorkingDirectory=/opt/wasmtime4j-app
ExecStart=/usr/bin/java \
    -Xms2g \
    -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=production \
    -jar wasmtime4j-app.jar

# Resource limits
LimitNOFILE=65536
LimitNPROC=32768

# Security
NoNewPrivileges=yes
PrivateTmp=yes
ProtectSystem=strict
ProtectHome=yes
ReadWritePaths=/opt/wasmtime4j-app/logs /opt/wasmtime4j-app/temp

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Windows Deployment

#### System Requirements

```powershell
# Windows Server 2016+ or Windows 10+
# Visual C++ Redistributable 2019+
# Java 8+ (Java 23+ recommended)

# Install Visual C++ Redistributable
# Download from Microsoft and install x64 version

# Verify installation
java -version
```

#### Windows Service Configuration

```xml
<!-- wasmtime4j-service.xml for WinSW -->
<service>
    <id>wasmtime4j-app</id>
    <name>Wasmtime4j Application</name>
    <description>Production Wasmtime4j Application</description>

    <executable>java</executable>
    <arguments>
        -Xms2g
        -Xmx4g
        -XX:+UseG1GC
        -Dspring.profiles.active=production
        -jar wasmtime4j-app.jar
    </arguments>

    <workingdirectory>C:\wasmtime4j-app</workingdirectory>
    <logpath>C:\wasmtime4j-app\logs</logpath>
    <logmode>rotate</logmode>

    <onfailure action="restart" delay="10 sec"/>
    <resetfailure>1 hour</resetfailure>
</service>
```

### macOS Deployment

#### System Requirements

```bash
# macOS 10.15+ (Catalina)
# Xcode Command Line Tools
# Java 8+ (Java 23+ recommended)

# Install Xcode Command Line Tools
xcode-select --install

# Verify installation
java -version
```

#### Launch Daemon Configuration

```xml
<!-- /Library/LaunchDaemons/com.example.wasmtime4j.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.example.wasmtime4j</string>

    <key>ProgramArguments</key>
    <array>
        <string>/usr/bin/java</string>
        <string>-Xms2g</string>
        <string>-Xmx4g</string>
        <string>-XX:+UseG1GC</string>
        <string>-Dspring.profiles.active=production</string>
        <string>-jar</string>
        <string>/opt/wasmtime4j-app/wasmtime4j-app.jar</string>
    </array>

    <key>WorkingDirectory</key>
    <string>/opt/wasmtime4j-app</string>

    <key>RunAtLoad</key>
    <true/>

    <key>KeepAlive</key>
    <true/>

    <key>StandardOutPath</key>
    <string>/opt/wasmtime4j-app/logs/stdout.log</string>

    <key>StandardErrorPath</key>
    <string>/opt/wasmtime4j-app/logs/stderr.log</string>
</dict>
</plist>
```

## Container Deployment

### Docker Configuration

Create an optimized Dockerfile:

```dockerfile
# Multi-stage build for production
FROM openjdk:23-jdk-slim as builder

WORKDIR /app
COPY . .

# Build application
RUN ./mvnw clean package -DskipTests

# Production stage
FROM openjdk:23-jre-slim

# Install required system packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libc6-dev \
        ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r wasmtime4j && useradd -r -g wasmtime4j wasmtime4j

# Create application directory
WORKDIR /app

# Copy application JAR
COPY --from=builder /app/target/wasmtime4j-app.jar ./

# Copy native libraries (if included)
COPY --from=builder /app/target/natives ./natives

# Set ownership
RUN chown -R wasmtime4j:wasmtime4j /app

# Switch to non-root user
USER wasmtime4j

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Runtime configuration
ENV JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE="production"

EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar wasmtime4j-app.jar"]
```

### Docker Compose for Production

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  wasmtime4j-app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC
      - WASMTIME4J_POOL_MAX_SIZE=16
      - WASMTIME4J_SECURITY_ENFORCEMENT_LEVEL=STRICT
    volumes:
      - ./logs:/app/logs
      - ./config:/app/config:ro
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 4G
        reservations:
          cpus: '1.0'
          memory: 2G
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "3"

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  grafana_data:
```

### Kubernetes Deployment

```yaml
# wasmtime4j-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wasmtime4j-app
  labels:
    app: wasmtime4j-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: wasmtime4j-app
  template:
    metadata:
      labels:
        app: wasmtime4j-app
    spec:
      containers:
      - name: wasmtime4j-app
        image: wasmtime4j-app:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: JAVA_OPTS
          value: "-Xms2g -Xmx4g -XX:+UseG1GC"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: config
        configMap:
          name: wasmtime4j-config
      - name: logs
        emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: wasmtime4j-service
spec:
  selector:
    app: wasmtime4j-app
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: LoadBalancer

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: wasmtime4j-config
data:
  application-production.properties: |
    wasmtime4j.runtime.preferredImplementation=auto
    wasmtime4j.engine.optimizationLevel=SPEED
    wasmtime4j.security.enforcementLevel=STRICT
    wasmtime4j.performance.enableMetrics=true
```

## Cloud Deployment

### AWS Deployment

#### EC2 Deployment

```bash
#!/bin/bash
# deploy-aws-ec2.sh

# Launch EC2 instance with proper configuration
aws ec2 run-instances \
    --image-id ami-0c55b159cbfafe1d0 \
    --instance-type c5.xlarge \
    --key-name my-key-pair \
    --security-groups wasmtime4j-sg \
    --user-data file://user-data.sh \
    --iam-instance-profile Name=wasmtime4j-instance-profile

# User data script for EC2 instance
cat > user-data.sh << 'EOF'
#!/bin/bash
yum update -y
yum install -y java-23-amazon-corretto docker

# Start Docker
systemctl start docker
systemctl enable docker

# Add ec2-user to docker group
usermod -a -G docker ec2-user

# Deploy application
docker run -d \
    --name wasmtime4j-app \
    --restart unless-stopped \
    -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=production \
    -e JAVA_OPTS="-Xms2g -Xmx4g" \
    wasmtime4j-app:latest
EOF
```

#### ECS Deployment

```json
{
    "family": "wasmtime4j-app",
    "networkMode": "awsvpc",
    "requiresCompatibilities": ["FARGATE"],
    "cpu": "2048",
    "memory": "4096",
    "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
    "taskRoleArn": "arn:aws:iam::account:role/ecsTaskRole",
    "containerDefinitions": [
        {
            "name": "wasmtime4j-app",
            "image": "your-account.dkr.ecr.region.amazonaws.com/wasmtime4j-app:latest",
            "portMappings": [
                {
                    "containerPort": 8080,
                    "protocol": "tcp"
                }
            ],
            "environment": [
                {
                    "name": "SPRING_PROFILES_ACTIVE",
                    "value": "production"
                },
                {
                    "name": "JAVA_OPTS",
                    "value": "-Xms2g -Xmx4g -XX:+UseG1GC"
                }
            ],
            "logConfiguration": {
                "logDriver": "awslogs",
                "options": {
                    "awslogs-group": "/ecs/wasmtime4j-app",
                    "awslogs-region": "us-west-2",
                    "awslogs-stream-prefix": "ecs"
                }
            },
            "healthCheck": {
                "command": [
                    "CMD-SHELL",
                    "curl -f http://localhost:8080/actuator/health || exit 1"
                ],
                "interval": 30,
                "timeout": 5,
                "retries": 3,
                "startPeriod": 60
            }
        }
    ]
}
```

### Google Cloud Platform

#### GKE Deployment

```yaml
# gke-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wasmtime4j-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: wasmtime4j-app
  template:
    metadata:
      labels:
        app: wasmtime4j-app
    spec:
      containers:
      - name: wasmtime4j-app
        image: gcr.io/your-project/wasmtime4j-app:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

### Azure Deployment

#### Azure Container Instances

```yaml
# azure-container-instance.yaml
apiVersion: 2019-12-01
location: eastus
name: wasmtime4j-app
properties:
  containers:
  - name: wasmtime4j-app
    properties:
      image: your-registry.azurecr.io/wasmtime4j-app:latest
      resources:
        requests:
          cpu: 2
          memoryInGb: 4
      ports:
      - port: 8080
      environmentVariables:
      - name: SPRING_PROFILES_ACTIVE
        value: production
      - name: JAVA_OPTS
        value: "-Xms2g -Xmx4g -XX:+UseG1GC"
  osType: Linux
  restartPolicy: Always
  ipAddress:
    type: Public
    ports:
    - protocol: tcp
      port: 8080
```

## Performance Optimization

### JVM Tuning

```bash
# Production JVM options
JAVA_OPTS="
-Xms4g
-Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=32m
-XX:+G1UseAdaptiveIHOP
-XX:G1MixedGCCountTarget=8
-XX:+UnlockExperimentalVMOptions
-XX:+UseJVMCICompiler
-XX:+PrintGC
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-Xloggc:gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=100M
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=heapdump.hprof
-Djava.security.egd=file:/dev/./urandom
"
```

### Application Performance Tuning

```java
@Configuration
public class PerformanceConfig {

    @Bean
    @ConfigurationProperties("wasmtime4j.performance")
    public PerformanceOptimizer performanceOptimizer() {
        return PerformanceOptimizer.builder()
            .enableJitOptimization(true)
            .enableInlining(true)
            .enableDeadCodeElimination(true)
            .enableConstantFolding(true)
            .optimizationPasses(3)
            .enableProfileGuidedOptimization(true)
            .build();
    }

    @Bean
    public MemoryOptimizationService memoryOptimizer() {
        return MemoryOptimizationService.builder()
            .enableMemoryPooling(true)
            .enableCompression(true)
            .garbageCollectionTuning(GcTuning.THROUGHPUT)
            .memoryMappingStrategy(MemoryMappingStrategy.AGGRESSIVE)
            .build();
    }
}
```

## Monitoring and Observability

### Metrics Configuration

```java
@Component
public class WasmMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final PerformanceMonitor performanceMonitor;

    public WasmMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.performanceMonitor = PerformanceMonitor.create();
    }

    @EventListener
    public void onWasmFunctionCall(WasmFunctionCallEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("wasm.function.execution")
            .description("WebAssembly function execution time")
            .tag("function", event.getFunctionName())
            .tag("module", event.getModuleName())
            .register(meterRegistry));

        Counter.builder("wasm.function.calls")
            .description("WebAssembly function call count")
            .tag("function", event.getFunctionName())
            .tag("module", event.getModuleName())
            .register(meterRegistry)
            .increment();
    }

    @Scheduled(fixedRate = 60000)
    public void collectPerformanceMetrics() {
        PerformanceReport report = performanceMonitor.generateReport();

        Gauge.builder("wasm.memory.usage")
            .description("WebAssembly memory usage")
            .register(meterRegistry, report, r -> r.getMemoryUsage());

        Gauge.builder("wasm.active.instances")
            .description("Active WebAssembly instances")
            .register(meterRegistry, report, r -> r.getActiveInstances());

        Gauge.builder("wasm.compilation.cache.hit.rate")
            .description("Module compilation cache hit rate")
            .register(meterRegistry, report, r -> r.getCacheHitRate());
    }
}
```

### Logging Configuration

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="production">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/wasmtime4j-app.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                <fileNamePattern>logs/wasmtime4j-app.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>

        <logger name="ai.tegmentum.wasmtime4j" level="INFO"/>
        <logger name="root" level="WARN"/>

        <root level="INFO">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

### Health Checks

```java
@Component
public class WasmHealthIndicator implements HealthIndicator {

    private final WasmRuntime runtime;
    private final ResourcePool<Engine> enginePool;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            // Check runtime availability
            if (!runtime.isValid()) {
                return builder.down()
                    .withDetail("runtime", "WebAssembly runtime is not available")
                    .build();
            }

            // Check engine pool health
            PoolStatistics poolStats = enginePool.getStatistics();
            if (poolStats.getActiveCount() >= poolStats.getMaxSize() * 0.9) {
                builder.status("DEGRADED")
                    .withDetail("engine_pool", "Engine pool near capacity");
            }

            // Check memory usage
            RuntimeMetrics metrics = runtime.getRuntimeMetrics();
            if (metrics.getMemoryUsage() > 0.85) {
                builder.status("DEGRADED")
                    .withDetail("memory", "High memory usage detected");
            }

            return builder.up()
                .withDetail("runtime_type", runtime.getRuntimeInfo().getRuntimeType())
                .withDetail("wasmtime_version", runtime.getRuntimeInfo().getWasmtimeVersion())
                .withDetail("active_engines", poolStats.getActiveCount())
                .withDetail("idle_engines", poolStats.getIdleCount())
                .withDetail("memory_usage", String.format("%.2f%%", metrics.getMemoryUsage() * 100))
                .build();

        } catch (Exception e) {
            return builder.down(e).build();
        }
    }
}
```

## Security Hardening

### Network Security

```yaml
# Security Group (AWS)
Resources:
  WasmSecurityGroup:
    Type: AWS::EC2::SecurityGroup
    Properties:
      GroupDescription: Security group for Wasmtime4j application
      SecurityGroupIngress:
        - IpProtocol: tcp
          FromPort: 8080
          ToPort: 8080
          SourceSecurityGroupId: !Ref LoadBalancerSecurityGroup
        - IpProtocol: tcp
          FromPort: 22
          ToPort: 22
          CidrIp: 10.0.0.0/16  # VPC CIDR only
      SecurityGroupEgress:
        - IpProtocol: tcp
          FromPort: 443
          ToPort: 443
          CidrIp: 0.0.0.0/0  # HTTPS outbound
        - IpProtocol: tcp
          FromPort: 80
          ToPort: 80
          CidrIp: 0.0.0.0/0  # HTTP outbound
```

### Application Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            )
            .build();
    }

    @Bean
    public WasmSecurityValidator securityValidator() {
        return WasmSecurityValidator.builder()
            .enableModuleValidation(true)
            .enableRuntimeValidation(true)
            .maxModuleSize(10 * 1024 * 1024) // 10MB
            .allowedImports(Set.of("env.log", "env.time"))
            .blockedImports(Set.of("env.system", "env.file"))
            .enableMalwareScanning(true)
            .build();
    }
}
```

## Scaling Strategies

### Horizontal Scaling

```yaml
# Horizontal Pod Autoscaler (Kubernetes)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: wasmtime4j-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: wasmtime4j-app
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
```

### Load Balancing

```nginx
# nginx.conf
upstream wasmtime4j_backend {
    least_conn;
    server app1:8080 max_fails=3 fail_timeout=30s;
    server app2:8080 max_fails=3 fail_timeout=30s;
    server app3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name wasmtime4j.example.com;

    location / {
        proxy_pass http://wasmtime4j_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 5s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;

        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503;
    }

    location /actuator/health {
        proxy_pass http://wasmtime4j_backend;
        access_log off;
    }
}
```

## Troubleshooting

### Common Issues and Solutions

#### Issue: High Memory Usage

**Symptoms:**
- OutOfMemoryError exceptions
- Slow garbage collection
- High heap usage

**Solutions:**
```java
// Monitor memory usage
@Component
public class MemoryMonitor {

    @Scheduled(fixedRate = 30000)
    public void checkMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        double usagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;

        if (usagePercent > 85) {
            logger.warn("High memory usage detected: {:.2f}%", usagePercent);

            // Force garbage collection
            System.gc();

            // Alert monitoring system
            alertingService.sendAlert(
                AlertLevel.HIGH,
                "High memory usage",
                String.format("Memory usage: %.2f%%", usagePercent)
            );
        }
    }
}
```

#### Issue: Performance Degradation

**Symptoms:**
- Slow response times
- High CPU usage
- Function call timeouts

**Solutions:**
```java
// Performance optimization
@Component
public class PerformanceOptimizer {

    public void optimizeRuntime() {
        // Enable compilation optimizations
        EngineConfig optimizedConfig = new EngineConfig()
            .optimizationLevel(OptimizationLevel.SPEED)
            .parallelCompilation(true)
            .enableTieredCompilation(true)
            .optimizationPasses(3);

        // Warm up frequently used modules
        warmupModules();

        // Tune garbage collection
        tuneGarbageCollection();
    }

    private void warmupModules() {
        // Pre-compile and cache frequently used modules
        commonModules.forEach(module -> {
            try {
                engine.compileModule(module);
            } catch (Exception e) {
                logger.warn("Failed to warm up module", e);
            }
        });
    }
}
```

### Diagnostic Tools

```bash
# JVM diagnostic commands
jstack <pid>              # Thread dump
jmap -dump:live,format=b,file=heap.hprof <pid>  # Heap dump
jstat -gc <pid> 5s        # GC statistics
jcmd <pid> VM.flags       # JVM flags

# System monitoring
top -p <pid>              # CPU and memory usage
iostat -x 1               # I/O statistics
netstat -tulpn            # Network connections

# Application logs
tail -f logs/wasmtime4j-app.log | grep ERROR
grep -i "outofmemory" logs/wasmtime4j-app.log
```

## Maintenance and Updates

### Update Strategy

```bash
#!/bin/bash
# update-deployment.sh

# 1. Backup current deployment
kubectl create backup wasmtime4j-backup-$(date +%Y%m%d)

# 2. Update application image
kubectl set image deployment/wasmtime4j-app \
    wasmtime4j-app=wasmtime4j-app:v1.1.0

# 3. Monitor rollout
kubectl rollout status deployment/wasmtime4j-app --timeout=300s

# 4. Verify health
kubectl exec deployment/wasmtime4j-app -- \
    curl -f http://localhost:8080/actuator/health

# 5. Run smoke tests
./run-smoke-tests.sh

# 6. If issues, rollback
if [ $? -ne 0 ]; then
    echo "Health check failed, rolling back..."
    kubectl rollout undo deployment/wasmtime4j-app
fi
```

### Monitoring Post-Deployment

```java
@Component
public class PostDeploymentMonitor {

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Run post-deployment checks
        scheduleHealthChecks();
        validateCriticalFunctionality();
        checkPerformanceBaseline();
    }

    private void validateCriticalFunctionality() {
        try {
            // Test critical WebAssembly functions
            testModuleLoading();
            testFunctionExecution();
            testMemoryOperations();

            logger.info("Post-deployment validation successful");
        } catch (Exception e) {
            logger.error("Post-deployment validation failed", e);
            alertingService.sendCriticalAlert("Deployment validation failed", e);
        }
    }
}
```

This production deployment guide provides comprehensive coverage of deploying Wasmtime4j applications to production environments. Follow these practices to ensure reliable, scalable, and secure deployments.