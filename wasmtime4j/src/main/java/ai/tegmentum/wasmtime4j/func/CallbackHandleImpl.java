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

import ai.tegmentum.wasmtime4j.type.FunctionType;

/**
 * Implementation of {@link CallbackRegistry.CallbackHandle}.
 *
 * @since 1.0.0
 */
public class CallbackHandleImpl implements CallbackRegistry.CallbackHandle {

  private final long id;
  private final String name;
  private final FunctionType functionType;
  private volatile boolean valid = true;

  /**
   * Creates a new callback handle.
   *
   * @param id the unique callback identifier
   * @param name the callback name
   * @param functionType the function type signature
   */
  public CallbackHandleImpl(final long id, final String name, final FunctionType functionType) {
    this.id = id;
    this.name = name;
    this.functionType = functionType;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public FunctionType getFunctionType() {
    return functionType;
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  /** Marks this handle as invalid. */
  public void invalidate() {
    this.valid = false;
  }
}
