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
package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.type.FuncType;

/**
 * Information about a function in a WebAssembly module.
 *
 * <p>This class provides metadata about functions including their index, name, type, and whether
 * they are imported or defined locally.
 *
 * @since 1.1.0
 */
public final class FunctionInfo {

  private final int index;
  private final String name;
  private final FuncType funcType;
  private final boolean isImport;

  /**
   * Creates a new FunctionInfo instance.
   *
   * @param index the function index within the module
   * @param name the function name, may be null for unnamed functions
   * @param funcType the function's type signature
   * @param isImport true if the function is imported, false if locally defined
   */
  public FunctionInfo(
      final int index, final String name, final FuncType funcType, final boolean isImport) {
    this.index = index;
    this.name = name;
    this.funcType = funcType;
    this.isImport = isImport;
  }

  /**
   * Gets the function index within the module.
   *
   * @return the function index
   */
  public int getIndex() {
    return index;
  }

  /**
   * Gets the function name.
   *
   * @return the function name, or null if unnamed
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the function's type signature.
   *
   * @return the function type
   */
  public FuncType getFuncType() {
    return funcType;
  }

  /**
   * Checks if this function is imported from another module.
   *
   * @return true if imported, false if locally defined
   */
  public boolean isImport() {
    return isImport;
  }

  /**
   * Checks if this function is locally defined in the module.
   *
   * @return true if locally defined, false if imported
   */
  public boolean isLocal() {
    return !isImport;
  }

  @Override
  public String toString() {
    return "FunctionInfo{"
        + "index="
        + index
        + ", name='"
        + name
        + '\''
        + ", funcType="
        + funcType
        + ", isImport="
        + isImport
        + '}';
  }
}
