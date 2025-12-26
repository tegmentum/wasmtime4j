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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExperimentalFeatureConfig} interface.
 *
 * <p>ExperimentalFeatureConfig defines configuration for experimental WebAssembly features.
 */
@DisplayName("ExperimentalFeatureConfig Tests")
class ExperimentalFeatureConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeInterface() {
      assertTrue(
          ExperimentalFeatureConfig.class.isInterface(),
          "ExperimentalFeatureConfig should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExperimentalFeatureConfig.class.getModifiers()),
          "ExperimentalFeatureConfig should be public");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have isGloballyEnabled method")
    void shouldHaveIsGloballyEnabledMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatureConfig.class.getMethod("isGloballyEnabled");
      assertNotNull(method, "isGloballyEnabled method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isGloballyEnabled should return boolean");
    }

    @Test
    @DisplayName("should have setGloballyEnabled method")
    void shouldHaveSetGloballyEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatureConfig.class.getMethod("setGloballyEnabled", boolean.class);
      assertNotNull(method, "setGloballyEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "setGloballyEnabled should return void");
    }

    @Test
    @DisplayName("should have getEnabledFeatures method")
    void shouldHaveGetEnabledFeaturesMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatureConfig.class.getMethod("getEnabledFeatures");
      assertNotNull(method, "getEnabledFeatures method should exist");
      assertEquals(List.class, method.getReturnType(), "getEnabledFeatures should return List");
    }

    @Test
    @DisplayName("should have enableFeature method")
    void shouldHaveEnableFeatureMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatureConfig.class.getMethod("enableFeature", String.class);
      assertNotNull(method, "enableFeature method should exist");
      assertEquals(void.class, method.getReturnType(), "enableFeature should return void");
    }

    @Test
    @DisplayName("should have disableFeature method")
    void shouldHaveDisableFeatureMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatureConfig.class.getMethod("disableFeature", String.class);
      assertNotNull(method, "disableFeature method should exist");
      assertEquals(void.class, method.getReturnType(), "disableFeature should return void");
    }

    @Test
    @DisplayName("should have isFeatureEnabled method")
    void shouldHaveIsFeatureEnabledMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatureConfig.class.getMethod("isFeatureEnabled", String.class);
      assertNotNull(method, "isFeatureEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFeatureEnabled should return boolean");
    }

    @Test
    @DisplayName("should have getConfigVersion method")
    void shouldHaveGetConfigVersionMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatureConfig.class.getMethod("getConfigVersion");
      assertNotNull(method, "getConfigVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "getConfigVersion should return String");
    }

    @Test
    @DisplayName("should have loadFromProperties method")
    void shouldHaveLoadFromPropertiesMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatureConfig.class.getMethod("loadFromProperties", Properties.class);
      assertNotNull(method, "loadFromProperties method should exist");
      assertEquals(void.class, method.getReturnType(), "loadFromProperties should return void");
    }

    @Test
    @DisplayName("should have saveToProperties method")
    void shouldHaveSaveToPropertiesMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatureConfig.class.getMethod("saveToProperties");
      assertNotNull(method, "saveToProperties method should exist");
      assertEquals(
          Properties.class, method.getReturnType(), "saveToProperties should return Properties");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatureConfig.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(
          ExperimentalFeatureConfig.ValidationResult.class,
          method.getReturnType(),
          "validate should return ValidationResult");
    }
  }

  @Nested
  @DisplayName("ValidationResult Interface Tests")
  class ValidationResultInterfaceTests {

    @Test
    @DisplayName("should have ValidationResult nested interface")
    void shouldHaveValidationResultInterface() {
      final Class<?>[] declaredClasses = ExperimentalFeatureConfig.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("ValidationResult".equals(clazz.getSimpleName()) && clazz.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ValidationResult nested interface");
    }

    @Test
    @DisplayName("ValidationResult should have isValid method")
    void validationResultShouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatureConfig.ValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("ValidationResult should have getErrors method")
    void validationResultShouldHaveGetErrorsMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeatureConfig.ValidationResult.class.getMethod("getErrors");
      assertNotNull(method, "getErrors method should exist");
      assertEquals(List.class, method.getReturnType(), "getErrors should return List");
    }

    @Test
    @DisplayName("ValidationResult should have getWarnings method")
    void validationResultShouldHaveGetWarningsMethod() throws NoSuchMethodException {
      final Method method =
          ExperimentalFeatureConfig.ValidationResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "getWarnings should return List");
    }
  }
}
