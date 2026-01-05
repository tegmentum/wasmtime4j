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
 * Comprehensive test suite for the Collector enum.
 *
 * <p>Collector represents different garbage collector implementations available in Wasmtime. This
 * test verifies the enum structure, values, and methods.
 */
@DisplayName("Collector Enum Tests")
class CollectorTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(Collector.class.isEnum(), "Collector should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Collector.class.getModifiers()), "Collector should be public");
    }
  }

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have AUTO value")
    void shouldHaveAutoValue() {
      Collector collector = Collector.AUTO;
      assertNotNull(collector, "Collector.AUTO should exist");
    }

    @Test
    @DisplayName("should have DEFERRED_REFERENCE_COUNTING value")
    void shouldHaveDeferredReferenceCountingValue() {
      Collector collector = Collector.DEFERRED_REFERENCE_COUNTING;
      assertNotNull(collector, "Collector.DEFERRED_REFERENCE_COUNTING should exist");
    }

    @Test
    @DisplayName("should have NULL value")
    void shouldHaveNullValue() {
      Collector collector = Collector.NULL;
      assertNotNull(collector, "Collector.NULL should exist");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactly3Values() {
      assertEquals(3, Collector.values().length, "Collector should have exactly 3 values");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("AUTO ordinal should be 0")
    void autoOrdinalShouldBe0() {
      assertEquals(0, Collector.AUTO.ordinal(), "AUTO ordinal should be 0");
    }

    @Test
    @DisplayName("DEFERRED_REFERENCE_COUNTING ordinal should be 1")
    void deferredReferenceCountingOrdinalShouldBe1() {
      assertEquals(
          1,
          Collector.DEFERRED_REFERENCE_COUNTING.ordinal(),
          "DEFERRED_REFERENCE_COUNTING ordinal should be 1");
    }

    @Test
    @DisplayName("NULL ordinal should be 2")
    void nullOrdinalShouldBe2() {
      assertEquals(2, Collector.NULL.ordinal(), "NULL ordinal should be 2");
    }
  }

  // ========================================================================
  // Name Tests
  // ========================================================================

  @Nested
  @DisplayName("Name Tests")
  class NameTests {

    @Test
    @DisplayName("AUTO name should be 'AUTO'")
    void autoNameShouldBeAuto() {
      assertEquals("AUTO", Collector.AUTO.name(), "AUTO name should be 'AUTO'");
    }

    @Test
    @DisplayName("DEFERRED_REFERENCE_COUNTING name should be 'DEFERRED_REFERENCE_COUNTING'")
    void deferredReferenceCountingNameShouldBeDeferredReferenceCounting() {
      assertEquals(
          "DEFERRED_REFERENCE_COUNTING",
          Collector.DEFERRED_REFERENCE_COUNTING.name(),
          "DEFERRED_REFERENCE_COUNTING name should be 'DEFERRED_REFERENCE_COUNTING'");
    }

    @Test
    @DisplayName("NULL name should be 'NULL'")
    void nullNameShouldBeNull() {
      assertEquals("NULL", Collector.NULL.name(), "NULL name should be 'NULL'");
    }
  }

  // ========================================================================
  // Native Code Tests
  // ========================================================================

  @Nested
  @DisplayName("Native Code Tests")
  class NativeCodeTests {

    @Test
    @DisplayName("should have toNativeCode method")
    void shouldHaveToNativeCodeMethod() throws NoSuchMethodException {
      final Method method = Collector.class.getMethod("toNativeCode");
      assertNotNull(method, "toNativeCode method should exist");
      assertEquals(int.class, method.getReturnType(), "toNativeCode should return int");
    }

    @Test
    @DisplayName("AUTO native code should be 0")
    void autoNativeCodeShouldBe0() {
      assertEquals(0, Collector.AUTO.toNativeCode(), "AUTO native code should be 0");
    }

    @Test
    @DisplayName("DEFERRED_REFERENCE_COUNTING native code should be 1")
    void deferredReferenceCountingNativeCodeShouldBe1() {
      assertEquals(
          1,
          Collector.DEFERRED_REFERENCE_COUNTING.toNativeCode(),
          "DEFERRED_REFERENCE_COUNTING native code should be 1");
    }

    @Test
    @DisplayName("NULL native code should be 2")
    void nullNativeCodeShouldBe2() {
      assertEquals(2, Collector.NULL.toNativeCode(), "NULL native code should be 2");
    }

    @Test
    @DisplayName("should have fromNativeCode method")
    void shouldHaveFromNativeCodeMethod() throws NoSuchMethodException {
      final Method method = Collector.class.getMethod("fromNativeCode", int.class);
      assertNotNull(method, "fromNativeCode method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromNativeCode should be static");
      assertEquals(
          Collector.class, method.getReturnType(), "fromNativeCode should return Collector");
    }

    @Test
    @DisplayName("fromNativeCode(0) should return AUTO")
    void fromNativeCode0ShouldReturnAuto() {
      assertEquals(
          Collector.AUTO, Collector.fromNativeCode(0), "fromNativeCode(0) should return AUTO");
    }

    @Test
    @DisplayName("fromNativeCode(1) should return DEFERRED_REFERENCE_COUNTING")
    void fromNativeCode1ShouldReturnDeferredReferenceCounting() {
      assertEquals(
          Collector.DEFERRED_REFERENCE_COUNTING,
          Collector.fromNativeCode(1),
          "fromNativeCode(1) should return DEFERRED_REFERENCE_COUNTING");
    }

    @Test
    @DisplayName("fromNativeCode(2) should return NULL")
    void fromNativeCode2ShouldReturnNull() {
      assertEquals(
          Collector.NULL, Collector.fromNativeCode(2), "fromNativeCode(2) should return NULL");
    }

    @Test
    @DisplayName("fromNativeCode with invalid code should return AUTO")
    void fromNativeCodeWithInvalidCodeShouldReturnAuto() {
      assertEquals(
          Collector.AUTO,
          Collector.fromNativeCode(999),
          "fromNativeCode with invalid code should return AUTO");
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
      final Method method = Collector.class.getMethod("values");
      assertNotNull(method, "values method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "values should be static");
      assertEquals(Collector[].class, method.getReturnType(), "values should return Collector[]");
    }

    @Test
    @DisplayName("should have valueOf method")
    void shouldHaveValueOfMethod() throws NoSuchMethodException {
      final Method method = Collector.class.getMethod("valueOf", String.class);
      assertNotNull(method, "valueOf method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "valueOf should be static");
      assertEquals(Collector.class, method.getReturnType(), "valueOf should return Collector");
    }

    @Test
    @DisplayName("valueOf should work correctly")
    void valueOfShouldWorkCorrectly() {
      assertEquals(Collector.AUTO, Collector.valueOf("AUTO"), "valueOf('AUTO') should work");
      assertEquals(
          Collector.DEFERRED_REFERENCE_COUNTING,
          Collector.valueOf("DEFERRED_REFERENCE_COUNTING"),
          "valueOf('DEFERRED_REFERENCE_COUNTING') should work");
      assertEquals(Collector.NULL, Collector.valueOf("NULL"), "valueOf('NULL') should work");
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
      Set<String> expectedMethods = Set.of("values", "valueOf", "toNativeCode", "fromNativeCode");

      Set<String> actualMethods =
          Arrays.stream(Collector.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Collector should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Collector Category Tests
  // ========================================================================

  @Nested
  @DisplayName("Collector Category Tests")
  class CollectorCategoryTests {

    @Test
    @DisplayName("should have automatic selection strategy")
    void shouldHaveAutomaticSelectionStrategy() {
      assertNotNull(Collector.AUTO, "Should have AUTO collector for automatic selection");
    }

    @Test
    @DisplayName("should have reference counting collector")
    void shouldHaveReferenceCountingCollector() {
      assertNotNull(
          Collector.DEFERRED_REFERENCE_COUNTING,
          "Should have DEFERRED_REFERENCE_COUNTING collector");
    }

    @Test
    @DisplayName("should have null collector")
    void shouldHaveNullCollector() {
      assertNotNull(Collector.NULL, "Should have NULL collector for no collection");
    }

    @Test
    @DisplayName("all collectors should have unique ordinals")
    void allCollectorsShouldHaveUniqueOrdinals() {
      Set<Integer> ordinals =
          Arrays.stream(Collector.values()).map(Collector::ordinal).collect(Collectors.toSet());

      assertEquals(
          Collector.values().length, ordinals.size(), "All collectors should have unique ordinals");
    }

    @Test
    @DisplayName("all collectors should have unique native codes")
    void allCollectorsShouldHaveUniqueNativeCodes() {
      Set<Integer> nativeCodes =
          Arrays.stream(Collector.values())
              .map(Collector::toNativeCode)
              .collect(Collectors.toSet());

      assertEquals(
          Collector.values().length,
          nativeCodes.size(),
          "All collectors should have unique native codes");
    }

    @Test
    @DisplayName("all collectors should have unique names")
    void allCollectorsShouldHaveUniqueNames() {
      Set<String> names =
          Arrays.stream(Collector.values()).map(Collector::name).collect(Collectors.toSet());

      assertEquals(
          Collector.values().length, names.size(), "All collectors should have unique names");
    }
  }
}
