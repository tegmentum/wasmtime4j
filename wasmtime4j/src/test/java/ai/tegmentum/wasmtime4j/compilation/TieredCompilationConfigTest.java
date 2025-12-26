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

package ai.tegmentum.wasmtime4j.compilation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TieredCompilationConfig} interface.
 *
 * <p>TieredCompilationConfig provides tiered compilation configuration for WebAssembly components.
 */
@DisplayName("TieredCompilationConfig Tests")
class TieredCompilationConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeInterface() {
      assertTrue(
          TieredCompilationConfig.class.isInterface(),
          "TieredCompilationConfig should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(TieredCompilationConfig.class.getModifiers()),
          "TieredCompilationConfig should be public");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEnabled should return boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "setEnabled should return void");
    }

    @Test
    @DisplayName("should have getBaselineTier method")
    void shouldHaveGetBaselineTierMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.class.getMethod("getBaselineTier");
      assertNotNull(method, "getBaselineTier method should exist");
      assertEquals(
          TieredCompilationConfig.TierConfig.class,
          method.getReturnType(),
          "getBaselineTier should return TierConfig");
    }

    @Test
    @DisplayName("should have getOptimizedTier method")
    void shouldHaveGetOptimizedTierMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.class.getMethod("getOptimizedTier");
      assertNotNull(method, "getOptimizedTier method should exist");
      assertEquals(
          TieredCompilationConfig.TierConfig.class,
          method.getReturnType(),
          "getOptimizedTier should return TierConfig");
    }

    @Test
    @DisplayName("should have getTransitionThreshold method")
    void shouldHaveGetTransitionThresholdMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.class.getMethod("getTransitionThreshold");
      assertNotNull(method, "getTransitionThreshold method should exist");
      assertEquals(int.class, method.getReturnType(), "getTransitionThreshold should return int");
    }

    @Test
    @DisplayName("should have setTransitionThreshold method")
    void shouldHaveSetTransitionThresholdMethod() throws NoSuchMethodException {
      final Method method =
          TieredCompilationConfig.class.getMethod("setTransitionThreshold", int.class);
      assertNotNull(method, "setTransitionThreshold method should exist");
      assertEquals(void.class, method.getReturnType(), "setTransitionThreshold should return void");
    }

    @Test
    @DisplayName("should have getCompilationTimeout method")
    void shouldHaveGetCompilationTimeoutMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.class.getMethod("getCompilationTimeout");
      assertNotNull(method, "getCompilationTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "getCompilationTimeout should return long");
    }

    @Test
    @DisplayName("should have setCompilationTimeout method")
    void shouldHaveSetCompilationTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          TieredCompilationConfig.class.getMethod("setCompilationTimeout", long.class);
      assertNotNull(method, "setCompilationTimeout method should exist");
      assertEquals(void.class, method.getReturnType(), "setCompilationTimeout should return void");
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have TierConfig nested interface")
    void shouldHaveTierConfigInterface() {
      final Class<?>[] declaredClasses = TieredCompilationConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("TierConfig".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have TierConfig nested interface");
    }
  }

  @Nested
  @DisplayName("Nested Enum Tests")
  class NestedEnumTests {

    @Test
    @DisplayName("should have CompilationStrategy nested enum")
    void shouldHaveCompilationStrategyEnum() {
      final Class<?>[] declaredClasses = TieredCompilationConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("CompilationStrategy".equals(clazz.getSimpleName()) && clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have CompilationStrategy nested enum");
    }
  }

  @Nested
  @DisplayName("TierConfig Interface Tests")
  class TierConfigInterfaceTests {

    @Test
    @DisplayName("should have getTierName method")
    void shouldHaveGetTierNameMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.TierConfig.class.getMethod("getTierName");
      assertNotNull(method, "getTierName method should exist");
      assertEquals(String.class, method.getReturnType(), "getTierName should return String");
    }

    @Test
    @DisplayName("should have getOptimizationLevel method")
    void shouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      final Method method =
          TieredCompilationConfig.TierConfig.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel method should exist");
      assertEquals(int.class, method.getReturnType(), "getOptimizationLevel should return int");
    }

    @Test
    @DisplayName("should have isInliningEnabled method")
    void shouldHaveIsInliningEnabledMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.TierConfig.class.getMethod("isInliningEnabled");
      assertNotNull(method, "isInliningEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isInliningEnabled should return boolean");
    }

    @Test
    @DisplayName("should have isVectorizationEnabled method")
    void shouldHaveIsVectorizationEnabledMethod() throws NoSuchMethodException {
      final Method method =
          TieredCompilationConfig.TierConfig.class.getMethod("isVectorizationEnabled");
      assertNotNull(method, "isVectorizationEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isVectorizationEnabled should return boolean");
    }

    @Test
    @DisplayName("should have getStrategy method")
    void shouldHaveGetStrategyMethod() throws NoSuchMethodException {
      final Method method = TieredCompilationConfig.TierConfig.class.getMethod("getStrategy");
      assertNotNull(method, "getStrategy method should exist");
      assertEquals(
          TieredCompilationConfig.CompilationStrategy.class,
          method.getReturnType(),
          "getStrategy should return CompilationStrategy");
    }
  }

  @Nested
  @DisplayName("CompilationStrategy Enum Tests")
  class CompilationStrategyEnumTests {

    @Test
    @DisplayName("should have FAST value")
    void shouldHaveFastValue() {
      assertNotNull(
          TieredCompilationConfig.CompilationStrategy.valueOf("FAST"),
          "Should have FAST enum value");
    }

    @Test
    @DisplayName("should have BALANCED value")
    void shouldHaveBalancedValue() {
      assertNotNull(
          TieredCompilationConfig.CompilationStrategy.valueOf("BALANCED"),
          "Should have BALANCED enum value");
    }

    @Test
    @DisplayName("should have OPTIMIZED value")
    void shouldHaveOptimizedValue() {
      assertNotNull(
          TieredCompilationConfig.CompilationStrategy.valueOf("OPTIMIZED"),
          "Should have OPTIMIZED enum value");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactlyThreeValues() {
      assertEquals(
          3,
          TieredCompilationConfig.CompilationStrategy.values().length,
          "CompilationStrategy should have exactly 3 values");
    }
  }
}
