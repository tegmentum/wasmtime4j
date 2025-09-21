#!/bin/sh

# Production health check script for wasmtime4j applications
# Performs comprehensive health validation including application, resources, and dependencies

set -e

# Configuration
HEALTH_ENDPOINT="http://localhost:8080/health"
READY_ENDPOINT="http://localhost:8080/ready"
METRICS_ENDPOINT="http://localhost:8080/metrics"
TIMEOUT=10
MAX_RETRIES=3

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Logging functions
log_info() {
    echo -e "${GREEN}[HEALTH]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[HEALTH]${NC} $1"
}

log_error() {
    echo -e "${RED}[HEALTH]${NC} $1"
}

# Check if curl is available
check_curl() {
    if ! command -v curl >/dev/null 2>&1; then
        log_error "curl is not available for health checks"
        return 1
    fi
    return 0
}

# Check basic HTTP endpoint
check_http_endpoint() {
    local endpoint=$1
    local expected_status=${2:-200}
    local description=${3:-"endpoint"}

    local response
    local status_code

    response=$(curl -s -w "%{http_code}" --connect-timeout $TIMEOUT --max-time $TIMEOUT "$endpoint" 2>/dev/null || echo "000")
    status_code=${response: -3}

    if [ "$status_code" = "$expected_status" ]; then
        log_info "$description check passed (status: $status_code)"
        return 0
    else
        log_error "$description check failed (status: $status_code, expected: $expected_status)"
        return 1
    fi
}

# Check application health endpoint
check_application_health() {
    log_info "Checking application health..."

    local retry=0
    while [ $retry -lt $MAX_RETRIES ]; do
        if check_http_endpoint "$HEALTH_ENDPOINT" "200" "Application health"; then
            return 0
        fi

        retry=$((retry + 1))
        if [ $retry -lt $MAX_RETRIES ]; then
            log_warn "Health check failed, retrying in 2 seconds... ($retry/$MAX_RETRIES)"
            sleep 2
        fi
    done

    return 1
}

# Check application readiness
check_application_readiness() {
    log_info "Checking application readiness..."

    if check_http_endpoint "$READY_ENDPOINT" "200" "Application readiness"; then
        return 0
    else
        log_warn "Application is not ready yet"
        return 1
    fi
}

