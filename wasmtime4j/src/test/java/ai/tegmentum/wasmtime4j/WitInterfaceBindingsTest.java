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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitInterfaceBindings} interface.
 *
 * <p>WitInterfaceBindings provides comprehensive binding capabilities for evolved WIT interfaces.
 */
@DisplayName("WitInterfaceBindings Tests")
class WitInterfaceBindingsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WitInterfaceBindings should be an interface")
    void witInterfaceBindingsShouldBeAnInterface() {
      assertTrue(WitInterfaceBindings.class.isInterface());
    }

    @Test
    @DisplayName("FunctionBinding should be a nested interface")
    void functionBindingShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.FunctionBinding.class.isInterface());
    }

    @Test
    @DisplayName("TypeBinding should be a nested interface")
    void typeBindingShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.TypeBinding.class.isInterface());
    }

    @Test
    @DisplayName("ImportBinding should be a nested interface")
    void importBindingShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.ImportBinding.class.isInterface());
    }

    @Test
    @DisplayName("ExportBinding should be a nested interface")
    void exportBindingShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.ExportBinding.class.isInterface());
    }

    @Test
    @DisplayName("BindingStatistics should be a nested interface")
    void bindingStatisticsShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.BindingStatistics.class.isInterface());
    }

    @Test
    @DisplayName("BindingValidationResult should be a nested interface")
    void bindingValidationResultShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.BindingValidationResult.class.isInterface());
    }

    @Test
    @DisplayName("BindingMetadata should be a nested interface")
    void bindingMetadataShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.BindingMetadata.class.isInterface());
    }

    @Test
    @DisplayName("AdaptationMetadata should be a nested interface")
    void adaptationMetadataShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.AdaptationMetadata.class.isInterface());
    }

    @Test
    @DisplayName("ResolutionMetadata should be a nested interface")
    void resolutionMetadataShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.ResolutionMetadata.class.isInterface());
    }

    @Test
    @DisplayName("ExportMetadata should be a nested interface")
    void exportMetadataShouldBeNestedInterface() {
      assertTrue(WitInterfaceBindings.ExportMetadata.class.isInterface());
    }
  }

  @Nested
  @DisplayName("BindingType Enum Tests")
  class BindingTypeEnumTests {

    @Test
    @DisplayName("BindingType should be an enum")
    void bindingTypeShouldBeEnum() {
      assertTrue(WitInterfaceBindings.BindingType.class.isEnum());
    }

    @Test
    @DisplayName("BindingType should have 4 values")
    void bindingTypeShouldHave4Values() {
      assertEquals(4, WitInterfaceBindings.BindingType.values().length);
    }

    @Test
    @DisplayName("BindingType should have all expected values")
    void bindingTypeShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceBindings.BindingType.DIRECT);
      assertNotNull(WitInterfaceBindings.BindingType.ADAPTED);
      assertNotNull(WitInterfaceBindings.BindingType.GENERATED);
      assertNotNull(WitInterfaceBindings.BindingType.CUSTOM);
    }

    @Test
    @DisplayName("BindingType valueOf should work")
    void bindingTypeValueOfShouldWork() {
      assertEquals(
          WitInterfaceBindings.BindingType.DIRECT,
          WitInterfaceBindings.BindingType.valueOf("DIRECT"));
      assertEquals(
          WitInterfaceBindings.BindingType.ADAPTED,
          WitInterfaceBindings.BindingType.valueOf("ADAPTED"));
    }
  }

  @Nested
  @DisplayName("ConversionDirection Enum Tests")
  class ConversionDirectionEnumTests {

    @Test
    @DisplayName("ConversionDirection should be an enum")
    void conversionDirectionShouldBeEnum() {
      assertTrue(WitInterfaceBindings.ConversionDirection.class.isEnum());
    }

    @Test
    @DisplayName("ConversionDirection should have 2 values")
    void conversionDirectionShouldHave2Values() {
      assertEquals(2, WitInterfaceBindings.ConversionDirection.values().length);
    }

    @Test
    @DisplayName("ConversionDirection should have FORWARD and REVERSE")
    void conversionDirectionShouldHaveForwardAndReverse() {
      assertNotNull(WitInterfaceBindings.ConversionDirection.FORWARD);
      assertNotNull(WitInterfaceBindings.ConversionDirection.REVERSE);
    }
  }

  @Nested
  @DisplayName("ComplexityLevel Enum Tests")
  class ComplexityLevelEnumTests {

    @Test
    @DisplayName("ComplexityLevel should be an enum")
    void complexityLevelShouldBeEnum() {
      assertTrue(WitInterfaceBindings.ComplexityLevel.class.isEnum());
    }

    @Test
    @DisplayName("ComplexityLevel should have 3 values")
    void complexityLevelShouldHave3Values() {
      assertEquals(3, WitInterfaceBindings.ComplexityLevel.values().length);
    }

    @Test
    @DisplayName("ComplexityLevel should have LOW, MEDIUM, HIGH")
    void complexityLevelShouldHaveLowMediumHigh() {
      assertNotNull(WitInterfaceBindings.ComplexityLevel.LOW);
      assertNotNull(WitInterfaceBindings.ComplexityLevel.MEDIUM);
      assertNotNull(WitInterfaceBindings.ComplexityLevel.HIGH);
    }
  }

  @Nested
  @DisplayName("PerformanceImpact Enum Tests")
  class PerformanceImpactEnumTests {

    @Test
    @DisplayName("PerformanceImpact should be an enum")
    void performanceImpactShouldBeEnum() {
      assertTrue(WitInterfaceBindings.PerformanceImpact.class.isEnum());
    }

    @Test
    @DisplayName("PerformanceImpact should have 4 values")
    void performanceImpactShouldHave4Values() {
      assertEquals(4, WitInterfaceBindings.PerformanceImpact.values().length);
    }

    @Test
    @DisplayName("PerformanceImpact should have all expected values")
    void performanceImpactShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceBindings.PerformanceImpact.MINIMAL);
      assertNotNull(WitInterfaceBindings.PerformanceImpact.LOW);
      assertNotNull(WitInterfaceBindings.PerformanceImpact.MEDIUM);
      assertNotNull(WitInterfaceBindings.PerformanceImpact.HIGH);
    }
  }

  @Nested
  @DisplayName("VisibilityLevel Enum Tests")
  class VisibilityLevelEnumTests {

    @Test
    @DisplayName("VisibilityLevel should be an enum")
    void visibilityLevelShouldBeEnum() {
      assertTrue(WitInterfaceBindings.VisibilityLevel.class.isEnum());
    }

    @Test
    @DisplayName("VisibilityLevel should have 4 values")
    void visibilityLevelShouldHave4Values() {
      assertEquals(4, WitInterfaceBindings.VisibilityLevel.values().length);
    }

    @Test
    @DisplayName("VisibilityLevel should have all expected values")
    void visibilityLevelShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceBindings.VisibilityLevel.PRIVATE);
      assertNotNull(WitInterfaceBindings.VisibilityLevel.PACKAGE);
      assertNotNull(WitInterfaceBindings.VisibilityLevel.PROTECTED);
      assertNotNull(WitInterfaceBindings.VisibilityLevel.PUBLIC);
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("BindingType ordinals should be consistent")
    void bindingTypeOrdinalsShouldBeConsistent() {
      final var values = WitInterfaceBindings.BindingType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal());
      }
    }

    @Test
    @DisplayName("ConversionDirection ordinals should be consistent")
    void conversionDirectionOrdinalsShouldBeConsistent() {
      assertEquals(0, WitInterfaceBindings.ConversionDirection.FORWARD.ordinal());
      assertEquals(1, WitInterfaceBindings.ConversionDirection.REVERSE.ordinal());
    }

    @Test
    @DisplayName("ComplexityLevel ordinals should be consistent")
    void complexityLevelOrdinalsShouldBeConsistent() {
      assertEquals(0, WitInterfaceBindings.ComplexityLevel.LOW.ordinal());
      assertEquals(1, WitInterfaceBindings.ComplexityLevel.MEDIUM.ordinal());
      assertEquals(2, WitInterfaceBindings.ComplexityLevel.HIGH.ordinal());
    }

    @Test
    @DisplayName("VisibilityLevel ordinals should be consistent")
    void visibilityLevelOrdinalsShouldBeConsistent() {
      assertEquals(0, WitInterfaceBindings.VisibilityLevel.PRIVATE.ordinal());
      assertEquals(1, WitInterfaceBindings.VisibilityLevel.PACKAGE.ordinal());
      assertEquals(2, WitInterfaceBindings.VisibilityLevel.PROTECTED.ordinal());
      assertEquals(3, WitInterfaceBindings.VisibilityLevel.PUBLIC.ordinal());
    }
  }
}
