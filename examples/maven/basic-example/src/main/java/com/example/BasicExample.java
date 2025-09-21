package com.example;

import ai.tegmentum.wasmtime4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Basic example demonstrating wasmtime4j usage.
 *
 * This example shows how to:
 * - Load a WebAssembly module
 * - Create an engine and store
 * - Instantiate the module
 * - Call WebAssembly functions
 * - Handle resources properly
 */
public final class BasicExample {

    private static final Logger logger = LoggerFactory.getLogger(BasicExample.class);

    /**
     * Main entry point for the basic example.
     *
     * @param args command line arguments (not used)
     */
    public static void main(final String[] args) {
        logger.info("Starting Wasmtime4j Basic Example");

        try {
            // Example 1: Simple arithmetic function
            demonstrateBasicArithmetic();

            // Example 2: Working with memory
            demonstrateMemoryAccess();

            // Example 3: Multiple function calls
            demonstrateMultipleFunctions();

            logger.info("All examples completed successfully");

        } catch (final Exception e) {
            logger.error("Example failed", e);
            System.exit(1);
        }
    }

    /**
     * Demonstrates calling a simple WebAssembly arithmetic function.
     */
    private static void demonstrateBasicArithmetic() {
        logger.info("=== Basic Arithmetic Example ===");

        // Create WebAssembly module with add function
        final byte[] wasmBytes = createAddModuleWasm();

        try (Engine engine = Engine.newBuilder().build();
             Store store = Store.newBuilder(engine).build()) {

            // Compile the module
            final Module module = Module.fromBinary(engine, wasmBytes);
            logger.info("Module compiled successfully");

            // Instantiate the module
            final Instance instance = Instance.newBuilder(store, module).build();
            logger.info("Module instantiated successfully");

            // Get the 'add' function
            final Function addFunction = instance.getFunction("add");
            if (addFunction == null) {
                throw new RuntimeException("Function 'add' not found");
            }

            // Call the function
            final Object[] results = addFunction.call(42, 13);
            final int result = (Integer) results[0];

            logger.info("add(42, 13) = {}", result);
            assert result == 55 : "Expected 55, got " + result;

            // Clean up
            instance.close();
            module.close();

        } catch (final Exception e) {
            logger.error("Basic arithmetic example failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Demonstrates working with WebAssembly linear memory.
     */
    private static void demonstrateMemoryAccess() {
        logger.info("=== Memory Access Example ===");

        // Create WebAssembly module with memory
        final byte[] wasmBytes = createMemoryModuleWasm();

        try (Engine engine = Engine.newBuilder().build();
             Store store = Store.newBuilder(engine).build()) {

            final Module module = Module.fromBinary(engine, wasmBytes);
            final Instance instance = Instance.newBuilder(store, module).build();

            // Get memory
            final Memory memory = instance.getMemory("memory");
            if (memory == null) {
                throw new RuntimeException("Memory 'memory' not found");
            }

            // Access memory buffer
            final java.nio.ByteBuffer buffer = memory.buffer();
            logger.info("Memory size: {} bytes", buffer.capacity());

            // Write some data
            buffer.put(0, (byte) 0x42);
            buffer.put(1, (byte) 0x13);

            // Read it back
            final byte value1 = buffer.get(0);
            final byte value2 = buffer.get(1);

            logger.info("Memory[0] = 0x{}, Memory[1] = 0x{}",
                       Integer.toHexString(value1 & 0xFF),
                       Integer.toHexString(value2 & 0xFF));

            instance.close();
            module.close();

        } catch (final Exception e) {
            logger.error("Memory access example failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Demonstrates calling multiple WebAssembly functions.
     */
    private static void demonstrateMultipleFunctions() {
        logger.info("=== Multiple Functions Example ===");

        final byte[] wasmBytes = createMathModuleWasm();

        try (Engine engine = Engine.newBuilder().build();
             Store store = Store.newBuilder(engine).build()) {

            final Module module = Module.fromBinary(engine, wasmBytes);
            final Instance instance = Instance.newBuilder(store, module).build();

            // Test multiple math functions
            testMathFunction(instance, "add", 10, 5, 15);
            testMathFunction(instance, "subtract", 10, 5, 5);
            testMathFunction(instance, "multiply", 10, 5, 50);

            instance.close();
            module.close();

        } catch (final Exception e) {
            logger.error("Multiple functions example failed", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to test a math function.
     */
    private static void testMathFunction(final Instance instance, final String functionName,
                                       final int a, final int b, final int expected) {
        final Function function = instance.getFunction(functionName);
        if (function == null) {
            throw new RuntimeException("Function '" + functionName + "' not found");
        }

        final Object[] results = function.call(a, b);
        final int result = (Integer) results[0];

        logger.info("{}({}, {}) = {}", functionName, a, b, result);
        assert result == expected : String.format("Expected %d, got %d", expected, result);
    }

    /**
     * Creates a simple WebAssembly module with an add function.
     * (module
     *   (func $add (param $a i32) (param $b i32) (result i32)
     *     local.get $a
     *     local.get $b
     *     i32.add)
     *   (export "add" (func $add)))
     */
    private static byte[] createAddModuleWasm() {
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // type section
            0x03, 0x02, 0x01, 0x00, // function section
            0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export section
            0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b // code section
        };
    }

    /**
     * Creates a WebAssembly module with memory.
     * (module
     *   (memory 1)
     *   (export "memory" (memory 0)))
     */
    private static byte[] createMemoryModuleWasm() {
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            0x05, 0x03, 0x01, 0x00, 0x01, // memory section
            0x07, 0x0a, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00 // export section
        };
    }

    /**
     * Creates a WebAssembly module with multiple math functions.
     */
    private static byte[] createMathModuleWasm() {
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // type section
            0x03, 0x04, 0x03, 0x00, 0x00, 0x00, // function section (3 functions)
            0x07, 0x20, 0x03, // export section
            0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export "add"
            0x08, 0x73, 0x75, 0x62, 0x74, 0x72, 0x61, 0x63, 0x74, 0x00, 0x01, // export "subtract"
            0x08, 0x6d, 0x75, 0x6c, 0x74, 0x69, 0x70, 0x6c, 0x79, 0x00, 0x02, // export "multiply"
            0x0a, 0x1c, 0x03, // code section (3 functions)
            0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b, // add function
            0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6b, 0x0b, // subtract function
            0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6c, 0x0b  // multiply function
        };
    }
}