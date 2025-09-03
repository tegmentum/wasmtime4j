#!/bin/bash

# Maven Cross-Compilation Build Script for Wasmtime4j
# This script orchestrates the complete Maven-based cross-compilation pipeline
# for CI/CD integration and local development

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
MAVEN_OPTS="${MAVEN_OPTS:--Xmx2g -XX:+UseParallelGC}"
DEFAULT_BUILD_MODE="release"
DEFAULT_PROFILES="native-dev"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_debug() {
    if [[ "${DEBUG:-}" == "true" ]]; then
        echo -e "${PURPLE}[DEBUG]${NC} $1"
    fi
}

log_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# Validate Maven installation
validate_maven() {
    log_step "Validating Maven installation..."
    
    if ! command -v mvn &> /dev/null; then
        log_error "Maven not found. Please install Maven 3.8+ or use the Maven wrapper (./mvnw)"
        exit 1
    fi
    
    local maven_version=$(mvn -version | head -1 | cut -d' ' -f3)
    log_info "Found Maven version: $maven_version"
    
    # Check for Maven wrapper as fallback
    if [[ -f "$PROJECT_ROOT/mvnw" ]]; then
        log_info "Maven wrapper available at $PROJECT_ROOT/mvnw"
    fi
    
    log_success "Maven validation passed"
}

# Setup cross-compilation environment
setup_cross_compilation() {
    log_step "Setting up cross-compilation environment..."
    
    cd "$PROJECT_ROOT/wasmtime4j-native"
    
    if [[ ! -d ".cross-compilation" ]]; then
        log_info "Running cross-compilation setup..."
        if ./scripts/setup-cross-compilation.sh; then
            log_success "Cross-compilation setup completed"
        else
            log_error "Cross-compilation setup failed"
            exit 1
        fi
    else
        log_info "Cross-compilation environment already exists"
        # Verify the setup
        if ./scripts/setup-cross-compilation.sh --verify-only; then
            log_success "Cross-compilation environment verified"
        else
            log_warn "Cross-compilation environment needs updates"
            ./scripts/setup-cross-compilation.sh --clean
        fi
    fi
    
    cd "$PROJECT_ROOT"
}

# Maven build with specific profile and targets
maven_build() {
    local mode="${1:-validate}"
    local profiles="${2:-$DEFAULT_PROFILES}"
    local additional_args="${3:-}"
    
    log_step "Running Maven build (mode: $mode, profiles: $profiles)"
    
    local maven_cmd="mvn"
    if [[ -f "$PROJECT_ROOT/mvnw" ]]; then
        maven_cmd="./mvnw"
    fi
    
    # Construct Maven command
    local mvn_args=()
    mvn_args+=("$mode")
    mvn_args+=("-P$profiles")
    
    if [[ -n "$additional_args" ]]; then
        IFS=' ' read -ra ADDR <<< "$additional_args"
        mvn_args+=("${ADDR[@]}")
    fi
    
    # Add common Maven options
    mvn_args+=("-Dmaven.test.skip=true")
    mvn_args+=("-Dspotless.check.skip=true")
    mvn_args+=("-Dcheckstyle.skip=true")
    mvn_args+=("-Dspotbugs.skip=true")
    
    log_info "Executing: $maven_cmd ${mvn_args[*]}"
    
    export MAVEN_OPTS="$MAVEN_OPTS"
    
    if "$maven_cmd" "${mvn_args[@]}"; then
        log_success "Maven build completed successfully"
    else
        log_error "Maven build failed"
        exit 1
    fi
}

# Build all platforms using Maven
build_all_platforms() {
    local build_mode="${1:-$DEFAULT_BUILD_MODE}"
    
    log_step "Building all platforms (mode: $build_mode)"
    
    # Setup environment first
    setup_cross_compilation
    
    # Use the all-platforms profile for comprehensive building
    maven_build "compile" "all-platforms" "-Dnative.build.all=true -Dcargo.build.mode=$build_mode"
    
    # Package the results
    maven_build "package" "all-platforms" "-Dnative.build.all=true"
    
    log_success "All platforms build completed"
}

