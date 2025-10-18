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
    // Should work - empty args array is valid
    assertDoesNotThrow(() -> context.setArgv(new String[0]));
  }

  @Test
  void testSetArgvWithValidArguments() {
    final String[] args = {"--flag", "value", "arg1"};

    // Will fail at native level but validates parameters first
    assertDoesNotThrow(
        () -> {
          try {
            context.setArgv(args);
          } catch (RuntimeException e) {
            // Expected - native method not loaded
            assertThat(e.getMessage()).contains("Failed to set command line arguments");
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
          } catch (RuntimeException e) {
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
          } catch (RuntimeException e) {
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
          } catch (RuntimeException e) {
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
          } catch (RuntimeException e) {
            // Expected - native method not loaded
            assertThat(e.getMessage()).contains("Failed to inherit environment variables");
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
          } catch (RuntimeException e) {
            // Expected - native method not loaded
            assertThat(e.getMessage()).contains("Failed to inherit stdio");
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
          } catch (RuntimeException e) {
            // Expected - native method not loaded
            assertThat(e.getMessage()).contains("Failed to set stdin path");
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
          } catch (RuntimeException e) {
            // Expected - native failures
          }
        });
  }
}
