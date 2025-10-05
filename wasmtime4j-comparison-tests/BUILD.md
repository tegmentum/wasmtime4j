# Building and Testing wasmtime4j-comparison-tests

This guide covers building the native library, running tests, and troubleshooting common issues.

## Prerequisites

- **Rust**: 1.89.0 or later
- **Java**: 23+ (for Panama) or 8+ (for JNI)
- **Maven**: 3.6+ (via `./mvnw`)
- **Wasmtime Repository**: Clone from https://github.com/bytecodealliance/wasmtime

## Quick Start

```bash
# 1. Build native library with JNI bindings
cd wasmtime4j-native
cargo build --features jni-bindings

# 2. Copy to resources directory (required for Maven to package it)
mkdir -p target/cargo/aarch64-apple-darwin/debug  # Adjust for your platform
cp target/debug/libwasmtime4j.* target/cargo/aarch64-apple-darwin/debug/
cp target/debug/libwasmtime4j.* src/main/resources/natives/macos-aarch64/  # Adjust for your platform

# 3. Build and install Java modules
cd ..
./mvnw clean install -pl wasmtime4j-native,wasmtime4j,wasmtime4j-jni \
  -DskipTests \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dspotbugs.skip=true \
  -Dspotless.check.skip=true \
  -Dmaven.javadoc.skip=true

# 4. Run tests
./mvnw test -pl wasmtime4j-comparison-tests \
  -Dtest=SimpleWatCompilationTest \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dspotbugs.skip=true \
  -Dspotless.check.skip=true
```

## Platform-Specific Paths

**macOS ARM64 (Apple Silicon)**:
- Target: `aarch64-apple-darwin`
- Library: `libwasmtime4j.dylib`
- Resources: `src/main/resources/natives/macos-aarch64/`

**macOS x86_64 (Intel)**:
- Target: `x86_64-apple-darwin`
- Library: `libwasmtime4j.dylib`
- Resources: `src/main/resources/natives/macos-x86_64/`

**Linux x86_64**:
- Target: `x86_64-unknown-linux-gnu`
- Library: `libwasmtime4j.so`
- Resources: `src/main/resources/natives/linux-x86_64/`

**Linux ARM64**:
- Target: `aarch64-unknown-linux-gnu`
- Library: `libwasmtime4j.so`
- Resources: `src/main/resources/natives/linux-aarch64/`

**Windows x86_64**:
- Target: `x86_64-pc-windows-msvc`
- Library: `wasmtime4j.dll`
- Resources: `src/main/resources/natives/windows-x86_64/`

## Building Native Library

### Standard Build (Current Platform Only)

```bash
cd wasmtime4j-native
cargo build --features jni-bindings
```

### Release Build

```bash
cd wasmtime4j-native
cargo build --release --features jni-bindings
```

### Verify Native Symbols

**macOS/Linux**:
```bash
# Check for compileWat symbol
nm -g target/debug/libwasmtime4j.* | grep CompileWat

# Expected output:
# 00000000002c3038 T _Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCompileWat
```

**Windows**:
```powershell
# Use dumpbin or similar tool
dumpbin /EXPORTS target\debug\wasmtime4j.dll | findstr CompileWat
```

## Maven Build Configuration

### Skip Static Analysis (for faster builds)

```bash
./mvnw clean install \
  -DskipTests \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dspotbugs.skip=true \
  -Dspotless.check.skip=true \
  -Dmaven.javadoc.skip=true
```

### Skip Native Compilation (use existing library)

```bash
./mvnw install -pl wasmtime4j-native \
  -Dnative.compile.skip=true
```

## Running Tests

### Run All Comparison Tests

```bash
./mvnw test -pl wasmtime4j-comparison-tests \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dspotbugs.skip=true \
  -Dspotless.check.skip=true
```

### Run Specific Test Class

```bash
./mvnw test -pl wasmtime4j-comparison-tests \
  -Dtest=SimpleWatCompilationTest \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dspotbugs.skip=true \
  -Dspotless.check.skip=true
```

### Run Specific Test Method

```bash
./mvnw test -pl wasmtime4j-comparison-tests \
  -Dtest=SimpleWatCompilationTest#testSimpleWatCompilation \
  -Dcheckstyle.skip=true \
  -Dpmd.skip=true \
  -Dspotbugs.skip=true \
  -Dspotless.check.skip=true
```

## Troubleshooting

### Error: UnsatisfiedLinkError - nativeCompileWat not found

**Cause**: Native library doesn't have the `nativeCompileWat` symbol or wasn't loaded.

