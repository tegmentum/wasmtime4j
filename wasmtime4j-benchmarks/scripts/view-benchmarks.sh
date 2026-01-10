#!/usr/bin/env bash
#
# Quick JMH Benchmark Viewer
# Opens jmh.morethan.io (runs locally in browser JS) and serves results via local HTTP.
#
# Usage: ./view-benchmarks.sh [results.json]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_FILE="${1:-${SCRIPT_DIR}/../target/benchmark-results.json}"
PORT="${JMH_SERVER_PORT:-8000}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check results file exists
if [ ! -f "${RESULTS_FILE}" ]; then
    log_error "Results file not found: ${RESULTS_FILE}"
    log_info "Run benchmarks first:"
    log_info "  ./mvnw package exec:java -Pbenchmark-report -pl wasmtime4j-benchmarks"
    log_info ""
    log_info "Or run directly with JAR:"
    log_info "  java -jar target/wasmtime4j-benchmarks.jar -rf json -rff target/benchmark-results.json"
    exit 1
fi

RESULTS_DIR=$(dirname "${RESULTS_FILE}")
RESULTS_NAME=$(basename "${RESULTS_FILE}")

log_info "=== JMH Benchmark Viewer ==="
log_info "Results: ${RESULTS_FILE}"
log_info ""

# Start HTTP server
start_server() {
    cd "${RESULTS_DIR}"

    if command -v python3 &> /dev/null; then
        log_info "Starting Python HTTP server on port ${PORT}..."
        python3 -m http.server ${PORT} --bind 127.0.0.1 &
        SERVER_PID=$!
    elif command -v python &> /dev/null; then
        log_info "Starting Python HTTP server on port ${PORT}..."
        python -m SimpleHTTPServer ${PORT} &
        SERVER_PID=$!
    elif command -v npx &> /dev/null; then
        log_info "Starting npx serve on port ${PORT}..."
        npx serve -l ${PORT} &
        SERVER_PID=$!
    elif command -v php &> /dev/null; then
        log_info "Starting PHP server on port ${PORT}..."
        php -S 127.0.0.1:${PORT} &
        SERVER_PID=$!
    else
        log_error "No HTTP server available (python3, python, npx, or php required)"
        log_info ""
        log_info "Alternative: Open jmh.morethan.io and drag-drop the file:"
        log_info "  ${RESULTS_FILE}"
        exit 1
    fi

    sleep 1
}

open_browser() {
    local url="$1"
    if command -v open &> /dev/null; then
        open "$url"
    elif command -v xdg-open &> /dev/null; then
        xdg-open "$url"
    elif command -v start &> /dev/null; then
        start "$url"
    else
        log_warn "Could not open browser. Please open: $url"
    fi
}

cleanup() {
    if [ -n "${SERVER_PID}" ]; then
        log_info "Stopping server..."
        kill $SERVER_PID 2>/dev/null || true
    fi
}

trap cleanup EXIT INT TERM

# Main
start_server

log_info ""
log_info "Local file server: http://localhost:${PORT}/${RESULTS_NAME}"
log_info ""
log_info "Opening jmh.morethan.io in browser..."
log_info "  1. Click 'Specify URL' or drag-drop the JSON file"
log_info "  2. Enter: http://localhost:${PORT}/${RESULTS_NAME}"
log_info ""
log_info "Press Ctrl+C to stop"

open_browser "https://jmh.morethan.io/"

wait $SERVER_PID
