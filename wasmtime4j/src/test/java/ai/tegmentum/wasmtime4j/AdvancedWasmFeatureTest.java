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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the AdvancedWasmFeature enum.
 *
 * <p>AdvancedWasmFeature represents advanced WebAssembly features for testing and validation,
 * focusing on cutting-edge capabilities that require comprehensive testing coverage.
 */
@DisplayName("AdvancedWasmFeature Enum Tests")
class AdvancedWasmFeatureTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(AdvancedWasmFeature.class.isEnum(), "AdvancedWasmFeature should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(AdvancedWasmFeature.class.getModifiers()),
          "AdvancedWasmFeature should be public");
    }

    @Test
    @DisplayName("should be final (enums are implicitly final)")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(AdvancedWasmFeature.class.getModifiers()),
          "AdvancedWasmFeature should be final (enums are implicitly final)");
    }
  }

  // ========================================================================
  // Enum Constants Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Constants Tests")
  class EnumConstantsTests {

    @Test
    @DisplayName("should have EXCEPTIONS constant")
    void shouldHaveExceptionsConstant() {
      assertNotNull(AdvancedWasmFeature.EXCEPTIONS, "EXCEPTIONS constant should exist");
    }

    @Test
    @DisplayName("should have SIMD_ARITHMETIC constant")
    void shouldHaveSimdArithmeticConstant() {
      assertNotNull(AdvancedWasmFeature.SIMD_ARITHMETIC, "SIMD_ARITHMETIC constant should exist");
    }

    @Test
    @DisplayName("should have SIMD_MEMORY constant")
    void shouldHaveSimdMemoryConstant() {
      assertNotNull(AdvancedWasmFeature.SIMD_MEMORY, "SIMD_MEMORY constant should exist");
    }

    @Test
    @DisplayName("should have SIMD_MANIPULATION constant")
    void shouldHaveSimdManipulationConstant() {
      assertNotNull(
          AdvancedWasmFeature.SIMD_MANIPULATION, "SIMD_MANIPULATION constant should exist");
    }

    @Test
    @DisplayName("should have ATOMIC_OPERATIONS constant")
    void shouldHaveAtomicOperationsConstant() {
      assertNotNull(
          AdvancedWasmFeature.ATOMIC_OPERATIONS, "ATOMIC_OPERATIONS constant should exist");
    }

    @Test
    @DisplayName("should have ATOMIC_CAS constant")
    void shouldHaveAtomicCasConstant() {
      assertNotNull(AdvancedWasmFeature.ATOMIC_CAS, "ATOMIC_CAS constant should exist");
    }

    @Test
    @DisplayName("should have SHARED_MEMORY constant")
    void shouldHaveSharedMemoryConstant() {
      assertNotNull(AdvancedWasmFeature.SHARED_MEMORY, "SHARED_MEMORY constant should exist");
    }

    @Test
    @DisplayName("should have MEMORY_ORDERING constant")
    void shouldHaveMemoryOrderingConstant() {
      assertNotNull(AdvancedWasmFeature.MEMORY_ORDERING, "MEMORY_ORDERING constant should exist");
    }

    @Test
    @DisplayName("should have CROSS_MODULE_EXCEPTIONS constant")
    void shouldHaveCrossModuleExceptionsConstant() {
      assertNotNull(
          AdvancedWasmFeature.CROSS_MODULE_EXCEPTIONS,
          "CROSS_MODULE_EXCEPTIONS constant should exist");
    }

    @Test
    @DisplayName("should have NESTED_EXCEPTIONS constant")
    void shouldHaveNestedExceptionsConstant() {
      assertNotNull(
          AdvancedWasmFeature.NESTED_EXCEPTIONS, "NESTED_EXCEPTIONS constant should exist");
    }

    @Test
    @DisplayName("should have EXCEPTION_TYPES constant")
    void shouldHaveExceptionTypesConstant() {
      assertNotNull(AdvancedWasmFeature.EXCEPTION_TYPES, "EXCEPTION_TYPES constant should exist");
    }

    @Test
    @DisplayName("should have THREAD_SAFETY constant")
    void shouldHaveThreadSafetyConstant() {
      assertNotNull(AdvancedWasmFeature.THREAD_SAFETY, "THREAD_SAFETY constant should exist");
    }

    @Test
    @DisplayName("should have SIMD_PERFORMANCE constant")
    void shouldHaveSimdPerformanceConstant() {
      assertNotNull(AdvancedWasmFeature.SIMD_PERFORMANCE, "SIMD_PERFORMANCE constant should exist");
    }

    @Test
    @DisplayName("should have ATOMIC_PERFORMANCE constant")
    void shouldHaveAtomicPerformanceConstant() {
      assertNotNull(
          AdvancedWasmFeature.ATOMIC_PERFORMANCE, "ATOMIC_PERFORMANCE constant should exist");
    }

    @Test
    @DisplayName("should have EXCEPTION_PERFORMANCE constant")
    void shouldHaveExceptionPerformanceConstant() {
      assertNotNull(
          AdvancedWasmFeature.EXCEPTION_PERFORMANCE, "EXCEPTION_PERFORMANCE constant should exist");
    }

    @Test
    @DisplayName("should have exactly 15 enum constants")
    void shouldHaveExactly15EnumConstants() {
      assertEquals(
          15,
          AdvancedWasmFeature.values().length,
          "AdvancedWasmFeature should have exactly 15 constants");
    }

    @Test
    @DisplayName("should have all expected enum constants")
    void shouldHaveAllExpectedEnumConstants() {
      Set<String> expectedConstants =
          Set.of(
              "EXCEPTIONS",
              "SIMD_ARITHMETIC",
              "SIMD_MEMORY",
              "SIMD_MANIPULATION",
              "ATOMIC_OPERATIONS",
              "ATOMIC_CAS",
              "SHARED_MEMORY",
              "MEMORY_ORDERING",
              "CROSS_MODULE_EXCEPTIONS",
              "NESTED_EXCEPTIONS",
              "EXCEPTION_TYPES",
              "THREAD_SAFETY",
              "SIMD_PERFORMANCE",
              "ATOMIC_PERFORMANCE",
              "EXCEPTION_PERFORMANCE");

      Set<String> actualConstants =
          Arrays.stream(AdvancedWasmFeature.values()).map(Enum::name).collect(Collectors.toSet());

      assertEquals(expectedConstants, actualConstants, "Should have all expected enum constants");
    }
  }

  // ========================================================================
  // Enum Behavior Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Behavior Tests")
  class EnumBehaviorTests {

    @Test
    @DisplayName("valueOf should return correct constant for EXCEPTIONS")
    void valueOfShouldReturnCorrectConstantForExceptions() {
      assertEquals(
          AdvancedWasmFeature.EXCEPTIONS,
          AdvancedWasmFeature.valueOf("EXCEPTIONS"),
          "valueOf should return EXCEPTIONS");
    }

    @Test
    @DisplayName("valueOf should return correct constant for SIMD_ARITHMETIC")
    void valueOfShouldReturnCorrectConstantForSimdArithmetic() {
      assertEquals(
          AdvancedWasmFeature.SIMD_ARITHMETIC,
          AdvancedWasmFeature.valueOf("SIMD_ARITHMETIC"),
          "valueOf should return SIMD_ARITHMETIC");
    }

    @Test
    @DisplayName("name should return correct string for each constant")
    void nameShouldReturnCorrectString() {
      assertEquals("EXCEPTIONS", AdvancedWasmFeature.EXCEPTIONS.name());
      assertEquals("SIMD_ARITHMETIC", AdvancedWasmFeature.SIMD_ARITHMETIC.name());
      assertEquals("ATOMIC_OPERATIONS", AdvancedWasmFeature.ATOMIC_OPERATIONS.name());
    }

    @Test
    @DisplayName("ordinal should return correct index")
    void ordinalShouldReturnCorrectIndex() {
      assertEquals(0, AdvancedWasmFeature.EXCEPTIONS.ordinal(), "EXCEPTIONS should be at index 0");
      assertEquals(
          1, AdvancedWasmFeature.SIMD_ARITHMETIC.ordinal(), "SIMD_ARITHMETIC should be at index 1");
    }

    @Test
    @DisplayName("toString should return enum name")
    void toStringShouldReturnEnumName() {
      assertEquals("EXCEPTIONS", AdvancedWasmFeature.EXCEPTIONS.toString());
      assertEquals("SIMD_ARITHMETIC", AdvancedWasmFeature.SIMD_ARITHMETIC.toString());
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Enum")
    void shouldExtendEnum() {
      assertTrue(
          Enum.class.isAssignableFrom(AdvancedWasmFeature.class),
          "AdvancedWasmFeature should extend Enum");
    }

    @Test
    @DisplayName("should not implement any additional interfaces")
    void shouldNotImplementAnyAdditionalInterfaces() {
      // Enums only implement Comparable and Serializable by default
      Class<?>[] interfaces = AdvancedWasmFeature.class.getInterfaces();
      assertEquals(
          0,
          interfaces.length,
          "AdvancedWasmFeature should not implement additional interfaces directly");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("values should return all constants")
    void valuesShouldReturnAllConstants() {
      AdvancedWasmFeature[] values = AdvancedWasmFeature.values();
      assertNotNull(values, "values() should not return null");
      assertEquals(15, values.length, "values() should return 15 constants");
    }

    @Test
    @DisplayName("values should return constants in declaration order")
    void valuesShouldReturnConstantsInDeclarationOrder() {
      AdvancedWasmFeature[] values = AdvancedWasmFeature.values();
      assertEquals(AdvancedWasmFeature.EXCEPTIONS, values[0]);
      assertEquals(AdvancedWasmFeature.SIMD_ARITHMETIC, values[1]);
      assertEquals(AdvancedWasmFeature.SIMD_MEMORY, values[2]);
    }
  }

  // ========================================================================
  // Category Tests
  // ========================================================================

  @Nested
  @DisplayName("Category Tests")
  class CategoryTests {

    @Test
    @DisplayName("should have exception-related features")
    void shouldHaveExceptionRelatedFeatures() {
      Set<AdvancedWasmFeature> exceptionFeatures =
          Set.of(
              AdvancedWasmFeature.EXCEPTIONS,
              AdvancedWasmFeature.CROSS_MODULE_EXCEPTIONS,
              AdvancedWasmFeature.NESTED_EXCEPTIONS,
              AdvancedWasmFeature.EXCEPTION_TYPES,
              AdvancedWasmFeature.EXCEPTION_PERFORMANCE);

      for (AdvancedWasmFeature feature : exceptionFeatures) {
        assertTrue(
            Arrays.asList(AdvancedWasmFeature.values()).contains(feature),
            "Should contain exception feature: " + feature);
      }
    }

    @Test
    @DisplayName("should have SIMD-related features")
    void shouldHaveSimdRelatedFeatures() {
      Set<AdvancedWasmFeature> simdFeatures =
          Set.of(
              AdvancedWasmFeature.SIMD_ARITHMETIC,
              AdvancedWasmFeature.SIMD_MEMORY,
              AdvancedWasmFeature.SIMD_MANIPULATION,
              AdvancedWasmFeature.SIMD_PERFORMANCE);

      for (AdvancedWasmFeature feature : simdFeatures) {
        assertTrue(
            Arrays.asList(AdvancedWasmFeature.values()).contains(feature),
            "Should contain SIMD feature: " + feature);
      }
    }

    @Test
    @DisplayName("should have atomic-related features")
    void shouldHaveAtomicRelatedFeatures() {
      Set<AdvancedWasmFeature> atomicFeatures =
          Set.of(
              AdvancedWasmFeature.ATOMIC_OPERATIONS,
              AdvancedWasmFeature.ATOMIC_CAS,
              AdvancedWasmFeature.ATOMIC_PERFORMANCE);

      for (AdvancedWasmFeature feature : atomicFeatures) {
        assertTrue(
            Arrays.asList(AdvancedWasmFeature.values()).contains(feature),
            "Should contain atomic feature: " + feature);
      }
    }

    @Test
    @DisplayName("should have memory-related features")
    void shouldHaveMemoryRelatedFeatures() {
      Set<AdvancedWasmFeature> memoryFeatures =
          Set.of(AdvancedWasmFeature.SHARED_MEMORY, AdvancedWasmFeature.MEMORY_ORDERING);

      for (AdvancedWasmFeature feature : memoryFeatures) {
        assertTrue(
            Arrays.asList(AdvancedWasmFeature.values()).contains(feature),
            "Should contain memory feature: " + feature);
      }
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no additional instance fields")
    void shouldHaveNoAdditionalInstanceFields() {
      // Enums have synthetic fields for values, we check for non-enum fields
      long nonEnumFields =
          Arrays.stream(AdvancedWasmFeature.class.getDeclaredFields())
              .filter(f -> !f.isEnumConstant())
              .filter(f -> !f.isSynthetic())
              .filter(f -> !f.getName().equals("$VALUES"))
              .count();
      assertEquals(
          0, nonEnumFields, "AdvancedWasmFeature should have no additional instance fields");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          AdvancedWasmFeature.class.getDeclaredClasses().length,
          "AdvancedWasmFeature should have no nested classes");
    }
  }

  // ========================================================================
  // Comparison Tests
  // ========================================================================

  @Nested
  @DisplayName("Comparison Tests")
  class ComparisonTests {

    @Test
    @DisplayName("enum constants should be comparable")
    void enumConstantsShouldBeComparable() {
      assertTrue(
          AdvancedWasmFeature.EXCEPTIONS.compareTo(AdvancedWasmFeature.SIMD_ARITHMETIC) < 0,
          "EXCEPTIONS should come before SIMD_ARITHMETIC");
      assertTrue(
          AdvancedWasmFeature.SIMD_ARITHMETIC.compareTo(AdvancedWasmFeature.EXCEPTIONS) > 0,
          "SIMD_ARITHMETIC should come after EXCEPTIONS");
      assertEquals(
          0,
          AdvancedWasmFeature.EXCEPTIONS.compareTo(AdvancedWasmFeature.EXCEPTIONS),
          "Same constant should compare equal");
    }

    @Test
    @DisplayName("enum constants should support equals")
    void enumConstantsShouldSupportEquals() {
      assertEquals(AdvancedWasmFeature.EXCEPTIONS, AdvancedWasmFeature.EXCEPTIONS);
      assertFalse(AdvancedWasmFeature.EXCEPTIONS.equals(AdvancedWasmFeature.SIMD_ARITHMETIC));
    }
  }
}
