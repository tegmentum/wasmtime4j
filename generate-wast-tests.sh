#!/bin/bash
#
# Script to generate Java test files from WAST files
# Usage: ./generate-wast-tests.sh
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

WAST_DIR="wasmtime4j-tests/src/test/resources/wasm/wasmtime-tests"
OUTPUT_DIR="wasmtime4j-comparison-tests/src/test/java"

echo "=== Generating tests from WAST files ==="
echo "WAST directory: $WAST_DIR"
echo "Output directory: $OUTPUT_DIR"
echo ""

# Find all WAST files with test directives
WAST_FILES=$(cd "$WAST_DIR" && grep -l "assert_return\|assert_trap\|assert_invalid" *.wast | sort)

COUNT=0
FAILED=0

for WAST_FILE in $WAST_FILES; do
  COUNT=$((COUNT + 1))
  WAST_PATH="$WAST_DIR/$WAST_FILE"

  echo "[$COUNT] Generating test from: $WAST_FILE"

  if ./mvnw exec:java -pl wasmtime4j-comparison-tests \
    -Dcheckstyle.skip=true \
    -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.codegen.WastTestGenerator" \
    -Dexec.args="$WAST_PATH $OUTPUT_DIR" \
    -q 2>&1 | grep -q "Generation complete"; then
    echo "  ✓ Success"
  else
    echo "  ✗ Failed"
    FAILED=$((FAILED + 1))
  fi
  echo ""
done

echo "=== Generation Summary ==="
echo "Total WAST files processed: $COUNT"
echo "Successful: $((COUNT - FAILED))"
echo "Failed: $FAILED"

if [ $FAILED -eq 0 ]; then
  echo ""
  echo "✓ All tests generated successfully!"
  exit 0
else
  echo ""
  echo "⚠ Some tests failed to generate"
  exit 1
fi
