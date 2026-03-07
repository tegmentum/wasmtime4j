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

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.type.TagType;
import java.util.logging.Logger;

/**
 * JNI implementation of the {@link Tag} interface.
 *
 * <p>This class wraps a native Wasmtime tag handle and provides exception tag functionality for the
 * WebAssembly exception handling proposal.
 *
 * @since 1.0.0
 */
public final class JniTag extends JniResource implements Tag {

  private static final Logger LOGGER = Logger.getLogger(JniTag.class.getName());

  private final long storeHandle;

  /**
   * Creates a new JniTag wrapping a native tag handle.
   *
   * @param nativeHandle the native tag handle
   * @param storeHandle the store handle this tag belongs to
   */
  JniTag(final long nativeHandle, final long storeHandle) {
    super(nativeHandle);
    this.storeHandle = storeHandle;
  }

  @Override
  public TagType getType(final Store store) throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance");
      }

      final JniStore jniStore = (JniStore) store;
      if (jniStore.isClosed()) {
        throw new WasmException("Store is closed");
      }
      final long currentStoreHandle = jniStore.getNativeHandle();

      // Validate store matches
      if (currentStoreHandle != storeHandle) {
        LOGGER.warning("Tag accessed with different store than it was created with");
      }

      final int[] paramTypes = nativeGetParamTypes(getNativeHandle(), currentStoreHandle);
      final int[] returnTypes = nativeGetReturnTypes(getNativeHandle(), currentStoreHandle);

      // Convert native type codes to Java types
      final ai.tegmentum.wasmtime4j.WasmValueType[] params =
          new ai.tegmentum.wasmtime4j.WasmValueType[paramTypes.length];
      for (int i = 0; i < paramTypes.length; i++) {
        params[i] = ai.tegmentum.wasmtime4j.WasmValueType.fromNativeTypeCode(paramTypes[i]);
      }

      final ai.tegmentum.wasmtime4j.WasmValueType[] returns =
          new ai.tegmentum.wasmtime4j.WasmValueType[returnTypes.length];
      for (int i = 0; i < returnTypes.length; i++) {
        returns[i] = ai.tegmentum.wasmtime4j.WasmValueType.fromNativeTypeCode(returnTypes[i]);
      }

      final ai.tegmentum.wasmtime4j.type.FunctionType funcType =
          new ai.tegmentum.wasmtime4j.type.FunctionType(params, returns);
      return TagType.create(funcType);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean equals(final Tag other, final Store store) throws WasmException {
    if (other == null) {
      return false;
    }
    if (!(other instanceof JniTag)) {
      return false;
    }
    beginOperation();
    try {
      if (!(store instanceof JniStore)) {
        throw new IllegalArgumentException("Store must be a JniStore instance");
      }

      final JniStore jniStore = (JniStore) store;
      if (jniStore.isClosed()) {
        throw new WasmException("Store is closed");
      }
      return nativeEquals(
          getNativeHandle(), ((JniTag) other).getNativeHandle(), jniStore.getNativeHandle());
    } finally {
      endOperation();
    }
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, Tags are owned by the Store. Destroying a Tag while the Store still
   * exists can corrupt the Store's internal slab state. We mark the Tag as closed but don't destroy
   * it - the Store will handle cleanup.
   */
  @Override
  protected void doClose() throws Exception {
    // Note: Do NOT call nativeDestroy here. Tags are Store-owned resources.
    // The Store will clean up all its Tags when it is destroyed.
    LOGGER.fine(
        "Tag marked as closed (handle: 0x"
            + Long.toHexString(nativeHandle)
            + "). Native resources freed with Store.");
  }

  @Override
  protected String getResourceType() {
    return "Tag";
  }

  // Native method declarations
  private static native int[] nativeGetParamTypes(long tagHandle, long storeHandle);

  private static native int[] nativeGetReturnTypes(long tagHandle, long storeHandle);

  private static native boolean nativeEquals(long tag1Handle, long tag2Handle, long storeHandle);
}
