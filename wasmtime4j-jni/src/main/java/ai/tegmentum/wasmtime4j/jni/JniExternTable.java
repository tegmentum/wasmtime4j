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

import ai.tegmentum.wasmtime4j.Extern;
import ai.tegmentum.wasmtime4j.ExternType;
import ai.tegmentum.wasmtime4j.WasmTable;

/**
 * JNI implementation of an extern table value.
 *
 * @since 1.0.0
 */
final class JniExternTable implements Extern {

  private final long nativeHandle;
  private final JniStore store;

  JniExternTable(final long nativeHandle, final JniStore store) {
    this.nativeHandle = nativeHandle;
    this.store = store;
  }

  @Override
  public ExternType getType() {
    return ExternType.TABLE;
  }

  @Override
  public WasmTable asTable() {
    return new JniTable(nativeHandle, store);
  }

  long getNativeHandle() {
    return nativeHandle;
  }
}
