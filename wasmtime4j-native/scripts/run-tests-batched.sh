#!/bin/bash
# Run tests in batches to avoid wasmtime GLOBAL_CODE registry exhaustion
# See WASMTIME_GLOBAL_CODE_ISSUE_REPORT.md for details

set -e

echo "Running wasmtime4j-native tests in batches..."
echo ""

TOTAL_PASSED=0
TOTAL_FAILED=0
TOTAL_IGNORED=0

# Define test module batches
# Each batch runs in a fresh process to avoid GLOBAL_CODE accumulation
BATCHES=(
    "engine"
    "store"
    "module"
    "instance"
    "memory"
    "table"
    "global"
    "component"
    "wasi"
    "gc"
    "thread"
    "linker"
    "hostfunc"
    "error"
    "ffi"
    "caller"
    "interop"
    "profiler"
    "security"
    "sandbox"
    "audit"
    "access"
    "execution"
    "fuel"
    "epoch"
    "coredump"
    "serialization"
    "streaming"
    "hot_reload"
    "simd"
    "typed"
    "value"
    "wit"
    "data"
    "element"
    "sourcemap"
    "cpu"
    "numa"
    "platform"
    "pooling"
    "process"
    "networking"
    "filesystem"
    "resource"
    "sync"
    "lockfree"
    "deadlock"
    "adaptive"
    "work_stealing"
    "distributed"
    "crypto"
    "advanced"
    "experimental"
    "panama"
    "jni"
    "shared"
    "wast"
    "test_runtime"
    "lib::tests"
)

for batch in "${BATCHES[@]}"; do
    echo "=== Running batch: $batch ==="

    # Capture output and parse results
    OUTPUT=$(cargo test --lib "$batch" -- --test-threads=1 2>&1) || true

    # Extract test results
    RESULT=$(echo "$OUTPUT" | grep "^test result:" | tail -1)

    if [ -n "$RESULT" ]; then
        # Parse: "test result: ok. X passed; Y failed; Z ignored; ..."
        PASSED=$(echo "$RESULT" | grep -oE '[0-9]+ passed' | grep -oE '[0-9]+' || echo 0)
        FAILED=$(echo "$RESULT" | grep -oE '[0-9]+ failed' | grep -oE '[0-9]+' || echo 0)
        IGNORED=$(echo "$RESULT" | grep -oE '[0-9]+ ignored' | grep -oE '[0-9]+' || echo 0)

        TOTAL_PASSED=$((TOTAL_PASSED + PASSED))
        TOTAL_FAILED=$((TOTAL_FAILED + FAILED))
        TOTAL_IGNORED=$((TOTAL_IGNORED + IGNORED))

        if [ "$FAILED" -gt 0 ]; then
            echo "  FAILED: $PASSED passed, $FAILED failed, $IGNORED ignored"
            echo "$OUTPUT" | grep -E "^test .* FAILED|^failures:" -A 100 || true
        else
            echo "  OK: $PASSED passed, $IGNORED ignored"
        fi
    else
        echo "  (no tests matched)"
    fi
done

echo ""
echo "=========================================="
echo "TOTAL: $TOTAL_PASSED passed, $TOTAL_FAILED failed, $TOTAL_IGNORED ignored"
echo "=========================================="

if [ "$TOTAL_FAILED" -gt 0 ]; then
    exit 1
fi
