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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiProcess} interface.
 *
 * <p>WasiProcess provides process management capabilities within the WASI sandbox.
 */
@DisplayName("WasiProcess Tests")
class WasiProcessTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiProcess.class.getModifiers()), "WasiProcess should be public");
      assertTrue(WasiProcess.class.isInterface(), "WasiProcess should be an interface");
    }
  }

  @Nested
  @DisplayName("Process Spawn Method Tests")
  class ProcessSpawnMethodTests {

    @Test
    @DisplayName("should have spawn method")
    void shouldHaveSpawnMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("spawn", WasiProcessConfig.class);
      assertNotNull(method, "spawn method should exist");
      assertEquals(WasiProcessId.class, method.getReturnType(), "Should return WasiProcessId");
    }

    @Test
    @DisplayName("should have waitFor method")
    void shouldHaveWaitForMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("waitFor", WasiProcessId.class);
      assertNotNull(method, "waitFor method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have kill method")
    void shouldHaveKillMethod() throws NoSuchMethodException {
      final Method method =
          WasiProcess.class.getMethod("kill", WasiProcessId.class, WasiSignal.class);
      assertNotNull(method, "kill method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("getEnvironment");
      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getEnvironmentVariable method")
    void shouldHaveGetEnvironmentVariableMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("getEnvironmentVariable", String.class);
      assertNotNull(method, "getEnvironmentVariable method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have setEnvironmentVariable method")
    void shouldHaveSetEnvironmentVariableMethod() throws NoSuchMethodException {
      final Method method =
          WasiProcess.class.getMethod("setEnvironmentVariable", String.class, String.class);
      assertNotNull(method, "setEnvironmentVariable method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Process ID Method Tests")
  class ProcessIdMethodTests {

    @Test
    @DisplayName("should have getCurrentProcessId method")
    void shouldHaveGetCurrentProcessIdMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("getCurrentProcessId");
      assertNotNull(method, "getCurrentProcessId method should exist");
      assertEquals(WasiProcessId.class, method.getReturnType(), "Should return WasiProcessId");
    }
  }

  @Nested
  @DisplayName("Working Directory Method Tests")
  class WorkingDirectoryMethodTests {

    @Test
    @DisplayName("should have getCurrentWorkingDirectory method")
    void shouldHaveGetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("getCurrentWorkingDirectory");
      assertNotNull(method, "getCurrentWorkingDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have setCurrentWorkingDirectory method")
    void shouldHaveSetCurrentWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("setCurrentWorkingDirectory", String.class);
      assertNotNull(method, "setCurrentWorkingDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Command Line Method Tests")
  class CommandLineMethodTests {

    @Test
    @DisplayName("should have getCommandLineArguments method")
    void shouldHaveGetCommandLineArgumentsMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("getCommandLineArguments");
      assertNotNull(method, "getCommandLineArguments method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Process Control Method Tests")
  class ProcessControlMethodTests {

    @Test
    @DisplayName("should have exit method")
    void shouldHaveExitMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("exit", int.class);
      assertNotNull(method, "exit method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have abort method")
    void shouldHaveAbortMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("abort");
      assertNotNull(method, "abort method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Process Status Method Tests")
  class ProcessStatusMethodTests {

    @Test
    @DisplayName("should have isProcessRunning method")
    void shouldHaveIsProcessRunningMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("isProcessRunning", WasiProcessId.class);
      assertNotNull(method, "isProcessRunning method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getProcessExitCode method")
    void shouldHaveGetProcessExitCodeMethod() throws NoSuchMethodException {
      final Method method = WasiProcess.class.getMethod("getProcessExitCode", WasiProcessId.class);
      assertNotNull(method, "getProcessExitCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }
}
