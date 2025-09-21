# Basic Calculator Example

This example demonstrates the fundamental concepts of Wasmtime4j by implementing a simple calculator that runs WebAssembly functions from Java.

## Overview

The example includes:
- A WebAssembly module with basic arithmetic functions (add, subtract, multiply, divide)
- A Java application that loads and executes the WebAssembly module
- Error handling for division by zero and invalid operations
- Both JNI and Panama FFI runtime usage examples

## Files Structure

```
basic-calculator/
├── README.md
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ai/tegmentum/wasmtime4j/examples/calculator/
│   │   │       ├── Calculator.java
│   │   │       ├── CalculatorService.java
│   │   │       └── Main.java
│   │   └── resources/
│   │       └── calculator.wasm
│   └── test/
│       └── java/
│           └── ai/tegmentum/wasmtime4j/examples/calculator/
│               └── CalculatorTest.java
├── calculator.wat
└── build.sh
```

## WebAssembly Module

The calculator module (`calculator.wat`) provides four basic operations:

```wat
(module
  ;; Add two 32-bit integers
  (func $add (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.add)
  (export "add" (func $add))

  ;; Subtract two 32-bit integers
  (func $subtract (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.sub)
  (export "subtract" (func $subtract))

  ;; Multiply two 32-bit integers
  (func $multiply (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.mul)
  (export "multiply" (func $multiply))

  ;; Divide two 32-bit integers (throws trap on division by zero)
  (func $divide (param $lhs i32) (param $rhs i32) (result i32)
    local.get $lhs
    local.get $rhs
    i32.div_s)
  (export "divide" (func $divide))

  ;; Advanced: Power function using loop
  (func $power (param $base i32) (param $exponent i32) (result i32)
    (local $result i32)
    (local $counter i32)

    i32.const 1
    local.set $result
    i32.const 0
    local.set $counter

    (loop $power_loop
      local.get $counter
      local.get $exponent
      i32.lt_s
      (if
        (then
          local.get $result
          local.get $base
          i32.mul
          local.set $result

          local.get $counter
          i32.const 1
          i32.add
          local.set $counter

          br $power_loop
        )
      )
    )

    local.get $result)
  (export "power" (func $power))
)
```

## Java Implementation

### Calculator Interface

```java
package ai.tegmentum.wasmtime4j.examples.calculator;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Calculator interface for WebAssembly-based arithmetic operations.
 */
public interface Calculator extends AutoCloseable {

    /**
     * Adds two integers.
     */
    int add(int a, int b) throws WasmException;

    /**
     * Subtracts two integers.
     */
    int subtract(int a, int b) throws WasmException;

    /**
     * Multiplies two integers.
     */
    int multiply(int a, int b) throws WasmException;

    /**
     * Divides two integers.
     * @throws WasmException if division by zero
     */
    int divide(int a, int b) throws WasmException;

    /**
     * Calculates power (base^exponent).
     */
    int power(int base, int exponent) throws WasmException;
}
```

### Calculator Implementation

