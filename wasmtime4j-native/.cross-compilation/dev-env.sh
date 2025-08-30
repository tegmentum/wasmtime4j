#!/bin/bash
# Development environment setup for Wasmtime4j

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${BLUE}Loading Wasmtime4j development environment...${NC}"

# Load build configuration
if [[ -f "$SCRIPT_DIR/build-config.env" ]]; then
    source "$SCRIPT_DIR/build-config.env"
    echo -e "${GREEN}✓${NC} Build configuration loaded"
else
    echo "Warning: Build configuration not found. Run: ./scripts/build-config.sh"
fi

# Set up path for native scripts
export PATH="$PROJECT_ROOT/scripts:$PATH"

# Aliases for common tasks
alias wasmtime4j-build='./scripts/build-native.sh'
alias wasmtime4j-test='./mvnw test -q'
alias wasmtime4j-clean='./mvnw clean && ./scripts/build-native.sh clean'
alias wasmtime4j-setup='./scripts/setup-cross-compilation.sh'

echo -e "${GREEN}✓${NC} Development environment ready"
echo
echo "Available commands:"
echo "  wasmtime4j-build    - Build native libraries"
echo "  wasmtime4j-test     - Run tests"
echo "  wasmtime4j-clean    - Clean all build artifacts"
echo "  wasmtime4j-setup    - Setup cross-compilation"
echo
echo "Environment variables:"
echo "  SOURCE_DATE_EPOCH:  ${SOURCE_DATE_EPOCH:-not set}"
echo "  HOST_PLATFORM:      ${HOST_PLATFORM:-unknown}"
echo "  WASMTIME_VERSION:   ${WASMTIME_VERSION:-unknown}"
