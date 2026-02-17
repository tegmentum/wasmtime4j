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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.StoreLimits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StoreBuilder} class.
 *
 * <p>StoreBuilder is a generic fluent builder for creating Store instances with custom user data,
 * fuel, epoch deadline, and resource limits. Constructor requires a non-null Engine.
 */
@DisplayName("StoreBuilder Tests")
class StoreBuilderTest {

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should reject null engine")
    void shouldRejectNullEngine() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new StoreBuilder<>(null),
              "Null engine should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().toLowerCase().contains("null"),
          "Message should mention null: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Fluent API Tests")
  class FluentApiTests {

    @Test
    @DisplayName("should set data via withData and return same builder")
    void shouldSetDataViaWithData() {
      final Engine engine = createMockEngine();
      final StoreBuilder<String> builder = new StoreBuilder<>(engine);
      final StoreBuilder<String> result = builder.withData("test-data");

      assertNotNull(result, "withData should return the builder");
      assertEquals("test-data", builder.getData(), "getData should return the set data");
    }

    @Test
    @DisplayName("should set fuel via withFuel and return same builder")
    void shouldSetFuelViaWithFuel() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);
      final StoreBuilder<Object> result = builder.withFuel(10000L);

      assertNotNull(result, "withFuel should return the builder");
      assertEquals(10000L, builder.getFuel(), "getFuel should return the set fuel");
    }

    @Test
    @DisplayName("should set epochDeadline via withEpochDeadline")
    void shouldSetEpochDeadline() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);
      final StoreBuilder<Object> result = builder.withEpochDeadline(100L);

      assertNotNull(result, "withEpochDeadline should return the builder");
      assertEquals(
          100L, builder.getEpochDeadline(), "getEpochDeadline should return the set value");
    }

    @Test
    @DisplayName("should set limits via withLimits")
    void shouldSetLimits() {
      final Engine engine = createMockEngine();
      final StoreLimits limits = StoreLimits.builder().memorySize(1024L).build();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);
      final StoreBuilder<Object> result = builder.withLimits(limits);

      assertNotNull(result, "withLimits should return the builder");
      assertNotNull(builder.getLimits(), "getLimits should return non-null limits");
      assertEquals(1024L, builder.getLimits().getMemorySize(), "Limits memorySize should be 1024");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject negative fuel")
    void shouldRejectNegativeFuel() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withFuel(-1L),
              "Negative fuel should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().toLowerCase().contains("negative"),
          "Message should mention negative: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative epochDeadline")
    void shouldRejectNegativeEpochDeadline() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withEpochDeadline(-1L),
              "Negative epochDeadline should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().toLowerCase().contains("negative"),
          "Message should mention negative: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject null limits")
    void shouldRejectNullLimits() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withLimits(null),
              "Null limits should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().toLowerCase().contains("null"),
          "Message should mention null: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Default State Tests")
  class DefaultStateTests {

    @Test
    @DisplayName("should have null data by default")
    void shouldHaveNullDataByDefault() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      assertNull(builder.getData(), "Default data should be null");
    }

    @Test
    @DisplayName("should have null fuel by default")
    void shouldHaveNullFuelByDefault() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      assertNull(builder.getFuel(), "Default fuel should be null");
    }

    @Test
    @DisplayName("should have null epochDeadline by default")
    void shouldHaveNullEpochDeadlineByDefault() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      assertNull(builder.getEpochDeadline(), "Default epochDeadline should be null");
    }

    @Test
    @DisplayName("should have null limits by default")
    void shouldHaveNullLimitsByDefault() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      assertNull(builder.getLimits(), "Default limits should be null");
    }

    @Test
    @DisplayName("should store engine reference from constructor")
    void shouldStoreEngineReference() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);

      assertNotNull(builder.getEngine(), "getEngine should return non-null engine");
    }
  }

  @Nested
  @DisplayName("Boundary Value Tests")
  class BoundaryValueTests {

    @Test
    @DisplayName("should accept zero fuel")
    void shouldAcceptZeroFuel() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);
      builder.withFuel(0L);

      assertEquals(0L, builder.getFuel(), "Zero fuel should be accepted");
    }

    @Test
    @DisplayName("should accept zero epochDeadline")
    void shouldAcceptZeroEpochDeadline() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);
      builder.withEpochDeadline(0L);

      assertEquals(0L, builder.getEpochDeadline(), "Zero epochDeadline should be accepted");
    }

    @Test
    @DisplayName("should accept Long.MAX_VALUE fuel")
    void shouldAcceptMaxValueFuel() {
      final Engine engine = createMockEngine();
      final StoreBuilder<Object> builder = new StoreBuilder<>(engine);
      builder.withFuel(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, builder.getFuel(), "Long.MAX_VALUE fuel should be accepted");
    }
  }

  /**
   * Creates a minimal Engine proxy for testing StoreBuilder's builder-only methods. This avoids
   * requiring a real native engine for builder configuration tests.
   */
  private Engine createMockEngine() {
    return (Engine)
        java.lang.reflect.Proxy.newProxyInstance(
            Engine.class.getClassLoader(),
            new Class<?>[] {Engine.class},
            (proxy, method, args) -> {
              if ("isFuelEnabled".equals(method.getName())) {
                return false;
              }
              if ("isEpochInterruptionEnabled".equals(method.getName())) {
                return false;
              }
              return null;
            });
  }
}
