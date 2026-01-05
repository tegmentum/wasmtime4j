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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FunctionTypeMetadata interface.
 *
 * <p>FunctionTypeMetadata represents additional metadata associated with function types.
 */
@DisplayName("FunctionTypeMetadata Interface Tests")
class FunctionTypeMetadataTest {

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
          FunctionTypeMetadata.class.isInterface(), "FunctionTypeMetadata should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FunctionTypeMetadata.class.getModifiers()),
          "FunctionTypeMetadata should be public");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getParameterNames method")
    void shouldHaveGetParameterNamesMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("getParameterNames");
      assertNotNull(method, "getParameterNames should exist");
      assertEquals(String[].class, method.getReturnType(), "Should return String[]");
    }

    @Test
    @DisplayName("should have getResultNames method")
    void shouldHaveGetResultNamesMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("getResultNames");
      assertNotNull(method, "getResultNames should exist");
      assertEquals(String[].class, method.getReturnType(), "Should return String[]");
    }

    @Test
    @DisplayName("should have getParameterDocumentation method")
    void shouldHaveGetParameterDocumentationMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("getParameterDocumentation", int.class);
      assertNotNull(method, "getParameterDocumentation should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getResultDocumentation method")
    void shouldHaveGetResultDocumentationMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("getResultDocumentation", int.class);
      assertNotNull(method, "getResultDocumentation should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getCallingConvention method")
    void shouldHaveGetCallingConventionMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("getCallingConvention");
      assertNotNull(method, "getCallingConvention should exist");
      assertEquals(
          FunctionTypeMetadata.CallingConvention.class,
          method.getReturnType(),
          "Should return CallingConvention");
    }

    @Test
    @DisplayName("should have getPerformanceHints method")
    void shouldHaveGetPerformanceHintsMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("getPerformanceHints");
      assertNotNull(method, "getPerformanceHints should exist");
      assertEquals(
          FunctionTypeMetadata.PerformanceHints.class,
          method.getReturnType(),
          "Should return PerformanceHints");
    }

    @Test
    @DisplayName("should have getCustomAttributes method")
    void shouldHaveGetCustomAttributesMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("getCustomAttributes");
      assertNotNull(method, "getCustomAttributes should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have hasMetadata method")
    void shouldHaveHasMetadataMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.class.getMethod(
              "hasMetadata", FunctionTypeMetadata.MetadataType.class);
      assertNotNull(method, "hasMetadata should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.class.getMethod(
              "getMetadata", FunctionTypeMetadata.MetadataType.class);
      assertNotNull(method, "getMetadata should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.class.getMethod("builder");
      assertNotNull(method, "builder should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }
  }

  // ========================================================================
  // CallingConvention Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("CallingConvention Enum Tests")
  class CallingConventionEnumTests {

    @Test
    @DisplayName("should have WASM_STANDARD value")
    void shouldHaveWasmStandardValue() {
      assertNotNull(
          FunctionTypeMetadata.CallingConvention.valueOf("WASM_STANDARD"),
          "Should have WASM_STANDARD value");
    }

    @Test
    @DisplayName("should have FAST_CALL value")
    void shouldHaveFastCallValue() {
      assertNotNull(
          FunctionTypeMetadata.CallingConvention.valueOf("FAST_CALL"),
          "Should have FAST_CALL value");
    }

    @Test
    @DisplayName("should have C_CALL value")
    void shouldHaveCCallValue() {
      assertNotNull(
          FunctionTypeMetadata.CallingConvention.valueOf("C_CALL"), "Should have C_CALL value");
    }

    @Test
    @DisplayName("should have SYSTEM_CALL value")
    void shouldHaveSystemCallValue() {
      assertNotNull(
          FunctionTypeMetadata.CallingConvention.valueOf("SYSTEM_CALL"),
          "Should have SYSTEM_CALL value");
    }

    @Test
    @DisplayName("should have CUSTOM value")
    void shouldHaveCustomValue() {
      assertNotNull(
          FunctionTypeMetadata.CallingConvention.valueOf("CUSTOM"), "Should have CUSTOM value");
    }

    @Test
    @DisplayName("should have exactly 5 values")
    void shouldHaveExactly5Values() {
      assertEquals(
          5,
          FunctionTypeMetadata.CallingConvention.values().length,
          "Should have exactly 5 enum values");
    }
  }

  // ========================================================================
  // PerformanceHints Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("PerformanceHints Interface Tests")
  class PerformanceHintsTests {

    @Test
    @DisplayName("should have getEstimatedCost method")
    void shouldHaveGetEstimatedCostMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.PerformanceHints.class.getMethod("getEstimatedCost");
      assertNotNull(method, "getEstimatedCost should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isHotPath method")
    void shouldHaveIsHotPathMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.PerformanceHints.class.getMethod("isHotPath");
      assertNotNull(method, "isHotPath should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have performsIO method")
    void shouldHavePerformsIOMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.PerformanceHints.class.getMethod("performsIO");
      assertNotNull(method, "performsIO should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isPure method")
    void shouldHaveIsPureMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.PerformanceHints.class.getMethod("isPure");
      assertNotNull(method, "isPure should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMemoryBehavior method")
    void shouldHaveGetMemoryBehaviorMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.PerformanceHints.class.getMethod("getMemoryBehavior");
      assertNotNull(method, "getMemoryBehavior should exist");
      assertEquals(
          FunctionTypeMetadata.PerformanceHints.MemoryBehavior.class,
          method.getReturnType(),
          "Should return MemoryBehavior");
    }
  }

  // ========================================================================
  // MemoryBehavior Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("MemoryBehavior Enum Tests")
  class MemoryBehaviorEnumTests {

    @Test
    @DisplayName("should have NO_ALLOCATION value")
    void shouldHaveNoAllocationValue() {
      assertNotNull(
          FunctionTypeMetadata.PerformanceHints.MemoryBehavior.valueOf("NO_ALLOCATION"),
          "Should have NO_ALLOCATION value");
    }

    @Test
    @DisplayName("should have LIGHT_ALLOCATION value")
    void shouldHaveLightAllocationValue() {
      assertNotNull(
          FunctionTypeMetadata.PerformanceHints.MemoryBehavior.valueOf("LIGHT_ALLOCATION"),
          "Should have LIGHT_ALLOCATION value");
    }

    @Test
    @DisplayName("should have HEAVY_ALLOCATION value")
    void shouldHaveHeavyAllocationValue() {
      assertNotNull(
          FunctionTypeMetadata.PerformanceHints.MemoryBehavior.valueOf("HEAVY_ALLOCATION"),
          "Should have HEAVY_ALLOCATION value");
    }

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(
          FunctionTypeMetadata.PerformanceHints.MemoryBehavior.valueOf("UNKNOWN"),
          "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactly4Values() {
      assertEquals(
          4,
          FunctionTypeMetadata.PerformanceHints.MemoryBehavior.values().length,
          "Should have exactly 4 enum values");
    }
  }

  // ========================================================================
  // MetadataType Class Tests
  // ========================================================================

  @Nested
  @DisplayName("MetadataType Class Tests")
  class MetadataTypeClassTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.MetadataType.class.getMethod("getName");
      assertNotNull(method, "getName should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.MetadataType.class.getMethod("getValueType");
      assertNotNull(method, "getValueType should exist");
      assertEquals(Class.class, method.getReturnType(), "Should return Class");
    }

    @Test
    @DisplayName("should have DOCUMENTATION static field")
    void shouldHaveDocumentationStaticField() throws NoSuchFieldException {
      var field = FunctionTypeMetadata.MetadataType.class.getDeclaredField("DOCUMENTATION");
      assertTrue(Modifier.isStatic(field.getModifiers()), "DOCUMENTATION should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "DOCUMENTATION should be final");
      assertTrue(Modifier.isPublic(field.getModifiers()), "DOCUMENTATION should be public");
    }

    @Test
    @DisplayName("should have VERSION static field")
    void shouldHaveVersionStaticField() throws NoSuchFieldException {
      var field = FunctionTypeMetadata.MetadataType.class.getDeclaredField("VERSION");
      assertTrue(Modifier.isStatic(field.getModifiers()), "VERSION should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "VERSION should be final");
      assertTrue(Modifier.isPublic(field.getModifiers()), "VERSION should be public");
    }

    @Test
    @DisplayName("should have DEPRECATED static field")
    void shouldHaveDeprecatedStaticField() throws NoSuchFieldException {
      var field = FunctionTypeMetadata.MetadataType.class.getDeclaredField("DEPRECATED");
      assertTrue(Modifier.isStatic(field.getModifiers()), "DEPRECATED should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "DEPRECATED should be final");
      assertTrue(Modifier.isPublic(field.getModifiers()), "DEPRECATED should be public");
    }

    @Test
    @DisplayName("should have PRIORITY static field")
    void shouldHavePriorityStaticField() throws NoSuchFieldException {
      var field = FunctionTypeMetadata.MetadataType.class.getDeclaredField("PRIORITY");
      assertTrue(Modifier.isStatic(field.getModifiers()), "PRIORITY should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "PRIORITY should be final");
      assertTrue(Modifier.isPublic(field.getModifiers()), "PRIORITY should be public");
    }
  }

  // ========================================================================
  // Builder Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Class Tests")
  class BuilderClassTests {

    @Test
    @DisplayName("should have parameterNames method")
    void shouldHaveParameterNamesMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.Builder.class.getMethod("parameterNames", String[].class);
      assertNotNull(method, "parameterNames should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have resultNames method")
    void shouldHaveResultNamesMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.Builder.class.getMethod("resultNames", String[].class);
      assertNotNull(method, "resultNames should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have parameterDocumentation method")
    void shouldHaveParameterDocumentationMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.Builder.class.getMethod("parameterDocumentation", String[].class);
      assertNotNull(method, "parameterDocumentation should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have resultDocumentation method")
    void shouldHaveResultDocumentationMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.Builder.class.getMethod("resultDocumentation", String[].class);
      assertNotNull(method, "resultDocumentation should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have callingConvention method")
    void shouldHaveCallingConventionMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.Builder.class.getMethod(
              "callingConvention", FunctionTypeMetadata.CallingConvention.class);
      assertNotNull(method, "callingConvention should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have performanceHints method")
    void shouldHavePerformanceHintsMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.Builder.class.getMethod(
              "performanceHints", FunctionTypeMetadata.PerformanceHints.class);
      assertNotNull(method, "performanceHints should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have customAttribute method")
    void shouldHaveCustomAttributeMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.Builder.class.getMethod(
              "customAttribute", String.class, Object.class);
      assertNotNull(method, "customAttribute should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have metadata method")
    void shouldHaveMetadataMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeMetadata.Builder.class.getMethod(
              "metadata", FunctionTypeMetadata.MetadataType.class, Object.class);
      assertNotNull(method, "metadata should exist");
      assertEquals(
          FunctionTypeMetadata.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = FunctionTypeMetadata.Builder.class.getMethod("build");
      assertNotNull(method, "build should exist");
      assertEquals(
          FunctionTypeMetadata.class, method.getReturnType(), "Should return FunctionTypeMetadata");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have CallingConvention nested enum")
    void shouldHaveCallingConventionNestedEnum() {
      Class<?>[] nestedClasses = FunctionTypeMetadata.class.getDeclaredClasses();
      boolean hasEnum =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("CallingConvention") && c.isEnum());
      assertTrue(hasEnum, "Should have CallingConvention nested enum");
    }

    @Test
    @DisplayName("should have PerformanceHints nested interface")
    void shouldHavePerformanceHintsNestedInterface() {
      Class<?>[] nestedClasses = FunctionTypeMetadata.class.getDeclaredClasses();
      boolean hasInterface =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("PerformanceHints") && c.isInterface());
      assertTrue(hasInterface, "Should have PerformanceHints nested interface");
    }

    @Test
    @DisplayName("should have MetadataType nested class")
    void shouldHaveMetadataTypeNestedClass() {
      Class<?>[] nestedClasses = FunctionTypeMetadata.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("MetadataType") && !c.isInterface());
      assertTrue(hasClass, "Should have MetadataType nested class");
    }

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      Class<?>[] nestedClasses = FunctionTypeMetadata.class.getDeclaredClasses();
      boolean hasClass =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("Builder") && !c.isInterface());
      assertTrue(hasClass, "Should have Builder nested class");
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
              "getParameterNames",
              "getResultNames",
              "getParameterDocumentation",
              "getResultDocumentation",
              "getCallingConvention",
              "getPerformanceHints",
              "getCustomAttributes",
              "hasMetadata",
              "getMetadata",
              "builder");

      Set<String> actualMethods =
          Arrays.stream(FunctionTypeMetadata.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
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
          FunctionTypeMetadata.class.getInterfaces().length,
          "FunctionTypeMetadata should not extend any interface");
    }
  }
}
