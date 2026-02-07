#!/bin/bash
# Synchronizes wasmtime version from wasmtime-version.properties to all configuration files
# This script is run automatically by Maven before Cargo build, or can be run manually

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PROPS_FILE="$PROJECT_ROOT/wasmtime-version.properties"

# Read properties file
if [ ! -f "$PROPS_FILE" ]; then
    echo "ERROR: $PROPS_FILE not found"
    exit 1
fi

# Parse properties (handles comments and empty lines)
WASMTIME_VERSION=$(grep '^wasmtime.version=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
WASMTIME4J_VERSION=$(grep '^wasmtime4j.version=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
FORK_BRANCH=$(grep '^wasmtime.fork.branch=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')

if [ -z "$WASMTIME_VERSION" ]; then
    echo "ERROR: wasmtime.version not found in $PROPS_FILE"
    exit 1
fi

if [ -z "$WASMTIME4J_VERSION" ]; then
    echo "ERROR: wasmtime4j.version not found in $PROPS_FILE"
    exit 1
fi

# Full version: ${wasmtime.version}-${wasmtime4j.version}
PKG_VERSION="${WASMTIME_VERSION}-${WASMTIME4J_VERSION}"

echo "Syncing versions:"
echo "  Wasmtime version: $WASMTIME_VERSION"
echo "  Wasmtime4j version: $WASMTIME4J_VERSION"
echo "  Full version: $PKG_VERSION"
echo "  Fork branch: $FORK_BRANCH"

# Update workspace Cargo.toml wasmtime dependency versions
echo "Updating Cargo.toml (workspace)..."
sed -i.bak -E "s/(wasmtime[^=]*= \{ version = \")[0-9.]+\"/\1${WASMTIME_VERSION}\"/g" "$PROJECT_ROOT/Cargo.toml"
rm -f "$PROJECT_ROOT/Cargo.toml.bak"

# Update fork branch in workspace Cargo.toml
if [ -n "$FORK_BRANCH" ]; then
    sed -i.bak -E "s|(branch = \"fix/global-code-registry-idempotent-)[^\"]+\"|\1${FORK_BRANCH#fix/global-code-registry-idempotent-}\"|g" "$PROJECT_ROOT/Cargo.toml"
    rm -f "$PROJECT_ROOT/Cargo.toml.bak"
fi

# Update wasmtime4j-native/Cargo.toml package version
echo "Updating wasmtime4j-native/Cargo.toml package version..."
sed -i.bak -E "s/^version = \"[0-9.-]+\"/version = \"${PKG_VERSION}\"/" "$PROJECT_ROOT/wasmtime4j-native/Cargo.toml"
rm -f "$PROJECT_ROOT/wasmtime4j-native/Cargo.toml.bak"

# Update lib.rs WASMTIME_VERSION constant
echo "Updating lib.rs WASMTIME_VERSION..."
sed -i.bak -E "s/(WASMTIME_VERSION: &str = \")[0-9.]+\"/\1${WASMTIME_VERSION}\"/" "$PROJECT_ROOT/wasmtime4j-native/src/lib.rs"
rm -f "$PROJECT_ROOT/wasmtime4j-native/src/lib.rs.bak"

# Update Maven properties in parent pom.xml
# Note: Maven uses CI-friendly versions with ${revision} = ${wasmtime.version}-${wasmtime4j.version}
# Child pom.xml files use ${revision} and don't need updating
echo "Updating pom.xml Maven properties..."
sed -i.bak -E "s|(<wasmtime.version>)[0-9.]+(<)|\1${WASMTIME_VERSION}\2|" "$PROJECT_ROOT/pom.xml"
sed -i.bak -E "s|(<wasmtime4j.version>)[0-9.]+(<)|\1${WASMTIME4J_VERSION}\2|" "$PROJECT_ROOT/pom.xml"
rm -f "$PROJECT_ROOT/pom.xml.bak"

echo ""
echo "Version sync complete!"
echo "  Wasmtime version: $WASMTIME_VERSION"
echo "  Wasmtime4j version: $WASMTIME4J_VERSION"
echo "  Full version: $PKG_VERSION"
echo "  Fork branch: $FORK_BRANCH"
echo ""
echo "Run './scripts/check-version-consistency.sh' to verify."
