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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for FuelExhaustionContext class.
 *
 * <p>Verifies the constructor and getter methods for fuel exhaustion event context.
 */
@DisplayName("FuelExhaustionContext Tests")
class FuelExhaustionContextTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create context with all values")
    void shouldCreateContextWithAllValues() {
      FuelExhaustionContext context =
          new FuelExhaustionContext(
              123L, // storeId
              1000L, // fuelConsumed
              5000L, // initialFuel
              3, // exhaustionCount
              "test_function" // functionName
              );

      assertNotNull(context, "Context should not be null");
      assertEquals(123L, context.getStoreId(), "storeId should match");
      assertEquals(1000L, context.getFuelConsumed(), "fuelConsumed should match");
      assertEquals(5000L, context.getInitialFuel(), "initialFuel should match");
      assertEquals(3, context.getExhaustionCount(), "exhaustionCount should match");
      assertTrue(context.getFunctionName().isPresent(), "functionName should be present");
      assertEquals("test_function", context.getFunctionName().get(), "functionName should match");
    }

    @Test
    @DisplayName("should create context with null function name")
    void shouldCreateContextWithNullFunctionName() {
      FuelExhaustionContext context = new FuelExhaustionContext(1L, 100L, 500L, 1, null);

      assertFalse(
          context.getFunctionName().isPresent(),
          "functionName should be empty when null is passed");
    }

    @Test
    @DisplayName("should create context with zero values")
    void shouldCreateContextWithZeroValues() {
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 0, null);

      assertEquals(0L, context.getStoreId(), "storeId should be 0");
      assertEquals(0L, context.getFuelConsumed(), "fuelConsumed should be 0");
      assertEquals(0L, context.getInitialFuel(), "initialFuel should be 0");
      assertEquals(0, context.getExhaustionCount(), "exhaustionCount should be 0");
    }

    @Test
    @DisplayName("should create context with large values")
    void shouldCreateContextWithLargeValues() {
      FuelExhaustionContext context =
          new FuelExhaustionContext(
              Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE, "very_long_fn");

      assertEquals(Long.MAX_VALUE, context.getStoreId(), "storeId should be Long.MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE, context.getFuelConsumed(), "fuelConsumed should be Long.MAX_VALUE");
      assertEquals(
          Long.MAX_VALUE, context.getInitialFuel(), "initialFuel should be Long.MAX_VALUE");
      assertEquals(
          Integer.MAX_VALUE, context.getExhaustionCount(), "exhaustionCount should be MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("GetStoreId Tests")
  class GetStoreIdTests {

    @Test
    @DisplayName("should return correct storeId")
    void shouldReturnCorrectStoreId() {
      long expectedStoreId = 42L;
      FuelExhaustionContext context = new FuelExhaustionContext(expectedStoreId, 0L, 0L, 0, null);

      assertEquals(expectedStoreId, context.getStoreId(), "getStoreId should return 42");
    }

    @Test
    @DisplayName("should handle negative storeId")
    void shouldHandleNegativeStoreId() {
      FuelExhaustionContext context = new FuelExhaustionContext(-1L, 0L, 0L, 0, null);

      assertEquals(-1L, context.getStoreId(), "getStoreId should return -1");
    }
  }

  @Nested
  @DisplayName("GetFuelConsumed Tests")
  class GetFuelConsumedTests {

    @Test
    @DisplayName("should return correct fuelConsumed")
    void shouldReturnCorrectFuelConsumed() {
      long expectedFuel = 10000L;
      FuelExhaustionContext context = new FuelExhaustionContext(0L, expectedFuel, 0L, 0, null);

      assertEquals(expectedFuel, context.getFuelConsumed(), "getFuelConsumed should return 10000");
    }

    @Test
    @DisplayName("should handle zero fuelConsumed")
    void shouldHandleZeroFuelConsumed() {
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 1000L, 0, null);

      assertEquals(0L, context.getFuelConsumed(), "getFuelConsumed should return 0");
    }
  }

  @Nested
  @DisplayName("GetInitialFuel Tests")
  class GetInitialFuelTests {

    @Test
    @DisplayName("should return correct initialFuel")
    void shouldReturnCorrectInitialFuel() {
      long expectedFuel = 50000L;
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, expectedFuel, 0, null);

      assertEquals(expectedFuel, context.getInitialFuel(), "getInitialFuel should return 50000");
    }
  }

  @Nested
  @DisplayName("GetExhaustionCount Tests")
  class GetExhaustionCountTests {

    @Test
    @DisplayName("should return correct exhaustionCount")
    void shouldReturnCorrectExhaustionCount() {
      int expectedCount = 5;
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, expectedCount, null);

      assertEquals(expectedCount, context.getExhaustionCount(), "getExhaustionCount should be 5");
    }

    @Test
    @DisplayName("should handle first exhaustion event")
    void shouldHandleFirstExhaustionEvent() {
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 1, null);

      assertEquals(1, context.getExhaustionCount(), "First exhaustion should have count 1");
    }
  }

  @Nested
  @DisplayName("GetFunctionName Tests")
  class GetFunctionNameTests {

    @Test
    @DisplayName("should return Optional with function name when present")
    void shouldReturnOptionalWithFunctionNameWhenPresent() {
      String expectedName = "my_wasm_function";
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 0, expectedName);

      Optional<String> result = context.getFunctionName();
      assertTrue(result.isPresent(), "functionName should be present");
      assertEquals(expectedName, result.get(), "functionName should match");
    }

    @Test
    @DisplayName("should return empty Optional when function name is null")
    void shouldReturnEmptyOptionalWhenFunctionNameIsNull() {
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 0, null);

      Optional<String> result = context.getFunctionName();
      assertFalse(result.isPresent(), "functionName should be empty");
    }

    @Test
    @DisplayName("should handle empty string function name")
    void shouldHandleEmptyStringFunctionName() {
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 0, "");

      Optional<String> result = context.getFunctionName();
      assertTrue(result.isPresent(), "Empty string should be present");
      assertEquals("", result.get(), "functionName should be empty string");
    }

    @Test
    @DisplayName("should handle function name with special characters")
    void shouldHandleFunctionNameWithSpecialCharacters() {
      String specialName = "module$foo::bar<T>";
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 0, specialName);

      Optional<String> result = context.getFunctionName();
      assertTrue(result.isPresent(), "functionName should be present");
      assertEquals(specialName, result.get(), "functionName should preserve special chars");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 0, null);

      assertNotNull(context.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      FuelExhaustionContext context = new FuelExhaustionContext(0L, 0L, 0L, 0, null);

      assertTrue(
          context.toString().contains("FuelExhaustionContext"),
          "toString should contain class name");
    }

    @Test
    @DisplayName("should include field names in toString")
    void shouldIncludeFieldNamesInToString() {
      FuelExhaustionContext context = new FuelExhaustionContext(1L, 2L, 3L, 4, "test");

      String result = context.toString();
      assertTrue(result.contains("storeId"), "toString should contain storeId");
      assertTrue(result.contains("fuelConsumed"), "toString should contain fuelConsumed");
      assertTrue(result.contains("initialFuel"), "toString should contain initialFuel");
      assertTrue(result.contains("exhaustionCount"), "toString should contain exhaustionCount");
      assertTrue(result.contains("functionName"), "toString should contain functionName");
    }

    @Test
    @DisplayName("should include values in toString")
    void shouldIncludeValuesInToString() {
      FuelExhaustionContext context = new FuelExhaustionContext(123L, 456L, 789L, 10, "my_func");

      String result = context.toString();
      assertTrue(result.contains("123"), "toString should contain storeId value");
      assertTrue(result.contains("456"), "toString should contain fuelConsumed value");
      assertTrue(result.contains("789"), "toString should contain initialFuel value");
      assertTrue(result.contains("10"), "toString should contain exhaustionCount value");
      assertTrue(result.contains("my_func"), "toString should contain functionName value");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent typical fuel exhaustion scenario")
    void shouldRepresentTypicalFuelExhaustionScenario() {
      // Simulate a module that started with 1 million fuel, consumed 999,000, exhausted 2 times
      FuelExhaustionContext context =
          new FuelExhaustionContext(
              42L, // store ID
              999_000L, // fuel consumed
              1_000_000L, // initial fuel
              2, // second time exhausting
              "compute_loop" // function name
              );

      assertEquals(42L, context.getStoreId());
      assertEquals(999_000L, context.getFuelConsumed());
      assertEquals(1_000_000L, context.getInitialFuel());
      assertEquals(2, context.getExhaustionCount());
      assertTrue(context.getFunctionName().isPresent());
      assertEquals("compute_loop", context.getFunctionName().get());

      // Verify consumption is meaningful (consumed most of initial fuel)
      assertTrue(
          context.getFuelConsumed() < context.getInitialFuel(),
          "Consumed fuel should be less than initial fuel");
      double consumptionRatio = (double) context.getFuelConsumed() / context.getInitialFuel();
      assertTrue(consumptionRatio > 0.9, "Should have consumed > 90% of fuel before exhaustion");
    }

    @Test
    @DisplayName("should handle first-time exhaustion with no function name")
    void shouldHandleFirstTimeExhaustionWithNoFunctionName() {
      // First exhaustion, unknown function (internal or unnamed)
      FuelExhaustionContext context = new FuelExhaustionContext(1L, 10000L, 10000L, 1, null);

      assertEquals(1L, context.getStoreId());
      assertEquals(10000L, context.getFuelConsumed());
      assertEquals(10000L, context.getInitialFuel());
      assertEquals(1, context.getExhaustionCount());
      assertFalse(context.getFunctionName().isPresent());
    }

    @Test
    @DisplayName("should handle repeated exhaustion scenario")
    void shouldHandleRepeatedExhaustionScenario() {
      // Module that keeps exhausting and being refueled
      FuelExhaustionContext context =
          new FuelExhaustionContext(
              100L,
              5_000_000L,
              1_000_000L, // Consumed more than initial = refueled multiple times
              10, // 10th exhaustion
              "infinite_loop");

      assertEquals(10, context.getExhaustionCount());
      assertTrue(
          context.getFuelConsumed() > context.getInitialFuel(),
          "Total consumed can exceed initial due to refueling");
    }
  }
}
