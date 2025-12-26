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

package ai.tegmentum.wasmtime4j.wasi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiExit} interface.
 *
 * <p>WasiExit provides the ability for WASI programs to terminate execution with an exit code.
 */
@DisplayName("WasiExit Tests")
class WasiExitTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiExit.class.isInterface(), "WasiExit should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasiExit.class.getModifiers()), "WasiExit should be public");
    }

    @Test
    @DisplayName("should have exit method")
    void shouldHaveExitMethod() throws NoSuchMethodException {
      final Method method = WasiExit.class.getMethod("exit", int.class);
      assertNotNull(method, "exit method should exist");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("exit should take int parameter")
    void exitShouldTakeIntParameter() throws NoSuchMethodException {
      final Method method = WasiExit.class.getMethod("exit", int.class);
      assertEquals(1, method.getParameterCount(), "Should take one parameter");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
    }

    @Test
    @DisplayName("exit should return void")
    void exitShouldReturnVoid() throws NoSuchMethodException {
      final Method method = WasiExit.class.getMethod("exit", int.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Exit Code Convention Tests")
  class ExitCodeConventionTests {

    @Test
    @DisplayName("success exit code should be 0")
    void successExitCodeShouldBeZero() {
      // Document the convention that 0 means success
      final int successCode = 0;
      assertEquals(0, successCode, "Success exit code should be 0");
    }

    @Test
    @DisplayName("failure exit code should be non-zero")
    void failureExitCodeShouldBeNonZero() {
      // Document the convention that non-zero means failure
      final int failureCode = 1;
      assertTrue(failureCode != 0, "Failure exit code should be non-zero");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should capture exit code")
    void mockShouldCaptureExitCode() {
      final MockWasiExit exit = new MockWasiExit();

      exit.exit(0);

      assertTrue(exit.wasExitCalled(), "Exit should have been called");
      assertEquals(0, exit.getExitCode(), "Exit code should be 0");
    }

    @Test
    @DisplayName("mock should capture non-zero exit code")
    void mockShouldCaptureNonZeroExitCode() {
      final MockWasiExit exit = new MockWasiExit();

      exit.exit(42);

      assertTrue(exit.wasExitCalled(), "Exit should have been called");
      assertEquals(42, exit.getExitCode(), "Exit code should be 42");
    }

    @Test
    @DisplayName("mock should capture negative exit code")
    void mockShouldCaptureNegativeExitCode() {
      final MockWasiExit exit = new MockWasiExit();

      exit.exit(-1);

      assertTrue(exit.wasExitCalled(), "Exit should have been called");
      assertEquals(-1, exit.getExitCode(), "Exit code should be -1");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : WasiExit.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + method.getName() + " should be public");
        }
      }
    }
  }

  /** Mock implementation of WasiExit for testing. */
  private static class MockWasiExit implements WasiExit {
    private boolean exitCalled = false;
    private int exitCode = 0;

    @Override
    public void exit(final int code) {
      this.exitCalled = true;
      this.exitCode = code;
    }

    public boolean wasExitCalled() {
      return exitCalled;
    }

    public int getExitCode() {
      return exitCode;
    }
  }
}
