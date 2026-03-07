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

    assertThat(function.getNativeHandle()).isEqualTo(VALID_HANDLE);
    assertThat(function.getName()).isEqualTo(FUNCTION_NAME);
    assertThat(function.getResourceType()).isEqualTo("Function[" + FUNCTION_NAME + "]");
    assertFalse(function.isClosed());
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JniFunction(0L, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testConstructorWithNullName() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JniFunction(VALID_HANDLE, null, VALID_MODULE_HANDLE, MOCK_STORE));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testConstructorWithNullStore() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, null));

    assertThat(exception.getMessage()).contains("store");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testGetName() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);
    assertThat(function.getName()).isEqualTo(FUNCTION_NAME);
  }

  @Test
  void testGetResourceType() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);
    assertThat(function.getResourceType()).isEqualTo("Function[" + FUNCTION_NAME + "]");
  }

  @Test
  void testCallWithNullParameters() {
    final JniFunction function =
        new JniFunction(VALID_HANDLE, FUNCTION_NAME, VALID_MODULE_HANDLE, MOCK_STORE);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> function.call((WasmValue[]) null));

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
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

    assertThat(toString).contains("Function[" + FUNCTION_NAME + "]");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    // Note: Testing toString() after close() requires native methods
    // Integration tests will verify toString() behavior after close()
  }
}
