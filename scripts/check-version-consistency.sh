#!/bin/bash
# Validates all version definitions match across the project
# Run this script to ensure Cargo.toml, pom.xml, and lib.rs are in sync

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PROPS_FILE="$PROJECT_ROOT/wasmtime-version.properties"

# Read expected versions from properties file (single source of truth)
if [ ! -f "$PROPS_FILE" ]; then
    echo "ERROR: $PROPS_FILE not found"
    exit 1
fi

EXPECTED_VERSION=$(grep '^wasmtime.version=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
WASMTIME4J_VERSION=$(grep '^wasmtime4j.version=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
EXPECTED_PKG_VERSION="${EXPECTED_VERSION}-${WASMTIME4J_VERSION}"

echo "Checking version consistency for wasmtime version $EXPECTED_VERSION..."

# Check workspace Cargo.toml wasmtime dependencies (single source of truth for Rust)
cargo_version=$(grep '^wasmtime = ' "$PROJECT_ROOT/Cargo.toml" | head -1 | grep -o '"[0-9.]*"' | tr -d '"')
if [ "$cargo_version" != "$EXPECTED_VERSION" ]; then
    echo "ERROR: Cargo.toml has wasmtime version $cargo_version, expected $EXPECTED_VERSION"
    exit 1
fi
echo "  ✓ Cargo.toml (workspace) wasmtime dependency: $cargo_version"

# Check wasmtime4j-native Cargo.toml package version (should be EXPECTED_VERSION-1.0.0)
cargo_pkg_version=$(grep '^version = ' "$PROJECT_ROOT/wasmtime4j-native/Cargo.toml" | head -1 | grep -o '"[0-9.-]*"' | tr -d '"')
expected_pkg_version="${EXPECTED_PKG_VERSION}"
if [ "$cargo_pkg_version" != "$expected_pkg_version" ]; then
    echo "ERROR: Cargo.toml package version is $cargo_pkg_version, expected $expected_pkg_version"
    exit 1
fi
echo "  ✓ Cargo.toml package version: $cargo_pkg_version"

# Check pom.xml wasmtime.version property
pom_version=$(grep '<wasmtime.version>' "$PROJECT_ROOT/pom.xml" | sed 's/.*<wasmtime.version>\([0-9.]*\)<.*/\1/')
if [ "$pom_version" != "$EXPECTED_VERSION" ]; then
    echo "ERROR: pom.xml has wasmtime.version $pom_version, expected $EXPECTED_VERSION"
    exit 1
fi
echo "  ✓ pom.xml wasmtime.version: $pom_version"

# Check pom.xml wasmtime4j.version property
pom_wasmtime4j_version=$(grep '<wasmtime4j.version>' "$PROJECT_ROOT/pom.xml" | sed 's/.*<wasmtime4j.version>\([0-9.]*\)<.*/\1/')
if [ "$pom_wasmtime4j_version" != "$WASMTIME4J_VERSION" ]; then
    echo "ERROR: pom.xml has wasmtime4j.version $pom_wasmtime4j_version, expected $WASMTIME4J_VERSION"
    exit 1
fi
echo "  ✓ pom.xml wasmtime4j.version: $pom_wasmtime4j_version"

# Check pom.xml uses CI-friendly versioning
pom_uses_revision=$(grep -c '<version>\${revision}</version>' "$PROJECT_ROOT/pom.xml" || true)
if [ "$pom_uses_revision" -eq 0 ]; then
    echo "ERROR: pom.xml should use \${revision} for version"
    exit 1
fi
echo "  ✓ pom.xml uses CI-friendly \${revision} versioning"

# Check lib.rs WASMTIME_VERSION constant
lib_version=$(grep 'WASMTIME_VERSION.*=' "$PROJECT_ROOT/wasmtime4j-native/src/lib.rs" | grep -o '"[0-9.]*"' | tr -d '"')
if [ "$lib_version" != "$EXPECTED_VERSION" ]; then
    echo "ERROR: lib.rs has WASMTIME_VERSION $lib_version, expected $EXPECTED_VERSION"
    exit 1
fi
echo "  ✓ lib.rs WASMTIME_VERSION: $lib_version"

# Check patch branch names in workspace Cargo.toml
patch_branch=$(grep 'branch = "fix/global-code-registry-idempotent' "$PROJECT_ROOT/Cargo.toml" | head -1 | sed 's/.*branch = "\([^"]*\)".*/\1/')
if [ -z "$patch_branch" ]; then
    echo "WARNING: No patch branch found in workspace Cargo.toml"
else
    echo "  ✓ Cargo.toml patch branch: $patch_branch"
    # Note: Branch should be updated when fork is rebased to match EXPECTED_VERSION
fi

echo ""
echo "All versions match: $EXPECTED_VERSION"
echo "Package version: $expected_pkg_version"
