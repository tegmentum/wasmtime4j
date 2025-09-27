# Multi-stage Docker build for wasmtime4j applications
# This Dockerfile demonstrates optimized production builds with separate build and runtime stages

# Build stage
FROM openjdk:17-jdk-slim AS builder

# Set working directory
WORKDIR /build

# Install build dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

# Copy build files
COPY pom.xml* ./
COPY mvnw* ./
COPY .mvn .mvn

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests -B

# Extract native libraries for verification
RUN mkdir -p target/extracted-natives && \
    cd target/extracted-natives && \
    java -cp ../classes -Dwasmtime4j.extract.natives=true -Dwasmtime4j.debug=true \
    ai.tegmentum.wasmtime4j.internal.NativeLibraryLoader || true

# Runtime stage
FROM openjdk:17-jre-slim AS runtime

# Set working directory
WORKDIR /app

# Create non-root user
RUN groupadd -r wasmtime && useradd -r -g wasmtime -m wasmtime

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Copy application from build stage
COPY --from=builder /build/target/*.jar app.jar

# Copy native libraries if extracted
COPY --from=builder /build/target/extracted-natives/ lib/ 2>/dev/null || true

# Set optimal JVM settings for containers
ENV JAVA_OPTS="-server \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/dumps/ \
    -Djava.security.egd=file:/dev/./urandom"

# Set wasmtime4j configuration
ENV WASMTIME4J_RUNTIME="auto" \
    WASMTIME4J_DEBUG="false"

# Create directories for logs and dumps
RUN mkdir -p /app/logs /app/dumps && \
    chown -R wasmtime:wasmtime /app

# Expose application port
EXPOSE 8080

# Add comprehensive health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Switch to non-root user
USER wasmtime

# Use exec form for proper signal handling
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

# Add labels for metadata
LABEL org.opencontainers.image.title="Wasmtime4j Application"
LABEL org.opencontainers.image.description="Production-ready wasmtime4j application"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.authors="Wasmtime4j Team"