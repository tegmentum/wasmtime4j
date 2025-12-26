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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniExperimentalFeatures} class.
 *
 * <p>JniExperimentalFeatures provides JNI implementation for experimental WebAssembly features.
 */
@DisplayName("JniExperimentalFeatures Tests")
class JniExperimentalFeaturesTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniExperimentalFeatures.class.getModifiers()),
          "JniExperimentalFeatures should be public");
      assertTrue(
          Modifier.isFinal(JniExperimentalFeatures.class.getModifiers()),
          "JniExperimentalFeatures should be final");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = JniExperimentalFeatures.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have allExperimentalEnabled static method")
    void shouldHaveAllExperimentalEnabledMethod() throws NoSuchMethodException {
      final Method method = JniExperimentalFeatures.class.getMethod("allExperimentalEnabled");
      assertNotNull(method, "allExperimentalEnabled method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "allExperimentalEnabled should be static");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "allExperimentalEnabled should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have isStackSwitchingSupported static method")
    void shouldHaveIsStackSwitchingSupportedMethod() throws NoSuchMethodException {
      final Method method = JniExperimentalFeatures.class.getMethod("isStackSwitchingSupported");
      assertNotNull(method, "isStackSwitchingSupported method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "isStackSwitchingSupported should be static");
      assertEquals(
          boolean.class, method.getReturnType(), "isStackSwitchingSupported should return boolean");
    }

    @Test
    @DisplayName("should have isCallCcSupported static method")
    void shouldHaveIsCallCcSupportedMethod() throws NoSuchMethodException {
      final Method method = JniExperimentalFeatures.class.getMethod("isCallCcSupported");
      assertNotNull(method, "isCallCcSupported method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isCallCcSupported should be static");
      assertEquals(
          boolean.class, method.getReturnType(), "isCallCcSupported should return boolean");
    }

    @Test
    @DisplayName("should have isExperimentalFeatureSupported static method")
    void shouldHaveIsExperimentalFeatureSupportedMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "isExperimentalFeatureSupported",
              JniExperimentalFeatures.ExperimentalFeatureId.class);
      assertNotNull(method, "isExperimentalFeatureSupported method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "isExperimentalFeatureSupported should be static");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isExperimentalFeatureSupported should return boolean");
    }

    @Test
    @DisplayName("should have getSupportedExperimentalFeatures static method")
    void shouldHaveGetSupportedExperimentalFeaturesMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod("getSupportedExperimentalFeatures");
      assertNotNull(method, "getSupportedExperimentalFeatures method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "getSupportedExperimentalFeatures should be static");
      assertEquals(
          Set.class, method.getReturnType(), "getSupportedExperimentalFeatures should return Set");
    }
  }

  @Nested
  @DisplayName("Feature Configuration Method Tests")
  class FeatureConfigurationMethodTests {

    @Test
    @DisplayName("should have enableStackSwitching method")
    void shouldHaveEnableStackSwitchingMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod("enableStackSwitching", long.class, int.class);
      assertNotNull(method, "enableStackSwitching method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableStackSwitching should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableCallCc method")
    void shouldHaveEnableCallCcMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableCallCc", int.class, JniExperimentalFeatures.ContinuationStorageStrategy.class);
      assertNotNull(method, "enableCallCc method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableCallCc should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableExtendedConstExpressions method")
    void shouldHaveEnableExtendedConstExpressionsMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableExtendedConstExpressions",
              boolean.class,
              boolean.class,
              JniExperimentalFeatures.ConstantFoldingLevel.class);
      assertNotNull(method, "enableExtendedConstExpressions method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableExtendedConstExpressions should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableFlexibleVectors method")
    void shouldHaveEnableFlexibleVectorsMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableFlexibleVectors", boolean.class, boolean.class, boolean.class);
      assertNotNull(method, "enableFlexibleVectors method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableFlexibleVectors should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableStringImports method")
    void shouldHaveEnableStringImportsMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableStringImports",
              JniExperimentalFeatures.StringEncodingFormat.class,
              boolean.class,
              boolean.class,
              boolean.class);
      assertNotNull(method, "enableStringImports method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableStringImports should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableResourceTypes method")
    void shouldHaveEnableResourceTypesMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableResourceTypes",
              boolean.class,
              boolean.class,
              JniExperimentalFeatures.ResourceCleanupStrategy.class);
      assertNotNull(method, "enableResourceTypes method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableResourceTypes should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableTypeImports method")
    void shouldHaveEnableTypeImportsMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableTypeImports",
              JniExperimentalFeatures.TypeValidationStrategy.class,
              JniExperimentalFeatures.ImportResolutionMechanism.class,
              boolean.class);
      assertNotNull(method, "enableTypeImports method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableTypeImports should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableSharedEverythingThreads method")
    void shouldHaveEnableSharedEverythingThreadsMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableSharedEverythingThreads", int.class, int.class, boolean.class, boolean.class);
      assertNotNull(method, "enableSharedEverythingThreads method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableSharedEverythingThreads should return JniExperimentalFeatures");
    }

    @Test
    @DisplayName("should have enableCustomPageSizes method")
    void shouldHaveEnableCustomPageSizesMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod(
              "enableCustomPageSizes",
              int.class,
              JniExperimentalFeatures.PageSizeStrategy.class,
              boolean.class);
      assertNotNull(method, "enableCustomPageSizes method should exist");
      assertEquals(
          JniExperimentalFeatures.class,
          method.getReturnType(),
          "enableCustomPageSizes should return JniExperimentalFeatures");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have applyToWasmtimeConfig method")
    void shouldHaveApplyToWasmtimeConfigMethod() throws NoSuchMethodException {
      final Method method =
          JniExperimentalFeatures.class.getMethod("applyToWasmtimeConfig", long.class);
      assertNotNull(method, "applyToWasmtimeConfig method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "applyToWasmtimeConfig should return boolean");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = JniExperimentalFeatures.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have dispose method")
    void shouldHaveDisposeMethod() throws NoSuchMethodException {
      final Method method = JniExperimentalFeatures.class.getMethod("dispose");
      assertNotNull(method, "dispose method should exist");
      assertEquals(void.class, method.getReturnType(), "dispose should return void");
    }
  }

  @Nested
  @DisplayName("Nested Enum Tests")
  class NestedEnumTests {

    @Test
    @DisplayName("should have ContinuationStorageStrategy enum")
    void shouldHaveContinuationStorageStrategyEnum() {
      final Class<?>[] declaredClasses = JniExperimentalFeatures.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ContinuationStorageStrategy") && clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "ContinuationStorageStrategy enum should exist");
    }

    @Test
    @DisplayName("should have ConstantFoldingLevel enum")
    void shouldHaveConstantFoldingLevelEnum() {
      final Class<?>[] declaredClasses = JniExperimentalFeatures.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ConstantFoldingLevel") && clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "ConstantFoldingLevel enum should exist");
    }

    @Test
    @DisplayName("should have StringEncodingFormat enum")
    void shouldHaveStringEncodingFormatEnum() {
      final Class<?>[] declaredClasses = JniExperimentalFeatures.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("StringEncodingFormat") && clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "StringEncodingFormat enum should exist");
    }

    @Test
    @DisplayName("should have ExperimentalFeatureId enum")
    void shouldHaveExperimentalFeatureIdEnum() {
      final Class<?>[] declaredClasses = JniExperimentalFeatures.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ExperimentalFeatureId") && clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "ExperimentalFeatureId enum should exist");
    }

    @Test
    @DisplayName("ExperimentalFeatureId should have correct values")
    void experimentalFeatureIdShouldHaveCorrectValues() {
      final JniExperimentalFeatures.ExperimentalFeatureId[] values =
          JniExperimentalFeatures.ExperimentalFeatureId.values();
      assertTrue(values.length >= 11, "Should have at least 11 experimental feature IDs");

      assertEquals(0, JniExperimentalFeatures.ExperimentalFeatureId.STACK_SWITCHING.getId());
      assertEquals(1, JniExperimentalFeatures.ExperimentalFeatureId.CALL_CC.getId());
      assertEquals(
          2, JniExperimentalFeatures.ExperimentalFeatureId.EXTENDED_CONST_EXPRESSIONS.getId());
      assertEquals(3, JniExperimentalFeatures.ExperimentalFeatureId.MEMORY64_EXTENDED.getId());
    }
  }
}
