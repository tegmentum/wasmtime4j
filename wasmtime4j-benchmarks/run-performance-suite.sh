#!/bin/bash

# Wasmtime4j Comprehensive Performance Suite
# Advanced JMH benchmark runner with CI/CD integration, regression detection, and performance validation
# Usage: ./run-performance-suite.sh [options]

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OUTPUT_DIR="$SCRIPT_DIR/benchmark-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RESULTS_FILE="$OUTPUT_DIR/jmh-results-$TIMESTAMP.json"
LOG_FILE="$OUTPUT_DIR/benchmark-execution-$TIMESTAMP.log"

# Default JMH parameters
DEFAULT_ITERATIONS=5
DEFAULT_WARMUP_ITERATIONS=3
DEFAULT_FORKS=2
DEFAULT_THREADS=1
DEFAULT_TIME=2
DEFAULT_TIMEOUT=600

# CI/CD integration
CI_MODE=false
FAIL_ON_REGRESSION=false
PERFORMANCE_BASELINE=""
COMPARISON_MODE=false
GENERATE_REPORTS=true
VISUALIZE=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

# Show comprehensive usage
show_usage() {
    cat << EOF
Wasmtime4j Comprehensive Performance Suite

USAGE:
    $0 [OPTIONS] [BENCHMARK_PATTERNS...]

OPTIONS:
    Performance Configuration:
        -i, --iterations NUM        Number of measurement iterations (default: $DEFAULT_ITERATIONS)
        -wi, --warmup-iterations NUM Number of warmup iterations (default: $DEFAULT_WARMUP_ITERATIONS)
        -f, --forks NUM             Number of benchmark forks (default: $DEFAULT_FORKS)
        -t, --threads NUM           Number of benchmark threads (default: $DEFAULT_THREADS)
        --time SECONDS              Time per iteration in seconds (default: $DEFAULT_TIME)
        --timeout SECONDS           Benchmark timeout in seconds (default: $DEFAULT_TIMEOUT)

    Runtime Selection:
        --jni-only                  Run only JNI benchmarks
        --panama-only               Run only Panama benchmarks
        --compare-runtimes          Run comparative analysis between runtimes

    Benchmark Categories:
        --all                       Run all benchmarks (default)
        --core                      Core performance benchmarks only
        --memory                    Memory allocation and GC pressure benchmarks
        --throughput                Throughput-focused benchmarks
        --latency                   Latency-focused benchmarks
        --optimization              Performance optimization benchmarks

    CI/CD Integration:
        --ci                        CI/CD mode - machine readable output
        --fail-on-regression        Exit with error code if regression detected
        --baseline FILE             Compare against performance baseline
        --threshold PERCENT         Regression threshold percentage (default: 5%)

    Output and Reporting:
        -o, --output DIR            Output directory (default: $OUTPUT_DIR)
        --no-reports                Skip HTML/CSV report generation
        --json-only                 Output only JSON results
        --visualize                 Open results in jmh.morethan.io after completion
        --verbose                   Verbose logging
        --quiet                     Minimal output

    Profiles (Quick Presets):
        --quick                     Quick run: 1 iteration, 1 warmup, 1 fork
        --standard                  Standard run: default parameters
        --thorough                  Thorough run: 10 iterations, 5 warmups, 3 forks
        --ci-validation             CI validation: 3 iterations, 2 warmups, 2 forks

BENCHMARK PATTERNS:
    Specify benchmark class patterns to run specific benchmarks:
    - ComparisonBenchmark       - JNI vs Panama comparisons
    - PerformanceOptimization   - Optimization-focused benchmarks
    - MemoryOperation          - Memory and GC benchmarks
    - FunctionExecution        - Function call benchmarks
    - ModuleOperation          - Module compilation benchmarks
    - RuntimeInitialization    - Runtime startup benchmarks

EXAMPLES:
    # Run all benchmarks with default settings
    $0

    # Quick performance check
    $0 --quick --compare-runtimes

    # CI/CD validation with regression detection
    $0 --ci --fail-on-regression --baseline baseline.json

    # Thorough memory analysis
    $0 --thorough --memory MemoryOperation

    # Runtime comparison analysis
    $0 --compare-runtimes --standard ComparisonBenchmark

    # Generate comprehensive performance report
    $0 --thorough --all --output ./performance-analysis

ENVIRONMENT VARIABLES:
    JAVA_HOME                   Java installation path
    JMH_EXTRA_ARGS             Additional JMH arguments
    WASMTIME4J_BENCHMARK_OPTS  Additional benchmark options

EOF
}

