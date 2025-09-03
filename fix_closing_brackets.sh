#!/bin/bash

# Fix incorrectly commented closing brackets
find wasmtime4j-tests -name "*.java" -exec sed -i '' 's|//     });|          });|g' {} \;

echo "Fixed incorrectly commented closing brackets"
