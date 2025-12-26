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

import ai.tegmentum.wasmtime4j.WitInterfaceEvolution;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWitInterfaceEvolution} class.
 *
 * <p>JniWitInterfaceEvolution provides JNI implementation of WIT interface evolution management.
 */
@DisplayName("JniWitInterfaceEvolution Tests")
class JniWitInterfaceEvolutionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniWitInterfaceEvolution.class.getModifiers()),
          "JniWitInterfaceEvolution should be public");
      assertTrue(
          Modifier.isFinal(JniWitInterfaceEvolution.class.getModifiers()),
          "JniWitInterfaceEvolution should be final");
    }

    @Test
    @DisplayName("should implement WitInterfaceEvolution interface")
    void shouldImplementWitInterfaceEvolutionInterface() {
      assertTrue(
          WitInterfaceEvolution.class.isAssignableFrom(JniWitInterfaceEvolution.class),
          "JniWitInterfaceEvolution should implement WitInterfaceEvolution");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = JniWitInterfaceEvolution.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Analysis Method Tests")
  class AnalysisMethodTests {

    @Test
    @DisplayName("should have analyzeEvolution method")
    void shouldHaveAnalyzeEvolutionMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "analyzeEvolution",
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class,
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class);
      assertNotNull(method, "analyzeEvolution method should exist");
    }

    @Test
    @DisplayName("should have checkBackwardCompatibility method")
    void shouldHaveCheckBackwardCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "checkBackwardCompatibility",
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class,
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class);
      assertNotNull(method, "checkBackwardCompatibility method should exist");
    }

    @Test
    @DisplayName("should have checkForwardCompatibility method")
    void shouldHaveCheckForwardCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "checkForwardCompatibility",
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class,
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class);
      assertNotNull(method, "checkForwardCompatibility method should exist");
    }
  }

  @Nested
  @DisplayName("Adapter Method Tests")
  class AdapterMethodTests {

    @Test
    @DisplayName("should have createAdapter method")
    void shouldHaveCreateAdapterMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "createAdapter",
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class,
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class,
              ai.tegmentum.wasmtime4j.AdaptationConfig.class);
      assertNotNull(method, "createAdapter method should exist");
    }

    @Test
    @DisplayName("should have validateEvolutionStrategy method")
    void shouldHaveValidateEvolutionStrategyMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "validateEvolutionStrategy", ai.tegmentum.wasmtime4j.InterfaceEvolutionStrategy.class);
      assertNotNull(method, "validateEvolutionStrategy method should exist");
    }
  }

  @Nested
  @DisplayName("Migration Method Tests")
  class MigrationMethodTests {

    @Test
    @DisplayName("should have createMigrationPlan method")
    void shouldHaveCreateMigrationPlanMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "createMigrationPlan",
              ai.tegmentum.wasmtime4j.WitInterfaceDefinition.class,
              ai.tegmentum.wasmtime4j.WitInterfaceDefinition.class,
              ai.tegmentum.wasmtime4j.MigrationConfig.class);
      assertNotNull(method, "createMigrationPlan method should exist");
    }

    @Test
    @DisplayName("should have executeMigration method")
    void shouldHaveExecuteMigrationMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "executeMigration", WitInterfaceEvolution.InterfaceMigrationPlan.class);
      assertNotNull(method, "executeMigration method should exist");
    }
  }

  @Nested
  @DisplayName("Version Management Tests")
  class VersionManagementTests {

    @Test
    @DisplayName("should have registerInterfaceVersion method")
    void shouldHaveRegisterInterfaceVersionMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "registerInterfaceVersion", ai.tegmentum.wasmtime4j.WitInterfaceVersion.class);
      assertNotNull(method, "registerInterfaceVersion method should exist");
    }

    @Test
    @DisplayName("should have deprecateInterfaceVersion method")
    void shouldHaveDeprecateInterfaceVersionMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "deprecateInterfaceVersion",
              ai.tegmentum.wasmtime4j.WitInterfaceVersion.class,
              WitInterfaceEvolution.DeprecationInfo.class);
      assertNotNull(method, "deprecateInterfaceVersion method should exist");
    }

    @Test
    @DisplayName("should have getInterfaceVersions method")
    void shouldHaveGetInterfaceVersionsMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceEvolution.class.getMethod("getInterfaceVersions", String.class);
      assertNotNull(method, "getInterfaceVersions method should exist");
      assertEquals(List.class, method.getReturnType(), "getInterfaceVersions should return List");
    }

    @Test
    @DisplayName("should have findCompatibleVersion method")
    void shouldHaveFindCompatibleVersionMethod() throws NoSuchMethodException {
      final Method method =
          JniWitInterfaceEvolution.class.getMethod(
              "findCompatibleVersion", String.class, ai.tegmentum.wasmtime4j.CompatibilityRequirements.class);
      assertNotNull(method, "findCompatibleVersion method should exist");
      assertEquals(Optional.class, method.getReturnType(), "findCompatibleVersion should return Optional");
    }

    @Test
    @DisplayName("should have getEvolutionHistory method")
    void shouldHaveGetEvolutionHistoryMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceEvolution.class.getMethod("getEvolutionHistory", String.class);
      assertNotNull(method, "getEvolutionHistory method should exist");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniWitInterfaceEvolution.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }
}
