package examples;

import ai.tegmentum.wasmtime4j.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Interactive WebAssembly calculator demonstrating:
 * - Module loading and compilation
 * - Function calling with different parameter types
 * - Error handling and user interaction
 * - Memory management best practices
 */
public class WebAssemblyCalculator {

    private final WasmRuntime runtime;
    private final Engine engine;
    private final Module calculatorModule;

    public WebAssemblyCalculator() throws Exception {
        // Initialize runtime with auto-detection
        this.runtime = WasmRuntime.builder()
            .enableMetrics(true)
            .build();

        // Create optimized engine for calculator operations
        EngineConfig config = EngineConfig.builder()
            .optimizationLevel(OptimizationLevel.SPEED)
            .enableInstancePooling(true)
            .maxPooledInstances(10)
            .build();

        this.engine = runtime.createEngine(config);

        // Load calculator WebAssembly module
        byte[] wasmBytes = loadCalculatorModule();
        this.calculatorModule = Module.fromBytes(engine, wasmBytes);

        System.out.println("WebAssembly Calculator initialized");
        System.out.println("Runtime: " + runtime.getRuntimeType());
        System.out.println("Engine optimizations: " + config.getOptimizationLevel());
    }

    public void runInteractiveCalculator() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== Interactive WebAssembly Calculator ===");
        System.out.println("Available operations: add, subtract, multiply, divide, power, factorial");
        System.out.println("Type 'quit' to exit\n");

