#!/bin/bash

# Cross-Platform Validation Script for Wasmtime4j
# This script validates wasmtime4j implementations across different platforms
# and generates comprehensive cross-platform compliance reports.

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
VALIDATION_OUTPUT_DIR="${PROJECT_ROOT}/target/cross-platform-validation"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
WASMTIME_VERSION="${WASMTIME_VERSION:-26.0.0}"

# Platform detection
detect_platform() {
    local os=$(uname -s)
    local arch=$(uname -m)

    case "$os" in
        Linux*)
            case "$arch" in
                x86_64) echo "linux-x86_64" ;;
                aarch64) echo "linux-aarch64" ;;
                *) echo "linux-unknown" ;;
            esac
            ;;
        Darwin*)
            case "$arch" in
                x86_64) echo "macos-x86_64" ;;
                arm64) echo "macos-aarch64" ;;
                *) echo "macos-unknown" ;;
            esac
            ;;
        CYGWIN*|MINGW*|MSYS*)
            echo "windows-x86_64"
            ;;
        *)
            echo "unknown"
            ;;
    esac
}

# Logging functions
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warn() {
    echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $1" >&2
}

# Setup validation environment
setup_validation_environment() {
    log_info "Setting up cross-platform validation environment"

    # Create output directories
    mkdir -p "${VALIDATION_OUTPUT_DIR}"/{reports,logs,artifacts}

    # Detect current platform
    local current_platform=$(detect_platform)
    log_info "Detected platform: ${current_platform}"

    # Check Java installations
    check_java_installations

    # Install Wasmtime if needed
    install_wasmtime_reference

    # Verify Maven setup
    verify_maven_setup
}

