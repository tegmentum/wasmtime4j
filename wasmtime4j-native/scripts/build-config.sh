#!/bin/bash

# Wasmtime4j Build Configuration Script
# This script ensures build reproducibility across different development environments
# by configuring consistent build settings and checking environment compatibility

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$PROJECT_ROOT/scripts"
CONFIG_DIR="$PROJECT_ROOT/.cross-compilation"

# Configuration constants
WASMTIME_VERSION="36.0.2"
REQUIRED_RUST_VERSION="1.75.0"
BUILD_CONFIG_VERSION="1.0.0"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_debug() { if [[ "${DEBUG:-}" == "true" ]]; then echo -e "${PURPLE}[DEBUG]${NC} $1"; fi; }
log_step() { echo -e "${CYAN}[STEP]${NC} $1"; }

# Detect operating system and architecture
detect_system_info() {
    local os_name=$(uname -s)
    local arch=$(uname -m)
    
    case "$os_name" in
        Linux*)   OS="linux" ;;
        Darwin*)  OS="macos" ;;
        MINGW*|MSYS*|CYGWIN*) OS="windows" ;;
        *) log_error "Unsupported OS: $os_name"; exit 1 ;;
    esac
    
    case "$arch" in
        x86_64|amd64) ARCH="x64" ;;
        aarch64|arm64) ARCH="aarch64" ;;
        *) log_error "Unsupported architecture: $arch"; exit 1 ;;
    esac
    
    HOST_PLATFORM="$OS-$ARCH"
    export OS ARCH HOST_PLATFORM
}

# Check system requirements
check_system_requirements() {
    log_step "Checking system requirements for reproducible builds..."
    
    local issues=()
    
    # Check Rust toolchain
    if ! command -v rustc &>/dev/null; then
        issues+=("Rust compiler not found")
    else
        local rust_version=$(rustc --version | cut -d' ' -f2)
        log_info "Rust version: $rust_version"
        
        # Version comparison (simplified)
        local required_major=$(echo $REQUIRED_RUST_VERSION | cut -d'.' -f1)
        local required_minor=$(echo $REQUIRED_RUST_VERSION | cut -d'.' -f2)
        local current_major=$(echo $rust_version | cut -d'.' -f1)
        local current_minor=$(echo $rust_version | cut -d'.' -f2)
        
        if [[ $current_major -lt $required_major ]] || 
           [[ $current_major -eq $required_major && $current_minor -lt $required_minor ]]; then
            issues+=("Rust version $rust_version is older than required $REQUIRED_RUST_VERSION")
        fi
    fi
    
    if ! command -v cargo &>/dev/null; then
        issues+=("Cargo not found")
    fi
    
    if ! command -v rustup &>/dev/null; then
        issues+=("rustup not found")
    fi
    
    # Check Git (for reproducible builds via commit hashes)
    if ! command -v git &>/dev/null; then
        issues+=("Git not found (needed for build reproducibility)")
    fi
    
    # Platform-specific checks
    case "$OS" in
        linux)
            if ! command -v gcc &>/dev/null; then
                issues+=("GCC not found (needed for native compilation)")
            fi
            ;;
        macos)
            if ! command -v clang &>/dev/null; then
                issues+=("Clang not found (needed for native compilation)")
            fi
            ;;
        windows)
            if ! command -v cl.exe &>/dev/null && ! command -v gcc &>/dev/null; then
                log_warn "No C compiler found (MSVC or MinGW recommended)"
            fi
            ;;
    esac
    
    if [[ ${#issues[@]} -gt 0 ]]; then
        log_error "System requirements check failed:"
        for issue in "${issues[@]}"; do
            log_error "  - $issue"
        done
        return 1
    fi
    
    log_success "System requirements check passed"
}

# Generate reproducible build configuration
generate_build_config() {
    log_step "Generating reproducible build configuration..."
    
    local config_file="$CONFIG_DIR/build-config.env"
    mkdir -p "$CONFIG_DIR"
    
    # Get reproducible timestamp
    local source_epoch="${SOURCE_DATE_EPOCH:-$(git log -1 --format=%ct 2>/dev/null || date +%s)}"
    
    # Get Git information for reproducibility
    local git_commit="unknown"
    local git_dirty=""
    if command -v git &>/dev/null && git rev-parse --git-dir >/dev/null 2>&1; then
        git_commit=$(git rev-parse HEAD)
        if ! git diff-index --quiet HEAD --; then
            git_dirty="-dirty"
        fi
    fi
    
    # Generate configuration
    cat > "$config_file" << EOF
# Wasmtime4j Build Configuration
# Generated on: $(date -u -d "@$source_epoch" 2>/dev/null || date -u -r "$source_epoch" 2>/dev/null || date -u)
# Host: $HOST_PLATFORM
# Git: $git_commit$git_dirty

# Build reproducibility
export SOURCE_DATE_EPOCH=$source_epoch
export RUST_BACKTRACE=1

# Cargo configuration
export CARGO_NET_RETRY=10
export CARGO_HTTP_TIMEOUT=300
export CARGO_HTTP_MULTIPLEXING=false
export CARGO_INCREMENTAL=0

# Build flags for reproducibility
export RUSTFLAGS="\${RUSTFLAGS} -C embed-bitcode=no -C debuginfo=2 -C strip=none"

# Version information
export WASMTIME_VERSION=$WASMTIME_VERSION
export BUILD_CONFIG_VERSION=$BUILD_CONFIG_VERSION
export GIT_COMMIT=$git_commit
export HOST_PLATFORM=$HOST_PLATFORM

# Build paths
export CARGO_TARGET_DIR=\${CARGO_TARGET_DIR:-$PROJECT_ROOT/target}
export NATIVE_BUILD_CACHE=\${NATIVE_BUILD_CACHE:-$CONFIG_DIR/build-cache}

# Platform-specific settings
case "\$(uname -s)" in
    Linux*)
        export CC=\${CC:-gcc}
        export CXX=\${CXX:-g++}
        export AR=\${AR:-ar}
        export STRIP=\${STRIP:-strip}
        ;;
    Darwin*)
        export CC=\${CC:-clang}
        export CXX=\${CXX:-clang++}
        export AR=\${AR:-ar}
        export STRIP=\${STRIP:-strip}
        ;;
    MINGW*|MSYS*|CYGWIN*)
        # Windows-specific settings
        export RUSTFLAGS="\${RUSTFLAGS} -C target-feature=+crt-static"
        ;;
