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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompilerConfig} class.
 *
 * <p>CompilerConfig provides configuration settings used by the WebAssembly compiler.
 */
@DisplayName("CompilerConfig Tests")
class CompilerConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CompilerConfig.class.getModifiers()), "CompilerConfig should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CompilerConfig.class.getModifiers()),
          "CompilerConfig should be public");
    }

    @Test
    @DisplayName("should have constructor with all parameters")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          CompilerConfig.class.getConstructor(
              String.class, String.class, boolean.class, boolean.class, Map.class, Map.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getCompilerName method")
    void shouldHaveGetCompilerNameMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("getCompilerName");
      assertNotNull(method, "getCompilerName method should exist");
      assertEquals(String.class, method.getReturnType(), "getCompilerName should return String");
    }

    @Test
    @DisplayName("should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel method should exist");
      assertEquals(
          String.class, method.getReturnType(), "getOptimizationLevel should return String");
    }

    @Test
    @DisplayName("should have hasDebugInfo method")
    void shouldHaveHasDebugInfoMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("hasDebugInfo");
      assertNotNull(method, "hasDebugInfo method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasDebugInfo should return boolean");
    }

    @Test
    @DisplayName("should have hasProfilingInfo method")
    void shouldHaveHasProfilingInfoMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("hasProfilingInfo");
      assertNotNull(method, "hasProfilingInfo method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasProfilingInfo should return boolean");
    }

    @Test
    @DisplayName("should have getFeatures method")
    void shouldHaveGetFeaturesMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("getFeatures");
      assertNotNull(method, "getFeatures method should exist");
      assertEquals(Map.class, method.getReturnType(), "getFeatures should return Map");
    }

    @Test
    @DisplayName("should have getOptions method")
    void shouldHaveGetOptionsMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("getOptions");
      assertNotNull(method, "getOptions method should exist");
      assertEquals(Map.class, method.getReturnType(), "getOptions should return Map");
    }
  }

  @Nested
  @DisplayName("Query Method Tests")
  class QueryMethodTests {

    @Test
    @DisplayName("should have isFeatureEnabled method")
    void shouldHaveIsFeatureEnabledMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("isFeatureEnabled", String.class);
      assertNotNull(method, "isFeatureEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFeatureEnabled should return boolean");
    }

    @Test
    @DisplayName("should have getOption method")
    void shouldHaveGetOptionMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("getOption", String.class);
      assertNotNull(method, "getOption method should exist");
      assertEquals(Object.class, method.getReturnType(), "getOption should return Object");
    }

    @Test
    @DisplayName("should have hasOptimizations method")
    void shouldHaveHasOptimizationsMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("hasOptimizations");
      assertNotNull(method, "hasOptimizations method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasOptimizations should return boolean");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = CompilerConfig.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }

  @Nested
  @DisplayName("Instance Creation Tests")
  class InstanceCreationTests {

    @Test
    @DisplayName("should create instance with valid parameters")
    void shouldCreateInstanceWithValidParameters() {
      final Map<String, Object> features = Map.of("simd", true, "threads", false);
      final Map<String, Object> options = Map.of("inlineThreshold", 100);

      final CompilerConfig config =
          new CompilerConfig("cranelift", "speed", true, false, features, options);

      assertEquals("cranelift", config.getCompilerName(), "Compiler name should match");
      assertEquals("speed", config.getOptimizationLevel(), "Optimization level should match");
      assertTrue(config.hasDebugInfo(), "Debug info should be true");
      assertFalse(config.hasProfilingInfo(), "Profiling info should be false");
      assertEquals(features, config.getFeatures(), "Features should match");
      assertEquals(options, config.getOptions(), "Options should match");
    }

    @Test
    @DisplayName("should throw exception for null compiler name")
    void shouldThrowExceptionForNullCompilerName() {
      assertThrows(
          NullPointerException.class,
          () -> new CompilerConfig(null, "speed", true, false, Map.of(), Map.of()),
          "Should throw exception for null compiler name");
    }

    @Test
    @DisplayName("should throw exception for null optimization level")
    void shouldThrowExceptionForNullOptimizationLevel() {
      assertThrows(
          NullPointerException.class,
          () -> new CompilerConfig("cranelift", null, true, false, Map.of(), Map.of()),
          "Should throw exception for null optimization level");
    }

    @Test
    @DisplayName("should check if feature is enabled with boolean value")
    void shouldCheckIfFeatureIsEnabledWithBooleanValue() {
      final Map<String, Object> features = Map.of("simd", true, "threads", false);
      final CompilerConfig config =
          new CompilerConfig("cranelift", "speed", false, false, features, Map.of());

      assertTrue(config.isFeatureEnabled("simd"), "SIMD should be enabled");
      assertFalse(config.isFeatureEnabled("threads"), "Threads should be disabled");
    }

    @Test
    @DisplayName("should check if feature is enabled with presence")
    void shouldCheckIfFeatureIsEnabledWithPresence() {
      final Map<String, Object> features = Map.of("simd", "auto");
      final CompilerConfig config =
          new CompilerConfig("cranelift", "speed", false, false, features, Map.of());

      assertTrue(config.isFeatureEnabled("simd"), "SIMD should be enabled (present)");
      assertFalse(config.isFeatureEnabled("unknown"), "Unknown should not be enabled");
    }

    @Test
    @DisplayName("should get option value")
    void shouldGetOptionValue() {
      final Map<String, Object> options = Map.of("inlineThreshold", 100, "unrollFactor", 4);
      final CompilerConfig config =
          new CompilerConfig("cranelift", "speed", false, false, Map.of(), options);

      assertEquals(100, config.getOption("inlineThreshold"), "Should return option value");
      assertNull(config.getOption("unknown"), "Should return null for unknown option");
    }

    @Test
    @DisplayName("should detect optimizations enabled")
    void shouldDetectOptimizationsEnabled() {
      final CompilerConfig speedConfig =
          new CompilerConfig("cranelift", "speed", false, false, Map.of(), Map.of());
      assertTrue(speedConfig.hasOptimizations(), "Speed level should have optimizations");

      final CompilerConfig sizeConfig =
          new CompilerConfig("cranelift", "size", false, false, Map.of(), Map.of());
      assertTrue(sizeConfig.hasOptimizations(), "Size level should have optimizations");

      final CompilerConfig noneConfig =
          new CompilerConfig("cranelift", "none", false, false, Map.of(), Map.of());
      assertFalse(noneConfig.hasOptimizations(), "None level should not have optimizations");

      final CompilerConfig upperNoneConfig =
          new CompilerConfig("cranelift", "NONE", false, false, Map.of(), Map.of());
      assertFalse(
          upperNoneConfig.hasOptimizations(), "NONE (uppercase) should not have optimizations");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final CompilerConfig config1 =
          new CompilerConfig("cranelift", "speed", true, false, Map.of("simd", true), Map.of());
      final CompilerConfig config2 =
          new CompilerConfig("cranelift", "speed", true, false, Map.of("simd", true), Map.of());
      final CompilerConfig config3 =
          new CompilerConfig("cranelift", "size", true, false, Map.of("simd", true), Map.of());

      assertEquals(config1, config2, "Identical configs should be equal");
      assertFalse(config1.equals(config3), "Different configs should not be equal");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final CompilerConfig config1 =
          new CompilerConfig("cranelift", "speed", true, false, Map.of("simd", true), Map.of());
      final CompilerConfig config2 =
          new CompilerConfig("cranelift", "speed", true, false, Map.of("simd", true), Map.of());

      assertEquals(
          config1.hashCode(), config2.hashCode(), "Identical configs should have same hash code");
    }
  }
}
