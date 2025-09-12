/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly table interface.
 *
 * <p>This implementation provides direct access to WebAssembly table operations through the Panama
 * Foreign Function API, offering zero-copy element access and high-performance table operations.
 *
 * <p>Tables in WebAssembly contain a homogeneous array of references that can be accessed by index.
 * This implementation supports all standard table operations including size queries, element
 * access, and table growth with comprehensive bounds checking and type safety.
 *
 * @since 1.0.0
 */
public final class PanamaTable implements WasmTable, AutoCloseable {
  private static final Logger logger = Logger.getLogger(PanamaTable.class.getName());

  private final MemorySegment tableHandle;
  private final ArenaResourceManager arenaManager;
  private final PanamaInstance parentInstance;
  private final NativeFunctionBindings nativeBindings;
  // Error handler is static, no field needed

  private volatile boolean closed = false;

  public boolean isValid() {
    return !closed;
  }

  /**
   * Creates a new Panama table wrapper with the given native table handle.
   *
   * @param tableHandle the native table handle from Wasmtime
   * @param arenaManager the arena resource manager for memory lifecycle
   * @param parentInstance the parent instance for context
   * @throws WasmException if table initialization fails
   * @throws IllegalArgumentException if any parameter is null
   */
  PanamaTable(
      final MemorySegment tableHandle,
      final ArenaResourceManager arenaManager,
      final PanamaInstance parentInstance)
      throws WasmException {
    this(tableHandle, arenaManager, parentInstance, NativeFunctionBindings.getInstance());
  }

