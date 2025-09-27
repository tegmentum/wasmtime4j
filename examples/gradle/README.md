# Gradle Configuration Examples

This directory contains comprehensive Gradle configuration examples for integrating wasmtime4j into your projects.

## Quick Start

### Basic Gradle Dependency

Add wasmtime4j to your project's `build.gradle`:

```gradle
dependencies {
    implementation 'ai.tegmentum:wasmtime4j:1.0.0-SNAPSHOT'
}
```

### Complete Basic Example

See [`basic-example/`](basic-example/) for a minimal working project.

### Spring Boot Integration

See [`spring-boot-example/`](spring-boot-example/) for Spring Boot integration.

### Multi-Module Example

See [`multi-module-example/`](multi-module-example/) for complex project structures.

## Examples

| Directory | Description |
|-----------|-------------|
| [`basic-example/`](basic-example/) | Minimal Gradle project with wasmtime4j |
| [`spring-boot-example/`](spring-boot-example/) | Spring Boot web application |
| [`multi-module-example/`](multi-module-example/) | Multi-module Gradle project |
| [`kotlin-example/`](kotlin-example/) | Kotlin integration example |

## Getting Started

1. Choose the example that best matches your use case
2. Copy the `build.gradle` configuration to your project
3. Adapt the source code examples to your needs
4. Run `./gradlew build` to verify setup

## Requirements

- **Java**: 8+ (JNI), 23+ (Panama FFI)
- **Gradle**: 7.0+
- **Operating System**: Linux, Windows, macOS (x86_64, ARM64)