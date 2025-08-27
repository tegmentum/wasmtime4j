package ai.tegmentum.wasmtime4j;

import java.io.Closeable;

/**
 * Represents a WebAssembly store.
 * 
 * <p>A Store is an execution context that holds the runtime state for WebAssembly
 * instances. Each store maintains isolated linear memory, globals, and execution
 * state. Objects like instances, functions, and memories are tied to a specific store.
 * 
 * <p>Stores are not thread-safe and should not be shared between threads without
 * external synchronization.
 * 
 * @since 1.0.0
 */
public interface Store extends Closeable {
    
    /**
     * Gets the engine associated with this store.
     * 
     * @return the Engine that created this store
     */
    Engine getEngine();
    
    /**
     * Gets custom data associated with this store, if any.
     * 
     * @return the custom data, or null if none was set
     */
    Object getData();
    
    /**
     * Sets custom data to be associated with this store.
     * 
     * @param data the custom data to associate
     */
    void setData(final Object data);
    
    /**
     * Sets the fuel amount available for WebAssembly execution.
     * 
     * <p>Fuel is consumed during execution and can be used to limit
     * the amount of computation performed by WebAssembly code.
     * 
     * @param fuel the amount of fuel to set
     * @throws IllegalArgumentException if fuel is negative
     */
    void setFuel(final long fuel);
    
    /**
     * Gets the remaining fuel amount.
     * 
     * @return the remaining fuel, or -1 if fuel consumption is disabled
     */
    long getFuel();
    
    /**
     * Adds fuel to the store.
     * 
     * @param fuel the amount of fuel to add
     * @throws IllegalArgumentException if fuel is negative
     */
    void addFuel(final long fuel);
    
    /**
     * Sets the epoch deadline for WebAssembly execution.
     * 
     * <p>Epochs provide a way to interrupt long-running WebAssembly code
     * at regular intervals.
     * 
     * @param ticks the number of epoch ticks before interruption
     */
    void setEpochDeadline(final long ticks);
    
    /**
     * Checks if the store is still valid and usable.
     * 
     * @return true if the store is valid, false otherwise
     */
    boolean isValid();
    
    /**
     * Closes the store and releases associated resources.
     * 
     * <p>After closing, the store becomes invalid and should not be used.
     * All instances and other objects associated with this store may also
     * become invalid.
     */
    @Override
    void close();
}