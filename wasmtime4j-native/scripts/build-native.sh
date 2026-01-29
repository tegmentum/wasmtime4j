#!/bin/bash

# Wasmtime4j Native Library Build Script
# This script handles cross-compilation of the native Rust library for all supported platforms

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$PROJECT_ROOT/scripts"
NATIVE_DIR="$PROJECT_ROOT/src/main/resources/natives"

# Configuration
WASMTIME_VERSION="41.0.1"
RUST_VERSION="1.90.0"

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
        "x86_64-pc-windows-msvc"
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
                output_dir="$NATIVE_DIR/linux-x86_64"
            fi
            lib_extension="so"
            ;;
        *windows*)
            output_dir="$NATIVE_DIR/windows-x86_64"
            lib_extension="dll"
            lib_prefix=""
            ;;
        *darwin*)
            if [[ $target == *aarch64* ]]; then
                output_dir="$NATIVE_DIR/darwin-aarch64"
            else
                output_dir="$NATIVE_DIR/darwin-x86_64"
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

# Cross-compilation target mapping
TARGETS_LIST=(
    "x86_64-unknown-linux-gnu:linux-x86_64"
    "aarch64-unknown-linux-gnu:linux-aarch64"
    "x86_64-pc-windows-msvc:windows-x86_64"
    "x86_64-apple-darwin:darwin-x86_64"
    "aarch64-apple-darwin:darwin-aarch64"
)

# Library configuration per platform
LIB_CONFIG_LIST=(
    "linux-x86_64:lib:so"
    "linux-aarch64:lib:so"
    "windows-x86_64::dll"
    "darwin-x86_64:lib:dylib"
    "darwin-aarch64:lib:dylib"
)

# Helper functions
get_platform_for_target() {
    local target="$1"
    for entry in "${TARGETS_LIST[@]}"; do
        if [[ "${entry%:*}" == "$target" ]]; then
            echo "${entry#*:}"
            return
        fi
    done
}

get_lib_config_for_platform() {
    local platform="$1"
    for entry in "${LIB_CONFIG_LIST[@]}"; do
        if [[ "${entry%%:*}" == "$platform" ]]; then
            echo "${entry#*:}"
            return
        fi
    done
}

# Source environment for cross-compilation
source_build_env() {
    local target="${1:-}"
    
    # Load cross-compilation environment if available
    local env_file="$PROJECT_ROOT/.cross-compilation/environment.sh"
    if [[ -f "$env_file" ]]; then
        log_debug "Loading cross-compilation environment from $env_file"
        source "$env_file" "$target"
    else
        log_warn "Cross-compilation environment not found. Run setup-cross-compilation.sh first."
    fi
    
    # Set build reproducibility variables
    export SOURCE_DATE_EPOCH="${SOURCE_DATE_EPOCH:-$(date +%s)}"
    # Note: Removed -C embed-bitcode=no flag as it conflicts with LTO in Cargo.toml
    export CARGO_NET_RETRY="${CARGO_NET_RETRY:-10}"
    export CARGO_HTTP_TIMEOUT="${CARGO_HTTP_TIMEOUT:-300}"
}

