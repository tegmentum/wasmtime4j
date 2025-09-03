#!/bin/bash

# CI/CD Platform Validation Script for Wasmtime4j
# Comprehensive cross-platform testing and validation
# Usage: ./ci-validation.sh [platform] [runtime]

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
PLATFORM="${1:-auto}"
RUNTIME="${2:-auto}"

# Detect platform if auto
if [[ "$PLATFORM" == "auto" ]]; then
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if [[ "$(uname -m)" == "x86_64" ]]; then
            PLATFORM="linux-x86_64"
        elif [[ "$(uname -m)" == "aarch64" ]]; then
            PLATFORM="linux-aarch64"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        if [[ "$(uname -m)" == "x86_64" ]]; then
            PLATFORM="macos-x86_64"
        elif [[ "$(uname -m)" == "arm64" ]]; then
            PLATFORM="macos-aarch64"
        fi
    elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
        PLATFORM="windows-x86_64"
    fi
fi

# Detect runtime if auto
if [[ "$RUNTIME" == "auto" ]]; then
    java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [[ $java_version -ge 23 ]]; then
        RUNTIME="panama"
    else
        RUNTIME="jni"
    fi
fi

OUTPUT_DIR="$SCRIPT_DIR/ci-validation-results/$PLATFORM-$RUNTIME-$TIMESTAMP"
mkdir -p "$OUTPUT_DIR"

LOG_FILE="$OUTPUT_DIR/validation.log"
RESULTS_FILE="$OUTPUT_DIR/results.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $*" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*" | tee -a "$LOG_FILE"
}

log_info() {
    echo -e "${CYAN}[INFO]${NC} $*" | tee -a "$LOG_FILE"
}

# Initialize validation results
init_results() {
    cat > "$RESULTS_FILE" << EOF
{
  "platform": "$PLATFORM",
  "runtime": "$RUNTIME", 
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
  "validation_results": {
    "system_info": {},
    "native_library_validation": {},
    "unit_tests": {},
    "integration_tests": {},
    "performance_validation": {},
    "memory_validation": {},
    "overall_status": "running"
  }
}
EOF
}

# Update results JSON
update_result() {
    local section="$1"
    local key="$2"
    local value="$3"
    
    if command -v jq &> /dev/null; then
        local tmp_file=$(mktemp)
        jq --arg section "$section" --arg key "$key" --arg value "$value" \
           '.validation_results[$section][$key] = $value' "$RESULTS_FILE" > "$tmp_file"
        mv "$tmp_file" "$RESULTS_FILE"
    fi
}

# Collect system information
collect_system_info() {
    log "Collecting system information..."
    
    local java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    local os_name=$(uname -s)
    local os_version=$(uname -r)
    local arch=$(uname -m)
    local cpu_cores=$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo "unknown")
    
    # Memory information
    local total_memory="unknown"
    if command -v free &> /dev/null; then
        total_memory=$(free -m | awk '/^Mem:/{print $2}')
    elif command -v vm_stat &> /dev/null; then
        total_memory=$(vm_stat | grep "Pages free" | awk '{print $3}' | sed 's/\.//' | awk '{print $1 * 4 / 1024}')
    fi
    
    log_info "System Information:"
    log_info "  OS: $os_name $os_version"
    log_info "  Architecture: $arch"
    log_info "  CPU Cores: $cpu_cores"
    log_info "  Total Memory: ${total_memory}MB"
    log_info "  Java Version: $java_version"
    log_info "  Platform: $PLATFORM"
    log_info "  Runtime: $RUNTIME"
    
    update_result "system_info" "os_name" "$os_name"
    update_result "system_info" "os_version" "$os_version"
    update_result "system_info" "arch" "$arch"
    update_result "system_info" "cpu_cores" "$cpu_cores"
    update_result "system_info" "total_memory" "$total_memory"
    update_result "system_info" "java_version" "$java_version"
}

