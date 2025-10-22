#!/bin/bash
set -e  # Exit on any error

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Wasmtime4j Verified Rebuild Script ===${NC}"
echo "This script performs a complete clean rebuild with verification"
echo

# Step 1: Kill any running Maven/Java processes
echo -e "${YELLOW}Step 1: Killing any running Maven/Java processes...${NC}"
pkill -f "maven" || true
pkill -f "surefire" || true
sleep 2

# Step 2: Clean all build artifacts
echo -e "${YELLOW}Step 2: Cleaning all build artifacts...${NC}"
cd /Users/zacharywhitley/git/wasmtime4j

# Clean Cargo
echo "  - Cleaning Cargo artifacts..."
cd wasmtime4j-native
cargo clean
cd ..

# Clean Maven
echo "  - Cleaning Maven artifacts..."
./mvnw clean -q

# Clean Maven local repository
echo "  - Cleaning Maven local repository..."
rm -rf ~/.m2/repository/ai/tegmentum/wasmtime4j-native
rm -rf ~/.m2/repository/ai/tegmentum/wasmtime4j-jni

# Clean JVM temp cache
echo "  - Cleaning JVM temp cache..."
rm -rf /var/folders/*/T/wasmtime4j-native-* 2>/dev/null || true

# Step 3: Add unique build marker
echo -e "${YELLOW}Step 3: Adding unique build timestamp marker...${NC}"
BUILD_MARKER="BUILD_VERIFIED_$(date +%Y%m%d_%H%M%S)"
echo "  Build marker: ${BUILD_MARKER}"

# Add marker to Rust code
sed -i.bak "s/ZYXWV_MARKER_PLACEHOLDER/ZYXWV_${BUILD_MARKER}/" wasmtime4j-native/src/jni_bindings.rs
rm wasmtime4j-native/src/jni_bindings.rs.bak

# Step 4: Build Rust library
echo -e "${YELLOW}Step 4: Building Rust library (release mode)...${NC}"
cd wasmtime4j-native
cargo build --release
cd ..

# Step 5: Verify marker in compiled library
echo -e "${YELLOW}Step 5: Verifying build marker in compiled library...${NC}"
DYLIB_PATH="wasmtime4j-native/target/release/libwasmtime4j.dylib"
if strings "$DYLIB_PATH" | grep -q "$BUILD_MARKER"; then
    echo -e "${GREEN}  ✓ Build marker found in compiled library${NC}"
else
    echo -e "${RED}  ✗ Build marker NOT found in compiled library${NC}"
    echo -e "${RED}  ERROR: Compilation may have failed or used cached artifacts${NC}"
    exit 1
fi

# Step 6: Maven install
echo -e "${YELLOW}Step 6: Installing to Maven local repository...${NC}"
./mvnw install -pl wasmtime4j-native,wasmtime4j-jni -DskipTests \
    -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true -q

# Step 7: Verify marker in JAR
echo -e "${YELLOW}Step 7: Verifying build marker in Maven JAR...${NC}"
JAR_PATH="$HOME/.m2/repository/ai/tegmentum/wasmtime4j-native/1.0.0-SNAPSHOT/wasmtime4j-native-1.0.0-SNAPSHOT-all-platforms.jar"
if [ -f "$JAR_PATH" ]; then
    if unzip -p "$JAR_PATH" natives/macos-aarch64/libwasmtime4j.dylib 2>/dev/null | strings | grep -q "$BUILD_MARKER"; then
        echo -e "${GREEN}  ✓ Build marker found in Maven JAR${NC}"
    else
        echo -e "${RED}  ✗ Build marker NOT found in Maven JAR${NC}"
        echo -e "${RED}  ERROR: JAR may contain old library${NC}"
        exit 1
    fi
else
    echo -e "${RED}  ✗ Maven JAR not found at expected location${NC}"
    exit 1
fi

# Step 8: Run test
echo -e "${YELLOW}Step 8: Running test...${NC}"
TEST_OUTPUT=$(./mvnw test -pl wasmtime4j-comparison-tests \
    -Dtest="MutableExternrefGlobalsTest" \
    -Djacoco.skip=true -Dspotbugs.skip=true -Dpmd.skip=true -Dcheckstyle.skip=true \
    2>&1 | tee /tmp/verified_build_test.txt)

# Step 9: Verify marker in test output
echo -e "${YELLOW}Step 9: Verifying build marker in test output...${NC}"
if echo "$TEST_OUTPUT" | grep -q "$BUILD_MARKER"; then
    echo -e "${GREEN}  ✓ Build marker found in test output${NC}"
    echo -e "${GREEN}  ✓ Test is running the freshly compiled library${NC}"
else
    echo -e "${RED}  ✗ Build marker NOT found in test output${NC}"
    echo -e "${RED}  ERROR: Test is running OLD library from unknown cache${NC}"
    echo
    echo -e "${YELLOW}Test output saved to: /tmp/verified_build_test.txt${NC}"
    echo -e "${YELLOW}Search for marker with: grep '$BUILD_MARKER' /tmp/verified_build_test.txt${NC}"
    exit 1
fi

# Step 10: Check test results
echo -e "${YELLOW}Step 10: Checking test results...${NC}"
if echo "$TEST_OUTPUT" | grep -q "BUILD SUCCESS"; then
    echo -e "${GREEN}═══════════════════════════════════${NC}"
    echo -e "${GREEN}  ✓ VERIFIED BUILD SUCCESSFUL${NC}"
    echo -e "${GREEN}═══════════════════════════════════${NC}"
    exit 0
elif echo "$TEST_OUTPUT" | grep -q "Parameter count mismatch"; then
    echo -e "${YELLOW}═══════════════════════════════════${NC}"
    echo -e "${YELLOW}  ⚠ Build verified, but test failed${NC}"
    echo -e "${YELLOW}  Error: Parameter count mismatch${NC}"
    echo -e "${YELLOW}  This is the actual bug to debug${NC}"
    echo -e "${YELLOW}═══════════════════════════════════${NC}"
    echo
    echo "Full test output:"
    cat /tmp/verified_build_test.txt
    exit 1
else
    echo -e "${RED}═══════════════════════════════════${NC}"
    echo -e "${RED}  ✗ Test failed with unexpected error${NC}"
    echo -e "${RED}═══════════════════════════════════${NC}"
    echo
    echo "Full test output:"
    cat /tmp/verified_build_test.txt
    exit 1
fi
