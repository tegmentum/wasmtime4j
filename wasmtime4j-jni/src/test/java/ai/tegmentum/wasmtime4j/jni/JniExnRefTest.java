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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ExnRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniExnRef} class.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and interface compliance without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly execution is tested in integration tests.
 */
@DisplayName("JniExnRef Tests")
class JniExnRefTest {

  private static final long VALID_HANDLE = 0x87654321L;
  private static final long VALID_STORE_HANDLE = 0x12345678L;
  private static final long VALID_ENGINE_HANDLE = 0xABCDEF01L;
  private static final JniEngine MOCK_ENGINE = new JniEngine(VALID_ENGINE_HANDLE);
  private static final JniStore MOCK_STORE = new JniStore(VALID_STORE_HANDLE, MOCK_ENGINE);

  @AfterAll
  static void tearDown() {
    MOCK_STORE.markClosedForTesting();
    MOCK_ENGINE.markClosedForTesting();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create JniExnRef with valid handle and store handle")
    void shouldCreateWithValidHandleAndStoreHandle() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      assertEquals(
          VALID_HANDLE,
          exnRef.getNativeHandle(),
          "Native handle should match the value passed to constructor");
      assertEquals(
          VALID_STORE_HANDLE,
          exnRef.getStoreHandle(),
          "Store handle should match the value passed to constructor");
      assertFalse(exnRef.isClosed(), "Newly created ExnRef should not be closed");
      assertEquals("ExnRef", exnRef.getResourceType(), "Resource type should be 'ExnRef'");
    }

    @Test
    @DisplayName("should reject zero native handle")
    void shouldRejectZeroNativeHandle() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniExnRef(0L, VALID_STORE_HANDLE),
              "Constructor should reject zero native handle");

      assertTrue(
          exception.getMessage().contains("nativeHandle"),
          "Exception message should mention 'nativeHandle', got: " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative native handle")
    void shouldRejectNegativeNativeHandle() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniExnRef(-1L, VALID_STORE_HANDLE),
              "Constructor should reject negative native handle");

      assertTrue(
          exception.getMessage().contains("nativeHandle"),
          "Exception message should mention 'nativeHandle', got: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("should implement ExnRef interface")
    void shouldImplementExnRefInterface() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      assertInstanceOf(ExnRef.class, exnRef, "JniExnRef should implement the ExnRef interface");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      assertInstanceOf(
          AutoCloseable.class, exnRef, "JniExnRef should implement AutoCloseable via JniResource");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("should mark as closed after close is called")
    void shouldMarkAsClosedAfterClose() throws Exception {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      assertFalse(exnRef.isClosed(), "ExnRef should not be closed before close() is called");

      exnRef.close();

      assertTrue(exnRef.isClosed(), "ExnRef should be closed after close() is called");
    }

    @Test
    @DisplayName("should handle double close gracefully")
    void shouldHandleDoubleCloseGracefully() throws Exception {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      exnRef.close();
      exnRef.close();

      assertTrue(exnRef.isClosed(), "ExnRef should remain closed after double close");
    }
  }

  @Nested
  @DisplayName("Parameter Validation Tests")
  class ParameterValidationTests {

    @Test
    @DisplayName("getTag should reject null store")
    void getTagShouldRejectNullStore() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> exnRef.getTag(null),
              "getTag should reject null store");

      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("field should reject null store")
    void fieldShouldRejectNullStore() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> exnRef.field(null, 0),
              "field should reject null store");

      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("field should reject negative index")
    void fieldShouldRejectNegativeIndex() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> exnRef.field(MOCK_STORE, -1),
              "field should reject negative index");

      assertTrue(
          exception.getMessage().contains("non-negative"),
          "Exception message should mention non-negative, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("fields should reject null store")
    void fieldsShouldRejectNullStore() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> exnRef.fields(null),
              "fields should reject null store");

      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("ty should reject null store")
    void tyShouldRejectNullStore() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> exnRef.ty(null), "ty should reject null store");

      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("toRaw should reject null store")
    void toRawShouldRejectNullStore() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> exnRef.toRaw(null),
              "toRaw should reject null store");

      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("matchesTy should reject null store")
    void matchesTyShouldRejectNullStore() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> exnRef.matchesTy(null, null),
              "matchesTy should reject null store");

      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null, got: " + exception.getMessage());
    }

    @Test
    @DisplayName("matchesTy should reject null heapType")
    void matchesTyShouldRejectNullHeapType() {
      final JniExnRef exnRef = new JniExnRef(VALID_HANDLE, VALID_STORE_HANDLE);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> exnRef.matchesTy(MOCK_STORE, null),
              "matchesTy should reject null heapType");

      assertTrue(
          exception.getMessage().contains("null"),
          "Exception message should mention null, got: " + exception.getMessage());
    }
  }
}