# Parse command line arguments
parse_arguments() {
    local iterations=$DEFAULT_ITERATIONS
    local warmup_iterations=$DEFAULT_WARMUP_ITERATIONS
    local forks=$DEFAULT_FORKS
    local threads=$DEFAULT_THREADS
    local time=$DEFAULT_TIME
    local timeout=$DEFAULT_TIMEOUT
    local runtime_filter=""
    local category_filter=""
    local output_dir="$OUTPUT_DIR"
    local profile=""
    local benchmark_patterns=()
    local regression_threshold=5.0
    local verbose=false
    local quiet=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -i|--iterations)
                iterations="$2"
                shift 2
                ;;
            -wi|--warmup-iterations)
                warmup_iterations="$2"
                shift 2
                ;;
            -f|--forks)
                forks="$2"
                shift 2
                ;;
            -t|--threads)
                threads="$2"
                shift 2
                ;;
            --time)
                time="$2"
                shift 2
                ;;
            --timeout)
                timeout="$2"
                shift 2
                ;;
            --jni-only)
                runtime_filter=".*JNI.*"
                shift
                ;;
            --panama-only)
                runtime_filter=".*PANAMA.*"
                shift
                ;;
            --compare-runtimes)
                COMPARISON_MODE=true
                shift
                ;;
            --all)
                category_filter=".*"
                shift
                ;;
            --core)
                category_filter=".*ComparisonBenchmark.*|.*FunctionExecutionBenchmark.*"
                shift
                ;;
            --memory)
                category_filter=".*MemoryOperation.*|.*PerformanceOptimization.*"
                shift
                ;;
            --throughput)
                category_filter=".*Throughput.*"
                shift
                ;;
            --latency)
                category_filter=".*Latency.*|.*Overhead.*"
                shift
                ;;
            --optimization)
                category_filter=".*PerformanceOptimization.*"
                shift
                ;;
            --ci)
                CI_MODE=true
                quiet=true
                shift
                ;;
            --fail-on-regression)
                FAIL_ON_REGRESSION=true
                shift
                ;;
            --baseline)
                PERFORMANCE_BASELINE="$2"
                shift 2
                ;;
            --threshold)
                regression_threshold="$2"
                shift 2
                ;;
            -o|--output)
                output_dir="$2"
                shift 2
                ;;
            --no-reports)
                GENERATE_REPORTS=false
                shift
                ;;
            --json-only)
                GENERATE_REPORTS=false
                shift
                ;;
            --verbose)
                verbose=true
                shift
                ;;
            --quiet)
                quiet=true
                shift
                ;;
            --visualize)
                VISUALIZE=true
                shift
                ;;
            --quick)
                profile="quick"
                iterations=1
                warmup_iterations=1
                forks=1
                shift
                ;;
            --standard)
                profile="standard"
                shift
                ;;
            --thorough)
                profile="thorough"
                iterations=10
                warmup_iterations=5
                forks=3
                shift
                ;;
            --ci-validation)
                profile="ci-validation"
                iterations=3
                warmup_iterations=2
                forks=2
                CI_MODE=true
                shift
                ;;
            -*)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                benchmark_patterns+=("$1")
                shift
                ;;
        esac
    done

    # Store parsed values in global variables
    ITERATIONS=$iterations
    WARMUP_ITERATIONS=$warmup_iterations
    FORKS=$forks
    THREADS=$threads
    TIME=$time
    TIMEOUT=$timeout
    RUNTIME_FILTER=$runtime_filter
    CATEGORY_FILTER=$category_filter
    OUTPUT_DIR=$output_dir
    PROFILE=$profile
    BENCHMARK_PATTERNS=("${benchmark_patterns[@]}")
    REGRESSION_THRESHOLD=$regression_threshold
    VERBOSE=$verbose
    QUIET=$quiet

    # Update file paths based on output directory
    RESULTS_FILE="$OUTPUT_DIR/jmh-results-$TIMESTAMP.json"
    LOG_FILE="$OUTPUT_DIR/benchmark-execution-$TIMESTAMP.log"
}

