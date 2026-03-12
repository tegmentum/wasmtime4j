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
package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link I32ExitException} class.
 *
 * <p>I32ExitException is thrown when a WASI module calls proc_exit.
 */
@DisplayName("I32ExitException Tests")
class I32ExitExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create exception with exit code 0")
    void shouldCreateExceptionWithExitCodeZero() {
      final I32ExitException exception = new I32ExitException(0);

      assertEquals(0, exception.getExitCode(), "Exit code should be 0");
      assertTrue(exception.isSuccess(), "Exit code 0 should indicate success");
      assertTrue(
          exception.getMessage().contains("0"),
          "Message should contain exit code. Got: " + exception.getMessage());
      assertEquals("proc_exit", exception.getOperation(), "Operation should be proc_exit");
      assertFalse(exception.isRetryable(), "Should not be retryable");
      assertEquals(
          WasiException.ErrorCategory.SYSTEM, exception.getCategory(), "Category should be SYSTEM");
    }

    @Test
    @DisplayName("should create exception with non-zero exit code")
    void shouldCreateExceptionWithNonZeroExitCode() {
      final I32ExitException exception = new I32ExitException(1);

      assertEquals(1, exception.getExitCode(), "Exit code should be 1");
      assertFalse(exception.isSuccess(), "Exit code 1 should not indicate success");
    }

    @Test
    @DisplayName("should create exception with negative exit code")
    void shouldCreateExceptionWithNegativeExitCode() {
      final I32ExitException exception = new I32ExitException(-1);

      assertEquals(-1, exception.getExitCode(), "Exit code should be -1");
      assertFalse(exception.isSuccess(), "Negative exit code should not indicate success");
    }

    @Test
    @DisplayName("should create exception with exit code and cause")
    void shouldCreateExceptionWithExitCodeAndCause() {
      final RuntimeException cause = new RuntimeException("underlying");
      final I32ExitException exception = new I32ExitException(42, cause);

      assertEquals(42, exception.getExitCode(), "Exit code should be 42");
      assertSame(cause, exception.getCause(), "Cause should be preserved");
      assertFalse(exception.isSuccess(), "Exit code 42 should not indicate success");
    }

    @Test
    @DisplayName("should create exception with exit code and null cause")
    void shouldCreateExceptionWithExitCodeAndNullCause() {
      final I32ExitException exception = new I32ExitException(0, null);

      assertEquals(0, exception.getExitCode(), "Exit code should be 0");
      assertNull(exception.getCause(), "Cause should be null");
      assertTrue(exception.isSuccess(), "Exit code 0 should indicate success");
    }
  }

  @Nested
  @DisplayName("isSuccess Tests")
  class IsSuccessTests {

    @Test
    @DisplayName("isSuccess should return true only for exit code 0")
    void isSuccessShouldReturnTrueOnlyForExitCodeZero() {
      assertTrue(new I32ExitException(0).isSuccess(), "Exit code 0 should be success");
      assertFalse(new I32ExitException(1).isSuccess(), "Exit code 1 should not be success");
      assertFalse(new I32ExitException(-1).isSuccess(), "Exit code -1 should not be success");
      assertFalse(
          new I32ExitException(Integer.MAX_VALUE).isSuccess(),
          "Max int exit code should not be success");
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should be instance of WasiException")
    void shouldBeInstanceOfWasiException() {
      final I32ExitException exception = new I32ExitException(0);
      assertTrue(exception instanceof WasiException, "Should be instance of WasiException");
    }

    @Test
    @DisplayName("should be instance of WasmException")
    void shouldBeInstanceOfWasmException() {
      final I32ExitException exception = new I32ExitException(0);
      assertTrue(exception instanceof WasmException, "Should be instance of WasmException");
    }

    @Test
    @DisplayName("message should contain proc_exit and exit code")
    void messageShouldContainProcExitAndExitCode() {
      final I32ExitException exception = new I32ExitException(127);
      final String message = exception.getMessage();

      assertNotNull(message, "Message should not be null");
      assertTrue(message.contains("proc_exit"), "Message should contain proc_exit");
      assertTrue(message.contains("127"), "Message should contain exit code");
    }
  }
}
