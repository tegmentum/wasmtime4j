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
package ai.tegmentum.wasmtime4j.type;

/**
 * Base interface for WebAssembly types.
 *
 * <p>This interface represents the various types in WebAssembly including function types, memory
 * types, global types, and table types.
 *
 * @since 1.0.0
 */
public interface WasmType {

  /**
   * Gets the kind of this WebAssembly type.
   *
   * @return the type kind
   */
  WasmTypeKind getKind();
}
