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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Memory64Config class.
 *
 * <p>Memory64Config provides configuration for 64-bit WebAssembly memory instances.
 */
@DisplayName("Memory64Config Class Tests")
class Memory64ConfigTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(Memory64Config.class.isInterface(), "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(Memory64Config.class.getModifiers()),
          "Memory64Config should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(Memory64Config.class.getModifiers()), "Memory64Config should be final");
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
      Class<?>[] nestedClasses = Memory64Config.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "Should have Builder nested class");
    }

    @Test
    @DisplayName("Builder should be public static final")
    void builderShouldBePublicStaticFinal() {
      Class<?> builderClass =
          Arrays.stream(Memory64Config.class.getDeclaredClasses())
              .filter(c -> c.getSimpleName().equals("Builder"))
              .findFirst()
              .orElseThrow();

      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
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
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("builder", long.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "builder should be public");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have createDefault64Bit static method")
    void shouldHaveCreateDefault64BitStaticMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("createDefault64Bit", long.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "createDefault64Bit should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createDefault64Bit should be public");
      assertEquals(Memory64Config.class, method.getReturnType(), "Should return Memory64Config");
    }

    @Test
    @DisplayName("should have createDefault32Bit static method")
    void shouldHaveCreateDefault32BitStaticMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("createDefault32Bit", long.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "createDefault32Bit should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createDefault32Bit should be public");
      assertEquals(Memory64Config.class, method.getReturnType(), "Should return Memory64Config");
    }

    @Test
    @DisplayName("should have createUnlimited64Bit static method")
    void shouldHaveCreateUnlimited64BitStaticMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("createUnlimited64Bit", long.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "createUnlimited64Bit should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createUnlimited64Bit should be public");
      assertEquals(Memory64Config.class, method.getReturnType(), "Should return Memory64Config");
    }
  }

  // ========================================================================
  // Getter Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getMinimumPages method")
    void shouldHaveGetMinimumPagesMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getMinimumPages");
      assertNotNull(method, "getMinimumPages should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaximumPages method")
    void shouldHaveGetMaximumPagesMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getMaximumPages");
      assertNotNull(method, "getMaximumPages should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have isShared method")
    void shouldHaveIsSharedMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("isShared");
      assertNotNull(method, "isShared should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getAddressingMode method")
    void shouldHaveGetAddressingModeMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getAddressingMode");
      assertNotNull(method, "getAddressingMode should exist");
      assertEquals(
          MemoryAddressingMode.class, method.getReturnType(), "Should return MemoryAddressingMode");
    }

    @Test
    @DisplayName("should have isAutoGrowthAllowed method")
    void shouldHaveIsAutoGrowthAllowedMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("isAutoGrowthAllowed");
      assertNotNull(method, "isAutoGrowthAllowed should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getGrowthFactor method")
    void shouldHaveGetGrowthFactorMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getGrowthFactor");
      assertNotNull(method, "getGrowthFactor should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getGrowthLimitPages method")
    void shouldHaveGetGrowthLimitPagesMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getGrowthLimitPages");
      assertNotNull(method, "getGrowthLimitPages should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getDebugName method")
    void shouldHaveGetDebugNameMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getDebugName");
      assertNotNull(method, "getDebugName should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  // ========================================================================
  // Computed Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Computed Method Tests")
  class ComputedMethodTests {

    @Test
    @DisplayName("should have is64BitAddressing method")
    void shouldHaveIs64BitAddressingMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("is64BitAddressing");
      assertNotNull(method, "is64BitAddressing should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMinimumSizeBytes method")
    void shouldHaveGetMinimumSizeBytesMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getMinimumSizeBytes");
      assertNotNull(method, "getMinimumSizeBytes should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaximumSizeBytes method")
    void shouldHaveGetMaximumSizeBytesMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("getMaximumSizeBytes");
      assertNotNull(method, "getMaximumSizeBytes should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have isWithinLimits method")
    void shouldHaveIsWithinLimitsMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("isWithinLimits", long.class);
      assertNotNull(method, "isWithinLimits should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have isWithinSizeLimits method")
    void shouldHaveIsWithinSizeLimitsMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("isWithinSizeLimits", long.class);
      assertNotNull(method, "isWithinSizeLimits should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have calculateGrowthSize method")
    void shouldHaveCalculateGrowthSizeMethod() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("calculateGrowthSize", long.class);
      assertNotNull(method, "calculateGrowthSize should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }
  }

  // ========================================================================
  // Object Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("equals", Object.class);
      assertEquals(Memory64Config.class, method.getDeclaringClass(), "Should override equals");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("hashCode");
      assertEquals(Memory64Config.class, method.getDeclaringClass(), "Should override hashCode");
    }

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      Method method = Memory64Config.class.getMethod("toString");
      assertEquals(Memory64Config.class, method.getDeclaringClass(), "Should override toString");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected public methods")
    void shouldHaveAllExpectedPublicMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getMinimumPages",
              "getMaximumPages",
              "isShared",
              "getAddressingMode",
              "isAutoGrowthAllowed",
              "getGrowthFactor",
              "getGrowthLimitPages",
              "getDebugName",
              "is64BitAddressing",
              "getMinimumSizeBytes",
              "getMaximumSizeBytes",
              "isWithinLimits",
              "isWithinSizeLimits",
              "calculateGrowthSize",
              "builder",
              "createDefault64Bit",
              "createDefault32Bit",
              "createUnlimited64Bit",
              "equals",
              "hashCode",
              "toString");

      Set<String> actualMethods =
          Arrays.stream(Memory64Config.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Method Tests")
  class BuilderMethodTests {

    @Test
    @DisplayName("Builder should have maximumPages method")
    void builderShouldHaveMaximumPagesMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("maximumPages", long.class);
      assertNotNull(method, "maximumPages should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have unlimitedGrowth method")
    void builderShouldHaveUnlimitedGrowthMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("unlimitedGrowth");
      assertNotNull(method, "unlimitedGrowth should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have shared method")
    void builderShouldHaveSharedMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("shared");
      assertNotNull(method, "shared should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have addressing32Bit method")
    void builderShouldHaveAddressing32BitMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("addressing32Bit");
      assertNotNull(method, "addressing32Bit should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have addressing64Bit method")
    void builderShouldHaveAddressing64BitMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("addressing64Bit");
      assertNotNull(method, "addressing64Bit should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have autoGrowth method")
    void builderShouldHaveAutoGrowthMethod() throws NoSuchMethodException {
      Method method =
          Memory64Config.Builder.class.getMethod("autoGrowth", boolean.class, double.class);
      assertNotNull(method, "autoGrowth should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have growthLimit method")
    void builderShouldHaveGrowthLimitMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("growthLimit", long.class);
      assertNotNull(method, "growthLimit should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have debugName method")
    void builderShouldHaveDebugNameMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("debugName", String.class);
      assertNotNull(method, "debugName should exist");
      assertEquals(Memory64Config.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = Memory64Config.Builder.class.getMethod("build");
      assertNotNull(method, "build should exist");
      assertEquals(Memory64Config.class, method.getReturnType(), "Should return Memory64Config");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class, Memory64Config.class.getSuperclass(), "Should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0, Memory64Config.class.getInterfaces().length, "Should not implement any interfaces");
    }
  }
}
