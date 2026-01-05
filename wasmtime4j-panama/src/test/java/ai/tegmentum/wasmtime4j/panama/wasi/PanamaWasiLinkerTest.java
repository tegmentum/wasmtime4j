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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiPermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiLinker} class.
 *
 * <p>PanamaWasiLinker is a Panama implementation of a WASI-enabled linker that wraps a standard
 * Panama linker and adds WASI functionality.
 */
@DisplayName("PanamaWasiLinker Tests")
class PanamaWasiLinkerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiLinker.class.getModifiers()),
          "PanamaWasiLinker should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiLinker.class.getModifiers()),
          "PanamaWasiLinker should be final");
    }

    @Test
    @DisplayName("should implement WasiLinker interface")
    void shouldImplementWasiLinkerInterface() {
      assertTrue(
          WasiLinker.class.isAssignableFrom(PanamaWasiLinker.class),
          "PanamaWasiLinker should implement WasiLinker");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with PanamaLinker, PanamaEngine, and WasiConfig")
    void shouldHaveConstructorWithParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiLinker.class.getConstructor(
              ai.tegmentum.wasmtime4j.panama.PanamaLinker.class,
              ai.tegmentum.wasmtime4j.panama.PanamaEngine.class,
              WasiConfig.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Directory Access Method Tests")
  class DirectoryAccessMethodTests {

    @Test
    @DisplayName("should have allowDirectoryAccess method with permissions")
    void shouldHaveAllowDirectoryAccessWithPermissions() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod(
              "allowDirectoryAccess", Path.class, String.class, WasiPermissions.class);
      assertNotNull(method, "allowDirectoryAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have allowDirectoryAccess method without permissions")
    void shouldHaveAllowDirectoryAccessWithoutPermissions() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod("allowDirectoryAccess", Path.class, String.class);
      assertNotNull(method, "allowDirectoryAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("should have setEnvironmentVariable method")
    void shouldHaveSetEnvironmentVariable() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod("setEnvironmentVariable", String.class, String.class);
      assertNotNull(method, "setEnvironmentVariable method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setEnvironmentVariables method")
    void shouldHaveSetEnvironmentVariables() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("setEnvironmentVariables", Map.class);
      assertNotNull(method, "setEnvironmentVariables method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have inheritEnvironment method")
    void shouldHaveInheritEnvironment() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("inheritEnvironment");
      assertNotNull(method, "inheritEnvironment method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have inheritEnvironmentVariables method")
    void shouldHaveInheritEnvironmentVariables() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod("inheritEnvironmentVariables", List.class);
      assertNotNull(method, "inheritEnvironmentVariables method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Arguments Method Tests")
  class ArgumentsMethodTests {

    @Test
    @DisplayName("should have setArguments method")
    void shouldHaveSetArguments() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("setArguments", List.class);
      assertNotNull(method, "setArguments method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Stdio Configuration Method Tests")
  class StdioConfigMethodTests {

    @Test
    @DisplayName("should have configureStdin method")
    void shouldHaveConfigureStdin() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod("configureStdin", WasiStdioConfig.class);
      assertNotNull(method, "configureStdin method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have configureStdout method")
    void shouldHaveConfigureStdout() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod("configureStdout", WasiStdioConfig.class);
      assertNotNull(method, "configureStdout method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have configureStderr method")
    void shouldHaveConfigureStderr() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod("configureStderr", WasiStdioConfig.class);
      assertNotNull(method, "configureStderr method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Network Access Method Tests")
  class NetworkAccessMethodTests {

    @Test
    @DisplayName("should have enableNetworkAccess method")
    void shouldHaveEnableNetworkAccess() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("enableNetworkAccess");
      assertNotNull(method, "enableNetworkAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have disableNetworkAccess method")
    void shouldHaveDisableNetworkAccess() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("disableNetworkAccess");
      assertNotNull(method, "disableNetworkAccess method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Resource Limits Method Tests")
  class ResourceLimitsMethodTests {

    @Test
    @DisplayName("should have setMaxFileSize method")
    void shouldHaveSetMaxFileSize() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("setMaxFileSize", Long.class);
      assertNotNull(method, "setMaxFileSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setMaxOpenFiles method")
    void shouldHaveSetMaxOpenFiles() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("setMaxOpenFiles", Integer.class);
      assertNotNull(method, "setMaxOpenFiles method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Instantiate Method Tests")
  class InstantiateMethodTests {

    @Test
    @DisplayName("should have instantiate method")
    void shouldHaveInstantiate() throws NoSuchMethodException {
      final Method method =
          PanamaWasiLinker.class.getMethod("instantiate", Store.class, Module.class);
      assertNotNull(method, "instantiate method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getLinker method")
    void shouldHaveGetLinker() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("getLinker");
      assertNotNull(method, "getLinker method should exist");
      assertEquals(Linker.class, method.getReturnType(), "Should return Linker");
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngine() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfig() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(WasiConfig.class, method.getReturnType(), "Should return WasiConfig");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValid() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveClose() throws NoSuchMethodException {
      final Method method = PanamaWasiLinker.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
