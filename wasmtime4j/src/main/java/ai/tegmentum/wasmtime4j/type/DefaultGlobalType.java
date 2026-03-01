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
import java.util.Objects;

/**
 * Default implementation of {@link GlobalType}.
 *
 * <p>This is a simple value-type implementation for use in the API module when no native handle is
 * needed.
 *
 * @since 1.1.0
 */
final class DefaultGlobalType implements GlobalType {

  private final WasmValueType valueType;
  private final boolean mutable;

  /**
   * Creates a new DefaultGlobalType.
   *
   * @param valueType the value type
   * @param mutability the mutability
   */
  DefaultGlobalType(final WasmValueType valueType, final Mutability mutability) {
    this.valueType = valueType;
    this.mutable = mutability.isVar();
  }

  @Override
  public WasmValueType getValueType() {
    return valueType;
  }

  @Override
  public boolean isMutable() {
    return mutable;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GlobalType)) {
      return false;
    }
    final GlobalType other = (GlobalType) obj;
    return this.valueType == other.getValueType() && this.mutable == other.isMutable();
  }

  @Override
  public int hashCode() {
    return Objects.hash(valueType, mutable);
  }

  @Override
  public String toString() {
    return "GlobalType(" + valueType + ", " + (mutable ? "var" : "const") + ")";
  }
}