# System validation
validate_system() {
    log "Validating system requirements..."

    # Check Java version
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install Java and set JAVA_HOME"
        exit 1
    fi

    local java_version
    java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    log_info "Java version: $java_version"

    # Check if Panama is available (Java 23+)
    local java_major
    java_major=$(echo "$java_version" | cut -d'.' -f1)
    if [[ $java_major -ge 23 ]]; then
        log_info "Panama FFI support available"
    else
        log_warning "Panama FFI not available in Java $java_major"
        if [[ "$RUNTIME_FILTER" == ".*PANAMA.*" ]]; then
            log_error "Cannot run Panama-only benchmarks on Java $java_major"
            exit 1
        fi
    fi

    # Check available memory
    local total_mem
    if command -v free &> /dev/null; then
        total_mem=$(free -m | awk '/^Mem:/{print $2}')
        log_info "Available memory: ${total_mem}MB"
        if [[ $total_mem -lt 4096 ]]; then
            log_warning "Low memory available. Consider reducing fork count or heap size"
        fi
    fi

    # Validate benchmark jar
    local benchmark_jar="$SCRIPT_DIR/target/wasmtime4j-benchmarks.jar"
    if [[ ! -f "$benchmark_jar" ]]; then
        log_error "Benchmark jar not found at $benchmark_jar"
        log_info "Please run 'mvn package' to build benchmarks"
        exit 1
    fi
}

