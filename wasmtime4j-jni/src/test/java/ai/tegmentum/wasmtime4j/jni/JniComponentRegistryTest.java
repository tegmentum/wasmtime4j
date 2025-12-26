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

import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link JniComponentRegistry}.
 *
 * <p>Tests verify class structure, method signatures, field definitions, and interface compliance
 * using reflection-based approach to avoid native library dependencies.
 */
@DisplayName("JniComponentRegistry Tests")
class JniComponentRegistryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniComponentRegistry should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniComponentRegistry.class.getModifiers()),
          "JniComponentRegistry should be final");
    }

    @Test
    @DisplayName("JniComponentRegistry should implement ComponentRegistry")
    void shouldImplementComponentRegistry() {
      assertTrue(
          ComponentRegistry.class.isAssignableFrom(JniComponentRegistry.class),
          "JniComponentRegistry should implement ComponentRegistry");
    }

    @Test
    @DisplayName("JniComponentRegistry should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniComponentRegistry.class.getModifiers()),
          "JniComponentRegistry should be public");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have constructor with JniComponentEngine parameter")
    void shouldHaveEngineConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniComponentRegistry.class.getDeclaredConstructor(JniComponentEngine.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("register(ComponentSimple) method should exist")
    void registerMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("register", ComponentSimple.class);
      assertNotNull(method, "register method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("register(String, ComponentSimple) method should exist")
    void registerWithNameMethodShouldExist() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getMethod("register", String.class, ComponentSimple.class);
      assertNotNull(method, "register with name method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("unregister method should exist")
    void unregisterMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("unregister", String.class);
      assertNotNull(method, "unregister method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("findById method should exist and return Optional")
    void findByIdMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("findById", String.class);
      assertNotNull(method, "findById method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("findByName method should exist and return Optional")
    void findByNameMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("findByName", String.class);
      assertNotNull(method, "findByName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("findByVersion method should exist and return List")
    void findByVersionMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("findByVersion", ComponentVersion.class);
      assertNotNull(method, "findByVersion method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("getAllComponents method should exist and return Set")
    void getAllComponentsMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("getAllComponents");
      assertNotNull(method, "getAllComponents method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("getAllComponentIds method should exist and return Set")
    void getAllComponentIdsMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("getAllComponentIds");
      assertNotNull(method, "getAllComponentIds method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("isRegistered method should exist and return boolean")
    void isRegisteredMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("isRegistered", String.class);
      assertNotNull(method, "isRegistered method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("getComponentCount method should exist and return int")
    void getComponentCountMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("getComponentCount");
      assertNotNull(method, "getComponentCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("resolveDependencies method should exist and return Set")
    void resolveDependenciesMethodShouldExist() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getMethod("resolveDependencies", ComponentSimple.class);
      assertNotNull(method, "resolveDependencies method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("validateDependencies method should exist and return ComponentValidationResult")
    void validateDependenciesMethodShouldExist() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getMethod("validateDependencies", ComponentSimple.class);
      assertNotNull(method, "validateDependencies method should exist");
      assertEquals(
          ComponentValidationResult.class,
          method.getReturnType(),
          "Should return ComponentValidationResult");
    }

    @Test
    @DisplayName("search method should exist and return List")
    void searchMethodShouldExist() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getMethod("search", ComponentSearchCriteria.class);
      assertNotNull(method, "search method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("clear method should exist")
    void clearMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("getStatistics method should exist and return ComponentRegistryStatistics")
    void getStatisticsMethodShouldExist() throws NoSuchMethodException {
      Method method = JniComponentRegistry.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ComponentRegistryStatistics.class,
          method.getReturnType(),
          "Should return ComponentRegistryStatistics");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertEquals(Logger.class, field.getType(), "Should be Logger type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have engine field")
    void shouldHaveEngineField() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("engine");
      assertNotNull(field, "engine field should exist");
      assertEquals(JniComponentEngine.class, field.getType(), "Should be JniComponentEngine type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have componentsById field")
    void shouldHaveComponentsByIdField() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("componentsById");
      assertNotNull(field, "componentsById field should exist");
      assertEquals(ConcurrentMap.class, field.getType(), "Should be ConcurrentMap type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have componentsByName field")
    void shouldHaveComponentsByNameField() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("componentsByName");
      assertNotNull(field, "componentsByName field should exist");
      assertEquals(ConcurrentMap.class, field.getType(), "Should be ConcurrentMap type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have componentsByVersion field")
    void shouldHaveComponentsByVersionField() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("componentsByVersion");
      assertNotNull(field, "componentsByVersion field should exist");
      assertEquals(ConcurrentMap.class, field.getType(), "Should be ConcurrentMap type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have registrationCounter field")
    void shouldHaveRegistrationCounterField() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("registrationCounter");
      assertNotNull(field, "registrationCounter field should exist");
      assertEquals(AtomicLong.class, field.getType(), "Should be AtomicLong type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have rollbackRegistration private method")
    void shouldHaveRollbackRegistrationMethod() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getDeclaredMethod(
              "rollbackRegistration", ComponentSimple.class);
      assertNotNull(method, "rollbackRegistration method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have hasCircularDependency private method")
    void shouldHaveHasCircularDependencyMethod() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getDeclaredMethod(
              "hasCircularDependency", ComponentSimple.class, Set.class);
      assertNotNull(method, "hasCircularDependency method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have hasCircularDependencyDFS private method")
    void shouldHaveHasCircularDependencyDfsMethod() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getDeclaredMethod(
              "hasCircularDependencyDFS",
              ComponentSimple.class,
              Set.class,
              Set.class,
              Set.class);
      assertNotNull(method, "hasCircularDependencyDFS method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have matchesCriteria private method")
    void shouldHaveMatchesCriteriaMethod() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getDeclaredMethod(
              "matchesCriteria", ComponentSimple.class, ComponentSearchCriteria.class);
      assertNotNull(method, "matchesCriteria method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have getComponentName private method")
    void shouldHaveGetComponentNameMethod() throws NoSuchMethodException {
      Method method =
          JniComponentRegistry.class.getDeclaredMethod("getComponentName", ComponentSimple.class);
      assertNotNull(method, "getComponentName method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Should implement all ComponentRegistry interface methods")
    void shouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : ComponentRegistry.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniComponentRegistry.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    private boolean arrayEquals(Class<?>[] a, Class<?>[] b) {
      if (a.length != b.length) {
        return false;
      }
      for (int i = 0; i < a.length; i++) {
        if (!a[i].equals(b[i])) {
          return false;
        }
      }
      return true;
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Registry should use ConcurrentHashMap for components by ID")
    void shouldUseConcurrentMapForComponentsById() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("componentsById");
      assertEquals(
          ConcurrentMap.class,
          field.getType(),
          "componentsById should be ConcurrentMap for thread safety");
    }

    @Test
    @DisplayName("Registry should use ConcurrentHashMap for components by name")
    void shouldUseConcurrentMapForComponentsByName() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("componentsByName");
      assertEquals(
          ConcurrentMap.class,
          field.getType(),
          "componentsByName should be ConcurrentMap for thread safety");
    }

    @Test
    @DisplayName("Registry should use ConcurrentHashMap for components by version")
    void shouldUseConcurrentMapForComponentsByVersion() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("componentsByVersion");
      assertEquals(
          ConcurrentMap.class,
          field.getType(),
          "componentsByVersion should be ConcurrentMap for thread safety");
    }

    @Test
    @DisplayName("Registry should use AtomicLong for registration counter")
    void shouldUseAtomicLongForCounter() throws NoSuchFieldException {
      Field field = JniComponentRegistry.class.getDeclaredField("registrationCounter");
      assertEquals(
          AtomicLong.class,
          field.getType(),
          "registrationCounter should be AtomicLong for thread safety");
    }
  }

  @Nested
  @DisplayName("Package Location Tests")
  class PackageLocationTests {

    @Test
    @DisplayName("Class should be in correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni",
          JniComponentRegistry.class.getPackage().getName(),
          "Should be in ai.tegmentum.wasmtime4j.jni package");
    }
  }
}
