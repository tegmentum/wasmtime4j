# Basic Docker deployment for wasmtime4j applications
# This Dockerfile demonstrates a simple single-stage build suitable for development and simple deployments

FROM openjdk:17-jre-slim

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -r wasmtime && useradd -r -g wasmtime wasmtime

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy application JAR (build artifact)
# This assumes you've already built your application with Maven or Gradle
COPY target/*.jar app.jar

# Copy native libraries if they exist
# wasmtime4j will extract native libraries automatically, but you can pre-extract them
COPY target/natives/ lib/ 2>/dev/null || true

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport" \
    WASMTIME4J_RUNTIME="auto" \
    WASMTIME4J_DEBUG="false"

# Configure JVM for container environment
ENV JAVA_OPTS="$JAVA_OPTS -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Expose application port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Change ownership to non-root user
RUN chown -R wasmtime:wasmtime /app

# Switch to non-root user
USER wasmtime

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]