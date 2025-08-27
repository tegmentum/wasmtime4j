#!/bin/bash

# Wasmtime4j Benchmarks Execution Script
# Usage: ./run-benchmarks.sh [category] [profile] [additional-args...]

set -e

# Default values
DEFAULT_CATEGORY="all"
DEFAULT_PROFILE="standard"
OUTPUT_DIR="benchmark-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Wasmtime4j Benchmarks Runner"
    echo ""
    echo "Usage: $0 [category] [profile] [additional-args...]"
    echo ""
    echo "Categories:"
    echo "  all         - Run all benchmarks (default)"
    echo "  runtime     - Runtime initialization benchmarks"
    echo "  module      - Module operation benchmarks"
    echo "  function    - Function execution benchmarks"
    echo "  memory      - Memory operation benchmarks"
    echo "  comparison  - JNI vs Panama comparison benchmarks"
    echo ""
    echo "Profiles:"
    echo "  quick       - Fast benchmarks for development (1 iteration, 1 warmup, 1 fork)"
    echo "  standard    - Standard benchmarks (5 iterations, 3 warmup, 2 forks) [default]"
    echo "  production  - Production benchmarks (10 iterations, 5 warmup, 3 forks)"
    echo "  comprehensive - Comprehensive benchmarks (15 iterations, 8 warmup, 5 forks)"
    echo ""
    echo "Additional arguments will be passed directly to the benchmark runner."
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all benchmarks with standard profile"
    echo "  $0 runtime quick                     # Run runtime benchmarks with quick profile"
    echo "  $0 comparison production --output results.json"
    echo "  $0 all comprehensive --iterations 20"
}

# Function to check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    # Check if Java is installed
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F'.' '{print $1}')
    if [ "$JAVA_VERSION" -lt 8 ]; then
        print_error "Java 8 or higher is required"
        exit 1
    fi
    
    print_info "Java version: $(java -version 2>&1 | head -n 1)"
    
    # Check if Maven is available (for building if needed)
    if command -v mvn &> /dev/null; then
        print_info "Maven is available for building"
    elif command -v ../mvnw &> /dev/null; then
        print_info "Maven wrapper is available for building"
    else
        print_warning "Neither Maven nor Maven wrapper found - assuming JAR is already built"
    fi
}

# Function to build the benchmarks if needed
build_benchmarks() {
    local benchmark_jar="target/wasmtime4j-benchmarks.jar"
    
    if [ ! -f "$benchmark_jar" ]; then
        print_info "Benchmark JAR not found, attempting to build..."
        
        if [ -x "../mvnw" ]; then
            print_info "Building with Maven wrapper..."
            cd .. && ./mvnw clean package -pl wasmtime4j-benchmarks -am -DskipTests && cd wasmtime4j-benchmarks
        elif command -v mvn &> /dev/null; then
            print_info "Building with Maven..."
            cd .. && mvn clean package -pl wasmtime4j-benchmarks -am -DskipTests && cd wasmtime4j-benchmarks
        else
            print_error "Cannot build benchmarks - no Maven found and JAR doesn't exist"
            exit 1
        fi
        
        if [ ! -f "$benchmark_jar" ]; then
            print_error "Build failed - benchmark JAR not created"
            exit 1
        fi
        
        print_success "Benchmarks built successfully"
    else
        print_info "Using existing benchmark JAR: $benchmark_jar"
    fi
}

# Function to prepare output directory
prepare_output_dir() {
    if [ ! -d "$OUTPUT_DIR" ]; then
        mkdir -p "$OUTPUT_DIR"
        print_info "Created output directory: $OUTPUT_DIR"
    fi
}

# Function to detect available runtimes
detect_runtimes() {
    print_info "Detecting available runtimes..."
    
    local java_version
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F'.' '{if($1 == 1) print $2; else print $1}')
    
    echo "Java Version: $java_version"
    
    if [ "$java_version" -ge 23 ]; then
        print_info "Java 23+ detected - Panama Foreign Function API available"
        echo "Available runtimes: JNI, Panama"
    else
        print_info "Java $java_version detected - Only JNI runtime available"
        echo "Available runtimes: JNI"
    fi
}

# Function to run benchmarks
run_benchmarks() {
    local category="${1:-$DEFAULT_CATEGORY}"
    local profile="${2:-$DEFAULT_PROFILE}"
    shift 2 || true # Remove first two arguments
    local additional_args="$*"
    
    local benchmark_jar="target/wasmtime4j-benchmarks.jar"
    local output_file="$OUTPUT_DIR/benchmark_${category}_${profile}_${TIMESTAMP}.json"
    local log_file="$OUTPUT_DIR/benchmark_${category}_${profile}_${TIMESTAMP}.log"
    
    print_info "Starting benchmarks..."
    print_info "Category: $category"
    print_info "Profile: $profile"
    print_info "Output file: $output_file"
    print_info "Log file: $log_file"
    
    # Prepare the command
    local java_cmd="java -cp $benchmark_jar ai.tegmentum.wasmtime4j.benchmarks.BenchmarkRunner"
    local benchmark_args="$category --profile $profile --output $output_file $additional_args"
    
    print_info "Executing: $java_cmd $benchmark_args"
    echo ""
    
    # Run the benchmarks
    if $java_cmd $benchmark_args 2>&1 | tee "$log_file"; then
        print_success "Benchmarks completed successfully!"
        print_info "Results saved to: $output_file"
        print_info "Log saved to: $log_file"
        
        # Generate a simple summary
        if [ -f "${output_file%.*}_summary.txt" ]; then
            echo ""
            print_info "Summary:"
            cat "${output_file%.*}_summary.txt" | tail -10
        fi
    else
        print_error "Benchmarks failed! Check $log_file for details."
        exit 1
    fi
}

# Function to generate system info
generate_system_info() {
    local info_file="$OUTPUT_DIR/system_info_${TIMESTAMP}.txt"
    
    print_info "Generating system information..."
    
    {
        echo "System Information - Generated $(date)"
        echo "================================================"
        echo ""
        echo "OS Information:"
        uname -a
        echo ""
        echo "Java Information:"
        java -version
        echo ""
        echo "Memory Information:"
        if command -v free &> /dev/null; then
            free -h
        elif [ "$(uname)" = "Darwin" ]; then
            top -l 1 -s 0 | grep PhysMem
        fi
        echo ""
        echo "CPU Information:"
        if [ -f /proc/cpuinfo ]; then
            grep "model name" /proc/cpuinfo | head -1
            grep "cpu cores" /proc/cpuinfo | head -1
        elif [ "$(uname)" = "Darwin" ]; then
            sysctl -n machdep.cpu.brand_string
            sysctl -n hw.ncpu
        fi
        echo ""
        echo "Environment Variables:"
        env | grep -E "^(JAVA_|JVM_|PATH=)" | sort
        echo ""
    } > "$info_file"
    
    print_info "System information saved to: $info_file"
}

# Main execution
main() {
    echo "Wasmtime4j Benchmarks Runner"
    echo "================================"
    
    # Handle help argument
    if [[ "$1" == "--help" ]] || [[ "$1" == "-h" ]]; then
        show_usage
        exit 0
    fi
    
    check_prerequisites
    detect_runtimes
    prepare_output_dir
    generate_system_info
    build_benchmarks
    
    echo ""
    run_benchmarks "$@"
    
    echo ""
    print_success "All operations completed successfully!"
}

# Run main function with all arguments
main "$@"