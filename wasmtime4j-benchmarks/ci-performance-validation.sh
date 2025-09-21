#!/bin/bash

# CI/CD Performance Validation Script for Wasmtime4j
# Automated performance testing, baseline validation, and regression detection for CI/CD pipelines
# Usage: ./ci-performance-validation.sh [mode] [options]

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OUTPUT_DIR="$SCRIPT_DIR/ci-performance-reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# CI/CD specific configuration
CI_MODE=true
FAIL_ON_REGRESSION=true
PERFORMANCE_THRESHOLD=10.0  # 10% regression threshold
TIMEOUT_SECONDS=600         # 10 minute timeout
ITERATIONS=10               # Reduced iterations for CI speed
WARMUP_ITERATIONS=3
FORKS=2

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*"
}

log_info() {
    echo -e "${CYAN}[INFO]${NC} $*"
}

# Show usage information
show_usage() {
    cat << EOF
CI/CD Performance Validation Script for Wasmtime4j

USAGE:
    $0 [MODE] [OPTIONS]

MODES:
    validation      Validate performance against targets (default)
    baseline        Establish new performance baselines
    regression      Check for performance regressions only
    quick           Quick performance check (minimal benchmarks)
    full            Full validation with all checks

OPTIONS:
    --output DIR            Output directory for reports (default: $OUTPUT_DIR)
    --threshold PERCENT     Regression threshold percentage (default: $PERFORMANCE_THRESHOLD%)
    --timeout SECONDS       Benchmark timeout (default: $TIMEOUT_SECONDS)
    --iterations N          Benchmark iterations (default: $ITERATIONS)
    --warmup N              Warmup iterations (default: $WARMUP_ITERATIONS)
    --forks N               Benchmark forks (default: $FORKS)
    --fail-on-regression    Exit with error if regression detected (default: enabled)
    --no-fail-on-regression Don't exit with error on regression
    --verbose               Verbose output
    --quiet                 Minimal output
    --help                  Show this help message

ENVIRONMENT VARIABLES:
    JAVA_HOME               Java installation path
    CI                      CI environment indicator (auto-detected)
    GITHUB_ACTIONS         GitHub Actions environment (auto-detected)
    JENKINS_URL            Jenkins environment (auto-detected)

EXAMPLES:
    # Standard CI/CD validation
    $0 validation

    # Quick regression check
    $0 quick --threshold 5.0

    # Establish new baselines
    $0 baseline --iterations 50 --warmup 10

    # Full validation with custom output
    $0 full --output ./performance-results

    # GitHub Actions integration
    $0 validation --output \$GITHUB_WORKSPACE/performance-reports

    # Jenkins integration with strict thresholds
    $0 validation --threshold 3.0 --fail-on-regression

EOF
}

# Parse command line arguments
parse_arguments() {
    local mode="validation"
    local output_dir="$OUTPUT_DIR"
    local threshold="$PERFORMANCE_THRESHOLD"
    local timeout="$TIMEOUT_SECONDS"
    local iterations="$ITERATIONS"
    local warmup_iterations="$WARMUP_ITERATIONS"
    local forks="$FORKS"
    local fail_on_regression=true
    local verbose=false
    local quiet=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            validation|baseline|regression|quick|full)
                mode="$1"
                shift
                ;;
            --output)
                output_dir="$2"
                shift 2
                ;;
            --threshold)
                threshold="$2"
                shift 2
                ;;
            --timeout)
                timeout="$2"
                shift 2
                ;;
            --iterations)
                iterations="$2"
                shift 2
                ;;
            --warmup)
                warmup_iterations="$2"
                shift 2
                ;;
            --forks)
                forks="$2"
                shift 2
                ;;
            --fail-on-regression)
                fail_on_regression=true
                shift
                ;;
            --no-fail-on-regression)
                fail_on_regression=false
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
            --help|-h)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done

    # Store parsed values in global variables
    MODE=$mode
    OUTPUT_DIR=$output_dir
    PERFORMANCE_THRESHOLD=$threshold
    TIMEOUT_SECONDS=$timeout
    ITERATIONS=$iterations
    WARMUP_ITERATIONS=$warmup_iterations
    FORKS=$forks
    FAIL_ON_REGRESSION=$fail_on_regression
    VERBOSE=$verbose
    QUIET=$quiet
}

# Detect CI/CD environment
detect_ci_environment() {
    if [[ -n "$GITHUB_ACTIONS" ]]; then
        CI_ENVIRONMENT="github-actions"
        log_info "Detected GitHub Actions environment"
    elif [[ -n "$JENKINS_URL" ]]; then
        CI_ENVIRONMENT="jenkins"
        log_info "Detected Jenkins environment"
    elif [[ -n "$CI" ]]; then
        CI_ENVIRONMENT="generic-ci"
        log_info "Detected generic CI environment"
    else
        CI_ENVIRONMENT="local"
        log_info "Running in local environment"
    fi
}