# Check Java installations
check_java_installations() {
    log_info "Checking Java installations"

    local java_versions=("8" "11" "17" "21" "23")
    local found_versions=()

    for version in "${java_versions[@]}"; do
        if command -v "java-${version}" >/dev/null 2>&1; then
            found_versions+=("${version}")
            log_info "Found Java ${version}"
        elif [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
            local java_version=$(${JAVA_HOME}/bin/java -version 2>&1 | head -n 1 | cut -d'"' -f2)
            log_info "Found Java at JAVA_HOME: ${java_version}"
        fi
    done

    if [ ${#found_versions[@]} -eq 0 ]; then
        log_error "No Java installations found"
        return 1
    fi

    log_info "Available Java versions: ${found_versions[*]}"
}

# Install Wasmtime reference implementation
install_wasmtime_reference() {
    log_info "Installing Wasmtime reference implementation"

    local platform=$(detect_platform)
    local wasmtime_dir="${VALIDATION_OUTPUT_DIR}/wasmtime-reference"

    mkdir -p "${wasmtime_dir}"

    case "$platform" in
        linux-*)
            local wasmtime_url="https://github.com/bytecodealliance/wasmtime/releases/download/v${WASMTIME_VERSION}/wasmtime-v${WASMTIME_VERSION}-x86_64-linux.tar.xz"
            curl -L "${wasmtime_url}" -o "${wasmtime_dir}/wasmtime.tar.xz"
            tar -xf "${wasmtime_dir}/wasmtime.tar.xz" -C "${wasmtime_dir}" --strip-components=1
            ;;
        macos-*)
            local wasmtime_url="https://github.com/bytecodealliance/wasmtime/releases/download/v${WASMTIME_VERSION}/wasmtime-v${WASMTIME_VERSION}-x86_64-macos.tar.xz"
            curl -L "${wasmtime_url}" -o "${wasmtime_dir}/wasmtime.tar.xz"
            tar -xf "${wasmtime_dir}/wasmtime.tar.xz" -C "${wasmtime_dir}" --strip-components=1
            ;;
        windows-*)
            local wasmtime_url="https://github.com/bytecodealliance/wasmtime/releases/download/v${WASMTIME_VERSION}/wasmtime-v${WASMTIME_VERSION}-x86_64-windows.zip"
            curl -L "${wasmtime_url}" -o "${wasmtime_dir}/wasmtime.zip"
            unzip "${wasmtime_dir}/wasmtime.zip" -d "${wasmtime_dir}"
            mv "${wasmtime_dir}"/wasmtime-v${WASMTIME_VERSION}-x86_64-windows/* "${wasmtime_dir}/"
            ;;
        *)
            log_error "Unsupported platform for Wasmtime installation: ${platform}"
            return 1
            ;;
    esac

    export PATH="${wasmtime_dir}:${PATH}"

    if command -v wasmtime >/dev/null 2>&1; then
        log_info "Wasmtime installed successfully: $(wasmtime --version)"
    else
        log_error "Failed to install Wasmtime"
        return 1
    fi
}

# Verify Maven setup
verify_maven_setup() {
    log_info "Verifying Maven setup"

    if [ ! -f "${PROJECT_ROOT}/mvnw" ]; then
        log_error "Maven wrapper not found at ${PROJECT_ROOT}/mvnw"
        return 1
    fi

    cd "${PROJECT_ROOT}"
    if ! ./mvnw --version >/dev/null 2>&1; then
        log_error "Maven wrapper execution failed"
        return 1
    fi

    log_info "Maven setup verified successfully"
}

# Run cross-platform compliance tests
run_cross_platform_tests() {
    log_info "Running cross-platform compliance tests"

    local platform=$(detect_platform)
    local test_results_dir="${VALIDATION_OUTPUT_DIR}/test-results/${platform}"

    mkdir -p "${test_results_dir}"

    cd "${PROJECT_ROOT}"

    # Test different runtime configurations
    local runtimes=("jni")

    # Add Panama runtime for Java 23+
    if command -v java-23 >/dev/null 2>&1 || [[ "${JAVA_HOME:-}" == *"23"* ]]; then
        runtimes+=("panama")
    fi

    for runtime in "${runtimes[@]}"; do
        log_info "Testing ${runtime} runtime on ${platform}"

        local runtime_output_dir="${test_results_dir}/${runtime}"
        mkdir -p "${runtime_output_dir}"

        # Run comparison tests
        if run_runtime_tests "${runtime}" "${runtime_output_dir}"; then
            log_info "${runtime} runtime tests completed successfully"
        else
            log_warn "${runtime} runtime tests failed"
        fi
    done
}

# Run tests for specific runtime
run_runtime_tests() {
    local runtime="$1"
    local output_dir="$2"

    cd "${PROJECT_ROOT}/wasmtime4j-comparison-tests"

    # Configure Java version based on runtime
    if [ "${runtime}" = "panama" ]; then
        export JAVA_HOME="${JAVA_23_HOME:-${JAVA_HOME}}"
    else
        export JAVA_HOME="${JAVA_8_HOME:-${JAVA_HOME}}"
    fi

    # Run compliance tests
    local test_command="./mvnw test -B \
        -Dcomparison.suite=smoke \
        -Dcomparison.targets=native,${runtime} \
        -Dtest.runtime=${runtime} \
        -Dcomparison.reports.dir=${output_dir} \
        -Dcomparison.tests.timeout=600"

    log_info "Executing: ${test_command}"

    if eval "${test_command}" 2>&1 | tee "${output_dir}/test-execution.log"; then
        return 0
    else
        return 1
    fi
}

# Generate cross-platform validation report
generate_validation_report() {
    log_info "Generating cross-platform validation report"

    local report_file="${VALIDATION_OUTPUT_DIR}/reports/cross-platform-validation-${TIMESTAMP}.md"
    local platform=$(detect_platform)

    cat > "${report_file}" << EOF
# Cross-Platform Validation Report

**Generated:** $(date -u)
**Platform:** ${platform}
**Wasmtime Version:** ${WASMTIME_VERSION}
**Timestamp:** ${TIMESTAMP}

## Executive Summary

This report provides comprehensive validation results for wasmtime4j across different
platforms and runtime configurations.

## Platform Information

- **Operating System:** $(uname -s)
- **Architecture:** $(uname -m)
- **Kernel Version:** $(uname -r)
- **Platform Identifier:** ${platform}

## Java Environment

EOF

    # Add Java version information
    if command -v java >/dev/null 2>&1; then
        echo "### Current Java Installation" >> "${report_file}"
        java -version 2>&1 | sed 's/^/- /' >> "${report_file}"
        echo "" >> "${report_file}"
    fi

    # Add test results summary
    echo "## Test Results Summary" >> "${report_file}"
    echo "" >> "${report_file}"

    local test_results_dir="${VALIDATION_OUTPUT_DIR}/test-results/${platform}"
    if [ -d "${test_results_dir}" ]; then
        for runtime_dir in "${test_results_dir}"/*; do
            if [ -d "${runtime_dir}" ]; then
                local runtime=$(basename "${runtime_dir}")
                echo "### ${runtime} Runtime" >> "${report_file}"

                if [ -f "${runtime_dir}/test-execution.log" ]; then
                    local test_status
                    if grep -q "BUILD SUCCESS" "${runtime_dir}/test-execution.log"; then
                        test_status="✅ PASSED"
                    else
                        test_status="❌ FAILED"
                    fi
                    echo "- **Status:** ${test_status}" >> "${report_file}"

                    # Extract test statistics
                    local tests_run=$(grep -c "Running" "${runtime_dir}/test-execution.log" 2>/dev/null || echo "0")
                    echo "- **Tests Executed:** ${tests_run}" >> "${report_file}"
                else
                    echo "- **Status:** ⚠️ NO DATA" >> "${report_file}"
                fi
                echo "" >> "${report_file}"
            fi
        done
    else
        echo "No test results found." >> "${report_file}"
    fi

    # Add platform-specific notes
    echo "## Platform-Specific Notes" >> "${report_file}"
    echo "" >> "${report_file}"

    case "${platform}" in
        linux-*)
            echo "- Linux platform with standard glibc compatibility" >> "${report_file}"
            echo "- Native library loading through standard mechanisms" >> "${report_file}"
            ;;
        macos-*)
            echo "- macOS platform with system integrity protection" >> "${report_file}"
            echo "- Code signing may affect native library loading" >> "${report_file}"
            ;;
        windows-*)
            echo "- Windows platform with MSVC runtime dependencies" >> "${report_file}"
            echo "- Visual C++ Redistributable may be required" >> "${report_file}"
            ;;
    esac
    echo "" >> "${report_file}"

    # Add recommendations
    echo "## Recommendations" >> "${report_file}"
    echo "" >> "${report_file}"
    echo "- Verify all test failures are investigated" >> "${report_file}"
    echo "- Ensure consistent behavior across all supported platforms" >> "${report_file}"
    echo "- Monitor performance characteristics per platform" >> "${report_file}"
    echo "- Validate native library compatibility" >> "${report_file}"
    echo "" >> "${report_file}"

    echo "---" >> "${report_file}"
    echo "*Generated by wasmtime4j cross-platform validation system*" >> "${report_file}"

    log_info "Validation report generated: ${report_file}"
}

# Generate JSON summary for CI integration
generate_json_summary() {
    log_info "Generating JSON summary for CI integration"

    local json_file="${VALIDATION_OUTPUT_DIR}/reports/validation-summary-${TIMESTAMP}.json"
    local platform=$(detect_platform)

    cat > "${json_file}" << EOF
{
  "timestamp": "$(date -u -Iseconds)",
  "platform": "${platform}",
  "wasmtime_version": "${WASMTIME_VERSION}",
  "validation_id": "${TIMESTAMP}",
  "summary": {
    "total_platforms_tested": 1,
    "successful_validations": 0,
    "failed_validations": 0,
    "warnings": 0
  },
  "platform_results": {
    "${platform}": {
      "os": "$(uname -s)",
      "arch": "$(uname -m)",
      "kernel": "$(uname -r)",
      "runtimes_tested": [],
      "overall_status": "unknown"
    }
  },
  "runtime_results": {},
  "performance_metrics": {
    "execution_time_seconds": 0,
    "memory_usage_mb": 0
  },
  "artifacts": {
    "report_file": "cross-platform-validation-${TIMESTAMP}.md",
    "log_files": [],
    "test_results": []
  }
}
EOF

    log_info "JSON summary generated: ${json_file}"
}

# Cleanup temporary files
cleanup() {
    log_info "Cleaning up temporary files"

    # Remove temporary wasmtime installations
    rm -rf "${VALIDATION_OUTPUT_DIR}/wasmtime-reference"/*.tar.xz
    rm -rf "${VALIDATION_OUTPUT_DIR}/wasmtime-reference"/*.zip

    log_info "Cleanup completed"
}

# Main execution flow
main() {
    log_info "Starting cross-platform validation for wasmtime4j"
    log_info "Project root: ${PROJECT_ROOT}"
    log_info "Output directory: ${VALIDATION_OUTPUT_DIR}"

    # Trap cleanup on exit
    trap cleanup EXIT

    # Setup environment
    if ! setup_validation_environment; then
        log_error "Failed to setup validation environment"
        exit 1
    fi

    # Run tests
    if ! run_cross_platform_tests; then
        log_error "Cross-platform tests failed"
        exit 1
    fi

    # Generate reports
    generate_validation_report
    generate_json_summary

    log_info "Cross-platform validation completed successfully"
    log_info "Results available in: ${VALIDATION_OUTPUT_DIR}"
}

# Script entry point
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi