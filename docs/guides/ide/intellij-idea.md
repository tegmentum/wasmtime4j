# IntelliJ IDEA Integration Guide

This guide provides comprehensive setup instructions for developing with wasmtime4j in IntelliJ IDEA.

## Quick Setup

### 1. Import Existing Project

```bash
# Clone the repository
git clone https://github.com/tegmentum/wasmtime4j.git
cd wasmtime4j

# Open in IntelliJ IDEA
idea .
```

Or use IntelliJ IDEA's File → Open and select the `wasmtime4j` directory.

### 2. Project SDK Configuration

1. **File → Project Structure → Project**
   - Set Project SDK to Java 8 (minimum) or higher
   - Set Project language level to "8 - Lambdas, type annotations etc."

2. **File → Project Structure → Modules**
   - IntelliJ should auto-detect Maven modules
   - Verify all modules are imported:
     - `wasmtime4j` (main API)
     - `wasmtime4j-jni` (JNI implementation)
     - `wasmtime4j-panama` (Panama implementation)
     - `wasmtime4j-native` (native library)
     - `wasmtime4j-tests` (integration tests)
     - `wasmtime4j-benchmarks` (performance tests)

### 3. Maven Integration

IntelliJ IDEA automatically detects Maven projects. Verify Maven integration:

1. **View → Tool Windows → Maven**
2. **Reload All Maven Projects** (refresh icon)
3. Verify all dependencies are resolved

## Development Configuration

### Build Configuration

#### Maven Runner Settings
1. **File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner**
2. Configure:
   - **VM Options**: `-Xmx2g -XX:+UseG1GC`
   - **JRE**: Use Project JDK
   - **Skip Tests**: Uncheck for full builds

#### Compiler Settings
1. **File → Settings → Build, Execution, Deployment → Compiler → Java Compiler**
2. Configure:
   - **Project bytecode version**: 8
   - **Use '--release' option for cross-compilation**: Check
   - **Additional command line parameters**: `--enable-preview` (for Panama features on Java 23+)

### Run Configurations

#### Unit Tests Configuration

Create a new JUnit configuration for running tests:

1. **Run → Edit Configurations**
2. **Add New → JUnit**
3. Configure:
   - **Name**: `Wasmtime4j Unit Tests`
   - **Test kind**: All in package
   - **Package**: `ai.tegmentum.wasmtime4j`
   - **Search for tests**: In whole project
   - **VM Options**:
     ```
     -Xmx1g
     -Djava.library.path=target/natives
     -Dwasmtime4j.runtime=auto
     ```

#### Integration Tests Configuration

1. **Run → Edit Configurations**
2. **Add New → JUnit**
3. Configure:
   - **Name**: `Wasmtime4j Integration Tests`
   - **Test kind**: All in package
   - **Package**: `ai.tegmentum.wasmtime4j`
   - **Search for tests**: In whole project
   - **VM Options**:
     ```
     -Xmx2g
     -Djava.library.path=target/natives
     - integration-tests
     ```

#### Benchmark Configuration

1. **Run → Edit Configurations**
2. **Add New → Application**
3. Configure:
   - **Name**: `JMH Benchmarks`
   - **Main class**: `org.openjdk.jmh.Main`
   - **Program arguments**: `.*` (run all benchmarks)
   - **VM Options**:
     ```
     -Xmx4g
     -XX:+UseG1GC
     -Djava.library.path=target/natives
     ```
   - **Working directory**: `wasmtime4j-benchmarks`

### Debugging Configuration

#### Debug Unit Tests

1. **Run → Edit Configurations**
2. **Add New → JUnit**
3. Configure:
   - **Name**: `Debug Wasmtime4j Tests`
   - **Test kind**: Class or Method
   - **VM Options**:
     ```
     -Xmx1g
     -Djava.library.path=target/natives
     -Dwasmtime4j.runtime=jni
     -Dwasmtime4j.debug=true
     -Djava.util.logging.level=FINE
     -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
     ```

#### Native Code Debugging

For debugging native code interactions:

1. Install **Native Debugging Support** plugin
2. Configure C/C++ debugging:
   - **Run → Edit Configurations**
   - **Add New → C/C++ Application**
   - **Target**: `target/natives/libwasmtime4j_native.so`
   - **Debug symbols**: Enable

### Code Style and Quality

#### Code Style Configuration

1. **File → Settings → Editor → Code Style → Java**
2. **Import Scheme**:
   - Download: [Google Java Style Guide](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml)
   - Import: **Settings → Editor → Code Style → Java → Scheme → Import Scheme**

#### Static Analysis Integration

##### Checkstyle Integration

1. Install **Checkstyle-IDEA** plugin
2. **File → Settings → Tools → Checkstyle**
3. Configure:
   - **Checkstyle version**: 10.12.4
   - **Configuration file**: `checkstyle.xml` (in project root)
   - **Suppress warnings**: `checkstyle-xpath-suppressions.xml`

##### SpotBugs Integration

1. Install **SpotBugs** plugin
2. **File → Settings → Tools → SpotBugs**
3. Configure:
   - **Exclude filter**: `spotbugs-exclude.xml`
   - **Effort**: Max
   - **Min rank to report**: 20

##### PMD Integration

1. Install **PMDPlugin** plugin
2. **File → Settings → Tools → PMD**
3. Configure:
   - **PMD installation**: Download latest
   - **Ruleset**: `pmd-ruleset.xml`

### File Templates

#### WebAssembly Test Template

Create a file template for WebAssembly tests:

1. **File → Settings → Editor → File and Code Templates**
2. **Create New Template**:

```java
#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import ai.tegmentum.wasmtime4j.*;

/**
 * Test class for ${NAME}.
 *
 * @author ${USER}
 * @since ${DATE}
 */
public class ${NAME} {

    private Engine engine;
    private Store store;

    @BeforeEach
    void setUp() {
        engine = Engine.newBuilder().build();
        store = Store.newBuilder(engine).build();
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.close();
        }
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    void testName() {
        // TODO: Implement test
        fail("Test not implemented");
    }
}
```

### Live Templates

Create useful live templates for common WebAssembly patterns:

1. **File → Settings → Editor → Live Templates**
2. Create new template group: "Wasmtime4j"

#### Common Templates

**wasm-test** - Basic WebAssembly test structure:
```java
@Test
void test$NAME$() {
    byte[] wasmBytes = loadWasmFile("$FILE$");
    Module module = Module.fromBinary(engine, wasmBytes);
    Instance instance = Instance.newBuilder(store, module).build();

    $END$

    instance.close();
    module.close();
}
```

**wasm-func** - Function call pattern:
```java
Function func = instance.getFunction("$FUNCTION_NAME$");
Object[] results = func.call($PARAMS$);
assertEquals($EXPECTED$, results[0]);
```

**wasm-memory** - Memory access pattern:
```java
Memory memory = instance.getMemory("memory");
ByteBuffer buffer = memory.buffer();
$END$
```

## Project Setup from Scratch

### Creating a New Project

#### Using Maven Archetype

```bash
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=my-wasmtime-app \
  -DarchetypeGroupId=ai.tegmentum \
  -DarchetypeArtifactId=wasmtime4j-archetype \
  -DinteractiveMode=false
```

#### Manual Setup

1. **Create new Maven project in IntelliJ**:
   - **File → New → Project**
   - **Maven**
   - **Project SDK**: Java 8+
   - **Create from archetype**: Uncheck

2. **Configure `pom.xml`**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-wasmtime-app</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.4</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

3. **Create sample code**:

```java
package com.example;

import ai.tegmentum.wasmtime4j.*;

public class WasmExample {
    public static void main(String[] args) {
        try (Engine engine = Engine.newBuilder().build();
             Store store = Store.newBuilder(engine).build()) {

            // Load and instantiate WebAssembly module
            byte[] wasmBytes = loadWasmFile("example.wasm");
            Module module = Module.fromBinary(engine, wasmBytes);
            Instance instance = Instance.newBuilder(store, module).build();

            // Call WebAssembly function
            Function addFunction = instance.getFunction("add");
            Object[] results = addFunction.call(42, 13);

            System.out.println("Result: " + results[0]);

            instance.close();
            module.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] loadWasmFile(String filename) {
        // Implementation to load WASM file
        return new byte[0];
    }
}
```

## Troubleshooting

### Common Issues

#### Native Library Loading Problems

**Problem**: `UnsatisfiedLinkError` when running tests
**Solution**:
1. Verify VM options include: `-Djava.library.path=target/natives`
2. Run Maven compile phase: `mvn compile`
3. Check native library extraction in `target/natives/`

#### Memory Issues

**Problem**: Out of memory errors during builds
**Solution**:
1. Increase heap size: `-Xmx4g`
2. Enable G1GC: `-XX:+UseG1GC`
3. Configure IntelliJ memory: **Help → Change Memory Settings**

#### Test Execution Problems

**Problem**: Tests not finding WebAssembly files
**Solution**:
1. Verify working directory in run configuration
2. Check resource path in test configuration
3. Ensure test resources are in `src/test/resources/`

### Performance Optimization

#### Build Performance

1. **Enable parallel builds**:
   - **File → Settings → Build, Execution, Deployment → Compiler**
   - **Build process heap size**: 2048 MB
   - **Compile independent modules in parallel**: Check

2. **Configure Maven import**:
   - **File → Settings → Build, Execution, Deployment → Build Tools → Maven → Importing**
   - **Import Maven projects automatically**: Check
   - **Download sources**: Uncheck (for faster imports)
   - **Download documentation**: Uncheck

#### IDE Performance

1. **Disable unnecessary plugins**
2. **Increase IDE memory**: **Help → Change Memory Settings** (4GB+)
3. **Enable power save mode** when not actively developing

## Integration with Build Tools

### Maven Integration

IntelliJ IDEA provides excellent Maven integration out of the box:

1. **Auto-import**: Automatically detects `pom.xml` changes
2. **Dependency management**: Downloads and manages dependencies
3. **Build lifecycle**: Execute Maven goals directly from IDE
4. **Multi-module support**: Handles complex project structures

### Native Development

For developers working on native code:

1. **CLion integration**: Use JetBrains CLion for Rust development
2. **Shared indexing**: Enable shared indexes for faster navigation
3. **Cross-language debugging**: Debug Java and native code together

## Additional Resources

- [IntelliJ IDEA Documentation](https://www.jetbrains.com/help/idea/)
- [Maven Integration Guide](https://www.jetbrains.com/help/idea/maven-support.html)
- [Debugging Java Applications](https://www.jetbrains.com/help/idea/debugging-code.html)
- [Code Style and Formatting](https://www.jetbrains.com/help/idea/code-style.html)

## Version Compatibility

| IntelliJ IDEA Version | Wasmtime4j Version | Java Version | Notes |
|----------------------|-------------------|--------------|-------|
| 2023.3+              | 1.0.0+            | 8-23         | Full support |
| 2023.1-2023.2        | 1.0.0+            | 8-21         | Limited Panama support |
| 2022.3+              | 1.0.0+            | 8-19         | JNI only |

For the best development experience, use IntelliJ IDEA 2023.3 or later with Java 23+ for full Panama FFI support.