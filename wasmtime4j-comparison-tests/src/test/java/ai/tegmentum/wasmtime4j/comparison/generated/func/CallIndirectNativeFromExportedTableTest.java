package ai.tegmentum.wasmtime4j.comparison.generated.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Equivalent Java test for Wasmtime test: func::call_indirect_native_from_exported_table
 *
 * <p>Original source: func.rs:332 Category: func
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case. Tests calling a native function via call_indirect through an
 * exported table.
 */
public final class CallIndirectNativeFromExportedTableTest extends DualRuntimeTest {

  @ParameterizedTest(name = "{0}")
  @EnumSource(RuntimeType.class)
  @DisplayName("func::call_indirect_native_from_exported_table")
  public void testCallIndirectNativeFromExportedTable(final RuntimeType runtime) throws Exception {
    // Skip Panama for now as funcref support is incomplete
    assumeTrue(
        runtime == RuntimeType.JNI,
        "Panama funcref support is incomplete - skipping for Panama runtime");

    // WAT code: exports a table and a function that does call_indirect
    final String wat =
        """
        (module
          (type $sig (func (result i32 i32 i32)))
          (table (export "table") 1 1 funcref)
          (func (export "run") (result i32 i32 i32)
            i32.const 0
            call_indirect (type $sig)
          )
        )
        """;

    // Function type for our host function: () -> (i32, i32, i32)
    final FunctionType funcType =
        new FunctionType(
            new WasmValueType[] {},
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32, WasmValueType.I32});

    // Host function that returns (10, 20, 30)
    final HostFunction hostFunc =
        (params) ->
            new WasmValue[] {WasmValue.i32(10), WasmValue.i32(20), WasmValue.i32(30)};

    try (final WastTestRunner runner = new WastTestRunner(runtime)) {
      // Compile and instantiate the module
      final Instance instance = runner.compileAndInstantiate(wat);

      // Get the exported table
      final Optional<WasmTable> tableOpt = instance.getTable("table");
      assertTrue(tableOpt.isPresent(), "Table 'table' should be exported");

      final WasmTable table = tableOpt.get();
      assertEquals(1, table.getSize(), "Table should have 1 element");

      // Create a WasmFunc from our host function and set it in the table at index 0
      // Note: This requires Store.createHostFunction which wraps our HostFunction
      final ai.tegmentum.wasmtime4j.Store store = runner.getStore();
      final ai.tegmentum.wasmtime4j.WasmFunction wasmFunc =
          store.createHostFunction("native_func", funcType, hostFunc);
      table.set(0, wasmFunc);

      // Call the exported run function which does call_indirect at index 0
      final WasmValue[] results = runner.invoke("run");

      // Verify results: should be (10, 20, 30)
      assertEquals(3, results.length, "Should return three values");
      assertEquals(10, results[0].asInt(), "First result should be 10");
      assertEquals(20, results[1].asInt(), "Second result should be 20");
      assertEquals(30, results[2].asInt(), "Third result should be 30");
    }
  }
}
