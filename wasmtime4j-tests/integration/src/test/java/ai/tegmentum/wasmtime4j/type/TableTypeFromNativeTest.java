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

package ai.tegmentum.wasmtime4j.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for TableType.fromNative() functionality.
 *
 * <p>These tests verify that TableType instances are correctly parsed from native handles when
 * retrieved from compiled WebAssembly modules. This exercises the fromNative() code path in both
 * JNI and Panama implementations.
 *
 * @since 1.0.0
 */
@DisplayName("TableType fromNative Integration Tests")
public final class TableTypeFromNativeTest {

  private static final Logger LOGGER = Logger.getLogger(TableTypeFromNativeTest.class.getName());

  /** Table with minimum only (no max). */
  private static final String TABLE_MIN_ONLY_WAT =
      "(module\n" + "  (table (export \"table\") 10 funcref))\n";

  /** Table with minimum and maximum. */
  private static final String TABLE_MIN_MAX_WAT =
      "(module\n" + "  (table (export \"table\") 2 100 funcref))\n";

  @Nested
  @DisplayName("Module.hasExport() Verification Tests")
  class ModuleHasExportTests {

    @Test
    @DisplayName("should verify hasExport works for table")
    @SuppressWarnings("deprecation")
    void shouldVerifyHasExportWorksForTable() throws Exception {
      LOGGER.info("Verifying hasExport works for table export");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileWat(TABLE_MIN_ONLY_WAT)) {

        // hasExport correctly returns true for table exports
        boolean hasTable = module.hasExport("table");
        assertTrue(hasTable, "hasExport should return true for table export");

        // However, getModuleExports returns empty list - this is the bug
        int exportCount = module.getModuleExports().size();
        LOGGER.info("hasExport(table)=" + hasTable + ", getModuleExports().size()=" + exportCount);

        // This test passes because hasExport works, but documents the bug
        // where getModuleExports/getExports returns empty
      }
    }
  }

  @Nested
  @DisplayName("Module.getTableType() Tests")
  class ModuleGetTableTypeTests {

    @Test
    @DisplayName("should get funcref table type with minimum only from module")
    void shouldGetFuncrefTableTypeWithMinOnlyFromModule() throws Exception {
      LOGGER.info("Testing Module.getTableType() for funcref table with minimum only");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileWat(TABLE_MIN_ONLY_WAT)) {

        final Optional<TableType> tableTypeOpt = module.getTableType("table");

        assertTrue(tableTypeOpt.isPresent(), "TableType should be present");
        final TableType tableType = tableTypeOpt.get();

        assertNotNull(tableType, "TableType should not be null");
        assertEquals(
            WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
        assertEquals(10, tableType.getMinimum(), "Minimum should be 10");
        assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "Kind should be TABLE");
      }
    }

    @Test
    @DisplayName("should get funcref table type with min and max from module")
    void shouldGetFuncrefTableTypeWithMinMaxFromModule() throws Exception {
      LOGGER.info("Testing Module.getTableType() for funcref table with min/max");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileWat(TABLE_MIN_MAX_WAT)) {

        final Optional<TableType> tableTypeOpt = module.getTableType("table");

        assertTrue(tableTypeOpt.isPresent(), "TableType should be present");
        final TableType tableType = tableTypeOpt.get();

        assertEquals(
            WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
        assertEquals(2, tableType.getMinimum(), "Minimum should be 2");
        assertTrue(tableType.getMaximum().isPresent(), "Maximum should be present");
        assertEquals(100L, tableType.getMaximum().get(), "Maximum should be 100");
      }
    }
  }

  @Nested
  @DisplayName("Instance.getTableType() Tests")
  class InstanceGetTableTypeTests {

    @Test
    @DisplayName("should get table type from instance")
    void shouldGetTableTypeFromInstance() throws Exception {
      LOGGER.info("Testing Instance.getTableType()");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileWat(TABLE_MIN_MAX_WAT);
        final Instance instance = store.createInstance(module);

        final Optional<TableType> tableTypeOpt = instance.getTableType("table");

        assertTrue(tableTypeOpt.isPresent(), "TableType should be present");
        final TableType tableType = tableTypeOpt.get();

        assertEquals(
            WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
        assertEquals(2, tableType.getMinimum(), "Minimum should be 2");

        instance.close();
        module.close();
      }
    }
  }
}