  /**
   * Creates a new Panama table wrapper with the given native table handle and bindings.
   * This constructor is primarily used for testing to allow dependency injection.
   *
   * @param tableHandle the native table handle from Wasmtime
   * @param arenaManager the arena resource manager for memory lifecycle
   * @param parentInstance the parent instance for context
   * @param nativeBindings the native function bindings for FFI calls
   * @throws WasmException if table initialization fails
   * @throws IllegalArgumentException if any parameter is null
   */
  PanamaTable(
      final MemorySegment tableHandle,
      final ArenaResourceManager arenaManager,
      final PanamaInstance parentInstance,
      final NativeFunctionBindings nativeBindings)
      throws WasmException {
    this.tableHandle = Objects.requireNonNull(tableHandle, "Table handle cannot be null");
    this.arenaManager = Objects.requireNonNull(arenaManager, "Arena manager cannot be null");
    this.parentInstance = Objects.requireNonNull(parentInstance, "Parent instance cannot be null");
    this.nativeBindings = Objects.requireNonNull(nativeBindings, "Native bindings cannot be null");

    try {
      // Register this table for automatic resource cleanup
      arenaManager.registerManagedNativeResource(this, tableHandle, this::closeNative);

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Created Panama table instance with handle: " + tableHandle);
      }
    } catch (Exception e) {
      throw PanamaErrorHandler.mapToWasmException(e, "Failed to initialize table");
    }
  }

  @Override
  public int getSize() {
    ensureNotClosed();

    try {
      final MethodHandle sizeHandle = nativeBindings.getTableSize();
      if (sizeHandle == null) {
        throw new RuntimeException("Table size function not available in native library");
      }

      final long size = (long) sizeHandle.invokeExact(tableHandle);

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Table size: " + size);
      }

      return (int) size;
    } catch (Throwable t) {
      logger.warning("Failed to get table size: " + t.getMessage());
      return 0; // Default size
    }
  }

  @Override
  public Object get(final int index) {
    ensureNotClosed();

    if (index < 0) {
      throw new IllegalArgumentException("Table index cannot be negative: " + index);
    }

    try {
      final MethodHandle getHandle = nativeBindings.getTableGet();
      if (getHandle == null) {
        throw new RuntimeException("Table get function not available in native library");
      }

      // Bounds check
      final long tableSize = getSize();
      if (index >= tableSize) {
        throw new RuntimeException("Table index out of bounds: " + index + " >= " + tableSize);
      }

      final MemorySegment elementHandle = (MemorySegment) getHandle.invokeExact(tableHandle, index);

      if (elementHandle == null || elementHandle.equals(MemorySegment.NULL)) {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("Table element at index " + index + " is null");
        }
        return null;
      }

      // Convert native element handle to appropriate Java object
      // For function references, wrap as PanamaFunction
      // For now, assuming function references (most common table element type)
      final PanamaFunction function =
          new PanamaFunction(elementHandle, arenaManager, parentInstance);

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Retrieved table element at index " + index);
      }

      return function;
    } catch (Throwable t) {
      if (t instanceof IndexOutOfBoundsException) {
        throw (IndexOutOfBoundsException) t; // Re-throw bounds exceptions per interface
      }
      logger.warning("Failed to get table element at index " + index + ": " + t.getMessage());
      return null; // Return null on failure
    }
  }

  @Override
  public void set(final int index, final Object value) {
    ensureNotClosed();

    if (index < 0) {
      throw new IllegalArgumentException("Table index cannot be negative: " + index);
    }

    try {
      final MethodHandle setHandle = nativeBindings.getTableSet();
      if (setHandle == null) {
        throw new RuntimeException("Table set function not available in native library");
      }

      // Bounds check
      final long tableSize = getSize();
      if (index >= tableSize) {
        throw new RuntimeException("Table index out of bounds: " + index + " >= " + tableSize);
      }

      // Convert Java object to native handle
      MemorySegment valueHandle = MemorySegment.NULL;
      if (value != null) {
        if (value instanceof PanamaFunction panamaFunction) {
          valueHandle = panamaFunction.getFunctionHandle();
        } else if (value instanceof WasmFunction) {
          throw new IllegalArgumentException(
              "Cannot set non-Panama function in Panama table. "
                  + "Value must be null or a PanamaFunction instance.");
        } else {
          throw new IllegalArgumentException(
              "Table elements must be WebAssembly functions or null. "
                  + "Got: "
                  + value.getClass().getName());
        }
      }

      final boolean success = (boolean) setHandle.invokeExact(tableHandle, index, valueHandle);
      if (!success) {
        throw new RuntimeException("Failed to set table element at index " + index);
      }

      if (logger.isLoggable(Level.FINE)) {
        logger.fine(
            "Set table element at index "
                + index
                + " to "
                + (value == null ? "null" : value.getClass().getSimpleName()));
      }
    } catch (Throwable t) {
      if (t instanceof IndexOutOfBoundsException) {
        throw (IndexOutOfBoundsException) t; // Re-throw bounds exceptions per interface
      }
      logger.warning("Failed to set table element at index " + index + ": " + t.getMessage());
    }
  }

  /**
   * Grows the table by the specified number of elements, initializing new elements with the given
   * value.
   *
   * @param elements the number of elements to add
   * @param initValue the initial value for new elements (can be null)
   * @return the previous size of the table, or -1 if growth failed
   */
  @Override
  public int grow(final int elements, final Object initValue) {
    ensureNotClosed();

    if (elements < 0) {
      throw new IllegalArgumentException("Growth elements cannot be negative: " + elements);
    }

    try {
      final MethodHandle growHandle = nativeBindings.getTableGrow();
      if (growHandle == null) {
        throw new RuntimeException("Table grow function not available in native library");
      }

      // Convert initial value to native handle
      MemorySegment initialHandle = MemorySegment.NULL;
      if (initValue != null) {
        if (initValue instanceof PanamaFunction panamaFunction) {
          initialHandle = panamaFunction.getFunctionHandle();
        } else if (initValue instanceof WasmFunction) {
          throw new IllegalArgumentException(
              "Cannot use non-Panama function for table growth. "
                  + "Initial value must be null or a PanamaFunction instance.");
        } else {
          throw new IllegalArgumentException(
              "Table initial value must be a WebAssembly function or null. "
                  + "Got: "
                  + initValue.getClass().getName());
        }
      }

      final long previousSize = getSize();
      final boolean success = (boolean) growHandle.invokeExact(tableHandle, elements, initValue);

      if (!success) {
        throw new RuntimeException("Failed to grow table by " + elements + " elements");
      }

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Grew table from " + previousSize + " to " + getSize() + " elements");
      }

      return (int) previousSize;
    } catch (Throwable t) {
      logger.warning("Failed to grow table by " + elements + " elements: " + t.getMessage());
      return -1; // Interface specifies -1 on failure
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Unregister from arena manager - this will call closeNative()
        arenaManager.unregisterManagedResource(this);

        if (logger.isLoggable(Level.FINE)) {
          logger.fine("Closed Panama table instance");
        }
      } catch (Exception e) {
        logger.severe("Failed to close table: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native table handle for direct FFI operations.
   *
   * <p>This method is intended for internal use by other Panama FFI components and should not be
   * used by application code.
   *
   * @return the native table handle
   * @throws IllegalStateException if the table has been closed
   */
  public MemorySegment getTableHandle() {
    ensureNotClosed();
    return tableHandle;
  }

  /**
   * Gets the maximum size of this table.
   *
   * @return the maximum size, or -1 if unlimited
   * @throws WasmException if the operation fails
   */
  @Override
  public int getMaxSize() {
    ensureNotClosed();

    try {
      // For now, return -1 (unlimited) as Wasmtime typically allows unlimited growth
      // In the future, this could be implemented by querying table type information
      return -1; // -1 means unlimited per interface
    } catch (Exception e) {
      logger.warning("Failed to get table maximum size: " + e.getMessage());
      return -1; // Default to unlimited
    }
  }

  /**
   * Gets the element type of this table.
   *
   * @return the element type name (e.g., "funcref", "externref")
   * @throws WasmException if the operation fails
   */
  @Override
  public WasmValueType getElementType() {
    ensureNotClosed();

    try {
      // For now, assume funcref as it's the most common table element type
      // In the future, this could be implemented by querying table type information
      return WasmValueType.FUNCREF;
    } catch (Exception e) {
      logger.warning("Failed to get table element type: " + e.getMessage());
      return WasmValueType.FUNCREF; // Default assumption
    }
  }

  /**
   * Checks if this table has been closed.
   *
   * @return true if the table has been closed
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Native cleanup method called by the arena manager.
   *
   * <p>This method performs the actual native resource cleanup and is called automatically by the
   * arena resource manager. It should not be called directly by application code.
   */
  private void closeNative() {
    try {
      final MethodHandle deleteHandle = nativeBindings.getTableDelete();
      if (deleteHandle != null) {
        deleteHandle.invokeExact(tableHandle);

        if (logger.isLoggable(Level.FINE)) {
          logger.fine("Released native table resources");
        }
      }
    } catch (Throwable t) {
      // Log but don't throw - cleanup methods should be resilient
      logger.log(Level.WARNING, "Failed to release native table resources: " + t.getMessage(), t);
    }
  }

  /**
   * Ensures the table is not closed, throwing an exception if it is.
   *
   * @throws IllegalStateException if the table has been closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Table has been closed and cannot be used");
    }
  }

  @Override
  public String toString() {
    if (closed) {
      return "PanamaTable{closed=true}";
    }

    try {
      return String.format(
          "PanamaTable{size=%d, elementType=%s, handle=%s}",
          getSize(), getElementType(), tableHandle);
    } catch (Exception e) {
      return String.format("PanamaTable{handle=%s, error=%s}", tableHandle, e.getMessage());
    }
  }
}
