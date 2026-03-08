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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Consolidated input validation tests for JNI implementation classes.
 *
 * <p>These tests exercise real pre-native-call validation code paths: null checks, zero/negative
 * handle rejection, negative value rejection, and type mismatch detection. All validation happens
 * in Java before any native call is made.
 *
 * <p>These tests use fake handles (which never reach native code) because the validation logic
 * under test runs entirely in Java. This is intentional — the goal is to verify that invalid inputs
 * are rejected before crossing the JNI boundary.
 */
@DisplayName("JNI Input Validation Tests")
class JniInputValidationTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
  }

  @AfterEach
  void tearDown() {
    // Mark fake-handle resources as closed to prevent GC-triggered native cleanup
    testStore.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  @Nested
  @DisplayName("Handle Validation")
  class HandleValidation {

    @Test
    @DisplayName("Zero handle should be rejected as null pointer")
    void zeroHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniEngine(0L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("null pointer"), "Expected message to contain: null pointer");
    }

    @Test
    @DisplayName("Negative handle should be rejected as invalid")
    void negativeHandleShouldBeRejected() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniEngine(-1L));
      assertTrue(
          e.getMessage().contains("nativeHandle"), "Expected message to contain: nativeHandle");
      assertTrue(
          e.getMessage().contains("invalid native handle"),
          "Expected message to contain: invalid native handle");
      assertTrue(
          e.getMessage().contains("negative value"), "Expected message to contain: negative value");
    }
  }

  @Nested
  @DisplayName("Constructor Null Parameter Rejection")
  class ConstructorNullParameterRejection {

    @Test
    @DisplayName("JniInstance should reject null module")
    void instanceShouldRejectNullModule() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> new JniInstance(VALID_HANDLE, null, testStore));
      assertTrue(e.getMessage().contains("module"), "Expected message to contain: module");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("JniInstance should reject null store")
    void instanceShouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniInstance(VALID_HANDLE, new JniModule(VALID_HANDLE, testEngine), null));
      assertTrue(e.getMessage().contains("store"), "Expected message to contain: store");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("JniTable should reject null store")
    void tableShouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniTable(VALID_HANDLE, null));
      assertTrue(e.getMessage().contains("store"), "Expected message to contain: store");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("JniGlobal should reject null store")
    void globalShouldRejectNullStore() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> new JniGlobal(VALID_HANDLE, null));
      assertTrue(e.getMessage().contains("store"), "Expected message to contain: store");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
    }

    @Test
    @DisplayName("JniCallbackRegistry should reject null store")
    void callbackRegistryShouldRejectNullStore() {
      NullPointerException e =
          assertThrows(NullPointerException.class, () -> new JniCallbackRegistry(null));
      assertTrue(
          e.getMessage().contains("Store cannot be null"),
          "Expected message to contain: Store cannot be null");
    }
  }

  @Nested
  @DisplayName("Method Null Parameter Rejection")
  class MethodNullParameterRejection {

    @Test
    @DisplayName("compileModule should reject null bytes")
    void compileModuleShouldRejectNullBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.compileModule(null));
      assertTrue(e.getMessage().contains("wasmBytes"), "Expected message to contain: wasmBytes");
      assertTrue(e.getMessage().contains("null"), "Expected message to contain: null");
    }

    @Test
    @DisplayName("compileModule should reject empty bytes")
    void compileModuleShouldRejectEmptyBytes() {
      final JniEngine engine = new JniEngine(VALID_HANDLE);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> engine.compileModule(new byte[0]));
      assertTrue(e.getMessage().contains("wasmBytes"), "Expected message to contain: wasmBytes");
      assertTrue(e.getMessage().contains("empty"), "Expected message to contain: empty");
    }

    @Test
    @DisplayName("getFunction should reject null name")
    void getFunctionShouldRejectNullName() {
      final JniModule testModule = new JniModule(VALID_HANDLE, testEngine);
      final JniInstance instance = new JniInstance(VALID_HANDLE, testModule, testStore);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> instance.getFunction(null));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
      instance.markClosedForTesting();
      testModule.markClosedForTesting();
    }

    @Test
    @DisplayName("getFunction should reject blank name")
    void getFunctionShouldRejectBlankName() {
      final JniModule testModule = new JniModule(VALID_HANDLE, testEngine);
      final JniInstance instance = new JniInstance(VALID_HANDLE, testModule, testStore);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> instance.getFunction("   "));
      assertTrue(e.getMessage().contains("name"), "Expected message to contain: name");
      assertTrue(
          e.getMessage().contains("must not be empty or whitespace-only"),
          "Expected message to contain: must not be empty or whitespace-only");
      instance.markClosedForTesting();
      testModule.markClosedForTesting();
    }

    @Test
    @DisplayName("readBytes should reject null buffer")
    void readBytesShouldRejectNullBuffer() {
      final JniMemory memory = new JniMemory(VALID_HANDLE, testStore);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> memory.readBytes(0L, null));
      assertTrue(e.getMessage().contains("buffer"), "Expected message to contain: buffer");
      assertTrue(
          e.getMessage().contains("must not be null"),
          "Expected message to contain: must not be null");
      memory.markClosedForTesting();
    }
  }

  @Nested
  @DisplayName("Negative Value Rejection")
  class NegativeValueRejection {

    @Test
    @DisplayName("addFuel should reject negative value")
    void addFuelShouldRejectNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.addFuel(-1));
      assertTrue(
          e.getMessage().contains("additionalFuel"), "Expected message to contain: additionalFuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("memory grow should reject negative pages")
    void memoryGrowShouldRejectNegativePages() {
      final JniMemory memory = new JniMemory(VALID_HANDLE, testStore);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> memory.grow(-1L));
      assertTrue(e.getMessage().contains("pages"), "Expected message to contain: pages");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
      memory.markClosedForTesting();
    }

    @Test
    @DisplayName("table get should reject negative index")
    void tableGetShouldRejectNegativeIndex() {
      final JniTable table = new JniTable(VALID_HANDLE, testStore);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> table.get(-1));
      assertTrue(e.getMessage().contains("index"), "Expected message to contain: index");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
      table.markClosedForTesting();
    }

    @Test
    @DisplayName("table fill should reject negative start")
    void tableFillShouldRejectNegativeStart() {
      final JniTable table = new JniTable(VALID_HANDLE, testStore);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> table.fill(-1, 1, null));
      assertTrue(e.getMessage().contains("start"), "Expected message to contain: start");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
      table.markClosedForTesting();
    }

    @Test
    @DisplayName("table fill should reject negative count")
    void tableFillShouldRejectNegativeCount() {
      final JniTable table = new JniTable(VALID_HANDLE, testStore);
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> table.fill(0, -1, null));
      assertTrue(e.getMessage().contains("count"), "Expected message to contain: count");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
      table.markClosedForTesting();
    }

    @Test
    @DisplayName("createMemory should reject negative initial pages")
    void createMemoryShouldRejectNegativeInitialPages() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.createMemory(-1, 10));
      assertTrue(
          e.getMessage().contains("Initial pages"), "Expected message to contain: Initial pages");
      assertTrue(e.getMessage().contains("negative"), "Expected message to contain: negative");
    }
  }

  @Nested
  @DisplayName("Invalid Range and Type Rejection")
  class InvalidRangeAndTypeRejection {

    @Test
    @DisplayName("createMemory should reject max pages less than initial")
    void createMemoryShouldRejectMaxLessThanInitial() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.createMemory(10, 5));
      assertTrue(e.getMessage().contains("Max pages"), "Expected message to contain: Max pages");
      assertTrue(
          e.getMessage().contains("cannot be less than initial pages"),
          "Expected message to contain: cannot be less than initial pages");
    }

    @Test
    @DisplayName("createTable should reject non-reference element type")
    void createTableShouldRejectInvalidElementType() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testStore.createTable(WasmValueType.I32, 10, 20));
      assertTrue(
          e.getMessage().contains("Element type"), "Expected message to contain: Element type");
      assertTrue(
          e.getMessage().contains("must be FUNCREF or EXTERNREF"),
          "Expected message to contain: must be FUNCREF or EXTERNREF");
    }

    @Test
    @DisplayName("createGlobal should reject mismatched value type")
    void createGlobalShouldRejectMismatchedType() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testStore.createGlobal(WasmValueType.I64, false, WasmValue.i32(42)));
      assertTrue(
          e.getMessage().contains("Initial value type"),
          "Expected message to contain: Initial value type");
      assertTrue(
          e.getMessage().contains("does not match global type"),
          "Expected message to contain: does not match global type");
    }
  }
}
