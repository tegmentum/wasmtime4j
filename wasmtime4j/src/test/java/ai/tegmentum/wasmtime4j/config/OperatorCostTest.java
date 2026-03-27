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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperatorCost")
class OperatorCostTest {

  @Test
  @DisplayName("defaults creates empty cost map")
  void defaultsCreatesEmptyCostMap() {
    final OperatorCost cost = OperatorCost.defaults();
    assertTrue(cost.getCosts().isEmpty());
  }

  @Test
  @DisplayName("set adds operator cost to map")
  void setAddsOperatorCost() {
    final OperatorCost cost = OperatorCost.defaults().set("Call", 5).set("I32Add", 2);
    assertEquals(2, cost.getCosts().size());
    assertEquals(5, cost.getCosts().get("Call"));
    assertEquals(2, cost.getCosts().get("I32Add"));
  }

  @Test
  @DisplayName("set rejects cost below 0")
  void setRejectsCostBelowZero() {
    assertThrows(IllegalArgumentException.class, () -> OperatorCost.defaults().set("Call", -1));
  }

  @Test
  @DisplayName("set rejects cost above 255")
  void setRejectsCostAbove255() {
    assertThrows(IllegalArgumentException.class, () -> OperatorCost.defaults().set("Call", 256));
  }

  @Test
  @DisplayName("set accepts boundary values 0 and 255")
  void setAcceptsBoundaryValues() {
    final OperatorCost cost = OperatorCost.defaults().set("Nop", 0).set("Call", 255);
    assertEquals(0, cost.getCosts().get("Nop"));
    assertEquals(255, cost.getCosts().get("Call"));
  }

  @Test
  @DisplayName("toJson produces valid JSON for empty cost")
  void toJsonEmpty() {
    assertEquals("{}", OperatorCost.defaults().toJson());
  }

  @Test
  @DisplayName("toJson produces valid JSON with costs")
  void toJsonWithCosts() {
    final String json = OperatorCost.defaults().set("Call", 5).set("I32Add", 2).toJson();
    assertTrue(json.contains("\"Call\":5"));
    assertTrue(json.contains("\"I32Add\":2"));
    assertTrue(json.startsWith("{"));
    assertTrue(json.endsWith("}"));
  }

  @Test
  @DisplayName("getCosts returns unmodifiable map")
  void getCostsReturnsUnmodifiableMap() {
    final OperatorCost cost = OperatorCost.defaults().set("Call", 5);
    assertThrows(UnsupportedOperationException.class, () -> cost.getCosts().put("Nop", 1));
  }

  @Test
  @DisplayName("set overwrites previous value for same operator")
  void setOverwritesPreviousValue() {
    final OperatorCost cost = OperatorCost.defaults().set("Call", 5).set("Call", 10);
    assertEquals(10, cost.getCosts().get("Call"));
    assertEquals(1, cost.getCosts().size());
  }

  @Test
  @DisplayName("EngineConfig accepts operatorCost")
  void engineConfigAcceptsOperatorCost() {
    final OperatorCost cost = OperatorCost.defaults().set("Call", 5);
    final EngineConfig config = new EngineConfig().consumeFuel(true).operatorCost(cost);
    final String json = new String(config.toJson(), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(json.contains("\"operatorCost\":{\"Call\":5}"));
  }
}
