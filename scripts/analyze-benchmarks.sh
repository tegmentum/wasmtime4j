#!/bin/bash
# Benchmark Regression Analyzer
# Usage: ./scripts/analyze-benchmarks.sh baseline.json current.json [threshold_percent]

set -e

BASELINE="$1"
CURRENT="$2"
THRESHOLD="${3:-5}"  # Default 5% regression threshold

if [[ -z "$BASELINE" || -z "$CURRENT" ]]; then
    echo "Usage: $0 <baseline.json> <current.json> [threshold_percent]"
    echo ""
    echo "Examples:"
    echo "  $0 benchmark-results/baseline.json benchmark-results/latest.json"
    echo "  $0 benchmark-results/baseline.json benchmark-results/latest.json 10"
    exit 1
fi

if [[ ! -f "$BASELINE" ]]; then
    echo "Error: Baseline file not found: $BASELINE"
    exit 1
fi

if [[ ! -f "$CURRENT" ]]; then
    echo "Error: Current file not found: $CURRENT"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo "Error: jq is required for analysis. Install with: brew install jq"
    exit 1
fi

echo "Benchmark Regression Analysis"
echo "=============================="
echo "Baseline: $BASELINE"
echo "Current:  $CURRENT"
echo "Threshold: ${THRESHOLD}%"
echo ""

# Create a comparison report
jq -r --slurpfile baseline "$BASELINE" '
    def find_baseline($name; $params):
        $baseline[0][] |
        select(.benchmark == $name) |
        select((.params // {}) == ($params // {}));

    .[] |
    . as $current |
    ($current.params // {}) as $params |
    (find_baseline($current.benchmark; $params) // null) as $base |
    if $base then
        {
            benchmark: $current.benchmark,
            params: $params,
            baseline_score: $base.primaryMetric.score,
            current_score: $current.primaryMetric.score,
            unit: $current.primaryMetric.scoreUnit,
            change_percent: ((($current.primaryMetric.score - $base.primaryMetric.score) / $base.primaryMetric.score) * 100)
        }
    else
        {
            benchmark: $current.benchmark,
            params: $params,
            baseline_score: null,
            current_score: $current.primaryMetric.score,
            unit: $current.primaryMetric.scoreUnit,
            change_percent: null
        }
    end
' "$CURRENT" | jq -s '.' > /tmp/comparison.json

# Summary statistics
echo "Summary"
echo "-------"

TOTAL=$(jq 'length' /tmp/comparison.json)
IMPROVED=$(jq "[.[] | select(.change_percent != null and .change_percent > $THRESHOLD)] | length" /tmp/comparison.json)
REGRESSED=$(jq "[.[] | select(.change_percent != null and .change_percent < -$THRESHOLD)] | length" /tmp/comparison.json)
UNCHANGED=$(jq "[.[] | select(.change_percent != null and .change_percent >= -$THRESHOLD and .change_percent <= $THRESHOLD)] | length" /tmp/comparison.json)
NEW=$(jq '[.[] | select(.baseline_score == null)] | length' /tmp/comparison.json)

echo "Total benchmarks: $TOTAL"
echo "  Improved (>${THRESHOLD}%): $IMPROVED"
echo "  Regressed (<-${THRESHOLD}%): $REGRESSED"
echo "  Unchanged: $UNCHANGED"
echo "  New (no baseline): $NEW"
echo ""

# Show regressions (IMPORTANT!)
if [[ "$REGRESSED" -gt 0 ]]; then
    echo "REGRESSIONS DETECTED!"
    echo "====================="
    jq -r "
        .[] |
        select(.change_percent != null and .change_percent < -$THRESHOLD) |
        \"\(.benchmark) \(.params | to_entries | map(\"\(.key)=\(.value)\") | join(\" \")): \(.baseline_score | floor) -> \(.current_score | floor) \(.unit) (\(.change_percent | . * 10 | floor / 10)%)\"
    " /tmp/comparison.json
    echo ""
fi

# Show improvements
if [[ "$IMPROVED" -gt 0 ]]; then
    echo "Improvements"
    echo "------------"
    jq -r "
        .[] |
        select(.change_percent != null and .change_percent > $THRESHOLD) |
        \"\(.benchmark) \(.params | to_entries | map(\"\(.key)=\(.value)\") | join(\" \")): \(.baseline_score | floor) -> \(.current_score | floor) \(.unit) (+\(.change_percent | . * 10 | floor / 10)%)\"
    " /tmp/comparison.json | head -20
    echo ""
fi

# Show new benchmarks
if [[ "$NEW" -gt 0 ]]; then
    echo "New Benchmarks (no baseline)"
    echo "----------------------------"
    jq -r '
        .[] |
        select(.baseline_score == null) |
        "\(.benchmark) \(.params | to_entries | map("\(.key)=\(.value)") | join(" ")): \(.current_score | floor) \(.unit)"
    ' /tmp/comparison.json | head -10
    echo ""
fi

# Exit with error if regressions detected
if [[ "$REGRESSED" -gt 0 ]]; then
    echo "WARNING: $REGRESSED benchmark(s) regressed more than ${THRESHOLD}%"
    exit 1
else
    echo "No significant regressions detected."
    exit 0
fi
