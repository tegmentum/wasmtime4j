#!/bin/sh

# Production entrypoint script for wasmtime4j applications
# Provides configuration validation, environment setup, and graceful startup

set -e

# Configuration
APP_JAR="/app/app.jar"
CONFIG_DIR="/app/config"
NATIVE_LIB_DIR="/app/native/lib"
LOG_DIR="/app/logs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validate environment
validate_environment() {
    log_info "Validating production environment..."

    # Check required files
    if [ ! -f "$APP_JAR" ]; then
        log_error "Application JAR not found: $APP_JAR"
        exit 1
    fi

    if [ ! -d "$NATIVE_LIB_DIR" ]; then
        log_error "Native library directory not found: $NATIVE_LIB_DIR"
        exit 1
    fi

    # Check Java version
    java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
    log_info "Java version: $java_version"

    # Validate memory settings
    if [ -z "$JAVA_OPTS" ]; then
        log_warn "JAVA_OPTS not set, using defaults"
        export JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
    fi

    # Check available memory
    available_memory=$(cat /proc/meminfo | grep MemAvailable | awk '{print $2}')
    available_memory_mb=$((available_memory / 1024))
    log_info "Available memory: ${available_memory_mb}MB"

    if [ "$available_memory_mb" -lt 512 ]; then
        log_warn "Low memory available: ${available_memory_mb}MB. Consider increasing container memory."
    fi
}

# Configure native libraries
configure_native_libraries() {
    log_info "Configuring native libraries..."

    # Set library path
    export LD_LIBRARY_PATH="$NATIVE_LIB_DIR:$LD_LIBRARY_PATH"

    # Verify native libraries exist
    if [ ! -f "$NATIVE_LIB_DIR/libwasmtime4j_native.so" ]; then
        log_warn "Native library not found, checking alternative locations..."

        # Try to find the library
        native_lib=$(find /app -name "libwasmtime4j_native.*" | head -n1)
        if [ -n "$native_lib" ]; then
            log_info "Found native library: $native_lib"
            export WASMTIME_NATIVE_LIBRARY_PATH=$(dirname "$native_lib")
        else
            log_error "No native library found. Application may not start correctly."
        fi
    else
        log_info "Native library verified: $NATIVE_LIB_DIR/libwasmtime4j_native.so"
    fi
}

# Configure logging
configure_logging() {
    log_info "Configuring logging..."

    # Create log directory if it doesn't exist
    mkdir -p "$LOG_DIR"

    # Set logging configuration
    if [ -z "$WASMTIME_LOGGING_LEVEL" ]; then
        export WASMTIME_LOGGING_LEVEL="INFO"
    fi

    # Configure Java logging
    export JAVA_OPTS="$JAVA_OPTS -Djava.util.logging.config.file=/app/config/logging.properties"

    log_info "Logging level: $WASMTIME_LOGGING_LEVEL"
}

# Configure wasmtime4j settings
configure_wasmtime() {
    log_info "Configuring wasmtime4j..."

    # Set default environment if not specified
    if [ -z "$WASMTIME_ENVIRONMENT" ]; then
        export WASMTIME_ENVIRONMENT="production"
    fi

    # Set default security level for production
    if [ -z "$WASMTIME_SECURITY_LEVEL" ]; then
        export WASMTIME_SECURITY_LEVEL="STRICT"
    fi

    # Enable sandboxing by default in production
    if [ -z "$WASMTIME_ENABLE_SANDBOXING" ]; then
        export WASMTIME_ENABLE_SANDBOXING="true"
    fi

    # Set engine pool size based on available CPU cores
    if [ -z "$WASMTIME_ENGINE_POOL_SIZE" ]; then
        cpu_cores=$(nproc)
        pool_size=$((cpu_cores * 2))
        if [ "$pool_size" -gt 20 ]; then
            pool_size=20
        fi
        export WASMTIME_ENGINE_POOL_SIZE="$pool_size"
    fi

    log_info "Wasmtime environment: $WASMTIME_ENVIRONMENT"
    log_info "Security level: $WASMTIME_SECURITY_LEVEL"
    log_info "Engine pool size: $WASMTIME_ENGINE_POOL_SIZE"
    log_info "Sandboxing enabled: $WASMTIME_ENABLE_SANDBOXING"
}