# Validate native libraries
validate_native_libraries() {
    log "Validating native libraries for $PLATFORM..."
    
    local lib_extension=""
    local lib_prefix=""
    
    case "$PLATFORM" in
        linux-*)
            lib_extension="so"
            lib_prefix="lib"
            ;;
        macos-*)
            lib_extension="dylib"
            lib_prefix="lib"
            ;;
        windows-*)
            lib_extension="dll"
            lib_prefix=""
            ;;
    esac
    
    local expected_lib="${lib_prefix}wasmtime4j.$lib_extension"
    local lib_found=false
    local lib_path=""
    
    # Search for native library in various locations
    for search_dir in \
        "$PROJECT_ROOT/wasmtime4j-native/target/classes/native/$PLATFORM" \
        "$PROJECT_ROOT/wasmtime4j-native/target/native-libs" \
        "$PROJECT_ROOT/target/classes/native/$PLATFORM" \
        "$PROJECT_ROOT/wasmtime4j-jni/target/classes/native" \
        "$PROJECT_ROOT/wasmtime4j-panama/target/classes/native"; do
        
        if [[ -d "$search_dir" ]]; then
            local found_lib=$(find "$search_dir" -name "*wasmtime4j*.$lib_extension" | head -1)
            if [[ -n "$found_lib" ]]; then
                lib_found=true
                lib_path="$found_lib"
                break
            fi
        fi
    done
    
    if [[ "$lib_found" == true ]]; then
        log_success "Native library found: $lib_path"
        
        # Validate library file
        if command -v file &> /dev/null; then
            local file_info=$(file "$lib_path")
            log_info "Library info: $file_info"
            update_result "native_library_validation" "file_info" "$file_info"
        fi
        
        # Check library dependencies (Linux/macOS only)
        if [[ "$PLATFORM" == linux-* ]] && command -v ldd &> /dev/null; then
            log_info "Library dependencies:"
            ldd "$lib_path" | tee -a "$LOG_FILE" || true
        elif [[ "$PLATFORM" == macos-* ]] && command -v otool &> /dev/null; then
            log_info "Library dependencies:"
            otool -L "$lib_path" | tee -a "$LOG_FILE" || true
        fi
        
        update_result "native_library_validation" "status" "success"
        update_result "native_library_validation" "library_path" "$lib_path"
    else
        log_error "Native library not found for $PLATFORM"
        update_result "native_library_validation" "status" "failed"
        update_result "native_library_validation" "error" "Native library not found"
        return 1
    fi
}

# Run unit tests
run_unit_tests() {
    log "Running unit tests with $RUNTIME runtime..."
    
    cd "$PROJECT_ROOT"
    
    local test_cmd=("./mvnw" "test" "-B" "-q")
    
    # Add runtime-specific options
    if [[ "$RUNTIME" == "panama" ]]; then
        test_cmd+=("-Dtest.runtime=panama")
    else
        test_cmd+=("-Dtest.runtime=jni")
    fi
    
    # Add platform-specific profile
    case "$PLATFORM" in
        linux-x86_64)
            test_cmd+=("-P" "linux-x86_64")
            ;;
        linux-aarch64)
            test_cmd+=("-P" "linux-aarch64")
            ;;
        macos-x86_64)
            test_cmd+=("-P" "macos-x86_64")
            ;;
        macos-aarch64)
            test_cmd+=("-P" "macos-aarch64")
            ;;
        windows-x86_64)
            test_cmd+=("-P" "windows-x86_64")
            ;;
    esac
    
    local test_start=$(date +%s)
    if "${test_cmd[@]}" 2>&1 | tee -a "$LOG_FILE"; then
        local test_end=$(date +%s)
        local test_duration=$((test_end - test_start))
        log_success "Unit tests passed in ${test_duration}s"
        update_result "unit_tests" "status" "success"
        update_result "unit_tests" "duration" "$test_duration"
    else
        log_error "Unit tests failed"
        update_result "unit_tests" "status" "failed"
        return 1
    fi
}

