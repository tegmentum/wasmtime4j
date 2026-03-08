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

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for {@link FuncType} implementations.
 *
 * <p>This class provides shared storage, validation, getters, {@code equals()}, {@code hashCode()},
 * and {@code toString()} for parameter and result type lists. Subclasses provide runtime-specific
 * construction and native interop.
 *
 * @since 1.0.0
 */
public abstract class AbstractFuncType implements FuncType {

  private final List<WasmValueType> params;
  private final List<WasmValueType> results;

  /**
   * Creates a new AbstractFuncType with validated parameter and result types.
   *
   * @param params the parameter types (must not be null, elements must not be null)
   * @param results the result types (must not be null, elements must not be null)
   * @throws IllegalArgumentException if params or results is null, or contains null elements
   */
  protected AbstractFuncType(final List<WasmValueType> params, final List<WasmValueType> results) {
    Validation.requireNonNull(params, "params");
    Validation.requireNonNull(results, "results");

    for (int i = 0; i < params.size(); i++) {
      if (params.get(i) == null) {
        throw new IllegalArgumentException("Parameter type at index " + i + " is null");
      }
    }
    for (int i = 0; i < results.size(); i++) {
      if (results.get(i) == null) {
        throw new IllegalArgumentException("Result type at index " + i + " is null");
      }
    }

    this.params = Collections.unmodifiableList(new ArrayList<>(params));
    this.results = Collections.unmodifiableList(new ArrayList<>(results));
  }

  @Override
  public List<WasmValueType> getParams() {
    return params;
  }

  @Override
  public List<WasmValueType> getResults() {
    return results;
  }

  @Override
  public WasmTypeKind getKind() {
    return WasmTypeKind.FUNCTION;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FuncType)) {
      return false;
    }

    final FuncType other = (FuncType) obj;
    return params.equals(other.getParams()) && results.equals(other.getResults());
  }

  @Override
  public int hashCode() {
    return Objects.hash(params, results);
  }

  @Override
  public String toString() {
    return String.format("FuncType{params=%s, results=%s}", params, results);
  }
}
