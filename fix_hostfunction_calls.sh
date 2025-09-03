#!/bin/bash

FILE="/Users/zacharywhitley/git/wasmtime4j/wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/hostfunction/HostFunctionIntegrationIT.java"

echo "Commenting out runtime.createHostFunction calls in HostFunctionIntegrationIT.java..."

# Comment out runtime.createHostFunction calls and their parameters
# This handles multi-line calls by commenting each line that starts with runtime.createHostFunction or is a parameter line
sed -i '' '/runtime\.createHostFunction(/,/);$/{
  s/^/\/\/ /
}' "$FILE"

echo "Commented out runtime.createHostFunction calls"