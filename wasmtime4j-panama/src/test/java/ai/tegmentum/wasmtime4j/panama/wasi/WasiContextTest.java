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
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiContext} class.
 *
 * <p>WasiContext provides Panama FFI implementation of WASI context management with comprehensive
 * security and permission controls.
 */
@DisplayName("WasiContext Tests")
class WasiContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiContext.class.getModifiers()), "WasiContext should be public");
      assertTrue(Modifier.isFinal(WasiContext.class.getModifiers()), "WasiContext should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiContext.class),
          "WasiContext should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiContextBuilder.class, method.getReturnType(), "Should return WasiContextBuilder");
    }
  }

  @Nested
  @DisplayName("Handle Access Method Tests")
  class HandleAccessMethodTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }
  }

  @Nested
  @DisplayName("Security Method Tests")
  class SecurityMethodTests {

    @Test
    @DisplayName("should have getSecurityValidator method")
    void shouldHaveGetSecurityValidatorMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getSecurityValidator");
      assertNotNull(method, "getSecurityValidator method should exist");
      assertEquals(
          WasiSecurityValidator.class,
          method.getReturnType(),
          "Should return WasiSecurityValidator");
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("should have getEnvironmentVariable method")
    void shouldHaveGetEnvironmentVariableMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getEnvironmentVariable", String.class);
      assertNotNull(method, "getEnvironmentVariable method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getEnvironment");
      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Arguments Method Tests")
  class ArgumentsMethodTests {

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getArguments");
      assertNotNull(method, "getArguments method should exist");
      assertEquals(String[].class, method.getReturnType(), "Should return String[]");
    }
  }

  @Nested
  @DisplayName("Directory Method Tests")
  class DirectoryMethodTests {

    @Test
    @DisplayName("should have getPreopenedDirectories method")
    void shouldHaveGetPreopenedDirectoriesMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getPreopenedDirectories");
      assertNotNull(method, "getPreopenedDirectories method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getWorkingDirectory");
      assertNotNull(method, "getWorkingDirectory method should exist");
      assertEquals(Path.class, method.getReturnType(), "Should return Path");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validatePath method with string")
    void shouldHaveValidatePathMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("validatePath", String.class);
      assertNotNull(method, "validatePath(String) method should exist");
      assertEquals(Path.class, method.getReturnType(), "Should return Path");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
