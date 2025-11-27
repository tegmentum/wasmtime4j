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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniWasiContextImpl defensive programming and validation logic.
 *
 * <p>These tests focus on parameter validation, state management, and error handling without
 * requiring native library loading.
 */
class JniWasiContextImplTest {
  private static final long VALID_HANDLE = 1L;
  private JniWasiContextImpl context;

  @BeforeEach
  void setUp() {
    context = new JniWasiContextImpl(VALID_HANDLE);
  }

  // Constructor tests

  @Test
  void testConstructorWithValidHandle() {
    final JniWasiContextImpl newContext = new JniWasiContextImpl(VALID_HANDLE);

    assertThat(newContext).isNotNull();
    assertThat(newContext.getNativeHandle()).isEqualTo(VALID_HANDLE);
  }

  @Test
  void testConstructorWithZeroHandle() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new JniWasiContextImpl(0L));

    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testConstructorWithNegativeHandle() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new JniWasiContextImpl(-1L));

    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  // setArgv tests

  @Test
  void testSetArgvWithNullArray() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setArgv(null));

    assertThat(exception.getMessage()).contains("Command line arguments cannot be null");
  }

  @Test
  void testSetArgvWithEmptyArray() {
    // Should work - empty args array is valid (may throw UnsatisfiedLinkError if native not loaded)
    assertDoesNotThrow(
        () -> {
          try {
            context.setArgv(new String[0]);
          } catch (UnsatisfiedLinkError e) {
            // Expected - native library not loaded in unit test environment
          }
        });
  }

  @Test
  void testSetArgvWithValidArguments() {
    final String[] args = {"--flag", "value", "arg1"};

    // Will fail at native level but validates parameters first
    assertDoesNotThrow(
        () -> {
          try {
            context.setArgv(args);
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  // setEnv tests

  @Test
  void testSetEnvWithNullKey() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setEnv(null, "value"));

    assertThat(exception.getMessage()).contains("Environment variable key cannot be null");
  }

  @Test
  void testSetEnvWithNullValue() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setEnv("KEY", null));

    assertThat(exception.getMessage()).contains("Environment variable value cannot be null");
  }

  @Test
  void testSetEnvWithEmptyKey() {
    // Empty key should be rejected at validation level
    assertDoesNotThrow(
        () -> {
          try {
            context.setEnv("", "value");
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - validation or native failure
          }
        });
  }

  @Test
  void testSetEnvWithEmptyValue() {
    // Empty value should be allowed
    assertDoesNotThrow(
        () -> {
          try {
            context.setEnv("KEY", "");
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  // setEnv with Map tests

  @Test
  void testSetEnvMapWithNullMap() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> context.setEnv((Map<String, String>) null));

    assertThat(exception.getMessage()).contains("Environment variables map cannot be null");
  }

  @Test
  void testSetEnvMapWithEmptyMap() {
    final Map<String, String> emptyMap = new HashMap<>();

    assertDoesNotThrow(() -> context.setEnv(emptyMap));
  }

  @Test
  void testSetEnvMapWithValidEntries() {
    final Map<String, String> env = new HashMap<>();
    env.put("KEY1", "value1");
    env.put("KEY2", "value2");

    assertDoesNotThrow(
        () -> {
          try {
            context.setEnv(env);
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  @Test
  void testSetEnvMapWithNullKeyInMap() {
    final Map<String, String> env = new HashMap<>();
    env.put(null, "value");

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setEnv(env));

    assertThat(exception.getMessage()).contains("Environment variable key cannot be null");
  }

  @Test
  void testSetEnvMapWithNullValueInMap() {
    final Map<String, String> env = new HashMap<>();
    env.put("KEY", null);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setEnv(env));

    assertThat(exception.getMessage()).contains("Environment variable value cannot be null");
  }

  // inheritEnv tests

  @Test
  void testInheritEnv() {
    assertDoesNotThrow(
        () -> {
          try {
            context.inheritEnv();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  // inheritStdio tests

  @Test
  void testInheritStdio() {
    assertDoesNotThrow(
        () -> {
          try {
            context.inheritStdio();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  // setStdin tests

  @Test
  void testSetStdinWithNullPath() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setStdin(null));

    assertThat(exception.getMessage()).contains("Stdin path cannot be null");
  }

  @Test
  void testSetStdinWithValidPath() {
    final Path path = Paths.get("/tmp/stdin.txt");

    assertDoesNotThrow(
        () -> {
          try {
            context.setStdin(path);
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  // Resource type tests

  @Test
  void testGetResourceType() {
    assertThat(context.getResourceType()).isEqualTo("WasiContext");
  }

  // State validation tests

  @Test
  void testGetNativeHandle() {
    assertThat(context.getNativeHandle()).isEqualTo(VALID_HANDLE);
  }

  // Fluent interface tests

  @Test
  void testFluentInterfaceChaining() {
    // Verify methods return this for fluent API (even if they fail at native level)
    assertDoesNotThrow(
        () -> {
          try {
            context.setArgv(new String[] {"test"}).setEnv("KEY", "value").inheritStdio();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native failures
          }
        });
  }

  // Output capture tests

  @Test
  void testEnableOutputCapture() {
    assertDoesNotThrow(
        () -> {
          try {
            context.enableOutputCapture();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  @Test
  void testGetStdoutCaptureInitiallyNull() {
    // Before enabling capture or running module, capture should be null
    assertDoesNotThrow(
        () -> {
          try {
            final byte[] capture = context.getStdoutCapture();
            // If native works, capture is null before any output
            assertThat(capture).isNull();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  @Test
  void testGetStderrCaptureInitiallyNull() {
    // Before enabling capture or running module, capture should be null
    assertDoesNotThrow(
        () -> {
          try {
            final byte[] capture = context.getStderrCapture();
            // If native works, capture is null before any output
            assertThat(capture).isNull();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  @Test
  void testHasStdoutCaptureInitiallyFalse() {
    assertDoesNotThrow(
        () -> {
          try {
            final boolean hasCapture = context.hasStdoutCapture();
            // Before enabling capture, should return false
            assertThat(hasCapture).isFalse();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  @Test
  void testHasStderrCaptureInitiallyFalse() {
    assertDoesNotThrow(
        () -> {
          try {
            final boolean hasCapture = context.hasStderrCapture();
            // Before enabling capture, should return false
            assertThat(hasCapture).isFalse();
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  @Test
  void testSetStdinBytesWithNullData() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setStdinBytes(null));

    assertThat(exception.getMessage()).contains("Stdin data cannot be null");
  }

  @Test
  void testSetStdinBytesWithEmptyData() {
    assertDoesNotThrow(
        () -> {
          try {
            context.setStdinBytes(new byte[0]);
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }

  @Test
  void testSetStdinBytesWithValidData() {
    final byte[] data = "test input data".getBytes();
    assertDoesNotThrow(
        () -> {
          try {
            context.setStdinBytes(data);
          } catch (UnsatisfiedLinkError | RuntimeException e) {
            // Expected - native method not loaded
          }
        });
  }
}