# Run integration tests
run_integration_tests() {
    log "Running integration tests..."
    
    cd "$PROJECT_ROOT"
    
    local verify_cmd=("./mvnw" "verify" "-B" "-q")
    
    if [[ "$RUNTIME" == "panama" ]]; then
        verify_cmd+=("-Dtest.runtime=panama")
    else
        verify_cmd+=("-Dtest.runtime=jni")
    fi
    
    local verify_start=$(date +%s)
    if "${verify_cmd[@]}" 2>&1 | tee -a "$LOG_FILE"; then
        local verify_end=$(date +%s)
        local verify_duration=$((verify_end - verify_start))
        log_success "Integration tests passed in ${verify_duration}s"
        update_result "integration_tests" "status" "success"
        update_result "integration_tests" "duration" "$verify_duration"
    else
        log_error "Integration tests failed"
        update_result "integration_tests" "status" "failed"
        return 1
    fi
}

# Run performance validation
run_performance_validation() {
    log "Running performance validation..."
    
    cd "$SCRIPT_DIR"
    
    # Quick performance test to ensure basic functionality
    local perf_cmd=("./run-performance-suite.sh" "--ci-validation" "--quiet")
    
    if [[ "$RUNTIME" == "panama" ]]; then
        perf_cmd+=("--panama-only")
    else
        perf_cmd+=("--jni-only")
    fi
    
    local perf_start=$(date +%s)
    if "${perf_cmd[@]}" > "$OUTPUT_DIR/performance.log" 2>&1; then
        local perf_end=$(date +%s)
        local perf_duration=$((perf_end - perf_start))
        log_success "Performance validation completed in ${perf_duration}s"
        update_result "performance_validation" "status" "success"
        update_result "performance_validation" "duration" "$perf_duration"
        
        # Copy performance results
        if [[ -d "benchmark-results" ]]; then
            cp -r benchmark-results/* "$OUTPUT_DIR/" 2>/dev/null || true
        fi
    else
        log_warning "Performance validation failed - not critical"
        update_result "performance_validation" "status" "warning"
        cat "$OUTPUT_DIR/performance.log" | tail -20 | tee -a "$LOG_FILE"
    fi
}

# Memory validation
run_memory_validation() {
    log "Running memory validation..."
    
    # Create a simple memory test
    cat > "$OUTPUT_DIR/MemoryTest.java" << 'EOF'
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class MemoryTest {
    public static void main(String[] args) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Initial memory
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("Initial Heap: " + formatBytes(heap.getUsed()) + " / " + formatBytes(heap.getMax()));
        System.out.println("Initial Non-Heap: " + formatBytes(nonHeap.getUsed()) + " / " + formatBytes(nonHeap.getMax()));
        
        // Force GC
        System.gc();
        Thread.yield();
        
        // Final memory
        heap = memoryBean.getHeapMemoryUsage();
        nonHeap = memoryBean.getNonHeapMemoryUsage();
        
        System.out.println("Final Heap: " + formatBytes(heap.getUsed()) + " / " + formatBytes(heap.getMax()));
        System.out.println("Final Non-Heap: " + formatBytes(nonHeap.getUsed()) + " / " + formatBytes(nonHeap.getMax()));
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
EOF
    
    if javac "$OUTPUT_DIR/MemoryTest.java" && java -cp "$OUTPUT_DIR" MemoryTest > "$OUTPUT_DIR/memory.log" 2>&1; then
        log_success "Memory validation completed"
        update_result "memory_validation" "status" "success"
        log_info "Memory usage:"
        cat "$OUTPUT_DIR/memory.log" | tee -a "$LOG_FILE"
    else
        log_warning "Memory validation failed"
        update_result "memory_validation" "status" "warning"
    fi
}

# Generate validation report
generate_report() {
    log "Generating validation report..."
    
    local report_file="$OUTPUT_DIR/validation-report.html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>CI/CD Validation Report - $PLATFORM - $RUNTIME</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background: #f5f5f5; padding: 20px; border-radius: 5px; }
        .success { color: green; }
        .warning { color: orange; }
        .error { color: red; }
        .section { margin: 20px 0; }
        .details { background: #f9f9f9; padding: 10px; border-left: 4px solid #ccc; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        pre { background: #f5f5f5; padding: 10px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="header">
        <h1>CI/CD Validation Report</h1>
        <p><strong>Platform:</strong> $PLATFORM</p>
        <p><strong>Runtime:</strong> $RUNTIME</p>
        <p><strong>Timestamp:</strong> $(date)</p>
    </div>
EOF

    # Add validation results from JSON
    if command -v jq &> /dev/null && [[ -f "$RESULTS_FILE" ]]; then
        echo "<div class='section'>" >> "$report_file"
        echo "<h2>Validation Results</h2>" >> "$report_file"
        echo "<table>" >> "$report_file"
        echo "<tr><th>Component</th><th>Status</th><th>Details</th></tr>" >> "$report_file"
        
        # Parse results and create table rows
        local components=("native_library_validation" "unit_tests" "integration_tests" "performance_validation" "memory_validation")
        for component in "${components[@]}"; do
            local status=$(jq -r ".validation_results.${component}.status // \"unknown\"" "$RESULTS_FILE")
            local duration=$(jq -r ".validation_results.${component}.duration // \"N/A\"" "$RESULTS_FILE")
            
            local status_class="unknown"
            case "$status" in
                "success") status_class="success" ;;
                "warning") status_class="warning" ;;
                "failed") status_class="error" ;;
            esac
            
            echo "<tr>" >> "$report_file"
            echo "<td>$(echo $component | tr '_' ' ' | sed 's/\b\w/\U&/g')</td>" >> "$report_file"
            echo "<td class='$status_class'>$status</td>" >> "$report_file"
            echo "<td>Duration: ${duration}s</td>" >> "$report_file"
            echo "</tr>" >> "$report_file"
        done
        
        echo "</table>" >> "$report_file"
        echo "</div>" >> "$report_file"
    fi
    
    # Add logs
    echo "<div class='section'>" >> "$report_file"
    echo "<h2>Execution Log</h2>" >> "$report_file"
    echo "<pre>" >> "$report_file"
    if [[ -f "$LOG_FILE" ]]; then
        # Remove ANSI color codes and add to HTML
        sed 's/\x1b\[[0-9;]*m//g' "$LOG_FILE" >> "$report_file"
    fi
    echo "</pre>" >> "$report_file"
    echo "</div>" >> "$report_file"
    
    echo "</body></html>" >> "$report_file"
    
    log_success "Validation report generated: $report_file"
}

# Main execution
main() {
    log_info "Starting CI/CD validation for $PLATFORM with $RUNTIME runtime"
    
    init_results
    
    # System information
    collect_system_info
    
    # Validation steps
    local overall_status="success"
    
    if ! validate_native_libraries; then
        overall_status="failed"
    fi
    
    if ! run_unit_tests; then
        overall_status="failed"
    fi
    
    if ! run_integration_tests; then
        overall_status="failed"
    fi
    
    # Performance and memory are not critical failures
    run_performance_validation
    run_memory_validation
    
    # Update overall status
    update_result "overall_status" "" "$overall_status"
    
    # Generate report
    generate_report
    
    # Final status
    if [[ "$overall_status" == "success" ]]; then
        log_success "CI/CD validation completed successfully"
        echo "validation_status=success"
        echo "validation_report=$OUTPUT_DIR/validation-report.html" 
        echo "validation_results=$RESULTS_FILE"
        exit 0
    else
        log_error "CI/CD validation failed"
        echo "validation_status=failed"
        echo "validation_report=$OUTPUT_DIR/validation-report.html"
        echo "validation_results=$RESULTS_FILE"
        exit 1
    fi
}

# Execute main function
main "$@"