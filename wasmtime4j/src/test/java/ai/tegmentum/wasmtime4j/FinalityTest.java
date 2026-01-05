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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Finality enum.
 *
 * <p>Finality represents the finality modifier for WebAssembly GC types. Types can be declared as
 * FINAL (cannot have subtypes) or NON_FINAL (can be extended). This test verifies the enum
 * structure, values, and methods.
 */
@DisplayName("Finality Enum Tests")
class FinalityTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(Finality.class.isEnum(), "Finality should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(Finality.class.getModifiers()), "Finality should be public");
    }
  }

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have FINAL value")
    void shouldHaveFinalValue() {
      Finality finality = Finality.FINAL;
      assertNotNull(finality, "Finality.FINAL should exist");
    }

    @Test
    @DisplayName("should have NON_FINAL value")
    void shouldHaveNonFinalValue() {
      Finality finality = Finality.NON_FINAL;
      assertNotNull(finality, "Finality.NON_FINAL should exist");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactly2Values() {
      assertEquals(2, Finality.values().length, "Finality should have exactly 2 values");
    }

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      Set<String> expectedValues = Set.of("FINAL", "NON_FINAL");
      Set<String> actualValues =
          Arrays.stream(Finality.values()).map(Enum::name).collect(Collectors.toSet());
      assertEquals(expectedValues, actualValues, "Finality should have all expected values");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("FINAL ordinal should be 0")
    void finalOrdinalShouldBe0() {
      assertEquals(0, Finality.FINAL.ordinal(), "FINAL ordinal should be 0");
    }

    @Test
    @DisplayName("NON_FINAL ordinal should be 1")
    void nonFinalOrdinalShouldBe1() {
      assertEquals(1, Finality.NON_FINAL.ordinal(), "NON_FINAL ordinal should be 1");
    }
  }

  // ========================================================================
  // Name Tests
  // ========================================================================

  @Nested
  @DisplayName("Name Tests")
  class NameTests {

    @Test
    @DisplayName("FINAL name should be 'FINAL'")
    void finalNameShouldBeFinal() {
      assertEquals("FINAL", Finality.FINAL.name(), "FINAL name should be 'FINAL'");
    }

    @Test
    @DisplayName("NON_FINAL name should be 'NON_FINAL'")
    void nonFinalNameShouldBeNonFinal() {
      assertEquals("NON_FINAL", Finality.NON_FINAL.name(), "NON_FINAL name should be 'NON_FINAL'");
    }
  }

  // ========================================================================
  // getWasmKeyword Method Tests
  // ========================================================================

  @Nested
  @DisplayName("getWasmKeyword Method Tests")
  class GetWasmKeywordMethodTests {

    @Test
    @DisplayName("should have getWasmKeyword method")
    void shouldHaveGetWasmKeywordMethod() throws NoSuchMethodException {
      final Method method = Finality.class.getMethod("getWasmKeyword");
      assertNotNull(method, "getWasmKeyword method should exist");
      assertEquals(String.class, method.getReturnType(), "getWasmKeyword should return String");
    }

    @Test
    @DisplayName("FINAL should have wasm keyword 'final'")
    void finalShouldHaveWasmKeywordFinal() {
      assertEquals(
          "final", Finality.FINAL.getWasmKeyword(), "FINAL should have wasm keyword 'final'");
    }

    @Test
    @DisplayName("NON_FINAL should have wasm keyword 'sub'")
    void nonFinalShouldHaveWasmKeywordSub() {
      assertEquals(
          "sub", Finality.NON_FINAL.getWasmKeyword(), "NON_FINAL should have wasm keyword 'sub'");
    }
  }

  // ========================================================================
  // allowsSubtypes Method Tests
  // ========================================================================

  @Nested
  @DisplayName("allowsSubtypes Method Tests")
  class AllowsSubtypesMethodTests {

    @Test
    @DisplayName("should have allowsSubtypes method")
    void shouldHaveAllowsSubtypesMethod() throws NoSuchMethodException {
      final Method method = Finality.class.getMethod("allowsSubtypes");
      assertNotNull(method, "allowsSubtypes method should exist");
      assertEquals(boolean.class, method.getReturnType(), "allowsSubtypes should return boolean");
    }

    @Test
    @DisplayName("FINAL should not allow subtypes")
    void finalShouldNotAllowSubtypes() {
      assertFalse(Finality.FINAL.allowsSubtypes(), "FINAL should not allow subtypes");
    }

    @Test
    @DisplayName("NON_FINAL should allow subtypes")
    void nonFinalShouldAllowSubtypes() {
      assertTrue(Finality.NON_FINAL.allowsSubtypes(), "NON_FINAL should allow subtypes");
    }
  }

  // ========================================================================
  // isFinal Method Tests
  // ========================================================================

  @Nested
  @DisplayName("isFinal Method Tests")
  class IsFinalMethodTests {

    @Test
    @DisplayName("should have isFinal method")
    void shouldHaveIsFinalMethod() throws NoSuchMethodException {
      final Method method = Finality.class.getMethod("isFinal");
      assertNotNull(method, "isFinal method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFinal should return boolean");
    }

    @Test
    @DisplayName("FINAL should be final")
    void finalShouldBeFinal() {
      assertTrue(Finality.FINAL.isFinal(), "FINAL should be final");
    }

    @Test
    @DisplayName("NON_FINAL should not be final")
    void nonFinalShouldNotBeFinal() {
      assertFalse(Finality.NON_FINAL.isFinal(), "NON_FINAL should not be final");
    }
  }

  // ========================================================================
  // fromWasmKeyword Method Tests
  // ========================================================================

  @Nested
  @DisplayName("fromWasmKeyword Method Tests")
  class FromWasmKeywordMethodTests {

    @Test
    @DisplayName("should have fromWasmKeyword static method")
    void shouldHaveFromWasmKeywordMethod() throws NoSuchMethodException {
      final Method method = Finality.class.getMethod("fromWasmKeyword", String.class);
      assertNotNull(method, "fromWasmKeyword method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromWasmKeyword should be static");
      assertEquals(
          Finality.class, method.getReturnType(), "fromWasmKeyword should return Finality");
    }

    @Test
    @DisplayName("fromWasmKeyword('final') should return FINAL")
    void fromWasmKeywordFinalShouldReturnFinal() {
      assertEquals(
          Finality.FINAL,
          Finality.fromWasmKeyword("final"),
          "fromWasmKeyword('final') should return FINAL");
    }

    @Test
    @DisplayName("fromWasmKeyword('sub') should return NON_FINAL")
    void fromWasmKeywordSubShouldReturnNonFinal() {
      assertEquals(
          Finality.NON_FINAL,
          Finality.fromWasmKeyword("sub"),
          "fromWasmKeyword('sub') should return NON_FINAL");
    }

    @Test
    @DisplayName("fromWasmKeyword should throw IllegalArgumentException for null")
    void fromWasmKeywordShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Finality.fromWasmKeyword(null),
          "fromWasmKeyword(null) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromWasmKeyword should throw IllegalArgumentException for unknown keyword")
    void fromWasmKeywordShouldThrowForUnknown() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Finality.fromWasmKeyword("unknown"),
          "fromWasmKeyword('unknown') should throw IllegalArgumentException");
    }
  }

  // ========================================================================
  // toString Method Tests
  // ========================================================================

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("FINAL toString should return 'final'")
    void finalToStringShouldReturnFinal() {
      assertEquals("final", Finality.FINAL.toString(), "FINAL toString should return 'final'");
    }

    @Test
    @DisplayName("NON_FINAL toString should return 'sub'")
    void nonFinalToStringShouldReturnSub() {
      assertEquals("sub", Finality.NON_FINAL.toString(), "NON_FINAL toString should return 'sub'");
    }

    @Test
    @DisplayName("toString should override Object.toString")
    void toStringShouldOverrideObjectToString() throws NoSuchMethodException {
      final Method method = Finality.class.getMethod("toString");
      assertEquals(
          Finality.class, method.getDeclaringClass(), "toString should be declared in Finality");
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
      final Method method = Finality.class.getMethod("values");
      assertNotNull(method, "values method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "values should be static");
      assertEquals(Finality[].class, method.getReturnType(), "values should return Finality[]");
    }

    @Test
    @DisplayName("should have valueOf method")
    void shouldHaveValueOfMethod() throws NoSuchMethodException {
      final Method method = Finality.class.getMethod("valueOf", String.class);
      assertNotNull(method, "valueOf method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "valueOf should be static");
      assertEquals(Finality.class, method.getReturnType(), "valueOf should return Finality");
    }

    @Test
    @DisplayName("valueOf should work correctly")
    void valueOfShouldWorkCorrectly() {
      assertEquals(Finality.FINAL, Finality.valueOf("FINAL"), "valueOf('FINAL') should work");
      assertEquals(
          Finality.NON_FINAL, Finality.valueOf("NON_FINAL"), "valueOf('NON_FINAL') should work");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for unknown value")
    void valueOfShouldThrowForUnknown() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Finality.valueOf("UNKNOWN"),
          "valueOf('UNKNOWN') should throw IllegalArgumentException");
    }
  }

  // ========================================================================
  // Round-Trip Tests
  // ========================================================================

  @Nested
  @DisplayName("Round-Trip Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getWasmKeyword and fromWasmKeyword should be inverses for all values")
    void getWasmKeywordAndFromWasmKeywordShouldBeInverses() {
      for (Finality finality : Finality.values()) {
        String wasmKeyword = finality.getWasmKeyword();
        Finality roundTrip = Finality.fromWasmKeyword(wasmKeyword);
        assertEquals(
            finality,
            roundTrip,
            "Round-trip conversion should preserve value for " + finality.name());
      }
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
      Set<String> expectedMethods =
          Set.of(
              "values",
              "valueOf",
              "getWasmKeyword",
              "allowsSubtypes",
              "isFinal",
              "fromWasmKeyword",
              "toString");

      Set<String> actualMethods =
          Arrays.stream(Finality.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Finality should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Semantic Consistency Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Consistency Tests")
  class SemanticConsistencyTests {

    @Test
    @DisplayName("isFinal and allowsSubtypes should be mutually exclusive")
    void isFinalAndAllowsSubtypesShouldBeMutuallyExclusive() {
      for (Finality finality : Finality.values()) {
        // If isFinal is true, allowsSubtypes must be false, and vice versa
        assertEquals(
            !finality.isFinal(),
            finality.allowsSubtypes(),
            "isFinal and allowsSubtypes should be opposite for " + finality.name());
      }
    }

    @Test
    @DisplayName("all values should have unique wasm keywords")
    void allValuesShouldHaveUniqueWasmKeywords() {
      Set<String> wasmKeywords =
          Arrays.stream(Finality.values())
              .map(Finality::getWasmKeyword)
              .collect(Collectors.toSet());
      assertEquals(
          Finality.values().length,
          wasmKeywords.size(),
          "All Finality values should have unique wasm keywords");
    }
  }
}
