#!/bin/bash

# Fix malformed //     } patterns in HostFunctionIntegrationIT.java
FILE="/Users/zacharywhitley/git/wasmtime4j/wasmtime4j-tests/src/test/java/ai/tegmentum/wasmtime4j/hostfunction/HostFunctionIntegrationIT.java"

echo "Fixing malformed //     } patterns in HostFunctionIntegrationIT.java..."

# Replace all instances of "//     }" with proper indentation
sed -i '' 's|^              //     }$|            }|g' "$FILE"
sed -i '' 's|^      //     }$|    }|g' "$FILE"

echo "Fixed bracket patterns in HostFunctionIntegrationIT.java"