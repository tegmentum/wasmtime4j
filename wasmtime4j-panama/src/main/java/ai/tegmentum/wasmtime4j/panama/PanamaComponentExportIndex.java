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

import ai.tegmentum.wasmtime4j.component.ComponentExportIndex;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama implementation of {@link ComponentExportIndex}.
 *
 * <p>Wraps a native pointer to a boxed {@code wasmtime::component::ComponentExportIndex}. The
 * native memory is freed when {@link #close()} is called.
 *
 * @since 1.0.0
 */
final class PanamaComponentExportIndex implements ComponentExportIndex {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentExportIndex.class.getName());

  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();

  private MemorySegment nativePtr;

  /**
   * Creates a new Panama component export index.
   *
   * @param nativePtr the native pointer to the boxed ComponentExportIndex
   */
  PanamaComponentExportIndex(final MemorySegment nativePtr) {
    if (nativePtr == null || nativePtr.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("nativePtr cannot be null");
    }
    this.nativePtr = nativePtr;
  }

  @Override
  public long getNativeHandle() {
    return nativePtr != null ? nativePtr.address() : 0;
  }

  /**
   * Gets the native pointer for this export index.
   *
   * @return the native memory segment
   */
  MemorySegment getNativePtr() {
    return nativePtr;
  }

  @Override
  public boolean isValid() {
    return nativePtr != null && !nativePtr.equals(MemorySegment.NULL);
  }

  @Override
  public void close() {
    if (nativePtr != null && !nativePtr.equals(MemorySegment.NULL)) {
      NATIVE_BINDINGS.componentExportIndexDestroy(nativePtr);
      nativePtr = null;
      LOGGER.fine("Closed component export index");
    }
  }
}
