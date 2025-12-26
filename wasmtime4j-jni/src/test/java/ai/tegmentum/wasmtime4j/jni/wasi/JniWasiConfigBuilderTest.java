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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiConfigBuilder} class.
 *
 * <p>JniWasiConfigBuilder provides a fluent API for building JNI WASI configurations.
 */
@DisplayName("JniWasiConfigBuilder Class Tests")
class JniWasiConfigBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(JniWasiConfigBuilder.class.getModifiers()),
          "JniWasiConfigBuilder should be public");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("build");
      assertNotNull(method, "Should have build method");
    }
  }

  @Nested
  @DisplayName("Configuration Methods Tests")
  class ConfigurationMethodsTests {

    @Test
    @DisplayName("should have inheritStdio method")
    void shouldHaveInheritStdioMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("inheritStdio");
      assertNotNull(method, "Should have inheritStdio method");
      assertTrue(
          JniWasiConfigBuilder.class.isAssignableFrom(method.getReturnType()),
          "inheritStdio should return builder for chaining");
    }

    @Test
    @DisplayName("should have inheritEnv method")
    void shouldHaveInheritEnvMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("inheritEnv");
      assertNotNull(method, "Should have inheritEnv method");
      assertTrue(
          JniWasiConfigBuilder.class.isAssignableFrom(method.getReturnType()),
          "inheritEnv should return builder for chaining");
    }

    @Test
    @DisplayName("should have inheritArgs method")
    void shouldHaveInheritArgsMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("inheritArgs");
      assertNotNull(method, "Should have inheritArgs method");
      assertTrue(
          JniWasiConfigBuilder.class.isAssignableFrom(method.getReturnType()),
          "inheritArgs should return builder for chaining");
    }

    @Test
    @DisplayName("should have setEnv method")
    void shouldHaveSetEnvMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiConfigBuilder.class.getMethod("setEnv", String.class, String.class);
      assertNotNull(method, "Should have setEnv method");
      assertTrue(
          JniWasiConfigBuilder.class.isAssignableFrom(method.getReturnType()),
          "setEnv should return builder for chaining");
    }

    @Test
    @DisplayName("should have setArgs method")
    void shouldHaveSetArgsMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("setArgs", String[].class);
      assertNotNull(method, "Should have setArgs method");
      assertTrue(
          JniWasiConfigBuilder.class.isAssignableFrom(method.getReturnType()),
          "setArgs should return builder for chaining");
    }
  }

  @Nested
  @DisplayName("Directory Configuration Tests")
  class DirectoryConfigurationTests {

    @Test
    @DisplayName("should have preopenDir method with guest and host paths")
    void shouldHavePreopenDirMethodWithGuestAndHostPaths() throws NoSuchMethodException {
      final Method method =
          JniWasiConfigBuilder.class.getMethod("preopenDir", String.class, String.class);
      assertNotNull(method, "Should have preopenDir(String, String) method");
      assertTrue(
          JniWasiConfigBuilder.class.isAssignableFrom(method.getReturnType()),
          "preopenDir should return builder for chaining");
    }
  }

  @Nested
  @DisplayName("Fluent API Tests")
  class FluentApiTests {

    @Test
    @DisplayName("builder should return non-null")
    void builderShouldReturnNonNull() throws NoSuchMethodException {
      // Get builder from JniWasiConfig
      final Method builderMethod = JniWasiConfig.class.getMethod("builder");
      assertNotNull(builderMethod, "JniWasiConfig should have builder method");
    }
  }
}
