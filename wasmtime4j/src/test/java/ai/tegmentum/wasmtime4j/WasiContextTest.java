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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiContext interface.
 *
 * <p>WasiContext provides configuration for WASI (WebAssembly System Interface) functionality,
 * including command-line arguments, environment variables, file system access, and standard I/O
 * redirection. This test verifies the interface structure and API conformance.
 */
@DisplayName("WasiContext Interface Tests")
class WasiContextTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiContext.class.isInterface(), "WasiContext should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiContext.class.getModifiers()), "WasiContext should be public");
    }
  }

  // ========================================================================
  // Argument Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Argument Methods Tests")
  class ArgumentMethodsTests {

    @Test
    @DisplayName("should have setArgv method")
    void shouldHaveSetArgvMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setArgv", String[].class);
      assertNotNull(method, "setArgv method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setArgv should return WasiContext");
    }

    @Test
    @DisplayName("setArgv should accept String array")
    void setArgvShouldAcceptStringArray() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setArgv", String[].class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "setArgv should have 1 parameter");
      assertEquals(String[].class, paramTypes[0], "Parameter should be String[]");
    }
  }

  // ========================================================================
  // Environment Variable Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Environment Variable Methods Tests")
  class EnvironmentVariableMethodsTests {

    @Test
    @DisplayName("should have setEnv method with key-value")
    void shouldHaveSetEnvMethodWithKeyValue() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setEnv", String.class, String.class);
      assertNotNull(method, "setEnv(String, String) method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setEnv should return WasiContext");
    }

    @Test
    @DisplayName("should have setEnv method with Map")
    void shouldHaveSetEnvMethodWithMap() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setEnv", Map.class);
      assertNotNull(method, "setEnv(Map) method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setEnv should return WasiContext");
    }

    @Test
    @DisplayName("should have inheritEnv method")
    void shouldHaveInheritEnvMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("inheritEnv");
      assertNotNull(method, "inheritEnv method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "inheritEnv should return WasiContext");
    }
  }

  // ========================================================================
  // Standard I/O Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Standard I/O Methods Tests")
  class StandardIoMethodsTests {

    @Test
    @DisplayName("should have inheritStdio method")
    void shouldHaveInheritStdioMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("inheritStdio");
      assertNotNull(method, "inheritStdio method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "inheritStdio should return WasiContext");
    }

    @Test
    @DisplayName("should have setStdin method with Path")
    void shouldHaveSetStdinMethodWithPath() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setStdin", Path.class);
      assertNotNull(method, "setStdin(Path) method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setStdin should return WasiContext");
    }

    @Test
    @DisplayName("should have setStdinBytes method")
    void shouldHaveSetStdinBytesMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setStdinBytes", byte[].class);
      assertNotNull(method, "setStdinBytes method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setStdinBytes should return WasiContext");
    }

    @Test
    @DisplayName("should have setStdout method")
    void shouldHaveSetStdoutMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setStdout", Path.class);
      assertNotNull(method, "setStdout method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setStdout should return WasiContext");
    }

    @Test
    @DisplayName("should have setStderr method")
    void shouldHaveSetStderrMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setStderr", Path.class);
      assertNotNull(method, "setStderr method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setStderr should return WasiContext");
    }
  }

  // ========================================================================
  // File System Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("File System Methods Tests")
  class FileSystemMethodsTests {

    @Test
    @DisplayName("should have preopenedDir method")
    void shouldHavePreopenedDirMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("preopenedDir", Path.class, String.class);
      assertNotNull(method, "preopenedDir method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "preopenedDir should return WasiContext");
    }

    @Test
    @DisplayName("should have preopenedDirReadOnly method")
    void shouldHavePreopenedDirReadOnlyMethod() throws NoSuchMethodException {
      final Method method =
          WasiContext.class.getMethod("preopenedDirReadOnly", Path.class, String.class);
      assertNotNull(method, "preopenedDirReadOnly method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "preopenedDirReadOnly should return WasiContext");
    }

    @Test
    @DisplayName("should have preopenedDirWithPermissions method")
    void shouldHavePreopenedDirWithPermissionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiContext.class.getMethod(
              "preopenedDirWithPermissions",
              Path.class,
              String.class,
              WasiDirectoryPermissions.class);
      assertNotNull(method, "preopenedDirWithPermissions method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "preopenedDirWithPermissions should return WasiContext");
    }

    @Test
    @DisplayName("should have setWorkingDirectory method")
    void shouldHaveSetWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setWorkingDirectory", String.class);
      assertNotNull(method, "setWorkingDirectory method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setWorkingDirectory should return WasiContext");
    }

    @Test
    @DisplayName("should have setFilesystemWorkingDir method")
    void shouldHaveSetFilesystemWorkingDirMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setFilesystemWorkingDir", Path.class);
      assertNotNull(method, "setFilesystemWorkingDir method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setFilesystemWorkingDir should return WasiContext");
    }
  }

  // ========================================================================
  // Network Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Network Methods Tests")
  class NetworkMethodsTests {

    @Test
    @DisplayName("should have setNetworkEnabled method")
    void shouldHaveSetNetworkEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setNetworkEnabled", boolean.class);
      assertNotNull(method, "setNetworkEnabled method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setNetworkEnabled should return WasiContext");
    }
  }

  // ========================================================================
  // Resource Limiting Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Limiting Methods Tests")
  class ResourceLimitingMethodsTests {

    @Test
    @DisplayName("should have setMaxOpenFiles method")
    void shouldHaveSetMaxOpenFilesMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setMaxOpenFiles", int.class);
      assertNotNull(method, "setMaxOpenFiles method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setMaxOpenFiles should return WasiContext");
    }

    @Test
    @DisplayName("should have setMaxAsyncOperations method")
    void shouldHaveSetMaxAsyncOperationsMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setMaxAsyncOperations", int.class);
      assertNotNull(method, "setMaxAsyncOperations method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setMaxAsyncOperations should return WasiContext");
    }
  }

  // ========================================================================
  // Async I/O Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Async I/O Methods Tests")
  class AsyncIoMethodsTests {

    @Test
    @DisplayName("should have setAsyncIoEnabled method")
    void shouldHaveSetAsyncIoEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setAsyncIoEnabled", boolean.class);
      assertNotNull(method, "setAsyncIoEnabled method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setAsyncIoEnabled should return WasiContext");
    }

    @Test
    @DisplayName("should have setAsyncTimeout method")
    void shouldHaveSetAsyncTimeoutMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setAsyncTimeout", long.class);
      assertNotNull(method, "setAsyncTimeout method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setAsyncTimeout should return WasiContext");
    }
  }

  // ========================================================================
  // Component Model Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Component Model Methods Tests")
  class ComponentModelMethodsTests {

    @Test
    @DisplayName("should have setComponentModelEnabled method")
    void shouldHaveSetComponentModelEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setComponentModelEnabled", boolean.class);
      assertNotNull(method, "setComponentModelEnabled method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setComponentModelEnabled should return WasiContext");
    }
  }

  // ========================================================================
  // Process Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Process Methods Tests")
  class ProcessMethodsTests {

    @Test
    @DisplayName("should have setProcessEnabled method")
    void shouldHaveSetProcessEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("setProcessEnabled", boolean.class);
      assertNotNull(method, "setProcessEnabled method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setProcessEnabled should return WasiContext");
    }
  }

  // ========================================================================
  // Output Capture Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Output Capture Methods Tests")
  class OutputCaptureMethodsTests {

    @Test
    @DisplayName("should have enableOutputCapture method")
    void shouldHaveEnableOutputCaptureMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("enableOutputCapture");
      assertNotNull(method, "enableOutputCapture method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "enableOutputCapture should return WasiContext");
    }

    @Test
    @DisplayName("should have getStdoutCapture method")
    void shouldHaveGetStdoutCaptureMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getStdoutCapture");
      assertNotNull(method, "getStdoutCapture method should exist");
      assertEquals(byte[].class, method.getReturnType(), "getStdoutCapture should return byte[]");
    }

    @Test
    @DisplayName("should have getStderrCapture method")
    void shouldHaveGetStderrCaptureMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("getStderrCapture");
      assertNotNull(method, "getStderrCapture method should exist");
      assertEquals(byte[].class, method.getReturnType(), "getStderrCapture should return byte[]");
    }

    @Test
    @DisplayName("should have hasStdoutCapture method")
    void shouldHaveHasStdoutCaptureMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("hasStdoutCapture");
      assertNotNull(method, "hasStdoutCapture method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasStdoutCapture should return boolean");
    }

    @Test
    @DisplayName("should have hasStderrCapture method")
    void shouldHaveHasStderrCaptureMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("hasStderrCapture");
      assertNotNull(method, "hasStderrCapture method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasStderrCapture should return boolean");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("create");
      assertNotNull(method, "static create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create method should be static");
      assertEquals(WasiContext.class, method.getReturnType(), "create should return WasiContext");
    }

    @Test
    @DisplayName("create method should have no parameters")
    void createMethodShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("create");
      assertEquals(0, method.getParameterCount(), "create method should have no parameters");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "setArgv",
              "setEnv",
              "inheritEnv",
              "inheritStdio",
              "setStdin",
              "setStdinBytes",
              "setStdout",
              "setStderr",
              "preopenedDir",
              "preopenedDirReadOnly",
              "preopenedDirWithPermissions",
              "setWorkingDirectory",
              "setFilesystemWorkingDir",
              "setNetworkEnabled",
              "setMaxOpenFiles",
              "setMaxAsyncOperations",
              "setAsyncIoEnabled",
              "setAsyncTimeout",
              "setComponentModelEnabled",
              "setProcessEnabled",
              "enableOutputCapture",
              "getStdoutCapture",
              "getStderrCapture",
              "hasStdoutCapture",
              "hasStderrCapture",
              "create");

      Set<String> actualMethods =
          Arrays.stream(WasiContext.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "WasiContext should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have correct number of static methods")
    void shouldHaveCorrectNumberOfStaticMethods() {
      long staticMethodCount =
          Arrays.stream(WasiContext.class.getDeclaredMethods())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();

      assertTrue(
          staticMethodCount >= 1,
          "WasiContext should have at least 1 static method (create), found: " + staticMethodCount);
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("preopenedDir should accept Path and String")
    void preopenedDirShouldAcceptPathAndString() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("preopenedDir", Path.class, String.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(2, paramTypes.length, "preopenedDir should have 2 parameters");
      assertEquals(Path.class, paramTypes[0], "First param should be Path (hostPath)");
      assertEquals(String.class, paramTypes[1], "Second param should be String (guestPath)");
    }

    @Test
    @DisplayName("preopenedDirWithPermissions should accept correct parameters")
    void preopenedDirWithPermissionsShouldAcceptCorrectParameters() throws NoSuchMethodException {
      final Method method =
          WasiContext.class.getMethod(
              "preopenedDirWithPermissions",
              Path.class,
              String.class,
              WasiDirectoryPermissions.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(3, paramTypes.length, "preopenedDirWithPermissions should have 3 parameters");
      assertEquals(Path.class, paramTypes[0], "First param should be Path");
      assertEquals(String.class, paramTypes[1], "Second param should be String");
      assertEquals(
          WasiDirectoryPermissions.class,
          paramTypes[2],
          "Third param should be WasiDirectoryPermissions");
    }
  }

  // ========================================================================
  // Fluent API Design Tests
  // ========================================================================

  @Nested
  @DisplayName("Fluent API Design Tests")
  class FluentApiDesignTests {

    @Test
    @DisplayName("all configuration methods should return WasiContext for method chaining")
    void allConfigurationMethodsShouldReturnWasiContextForMethodChaining() {
      Set<String> configMethods =
          Set.of(
              "setArgv",
              "setEnv",
              "inheritEnv",
              "inheritStdio",
              "setStdin",
              "setStdinBytes",
              "setStdout",
              "setStderr",
              "preopenedDir",
              "preopenedDirReadOnly",
              "preopenedDirWithPermissions",
              "setWorkingDirectory",
              "setFilesystemWorkingDir",
              "setNetworkEnabled",
              "setMaxOpenFiles",
              "setMaxAsyncOperations",
              "setAsyncIoEnabled",
              "setAsyncTimeout",
              "setComponentModelEnabled",
              "setProcessEnabled",
              "enableOutputCapture");

      for (Method method : WasiContext.class.getDeclaredMethods()) {
        if (configMethods.contains(method.getName())) {
          assertEquals(
              WasiContext.class,
              method.getReturnType(),
              method.getName() + " should return WasiContext for fluent API");
        }
      }
    }
  }

  // ========================================================================
  // Exception Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("preopenedDir should declare WasmException")
    void preopenedDirShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("preopenedDir", Path.class, String.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "preopenedDir should declare at least one exception");
    }

    @Test
    @DisplayName("preopenedDirReadOnly should declare WasmException")
    void preopenedDirReadOnlyShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          WasiContext.class.getMethod("preopenedDirReadOnly", Path.class, String.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          exceptionTypes.length > 0, "preopenedDirReadOnly should declare at least one exception");
    }

    @Test
    @DisplayName("preopenedDirWithPermissions should declare WasmException")
    void preopenedDirWithPermissionsShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method =
          WasiContext.class.getMethod(
              "preopenedDirWithPermissions",
              Path.class,
              String.class,
              WasiDirectoryPermissions.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          exceptionTypes.length > 0,
          "preopenedDirWithPermissions should declare at least one exception");
    }

    @Test
    @DisplayName("enableOutputCapture should declare WasmException")
    void enableOutputCaptureShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("enableOutputCapture");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          exceptionTypes.length > 0, "enableOutputCapture should declare at least one exception");
    }

    @Test
    @DisplayName("create should declare WasmException")
    void createShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = WasiContext.class.getMethod("create");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "create should declare at least one exception");
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getter methods should return appropriate types")
    void getterMethodsShouldReturnAppropriateTypes() throws NoSuchMethodException {
      final Method getStdoutCapture = WasiContext.class.getMethod("getStdoutCapture");
      final Method getStderrCapture = WasiContext.class.getMethod("getStderrCapture");
      final Method hasStdoutCapture = WasiContext.class.getMethod("hasStdoutCapture");
      final Method hasStderrCapture = WasiContext.class.getMethod("hasStderrCapture");

      assertEquals(
          byte[].class, getStdoutCapture.getReturnType(), "getStdoutCapture should return byte[]");
      assertEquals(
          byte[].class, getStderrCapture.getReturnType(), "getStderrCapture should return byte[]");
      assertEquals(
          boolean.class,
          hasStdoutCapture.getReturnType(),
          "hasStdoutCapture should return boolean");
      assertEquals(
          boolean.class,
          hasStderrCapture.getReturnType(),
          "hasStderrCapture should return boolean");
    }
  }
}
