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
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly global interface.
 *
 * <p>WebAssembly globals provide mutable or immutable values that can be accessed by both
 * WebAssembly code and host code. This implementation uses Panama FFI with Stream 1 & 2
 * infrastructure for direct memory access to global values with zero-copy operations and
 * comprehensive type safety.
 *
 * <p>Global value access includes type validation, mutability checking, and automatic conversion
 * between Java and WebAssembly value types. All operations use Arena-based resource management for
 * automatic cleanup.
 *
 * @since 1.0.0
 */
public final class PanamaGlobal implements WasmGlobal, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaGlobal.class.getName());

  // Core infrastructure from Streams 1 & 2
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final ArenaResourceManager.ManagedNativeResource globalResource;
  private final PanamaInstance parentInstance;

  // Global state and metadata
  private volatile boolean closed = false;
  private volatile Integer valueType;
  private volatile Boolean mutable;

  /**
   * Creates a new Panama global instance using Stream 1 & 2 infrastructure.
   *
   * @param globalPtr the native global pointer from export
   * @param resourceManager the arena resource manager for lifecycle management
   * @param parentInstance the parent instance that owns this global
   * @throws WasmException if the global cannot be created
   */
  public PanamaGlobal(
      final MemorySegment globalPtr,
      final ArenaResourceManager resourceManager,
      final PanamaInstance parentInstance)
      throws WasmException {
    // Defensive parameter validation
    PanamaErrorHandler.requireValidPointer(globalPtr, "globalPtr");
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.parentInstance = Objects.requireNonNull(parentInstance, "Parent instance cannot be null");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    try {
      // Create managed resource with cleanup for global
      this.globalResource =
          resourceManager.manageNativeResource(
              globalPtr, () -> destroyNativeGlobalInternal(globalPtr), "Wasmtime Global");

      // Initialize global type metadata
      initializeGlobalType();

      LOGGER.fine("Created Panama global with managed resource");

    } catch (Exception e) {
      throw new WasmException("Failed to create global wrapper", e);
    }
  }

  public boolean isValid() {
    return !closed;
  }

  @Override
  public WasmValue get() {
    ensureNotClosed();

    try {
      // Allocate memory for value result
      ArenaResourceManager.ManagedMemorySegment valueMemory =
          resourceManager.allocate(MemoryLayouts.WASM_VAL);
      MemorySegment valueSegment = valueMemory.getSegment();

      // Get global value through optimized FFI call
      MethodHandle globalGet =
          nativeFunctions.getFunction(
              "wasmtime_global_get",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // global
                  ValueLayout.ADDRESS // value (out)
                  ));

      globalGet.invoke(globalResource.getNativePointer(), valueSegment);

      // Unmarshal the value to WasmValue
      int wasmType = getValueType();
      return unmarshalWasmValue(valueSegment, wasmType);

    } catch (Throwable e) {
      LOGGER.warning("Global value get failed: " + e.getMessage());
      return WasmValue.ofI32(0); // Default value
    }
  }

  @Override
  public void set(final WasmValue value) {
    ensureNotClosed();

    // Check mutability
    if (!isMutable()) {
      throw new UnsupportedOperationException("Cannot set value of immutable global");
    }

    // Parameter validation
    Objects.requireNonNull(value, "Value cannot be null");

    try {
      // Allocate memory for value parameter
      ArenaResourceManager.ManagedMemorySegment valueMemory =
          resourceManager.allocate(MemoryLayouts.WASM_VAL);
      MemorySegment valueSegment = valueMemory.getSegment();

      // Marshal WasmValue to native format
      marshalWasmValue(value, valueSegment);

      // Set global value through optimized FFI call
      MethodHandle globalSet =
          nativeFunctions.getFunction(
              "wasmtime_global_set",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // global
                  ValueLayout.ADDRESS // value
                  ));

      globalSet.invoke(globalResource.getNativePointer(), valueSegment);

    } catch (Throwable e) {
      if (e instanceof UnsupportedOperationException) {
        throw (UnsupportedOperationException) e;
      }
      LOGGER.warning("Global value set failed: " + e.getMessage());
    }
  }

  @Override
  public WasmValueType getType() {
    try {
      ensureNotClosed();
      int wasmType = getValueType();
      return convertToWasmValueType(wasmType);
    } catch (Exception e) {
      LOGGER.warning("Failed to get global type: " + e.getMessage());
      return WasmValueType.I32; // Default
    }
  }

  @Override
  public boolean isMutable() {
    try {
      ensureNotClosed();

      if (mutable == null) {
        initializeGlobalType();
      }

      return mutable;
    } catch (Exception e) {
      LOGGER.warning("Failed to check global mutability: " + e.getMessage());
      return false; // Default to immutable
    }
  }

  /**
   * Gets the WebAssembly value type of this global.
   *
   * @return the WASM value type kind
   * @throws WasmException if type information cannot be retrieved
   */
  public int getValueType() throws WasmException {
    ensureNotClosed();

    if (valueType == null) {
      initializeGlobalType();
    }

    return valueType;
  }

  /**
   * Gets the type name of this global as a string.
   *
   * @return the type name
   * @throws WasmException if type information cannot be retrieved
   */
  public String getTypeName() throws WasmException {
    int type = getValueType();
    return MemoryLayouts.valkindToString(type);
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
        // Close the managed native resource (automatic cleanup)
        globalResource.close();

        LOGGER.fine("Closed Panama global");

      } catch (Exception e) {
        LOGGER.severe("Failed to close global: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native global handle for this global.
   *
   * @return the native global handle
   * @throws IllegalStateException if the global is closed
   */
  public MemorySegment getGlobalHandle() {
    ensureNotClosed();
    return globalResource.getNativePointer();
  }

  /**
   * Gets the parent instance that owns this global.
   *
   * @return the parent instance
   */
  public PanamaInstance getParentInstance() {
    ensureNotClosed();
    return parentInstance;
  }

  // Private helper methods for global operations

  /** Initializes global type metadata by querying the native global. */
  private void initializeGlobalType() throws WasmException {
    try {
      // Get global type through FFI
      MemorySegment globalTypePtr = getGlobalType();

      // Extract value type and mutability from global type
      this.valueType = extractValueType(globalTypePtr);
      this.mutable = extractMutability(globalTypePtr);

      LOGGER.fine(
          "Initialized global type: type="
              + MemoryLayouts.valkindToString(valueType)
              + ", mutable="
              + mutable);

    } catch (Throwable e) {
      throw new WasmException("Failed to initialize global type", e);
    }
  }

  /** Gets the global type pointer through FFI calls. */
  private MemorySegment getGlobalType() throws Throwable {
    // Call wasmtime_global_type through cached method handle
    MethodHandle globalType =
        nativeFunctions.getFunction(
            "wasmtime_global_type",
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, ValueLayout.ADDRESS // global
                ));

    MemorySegment typePtr = (MemorySegment) globalType.invoke(globalResource.getNativePointer());
    PanamaErrorHandler.requireValidPointer(typePtr, "global type pointer");

    return typePtr;
  }

  /** Extracts value type from global type structure. */
  private int extractValueType(final MemorySegment globalTypePtr) throws Exception {
    // Read value type from global type structure using WASM_GLOBALTYPE layout
    return (Integer) MemoryLayouts.WASM_GLOBALTYPE_CONTENT.get(globalTypePtr);
  }

  /** Extracts mutability from global type structure. */
  private boolean extractMutability(final MemorySegment globalTypePtr) throws Exception {
    // Read mutability from global type structure using WASM_GLOBALTYPE layout
    int mutabilityValue = (Integer) MemoryLayouts.WASM_GLOBALTYPE_MUTABILITY.get(globalTypePtr);
    return mutabilityValue != 0; // Non-zero means mutable
  }

  /** Converts int WASM type to WasmValueType enum. */
  private WasmValueType convertToWasmValueType(final int wasmType) {
    return switch (wasmType) {
      case MemoryLayouts.WASM_I32 -> WasmValueType.I32;
      case MemoryLayouts.WASM_I64 -> WasmValueType.I64;
      case MemoryLayouts.WASM_F32 -> WasmValueType.F32;
      case MemoryLayouts.WASM_F64 -> WasmValueType.F64;
      case MemoryLayouts.WASM_ANYREF -> WasmValueType.EXTERNREF;
      case MemoryLayouts.WASM_FUNCREF -> WasmValueType.FUNCREF;
      default -> WasmValueType.I32; // Default
    };
  }

  /** Unmarshals a WebAssembly value to WasmValue object. */
  private WasmValue unmarshalWasmValue(final MemorySegment valueSlot, final int wasmType) {
    return switch (wasmType) {
      case MemoryLayouts.WASM_I32 -> WasmValue.i32((Integer) MemoryLayouts.WASM_VAL_I32.get(valueSlot));
      case MemoryLayouts.WASM_I64 -> WasmValue.i64((Long) MemoryLayouts.WASM_VAL_I64.get(valueSlot));
      case MemoryLayouts.WASM_F32 -> WasmValue.f32((Float) MemoryLayouts.WASM_VAL_F32.get(valueSlot));
      case MemoryLayouts.WASM_F64 -> WasmValue.f64((Double) MemoryLayouts.WASM_VAL_F64.get(valueSlot));
      case MemoryLayouts.WASM_ANYREF -> WasmValue.externref(null);
      case MemoryLayouts.WASM_FUNCREF -> WasmValue.funcref(null);
      default -> WasmValue.i32(0); // Default
    };
  }

  /** Marshals a WasmValue to native WebAssembly value format. */
  private void marshalWasmValue(final WasmValue wasmValue, final MemorySegment valueSlot) {
    switch (wasmValue.getType()) {
      case I32:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_I32);
        MemoryLayouts.WASM_VAL_I32.set(valueSlot, wasmValue.asI32());
        break;
      case I64:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_I64);
        MemoryLayouts.WASM_VAL_I64.set(valueSlot, wasmValue.asI64());
        break;
      case F32:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_F32);
        MemoryLayouts.WASM_VAL_F32.set(valueSlot, wasmValue.asF32());
        break;
      case F64:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_F64);
        MemoryLayouts.WASM_VAL_F64.set(valueSlot, wasmValue.asF64());
        break;
      case EXTERNREF:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_ANYREF);
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
        break;
      case FUNCREF:
        MemoryLayouts.WASM_VAL_KIND.set(valueSlot, MemoryLayouts.WASM_FUNCREF);
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
        break;
      default:
        throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmValue.getType());
    }
  }

  /** Marshals a Java value to WebAssembly value format. */
  private void marshalValue(
      final Object javaValue, final int wasmType, final MemorySegment valueSlot) {
    // Set the value type kind
    MemoryLayouts.WASM_VAL_KIND.set(valueSlot, wasmType);

    // Marshal the value based on type
    switch (wasmType) {
      case MemoryLayouts.WASM_I32:
        int i32Value = convertToI32(javaValue);
        MemoryLayouts.WASM_VAL_I32.set(valueSlot, i32Value);
        break;

      case MemoryLayouts.WASM_I64:
        long i64Value = convertToI64(javaValue);
        MemoryLayouts.WASM_VAL_I64.set(valueSlot, i64Value);
        break;

      case MemoryLayouts.WASM_F32:
        float f32Value = convertToF32(javaValue);
        MemoryLayouts.WASM_VAL_F32.set(valueSlot, f32Value);
        break;

      case MemoryLayouts.WASM_F64:
        double f64Value = convertToF64(javaValue);
        MemoryLayouts.WASM_VAL_F64.set(valueSlot, f64Value);
        break;

      case MemoryLayouts.WASM_ANYREF:
      case MemoryLayouts.WASM_FUNCREF:
        // Reference types - set to null for now
        MemoryLayouts.WASM_VAL_REF.set(valueSlot, MemorySegment.NULL);
        break;

      default:
        throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmType);
    }
  }

  /** Unmarshals a WebAssembly value to Java object. */
  private Object unmarshalValue(final MemorySegment valueSlot, final int wasmType) {
    // Get the value based on type
    return switch (wasmType) {
      case MemoryLayouts.WASM_I32 -> (Integer) MemoryLayouts.WASM_VAL_I32.get(valueSlot);
      case MemoryLayouts.WASM_I64 -> (Long) MemoryLayouts.WASM_VAL_I64.get(valueSlot);
      case MemoryLayouts.WASM_F32 -> (Float) MemoryLayouts.WASM_VAL_F32.get(valueSlot);
      case MemoryLayouts.WASM_F64 -> (Double) MemoryLayouts.WASM_VAL_F64.get(valueSlot);
      case MemoryLayouts.WASM_ANYREF, MemoryLayouts.WASM_FUNCREF -> {
        MemorySegment ref = (MemorySegment) MemoryLayouts.WASM_VAL_REF.get(valueSlot);
        yield ref.equals(MemorySegment.NULL) ? null : ref;
      }
      default -> throw new IllegalArgumentException("Unsupported WebAssembly type: " + wasmType);
    };
  }

  // Type conversion helpers

  private int convertToI32(final Object value) {
    if (value instanceof Integer) {
      return (Integer) value;
    } else if (value instanceof Number) {
      return ((Number) value).intValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to i32");
    }
  }

  private long convertToI64(final Object value) {
    if (value instanceof Long) {
      return (Long) value;
    } else if (value instanceof Number) {
      return ((Number) value).longValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to i64");
    }
  }

  private float convertToF32(final Object value) {
    if (value instanceof Float) {
      return (Float) value;
    } else if (value instanceof Number) {
      return ((Number) value).floatValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to f32");
    }
  }

  private double convertToF64(final Object value) {
    if (value instanceof Double) {
      return (Double) value;
    } else if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else {
      throw new IllegalArgumentException(
          "Cannot convert " + value.getClass().getSimpleName() + " to f64");
    }
  }

  /**
   * Internal cleanup method called by managed resource.
   *
   * @param globalPtr the native global pointer to destroy
   */
  private void destroyNativeGlobalInternal(final MemorySegment globalPtr) {
    try {
      // Global is typically owned by the instance, so no specific cleanup needed
      LOGGER.fine("Destroying native global: " + globalPtr);

    } catch (Exception e) {
      LOGGER.warning("Error during global cleanup: " + e.getMessage());
      // Don't throw exceptions from cleanup methods
    }
  }

  /**
   * Ensures that this global instance is not closed.
   *
   * @throws IllegalStateException if the global is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Global has been closed");
    }
  }
}
