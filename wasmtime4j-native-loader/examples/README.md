# Usage Examples

This directory contains practical examples demonstrating how to use the Wasmtime4j Native Library Loader in various scenarios.

## Structure

### Basic Usage (`basic-usage/`)

Simple examples showing fundamental usage patterns:

- **`SimpleLoading.java`** - Basic library loading with default settings
- Demonstrates error handling and result inspection
- Shows platform detection and diagnostic information

### Advanced Usage (`advanced-usage/`)

Examples of advanced configuration and specialized scenarios:

- **`CustomConfiguration.java`** - Advanced builder API usage
- Security level configuration (STRICT, MODERATE, PERMISSIVE)
- Custom path conventions and patterns
- Fallback strategies with multiple conventions
- Comprehensive error handling and diagnostics

### Framework Integration (`framework-integration/`)

Real-world integration patterns with popular frameworks:

- **`SpringBootIntegration.java`** - Spring Boot application integration
- Component initialization with `@PostConstruct`
- Configuration-based loading with `@Value` properties
- Health check integration with Spring Boot Actuator
- Production-ready error handling and logging

## Running the Examples

### Prerequisites

1. Add the wasmtime4j-native-loader dependency to your project:

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j-native-loader</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. Ensure your native libraries are packaged correctly in your JAR according to the path convention you're using.

### Compilation

The examples use only public APIs from the native loader library. Compile them with:

```bash
javac -cp wasmtime4j-native-loader-1.0.0.jar examples/**/*.java
```

### Execution

Run any example with:

```bash
java -cp .:wasmtime4j-native-loader-1.0.0.jar examples.basicusage.SimpleLoading
```

## Example Library Structure

For these examples to work with actual native libraries, your JAR should contain native libraries following one of these conventions:

### WASMTIME4J Convention (Default)
```
/native/
├── linux-x86_64/
│   └── libmylib.so
├── linux-aarch64/
│   └── libmylib.so
├── windows-x86_64/
│   └── mylib.dll
├── windows-aarch64/
│   └── mylib.dll
├── macos-x86_64/
│   └── libmylib.dylib
└── macos-aarch64/
    └── libmylib.dylib
```

### Maven Native Convention
```
/META-INF/native/
├── linux-x86_64/
│   └── libmylib.so
├── windows-x86_64/
│   └── mylib.dll
└── macos-x86_64/
    └── libmylib.dylib
```

### Gradle Native Convention
```
/native-libs/
├── linux-x86_64/
│   └── libmylib.so
├── windows-x86_64/
│   └── mylib.dll
└── macos-x86_64/
    └── libmylib.dylib
```

## Key Concepts Demonstrated

### Security Levels

- **STRICT**: Maximum validation, enhanced path protection, conservative permissions
- **MODERATE**: Balanced security and compatibility (recommended default)
- **PERMISSIVE**: Minimal restrictions for maximum compatibility

### Path Conventions

- **Built-in conventions** for popular build tools (Maven, Gradle)
- **Custom path patterns** with placeholder substitution
- **Fallback strategies** when multiple conventions might be used

### Error Handling

- **Comprehensive diagnostics** with attempted paths and detailed error messages
- **Graceful degradation** for optional libraries
- **Fail-fast behavior** for critical components

### Platform Detection

- **Automatic detection** of OS and architecture
- **Cross-platform compatibility** across Linux, Windows, and macOS
- **Architecture support** for both x86_64 and ARM64/aarch64

## Best Practices Shown

1. **Always check loading results** - Never assume libraries load successfully
2. **Use appropriate security levels** - STRICT for production, MODERATE for development
3. **Provide meaningful error messages** - Include diagnostic information in failures
4. **Use fallback strategies** - Multiple path conventions for broader compatibility
5. **Log important events** - Library loading success/failure affects application behavior
6. **Handle optional vs. critical libraries differently** - Fail fast for critical, graceful degradation for optional

## Troubleshooting

If examples fail to run:

1. **Verify library paths** - Check that your JAR contains libraries at expected locations
2. **Check platform detection** - Ensure your platform is supported
3. **Enable debug logging** - Add `-Djava.util.logging.level=FINE` for detailed output
4. **Try different security levels** - Start with PERMISSIVE for compatibility testing
5. **Review attempted paths** - The `LibraryLoadInfo` shows exactly what was tried

## Integration Patterns

The examples demonstrate several integration patterns:

- **Static initialization** - Load libraries during class loading
- **Component initialization** - Load libraries when components are created (Spring)
- **Configuration-driven loading** - Use external configuration to control behavior
- **Health checks** - Monitor native library status in production
- **Graceful fallback** - Handle optional native acceleration libraries