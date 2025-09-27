# Basic Maven Example

This example demonstrates the minimal setup required to use wasmtime4j in a Maven project.

## Project Structure

```
basic-example/
├── pom.xml                           # Maven configuration
├── src/main/java/com/example/
│   └── BasicExample.java            # Main example application
├── src/test/java/com/example/
│   └── BasicExampleTest.java        # Unit tests
└── README.md                        # This file
```

## Features Demonstrated

- **Basic WebAssembly module loading**: Compile and instantiate WASM modules
- **Function calls**: Call exported WebAssembly functions from Java
- **Memory access**: Read and write WebAssembly linear memory
- **Resource management**: Proper cleanup of native resources
- **Error handling**: Handling common error scenarios
- **Testing**: Comprehensive unit tests for WASM functionality

## Requirements

- **Java**: 8+ (JNI implementation) or 23+ (Panama implementation)
- **Maven**: 3.6.0+

## Building and Running

### Build the project

```bash
mvn clean compile
```

### Run tests

```bash
mvn test
```

### Run the example

```bash
mvn exec:java
```

Or manually:

```bash
mvn compile
java -cp target/classes:target/dependency/* com.example.BasicExample
```

### Run with specific runtime

**Force JNI runtime:**
```bash
mvn test -Pjni
```

**Force Panama runtime (Java 23+):**
```bash
mvn test -Ppanama
```

### Debug mode

```bash
mvn test -Pdebug
```

This enables detailed logging to help troubleshoot issues.

## Example Output

When you run the example, you should see output like:

```
INFO  - Starting Wasmtime4j Basic Example
INFO  - === Basic Arithmetic Example ===
INFO  - Module compiled successfully
INFO  - Module instantiated successfully
INFO  - add(42, 13) = 55
INFO  - === Memory Access Example ===
INFO  - Memory size: 65536 bytes
INFO  - Memory[0] = 0x42, Memory[1] = 0x13
INFO  - === Multiple Functions Example ===
INFO  - add(10, 5) = 15
INFO  - subtract(10, 5) = 5
INFO  - multiply(10, 5) = 50
INFO  - All examples completed successfully
```

## Key Code Patterns

### Resource Management

Always use try-with-resources for proper cleanup:

```java
try (Engine engine = Engine.newBuilder().build();
     Store store = Store.newBuilder(engine).build()) {

    // Use engine and store

} // Automatic cleanup
```

### Function Calls

```java
Function addFunction = instance.getFunction("add");
Object[] results = addFunction.call(42, 13);
int result = (Integer) results[0];
```

### Memory Access

```java
Memory memory = instance.getMemory("memory");
ByteBuffer buffer = memory.buffer();
buffer.put(0, (byte) 0x42);
byte value = buffer.get(0);
```

## Configuration Options

The `pom.xml` includes several configuration options:

### System Properties

- `wasmtime4j.runtime`: `auto`, `jni`, or `panama`
- `wasmtime4j.debug`: `true` or `false`
- `java.util.logging.level`: Logging level

### Maven Profiles

- `jni`: Force JNI runtime
- `panama`: Force Panama runtime (Java 23+)
- `debug`: Enable debug logging

## Troubleshooting

### Native Library Issues

If you see `UnsatisfiedLinkError`:

1. Check that the native library was extracted:
   ```bash
   ls target/dependency/natives/
   ```

2. Run with debug logging:
   ```bash
   mvn test -Pdebug
   ```

### Java Version Issues

For Panama features on Java 23+:

```bash
export JAVA_HOME=/path/to/java-23
mvn clean test -Ppanama
```

### Memory Issues

For large WASM modules, increase heap size:

```bash
export MAVEN_OPTS="-Xmx2g"
mvn test
```

## Next Steps

- See [`spring-boot-example/`](../spring-boot-example/) for web application integration
- See [`multi-module-example/`](../multi-module-example/) for complex project structures
- See [`testing-example/`](../testing-example/) for advanced testing patterns
- Review the [troubleshooting guide](../../docs/guides/troubleshooting.md) for common issues