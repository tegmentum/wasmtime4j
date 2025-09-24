# Multi-stage Dockerfile for Wasmtime4j production deployment
# Optimized for both development and production use cases

# Build stage - includes all build dependencies
FROM eclipse-temurin:23-jdk-jammy AS builder

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    build-essential \
    pkg-config \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Install Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
ENV PATH="/root/.cargo/bin:${PATH}"
RUN rustup default stable
RUN rustup target add \
    x86_64-unknown-linux-gnu \
    aarch64-unknown-linux-gnu

# Set working directory
WORKDIR /workspace

# Copy project files
COPY . .

# Build the project with all platform support
RUN ./mvnw clean package -P release -DskipTests -B

# Create a minimal runtime image
FROM eclipse-temurin:23-jre-jammy AS runtime

# Install minimal runtime dependencies
RUN apt-get update && apt-get install -y \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r wasmtime4j \
    && useradd -r -g wasmtime4j wasmtime4j

# Copy built artifacts from builder stage
COPY --from=builder /workspace/target/*.jar /opt/wasmtime4j/
COPY --from=builder /workspace/wasmtime4j/target/*.jar /opt/wasmtime4j/
COPY --from=builder /workspace/wasmtime4j-jni/target/*.jar /opt/wasmtime4j/
COPY --from=builder /workspace/wasmtime4j-panama/target/*.jar /opt/wasmtime4j/
COPY --from=builder /workspace/wasmtime4j-native/target/classes/native/ /opt/wasmtime4j/native/

# Create application directories
RUN mkdir -p /opt/wasmtime4j/wasm \
    && mkdir -p /opt/wasmtime4j/logs \
    && mkdir -p /opt/wasmtime4j/config \
    && chown -R wasmtime4j:wasmtime4j /opt/wasmtime4j

# Switch to non-root user
USER wasmtime4j

# Set working directory
WORKDIR /opt/wasmtime4j

# Configure JVM for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=75.0 \
              -XX:+UseG1GC \
              -XX:+UseStringDeduplication \
              -XX:+OptimizeStringConcat \
              -Djava.security.egd=file:/dev/./urandom \
              -Dfile.encoding=UTF-8"

# Configure Wasmtime4j settings
ENV WASMTIME4J_NATIVE_PATH="/opt/wasmtime4j/native"
ENV WASMTIME4J_LOG_LEVEL="INFO"
ENV WASMTIME4J_CACHE_SIZE="256MB"

# Health check endpoint (assuming a sample application)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD java -cp "/opt/wasmtime4j/*" ai.tegmentum.wasmtime4j.util.HealthCheck || exit 1

# Default command - runs a simple validation
CMD ["java", "-cp", "/opt/wasmtime4j/*", "ai.tegmentum.wasmtime4j.util.LibraryValidator"]

# Development variant with debugging enabled
FROM runtime AS development

USER root

# Install development tools
RUN apt-get update && apt-get install -y \
    htop \
    curl \
    netcat-openbsd \
    strace \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Enable JVM debugging
ENV JAVA_OPTS="${JAVA_OPTS} \
              -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
              -XX:+PrintGCDetails \
              -XX:+PrintGCTimeStamps \
              -Xloggc:/opt/wasmtime4j/logs/gc.log"

# Expose debugging port
EXPOSE 5005

USER wasmtime4j

# Benchmark variant optimized for performance testing
FROM runtime AS benchmark

# Copy benchmark tools
COPY --from=builder /workspace/wasmtime4j-benchmarks/target/*.jar /opt/wasmtime4j/benchmarks/

# Configure for benchmark execution
ENV JAVA_OPTS="-XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=90.0 \
              -XX:+UseG1GC \
              -XX:+UnlockExperimentalVMOptions \
              -XX:+UseTransparentHugePages \
              -XX:+AlwaysPreTouch"

CMD ["java", "-cp", "/opt/wasmtime4j/*:/opt/wasmtime4j/benchmarks/*", \
     "ai.tegmentum.wasmtime4j.benchmarks.BenchmarkRunner"]