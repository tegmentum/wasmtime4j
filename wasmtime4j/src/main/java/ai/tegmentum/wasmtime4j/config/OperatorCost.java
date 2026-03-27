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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-operator fuel cost configuration for WebAssembly execution.
 *
 * <p>Each WebAssembly operator can be assigned a fuel cost (0-255). The default cost is 1 for most
 * operators and 0 for control flow operators (block, loop, end, else, return, nop, drop).
 *
 * <p>This is only meaningful when fuel consumption is enabled via {@link
 * EngineConfig#consumeFuel(boolean)}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * OperatorCost costs = OperatorCost.defaults()
 *     .set("Call", 5)        // function calls cost 5 fuel
 *     .set("MemoryGrow", 10) // memory growth costs 10 fuel
 *     .set("I32Add", 1);     // i32 addition costs 1 fuel (default)
 *
 * EngineConfig config = new EngineConfig()
 *     .consumeFuel(true)
 *     .operatorCost(costs);
 * }</pre>
 *
 * @since 1.1.0
 */
public final class OperatorCost {

  private final Map<String, Integer> costs;

  private OperatorCost(final Map<String, Integer> costs) {
    this.costs = new LinkedHashMap<>(costs);
  }

  /**
   * Creates an OperatorCost with all default values.
   *
   * @return a new OperatorCost with default costs
   */
  public static OperatorCost defaults() {
    return new OperatorCost(new LinkedHashMap<>());
  }

  /**
   * Sets the fuel cost for a specific operator.
   *
   * <p>Operator names match WebAssembly operator names as defined by wasmparser (e.g., "Call",
   * "I32Add", "MemoryGrow", "Block", "Loop").
   *
   * @param operatorName the operator name (case-sensitive)
   * @param cost the fuel cost (0-255)
   * @return this OperatorCost for chaining
   * @throws IllegalArgumentException if cost is outside 0-255 range
   */
  public OperatorCost set(final String operatorName, final int cost) {
    if (cost < 0 || cost > 255) {
      throw new IllegalArgumentException("Cost must be 0-255, got: " + cost);
    }
    costs.put(operatorName, cost);
    return this;
  }

  /**
   * Gets the configured cost overrides.
   *
   * @return an unmodifiable map of operator name to cost
   */
  public Map<String, Integer> getCosts() {
    return Collections.unmodifiableMap(costs);
  }

  /**
   * Serializes the operator costs to a JSON object string.
   *
   * @return JSON representation of the cost overrides
   */
  public String toJson() {
    if (costs.isEmpty()) {
      return "{}";
    }
    final StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (final Map.Entry<String, Integer> entry : costs.entrySet()) {
      if (!first) {
        sb.append(',');
      }
      sb.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
      first = false;
    }
    sb.append('}');
    return sb.toString();
  }
}
