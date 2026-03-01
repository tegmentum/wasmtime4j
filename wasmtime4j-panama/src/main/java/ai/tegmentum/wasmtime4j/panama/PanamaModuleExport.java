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

import ai.tegmentum.wasmtime4j.ModuleExport;
import java.lang.foreign.MemorySegment;

/**
 * Panama FFI implementation of {@link ModuleExport}.
 *
 * <p>Wraps a native pointer to a Wasmtime ModuleExport for O(1) export lookups. The native handle
 * must be destroyed when no longer needed.
 *
 * @since 1.1.0
 */
final class PanamaModuleExport implements ModuleExport {

  private final String name;
  private final MemorySegment nativePtr;

  PanamaModuleExport(final String name, final MemorySegment nativePtr) {
    this.name = name;
    this.nativePtr = nativePtr;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public long nativeHandle() {
    return nativePtr.address();
  }

  /**
   * Gets the native memory segment pointer.
   *
   * @return the native pointer
   */
  MemorySegment getNativePtr() {
    return nativePtr;
  }
}
