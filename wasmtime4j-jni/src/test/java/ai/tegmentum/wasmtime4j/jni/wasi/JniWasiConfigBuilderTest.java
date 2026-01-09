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

import ai.tegmentum.wasmtime4j.wasi.WasiConfigBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiConfigBuilder.class.getModifiers()),
          "JniWasiConfigBuilder should be final");
    }

    @Test
    @DisplayName("should implement WasiConfigBuilder")
    void shouldImplementWasiConfigBuilder() {
      assertTrue(
          WasiConfigBuilder.class.isAssignableFrom(JniWasiConfigBuilder.class),
          "JniWasiConfigBuilder should implement WasiConfigBuilder");
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
    @DisplayName("should have inheritEnvironment method")
    void shouldHaveInheritEnvironmentMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("inheritEnvironment");
      assertNotNull(method, "Should have inheritEnvironment method");
    }

    @Test
    @DisplayName("should have withEnvironment method")
    void shouldHaveWithEnvironmentMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiConfigBuilder.class.getMethod("withEnvironment", String.class, String.class);
      assertNotNull(method, "Should have withEnvironment method");
    }

    @Test
    @DisplayName("should have withEnvironment(Map) method")
    void shouldHaveWithEnvironmentMapMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("withEnvironment", Map.class);
      assertNotNull(method, "Should have withEnvironment(Map) method");
    }

    @Test
    @DisplayName("should have withArgument method")
    void shouldHaveWithArgumentMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("withArgument", String.class);
      assertNotNull(method, "Should have withArgument method");
    }

    @Test
    @DisplayName("should have withArguments method")
    void shouldHaveWithArgumentsMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfigBuilder.class.getMethod("withArguments", List.class);
      assertNotNull(method, "Should have withArguments method");
    }
  }

  @Nested
  @DisplayName("Directory Configuration Tests")
  class DirectoryConfigurationTests {

    @Test
    @DisplayName("should have withPreopenDirectory method")
    void shouldHaveWithPreopenDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiConfigBuilder.class.getMethod("withPreopenDirectory", String.class, Path.class);
      assertNotNull(method, "Should have withPreopenDirectory method");
    }

    @Test
    @DisplayName("should have withPreopenDirectories method")
    void shouldHaveWithPreopenDirectoriesMethod() throws NoSuchMethodException {
      final Method method =
          JniWasiConfigBuilder.class.getMethod("withPreopenDirectories", Map.class);
      assertNotNull(method, "Should have withPreopenDirectories method");
    }
  }

  @Nested
  @DisplayName("Builder Instance Tests")
  class BuilderInstanceTests {

    @Test
    @DisplayName("builder should have public constructor")
    void builderShouldHavePublicConstructor() throws NoSuchMethodException {
      assertNotNull(
          JniWasiConfigBuilder.class.getConstructor(),
          "JniWasiConfigBuilder should have public constructor");
    }

    @Test
    @DisplayName("should create builder instance")
    void shouldCreateBuilderInstance() {
      final JniWasiConfigBuilder builder = new JniWasiConfigBuilder();
      assertNotNull(builder, "Should create builder instance");
    }
  }
}
