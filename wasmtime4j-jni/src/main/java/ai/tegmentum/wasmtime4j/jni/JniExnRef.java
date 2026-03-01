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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.gc.ExnType;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.type.HeapType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * JNI implementation of the {@link ExnRef} interface.
 *
 * <p>This class wraps a native Wasmtime exception reference handle for the WebAssembly exception
 * handling proposal. Exception references are rooted GC references that can be thrown and caught.
 *
 * @since 1.0.0
 */
public final class JniExnRef extends JniResource implements ExnRef {

  private static final Logger LOGGER = Logger.getLogger(JniExnRef.class.getName());

  private final long storeHandle;

  /**
   * Creates a new JniExnRef wrapping a native exception reference handle.
   *
   * @param nativeHandle the native exception reference handle
   * @param storeHandle the store handle this exception belongs to
   */
  JniExnRef(final long nativeHandle, final long storeHandle) {
    super(nativeHandle);
    this.storeHandle = storeHandle;
  }

  @Override
  public Tag getTag(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }
    final long currentStoreHandle = jniStore.getNativeHandle();

    final long tagHandle = nativeGetTag(getNativeHandle(), currentStoreHandle);

    if (tagHandle == 0) {
      throw new WasmException("Failed to get tag from exception reference");
    }

    return new JniTag(tagHandle, currentStoreHandle);
  }

  @Override
  public WasmValue field(final Store store, final int index) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (index < 0) {
      throw new IllegalArgumentException("index must be non-negative");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }

    final Object[] result = nativeGetField(getNativeHandle(), jniStore.getNativeHandle(), index);

    if (result == null || result.length == 0) {
      throw new WasmException("Failed to get field " + index + " from exception reference");
    }

    return (WasmValue) result[0];
  }

  @Override
  public List<WasmValue> fields(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }

    final Object[] result = nativeGetFields(getNativeHandle(), jniStore.getNativeHandle());

    if (result == null) {
      throw new WasmException("Failed to get fields from exception reference");
    }

    final List<WasmValue> fieldValues = new ArrayList<>(result.length);
    for (final Object val : result) {
      fieldValues.add((WasmValue) val);
    }
    return Collections.unmodifiableList(fieldValues);
  }

  @Override
  public ExnType ty(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    final Tag tag = getTag(store);
    return new ExnType(tag.getType(store));
  }

  @Override
  public boolean isValid() {
    if (isClosed()) {
      return false;
    }
    return nativeIsValid(getNativeHandle(), storeHandle);
  }

  /**
   * Gets the store handle this exception belongs to.
   *
   * @return the store handle
   */
  long getStoreHandle() {
    return storeHandle;
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, ExnRefs are owned by the Store. Destroying an ExnRef while the Store
   * still exists can corrupt the Store's internal slab state. We mark the ExnRef as closed but
   * don't destroy it - the Store will handle cleanup.
   */
  @Override
  protected void doClose() throws Exception {
    // Note: Do NOT call nativeDestroy here. ExnRefs are Store-owned resources.
    // The Store will clean up all its ExnRefs when it is destroyed.
    LOGGER.fine(
        "ExnRef marked as closed (handle: 0x"
            + Long.toHexString(nativeHandle)
            + "). Native resources freed with Store.");
  }

  @Override
  protected String getResourceType() {
    return "ExnRef";
  }

  @Override
  public long toRaw(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }

    final long raw = nativeToRaw(getNativeHandle(), jniStore.getNativeHandle());
    if (raw < 0) {
      throw new WasmException("Failed to convert ExnRef to raw representation");
    }
    return raw;
  }

  @Override
  public boolean matchesTy(final Store store, final HeapType heapType) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (heapType == null) {
      throw new IllegalArgumentException("heapType cannot be null");
    }
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance");
    }

    final JniStore jniStore = (JniStore) store;
    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }

    return nativeMatchesTy(getNativeHandle(), jniStore.getNativeHandle(), heapType.ordinal());
  }

  /**
   * Creates a new JniExnRef from a tag and field values.
   *
   * @param store the JNI store
   * @param tag the exception tag
   * @param fields the field values
   * @return a new JniExnRef
   * @throws WasmException if creation fails
   */
  static JniExnRef createExnRef(final JniStore store, final Tag tag, final WasmValue[] fields)
      throws WasmException {
    final int fieldCount = fields.length;
    final int[] fieldTypes = new int[fieldCount];
    final long[] fieldI64Values = new long[fieldCount];
    final double[] fieldF64Values = new double[fieldCount];

    for (int i = 0; i < fieldCount; i++) {
      final WasmValue val = fields[i];
      switch (val.getType()) {
        case I32:
          fieldTypes[i] = 0;
          fieldI64Values[i] = val.asInt();
          break;
        case I64:
          fieldTypes[i] = 1;
          fieldI64Values[i] = val.asLong();
          break;
        case F32:
          fieldTypes[i] = 2;
          fieldF64Values[i] = val.asFloat();
          break;
        case F64:
          fieldTypes[i] = 3;
          fieldF64Values[i] = val.asDouble();
          break;
        default:
          throw new WasmException("Unsupported field type: " + val.getType());
      }
    }

    final long handle =
        nativeCreate(
            store.getNativeHandle(),
            tag.getNativeHandle(),
            fieldTypes,
            fieldI64Values,
            fieldF64Values);
    if (handle == 0) {
      throw new WasmException("Failed to create ExnRef");
    }
    return new JniExnRef(handle, store.getNativeHandle());
  }

  /**
   * Creates a JniExnRef from a raw representation.
   *
   * @param store the JNI store
   * @param raw the raw u32 value
   * @return a new JniExnRef, or null if raw is 0
   * @throws WasmException if creation fails
   */
  static JniExnRef fromRawExnRef(final JniStore store, final long raw) throws WasmException {
    final long handle = nativeFromRaw(store.getNativeHandle(), raw);
    if (handle == 0) {
      return null;
    }
    return new JniExnRef(handle, store.getNativeHandle());
  }

  // Native method declarations
  private static native long nativeGetTag(long exnRefHandle, long storeHandle);

  private static native boolean nativeIsValid(long exnRefHandle, long storeHandle);

  private static native Object[] nativeGetField(long exnRefHandle, long storeHandle, int index);

  private static native Object[] nativeGetFields(long exnRefHandle, long storeHandle);

  private static native long nativeCreate(
      long storeHandle,
      long tagHandle,
      int[] fieldTypes,
      long[] fieldI64Values,
      double[] fieldF64Values);

  private static native long nativeToRaw(long exnRefHandle, long storeHandle);

  private static native long nativeFromRaw(long storeHandle, long raw);

  private static native boolean nativeMatchesTy(
      long exnRefHandle, long storeHandle, int heapTypeCode);
}
