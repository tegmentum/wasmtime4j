package ai.tegmentum.wasmtime4j.examples.basic;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;

/**
 * Simple standalone WebAssembly application demonstrating basic usage patterns.
 * 
 * This example shows:
 * - Runtime initialization and selection
 * - Module loading and compilation
 * - Function execution with different parameter types
 * - Memory operations
 * - Resource cleanup
 * - Error handling
 */
public class SimpleWebAssemblyApp {
    
    public static void main(String[] args) {
        System.out.println("Wasmtime4j Simple Example");
        System.out.println("=========================");
        
        // Demonstrate different usage patterns
        demonstrateBasicUsage();
        demonstrateRuntimeSelection();
        demonstrateErrorHandling();
        demonstrateMemoryOperations();
        demonstrateAdvancedFeatures();
    }
    
    /**
     * Basic WebAssembly module loading and function execution.
     */
    private static void demonstrateBasicUsage() {
        System.out.println("\n1. Basic Usage Example");
        System.out.println("----------------------");
        
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            // Display runtime information
            RuntimeInfo info = runtime.getRuntimeInfo();
            System.out.printf("Runtime: %s v%s%n", info.getRuntimeType(), info.getVersion());
            
            // Create an optimized engine
            EngineConfig config = EngineConfig.builder()
                .optimizationLevel(OptimizationLevel.SPEED)
                .build();
            Engine engine = runtime.createEngine(config);
            
            // Simple arithmetic module (add two numbers)
            byte[] addModuleWasm = createSimpleAddModule();
            Module addModule = runtime.compileModule(engine, addModuleWasm);
            Instance addInstance = runtime.instantiate(addModule);
            
            // Execute the add function
            WasmFunction addFunc = addInstance.getFunction("add");
            WasmValue[] args = {WasmValue.i32(42), WasmValue.i32(8)};
            WasmValue[] results = addFunc.call(args);
            
            System.out.printf("42 + 8 = %d%n", results[0].asI32());
            
        } catch (WasmException e) {
            System.err.println("WebAssembly error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate runtime selection and information.
     */
    private static void demonstrateRuntimeSelection() {
        System.out.println("\n2. Runtime Selection Example");
        System.out.println("-----------------------------");
        
        // Show available runtimes
        System.out.println("Available runtimes:");
        for (RuntimeType type : RuntimeType.values()) {
            boolean available = WasmRuntimeFactory.isRuntimeAvailable(type);
            System.out.printf("  %s: %s%n", type, available ? "Available" : "Not Available");
        }
        
        // Show selected runtime
        RuntimeType selectedType = WasmRuntimeFactory.getSelectedRuntimeType();
        System.out.printf("Auto-selected runtime: %s%n", selectedType);
        
        // Try specific runtime types
        tryRuntimeType(RuntimeType.JNI);
        tryRuntimeType(RuntimeType.PANAMA);
    }
    
    private static void tryRuntimeType(RuntimeType type) {
        if (WasmRuntimeFactory.isRuntimeAvailable(type)) {
            try (WasmRuntime runtime = WasmRuntimeFactory.create(type)) {
                RuntimeInfo info = runtime.getRuntimeInfo();
                System.out.printf("  Using %s: Implementation=%s, Valid=%s%n", 
                    type, info.getImplementation(), runtime.isValid());
            } catch (Exception e) {
                System.out.printf("  Error with %s: %s%n", type, e.getMessage());
            }
        } else {
            System.out.printf("  %s runtime not available%n", type);
        }
    }
    
    /**
     * Demonstrate comprehensive error handling.
     */
    private static void demonstrateErrorHandling() {
        System.out.println("\n3. Error Handling Example");
        System.out.println("--------------------------");
        
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            Engine engine = runtime.createEngine();
            
            // Test 1: Invalid WebAssembly bytecode
            try {
                byte[] invalidWasm = {0x00, 0x61, 0x73, 0x6d}; // Incomplete magic number
                runtime.compileModule(engine, invalidWasm);
            } catch (ValidationException e) {
                System.out.println("✓ Caught validation error: " + e.getMessage());
            }
            
            // Test 2: Missing function
            try {
                byte[] validWasm = createSimpleAddModule();
                Module module = runtime.compileModule(engine, validWasm);
                Instance instance = runtime.instantiate(module);
                instance.getFunction("nonexistent_function");
            } catch (RuntimeException e) {
                System.out.println("✓ Caught missing function error: " + e.getMessage());
            }
            
            // Test 3: Wrong argument count
            try {
                byte[] validWasm = createSimpleAddModule();
                Module module = runtime.compileModule(engine, validWasm);
                Instance instance = runtime.instantiate(module);
                WasmFunction addFunc = instance.getFunction("add");
                
                // Call with wrong number of arguments
                WasmValue[] wrongArgs = {WasmValue.i32(1)}; // Should be 2 args
                addFunc.call(wrongArgs);
            } catch (RuntimeException e) {
                System.out.println("✓ Caught argument count error: " + e.getMessage());
            }
            
        } catch (WasmException e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate WebAssembly memory operations.
     */
    private static void demonstrateMemoryOperations() {
        System.out.println("\n4. Memory Operations Example");
        System.out.println("-----------------------------");
        
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            Engine engine = runtime.createEngine();
            
            // Module with memory export
            byte[] memoryModuleWasm = createMemoryModule();
            Module module = runtime.compileModule(engine, memoryModuleWasm);
            Instance instance = runtime.instantiate(module);
            
            // Get the memory
            WasmMemory memory = instance.getMemory("memory");
            System.out.printf("Memory size: %d pages (%d bytes)%n", 
                memory.size(), memory.size() * 65536);
            
            // Write data to memory
            String message = "Hello, WebAssembly Memory!";
            byte[] messageBytes = message.getBytes();
            memory.write(0, messageBytes);
            System.out.printf("Wrote %d bytes to memory%n", messageBytes.length);
            
            // Read data back
            byte[] readBytes = memory.read(0, messageBytes.length);
            String readMessage = new String(readBytes);
            System.out.printf("Read back: '%s'%n", readMessage);
            
            // Demonstrate memory growth
            int oldSize = memory.size();
            memory.grow(1); // Grow by 1 page (64KB)
            int newSize = memory.size();
            System.out.printf("Memory grown from %d to %d pages%n", oldSize, newSize);
            
        } catch (WasmException e) {
            System.err.println("Memory operations error: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate advanced features like globals and multiple return values.
     */
    private static void demonstrateAdvancedFeatures() {
        System.out.println("\n5. Advanced Features Example");
        System.out.println("-----------------------------");
        
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            Engine engine = runtime.createEngine();
            
            // Module with multiple features
            byte[] advancedModuleWasm = createAdvancedModule();
            Module module = runtime.compileModule(engine, advancedModuleWasm);
            Instance instance = runtime.instantiate(module);
            
            // Test function with multiple return values
            WasmFunction divModFunc = instance.getFunction("divmod");
            WasmValue[] args = {WasmValue.i32(17), WasmValue.i32(3)};
            WasmValue[] results = divModFunc.call(args);
            
            System.out.printf("17 ÷ 3 = %d remainder %d%n", 
                results[0].asI32(), results[1].asI32());
            
            // Test global variable access
            WasmGlobal counter = instance.getGlobal("counter");
            System.out.printf("Initial counter value: %d%n", counter.get().asI32());
            
            // Call function that modifies global
            WasmFunction incrementFunc = instance.getFunction("increment");
            incrementFunc.call();
            
            System.out.printf("Counter after increment: %d%n", counter.get().asI32());
            
            // Test floating point operations
            WasmFunction sqrtFunc = instance.getFunction("sqrt_approx");
            WasmValue[] sqrtArgs = {WasmValue.f32(16.0f)};
            WasmValue[] sqrtResults = sqrtFunc.call(sqrtArgs);
            
            System.out.printf("sqrt(16) ≈ %.3f%n", sqrtResults[0].asF32());
            
        } catch (WasmException e) {
            System.err.println("Advanced features error: " + e.getMessage());
        }
    }
    
    // Helper methods to create WebAssembly modules programmatically
    // In real applications, you would load these from .wasm files
    
    private static byte[] createSimpleAddModule() {
        // WebAssembly module: (module (func $add (export "add") (param i32 i32) (result i32) local.get 0 local.get 1 i32.add))
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // type section
            0x03, 0x02, 0x01, 0x00, // function section
            0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export section
            0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b // code section
        };
    }
    
    private static byte[] createMemoryModule() {
        // Simple module with 1 page of memory
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            0x05, 0x03, 0x01, 0x00, 0x01, // memory section (1 page min)
            0x07, 0x0a, 0x01, 0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00 // export memory
        };
    }
    
    private static byte[] createAdvancedModule() {
        // Module with divmod function, global counter, and sqrt approximation
        // This is a simplified representation - real modules would be more complex
        return new byte[] {
            0x00, 0x61, 0x73, 0x6d, // magic
            0x01, 0x00, 0x00, 0x00, // version
            // Type section - function signatures
            0x01, 0x0f, 0x03, 
            0x60, 0x02, 0x7f, 0x7f, 0x02, 0x7f, 0x7f, // divmod: (i32, i32) -> (i32, i32)
            0x60, 0x00, 0x00, // increment: () -> ()
            0x60, 0x01, 0x7d, 0x01, 0x7d, // sqrt_approx: (f32) -> (f32)
            // Function section
            0x03, 0x04, 0x03, 0x00, 0x01, 0x02,
            // Global section - mutable i32 counter
            0x06, 0x06, 0x01, 0x7f, 0x01, 0x41, 0x00, 0x0b,
            // Export section
            0x07, 0x2a, 0x04,
            0x06, 0x64, 0x69, 0x76, 0x6d, 0x6f, 0x64, 0x00, 0x00, // export divmod
            0x09, 0x69, 0x6e, 0x63, 0x72, 0x65, 0x6d, 0x65, 0x6e, 0x74, 0x00, 0x01, // export increment
            0x0b, 0x73, 0x71, 0x72, 0x74, 0x5f, 0x61, 0x70, 0x70, 0x72, 0x6f, 0x78, 0x00, 0x02, // export sqrt_approx
            0x07, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x65, 0x72, 0x03, 0x00, // export counter global
            // Code section (simplified implementations)
            0x0a, 0x20, 0x03,
            // divmod function: return (a/b, a%b)
            0x0b, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6d, 0x20, 0x00, 0x20, 0x01, 0x6f, 0x0b,
            // increment function: global.get 0, i32.const 1, i32.add, global.set 0
            0x08, 0x00, 0x23, 0x00, 0x41, 0x01, 0x6a, 0x24, 0x00, 0x0b,
            // sqrt_approx function: simple approximation
            0x07, 0x00, 0x20, 0x00, 0x43, 0x00, 0x00, 0x80, 0x3f, 0x94, 0x0b
        };
    }
}