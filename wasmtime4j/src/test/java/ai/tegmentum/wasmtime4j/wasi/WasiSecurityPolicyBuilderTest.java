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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiSecurityPolicyBuilder interface.
 *
 * <p>WasiSecurityPolicyBuilder provides a fluent API for creating WASI security policies that
 * control access to system resources.
 */
@DisplayName("WasiSecurityPolicyBuilder Interface Tests")
class WasiSecurityPolicyBuilderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiSecurityPolicyBuilder.class.isInterface(),
          "WasiSecurityPolicyBuilder should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiSecurityPolicyBuilder.class.getModifiers()),
          "WasiSecurityPolicyBuilder should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          WasiSecurityPolicyBuilder.class.getInterfaces().length,
          "WasiSecurityPolicyBuilder should not extend any interfaces");
    }
  }

  // ========================================================================
  // Mode Configuration Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Mode Configuration Method Tests")
  class ModeConfigurationMethodTests {

    @Test
    @DisplayName("should have withPermissiveMode method")
    void shouldHaveWithPermissiveModeMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyBuilder.class.getMethod("withPermissiveMode", boolean.class);
      assertNotNull(method, "withPermissiveMode method should exist");
      assertEquals(
          WasiSecurityPolicyBuilder.class,
          method.getReturnType(),
          "Should return WasiSecurityPolicyBuilder for chaining");
    }

    @Test
    @DisplayName("should have withRestrictiveMode method")
    void shouldHaveWithRestrictiveModeMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyBuilder.class.getMethod("withRestrictiveMode", boolean.class);
      assertNotNull(method, "withRestrictiveMode method should exist");
      assertEquals(
          WasiSecurityPolicyBuilder.class,
          method.getReturnType(),
          "Should return WasiSecurityPolicyBuilder for chaining");
    }
  }

  // ========================================================================
  // Path Configuration Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Path Configuration Method Tests")
  class PathConfigurationMethodTests {

    @Test
    @DisplayName("should have withAllowedPath method")
    void shouldHaveWithAllowedPathMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyBuilder.class.getMethod("withAllowedPath", Path.class);
      assertNotNull(method, "withAllowedPath method should exist");
      assertEquals(
          WasiSecurityPolicyBuilder.class,
          method.getReturnType(),
          "Should return WasiSecurityPolicyBuilder for chaining");
    }

    @Test
    @DisplayName("should have withBlockedPath method")
    void shouldHaveWithBlockedPathMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyBuilder.class.getMethod("withBlockedPath", Path.class);
      assertNotNull(method, "withBlockedPath method should exist");
      assertEquals(
          WasiSecurityPolicyBuilder.class,
          method.getReturnType(),
          "Should return WasiSecurityPolicyBuilder for chaining");
    }
  }

  // ========================================================================
  // Operations Configuration Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Operations Configuration Method Tests")
  class OperationsConfigurationMethodTests {

    @Test
    @DisplayName("should have withAllowedFileSystemOperations method")
    void shouldHaveWithAllowedFileSystemOperationsMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyBuilder.class.getMethod("withAllowedFileSystemOperations", Set.class);
      assertNotNull(method, "withAllowedFileSystemOperations method should exist");
      assertEquals(
          WasiSecurityPolicyBuilder.class,
          method.getReturnType(),
          "Should return WasiSecurityPolicyBuilder for chaining");
    }

    @Test
    @DisplayName("should have withAllowedNetworkOperations method")
    void shouldHaveWithAllowedNetworkOperationsMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyBuilder.class.getMethod("withAllowedNetworkOperations", Set.class);
      assertNotNull(method, "withAllowedNetworkOperations method should exist");
      assertEquals(
          WasiSecurityPolicyBuilder.class,
          method.getReturnType(),
          "Should return WasiSecurityPolicyBuilder for chaining");
    }
  }

  // ========================================================================
  // Build Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiSecurityPolicy.class, method.getReturnType(), "Should return WasiSecurityPolicy");
    }
  }

  // ========================================================================
  // Method Chaining Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Chaining Tests")
  class MethodChainingTests {

    @Test
    @DisplayName("all configuration methods should return the builder")
    void allConfigurationMethodsShouldReturnBuilder() {
      long builderReturningMethods =
          Arrays.stream(WasiSecurityPolicyBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> m.getReturnType().equals(WasiSecurityPolicyBuilder.class))
              .count();
      assertTrue(builderReturningMethods >= 6, "Should have at least 6 methods returning builder");
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
          WasiSecurityPolicyBuilder.class.getDeclaredClasses().length,
          "WasiSecurityPolicyBuilder should have no nested classes");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 7 abstract methods")
    void shouldHaveExpectedMethodCount() {
      long abstractMethodCount =
          Arrays.stream(WasiSecurityPolicyBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractMethodCount >= 7, "Should have at least 7 abstract methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          WasiSecurityPolicyBuilder.class.getDeclaredFields().length,
          "WasiSecurityPolicyBuilder should have no declared fields");
    }
  }
}