```java
package ai.tegmentum.wasmtime4j.examples.calculator;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * WebAssembly-based calculator implementation.
 */
public class CalculatorService implements Calculator {

    private static final Logger LOGGER = Logger.getLogger(CalculatorService.class.getName());

    private final WasmRuntime runtime;
    private final Engine engine;
    private final Store store;
    private final Instance instance;

    public CalculatorService() throws WasmException, IOException {
        this(RuntimeType.AUTO);
    }

    public CalculatorService(RuntimeType runtimeType) throws WasmException, IOException {
        LOGGER.info("Initializing Calculator with runtime type: " + runtimeType);

        // Create runtime
        this.runtime = (runtimeType == RuntimeType.AUTO)
            ? WasmRuntimeFactory.create()
            : WasmRuntimeFactory.create(runtimeType);

        try {
            // Create optimized engine
            EngineConfig config = EngineConfig.builder()
                .optimizationLevel(OptimizationLevel.SPEED)
                .debugInfo(false)
                .build();

            this.engine = runtime.createEngine(config);

            // Load and compile WebAssembly module
            byte[] wasmBytes = loadCalculatorModule();
            Module module = engine.compile(wasmBytes);

            // Create store and instantiate module
            this.store = engine.createStore();
            this.instance = module.instantiate(store, new ImportMap());

            LOGGER.info("Calculator initialized successfully");

        } catch (Exception e) {
            // Clean up on initialization failure
            cleanup();
            throw new WasmException("Failed to initialize calculator", e);
        }
    }

    private byte[] loadCalculatorModule() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/calculator.wasm")) {
            if (stream == null) {
                throw new IOException("Calculator WebAssembly module not found in resources");
            }
            return stream.readAllBytes();
        }
    }

    @Override
    public int add(int a, int b) throws WasmException {
        return callBinaryOperation("add", a, b);
    }

    @Override
    public int subtract(int a, int b) throws WasmException {
        return callBinaryOperation("subtract", a, b);
    }

    @Override
    public int multiply(int a, int b) throws WasmException {
        return callBinaryOperation("multiply", a, b);
    }

    @Override
    public int divide(int a, int b) throws WasmException {
        if (b == 0) {
            throw new WasmException("Division by zero is not allowed");
        }
        return callBinaryOperation("divide", a, b);
    }

    @Override
    public int power(int base, int exponent) throws WasmException {
        if (exponent < 0) {
            throw new WasmException("Negative exponents are not supported");
        }
        return callBinaryOperation("power", base, exponent);
    }

    private int callBinaryOperation(String functionName, int a, int b) throws WasmException {
        try {
            WasmFunction function = instance.getFunction(functionName);
            WasmValue[] result = function.call(
                WasmValue.i32(a),
                WasmValue.i32(b)
            );

            if (result.length != 1) {
                throw new WasmException("Expected single result from " + functionName);
            }

            return result[0].asInt();

        } catch (WasmException e) {
            LOGGER.warning("Error calling " + functionName + "(" + a + ", " + b + "): " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void close() throws WasmException {
        cleanup();
    }

    private void cleanup() {
        Exception lastException = null;

        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                lastException = e;
                LOGGER.warning("Error closing store: " + e.getMessage());
            }
        }

        if (engine != null) {
            try {
                engine.close();
            } catch (Exception e) {
                lastException = e;
                LOGGER.warning("Error closing engine: " + e.getMessage());
            }
        }

        if (runtime != null) {
            try {
                runtime.close();
            } catch (Exception e) {
                lastException = e;
                LOGGER.warning("Error closing runtime: " + e.getMessage());
            }
        }

        if (lastException != null && lastException instanceof WasmException) {
            throw (WasmException) lastException;
        }
    }
}
```

### Main Application

```java
package ai.tegmentum.wasmtime4j.examples.calculator;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Interactive calculator application using WebAssembly.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        System.out.println("=== Wasmtime4j Calculator Example ===");
        System.out.println();

        // Determine runtime type from args or system property
        RuntimeType runtimeType = getRuntimeType(args);
        System.out.println("Using runtime: " + runtimeType);

        try (Calculator calculator = new CalculatorService(runtimeType)) {
            runInteractiveCalculator(calculator);
        } catch (WasmException | IOException e) {
            System.err.println("Error initializing calculator: " + e.getMessage());
            LOGGER.severe("Calculator initialization failed: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            LOGGER.severe("Unexpected error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static RuntimeType getRuntimeType(String[] args) {
        // Check command line arguments
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "jni":
                    return RuntimeType.JNI;
                case "panama":
                    return RuntimeType.PANAMA;
                case "auto":
                default:
                    return RuntimeType.AUTO;
            }
        }

        // Check system property
        String runtimeProperty = System.getProperty("wasmtime4j.runtime", "auto");
        switch (runtimeProperty.toLowerCase()) {
            case "jni":
                return RuntimeType.JNI;
            case "panama":
                return RuntimeType.PANAMA;
            case "auto":
            default:
                return RuntimeType.AUTO;
        }
    }

    private static void runInteractiveCalculator(Calculator calculator) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Interactive calculator ready!");
        System.out.println("Commands: add, subtract, multiply, divide, power, demo, quit");
        System.out.println();

        while (true) {
            System.out.print("calculator> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                break;
            }

            if (input.equalsIgnoreCase("demo")) {
                runDemo(calculator);
                continue;
            }

            if (input.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }

            if (input.isEmpty()) {
                continue;
            }

            try {
                processCommand(calculator, input);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Goodbye!");
    }

    private static void processCommand(Calculator calculator, String input) throws WasmException {
        String[] parts = input.split("\\s+");
        if (parts.length != 3) {
            System.err.println("Usage: <operation> <number1> <number2>");
            return;
        }

        String operation = parts[0].toLowerCase();
        int a = Integer.parseInt(parts[1]);
        int b = Integer.parseInt(parts[2]);

        int result;
        switch (operation) {
            case "add":
            case "+":
                result = calculator.add(a, b);
                System.out.println(a + " + " + b + " = " + result);
                break;
            case "subtract":
            case "-":
                result = calculator.subtract(a, b);
                System.out.println(a + " - " + b + " = " + result);
                break;
            case "multiply":
            case "*":
                result = calculator.multiply(a, b);
                System.out.println(a + " * " + b + " = " + result);
                break;
            case "divide":
            case "/":
                result = calculator.divide(a, b);
                System.out.println(a + " / " + b + " = " + result);
                break;
            case "power":
            case "^":
                result = calculator.power(a, b);
                System.out.println(a + " ^ " + b + " = " + result);
                break;
            default:
                System.err.println("Unknown operation: " + operation);
                printHelp();
        }
    }

    private static void runDemo(Calculator calculator) {
        System.out.println("Running calculator demo...");
        System.out.println();

        try {
            // Basic arithmetic
            System.out.println("=== Basic Arithmetic ===");
            System.out.println("10 + 5 = " + calculator.add(10, 5));
            System.out.println("10 - 5 = " + calculator.subtract(10, 5));
            System.out.println("10 * 5 = " + calculator.multiply(10, 5));
            System.out.println("10 / 5 = " + calculator.divide(10, 5));
            System.out.println();

            // Power operations
            System.out.println("=== Power Operations ===");
            System.out.println("2 ^ 3 = " + calculator.power(2, 3));
            System.out.println("5 ^ 2 = " + calculator.power(5, 2));
            System.out.println("10 ^ 0 = " + calculator.power(10, 0));
            System.out.println();

            // Edge cases
            System.out.println("=== Edge Cases ===");
            System.out.println("0 + 0 = " + calculator.add(0, 0));
            System.out.println("100 - 200 = " + calculator.subtract(100, 200));
            System.out.println("-5 * 3 = " + calculator.multiply(-5, 3));
            System.out.println();

            // Error handling demo
            System.out.println("=== Error Handling ===");
            try {
                calculator.divide(10, 0);
            } catch (WasmException e) {
                System.out.println("Division by zero correctly caught: " + e.getMessage());
            }

            try {
                calculator.power(2, -1);
            } catch (WasmException e) {
                System.out.println("Negative exponent correctly caught: " + e.getMessage());
            }

        } catch (WasmException e) {
            System.err.println("Demo error: " + e.getMessage());
        }

        System.out.println("Demo completed!");
        System.out.println();
    }

    private static void printHelp() {
        System.out.println("Available operations:");
        System.out.println("  add <a> <b>      - Add two numbers");
        System.out.println("  subtract <a> <b> - Subtract two numbers");
        System.out.println("  multiply <a> <b> - Multiply two numbers");
        System.out.println("  divide <a> <b>   - Divide two numbers");
        System.out.println("  power <a> <b>    - Calculate a^b");
        System.out.println("  demo             - Run demonstration");
        System.out.println("  help             - Show this help");
        System.out.println("  quit             - Exit calculator");
        System.out.println();
    }
}
```

