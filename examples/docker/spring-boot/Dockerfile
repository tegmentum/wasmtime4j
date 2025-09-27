# Spring Boot Docker deployment for wasmtime4j applications
# Optimized for Spring Boot applications with wasmtime4j integration

# Build stage
FROM openjdk:17-jdk-slim AS builder

WORKDIR /build

# Install build tools
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy Maven/Gradle files for dependency caching
COPY pom.xml* mvnw* ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B || true

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Extract Spring Boot JAR layers for better caching
RUN java -Djarmode=layertools -jar target/*.jar extract

# Runtime stage
FROM openjdk:17-jre-slim AS runtime

WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring -m spring

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Copy Spring Boot layers for optimal caching
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# Create directories
RUN mkdir -p /app/logs /app/tmp /app/dumps && \
    chown -R spring:spring /app

# Set JVM options optimized for Spring Boot and containers
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/dumps/ \
    -Djava.awt.headless=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.io.tmpdir=/app/tmp"

# Spring Boot configuration
ENV SPRING_PROFILES_ACTIVE="docker" \
    SPRING_OUTPUT_ANSI_ENABLED="ALWAYS" \
    LOGGING_LEVEL_AI_TEGMENTUM_WASMTIME4J="INFO"

# Wasmtime4j configuration
ENV WASMTIME4J_RUNTIME="auto" \
    WASMTIME4J_DEBUG="false"

# Management and monitoring
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="health,info,metrics,prometheus" \
    MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS="when_authorized" \
    MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED="true"

# Expose ports
EXPOSE 8080 8081

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Switch to non-root user
USER spring

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]

# Labels
LABEL org.opencontainers.image.title="Wasmtime4j Spring Boot Application"
LABEL org.opencontainers.image.description="Spring Boot application with wasmtime4j integration"
LABEL org.springframework.boot.version="3.1.5"