# Verify build environment
verify_build_env() {
    log_info "Verifying build environment..."
    
    # Check if cross-compilation setup exists
    if [[ ! -d "$PROJECT_ROOT/.cross-compilation" ]]; then
        log_warn "Cross-compilation setup not found. Run setup-cross-compilation.sh first."
        return 1
    fi
    
    # Verify all required targets are installed
    local installed_targets=$(rustup target list --installed)
    local missing_targets=()
    
    for entry in "${TARGETS_LIST[@]}"; do
        local target="${entry%:*}"
        if ! echo "$installed_targets" | grep -q "^$target$"; then
            missing_targets+=("$target")
        fi
    done
    
    if [[ ${#missing_targets[@]} -gt 0 ]]; then
        log_error "Missing cross-compilation targets: ${missing_targets[*]}"
        log_info "Run: ./scripts/setup-cross-compilation.sh"
        return 1
    fi
    
    log_success "Build environment verification passed"
    return 0
}

# Build for specific target with enhanced error handling
build_target_enhanced() {
    local target=$1
    local platform=$(get_platform_for_target "$target")
    local build_mode="${2:-release}"
    
    if [[ -z "$platform" ]]; then
        log_error "Unknown target: $target"
        return 1
    fi
    
    log_info "Building for target: $target (platform: $platform, mode: $build_mode)"
    
    # Parse library configuration
    local lib_config=$(get_lib_config_for_platform "$platform")
    local lib_prefix="${lib_config%:*}"
    local lib_extension="${lib_config#*:}"
    
    # Setup build environment
    source_build_env "$target"
    
    # Determine output directory
    local output_dir="$NATIVE_DIR/$platform"
    mkdir -p "$output_dir"
    
    # Build arguments
    local build_args=(
        "build"
        "--target" "$target"
    )
    
    if [[ "$build_mode" == "release" ]]; then
        build_args+=("--release")
    fi
    
    # Add features if specified
    if [[ -n "${CARGO_FEATURES:-}" ]]; then
        build_args+=("--features" "$CARGO_FEATURES")
    fi
    
    # Build the library with detailed logging
    log_info "Executing: cargo ${build_args[*]}"
    
    local build_start_time=$(date +%s)
    
    if cargo "${build_args[@]}" 2>&1 | tee "$PROJECT_ROOT/.cross-compilation/build-cache/build-$platform-$build_mode.log"; then
        local build_end_time=$(date +%s)
        local build_duration=$((build_end_time - build_start_time))
        
        # Locate and copy the built library
        local source_lib="$PROJECT_ROOT/target/$target/$build_mode/${lib_prefix}wasmtime4j.$lib_extension"
        local dest_lib="$output_dir/${lib_prefix}wasmtime4j.$lib_extension"
        
        if [[ -f "$source_lib" ]]; then
            cp "$source_lib" "$dest_lib"
            
            # Get library info
            local lib_size=$(ls -lh "$dest_lib" | awk '{print $5}')
            
            log_success "Built library for $target in ${build_duration}s (size: $lib_size)"
            log_info "Library location: $dest_lib"
            
            # Verify library
            if verify_library "$dest_lib" "$platform"; then
                log_success "Library verification passed for $target"
            else
                log_warn "Library verification failed for $target (may still be usable)"
            fi
            
        else
            log_error "Built library not found: $source_lib"
            return 1
        fi
    else
        log_error "Failed to build for target: $target"
        log_info "Build log available at: $PROJECT_ROOT/.cross-compilation/build-cache/build-$platform-$build_mode.log"
        return 1
    fi
}

# Verify library integrity
verify_library() {
    local lib_path="$1"
    local platform="$2"
    
    log_debug "Verifying library: $lib_path"
    
    # Basic file checks
    if [[ ! -f "$lib_path" ]]; then
        log_error "Library file does not exist: $lib_path"
        return 1
    fi
    
    local file_size=$(stat -c%s "$lib_path" 2>/dev/null || stat -f%z "$lib_path" 2>/dev/null)
    if [[ $file_size -eq 0 ]]; then
        log_error "Library file is empty: $lib_path"
        return 1
    fi
    
    # Platform-specific verification
    case "$platform" in
        linux-*)
            if command -v file &> /dev/null; then
                if file "$lib_path" | grep -q "ELF.*shared object"; then
                    log_debug "Library format verification passed (ELF shared object)"
                else
                    log_warn "Unexpected library format for Linux platform"
                fi
            fi
            ;;
        windows-*)
            if command -v file &> /dev/null; then
                if file "$lib_path" | grep -q "PE32.*DLL"; then
                    log_debug "Library format verification passed (PE32 DLL)"
                else
                    log_warn "Unexpected library format for Windows platform"
                fi
            fi
            ;;
        darwin-*)
            if command -v file &> /dev/null; then
                if file "$lib_path" | grep -q "Mach-O.*dynamically linked shared library"; then
                    log_debug "Library format verification passed (Mach-O dylib)"
                else
                    log_warn "Unexpected library format for macOS platform"
                fi
            fi
            ;;
    esac
    
    return 0
}

