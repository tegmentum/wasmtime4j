# Wasmtime4j Native Library

This module contains the native Rust library that provides unified bindings for the Wasmtime WebAssembly runtime. It serves as the foundation for both JNI (Java 8-22) and Panama FFI (Java 23+) implementations.

## Architecture Overview

The native library is designed to provide a single source of truth for Wasmtime functionality, eliminating code duplication between JNI and Panama implementations while ensuring consistent behavior across both interfaces.

```
wasmtime4j-native/
├── Cargo.toml              # Rust project configuration
├── build.rs                # Build script for platform-specific configuration
├── pom.xml                 # Maven configuration for packaging
├── src/
│   ├── main/rust/          # Rust source code
│   │   ├── lib.rs          # Main library entry point
│   │   ├── engine.rs       # Engine management
│   │   ├── module.rs       # Module compilation
│   │   ├── instance.rs     # Instance management
│   │   ├── store.rs        # Store management
│   │   ├── wasi.rs         # WASI support
│   │   ├── error.rs        # Error handling
│   │   ├── jni_bindings.rs # JNI-specific bindings
│   │   └── panama_ffi.rs   # Panama FFI bindings
│   ├── bin/
│   │   └── build-info.rs   # Build information utility
│   └── main/resources/natives/  # Platform-specific native libraries
│       ├── linux-x64/      # Linux x86_64 libraries
│       ├── linux-aarch64/  # Linux ARM64 libraries
│       ├── windows-x64/    # Windows x86_64 libraries
│       ├── macos-x64/      # macOS x86_64 libraries
│       └── macos-aarch64/  # macOS ARM64 libraries
└── scripts/
    ├── build-native.sh     # Unix build script
    └── build-native.bat    # Windows build script
```

## Key Features

### Dual Interface Support
- **JNI Bindings**: Compatible with Java 8-22 using traditional JNI approach
- **Panama FFI**: Modern C interop for Java 23+ using Foreign Function & Memory API

### Cross-Platform Support
- **Linux**: x86_64 and ARM64 (aarch64)
- **macOS**: Intel (x86_64) and Apple Silicon (ARM64)
- **Windows**: x86_64

### Wasmtime Integration
- **Version**: 36.0.2
- **Features**: Full Wasmtime API coverage including WASI, component model, async support
- **Performance**: Optimized for production use with minimal overhead

## Build Process

### Prerequisites
- Rust 1.75.0 or later
- Cargo (included with Rust)
- Platform-specific build tools (gcc, clang, MSVC, etc.)

### Maven Integration
The native library is built automatically during the Maven build process:

```bash
# Build with Maven (includes native compilation)
./mvnw clean compile -pl wasmtime4j-native

# Skip native compilation for CI/CD
./mvnw clean compile -pl wasmtime4j-native -Dnative.compile.skip=true

# Clean native artifacts
./mvnw clean -pl wasmtime4j-native
```

### Manual Build
For development and testing, you can build the native library directly:

```bash
# Build for current platform
./scripts/build-native.sh

# Clean build artifacts
./scripts/build-native.sh clean

# Check prerequisites only
./scripts/build-native.sh check
```

### Cross-Compilation
The build system supports cross-compilation for all target platforms:

```bash
# Install cross-compilation targets
rustup target add x86_64-unknown-linux-gnu
rustup target add aarch64-unknown-linux-gnu
rustup target add x86_64-pc-windows-gnu
rustup target add x86_64-apple-darwin
rustup target add aarch64-apple-darwin

# Build for specific target
cargo build --release --target x86_64-unknown-linux-gnu
```

## Library Structure

### Core Components

#### Engine (`engine.rs`)
- WebAssembly engine creation and configuration
- Compilation settings and optimization options
- Resource management and lifecycle

#### Module (`module.rs`)
- WebAssembly module compilation from bytecode
- Module validation and preprocessing
- Export/import analysis

