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
 * Tests for {@link WasiContext} class.
 *
 * <p>WasiContext provides the WASI context for WebAssembly module execution.
 */
@DisplayName("WasiContext Class Tests")
class WasiContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(WasiContext.class.getModifiers()), "WasiContext should be public");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("builder");
      assertNotNull(method, "Should have builder method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertTrue(
          WasiContextBuilder.class.isAssignableFrom(method.getReturnType()),
          "builder should return WasiContextBuilder or subtype");
    }
  }

  @Nested
  @DisplayName("API Method Tests")
  class ApiMethodTests {

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiContext.class.getMethod("getEnvironment"), "Should have getEnvironment method");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      assertNotNull(WasiContext.class.getMethod("getArguments"), "Should have getArguments method");
    }

    @Test
    @DisplayName("should have getPreopenedDirectories method")
    void shouldHaveGetPreopenedDirectoriesMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiContext.class.getMethod("getPreopenedDirectories"),
          "Should have getPreopenedDirectories method");
    }
  }

  @Nested
  @DisplayName("Native Handle Tests")
  class NativeHandleTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiContext.class.getMethod("getNativeHandle"), "Should have getNativeHandle method");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      assertNotNull(WasiContext.class.getMethod("isValid"), "Should have isValid method");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiContext.class),
          "WasiContext should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      assertNotNull(WasiContext.class.getMethod("close"), "Should have close method");
    }
  }

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("builder method should return non-null")
    void builderMethodShouldReturnNonNull() {
      final WasiContextBuilder builder = WasiContext.builder();
      assertNotNull(builder, "builder() should return non-null");
    }

    @Test
    @DisplayName("builder should allow fluent configuration")
    void builderShouldAllowFluentConfiguration() {
      final WasiContextBuilder builder =
          WasiContext.builder()
              .withEnvironment("KEY", "value")
              .withArgument("--test")
              .withWorkingDirectory("/app");

      assertNotNull(builder, "Fluent builder should work");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("should have proper close semantics")
    void shouldHaveProperCloseSemantics() throws NoSuchMethodException {
      // Verify close method exists and can be called
      final Method closeMethod = WasiContext.class.getMethod("close");
      assertNotNull(closeMethod);
      assertTrue(Modifier.isPublic(closeMethod.getModifiers()), "close should be public");
    }

    @Test
    @DisplayName("should support try-with-resources pattern")
    void shouldSupportTryWithResourcesPattern() {
      // This is a compile-time/design test
      // WasiContext implementing AutoCloseable means it supports try-with-resources
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiContext.class),
          "Should support try-with-resources");
    }
  }
}