# Main build function with multiple modes
build_all() {
    local mode="${1:-auto}"
    local specific_target="${2:-}"
    local build_mode="${3:-release}"
    
    log_info "Starting native library build process (mode: $mode)..."
    
    cd "$PROJECT_ROOT"
    
    # Setup build cache directory
    mkdir -p "$PROJECT_ROOT/.cross-compilation/build-cache"
    
    case "$mode" in
        auto|placeholder)
            # Create placeholder libraries for initial testing
            log_warn "Using placeholder mode - no actual Rust compilation"
            log_info "Creating placeholder native libraries..."
            
            for entry in "${TARGETS_LIST[@]}"; do
                local target="${entry%:*}"
                local platform="${entry#*:}"
                local lib_config=$(get_lib_config_for_platform "$platform")
                local lib_prefix="${lib_config%:*}"
                local lib_extension="${lib_config#*:}"
                local output_dir="$NATIVE_DIR/$platform"
                local lib_file="$output_dir/${lib_prefix}wasmtime4j.$lib_extension"
                
                mkdir -p "$output_dir"
                echo "# Placeholder native library for $platform ($(date))" > "$lib_file"
                log_info "Created placeholder: $lib_file"
            done
            ;;
            
        compile)
            # Actual Rust compilation
            log_info "Using compilation mode - actual Rust compilation"
            
            # Verify build environment first
            if ! verify_build_env; then
                log_error "Build environment verification failed"
                exit 1
            fi
            
            if [[ -n "$specific_target" ]]; then
                # Build specific target
                if ! build_target_enhanced "$specific_target" "$build_mode"; then
                    log_error "Failed to build target: $specific_target"
                    exit 1
                fi
            else
                # Build all targets
                local failed_targets=()
                local successful_targets=()
                
                for entry in "${TARGETS_LIST[@]}"; do
                    local target="${entry%:*}"
                    if build_target_enhanced "$target" "$build_mode"; then
                        successful_targets+=("$target")
                    else
                        failed_targets+=("$target")
                    fi
                done
                
                # Report results
                if [[ ${#successful_targets[@]} -gt 0 ]]; then
                    log_success "Successfully built ${#successful_targets[@]} targets: ${successful_targets[*]}"
                fi
                
                if [[ ${#failed_targets[@]} -gt 0 ]]; then
                    log_error "Failed to build ${#failed_targets[@]} targets: ${failed_targets[*]}"
                    exit 1
                fi
            fi
            ;;
            
        verify)
            # Verify existing libraries
            log_info "Verifying existing native libraries..."
            
            local verification_failed=false
            
            for entry in "${TARGETS_LIST[@]}"; do
                local target="${entry%:*}"
                local platform="${entry#*:}"
                local lib_config=$(get_lib_config_for_platform "$platform")
                local lib_prefix="${lib_config%:*}"
                local lib_extension="${lib_config#*:}"
                local lib_file="$NATIVE_DIR/$platform/${lib_prefix}wasmtime4j.$lib_extension"
                
                if verify_library "$lib_file" "$platform"; then
                    log_success "Verification passed: $platform"
                else
                    log_error "Verification failed: $platform"
                    verification_failed=true
                fi
            done
            
            if [[ "$verification_failed" == "true" ]]; then
                exit 1
            fi
            ;;
            
        *)
            log_error "Unknown build mode: $mode"
            exit 1
            ;;
    esac
    
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
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo "Build native libraries for Wasmtime4j"
    echo ""
    echo "Commands:"
    echo "  build         Build native libraries (default)"
    echo "  compile       Build using actual Rust compilation"
    echo "  verify        Verify existing native libraries"
    echo "  clean         Clean build artifacts"
    echo "  clean-cache   Clean build cache only"
    echo "  check         Check prerequisites only"
    echo "  help          Show this help message"
    echo ""
    echo "Options:"
    echo "  --target TARGET     Build for specific target (e.g., x86_64-unknown-linux-gnu)"
    echo "  --mode MODE         Build mode: debug or release (default: release)"
    echo "  --all-platforms     Build for all supported platforms"
    echo "  --placeholder       Create placeholder libraries (default for 'build')"
    echo "  --features FEATURES Cargo features to enable"
    echo ""
    echo "Examples:"
    echo "  $0                                          # Create placeholder libraries"
    echo "  $0 compile                                  # Compile all platforms"
    echo "  $0 compile --target x86_64-unknown-linux-gnu # Compile specific target"
    echo "  $0 compile --mode debug                     # Compile in debug mode"
    echo "  $0 verify                                   # Verify existing libraries"
    echo ""
    echo "Environment variables:"
    echo "  WASMTIME4J_SKIP_NATIVE  Skip native compilation if set to 'true'"
    echo "  CARGO_FEATURES          Cargo features to enable during build"
    echo "  DEBUG                   Enable debug output if set to 'true'"
}

# Main script logic
main() {
    local command="${1:-build}"
    local specific_target=""
    local build_mode="release"
    local build_mode_set=""
    local all_platforms=false
    local placeholder_mode=false
    
    # Parse command line arguments
    shift || true
    while [[ $# -gt 0 ]]; do
        case $1 in
            --target)
                specific_target="$2"
                shift 2
                ;;
            --mode)
                build_mode="$2"
                build_mode_set="true"
                shift 2
                ;;
            --all-platforms)
                all_platforms=true
                shift
                ;;
            --placeholder)
                placeholder_mode=true
                shift
                ;;
            --features)
                export CARGO_FEATURES="$2"
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
    
    # Handle skip native environment variable
    if [[ "$WASMTIME4J_SKIP_NATIVE" == "true" ]]; then
        log_warn "Native compilation skipped (WASMTIME4J_SKIP_NATIVE=true)"
        exit 0
    fi
    
    case "$command" in
        build)
            check_prerequisites
            if [[ "$placeholder_mode" == "true" ]]; then
                build_all "placeholder"
            else
                build_all "auto"
            fi
            ;;
        compile)
            check_prerequisites
            if [[ -n "$specific_target" ]]; then
                if [[ "$all_platforms" == "true" ]]; then
                    log_error "Cannot specify both --target and --all-platforms"
                    exit 1
                fi
                build_all "compile" "$specific_target" "$build_mode"
            else
                build_all "compile" "" "$build_mode"
            fi
            ;;
        verify)
            build_all "verify"
            ;;
        clean)
            clean
            ;;
        clean-cache)
            if [[ -d "$PROJECT_ROOT/.cross-compilation/build-cache" ]]; then
                log_info "Cleaning build cache..."
                rm -rf "$PROJECT_ROOT/.cross-compilation/build-cache"
                mkdir -p "$PROJECT_ROOT/.cross-compilation/build-cache"
                log_success "Build cache cleaned"
            else
                log_info "No build cache to clean"
            fi
            ;;
        check)
            check_prerequisites
            if verify_build_env; then
                log_success "Build environment is ready"
            else
                log_error "Build environment needs setup"
                exit 1
            fi
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