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

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.type.ExternType;
import java.lang.foreign.MemorySegment;

/**
 * Panama implementation of an extern function value.
 *
 * @since 1.0.0
 */
final class PanamaExternFunc implements Extern {

  private final MemorySegment nativeHandle;
  private final PanamaStore store;

  PanamaExternFunc(final MemorySegment nativeHandle, final PanamaStore store) {
    this.nativeHandle = nativeHandle;
    this.store = store;
  }

  @Override
  public ExternType getType() {
    return ExternType.FUNC;
  }

  @Override
  public WasmFunction asFunction() {
    return new PanamaCallerFunction(nativeHandle, store, "extern");
  }

  MemorySegment getNativeHandle() {
    return nativeHandle;
  }
}
