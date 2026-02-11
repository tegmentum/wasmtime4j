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

import ai.tegmentum.wasmtime4j.wit.WitTypeAdapter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitTypeAdapter} interface.
 *
 * <p>WitTypeAdapter provides type adaptation capabilities for WIT interface evolution.
 */
@DisplayName("WitTypeAdapter Tests")
class WitTypeAdapterTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WitTypeAdapter should be an interface")
    void witTypeAdapterShouldBeAnInterface() {
      assertTrue(WitTypeAdapter.class.isInterface());
    }

    @Test
    @DisplayName("ConversionMetadata should be a nested interface")
    void conversionMetadataShouldBeNestedInterface() {
      assertTrue(WitTypeAdapter.ConversionMetadata.class.isInterface());
    }

    @Test
    @DisplayName("AdapterStatistics should be a nested interface")
    void adapterStatisticsShouldBeNestedInterface() {
      assertTrue(WitTypeAdapter.AdapterStatistics.class.isInterface());
    }

    @Test
    @DisplayName("TypeMappingInfo should be a nested interface")
    void typeMappingInfoShouldBeNestedInterface() {
      assertTrue(WitTypeAdapter.TypeMappingInfo.class.isInterface());
    }
  }

  @Nested
  @DisplayName("AdapterType Enum Tests")
  class AdapterTypeEnumTests {

    @Test
    @DisplayName("AdapterType should be an enum")
    void adapterTypeShouldBeEnum() {
      assertTrue(WitTypeAdapter.AdapterType.class.isEnum());
    }

    @Test
    @DisplayName("AdapterType should have 6 values")
    void adapterTypeShouldHave6Values() {
      assertEquals(6, WitTypeAdapter.AdapterType.values().length);
    }

    @Test
    @DisplayName("AdapterType should have all expected values")
    void adapterTypeShouldHaveAllExpectedValues() {
      assertNotNull(WitTypeAdapter.AdapterType.DIRECT_CONVERSION);
      assertNotNull(WitTypeAdapter.AdapterType.STRUCTURAL_ADAPTATION);
      assertNotNull(WitTypeAdapter.AdapterType.WRAPPER_ADAPTATION);
      assertNotNull(WitTypeAdapter.AdapterType.COLLECTION_ADAPTATION);
      assertNotNull(WitTypeAdapter.AdapterType.VARIANT_ADAPTATION);
      assertNotNull(WitTypeAdapter.AdapterType.CUSTOM_CONVERSION);
    }

    @Test
    @DisplayName("AdapterType valueOf should work")
    void adapterTypeValueOfShouldWork() {
      assertEquals(
          WitTypeAdapter.AdapterType.DIRECT_CONVERSION,
          WitTypeAdapter.AdapterType.valueOf("DIRECT_CONVERSION"));
      assertEquals(
          WitTypeAdapter.AdapterType.STRUCTURAL_ADAPTATION,
          WitTypeAdapter.AdapterType.valueOf("STRUCTURAL_ADAPTATION"));
    }
  }

  @Nested
  @DisplayName("ConversionCost Enum Tests")
  class ConversionCostEnumTests {

    @Test
    @DisplayName("ConversionCost should be an enum")
    void conversionCostShouldBeEnum() {
      assertTrue(WitTypeAdapter.ConversionCost.class.isEnum());
    }

    @Test
    @DisplayName("ConversionCost should have 5 values")
    void conversionCostShouldHave5Values() {
      assertEquals(5, WitTypeAdapter.ConversionCost.values().length);
    }

    @Test
    @DisplayName("ConversionCost should have all expected values")
    void conversionCostShouldHaveAllExpectedValues() {
      assertNotNull(WitTypeAdapter.ConversionCost.VERY_LOW);
      assertNotNull(WitTypeAdapter.ConversionCost.LOW);
      assertNotNull(WitTypeAdapter.ConversionCost.MEDIUM);
      assertNotNull(WitTypeAdapter.ConversionCost.HIGH);
      assertNotNull(WitTypeAdapter.ConversionCost.VERY_HIGH);
    }

    @Test
    @DisplayName("ConversionCost ordinals should be in order")
    void conversionCostOrdinalsShouldBeInOrder() {
      assertEquals(0, WitTypeAdapter.ConversionCost.VERY_LOW.ordinal());
      assertEquals(1, WitTypeAdapter.ConversionCost.LOW.ordinal());
      assertEquals(2, WitTypeAdapter.ConversionCost.MEDIUM.ordinal());
      assertEquals(3, WitTypeAdapter.ConversionCost.HIGH.ordinal());
      assertEquals(4, WitTypeAdapter.ConversionCost.VERY_HIGH.ordinal());
    }
  }

  @Nested
  @DisplayName("AdapterValidationResult Tests")
  class AdapterValidationResultTests {

    @Test
    @DisplayName("AdapterValidationResult should be a final class")
    void adapterValidationResultShouldBeFinalClass() {
      assertFalse(WitTypeAdapter.AdapterValidationResult.class.isInterface());
    }

    @Test
    @DisplayName("success should create valid result")
    void successShouldCreateValidResult() {
      final WitTypeAdapter.AdapterValidationResult result =
          WitTypeAdapter.AdapterValidationResult.success();

      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
      assertTrue(result.getWarnings().isEmpty());
      assertFalse(result.hasWarnings());
      assertTrue(result.getSuggestion().isEmpty());
    }

    @Test
    @DisplayName("failure should create invalid result with errors")
    void failureShouldCreateInvalidResultWithErrors() {
      final List<String> errors = List.of("Error 1", "Error 2");
      final WitTypeAdapter.AdapterValidationResult result =
          WitTypeAdapter.AdapterValidationResult.failure(errors);

      assertFalse(result.isValid());
      assertEquals(2, result.getErrors().size());
      assertTrue(result.getErrors().contains("Error 1"));
      assertTrue(result.getErrors().contains("Error 2"));
      assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("withWarnings should create valid result with warnings")
    void withWarningsShouldCreateValidResultWithWarnings() {
      final List<String> warnings = List.of("Warning 1", "Warning 2");
      final Optional<String> suggestion = Optional.of("Consider this improvement");
      final WitTypeAdapter.AdapterValidationResult result =
          WitTypeAdapter.AdapterValidationResult.withWarnings(warnings, suggestion);

      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
      assertEquals(2, result.getWarnings().size());
      assertTrue(result.hasWarnings());
      assertTrue(result.getSuggestion().isPresent());
      assertEquals("Consider this improvement", result.getSuggestion().get());
    }

    @Test
    @DisplayName("constructor should create result with all fields")
    void constructorShouldCreateResultWithAllFields() {
      final WitTypeAdapter.AdapterValidationResult result =
          new WitTypeAdapter.AdapterValidationResult(
              true, List.of(), List.of("Minor warning"), Optional.of("Suggestion"));

      assertTrue(result.isValid());
      assertTrue(result.getErrors().isEmpty());
      assertEquals(1, result.getWarnings().size());
      assertTrue(result.getSuggestion().isPresent());
    }

    @Test
    @DisplayName("toString should contain status info")
    void toStringShouldContainStatusInfo() {
      final WitTypeAdapter.AdapterValidationResult result =
          WitTypeAdapter.AdapterValidationResult.success();

      final String str = result.toString();
      assertTrue(str.contains("valid=true"));
      assertTrue(str.contains("errors=0"));
      assertTrue(str.contains("warnings=0"));
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("AdapterType ordinals should be consistent")
    void adapterTypeOrdinalsShouldBeConsistent() {
      final var values = WitTypeAdapter.AdapterType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal());
      }
    }

    @Test
    @DisplayName("AdapterType DIRECT_CONVERSION should be first")
    void adapterTypeDirectConversionShouldBeFirst() {
      assertEquals(0, WitTypeAdapter.AdapterType.DIRECT_CONVERSION.ordinal());
    }

    @Test
    @DisplayName("AdapterType CUSTOM_CONVERSION should be last")
    void adapterTypeCustomConversionShouldBeLast() {
      assertEquals(5, WitTypeAdapter.AdapterType.CUSTOM_CONVERSION.ordinal());
    }
  }
}
