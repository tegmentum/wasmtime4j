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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    assertNotNull(newContext);
    assertEquals(VALID_HANDLE, newContext.getNativeHandle());
  }

  @Test
  void testConstructorWithZeroHandle() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new JniWasiContextImpl(0L));

    assertTrue(
        exception.getMessage().contains("nativeHandle"),
        "Expected message to contain: nativeHandle");
  }

  @Test
  void testConstructorWithNegativeHandle() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new JniWasiContextImpl(-1L));

    assertTrue(
        exception.getMessage().contains("nativeHandle"),
        "Expected message to contain: nativeHandle");
  }

  // setArgv validation tests

  @Test
  void testSetArgvWithNullArray() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setArgv(null));

    assertTrue(
        exception.getMessage().contains("Command line arguments cannot be null"),
        "Expected message to contain: Command line arguments cannot be null");
  }

  // setEnv validation tests

  @Test
  void testSetEnvWithNullKey() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setEnv(null, "value"));

    assertTrue(
        exception.getMessage().contains("Environment variable key cannot be null"),
        "Expected message to contain: Environment variable key cannot be null");
  }

  @Test
  void testSetEnvWithNullValue() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setEnv("KEY", null));

    assertTrue(
        exception.getMessage().contains("Environment variable value cannot be null"),
        "Expected message to contain: Environment variable value cannot be null");
  }

  // setEnv with Map validation tests

  @Test
  void testSetEnvMapWithNullMap() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> context.setEnv((Map<String, String>) null));

    assertTrue(
        exception.getMessage().contains("Environment variables map cannot be null"),
        "Expected message to contain: Environment variables map cannot be null");
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

    assertTrue(
        exception.getMessage().contains("Environment variable key cannot be null"),
        "Expected message to contain: Environment variable key cannot be null");
  }

  @Test
  void testSetEnvMapWithNullValueInMap() {
    final Map<String, String> env = new HashMap<>();
    env.put("KEY", null);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setEnv(env));

    assertTrue(
        exception.getMessage().contains("Environment variable value cannot be null"),
        "Expected message to contain: Environment variable value cannot be null");
  }

  // setStdin validation tests

  @Test
  void testSetStdinWithNullPath() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setStdin(null));

    assertTrue(
        exception.getMessage().contains("Stdin path cannot be null"),
        "Expected message to contain: Stdin path cannot be null");
  }

  // setStdinBytes validation tests

  @Test
  void testSetStdinBytesWithNullData() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> context.setStdinBytes(null));

    assertTrue(
        exception.getMessage().contains("Stdin data cannot be null"),
        "Expected message to contain: Stdin data cannot be null");
  }

  // Pure Java getter tests

  @Test
  void testGetResourceType() {
    assertEquals("WasiContext", context.getResourceType());
  }

  @Test
  void testGetNativeHandle() {
    assertEquals(VALID_HANDLE, context.getNativeHandle());
  }
}
