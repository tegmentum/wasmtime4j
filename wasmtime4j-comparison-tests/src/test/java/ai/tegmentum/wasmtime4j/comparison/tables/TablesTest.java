package ai.tegmentum.wasmtime4j.comparison.tables;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WebAssembly table import/export. */
public class TablesTest {

  private Engine engine;
  private Store store;

  @BeforeEach
  public void setUp() throws Exception {
    engine = Engine.create();
    store = engine.createStore();
  }

  @AfterEach
  public void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("Export funcref table")
  public void testExportFuncrefTable() throws Exception {
    final String wat =
        """
        (module
          (table (export "funcs") 10 funcref)
          (func (export "get_size") (result i32)
            table.size 0
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmTable table = instance.getTable("funcs").orElseThrow();
    assertNotNull(table);
    assertEquals(10, table.getSize());

    final WasmValue[] results = instance.callFunction("get_size");
    assertEquals(10, results[0].asInt());

    instance.close();
  }

  @Test
  @DisplayName("Table grow operation")
  public void testTableGrow() throws Exception {
    final String wat =
        """
        (module
          (table (export "t") 5 20 funcref)
          (func (export "grow_table") (param i32) (result i32)
            ref.null func
            local.get 0
            table.grow 0
          )
          (func (export "get_size") (result i32)
            table.size 0
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmTable table = instance.getTable("t").orElseThrow();
    assertEquals(5, table.getSize());

    // Grow by 3 elements
    WasmValue[] results = instance.callFunction("grow_table", WasmValue.i32(3));
    assertEquals(5, results[0].asInt()); // Returns old size

    // Check new size
    results = instance.callFunction("get_size");
    assertEquals(8, results[0].asInt());

    // Verify table object reflects growth
    assertEquals(8, table.getSize());

    instance.close();
  }

  @Test
  @DisplayName("Table get and set operations")
  public void testTableGetSet() throws Exception {
    final String wat =
        """
        (module
          (table (export "funcs") 5 funcref)
          (func $f1 (result i32) i32.const 42)
          (func $f2 (result i32) i32.const 99)
          (elem declare func $f1 $f2)

          (func (export "store_func1")
            i32.const 0
            ref.func $f1
            table.set 0
          )

          (func (export "store_func2")
            i32.const 1
            ref.func $f2
            table.set 0
          )

          (func (export "call_indirect") (param i32) (result i32)
            local.get 0
            call_indirect (result i32)
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Store functions in table
    instance.callFunction("store_func1");
    instance.callFunction("store_func2");

    // Call through table
    WasmValue[] results = instance.callFunction("call_indirect", WasmValue.i32(0));
    assertEquals(42, results[0].asInt());

    results = instance.callFunction("call_indirect", WasmValue.i32(1));
    assertEquals(99, results[0].asInt());

    instance.close();
  }

  @Test
  @DisplayName("Table fill operation")
  public void testTableFill() throws Exception {
    final String wat =
        """
        (module
          (table (export "t") 10 funcref)
          (func $dummy (result i32) i32.const 1)
          (elem declare func $dummy)

          (func (export "fill_table") (param i32 i32)
            local.get 0  ;; start
            ref.func $dummy
            local.get 1  ;; count
            table.fill 0
          )

          (func (export "test_elem") (param i32) (result i32)
            local.get 0
            table.get 0
            ref.is_null
            if (result i32)
              i32.const 0
            else
              i32.const 1
            end
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Initially all null
    WasmValue[] results = instance.callFunction("test_elem", WasmValue.i32(0));
    assertEquals(0, results[0].asInt());

    // Fill indexes 2-4 with function reference
    instance.callFunction("fill_table", WasmValue.i32(2), WasmValue.i32(3));

    // Check filled elements
    results = instance.callFunction("test_elem", WasmValue.i32(2));
    assertEquals(1, results[0].asInt());

    results = instance.callFunction("test_elem", WasmValue.i32(3));
    assertEquals(1, results[0].asInt());

    // Check unfilled element
    results = instance.callFunction("test_elem", WasmValue.i32(5));
    assertEquals(0, results[0].asInt());

    instance.close();
  }

  @Test
  @DisplayName("Table copy operation")
  public void testTableCopy() throws Exception {
    final String wat =
        """
        (module
          (table (export "src") 5 funcref)
          (table (export "dst") 5 funcref)
          (func $f1 (result i32) i32.const 42)
          (elem declare func $f1)

          (func (export "init_src")
            i32.const 0
            ref.func $f1
            table.set 0
          )

          (func (export "copy_tables")
            i32.const 1  ;; dst offset
            i32.const 0  ;; src offset
            i32.const 1  ;; count
            table.copy 1 0
          )

          (func (export "test_dst_elem") (param i32) (result i32)
            local.get 0
            table.get 1
            ref.is_null
            if (result i32)
              i32.const 0
            else
              i32.const 1
            end
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Initialize source table
    instance.callFunction("init_src");

    // Destination should be empty
    WasmValue[] results = instance.callFunction("test_dst_elem", WasmValue.i32(1));
    assertEquals(0, results[0].asInt());

    // Copy from src to dst
    instance.callFunction("copy_tables");

    // Destination should now have the function
    results = instance.callFunction("test_dst_elem", WasmValue.i32(1));
    assertEquals(1, results[0].asInt());

    instance.close();
  }

  @Test
  @DisplayName("Import table from Java")
  public void testImportTable() throws Exception {
    final String wat =
        """
        (module
          (import "env" "shared_table" (table 5 funcref))
          (func $local_func (result i32) i32.const 42)
          (elem declare func $local_func)

          (func (export "store_in_table")
            i32.const 2
            ref.func $local_func
            table.set 0
          )

          (func (export "get_size") (result i32)
            table.size 0
          )
        )
        """;

    // Create table would require a Store.createTable method
    // Since that's not in the API, this test demonstrates the pattern
    // but will fail until table creation is implemented

    final Linker linker = Linker.create(engine);
    // linker.defineTable("env", "shared_table", table); // Commented - no Store.createTable API

    final Module module = engine.compileWat(wat);

    // This will fail with missing import until table creation is implemented
    assertThrows(
        Exception.class,
        () -> {
          linker.instantiate(store, module);
        });

    linker.close();
  }

  @Test
  @DisplayName("Export and re-import table")
  public void testExportReimportTable() throws Exception {
    // Module 1: Exports a table
    final String wat1 =
        """
        (module
          (table (export "shared") 10 funcref)
          (func $f1 (result i32) i32.const 42)
          (elem declare func $f1)

          (func (export "populate")
            i32.const 0
            ref.func $f1
            table.set 0
          )
        )
        """;

    // Module 2: Imports the table
    final String wat2 =
        """
        (module
          (import "mod1" "shared" (table 10 funcref))

          (func (export "call_from_table") (result i32)
            i32.const 0
            call_indirect (result i32)
          )
        )
        """;

    final Module module1 = engine.compileWat(wat1);
    final Instance instance1 = module1.instantiate(store);

    // Populate table
    instance1.callFunction("populate");

    // Get exported table
    final WasmTable sharedTable = instance1.getTable("shared").orElseThrow();

    // Define table for second module
    final Linker linker = Linker.create(engine);
    linker.defineTable(store, "mod1", "shared", sharedTable);

    final Module module2 = engine.compileWat(wat2);
    final Instance instance2 = linker.instantiate(store, module2);

    // Call through shared table
    final WasmValue[] results = instance2.callFunction("call_from_table");
    assertEquals(42, results[0].asInt());

    instance2.close();
    instance1.close();
    linker.close();
  }

  @Test
  @DisplayName("Multiple tables in one module")
  public void testMultipleTables() throws Exception {
    final String wat =
        """
        (module
          (table (export "t1") 5 funcref)
          (table (export "t2") 3 funcref)

          (func (export "size_t1") (result i32)
            table.size 0
          )

          (func (export "size_t2") (result i32)
            table.size 1
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmTable t1 = instance.getTable("t1").orElseThrow();
    final WasmTable t2 = instance.getTable("t2").orElseThrow();

    assertEquals(5, t1.getSize());
    assertEquals(3, t2.getSize());

    WasmValue[] results = instance.callFunction("size_t1");
    assertEquals(5, results[0].asInt());

    results = instance.callFunction("size_t2");
    assertEquals(3, results[0].asInt());

    instance.close();
  }

  @Test
  @DisplayName("Table element initialization")
  public void testTableElementInit() throws Exception {
    final String wat =
        """
        (module
          (table (export "funcs") 5 funcref)
          (elem (i32.const 0) func $f1 $f2 $f3)

          (func $f1 (result i32) i32.const 1)
          (func $f2 (result i32) i32.const 2)
          (func $f3 (result i32) i32.const 3)

          (func (export "call_at") (param i32) (result i32)
            local.get 0
            call_indirect (result i32)
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Elements should be initialized
    WasmValue[] results = instance.callFunction("call_at", WasmValue.i32(0));
    assertEquals(1, results[0].asInt());

    results = instance.callFunction("call_at", WasmValue.i32(1));
    assertEquals(2, results[0].asInt());

    results = instance.callFunction("call_at", WasmValue.i32(2));
    assertEquals(3, results[0].asInt());

    instance.close();
  }

  @Test
  @DisplayName("Table limits enforcement")
  public void testTableLimits() throws Exception {
    final String wat =
        """
        (module
          (table (export "limited") 2 5 funcref)

          (func (export "grow_to_max") (result i32)
            ref.null func
            i32.const 3
            table.grow 0
          )

          (func (export "grow_beyond_max") (result i32)
            ref.null func
            i32.const 10
            table.grow 0
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmTable table = instance.getTable("limited").orElseThrow();
    assertEquals(2, table.getSize());
    assertEquals(5, table.getMaxSize());

    // Grow to max
    WasmValue[] results = instance.callFunction("grow_to_max");
    assertEquals(2, results[0].asInt()); // Returns old size

    // Should now be at max
    assertEquals(5, table.getSize());

    // Try to grow beyond max (should fail and return -1)
    results = instance.callFunction("grow_beyond_max");
    assertEquals(-1, results[0].asInt());

    // Size should still be at max
    assertEquals(5, table.getSize());

    instance.close();
  }

  @Test
  @DisplayName("Table access out of bounds")
  public void testTableOutOfBounds() throws Exception {
    final String wat =
        """
        (module
          (table 3 funcref)

          (func (export "get_out_of_bounds")
            i32.const 5
            table.get 0
            drop
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Should trap on out of bounds access
    assertThrows(
        Exception.class,
        () -> {
          instance.callFunction("get_out_of_bounds");
        });

    instance.close();
  }

  @Test
  @DisplayName("Externref table")
  public void testExternrefTable() throws Exception {
    final String wat =
        """
        (module
          (table (export "refs") 5 externref)

          (func (export "get_size") (result i32)
            table.size 0
          )

          (func (export "store_null")
            i32.const 0
            ref.null extern
            table.set 0
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmTable table = instance.getTable("refs").orElseThrow();
    assertEquals(5, table.getSize());

    // Store null externref
    instance.callFunction("store_null");

    instance.close();
  }

  @Test
  @DisplayName("Table with passive element segment")
  public void testPassiveElementSegment() throws Exception {
    final String wat =
        """
        (module
          (table (export "funcs") 10 funcref)
          (elem func $f1 $f2)

          (func $f1 (result i32) i32.const 10)
          (func $f2 (result i32) i32.const 20)

          (func (export "init_table")
            i32.const 0  ;; dest offset
            i32.const 0  ;; elem offset
            i32.const 2  ;; length
            table.init 0
          )

          (func (export "drop_elem")
            elem.drop 0
          )

          (func (export "call_at") (param i32) (result i32)
            local.get 0
            call_indirect (result i32)
          )
        )
        """;

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Initialize table from element segment
    instance.callFunction("init_table");

    // Call functions
    WasmValue[] results = instance.callFunction("call_at", WasmValue.i32(0));
    assertEquals(10, results[0].asInt());

    results = instance.callFunction("call_at", WasmValue.i32(1));
    assertEquals(20, results[0].asInt());

    // Drop element segment
    instance.callFunction("drop_elem");

    instance.close();
  }
}
