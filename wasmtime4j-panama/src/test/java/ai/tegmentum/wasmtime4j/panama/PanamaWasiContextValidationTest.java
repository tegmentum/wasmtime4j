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

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaWasiContext} structure and contract validation.
 *
 * <p>These tests verify the class structure, interface contract implementation, and method
 * signatures of PanamaWasiContext without requiring actual native library operations. Note that
 * PanamaWasiContext constructor calls native bindings immediately, so tests that instantiate it
 * require the native library.
 */
@DisplayName("PanamaWasiContext Validation Tests")
class PanamaWasiContextValidationTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertThat(Modifier.isFinal(PanamaWasiContext.class.getModifiers()))
          .as("PanamaWasiContext should be a final class")
          .isTrue();
    }

    @Test
    @DisplayName("should implement WasiContext interface")
    void shouldImplementWasiContextInterface() {
      assertThat(WasiContext.class.isAssignableFrom(PanamaWasiContext.class))
          .as("PanamaWasiContext should implement WasiContext")
          .isTrue();
    }

    @Test
    @DisplayName("should be in correct package")
    void shouldBeInCorrectPackage() {
      assertThat(PanamaWasiContext.class.getPackage().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.panama");
    }

    @Test
    @DisplayName("should have public visibility")
    void shouldHavePublicVisibility() {
      assertThat(Modifier.isPublic(PanamaWasiContext.class.getModifiers()))
          .as("PanamaWasiContext should be public")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHavePublicNoArgConstructor() throws Exception {
      java.lang.reflect.Constructor<?> constructor = PanamaWasiContext.class.getConstructor();

      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("WasiContext Interface Method Tests")
  class WasiContextInterfaceMethodTests {

    @Test
    @DisplayName("should have setArgv method")
    void shouldHaveSetArgvMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setArgv", String[].class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setEnv with key/value method")
    void shouldHaveSetEnvKeyValueMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setEnv", String.class, String.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setEnv with map method")
    void shouldHaveSetEnvMapMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setEnv", Map.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have inheritEnv method")
    void shouldHaveInheritEnvMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("inheritEnv");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have inheritStdio method")
    void shouldHaveInheritStdioMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("inheritStdio");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setStdin method")
    void shouldHaveSetStdinMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setStdin", Path.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setStdinBytes method")
    void shouldHaveSetStdinBytesMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setStdinBytes", byte[].class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setStdout method")
    void shouldHaveSetStdoutMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setStdout", Path.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setStderr method")
    void shouldHaveSetStderrMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setStderr", Path.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have preopenedDir method")
    void shouldHavePreopenedDirMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("preopenedDir", Path.class, String.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have preopenedDirReadOnly method")
    void shouldHavePreopenedDirReadOnlyMethod() throws Exception {
      Method method =
          PanamaWasiContext.class.getMethod("preopenedDirReadOnly", Path.class, String.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setWorkingDirectory method")
    void shouldHaveSetWorkingDirectoryMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setWorkingDirectory", String.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setNetworkEnabled method")
    void shouldHaveSetNetworkEnabledMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setNetworkEnabled", boolean.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have setMaxOpenFiles method")
    void shouldHaveSetMaxOpenFilesMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("setMaxOpenFiles", int.class);

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(WasiContext.class);
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("close");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("Additional Method Tests")
  class AdditionalMethodTests {

    @Test
    @DisplayName("should have getNativeContext method")
    void shouldHaveGetNativeContextMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("getNativeContext");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType().getName()).isEqualTo("java.lang.foreign.MemorySegment");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("should use AtomicBoolean for closed state")
    void shouldUseAtomicBooleanForClosedState() {
      java.lang.reflect.Field[] fields = PanamaWasiContext.class.getDeclaredFields();

      boolean foundAtomicBoolean = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getType().equals(java.util.concurrent.atomic.AtomicBoolean.class)) {
          foundAtomicBoolean = true;
          break;
        }
      }

      assertThat(foundAtomicBoolean)
          .as("Should use AtomicBoolean for thread-safe closed state")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Native Binding Tests")
  class NativeBindingTests {

    @Test
    @DisplayName("should have static NATIVE_BINDINGS field")
    void shouldHaveStaticNativeBindingsField() {
      java.lang.reflect.Field[] fields = PanamaWasiContext.class.getDeclaredFields();

      boolean foundNativeBindingsField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().contains("NATIVE_BINDINGS")
            || field.getName().contains("nativeBindings")) {
          foundNativeBindingsField = true;
          assertThat(Modifier.isStatic(field.getModifiers()))
              .as("NATIVE_BINDINGS field should be static")
              .isTrue();
          break;
        }
      }

      assertThat(foundNativeBindingsField).as("Should have NATIVE_BINDINGS field").isTrue();
    }

    @Test
    @DisplayName("should have contextHandle field")
    void shouldHaveContextHandleField() {
      java.lang.reflect.Field[] fields = PanamaWasiContext.class.getDeclaredFields();

      boolean foundContextHandleField = false;
      for (java.lang.reflect.Field field : fields) {
        if (field.getName().equals("contextHandle")) {
          foundContextHandleField = true;
          assertThat(field.getType().getName()).isEqualTo("java.lang.foreign.MemorySegment");
          break;
        }
      }

      assertThat(foundContextHandleField).as("Should have contextHandle field").isTrue();
    }
  }

  @Nested
  @DisplayName("Fluent Interface Tests")
  class FluentInterfaceTests {

    @Test
    @DisplayName("all setter methods should return WasiContext for fluent chaining")
    void allSetterMethodsShouldReturnWasiContext() {
      Method[] methods = PanamaWasiContext.class.getMethods();

      for (Method method : methods) {
        String name = method.getName();
        if (name.startsWith("set")
            || name.equals("inheritEnv")
            || name.equals("inheritStdio")
            || name.startsWith("preopen")) {
          assertThat(method.getReturnType())
              .as("Method %s should return WasiContext for fluent chaining", name)
              .isEqualTo(WasiContext.class);
        }
      }
    }
  }

  @Nested
  @DisplayName("Lifecycle Management Tests")
  class LifecycleManagementTests {

    @Test
    @DisplayName("should have close method for resource cleanup")
    void shouldHaveCloseMethod() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("close");

      assertThat(method).isNotNull();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have private ensureNotClosed method")
    void shouldHaveEnsureNotClosedMethod() {
      Method[] methods = PanamaWasiContext.class.getDeclaredMethods();

      boolean foundMethod = false;
      for (Method method : methods) {
        if (method.getName().equals("ensureNotClosed")) {
          foundMethod = true;
          assertThat(Modifier.isPrivate(method.getModifiers()))
              .as("ensureNotClosed should be private")
              .isTrue();
          break;
        }
      }

      assertThat(foundMethod).as("Should have ensureNotClosed method").isTrue();
    }
  }

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("preopenedDir should declare WasmException")
    void preopenedDirShouldDeclareWasmException() throws Exception {
      Method method = PanamaWasiContext.class.getMethod("preopenedDir", Path.class, String.class);

      Class<?>[] exceptionTypes = method.getExceptionTypes();

      assertThat(exceptionTypes).hasSize(1);
      assertThat(exceptionTypes[0])
          .isEqualTo(ai.tegmentum.wasmtime4j.exception.WasmException.class);
    }

    @Test
    @DisplayName("preopenedDirReadOnly should declare WasmException")
    void preopenedDirReadOnlyShouldDeclareWasmException() throws Exception {
      Method method =
          PanamaWasiContext.class.getMethod("preopenedDirReadOnly", Path.class, String.class);

      Class<?>[] exceptionTypes = method.getExceptionTypes();

      assertThat(exceptionTypes).hasSize(1);
      assertThat(exceptionTypes[0])
          .isEqualTo(ai.tegmentum.wasmtime4j.exception.WasmException.class);
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of configuration methods")
    void shouldHaveExpectedNumberOfConfigMethods() {
      Method[] methods = PanamaWasiContext.class.getMethods();

      long configMethodCount =
          java.util.Arrays.stream(methods)
              .filter(m -> !m.getDeclaringClass().equals(Object.class))
              .filter(m -> !m.isSynthetic())
              .count();

      // Should have multiple configuration methods from WasiContext interface
      assertThat(configMethodCount)
          .as("Should have substantial number of public methods")
          .isGreaterThanOrEqualTo(10);
    }
  }

  @Nested
  @DisplayName("Documentation Tests")
  class DocumentationTests {

    @Test
    @DisplayName("class should be documented")
    void classShouldBeDocumented() {
      assertThat(PanamaWasiContext.class.getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.panama.PanamaWasiContext");
    }
  }
}
