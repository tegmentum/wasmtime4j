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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentStoreConfig}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentStoreConfig")
class ComponentStoreConfigTest {

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("builder creates config with all defaults at zero/false")
    void defaultValues() {
      final ComponentStoreConfig config = ComponentStoreConfig.builder().build();
      assertEquals(0, config.getFuelLimit());
      assertEquals(0, config.getEpochDeadline());
      assertEquals(0, config.getMaxMemoryBytes());
      assertEquals(0, config.getMaxTableElements());
      assertEquals(0, config.getMaxInstances());
      assertEquals(0, config.getMaxTables());
      assertEquals(0, config.getMaxMemories());
      assertFalse(config.isTrapOnGrowFailure());
    }
  }

  @Nested
  @DisplayName("builder setters")
  class BuilderSetters {

    @Test
    @DisplayName("sets fuel limit")
    void setsFuelLimit() {
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().fuelLimit(1_000_000).build();
      assertEquals(1_000_000, config.getFuelLimit());
    }

    @Test
    @DisplayName("sets epoch deadline")
    void setsEpochDeadline() {
      final ComponentStoreConfig config = ComponentStoreConfig.builder().epochDeadline(5).build();
      assertEquals(5, config.getEpochDeadline());
    }

    @Test
    @DisplayName("sets max memory bytes")
    void setsMaxMemoryBytes() {
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxMemoryBytes(64 * 1024 * 1024).build();
      assertEquals(64 * 1024 * 1024, config.getMaxMemoryBytes());
    }

    @Test
    @DisplayName("sets max table elements")
    void setsMaxTableElements() {
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxTableElements(1000).build();
      assertEquals(1000, config.getMaxTableElements());
    }

    @Test
    @DisplayName("sets max instances")
    void setsMaxInstances() {
      final ComponentStoreConfig config = ComponentStoreConfig.builder().maxInstances(10).build();
      assertEquals(10, config.getMaxInstances());
    }

    @Test
    @DisplayName("sets max tables")
    void setsMaxTables() {
      final ComponentStoreConfig config = ComponentStoreConfig.builder().maxTables(5).build();
      assertEquals(5, config.getMaxTables());
    }

    @Test
    @DisplayName("sets max memories")
    void setsMaxMemories() {
      final ComponentStoreConfig config = ComponentStoreConfig.builder().maxMemories(3).build();
      assertEquals(3, config.getMaxMemories());
    }

    @Test
    @DisplayName("sets trap on grow failure")
    void setsTrapOnGrowFailure() {
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().trapOnGrowFailure(true).build();
      assertTrue(config.isTrapOnGrowFailure());
    }

    @Test
    @DisplayName("supports method chaining")
    void supportsChaining() {
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder()
              .fuelLimit(100)
              .epochDeadline(10)
              .maxMemoryBytes(1024)
              .maxTableElements(50)
              .maxInstances(5)
              .maxTables(3)
              .maxMemories(2)
              .trapOnGrowFailure(true)
              .build();
      assertEquals(100, config.getFuelLimit());
      assertEquals(10, config.getEpochDeadline());
      assertEquals(1024, config.getMaxMemoryBytes());
      assertEquals(50, config.getMaxTableElements());
      assertEquals(5, config.getMaxInstances());
      assertEquals(3, config.getMaxTables());
      assertEquals(2, config.getMaxMemories());
      assertTrue(config.isTrapOnGrowFailure());
    }
  }

  @Nested
  @DisplayName("validation")
  class Validation {

    @Test
    @DisplayName("rejects negative fuel limit")
    void rejectsNegativeFuelLimit() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentStoreConfig.builder().fuelLimit(-1));
    }

    @Test
    @DisplayName("rejects negative epoch deadline")
    void rejectsNegativeEpochDeadline() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentStoreConfig.builder().epochDeadline(-1));
    }

    @Test
    @DisplayName("rejects negative max memory bytes")
    void rejectsNegativeMaxMemoryBytes() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentStoreConfig.builder().maxMemoryBytes(-1));
    }

    @Test
    @DisplayName("rejects negative max table elements")
    void rejectsNegativeMaxTableElements() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentStoreConfig.builder().maxTableElements(-1));
    }

    @Test
    @DisplayName("rejects negative max instances")
    void rejectsNegativeMaxInstances() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentStoreConfig.builder().maxInstances(-1));
    }

    @Test
    @DisplayName("rejects negative max tables")
    void rejectsNegativeMaxTables() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentStoreConfig.builder().maxTables(-1));
    }

    @Test
    @DisplayName("rejects negative max memories")
    void rejectsNegativeMaxMemories() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentStoreConfig.builder().maxMemories(-1));
    }
  }
}
