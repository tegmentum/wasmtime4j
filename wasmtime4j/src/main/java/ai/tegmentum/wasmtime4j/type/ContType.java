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

import ai.tegmentum.wasmtime4j.DefaultContType;

/**
 * Represents the type of a WebAssembly continuation.
 *
 * <p>ContType wraps a {@link FunctionType} to describe the signature of a continuation. The
 * function type defines the parameter and result types of the continuation's suspended computation.
 *
 * <p>This is part of the WebAssembly stack switching (typed continuations) proposal. A continuation
 * type describes what values a continuation expects when resumed and what values it produces when
 * it suspends or completes.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a continuation type with an i32 parameter and i64 result
 * FunctionType funcType = FunctionType.of(
 *     List.of(WasmValueType.I32),
 *     List.of(WasmValueType.I64)
 * );
 * ContType contType = ContType.create(funcType);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ContType {

  /**
   * Creates a new continuation type from a function type.
   *
   * <p>The function type defines the signature of the continuation: its parameters are the types of
   * values the continuation expects when resumed, and its results are the types of values produced
   * when it suspends or completes.
   *
   * @param funcType the function type describing the continuation signature
   * @return a new ContType instance
   * @throws IllegalArgumentException if funcType is null
   */
  static ContType create(final FunctionType funcType) {
    return new DefaultContType(funcType);
  }

  /**
   * Returns the underlying function type of this continuation type.
   *
   * <p>The function type describes the continuation's signature: parameters represent the values
   * expected on resume, and results represent the values produced on suspend or complete.
   *
   * @return the FunctionType describing this continuation's signature
   */
  FunctionType getFunctionType();
}
