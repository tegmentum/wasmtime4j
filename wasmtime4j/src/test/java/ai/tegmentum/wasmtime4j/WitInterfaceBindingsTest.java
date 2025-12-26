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
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WitInterfaceBindings} interface.
 *
 * <p>This test class verifies the interface structure, method signatures, nested interfaces, and
 * enums for the WIT interface bindings API.
 */
@DisplayName("WitInterfaceBindings Tests")
class WitInterfaceBindingsTest {

  // ========================================================================
  // Main Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WitInterfaceBindings should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WitInterfaceBindings.class.isInterface(), "WitInterfaceBindings should be an interface");
    }

    @Test
    @DisplayName("WitInterfaceBindings should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WitInterfaceBindings.class.getModifiers()),
          "WitInterfaceBindings should be public");
    }

    @Test
    @DisplayName("WitInterfaceBindings should not extend any interface")
    void shouldNotExtendAnyInterface() {
      Class<?>[] interfaces = WitInterfaceBindings.class.getInterfaces();
      assertEquals(0, interfaces.length, "WitInterfaceBindings should not extend any interface");
    }
  }

  @Nested
  @DisplayName("Main Interface Method Tests")
  class MainInterfaceMethodTests {

    @Test
    @DisplayName("should have getSourceInterface method")
    void shouldHaveGetSourceInterfaceMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getSourceInterface");
      assertNotNull(method, "getSourceInterface method should exist");
      assertEquals(
          WitInterfaceDefinition.class,
          method.getReturnType(),
          "Return type should be WitInterfaceDefinition");
      assertEquals(0, method.getParameterCount(), "getSourceInterface should have no parameters");
    }

    @Test
    @DisplayName("should have getTargetInterface method")
    void shouldHaveGetTargetInterfaceMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getTargetInterface");
      assertNotNull(method, "getTargetInterface method should exist");
      assertEquals(
          WitInterfaceDefinition.class,
          method.getReturnType(),
          "Return type should be WitInterfaceDefinition");
      assertEquals(0, method.getParameterCount(), "getTargetInterface should have no parameters");
    }

    @Test
    @DisplayName("should have getFunctionBindings method returning Map")
    void shouldHaveGetFunctionBindingsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getFunctionBindings");
      assertNotNull(method, "getFunctionBindings method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
      assertEquals(0, method.getParameterCount(), "getFunctionBindings should have no parameters");
    }

    @Test
    @DisplayName("should have getTypeBindings method returning Map")
    void shouldHaveGetTypeBindingsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getTypeBindings");
      assertNotNull(method, "getTypeBindings method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
      assertEquals(0, method.getParameterCount(), "getTypeBindings should have no parameters");
    }

    @Test
    @DisplayName("should have getImportBindings method returning Map")
    void shouldHaveGetImportBindingsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getImportBindings");
      assertNotNull(method, "getImportBindings method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
      assertEquals(0, method.getParameterCount(), "getImportBindings should have no parameters");
    }

    @Test
    @DisplayName("should have getExportBindings method returning Map")
    void shouldHaveGetExportBindingsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getExportBindings");
      assertNotNull(method, "getExportBindings method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
      assertEquals(0, method.getParameterCount(), "getExportBindings should have no parameters");
    }

    @Test
    @DisplayName("should have getFunctionBinding method with String parameter")
    void shouldHaveGetFunctionBindingMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getFunctionBinding", String.class);
      assertNotNull(method, "getFunctionBinding method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
      assertEquals(1, method.getParameterCount(), "getFunctionBinding should have 1 parameter");
    }

    @Test
    @DisplayName("should have getTypeBinding method with String parameter")
    void shouldHaveGetTypeBindingMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getTypeBinding", String.class);
      assertNotNull(method, "getTypeBinding method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
      assertEquals(1, method.getParameterCount(), "getTypeBinding should have 1 parameter");
    }

    @Test
    @DisplayName("should have isFunctionBound method")
    void shouldHaveIsFunctionBoundMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("isFunctionBound", String.class);
      assertNotNull(method, "isFunctionBound method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(1, method.getParameterCount(), "isFunctionBound should have 1 parameter");
    }

    @Test
    @DisplayName("should have isTypeBound method")
    void shouldHaveIsTypeBoundMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("isTypeBound", String.class);
      assertNotNull(method, "isTypeBound method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(1, method.getParameterCount(), "isTypeBound should have 1 parameter");
    }

    @Test
    @DisplayName("should have getUnboundFunctions method returning Set")
    void shouldHaveGetUnboundFunctionsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getUnboundFunctions");
      assertNotNull(method, "getUnboundFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
      assertEquals(0, method.getParameterCount(), "getUnboundFunctions should have no parameters");
    }

    @Test
    @DisplayName("should have getUnboundTypes method returning Set")
    void shouldHaveGetUnboundTypesMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getUnboundTypes");
      assertNotNull(method, "getUnboundTypes method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
      assertEquals(0, method.getParameterCount(), "getUnboundTypes should have no parameters");
    }

    @Test
    @DisplayName("should have invoke method with varargs")
    void shouldHaveInvokeMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceBindings.class.getMethod("invoke", String.class, WasmValue[].class);
      assertNotNull(method, "invoke method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Return type should be WasmValue");
      assertEquals(2, method.getParameterCount(), "invoke should have 2 parameters");
      assertTrue(method.isVarArgs(), "invoke should accept varargs");
    }

    @Test
    @DisplayName("should have createInstance method with varargs")
    void shouldHaveCreateInstanceMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceBindings.class.getMethod("createInstance", String.class, WasmValue[].class);
      assertNotNull(method, "createInstance method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Return type should be WasmValue");
      assertEquals(2, method.getParameterCount(), "createInstance should have 2 parameters");
      assertTrue(method.isVarArgs(), "createInstance should accept varargs");
    }

    @Test
    @DisplayName("should have convertType method")
    void shouldHaveConvertTypeMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceBindings.class.getMethod("convertType", WasmValue.class, String.class);
      assertNotNull(method, "convertType method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Return type should be WasmValue");
      assertEquals(2, method.getParameterCount(), "convertType should have 2 parameters");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          WitInterfaceBindings.BindingStatistics.class,
          method.getReturnType(),
          "Return type should be BindingStatistics");
    }

    @Test
    @DisplayName("should have validateBindings method")
    void shouldHaveValidateBindingsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("validateBindings");
      assertNotNull(method, "validateBindings method should exist");
      assertEquals(
          WitInterfaceBindings.BindingValidationResult.class,
          method.getReturnType(),
          "Return type should be BindingValidationResult");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(
          WitInterfaceBindings.BindingMetadata.class,
          method.getReturnType(),
          "Return type should be BindingMetadata");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("FunctionBinding Interface Tests")
  class FunctionBindingInterfaceTests {

    @Test
    @DisplayName("FunctionBinding should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.FunctionBinding.class;
      assertTrue(nestedClass.isInterface(), "FunctionBinding should be an interface");
      assertTrue(nestedClass.isMemberClass(), "FunctionBinding should be a member class (nested)");
    }

    @Test
    @DisplayName("FunctionBinding should have all required methods")
    void shouldHaveAllRequiredMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "getSourceFunctionName",
                  "getTargetFunctionName",
                  "getParameterAdapters",
                  "getReturnValueAdapter",
                  "getBindingType",
                  "isDirect",
                  "getAdaptationMetadata",
                  "invoke",
                  "validate"));

      Set<String> actualMethods = new HashSet<>();
      for (Method m : WitInterfaceBindings.FunctionBinding.class.getDeclaredMethods()) {
        actualMethods.add(m.getName());
      }

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "FunctionBinding should have method: " + expected);
      }
    }

    @Test
    @DisplayName("getSourceFunctionName should return String")
    void getSourceFunctionNameShouldReturnString() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.FunctionBinding.class.getMethod("getSourceFunctionName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("getParameterAdapters should return List")
    void getParameterAdaptersShouldReturnList() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.FunctionBinding.class.getMethod("getParameterAdapters");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("getReturnValueAdapter should return Optional")
    void getReturnValueAdapterShouldReturnOptional() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.FunctionBinding.class.getMethod("getReturnValueAdapter");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("isDirect should return boolean")
    void isDirectShouldReturnBoolean() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.FunctionBinding.class.getMethod("isDirect");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("TypeBinding Interface Tests")
  class TypeBindingInterfaceTests {

    @Test
    @DisplayName("TypeBinding should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.TypeBinding.class;
      assertTrue(nestedClass.isInterface(), "TypeBinding should be an interface");
    }

    @Test
    @DisplayName("TypeBinding should have all required methods")
    void shouldHaveAllRequiredMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "getSourceTypeName",
                  "getTargetTypeName",
                  "getTypeAdapter",
                  "getBindingType",
                  "isDirect",
                  "getConstructorBindings",
                  "getMethodBindings",
                  "convert",
                  "createInstance",
                  "validate"));

      Set<String> actualMethods = new HashSet<>();
      for (Method m : WitInterfaceBindings.TypeBinding.class.getDeclaredMethods()) {
        actualMethods.add(m.getName());
      }

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "TypeBinding should have method: " + expected);
      }
    }

    @Test
    @DisplayName("convert should have WasmValue and ConversionDirection parameters")
    void convertShouldHaveCorrectParameters() throws NoSuchMethodException {
      Method method =
          WitInterfaceBindings.TypeBinding.class.getMethod(
              "convert", WasmValue.class, WitInterfaceBindings.ConversionDirection.class);
      assertNotNull(method, "convert method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Should return WasmValue");
    }

    @Test
    @DisplayName("getConstructorBindings should return Map")
    void getConstructorBindingsShouldReturnMap() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.TypeBinding.class.getMethod("getConstructorBindings");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("ImportBinding Interface Tests")
  class ImportBindingInterfaceTests {

    @Test
    @DisplayName("ImportBinding should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.ImportBinding.class;
      assertTrue(nestedClass.isInterface(), "ImportBinding should be an interface");
    }

    @Test
    @DisplayName("ImportBinding should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceBindings.ImportBinding.class.getMethod("getImportName"),
          "Should have getImportName");
      assertNotNull(
          WitInterfaceBindings.ImportBinding.class.getMethod("getResolvedImport"),
          "Should have getResolvedImport");
      assertNotNull(
          WitInterfaceBindings.ImportBinding.class.getMethod("getBindingType"),
          "Should have getBindingType");
      assertNotNull(
          WitInterfaceBindings.ImportBinding.class.getMethod("isResolved"),
          "Should have isResolved");
      assertNotNull(
          WitInterfaceBindings.ImportBinding.class.getMethod("getResolutionMetadata"),
          "Should have getResolutionMetadata");
    }
  }

  @Nested
  @DisplayName("ExportBinding Interface Tests")
  class ExportBindingInterfaceTests {

    @Test
    @DisplayName("ExportBinding should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.ExportBinding.class;
      assertTrue(nestedClass.isInterface(), "ExportBinding should be an interface");
    }

    @Test
    @DisplayName("ExportBinding should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceBindings.ExportBinding.class.getMethod("getExportName"),
          "Should have getExportName");
      assertNotNull(
          WitInterfaceBindings.ExportBinding.class.getMethod("getExportedValue"),
          "Should have getExportedValue");
      assertNotNull(
          WitInterfaceBindings.ExportBinding.class.getMethod("getBindingType"),
          "Should have getBindingType");
      assertNotNull(
          WitInterfaceBindings.ExportBinding.class.getMethod("getExportMetadata"),
          "Should have getExportMetadata");
    }
  }

  @Nested
  @DisplayName("BindingStatistics Interface Tests")
  class BindingStatisticsInterfaceTests {

    @Test
    @DisplayName("BindingStatistics should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.BindingStatistics.class;
      assertTrue(nestedClass.isInterface(), "BindingStatistics should be an interface");
    }

    @Test
    @DisplayName("BindingStatistics should have getTotalBindings returning int")
    void shouldHaveGetTotalBindings() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingStatistics.class.getMethod("getTotalBindings");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("BindingStatistics should have getSuccessRate returning double")
    void shouldHaveGetSuccessRate() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingStatistics.class.getMethod("getSuccessRate");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("BindingStatistics should have getAverageBindingTime returning double")
    void shouldHaveGetAverageBindingTime() throws NoSuchMethodException {
      Method method =
          WitInterfaceBindings.BindingStatistics.class.getMethod("getAverageBindingTime");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("BindingStatistics should have getDetailedStatistics returning Map")
    void shouldHaveGetDetailedStatistics() throws NoSuchMethodException {
      Method method =
          WitInterfaceBindings.BindingStatistics.class.getMethod("getDetailedStatistics");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("BindingValidationResult Interface Tests")
  class BindingValidationResultInterfaceTests {

    @Test
    @DisplayName("BindingValidationResult should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.BindingValidationResult.class;
      assertTrue(nestedClass.isInterface(), "BindingValidationResult should be an interface");
    }

    @Test
    @DisplayName("BindingValidationResult should have isValid returning boolean")
    void shouldHaveIsValid() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingValidationResult.class.getMethod("isValid");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("BindingValidationResult should have getErrors returning List")
    void shouldHaveGetErrors() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingValidationResult.class.getMethod("getErrors");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("BindingValidationResult should have getCoverage returning double")
    void shouldHaveGetCoverage() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingValidationResult.class.getMethod("getCoverage");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("BindingMetadata Interface Tests")
  class BindingMetadataInterfaceTests {

    @Test
    @DisplayName("BindingMetadata should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.BindingMetadata.class;
      assertTrue(nestedClass.isInterface(), "BindingMetadata should be an interface");
    }

    @Test
    @DisplayName("BindingMetadata should have getCreationTime returning Instant")
    void shouldHaveGetCreationTime() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingMetadata.class.getMethod("getCreationTime");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("BindingMetadata should have getVersion returning String")
    void shouldHaveGetVersion() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingMetadata.class.getMethod("getVersion");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("BindingMetadata should have getProperties returning Map")
    void shouldHaveGetProperties() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.BindingMetadata.class.getMethod("getProperties");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("AdaptationMetadata Interface Tests")
  class AdaptationMetadataInterfaceTests {

    @Test
    @DisplayName("AdaptationMetadata should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.AdaptationMetadata.class;
      assertTrue(nestedClass.isInterface(), "AdaptationMetadata should be an interface");
    }

    @Test
    @DisplayName("AdaptationMetadata should have getComplexity returning ComplexityLevel")
    void shouldHaveGetComplexity() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.AdaptationMetadata.class.getMethod("getComplexity");
      assertEquals(
          WitInterfaceBindings.ComplexityLevel.class,
          method.getReturnType(),
          "Should return ComplexityLevel");
    }

    @Test
    @DisplayName("AdaptationMetadata should have isLossy returning boolean")
    void shouldHaveIsLossy() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.AdaptationMetadata.class.getMethod("isLossy");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("AdaptationMetadata should have getPerformanceImpact returning PerformanceImpact")
    void shouldHaveGetPerformanceImpact() throws NoSuchMethodException {
      Method method =
          WitInterfaceBindings.AdaptationMetadata.class.getMethod("getPerformanceImpact");
      assertEquals(
          WitInterfaceBindings.PerformanceImpact.class,
          method.getReturnType(),
          "Should return PerformanceImpact");
    }
  }

  @Nested
  @DisplayName("ResolutionMetadata Interface Tests")
  class ResolutionMetadataInterfaceTests {

    @Test
    @DisplayName("ResolutionMetadata should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.ResolutionMetadata.class;
      assertTrue(nestedClass.isInterface(), "ResolutionMetadata should be an interface");
    }

    @Test
    @DisplayName("ResolutionMetadata should have getResolutionTime returning Instant")
    void shouldHaveGetResolutionTime() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.ResolutionMetadata.class.getMethod("getResolutionTime");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("ResolutionMetadata should have getFallbackInfo returning Optional")
    void shouldHaveGetFallbackInfo() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.ResolutionMetadata.class.getMethod("getFallbackInfo");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("ExportMetadata Interface Tests")
  class ExportMetadataInterfaceTests {

    @Test
    @DisplayName("ExportMetadata should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceBindings.ExportMetadata.class;
      assertTrue(nestedClass.isInterface(), "ExportMetadata should be an interface");
    }

    @Test
    @DisplayName("ExportMetadata should have getVisibility returning VisibilityLevel")
    void shouldHaveGetVisibility() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.ExportMetadata.class.getMethod("getVisibility");
      assertEquals(
          WitInterfaceBindings.VisibilityLevel.class,
          method.getReturnType(),
          "Should return VisibilityLevel");
    }

    @Test
    @DisplayName("ExportMetadata should have getDocumentation returning Optional")
    void shouldHaveGetDocumentation() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.ExportMetadata.class.getMethod("getDocumentation");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("ExportMetadata should have getAttributes returning Map")
    void shouldHaveGetAttributes() throws NoSuchMethodException {
      Method method = WitInterfaceBindings.ExportMetadata.class.getMethod("getAttributes");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  // ========================================================================
  // Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("BindingType Enum Tests")
  class BindingTypeEnumTests {

    @Test
    @DisplayName("BindingType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitInterfaceBindings.BindingType.class.isEnum(), "BindingType should be an enum");
    }

    @Test
    @DisplayName("BindingType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceBindings.BindingType[] values = WitInterfaceBindings.BindingType.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceBindings.BindingType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("DIRECT"), "Should have DIRECT");
      assertTrue(valueNames.contains("ADAPTED"), "Should have ADAPTED");
      assertTrue(valueNames.contains("GENERATED"), "Should have GENERATED");
      assertTrue(valueNames.contains("CUSTOM"), "Should have CUSTOM");
      assertEquals(4, values.length, "Should have exactly 4 values");
    }

    @Test
    @DisplayName("BindingType.valueOf should work correctly")
    void valueOfShouldWork() {
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
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceBindings.ConversionDirection.class.isEnum(),
          "ConversionDirection should be an enum");
    }

    @Test
    @DisplayName("ConversionDirection should have FORWARD and REVERSE")
    void shouldHaveExpectedValues() {
      WitInterfaceBindings.ConversionDirection[] values =
          WitInterfaceBindings.ConversionDirection.values();
      assertEquals(2, values.length, "Should have exactly 2 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceBindings.ConversionDirection v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("FORWARD"), "Should have FORWARD");
      assertTrue(valueNames.contains("REVERSE"), "Should have REVERSE");
    }
  }

  @Nested
  @DisplayName("ComplexityLevel Enum Tests")
  class ComplexityLevelEnumTests {

    @Test
    @DisplayName("ComplexityLevel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceBindings.ComplexityLevel.class.isEnum(), "ComplexityLevel should be an enum");
    }

    @Test
    @DisplayName("ComplexityLevel should have LOW, MEDIUM, HIGH")
    void shouldHaveExpectedValues() {
      WitInterfaceBindings.ComplexityLevel[] values = WitInterfaceBindings.ComplexityLevel.values();
      assertEquals(3, values.length, "Should have exactly 3 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceBindings.ComplexityLevel v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("LOW"), "Should have LOW");
      assertTrue(valueNames.contains("MEDIUM"), "Should have MEDIUM");
      assertTrue(valueNames.contains("HIGH"), "Should have HIGH");
    }
  }

  @Nested
  @DisplayName("PerformanceImpact Enum Tests")
  class PerformanceImpactEnumTests {

    @Test
    @DisplayName("PerformanceImpact should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceBindings.PerformanceImpact.class.isEnum(),
          "PerformanceImpact should be an enum");
    }

    @Test
    @DisplayName("PerformanceImpact should have all expected values")
    void shouldHaveExpectedValues() {
      WitInterfaceBindings.PerformanceImpact[] values =
          WitInterfaceBindings.PerformanceImpact.values();
      assertEquals(4, values.length, "Should have exactly 4 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceBindings.PerformanceImpact v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("MINIMAL"), "Should have MINIMAL");
      assertTrue(valueNames.contains("LOW"), "Should have LOW");
      assertTrue(valueNames.contains("MEDIUM"), "Should have MEDIUM");
      assertTrue(valueNames.contains("HIGH"), "Should have HIGH");
    }
  }

  @Nested
  @DisplayName("VisibilityLevel Enum Tests")
  class VisibilityLevelEnumTests {

    @Test
    @DisplayName("VisibilityLevel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceBindings.VisibilityLevel.class.isEnum(), "VisibilityLevel should be an enum");
    }

    @Test
    @DisplayName("VisibilityLevel should have all expected values")
    void shouldHaveExpectedValues() {
      WitInterfaceBindings.VisibilityLevel[] values = WitInterfaceBindings.VisibilityLevel.values();
      assertEquals(4, values.length, "Should have exactly 4 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceBindings.VisibilityLevel v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("PRIVATE"), "Should have PRIVATE");
      assertTrue(valueNames.contains("PACKAGE"), "Should have PACKAGE");
      assertTrue(valueNames.contains("PROTECTED"), "Should have PROTECTED");
      assertTrue(valueNames.contains("PUBLIC"), "Should have PUBLIC");
    }
  }

  // ========================================================================
  // Nested Interface Count Test
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Count Tests")
  class NestedInterfaceCountTests {

    @Test
    @DisplayName("WitInterfaceBindings should have expected number of nested interfaces")
    void shouldHaveExpectedNestedInterfaces() {
      Class<?>[] declaredClasses = WitInterfaceBindings.class.getDeclaredClasses();

      int interfaceCount = 0;
      int enumCount = 0;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.isInterface()) {
          interfaceCount++;
        } else if (clazz.isEnum()) {
          enumCount++;
        }
      }

      // Expected nested interfaces: FunctionBinding, TypeBinding, ImportBinding, ExportBinding,
      // BindingStatistics, BindingValidationResult, BindingMetadata, AdaptationMetadata,
      // ResolutionMetadata, ExportMetadata
      assertEquals(10, interfaceCount, "Should have 10 nested interfaces");

      // Expected enums: BindingType, ConversionDirection, ComplexityLevel, PerformanceImpact,
      // VisibilityLevel
      assertEquals(5, enumCount, "Should have 5 enums");
    }
  }
}