# Build specific platform
build_platform() {
    local target="$1"
    local build_mode="${2:-$DEFAULT_BUILD_MODE}"
    
    log_step "Building for specific platform: $target (mode: $build_mode)"
    
    # Map target to Maven profile
    local profile=""
    case "$target" in
        "x86_64-unknown-linux-gnu"|"linux-x86_64")
            profile="linux-x86_64"
            ;;
        "aarch64-unknown-linux-gnu"|"linux-aarch64")
            profile="linux-aarch64"
            ;;
        "x86_64-pc-windows-msvc"|"windows-x86_64")
            profile="windows-x86_64"
            ;;
        "x86_64-apple-darwin"|"macos-x86_64")
            profile="macos-x86_64"
            ;;
        "aarch64-apple-darwin"|"macos-aarch64")
            profile="macos-aarch64"
            ;;
        *)
            log_error "Unknown target platform: $target"
            exit 1
            ;;
    esac
    
    # Build with specific profile
    maven_build "compile" "$profile" "-Dcargo.build.mode=$build_mode"
    maven_build "package" "$profile"
    
    log_success "Platform-specific build completed for $target"
}

# Parallel build using Maven threading
build_parallel() {
    local build_mode="${1:-$DEFAULT_BUILD_MODE}"
    local thread_count="${2:-4}"
    
    log_step "Building all platforms in parallel (mode: $build_mode, threads: $thread_count)"
    
    setup_cross_compilation
    
    # Use parallel-build profile with Maven threading
    maven_build "compile" "parallel-build" "-T$thread_count -Dcargo.build.mode=$build_mode"
    maven_build "package" "parallel-build" "-T$thread_count"
    
    log_success "Parallel build completed"
}

# Validate build environment
validate_environment() {
    log_step "Validating complete build environment..."
    
    validate_maven
    
    # Validate just the native module to focus on cross-compilation
    maven_build "validate" "native-dev" "-pl wasmtime4j-native"
    
    log_success "Build environment validation completed"
}

# Clean all build artifacts
clean_all() {
    log_step "Cleaning all build artifacts..."
    
    maven_build "clean" "" "-q"
    
    # Also clean native-specific artifacts
    if [[ -f "$PROJECT_ROOT/wasmtime4j-native/scripts/build-native.sh" ]]; then
        cd "$PROJECT_ROOT/wasmtime4j-native"
        ./scripts/build-native.sh clean
        cd "$PROJECT_ROOT"
    fi
    
    log_success "Clean completed"
}

