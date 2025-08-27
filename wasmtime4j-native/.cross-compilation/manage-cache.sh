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
