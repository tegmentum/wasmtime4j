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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniWasiContextImpl parameter validation logic.
 *
 * <p>These tests focus on Java-side parameter validation that occurs before any native method is
 * called. Tests that require actual native execution are in the integration test suite
 * (WasiBackwardCompatibilityTest) which uses real native handles from proper object creation.
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

    assertThat(exception.getMessage()).contains("nativeHandle");
  }

  @Test
  void testConstructorWithNegativeHandle() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new JniWasiContextImpl(-1L));

    assertThat(exception.getMessage()).contains("nativeHandle");
  }

  // setArgv validation tests

  @Test
  void testSetArgvWithNullArray() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setArgv(null));

    assertThat(exception.getMessage()).contains("Command line arguments cannot be null");
  }

  // setEnv validation tests

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

  // setEnv with Map validation tests

  @Test
  void testSetEnvMapWithNullMap() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> context.setEnv((Map<String, String>) null));

    assertThat(exception.getMessage()).contains("Environment variables map cannot be null");
  }

  @Test
  void testSetEnvMapWithEmptyMap() {
    // Empty map short-circuits without calling native methods
    final Map<String, String> emptyMap = new HashMap<>();

    assertDoesNotThrow(() -> context.setEnv(emptyMap));
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

  // setStdin validation tests

  @Test
  void testSetStdinWithNullPath() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setStdin(null));

    assertThat(exception.getMessage()).contains("Stdin path cannot be null");
  }

  // setStdinBytes validation tests

  @Test
  void testSetStdinBytesWithNullData() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setStdinBytes(null));

    assertThat(exception.getMessage()).contains("Stdin data cannot be null");
  }

  // Pure Java getter tests

  @Test
  void testGetResourceType() {
    assertThat(context.getResourceType()).isEqualTo("WasiContext");
  }

  @Test
  void testGetNativeHandle() {
    assertThat(context.getNativeHandle()).isEqualTo(VALID_HANDLE);
  }
}