        while (true) {
            try {
                System.out.print("calc> ");
                String input = scanner.nextLine().trim();

                if ("quit".equalsIgnoreCase(input)) {
                    break;
                }

                processCommand(input);

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("Calculator closed.");
    }

    private void processCommand(String input) throws WasmException {
        String[] parts = input.split("\\s+");
        if (parts.length < 2) {
            System.out.println("Usage: <operation> <numbers...>");
            return;
        }

        String operation = parts[0].toLowerCase();

        try (Store store = engine.createStore()) {
            Instance instance = Instance.create(store, calculatorModule);

            switch (operation) {
                case "add":
                    if (parts.length >= 3) {
                        double a = Double.parseDouble(parts[1]);
                        double b = Double.parseDouble(parts[2]);
                        double result = callBinaryOperation(instance, "add_f64", a, b);
                        System.out.println(a + " + " + b + " = " + result);
                    } else {
                        System.out.println("Usage: add <number1> <number2>");
                    }
                    break;

                case "subtract":
                    if (parts.length >= 3) {
                        double a = Double.parseDouble(parts[1]);
                        double b = Double.parseDouble(parts[2]);
                        double result = callBinaryOperation(instance, "subtract_f64", a, b);
                        System.out.println(a + " - " + b + " = " + result);
                    } else {
                        System.out.println("Usage: subtract <number1> <number2>");
                    }
                    break;

                case "multiply":
                    if (parts.length >= 3) {
                        double a = Double.parseDouble(parts[1]);
                        double b = Double.parseDouble(parts[2]);
                        double result = callBinaryOperation(instance, "multiply_f64", a, b);
                        System.out.println(a + " * " + b + " = " + result);
                    } else {
                        System.out.println("Usage: multiply <number1> <number2>");
                    }
                    break;

                case "divide":
                    if (parts.length >= 3) {
                        double a = Double.parseDouble(parts[1]);
                        double b = Double.parseDouble(parts[2]);
                        if (b == 0) {
                            System.out.println("Error: Division by zero");
                        } else {
                            double result = callBinaryOperation(instance, "divide_f64", a, b);
                            System.out.println(a + " / " + b + " = " + result);
                        }
                    } else {
                        System.out.println("Usage: divide <number1> <number2>");
                    }
                    break;

                case "power":
                    if (parts.length >= 3) {
                        double base = Double.parseDouble(parts[1]);
                        double exponent = Double.parseDouble(parts[2]);
                        double result = callBinaryOperation(instance, "power_f64", base, exponent);
                        System.out.println(base + " ^ " + exponent + " = " + result);
                    } else {
                        System.out.println("Usage: power <base> <exponent>");
                    }
                    break;

                case "factorial":
                    if (parts.length >= 2) {
                        int n = Integer.parseInt(parts[1]);
                        if (n < 0) {
                            System.out.println("Error: Factorial of negative number");
                        } else if (n > 20) {
                            System.out.println("Error: Number too large (max 20)");
                        } else {
                            long result = callUnaryIntOperation(instance, "factorial", n);
                            System.out.println(n + "! = " + result);
                        }
                    } else {
                        System.out.println("Usage: factorial <number>");
                    }
                    break;

                default:
                    System.out.println("Unknown operation: " + operation);
                    System.out.println("Available: add, subtract, multiply, divide, power, factorial");
            }
        }
    }

    private double callBinaryOperation(Instance instance, String functionName, double a, double b) throws WasmException {
        Function function = instance.getFunction(functionName);
        if (function == null) {
            throw new WasmException("Function not found: " + functionName);
        }

        Value[] params = {Value.f64(a), Value.f64(b)};
        Value[] results = function.call(params);

        return results[0].asF64();
    }

    private long callUnaryIntOperation(Instance instance, String functionName, int input) throws WasmException {
        Function function = instance.getFunction(functionName);
        if (function == null) {
            throw new WasmException("Function not found: " + functionName);
        }

        Value[] params = {Value.i32(input)};
        Value[] results = function.call(params);

        return results[0].asI64();
    }

    private byte[] loadCalculatorModule() throws Exception {
        // In a real application, this would load from a resource or file
        // For this example, we'll create a simple calculator module in WAT format
        String watSource = """
            (module
              (func $add_f64 (param $a f64) (param $b f64) (result f64)
                local.get $a
                local.get $b
                f64.add)

              (func $subtract_f64 (param $a f64) (param $b f64) (result f64)
                local.get $a
                local.get $b
                f64.sub)

              (func $multiply_f64 (param $a f64) (param $b f64) (result f64)
                local.get $a
                local.get $b
                f64.mul)

              (func $divide_f64 (param $a f64) (param $b f64) (result f64)
                local.get $a
                local.get $b
                f64.div)

              (func $power_f64 (param $base f64) (param $exp f64) (result f64)
                ;; Simple power implementation using loop
                (local $result f64)
                (local $counter f64)

                f64.const 1.0
                local.set $result
                f64.const 0.0
                local.set $counter

                (block $exit
                  (loop $loop
                    local.get $counter
                    local.get $exp
                    f64.ge
                    br_if $exit

                    local.get $result
                    local.get $base
                    f64.mul
                    local.set $result

                    local.get $counter
                    f64.const 1.0
                    f64.add
                    local.set $counter

                    br $loop))

                local.get $result)

              (func $factorial (param $n i32) (result i64)
                (local $result i64)
                (local $counter i32)

                i64.const 1
                local.set $result
                i32.const 1
                local.set $counter

                (block $exit
                  (loop $loop
                    local.get $counter
                    local.get $n
                    i32.gt_s
                    br_if $exit

                    local.get $result
                    local.get $counter
                    i64.extend_i32_s
                    i64.mul
                    local.set $result

                    local.get $counter
                    i32.const 1
                    i32.add
                    local.set $counter

                    br $loop))

                local.get $result)

              (export "add_f64" (func $add_f64))
              (export "subtract_f64" (func $subtract_f64))
              (export "multiply_f64" (func $multiply_f64))
              (export "divide_f64" (func $divide_f64))
              (export "power_f64" (func $power_f64))
              (export "factorial" (func $factorial))
            )
            """;

        // For demonstration, we'll return pre-compiled bytes
        // In practice, you would use wat2wasm or similar tool
        return compileWatToWasm(watSource);
    }

    private byte[] compileWatToWasm(String watSource) {
        // This is a placeholder - in a real implementation you would:
        // 1. Use an external wat2wasm compiler
        // 2. Load pre-compiled .wasm files
        // 3. Use a Java-based WebAssembly compiler

        // For this example, return a minimal working WASM module
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // WASM magic number
            0x01, 0x00, 0x00, 0x00  // WASM version
            // ... (rest would be actual compiled bytecode)
        };
    }

    public void showStatistics() throws WasmException {
        if (runtime.supportsMetrics()) {
            RuntimeMetrics metrics = runtime.getMetrics();
            System.out.println("\n=== Runtime Statistics ===");
            System.out.println("Total compilations: " + metrics.getTotalCompilations());
            System.out.println("Total instantiations: " + metrics.getTotalInstantiations());
            System.out.println("Cache hits: " + metrics.getCacheHits());
            System.out.println("Memory usage: " + metrics.getMemoryUsage() + " bytes");
        }
    }

    public void cleanup() throws Exception {
        if (engine != null) {
            engine.close();
        }
        if (runtime != null) {
            runtime.close();
        }
    }

    public static void main(String[] args) {
        WebAssemblyCalculator calculator = null;

        try {
            calculator = new WebAssemblyCalculator();

            // Run interactive calculator
            calculator.runInteractiveCalculator();

            // Show statistics
            calculator.showStatistics();

        } catch (Exception e) {
            System.err.println("Calculator failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (calculator != null) {
                try {
                    calculator.cleanup();
                } catch (Exception e) {
                    System.err.println("Cleanup failed: " + e.getMessage());
                }
            }
        }
    }
}