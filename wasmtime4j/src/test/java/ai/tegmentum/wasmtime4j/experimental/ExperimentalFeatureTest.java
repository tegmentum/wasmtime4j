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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExperimentalFeature} interface.
 *
 * <p>ExperimentalFeature defines the contract for experimental WebAssembly features.
 */
@DisplayName("ExperimentalFeature Tests")
class ExperimentalFeatureTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeInterface() {
      assertTrue(
          ExperimentalFeature.class.isInterface(), "ExperimentalFeature should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExperimentalFeature.class.getModifiers()),
          "ExperimentalFeature should be public");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getFeatureName method")
    void shouldHaveGetFeatureNameMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("getFeatureName");
      assertNotNull(method, "getFeatureName method should exist");
      assertEquals(String.class, method.getReturnType(), "getFeatureName should return String");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "getVersion should return String");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEnabled should return boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "setEnabled should return void");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          ExperimentalFeature.FeatureStatus.class,
          method.getReturnType(),
          "getStatus should return FeatureStatus");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "getDescription should return String");
    }

    @Test
    @DisplayName("should have getDependencies method")
    void shouldHaveGetDependenciesMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("getDependencies");
      assertNotNull(method, "getDependencies method should exist");
      assertEquals(List.class, method.getReturnType(), "getDependencies should return List");
    }

    @Test
    @DisplayName("should have isStable method")
    void shouldHaveIsStableMethod() throws NoSuchMethodException {
      final Method method = ExperimentalFeature.class.getMethod("isStable");
      assertNotNull(method, "isStable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isStable should return boolean");
    }
  }

  @Nested
  @DisplayName("FeatureStatus Enum Tests")
  class FeatureStatusEnumTests {

    @Test
    @DisplayName("should have FeatureStatus nested enum")
    void shouldHaveFeatureStatusEnum() {
      final Class<?>[] declaredClasses = ExperimentalFeature.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if ("FeatureStatus".equals(clazz.getSimpleName()) && clazz.isEnum()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have FeatureStatus nested enum");
    }

    @Test
    @DisplayName("should have EXPERIMENTAL status")
    void shouldHaveExperimentalStatus() {
      assertNotNull(
          ExperimentalFeature.FeatureStatus.valueOf("EXPERIMENTAL"),
          "Should have EXPERIMENTAL enum value");
    }

    @Test
    @DisplayName("should have DEPRECATED status")
    void shouldHaveDeprecatedStatus() {
      assertNotNull(
          ExperimentalFeature.FeatureStatus.valueOf("DEPRECATED"),
          "Should have DEPRECATED enum value");
    }

    @Test
    @DisplayName("should have STABLE status")
    void shouldHaveStableStatus() {
      assertNotNull(
          ExperimentalFeature.FeatureStatus.valueOf("STABLE"), "Should have STABLE enum value");
    }

    @Test
    @DisplayName("should have DISABLED status")
    void shouldHaveDisabledStatus() {
      assertNotNull(
          ExperimentalFeature.FeatureStatus.valueOf("DISABLED"), "Should have DISABLED enum value");
    }

    @Test
    @DisplayName("should have exactly 4 status values")
    void shouldHaveExactlyFourStatusValues() {
      assertEquals(
          4,
          ExperimentalFeature.FeatureStatus.values().length,
          "FeatureStatus should have exactly 4 values");
    }
  }
}
