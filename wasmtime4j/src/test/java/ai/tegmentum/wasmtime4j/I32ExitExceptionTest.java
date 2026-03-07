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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.I32ExitException;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link I32ExitException}.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Exit code storage and retrieval
 *   <li>Success/failure distinction (exit code 0 = success)
 *   <li>Exception hierarchy (extends WasiException extends WasmException)
 *   <li>Message formatting
 *   <li>Cause chaining
 *   <li>Inherited WasiException fields (operation, category, retryable)
 * </ul>
 */
@DisplayName("I32ExitException")
class I32ExitExceptionTest {

  @Test
  @DisplayName("exit code 0 indicates success")
  void exitCodeZeroIsSuccess() {
    final I32ExitException e = new I32ExitException(0);

    assertEquals(0, e.getExitCode(), "Exit code should be 0");
    assertTrue(e.isSuccess(), "Exit code 0 should indicate success");
  }

  @Test
  @DisplayName("exit code 1 indicates failure")
  void exitCodeOneIsFailure() {
    final I32ExitException e = new I32ExitException(1);

    assertEquals(1, e.getExitCode(), "Exit code should be 1");
    assertFalse(e.isSuccess(), "Exit code 1 should indicate failure");
  }

  @Test
  @DisplayName("negative exit code indicates failure")
  void negativeExitCodeIsFailure() {
    final I32ExitException e = new I32ExitException(-1);

    assertEquals(-1, e.getExitCode(), "Exit code should be -1");
    assertFalse(e.isSuccess(), "Negative exit code should indicate failure");
  }

  @Test
  @DisplayName("large exit code is preserved")
  void largeExitCodePreserved() {
    final I32ExitException e = new I32ExitException(127);

    assertEquals(127, e.getExitCode(), "Exit code should be 127");
    assertFalse(e.isSuccess(), "Exit code 127 should indicate failure");
  }

  @Test
  @DisplayName("Integer.MAX_VALUE exit code is preserved")
  void maxIntExitCode() {
    final I32ExitException e = new I32ExitException(Integer.MAX_VALUE);

    assertEquals(Integer.MAX_VALUE, e.getExitCode(), "Exit code should be Integer.MAX_VALUE");
    assertFalse(e.isSuccess(), "Non-zero exit code should indicate failure");
  }

  @Test
  @DisplayName("Integer.MIN_VALUE exit code is preserved")
  void minIntExitCode() {
    final I32ExitException e = new I32ExitException(Integer.MIN_VALUE);

    assertEquals(Integer.MIN_VALUE, e.getExitCode(), "Exit code should be Integer.MIN_VALUE");
    assertFalse(e.isSuccess(), "Non-zero exit code should indicate failure");
  }

  @Test
  @DisplayName("message contains exit code")
  void messageContainsExitCode() {
    final I32ExitException e = new I32ExitException(42);

    assertNotNull(e.getMessage(), "Message should not be null");
    assertTrue(
        e.getMessage().contains("42"), "Message should contain the exit code: " + e.getMessage());
    assertTrue(
        e.getMessage().contains("proc_exit"),
        "Message should mention proc_exit: " + e.getMessage());
  }

  @Test
  @DisplayName("cause is propagated")
  void causeIsPropagated() {
    final RuntimeException cause = new RuntimeException("underlying");
    final I32ExitException e = new I32ExitException(1, cause);

    assertEquals(1, e.getExitCode(), "Exit code should be 1");
    assertSame(cause, e.getCause(), "Cause should be the same object");
    assertFalse(e.isSuccess(), "Exit code 1 should indicate failure");
  }

  @Test
  @DisplayName("cause constructor preserves exit code")
  void causeConstructorPreservesExitCode() {
    final I32ExitException e = new I32ExitException(0, new RuntimeException("test"));

    assertEquals(0, e.getExitCode(), "Exit code should be 0");
    assertTrue(e.isSuccess(), "Exit code 0 should indicate success even with cause");
  }

  @Test
  @DisplayName("extends WasiException")
  void extendsWasiException() {
    final I32ExitException e = new I32ExitException(1);

    assertInstanceOf(WasiException.class, e, "I32ExitException should be a WasiException");
  }

  @Test
  @DisplayName("extends WasmException")
  void extendsWasmException() {
    final I32ExitException e = new I32ExitException(1);

    assertInstanceOf(WasmException.class, e, "I32ExitException should be a WasmException");
  }

  @Test
  @DisplayName("operation is proc_exit")
  void operationIsProcExit() {
    final I32ExitException e = new I32ExitException(0);

    assertEquals("proc_exit", e.getOperation(), "Operation should be 'proc_exit'");
  }

  @Test
  @DisplayName("category is SYSTEM")
  void categoryIsSystem() {
    final I32ExitException e = new I32ExitException(0);

    assertEquals(WasiException.ErrorCategory.SYSTEM, e.getCategory(), "Category should be SYSTEM");
  }

  @Test
  @DisplayName("is not retryable")
  void isNotRetryable() {
    final I32ExitException e = new I32ExitException(0);

    assertFalse(e.isRetryable(), "I32ExitException should not be retryable");
  }

  @Test
  @DisplayName("resource is null")
  void resourceIsNull() {
    final I32ExitException e = new I32ExitException(0);

    assertNull(e.getResource(), "Resource should be null for proc_exit");
  }

  @Test
  @DisplayName("can be caught as WasiException")
  void canBeCaughtAsWasiException() {
    boolean caught = false;
    try {
      throw new I32ExitException(42);
    } catch (WasiException e) {
      caught = true;
      assertInstanceOf(I32ExitException.class, e, "Should be I32ExitException");
      assertEquals(42, ((I32ExitException) e).getExitCode(), "Should have exit code 42");
    }
    assertTrue(caught, "Should have been caught as WasiException");
  }

  @Test
  @DisplayName("can be caught as WasmException")
  void canBeCaughtAsWasmException() {
    boolean caught = false;
    try {
      throw new I32ExitException(1);
    } catch (WasmException e) {
      caught = true;
      assertInstanceOf(I32ExitException.class, e, "Should be I32ExitException");
    }
    assertTrue(caught, "Should have been caught as WasmException");
  }
}
