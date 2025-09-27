# Maven Configuration Examples

This directory contains comprehensive Maven configuration examples for integrating wasmtime4j into your projects.

## Quick Start

### Basic Maven Dependency

Add wasmtime4j to your project's `pom.xml`:

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
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
| [`basic-example/`](basic-example/) | Minimal Maven project with wasmtime4j |
| [`spring-boot-example/`](spring-boot-example/) | Spring Boot web application |
| [`multi-module-example/`](multi-module-example/) | Multi-module Maven project |
| [`testing-example/`](testing-example/) | Comprehensive testing setup |
| [`performance-example/`](performance-example/) | Performance testing and benchmarking |

## Maven Archetype

Use the wasmtime4j Maven archetype to quickly bootstrap new projects:

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=my-wasmtime-app \
  -DarchetypeGroupId=ai.tegmentum \
  -DarchetypeArtifactId=wasmtime4j-archetype \
  -DarchetypeVersion=1.0.0-SNAPSHOT \
  -DinteractiveMode=false
```

## Getting Started

1. Choose the example that best matches your use case
2. Copy the `pom.xml` configuration to your project
3. Adapt the source code examples to your needs
4. Run `mvn clean compile test` to verify setup

## Requirements

- **Java**: 8+ (JNI), 23+ (Panama FFI)
- **Maven**: 3.6.0+
- **Operating System**: Linux, Windows, macOS (x86_64, ARM64)