package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Utility class for adding WASI (WebAssembly System Interface) imports to a linker.
 *
 * <p>WasiLinker provides convenient methods for setting up WASI functionality in
 * WebAssembly modules by automatically defining all necessary WASI imports in a linker.
 *
 * <p>WASI enables WebAssembly modules to interact with the host system in a standardized
 * way, providing access to file systems, environment variables, command-line arguments,
 * and other system resources.
 *
 * @since 1.0.0
 */
public final class WasiLinker {

    // Prevent instantiation
    private WasiLinker() {
    }

    /**
     * Adds all WASI imports to the specified linker using the provided WASI context.
     *
     * <p>This method defines all necessary WASI functions in the linker, including:
     * <ul>
     *   <li>File system operations (open, read, write, etc.)</li>
     *   <li>Environment variable access</li>
     *   <li>Command-line argument access</li>
     *   <li>Time and random number functions</li>
     *   <li>Process and memory management functions</li>
     * </ul>
     *
     * @param linker the linker to add WASI imports to
     * @param context the WASI context containing configuration
     * @throws WasmException if adding WASI imports fails
     * @throws IllegalArgumentException if linker or context is null
     * @since 1.0.0
     */
    public static void addToLinker(Linker<WasiContext> linker, WasiContext context) throws WasmException {
        if (linker == null) {
            throw new IllegalArgumentException("Linker cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("WasiContext cannot be null");
        }

        // Implementation will be provided by the runtime
        WasmRuntimeFactory.create().addWasiToLinker(linker, context);
    }

    /**
     * Adds all WASI imports to the specified linker using a default WASI context.
     *
     * <p>This is a convenience method that creates a default WASI context with
     * inherited stdio and no file system access.
     *
     * @param linker the linker to add WASI imports to
     * @throws WasmException if adding WASI imports fails
     * @throws IllegalArgumentException if linker is null
     * @since 1.0.0
     */
    public static void addToLinker(Linker<WasiContext> linker) throws WasmException {
        WasiContext defaultContext = WasiContext.create().inheritStdio();
        addToLinker(linker, defaultContext);
    }

    /**
     * Creates a new linker with WASI imports already configured.
     *
     * <p>This is a convenience method that creates a new linker and adds all
     * WASI imports using the provided context.
     *
     * @param engine the engine to create the linker for
     * @param context the WASI context containing configuration
     * @return a new linker with WASI imports configured
     * @throws WasmException if creating the linker or adding WASI imports fails
     * @throws IllegalArgumentException if engine or context is null
     * @since 1.0.0
     */
    public static Linker<WasiContext> createLinker(Engine engine, WasiContext context) throws WasmException {
        if (engine == null) {
            throw new IllegalArgumentException("Engine cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("WasiContext cannot be null");
        }

        Linker<WasiContext> linker = WasmRuntimeFactory.create().createLinker(engine);
        addToLinker(linker, context);
        return linker;
    }

    /**
     * Creates a new linker with WASI imports using a default context.
     *
     * <p>This is a convenience method that creates a new linker with WASI imports
     * configured using a default context (inherited stdio, no file system access).
     *
     * @param engine the engine to create the linker for
     * @return a new linker with default WASI imports configured
     * @throws WasmException if creating the linker or adding WASI imports fails
     * @throws IllegalArgumentException if engine is null
     * @since 1.0.0
     */
    public static Linker<WasiContext> createLinker(Engine engine) throws WasmException {
        WasiContext defaultContext = WasiContext.create().inheritStdio();
        return createLinker(engine, defaultContext);
    }

    /**
     * Checks if a linker has WASI imports configured.
     *
     * <p>This method checks for the presence of common WASI imports to determine
     * if WASI functionality has been added to the linker.
     *
     * @param linker the linker to check
     * @return true if WASI imports are present, false otherwise
     * @throws IllegalArgumentException if linker is null
     * @since 1.0.0
     */
    public static boolean hasWasiImports(Linker<?> linker) {
        if (linker == null) {
            throw new IllegalArgumentException("Linker cannot be null");
        }

        // Check for common WASI imports
        return linker.hasImport("wasi_snapshot_preview1", "fd_write") ||
               linker.hasImport("wasi_snapshot_preview1", "proc_exit") ||
               linker.hasImport("wasi_unstable", "fd_write");
    }
}