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
 * Comprehensive test suite for the DefaultInstanceManagerConfigBuilder class.
 *
 * <p>DefaultInstanceManagerConfigBuilder is a package-private implementation of
 * InstanceManager.InstanceManagerConfig.Builder.
 */
@DisplayName("DefaultInstanceManagerConfigBuilder Class Tests")
class DefaultInstanceManagerConfigBuilderTest {

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
          DefaultInstanceManagerConfigBuilder.class.isInterface(),
          "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be package-private")
    void shouldBePackagePrivate() {
      int modifiers = DefaultInstanceManagerConfigBuilder.class.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Should not be public");
      assertFalse(Modifier.isProtected(modifiers), "Should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "Should not be private");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(DefaultInstanceManagerConfigBuilder.class.getModifiers()),
          "DefaultInstanceManagerConfigBuilder should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should implement InstanceManager.InstanceManagerConfig.Builder")
    void shouldImplementInstanceManagerConfigBuilder() {
      Class<?>[] interfaces = DefaultInstanceManagerConfigBuilder.class.getInterfaces();
      assertEquals(1, interfaces.length, "Should implement exactly 1 interface");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          interfaces[0],
          "Should implement InstanceManager.InstanceManagerConfig.Builder");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() {
      Constructor<?>[] constructors =
          DefaultInstanceManagerConfigBuilder.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");

      Constructor<?> constructor = constructors[0];
      assertEquals(
          0, constructor.getParameterCount(), "Should have 0 parameters (default constructor)");
    }

    @Test
    @DisplayName("constructor should be package-private")
    void constructorShouldBePackagePrivate() {
      Constructor<?>[] constructors =
          DefaultInstanceManagerConfigBuilder.class.getDeclaredConstructors();
      Constructor<?> constructor = constructors[0];
      int modifiers = constructor.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Constructor should not be public");
      assertFalse(Modifier.isPrivate(modifiers), "Constructor should not be private");
      assertFalse(Modifier.isProtected(modifiers), "Constructor should not be protected");
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
          Arrays.stream(DefaultInstanceManagerConfigBuilder.class.getDeclaredFields())
              .filter(f -> Modifier.isPrivate(f.getModifiers()))
              .filter(f -> !f.isSynthetic())
              .count();
      assertEquals(9, privateFields, "Should have 9 private fields");
    }

    @Test
    @DisplayName("should have defaultPoolSize field")
    void shouldHaveDefaultPoolSizeField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfigBuilder.class.getDeclaredField("defaultPoolSize");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "defaultPoolSize should be private");
      assertFalse(
          Modifier.isFinal(field.getModifiers()), "defaultPoolSize should not be final (mutable)");
      assertEquals(int.class, field.getType(), "defaultPoolSize should be int");
    }

    @Test
    @DisplayName("should have maxPoolSize field")
    void shouldHaveMaxPoolSizeField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfigBuilder.class.getDeclaredField("maxPoolSize");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "maxPoolSize should be private");
      assertFalse(
          Modifier.isFinal(field.getModifiers()), "maxPoolSize should not be final (mutable)");
      assertEquals(int.class, field.getType(), "maxPoolSize should be int");
    }

    @Test
    @DisplayName("should have autoScalingEnabled field")
    void shouldHaveAutoScalingEnabledField() throws NoSuchFieldException {
      Field field =
          DefaultInstanceManagerConfigBuilder.class.getDeclaredField("autoScalingEnabled");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "autoScalingEnabled should be private");
      assertEquals(boolean.class, field.getType(), "autoScalingEnabled should be boolean");
    }

    @Test
    @DisplayName("should have scalingThreshold field")
    void shouldHaveScalingThresholdField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfigBuilder.class.getDeclaredField("scalingThreshold");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "scalingThreshold should be private");
      assertEquals(double.class, field.getType(), "scalingThreshold should be double");
    }

    @Test
    @DisplayName("should have healthMonitoringEnabled field")
    void shouldHaveHealthMonitoringEnabledField() throws NoSuchFieldException {
      Field field =
          DefaultInstanceManagerConfigBuilder.class.getDeclaredField("healthMonitoringEnabled");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "healthMonitoringEnabled should be private");
      assertEquals(boolean.class, field.getType(), "healthMonitoringEnabled should be boolean");
    }

    @Test
    @DisplayName("should have healthCheckInterval field")
    void shouldHaveHealthCheckIntervalField() throws NoSuchFieldException {
      Field field =
          DefaultInstanceManagerConfigBuilder.class.getDeclaredField("healthCheckInterval");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "healthCheckInterval should be private");
      assertEquals(Duration.class, field.getType(), "healthCheckInterval should be Duration");
    }

    @Test
    @DisplayName("should have migrationEnabled field")
    void shouldHaveMigrationEnabledField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfigBuilder.class.getDeclaredField("migrationEnabled");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "migrationEnabled should be private");
      assertEquals(boolean.class, field.getType(), "migrationEnabled should be boolean");
    }

    @Test
    @DisplayName("should have checkpointingEnabled field")
    void shouldHaveCheckpointingEnabledField() throws NoSuchFieldException {
      Field field =
          DefaultInstanceManagerConfigBuilder.class.getDeclaredField("checkpointingEnabled");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "checkpointingEnabled should be private");
      assertEquals(boolean.class, field.getType(), "checkpointingEnabled should be boolean");
    }

    @Test
    @DisplayName("should have instanceTimeout field")
    void shouldHaveInstanceTimeoutField() throws NoSuchFieldException {
      Field field = DefaultInstanceManagerConfigBuilder.class.getDeclaredField("instanceTimeout");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "instanceTimeout should be private");
      assertEquals(Duration.class, field.getType(), "instanceTimeout should be Duration");
    }
  }

  // ========================================================================
  // Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Method Tests")
  class BuilderMethodTests {

    @Test
    @DisplayName("should have defaultPoolSize method")
    void shouldHaveDefaultPoolSizeMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod("defaultPoolSize", int.class);
      assertNotNull(method, "defaultPoolSize should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have maxPoolSize method")
    void shouldHaveMaxPoolSizeMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfigBuilder.class.getMethod("maxPoolSize", int.class);
      assertNotNull(method, "maxPoolSize should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have autoScalingEnabled method")
    void shouldHaveAutoScalingEnabledMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod("autoScalingEnabled", boolean.class);
      assertNotNull(method, "autoScalingEnabled should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have scalingThreshold method")
    void shouldHaveScalingThresholdMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod("scalingThreshold", double.class);
      assertNotNull(method, "scalingThreshold should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have healthMonitoringEnabled method")
    void shouldHaveHealthMonitoringEnabledMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod(
              "healthMonitoringEnabled", boolean.class);
      assertNotNull(method, "healthMonitoringEnabled should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have healthCheckInterval method")
    void shouldHaveHealthCheckIntervalMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod(
              "healthCheckInterval", Duration.class);
      assertNotNull(method, "healthCheckInterval should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have migrationEnabled method")
    void shouldHaveMigrationEnabledMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod("migrationEnabled", boolean.class);
      assertNotNull(method, "migrationEnabled should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have checkpointingEnabled method")
    void shouldHaveCheckpointingEnabledMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod(
              "checkpointingEnabled", boolean.class);
      assertNotNull(method, "checkpointingEnabled should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have instanceTimeout method")
    void shouldHaveInstanceTimeoutMethod() throws NoSuchMethodException {
      Method method =
          DefaultInstanceManagerConfigBuilder.class.getMethod("instanceTimeout", Duration.class);
      assertNotNull(method, "instanceTimeout should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.Builder.class,
          method.getReturnType(),
          "Should return Builder");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = DefaultInstanceManagerConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build should exist");
      assertEquals(
          InstanceManager.InstanceManagerConfig.class,
          method.getReturnType(),
          "Should return InstanceManagerConfig");
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
              "defaultPoolSize",
              "maxPoolSize",
              "autoScalingEnabled",
              "scalingThreshold",
              "healthMonitoringEnabled",
              "healthCheckInterval",
              "migrationEnabled",
              "checkpointingEnabled",
              "instanceTimeout",
              "build");

      Set<String> actualMethods =
          Arrays.stream(DefaultInstanceManagerConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 10 declared public methods")
    void shouldHaveExactly10DeclaredPublicMethods() {
      long methodCount =
          Arrays.stream(DefaultInstanceManagerConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertEquals(10, methodCount, "Should have exactly 10 declared public methods");
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
          DefaultInstanceManagerConfigBuilder.class.getDeclaredClasses().length,
          "DefaultInstanceManagerConfigBuilder should have no nested classes");
    }
  }
}
