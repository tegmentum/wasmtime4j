# Wasmtime4j Documentation

This directory contains comprehensive documentation for Wasmtime4j, a Java library providing unified bindings for the Wasmtime WebAssembly runtime.

## Documentation Structure

### [Getting Started](guides/getting-started.md)
Quick start guide to get you running your first WebAssembly module in Java within 15 minutes.

### [API Documentation](api/)
Complete API reference with Javadoc documentation for all public classes and methods.

### [User Guides](guides/)
- [Getting Started](guides/getting-started.md) - Installation and basic usage
- [Advanced Usage](guides/advanced-usage.md) - WASI integration and host functions
- [Performance Guide](guides/performance.md) - Optimization recommendations
- [Security Guide](guides/security.md) - Security considerations and best practices
- [Troubleshooting](guides/troubleshooting.md) - Common issues and solutions
- [Migration Guide](guides/migration.md) - Migrating from other WebAssembly runtimes

### [Examples](examples/)
Working code examples covering all major use cases:
- [Basic Examples](examples/basic/) - Simple WebAssembly operations
- [Advanced Examples](examples/advanced/) - Complex scenarios and optimizations
- [Integration Examples](examples/integration/) - Framework integrations
- [Spring Boot Examples](examples/spring-boot/) - Spring Boot integration patterns
- [WASI Examples](examples/wasi/) - WebAssembly System Interface usage

### [Tutorials](tutorials/)
Step-by-step tutorials for common development scenarios:
- Creating your first WebAssembly module
- Building a plugin system with WebAssembly
- Optimizing WebAssembly performance in Java

### [Architecture](architecture/)
Deep dive into the library's internal design:
- [Runtime Selection](architecture/runtime-selection.md) - JNI vs Panama FFI selection
- [Native Library Management](architecture/native-libraries.md) - Cross-platform native library handling
- [Memory Management](architecture/memory.md) - Memory safety and resource management

### [Reference](reference/)
- [WebAssembly Module Development](reference/webassembly-modules.md) - Creating compatible WASM modules
- [Build and Deployment](reference/build-deployment.md) - Production configuration
- [Platform Support](reference/platforms.md) - Supported platforms and requirements
- [Configuration Reference](reference/configuration.md) - All configuration options

## Quick Links

- **New Users**: Start with [Getting Started](guides/getting-started.md)
- **API Reference**: Browse the [complete API documentation](api/)
- **Examples**: See [working code examples](examples/) for your use case
- **Issues**: Check the [troubleshooting guide](guides/troubleshooting.md)
- **Performance**: Read the [performance optimization guide](guides/performance.md)

## Contributing

This documentation is part of the Wasmtime4j project. For contribution guidelines, see the main [CONTRIBUTING.md](../CONTRIBUTING.md) file.