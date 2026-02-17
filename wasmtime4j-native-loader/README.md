# Wasmtime4j Native Library Loader

A high-performance, secure native library loading solution for Java applications. This library provides platform detection, resource extraction, and native library loading capabilities with comprehensive security controls and flexible configuration options.

## Features

- **Cross-Platform Support**: Automatic platform detection for Linux, Windows, and macOS (x86_64 and ARM64)
- **Secure Loading**: Comprehensive validation and path traversal protection
- **Flexible Resource Paths**: Support for multiple path conventions including Maven native, custom patterns, and wasmtime4j conventions
- **Performance Optimized**: Efficient resource extraction with caching and minimal overhead
- **Thread-Safe**: All operations are thread-safe and configurations are immutable
- **Zero Dependencies**: Pure Java 8+ implementation with no external runtime dependencies
- **Builder Pattern**: Fluent API for advanced configuration scenarios

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j-native-loader</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Simple Usage

```java
import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils.LibraryLoadInfo;

// Load with default configuration
LibraryLoadInfo info = NativeLoader.loadLibrary("mylib");

if (info.isSuccessful()) {
    System.out.println("Library loaded from: " + info.getExtractedPath());
} else {
    System.err.println("Failed to load: " + info.getErrorMessage());
}
```

### Advanced Configuration

```java
import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.PathConvention;

LibraryLoadInfo info = NativeLoader.builder()
    .libraryName("mylib")
    .tempFilePrefix("mylib-native-")
    .pathConvention(PathConvention.MAVEN_NATIVE)
    .load();
```

## Platform Support

### Supported Operating Systems

| OS | Architectures | Status |
|---|---|---|
| Linux | x86_64, aarch64 | ✅ Fully Supported |
| Windows | x86_64, aarch64 | ✅ Fully Supported |
| macOS | x86_64, aarch64 (Apple Silicon) | ✅ Fully Supported |

### Platform Detection

The library automatically detects the current platform and selects the appropriate native library:

```java
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;

// Get current platform information
String platform = PlatformDetector.getCurrentPlatform(); // e.g., "linux-x86_64"
String os = PlatformDetector.getOperatingSystem();       // e.g., "linux"
String arch = PlatformDetector.getArchitecture();        // e.g., "x86_64"
```

## Resource Path Conventions

The library supports multiple conventions for locating native libraries within JAR resources:

### Built-in Conventions

1. **WASMTIME4J** (default): `/native/{platform}/{lib}{name}{ext}`
2. **MAVEN_NATIVE**: `/META-INF/native/{platform}/{lib}{name}{ext}`
3. **JNA**: `/{platform}/{name}{ext}`

### Custom Path Patterns

Create custom path patterns with placeholder substitution:

```java
LibraryLoadInfo info = NativeLoader.builder()
    .libraryName("mylib")
    .customPathPattern("/libs/{platform}/{name}.{ext}")
    .load();
```

### Available Placeholders

- `{platform}` - Full platform identifier (e.g., "linux-x86_64")
- `{os}` - Operating system name (e.g., "linux")
- `{arch}` - Architecture name (e.g., "x86_64")
- `{lib}` - Platform-specific library prefix ("lib" on Unix, empty on Windows)
- `{name}` - Library name
- `{ext}` - Platform-specific library extension (".so", ".dll", ".dylib")

### Convention Priority

Use multiple conventions with fallback behavior:

```java
LibraryLoadInfo info = NativeLoader.builder()
    .libraryName("mylib")
    .conventionPriority(
        PathConvention.MAVEN_NATIVE,
        PathConvention.WASMTIME4J,
        PathConvention.JNA
    )
    .load();
```

## Performance Characteristics

### Benchmarks

Based on JMH benchmarks with Java 17 on Linux x86_64:

| Operation | Throughput (ops/ms) | Average Time |
|---|---|---|
| Platform Detection | ~50,000 | ~0.02 ms |
| Path Resolution | ~25,000 | ~0.04 ms |
| Library Loading | ~1,000 | ~1.0 ms |

### Memory Usage

- Minimal memory footprint (~50KB for core classes)
- Efficient resource extraction with streaming
- No memory leaks or resource retention

### Caching

- Temporary files are automatically cleaned up
- Platform detection results are cached
- Resource paths are resolved once per configuration

## Integration Examples

### Spring Boot Application

```java
@Component
public class NativeLibraryInitializer {

    @PostConstruct
    public void loadNativeLibraries() {
        LibraryLoadInfo info = NativeLoader.builder()
            .libraryName("mylib")
            .tempFilePrefix("myapp-")
            .load();

        if (!info.isSuccessful()) {
            throw new RuntimeException("Failed to load native library: " +
                info.getErrorMessage());
        }
    }
}
```

