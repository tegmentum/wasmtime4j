#!/bin/bash

# Fix the incorrect indentation for closing brackets in Java files
find wasmtime4j-tests -name "*.java" -exec sed -i '' 's/^[[:space:]]*});$/          });/g' {} \;

echo "Fixed bracket indentation"
