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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Objects;

/**
 * Panama implementation of a WebAssembly Component Model function.
 *
 * <p>This class wraps a function name and its parent component instance, providing a first-class
 * function object that can be invoked multiple times without name lookup overhead.
 *
 * @since 1.0.0
 */
final class PanamaComponentFunction implements ComponentFunction {

  private final String functionName;
  private final PanamaComponentInstance instance;

  /**
   * Creates a new Panama component function.
   *
   * @param functionName the function name
   * @param instance the parent component instance
   */
  PanamaComponentFunction(final String functionName, final PanamaComponentInstance instance) {
    this.functionName = Objects.requireNonNull(functionName, "functionName cannot be null");
    this.instance = Objects.requireNonNull(instance, "instance cannot be null");
  }

  @Override
  public String getName() {
    return functionName;
  }

  @Override
  public Object call(final Object... args) throws WasmException {
    if (!isValid()) {
      throw new WasmException("Cannot call function: component instance is closed");
    }
    return instance.invoke(functionName, args);
  }

  @Override
  public boolean isValid() {
    return instance.isValid();
  }

  @Override
  public ComponentInstance getInstance() {
    return instance;
  }

  @Override
  public String toString() {
    return "PanamaComponentFunction{name='" + functionName + "', valid=" + isValid() + "}";
  }
}
