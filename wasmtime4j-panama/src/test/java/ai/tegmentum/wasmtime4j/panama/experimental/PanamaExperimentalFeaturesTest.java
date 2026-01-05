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

package ai.tegmentum.wasmtime4j.panama.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.experimental.ExperimentalFeature;
import ai.tegmentum.wasmtime4j.experimental.ExperimentalFeatureConfig;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaExperimentalFeatures} class.
 *
 * <p>PanamaExperimentalFeatures provides access to experimental WebAssembly features.
 */
@DisplayName("PanamaExperimentalFeatures Tests")
class PanamaExperimentalFeaturesTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaExperimentalFeatures.class.getModifiers()),
          "PanamaExperimentalFeatures should be public");
      assertTrue(
          Modifier.isFinal(PanamaExperimentalFeatures.class.getModifiers()),
          "PanamaExperimentalFeatures should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaExperimentalFeatures.class),
          "PanamaExperimentalFeatures should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Feature Control Method Tests")
  class FeatureControlMethodTests {

    @Test
    @DisplayName("should have enableExperimentalFeature method")
    void shouldHaveEnableExperimentalFeatureMethod() throws NoSuchMethodException {
      final Method method =
          PanamaExperimentalFeatures.class.getMethod(
              "enableExperimentalFeature", ExperimentalFeature.class);
      assertNotNull(method, "enableExperimentalFeature method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have disableExperimentalFeature method")
    void shouldHaveDisableExperimentalFeatureMethod() throws NoSuchMethodException {
      final Method method =
          PanamaExperimentalFeatures.class.getMethod(
              "disableExperimentalFeature", ExperimentalFeature.class);
      assertNotNull(method, "disableExperimentalFeature method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isFeatureEnabled method")
    void shouldHaveIsFeatureEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PanamaExperimentalFeatures.class.getMethod("isFeatureEnabled", ExperimentalFeature.class);
      assertNotNull(method, "isFeatureEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("should have configureStackSwitching method")
    void shouldHaveConfigureStackSwitchingMethod() throws NoSuchMethodException {
      final Method method =
          PanamaExperimentalFeatures.class.getMethod(
              "configureStackSwitching", long.class, int.class, int.class);
      assertNotNull(method, "configureStackSwitching method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have configureCallCc method")
    void shouldHaveConfigureCallCcMethod() throws NoSuchMethodException {
      final Method method =
          PanamaExperimentalFeatures.class.getMethod(
              "configureCallCc", int.class, int.class, int.class);
      assertNotNull(method, "configureCallCc method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have configureAdvancedSecurity method")
    void shouldHaveConfigureAdvancedSecurityMethod() throws NoSuchMethodException {
      final Method method =
          PanamaExperimentalFeatures.class.getMethod(
              "configureAdvancedSecurity", int.class, int.class, int.class, int.class);
      assertNotNull(method, "configureAdvancedSecurity method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have configureAdvancedProfiling method")
    void shouldHaveConfigureAdvancedProfilingMethod() throws NoSuchMethodException {
      final Method method =
          PanamaExperimentalFeatures.class.getMethod(
              "configureAdvancedProfiling", int.class, int.class, int.class, long.class);
      assertNotNull(method, "configureAdvancedProfiling method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Profiling Method Tests")
  class ProfilingMethodTests {

    @Test
    @DisplayName("should have startProfiling method")
    void shouldHaveStartProfilingMethod() throws NoSuchMethodException {
      final Method method = PanamaExperimentalFeatures.class.getMethod("startProfiling");
      assertNotNull(method, "startProfiling method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stopProfiling method")
    void shouldHaveStopProfilingMethod() throws NoSuchMethodException {
      final Method method = PanamaExperimentalFeatures.class.getMethod("stopProfiling");
      assertNotNull(method, "stopProfiling method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getProfilingResults method")
    void shouldHaveGetProfilingResultsMethod() throws NoSuchMethodException {
      final Method method = PanamaExperimentalFeatures.class.getMethod("getProfilingResults");
      assertNotNull(method, "getProfilingResults method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have isInitialized method")
    void shouldHaveIsInitializedMethod() throws NoSuchMethodException {
      final Method method = PanamaExperimentalFeatures.class.getMethod("isInitialized");
      assertNotNull(method, "isInitialized method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaExperimentalFeatures.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with ExperimentalFeatureConfig")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      var constructor =
          PanamaExperimentalFeatures.class.getConstructor(ExperimentalFeatureConfig.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
