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
package ai.tegmentum.wasmtime4j.component;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single function call within a concurrent execution batch.
 *
 * <p>Used with {@link ComponentInstance#runConcurrent(List)} to execute multiple component function
 * calls concurrently using Wasmtime's native concurrent call support.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * List<ConcurrentCall> calls = List.of(
 *     ConcurrentCall.of("add", ComponentVal.s32(1), ComponentVal.s32(2)),
 *     ConcurrentCall.of("multiply", ComponentVal.s32(3), ComponentVal.s32(4))
 * );
 *
 * List<List<ComponentVal>> results = instance.runConcurrent(calls);
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ConcurrentCall {

  private final String functionName;
  private final List<ComponentVal> args;

  private ConcurrentCall(final String functionName, final List<ComponentVal> args) {
    if (functionName == null || functionName.isEmpty()) {
      throw new IllegalArgumentException("functionName cannot be null or empty");
    }
    if (args == null) {
      throw new IllegalArgumentException("args cannot be null");
    }
    this.functionName = functionName;
    this.args = Collections.unmodifiableList(args);
  }

  /**
   * Creates a concurrent call with the given function name and arguments.
   *
   * @param functionName the name of the function to call
   * @param args the arguments to pass to the function
   * @return a new ConcurrentCall
   * @throws IllegalArgumentException if functionName is null or empty, or args is null
   */
  public static ConcurrentCall of(final String functionName, final ComponentVal... args) {
    return new ConcurrentCall(functionName, List.of(args));
  }

  /**
   * Creates a concurrent call with the given function name and argument list.
   *
   * @param functionName the name of the function to call
   * @param args the arguments to pass to the function
   * @return a new ConcurrentCall
   * @throws IllegalArgumentException if functionName is null or empty, or args is null
   */
  public static ConcurrentCall of(final String functionName, final List<ComponentVal> args) {
    return new ConcurrentCall(functionName, List.copyOf(args));
  }

  /**
   * Gets the function name for this call.
   *
   * @return the function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the arguments for this call.
   *
   * @return an unmodifiable list of arguments
   */
  public List<ComponentVal> getArgs() {
    return args;
  }

  @Override
  public String toString() {
    return "ConcurrentCall{functionName='" + functionName + "', args=" + args + "}";
  }
}
