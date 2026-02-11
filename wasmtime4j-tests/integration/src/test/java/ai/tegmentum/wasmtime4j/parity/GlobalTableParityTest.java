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

package ai.tegmentum.wasmtime4j.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cross-runtime parity tests for Global and Table operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * global variable creation, mutation, and table operations.
 */
@DisplayName("Global and Table Parity Tests")
@Tag("integration")
class GlobalTableParityTest {

  private static final Logger LOGGER = Logger.getLogger(GlobalTableParityTest.class.getName());

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private Store jniStore;
  private Store panamaStore;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for global/table parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up parity test runtimes");

    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniStore = jniEngine.createStore();
        jniCreatedSuccessfully = true;
        LOGGER.info("JNI runtime created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI runtime: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaStore = panamaEngine.createStore();
        panamaCreatedSuccessfully = true;
        LOGGER.info("Panama runtime created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama runtime: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up parity test runtimes");

    closeResource(jniStore, "JNI store");
    closeResource(jniEngine, "JNI engine");
    closeResource(jniRuntime, "JNI runtime");
    closeResource(panamaStore, "Panama store");
    closeResource(panamaEngine, "Panama engine");
    closeResource(panamaRuntime, "Panama runtime");
  }

  private void closeResource(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.info(name + " closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimes() {
    assumeTrue(
        jniCreatedSuccessfully && panamaCreatedSuccessfully,
        "Both JNI and Panama runtimes required");
  }

  @Nested
  @DisplayName("Global Creation Parity Tests")
  class GlobalCreationParityTests {

    @Test
    @DisplayName("should create mutable i32 globals identically on both runtimes")
    void shouldCreateMutableI32GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final int initialValue = 42;

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(initialValue));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(initialValue));

      LOGGER.info("Created JNI i32 global with value: " + jniGlobal.get().asI32());
      LOGGER.info("Created Panama i32 global with value: " + panamaGlobal.get().asI32());

