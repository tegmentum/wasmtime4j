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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for JniMemory.supports64BitAddressing() defensive behavior.
 *
 * <p>Verifies that when the native call fails (e.g., invalid handle), the method returns false and
 * logs a WARNING rather than silently swallowing the exception.
 */
@DisplayName("JniMemory supports64BitAddressing Tests")
class JniMemorySupports64BitTest {

  private static final long FAKE_HANDLE = 0xDEADBEEFL;

  private TestLogHandler logHandler;
  private Logger memoryLogger;

  @BeforeEach
  void setUp() {
    logHandler = new TestLogHandler();
    memoryLogger = Logger.getLogger(JniMemory.class.getName());
    memoryLogger.addHandler(logHandler);
  }

  @AfterEach
  void tearDown() {
    memoryLogger.removeHandler(logHandler);
  }

  @Test
  @DisplayName("should return false and log warning when native call fails with invalid handle")
  void shouldReturnFalseAndLogWarningOnNativeFailure() {
    // Create a JniMemory with a fake handle and null store.
    // ensureUsable() passes (handle is non-zero, store is null so no store check).
    // The native call will fail because the handle is not registered in the Rust
    // memory handle registry, triggering the catch block.
    final JniMemory memory = new JniMemory(FAKE_HANDLE, null);
    try {
      final boolean result = memory.supports64BitAddressing();

      assertFalse(
          result,
          "supports64BitAddressing should return false when native call fails");

      final String capturedLog = logHandler.getCapturedOutput();
      assertTrue(
          capturedLog.contains("Failed to query 64-bit addressing support"),
          "A WARNING log should be emitted when the native call fails");
    } finally {
      memory.markClosedForTesting();
    }
  }

  @Test
  @DisplayName("should return false without crashing JVM for completely invalid handle")
  void shouldNotCrashJvmForInvalidHandle() {
    // Use a different fake handle to confirm the defensive behavior is consistent
    final JniMemory memory = new JniMemory(0x1111L, null);
    try {
      final boolean result = memory.supports64BitAddressing();

      assertFalse(result, "Invalid handle should safely return false");
    } finally {
      memory.markClosedForTesting();
    }
  }

  /**
   * Test log handler that captures log output for assertion.
   *
   * <p>Uses the same pattern as PlatformDetectorSecurityTest.
   */
  private static final class TestLogHandler extends Handler {
    private final StringBuilder logOutput = new StringBuilder();

    TestLogHandler() {
      setFormatter(new SimpleFormatter());
    }

    @Override
    public void publish(final LogRecord record) {
      if (isLoggable(record)) {
        logOutput.append(getFormatter().format(record));
      }
    }

    @Override
    public void flush() {
      // No-op for in-memory handler
    }

    @Override
    public void close() throws SecurityException {
      // No-op for in-memory handler
    }

    String getCapturedOutput() {
      return logOutput.toString();
    }
  }
}