# Check system resources
check_system_resources() {
    log_info "Checking system resources..."

    local health_status=0

    # Check memory usage
    if [ -f /proc/meminfo ]; then
        local total_memory=$(grep MemTotal /proc/meminfo | awk '{print $2}')
        local available_memory=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
        local memory_usage=$((100 - (available_memory * 100 / total_memory)))

        if [ $memory_usage -gt 90 ]; then
            log_error "Critical memory usage: ${memory_usage}%"
            health_status=1
        elif [ $memory_usage -gt 80 ]; then
            log_warn "High memory usage: ${memory_usage}%"
        else
            log_info "Memory usage: ${memory_usage}%"
        fi
    fi

    # Check disk space
    local disk_usage=$(df /app | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$disk_usage" -gt 95 ]; then
        log_error "Critical disk usage: ${disk_usage}%"
        health_status=1
    elif [ "$disk_usage" -gt 85 ]; then
        log_warn "High disk usage: ${disk_usage}%"
    else
        log_info "Disk usage: ${disk_usage}%"
    fi

    # Check load average
    if [ -f /proc/loadavg ]; then
        local load_avg=$(cat /proc/loadavg | awk '{print $1}')
        local cpu_cores=$(nproc)
        local load_per_core=$(echo "$load_avg $cpu_cores" | awk '{print $1/$2}')

        # Check if load average is more than 2x the number of cores
        if [ "$(echo "$load_per_core > 2" | bc 2>/dev/null || echo 0)" = "1" ]; then
            log_warn "High load average: $load_avg (${load_per_core} per core)"
        else
            log_info "Load average: $load_avg"
        fi
    fi

    return $health_status
}

# Check wasmtime4j specific health indicators
check_wasmtime_health() {
    log_info "Checking wasmtime4j specific health..."

    # Check if native libraries are loaded
    if [ -n "$WASMTIME_NATIVE_LIBRARY_PATH" ] && [ -d "$WASMTIME_NATIVE_LIBRARY_PATH" ]; then
        log_info "Native library path configured: $WASMTIME_NATIVE_LIBRARY_PATH"
    else
        log_warn "Native library path not configured or invalid"
    fi

    # Check application logs for errors (if available)
    local log_file="/app/logs/application.log"
    if [ -f "$log_file" ]; then
        local recent_errors=$(tail -100 "$log_file" | grep -i "error\|exception\|failed" | wc -l)
        if [ "$recent_errors" -gt 5 ]; then
            log_warn "Recent errors found in logs: $recent_errors"
        else
            log_info "Application logs look healthy"
        fi
    fi

    return 0
}

# Check metrics endpoint for detailed health information
check_metrics_health() {
    log_info "Checking metrics health..."

    local metrics_response
    metrics_response=$(curl -s --connect-timeout $TIMEOUT --max-time $TIMEOUT "$METRICS_ENDPOINT" 2>/dev/null || echo "")

    if [ -n "$metrics_response" ]; then
        # Check for specific metrics that indicate health
        if echo "$metrics_response" | grep -q "wasmtime.*healthy"; then
            log_info "Metrics indicate healthy wasmtime operations"
        fi

        # Check error rates in metrics
        local error_count=$(echo "$metrics_response" | grep -o "wasmtime_error_total [0-9]*" | awk '{sum+=$2} END {print sum+0}')
        local success_count=$(echo "$metrics_response" | grep -o "wasmtime_success_total [0-9]*" | awk '{sum+=$2} END {print sum+0}')

        if [ "$error_count" -gt 0 ] && [ "$success_count" -gt 0 ]; then
            local error_rate=$(echo "$error_count $success_count" | awk '{print ($1/($1+$2))*100}')
            if [ "$(echo "$error_rate > 10" | bc 2>/dev/null || echo 0)" = "1" ]; then
                log_warn "High error rate detected: ${error_rate}%"
            else
                log_info "Error rate within acceptable limits: ${error_rate}%"
            fi
        fi

        return 0
    else
        log_warn "Metrics endpoint not available"
        return 1
    fi
}

# Perform startup health check (more lenient)
startup_health_check() {
    log_info "Performing startup health check..."

    # Basic system checks
    check_system_resources

    # Check if the application port is listening
    if netstat -ln 2>/dev/null | grep -q ":8080 "; then
        log_info "Application port 8080 is listening"
    else
        log_warn "Application port 8080 is not yet listening"
        return 1
    fi

    # Try basic health endpoint (may not be ready yet)
    if check_http_endpoint "$HEALTH_ENDPOINT" "200" "Basic health" 2>/dev/null; then
        log_info "Application health endpoint is responding"
        return 0
    else
        log_info "Application is still starting up"
        return 1
    fi
}

# Perform comprehensive health check
comprehensive_health_check() {
    log_info "Performing comprehensive health check..."

    local overall_status=0

    # Check curl availability
    if ! check_curl; then
        return 1
    fi

    # Application health checks
    if ! check_application_health; then
        overall_status=1
    fi

    if ! check_application_readiness; then
        # Readiness failure is not critical for health check
        log_info "Application not ready, but may still be healthy"
    fi

    # System resource checks
    if ! check_system_resources; then
        overall_status=1
    fi

    # Wasmtime-specific checks
    if ! check_wasmtime_health; then
        # Non-critical, just informational
        log_info "Wasmtime health check completed with warnings"
    fi

    # Metrics checks (optional)
    check_metrics_health

    return $overall_status
}

# Main health check logic
main() {
    local check_type=${1:-"comprehensive"}

    case "$check_type" in
        "startup")
            startup_health_check
            ;;
        "comprehensive")
            comprehensive_health_check
            ;;
        *)
            log_error "Unknown health check type: $check_type"
            echo "Usage: $0 [startup|comprehensive]"
            exit 1
            ;;
    esac

    local exit_code=$?

    if [ $exit_code -eq 0 ]; then
        log_info "Health check PASSED"
    else
        log_error "Health check FAILED"
    fi

    exit $exit_code
}

# Execute main function
main "$@"