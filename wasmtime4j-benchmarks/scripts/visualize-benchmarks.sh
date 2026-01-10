#!/usr/bin/env bash
#
# JMH Benchmark Visualizer
# Downloads, installs, and runs jmh-visualizer locally to view benchmark results.
#
# Usage: ./visualize-benchmarks.sh [results.json]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VISUALIZER_DIR="${SCRIPT_DIR}/.jmh-visualizer"
RESULTS_FILE="${1:-${SCRIPT_DIR}/../target/benchmark-results.json}"
PORT="${JMH_VISUALIZER_PORT:-3000}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    # Check for Node.js
    if ! command -v node &> /dev/null; then
        log_error "Node.js is required but not installed."
        log_info "Install Node.js from https://nodejs.org/ or via:"
        log_info "  brew install node       # macOS"
        log_info "  apt install nodejs npm  # Debian/Ubuntu"
        exit 1
    fi

    # Check for npm
    if ! command -v npm &> /dev/null; then
        log_error "npm is required but not installed."
        exit 1
    fi

    log_info "Node.js $(node --version) and npm $(npm --version) found"
}

install_visualizer() {
    if [ -d "${VISUALIZER_DIR}" ] && [ -f "${VISUALIZER_DIR}/package.json" ]; then
        log_info "jmh-visualizer already installed at ${VISUALIZER_DIR}"
        return 0
    fi

    log_info "Downloading jmh-visualizer..."

    # Clone the repository
    if command -v git &> /dev/null; then
        git clone --depth 1 https://github.com/jzillmann/jmh-visualizer.git "${VISUALIZER_DIR}"
    else
        # Fallback to downloading as zip
        log_info "Git not found, downloading as archive..."
        mkdir -p "${VISUALIZER_DIR}"
        curl -sL https://github.com/jzillmann/jmh-visualizer/archive/refs/heads/master.zip -o /tmp/jmh-visualizer.zip
        unzip -q /tmp/jmh-visualizer.zip -d /tmp
        mv /tmp/jmh-visualizer-master/* "${VISUALIZER_DIR}/"
        rm -rf /tmp/jmh-visualizer.zip /tmp/jmh-visualizer-master
    fi

    log_info "Installing dependencies..."
    cd "${VISUALIZER_DIR}"
    npm install --silent

    log_info "jmh-visualizer installed successfully"
}

copy_results() {
    if [ ! -f "${RESULTS_FILE}" ]; then
        log_error "Results file not found: ${RESULTS_FILE}"
        log_info "Run benchmarks first with: ./mvnw exec:java -Pbenchmark-report"
        exit 1
    fi

    # Copy results to visualizer's public directory
    mkdir -p "${VISUALIZER_DIR}/public/examples"
    cp "${RESULTS_FILE}" "${VISUALIZER_DIR}/public/examples/latest-results.json"
    log_info "Copied results to visualizer"
}

start_server() {
    cd "${VISUALIZER_DIR}"

    # Check if port is already in use
    if lsof -Pi :${PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then
        log_warn "Port ${PORT} is already in use"
        log_info "Opening browser to existing server..."
    else
        log_info "Starting jmh-visualizer on port ${PORT}..."

        # Start in background
        npm start &
        SERVER_PID=$!

        # Wait for server to start
        sleep 3

        if ! kill -0 $SERVER_PID 2>/dev/null; then
            log_error "Failed to start server"
            exit 1
        fi

        log_info "Server started (PID: ${SERVER_PID})"
    fi

    # Open browser
    open_browser "http://localhost:${PORT}"

    log_info ""
    log_info "=== JMH Visualizer ==="
    log_info "Server running at: http://localhost:${PORT}"
    log_info "Results file: ${RESULTS_FILE}"
    log_info ""
    log_info "To load your results:"
    log_info "  1. Click 'Select Files' or drag & drop"
    log_info "  2. Choose: ${RESULTS_FILE}"
    log_info ""
    log_info "Press Ctrl+C to stop the server"

    # Keep script running
    if [ -n "${SERVER_PID}" ]; then
        wait $SERVER_PID
    fi
}

open_browser() {
    local url="$1"

    if command -v xdg-open &> /dev/null; then
        xdg-open "$url" 2>/dev/null &
    elif command -v open &> /dev/null; then
        open "$url" 2>/dev/null &
    elif command -v start &> /dev/null; then
        start "$url" 2>/dev/null &
    else
        log_warn "Could not open browser automatically"
        log_info "Please open: $url"
    fi
}

cleanup() {
    if [ -n "${SERVER_PID}" ]; then
        log_info "Stopping server..."
        kill $SERVER_PID 2>/dev/null || true
    fi
}

trap cleanup EXIT

main() {
    log_info "JMH Benchmark Visualizer"
    log_info "========================"

    check_prerequisites
    install_visualizer
    copy_results
    start_server
}

main "$@"
