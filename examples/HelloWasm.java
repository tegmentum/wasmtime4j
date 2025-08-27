package ai.tegmentum.wasmtime4j.examples;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Optional;

/**
 * Basic example showing how to load and execute a simple WebAssembly module.
 *
 * <p>This example demonstrates:
 * - Creating a WebAssembly runtime with automatic implementation selection
 * - Loading and compiling a WebAssembly module
 * - Instantiating the module and calling exported functions
 * - Proper resource management with try-with-resources
 */
public final class HelloWasm {

    /** Private constructor to prevent instantiation. */
    private HelloWasm() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Main entry point for the example.
     *
     * @param args command line arguments (not used)
     */
    public static void main(final String[] args) {
        try {
            // Simple WebAssembly module that exports an "add" function
            // This is the binary representation of:
            // (module
            //   (func $add (param i32 i32) (result i32)
            //     local.get 0
            //     local.get 1
            //     i32.add)
            //   (export "add" (func $add)))
            final byte[] wasmBytes = {
                0x00, 0x61, 0x73, 0x6d, // WASM magic number
                0x01, 0x00, 0x00, 0x00, // WASM version
                0x01, 0x07,             // Type section
                0x01,                   // 1 type
                0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // (i32, i32) -> i32
                0x03, 0x02,             // Function section
                0x01, 0x00,             // 1 function, type 0
                0x07, 0x07,             // Export section
                0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export "add" as function 0
                0x0a, 0x09,             // Code section
                0x01, 0x07, 0x00,       // 1 function, 7 bytes, 0 locals
                0x20, 0x00,             // local.get 0
                0x20, 0x01,             // local.get 1
                0x6a,                   // i32.add
                0x0b                    // end
            };

            runExample(wasmBytes);
            
        } catch (final Exception e) {
            System.err.println("Error running WebAssembly example: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Runs the WebAssembly example with the given bytecode.
     *
     * @param wasmBytes the WebAssembly module bytecode
     * @throws Exception if any error occurs during execution
     */
    private static void runExample(final byte[] wasmBytes) throws Exception {
        // Create a runtime with automatic implementation selection
        try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
            
            // Print runtime information
            final RuntimeInfo info = runtime.getRuntimeInfo();
            System.out.println("Using runtime: " + info.getRuntimeType());
            System.out.println("Runtime version: " + info.getRuntimeVersion());
            System.out.println("Wasmtime version: " + info.getWasmtimeVersion());
            System.out.println("Java version: " + info.getJavaVersion());
            System.out.println("Platform: " + info.getPlatformInfo());
            System.out.println();

            // Create an engine with default configuration
            try (Engine engine = runtime.createEngine()) {
                
                // Compile the module
                try (Module module = engine.compileModule(wasmBytes)) {
                    
                    System.out.println("Successfully compiled WebAssembly module");
                    System.out.println("Module exports: " + module.getExports().size());
                    
                    // Create a store for execution context
                    try (Store store = engine.createStore()) {
                        
                        // Instantiate the module
                        try (Instance instance = module.instantiate(store)) {
                            
                            System.out.println("Successfully instantiated WebAssembly module");
                            System.out.println("Available exports:");
                            for (final String exportName : instance.getExportNames()) {
                                System.out.println("  - " + exportName);
                            }
                            System.out.println();

                            // Get and call the "add" function
                            final Optional<WasmFunction> addFunc = instance.getFunction("add");
                            if (addFunc.isPresent()) {
                                // Call the function with two i32 values
                                final WasmValue[] result = addFunc.get().call(
                                    WasmValue.i32(5), 
                                    WasmValue.i32(3)
                                );
                                
                                System.out.println("Calling add(5, 3):");
                                System.out.println("Result: " + result[0].asInt());
                                
                                // Call with different values
                                final WasmValue[] result2 = addFunc.get().call(
                                    WasmValue.i32(10), 
                                    WasmValue.i32(-5)
                                );
                                
                                System.out.println("Calling add(10, -5):");
                                System.out.println("Result: " + result2[0].asInt());
                                
                            } else {
                                System.err.println("Function 'add' not found in module exports");
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("\nExample completed successfully!");
    }
}