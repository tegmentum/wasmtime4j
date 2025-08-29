package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the WebAssembly Store.
 *
 * <p>This class represents a WebAssembly store, which serves as an execution context for
 * WebAssembly instances. A store manages the runtime state of WebAssembly instances including
 * memory, globals, tables, and functions. All WebAssembly instances must be created within a store
 * context, and instances from different stores cannot interact directly.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource management with {@link AutoCloseable}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Thread-safe operations
 *   <li>Execution context management for WebAssembly instances
 *   <li>Resource isolation between different stores
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (JniEngine engine = JniEngine.create();
 *      JniStore store = engine.createStore()) {
 *
 *   // Compile module
 *   JniModule module = engine.compileModule(wasmBytes);
 *
 *   // Create instance within this store
 *   try (JniInstance instance = module.instantiate(store)) {
 *     // All operations on the instance are within this store context
 *     JniFunction exportedFunction = instance.getFunction("my_function");
 *     Object[] results = exportedFunction.call(args);
 *   }
 * }
 * }</pre>
 *
 * <p>Store Lifecycle:
 *
 * <ul>
 *   <li>Stores are created by engines using {@link JniEngine#createStore()}
 *   <li>Instances created within a store are tied to that store's lifetime
 *   <li>Closing a store invalidates all instances created within it
 *   <li>Stores cannot be shared between threads safely
 * </ul>
 *
 * <p>This implementation extends {@link JniResource} to provide automatic native resource
 * management and follows defensive programming practices to prevent native crashes.
 *
 * @since 1.0.0
 */
public final class JniStore extends JniResource implements Store {

  private static final Logger LOGGER = Logger.getLogger(JniStore.class.getName());

  /** Custom data associated with this store. */
  private volatile Object customData;

  /** The engine that created this store. */
  private final Engine engine;

  /**
   * Creates a new JNI store with the given native handle.
   *
   * <p>This constructor is package-private and should only be used by the JniEngine or other JNI
   * classes. External code should create stores through {@link JniEngine#createStore()}.
   *
   * @param nativeHandle the native store handle from Wasmtime
   * @param engine the engine that created this store
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniStore(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    this.engine = engine;
    LOGGER.fine("Created JNI store with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Gets runtime information about this store.
   *
   * <p>This method provides diagnostic information about the store's current state, including
   * memory usage, number of active instances, and other runtime metrics that can be useful for
   * debugging and monitoring.
   *
   * @return a string containing store runtime information
   * @throws JniException if information cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public String getRuntimeInfo() {
    ensureNotClosed();

    try {
      final String info = nativeGetStoreInfo(getNativeHandle());
      return info != null ? info : "No information available";
    } catch (final Exception e) {
      throw new JniException("Failed to get store runtime information", e);
    }
  }

  /**
   * Performs garbage collection within this store.
   *
   * <p>This method triggers garbage collection of unused WebAssembly resources within this store
   * context. This can help reclaim memory from instances, functions, and other WebAssembly objects
   * that are no longer reachable.
   *
   * <p>Note: This is separate from Java's garbage collection and only affects WebAssembly-specific
   * resources managed by this store.
   *
   * @throws JniException if garbage collection fails
   * @throws JniResourceException if this store has been closed
   */
  public void gc() {
    ensureNotClosed();

    try {
      final boolean success = nativeStoreGc(getNativeHandle());
      if (!success) {
        throw new JniException("Store garbage collection failed");
      }
      LOGGER.fine(
          "Performed garbage collection for store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Unexpected error during store garbage collection", e);
    }
  }

  /**
   * Sets the fuel limit for this store.
   *
   * <p>Fuel is a mechanism for limiting WebAssembly execution time. When fuel is enabled,
   * WebAssembly execution will be interrupted when the fuel limit is reached. This can be used to
   * prevent runaway computations.
   *
   * @param fuel the fuel limit (must be positive)
   * @throws JniException if the fuel limit cannot be set
   * @throws JniResourceException if this store has been closed
   */
  public void setFuelLimit(final long fuel) {
    JniValidation.requirePositive(fuel, "fuel");
    ensureNotClosed();

    try {
      final boolean success = nativeSetFuelLimit(getNativeHandle(), fuel);
      if (!success) {
        throw new JniException("Failed to set fuel limit to " + fuel);
      }
      LOGGER.fine(
          "Set fuel limit to " + fuel + " for store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Unexpected error setting fuel limit", e);
    }
  }

  /**
   * Gets the remaining fuel for this store.
   *
   * <p>Returns the amount of fuel remaining for WebAssembly execution in this store. If fuel is not
   * enabled, this method returns -1.
   *
   * @return the remaining fuel, or -1 if fuel is not enabled
   * @throws JniException if the fuel amount cannot be retrieved
   * @throws JniResourceException if this store has been closed
   */
  public long getRemainingFuel() {
    ensureNotClosed();

    try {
      return nativeGetRemainingFuel(getNativeHandle());
    } catch (final Exception e) {
      throw new JniException("Failed to get remaining fuel", e);
    }
  }

  /**
   * Adds fuel to this store.
   *
   * <p>This method adds additional fuel to the store's fuel limit. This can be used to extend
   * execution time for long-running WebAssembly computations.
   *
   * @param additionalFuel the amount of fuel to add (must be positive)
   * @throws JniException if fuel cannot be added
   * @throws JniResourceException if this store has been closed
   */
  public void addFuel(final long additionalFuel) {
    JniValidation.requirePositive(additionalFuel, "additionalFuel");
    ensureNotClosed();

    try {
      final boolean success = nativeAddFuel(getNativeHandle(), additionalFuel);
      if (!success) {
        throw new JniException("Failed to add fuel: " + additionalFuel);
      }
      LOGGER.fine(
          "Added " + additionalFuel + " fuel to store 0x" + Long.toHexString(getNativeHandle()));
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Unexpected error adding fuel", e);
    }
  }

  // Interface implementation methods

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public Object getData() {
    return customData;
  }

  @Override
  public void setData(final Object data) {
    this.customData = data;
  }

  @Override
  public void setFuel(final long fuel) {
    setFuelLimit(fuel);
  }

  @Override
  public long getFuel() {
    return getRemainingFuel();
  }

  @Override
  public void setEpochDeadline(final long ticks) {
    ensureNotClosed();
    try {
      final boolean success = nativeSetEpochDeadline(getNativeHandle(), ticks);
      if (!success) {
        throw new JniException("Failed to set epoch deadline to " + ticks);
      }
    } catch (final Exception e) {
      if (e instanceof JniException) {
        throw e;
      }
      throw new JniException("Unexpected error setting epoch deadline", e);
    }
  }

  @Override
  public boolean isValid() {
    return !isClosed() && getNativeHandle() != 0;
  }

  @Override
  protected void doClose() throws Exception {
    if (getNativeHandle() != 0) {
      nativeDestroyStore(getNativeHandle());
      LOGGER.fine("Destroyed JNI store with handle: 0x" + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "Store";
  }

  // Native method declarations

  /**
   * Gets runtime information about a store.
   *
   * @param storeHandle the native store handle
   * @return information string or null on error
   */
  private static native String nativeGetStoreInfo(long storeHandle);

  /**
   * Triggers garbage collection within a store.
   *
   * @param storeHandle the native store handle
   * @return true on success, false on failure
   */
  private static native boolean nativeStoreGc(long storeHandle);

  /**
   * Sets the fuel limit for a store.
   *
   * @param storeHandle the native store handle
   * @param fuel the fuel limit
   * @return true on success, false on failure
   */
  private static native boolean nativeSetFuelLimit(long storeHandle, long fuel);

  /**
   * Gets the remaining fuel for a store.
   *
   * @param storeHandle the native store handle
   * @return remaining fuel or -1 if fuel is not enabled
   */
  private static native long nativeGetRemainingFuel(long storeHandle);

  /**
   * Adds fuel to a store.
   *
   * @param storeHandle the native store handle
   * @param additionalFuel the amount of fuel to add
   * @return true on success, false on failure
   */
  private static native boolean nativeAddFuel(long storeHandle, long additionalFuel);

  /**
   * Sets the epoch deadline for a store.
   *
   * @param storeHandle the native store handle
   * @param ticks the number of epoch ticks before interruption
   * @return true on success, false on failure
   */
  private static native boolean nativeSetEpochDeadline(long storeHandle, long ticks);

  /**
   * Destroys a native store and releases all associated resources.
   *
   * @param storeHandle the native store handle
   */
  private static native void nativeDestroyStore(long storeHandle);
}