**Solution**:
1. Verify symbol exists: `nm -g wasmtime4j-native/target/debug/libwasmtime4j.* | grep CompileWat`
2. Copy library to resources: `cp wasmtime4j-native/target/debug/libwasmtime4j.* wasmtime4j-native/src/main/resources/natives/YOUR_PLATFORM/`
3. Rebuild: `./mvnw clean install -pl wasmtime4j-native,wasmtime4j-jni -DskipTests`

### Error: Constant string too long

**Cause**: Some generated tests have WAT strings exceeding Java's 65535-byte limit.

**Solution**: These tests are automatically excluded via `.gitignore` in the generated test directory.

**Affected Tests**:
- `Embenchen*.java`
- `BrTableTest.java`

### Error: Instantiation not yet implemented

**Cause**: Module instantiation is not implemented yet.

**Status**: This is expected. WAT compilation works, but you cannot instantiate and execute modules yet.

**Workaround**: Tests can verify WAT compilation succeeds, but cannot execute functions.

### Build Hangs During Cross-Compilation

**Cause**: Maven is trying to cross-compile for multiple platforms.

**Solution**: Skip native compilation and use existing library:
```bash
./mvnw install -pl wasmtime4j-native -Dnative.compile.skip=true
```

### Library Copied from Wrong Location

**Issue**: Maven packages old library instead of newly built one.

**Solution**: Always copy fresh build to resources directory:
```bash
cp wasmtime4j-native/target/debug/libwasmtime4j.* \
   wasmtime4j-native/src/main/resources/natives/YOUR_PLATFORM/
```

## Generating Tests from Wasmtime Repository

### Prerequisites

1. Clone Wasmtime repository:
```bash
git clone https://github.com/bytecodealliance/wasmtime.git ~/git/wasmtime
```

2. Checkout specific version (optional):
```bash
cd ~/git/wasmtime
git checkout v36.0.2
```

### Run Test Generator

```bash
cd wasmtime4j-comparison-tests

./mvnw exec:java \
  -Dexec.mainClass="ai.tegmentum.wasmtime4j.comparison.wasmtime.WasmtimeTestGeneratorCli" \
  -Dexec.args="~/git/wasmtime src/test/java"
```

### Generated Test Structure

```
src/test/java/ai/tegmentum/wasmtime4j/comparison/generated/
├── component_model/    # Component model tests
├── func/               # Function tests
├── host_funcs/         # Host function tests
├── misc_testsuite/     # WAST tests (117 tests)
└── traps/              # Trap handling tests
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Comparison Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]

    steps:
    - uses: actions/checkout@v3

    - name: Set up Rust
      uses: actions-rs/toolchain@v1
      with:
        toolchain: stable

    - name: Build native library
      working-directory: wasmtime4j-native
      run: cargo build --features jni-bindings

    - name: Copy native library
      run: |
        # Platform-specific copy commands
        # (see platform-specific paths above)

    - name: Set up JDK 23
      uses: actions/setup-java@v3
      with:
        java-version: '23'
        distribution: 'temurin'

    - name: Build with Maven
      run: ./mvnw clean install -DskipTests

    - name: Run tests
      run: ./mvnw test -pl wasmtime4j-comparison-tests
```

## Development Workflow

### Making Changes to Native Code

1. Edit Rust code in `wasmtime4j-native/src/`
2. Build: `cd wasmtime4j-native && cargo build --features jni-bindings`
3. Copy library: `cp target/debug/libwasmtime4j.* src/main/resources/natives/YOUR_PLATFORM/`
4. Rebuild Java: `cd .. && ./mvnw clean install -pl wasmtime4j-native,wasmtime4j-jni -DskipTests`
5. Test: `./mvnw test -pl wasmtime4j-comparison-tests -Dtest=SimpleWatCompilationTest`

### Adding New Tests

1. Create test class in `src/test/java/ai/tegmentum/wasmtime4j/comparison/`
2. Use `SimpleWatCompilationTest` as reference
3. Remember to use fully-qualified `ai.tegmentum.wasmtime4j.Module` type
4. Run: `./mvnw test -pl wasmtime4j-comparison-tests -Dtest=YourTestClass`

## Performance Optimization

### Debug vs Release Builds

**Debug Build** (default):
- Faster compilation
- Larger binary size
- No optimizations
- Useful for development

**Release Build**:
- Slower compilation
- Smaller binary size
- Full optimizations
- Use for production

```bash
cargo build --release --features jni-bindings
```

### Parallel Test Execution

```bash
./mvnw test -pl wasmtime4j-comparison-tests -T 4  # Use 4 threads
```

## Additional Resources

- [Wasmtime Documentation](https://docs.wasmtime.dev/)
- [JNI Specification](https://docs.oracle.com/en/java/javase/23/docs/specs/jni/index.html)
- [Rust FFI Guide](https://doc.rust-lang.org/nomicon/ffi.html)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