## Project Configuration

### Maven POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>ai.tegmentum.wasmtime4j.examples</groupId>
    <artifactId>basic-calculator</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Wasmtime4j Basic Calculator Example</name>
    <description>Simple calculator example using Wasmtime4j</description>

    <properties>
        <maven.compiler.source>23</maven.compiler.source>
        <maven.compiler.target>23</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <wasmtime4j.version>1.0.0</wasmtime4j.version>
        <junit.version>5.10.0</junit.version>
    </properties>

    <dependencies>
        <!-- Wasmtime4j dependencies -->
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j</artifactId>
            <version>${wasmtime4j.version}</version>
        </dependency>
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j-jni</artifactId>
            <version>${wasmtime4j.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j-panama</artifactId>
            <version>${wasmtime4j.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>ai.tegmentum</groupId>
            <artifactId>wasmtime4j-native-loader</artifactId>
            <version>${wasmtime4j.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
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
                    <source>23</source>
                    <target>23</target>
                    <compilerArgs>
                        <arg>--enable-preview</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
                <configuration>
                    <argLine>--enable-preview --add-modules jdk.incubator.foreign</argLine>
                </configuration>
            </plugin>

            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>ai.tegmentum.wasmtime4j.examples.calculator.Main</mainClass>
                    <options>
                        <option>--enable-preview</option>
                        <option>--add-modules</option>
                        <option>jdk.incubator.foreign</option>
                    </options>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Build and Run

### Building WebAssembly Module

```bash
#!/bin/bash
# build.sh

# Compile WebAssembly text format to binary
wat2wasm calculator.wat -o src/main/resources/calculator.wasm

# Verify the binary
wasm-validate src/main/resources/calculator.wasm

echo "WebAssembly module built successfully!"
```

### Running the Example

```bash
# Build the project
mvn clean compile

# Run with automatic runtime selection
mvn exec:java

# Run with specific runtime
mvn exec:java -Dexec.args="jni"
mvn exec:java -Dexec.args="panama"

# Run tests
mvn test

# Package as executable JAR
mvn package
java --enable-preview --add-modules jdk.incubator.foreign \
     -jar target/basic-calculator-1.0.0.jar
```

## Testing

### Unit Tests

```java
package ai.tegmentum.wasmtime4j.examples.calculator;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    private Calculator calculator;

    @BeforeEach
    void setUp() throws WasmException, IOException {
        calculator = new CalculatorService(RuntimeType.AUTO);
    }

    @AfterEach
    void tearDown() throws WasmException {
        if (calculator != null) {
            calculator.close();
        }
    }

    @Test
    void testAddition() throws WasmException {
        assertEquals(30, calculator.add(10, 20));
        assertEquals(0, calculator.add(-5, 5));
        assertEquals(-10, calculator.add(-15, 5));
    }

    @Test
    void testSubtraction() throws WasmException {
        assertEquals(10, calculator.subtract(20, 10));
        assertEquals(-10, calculator.subtract(5, 15));
        assertEquals(0, calculator.subtract(5, 5));
    }

    @Test
    void testMultiplication() throws WasmException {
        assertEquals(200, calculator.multiply(10, 20));
        assertEquals(0, calculator.multiply(0, 100));
        assertEquals(-50, calculator.multiply(-5, 10));
    }

    @Test
    void testDivision() throws WasmException {
        assertEquals(5, calculator.divide(20, 4));
        assertEquals(-2, calculator.divide(-10, 5));
        assertEquals(0, calculator.divide(0, 5));
    }

    @Test
    void testDivisionByZero() {
        WasmException exception = assertThrows(WasmException.class,
            () -> calculator.divide(10, 0));
        assertTrue(exception.getMessage().contains("zero"));
    }

    @Test
    void testPower() throws WasmException {
        assertEquals(8, calculator.power(2, 3));
        assertEquals(25, calculator.power(5, 2));
        assertEquals(1, calculator.power(10, 0));
        assertEquals(1, calculator.power(1, 100));
    }

    @Test
    void testNegativeExponent() {
        WasmException exception = assertThrows(WasmException.class,
            () -> calculator.power(2, -1));
        assertTrue(exception.getMessage().contains("Negative exponents"));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1, 2",
        "0, 0, 0",
        "-1, 1, 0",
        "100, -50, 50",
        "2147483647, 1, -2147483648"  // Integer overflow test
    })
    void testAdditionParameterized(int a, int b, int expected) throws WasmException {
        assertEquals(expected, calculator.add(a, b));
    }

    @ParameterizedTest
    @EnumSource(RuntimeType.class)
    void testAllRuntimeTypes(RuntimeType runtimeType) throws Exception {
        // Skip if runtime type not available
        if (!isRuntimeAvailable(runtimeType)) {
            Assumptions.assumeTrue(false, "Runtime not available: " + runtimeType);
        }

        try (Calculator calc = new CalculatorService(runtimeType)) {
            assertEquals(15, calc.add(10, 5));
            assertEquals(50, calc.multiply(10, 5));
        }
    }

    private boolean isRuntimeAvailable(RuntimeType runtimeType) {
        try {
            new CalculatorService(runtimeType).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testResourceCleanup() throws Exception {
        Calculator calc = new CalculatorService();
        calc.add(1, 2);  // Use the calculator
        calc.close();    // Should not throw exception

        // Multiple closes should be safe
        assertDoesNotThrow(() -> calc.close());
    }
}
```

## Expected Output

When running the interactive calculator:

```
=== Wasmtime4j Calculator Example ===

Using runtime: PANAMA
Interactive calculator ready!
Commands: add, subtract, multiply, divide, power, demo, quit

calculator> demo
Running calculator demo...

=== Basic Arithmetic ===
10 + 5 = 15
10 - 5 = 5
10 * 5 = 50
10 / 5 = 2

=== Power Operations ===
2 ^ 3 = 8
5 ^ 2 = 25
10 ^ 0 = 1

=== Edge Cases ===
0 + 0 = 0
100 - 200 = -100
-5 * 3 = -15

=== Error Handling ===
Division by zero correctly caught: Division by zero is not allowed
Negative exponent correctly caught: Negative exponents are not supported

Demo completed!

calculator> add 15 25
15 + 25 = 40

calculator> power 3 4
3 ^ 4 = 81

calculator> quit
Goodbye!
```

## Key Learning Points

1. **Resource Management**: Proper use of try-with-resources and cleanup patterns
2. **Error Handling**: Comprehensive exception handling for WebAssembly operations
3. **Runtime Selection**: Supporting both JNI and Panama FFI implementations
4. **Performance**: Using optimized engine configuration for production use
5. **Testing**: Complete test coverage including parameterized and runtime-specific tests

This example provides a solid foundation for understanding Wasmtime4j concepts and can be extended to build more complex WebAssembly applications.