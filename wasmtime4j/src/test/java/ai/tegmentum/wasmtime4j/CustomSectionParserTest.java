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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CustomSectionParser interface.
 *
 * <p>CustomSectionParser provides functionality for parsing WebAssembly custom sections from binary
 * data and creating custom sections from structured data. This test verifies the interface
 * structure and method signatures.
 */
@DisplayName("CustomSectionParser Interface Tests")
class CustomSectionParserTest {

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
          CustomSectionParser.class.isInterface(), "CustomSectionParser should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CustomSectionParser.class.getModifiers()),
          "CustomSectionParser should be public");
    }
  }

  // ========================================================================
  // Parse Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Parse Methods Tests")
  class ParseMethodsTests {

    @Test
    @DisplayName("should have parseCustomSection method")
    void shouldHaveParseCustomSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("parseCustomSection", String.class, byte[].class);
      assertNotNull(method, "parseCustomSection method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "parseCustomSection should return Optional");
    }

    @Test
    @DisplayName("should have parseNameSection method")
    void shouldHaveParseNameSectionMethod() throws NoSuchMethodException {
      final Method method = CustomSectionParser.class.getMethod("parseNameSection", byte[].class);
      assertNotNull(method, "parseNameSection method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "parseNameSection should return Optional");
    }

    @Test
    @DisplayName("should have parseProducersSection method")
    void shouldHaveParseProducersSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("parseProducersSection", byte[].class);
      assertNotNull(method, "parseProducersSection method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "parseProducersSection should return Optional");
    }

    @Test
    @DisplayName("should have parseTargetFeaturesSection method")
    void shouldHaveParseTargetFeaturesSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("parseTargetFeaturesSection", byte[].class);
      assertNotNull(method, "parseTargetFeaturesSection method should exist");
      assertEquals(
          Optional.class,
          method.getReturnType(),
          "parseTargetFeaturesSection should return Optional");
    }
  }

  // ========================================================================
  // Query Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Query Methods Tests")
  class QueryMethodsTests {

    @Test
    @DisplayName("should have supports method")
    void shouldHaveSupportsMethod() throws NoSuchMethodException {
      final Method method = CustomSectionParser.class.getMethod("supports", String.class);
      assertNotNull(method, "supports method should exist");
      assertEquals(boolean.class, method.getReturnType(), "supports should return boolean");
    }

    @Test
    @DisplayName("should have getSupportedTypes method")
    void shouldHaveGetSupportedTypesMethod() throws NoSuchMethodException {
      final Method method = CustomSectionParser.class.getMethod("getSupportedTypes");
      assertNotNull(method, "getSupportedTypes method should exist");
      assertEquals(Set.class, method.getReturnType(), "getSupportedTypes should return Set");
    }
  }

  // ========================================================================
  // Validation Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Validation Methods Tests")
  class ValidationMethodsTests {

    @Test
    @DisplayName("should have validateSection method")
    void shouldHaveValidateSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("validateSection", String.class, byte[].class);
      assertNotNull(method, "validateSection method should exist");
      assertEquals(
          CustomSectionValidationResult.class,
          method.getReturnType(),
          "validateSection should return CustomSectionValidationResult");
    }
  }

  // ========================================================================
  // Creation Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Creation Methods Tests")
  class CreationMethodsTests {

    @Test
    @DisplayName("should have createCustomSection method")
    void shouldHaveCreateCustomSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod(
              "createCustomSection", String.class, CustomSectionType.class, Object.class);
      assertNotNull(method, "createCustomSection method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "createCustomSection should return Optional");
    }
  }

  // ========================================================================
  // Serialization Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Serialization Methods Tests")
  class SerializationMethodsTests {

    @Test
    @DisplayName("should have serializeNameSection method")
    void shouldHaveSerializeNameSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("serializeNameSection", NameSection.class);
      assertNotNull(method, "serializeNameSection method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "serializeNameSection should return Optional");
    }

    @Test
    @DisplayName("should have serializeProducersSection method")
    void shouldHaveSerializeProducersSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("serializeProducersSection", ProducersSection.class);
      assertNotNull(method, "serializeProducersSection method should exist");
      assertEquals(
          Optional.class,
          method.getReturnType(),
          "serializeProducersSection should return Optional");
    }

    @Test
    @DisplayName("should have serializeTargetFeaturesSection method")
    void shouldHaveSerializeTargetFeaturesSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod(
              "serializeTargetFeaturesSection", TargetFeaturesSection.class);
      assertNotNull(method, "serializeTargetFeaturesSection method should exist");
      assertEquals(
          Optional.class,
          method.getReturnType(),
          "serializeTargetFeaturesSection should return Optional");
    }
  }

  // ========================================================================
  // Method Parameters Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameters Tests")
  class MethodParametersTests {

    @Test
    @DisplayName("parseCustomSection should have 2 parameters")
    void parseCustomSectionShouldHave2Parameters() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("parseCustomSection", String.class, byte[].class);
      assertEquals(2, method.getParameterCount(), "parseCustomSection should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          byte[].class, method.getParameterTypes()[1], "Second parameter should be byte[]");
    }

    @Test
    @DisplayName("parseNameSection should have 1 parameter")
    void parseNameSectionShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = CustomSectionParser.class.getMethod("parseNameSection", byte[].class);
      assertEquals(1, method.getParameterCount(), "parseNameSection should have 1 parameter");
      assertEquals(byte[].class, method.getParameterTypes()[0], "Parameter should be byte[]");
    }

    @Test
    @DisplayName("createCustomSection should have 3 parameters")
    void createCustomSectionShouldHave3Parameters() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod(
              "createCustomSection", String.class, CustomSectionType.class, Object.class);
      assertEquals(3, method.getParameterCount(), "createCustomSection should have 3 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          CustomSectionType.class,
          method.getParameterTypes()[1],
          "Second parameter should be CustomSectionType");
      assertEquals(Object.class, method.getParameterTypes()[2], "Third parameter should be Object");
    }

    @Test
    @DisplayName("validateSection should have 2 parameters")
    void validateSectionShouldHave2Parameters() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("validateSection", String.class, byte[].class);
      assertEquals(2, method.getParameterCount(), "validateSection should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          byte[].class, method.getParameterTypes()[1], "Second parameter should be byte[]");
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
              "parseCustomSection",
              "parseNameSection",
              "parseProducersSection",
              "parseTargetFeaturesSection",
              "supports",
              "getSupportedTypes",
              "validateSection",
              "createCustomSection",
              "serializeNameSection",
              "serializeProducersSection",
              "serializeTargetFeaturesSection");

      Set<String> actualMethods =
          Arrays.stream(CustomSectionParser.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "CustomSectionParser should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 11 declared methods")
    void shouldHaveExactly11DeclaredMethods() {
      assertEquals(
          11,
          CustomSectionParser.class.getDeclaredMethods().length,
          "CustomSectionParser should have exactly 11 methods (found "
              + CustomSectionParser.class.getDeclaredMethods().length
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
          CustomSectionParser.class.getInterfaces().length,
          "CustomSectionParser should not extend any interface");
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("parseCustomSection should return Optional<CustomSection>")
    void parseCustomSectionShouldReturnOptionalOfCustomSection() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("parseCustomSection", String.class, byte[].class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(CustomSection.class, typeArgs[0], "Type argument should be CustomSection");
    }

    @Test
    @DisplayName("parseNameSection should return Optional<NameSection>")
    void parseNameSectionShouldReturnOptionalOfNameSection() throws NoSuchMethodException {
      final Method method = CustomSectionParser.class.getMethod("parseNameSection", byte[].class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(NameSection.class, typeArgs[0], "Type argument should be NameSection");
    }

    @Test
    @DisplayName("parseProducersSection should return Optional<ProducersSection>")
    void parseProducersSectionShouldReturnOptionalOfProducersSection()
        throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("parseProducersSection", byte[].class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(ProducersSection.class, typeArgs[0], "Type argument should be ProducersSection");
    }

    @Test
    @DisplayName("serializeNameSection should return Optional<byte[]>")
    void serializeNameSectionShouldReturnOptionalOfByteArray() throws NoSuchMethodException {
      final Method method =
          CustomSectionParser.class.getMethod("serializeNameSection", NameSection.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(byte[].class, typeArgs[0], "Type argument should be byte[]");
    }

    @Test
    @DisplayName("getSupportedTypes should return Set<CustomSectionType>")
    void getSupportedTypesShouldReturnSetOfCustomSectionType() throws NoSuchMethodException {
      final Method method = CustomSectionParser.class.getMethod("getSupportedTypes");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Set.class, paramType.getRawType(), "Raw type should be Set");
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertEquals(
          CustomSectionType.class, typeArgs[0], "Type argument should be CustomSectionType");
    }
  }
}
