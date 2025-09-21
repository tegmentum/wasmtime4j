package com.example;

import ai.tegmentum.wasmtime4j.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Logger;

/**
 * Test class for BasicExample.
 *
 * This test demonstrates proper testing patterns for wasmtime4j applications:
 * - Resource management with try-with-resources
 * - Proper setup and teardown
 * - Error handling
 * - Multiple test scenarios
 */
public class BasicExampleTest {

    private static final Logger logger = Logger.getLogger(BasicExampleTest.class.getName());

    private Engine engine;
    private Store store;

    @BeforeEach
    void setUp(final TestInfo testInfo) {
        logger.info("Setting up test: " + testInfo.getDisplayName());
        engine = Engine.newBuilder().build();
        store = Store.newBuilder(engine).build();
    }

    @AfterEach
    void tearDown(final TestInfo testInfo) {
        logger.info("Tearing down test: " + testInfo.getDisplayName());
        if (store != null) {
            store.close();
        }
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    @DisplayName("Test basic arithmetic operations")
    void testBasicArithmetic() {
        // Create simple add module
        final byte[] wasmBytes = createAddModuleWasm();

        final Module module = Module.fromBinary(engine, wasmBytes);
        final Instance instance = Instance.newBuilder(store, module).build();

        // Test the add function
        final Function addFunction = instance.getFunction("add");
        assertNotNull(addFunction, "Add function should be exported");

        // Test various inputs
        testAddFunction(addFunction, 0, 0, 0);
        testAddFunction(addFunction, 1, 1, 2);
        testAddFunction(addFunction, 42, 13, 55);
        testAddFunction(addFunction, -10, 5, -5);
        testAddFunction(addFunction, Integer.MAX_VALUE - 1, 1, Integer.MAX_VALUE);

        instance.close();
        module.close();
    }

    @Test
    @DisplayName("Test memory access operations")
    void testMemoryAccess() {
        final byte[] wasmBytes = createMemoryModuleWasm();

        final Module module = Module.fromBinary(engine, wasmBytes);
        final Instance instance = Instance.newBuilder(store, module).build();

        // Test memory access
        final Memory memory = instance.getMemory("memory");
        assertNotNull(memory, "Memory should be exported");

        final java.nio.ByteBuffer buffer = memory.buffer();
        assertNotNull(buffer, "Memory buffer should be accessible");
        assertTrue(buffer.capacity() >= 65536, "Memory should be at least 1 page (64KB)");

        // Test writing and reading
        buffer.put(0, (byte) 0x42);
        buffer.put(100, (byte) 0x13);

        assertEquals((byte) 0x42, buffer.get(0), "Should read back written value");
        assertEquals((byte) 0x13, buffer.get(100), "Should read back written value");

        instance.close();
        module.close();
    }

    @Test
    @DisplayName("Test multiple function exports")
    void testMultipleFunctions() {
        final byte[] wasmBytes = createMathModuleWasm();

        final Module module = Module.fromBinary(engine, wasmBytes);
        final Instance instance = Instance.newBuilder(store, module).build();

        // Test all math functions
        testMathFunction(instance, "add", 10, 5, 15);
        testMathFunction(instance, "subtract", 10, 5, 5);
        testMathFunction(instance, "multiply", 10, 5, 50);

        // Test edge cases
        testMathFunction(instance, "add", 0, 0, 0);
        testMathFunction(instance, "subtract", 5, 10, -5);
        testMathFunction(instance, "multiply", 0, 100, 0);

        instance.close();
        module.close();
    }

    @Test
    @DisplayName("Test error handling")
    void testErrorHandling() {
        final byte[] wasmBytes = createAddModuleWasm();

        final Module module = Module.fromBinary(engine, wasmBytes);
        final Instance instance = Instance.newBuilder(store, module).build();

        // Test calling non-existent function
        final Function nonExistentFunction = instance.getFunction("nonexistent");
        assertNull(nonExistentFunction, "Non-existent function should return null");

        // Test calling with wrong number of arguments
        final Function addFunction = instance.getFunction("add");
        assertNotNull(addFunction);

        // This should throw an exception for wrong arity
        assertThrows(RuntimeException.class, () -> {
            addFunction.call(1); // Only one argument instead of two
        }, "Should throw exception for wrong number of arguments");

        assertThrows(RuntimeException.class, () -> {
            addFunction.call(1, 2, 3); // Too many arguments
        }, "Should throw exception for too many arguments");

        instance.close();
        module.close();
    }

    @Test
    @DisplayName("Test invalid WebAssembly module")
    void testInvalidModule() {
        // Invalid WebAssembly bytes
        final byte[] invalidWasm = new byte[] { 0x00, 0x01, 0x02, 0x03 };

        assertThrows(RuntimeException.class, () -> {
            Module.fromBinary(engine, invalidWasm);
        }, "Should throw exception for invalid WebAssembly module");
    }

    @Test
    @DisplayName("Test resource cleanup")
    void testResourceCleanup() {
        final byte[] wasmBytes = createAddModuleWasm();

        // Test that we can create and destroy multiple instances
        for (int i = 0; i < 10; i++) {
            final Module module = Module.fromBinary(engine, wasmBytes);
            final Instance instance = Instance.newBuilder(store, module).build();

            final Function addFunction = instance.getFunction("add");
            final Object[] results = addFunction.call(i, i);
            assertEquals(i * 2, results[0]);

            instance.close();
            module.close();
        }

        // Test should complete without memory leaks
        logger.info("Resource cleanup test completed successfully");
    }

    /**
     * Helper method to test add function with specific inputs.
     */
    private void testAddFunction(final Function addFunction, final int a, final int b, final int expected) {
        final Object[] results = addFunction.call(a, b);
        assertEquals(1, results.length, "Add function should return one result");
        assertEquals(expected, results[0], String.format("add(%d, %d) should equal %d", a, b, expected));
    }

    /**
     * Helper method to test math functions.
     */
    private void testMathFunction(final Instance instance, final String functionName,
                                final int a, final int b, final int expected) {
        final Function function = instance.getFunction(functionName);
        assertNotNull(function, "Function '" + functionName + "' should be exported");

        final Object[] results = function.call(a, b);
        assertEquals(1, results.length, "Function should return one result");
        assertEquals(expected, results[0],
                   String.format("%s(%d, %d) should equal %d", functionName, a, b, expected));
    }

    /**
     * Creates a simple WebAssembly module with an add function.
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