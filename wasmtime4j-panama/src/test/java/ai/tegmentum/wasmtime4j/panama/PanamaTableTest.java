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
package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Panama-specific tests for {@link PanamaTable}.
 *
 * <p>Tests cover constructor validation, lifecycle management (close behavior), native pointer
 * access, and Panama-specific fill(long, Object, long) overload. Generic WasmTable API tests are in
 * {@code TableApiDualRuntimeTest} and {@code TableOperationsIntegrationTest}.
 */
@DisplayName("PanamaTable Tests")
class PanamaTableTest {
  private static final Logger LOGGER = Logger.getLogger(PanamaTableTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
    LOGGER.info("Native library loaded for PanamaTableTest");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  private PanamaEngine createEngine() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    resources.add(engine);
    return engine;
  }

  private PanamaStore createStore(final PanamaEngine engine) throws Exception {
    final PanamaStore store = new PanamaStore(engine);
    resources.add(store);
    return store;
  }

  private PanamaInstance createInstance(final PanamaModule module, final PanamaStore store)
      throws Exception {
    final PanamaInstance instance = new PanamaInstance(module, store);
    resources.add(instance);
    return instance;
  }

  /**
   * Creates a standard table test instance from the exports-test.wasm which has a funcref table
   * with 5 elements and no maximum.
   */
  private PanamaInstance createTableTestInstance() throws Exception {
    final PanamaEngine engine = createEngine();
    final PanamaStore store = createStore(engine);
    final byte[] wasmBytes = loadTestWasmBytes();
    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    resources.add(module);
    return createInstance(module, store);
  }

  private WasmTable getTable(final PanamaInstance instance) {
    final Optional<WasmTable> tableOpt = instance.getTable("table");
    assertTrue(tableOpt.isPresent(), "Table export 'table' should be present");
    return tableOpt.get();
  }

  private static byte[] loadTestWasmBytes() throws Exception {
    try (InputStream is =
        PanamaTableTest.class.getClassLoader().getResourceAsStream("wasm/exports-test.wasm")) {
      assertNotNull(is, "Test WASM file should exist in classpath");
      return is.readAllBytes();
    }
  }

  // ==================== Constructor Validation Tests ====================

  @Test
  @DisplayName("Constructor with instance: null native pointer should throw")
  void testConstructorWithInstanceNullNativePointer() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new PanamaTable(null, (PanamaInstance) null));
    assertThat(exception.getMessage()).contains("Native table pointer cannot be null");
  }

  @Test
  @DisplayName("Constructor with instance: MemorySegment.NULL should throw")
  void testConstructorWithInstanceNullMemorySegment() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PanamaTable(MemorySegment.NULL, (PanamaInstance) null));
    assertThat(exception.getMessage()).contains("Native table pointer cannot be null");
  }

  @Test
  @DisplayName("Constructor with store: null native pointer should throw")
  void testConstructorWithStoreNullNativePointer() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new PanamaTable(null, WasmValueType.FUNCREF, (PanamaStore) null));
    assertThat(exception.getMessage()).contains("Native table pointer cannot be null");
  }

  // ==================== Table Retrieval Tests ====================

  @Nested
  @DisplayName("Table Retrieval Tests")
  class TableRetrievalTests {

    @Test
    @DisplayName("getTable should return PanamaTable instance")
    void shouldReturnPanamaTableInstance() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final WasmTable table = getTable(instance);
      assertThat(table).isInstanceOf(PanamaTable.class);
    }
  }

  // ==================== Fill(long, Object, long) Tests ====================

  @Nested
  @DisplayName("Fill Long Overload Tests")
  class FillLongOverloadTests {

    @Test
    @DisplayName("fill(long, Object, long) overload should delegate correctly")
    void shouldDelegateFromLongOverload() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertDoesNotThrow(() -> table.fill(0L, null, 2L), "Long overload fill should not throw");
    }

    @Test
    @DisplayName("fill(long, Object, long) with negative dstIndex should throw")
    void shouldThrowForNegativeDstIndex() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(-1L, null, 1L),
          "Negative dstIndex should throw");
    }

    @Test
    @DisplayName("fill(long, Object, long) with negative length should throw")
    void shouldThrowForNegativeLength() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(0L, null, -1L),
          "Negative length should throw");
    }
  }

  // ==================== Lifecycle Tests ====================

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("close should not throw")
    void shouldCloseWithoutError() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      assertDoesNotThrow(table::close, "Closing table should not throw");
    }

    @Test
    @DisplayName("double close should not throw")
    void shouldHandleDoubleClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertDoesNotThrow(table::close, "Double close should not throw");
    }

    @Test
    @DisplayName("getSize after close should throw IllegalStateException")
    void shouldThrowOnGetSizeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          table::getSize,
          "getSize on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("getMaxSize after close should throw IllegalStateException")
    void shouldThrowOnGetMaxSizeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          table::getMaxSize,
          "getMaxSize on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("get after close should throw IllegalStateException")
    void shouldThrowOnGetAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.get(0),
          "get on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("set after close should throw IllegalStateException")
    void shouldThrowOnSetAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.set(0, null),
          "set on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("grow after close should throw IllegalStateException")
    void shouldThrowOnGrowAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.grow(1, null),
          "grow on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("fill after close should throw IllegalStateException")
    void shouldThrowOnFillAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.fill(0, 1, null),
          "fill on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("copy after close should throw IllegalStateException")
    void shouldThrowOnCopyAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          () -> table.copy(0, 0, 1),
          "copy on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("getTableType after close should throw IllegalStateException")
    void shouldThrowOnGetTableTypeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThrows(
          IllegalStateException.class,
          table::getTableType,
          "getTableType on closed table should throw IllegalStateException");
    }

    @Test
    @DisplayName("getType should work after close (cached field)")
    void shouldReturnTypeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      final WasmValueType typeBefore = table.getType();
      table.close();
      assertEquals(
          typeBefore, table.getType(), "getType should return cached value even after close");
    }

    @Test
    @DisplayName("getElementType should work after close (cached field)")
    void shouldReturnElementTypeAfterClose() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      final WasmValueType typeBefore = table.getElementType();
      table.close();
      assertEquals(
          typeBefore,
          table.getElementType(),
          "getElementType should return cached value even after close");
    }
  }

  // ==================== Native Pointer Tests ====================

  @Nested
  @DisplayName("Native Pointer Tests")
  class NativePointerTests {

    @Test
    @DisplayName("getNativeTable should return non-null pointer")
    void shouldReturnNonNullPointer() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      final MemorySegment nativePtr = table.getNativeTable();
      assertNotNull(nativePtr, "Native table pointer should not be null");
      assertThat(nativePtr).isNotEqualTo(MemorySegment.NULL);
      LOGGER.info("Native table pointer: " + nativePtr);
    }
  }

  // ==================== Supports 64-Bit Addressing Tests ====================

  @Nested
  @DisplayName("Supports 64-Bit Addressing Tests")
  class Supports64BitAddressingTests {

    @Test
    @DisplayName("supports64BitAddressing on closed table should return false")
    void shouldReturnFalseOnClosedTable() throws Exception {
      final PanamaInstance instance = createTableTestInstance();
      final PanamaTable table = (PanamaTable) getTable(instance);
      table.close();
      assertThat(table.supports64BitAddressing()).isFalse();
    }
  }
}
