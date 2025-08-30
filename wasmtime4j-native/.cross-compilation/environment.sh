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
