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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmFeature enum.
 *
 * <p>WasmFeature represents the various WebAssembly feature flags that can be enabled or disabled
 * in the Wasmtime engine configuration. This test verifies the enum structure and values.
 */
@DisplayName("WasmFeature Enum Tests")
class WasmFeatureTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasmFeature.class.isEnum(), "WasmFeature should be an enum");
    }

    @Test
    @DisplayName("should have at least 20 values")
    void shouldHaveAtLeastTwentyValues() {
      assertTrue(
          WasmFeature.values().length >= 20,
          "WasmFeature should have at least 20 values, but has " + WasmFeature.values().length);
    }
  }

  // ========================================================================
  // Core Feature Tests
  // ========================================================================

  @Nested
  @DisplayName("Core Feature Tests")
  class CoreFeatureTests {

    @Test
    @DisplayName("should have REFERENCE_TYPES feature")
    void shouldHaveReferenceTypesFeature() {
      WasmFeature feature = WasmFeature.REFERENCE_TYPES;
      assertNotNull(feature, "REFERENCE_TYPES should exist");
      assertEquals("REFERENCE_TYPES", feature.name(), "REFERENCE_TYPES should have correct name");
    }

    @Test
    @DisplayName("should have SIMD feature")
    void shouldHaveSimdFeature() {
      WasmFeature feature = WasmFeature.SIMD;
      assertNotNull(feature, "SIMD should exist");
      assertEquals("SIMD", feature.name(), "SIMD should have correct name");
    }

    @Test
    @DisplayName("should have BULK_MEMORY feature")
    void shouldHaveBulkMemoryFeature() {
      WasmFeature feature = WasmFeature.BULK_MEMORY;
      assertNotNull(feature, "BULK_MEMORY should exist");
      assertEquals("BULK_MEMORY", feature.name(), "BULK_MEMORY should have correct name");
    }

    @Test
    @DisplayName("should have MULTI_VALUE feature")
    void shouldHaveMultiValueFeature() {
      WasmFeature feature = WasmFeature.MULTI_VALUE;
      assertNotNull(feature, "MULTI_VALUE should exist");
      assertEquals("MULTI_VALUE", feature.name(), "MULTI_VALUE should have correct name");
    }

    @Test
    @DisplayName("should have MULTI_MEMORY feature")
    void shouldHaveMultiMemoryFeature() {
      WasmFeature feature = WasmFeature.MULTI_MEMORY;
      assertNotNull(feature, "MULTI_MEMORY should exist");
      assertEquals("MULTI_MEMORY", feature.name(), "MULTI_MEMORY should have correct name");
    }
  }

  // ========================================================================
  // Advanced Feature Tests
  // ========================================================================

  @Nested
  @DisplayName("Advanced Feature Tests")
  class AdvancedFeatureTests {

    @Test
    @DisplayName("should have GC feature")
    void shouldHaveGcFeature() {
      WasmFeature feature = WasmFeature.GC;
      assertNotNull(feature, "GC should exist");
      assertEquals("GC", feature.name(), "GC should have correct name");
    }

    @Test
    @DisplayName("should have MEMORY64 feature")
    void shouldHaveMemory64Feature() {
      WasmFeature feature = WasmFeature.MEMORY64;
      assertNotNull(feature, "MEMORY64 should exist");
      assertEquals("MEMORY64", feature.name(), "MEMORY64 should have correct name");
    }

    @Test
    @DisplayName("should have THREADS feature")
    void shouldHaveThreadsFeature() {
      WasmFeature feature = WasmFeature.THREADS;
      assertNotNull(feature, "THREADS should exist");
      assertEquals("THREADS", feature.name(), "THREADS should have correct name");
    }

    @Test
    @DisplayName("should have TAIL_CALL feature")
    void shouldHaveTailCallFeature() {
      WasmFeature feature = WasmFeature.TAIL_CALL;
      assertNotNull(feature, "TAIL_CALL should exist");
      assertEquals("TAIL_CALL", feature.name(), "TAIL_CALL should have correct name");
    }

    @Test
    @DisplayName("should have RELAXED_SIMD feature")
    void shouldHaveRelaxedSimdFeature() {
      WasmFeature feature = WasmFeature.RELAXED_SIMD;
      assertNotNull(feature, "RELAXED_SIMD should exist");
      assertEquals("RELAXED_SIMD", feature.name(), "RELAXED_SIMD should have correct name");
    }

    @Test
    @DisplayName("should have EXTENDED_CONST_EXPRESSIONS feature")
    void shouldHaveExtendedConstExpressionsFeature() {
      WasmFeature feature = WasmFeature.EXTENDED_CONST_EXPRESSIONS;
      assertNotNull(feature, "EXTENDED_CONST_EXPRESSIONS should exist");
      assertEquals(
          "EXTENDED_CONST_EXPRESSIONS",
          feature.name(),
          "EXTENDED_CONST_EXPRESSIONS should have correct name");
    }
  }

  // ========================================================================
  // Component Model Feature Tests
  // ========================================================================

  @Nested
  @DisplayName("Component Model Feature Tests")
  class ComponentModelFeatureTests {

    @Test
    @DisplayName("should have COMPONENT_MODEL feature")
    void shouldHaveComponentModelFeature() {
      WasmFeature feature = WasmFeature.COMPONENT_MODEL;
      assertNotNull(feature, "COMPONENT_MODEL should exist");
      assertEquals("COMPONENT_MODEL", feature.name(), "COMPONENT_MODEL should have correct name");
    }

    @Test
    @DisplayName("should have TYPED_FUNCTION_REFERENCES feature")
    void shouldHaveTypedFunctionReferencesFeature() {
      WasmFeature feature = WasmFeature.TYPED_FUNCTION_REFERENCES;
      assertNotNull(feature, "TYPED_FUNCTION_REFERENCES should exist");
      assertEquals(
          "TYPED_FUNCTION_REFERENCES",
          feature.name(),
          "TYPED_FUNCTION_REFERENCES should have correct name");
    }
  }

  // ========================================================================
  // Exception Handling Feature Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Handling Feature Tests")
  class ExceptionHandlingFeatureTests {

    @Test
    @DisplayName("should have EXCEPTIONS feature")
    void shouldHaveExceptionsFeature() {
      WasmFeature feature = WasmFeature.EXCEPTIONS;
      assertNotNull(feature, "EXCEPTIONS should exist");
      assertEquals("EXCEPTIONS", feature.name(), "EXCEPTIONS should have correct name");
    }
  }

  // ========================================================================
  // Unique Value Tests
  // ========================================================================

  @Nested
  @DisplayName("Unique Value Tests")
  class UniqueValueTests {

    @Test
    @DisplayName("all values should have unique names")
    void allValuesShouldHaveUniqueNames() {
      Set<String> names =
          Arrays.stream(WasmFeature.values()).map(Enum::name).collect(Collectors.toSet());
      assertEquals(
          WasmFeature.values().length,
          names.size(),
          "All WasmFeature values should have unique names");
    }
  }

  // ========================================================================
  // Enum Standard Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Standard Method Tests")
  class EnumStandardMethodTests {

    @Test
    @DisplayName("valueOf should work for all values")
    void valueOfShouldWorkForAllValues() {
      for (WasmFeature feature : WasmFeature.values()) {
        assertEquals(
            feature,
            WasmFeature.valueOf(feature.name()),
            "valueOf should work for " + feature.name());
      }
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for unknown value")
    void valueOfShouldThrowForUnknownValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmFeature.valueOf("UNKNOWN_FEATURE"),
          "valueOf should throw for unknown value");
    }

    @Test
    @DisplayName("toString should return name")
    void toStringShouldReturnName() {
      for (WasmFeature feature : WasmFeature.values()) {
        assertEquals(
            feature.name(),
            feature.toString(),
            "toString should return name for " + feature.name());
      }
    }
  }

  // ========================================================================
  // Feature Category Tests
  // ========================================================================

  @Nested
  @DisplayName("Feature Category Tests")
  class FeatureCategoryTests {

    @Test
    @DisplayName("should have all core WebAssembly 1.0 features")
    void shouldHaveAllCoreWebAssembly10Features() {
      // Core WASM 1.0 features that should be present
      Set<String> coreFeatures = Set.of("MULTI_VALUE", "BULK_MEMORY");

      Set<String> actualFeatures =
          Arrays.stream(WasmFeature.values()).map(Enum::name).collect(Collectors.toSet());

      for (String feature : coreFeatures) {
        assertTrue(
            actualFeatures.contains(feature), "Core feature " + feature + " should be present");
      }
    }

    @Test
    @DisplayName("should have all WebAssembly 2.0 features")
    void shouldHaveAllWebAssembly20Features() {
      // WASM 2.0 features
      Set<String> wasm2Features = Set.of("REFERENCE_TYPES", "SIMD", "BULK_MEMORY", "MULTI_VALUE");

      Set<String> actualFeatures =
          Arrays.stream(WasmFeature.values()).map(Enum::name).collect(Collectors.toSet());

      for (String feature : wasm2Features) {
        assertTrue(
            actualFeatures.contains(feature), "WASM 2.0 feature " + feature + " should be present");
      }
    }

    @Test
    @DisplayName("should have all proposal features")
    void shouldHaveAllProposalFeatures() {
      // Proposal features (various stages)
      Set<String> proposalFeatures = Set.of("GC", "THREADS", "EXCEPTIONS", "TAIL_CALL", "MEMORY64");

      Set<String> actualFeatures =
          Arrays.stream(WasmFeature.values()).map(Enum::name).collect(Collectors.toSet());

      for (String feature : proposalFeatures) {
        assertTrue(
            actualFeatures.contains(feature), "Proposal feature " + feature + " should be present");
      }
    }
  }

  // ========================================================================
  // EnumSet Compatibility Tests
  // ========================================================================

  @Nested
  @DisplayName("EnumSet Compatibility Tests")
  class EnumSetCompatibilityTests {

    @Test
    @DisplayName("should be usable in EnumSet")
    void shouldBeUsableInEnumSet() {
      Set<WasmFeature> features = new HashSet<>();
      features.add(WasmFeature.SIMD);
      features.add(WasmFeature.THREADS);
      features.add(WasmFeature.GC);

      assertEquals(3, features.size(), "EnumSet should contain 3 features");
      assertTrue(features.contains(WasmFeature.SIMD), "EnumSet should contain SIMD");
      assertTrue(features.contains(WasmFeature.THREADS), "EnumSet should contain THREADS");
      assertTrue(features.contains(WasmFeature.GC), "EnumSet should contain GC");
    }

    @Test
    @DisplayName("should be usable with stream operations")
    void shouldBeUsableWithStreamOperations() {
      long count =
          Arrays.stream(WasmFeature.values()).filter(f -> f.name().contains("MEMORY")).count();

      assertTrue(count >= 2, "Should have at least 2 memory-related features");
    }
  }
}
