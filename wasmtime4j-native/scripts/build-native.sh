#!/bin/bash

# Wasmtime4j Native Library Build Script
# This script handles cross-compilation of the native Rust library for all supported platforms

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$PROJECT_ROOT/scripts"
NATIVE_DIR="$PROJECT_ROOT/src/main/resources/natives"

# Configuration
WASMTIME_VERSION="36.0.2"
RUST_VERSION="1.75.0"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v rustc &> /dev/null; then
        log_error "Rust compiler not found. Please install Rust $RUST_VERSION or later."
        exit 1
    fi
    
    if ! command -v cargo &> /dev/null; then
        log_error "Cargo not found. Please install Rust toolchain."
        exit 1
    fi
    
    # Check Rust version
    CURRENT_RUST_VERSION=$(rustc --version | cut -d' ' -f2)
    log_info "Found Rust version: $CURRENT_RUST_VERSION"
    
    log_success "Prerequisites check passed"
}

# Install cross-compilation targets
install_targets() {
    log_info "Installing cross-compilation targets..."
    
    local targets=(
        "x86_64-unknown-linux-gnu"
        "aarch64-unknown-linux-gnu"
        "x86_64-pc-windows-gnu"
        "x86_64-apple-darwin"
        "aarch64-apple-darwin"
    )
    
    for target in "${targets[@]}"; do
        log_info "Installing target: $target"
        rustup target add "$target" || log_warn "Failed to install target: $target"
    done
    
    log_success "Cross-compilation targets installed"
}

# Build for specific target
build_target() {
    local target=$1
    local output_dir=""
    local lib_extension=""
    local lib_prefix="lib"
    
    log_info "Building for target: $target"
    
    # Determine output directory and library extension based on target
    case $target in
        *linux*)
            if [[ $target == *aarch64* ]]; then
                output_dir="$NATIVE_DIR/linux-aarch64"
            else
                output_dir="$NATIVE_DIR/linux-x64"
            fi
            lib_extension="so"
            ;;
        *windows*)
            output_dir="$NATIVE_DIR/windows-x64"
            lib_extension="dll"
            lib_prefix=""
            ;;
        *darwin*)
            if [[ $target == *aarch64* ]]; then
                output_dir="$NATIVE_DIR/macos-aarch64"
            else
                output_dir="$NATIVE_DIR/macos-x64"
            fi
            lib_extension="dylib"
            ;;
        *)
            log_error "Unknown target: $target"
            return 1
            ;;
    esac
    
    # Create output directory
    mkdir -p "$output_dir"
    
    # Build the library
    log_info "Compiling native library for $target..."
    if cargo build --release --target "$target"; then
        local source_lib="$PROJECT_ROOT/target/$target/release/${lib_prefix}wasmtime4j.$lib_extension"
        local dest_lib="$output_dir/${lib_prefix}wasmtime4j.$lib_extension"
        
        if [[ -f "$source_lib" ]]; then
            cp "$source_lib" "$dest_lib"
            log_success "Built and copied library for $target to $dest_lib"
        else
            log_error "Built library not found: $source_lib"
            return 1
        fi
    else
        log_error "Failed to build for target: $target"
        return 1
    fi
}

# Main build function
build_all() {
    log_info "Starting native library build process..."
    
    cd "$PROJECT_ROOT"
    
    # For now, this is a placeholder that creates empty placeholder files
    # TODO: Replace with actual Rust compilation when Rust source is ready
    
    log_warn "Native compilation is currently a placeholder"
    log_info "Creating placeholder native libraries..."
    
    # Create placeholder libraries for testing
    local platforms=("linux-x64" "linux-aarch64" "windows-x64" "macos-x64" "macos-aarch64")
    local extensions=("so" "so" "dll" "dylib" "dylib")
    local prefixes=("lib" "lib" "" "lib" "lib")
    
    for i in "${!platforms[@]}"; do
        local platform="${platforms[$i]}"
        local extension="${extensions[$i]}"
        local prefix="${prefixes[$i]}"
        local output_dir="$NATIVE_DIR/$platform"
        local lib_file="$output_dir/${prefix}wasmtime4j.$extension"
        
        mkdir -p "$output_dir"
        echo "# Placeholder native library for $platform" > "$lib_file"
        log_info "Created placeholder: $lib_file"
    done
    
    log_success "Native library build process completed"
}

# Clean build artifacts
clean() {
    log_info "Cleaning build artifacts..."
    
    if [[ -d "$PROJECT_ROOT/target" ]]; then
        rm -rf "$PROJECT_ROOT/target"
        log_info "Removed Cargo target directory"
    fi
    
    # Clean placeholder libraries but preserve .gitkeep files
    for platform_dir in "$NATIVE_DIR"/*; do
        if [[ -d "$platform_dir" ]]; then
            find "$platform_dir" -name "*.so" -o -name "*.dll" -o -name "*.dylib" | xargs rm -f
            log_info "Cleaned native libraries from $(basename "$platform_dir")"
        fi
    done
    
    log_success "Build artifacts cleaned"
}

# Print usage
usage() {
    echo "Usage: $0 [OPTION]"
    echo "Build native libraries for Wasmtime4j"
    echo ""
    echo "Options:"
    echo "  build     Build native libraries for all platforms (default)"
    echo "  clean     Clean build artifacts"
    echo "  check     Check prerequisites only"
    echo "  help      Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  WASMTIME4J_SKIP_NATIVE  Skip native compilation if set to 'true'"
}

# Main script logic
main() {
    local command="${1:-build}"
    
    case "$command" in
        build)
            if [[ "$WASMTIME4J_SKIP_NATIVE" == "true" ]]; then
                log_warn "Native compilation skipped (WASMTIME4J_SKIP_NATIVE=true)"
                exit 0
            fi
            check_prerequisites
            build_all
            ;;
        clean)
            clean
            ;;
        check)
            check_prerequisites
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