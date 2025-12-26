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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEventConfig} class.
 *
 * <p>ComponentEventConfig provides configuration for component event handling including queue size,
 * timeouts, persistence, ordering, and retry settings.
 */
@DisplayName("ComponentEventConfig Tests")
class ComponentEventConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentEventConfig.class.getModifiers()),
          "ComponentEventConfig should be public");
      assertTrue(
          Modifier.isFinal(ComponentEventConfig.class.getModifiers()),
          "ComponentEventConfig should be final");
    }

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentEventConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }
  }

  @Nested
  @DisplayName("Default Constructor Tests")
  class DefaultConstructorTests {

    @Test
    @DisplayName("should create instance with default values")
    void shouldCreateInstanceWithDefaultValues() {
      final ComponentEventConfig config = new ComponentEventConfig();
      assertNotNull(config, "Config should be created");
      assertEquals(1000, config.getMaxQueueSize(), "Default max queue size should be 1000");
      assertEquals(
          Duration.ofSeconds(30), config.getEventTimeout(), "Default timeout should be 30 seconds");
      assertFalse(config.isPersistenceEnabled(), "Persistence should be disabled by default");
      assertTrue(config.isOrderingEnabled(), "Ordering should be enabled by default");
      assertEquals(3, config.getMaxRetries(), "Default max retries should be 3");
    }
  }

  @Nested
  @DisplayName("Custom Constructor Tests")
  class CustomConstructorTests {

    @Test
    @DisplayName("should create instance with custom values")
    void shouldCreateInstanceWithCustomValues() {
      final ComponentEventConfig config =
          new ComponentEventConfig(500, Duration.ofSeconds(60), true, false, 5);

      assertEquals(500, config.getMaxQueueSize(), "Max queue size should be 500");
      assertEquals(
          Duration.ofSeconds(60), config.getEventTimeout(), "Timeout should be 60 seconds");
      assertTrue(config.isPersistenceEnabled(), "Persistence should be enabled");
      assertFalse(config.isOrderingEnabled(), "Ordering should be disabled");
      assertEquals(5, config.getMaxRetries(), "Max retries should be 5");
    }

    @Test
    @DisplayName("should throw for zero queue size")
    void shouldThrowForZeroQueueSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentEventConfig(0, Duration.ofSeconds(30), false, true, 3),
          "Should throw for zero queue size");
    }

    @Test
    @DisplayName("should throw for negative queue size")
    void shouldThrowForNegativeQueueSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentEventConfig(-1, Duration.ofSeconds(30), false, true, 3),
          "Should throw for negative queue size");
    }

    @Test
    @DisplayName("should throw for null timeout")
    void shouldThrowForNullTimeout() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentEventConfig(1000, null, false, true, 3),
          "Should throw for null timeout");
    }

    @Test
    @DisplayName("should normalize negative retries to zero")
    void shouldNormalizeNegativeRetriesToZero() {
      final ComponentEventConfig config =
          new ComponentEventConfig(1000, Duration.ofSeconds(30), false, true, -5);
      assertEquals(0, config.getMaxRetries(), "Negative retries should be normalized to 0");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getMaxQueueSize should return queue size")
    void getMaxQueueSizeShouldReturnQueueSize() {
      final ComponentEventConfig config = new ComponentEventConfig();
      assertEquals(1000, config.getMaxQueueSize(), "Should return max queue size");
    }

    @Test
    @DisplayName("getEventTimeout should return timeout")
    void getEventTimeoutShouldReturnTimeout() {
      final ComponentEventConfig config = new ComponentEventConfig();
      assertEquals(Duration.ofSeconds(30), config.getEventTimeout(), "Should return event timeout");
    }

    @Test
    @DisplayName("isPersistenceEnabled should return persistence flag")
    void isPersistenceEnabledShouldReturnPersistenceFlag() {
      final ComponentEventConfig config = new ComponentEventConfig();
      assertFalse(config.isPersistenceEnabled(), "Should return persistence flag");
    }

    @Test
    @DisplayName("isOrderingEnabled should return ordering flag")
    void isOrderingEnabledShouldReturnOrderingFlag() {
      final ComponentEventConfig config = new ComponentEventConfig();
      assertTrue(config.isOrderingEnabled(), "Should return ordering flag");
    }

    @Test
    @DisplayName("getMaxRetries should return max retries")
    void getMaxRetriesShouldReturnMaxRetries() {
      final ComponentEventConfig config = new ComponentEventConfig();
      assertEquals(3, config.getMaxRetries(), "Should return max retries");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create config with defaults")
    void builderShouldCreateConfigWithDefaults() {
      final ComponentEventConfig config = ComponentEventConfig.builder().build();

      assertNotNull(config, "Builder should create config");
      assertEquals(1000, config.getMaxQueueSize(), "Default max queue size should be 1000");
      assertEquals(
          Duration.ofSeconds(30), config.getEventTimeout(), "Default timeout should be 30 seconds");
      assertFalse(config.isPersistenceEnabled(), "Persistence should be disabled by default");
      assertTrue(config.isOrderingEnabled(), "Ordering should be enabled by default");
      assertEquals(3, config.getMaxRetries(), "Default max retries should be 3");
    }

    @Test
    @DisplayName("builder should set max queue size")
    void builderShouldSetMaxQueueSize() {
      final ComponentEventConfig config = ComponentEventConfig.builder().maxQueueSize(2000).build();

      assertEquals(2000, config.getMaxQueueSize(), "Max queue size should be 2000");
    }

    @Test
    @DisplayName("builder should set event timeout")
    void builderShouldSetEventTimeout() {
      final ComponentEventConfig config =
          ComponentEventConfig.builder().eventTimeout(Duration.ofMinutes(1)).build();

      assertEquals(Duration.ofMinutes(1), config.getEventTimeout(), "Timeout should be 1 minute");
    }

    @Test
    @DisplayName("builder should set persistence enabled")
    void builderShouldSetPersistenceEnabled() {
      final ComponentEventConfig config =
          ComponentEventConfig.builder().enablePersistence(true).build();

      assertTrue(config.isPersistenceEnabled(), "Persistence should be enabled");
    }

    @Test
    @DisplayName("builder should set ordering enabled")
    void builderShouldSetOrderingEnabled() {
      final ComponentEventConfig config =
          ComponentEventConfig.builder().enableOrdering(false).build();

      assertFalse(config.isOrderingEnabled(), "Ordering should be disabled");
    }

    @Test
    @DisplayName("builder should set max retries")
    void builderShouldSetMaxRetries() {
      final ComponentEventConfig config = ComponentEventConfig.builder().maxRetries(10).build();

      assertEquals(10, config.getMaxRetries(), "Max retries should be 10");
    }

    @Test
    @DisplayName("builder should allow chaining")
    void builderShouldAllowChaining() {
      final var builder = ComponentEventConfig.builder();
      assertNotNull(builder.maxQueueSize(500), "Should return builder for chaining");
      assertNotNull(builder.eventTimeout(Duration.ofSeconds(10)), "Should return builder");
      assertNotNull(builder.enablePersistence(true), "Should return builder");
      assertNotNull(builder.enableOrdering(false), "Should return builder");
      assertNotNull(builder.maxRetries(5), "Should return builder");
    }

    @Test
    @DisplayName("builder should set all options")
    void builderShouldSetAllOptions() {
      final ComponentEventConfig config =
          ComponentEventConfig.builder()
              .maxQueueSize(500)
              .eventTimeout(Duration.ofMinutes(2))
              .enablePersistence(true)
              .enableOrdering(false)
              .maxRetries(10)
              .build();

      assertEquals(500, config.getMaxQueueSize(), "Max queue size should be 500");
      assertEquals(Duration.ofMinutes(2), config.getEventTimeout(), "Timeout should be 2 minutes");
      assertTrue(config.isPersistenceEnabled(), "Persistence should be enabled");
      assertFalse(config.isOrderingEnabled(), "Ordering should be disabled");
      assertEquals(10, config.getMaxRetries(), "Max retries should be 10");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should allow minimum queue size")
    void shouldAllowMinimumQueueSize() {
      final ComponentEventConfig config = ComponentEventConfig.builder().maxQueueSize(1).build();

      assertEquals(1, config.getMaxQueueSize(), "Queue size should be 1");
    }

    @Test
    @DisplayName("should allow zero duration timeout")
    void shouldAllowZeroDurationTimeout() {
      final ComponentEventConfig config =
          ComponentEventConfig.builder().eventTimeout(Duration.ZERO).build();

      assertEquals(Duration.ZERO, config.getEventTimeout(), "Timeout should be zero");
    }

    @Test
    @DisplayName("should allow zero retries")
    void shouldAllowZeroRetries() {
      final ComponentEventConfig config = ComponentEventConfig.builder().maxRetries(0).build();

      assertEquals(0, config.getMaxRetries(), "Max retries should be 0");
    }

    @Test
    @DisplayName("multiple builder calls should not interfere")
    void multipleBuilderCallsShouldNotInterfere() {
      final var builder1 = ComponentEventConfig.builder().maxQueueSize(100);
      final var builder2 = ComponentEventConfig.builder().maxQueueSize(200);

      final ComponentEventConfig config1 = builder1.build();
      final ComponentEventConfig config2 = builder2.build();

      assertEquals(100, config1.getMaxQueueSize(), "Config1 should have queue size 100");
      assertEquals(200, config2.getMaxQueueSize(), "Config2 should have queue size 200");
    }

    @Test
    @DisplayName("should handle large queue size")
    void shouldHandleLargeQueueSize() {
      final ComponentEventConfig config =
          ComponentEventConfig.builder().maxQueueSize(Integer.MAX_VALUE).build();

      assertEquals(
          Integer.MAX_VALUE, config.getMaxQueueSize(), "Should handle max integer queue size");
    }

    @Test
    @DisplayName("should handle long duration timeout")
    void shouldHandleLongDurationTimeout() {
      final Duration longDuration = Duration.ofDays(365);
      final ComponentEventConfig config =
          ComponentEventConfig.builder().eventTimeout(longDuration).build();

      assertEquals(longDuration, config.getEventTimeout(), "Should handle long duration timeout");
    }
  }
}
