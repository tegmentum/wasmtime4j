---
created: 2025-08-27T00:32:32Z
last_updated: 2025-08-27T00:32:32Z
version: 1.0
author: Claude Code PM System
---

# Technology Context

## Primary Technology Stack

### Target Platform: Java WebAssembly Bindings
- **Purpose**: Unified Java bindings for Wasmtime WebAssembly runtime
- **Target JVM**: Java 8+ (JNI), Java 23+ (Panama FFI)
- **Native Runtime**: Wasmtime 36.0.2 (latest stable)
- **Build System**: Maven with wrapper (mvnw)

### Language Stack
- **Java**: Multi-version support (Java 8-23+)
- **Rust**: For native Wasmtime bindings library
- **Native Code**: C FFI layer for JNI/Panama interop

### Runtime Implementations
1. **JNI Implementation** (`wasmtime4j-jni`)
   - Target: Java 8-22
   - Uses: Rust API via JNI
   - Package: `ai.tegmentum.wasmtime4j.jni`

2. **Panama FFI Implementation** (`wasmtime4j-panama`)
   - Target: Java 23+
   - Uses: Foreign Function API
   - Package: `ai.tegmentum.wasmtime4j.panama`

## Development Dependencies

### Build Tools (Planned)
- **Maven**: Primary build system
- **Maven Wrapper**: `mvnw`/`mvnw.cmd` for consistent builds
- **Cross-compilation**: Multi-platform native library builds

### Quality Assurance Tools (Specified)
- **Checkstyle**: Google Java Style Guide enforcement
- **Spotless**: Automatic code formatting
- **SpotBugs**: Bug detection with FindSecBugs plugin
- **PMD**: Static analysis
- **JaCoCo**: Code coverage reporting
- **JMH**: Java Microbenchmark Harness for performance

### Testing Framework
- **JUnit 5** (Jupiter): Primary test framework
- **Maven Surefire Plugin**: Test execution
- **Test Categories**:
  - Unit tests
  - Integration tests  
  - WebAssembly test suites
  - Native library tests
  - Performance benchmarks

## External Dependencies

### Core Runtime
- **Wasmtime**: Latest stable (36.0.2)
- **WebAssembly**: Official test suites
- **Native Libraries**: Cross-platform compilation required

### Platform Support
- **Operating Systems**: Linux, Windows, macOS
- **Architectures**: x86_64, ARM64
- **Build Matrix**: All OS/architecture combinations

### Logging
- **Framework**: Java Util Logging (JUL)
- **Rationale**: Minimize external dependencies
- **Pattern**: Structured logging for consistency

## Current Tool Status

### Present
- **Git**: Repository initialized with remote
- **Claude Code PM**: Complete system installed
- **Documentation**: Comprehensive project specifications

### Missing/Not Yet Configured
- **Maven**: No `pom.xml` files
- **Java SDK**: Version requirements specified but not configured
- **Rust Toolchain**: Required for native library compilation
- **Native Wasmtime**: Source compilation during build
- **IDE Configuration**: No IDE-specific files
- **CI/CD Pipeline**: No automation configured

## Development Environment Requirements

### System Dependencies
- **Java Development Kit**: Multiple versions (8, 23+)
- **Rust Toolchain**: For native library compilation
- **Maven**: Build tool (via wrapper)
- **GitHub CLI**: For PM system integration
- **Git**: Version control

### Cross-Compilation Requirements
- **Target Platforms**: Linux, Windows, macOS
- **Target Architectures**: x86_64, ARM64
- **Native Toolchains**: Platform-specific compilation tools

## Architecture Decisions

### API Strategy
- **Public API**: Pure interfaces in `wasmtime4j` module
- **Implementation**: Private modules for JNI/Panama
- **Factory Pattern**: Runtime selection and loading
- **Shared Native**: Single Rust library for both implementations

### Performance Priorities
1. **Defensive Programming**: Prevent JVM crashes (highest priority)
2. **Performance Optimization**: Speed while maintaining safety
3. **Memory Management**: Efficient allocation and GC patterns
4. **Native Call Optimization**: Minimize JNI/Panama overhead

### Quality Standards
- **Code Style**: Google Java Style Guide (strict)
- **Line Length**: 120 characters maximum
- **Test Coverage**: Comprehensive with JaCoCo reporting
- **Static Analysis**: Multiple tools for quality assurance