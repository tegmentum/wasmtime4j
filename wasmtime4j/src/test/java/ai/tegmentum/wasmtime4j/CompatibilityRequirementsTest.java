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
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CompatibilityRequirements class.
 *
 * <p>CompatibilityRequirements defines requirements and constraints for interface compatibility
 * checking, including version ranges, feature requirements, and compatibility levels. This test
 * verifies the class structure, builder pattern, and nested types.
 */
@DisplayName("CompatibilityRequirements Class Tests")
class CompatibilityRequirementsTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(CompatibilityRequirements.class.getModifiers()),
          "CompatibilityRequirements should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CompatibilityRequirements.class.getModifiers()),
          "CompatibilityRequirements should be public");
    }

    @Test
    @DisplayName("should not be an interface")
    void shouldNotBeAnInterface() {
      assertTrue(
          !CompatibilityRequirements.class.isInterface(),
          "CompatibilityRequirements should not be an interface");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "builder should return Builder");
    }

    @Test
    @DisplayName("should have static defaults method")
    void shouldHaveDefaultsMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("defaults");
      assertNotNull(method, "defaults method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaults should be static");
      assertEquals(
          CompatibilityRequirements.class,
          method.getReturnType(),
          "defaults should return CompatibilityRequirements");
    }

    @Test
    @DisplayName("should have static strict method")
    void shouldHaveStrictMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("strict");
      assertNotNull(method, "strict method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "strict should be static");
      assertEquals(
          CompatibilityRequirements.class,
          method.getReturnType(),
          "strict should return CompatibilityRequirements");
    }

    @Test
    @DisplayName("should have static lenient method")
    void shouldHaveLenientMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("lenient");
      assertNotNull(method, "lenient method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "lenient should be static");
      assertEquals(
          CompatibilityRequirements.class,
          method.getReturnType(),
          "lenient should return CompatibilityRequirements");
    }
  }

  // ========================================================================
  // Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Methods Tests")
  class GetterMethodsTests {

    @Test
    @DisplayName("should have getVersionRange method")
    void shouldHaveGetVersionRangeMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("getVersionRange");
      assertNotNull(method, "getVersionRange method should exist");
      assertEquals(
          CompatibilityRequirements.VersionRange.class,
          method.getReturnType(),
          "getVersionRange should return VersionRange");
    }

    @Test
    @DisplayName("should have getRequiredFeatures method")
    void shouldHaveGetRequiredFeaturesMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("getRequiredFeatures");
      assertNotNull(method, "getRequiredFeatures method should exist");
      assertEquals(Set.class, method.getReturnType(), "getRequiredFeatures should return Set");
    }

    @Test
    @DisplayName("should have getOptionalFeatures method")
    void shouldHaveGetOptionalFeaturesMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("getOptionalFeatures");
      assertNotNull(method, "getOptionalFeatures method should exist");
      assertEquals(Set.class, method.getReturnType(), "getOptionalFeatures should return Set");
    }

    @Test
    @DisplayName("should have getExcludedVersions method")
    void shouldHaveGetExcludedVersionsMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("getExcludedVersions");
      assertNotNull(method, "getExcludedVersions method should exist");
      assertEquals(Set.class, method.getReturnType(), "getExcludedVersions should return Set");
    }

    @Test
    @DisplayName("should have getMinimumCompatibilityLevel method")
    void shouldHaveGetMinimumCompatibilityLevelMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.class.getMethod("getMinimumCompatibilityLevel");
      assertNotNull(method, "getMinimumCompatibilityLevel method should exist");
      assertEquals(
          CompatibilityRequirements.CompatibilityLevel.class,
          method.getReturnType(),
          "getMinimumCompatibilityLevel should return CompatibilityLevel");
    }

    @Test
    @DisplayName("should have isAllowPreRelease method")
    void shouldHaveIsAllowPreReleaseMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("isAllowPreRelease");
      assertNotNull(method, "isAllowPreRelease method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isAllowPreRelease should return boolean");
    }

    @Test
    @DisplayName("should have isAllowBuildMetadata method")
    void shouldHaveIsAllowBuildMetadataMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("isAllowBuildMetadata");
      assertNotNull(method, "isAllowBuildMetadata method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isAllowBuildMetadata should return boolean");
    }

    @Test
    @DisplayName("should have getRequiredFunctions method")
    void shouldHaveGetRequiredFunctionsMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("getRequiredFunctions");
      assertNotNull(method, "getRequiredFunctions method should exist");
      assertEquals(Set.class, method.getReturnType(), "getRequiredFunctions should return Set");
    }

    @Test
    @DisplayName("should have getRequiredTypes method")
    void shouldHaveGetRequiredTypesMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("getRequiredTypes");
      assertNotNull(method, "getRequiredTypes method should exist");
      assertEquals(Set.class, method.getReturnType(), "getRequiredTypes should return Set");
    }

    @Test
    @DisplayName("should have isStrictFunctionSignatures method")
    void shouldHaveIsStrictFunctionSignaturesMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("isStrictFunctionSignatures");
      assertNotNull(method, "isStrictFunctionSignatures method should exist");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isStrictFunctionSignatures should return boolean");
    }

    @Test
    @DisplayName("should have isStrictTypeDefinitions method")
    void shouldHaveIsStrictTypeDefinitionsMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("isStrictTypeDefinitions");
      assertNotNull(method, "isStrictTypeDefinitions method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isStrictTypeDefinitions should return boolean");
    }

    @Test
    @DisplayName("should have isAllowBackwardCompatible method")
    void shouldHaveIsAllowBackwardCompatibleMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("isAllowBackwardCompatible");
      assertNotNull(method, "isAllowBackwardCompatible method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isAllowBackwardCompatible should return boolean");
    }

    @Test
    @DisplayName("should have isAllowForwardCompatible method")
    void shouldHaveIsAllowForwardCompatibleMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.class.getMethod("isAllowForwardCompatible");
      assertNotNull(method, "isAllowForwardCompatible method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isAllowForwardCompatible should return boolean");
    }

    @Test
    @DisplayName("should have getMaxMajorVersionDifference method")
    void shouldHaveGetMaxMajorVersionDifferenceMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.class.getMethod("getMaxMajorVersionDifference");
      assertNotNull(method, "getMaxMajorVersionDifference method should exist");
      assertEquals(
          int.class, method.getReturnType(), "getMaxMajorVersionDifference should return int");
    }

    @Test
    @DisplayName("should have getMaxMinorVersionDifference method")
    void shouldHaveGetMaxMinorVersionDifferenceMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.class.getMethod("getMaxMinorVersionDifference");
      assertNotNull(method, "getMaxMinorVersionDifference method should exist");
      assertEquals(
          int.class, method.getReturnType(), "getMaxMinorVersionDifference should return int");
    }
  }

  // ========================================================================
  // Builder Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Class Tests")
  class BuilderClassTests {

    @Test
    @DisplayName("Builder should be a final class")
    void builderShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CompatibilityRequirements.Builder.class.getModifiers()),
          "Builder should be final");
    }

    @Test
    @DisplayName("Builder should be public")
    void builderShouldBePublic() {
      assertTrue(
          Modifier.isPublic(CompatibilityRequirements.Builder.class.getModifiers()),
          "Builder should be public");
    }

    @Test
    @DisplayName("Builder should be static")
    void builderShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(CompatibilityRequirements.Builder.class.getModifiers()),
          "Builder should be static");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          CompatibilityRequirements.class,
          method.getReturnType(),
          "build should return CompatibilityRequirements");
    }

    @Test
    @DisplayName("Builder should have versionRange setter")
    void builderShouldHaveVersionRangeSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod(
              "versionRange", CompatibilityRequirements.VersionRange.class);
      assertNotNull(method, "versionRange setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "versionRange should return Builder");
    }

    @Test
    @DisplayName("Builder should have requiredFeatures setter")
    void builderShouldHaveRequiredFeaturesSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod("requiredFeatures", Set.class);
      assertNotNull(method, "requiredFeatures setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "requiredFeatures should return Builder");
    }

    @Test
    @DisplayName("Builder should have optionalFeatures setter")
    void builderShouldHaveOptionalFeaturesSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod("optionalFeatures", Set.class);
      assertNotNull(method, "optionalFeatures setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "optionalFeatures should return Builder");
    }

    @Test
    @DisplayName("Builder should have excludedVersions setter")
    void builderShouldHaveExcludedVersionsSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod("excludedVersions", Set.class);
      assertNotNull(method, "excludedVersions setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "excludedVersions should return Builder");
    }

    @Test
    @DisplayName("Builder should have minimumCompatibilityLevel setter")
    void builderShouldHaveMinimumCompatibilityLevelSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod(
              "minimumCompatibilityLevel", CompatibilityRequirements.CompatibilityLevel.class);
      assertNotNull(method, "minimumCompatibilityLevel setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "minimumCompatibilityLevel should return Builder");
    }

    @Test
    @DisplayName("Builder should have allowPreRelease setter")
    void builderShouldHaveAllowPreReleaseSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod("allowPreRelease", boolean.class);
      assertNotNull(method, "allowPreRelease setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "allowPreRelease should return Builder");
    }

    @Test
    @DisplayName("Builder should have strictFunctionSignatures setter")
    void builderShouldHaveStrictFunctionSignaturesSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod(
              "strictFunctionSignatures", boolean.class);
      assertNotNull(method, "strictFunctionSignatures setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "strictFunctionSignatures should return Builder");
    }

    @Test
    @DisplayName("Builder should have maxMajorVersionDifference setter")
    void builderShouldHaveMaxMajorVersionDifferenceSetter() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.Builder.class.getMethod("maxMajorVersionDifference", int.class);
      assertNotNull(method, "maxMajorVersionDifference setter should exist");
      assertEquals(
          CompatibilityRequirements.Builder.class,
          method.getReturnType(),
          "maxMajorVersionDifference should return Builder");
    }
  }

  // ========================================================================
  // VersionRange Class Tests
  // ========================================================================

  @Nested
  @DisplayName("VersionRange Class Tests")
  class VersionRangeClassTests {

    @Test
    @DisplayName("VersionRange should be a final class")
    void versionRangeShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CompatibilityRequirements.VersionRange.class.getModifiers()),
          "VersionRange should be final");
    }

    @Test
    @DisplayName("VersionRange should be public")
    void versionRangeShouldBePublic() {
      assertTrue(
          Modifier.isPublic(CompatibilityRequirements.VersionRange.class.getModifiers()),
          "VersionRange should be public");
    }

    @Test
    @DisplayName("VersionRange should be static")
    void versionRangeShouldBeStatic() {
      assertTrue(
          Modifier.isStatic(CompatibilityRequirements.VersionRange.class.getModifiers()),
          "VersionRange should be static");
    }

    @Test
    @DisplayName("VersionRange should have any factory method")
    void versionRangeShouldHaveAnyMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.VersionRange.class.getMethod("any");
      assertNotNull(method, "any method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "any should be static");
      assertEquals(
          CompatibilityRequirements.VersionRange.class,
          method.getReturnType(),
          "any should return VersionRange");
    }

    @Test
    @DisplayName("VersionRange should have between factory method")
    void versionRangeShouldHaveBetweenMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.VersionRange.class.getMethod(
              "between", WitInterfaceVersion.class, WitInterfaceVersion.class);
      assertNotNull(method, "between method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "between should be static");
      assertEquals(
          CompatibilityRequirements.VersionRange.class,
          method.getReturnType(),
          "between should return VersionRange");
    }

    @Test
    @DisplayName("VersionRange should have atLeast factory method")
    void versionRangeShouldHaveAtLeastMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.VersionRange.class.getMethod(
              "atLeast", WitInterfaceVersion.class);
      assertNotNull(method, "atLeast method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "atLeast should be static");
      assertEquals(
          CompatibilityRequirements.VersionRange.class,
          method.getReturnType(),
          "atLeast should return VersionRange");
    }

    @Test
    @DisplayName("VersionRange should have atMost factory method")
    void versionRangeShouldHaveAtMostMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.VersionRange.class.getMethod(
              "atMost", WitInterfaceVersion.class);
      assertNotNull(method, "atMost method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "atMost should be static");
      assertEquals(
          CompatibilityRequirements.VersionRange.class,
          method.getReturnType(),
          "atMost should return VersionRange");
    }

    @Test
    @DisplayName("VersionRange should have contains method")
    void versionRangeShouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.VersionRange.class.getMethod(
              "contains", WitInterfaceVersion.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");
    }

    @Test
    @DisplayName("VersionRange should have getMinimum method")
    void versionRangeShouldHaveGetMinimumMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.VersionRange.class.getMethod("getMinimum");
      assertNotNull(method, "getMinimum method should exist");
      assertEquals(
          WitInterfaceVersion.class,
          method.getReturnType(),
          "getMinimum should return WitInterfaceVersion");
    }

    @Test
    @DisplayName("VersionRange should have getMaximum method")
    void versionRangeShouldHaveGetMaximumMethod() throws NoSuchMethodException {
      final Method method = CompatibilityRequirements.VersionRange.class.getMethod("getMaximum");
      assertNotNull(method, "getMaximum method should exist");
      assertEquals(
          WitInterfaceVersion.class,
          method.getReturnType(),
          "getMaximum should return WitInterfaceVersion");
    }

    @Test
    @DisplayName("VersionRange should have isIncludeMinimum method")
    void versionRangeShouldHaveIsIncludeMinimumMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.VersionRange.class.getMethod("isIncludeMinimum");
      assertNotNull(method, "isIncludeMinimum method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isIncludeMinimum should return boolean");
    }

    @Test
    @DisplayName("VersionRange should have isIncludeMaximum method")
    void versionRangeShouldHaveIsIncludeMaximumMethod() throws NoSuchMethodException {
      final Method method =
          CompatibilityRequirements.VersionRange.class.getMethod("isIncludeMaximum");
      assertNotNull(method, "isIncludeMaximum method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isIncludeMaximum should return boolean");
    }
  }

  // ========================================================================
  // CompatibilityLevel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("CompatibilityLevel Enum Tests")
  class CompatibilityLevelEnumTests {

    @Test
    @DisplayName("CompatibilityLevel should be an enum")
    void compatibilityLevelShouldBeEnum() {
      assertTrue(
          CompatibilityRequirements.CompatibilityLevel.class.isEnum(),
          "CompatibilityLevel should be an enum");
    }

    @Test
    @DisplayName("CompatibilityLevel should be public")
    void compatibilityLevelShouldBePublic() {
      assertTrue(
          Modifier.isPublic(CompatibilityRequirements.CompatibilityLevel.class.getModifiers()),
          "CompatibilityLevel should be public");
    }

    @Test
    @DisplayName("CompatibilityLevel should have FULL value")
    void compatibilityLevelShouldHaveFullValue() {
      assertNotNull(
          CompatibilityRequirements.CompatibilityLevel.FULL,
          "CompatibilityLevel.FULL should exist");
    }

    @Test
    @DisplayName("CompatibilityLevel should have PARTIAL value")
    void compatibilityLevelShouldHavePartialValue() {
      assertNotNull(
          CompatibilityRequirements.CompatibilityLevel.PARTIAL,
          "CompatibilityLevel.PARTIAL should exist");
    }

    @Test
    @DisplayName("CompatibilityLevel should have LIMITED value")
    void compatibilityLevelShouldHaveLimitedValue() {
      assertNotNull(
          CompatibilityRequirements.CompatibilityLevel.LIMITED,
          "CompatibilityLevel.LIMITED should exist");
    }

    @Test
    @DisplayName("CompatibilityLevel should have ANY value")
    void compatibilityLevelShouldHaveAnyValue() {
      assertNotNull(
          CompatibilityRequirements.CompatibilityLevel.ANY, "CompatibilityLevel.ANY should exist");
    }

    @Test
    @DisplayName("CompatibilityLevel should have exactly 4 values")
    void compatibilityLevelShouldHave4Values() {
      assertEquals(
          4,
          CompatibilityRequirements.CompatibilityLevel.values().length,
          "CompatibilityLevel should have exactly 4 values");
    }

    @Test
    @DisplayName("CompatibilityLevel ordinals should be correct")
    void compatibilityLevelOrdinalsShouldBeCorrect() {
      assertEquals(0, CompatibilityRequirements.CompatibilityLevel.FULL.ordinal());
      assertEquals(1, CompatibilityRequirements.CompatibilityLevel.PARTIAL.ordinal());
      assertEquals(2, CompatibilityRequirements.CompatibilityLevel.LIMITED.ordinal());
      assertEquals(3, CompatibilityRequirements.CompatibilityLevel.ANY.ordinal());
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
              "builder",
              "defaults",
              "strict",
              "lenient",
              "getVersionRange",
              "getRequiredFeatures",
              "getOptionalFeatures",
              "getExcludedVersions",
              "getMinimumCompatibilityLevel",
              "isAllowPreRelease",
              "isAllowBuildMetadata",
              "getRequiredFunctions",
              "getRequiredTypes",
              "isStrictFunctionSignatures",
              "isStrictTypeDefinitions",
              "isAllowBackwardCompatible",
              "isAllowForwardCompatible",
              "getMaxMajorVersionDifference",
              "getMaxMinorVersionDifference");

      Set<String> actualMethods =
          Arrays.stream(CompatibilityRequirements.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "CompatibilityRequirements should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 19 declared methods")
    void shouldHaveAtLeast19DeclaredMethods() {
      assertTrue(
          CompatibilityRequirements.class.getDeclaredMethods().length >= 19,
          "CompatibilityRequirements should have at least 19 methods (found "
              + CompatibilityRequirements.class.getDeclaredMethods().length
              + ")");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      Class<?>[] nestedClasses = CompatibilityRequirements.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "Should have Builder nested class");
    }

    @Test
    @DisplayName("should have VersionRange nested class")
    void shouldHaveVersionRangeNestedClass() {
      Class<?>[] nestedClasses = CompatibilityRequirements.class.getDeclaredClasses();
      boolean hasVersionRange =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("VersionRange"));
      assertTrue(hasVersionRange, "Should have VersionRange nested class");
    }

    @Test
    @DisplayName("should have CompatibilityLevel nested enum")
    void shouldHaveCompatibilityLevelNestedEnum() {
      Class<?>[] nestedClasses = CompatibilityRequirements.class.getDeclaredClasses();
      boolean hasCompatibilityLevel =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("CompatibilityLevel"));
      assertTrue(hasCompatibilityLevel, "Should have CompatibilityLevel nested enum");
    }

    @Test
    @DisplayName("should have exactly 3 public nested classes")
    void shouldHave3PublicNestedClasses() {
      long publicNestedCount =
          Arrays.stream(CompatibilityRequirements.class.getDeclaredClasses())
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .count();
      assertEquals(3, publicNestedCount, "Should have exactly 3 public nested classes");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object directly")
    void shouldExtendObjectDirectly() {
      assertEquals(
          Object.class,
          CompatibilityRequirements.class.getSuperclass(),
          "CompatibilityRequirements should extend Object");
    }

    @Test
    @DisplayName("should not implement any interface")
    void shouldNotImplementAnyInterface() {
      assertEquals(
          0,
          CompatibilityRequirements.class.getInterfaces().length,
          "CompatibilityRequirements should not implement any interface");
    }
  }
}
