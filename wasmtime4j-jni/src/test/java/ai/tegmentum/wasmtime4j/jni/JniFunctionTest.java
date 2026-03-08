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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniFunction}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly execution is tested in integration tests.
 */
class JniFunctionTest {

  private static final long VALID_HANDLE = 0x87654321L;
  private static final long VALID_STORE_HANDLE = 0x12345678L;
  private static final long VALID_ENGINE_HANDLE = 0xABCDEF01L;
  private static final long VALID_MODULE_HANDLE = 0x300000L;
  private static final String FUNCTION_NAME = "test_function";
  private static final JniEngine MOCK_ENGINE = new JniEngine(VALID_ENGINE_HANDLE);
  private static final JniStore MOCK_STORE = new JniStore(VALID_STORE_HANDLE, MOCK_ENGINE);

  @Test
  void testConstructorWithValidParameters() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);

    assertEquals(VALID_HANDLE, function.getNativeHandle());
    assertEquals(FUNCTION_NAME, function.getName());
    assertEquals("Function[" + FUNCTION_NAME + "]", function.getResourceType());
    assertFalse(function.isClosed());
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JniFunction(0L, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE));

    assertTrue(
        exception.getMessage().contains("nativeHandle"),
        "Expected message to contain: nativeHandle");
    assertTrue(
        exception.getMessage().contains("invalid native handle"),
        "Expected message to contain: invalid native handle");
  }

  @Test
  void testConstructorWithNullName() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JniFunction(VALID_HANDLE, null, VALID_MODULE_HANDLE, MOCK_STORE));

    assertTrue(exception.getMessage().contains("name"), "Expected message to contain: name");
    assertTrue(
        exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testConstructorWithNullStore() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, null));

    assertTrue(exception.getMessage().contains("store"), "Expected message to contain: store");
    assertTrue(
        exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testGetName() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);
    assertEquals(FUNCTION_NAME, function.getName());
  }

  @Test
  void testGetResourceType() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);
    assertEquals("Function[" + FUNCTION_NAME + "]", function.getResourceType());
  }

  @Test
  void testCallWithNullParameters() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> function.call((WasmValue[]) null));

    assertTrue(
        exception.getMessage().contains("parameters"), "Expected message to contain: parameters");
    assertTrue(
        exception.getMessage().contains("must not be null"),
        "Expected message to contain: must not be null");
  }

  @Test
  void testResourceManagement() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);
    assertFalse(function.isClosed());

    function.close();
    assertTrue(function.isClosed());
  }

  @Test
  void testOperationsOnClosedFunction() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);

    function.close();
    assertTrue(function.isClosed());
  }

  @Test
  void testToString() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);
    final String toString = function.toString();

    assertTrue(
        toString.contains("Function[" + FUNCTION_NAME + "]"),
        "Expected string to contain: Function[" + FUNCTION_NAME + "]");
    assertTrue(
        toString.contains("handle=0x" + Long.toHexString(VALID_HANDLE)),
        "Expected string to contain: handle=0x" + Long.toHexString(VALID_HANDLE));
    assertTrue(toString.contains("closed=false"), "Expected string to contain: closed=false");

    // Note: Testing toString() after close() requires native methods
    // Integration tests will verify toString() behavior after close()
  }
}
