# Wasmtime4j

[![Build Status](https://github.com/tegmentum/wasmtime4j/actions/workflows/ci.yml/badge.svg)](https://github.com/tegmentum/wasmtime4j/actions)
[![Maven Central](https://img.shields.io/maven-central/v/ai.tegmentum/wasmtime4j)](https://central.sonatype.com/artifact/ai.tegmentum/wasmtime4j)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Java bindings for the [Wasmtime](https://wasmtime.dev/) WebAssembly runtime. Provides both JNI and Panama Foreign Function Interface implementations with automatic runtime selection based on Java version.

Built against **Wasmtime 43.0.0**.

## Features

- **Dual runtime**: JNI for Java 8-22, Panama FFI for Java 23+ (auto-detected)
- **Full Wasmtime API**: Engine, Module, Instance, Store, Linker, host functions, memory, tables, globals
- **Component Model**: First-class support for the WebAssembly Component Model
- **WASI**: WASI Preview 1 and Preview 2 support
- **Typed function calls**: Zero-boxing fast paths for common signatures (`i32 -> i32`, `(i32, i32) -> i32`, etc.)
- **Cross-platform**: Linux, macOS, Windows on x86_64 and ARM64
- **Resource safety**: All resources implement `AutoCloseable` with defensive lifecycle management

## Quick Start

Add the dependency:

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>43.0.0-1.1.0</version>
</dependency>
```
### Basic Usage

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;

try (WasmRuntime runtime = WasmRuntimeFactory.create();
     Engine engine = runtime.createEngine();
     Module module = engine.compileModule(wasmBytes);
     Store store = engine.createStore();
     Instance instance = module.instantiate(store)) {

    WasmFunction add = instance.getFunction("add").orElseThrow();

    // Generic call with WasmValue boxing
    WasmValue[] result = add.call(WasmValue.i32(5), WasmValue.i32(3));
    System.out.println(result[0].asInt()); // 8

    // Typed fast-path call (zero boxing overhead)
    int sum = add.callI32I32ToI32(5, 3); // 8
}
```

### Runtime Selection

```java
// Automatic (recommended) - picks Panama on Java 23+, JNI otherwise
WasmRuntime runtime = WasmRuntimeFactory.create();

// Manual override
WasmRuntime jni = WasmRuntimeFactory.createJni();
WasmRuntime panama = WasmRuntimeFactory.createPanama(); // Java 23+ only

// Override via system property
// -Dwasmtime4j.runtime=jni
```

## Host Functions

```java
Linker linker = runtime.createLinker(engine);

linker.defineFunction("env", "log", new FunctionType(
    new WasmValueType[]{WasmValueType.I32}, new WasmValueType[]{}),
    (caller, args) -> {
        System.out.println("WASM says: " + args[0].asInt());
        return new WasmValue[0];
    });

Instance instance = linker.instantiate(store, module);
```

## Memory Access

```java
WasmMemory memory = instance.getMemory("memory").orElseThrow();

byte[] data = memory.read(0, 1024);
memory.write(0, "Hello, WASM!".getBytes());
memory.grow(1); // Grow by 1 page (64KB)
```

## Engine Configuration

```java
EngineConfig config = new EngineConfig()
    .optimizationLevel(OptimizationLevel.SPEED)
    .consumeFuel(true)
    .parallelCompilation(true);

Engine engine = runtime.createEngine(config);
```

## WASI

```java
WasiConfig wasiConfig = new WasiConfig()
    .inheritEnv()
    .inheritStdin()
    .inheritStdout()
    .inheritStderr();

Store store = engine.createStore();
store.setWasiConfig(wasiConfig);
```

## Project Structure

```
wasmtime4j/
├── wasmtime4j/               # Public API interfaces and factory (users depend on this)
├── wasmtime4j-native/        # Shared Rust library with JNI + Panama C exports
├── wasmtime4j-jni/           # JNI runtime implementation (Java 8+)
├── wasmtime4j-panama/        # Panama FFI runtime implementation (Java 23+)
├── wasmtime4j-native-loader/ # Cross-platform native library extraction and loading
├── wasmtime4j-tests/         # Integration tests and WebAssembly test suites
└── wasmtime4j-benchmarks/    # JMH benchmarks for JNI vs Panama comparison
```

The native library is built from Rust source in `wasmtime4j-native/` and bundled into a
single JAR containing binaries for all supported platforms. At runtime, `wasmtime4j-native-loader`
detects the platform and extracts the correct library.

## Building from Source

Requires Java 23+, Rust (stable), and Maven 3.6+.

```bash
# Build
./mvnw clean compile

# Test
./mvnw test

# Package
./mvnw clean package

# Code style
./mvnw spotless:apply
```

## Platform Support

| Platform | Architecture | JNI | Panama |
|----------|-------------|-----|--------|
| Linux    | x86_64      | Yes | Yes    |
| Linux    | ARM64       | Yes | Yes    |
| macOS    | ARM64       | Yes | Yes    |
| Windows  | x86_64      | Yes | Yes    |

Additional platforms can be added on request.

## Benchmarks

```bash
# Run JNI vs Panama comparison benchmarks
java --enable-native-access=ALL-UNNAMED \
  -jar wasmtime4j-benchmarks/target/wasmtime4j-benchmarks.jar \
  -f 1 -wi 2 -i 3 ".*PanamaVsJniBenchmark.*"
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

```bash
# Development workflow
git clone https://github.com/tegmentum/wasmtime4j.git
cd wasmtime4j
./mvnw test
# Make changes, ensure tests pass
# Submit a pull request
```

The project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Run `./mvnw spotless:apply` to auto-format.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Acknowledgments

- [Wasmtime](https://wasmtime.dev/) and the [Bytecode Alliance](https://bytecodealliance.org/) for the WebAssembly runtime
- The Java and Rust communities
