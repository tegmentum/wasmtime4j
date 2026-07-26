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

import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.spi.CallerContextProvider;

/**
 * JNI implementation of CallerContextProvider.
 *
 * <p>Looks up the current caller in this order:
 *
 * <ol>
 *   <li>{@link JniLinker#currentCaller()} — the ThreadLocal set by {@code
 *       invokeHostFunctionCallback} for Linker-defined host functions (this is the live path
 *       connected to the wasmtime {@code Caller<'_, StoreData>} borrow since wasmtime4j 1.6.0).
 *   <li>{@link JniHostFunction#getCurrentCaller()} — the pre-existing ThreadLocal set by {@code
 *       JniHostFunction.hostFunctionCallback} for store-created host functions.
 * </ol>
 *
 * @since 1.0.0
 */
public final class JniCallerContextProvider implements CallerContextProvider {

  @Override
  public <T> Caller<T> getCurrentCaller() {
    final Caller<T> linkerCaller = JniLinker.currentCaller();
    if (linkerCaller != null) {
      return linkerCaller;
    }
    final Caller<T> storeCaller = JniHostFunction.getCurrentCaller();
    if (storeCaller != null) {
      return storeCaller;
    }
    throw new UnsupportedOperationException(
        "Caller context not available - this thread is not inside a caller-aware host callback");
  }
}