# Build benchmark filter pattern
build_benchmark_filter() {
    local filter=".*"
    
    if [[ ${#BENCHMARK_PATTERNS[@]} -gt 0 ]]; then
        filter=""
        for pattern in "${BENCHMARK_PATTERNS[@]}"; do
            if [[ -n "$filter" ]]; then
                filter="$filter|"
            fi
            filter="$filter.*$pattern.*"
        done
    elif [[ -n "$CATEGORY_FILTER" ]]; then
        filter="$CATEGORY_FILTER"
    fi

    if [[ -n "$RUNTIME_FILTER" ]]; then
        filter="$filter"
    fi

    echo "$filter"
}

# Execute JMH benchmarks
run_benchmarks() {
    local benchmark_filter
    benchmark_filter=$(build_benchmark_filter)
    
    log "Starting JMH benchmark execution..."
    log_info "Configuration:"
    log_info "  Iterations: $ITERATIONS"
    log_info "  Warmup iterations: $WARMUP_ITERATIONS" 
    log_info "  Forks: $FORKS"
    log_info "  Threads: $THREADS"
    log_info "  Time per iteration: ${TIME}s"
    log_info "  Timeout: ${TIMEOUT}s"
    log_info "  Filter: $benchmark_filter"
    log_info "  Output: $RESULTS_FILE"

    # Build JMH command
    local jmh_cmd=(
        java -jar "$SCRIPT_DIR/target/wasmtime4j-benchmarks.jar"
        -rf json
        -rff "$RESULTS_FILE"
        -i "$ITERATIONS"
        -wi "$WARMUP_ITERATIONS" 
        -f "$FORKS"
        -t "$THREADS"
        -to "${TIMEOUT}s"
        -bm thrpt
        -tu s
    )

    # Add filter if specified
    if [[ "$benchmark_filter" != ".*" ]]; then
        jmh_cmd+=("$benchmark_filter")
    fi

    # Add extra JMH arguments from environment
    if [[ -n "$JMH_EXTRA_ARGS" ]]; then
        # shellcheck disable=SC2086
        jmh_cmd+=($JMH_EXTRA_ARGS)
    fi

    # Add benchmark options from environment
    if [[ -n "$WASMTIME4J_BENCHMARK_OPTS" ]]; then
        # shellcheck disable=SC2086  
        jmh_cmd+=($WASMTIME4J_BENCHMARK_OPTS)
    fi

    # Execute benchmarks
    local start_time
    start_time=$(date +%s)
    
    if [[ "$VERBOSE" == true ]]; then
        log_info "Executing: ${jmh_cmd[*]}"
    fi

    if "${jmh_cmd[@]}" 2>&1 | tee -a "$LOG_FILE"; then
        local end_time
        end_time=$(date +%s)
        local duration=$((end_time - start_time))
        log_success "Benchmarks completed in ${duration} seconds"
    else
        log_error "Benchmark execution failed"
        return 1
    fi
}

# Analyze results
analyze_results() {
    if [[ ! -f "$RESULTS_FILE" ]]; then
        log_error "Results file not found: $RESULTS_FILE"
        return 1
    fi

    log "Analyzing benchmark results..."

    # Count results
    local result_count
    if command -v jq &> /dev/null; then
        result_count=$(jq length "$RESULTS_FILE")
        log_info "Processed $result_count benchmark results"
    fi

    # Generate analysis reports
    if [[ "$GENERATE_REPORTS" == true ]]; then
        log "Generating performance reports..."
        
        if command -v java &> /dev/null; then
            java -cp "$SCRIPT_DIR/target/wasmtime4j-benchmarks.jar" \
                ai.tegmentum.wasmtime4j.benchmarks.BenchmarkResultAnalyzer \
                "$RESULTS_FILE" "$OUTPUT_DIR" || log_warning "Report generation failed"
        fi
    fi

    # Regression detection
    if [[ -n "$PERFORMANCE_BASELINE" && -f "$PERFORMANCE_BASELINE" ]]; then
        log "Performing regression analysis..."
        detect_regressions "$PERFORMANCE_BASELINE" "$RESULTS_FILE"
    fi

    # Runtime comparison
    if [[ "$COMPARISON_MODE" == true ]]; then
        log "Generating runtime comparison..."
        generate_runtime_comparison
    fi
}

# Detect performance regressions
detect_regressions() {
    local baseline_file="$1"
    local current_file="$2"
    
    log_info "Comparing against baseline: $baseline_file"
    
    # This would use the PerformanceRegressionDetector class
    # For now, implement basic comparison using jq if available
    if command -v jq &> /dev/null; then
        local regressions_found=false
        
        # Basic regression detection (simplified)
        while IFS= read -r benchmark; do
            local current_score baseline_score
            current_score=$(jq -r --arg bench "$benchmark" '.[] | select(.benchmark == $bench) | .primaryMetric.score' "$current_file" | head -1)
            baseline_score=$(jq -r --arg bench "$benchmark" '.[] | select(.benchmark == $bench) | .primaryMetric.score' "$baseline_file" | head -1)
            
            if [[ -n "$current_score" && -n "$baseline_score" && "$current_score" != "null" && "$baseline_score" != "null" ]]; then
                local change_percent
                change_percent=$(echo "scale=2; (($baseline_score - $current_score) / $baseline_score) * 100" | bc -l 2>/dev/null || echo "0")
                
                if (( $(echo "$change_percent > $REGRESSION_THRESHOLD" | bc -l 2>/dev/null || echo 0) )); then
                    log_warning "Regression detected in $benchmark: ${change_percent}% performance decrease"
                    regressions_found=true
                fi
            fi
        done < <(jq -r '.[].benchmark' "$current_file" | sort -u)
        
        if [[ "$regressions_found" == true && "$FAIL_ON_REGRESSION" == true ]]; then
            log_error "Performance regressions detected. Failing due to --fail-on-regression flag"
            exit 1
        fi
    else
        log_warning "jq not available, skipping detailed regression analysis"
    fi
}

# Generate runtime comparison
generate_runtime_comparison() {
    log_info "Generating JNI vs Panama comparison..."
    
    if command -v jq &> /dev/null && [[ -f "$RESULTS_FILE" ]]; then
        local comparison_file="$OUTPUT_DIR/runtime-comparison-$TIMESTAMP.txt"
        
        {
            echo "=== JNI vs Panama Performance Comparison ==="
            echo "Generated: $(date)"
            echo ""
            
            # Get unique benchmark names
            while IFS= read -r benchmark; do
                local jni_score panama_score
                jni_score=$(jq -r --arg bench "$benchmark" '.[] | select(.benchmark == $bench and (.params.runtimeTypeName == "JNI")) | .primaryMetric.score' "$RESULTS_FILE" | head -1)
                panama_score=$(jq -r --arg bench "$benchmark" '.[] | select(.benchmark == $bench and (.params.runtimeTypeName == "PANAMA")) | .primaryMetric.score' "$RESULTS_FILE" | head -1)
                
                if [[ -n "$jni_score" && -n "$panama_score" && "$jni_score" != "null" && "$panama_score" != "null" ]]; then
                    local speedup
                    speedup=$(echo "scale=2; $panama_score / $jni_score" | bc -l 2>/dev/null || echo "N/A")
                    printf "%-50s JNI: %12.2f  Panama: %12.2f  Speedup: %6s\n" "$benchmark" "$jni_score" "$panama_score" "${speedup}x"
                fi
            done < <(jq -r '.[].benchmark' "$RESULTS_FILE" | sort -u)
            
        } | tee "$comparison_file"
        
        log_success "Runtime comparison saved to: $comparison_file"
    fi
}

# Cleanup and finalization
cleanup() {
    log "Cleaning up temporary files..."
    
    # Archive old results (keep last 10)
    if [[ -d "$OUTPUT_DIR" ]]; then
        find "$OUTPUT_DIR" -name "jmh-results-*.json" -type f | sort -r | tail -n +11 | xargs rm -f 2>/dev/null || true
        find "$OUTPUT_DIR" -name "benchmark-execution-*.log" -type f | sort -r | tail -n +11 | xargs rm -f 2>/dev/null || true
    fi
}

# Generate CI/CD summary
generate_ci_summary() {
    if [[ "$CI_MODE" == true && -f "$RESULTS_FILE" ]]; then
        local summary_file="$OUTPUT_DIR/ci-summary.json"
        
        if command -v jq &> /dev/null; then
            {
                echo "{"
                echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\","
                echo "  \"benchmark_count\": $(jq length "$RESULTS_FILE"),"
                echo "  \"configuration\": {"
                echo "    \"iterations\": $ITERATIONS,"
                echo "    \"warmup_iterations\": $WARMUP_ITERATIONS,"
                echo "    \"forks\": $FORKS,"
                echo "    \"profile\": \"$PROFILE\""
                echo "  },"
                echo "  \"results_file\": \"$(basename "$RESULTS_FILE")\","
                echo "  \"log_file\": \"$(basename "$LOG_FILE")\""
                echo "}"
            } > "$summary_file"
            
            log_info "CI summary generated: $summary_file"
        fi
    fi
}

# Main execution function
main() {
    # Create output directory
    mkdir -p "$OUTPUT_DIR"
    
    # Parse arguments
    parse_arguments "$@"
    
    # Setup logging
    exec 19>&2
    if [[ "$QUIET" == true ]]; then
        exec 2>/dev/null
    fi
    
    log_info "Wasmtime4j Performance Suite Starting"
    log_info "Profile: ${PROFILE:-default}"
    log_info "Output directory: $OUTPUT_DIR"
    
    # Validate system
    validate_system
    
    # Run benchmarks
    if ! run_benchmarks; then
        log_error "Benchmark execution failed"
        exit 1
    fi
    
    # Analyze results
    if ! analyze_results; then
        log_error "Result analysis failed"
        exit 1
    fi
    
    # Generate CI summary
    generate_ci_summary
    
    # Cleanup
    cleanup
    
    # Visualization (after reports generated)
    if [[ "$VISUALIZE" == true && -f "$RESULTS_FILE" ]]; then
        log_info "Opening results in browser..."
        if [[ -x "$SCRIPT_DIR/scripts/view-benchmarks.sh" ]]; then
            "$SCRIPT_DIR/scripts/view-benchmarks.sh" "$RESULTS_FILE" &
        else
            log_warning "Visualizer script not found: $SCRIPT_DIR/scripts/view-benchmarks.sh"
        fi
    fi

    # Final status
    if [[ "$CI_MODE" == true ]]; then
        echo "benchmark_results_file=$RESULTS_FILE"
        echo "benchmark_log_file=$LOG_FILE"
        echo "benchmark_output_dir=$OUTPUT_DIR"
    else
        log_success "Performance suite completed successfully!"
        log_info "Results available in: $OUTPUT_DIR"
        log_info "JSON results: $RESULTS_FILE"
        log_info "Execution log: $LOG_FILE"
        
        if [[ "$GENERATE_REPORTS" == true ]]; then
            log_info "HTML report: $OUTPUT_DIR/performance-report.html"
            log_info "CSV export: $OUTPUT_DIR/benchmark-results.csv"
        fi
    fi
    
    # Restore stderr
    exec 2>&19 19>&-
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi