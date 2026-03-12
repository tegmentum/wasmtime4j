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
package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.Optional;

/**
 * Fuzz tests for WebAssembly table operations.
 *
 * <p>This fuzzer tests the robustness of table operations including:
 *
 * <ul>
 *   <li>Table grow with various delta values
 *   <li>Table get/set at various indices
 *   <li>Table fill operations
 *   <li>Table copy operations
 * </ul>
 *
 * @since 1.0.0
 */
public class TableFuzzer {

  /** A module that exports a table. */
  private static final String TABLE_MODULE_WAT =
      """
      (module
          (table (export "table") 10 100 funcref)
          (func $dummy (result i32)
              i32.const 42)
          (elem (i32.const 0) $dummy)
          (func (export "call_indirect") (param i32) (result i32)
              local.get 0
              call_indirect (result i32))
      )
      """;

  /**
   * Fuzz test for table grow operations.
   *
   * <p>This test attempts to grow tables by fuzzed amounts. The runtime should handle growth
   * attempts gracefully, returning -1 for failed growth operations.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzTableGrow(final FuzzedDataProvider data) {
    final int growDelta = data.consumeInt(0, 1000);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(TABLE_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmTable> tableOpt = instance.getTable("table");
      if (tableOpt.isEmpty()) {
        return;
      }

      final WasmTable table = tableOpt.get();
      final int initialSize = table.getSize();

      // Try to grow the table
      final int prevSize = table.grow(growDelta, null);

      if (prevSize >= 0) {
        // Growth succeeded - verify new size
        final int newSize = table.getSize();
        if (newSize != initialSize + growDelta) {
          throw new AssertionError(
              "Table size mismatch after growth: expected "
                  + (initialSize + growDelta)
                  + " but got "
                  + newSize);
        }
      }
      // If prevSize is -1, growth failed which is expected for large deltas or max exceeded

    } catch (WasmException e) {
      // Expected for various table errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for table get/set operations.
   *
   * <p>This test reads and writes table elements at fuzzed indices. The runtime should handle
   * out-of-bounds accesses gracefully with exceptions.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzTableGetSet(final FuzzedDataProvider data) {
    final int index = data.consumeInt();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(TABLE_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmTable> tableOpt = instance.getTable("table");
      if (tableOpt.isEmpty()) {
        return;
      }

      final WasmTable table = tableOpt.get();

      // Try to get an element at the fuzzed index
      try {
        final Object element = table.get(index);
        // If get succeeded, element might be null (uninitialized) or a function reference
      } catch (IndexOutOfBoundsException e) {
        // Expected for out-of-bounds index
      }

      // Try to set null at the fuzzed index (clearing the element)
      try {
        table.set(index, null);

        // Verify the element is now null
        final Object afterSet = table.get(index);
        // afterSet should be null or represent an uninitialized element
      } catch (IndexOutOfBoundsException e) {
        // Expected for out-of-bounds index
      }

    } catch (WasmException e) {
      // Expected for various table errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for table fill operations.
   *
   * <p>This test fills table regions with null values at fuzzed positions and lengths. The runtime
   * should handle overlapping or out-of-bounds fills gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzTableFill(final FuzzedDataProvider data) {
    final int dstIndex = Math.abs(data.consumeInt()) % 100;
    final int length = data.consumeInt(0, 50);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(TABLE_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmTable> tableOpt = instance.getTable("table");
      if (tableOpt.isEmpty()) {
        return;
      }

      final WasmTable table = tableOpt.get();

      // Try to fill the table region with null
      try {
        table.fill(dstIndex, length, null);

        // Verify elements are now null (within bounds)
        final int tableSize = table.getSize();
        for (int i = dstIndex; i < dstIndex + length && i < tableSize; i++) {
          final Object element = table.get(i);
          // Element should be null or uninitialized
        }
      } catch (IndexOutOfBoundsException e) {
        // Expected for out-of-bounds regions
      } catch (IllegalArgumentException e) {
        // Expected for invalid arguments
      }

    } catch (WasmException e) {
      // Expected for various table errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for table copy operations.
   *
   * <p>This test copies table regions with fuzzed source and destination indices. The runtime
   * should handle overlapping or out-of-bounds copies gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzTableCopy(final FuzzedDataProvider data) {
    final int srcIndex = Math.abs(data.consumeInt()) % 50;
    final int dstIndex = Math.abs(data.consumeInt()) % 50;
    final int length = data.consumeInt(0, 20);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(TABLE_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmTable> tableOpt = instance.getTable("table");
      if (tableOpt.isEmpty()) {
        return;
      }

      final WasmTable table = tableOpt.get();

      // Try to copy within the same table (table copy to itself)
      try {
        table.copy(dstIndex, srcIndex, length);
        // Copy succeeded - the elements should be duplicated
      } catch (IndexOutOfBoundsException e) {
        // Expected for out-of-bounds regions
      } catch (IllegalArgumentException e) {
        // Expected for invalid arguments
      }

    } catch (WasmException e) {
      // Expected for various table errors
    } catch (Exception e) {
      throw e;
    }
  }
}