# Validate system requirements
validate_system() {
    log "Validating system requirements for CI/CD..."

    # Check Java version
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install Java and set JAVA_HOME"
        exit 1
    fi

    local java_version
    java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    log_info "Java version: $java_version"

    # Check benchmark JAR
    local benchmark_jar="$SCRIPT_DIR/target/wasmtime4j-benchmarks.jar"
    if [[ ! -f "$benchmark_jar" ]]; then
        log_error "Benchmark JAR not found at $benchmark_jar"
        log_info "Building benchmark JAR..."

        if ! (cd "$PROJECT_ROOT" && ./mvnw package -pl wasmtime4j-benchmarks -q -DskipTests); then
            log_error "Failed to build benchmark JAR"
            exit 1
        fi
    fi

    # Check available memory for CI environments
    if [[ "$CI_ENVIRONMENT" != "local" ]]; then
        local total_mem
        if command -v free &> /dev/null; then
            total_mem=$(free -m | awk '/^Mem:/{print $2}')
            if [[ $total_mem -lt 2048 ]]; then
                log_warning "Low memory available: ${total_mem}MB. Reducing benchmark parameters..."
                ITERATIONS=$((ITERATIONS / 2))
                FORKS=1
            fi
        fi
    fi
}

# Execute performance benchmarks
execute_benchmarks() {
    log "Executing performance benchmarks for CI/CD validation..."

    mkdir -p "$OUTPUT_DIR"

    local results_file="$OUTPUT_DIR/ci-benchmark-results-$TIMESTAMP.json"

    log_info "Configuration:"
    log_info "  Mode: $MODE"
    log_info "  Iterations: $ITERATIONS"
    log_info "  Warmup iterations: $WARMUP_ITERATIONS"
    log_info "  Forks: $FORKS"
    log_info "  Timeout: ${TIMEOUT_SECONDS}s"
    log_info "  Output: $results_file"

    # Build benchmark command based on mode
    local benchmark_pattern
    case "$MODE" in
        validation|full)
            benchmark_pattern=".*ComparisonBenchmark.*|.*RuntimeInitializationBenchmark.*"
            ;;
        baseline)
            benchmark_pattern=".*"
            ;;
        regression|quick)
            benchmark_pattern=".*ComparisonBenchmark.*"
            ;;
    esac

    # Execute JMH benchmarks
    local jmh_cmd=(
        java -jar "$SCRIPT_DIR/target/wasmtime4j-benchmarks.jar"
        -rf json
        -rff "$results_file"
        -i "$ITERATIONS"
        -wi "$WARMUP_ITERATIONS"
        -f "$FORKS"
        -t 1
        -to "${TIMEOUT_SECONDS}s"
        -bm thrpt
        -tu s
        "$benchmark_pattern"
    )

    log_info "Executing: ${jmh_cmd[*]}"

    local start_time
    start_time=$(date +%s)

    if [[ "$VERBOSE" == true ]]; then
        "${jmh_cmd[@]}"
    else
        "${jmh_cmd[@]}" 2>/dev/null | grep -E "(# Benchmark:|# Run complete|ERROR|WARN)" || true
    fi

    local exit_code=$?
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))

    if [[ $exit_code -eq 0 ]]; then
        log_success "Benchmarks completed in ${duration} seconds"
        echo "$results_file"
    else
        log_error "Benchmark execution failed with exit code $exit_code"
        exit 1
    fi
}

# Validate performance results
validate_performance() {
    local results_file="$1"

    log "Validating performance results..."

    if [[ ! -f "$results_file" ]]; then
        log_error "Results file not found: $results_file"
        exit 1
    fi

    # Run performance target validation
    local validation_output
    if validation_output=$(java -cp "$SCRIPT_DIR/target/classes:$SCRIPT_DIR/target/dependency/*" \
        ai.tegmentum.wasmtime4j.benchmarks.PerformanceTargetValidator "$results_file" 2>&1); then

        log_success "Performance validation completed"

        if [[ "$VERBOSE" == true ]]; then
            echo "$validation_output"
        fi

        # Check if all targets were achieved
        if echo "$validation_output" | grep -q "All performance targets achieved!"; then
            log_success "🎉 All performance targets achieved!"
            return 0
        else
            log_warning "⚠️  Some performance targets were not achieved"
            if [[ "$FAIL_ON_REGRESSION" == true ]]; then
                return 1
            else
                return 0
            fi
        fi
    else
        log_error "Performance validation failed"
        echo "$validation_output"
        return 1
    fi
}

# Generate CI/CD reports
generate_ci_reports() {
    local results_file="$1"

    log "Generating CI/CD reports..."

    # Generate performance analysis report
    local analysis_file="$OUTPUT_DIR/performance-analysis-$TIMESTAMP.txt"
    if java -cp "$SCRIPT_DIR/target/classes:$SCRIPT_DIR/target/dependency/*" \
        ai.tegmentum.wasmtime4j.benchmarks.PerformanceTargetValidator "$results_file" > "$analysis_file" 2>&1; then
        log_info "Performance analysis saved to: $analysis_file"
    fi

    # Generate GitHub Actions output if in GitHub Actions
    if [[ "$CI_ENVIRONMENT" == "github-actions" ]]; then
        generate_github_actions_output "$results_file"
    fi

    # Generate Jenkins output if in Jenkins
    if [[ "$CI_ENVIRONMENT" == "jenkins" ]]; then
        generate_jenkins_output "$results_file"
    fi

    # Generate summary for any CI environment
    generate_ci_summary "$results_file"
}

