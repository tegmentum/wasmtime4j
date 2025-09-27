package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Provides access to the calling WebAssembly instance context within host functions.
 *
 * <p>The Caller interface allows host functions to access exports from the calling
 * WebAssembly instance, including memory, tables, globals, and functions. This enables
 * host functions to interact with the WebAssembly module's state and resources.
 *
 * <p>Caller instances are passed to host functions that are defined with caller context
 * support and provide safe access to the execution environment.
 *
 * @param <T> the type of user data associated with the store
 * @since 1.0.0
 */
public interface Caller<T> {

    /**
     * Gets the user data associated with the store.
     *
     * <p>This is the same data that was provided when creating the store and
     * can be used to maintain state across host function calls.
     *
     * @return the store's user data
     * @since 1.0.0
     */
    T data();

    /**
     * Gets an exported item by name from the calling instance.
     *
     * <p>This method provides access to any export (function, memory, table, or global)
     * that the calling WebAssembly instance has made available.
     *
     * @param name the name of the export to retrieve
     * @return the export if it exists, empty otherwise
     * @throws IllegalArgumentException if name is null
     * @since 1.0.0
     */
    Optional<Export> getExport(String name);

    /**
     * Gets an exported function by name from the calling instance.
     *
     * @param name the name of the function export
     * @return the function if it exists and is a function, empty otherwise
     * @throws IllegalArgumentException if name is null
     * @since 1.0.0
     */
    Optional<Function> getFunction(String name);

    /**
     * Gets an exported memory by name from the calling instance.
     *
     * @param name the name of the memory export
     * @return the memory if it exists and is a memory, empty otherwise
     * @throws IllegalArgumentException if name is null
     * @since 1.0.0
     */
    Optional<Memory> getMemory(String name);

    /**
     * Gets the default memory export from the calling instance.
     *
     * <p>This is a convenience method that looks for a memory export named "memory",
     * which is the default export name for WebAssembly memory.
     *
     * @return the default memory if it exists, empty otherwise
     * @since 1.0.0
     */
    default Optional<Memory> getMemory() {
        return getMemory("memory");
    }

    /**
     * Gets an exported table by name from the calling instance.
     *
     * @param name the name of the table export
     * @return the table if it exists and is a table, empty otherwise
     * @throws IllegalArgumentException if name is null
     * @since 1.0.0
     */
    Optional<Table> getTable(String name);

    /**
     * Gets an exported global by name from the calling instance.
     *
     * @param name the name of the global export
     * @return the global if it exists and is a global, empty otherwise
     * @throws IllegalArgumentException if name is null
     * @since 1.0.0
     */
    Optional<Global> getGlobal(String name);

    /**
     * Checks if the calling instance has an export with the given name.
     *
     * @param name the name to check for
     * @return true if an export with that name exists
     * @throws IllegalArgumentException if name is null
     * @since 1.0.0
     */
    boolean hasExport(String name);

    /**
     * Gets the current fuel consumption if fuel metering is enabled.
     *
     * <p>Fuel metering allows limiting the execution time of WebAssembly code.
     * This method returns the amount of fuel consumed so far in the current call.
     *
     * @return the fuel consumed, or empty if fuel metering is not enabled
     * @since 1.0.0
     */
    Optional<Long> fuelConsumed();

    /**
     * Checks if an epoch deadline has been set for the current execution.
     *
     * <p>Epoch-based interruption provides another mechanism for limiting
     * execution time and ensuring responsiveness.
     *
     * @return true if an epoch deadline is active
     * @since 1.0.0
     */
    boolean hasEpochDeadline();

    /**
     * Gets the current epoch deadline if one is set.
     *
     * @return the epoch deadline, or empty if none is set
     * @since 1.0.0
     */
    Optional<Long> epochDeadline();
}