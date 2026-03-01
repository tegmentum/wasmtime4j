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
 * Kinds of WebAssembly types.
 *
 * <p>This enum identifies the different categories of WebAssembly types that can be imported or
 * exported.
 *
 * @since 1.0.0
 */
public enum WasmTypeKind {
  /** A WebAssembly function type. */
  FUNCTION,

  /** A WebAssembly global type. */
  GLOBAL,

  /** A WebAssembly memory type. */
  MEMORY,

  /** A WebAssembly table type. */
  TABLE,

  /**
   * A WebAssembly tag type (exception handling proposal).
   *
   * @since 1.1.0
   */
  TAG
}
