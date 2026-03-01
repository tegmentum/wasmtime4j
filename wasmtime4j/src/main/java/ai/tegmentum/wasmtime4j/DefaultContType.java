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
package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.ContType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Objects;

/**
 * Default implementation of {@link ContType}.
 *
 * <p>This class wraps a {@link FunctionType} to provide continuation type functionality for the
 * WebAssembly stack switching (typed continuations) proposal.
 *
 * @since 1.0.0
 */
public final class DefaultContType implements ContType {

  private final FunctionType funcType;

  /**
   * Creates a new DefaultContType wrapping the given function type.
   *
   * @param funcType the function type to wrap
   * @throws IllegalArgumentException if funcType is null
   */
  public DefaultContType(final FunctionType funcType) {
    if (funcType == null) {
      throw new IllegalArgumentException("funcType cannot be null");
    }
    this.funcType = funcType;
  }

  @Override
  public FunctionType getFunctionType() {
    return funcType;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ContType)) {
      return false;
    }
    final ContType other = (ContType) obj;
    return funcType.equals(other.getFunctionType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(funcType);
  }

  @Override
  public String toString() {
    return "ContType[" + funcType + "]";
  }
}
