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

import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiConfig} class.
 *
 * <p>JniWasiConfig represents WASI configuration for JNI implementation.
 */
@DisplayName("JniWasiConfig Class Tests")
class JniWasiConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(JniWasiConfig.class.getModifiers()), "JniWasiConfig should be public");
    }

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiConfig.class.getModifiers()), "JniWasiConfig should be final");
    }

    @Test
    @DisplayName("should implement WasiConfig")
    void shouldImplementWasiConfig() {
      assertTrue(
          WasiConfig.class.isAssignableFrom(JniWasiConfig.class),
          "JniWasiConfig should implement WasiConfig");
    }

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      final Method method = JniWasiConfig.class.getMethod("toBuilder");
      assertNotNull(method, "Should have toBuilder method");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      assertNotNull(
          JniWasiConfig.class.getMethod("getEnvironment"), "Should have getEnvironment method");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      assertNotNull(
          JniWasiConfig.class.getMethod("getArguments"), "Should have getArguments method");
    }

    @Test
    @DisplayName("should have getPreopenDirectories method")
    void shouldHaveGetPreopenDirectoriesMethod() throws NoSuchMethodException {
      assertNotNull(
          JniWasiConfig.class.getMethod("getPreopenDirectories"),
          "Should have getPreopenDirectories method");
    }

    @Test
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          JniWasiConfig.class.getMethod("getWorkingDirectory"),
          "Should have getWorkingDirectory method");
    }

    @Test
    @DisplayName("should have getWasiVersion method")
    void shouldHaveGetWasiVersionMethod() throws NoSuchMethodException {
      assertNotNull(
          JniWasiConfig.class.getMethod("getWasiVersion"), "Should have getWasiVersion method");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      assertNotNull(JniWasiConfig.class.getMethod("validate"), "Should have validate method");
    }
  }
}
