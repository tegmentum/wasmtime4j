# Wasmtime4j - Java Bindings for Wasmtime WebAssembly Runtime

[![Build Status](https://github.com/wasmtime4j/wasmtime4j/actions/workflows/ci.yml/badge.svg)](https://github.com/wasmtime4j/wasmtime4j/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ai.tegmentum/wasmtime4j/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ai.tegmentum/wasmtime4j)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Wasmtime4j provides unified Java bindings for the [Wasmtime](https://wasmtime.dev/) WebAssembly runtime, offering both JNI and Panama Foreign Function Interface implementations with automatic runtime selection based on Java version.

## Key Features

- **Dual Implementation Strategy**: JNI for Java 8-22, Panama FFI for Java 23+
- **Automatic Runtime Selection**: Detects Java version and chooses optimal implementation
- **Unified API**: Single interface across different runtime implementations
- **Cross-Platform Support**: Linux, Windows, macOS (x86_64 and ARM64)
- **Production Ready**: Comprehensive error handling, resource management, and defensive programming
- **Latest Wasmtime**: Built against Wasmtime 37.0.2 for cutting-edge WebAssembly support

## Quick Start

### Prerequisites

- **Java 8+** (JNI implementation)
- **Java 23+** (Panama FFI implementation - recommended for best performance)
- **Rust toolchain** (for building from source)

### Installation

Add Wasmtime4j to your Maven project:

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or for Gradle:

```gradle
implementation 'ai.tegmentum:wasmtime4j:1.0.0'
```

### Basic Usage

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

public class HelloWasm {
    public static void main(String[] args) throws Exception {
        // Create a runtime with automatic implementation selection
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {

            // Create an engine with default configuration
            try (Engine engine = runtime.createEngine()) {

                // Load WebAssembly bytecode (example: simple add function)
                byte[] wasmBytes = loadWasmFromFile("example.wasm");

                // Compile the module
                try (Module module = engine.compileModule(wasmBytes)) {

                    // Create a store for execution context
                    try (Store store = engine.createStore()) {

                        // Instantiate the module
                        try (Instance instance = module.instantiate(store)) {

                            // Get and call an exported function
                            Optional<WasmFunction> addFunc = instance.getFunction("add");
                            if (addFunc.isPresent()) {
                                WasmValue[] result = addFunc.get().call(
                                    WasmValue.i32(5),
                                    WasmValue.i32(3)
                                );
                                System.out.println("Result: " + result[0].asInt()); // Outputs: 8
                            }
                        }
                    }
                }
            }
        }
    }

    private static byte[] loadWasmFromFile(String path) {
        // Implementation to load WASM file...
        return new byte[0];
    }
}
```

### Runtime Selection

Wasmtime4j automatically selects the best available implementation:

```java
// Automatic selection (recommended)
WasmRuntime runtime = WasmRuntimeFactory.create();

// Manual selection
WasmRuntime jniRuntime = WasmRuntimeFactory.createJni();
WasmRuntime panamaRuntime = WasmRuntimeFactory.createPanama(); // Java 23+ only

// Check runtime information
RuntimeInfo info = runtime.getRuntimeInfo();
System.out.println("Using: " + info.getRuntimeType()); // JNI or PANAMA
System.out.println("Wasmtime version: " + info.getWasmtimeVersion());
```

## Project Structure

```
wasmtime4j/
├── wasmtime4j/               # Public API interfaces and factory
├── wasmtime4j-jni/           # JNI implementation (Java 8+)
├── wasmtime4j-panama/        # Panama FFI implementation (Java 23+)
├── wasmtime4j-native/        # Shared native Rust library
├── wasmtime4j-benchmarks/    # Performance benchmarks
└── wasmtime4j-tests/         # Integration tests and WebAssembly test suites
```

## Building from Source

### Prerequisites

- Java 8+ (for JNI) or Java 23+ (for Panama)
- Rust toolchain (latest stable)
- Maven 3.6+

### Build Commands

```bash
# Clean build
./mvnw clean compile

# Run tests (simplified JUnit testing framework - 75%+ faster execution)
./mvnw test

# Package artifacts
./mvnw clean package

# Install to local repository
./mvnw clean install

# Run quality checks
./mvnw checkstyle:check spotless:check

# Auto-format code
./mvnw spotless:apply
```

### Cross-Platform Build

The build process automatically cross-compiles native libraries for all supported platforms:

```bash
# Build for all platforms (requires cross-compilation setup)
./mvnw clean package -Pcross-compile

# Build for current platform only
./mvnw clean package
```

## Configuration

### Engine Configuration

```java
// Create engine with custom configuration
EngineConfig config = new EngineConfig()
    .optimizationLevel(OptimizationLevel.SPEED)
    .debugInfo(true)
    .consumeFuel(true)
    .parallelCompilation(true);

Engine engine = runtime.createEngine(config);
```

### Built-in Configurations

```java
// Optimized for speed
Engine speedEngine = runtime.createEngine(EngineConfig.forSpeed());

// Optimized for size
Engine sizeEngine = runtime.createEngine(EngineConfig.forSize());

// Optimized for debugging
Engine debugEngine = runtime.createEngine(EngineConfig.forDebug());
```

## Performance

Wasmtime4j includes comprehensive benchmarks comparing JNI and Panama implementations:

```bash
# Run all benchmarks
./mvnw exec:java -pl wasmtime4j-benchmarks

# Run specific benchmarks
java -jar wasmtime4j-benchmarks/target/benchmarks.jar RuntimeInitializationBenchmark
```

### Performance Characteristics

- **Panama FFI**: Lower overhead for function calls, better with Java 23+
- **JNI**: Broader compatibility, mature implementation, good performance on older Java versions
- **Automatic Selection**: Chooses Panama on Java 23+ for optimal performance

## Memory Management

Wasmtime4j uses defensive programming to prevent native resource leaks:

```java
// All resources implement AutoCloseable for try-with-resources
try (WasmRuntime runtime = WasmRuntimeFactory.create();
     Engine engine = runtime.createEngine();
     Module module = engine.compileModule(wasmBytes);
     Store store = engine.createStore();
     Instance instance = module.instantiate(store)) {

    // Resources automatically cleaned up when leaving scope

} // All native resources properly released here
```

## Error Handling

Wasmtime4j provides structured exception handling:

```java
try {
    Module module = engine.compileModule(invalidWasm);
} catch (CompilationException e) {
    System.err.println("Compilation failed: " + e.getMessage());
} catch (ValidationException e) {
    System.err.println("Module validation failed: " + e.getMessage());
} catch (WasmException e) {
    System.err.println("WebAssembly error: " + e.getMessage());
}
```

## Advanced Features

### WASI Support

```java
// Create WASI context (when available)
WasiConfig wasiConfig = new WasiConfig()
    .inheritEnv()
    .inheritStdin()
    .inheritStdout()
    .inheritStderr();

Store store = engine.createStore();
// Configure WASI imports...
```

### Host Functions

```java
// Define host function to call from WebAssembly
HostFunction logFunction = (args) -> {
    System.out.println("WASM says: " + args[0].asInt());
    return new WasmValue[0];
};

// Add to import map
ImportMap imports = ImportMap.empty()
    .addFunction("env", "log", logFunction);

Instance instance = module.instantiate(store, imports);
```

### Memory Access

```java
// Get exported memory
Optional<WasmMemory> memory = instance.getMemory("memory");
if (memory.isPresent()) {
    WasmMemory mem = memory.get();

    // Read/write memory
    byte[] data = mem.read(0, 1024);
    mem.write(0, "Hello, WASM!".getBytes());

    // Memory growth
    mem.grow(1); // Grow by 1 page (64KB)
}
```

## Platform Support

| Platform | Architecture | JNI Support | Panama Support |
|----------|-------------|-------------|----------------|
| Linux    | x86_64      | ✅          | ✅             |
| Linux    | ARM64       | ✅          | ✅             |
| Windows  | x86_64      | ✅          | ✅             |
| macOS    | x86_64      | ✅          | ✅             |
| macOS    | ARM64       | ✅          | ✅             |

## Testing Framework

Wasmtime4j uses a simplified, high-performance testing approach:

### Test Architecture
- **Simple JUnit Tests**: Direct JUnit 5 tests without complex wrappers or analytics
- **Fast Execution**: 75%+ faster than traditional BI-integrated test frameworks
- **Standard Reporting**: Compatible with Maven Surefire and standard CI/CD pipelines
- **Comprehensive Coverage**: All WebAssembly operations and edge cases covered

### Running Tests
```bash
# Run all tests (fast execution)
./mvnw test

# Run specific test class
./mvnw test -Dtest=WasmRuntimeTest

# Run tests with verbose output for debugging
./mvnw test -Dtest.verbose=true
```

### Test Categories
- **Unit Tests**: Core WebAssembly functionality testing
- **Integration Tests**: Cross-module compatibility validation
- **Performance Tests**: Runtime performance validation
- **WASI Tests**: WebAssembly System Interface compatibility

### Developer Benefits
- **Fast Feedback**: Quick test execution for rapid development cycles
- **Simple Debugging**: Clear, direct test output without analytics overhead
- **Standard Tools**: Works with all standard Java development tools
- **Maintainable**: Easy to understand and modify test cases

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup

1. Clone the repository
2. Install prerequisites (Java, Rust, Maven)
3. Run tests: `./mvnw test`
4. Make changes and ensure tests pass
5. Submit a pull request

### Code Style

The project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html):

```bash
# Check code style
./mvnw checkstyle:check

# Auto-format
./mvnw spotless:apply
```

## Troubleshooting

### Common Issues

**Native library loading fails:**
```bash
# Ensure native library is in classpath or system path
java -Djava.library.path=/path/to/natives -jar your-app.jar
```

**Java version compatibility:**
```bash
# Check Java version
java -version

# For Java 23+ with Panama
java --enable-preview --add-modules jdk.incubator.foreign -jar your-app.jar
```

**Build fails on cross-compilation:**
```bash
# Install Rust cross-compilation targets
rustup target add x86_64-pc-windows-gnu
rustup target add aarch64-apple-darwin
```

### Getting Help

- 📖 [Wiki](https://github.com/wasmtime4j/wasmtime4j/wiki)
- 🐛 [Issues](https://github.com/wasmtime4j/wasmtime4j/issues)
- 💬 [Discussions](https://github.com/wasmtime4j/wasmtime4j/discussions)
- 📧 [Mailing List](mailto:wasmtime4j@tegmentum.ai)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Wasmtime](https://wasmtime.dev/) - The WebAssembly runtime that powers this project
- [Bytecode Alliance](https://bytecodealliance.org/) - For their excellent WebAssembly ecosystem
- The Java and Rust communities for their invaluable tools and libraries

## Roadmap

- [ ] WASI Preview 2 support
- [ ] WebAssembly GC support
- [ ] Advanced debugging features
- [ ] Performance optimizations
- [ ] Extended platform support
- [ ] Integration with popular Java frameworks

---

**Built with ❤️ for the Java and WebAssembly communities**