# Configure monitoring and observability
configure_monitoring() {
    log_info "Configuring monitoring..."

    # Enable metrics by default
    if [ -z "$WASMTIME_ENABLE_METRICS" ]; then
        export WASMTIME_ENABLE_METRICS="true"
    fi

    # Enable health checks
    if [ -z "$WASMTIME_ENABLE_HEALTH_CHECKS" ]; then
        export WASMTIME_ENABLE_HEALTH_CHECKS="true"
    fi

    # Configure JMX for monitoring (if enabled)
    if [ "$WASMTIME_ENABLE_JMX" = "true" ]; then
        export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote"
        export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=9999"
        export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.authenticate=false"
        export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.ssl=false"
        log_info "JMX monitoring enabled on port 9999"
    fi

    log_info "Metrics enabled: $WASMTIME_ENABLE_METRICS"
    log_info "Health checks enabled: $WASMTIME_ENABLE_HEALTH_CHECKS"
}

# Setup signal handlers for graceful shutdown
setup_signal_handlers() {
    log_info "Setting up signal handlers for graceful shutdown..."

    # Function to handle shutdown signals
    shutdown_handler() {
        log_info "Received shutdown signal, initiating graceful shutdown..."

        # Send TERM signal to Java process
        if [ -n "$JAVA_PID" ]; then
            kill -TERM "$JAVA_PID"

            # Wait for graceful shutdown with timeout
            timeout=30
            while [ $timeout -gt 0 ] && kill -0 "$JAVA_PID" 2>/dev/null; do
                sleep 1
                timeout=$((timeout - 1))
            done

            # Force kill if still running
            if kill -0 "$JAVA_PID" 2>/dev/null; then
                log_warn "Graceful shutdown timeout, forcing termination"
                kill -KILL "$JAVA_PID"
            else
                log_info "Application shut down gracefully"
            fi
        fi

        exit 0
    }

    # Register signal handlers
    trap shutdown_handler TERM INT QUIT
}

# Perform pre-start health check
pre_start_health_check() {
    log_info "Performing pre-start health check..."

    # Check disk space
    disk_usage=$(df /app | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$disk_usage" -gt 90 ]; then
        log_error "Disk usage is ${disk_usage}%, which is above 90% threshold"
        exit 1
    fi

    # Test Java application startup (dry run)
    log_info "Testing application configuration..."
    if ! java $JAVA_OPTS -cp "$APP_JAR" ai.tegmentum.wasmtime4j.production.ProductionWasmtimeConfig --validate-only >/dev/null 2>&1; then
        log_warn "Configuration validation failed, proceeding with caution"
    else
        log_info "Configuration validation passed"
    fi
}

# Main execution
main() {
    log_info "Starting wasmtime4j production application..."
    log_info "Container: $(hostname)"
    log_info "User: $(whoami)"
    log_info "Working directory: $(pwd)"

    # Perform all configuration steps
    validate_environment
    configure_native_libraries
    configure_logging
    configure_wasmtime
    configure_monitoring
    setup_signal_handlers
    pre_start_health_check

    # Log final configuration
    log_info "Final Java options: $JAVA_OPTS"
    log_info "Application arguments: $*"

    # Start the application
    log_info "Starting application..."
    if [ $# -eq 0 ]; then
        # No arguments provided, use default command
        exec java $JAVA_OPTS -jar "$APP_JAR" &
    else
        # Execute provided command
        exec "$@" &
    fi

    # Store process ID for signal handling
    JAVA_PID=$!

    # Wait for the process to complete
    wait $JAVA_PID
}

# Execute main function with all arguments
main "$@"