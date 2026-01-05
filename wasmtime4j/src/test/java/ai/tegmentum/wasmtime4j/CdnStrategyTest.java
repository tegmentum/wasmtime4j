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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CdnStrategy enum.
 *
 * <p>CdnStrategy represents different strategies for selecting CDN endpoints when streaming
 * WebAssembly modules. This test verifies the enum structure and values.
 */
@DisplayName("CdnStrategy Enum Tests")
class CdnStrategyTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(CdnStrategy.class.isEnum(), "CdnStrategy should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CdnStrategy.class.getModifiers()), "CdnStrategy should be public");
    }
  }

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have FASTEST_FIRST value")
    void shouldHaveFastestFirstValue() {
      CdnStrategy strategy = CdnStrategy.FASTEST_FIRST;
      assertNotNull(strategy, "CdnStrategy.FASTEST_FIRST should exist");
    }

    @Test
    @DisplayName("should have SEQUENTIAL value")
    void shouldHaveSequentialValue() {
      CdnStrategy strategy = CdnStrategy.SEQUENTIAL;
      assertNotNull(strategy, "CdnStrategy.SEQUENTIAL should exist");
    }

    @Test
    @DisplayName("should have GEOGRAPHIC value")
    void shouldHaveGeographicValue() {
      CdnStrategy strategy = CdnStrategy.GEOGRAPHIC;
      assertNotNull(strategy, "CdnStrategy.GEOGRAPHIC should exist");
    }

    @Test
    @DisplayName("should have LOAD_BALANCED value")
    void shouldHaveLoadBalancedValue() {
      CdnStrategy strategy = CdnStrategy.LOAD_BALANCED;
      assertNotNull(strategy, "CdnStrategy.LOAD_BALANCED should exist");
    }

    @Test
    @DisplayName("should have HIGHEST_BANDWIDTH value")
    void shouldHaveHighestBandwidthValue() {
      CdnStrategy strategy = CdnStrategy.HIGHEST_BANDWIDTH;
      assertNotNull(strategy, "CdnStrategy.HIGHEST_BANDWIDTH should exist");
    }

    @Test
    @DisplayName("should have ADAPTIVE value")
    void shouldHaveAdaptiveValue() {
      CdnStrategy strategy = CdnStrategy.ADAPTIVE;
      assertNotNull(strategy, "CdnStrategy.ADAPTIVE should exist");
    }

    @Test
    @DisplayName("should have exactly 6 values")
    void shouldHaveExactly6Values() {
      assertEquals(6, CdnStrategy.values().length, "CdnStrategy should have exactly 6 values");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("FASTEST_FIRST ordinal should be 0")
    void fastestFirstOrdinalShouldBe0() {
      assertEquals(0, CdnStrategy.FASTEST_FIRST.ordinal(), "FASTEST_FIRST ordinal should be 0");
    }

    @Test
    @DisplayName("SEQUENTIAL ordinal should be 1")
    void sequentialOrdinalShouldBe1() {
      assertEquals(1, CdnStrategy.SEQUENTIAL.ordinal(), "SEQUENTIAL ordinal should be 1");
    }

    @Test
    @DisplayName("GEOGRAPHIC ordinal should be 2")
    void geographicOrdinalShouldBe2() {
      assertEquals(2, CdnStrategy.GEOGRAPHIC.ordinal(), "GEOGRAPHIC ordinal should be 2");
    }

    @Test
    @DisplayName("LOAD_BALANCED ordinal should be 3")
    void loadBalancedOrdinalShouldBe3() {
      assertEquals(3, CdnStrategy.LOAD_BALANCED.ordinal(), "LOAD_BALANCED ordinal should be 3");
    }

    @Test
    @DisplayName("HIGHEST_BANDWIDTH ordinal should be 4")
    void highestBandwidthOrdinalShouldBe4() {
      assertEquals(
          4, CdnStrategy.HIGHEST_BANDWIDTH.ordinal(), "HIGHEST_BANDWIDTH ordinal should be 4");
    }

    @Test
    @DisplayName("ADAPTIVE ordinal should be 5")
    void adaptiveOrdinalShouldBe5() {
      assertEquals(5, CdnStrategy.ADAPTIVE.ordinal(), "ADAPTIVE ordinal should be 5");
    }
  }

  // ========================================================================
  // Name Tests
  // ========================================================================

  @Nested
  @DisplayName("Name Tests")
  class NameTests {

    @Test
    @DisplayName("FASTEST_FIRST name should be 'FASTEST_FIRST'")
    void fastestFirstNameShouldBeFastestFirst() {
      assertEquals(
          "FASTEST_FIRST",
          CdnStrategy.FASTEST_FIRST.name(),
          "FASTEST_FIRST name should be 'FASTEST_FIRST'");
    }

    @Test
    @DisplayName("SEQUENTIAL name should be 'SEQUENTIAL'")
    void sequentialNameShouldBeSequential() {
      assertEquals(
          "SEQUENTIAL", CdnStrategy.SEQUENTIAL.name(), "SEQUENTIAL name should be 'SEQUENTIAL'");
    }

    @Test
    @DisplayName("GEOGRAPHIC name should be 'GEOGRAPHIC'")
    void geographicNameShouldBeGeographic() {
      assertEquals(
          "GEOGRAPHIC", CdnStrategy.GEOGRAPHIC.name(), "GEOGRAPHIC name should be 'GEOGRAPHIC'");
    }

    @Test
    @DisplayName("LOAD_BALANCED name should be 'LOAD_BALANCED'")
    void loadBalancedNameShouldBeLoadBalanced() {
      assertEquals(
          "LOAD_BALANCED",
          CdnStrategy.LOAD_BALANCED.name(),
          "LOAD_BALANCED name should be 'LOAD_BALANCED'");
    }

    @Test
    @DisplayName("HIGHEST_BANDWIDTH name should be 'HIGHEST_BANDWIDTH'")
    void highestBandwidthNameShouldBeHighestBandwidth() {
      assertEquals(
          "HIGHEST_BANDWIDTH",
          CdnStrategy.HIGHEST_BANDWIDTH.name(),
          "HIGHEST_BANDWIDTH name should be 'HIGHEST_BANDWIDTH'");
    }

    @Test
    @DisplayName("ADAPTIVE name should be 'ADAPTIVE'")
    void adaptiveNameShouldBeAdaptive() {
      assertEquals("ADAPTIVE", CdnStrategy.ADAPTIVE.name(), "ADAPTIVE name should be 'ADAPTIVE'");
    }
  }

  // ========================================================================
  // Enum Standard Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Standard Methods Tests")
  class EnumStandardMethodsTests {

    @Test
    @DisplayName("should have values method")
    void shouldHaveValuesMethod() throws NoSuchMethodException {
      final Method method = CdnStrategy.class.getMethod("values");
      assertNotNull(method, "values method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "values should be static");
      assertEquals(
          CdnStrategy[].class, method.getReturnType(), "values should return CdnStrategy[]");
    }

    @Test
    @DisplayName("should have valueOf method")
    void shouldHaveValueOfMethod() throws NoSuchMethodException {
      final Method method = CdnStrategy.class.getMethod("valueOf", String.class);
      assertNotNull(method, "valueOf method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "valueOf should be static");
      assertEquals(CdnStrategy.class, method.getReturnType(), "valueOf should return CdnStrategy");
    }

    @Test
    @DisplayName("valueOf should work correctly")
    void valueOfShouldWorkCorrectly() {
      assertEquals(
          CdnStrategy.FASTEST_FIRST,
          CdnStrategy.valueOf("FASTEST_FIRST"),
          "valueOf('FASTEST_FIRST') should work");
      assertEquals(
          CdnStrategy.SEQUENTIAL,
          CdnStrategy.valueOf("SEQUENTIAL"),
          "valueOf('SEQUENTIAL') should work");
      assertEquals(
          CdnStrategy.GEOGRAPHIC,
          CdnStrategy.valueOf("GEOGRAPHIC"),
          "valueOf('GEOGRAPHIC') should work");
      assertEquals(
          CdnStrategy.LOAD_BALANCED,
          CdnStrategy.valueOf("LOAD_BALANCED"),
          "valueOf('LOAD_BALANCED') should work");
      assertEquals(
          CdnStrategy.HIGHEST_BANDWIDTH,
          CdnStrategy.valueOf("HIGHEST_BANDWIDTH"),
          "valueOf('HIGHEST_BANDWIDTH') should work");
      assertEquals(
          CdnStrategy.ADAPTIVE, CdnStrategy.valueOf("ADAPTIVE"), "valueOf('ADAPTIVE') should work");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods = Set.of("values", "valueOf");

      Set<String> actualMethods =
          Arrays.stream(CdnStrategy.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "CdnStrategy should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Strategy Category Tests
  // ========================================================================

  @Nested
  @DisplayName("Strategy Category Tests")
  class StrategyCategoryTests {

    @Test
    @DisplayName("should have performance-oriented strategies")
    void shouldHavePerformanceOrientedStrategies() {
      // FASTEST_FIRST and HIGHEST_BANDWIDTH are performance-oriented
      assertNotNull(CdnStrategy.FASTEST_FIRST, "Should have FASTEST_FIRST strategy");
      assertNotNull(CdnStrategy.HIGHEST_BANDWIDTH, "Should have HIGHEST_BANDWIDTH strategy");
    }

    @Test
    @DisplayName("should have distribution strategies")
    void shouldHaveDistributionStrategies() {
      // SEQUENTIAL, GEOGRAPHIC, and LOAD_BALANCED are distribution strategies
      assertNotNull(CdnStrategy.SEQUENTIAL, "Should have SEQUENTIAL strategy");
      assertNotNull(CdnStrategy.GEOGRAPHIC, "Should have GEOGRAPHIC strategy");
      assertNotNull(CdnStrategy.LOAD_BALANCED, "Should have LOAD_BALANCED strategy");
    }

    @Test
    @DisplayName("should have adaptive strategy")
    void shouldHaveAdaptiveStrategy() {
      // ADAPTIVE is a dynamic strategy
      assertNotNull(CdnStrategy.ADAPTIVE, "Should have ADAPTIVE strategy");
    }

    @Test
    @DisplayName("all strategies should have unique ordinals")
    void allStrategiesShouldHaveUniqueOrdinals() {
      Set<Integer> ordinals =
          Arrays.stream(CdnStrategy.values()).map(CdnStrategy::ordinal).collect(Collectors.toSet());

      assertEquals(
          CdnStrategy.values().length,
          ordinals.size(),
          "All strategies should have unique ordinals");
    }

    @Test
    @DisplayName("all strategies should have unique names")
    void allStrategiesShouldHaveUniqueNames() {
      Set<String> names =
          Arrays.stream(CdnStrategy.values()).map(CdnStrategy::name).collect(Collectors.toSet());

      assertEquals(
          CdnStrategy.values().length, names.size(), "All strategies should have unique names");
    }
  }
}
