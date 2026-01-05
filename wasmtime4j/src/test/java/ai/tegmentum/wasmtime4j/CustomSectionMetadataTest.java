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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CustomSectionMetadata interface.
 *
 * <p>CustomSectionMetadata provides access to WebAssembly custom section metadata, including
 * standard sections like "name" and "producers", as well as arbitrary custom sections. This test
 * verifies the interface structure and method signatures.
 */
@DisplayName("CustomSectionMetadata Interface Tests")
class CustomSectionMetadataTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          CustomSectionMetadata.class.isInterface(),
          "CustomSectionMetadata should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CustomSectionMetadata.class.getModifiers()),
          "CustomSectionMetadata should be public");
    }
  }

  // ========================================================================
  // Basic Section Access Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Basic Section Access Methods Tests")
  class BasicSectionAccessMethodsTests {

    @Test
    @DisplayName("should have getAllCustomSections method")
    void shouldHaveGetAllCustomSectionsMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getAllCustomSections");
      assertNotNull(method, "getAllCustomSections method should exist");
      assertEquals(List.class, method.getReturnType(), "getAllCustomSections should return List");
    }

    @Test
    @DisplayName("should have getCustomSectionsByName method")
    void shouldHaveGetCustomSectionsByNameMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("getCustomSectionsByName", String.class);
      assertNotNull(method, "getCustomSectionsByName method should exist");
      assertEquals(
          List.class, method.getReturnType(), "getCustomSectionsByName should return List");
    }

    @Test
    @DisplayName("should have getCustomSectionsByType method")
    void shouldHaveGetCustomSectionsByTypeMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("getCustomSectionsByType", CustomSectionType.class);
      assertNotNull(method, "getCustomSectionsByType method should exist");
      assertEquals(
          List.class, method.getReturnType(), "getCustomSectionsByType should return List");
    }

    @Test
    @DisplayName("should have getFirstCustomSection method")
    void shouldHaveGetFirstCustomSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("getFirstCustomSection", String.class);
      assertNotNull(method, "getFirstCustomSection method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getFirstCustomSection should return Optional");
    }
  }

  // ========================================================================
  // Query Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Query Methods Tests")
  class QueryMethodsTests {

    @Test
    @DisplayName("should have hasCustomSection method with String parameter")
    void shouldHaveHasCustomSectionStringMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("hasCustomSection", String.class);
      assertNotNull(method, "hasCustomSection(String) method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "hasCustomSection(String) should return boolean");
    }

    @Test
    @DisplayName("should have hasCustomSection method with CustomSectionType parameter")
    void shouldHaveHasCustomSectionTypeMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("hasCustomSection", CustomSectionType.class);
      assertNotNull(method, "hasCustomSection(CustomSectionType) method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "hasCustomSection(CustomSectionType) should return boolean");
    }

    @Test
    @DisplayName("should have getCustomSectionCount method")
    void shouldHaveGetCustomSectionCountMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionCount");
      assertNotNull(method, "getCustomSectionCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getCustomSectionCount should return int");
    }

    @Test
    @DisplayName("should have getCustomSectionsTotalSize method")
    void shouldHaveGetCustomSectionsTotalSizeMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionsTotalSize");
      assertNotNull(method, "getCustomSectionsTotalSize method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getCustomSectionsTotalSize should return long");
    }
  }

  // ========================================================================
  // Collection Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Collection Methods Tests")
  class CollectionMethodsTests {

    @Test
    @DisplayName("should have getCustomSectionNames method")
    void shouldHaveGetCustomSectionNamesMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionNames");
      assertNotNull(method, "getCustomSectionNames method should exist");
      assertEquals(Set.class, method.getReturnType(), "getCustomSectionNames should return Set");
    }

    @Test
    @DisplayName("should have getCustomSectionTypes method")
    void shouldHaveGetCustomSectionTypesMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionTypes");
      assertNotNull(method, "getCustomSectionTypes method should exist");
      assertEquals(Set.class, method.getReturnType(), "getCustomSectionTypes should return Set");
    }

    @Test
    @DisplayName("should have getCustomSectionsByTypeMap method")
    void shouldHaveGetCustomSectionsByTypeMapMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionsByTypeMap");
      assertNotNull(method, "getCustomSectionsByTypeMap method should exist");
      assertEquals(
          Map.class, method.getReturnType(), "getCustomSectionsByTypeMap should return Map");
    }

    @Test
    @DisplayName("should have getCustomSectionsByNameMap method")
    void shouldHaveGetCustomSectionsByNameMapMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionsByNameMap");
      assertNotNull(method, "getCustomSectionsByNameMap method should exist");
      assertEquals(
          Map.class, method.getReturnType(), "getCustomSectionsByNameMap should return Map");
    }
  }

  // ========================================================================
  // Specific Section Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Specific Section Methods Tests")
  class SpecificSectionMethodsTests {

    @Test
    @DisplayName("should have getNameSection method")
    void shouldHaveGetNameSectionMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getNameSection");
      assertNotNull(method, "getNameSection method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getNameSection should return Optional");
    }

    @Test
    @DisplayName("should have getProducersSection method")
    void shouldHaveGetProducersSectionMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getProducersSection");
      assertNotNull(method, "getProducersSection method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getProducersSection should return Optional");
    }

    @Test
    @DisplayName("should have getTargetFeaturesSection method")
    void shouldHaveGetTargetFeaturesSectionMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getTargetFeaturesSection");
      assertNotNull(method, "getTargetFeaturesSection method should exist");
      assertEquals(
          Optional.class,
          method.getReturnType(),
          "getTargetFeaturesSection should return Optional");
    }

    @Test
    @DisplayName("should have getDebuggingSections method")
    void shouldHaveGetDebuggingSectionsMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getDebuggingSections");
      assertNotNull(method, "getDebuggingSections method should exist");
      assertEquals(List.class, method.getReturnType(), "getDebuggingSections should return List");
    }

    @Test
    @DisplayName("should have hasDebuggingInfo method")
    void shouldHaveHasDebuggingInfoMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("hasDebuggingInfo");
      assertNotNull(method, "hasDebuggingInfo method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasDebuggingInfo should return boolean");
    }
  }

  // ========================================================================
  // Utility Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Utility Methods Tests")
  class UtilityMethodsTests {

    @Test
    @DisplayName("should have getCustomSectionsSummary method")
    void shouldHaveGetCustomSectionsSummaryMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionsSummary");
      assertNotNull(method, "getCustomSectionsSummary method should exist");
      assertEquals(
          String.class, method.getReturnType(), "getCustomSectionsSummary should return String");
    }

    @Test
    @DisplayName("should have validateCustomSections method")
    void shouldHaveValidateCustomSectionsMethod() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("validateCustomSections");
      assertNotNull(method, "validateCustomSections method should exist");
      assertEquals(
          CustomSectionValidationResult.class,
          method.getReturnType(),
          "validateCustomSections should return CustomSectionValidationResult");
    }
  }

  // ========================================================================
  // Method Parameters Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameters Tests")
  class MethodParametersTests {

    @Test
    @DisplayName("getCustomSectionsByName should have 1 String parameter")
    void getCustomSectionsByNameShouldHave1Parameter() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("getCustomSectionsByName", String.class);
      assertEquals(
          1, method.getParameterCount(), "getCustomSectionsByName should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("getCustomSectionsByType should have 1 CustomSectionType parameter")
    void getCustomSectionsByTypeShouldHave1Parameter() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("getCustomSectionsByType", CustomSectionType.class);
      assertEquals(
          1, method.getParameterCount(), "getCustomSectionsByType should have 1 parameter");
      assertEquals(
          CustomSectionType.class,
          method.getParameterTypes()[0],
          "Parameter should be CustomSectionType");
    }

    @Test
    @DisplayName("getFirstCustomSection should have 1 String parameter")
    void getFirstCustomSectionShouldHave1Parameter() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("getFirstCustomSection", String.class);
      assertEquals(1, method.getParameterCount(), "getFirstCustomSection should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
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
              "getAllCustomSections",
              "getCustomSectionsByName",
              "getCustomSectionsByType",
              "getFirstCustomSection",
              "hasCustomSection",
              "getCustomSectionCount",
              "getCustomSectionsTotalSize",
              "getCustomSectionNames",
              "getCustomSectionTypes",
              "getCustomSectionsByTypeMap",
              "getCustomSectionsByNameMap",
              "getNameSection",
              "getProducersSection",
              "getTargetFeaturesSection",
              "getDebuggingSections",
              "hasDebuggingInfo",
              "getCustomSectionsSummary",
              "validateCustomSections");

      Set<String> actualMethods =
          Arrays.stream(CustomSectionMetadata.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "CustomSectionMetadata should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 18 declared methods")
    void shouldHaveAtLeast18DeclaredMethods() {
      // Note: hasCustomSection is overloaded so there are 19 total
      assertTrue(
          CustomSectionMetadata.class.getDeclaredMethods().length >= 18,
          "CustomSectionMetadata should have at least 18 methods (found "
              + CustomSectionMetadata.class.getDeclaredMethods().length
              + ")");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0,
          CustomSectionMetadata.class.getInterfaces().length,
          "CustomSectionMetadata should not extend any interface");
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getAllCustomSections should return List<CustomSection>")
    void getAllCustomSectionsShouldReturnListOfCustomSection() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getAllCustomSections");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(CustomSection.class, typeArgs[0], "Type argument should be CustomSection");
    }

    @Test
    @DisplayName("getCustomSectionNames should return Set<String>")
    void getCustomSectionNamesShouldReturnSetOfString() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getCustomSectionNames");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(String.class, typeArgs[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getFirstCustomSection should return Optional<CustomSection>")
    void getFirstCustomSectionShouldReturnOptionalOfCustomSection() throws NoSuchMethodException {
      final Method method =
          CustomSectionMetadata.class.getMethod("getFirstCustomSection", String.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(CustomSection.class, typeArgs[0], "Type argument should be CustomSection");
    }

    @Test
    @DisplayName("getNameSection should return Optional<NameSection>")
    void getNameSectionShouldReturnOptionalOfNameSection() throws NoSuchMethodException {
      final Method method = CustomSectionMetadata.class.getMethod("getNameSection");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(NameSection.class, typeArgs[0], "Type argument should be NameSection");
    }
  }
}
