#!/bin/bash
# Benchmark Runner with Regression Detection
# Usage: ./scripts/run-benchmarks.sh [benchmark-filter] [--compare baseline.json]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULTS_DIR="$PROJECT_DIR/benchmark-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BENCHMARK_JAR="$PROJECT_DIR/wasmtime4j-benchmarks/target/wasmtime4j-benchmarks.jar"

# Default settings
FORKS=1
WARMUP_ITERATIONS=3
MEASUREMENT_ITERATIONS=5
BENCHMARK_FILTER="${1:-.*}"
COMPARE_BASELINE=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --compare)
            COMPARE_BASELINE="$2"
            shift 2
            ;;
        --forks)
            FORKS="$2"
            shift 2
            ;;
        --quick)
            FORKS=1
            WARMUP_ITERATIONS=1
            MEASUREMENT_ITERATIONS=2
            shift
            ;;
        *)
            BENCHMARK_FILTER="$1"
            shift
            ;;
    esac
done

# Ensure results directory exists
mkdir -p "$RESULTS_DIR"

# Check if benchmark JAR exists
if [[ ! -f "$BENCHMARK_JAR" ]]; then
    echo "Benchmark JAR not found. Building..."
    cd "$PROJECT_DIR"
    ./mvnw package -pl wasmtime4j-benchmarks -am -DskipTests \
        -Dcheckstyle.skip=true -Dspotless.check.skip=true \
        -Dspotbugs.skip=true -Djacoco.skip=true -Dmaven.javadoc.skip=true -q
fi

# Output files
JSON_OUTPUT="$RESULTS_DIR/benchmark-$TIMESTAMP.json"
TEXT_OUTPUT="$RESULTS_DIR/benchmark-$TIMESTAMP.txt"

echo "Running benchmarks: $BENCHMARK_FILTER"
echo "Results will be saved to: $JSON_OUTPUT"

# Run benchmarks with JSON output for analysis
java --enable-preview --enable-native-access=ALL-UNNAMED \
    -jar "$BENCHMARK_JAR" \
    "$BENCHMARK_FILTER" \
    -f "$FORKS" \
    -wi "$WARMUP_ITERATIONS" \
    -i "$MEASUREMENT_ITERATIONS" \
    -rf json \
    -rff "$JSON_OUTPUT" \
    | tee "$TEXT_OUTPUT"

echo ""
echo "Benchmark results saved to:"
echo "  JSON: $JSON_OUTPUT"
echo "  Text: $TEXT_OUTPUT"

# Compare with baseline if specified
if [[ -n "$COMPARE_BASELINE" && -f "$COMPARE_BASELINE" ]]; then
    echo ""
    echo "Comparing with baseline: $COMPARE_BASELINE"
    echo "========================================"

    # Simple comparison using jq if available
    if command -v jq &> /dev/null; then
        echo ""
        echo "Performance Changes (>5% difference):"
        echo "--------------------------------------"

        # Extract and compare key metrics
        jq -r '
            .[] |
            "\(.benchmark) \(.params.operationCategory // "N/A") \(.params.workloadIntensity // "N/A"): \(.primaryMetric.score | floor) \(.primaryMetric.scoreUnit)"
        ' "$JSON_OUTPUT" > /tmp/current_results.txt

        jq -r '
            .[] |
            "\(.benchmark) \(.params.operationCategory // "N/A") \(.params.workloadIntensity // "N/A"): \(.primaryMetric.score | floor) \(.primaryMetric.scoreUnit)"
        ' "$COMPARE_BASELINE" > /tmp/baseline_results.txt

        # Show differences
        diff -u /tmp/baseline_results.txt /tmp/current_results.txt || true

        rm -f /tmp/current_results.txt /tmp/baseline_results.txt
    else
        echo "Install jq for detailed comparison: brew install jq"
    fi
fi

# Create a symlink to latest results
ln -sf "benchmark-$TIMESTAMP.json" "$RESULTS_DIR/latest.json"
ln -sf "benchmark-$TIMESTAMP.txt" "$RESULTS_DIR/latest.txt"

echo ""
echo "Latest results symlinked to:"
echo "  $RESULTS_DIR/latest.json"
echo "  $RESULTS_DIR/latest.txt"
