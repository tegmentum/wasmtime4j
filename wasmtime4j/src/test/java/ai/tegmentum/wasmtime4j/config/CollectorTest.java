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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Collector} enum.
 *
 * <p>Collector represents the garbage collection strategy used by the Wasmtime engine.
 */
@DisplayName("Collector Enum Tests")
class CollectorTest {

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all expected enum values")
    void shouldHaveAllExpectedEnumValues() {
      assertNotNull(Collector.AUTO, "Should have AUTO");
      assertNotNull(
          Collector.DEFERRED_REFERENCE_COUNTING, "Should have DEFERRED_REFERENCE_COUNTING");
      assertNotNull(Collector.NULL, "Should have NULL");
      assertEquals(3, Collector.values().length, "Should have exactly 3 values");
    }
  }

  @Nested
  @DisplayName("Rust Name Tests")
  class RustNameTests {

    @Test
    @DisplayName("AUTO should have rust name 'auto'")
    void autoShouldHaveCorrectRustName() {
      assertEquals("auto", Collector.AUTO.getRustName());
    }

    @Test
    @DisplayName("DEFERRED_REFERENCE_COUNTING should have correct rust name")
    void deferredReferenceCoundingShouldHaveCorrectRustName() {
      assertEquals(
          "deferred_reference_counting", Collector.DEFERRED_REFERENCE_COUNTING.getRustName());
    }

    @Test
    @DisplayName("NULL should have rust name 'null'")
    void nullShouldHaveCorrectRustName() {
      assertEquals("null", Collector.NULL.getRustName());
    }
  }

  @Nested
  @DisplayName("fromString Tests")
  class FromStringTests {

    @Test
    @DisplayName("should parse 'auto' to AUTO")
    void shouldParseAutoString() {
      assertEquals(Collector.AUTO, Collector.fromString("auto"));
    }

    @Test
    @DisplayName("should parse 'deferred_reference_counting'")
    void shouldParseDeferredReferenceCounting() {
      assertEquals(
          Collector.DEFERRED_REFERENCE_COUNTING,
          Collector.fromString("deferred_reference_counting"));
    }

    @Test
    @DisplayName("should parse 'null' to NULL")
    void shouldParseNullString() {
      assertEquals(Collector.NULL, Collector.fromString("null"));
    }

    @Test
    @DisplayName("should throw on unknown string")
    void shouldThrowOnUnknownString() {
      assertThrows(IllegalArgumentException.class, () -> Collector.fromString("unknown"));
    }

    @Test
    @DisplayName("should throw on null input")
    void shouldThrowOnNullInput() {
      assertThrows(IllegalArgumentException.class, () -> Collector.fromString(null));
    }
  }

  @Nested
  @DisplayName("EngineConfig Integration Tests")
  class EngineConfigIntegrationTests {

    @Test
    @DisplayName("should set collector on EngineConfig")
    void shouldSetCollectorOnEngineConfig() {
      final EngineConfig config = new EngineConfig();
      config.collector(Collector.NULL);
      assertEquals(Collector.NULL, config.getCollector());
    }

    @Test
    @DisplayName("should default to AUTO")
    void shouldDefaultToAuto() {
      final EngineConfig config = new EngineConfig();
      assertEquals(Collector.AUTO, config.getCollector());
    }

    @Test
    @DisplayName("collector should serialize in toJson")
    void collectorShouldSerializeInToJson() {
      final EngineConfig config = new EngineConfig();
      config.collector(Collector.DEFERRED_REFERENCE_COUNTING);
      final String json = new String(config.toJson());
      assertTrue(
          json.contains("deferred_reference_counting"),
          "JSON should contain collector value: " + json);
    }
  }
}