### Custom Framework Integration

```java
public class MyFrameworkNativeLoader {

    public static void loadFrameworkNatives() {
        // Try framework-specific path first, then fallback to standard
        LibraryLoadInfo info = NativeLoader.builder()
            .libraryName("myframework-native")
            .conventionPriority(
                PathConvention.custom("/framework/native/{platform}/{lib}{name}{ext}"),
                PathConvention.MAVEN_NATIVE
            )
            .load();

        if (!info.isSuccessful()) {
            // Handle error appropriately
            throw new RuntimeException("Framework native library not found");
        }
    }
}
```

## API Reference

### NativeLoader

Static factory methods for common loading scenarios.

**Methods:**
- `loadLibrary(String libraryName)` - Load with default configuration
- `builder()` - Create a new builder for advanced configuration

### NativeLoaderBuilder

Fluent builder for configuring native library loading.

**Configuration Methods:**
- `libraryName(String name)` - Set the library name
- `tempFilePrefix(String prefix)` - Set temporary file prefix
- `tempDirSuffix(String suffix)` - Set temporary directory suffix
- `pathConvention(PathConvention convention)` - Set resource path convention
- `customPathPattern(String pattern)` - Use custom path pattern
- `conventionPriority(PathConvention... conventions)` - Set fallback order

**Loading Method:**
- `load()` - Execute the configuration and load the library

### LibraryLoadInfo

Result object containing information about the loading attempt.

**Methods:**
- `isSuccessful()` - Whether loading succeeded
- `getExtractedPath()` - Path where library was extracted to
- `getLoadingMethod()` - How the library was loaded (SYSTEM_LIBRARY_PATH, EXTRACTED_FROM_JAR)
- `getErrorMessage()` - Error details if loading failed
- `getAttemptedPaths()` - List of resource paths that were tried

## Troubleshooting

### Common Issues

**Issue: Library not found**
```
Solution: Verify the library exists in your JAR at the expected path:
- Check path convention matches your JAR structure
- Use conventionPriority() to try multiple paths
- Enable debug logging to see attempted paths
```

**Issue: Platform not detected**
```
Solution: The platform may not be supported or detected correctly:
- Check PlatformDetector.getCurrentPlatform() output
- Verify your platform is in the supported list
- Consider using a custom path pattern for non-standard platforms
```

**Issue: Permission denied**
```
Solution: Temporary directory or extracted file lacks permissions:
- Check java.io.tmpdir system property
- Verify application has write permissions to temp directory
```

**Issue: Path traversal validation errors**
```
Solution: Path validation is rejecting the library path:
- Check that resource paths don't contain ".." or other suspicious patterns
- Verify your custom path pattern is valid
```

### Debug Logging

Enable verbose logging to diagnose issues:

```java
// Add before loading
System.setProperty("java.util.logging.level", "FINE");

LibraryLoadInfo info = NativeLoader.builder()
    .libraryName("mylib")
    .load();
```

## Migration Guide

### From System.loadLibrary()

**Before:**
```java
try {
    System.loadLibrary("mylib");
} catch (UnsatisfiedLinkError e) {
    // Handle error
}
```

**After:**
```java
LibraryLoadInfo info = NativeLoader.loadLibrary("mylib");
if (!info.isSuccessful()) {
    // Handle error with detailed info
    throw new RuntimeException(info.getErrorMessage());
}
```

### From JNA NativeLibrary.addSearchPath()

**Before:**
```java
NativeLibrary.addSearchPath("mylib", "/path/to/libs");
NativeLibrary library = NativeLibrary.getInstance("mylib");
```

**After:**
```java
LibraryLoadInfo info = NativeLoader.builder()
    .libraryName("mylib")
    .pathConvention(PathConvention.MAVEN_NATIVE)
    .load();
```

### From Custom Loading Code

**Before:**
```java
// Custom platform detection and extraction
String platform = detectPlatform();
InputStream stream = getClass().getResourceAsStream("/native/" + platform + "/libmylib.so");
// Complex extraction and loading logic...
```

**After:**
```java
// Simple, robust loading
LibraryLoadInfo info = NativeLoader.loadLibrary("mylib");
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:

- Setting up the development environment
- Running tests and benchmarks
- Code style and quality requirements
- Submitting pull requests

### Development Commands

```bash
# Build and test
./mvnw clean test

# Run benchmarks
./mvnw test -Dtest="*Benchmark*"

# Check code quality
./mvnw checkstyle:check spotbugs:check

# Generate documentation
./mvnw javadoc:javadoc
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/tegmentum/wasmtime4j/issues)
- **Documentation**: [API JavaDoc](https://javadoc.io/doc/ai.tegmentum/wasmtime4j-native-loader)
- **Examples**: [Example Projects](examples/)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and migration notes.
