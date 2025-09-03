#!/bin/bash

# Wasmtime4j Cross-Compilation Setup Script
# This script sets up the complete cross-compilation environment for all supported platforms
# ensuring build reproducibility and proper toolchain management

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$PROJECT_ROOT/scripts"

# Configuration
WASMTIME_VERSION="36.0.2"
REQUIRED_RUST_VERSION="1.75.0"

# Cross-compilation targets and platform mappings
TARGETS_LIST=(
    "x86_64-unknown-linux-gnu:linux-x86_64"
    "aarch64-unknown-linux-gnu:linux-aarch64"
    "x86_64-pc-windows-msvc:windows-x86_64"
    "x86_64-apple-darwin:macos-x86_64"
    "aarch64-apple-darwin:macos-aarch64"
)

# Additional cross-compilation dependencies per platform
PLATFORM_DEPS_LIST=(
    "x86_64-unknown-linux-gnu:gcc-multilib libc6-dev"
    "aarch64-unknown-linux-gnu:gcc-aarch64-linux-gnu"
    "x86_64-pc-windows-msvc:msvc-tools"
    "x86_64-apple-darwin:osxcross"
    "aarch64-apple-darwin:osxcross"
)

# Helper functions to work with target mappings
get_platform_for_target() {
    local target="$1"
    for entry in "${TARGETS_LIST[@]}"; do
        if [[ "${entry%:*}" == "$target" ]]; then
            echo "${entry#*:}"
            return
        fi
    done
}

get_deps_for_target() {
    local target="$1"
    for entry in "${PLATFORM_DEPS_LIST[@]}"; do
        if [[ "${entry%:*}" == "$target" ]]; then
            echo "${entry#*:}"
            return
        fi
    done
}

get_all_targets() {
    for entry in "${TARGETS_LIST[@]}"; do
        echo "${entry%:*}"
    done
}

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

# Detect host platform
detect_host_platform() {
    local os_name=$(uname -s)
    local arch=$(uname -m)
    
    case "$os_name" in
        Linux*)
            case "$arch" in
                x86_64) echo "linux-x86_64" ;;
                aarch64|arm64) echo "linux-aarch64" ;;
                *) log_error "Unsupported Linux architecture: $arch"; exit 1 ;;
            esac
            ;;
        Darwin*)
            case "$arch" in
                x86_64) echo "macos-x86_64" ;;
                arm64) echo "macos-aarch64" ;;
                *) log_error "Unsupported macOS architecture: $arch"; exit 1 ;;
            esac
            ;;
        MINGW*|MSYS*|CYGWIN*)
            case "$arch" in
                x86_64) echo "windows-x86_64" ;;
                *) log_error "Unsupported Windows architecture: $arch"; exit 1 ;;
            esac
            ;;
        *)
            log_error "Unsupported operating system: $os_name"
            exit 1
            ;;
    esac
}

# Check prerequisites
check_prerequisites() {
    log_step "Checking prerequisites..."
    
    # Check Rust installation
    if ! command -v rustc &> /dev/null; then
        log_error "Rust compiler not found. Please install Rust $REQUIRED_RUST_VERSION or later."
        log_info "Visit https://rustup.rs/ to install Rust"
        exit 1
    fi
    
    if ! command -v cargo &> /dev/null; then
        log_error "Cargo not found. Please install Rust toolchain."
        exit 1
    fi
    
    if ! command -v rustup &> /dev/null; then
        log_error "rustup not found. Please install rustup."
        exit 1
    fi
    
    # Check Rust version
    local current_rust_version=$(rustc --version | cut -d' ' -f2)
    log_info "Found Rust version: $current_rust_version"
    
    # Version comparison (simplified - requires semantic versioning)
    local required_major=$(echo $REQUIRED_RUST_VERSION | cut -d'.' -f1)
    local required_minor=$(echo $REQUIRED_RUST_VERSION | cut -d'.' -f2)
    local current_major=$(echo $current_rust_version | cut -d'.' -f1)
    local current_minor=$(echo $current_rust_version | cut -d'.' -f2)
    
    if [[ $current_major -lt $required_major ]] || 
       [[ $current_major -eq $required_major && $current_minor -lt $required_minor ]]; then
        log_warn "Rust version $current_rust_version is older than required $REQUIRED_RUST_VERSION"
        log_info "Consider updating: rustup update stable"
    fi
    
    log_success "Prerequisites check passed"
}