esac

# Load target-specific configuration if provided
if [[ -n "\${1:-}" ]]; then
    case "\$1" in
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
            if [[ -n "\${OSXCROSS_PATH:-}" ]]; then
                export CC=o64-clang
                export CXX=o64-clang++
                export AR=x86_64-apple-darwin15-ar
                export STRIP=x86_64-apple-darwin15-strip
                export CARGO_TARGET_X86_64_APPLE_DARWIN_LINKER=o64-clang
            fi
            ;;
        aarch64-apple-darwin)
            if [[ -n "\${OSXCROSS_PATH:-}" ]]; then
                export CC=oa64-clang
                export CXX=oa64-clang++
                export AR=aarch64-apple-darwin20-ar
                export STRIP=aarch64-apple-darwin20-strip
                export CARGO_TARGET_AARCH64_APPLE_DARWIN_LINKER=oa64-clang
            fi
            ;;
    esac
fi
EOF

    chmod +x "$config_file"
    log_success "Build configuration saved to: $config_file"
    
    # Create build info file
    local build_info_file="$CONFIG_DIR/build-info.json"
    cat > "$build_info_file" << EOF
{
  "version": "$BUILD_CONFIG_VERSION",
  "wasmtime_version": "$WASMTIME_VERSION",
  "host_platform": "$HOST_PLATFORM",
  "git_commit": "$git_commit",
  "git_dirty": "$git_dirty",
  "source_date_epoch": $source_epoch,
  "generated_at": "$(date -u -d "@$source_epoch" 2>/dev/null || date -u -r "$source_epoch" 2>/dev/null || date -u)",
  "rust_version": "$(rustc --version 2>/dev/null || echo 'unknown')",
  "cargo_version": "$(cargo --version 2>/dev/null || echo 'unknown')"
}
EOF
    
    log_success "Build info saved to: $build_info_file"
}

