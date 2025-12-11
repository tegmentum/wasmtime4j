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

package ai.tegmentum.wasmtime4j.config;

/**
 * Register allocation algorithm selection for the Cranelift compiler.
 *
 * <p>The register allocator is responsible for mapping virtual registers to physical
 * machine registers during code generation. Different algorithms provide different
 * tradeoffs between compilation speed and generated code quality.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * EngineConfig config = new EngineConfig()
 *     .regallocAlgorithm(RegallocAlgorithm.BACKTRACKING);
 * Engine engine = Engine.create(config);
 * }</pre>
 *
 * @since 1.1.0
 */
public enum RegallocAlgorithm {

  /**
   * Single-pass register allocation algorithm.
   *
   * <p>This is the fastest register allocation algorithm but may produce lower quality code.
   * It performs a single linear scan over the code, making allocation decisions without
   * backtracking.
   *
   * <p>Best suited for:
   * <ul>
   *   <li>Debug builds where fast compilation is more important</li>
   *   <li>JIT compilation where startup time is critical</li>
   *   <li>Very large functions where other algorithms may be slow</li>
   * </ul>
   */
  SINGLE_PASS("single_pass"),

  /**
   * Backtracking register allocation algorithm.
   *
   * <p>This algorithm may revisit allocation decisions to find better solutions,
   * producing higher quality code at the cost of increased compilation time.
   *
   * <p>Best suited for:
   * <ul>
   *   <li>Release builds where code quality is important</li>
   *   <li>AOT compilation where compilation time is less critical</li>
   *   <li>Performance-critical code paths</li>
   * </ul>
   */
  BACKTRACKING("backtracking");

  private final String value;

  RegallocAlgorithm(final String value) {
    this.value = value;
  }

  /**
   * Gets the string value for this algorithm.
   *
   * @return the algorithm name as used by Cranelift
   */
  public String getValue() {
    return value;
  }

  /**
   * Gets the algorithm from its string value.
   *
   * @param value the string value
   * @return the corresponding algorithm
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static RegallocAlgorithm fromValue(final String value) {
    for (RegallocAlgorithm algo : values()) {
      if (algo.value.equals(value)) {
        return algo;
      }
    }
    throw new IllegalArgumentException("Unknown regalloc algorithm: " + value);
  }

  @Override
  public String toString() {
    return value;
  }
}
