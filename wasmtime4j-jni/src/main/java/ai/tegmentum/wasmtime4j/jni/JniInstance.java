package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Instance;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Instance interface.
 *
 * <p>This class represents an instantiated WebAssembly module and provides access to its functions,
 * memories, tables, and globals through JNI calls to the native Wasmtime library. An instance is
 * the runtime representation of a compiled module.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 */
public final class JniInstance implements Instance, AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniInstance.class.getName());

  /** Native instance handle. */
  private volatile long nativeHandle;

  /** Flag to track if this instance has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JNI instance with the given native handle.
   *
   * @param nativeHandle the native instance handle
   * @throws IllegalArgumentException if nativeHandle is 0
   */
  JniInstance(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be 0");
    }
    this.nativeHandle = nativeHandle;
    LOGGER.fine("Created JNI instance with handle: " + nativeHandle);
  }

  /**
   * Gets a function export by name.
   *
   * @param name the name of the exported function
   * @return the function wrapper
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   * @throws RuntimeException if the function is not found
   */
  public JniFunction getFunction(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Function name cannot be null or empty");
    }
    validateNotClosed();

    try {
      final long functionHandle = nativeGetFunction(nativeHandle, name);
      if (functionHandle == 0) {
        throw new RuntimeException("Function not found: " + name);
      }
      return new JniFunction(functionHandle, name);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting function: " + name, e);
    }
  }

  /**
   * Gets a memory export by name.
   *
   * @param name the name of the exported memory
   * @return the memory wrapper
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   * @throws RuntimeException if the memory is not found
   */
  public JniMemory getMemory(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Memory name cannot be null or empty");
    }
    validateNotClosed();

    try {
      final long memoryHandle = nativeGetMemory(nativeHandle, name);
      if (memoryHandle == 0) {
        throw new RuntimeException("Memory not found: " + name);
      }
      return new JniMemory(memoryHandle);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory: " + name, e);
    }
  }

  /**
   * Gets a table export by name.
   *
   * @param name the name of the exported table
   * @return the table wrapper
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   * @throws RuntimeException if the table is not found
   */
  public JniTable getTable(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Table name cannot be null or empty");
    }
    validateNotClosed();

    try {
      final long tableHandle = nativeGetTable(nativeHandle, name);
      if (tableHandle == 0) {
        throw new RuntimeException("Table not found: " + name);
      }
      return new JniTable(tableHandle);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting table: " + name, e);
    }
  }

  /**
   * Gets a global export by name.
   *
   * @param name the name of the exported global
   * @return the global wrapper
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   * @throws RuntimeException if the global is not found
   */
  public JniGlobal getGlobal(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Global name cannot be null or empty");
    }
    validateNotClosed();

    try {
      final long globalHandle = nativeGetGlobal(nativeHandle, name);
      if (globalHandle == 0) {
        throw new RuntimeException("Global not found: " + name);
      }
      return new JniGlobal(globalHandle);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global: " + name, e);
    }
  }

  /**
   * Gets the default memory export (named "memory").
   *
   * @return the default memory wrapper
   * @throws IllegalStateException if this instance is closed
   * @throws RuntimeException if no default memory is found
   */
  public JniMemory getDefaultMemory() {
    return getMemory("memory");
  }

  /**
   * Checks if this instance has an export with the given name.
   *
   * @param name the export name to check
   * @return true if the export exists
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if this instance is closed
   */
  public boolean hasExport(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Export name cannot be null or empty");
    }
    validateNotClosed();

    try {
      return nativeHasExport(nativeHandle, name);
    } catch (final Exception e) {
      LOGGER.warning("Error checking export existence: " + e.getMessage());
      return false;
    }
  }

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name
   */
  @Override
  protected String getResourceType() {
    return "Instance";
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    nativeDestroyInstance(nativeHandle);
  }

  // Native method declarations

  /**
   * Gets a function export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the function name
   * @return native function handle or 0 if not found
   */
  private static native long nativeGetFunction(long instanceHandle, String name);

  /**
   * Gets a memory export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the memory name
   * @return native memory handle or 0 if not found
   */
  private static native long nativeGetMemory(long instanceHandle, String name);

  /**
   * Gets a table export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the table name
   * @return native table handle or 0 if not found
   */
  private static native long nativeGetTable(long instanceHandle, String name);

  /**
   * Gets a global export from an instance.
   *
   * @param instanceHandle the native instance handle
   * @param name the global name
   * @return native global handle or 0 if not found
   */
  private static native long nativeGetGlobal(long instanceHandle, String name);

  /**
   * Checks if an instance has an export with the given name.
   *
   * @param instanceHandle the native instance handle
   * @param name the export name
   * @return true if the export exists
   */
  private static native boolean nativeHasExport(long instanceHandle, String name);

  /**
   * Destroys a native instance.
   *
   * @param instanceHandle the native instance handle
   */
  private static native void nativeDestroyInstance(long instanceHandle);
}
