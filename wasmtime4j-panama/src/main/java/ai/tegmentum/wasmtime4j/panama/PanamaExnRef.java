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

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.gc.ExnType;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.type.HeapType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the {@link ExnRef} interface.
 *
 * <p>This class wraps a native Wasmtime exception reference using the Panama Foreign Function API
 * for the WebAssembly exception handling proposal.
 *
 * @since 1.0.0
 */
public final class PanamaExnRef implements ExnRef {

  private static final Logger LOGGER = Logger.getLogger(PanamaExnRef.class.getName());
  private static final NativeInstanceBindings NATIVE_BINDINGS =
      NativeInstanceBindings.getInstance();

  private final MemorySegment nativeHandle;
  private final MemorySegment storeHandle;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new PanamaExnRef wrapping a native exception reference.
   *
   * @param nativeHandle the native exception reference segment
   * @param storeHandle the store segment this exception belongs to
   */
  PanamaExnRef(final MemorySegment nativeHandle, final MemorySegment storeHandle) {
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("nativeHandle cannot be null");
    }
    this.nativeHandle = nativeHandle;
    this.storeHandle = storeHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaExnRef",
            () -> {
              NATIVE_BINDINGS.exnRefDestroy(nativeHandle);
              LOGGER.fine("Destroyed exception reference");
            });
  }

  @Override
  public Tag getTag(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      if (!(store instanceof PanamaStore)) {
        throw new IllegalArgumentException("Store must be a PanamaStore instance");
      }

      final PanamaStore panamaStore = (PanamaStore) store;
      final MemorySegment tagPtr =
          NATIVE_BINDINGS.exnRefGetTag(nativeHandle, panamaStore.getNativeStore());

      if (tagPtr == null || tagPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to get tag from exception reference");
      }

      return new PanamaTag(tagPtr, panamaStore.getNativeStore());
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public WasmValue field(final Store store, final int index) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (index < 0) {
      throw new IllegalArgumentException("index must be non-negative");
    }
    resourceHandle.beginOperation();
    try {

      if (!(store instanceof PanamaStore)) {
        throw new IllegalArgumentException("Store must be a PanamaStore instance");
      }

      final PanamaStore panamaStore = (PanamaStore) store;
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment outType = arena.allocate(ValueLayout.JAVA_INT);
        final MemorySegment outValueI64 = arena.allocate(ValueLayout.JAVA_LONG);
        final MemorySegment outValueF64 = arena.allocate(ValueLayout.JAVA_DOUBLE);
        final MemorySegment outValueV128 = arena.allocate(16);

        final int result =
            NATIVE_BINDINGS.exnRefGetField(
                nativeHandle,
                panamaStore.getNativeStore(),
                index,
                outType,
                outValueI64,
                outValueF64,
                outValueV128);

        if (result != 0) {
          throw new WasmException("Failed to get field " + index + " from exception reference");
        }

        return decodeFieldValue(
            outType.get(ValueLayout.JAVA_INT, 0),
            outValueI64.get(ValueLayout.JAVA_LONG, 0),
            outValueF64.get(ValueLayout.JAVA_DOUBLE, 0),
            outValueV128);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public List<WasmValue> fields(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      if (!(store instanceof PanamaStore)) {
        throw new IllegalArgumentException("Store must be a PanamaStore instance");
      }

      final PanamaStore panamaStore = (PanamaStore) store;
      final int count =
          NATIVE_BINDINGS.exnRefFieldCount(nativeHandle, panamaStore.getNativeStore());

      if (count < 0) {
        throw new WasmException("Failed to get field count from exception reference");
      }

      final List<WasmValue> fieldValues = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        fieldValues.add(field(store, i));
      }
      return Collections.unmodifiableList(fieldValues);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ExnType ty(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      final Tag tag = getTag(store);
      return new ExnType(tag.getType(store));
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long getNativeHandle() {
    return nativeHandle.address();
  }

  /**
   * Gets the native memory segment handle.
   *
   * @return the native memory segment
   */
  MemorySegment getNativeSegment() {
    return nativeHandle;
  }

  @Override
  public boolean isValid() {
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      return NATIVE_BINDINGS.exnRefIsValid(nativeHandle, storeHandle) != 0;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long toRaw(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      if (!(store instanceof PanamaStore)) {
        throw new IllegalArgumentException("Store must be a PanamaStore instance");
      }

      final PanamaStore panamaStore = (PanamaStore) store;
      final long raw = NATIVE_BINDINGS.exnRefToRaw(nativeHandle, panamaStore.getNativeStore());
      if (raw < 0) {
        throw new WasmException("Failed to convert ExnRef to raw representation");
      }
      return raw;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean matchesTy(final Store store, final HeapType heapType) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (heapType == null) {
      throw new IllegalArgumentException("heapType cannot be null");
    }
    resourceHandle.beginOperation();
    try {

      if (!(store instanceof PanamaStore)) {
        throw new IllegalArgumentException("Store must be a PanamaStore instance");
      }

      final PanamaStore panamaStore = (PanamaStore) store;
      final int result =
          NATIVE_BINDINGS.exnRefMatchesTy(
              nativeHandle, panamaStore.getNativeStore(), heapType.ordinal());
      if (result < 0) {
        throw new WasmException("Failed to check ExnRef type match");
      }
      return result == 1;
    } finally {
      resourceHandle.endOperation();
    }
  }

  /**
   * Creates a new PanamaExnRef from a tag and field values.
   *
   * @param store the Panama store
   * @param tag the exception tag
   * @param fields the field values
   * @return a new PanamaExnRef
   * @throws WasmException if creation fails
   */
  static PanamaExnRef createExnRef(final PanamaStore store, final Tag tag, final WasmValue[] fields)
      throws WasmException {
    final int fieldCount = fields.length;

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment fieldTypes = arena.allocate(ValueLayout.JAVA_INT, fieldCount);
      final MemorySegment fieldI64Values = arena.allocate(ValueLayout.JAVA_LONG, fieldCount);
      final MemorySegment fieldF64Values = arena.allocate(ValueLayout.JAVA_DOUBLE, fieldCount);
      // V128 parallel array: each field gets 16 bytes at offset i*16
      final MemorySegment fieldV128Values = arena.allocate(16L * fieldCount);

      for (int i = 0; i < fieldCount; i++) {
        final WasmValue val = fields[i];
        switch (val.getType()) {
          case I32:
            fieldTypes.setAtIndex(ValueLayout.JAVA_INT, i, 0);
            fieldI64Values.setAtIndex(ValueLayout.JAVA_LONG, i, val.asInt());
            break;
          case I64:
            fieldTypes.setAtIndex(ValueLayout.JAVA_INT, i, 1);
            fieldI64Values.setAtIndex(ValueLayout.JAVA_LONG, i, val.asLong());
            break;
          case F32:
            fieldTypes.setAtIndex(ValueLayout.JAVA_INT, i, 2);
            fieldF64Values.setAtIndex(ValueLayout.JAVA_DOUBLE, i, val.asFloat());
            break;
          case F64:
            fieldTypes.setAtIndex(ValueLayout.JAVA_INT, i, 3);
            fieldF64Values.setAtIndex(ValueLayout.JAVA_DOUBLE, i, val.asDouble());
            break;
          case V128:
            fieldTypes.setAtIndex(ValueLayout.JAVA_INT, i, 4);
            final byte[] v128Bytes = val.asV128();
            MemorySegment.copy(
                v128Bytes, 0, fieldV128Values, ValueLayout.JAVA_BYTE, (long) i * 16, 16);
            break;
          default:
            throw new WasmException("Unsupported field type: " + val.getType());
        }
      }

      final MemorySegment tagSegment =
          MemorySegment.ofAddress(tag.getNativeHandle()).reinterpret(Long.BYTES);

      final MemorySegment handle =
          NATIVE_BINDINGS.exnRefCreate(
              store.getNativeStore(),
              tagSegment,
              fieldCount,
              fieldTypes,
              fieldI64Values,
              fieldF64Values,
              fieldV128Values);

      if (handle == null || handle.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create ExnRef");
      }
      return new PanamaExnRef(handle, store.getNativeStore());
    }
  }

  /**
   * Creates a PanamaExnRef from a raw representation.
   *
   * @param store the Panama store
   * @param raw the raw u32 value
   * @return a new PanamaExnRef, or null if raw is 0
   * @throws WasmException if creation fails
   */
  static PanamaExnRef fromRawExnRef(final PanamaStore store, final long raw) throws WasmException {
    final MemorySegment handle = NATIVE_BINDINGS.exnRefFromRaw(store.getNativeStore(), (int) raw);
    if (handle == null || handle.equals(MemorySegment.NULL)) {
      return null;
    }
    return new PanamaExnRef(handle, store.getNativeStore());
  }

  /** Closes this exception reference and releases native resources. */
  public void close() {
    resourceHandle.close();
  }

  private static WasmValue decodeFieldValue(
      final int typeCode,
      final long i64Value,
      final double f64Value,
      final MemorySegment v128Buffer)
      throws WasmException {
    switch (typeCode) {
      case 0:
        return WasmValue.i32((int) i64Value);
      case 1:
        return WasmValue.i64(i64Value);
      case 2:
        return WasmValue.f32((float) f64Value);
      case 3:
        return WasmValue.f64(f64Value);
      case 4:
        final byte[] v128Bytes = new byte[16];
        MemorySegment.copy(v128Buffer, ValueLayout.JAVA_BYTE, 0, v128Bytes, 0, 16);
        return WasmValue.v128(v128Bytes);
      default:
        throw new WasmException("Unsupported field value type code: " + typeCode);
    }
  }
}
