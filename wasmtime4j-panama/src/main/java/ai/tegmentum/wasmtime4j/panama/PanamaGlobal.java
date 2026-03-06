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
 * Panama FFI implementation of WebAssembly Global with native handle support.
 *
 * @since 1.0.0
 */
public final class PanamaGlobal implements WasmGlobal, AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PanamaGlobal.class.getName());

  private static final NativeMemoryBindings NATIVE_BINDINGS = NativeMemoryBindings.getInstance();

  private final PanamaStore store;
  private final MemorySegment nativeGlobal;
  private final Arena arena;
  private WasmValueType type;
  private boolean mutable;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama global with a native handle.
   *
   * @param type the value type of this global
   * @param mutable whether this global is mutable
   * @param initialValue the initial value
   * @param store the store context
   */
  public PanamaGlobal(
      final WasmValueType type,
      final boolean mutable,
      final WasmValue initialValue,
      final PanamaStore store) {
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null");
    }
    if (initialValue == null) {
      throw new IllegalArgumentException("Initial value cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.type = type;
    this.mutable = mutable;
    this.store = store;
    this.arena = Arena.ofShared();

    // Create native global via Panama FFI
    final MemorySegment globalPtrPtr = arena.allocate(ValueLayout.ADDRESS);
    final int result =
        NATIVE_BINDINGS.panamaGlobalCreate(
            store.getNativeStore(),
            type.toNativeTypeCode(),
            mutable ? 1 : 0,
            initialValue,
            null, // name - optional
            globalPtrPtr);

    if (result != 0) {
      arena.close();
      throw new RuntimeException(
          "Failed to create global: " + PanamaErrorMapper.getErrorDescription(result));
    }

    this.nativeGlobal = globalPtrPtr.get(ValueLayout.ADDRESS, 0);
    if (nativeGlobal == null || nativeGlobal.equals(MemorySegment.NULL)) {
      arena.close();
      throw new RuntimeException("Failed to create global: null pointer returned");
    }

    final MemorySegment globalHandle = this.nativeGlobal;
    final Arena globalArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaGlobal",
            () -> {
              if (nativeGlobal != null && !nativeGlobal.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.panamaGlobalDestroy(nativeGlobal);
              }
              arena.close();
            },
            this,
            () -> {
              if (globalHandle != null && !globalHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.panamaGlobalDestroy(globalHandle);
              }
              globalArena.close();
            });

    LOGGER.fine("Created Panama global with type: " + type + ", mutable: " + mutable);
  }

  /**
   * Package-private constructor for wrapping an existing native global.
   *
   * @param nativeGlobal the native global pointer
   * @param store the store context
   */
  PanamaGlobal(final MemorySegment nativeGlobal, final PanamaStore store) {
    if (nativeGlobal == null || nativeGlobal.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native global cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.nativeGlobal = nativeGlobal;
    this.store = store;
    this.arena = Arena.ofShared();

    // Query type and mutability from native global
    queryMetadata();

    final MemorySegment globalHandle = this.nativeGlobal;
    final Arena globalArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaGlobal",
            () -> {
              if (nativeGlobal != null && !nativeGlobal.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.panamaGlobalDestroy(nativeGlobal);
              }
              arena.close();
            },
            this,
            () -> {
              if (globalHandle != null && !globalHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.panamaGlobalDestroy(globalHandle);
              }
              globalArena.close();
            });

    LOGGER.fine("Wrapped existing Panama global");
  }

  /**
   * Package-private constructor for wrapping an existing native global with known type and
   * mutability.
   *
   * @param nativeGlobal the native global pointer
   * @param type the value type of this global
   * @param mutable whether this global is mutable
   * @param store the store context
   */
  PanamaGlobal(
      final MemorySegment nativeGlobal,
      final WasmValueType type,
      final boolean mutable,
      final PanamaStore store) {
    if (nativeGlobal == null || nativeGlobal.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native global cannot be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }

    this.nativeGlobal = nativeGlobal;
    this.type = type;
    this.mutable = mutable;
    this.store = store;
    this.arena = Arena.ofShared();

    final MemorySegment globalHandle = this.nativeGlobal;
    final Arena globalArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaGlobal",
            () -> {
              if (nativeGlobal != null && !nativeGlobal.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.panamaGlobalDestroy(nativeGlobal);
              }
              arena.close();
            },
            this,
            () -> {
              if (globalHandle != null && !globalHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.panamaGlobalDestroy(globalHandle);
              }
              globalArena.close();
            });

    LOGGER.fine("Wrapped existing Panama global with type: " + type + ", mutable: " + mutable);
  }

  @Override
  public WasmValue get() {
    ensureNotClosed();

    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment i32Value = tempArena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment i64Value = tempArena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment f32Value = tempArena.allocate(ValueLayout.JAVA_DOUBLE);
      final MemorySegment f64Value = tempArena.allocate(ValueLayout.JAVA_DOUBLE);
      final MemorySegment refIdPresent = tempArena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment refId = tempArena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment v128Bytes = tempArena.allocate(16);

      final int result =
          NATIVE_BINDINGS.panamaGlobalGet(
              nativeGlobal,
              store.getNativeStore(),
              i32Value,
              i64Value,
              f32Value,
              f64Value,
              refIdPresent,
              refId,
              v128Bytes);

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
        case V128:
          final byte[] bytes = new byte[16];
          MemorySegment.copy(v128Bytes, ValueLayout.JAVA_BYTE, 0, bytes, 0, 16);
          return WasmValue.v128(bytes);
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
        case ANYREF:
          if (refIdPresent.get(ValueLayout.JAVA_INT, 0) != 0) {
            return WasmValue.anyref(refId.get(ValueLayout.JAVA_LONG, 0));
          }
          return WasmValue.nullAnyRef();
        case EQREF:
          if (refIdPresent.get(ValueLayout.JAVA_INT, 0) != 0) {
            return WasmValue.eqref(refId.get(ValueLayout.JAVA_LONG, 0));
          }
          return WasmValue.nullEqRef();
        case I31REF:
          if (refIdPresent.get(ValueLayout.JAVA_INT, 0) != 0) {
            return WasmValue.i31ref((int) refId.get(ValueLayout.JAVA_LONG, 0));
          }
          return WasmValue.nullI31Ref();
        case STRUCTREF:
          if (refIdPresent.get(ValueLayout.JAVA_INT, 0) != 0) {
            return WasmValue.structref(refId.get(ValueLayout.JAVA_LONG, 0));
          }
          return WasmValue.nullStructRef();
        case ARRAYREF:
          if (refIdPresent.get(ValueLayout.JAVA_INT, 0) != 0) {
            return WasmValue.arrayref(refId.get(ValueLayout.JAVA_LONG, 0));
          }
          return WasmValue.nullArrayRef();
        case NULLREF:
          return WasmValue.nullRef();
        case NULLFUNCREF:
          return WasmValue.nullNullFuncRef();
        case NULLEXTERNREF:
          return WasmValue.nullNullExternRef();
        default:
          throw new WasmTypeException("Unsupported global type: " + type);
      }
    }
  }

  @Override
  public void set(final WasmValue value) {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null");
    }
    if (!mutable) {
      throw new UnsupportedOperationException("Cannot set value of immutable global");
    }
    ensureNotClosed();

    // Validate type matches
    if (value.getType() != type) {
      throw new WasmTypeException(
          "Type mismatch: cannot set " + value.getType() + " value on " + type + " global");
    }

    final int result = NATIVE_BINDINGS.panamaGlobalSet(nativeGlobal, store.getNativeStore(), value);

    if (result != 0) {
      throw new RuntimeException(
          "Failed to set global value: " + PanamaErrorMapper.getErrorDescription(result));
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
    ensureNotClosed();
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
  }

  /**
   * Gets the native global pointer.
   *
   * @return native global memory segment
   */
  public MemorySegment getNativeGlobal() {
    return nativeGlobal;
  }

  /**
   * Checks if the global has been closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return resourceHandle.isClosed();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /** Queries type and mutability metadata from the native global. */
  private void queryMetadata() {
    try (final Arena tempArena = Arena.ofConfined()) {
      final MemorySegment typeCode = tempArena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment mutabilityCode = tempArena.allocate(ValueLayout.JAVA_INT);

      final int result =
          NATIVE_BINDINGS.panamaGlobalMetadata(nativeGlobal, typeCode, mutabilityCode);

      if (result == 0) {
        this.type = mapTypeCodeToWasmValueType(typeCode.get(ValueLayout.JAVA_INT, 0));
        this.mutable = mutabilityCode.get(ValueLayout.JAVA_INT, 0) != 0;
        LOGGER.fine("Global metadata: type=" + type + ", mutable=" + mutable);
      } else {
        LOGGER.warning(
            "Failed to query global metadata: " + PanamaErrorMapper.getErrorDescription(result));
        this.type = WasmValueType.I32; // Default fallback
        this.mutable = true;
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to query global metadata: " + e.getMessage());
      this.type = WasmValueType.I32; // Default fallback
      this.mutable = true;
    }
  }

  /**
   * Maps type code to WasmValueType.
   *
   * @param typeCode the type code from native
   * @return the WasmValueType
   */
  private static WasmValueType mapTypeCodeToWasmValueType(final int typeCode) {
    switch (typeCode) {
      case 0:
        return WasmValueType.I32;
      case 1:
        return WasmValueType.I64;
      case 2:
        return WasmValueType.F32;
      case 3:
        return WasmValueType.F64;
      case 4:
        return WasmValueType.V128;
      case 5:
        return WasmValueType.FUNCREF;
      case 6:
        return WasmValueType.EXTERNREF;
      case 7:
        return WasmValueType.ANYREF;
      case 8:
        return WasmValueType.EQREF;
      case 9:
        return WasmValueType.I31REF;
      case 10:
        return WasmValueType.STRUCTREF;
      case 11:
        return WasmValueType.ARRAYREF;
      case 12:
        return WasmValueType.NULLREF;
      case 13:
        return WasmValueType.NULLFUNCREF;
      case 14:
        return WasmValueType.NULLEXTERNREF;
      default:
        LOGGER.warning("Unknown type code: " + typeCode);
        return WasmValueType.I32; // Default fallback
    }
  }

  /**
   * Ensures the global is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