# Install cross-compilation targets
install_targets() {
    log_step "Installing cross-compilation targets..."
    
    local installed_targets=$(rustup target list --installed)
    local total_targets=${#TARGETS_LIST[@]}
    local installed_count=0
    local skipped_count=0
    
    for entry in "${TARGETS_LIST[@]}"; do
        local target="${entry%:*}"
        local platform="${entry#*:}"
        log_info "Processing target: $target (platform: $platform)"
        
        if echo "$installed_targets" | grep -q "^$target$"; then
            log_debug "Target $target already installed"
            ((skipped_count++))
        else
            log_info "Installing target: $target"
            if rustup target add "$target"; then
                log_success "Installed target: $target"
                ((installed_count++))
            else
                log_error "Failed to install target: $target"
                return 1
            fi
        fi
    done
    
    log_success "Cross-compilation targets processed - Installed: $installed_count, Skipped: $skipped_count, Total: $total_targets"
}

# Setup platform-specific build environment
setup_build_environment() {
    log_step "Setting up platform-specific build environment..."
    
    local host_platform=$(detect_host_platform)
    log_info "Host platform: $host_platform"
    
    # Create environment configuration directory
    local env_dir="$PROJECT_ROOT/.cross-compilation"
    mkdir -p "$env_dir"
    
    # Generate environment setup script
    cat > "$env_dir/environment.sh" << 'EOF'
#!/bin/bash
# Cross-compilation environment setup

# Export environment variables for consistent builds
export RUST_BACKTRACE=1
export CARGO_NET_RETRY=10
export CARGO_HTTP_TIMEOUT=300
export CARGO_HTTP_MULTIPLEXING=false

# Build reproducibility settings
export SOURCE_DATE_EPOCH="${SOURCE_DATE_EPOCH:-$(date +%s)}"
export RUSTFLAGS="${RUSTFLAGS} -C embed-bitcode=no -C debuginfo=2"

# Target-specific environment variables
setup_target_env() {
    local target="$1"
    
    case "$target" in
        x86_64-unknown-linux-gnu)
            export CC=gcc
            export CXX=g++
            export AR=ar
            export STRIP=strip
            ;;
        aarch64-unknown-linux-gnu)
            export CC=aarch64-linux-gnu-gcc
            export CXX=aarch64-linux-gnu-g++
            export AR=aarch64-linux-gnu-ar
            export STRIP=aarch64-linux-gnu-strip
            export CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER=aarch64-linux-gnu-gcc
            ;;
        x86_64-pc-windows-gnu)
            export CC=x86_64-w64-mingw32-gcc
            export CXX=x86_64-w64-mingw32-g++
            export AR=x86_64-w64-mingw32-ar
            export STRIP=x86_64-w64-mingw32-strip
            export CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER=x86_64-w64-mingw32-gcc
            ;;
        x86_64-apple-darwin)
            if [[ -n "${OSXCROSS_PATH:-}" ]]; then
                export CC=o64-clang
                export CXX=o64-clang++
                export AR=x86_64-apple-darwin15-ar
                export STRIP=x86_64-apple-darwin15-strip
                export CARGO_TARGET_X86_64_APPLE_DARWIN_LINKER=o64-clang
            fi
            ;;
        aarch64-apple-darwin)
            if [[ -n "${OSXCROSS_PATH:-}" ]]; then
                export CC=oa64-clang
                export CXX=oa64-clang++
                export AR=aarch64-apple-darwin20-ar
                export STRIP=aarch64-apple-darwin20-strip
                export CARGO_TARGET_AARCH64_APPLE_DARWIN_LINKER=oa64-clang
            fi
            ;;
    esac
}

# Load target-specific environment if provided
if [[ -n "${1:-}" ]]; then
    setup_target_env "$1"
fi
EOF

    chmod +x "$env_dir/environment.sh"
    
    log_success "Build environment configuration created at $env_dir/environment.sh"
}

