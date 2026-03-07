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
import ai.tegmentum.wasmtime4j.exception.WasmTypeException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Global that is bound to an instance export.
 *
 * <p>This class represents a global that is retrieved from a WebAssembly instance and uses
 * instance-specific methods for getting and setting values rather than standalone global handles.
 *
 * @since 1.0.0
 */
final class PanamaInstanceGlobal implements WasmGlobal, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaInstanceGlobal.class.getName());

  private static final NativeInstanceBindings NATIVE_BINDINGS =
      NativeInstanceBindings.getInstance();

  private final PanamaInstance instance;
  private final PanamaStore store;
  private final String name;
  private final WasmValueType type;
  private final boolean mutable;
  private final Arena arena;
  private final NativeResourceHandle resourceHandle;

  /**
   * Package-private constructor for wrapping a global export from an instance.
   *
   * @param instance the instance containing this global
   * @param store the store context
   * @param name the name of the global export
   * @param type the value type of this global
   * @param mutable whether this global is mutable
   */
  PanamaInstanceGlobal(
      final PanamaInstance instance,
      final PanamaStore store,
      final String name,
      final WasmValueType type,
      final boolean mutable) {
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null");
    }

    this.instance = instance;
    this.store = store;
    this.name = name;
    this.type = type;
    this.mutable = mutable;
    this.arena = Arena.ofShared();
    final Arena capturedArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaInstanceGlobal",
            () -> {
              if (arena != null && arena.scope().isAlive()) {
                arena.close();
              }

              LOGGER.fine("Closed instance global: " + name);
            },
            this,
            () -> {
              if (capturedArena != null && capturedArena.scope().isAlive()) {
                capturedArena.close();
              }
            });

    LOGGER.fine(
        "Created instance global '" + name + "' with type: " + type + ", mutable: " + mutable);
  }

  @Override
  public WasmValue get() {
    resourceHandle.beginOperation();
    try {

      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment nameSegment =
            tempArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);
        final MemorySegment i32Value = tempArena.allocate(ValueLayout.JAVA_INT);
        final MemorySegment i64Value = tempArena.allocate(ValueLayout.JAVA_LONG);
        final MemorySegment f32Value = tempArena.allocate(ValueLayout.JAVA_DOUBLE);
        final MemorySegment f64Value = tempArena.allocate(ValueLayout.JAVA_DOUBLE);
        final MemorySegment refIdPresent = tempArena.allocate(ValueLayout.JAVA_INT);
        final MemorySegment refId = tempArena.allocate(ValueLayout.JAVA_LONG);

        final int result =
            NATIVE_BINDINGS.instanceGetGlobalValue(
                instance.getNativeInstance(),
                store.getNativeStore(),
                nameSegment,
                i32Value,
                i64Value,
                f32Value,
                f64Value,
                refIdPresent,
                refId);

        if (result != 0) {
          throw new RuntimeException(
              "Failed to get global value: " + PanamaErrorMapper.getErrorDescription(result));
        }

        // Convert based on type
        switch (type) {
          case I32:
            return WasmValue.i32(i32Value.get(ValueLayout.JAVA_INT, 0));
          case I64:
            return WasmValue.i64(i64Value.get(ValueLayout.JAVA_LONG, 0));
          case F32:
            return WasmValue.f32((float) f32Value.get(ValueLayout.JAVA_DOUBLE, 0));
          case F64:
            return WasmValue.f64(f64Value.get(ValueLayout.JAVA_DOUBLE, 0));
          case FUNCREF:
            if (refIdPresent.get(ValueLayout.JAVA_INT, 0) != 0) {
              return WasmValue.funcref(refId.get(ValueLayout.JAVA_LONG, 0));
            }
            return WasmValue.funcref(null);
          case EXTERNREF:
            if (refIdPresent.get(ValueLayout.JAVA_INT, 0) != 0) {
              return WasmValue.externref(refId.get(ValueLayout.JAVA_LONG, 0));
            }
            return WasmValue.externref(null);
          default:
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void set(final WasmValue value) {
    resourceHandle.beginOperation();
    try {

      if (value == null) {
        throw new IllegalArgumentException("Value cannot be null");
      }
      if (!mutable) {
        throw new UnsupportedOperationException("Cannot set value of immutable global");
      }
      if (value.getType() != type) {
        throw new WasmTypeException(
            "Type mismatch: cannot set " + value.getType() + " value on " + type + " global");
      }

      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment nameSegment =
            tempArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);

        // Extract value based on type
        int valueTypeCode = type.toNativeTypeCode();
        int i32Value = 0;
        long i64Value = 0L;
        double f32Value = 0.0;
        double f64Value = 0.0;
        int refIdPresent = 0;
        long refId = 0L;

        switch (type) {
          case I32:
            i32Value = value.asInt();
            break;
          case I64:
            i64Value = value.asLong();
            break;
          case F32:
            f32Value = value.asFloat();
            break;
          case F64:
            f64Value = value.asDouble();
            break;
          case FUNCREF:
          case EXTERNREF:
            final Object refValue = value.getValue();
            if (refValue != null) {
              refIdPresent = 1;
              if (refValue
                  instanceof ai.tegmentum.wasmtime4j.func.FunctionReference funcReference) {
                refId = funcReference.getId();
              } else if (refValue instanceof Long) {
                refId = (Long) refValue;
              } else if (refValue instanceof Integer) {
                refId = ((Integer) refValue).longValue();
              } else {
                throw new IllegalArgumentException(
                    "Unsupported reference value type: "
                        + refValue.getClass().getName()
                        + ". Expected FunctionReference, Long, or Integer.");
              }
            }
            break;
          default:
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }

        final int result =
            NATIVE_BINDINGS.instanceSetGlobalValue(
                instance.getNativeInstance(),
                store.getNativeStore(),
                nameSegment,
                valueTypeCode,
                i32Value,
                i64Value,
                f32Value,
                f64Value,
                refIdPresent,
                refId);

        if (result != 0) {
          throw new RuntimeException(
              "Failed to set global value: " + PanamaErrorMapper.getErrorDescription(result));
        }
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public WasmValueType getType() {
    return type;
  }

  @Override
  public boolean isMutable() {
    return mutable;
  }

  @Override
  public ai.tegmentum.wasmtime4j.type.GlobalType getGlobalType() {
    resourceHandle.beginOperation();
    try {
      final WasmValueType valueType = getType();
      final boolean mutableFlag = isMutable();
      return new ai.tegmentum.wasmtime4j.type.GlobalType() {
        @Override
        public WasmValueType getValueType() {
          return valueType;
        }

        @Override
        public boolean isMutable() {
          return mutableFlag;
        }

        @Override
        public ai.tegmentum.wasmtime4j.type.WasmTypeKind getKind() {
          return ai.tegmentum.wasmtime4j.type.WasmTypeKind.GLOBAL;
        }
      };
    } finally {
      resourceHandle.endOperation();
    }
  }

  /**
   * Gets the native global pointer by looking up the global export from the instance.
   *
   * <p>This method allows instance globals to be used with the linker's defineGlobal method. It
   * returns a properly wrapped Global struct that is compatible with the linker's define
   * operations.
   *
   * @return the native global pointer, or NULL if the global cannot be found
   */
  MemorySegment getGlobalPointer() {
    resourceHandle.beginOperation();
    try {

      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment nameSegment =
            tempArena.allocateFrom(name, java.nio.charset.StandardCharsets.UTF_8);

        // Use the wrapped version that returns a properly wrapped Global struct
        // compatible with panamaLinkerDefineGlobal
        final MemorySegment globalPtr =
            NATIVE_BINDINGS.instanceGetGlobalWrapped(
                instance.getNativeInstance(), store.getNativeStore(), nameSegment);

        if (globalPtr == null || globalPtr.equals(MemorySegment.NULL)) {
          LOGGER.warning("Failed to get native global pointer for: " + name);
          return MemorySegment.NULL;
        }

        return globalPtr;
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void close() {
    resourceHandle.close();
  }
}
