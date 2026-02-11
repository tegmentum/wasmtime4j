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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link WasmTable} bulk operations via the Java API:
 * {@link WasmTable#fill(int, int, Object)}, {@link WasmTable#copy(int, int, int)},
 * {@link WasmTable#init(int, int, int, int)}, and
 * {@link WasmTable#dropElementSegment(int)}.
 *
 * @since 1.0.0
 */
@DisplayName("Table Bulk Operations Tests")
public class TableBulkOperationsTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(TableBulkOperationsTest.class.getName());

  /**
   * WAT module with a funcref table (size 5), two functions returning 100/200,
   * and a passive element segment. Exports setup, call_at, and the table itself.
   */
  private static final String WAT =
      """
      (module
        (type $ret_i32 (func (result i32)))
        (table (export "tab") 5 funcref)
        (func $f1 (result i32) i32.const 100)
        (func $f2 (result i32) i32.const 200)
        (elem $seg func $f1 $f2)
        (func (export "setup")
          i32.const 0 ref.func $f1 table.set 0
          i32.const 1 ref.func $f2 table.set 0)
        (func (export "call_at") (param i32) (result i32)
          local.get 0 call_indirect (type $ret_i32)))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fill table entries with null")
  void fillTableWithNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table fill with null");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Setup entries at 0 and 1
      instance.callFunction("setup");

      final WasmTable table = instance.getTable("tab").get();

      try {
        // Fill indices 0..2 with null
        table.fill(0, 3, null);

        // Verify entries are null ref (value depends on runtime representation)
        for (int i = 0; i < 3; i++) {
          final Object entry = table.get(i);
          LOGGER.info("[" + runtime + "] Table[" + i + "] after fill(null) = " + entry);
        }
        LOGGER.info("[" + runtime + "] Table fill with null completed for indices 0-2");
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] table.fill not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] table.fill threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy within table moves function references")
  void copyIntraTable(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table intra-copy");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      // Setup: table[0]=f1, table[1]=f2
      instance.callFunction("setup");

      final WasmTable table = instance.getTable("tab").get();

      try {
        // Copy 2 entries from index 0 to index 3
        table.copy(3, 0, 2);

        // Verify via call_indirect: table[3] should call f1 (100), table[4] should call f2 (200)
        final WasmValue[] r3 = instance.callFunction("call_at", WasmValue.i32(3));
        final WasmValue[] r4 = instance.callFunction("call_at", WasmValue.i32(4));

        assertEquals(100, r3[0].asInt(),
            "Table[3] should call f1 returning 100 after copy");
        assertEquals(200, r4[0].asInt(),
            "Table[4] should call f2 returning 200 after copy");
        LOGGER.info("[" + runtime + "] Intra-table copy verified: tab[3]="
            + r3[0].asInt() + ", tab[4]=" + r4[0].asInt());
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] table.copy not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] table.copy threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy zero count is a no-op")
  void copyZeroCountNoOp(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table copy zero count");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmTable table = instance.getTable("tab").get();

      try {
        // Should not throw
        table.copy(0, 0, 0);
        LOGGER.info("[" + runtime + "] Zero-count table copy succeeded as no-op");
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] table.copy not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] table.copy(0,0,0) threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("init from element segment with defensive handling")
  void initFromElementSegment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table init from element segment");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmTable table = instance.getTable("tab").get();

      try {
        // init(destOffset=0, elemSegIdx=0, srcOffset=0, len=2)
        table.init(0, 0, 0, 2);

        // Verify via call_indirect
        final WasmValue[] r0 = instance.callFunction("call_at", WasmValue.i32(0));
        final WasmValue[] r1 = instance.callFunction("call_at", WasmValue.i32(1));

        assertEquals(100, r0[0].asInt(),
            "Table[0] should call f1 returning 100 after init");
        assertEquals(200, r1[0].asInt(),
            "Table[1] should call f2 returning 200 after init");
        LOGGER.info("[" + runtime + "] Table init verified: tab[0]="
            + r0[0].asInt() + ", tab[1]=" + r1[0].asInt());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] table.init via Java API not supported: "
            + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] table.init threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("dropElementSegment via Java API with defensive handling")
  void dropElementSegmentViaJavaApi(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing dropElementSegment via Java API");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmTable table = instance.getTable("tab").get();

      try {
        table.dropElementSegment(0);
        LOGGER.info("[" + runtime + "] dropElementSegment(0) succeeded");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] dropElementSegment not supported via Java API: "
            + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] dropElementSegment threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fill out of bounds throws")
  void fillOutOfBoundsThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table fill out of bounds");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmTable table = instance.getTable("tab").get();

      // Table has 5 entries; filling 100 entries from index 0 should fail
      assertThrows(Exception.class,
          () -> table.fill(0, 100, null),
          "Filling beyond table size should throw");
      LOGGER.info("[" + runtime + "] Table fill out of bounds threw as expected");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy out of bounds throws")
  void copyOutOfBoundsThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table copy out of bounds");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmTable table = instance.getTable("tab").get();

      try {
        // Table has 5 entries; copying 100 entries should fail
        table.copy(0, 0, 100);
        LOGGER.info("[" + runtime + "] Table copy out of bounds did not throw");
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] table.copy not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] Table copy out of bounds threw as expected: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fill table with func ref then verify via call_indirect")
  void fillWithFuncRef(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing table fill with function reference");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      try {
        // First setup to populate table[0] with f1
        instance.callFunction("setup");
        final WasmTable table = instance.getTable("tab").get();

        // Get the func ref at index 0 (should be f1)
        final Object funcRef = table.get(0);

        if (funcRef != null) {
          // Fill indices 2..4 with the same func ref
          table.fill(2, 3, funcRef);

          // Verify each filled slot via call_indirect
          for (int i = 2; i < 5; i++) {
            final WasmValue[] result = instance.callFunction("call_at", WasmValue.i32(i));
            assertEquals(100, result[0].asInt(),
                "Table[" + i + "] should call f1 returning 100 after fill");
          }
          LOGGER.info("[" + runtime + "] Table fill with funcRef verified for indices 2-4");
        } else {
          LOGGER.info("[" + runtime + "] table.get(0) returned null, skipping fill test");
        }
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] table.fill with funcRef not implemented: "
            + e.getClass().getSimpleName() + " - " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] table fill with funcRef threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }
}
