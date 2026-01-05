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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the DefaultInstanceManagerConfig class.
 *
 * <p>DefaultInstanceManagerConfig is a package-private implementation of
 * InstanceManager.InstanceManagerConfig.
 */
@DisplayName("DefaultInstanceManagerConfig Class Tests")
class DefaultInstanceManagerConfigTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(
          DefaultInstanceManagerConfig.class.isInterface(), "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be package-private")
    void shouldBePackagePrivate() {
      int modifiers = DefaultInstanceManagerConfig.class.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Should not be public");
      assertFalse(Modifier.isProtected(modifiers), "Should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "Should not be private");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(DefaultInstanceManagerConfig.class.getModifiers()),
          "DefaultInstanceManagerConfig should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should implement InstanceManager.InstanceManagerConfig")
    void shouldImplementInstanceManagerConfig() {
      Class<?>[] interfaces = DefaultInstanceManagerConfig.class.getInterfaces();
      assertEquals(1, interfaces.length, "Should implement exactly 1 interface");
      assertEquals(
          InstanceManager.InstanceManagerConfig.class,
          interfaces[0],
          "Should implement InstanceManager.InstanceManagerConfig");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with 9 parameters")
    void shouldHavePackagePrivateConstructorWith9Parameters() {
      Constructor<?>[] constructors = DefaultInstanceManagerConfig.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      int modifiers = constructor.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Constructor should not be public");
      assertFalse(Modifier.isPrivate(modifiers), "Constructor should not be private");
      assertEquals(9, constructor.getParameterCount(), "Should have 9 parameters");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() {
      Constructor<?>[] constructors = DefaultInstanceManagerConfig.class.getDeclaredConstructors();
      Constructor<?> constructor = constructors[0];
      Class<?>[] paramTypes = constructor.getParameterTypes();

      assertEquals(int.class, paramTypes[0], "First param should be int (defaultPoolSize)");
      assertEquals(int.class, paramTypes[1], "Second param should be int (maxPoolSize)");
      assertEquals(
          boolean.class, paramTypes[2], "Third param should be boolean (autoScalingEnabled)");
      assertEquals(double.class, paramTypes[3], "Fourth param should be double (scalingThreshold)");
      assertEquals(
          boolean.class, paramTypes[4], "Fifth param should be boolean (healthMonitoringEnabled)");
      assertEquals(
          Duration.class, paramTypes[5], "Sixth param should be Duration (healthCheckInterval)");
      assertEquals(
          boolean.class, paramTypes[6], "Seventh param should be boolean (migrationEnabled)");
      assertEquals(
          boolean.class, paramTypes[7], "Eighth param should be boolean (checkpointingEnabled)");
      assertEquals(
          Duration.class, paramTypes[8], "Ninth param should be Duration (instanceTimeout)");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have 9 private fields")
    void shouldHave9PrivateFields() {
      long privateFields =
          Arrays.stream(DefaultInstanceManagerConfig.class.getDeclaredFields())
              .filter(f -> Modifier.isPrivate(f.getModifiers()))
              .filter(f -> !f.isSynthetic())
              .count();
      assertEquals(9, privateFields, "Should have 9 private fields");
    }

    @Test
    @DisplayName("should have defaultPoolSize field")
    void shouldHaveDefaultPoolSizeField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfig.class.getDeclaredField("defaultPoolSize");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "defaultPoolSize should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "defaultPoolSize should be final");
      assertEquals(int.class, field.getType(), "defaultPoolSize should be int");
    }

    @Test
    @DisplayName("should have maxPoolSize field")
    void shouldHaveMaxPoolSizeField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfig.class.getDeclaredField("maxPoolSize");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "maxPoolSize should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "maxPoolSize should be final");
      assertEquals(int.class, field.getType(), "maxPoolSize should be int");
    }

    @Test
    @DisplayName("should have autoScalingEnabled field")
    void shouldHaveAutoScalingEnabledField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfig.class.getDeclaredField("autoScalingEnabled");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "autoScalingEnabled should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "autoScalingEnabled should be final");
      assertEquals(boolean.class, field.getType(), "autoScalingEnabled should be boolean");
    }

    @Test
    @DisplayName("should have instanceTimeout field")
    void shouldHaveInstanceTimeoutField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfig.class.getDeclaredField("instanceTimeout");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "instanceTimeout should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "instanceTimeout should be final");
      assertEquals(Duration.class, field.getType(), "instanceTimeout should be Duration");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getDefaultPoolSize method")
    void shouldHaveGetDefaultPoolSizeMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("getDefaultPoolSize");
      assertNotNull(method, "getDefaultPoolSize should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxPoolSize method")
    void shouldHaveGetMaxPoolSizeMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("getMaxPoolSize");
      assertNotNull(method, "getMaxPoolSize should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isAutoScalingEnabled method")
    void shouldHaveIsAutoScalingEnabledMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("isAutoScalingEnabled");
      assertNotNull(method, "isAutoScalingEnabled should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getScalingThreshold method")
    void shouldHaveGetScalingThresholdMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("getScalingThreshold");
      assertNotNull(method, "getScalingThreshold should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have isHealthMonitoringEnabled method")
    void shouldHaveIsHealthMonitoringEnabledMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("isHealthMonitoringEnabled");
      assertNotNull(method, "isHealthMonitoringEnabled should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getHealthCheckInterval method")
    void shouldHaveGetHealthCheckIntervalMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("getHealthCheckInterval");
      assertNotNull(method, "getHealthCheckInterval should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have isMigrationEnabled method")
    void shouldHaveIsMigrationEnabledMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("isMigrationEnabled");
      assertNotNull(method, "isMigrationEnabled should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isCheckpointingEnabled method")
    void shouldHaveIsCheckpointingEnabledMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("isCheckpointingEnabled");
      assertNotNull(method, "isCheckpointingEnabled should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getInstanceTimeout method")
    void shouldHaveGetInstanceTimeoutMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfig.class.getMethod("getInstanceTimeout");
      assertNotNull(method, "getInstanceTimeout should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
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
              "getDefaultPoolSize",
              "getMaxPoolSize",
              "isAutoScalingEnabled",
              "getScalingThreshold",
              "isHealthMonitoringEnabled",
              "getHealthCheckInterval",
              "isMigrationEnabled",
              "isCheckpointingEnabled",
              "getInstanceTimeout");

      Set<String> actualMethods =
          Arrays.stream(DefaultInstanceManagerConfig.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 9 declared public methods")
    void shouldHaveExactly9DeclaredPublicMethods() {
      long methodCount =
          Arrays.stream(DefaultInstanceManagerConfig.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertEquals(9, methodCount, "Should have exactly 9 declared public methods");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          DefaultInstanceManagerConfig.class.getDeclaredClasses().length,
          "DefaultInstanceManagerConfig should have no nested classes");
    }
  }
}
