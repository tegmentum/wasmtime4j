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

package ai.tegmentum.wasmtime4j.panama.debug;

import java.util.Objects;

/**
 * Represents a variable in WebAssembly debugging context using Panama FFI.
 *
 * <p>Variables can be local, parameter, global, or temporary. They have a name, type, and value
 * that can be inspected during debugging.
 *
 * @since 1.0.0
 */
public final class PanamaVariable {

  private final String name;
  private final String varType;
  private final PanamaVariableValue value;
  private final VariableScope scope;
  private final int index;
  private final boolean mutable;
  private final String description;

  /**
   * Creates a new variable.
   *
   * @param name the variable name
   * @param varType the variable type
   * @param value the variable value
   * @param scope the variable scope
   * @param index the variable index
   * @param mutable whether the variable is mutable
   * @param description optional description
   */
  public PanamaVariable(
      final String name,
      final String varType,
      final PanamaVariableValue value,
      final VariableScope scope,
      final int index,
      final boolean mutable,
      final String description) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.varType = Objects.requireNonNull(varType, "varType cannot be null");
    this.value = Objects.requireNonNull(value, "value cannot be null");
    this.scope = Objects.requireNonNull(scope, "scope cannot be null");
    this.index = index;
    this.mutable = mutable;
    this.description = description;
  }

  /**
   * Creates a local variable.
   *
   * @param name the variable name
   * @param varType the variable type
   * @param value the variable value
   * @param index the local index
   * @return a new local variable
   */
  public static PanamaVariable local(
      final String name, final String varType, final PanamaVariableValue value, final int index) {
    return new PanamaVariable(name, varType, value, VariableScope.LOCAL, index, true, null);
  }

  /**
   * Creates a parameter variable.
   *
   * @param name the parameter name
   * @param varType the parameter type
   * @param value the parameter value
   * @param index the parameter index
   * @return a new parameter variable
   */
  public static PanamaVariable parameter(
      final String name, final String varType, final PanamaVariableValue value, final int index) {
    return new PanamaVariable(name, varType, value, VariableScope.PARAMETER, index, false, null);
  }

  /**
   * Creates a global variable.
   *
   * @param name the global name
   * @param varType the global type
   * @param value the global value
   * @param index the global index
   * @param mutable whether the global is mutable
   * @return a new global variable
   */
  public static PanamaVariable global(
      final String name,
      final String varType,
      final PanamaVariableValue value,
      final int index,
      final boolean mutable) {
    return new PanamaVariable(name, varType, value, VariableScope.GLOBAL, index, mutable, null);
  }

  /**
   * Gets the variable name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the variable type.
   *
   * @return the type string
   */
  public String getVarType() {
    return varType;
  }

  /**
   * Gets the variable value.
   *
   * @return the value
   */
  public PanamaVariableValue getValue() {
    return value;
  }

  /**
   * Gets the variable scope.
   *
   * @return the scope
   */
  public VariableScope getScope() {
    return scope;
  }

  /**
   * Gets the variable index.
   *
   * @return the index
   */
  public int getIndex() {
    return index;
  }

  /**
   * Checks if the variable is mutable.
   *
   * @return true if mutable
   */
  public boolean isMutable() {
    return mutable;
  }

  /**
   * Gets the optional description.
   *
   * @return the description, or null
   */
  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "PanamaVariable{"
        + "name='"
        + name
        + '\''
        + ", type='"
        + varType
        + '\''
        + ", value="
        + value
        + ", scope="
        + scope
        + ", index="
        + index
        + ", mutable="
        + mutable
        + '}';
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PanamaVariable)) {
      return false;
    }
    final PanamaVariable other = (PanamaVariable) obj;
    return index == other.index
        && mutable == other.mutable
        && name.equals(other.name)
        && varType.equals(other.varType)
        && scope == other.scope;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, varType, scope, index);
  }

  /** Variable scope types. */
  public enum VariableScope {
    /** Local variable in a function. */
    LOCAL,
    /** Function parameter. */
    PARAMETER,
    /** Global variable. */
    GLOBAL,
    /** Imported value. */
    IMPORTED,
    /** Exported value. */
    EXPORTED,
    /** Temporary/intermediate value. */
    TEMPORARY
  }
}
