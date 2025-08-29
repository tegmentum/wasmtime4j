# Getting Started with Wasmtime4j

This guide will help you get up and running with Wasmtime4j in under 15 minutes. You'll learn how to install the library, load your first WebAssembly module, and execute WebAssembly functions from Java.

## Prerequisites

- **Java**: Java 8 or later (Java 23+ recommended for optimal performance with Panama FFI)
- **Maven** or **Gradle**: For dependency management
- **WebAssembly Module**: A `.wasm` file to test with (we'll provide a simple one)

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>ai.tegmentum</groupId>
    <artifactId>wasmtime4j</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
implementation 'ai.tegmentum:wasmtime4j:1.0.0-SNAPSHOT'
```

## Runtime Selection

Wasmtime4j automatically selects the best runtime implementation:
- **Java 23+**: Panama Foreign Function API (optimal performance)
- **Java 8-22**: JNI implementation (full compatibility)

You can manually override the selection:
```bash
# Force JNI runtime
java -Dwasmtime4j.runtime=jni YourApp

# Force Panama runtime (Java 23+ only)
java -Dwasmtime4j.runtime=panama YourApp
```

## Your First WebAssembly Module

Let's start with a simple "Hello, World!" example.

### Step 1: Create a Simple WebAssembly Module

Create a file called `add.wat` (WebAssembly Text format):

```wat
(module
  (func $add (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add)
  (export "add" (func $add)))
```

Convert to binary format using [wabt](https://github.com/WebAssembly/wabt):
```bash
wat2wasm add.wat -o add.wasm
```

Or use our provided test module at `wasmtime4j-tests/src/test/resources/wasm/custom-tests/add.wasm`.

### Step 2: Load and Execute the Module

```java
import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HelloWasm {
    public static void main(String[] args) {
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            // Create an engine for WebAssembly compilation and execution
            Engine engine = runtime.createEngine();
            
            // Load and compile the WebAssembly module
            byte[] wasmBytes = Files.readAllBytes(Paths.get("add.wasm"));
            Module module = runtime.compileModule(engine, wasmBytes);
            
            // Create an instance of the module
            Instance instance = runtime.instantiate(module);
            
            // Get the exported "add" function
            WasmFunction addFunction = instance.getFunction("add");
            
            // Call the function with arguments
            WasmValue[] args = {
                WasmValue.i32(5),
                WasmValue.i32(3)
            };
            
            WasmValue[] results = addFunction.call(args);
            
            // Print the result
            System.out.println("5 + 3 = " + results[0].asI32());
            
        } catch (WasmException | IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

### Step 3: Run Your Application

```bash
javac -cp "wasmtime4j-1.0.0-SNAPSHOT.jar" HelloWasm.java
java -cp ".:wasmtime4j-1.0.0-SNAPSHOT.jar" HelloWasm
```

Expected output:
```
5 + 3 = 8
```

## Working with Memory

WebAssembly modules often work with linear memory. Here's how to interact with it:

```java
try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
    Engine engine = runtime.createEngine();
    
    // Module with memory export
    byte[] wasmBytes = loadModuleWithMemory(); // Your module here
    Module module = runtime.compileModule(engine, wasmBytes);
    Instance instance = runtime.instantiate(module);
    
    // Get the memory export
    WasmMemory memory = instance.getMemory("memory");
    
    // Write data to memory
    byte[] data = "Hello, WebAssembly!".getBytes();
    memory.write(0, data);
    
    // Read data from memory
    byte[] result = memory.read(0, data.length);
    System.out.println("Read from memory: " + new String(result));
    
} catch (WasmException e) {
    System.err.println("Error: " + e.getMessage());
}
```

## Error Handling

Wasmtime4j provides specific exception types for different error conditions:

```java
try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
    // Your WebAssembly operations here
    
} catch (ValidationException e) {
    // Module validation failed
    System.err.println("Invalid WebAssembly module: " + e.getMessage());
} catch (CompilationException e) {
    // Module compilation failed
    System.err.println("Compilation error: " + e.getMessage());
} catch (InstantiationException e) {
    // Module instantiation failed
    System.err.println("Instantiation error: " + e.getMessage());
} catch (ai.tegmentum.wasmtime4j.exception.RuntimeException e) {
    // Runtime execution error
    System.err.println("Runtime error: " + e.getMessage());
} catch (WasmException e) {
    // General WebAssembly error
    System.err.println("WebAssembly error: " + e.getMessage());
}
```

## Runtime Information

You can get information about the selected runtime:

```java
try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
    RuntimeInfo info = runtime.getRuntimeInfo();
    
    System.out.println("Runtime Type: " + info.getRuntimeType());
    System.out.println("Version: " + info.getVersion());
    System.out.println("Implementation: " + info.getImplementation());
    System.out.println("Native Library: " + info.getNativeLibraryPath());
}
```

## Configuration Options

### Engine Configuration

You can customize the WebAssembly engine:

```java
EngineConfig config = EngineConfig.builder()
    .optimizationLevel(OptimizationLevel.SPEED)
    .enableDebugInfo(false)
    .enableProfiling(false)
    .build();

Engine engine = runtime.createEngine(config);
```

### System Properties

Control runtime behavior with system properties:
- `wasmtime4j.runtime`: Force specific runtime (`jni` or `panama`)
- `wasmtime4j.debug`: Enable debug logging
- `wasmtime4j.native.path`: Custom native library path

Example:
```bash
java -Dwasmtime4j.runtime=panama -Dwasmtime4j.debug=true YourApp
```

## Best Practices

1. **Resource Management**: Always use try-with-resources for automatic cleanup
2. **Runtime Selection**: Let the library auto-select unless you have specific requirements
3. **Error Handling**: Use specific exception types for better error handling
4. **Performance**: Reuse Engine and Module instances when possible
5. **Memory**: Monitor WebAssembly memory usage in long-running applications

## Next Steps

- Read the [Advanced Usage Guide](advanced-usage.md) for WASI integration and host functions
- Check out more [examples](../examples/) for your specific use case
- Review [performance optimization tips](performance.md)
- Learn about [security considerations](security.md)

## Common Issues

If you encounter problems:
1. Check the [troubleshooting guide](troubleshooting.md)
2. Verify your Java version compatibility
3. Ensure your WebAssembly module is valid
4. Check system properties and configuration

For more help, see our [troubleshooting guide](troubleshooting.md) or [file an issue](https://github.com/tegmentum/wasmtime4j/issues).