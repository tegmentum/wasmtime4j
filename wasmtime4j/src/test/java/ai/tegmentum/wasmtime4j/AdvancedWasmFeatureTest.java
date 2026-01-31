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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AdvancedWasmFeature}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("AdvancedWasmFeature Tests")
class AdvancedWasmFeatureTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(AdvancedWasmFeature.class.isEnum(),
          "AdvancedWasmFeature should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 15 values")
    void shouldHaveExactValueCount() {
      assertEquals(15, AdvancedWasmFeature.values().length,
          "AdvancedWasmFeature should have exactly 15 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain EXCEPTIONS")
    void shouldContainExceptions() {
      assertNotNull(AdvancedWasmFeature.EXCEPTIONS,
          "EXCEPTIONS constant should exist");
      assertEquals("EXCEPTIONS", AdvancedWasmFeature.EXCEPTIONS.name(),
          "EXCEPTIONS name should match");
    }

    @Test
    @DisplayName("should contain SIMD_ARITHMETIC")
    void shouldContainSimdArithmetic() {
      assertNotNull(AdvancedWasmFeature.SIMD_ARITHMETIC,
          "SIMD_ARITHMETIC constant should exist");
      assertEquals("SIMD_ARITHMETIC", AdvancedWasmFeature.SIMD_ARITHMETIC.name(),
          "SIMD_ARITHMETIC name should match");
    }

    @Test
    @DisplayName("should contain SIMD_MEMORY")
    void shouldContainSimdMemory() {
      assertNotNull(AdvancedWasmFeature.SIMD_MEMORY,
          "SIMD_MEMORY constant should exist");
    }

    @Test
    @DisplayName("should contain SIMD_MANIPULATION")
    void shouldContainSimdManipulation() {
      assertNotNull(AdvancedWasmFeature.SIMD_MANIPULATION,
          "SIMD_MANIPULATION constant should exist");
    }

    @Test
    @DisplayName("should contain ATOMIC_OPERATIONS")
    void shouldContainAtomicOperations() {
      assertNotNull(AdvancedWasmFeature.ATOMIC_OPERATIONS,
          "ATOMIC_OPERATIONS constant should exist");
    }

    @Test
    @DisplayName("should contain ATOMIC_CAS")
    void shouldContainAtomicCas() {
      assertNotNull(AdvancedWasmFeature.ATOMIC_CAS,
          "ATOMIC_CAS constant should exist");
    }

    @Test
    @DisplayName("should contain SHARED_MEMORY")
    void shouldContainSharedMemory() {
      assertNotNull(AdvancedWasmFeature.SHARED_MEMORY,
          "SHARED_MEMORY constant should exist");
    }

    @Test
    @DisplayName("should contain MEMORY_ORDERING")
    void shouldContainMemoryOrdering() {
      assertNotNull(AdvancedWasmFeature.MEMORY_ORDERING,
          "MEMORY_ORDERING constant should exist");
    }

    @Test
    @DisplayName("should contain CROSS_MODULE_EXCEPTIONS")
    void shouldContainCrossModuleExceptions() {
      assertNotNull(AdvancedWasmFeature.CROSS_MODULE_EXCEPTIONS,
          "CROSS_MODULE_EXCEPTIONS constant should exist");
    }

    @Test
    @DisplayName("should contain NESTED_EXCEPTIONS")
    void shouldContainNestedException() {
      assertNotNull(AdvancedWasmFeature.NESTED_EXCEPTIONS,
          "NESTED_EXCEPTIONS constant should exist");
    }

    @Test
    @DisplayName("should contain EXCEPTION_TYPES")
    void shouldContainExceptionTypes() {
      assertNotNull(AdvancedWasmFeature.EXCEPTION_TYPES,
          "EXCEPTION_TYPES constant should exist");
    }

    @Test
    @DisplayName("should contain THREAD_SAFETY")
    void shouldContainThreadSafety() {
      assertNotNull(AdvancedWasmFeature.THREAD_SAFETY,
          "THREAD_SAFETY constant should exist");
    }

    @Test
    @DisplayName("should contain SIMD_PERFORMANCE")
    void shouldContainSimdPerformance() {
      assertNotNull(AdvancedWasmFeature.SIMD_PERFORMANCE,
          "SIMD_PERFORMANCE constant should exist");
    }

    @Test
    @DisplayName("should contain ATOMIC_PERFORMANCE")
    void shouldContainAtomicPerformance() {
      assertNotNull(AdvancedWasmFeature.ATOMIC_PERFORMANCE,
          "ATOMIC_PERFORMANCE constant should exist");
    }

    @Test
    @DisplayName("should contain EXCEPTION_PERFORMANCE")
    void shouldContainExceptionPerformance() {
      assertNotNull(AdvancedWasmFeature.EXCEPTION_PERFORMANCE,
          "EXCEPTION_PERFORMANCE constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final AdvancedWasmFeature value : AdvancedWasmFeature.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(AdvancedWasmFeature.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final AdvancedWasmFeature[] values = AdvancedWasmFeature.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final AdvancedWasmFeature value : AdvancedWasmFeature.values()) {
        assertEquals(value, AdvancedWasmFeature.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> AdvancedWasmFeature.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final AdvancedWasmFeature[] first = AdvancedWasmFeature.values();
      final AdvancedWasmFeature[] second = AdvancedWasmFeature.values();
      assertTrue(first != second,
          "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final AdvancedWasmFeature value : AdvancedWasmFeature.values()) {
        assertEquals(value.name(), value.toString(),
            "toString should return the enum name for " + value.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support switch statement over all values")
    void shouldSupportSwitchStatement() {
      for (final AdvancedWasmFeature feature : AdvancedWasmFeature.values()) {
        final String result;
        switch (feature) {
          case EXCEPTIONS:
          case SIMD_ARITHMETIC:
          case SIMD_MEMORY:
          case SIMD_MANIPULATION:
          case ATOMIC_OPERATIONS:
          case ATOMIC_CAS:
          case SHARED_MEMORY:
          case MEMORY_ORDERING:
          case CROSS_MODULE_EXCEPTIONS:
          case NESTED_EXCEPTIONS:
          case EXCEPTION_TYPES:
          case THREAD_SAFETY:
          case SIMD_PERFORMANCE:
          case ATOMIC_PERFORMANCE:
          case EXCEPTION_PERFORMANCE:
            result = feature.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(feature.name(), result,
            "Switch should handle " + feature.name());
      }
    }
  }
}