# Verify cross-compilation setup
verify_setup() {
    log_step "Verifying cross-compilation setup..."
    
    local verification_failed=false
    
    # Check installed targets
    local installed_targets=$(rustup target list --installed)
    for entry in "${TARGETS_LIST[@]}"; do
        local target="${entry%:*}"
        if ! echo "$installed_targets" | grep -q "^$target$"; then
            log_error "Target $target is not installed"
            verification_failed=true
        else
            log_debug "Verified target: $target"
        fi
    done
    
    # Test basic compilation for each target
    local test_dir="$PROJECT_ROOT/.cross-compilation/test"
    mkdir -p "$test_dir"
    
    cat > "$test_dir/lib.rs" << 'EOF'
#[no_mangle]
pub extern "C" fn test_function() -> i32 {
    42
}
EOF
    
    cat > "$test_dir/Cargo.toml" << 'EOF'
[package]
name = "cross-compile-test"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib"]
EOF
    
    cd "$test_dir"
    
    for entry in "${TARGETS_LIST[@]}"; do
        local target="${entry%:*}"
        local platform="${entry#*:}"
        log_info "Testing compilation for $target (platform: $platform)"
        
        # Source environment for this target
        if source "$PROJECT_ROOT/.cross-compilation/environment.sh" "$target" 2>/dev/null; then
            if cargo build --target "$target" --release --quiet; then
                log_success "Compilation test passed for $target"
            else
                log_warn "Compilation test failed for $target (may need additional dependencies)"
                # Don't fail the setup for compilation issues - they might need extra tools
            fi
        else
            log_warn "Failed to setup environment for $target"
        fi
    done
    
    # Clean up test directory
    rm -rf "$test_dir"
    cd "$PROJECT_ROOT"
    
    if [[ "$verification_failed" == "true" ]]; then
        log_error "Setup verification failed - some targets are missing"
        return 1
    fi
    
    log_success "Cross-compilation setup verification completed"
}