# Generate GitHub Actions specific output
generate_github_actions_output() {
    local results_file="$1"

    log_info "Generating GitHub Actions output..."

    # Count benchmark results
    local result_count
    if command -v jq &> /dev/null && [[ -f "$results_file" ]]; then
        result_count=$(jq length "$results_file" 2>/dev/null || echo "0")
    else
        result_count="0"
    fi

    # Set GitHub Actions outputs
    if [[ -n "$GITHUB_OUTPUT" ]]; then
        {
            echo "performance-success=true"
            echo "performance-results-file=$results_file"
            echo "performance-result-count=$result_count"
            echo "performance-output-dir=$OUTPUT_DIR"
        } >> "$GITHUB_OUTPUT"
    fi

    # Create GitHub Actions summary
    if [[ -n "$GITHUB_STEP_SUMMARY" ]]; then
        {
            echo "## Performance Validation Results"
            echo ""
            echo "- **Mode**: $MODE"
            echo "- **Benchmark Results**: $result_count"
            echo "- **Results File**: \`$(basename "$results_file")\`"
            echo "- **Output Directory**: \`$OUTPUT_DIR\`"
            echo ""
            echo "Performance validation completed successfully ✅"
        } >> "$GITHUB_STEP_SUMMARY"
    fi
}

# Generate Jenkins specific output
generate_jenkins_output() {
    local results_file="$1"

    log_info "Generating Jenkins output..."

    # Generate Jenkins-compatible properties file
    local jenkins_props="$OUTPUT_DIR/jenkins-performance.properties"
    {
        echo "performance.mode=$MODE"
        echo "performance.results.file=$results_file"
        echo "performance.output.dir=$OUTPUT_DIR"
        echo "performance.timestamp=$TIMESTAMP"
        echo "performance.success=true"
    } > "$jenkins_props"

    log_info "Jenkins properties saved to: $jenkins_props"
}

# Generate general CI summary
generate_ci_summary() {
    local results_file="$1"

    local summary_file="$OUTPUT_DIR/ci-performance-summary.json"

    {
        echo "{"
        echo "  \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\","
        echo "  \"mode\": \"$MODE\","
        echo "  \"ci_environment\": \"$CI_ENVIRONMENT\","
        echo "  \"results_file\": \"$results_file\","
        echo "  \"output_directory\": \"$OUTPUT_DIR\","
        echo "  \"configuration\": {"
        echo "    \"iterations\": $ITERATIONS,"
        echo "    \"warmup_iterations\": $WARMUP_ITERATIONS,"
        echo "    \"forks\": $FORKS,"
        echo "    \"timeout_seconds\": $TIMEOUT_SECONDS,"
        echo "    \"threshold_percent\": $PERFORMANCE_THRESHOLD"
        echo "  },"
        echo "  \"success\": true"
        echo "}"
    } > "$summary_file"

    log_info "CI summary saved to: $summary_file"
}

# Cleanup temporary files
cleanup() {
    log "Cleaning up temporary files..."

    # Archive old reports (keep last 5)
    if [[ -d "$OUTPUT_DIR" ]]; then
        find "$OUTPUT_DIR" -name "ci-benchmark-results-*.json" -type f | sort -r | tail -n +6 | xargs rm -f 2>/dev/null || true
        find "$OUTPUT_DIR" -name "performance-analysis-*.txt" -type f | sort -r | tail -n +6 | xargs rm -f 2>/dev/null || true
    fi
}

# Main execution function
main() {
    # Parse arguments
    parse_arguments "$@"

    # Detect CI environment
    detect_ci_environment

    # Setup logging based on quiet mode
    if [[ "$QUIET" == true ]]; then
        exec 2>/dev/null
    fi

    log_info "Starting CI/CD Performance Validation"
    log_info "Mode: $MODE"
    log_info "CI Environment: $CI_ENVIRONMENT"
    log_info "Output Directory: $OUTPUT_DIR"

    # Validate system
    validate_system

    # Execute benchmarks
    local results_file
    results_file=$(execute_benchmarks)

    # Validate performance
    if ! validate_performance "$results_file"; then
        if [[ "$FAIL_ON_REGRESSION" == true ]]; then
            log_error "Performance validation failed - exiting with error"
            generate_ci_reports "$results_file"
            cleanup
            exit 1
        else
            log_warning "Performance validation had issues but continuing..."
        fi
    fi

    # Generate reports
    generate_ci_reports "$results_file"

    # Cleanup
    cleanup

    log_success "CI/CD Performance validation completed successfully!"
    log_info "Reports available in: $OUTPUT_DIR"
}

# Script execution
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi