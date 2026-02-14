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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.security.WasiSecurityValidator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiContextBuilder} class.
 *
 * <p>WasiContextBuilder provides a fluent API for configuring WASI contexts with security,
 * permissions, and resource management.
 */
@DisplayName("WasiContextBuilder Tests")
class WasiContextBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiContextBuilder.class.getModifiers()),
          "WasiContextBuilder should be public");
      assertTrue(
          Modifier.isFinal(WasiContextBuilder.class.getModifiers()),
          "WasiContextBuilder should be final");
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("should have withEnvironment method with name and value")
    void shouldHaveWithEnvironmentMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextBuilder.class.getMethod("withEnvironment", String.class, String.class);
      assertNotNull(method, "withEnvironment(name, value) method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }

    @Test
    @DisplayName("should have withEnvironment method with map")
    void shouldHaveWithEnvironmentMapMethod() throws NoSuchMethodException {
      final Method method = WasiContextBuilder.class.getMethod("withEnvironment", Map.class);
      assertNotNull(method, "withEnvironment(map) method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }

    @Test
    @DisplayName("should have withInheritedEnvironment method")
    void shouldHaveWithInheritedEnvironmentMethod() throws NoSuchMethodException {
      final Method method = WasiContextBuilder.class.getMethod("withInheritedEnvironment");
      assertNotNull(method, "withInheritedEnvironment method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }
  }

  @Nested
  @DisplayName("Arguments Method Tests")
  class ArgumentsMethodTests {

    @Test
    @DisplayName("should have withArgument method")
    void shouldHaveWithArgumentMethod() throws NoSuchMethodException {
      final Method method = WasiContextBuilder.class.getMethod("withArgument", String.class);
      assertNotNull(method, "withArgument method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }

    @Test
    @DisplayName("should have withArguments method")
    void shouldHaveWithArgumentsMethod() throws NoSuchMethodException {
      final Method method = WasiContextBuilder.class.getMethod("withArguments", String[].class);
      assertNotNull(method, "withArguments method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }
  }

  @Nested
  @DisplayName("Directory Method Tests")
  class DirectoryMethodTests {

    @Test
    @DisplayName("should have withPreopenDirectory method with guest and host dirs")
    void shouldHaveWithPreopenDirectoryFullMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextBuilder.class.getMethod("withPreopenDirectory", String.class, String.class);
      assertNotNull(method, "withPreopenDirectory(guest, host) method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }

    @Test
    @DisplayName("should have withPreopenDirectory method with single directory")
    void shouldHaveWithPreopenDirectorySimpleMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextBuilder.class.getMethod("withPreopenDirectory", String.class);
      assertNotNull(method, "withPreopenDirectory(dir) method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }

    @Test
    @DisplayName("should have withWorkingDirectory method")
    void shouldHaveWithWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextBuilder.class.getMethod("withWorkingDirectory", String.class);
      assertNotNull(method, "withWorkingDirectory method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }
  }

  @Nested
  @DisplayName("Security Method Tests")
  class SecurityMethodTests {

    @Test
    @DisplayName("should have withSecurityValidator method")
    void shouldHaveWithSecurityValidatorMethod() throws NoSuchMethodException {
      final Method method =
          WasiContextBuilder.class.getMethod("withSecurityValidator", WasiSecurityValidator.class);
      assertNotNull(method, "withSecurityValidator method should exist");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }
  }

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WasiContextBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "Should return WasiContext");
    }
  }
}