# Generate build report
generate_build_report() {
    local report_file="${1:-build-report.md}"
    
    log_step "Generating build report..."
    
    {
        echo "# Wasmtime4j Cross-Compilation Build Report"
        echo ""
        echo "Generated on: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
        echo "Host: $(uname -s)-$(uname -m)"
        echo ""
        
        echo "## Build Environment"
        echo ""
        echo "- Maven: $(mvn -version | head -1)"
        echo "- Java: $(java -version 2>&1 | head -1)"
        echo "- Rust: $(rustc --version 2>/dev/null || echo 'Not available')"
        echo "- Cargo: $(cargo --version 2>/dev/null || echo 'Not available')"
        echo ""
        
        echo "## Cross-Compilation Targets"
        echo ""
        if command -v rustup &> /dev/null; then
            echo "Installed Rust targets:"
            rustup target list --installed | sed 's/^/- /'
        else
            echo "Rustup not available"
        fi
        echo ""
        
        echo "## Native Libraries"
        echo ""
        local native_dir="$PROJECT_ROOT/wasmtime4j-native/src/main/resources/natives"
        if [[ -d "$native_dir" ]]; then
            find "$native_dir" -name "*.so" -o -name "*.dll" -o -name "*.dylib" | while read lib; do
                local rel_path=${lib#$native_dir/}
                local size=$(ls -lh "$lib" | awk '{print $5}')
                echo "- $rel_path ($size)"
            done
        else
            echo "No native libraries found"
        fi
        echo ""
        
        echo "## Maven Modules"
        echo ""
        if [[ -f "$PROJECT_ROOT/pom.xml" ]]; then
            mvn help:evaluate -Dexpression=project.modules -q -DforceStdout | grep -v '^$' | sed 's/^/- /'
        fi
        
    } > "$report_file"
    
    log_success "Build report generated: $report_file"
}

# CI/CD specific build
ci_build() {
    local mode="${1:-release}"
    
    log_step "Running CI/CD build pipeline..."
    
    # Set CI-friendly options
    export MAVEN_OPTS="$MAVEN_OPTS -Dorg.slf4j.simpleLogger.showDateTime=true"
    
    # Validate environment
    validate_environment
    
    # Build all platforms
    build_all_platforms "$mode"
    
    # Generate report
    generate_build_report "ci-build-report.md"
    
    log_success "CI/CD build pipeline completed"
}

# Print usage information
usage() {
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo "Maven-based cross-compilation build script for Wasmtime4j"
    echo ""
    echo "Commands:"
    echo "  validate        Validate build environment (default)"
    echo "  build           Build all platforms"
    echo "  build-platform  Build specific platform"
    echo "  build-parallel  Build all platforms in parallel"
    echo "  ci              Run CI/CD build pipeline"
    echo "  clean           Clean all build artifacts"
    echo "  report          Generate build report"
    echo "  help            Show this help message"
    echo ""
    echo "Options:"
    echo "  --mode MODE           Build mode: debug or release (default: release)"
    echo "  --target TARGET       Target platform for platform-specific builds"
    echo "  --threads THREADS     Number of parallel threads (default: 4)"
    echo "  --profiles PROFILES   Maven profiles to activate (default: native-dev)"
    echo "  --maven-args ARGS     Additional Maven arguments"
    echo ""
    echo "Examples:"
    echo "  $0                                        # Validate environment"
    echo "  $0 build                                  # Build all platforms"
    echo "  $0 build --mode debug                     # Debug build"
    echo "  $0 build-platform --target linux-x86_64  # Build specific platform"
    echo "  $0 build-parallel --threads 8             # Parallel build with 8 threads"
    echo "  $0 ci --mode release                      # CI/CD pipeline"
    echo ""
    echo "Environment Variables:"
    echo "  DEBUG=true         Enable debug output"
    echo "  MAVEN_OPTS         Maven JVM options"
    echo "  RUST_BACKTRACE     Rust backtrace level"
}

# Main script logic
main() {
    local command="${1:-validate}"
    local mode="$DEFAULT_BUILD_MODE"
    local target=""
    local threads="4"
    local profiles="$DEFAULT_PROFILES"
    local maven_args=""
    
    # Parse command line arguments
    shift || true
    while [[ $# -gt 0 ]]; do
        case $1 in
            --mode)
                mode="$2"
                shift 2
                ;;
            --target)
                target="$2"
                shift 2
                ;;
            --threads)
                threads="$2"
                shift 2
                ;;
            --profiles)
                profiles="$2"
                shift 2
                ;;
            --maven-args)
                maven_args="$2"
                shift 2
                ;;
            --help|-h)
                usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
    
    cd "$PROJECT_ROOT"
    
    case "$command" in
        validate)
            validate_environment
            ;;
        build)
            build_all_platforms "$mode"
            ;;
        build-platform)
            if [[ -z "$target" ]]; then
                log_error "Target platform must be specified with --target"
                exit 1
            fi
            build_platform "$target" "$mode"
            ;;
        build-parallel)
            build_parallel "$mode" "$threads"
            ;;
        ci)
            ci_build "$mode"
            ;;
        clean)
            clean_all
            ;;
        report)
            generate_build_report
            ;;
        help|--help|-h)
            usage
            ;;
        *)
            log_error "Unknown command: $command"
            usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"