#### Instance (`instance.rs`)
- WebAssembly instance creation and execution
- Function invocation and result handling
- Memory and table management

#### Store (`store.rs`)
- Execution context and state management
- Resource tracking and cleanup
- Thread-safe operations

#### WASI (`wasi.rs`)
- WebAssembly System Interface implementation
- File system, networking, and environment access
- Security sandbox configuration

### Interface Layers

#### JNI Bindings (`jni_bindings.rs`)
- Java Native Interface compatibility
- Java object marshalling and unmarshalling
- Exception handling and propagation
- Thread safety for JVM integration

#### Panama FFI (`panama_ffi.rs`)
- C-compatible function exports
- Memory layout compatibility
- Direct buffer access
- Native heap management

### Error Handling (`error.rs`)
- Unified error types and conversion
- Platform-specific error mapping
- Defensive programming practices
- Graceful failure modes

## Runtime Selection

The native library supports both JNI and Panama interfaces simultaneously:

1. **JNI Mode**: Used by wasmtime4j-jni module for Java 8-22
2. **Panama Mode**: Used by wasmtime4j-panama module for Java 23+

The selection is made at runtime by the calling Java code, allowing the same native library to serve both interfaces.

## Memory Management

### Safety Guarantees
- All native memory is properly managed and cleaned up
- No memory leaks or dangling pointers
- Defensive bounds checking on all operations
- Exception safety with RAII patterns

### Performance Optimizations
- Minimal allocation in hot paths
- Efficient data structure layouts
- Cache-friendly memory access patterns
- Reduced JNI/FFI call overhead

## Testing Strategy

### Unit Tests
```bash
# Run native unit tests
cargo test

# Run with coverage
cargo test --coverage
```

### Integration Tests
```bash
# Test native library loading
./mvnw test -pl wasmtime4j-native

# Test cross-platform compatibility
./mvnw test -pl wasmtime4j-native -Dtest=PlatformCompatibilityTest
```

## Development Guidelines

### Code Style
- Follow Rust idioms and best practices
- Use `rustfmt` for consistent formatting
- Enable all clippy lints for quality checks
- Document all public APIs with rustdoc

### Safety Requirements
- All unsafe code must be documented and justified
- Defensive programming for all external interfaces
- Comprehensive error handling without panics
- Memory safety verification with Miri

### Performance Considerations
- Profile hot paths with cargo flamegraph
- Minimize allocations in performance-critical sections
- Use appropriate data structures for access patterns
- Consider NUMA topology for memory placement

## Troubleshooting

### Common Issues

#### Build Failures
- Ensure Rust toolchain is properly installed
- Check that required system libraries are available
- Verify cross-compilation targets are installed

#### Runtime Issues
- Check native library path configuration
- Verify platform compatibility (architecture/OS)
- Review JVM/process permissions for native code

#### Performance Problems
- Profile native code execution
- Check for excessive JNI/FFI boundary crossings
- Verify proper resource cleanup

### Debug Configuration
```toml
[profile.dev]
debug = true
opt-level = 0
overflow-checks = true
```

### Release Configuration
```toml
[profile.release]
opt-level = 3
lto = true
codegen-units = 1
panic = "abort"
```

## Future Enhancements

### Planned Features
- Component Model support for WASI 2.0
- Advanced profiling and debugging integration
- Enhanced async/await support
- Memory64 for large address spaces

### Performance Improvements
- SIMD optimizations for data processing
- Multi-threaded compilation pipeline
- Advanced JIT compilation strategies
- Zero-copy data transfer optimizations

## Contributing

When contributing to the native library:

1. Follow Rust coding standards and use `rustfmt`
2. Add comprehensive tests for new functionality
3. Update documentation for API changes
4. Ensure cross-platform compatibility
5. Test with both JNI and Panama interfaces
6. Verify memory safety with appropriate tools

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](../LICENSE) file for details.