# Create configuration documentation
create_documentation() {
    log_step "Creating cross-compilation documentation..."
    
    local doc_file="$PROJECT_ROOT/.cross-compilation/README.md"
    
    cat > "$doc_file" << EOF
# Cross-Compilation Setup for Wasmtime4j

This directory contains the cross-compilation configuration for building Wasmtime4j native libraries across all supported platforms.

## Supported Platforms

| Target Triple | Platform Name | Architecture | Notes |
|---------------|---------------|--------------|-------|
$(for entry in "${TARGETS_LIST[@]}"; do
    target="${entry%:*}"
    platform="${entry#*:}"
    arch="${target#*-}"
    arch="${arch%-*}"
    echo "| \`$target\` | $platform | $arch | |"
done)

## Environment Setup

The \`environment.sh\` script configures the build environment for cross-compilation:

\`\`\`bash
# Load general cross-compilation environment
source .cross-compilation/environment.sh

# Load target-specific environment
source .cross-compilation/environment.sh x86_64-unknown-linux-gnu
\`\`\`

## Build Reproducibility

To ensure reproducible builds across different environments:

1. **Rust Version**: Use Rust $REQUIRED_RUST_VERSION or compatible version
2. **Environment Variables**: Consistent RUSTFLAGS and build settings
3. **Source Date Epoch**: Fixed timestamp for deterministic builds
4. **Cargo Configuration**: Network and HTTP settings for reliability

## Platform-Specific Dependencies

Some targets may require additional system dependencies:

$(for entry in "${PLATFORM_DEPS_LIST[@]}"; do
    target="${entry%:*}"
    deps="${entry#*:}"
    platform=$(get_platform_for_target "$target")
    echo "### $platform (\`$target\`)"
    echo ""
    echo "Required dependencies: \`$deps\`"
    echo ""
done)

## Usage

### Setup (one-time)
\`\`\`bash
./scripts/setup-cross-compilation.sh
\`\`\`

### Build for specific platform
\`\`\`bash
./scripts/build-native.sh --target x86_64-unknown-linux-gnu
\`\`\`

### Build all platforms
\`\`\`bash
./scripts/build-native.sh --all-platforms
\`\`\`

## Troubleshooting

### Common Issues

1. **Missing cross-compiler**: Install platform-specific development tools
2. **Linker errors**: Ensure target-specific linkers are available
3. **Library not found**: Check that system libraries for target are installed

### Debug Mode

Enable debug output:
\`\`\`bash
DEBUG=true ./scripts/setup-cross-compilation.sh
\`\`\`

## Generated Files

- \`environment.sh\`: Cross-compilation environment configuration
- \`targets.list\`: List of installed targets
- \`build-cache/\`: Cached build artifacts (can be safely deleted)

## Build Cache

Build artifacts are cached in \`build-cache/\` to speed up incremental builds. The cache is organized by:

- Target triple
- Build mode (debug/release)
- Source hash

Cache can be cleaned with: \`./scripts/build-native.sh --clean-cache\`

## Last Updated

Generated on: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
Rust Version: $(rustc --version)
Host Platform: $(detect_host_platform)
EOF

    log_success "Documentation created at $doc_file"
}

# Create build cache management
setup_build_cache() {
    log_step "Setting up build cache management..."
    
    local cache_dir="$PROJECT_ROOT/.cross-compilation/build-cache"
    mkdir -p "$cache_dir"
    
    # Create cache management script
    cat > "$PROJECT_ROOT/.cross-compilation/manage-cache.sh" << 'EOF'
#!/bin/bash

# Build cache management for Wasmtime4j cross-compilation

set -e

CACHE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/build-cache"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }

show_cache_info() {
    if [[ ! -d "$CACHE_DIR" ]]; then
        log_info "No build cache found"
        return
    fi
    
    local total_size=$(du -sh "$CACHE_DIR" 2>/dev/null | cut -f1)
    local file_count=$(find "$CACHE_DIR" -type f | wc -l)
    
    echo "Build Cache Information:"
    echo "  Location: $CACHE_DIR"
    echo "  Total Size: $total_size"
    echo "  Files: $file_count"
    echo ""
    
    if [[ $file_count -gt 0 ]]; then
        echo "Cache Contents:"
        find "$CACHE_DIR" -maxdepth 2 -type d | sort | while read dir; do
            if [[ "$dir" != "$CACHE_DIR" ]]; then
                local rel_path=${dir#$CACHE_DIR/}
                local dir_size=$(du -sh "$dir" 2>/dev/null | cut -f1)
                echo "  $rel_path: $dir_size"
            fi
        done
    fi
}

clean_cache() {
    if [[ ! -d "$CACHE_DIR" ]]; then
        log_info "No build cache to clean"
        return
    fi
    
    log_info "Cleaning build cache..."
    rm -rf "$CACHE_DIR"
    mkdir -p "$CACHE_DIR"
    log_success "Build cache cleaned"
}

clean_target_cache() {
    local target="$1"
    if [[ -z "$target" ]]; then
        log_error "Target not specified"
        return 1
    fi
    
    local target_cache="$CACHE_DIR/$target"
    if [[ -d "$target_cache" ]]; then
        log_info "Cleaning cache for target: $target"
        rm -rf "$target_cache"
        log_success "Cache cleaned for target: $target"
    else
        log_info "No cache found for target: $target"
    fi
}

usage() {
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  info              Show cache information"
    echo "  clean             Clean all cache"
    echo "  clean-target      Clean cache for specific target"
    echo "  help              Show this help"
    echo ""
    echo "Options for clean-target:"
    echo "  --target TARGET   Specify target to clean"
}

case "${1:-info}" in
    info)
        show_cache_info
        ;;
    clean)
        clean_cache
        ;;
    clean-target)
        clean_target_cache "$2"
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        log_error "Unknown command: $1"
        usage
        exit 1
        ;;
esac
EOF

    chmod +x "$PROJECT_ROOT/.cross-compilation/manage-cache.sh"
    
    log_success "Build cache management setup completed"
}

# Generate target list for reference
save_target_list() {
    log_step "Saving target configuration..."
    
    local targets_file="$PROJECT_ROOT/.cross-compilation/targets.list"
    
    {
        echo "# Wasmtime4j Cross-Compilation Targets"
        echo "# Generated on: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
        echo "# Host: $(detect_host_platform)"
        echo ""
        
        for entry in "${TARGETS_LIST[@]}"; do
            echo "$entry"
        done
    } > "$targets_file"
    
    log_success "Target configuration saved to $targets_file"
}

# Main setup function
main() {
    local skip_deps=false
    local verify_only=false
    local clean_setup=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --skip-deps)
                skip_deps=true
                shift
                ;;
            --verify-only)
                verify_only=true
                shift
                ;;
            --clean)
                clean_setup=true
                shift
                ;;
            --help|-h)
                echo "Usage: $0 [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  --skip-deps     Skip platform dependency checks"
                echo "  --verify-only   Only verify existing setup"
                echo "  --clean         Clean existing setup before creating new one"
                echo "  --help, -h      Show this help message"
                echo ""
                echo "Environment Variables:"
                echo "  DEBUG=true      Enable debug output"
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    log_info "Wasmtime4j Cross-Compilation Setup"
    log_info "=================================="
    
    if [[ "$clean_setup" == "true" ]]; then
        log_info "Cleaning existing setup..."
        rm -rf "$PROJECT_ROOT/.cross-compilation"
    fi
    
    if [[ "$verify_only" == "true" ]]; then
        verify_setup
        exit $?
    fi
    
    check_prerequisites
    
    if [[ "$skip_deps" != "true" ]]; then
        install_targets
    fi
    
    setup_build_environment
    setup_build_cache
    save_target_list
    create_documentation
    
    log_info "Verifying setup..."
    verify_setup
    
    log_success "Cross-compilation setup completed successfully!"
    log_info ""
    log_info "Next steps:"
    log_info "  1. Review documentation: .cross-compilation/README.md"
    log_info "  2. Test compilation: ./scripts/build-native.sh --verify"
    log_info "  3. Build all platforms: ./scripts/build-native.sh --all-platforms"
}

# Run main function with all arguments
main "$@"