# Verify build reproducibility
verify_reproducibility() {
    log_step "Verifying build reproducibility setup..."
    
    local issues=()
    
    # Check if build config exists
    if [[ ! -f "$CONFIG_DIR/build-config.env" ]]; then
        issues+=("Build configuration file not found")
    fi
    
    # Check if SOURCE_DATE_EPOCH is set for reproducible builds
    if [[ -z "${SOURCE_DATE_EPOCH:-}" ]]; then
        log_warn "SOURCE_DATE_EPOCH not set - builds may not be reproducible"
    fi
    
    # Verify cross-compilation setup
    if [[ ! -d "$CONFIG_DIR" ]]; then
        issues+=("Cross-compilation setup not found (run setup-cross-compilation.sh)")
    fi
    
    # Check Cargo.lock exists for reproducible dependencies
    if [[ ! -f "$PROJECT_ROOT/Cargo.lock" ]]; then
        log_warn "Cargo.lock not found - dependency versions may vary between builds"
    fi
    
    # Check if git repository is clean
    if command -v git &>/dev/null && git rev-parse --git-dir >/dev/null 2>&1; then
        if ! git diff-index --quiet HEAD --; then
            log_warn "Git repository has uncommitted changes - build won't be reproducible"
        fi
    fi
    
    if [[ ${#issues[@]} -gt 0 ]]; then
        log_error "Build reproducibility verification failed:"
        for issue in "${issues[@]}"; do
            log_error "  - $issue"
        done
        return 1
    fi
    
    log_success "Build reproducibility verification passed"
}

# Create development environment setup
create_dev_env() {
    log_step "Creating development environment setup..."
    
    local dev_script="$CONFIG_DIR/dev-env.sh"
    
    cat > "$dev_script" << 'EOF'
#!/bin/bash
# Development environment setup for Wasmtime4j

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${BLUE}Loading Wasmtime4j development environment...${NC}"

# Load build configuration
if [[ -f "$SCRIPT_DIR/build-config.env" ]]; then
    source "$SCRIPT_DIR/build-config.env"
    echo -e "${GREEN}✓${NC} Build configuration loaded"
else
    echo "Warning: Build configuration not found. Run: ./scripts/build-config.sh"
fi

# Set up path for native scripts
export PATH="$PROJECT_ROOT/scripts:$PATH"

# Aliases for common tasks
alias wasmtime4j-build='./scripts/build-native.sh'
alias wasmtime4j-test='./mvnw test -q'
alias wasmtime4j-clean='./mvnw clean && ./scripts/build-native.sh clean'
alias wasmtime4j-setup='./scripts/setup-cross-compilation.sh'

echo -e "${GREEN}✓${NC} Development environment ready"
echo
echo "Available commands:"
echo "  wasmtime4j-build    - Build native libraries"
echo "  wasmtime4j-test     - Run tests"
echo "  wasmtime4j-clean    - Clean all build artifacts"
echo "  wasmtime4j-setup    - Setup cross-compilation"
echo
echo "Environment variables:"
echo "  SOURCE_DATE_EPOCH:  ${SOURCE_DATE_EPOCH:-not set}"
echo "  HOST_PLATFORM:      ${HOST_PLATFORM:-unknown}"
echo "  WASMTIME_VERSION:   ${WASMTIME_VERSION:-unknown}"
EOF

    chmod +x "$dev_script"
    log_success "Development environment script created: $dev_script"
    
    echo
    log_info "To load the development environment in your shell, run:"
    log_info "  source .cross-compilation/dev-env.sh"
}

# Main configuration function
main() {
    local action="${1:-setup}"
    
    case "$action" in
        setup)
            detect_system_info
            check_system_requirements
            generate_build_config
            create_dev_env
            verify_reproducibility
            ;;
        verify)
            detect_system_info
            verify_reproducibility
            ;;
        info)
            detect_system_info
            echo "System Information:"
            echo "  OS: $OS"
            echo "  Architecture: $ARCH"
            echo "  Platform: $HOST_PLATFORM"
            echo "  Rust: $(rustc --version 2>/dev/null || echo 'not found')"
            echo "  Cargo: $(cargo --version 2>/dev/null || echo 'not found')"
            
            if [[ -f "$CONFIG_DIR/build-info.json" ]]; then
                echo
                echo "Build Configuration:"
                cat "$CONFIG_DIR/build-info.json" | grep -E '"(version|wasmtime_version|host_platform|git_commit)"' | sed 's/^/  /'
            fi
            ;;
        clean)
            if [[ -d "$CONFIG_DIR" ]]; then
                log_info "Cleaning build configuration..."
                rm -f "$CONFIG_DIR/build-config.env"
                rm -f "$CONFIG_DIR/build-info.json" 
                rm -f "$CONFIG_DIR/dev-env.sh"
                log_success "Build configuration cleaned"
            else
                log_info "No build configuration to clean"
            fi
            ;;
        help|--help|-h)
            echo "Usage: $0 [ACTION]"
            echo ""
            echo "Actions:"
            echo "  setup    Setup reproducible build configuration (default)"
            echo "  verify   Verify existing configuration"
            echo "  info     Show system and build information"
            echo "  clean    Clean build configuration"
            echo "  help     Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  DEBUG=true    Enable debug output"
            ;;
        *)
            log_error "Unknown action: $action"
            echo "Run: $0 help"
            exit 1
            ;;
    esac
    
    if [[ "$action" == "setup" ]]; then
        echo
        log_success "Build configuration setup completed!"
        echo
        log_info "Next steps:"
        log_info "  1. Load dev environment: source .cross-compilation/dev-env.sh"
        log_info "  2. Test build: ./scripts/build-native.sh check"
        log_info "  3. Build libraries: ./scripts/build-native.sh"
    fi
}

# Run main function with all arguments
main "$@"