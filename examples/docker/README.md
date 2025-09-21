# Docker Deployment Examples

This directory contains comprehensive Docker deployment patterns and examples for wasmtime4j applications.

## Quick Start

### Basic Deployment

```dockerfile
FROM openjdk:17-jre-slim
COPY target/my-app.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
```

### Multi-Stage Build

See [`multi-stage/`](multi-stage/) for optimized production builds.

### Spring Boot Application

See [`spring-boot/`](spring-boot/) for complete Spring Boot deployment.

## Examples

| Directory | Description |
|-----------|-------------|
| [`basic/`](basic/) | Simple single-stage Docker deployment |
| [`multi-stage/`](multi-stage/) | Optimized multi-stage builds |
| [`spring-boot/`](spring-boot/) | Spring Boot web application deployment |
| [`microservice/`](microservice/) | Microservice architecture patterns |
| [`kubernetes/`](kubernetes/) | Kubernetes deployment configurations |

## Container Optimization

### Image Size Optimization

1. **Use minimal base images**: `openjdk:17-jre-slim` or `eclipse-temurin:17-jre-alpine`
2. **Multi-stage builds**: Separate build and runtime environments
3. **Layer optimization**: Group related operations to reduce layers
4. **Dependency caching**: Leverage Docker layer caching

### Performance Optimization

1. **JVM tuning**: Optimize heap size and GC settings
2. **Native library handling**: Ensure proper native library loading
3. **Resource allocation**: Set appropriate CPU and memory limits
4. **Startup time**: Use application class data sharing (CDS)

## Security Best Practices

### Container Security

1. **Non-root user**: Run applications as non-root user
2. **Minimal attack surface**: Use distroless or minimal base images
3. **Security scanning**: Scan images for vulnerabilities
4. **Resource limits**: Set appropriate resource constraints

### Runtime Security

1. **Read-only filesystem**: Mount root filesystem as read-only
2. **Capability dropping**: Drop unnecessary Linux capabilities
3. **Secret management**: Use proper secret injection mechanisms
4. **Network segmentation**: Configure appropriate network policies

## Deployment Patterns

### Single Container

Simple deployment for development and small applications:

```yaml
version: '3.8'
services:
  wasmtime-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - WASMTIME4J_RUNTIME=auto
```

### Microservices

Multiple services with shared infrastructure:

```yaml
version: '3.8'
services:
  wasmtime-service-1:
    build: ./service1
    ports:
      - "8081:8080"

  wasmtime-service-2:
    build: ./service2
    ports:
      - "8082:8080"
```

### Kubernetes

Production-ready Kubernetes deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wasmtime-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: wasmtime-app
  template:
    metadata:
      labels:
        app: wasmtime-app
    spec:
      containers:
      - name: app
        image: wasmtime-app:latest
        ports:
        - containerPort: 8080
```

## Environment Configuration

### Runtime Selection

Configure wasmtime4j runtime through environment variables:

```bash
# Automatic runtime selection
WASMTIME4J_RUNTIME=auto

# Force JNI runtime
WASMTIME4J_RUNTIME=jni

# Force Panama runtime (Java 23+)
WASMTIME4J_RUNTIME=panama
```

### JVM Configuration

Optimize JVM settings for containerized environments:

```bash
# Memory settings
JAVA_OPTS="-Xmx1g -XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

# GC settings
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=100"

# Debug settings (development only)
JAVA_OPTS="$JAVA_OPTS -Dwasmtime4j.debug=true"
```

### Logging Configuration

Configure logging for containerized applications:

```bash
# JSON logging for structured logs
LOGGING_CONFIG=logback-json.xml

# Console logging for development
LOGGING_CONFIG=logback-console.xml
```

## Monitoring and Observability

### Health Checks

Configure proper health checks:

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### Metrics Collection

Enable metrics collection:

```yaml
environment:
  - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
  - MANAGEMENT_ENDPOINT_METRICS_ENABLED=true
```

### Log Aggregation

Configure log forwarding:

```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
    labels: "service=wasmtime-app"
```

## Troubleshooting

### Common Issues

#### Native Library Loading

**Problem**: `UnsatisfiedLinkError` in container
**Solution**: Ensure native libraries are properly included:

```dockerfile
# Verify native libraries are copied
RUN ls -la /app/lib/
RUN ldd /app/lib/libwasmtime4j_native.so
```

#### Memory Issues

**Problem**: Out of memory errors
**Solution**: Tune JVM memory settings:

```dockerfile
ENV JAVA_OPTS="-Xmx1g -XX:+UseContainerSupport"
```

#### Architecture Mismatch

**Problem**: Wrong architecture native libraries
**Solution**: Use multi-arch builds:

```dockerfile
FROM --platform=$BUILDPLATFORM openjdk:17-jdk AS builder
# Build stage

FROM openjdk:17-jre-slim
# Runtime stage
```

### Debugging

#### Container Debugging

Debug running containers:

```bash
# Shell access
docker exec -it container-name /bin/bash

# Log inspection
docker logs -f container-name

# Resource usage
docker stats container-name
```

#### Application Debugging

Enable debug mode:

```bash
# Run with debug logging
docker run -e WASMTIME4J_DEBUG=true app:latest

# Remote debugging
docker run -p 5005:5005 -e JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" app:latest
```

## Getting Started

1. Choose the deployment pattern that matches your use case
2. Copy the relevant Dockerfile and configuration
3. Adapt the configuration to your application
4. Build and test the container locally
5. Deploy to your target environment

## Requirements

- **Docker**: 20.10+
- **Docker Compose**: 2.0+ (for multi-container examples)
- **Kubernetes**: 1.20+ (for Kubernetes examples)
- **Base Images**: Support for your target architecture (x86_64, ARM64)