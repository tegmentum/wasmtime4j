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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentRegistry;
import ai.tegmentum.wasmtime4j.component.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaComponentRegistry} class.
 *
 * <p>PanamaComponentRegistry provides component registration and discovery functionality.
 */
@DisplayName("PanamaComponentRegistry Tests")
class PanamaComponentRegistryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaComponentRegistry.class.getModifiers()),
          "PanamaComponentRegistry should be public");
      assertTrue(
          Modifier.isFinal(PanamaComponentRegistry.class.getModifiers()),
          "PanamaComponentRegistry should be final");
    }

    @Test
    @DisplayName("should implement ComponentRegistry interface")
    void shouldImplementComponentRegistryInterface() {
      assertTrue(
          ComponentRegistry.class.isAssignableFrom(PanamaComponentRegistry.class),
          "PanamaComponentRegistry should implement ComponentRegistry");
    }
  }

  @Nested
  @DisplayName("Registration Method Tests")
  class RegistrationMethodTests {

    @Test
    @DisplayName("should have register method with Component")
    void shouldHaveRegisterMethodWithComponent() throws NoSuchMethodException {
      final Method method =
          PanamaComponentRegistry.class.getMethod("register", Component.class);
      assertNotNull(method, "register method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have register method with name and Component")
    void shouldHaveRegisterMethodWithNameAndComponent() throws NoSuchMethodException {
      final Method method =
          PanamaComponentRegistry.class.getMethod("register", String.class, Component.class);
      assertNotNull(method, "register method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have unregister method")
    void shouldHaveUnregisterMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("unregister", String.class);
      assertNotNull(method, "unregister method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Lookup Method Tests")
  class LookupMethodTests {

    @Test
    @DisplayName("should have findById method")
    void shouldHaveFindByIdMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("findById", String.class);
      assertNotNull(method, "findById method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have findByName method")
    void shouldHaveFindByNameMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("findByName", String.class);
      assertNotNull(method, "findByName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have findByVersion method")
    void shouldHaveFindByVersionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentRegistry.class.getMethod("findByVersion", ComponentVersion.class);
      assertNotNull(method, "findByVersion method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getAllComponents method")
    void shouldHaveGetAllComponentsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("getAllComponents");
      assertNotNull(method, "getAllComponents method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getAllComponentIds method")
    void shouldHaveGetAllComponentIdsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("getAllComponentIds");
      assertNotNull(method, "getAllComponentIds method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have isRegistered method")
    void shouldHaveIsRegisteredMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("isRegistered", String.class);
      assertNotNull(method, "isRegistered method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getComponentCount method")
    void shouldHaveGetComponentCountMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("getComponentCount");
      assertNotNull(method, "getComponentCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Dependency Method Tests")
  class DependencyMethodTests {

    @Test
    @DisplayName("should have resolveDependencies method")
    void shouldHaveResolveDependenciesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentRegistry.class.getMethod("resolveDependencies", Component.class);
      assertNotNull(method, "resolveDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have validateDependencies method")
    void shouldHaveValidateDependenciesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentRegistry.class.getMethod("validateDependencies", Component.class);
      assertNotNull(method, "validateDependencies method should exist");
      assertEquals(
          ComponentValidationResult.class,
          method.getReturnType(),
          "Should return ComponentValidationResult");
    }
  }

  @Nested
  @DisplayName("Search Method Tests")
  class SearchMethodTests {

    @Test
    @DisplayName("should have search method")
    void shouldHaveSearchMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentRegistry.class.getMethod("search", ComponentSearchCriteria.class);
      assertNotNull(method, "search method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Management Method Tests")
  class ManagementMethodTests {

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentRegistry.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ComponentRegistryStatistics.class,
          method.getReturnType(),
          "Should return ComponentRegistryStatistics");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with PanamaComponentEngine")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      var constructor = PanamaComponentRegistry.class.getConstructor(PanamaComponentEngine.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
