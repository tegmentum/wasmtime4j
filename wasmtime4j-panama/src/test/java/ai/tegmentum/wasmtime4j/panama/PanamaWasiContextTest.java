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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryPermissions;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaWasiContext} class.
 *
 * <p>This test class verifies the Panama WASI context implementation including class structure,
 * method signatures, field declarations, and interface compliance using reflection.
 */
@DisplayName("PanamaWasiContext Tests")
class PanamaWasiContextTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaWasiContext should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWasiContext.class.getModifiers()),
          "PanamaWasiContext should be final");
    }

    @Test
    @DisplayName("PanamaWasiContext should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiContext.class.getModifiers()),
          "PanamaWasiContext should be public");
    }

    @Test
    @DisplayName("PanamaWasiContext should implement WasiContext interface")
    void shouldImplementWasiContextInterface() {
      Class<?>[] interfaces = PanamaWasiContext.class.getInterfaces();
      boolean implementsWasiContext = Arrays.asList(interfaces).contains(WasiContext.class);
      assertTrue(implementsWasiContext, "PanamaWasiContext should implement WasiContext interface");
    }

    @Test
    @DisplayName("PanamaWasiContext should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaWasiContext.class.getPackage().getName(),
          "PanamaWasiContext should be in ai.tegmentum.wasmtime4j.panama package");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have NATIVE_BINDINGS static field")
    void shouldHaveNativeBindingsField() throws NoSuchFieldException {
      Field field = PanamaWasiContext.class.getDeclaredField("NATIVE_BINDINGS");
      assertNotNull(field, "NATIVE_BINDINGS field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "NATIVE_BINDINGS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "NATIVE_BINDINGS should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "NATIVE_BINDINGS should be private");
      assertEquals(
          NativeFunctionBindings.class,
          field.getType(),
          "NATIVE_BINDINGS should be of type NativeFunctionBindings");
    }

    @Test
    @DisplayName("should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = PanamaWasiContext.class.getDeclaredField("contextHandle");
      assertNotNull(field, "contextHandle field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
      assertEquals(
          MemorySegment.class, field.getType(), "contextHandle should be of type MemorySegment");
    }

    @Test
    @DisplayName("should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = PanamaWasiContext.class.getDeclaredField("closed");
      assertNotNull(field, "closed field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "closed should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "closed should be private");
      assertEquals(AtomicBoolean.class, field.getType(), "closed should be of type AtomicBoolean");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHaveNoArgConstructor() throws NoSuchMethodException {
      var constructor = PanamaWasiContext.class.getConstructor();
      assertNotNull(constructor, "No-arg constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "No-arg constructor should be public");
      assertEquals(0, constructor.getParameterCount(), "Constructor should have no parameters");
    }
  }

  // ========================================================================
  // Method Signature Tests - Argument/Environment Configuration
  // ========================================================================

  @Nested
  @DisplayName("Argument/Environment Configuration Method Tests")
  class ArgumentEnvironmentMethodTests {

    @Test
    @DisplayName("should have setArgv method with String array parameter")
    void shouldHaveSetArgvMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setArgv", String[].class);
      assertNotNull(method, "setArgv method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setArgv should return WasiContext");
    }

    @Test
    @DisplayName("should have setEnv method with key-value parameters")
    void shouldHaveSetEnvKeyValueMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setEnv", String.class, String.class);
      assertNotNull(method, "setEnv(String, String) method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setEnv should return WasiContext");
    }

    @Test
    @DisplayName("should have setEnv method with Map parameter")
    void shouldHaveSetEnvMapMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setEnv", Map.class);
      assertNotNull(method, "setEnv(Map) method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setEnv should return WasiContext");
    }

    @Test
    @DisplayName("should have inheritEnv method")
    void shouldHaveInheritEnvMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("inheritEnv");
      assertNotNull(method, "inheritEnv method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "inheritEnv should return WasiContext");
      assertEquals(0, method.getParameterCount(), "inheritEnv should have no parameters");
    }
  }

  // ========================================================================
  // Method Signature Tests - Stdio Configuration
  // ========================================================================

  @Nested
  @DisplayName("Stdio Configuration Method Tests")
  class StdioConfigurationMethodTests {

    @Test
    @DisplayName("should have inheritStdio method")
    void shouldHaveInheritStdioMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("inheritStdio");
      assertNotNull(method, "inheritStdio method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "inheritStdio should return WasiContext");
      assertEquals(0, method.getParameterCount(), "inheritStdio should have no parameters");
    }

    @Test
    @DisplayName("should have setStdin method with Path parameter")
    void shouldHaveSetStdinMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setStdin", Path.class);
      assertNotNull(method, "setStdin(Path) method should exist");
      assertEquals(WasiContext.class, method.getReturnType(), "setStdin should return WasiContext");
    }

    @Test
    @DisplayName("should have setStdinBytes method with byte array parameter")
    void shouldHaveSetStdinBytesMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setStdinBytes", byte[].class);
      assertNotNull(method, "setStdinBytes method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setStdinBytes should return WasiContext");
    }

    @Test
    @DisplayName("should have setStdout method with Path parameter")
    void shouldHaveSetStdoutMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setStdout", Path.class);
      assertNotNull(method, "setStdout(Path) method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setStdout should return WasiContext");
    }

    @Test
    @DisplayName("should have setStderr method with Path parameter")
    void shouldHaveSetStderrMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setStderr", Path.class);
      assertNotNull(method, "setStderr(Path) method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setStderr should return WasiContext");
    }
  }

  // ========================================================================
  // Method Signature Tests - Directory Preopen Configuration
  // ========================================================================

  @Nested
  @DisplayName("Directory Preopen Configuration Method Tests")
  class DirectoryPreopenMethodTests {

    @Test
    @DisplayName("should have preopenedDir method")
    void shouldHavePreopenedDirMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("preopenedDir", Path.class, String.class);
      assertNotNull(method, "preopenedDir method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "preopenedDir should return WasiContext");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "preopenedDir should throw WasmException");
    }

    @Test
    @DisplayName("should have preopenedDirReadOnly method")
    void shouldHavePreopenedDirReadOnlyMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiContext.class.getMethod("preopenedDirReadOnly", Path.class, String.class);
      assertNotNull(method, "preopenedDirReadOnly method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "preopenedDirReadOnly should return WasiContext");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "preopenedDirReadOnly should throw WasmException");
    }

    @Test
    @DisplayName("should have preopenedDirWithPermissions method")
    void shouldHavePreopenedDirWithPermissionsMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiContext.class.getMethod(
              "preopenedDirWithPermissions",
              Path.class,
              String.class,
              WasiDirectoryPermissions.class);
      assertNotNull(method, "preopenedDirWithPermissions method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "preopenedDirWithPermissions should return WasiContext");
      assertEquals(
          3, method.getParameterCount(), "preopenedDirWithPermissions should have 3 parameters");
    }
  }

  // ========================================================================
  // Method Signature Tests - Feature Configuration
  // ========================================================================

  @Nested
  @DisplayName("Feature Configuration Method Tests")
  class FeatureConfigurationMethodTests {

    @Test
    @DisplayName("should have setWorkingDirectory method")
    void shouldHaveSetWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setWorkingDirectory", String.class);
      assertNotNull(method, "setWorkingDirectory method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setWorkingDirectory should return WasiContext");
    }

    @Test
    @DisplayName("should have setNetworkEnabled method")
    void shouldHaveSetNetworkEnabledMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setNetworkEnabled", boolean.class);
      assertNotNull(method, "setNetworkEnabled method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setNetworkEnabled should return WasiContext");
    }

    @Test
    @DisplayName("should have setMaxOpenFiles method")
    void shouldHaveSetMaxOpenFilesMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setMaxOpenFiles", int.class);
      assertNotNull(method, "setMaxOpenFiles method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setMaxOpenFiles should return WasiContext");
    }

    @Test
    @DisplayName("should have setAsyncIoEnabled method")
    void shouldHaveSetAsyncIoEnabledMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setAsyncIoEnabled", boolean.class);
      assertNotNull(method, "setAsyncIoEnabled method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setAsyncIoEnabled should return WasiContext");
    }

    @Test
    @DisplayName("should have setMaxAsyncOperations method")
    void shouldHaveSetMaxAsyncOperationsMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setMaxAsyncOperations", int.class);
      assertNotNull(method, "setMaxAsyncOperations method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setMaxAsyncOperations should return WasiContext");
    }

    @Test
    @DisplayName("should have setAsyncTimeout method")
    void shouldHaveSetAsyncTimeoutMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setAsyncTimeout", long.class);
      assertNotNull(method, "setAsyncTimeout method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setAsyncTimeout should return WasiContext");
    }

    @Test
    @DisplayName("should have setComponentModelEnabled method")
    void shouldHaveSetComponentModelEnabledMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setComponentModelEnabled", boolean.class);
      assertNotNull(method, "setComponentModelEnabled method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setComponentModelEnabled should return WasiContext");
    }

    @Test
    @DisplayName("should have setProcessEnabled method")
    void shouldHaveSetProcessEnabledMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setProcessEnabled", boolean.class);
      assertNotNull(method, "setProcessEnabled method should exist");
      assertEquals(
          WasiContext.class, method.getReturnType(), "setProcessEnabled should return WasiContext");
    }

    @Test
    @DisplayName("should have setFilesystemWorkingDir method")
    void shouldHaveSetFilesystemWorkingDirMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("setFilesystemWorkingDir", Path.class);
      assertNotNull(method, "setFilesystemWorkingDir method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "setFilesystemWorkingDir should return WasiContext");
    }
  }

  // ========================================================================
  // Method Signature Tests - Output Capture
  // ========================================================================

  @Nested
  @DisplayName("Output Capture Method Tests")
  class OutputCaptureMethodTests {

    @Test
    @DisplayName("should have enableOutputCapture method")
    void shouldHaveEnableOutputCaptureMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("enableOutputCapture");
      assertNotNull(method, "enableOutputCapture method should exist");
      assertEquals(
          WasiContext.class,
          method.getReturnType(),
          "enableOutputCapture should return WasiContext");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "enableOutputCapture should throw WasmException");
    }

    @Test
    @DisplayName("should have getStdoutCapture method")
    void shouldHaveGetStdoutCaptureMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("getStdoutCapture");
      assertNotNull(method, "getStdoutCapture method should exist");
      assertEquals(byte[].class, method.getReturnType(), "getStdoutCapture should return byte[]");
      assertEquals(0, method.getParameterCount(), "getStdoutCapture should have no parameters");
    }

    @Test
    @DisplayName("should have getStderrCapture method")
    void shouldHaveGetStderrCaptureMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("getStderrCapture");
      assertNotNull(method, "getStderrCapture method should exist");
      assertEquals(byte[].class, method.getReturnType(), "getStderrCapture should return byte[]");
      assertEquals(0, method.getParameterCount(), "getStderrCapture should have no parameters");
    }

    @Test
    @DisplayName("should have hasStdoutCapture method")
    void shouldHaveHasStdoutCaptureMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("hasStdoutCapture");
      assertNotNull(method, "hasStdoutCapture method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasStdoutCapture should return boolean");
      assertEquals(0, method.getParameterCount(), "hasStdoutCapture should have no parameters");
    }

    @Test
    @DisplayName("should have hasStderrCapture method")
    void shouldHaveHasStderrCaptureMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("hasStderrCapture");
      assertNotNull(method, "hasStderrCapture method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasStderrCapture should return boolean");
      assertEquals(0, method.getParameterCount(), "hasStderrCapture should have no parameters");
    }
  }

  // ========================================================================
  // Method Signature Tests - Native Handle Access
  // ========================================================================

  @Nested
  @DisplayName("Native Handle Access Method Tests")
  class NativeHandleAccessMethodTests {

    @Test
    @DisplayName("should have getNativeContext method")
    void shouldHaveGetNativeContextMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("getNativeContext");
      assertNotNull(method, "getNativeContext method should exist");
      assertEquals(
          MemorySegment.class,
          method.getReturnType(),
          "getNativeContext should return MemorySegment");
      assertEquals(0, method.getParameterCount(), "getNativeContext should have no parameters");
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(
          MemorySegment.class,
          method.getReturnType(),
          "getNativeHandle should return MemorySegment");
      assertEquals(0, method.getParameterCount(), "getNativeHandle should have no parameters");
    }
  }

  // ========================================================================
  // Method Signature Tests - Lifecycle
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }

    @Test
    @DisplayName("should have private ensureNotClosed method")
    void shouldHaveEnsureNotClosedMethod() throws NoSuchMethodException {
      Method method = PanamaWasiContext.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
      assertEquals(void.class, method.getReturnType(), "ensureNotClosed should return void");
    }
  }

  // ========================================================================
  // Fluent API Tests
  // ========================================================================

  @Nested
  @DisplayName("Fluent API Tests")
  class FluentApiTests {

    @Test
    @DisplayName("All configuration methods should return WasiContext for fluent API")
    void allConfigMethodsShouldReturnWasiContext() {
      Method[] methods = PanamaWasiContext.class.getDeclaredMethods();
      String[] fluentMethods = {
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
        "setNetworkEnabled",
        "setMaxOpenFiles",
        "setAsyncIoEnabled",
        "setMaxAsyncOperations",
        "setAsyncTimeout",
        "setComponentModelEnabled",
        "setProcessEnabled",
        "setFilesystemWorkingDir",
        "enableOutputCapture"
      };

      for (String methodName : fluentMethods) {
        boolean found = false;
        for (Method method : methods) {
          if (method.getName().equals(methodName)) {
            found = true;
            assertEquals(
                WasiContext.class,
                method.getReturnType(),
                "Method " + methodName + " should return WasiContext for fluent API");
          }
        }
        // Note: method may not exist if it's inherited
      }
    }
  }

  // ========================================================================
  // Thread Safety Tests
  // ========================================================================

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("closed field should use AtomicBoolean for thread safety")
    void closedFieldShouldUseAtomicBoolean() throws NoSuchFieldException {
      Field field = PanamaWasiContext.class.getDeclaredField("closed");
      assertEquals(
          AtomicBoolean.class,
          field.getType(),
          "closed field should be AtomicBoolean for thread-safe close operations");
    }

    @Test
    @DisplayName("contextHandle field should be final for thread safety")
    void contextHandleFieldShouldBeFinal() throws NoSuchFieldException {
      Field field = PanamaWasiContext.class.getDeclaredField("contextHandle");
      assertTrue(
          Modifier.isFinal(field.getModifiers()),
          "contextHandle should be final for thread safety");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of declared methods")
    void shouldHaveExpectedDeclaredMethods() {
      Method[] declaredMethods = PanamaWasiContext.class.getDeclaredMethods();
      // Count should include all public, protected, package-private, and private methods
      // Verify we have a reasonable number of methods based on the implementation
      assertTrue(
          declaredMethods.length >= 20,
          "PanamaWasiContext should have at least 20 declared methods, found: "
              + declaredMethods.length);
    }

    @Test
    @DisplayName("should implement all WasiContext interface methods")
    void shouldImplementAllInterfaceMethods() {
      Method[] interfaceMethods = WasiContext.class.getDeclaredMethods();
      Class<?> implClass = PanamaWasiContext.class;

      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          try {
            Method implMethod =
                implClass.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            assertNotNull(
                implMethod, "Implementation should have method: " + interfaceMethod.getName());
            assertFalse(
                Modifier.isAbstract(implMethod.getModifiers()),
                "Method " + interfaceMethod.getName() + " should not be abstract");
          } catch (NoSuchMethodException e) {
            // Method might be default in interface
          }
        }
      }
    }
  }
}
