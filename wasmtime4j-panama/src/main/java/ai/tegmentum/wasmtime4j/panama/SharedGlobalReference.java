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

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A shared reference to a WebAssembly global for cross-module sharing.
 *
 * <p>This class provides a lightweight reference to a global that can be passed between different
 * WebAssembly modules while maintaining type safety and mutability constraints. It uses weak
 * references to avoid preventing garbage collection of the underlying global.
 *
 * <p>Shared references are thread-safe and can be used concurrently from multiple modules. They
 * provide the same interface as regular globals but delegate operations to the underlying shared
 * global.
 *
 * @since 1.0.0
 */
public final class SharedGlobalReference implements WasmGlobal, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(SharedGlobalReference.class.getName());

  // Weak reference to avoid preventing GC of the original global
  private final WeakReference<PanamaGlobal> globalRef;

  // Cached metadata for when original global is GC'd
  private final int wasmType;
  private final boolean mutable;
  private final WasmValueType valueType;
  private final String typeName;

  // Resource management
  private final ArenaResourceManager resourceManager;
  private volatile boolean closed = false;

  /**
   * Creates a shared reference to a global.
   *
   * @param global the global to create a reference to
   * @param wasmType the WebAssembly type constant
   * @param mutable whether the global is mutable
   * @param initialValue the initial value (for metadata caching)
   * @param resourceManager the resource manager
   */
  SharedGlobalReference(
      final PanamaGlobal global,
      final int wasmType,
      final boolean mutable,
      final WasmValue initialValue,
      final ArenaResourceManager resourceManager) {
    this.globalRef = new WeakReference<>(Objects.requireNonNull(global, "global cannot be null"));
    this.wasmType = wasmType;
    this.mutable = mutable;
    this.valueType = Objects.requireNonNull(initialValue, "initialValue cannot be null").getType();
    this.typeName = MemoryLayouts.valkindToString(wasmType);
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "resourceManager cannot be null");

    LOGGER.fine("Created shared global reference: type=" + typeName + ", mutable=" + mutable);
  }

  @Override
  public WasmValue get() {
    ensureNotClosed();

    PanamaGlobal global = globalRef.get();
    if (global == null || global.isClosed()) {
      throw new IllegalStateException(
          "Shared global reference is no longer valid - original global was garbage collected or"
              + " closed");
    }

    return global.get();
  }

  @Override
  public void set(final WasmValue value) {
    ensureNotClosed();
    Objects.requireNonNull(value, "value cannot be null");

    if (!mutable) {
      throw new UnsupportedOperationException("Cannot set value of immutable global reference");
    }

    PanamaGlobal global = globalRef.get();
    if (global == null || global.isClosed()) {
      throw new IllegalStateException(
          "Shared global reference is no longer valid - original global was garbage collected or"
              + " closed");
    }

    // Validate type compatibility
    if (value.getType() != valueType) {
      throw new IllegalArgumentException(
          String.format("Type mismatch: expected %s, got %s", valueType, value.getType()));
    }

    global.set(value);
  }

  @Override
  public WasmValueType getType() {
    ensureNotClosed();
    return valueType;
  }

  @Override
  public boolean isMutable() {
    ensureNotClosed();
    return mutable;
  }

  /**
   * Gets the WebAssembly type constant.
   *
   * @return the WASM type constant
   */
  public int getWasmType() {
    ensureNotClosed();
    return wasmType;
  }

  /**
   * Gets the type name as a string.
   *
   * @return the type name
   */
  public String getTypeName() {
    ensureNotClosed();
    return typeName;
  }

  /**
   * Checks if the referenced global is still valid and accessible.
   *
   * @return true if the reference is still valid
   */
  public boolean isValid() {
    if (closed) {
      return false;
    }

    PanamaGlobal global = globalRef.get();
    return global != null && !global.isClosed();
  }

  /**
   * Gets the underlying global if still available. This should be used carefully as it breaks the
   * weak reference pattern.
   *
   * @return the underlying global, or null if no longer available
   */
  public PanamaGlobal getUnderlyingGlobal() {
    ensureNotClosed();
    return globalRef.get();
  }

  /**
   * Performs a zero-copy get operation if the underlying global supports it.
   *
   * @return the raw numeric value, or null if not supported
   * @throws WasmException if zero-copy get fails
   */
  public Object getZeroCopy() throws WasmException {
    ensureNotClosed();

    PanamaGlobal global = globalRef.get();
    if (global == null || global.isClosed()) {
      throw new IllegalStateException("Shared global reference is no longer valid");
    }

    return global.getZeroCopy();
  }

  /**
   * Performs a zero-copy set operation if the underlying global supports it.
   *
   * @param rawValue the raw numeric value to set
   * @throws WasmException if zero-copy set fails
   */
  public void setZeroCopy(final Object rawValue) throws WasmException {
    ensureNotClosed();

    if (!mutable) {
      throw new UnsupportedOperationException("Cannot set value of immutable global reference");
    }

    PanamaGlobal global = globalRef.get();
    if (global == null || global.isClosed()) {
      throw new IllegalStateException("Shared global reference is no longer valid");
    }

    global.setZeroCopy(rawValue);
  }

  /**
   * Checks if the underlying global supports direct access for zero-copy operations.
   *
   * @return true if direct access is supported
   */
  public boolean supportsDirectAccess() {
    ensureNotClosed();

    PanamaGlobal global = globalRef.get();
    if (global == null || global.isClosed()) {
      return false;
    }

    try {
      return global.supportsDirectAccess();
    } catch (Exception e) {
      LOGGER.warning("Failed to check direct access support: " + e.getMessage());
      return false;
    }
  }

  /**
   * Creates a direct access handle if supported.
   *
   * @return direct access handle, or null if not supported
   * @throws WasmException if direct access creation fails
   */
  public PanamaGlobal.DirectGlobalAccess getDirectAccess() throws WasmException {
    ensureNotClosed();

    PanamaGlobal global = globalRef.get();
    if (global == null || global.isClosed()) {
      throw new IllegalStateException("Shared global reference is no longer valid");
    }

    return global.getDirectAccess();
  }

  /**
   * Gets comprehensive metadata about the referenced global.
   *
   * @return global metadata, or cached metadata if original global is unavailable
   */
  public PanamaGlobal.GlobalMetadata getMetadata() {
    ensureNotClosed();

    PanamaGlobal global = globalRef.get();
    if (global != null && !global.isClosed()) {
      try {
        return global.getMetadata();
      } catch (Exception e) {
        LOGGER.warning("Failed to get metadata from original global: " + e.getMessage());
      }
    }

    // Return cached metadata if original global is unavailable
    WasmValue defaultValue = createDefaultValue();
    return new PanamaGlobal.GlobalMetadata(wasmType, typeName, mutable, defaultValue);
  }

  /**
   * Creates a default value for the global's type.
   *
   * @return default value
   */
  private WasmValue createDefaultValue() {
    return switch (valueType) {
      case I32 -> WasmValue.i32(0);
      case I64 -> WasmValue.i64(0L);
      case F32 -> WasmValue.f32(0.0f);
      case F64 -> WasmValue.f64(0.0);
      case V128 -> WasmValue.v128(new byte[16]);
      case EXTERNREF -> WasmValue.externref(null);
      case FUNCREF -> WasmValue.funcref(null);
    };
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
        // Clear the weak reference
        globalRef.clear();

        LOGGER.fine("Closed shared global reference");
      } catch (Exception e) {
        LOGGER.warning("Error during shared reference closure: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Checks if this shared reference is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Ensures this shared reference is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Shared global reference has been closed");
    }
  }

  @Override
  public String toString() {
    if (closed) {
      return "SharedGlobalReference{closed=true}";
    }

    boolean valid = isValid();
    return String.format(
        "SharedGlobalReference{type=%s, mutable=%s, valid=%s}", typeName, mutable, valid);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    SharedGlobalReference that = (SharedGlobalReference) obj;
    return wasmType == that.wasmType
        && mutable == that.mutable
        && Objects.equals(valueType, that.valueType)
        && Objects.equals(globalRef.get(), that.globalRef.get());
  }

  @Override
  public int hashCode() {
    PanamaGlobal global = globalRef.get();
    return Objects.hash(wasmType, mutable, valueType, global);
  }
}
