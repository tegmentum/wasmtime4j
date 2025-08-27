package ai.tegmentum.wasmtime4j.utils;

/**
 * Test category constants for Wasmtime4j tests.
 * These categories can be used to enable/disable specific test groups.
 */
public final class TestCategories {
    
    /** Runtime selection and switching tests */
    public static final String RUNTIME = "runtime";
    
    /** Cross-platform compatibility tests */
    public static final String PLATFORM = "platform";
    
    /** WebAssembly test suite integration */
    public static final String WASM_SUITE = "wasm.suite";
    
    /** Native library loading and management tests */
    public static final String NATIVE = "native";
    
    /** Performance and benchmarking tests */
    public static final String PERFORMANCE = "performance";
    
    /** WASI (WebAssembly System Interface) tests */
    public static final String WASI = "wasi";
    
    /** Memory management and garbage collection tests */
    public static final String MEMORY = "memory";
    
    /** Security and sandboxing tests */
    public static final String SECURITY = "security";
    
    /** Error handling and edge case tests */
    public static final String ERROR_HANDLING = "error";
    
    /** Multi-threading and concurrency tests */
    public static final String CONCURRENCY = "concurrency";
    
    private TestCategories() {
        // Utility class - prevent instantiation
    }
}