      assertThat(jniGlobal.isMutable()).isEqualTo(panamaGlobal.isMutable());
      assertThat(jniGlobal.get().asI32()).isEqualTo(panamaGlobal.get().asI32());
      assertThat(jniGlobal.get().asI32()).isEqualTo(initialValue);
    }

    @Test
    @DisplayName("should create immutable i32 globals identically on both runtimes")
    void shouldCreateImmutableI32GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final int initialValue = 100;

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.I32, false, WasmValue.i32(initialValue));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.I32, false, WasmValue.i32(initialValue));

      LOGGER.info("Created JNI immutable i32 global with value: " + jniGlobal.get().asI32());
      LOGGER.info("Created Panama immutable i32 global with value: " + panamaGlobal.get().asI32());

      assertThat(jniGlobal.isMutable()).isFalse();
      assertThat(panamaGlobal.isMutable()).isFalse();
      assertThat(jniGlobal.get().asI32()).isEqualTo(panamaGlobal.get().asI32());
    }

    @Test
    @DisplayName("should create i64 globals identically on both runtimes")
    void shouldCreateI64GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final long initialValue = 9876543210L;

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.I64, true, WasmValue.i64(initialValue));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.I64, true, WasmValue.i64(initialValue));

      LOGGER.info("Created JNI i64 global with value: " + jniGlobal.get().asI64());
      LOGGER.info("Created Panama i64 global with value: " + panamaGlobal.get().asI64());

      assertThat(jniGlobal.get().asI64()).isEqualTo(panamaGlobal.get().asI64());
      assertThat(jniGlobal.get().asI64()).isEqualTo(initialValue);
    }

    @Test
    @DisplayName("should create f32 globals identically on both runtimes")
    void shouldCreateF32GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final float initialValue = 3.14159f;

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.F32, true, WasmValue.f32(initialValue));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.F32, true, WasmValue.f32(initialValue));

      LOGGER.info("Created JNI f32 global with value: " + jniGlobal.get().asF32());
      LOGGER.info("Created Panama f32 global with value: " + panamaGlobal.get().asF32());

      assertThat(jniGlobal.get().asF32()).isEqualTo(panamaGlobal.get().asF32());
      assertThat(jniGlobal.get().asF32()).isEqualTo(initialValue);
    }

    @Test
    @DisplayName("should create f64 globals identically on both runtimes")
    void shouldCreateF64GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final double initialValue = 2.718281828459045;

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.F64, true, WasmValue.f64(initialValue));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.F64, true, WasmValue.f64(initialValue));

      LOGGER.info("Created JNI f64 global with value: " + jniGlobal.get().asF64());
      LOGGER.info("Created Panama f64 global with value: " + panamaGlobal.get().asF64());

      assertThat(jniGlobal.get().asF64()).isEqualTo(panamaGlobal.get().asF64());
      assertThat(jniGlobal.get().asF64()).isEqualTo(initialValue);
    }
  }

  @Nested
  @DisplayName("Global Mutation Parity Tests")
  class GlobalMutationParityTests {

    @Test
    @DisplayName("should mutate i32 globals identically on both runtimes")
    void shouldMutateI32GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final WasmGlobal jniGlobal = jniStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));

      final int newValue = 999;
      jniGlobal.set(WasmValue.i32(newValue));
      panamaGlobal.set(WasmValue.i32(newValue));

      LOGGER.info("After mutation - JNI global: " + jniGlobal.get().asI32());
      LOGGER.info("After mutation - Panama global: " + panamaGlobal.get().asI32());

      assertThat(jniGlobal.get().asI32()).isEqualTo(panamaGlobal.get().asI32());
      assertThat(jniGlobal.get().asI32()).isEqualTo(newValue);
    }

    @Test
    @DisplayName("should mutate i64 globals identically on both runtimes")
    void shouldMutateI64GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.I64, true, WasmValue.i64(0L));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.I64, true, WasmValue.i64(0L));

      final long newValue = Long.MAX_VALUE;
      jniGlobal.set(WasmValue.i64(newValue));
      panamaGlobal.set(WasmValue.i64(newValue));

      LOGGER.info("After mutation - JNI global: " + jniGlobal.get().asI64());
      LOGGER.info("After mutation - Panama global: " + panamaGlobal.get().asI64());

      assertThat(jniGlobal.get().asI64()).isEqualTo(panamaGlobal.get().asI64());
      assertThat(jniGlobal.get().asI64()).isEqualTo(newValue);
    }

    @Test
    @DisplayName("should mutate f32 globals identically on both runtimes")
    void shouldMutateF32GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.F32, true, WasmValue.f32(0.0f));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.F32, true, WasmValue.f32(0.0f));

      final float newValue = 123.456f;
      jniGlobal.set(WasmValue.f32(newValue));
      panamaGlobal.set(WasmValue.f32(newValue));

      LOGGER.info("After mutation - JNI global: " + jniGlobal.get().asF32());
      LOGGER.info("After mutation - Panama global: " + panamaGlobal.get().asF32());

      assertThat(jniGlobal.get().asF32()).isEqualTo(panamaGlobal.get().asF32());
      assertThat(jniGlobal.get().asF32()).isEqualTo(newValue);
    }

    @Test
    @DisplayName("should mutate f64 globals identically on both runtimes")
    void shouldMutateF64GlobalsIdentically() throws Exception {
      requireBothRuntimes();

      final WasmGlobal jniGlobal =
          jniStore.createGlobal(WasmValueType.F64, true, WasmValue.f64(0.0));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.F64, true, WasmValue.f64(0.0));

      final double newValue = Double.MAX_VALUE;
      jniGlobal.set(WasmValue.f64(newValue));
      panamaGlobal.set(WasmValue.f64(newValue));

      LOGGER.info("After mutation - JNI global: " + jniGlobal.get().asF64());
      LOGGER.info("After mutation - Panama global: " + panamaGlobal.get().asF64());

      assertThat(jniGlobal.get().asF64()).isEqualTo(panamaGlobal.get().asF64());
      assertThat(jniGlobal.get().asF64()).isEqualTo(newValue);
    }

    @Test
    @DisplayName("should handle multiple mutations identically on both runtimes")
    void shouldHandleMultipleMutationsIdentically() throws Exception {
      requireBothRuntimes();

      final WasmGlobal jniGlobal = jniStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));
      final WasmGlobal panamaGlobal =
          panamaStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(0));

      for (int i = 0; i < 100; i++) {
        jniGlobal.set(WasmValue.i32(i));
        panamaGlobal.set(WasmValue.i32(i));
        assertThat(jniGlobal.get().asI32()).isEqualTo(panamaGlobal.get().asI32());
      }

      LOGGER.info("Successfully performed 100 mutations with identical results");
    }
  }

  @Nested
  @DisplayName("Table Creation Parity Tests")
  class TableCreationParityTests {

    @Test
    @DisplayName("should create funcref tables identically on both runtimes")
    void shouldCreateFuncrefTablesIdentically() throws Exception {
      requireBothRuntimes();

      final int initialSize = 10;
      final int maxSize = 100;

      final WasmTable jniTable = jniStore.createTable(WasmValueType.FUNCREF, initialSize, maxSize);
      final WasmTable panamaTable =
          panamaStore.createTable(WasmValueType.FUNCREF, initialSize, maxSize);

      LOGGER.info("Created JNI funcref table with size: " + jniTable.getSize());
      LOGGER.info("Created Panama funcref table with size: " + panamaTable.getSize());

      assertThat(jniTable.getSize()).isEqualTo(panamaTable.getSize());
      assertThat(jniTable.getSize()).isEqualTo(initialSize);
    }

    @Test
    @DisplayName("should create externref tables identically on both runtimes")
    void shouldCreateExternrefTablesIdentically() throws Exception {
      requireBothRuntimes();

      final int initialSize = 5;
      final int maxSize = 50;

      final WasmTable jniTable =
          jniStore.createTable(WasmValueType.EXTERNREF, initialSize, maxSize);
      final WasmTable panamaTable =
          panamaStore.createTable(WasmValueType.EXTERNREF, initialSize, maxSize);

      LOGGER.info("Created JNI externref table with size: " + jniTable.getSize());
      LOGGER.info("Created Panama externref table with size: " + panamaTable.getSize());

      assertThat(jniTable.getSize()).isEqualTo(panamaTable.getSize());
      assertThat(jniTable.getSize()).isEqualTo(initialSize);
    }

    @Test
    @DisplayName("should create unbounded tables identically on both runtimes")
    void shouldCreateUnboundedTablesIdentically() throws Exception {
      requireBothRuntimes();

      final int initialSize = 5;
      final int maxSize = -1; // Unbounded

      final WasmTable jniTable = jniStore.createTable(WasmValueType.FUNCREF, initialSize, maxSize);
      final WasmTable panamaTable =
          panamaStore.createTable(WasmValueType.FUNCREF, initialSize, maxSize);

      LOGGER.info("Created JNI unbounded table with size: " + jniTable.getSize());
      LOGGER.info("Created Panama unbounded table with size: " + panamaTable.getSize());

      assertThat(jniTable.getSize()).isEqualTo(panamaTable.getSize());
      assertThat(jniTable.getSize()).isEqualTo(initialSize);
    }
  }

  @Nested
  @DisplayName("Table Growth Parity Tests")
  class TableGrowthParityTests {

    @Test
    @DisplayName("should grow tables identically on both runtimes")
    void shouldGrowTablesIdentically() throws Exception {
      requireBothRuntimes();

      final WasmTable jniTable = jniStore.createTable(WasmValueType.FUNCREF, 5, 100);
      final WasmTable panamaTable = panamaStore.createTable(WasmValueType.FUNCREF, 5, 100);

      final long jniOldSize = jniTable.grow(10, null);
      final long panamaOldSize = panamaTable.grow(10, null);

      LOGGER.info("JNI table old size: " + jniOldSize + ", new size: " + jniTable.getSize());
      LOGGER.info(
          "Panama table old size: " + panamaOldSize + ", new size: " + panamaTable.getSize());

      assertThat(jniOldSize).isEqualTo(panamaOldSize);
      assertThat(jniTable.getSize()).isEqualTo(panamaTable.getSize());
      assertThat(jniTable.getSize()).isEqualTo(15);
    }

    @Test
    @DisplayName("should handle growth limits identically on both runtimes")
    void shouldHandleGrowthLimitsIdentically() throws Exception {
      requireBothRuntimes();

      final WasmTable jniTable = jniStore.createTable(WasmValueType.FUNCREF, 5, 10);
      final WasmTable panamaTable = panamaStore.createTable(WasmValueType.FUNCREF, 5, 10);

      // Grow to max
      final long jniResult1 = jniTable.grow(5, null);
      final long panamaResult1 = panamaTable.grow(5, null);

      assertThat(jniResult1).isEqualTo(panamaResult1);
      assertThat(jniTable.getSize()).isEqualTo(panamaTable.getSize());

      LOGGER.info("Tables grown to max size: " + jniTable.getSize());

      // Try to grow beyond max (should fail)
      final long jniResult2 = jniTable.grow(1, null);
      final long panamaResult2 = panamaTable.grow(1, null);

      assertThat(jniResult2).isEqualTo(panamaResult2);
      assertThat(jniResult2).isEqualTo(-1); // Growth failure

      LOGGER.info("Growth beyond max returned: " + jniResult2 + " (expected -1)");
    }
  }

  @Nested
  @DisplayName("Multiple Globals Parity Tests")
  class MultipleGlobalsParityTests {

    @Test
    @DisplayName("should handle multiple globals identically on both runtimes")
    void shouldHandleMultipleGlobalsIdentically() throws Exception {
      requireBothRuntimes();

      // Create multiple globals of different types
      final WasmGlobal jniI32 = jniStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(10));
      final WasmGlobal jniI64 = jniStore.createGlobal(WasmValueType.I64, true, WasmValue.i64(20L));
      final WasmGlobal jniF32 =
          jniStore.createGlobal(WasmValueType.F32, true, WasmValue.f32(30.0f));
      final WasmGlobal jniF64 = jniStore.createGlobal(WasmValueType.F64, true, WasmValue.f64(40.0));

      final WasmGlobal panamaI32 =
          panamaStore.createGlobal(WasmValueType.I32, true, WasmValue.i32(10));
      final WasmGlobal panamaI64 =
          panamaStore.createGlobal(WasmValueType.I64, true, WasmValue.i64(20L));
      final WasmGlobal panamaF32 =
          panamaStore.createGlobal(WasmValueType.F32, true, WasmValue.f32(30.0f));
      final WasmGlobal panamaF64 =
          panamaStore.createGlobal(WasmValueType.F64, true, WasmValue.f64(40.0));

      assertThat(jniI32.get().asI32()).isEqualTo(panamaI32.get().asI32());
      assertThat(jniI64.get().asI64()).isEqualTo(panamaI64.get().asI64());
      assertThat(jniF32.get().asF32()).isEqualTo(panamaF32.get().asF32());
      assertThat(jniF64.get().asF64()).isEqualTo(panamaF64.get().asF64());

      LOGGER.info(
          "Successfully created and verified 4 globals of different types on both runtimes");
    }
  }
}
