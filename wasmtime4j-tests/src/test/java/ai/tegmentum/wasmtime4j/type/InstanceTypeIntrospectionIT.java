package ai.tegmentum.wasmtime4j.type;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for runtime instance type introspection.
 *
 * <p>These tests verify that type introspection works correctly at runtime with actual WebAssembly
 * instances, testing both static module information and dynamic instance state.
 *
 * @since 1.0.0
 */
@DisplayName("Instance Type Introspection Integration Tests")
public class InstanceTypeIntrospectionIT {

  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() {
    engine = Engine.newBuilder().build();
    store = Store.newBuilder(engine).build();
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("Runtime Instance Type Introspection")
  void testRuntimeInstanceTypeIntrospection() throws WasmException {
    // Create a module with various exported types
    final String wat =
        """
        (module
          (func $simple_add (param i32 i32) (result i32)
            local.get 0
            local.get 1
            i32.add)

          (func $multi_param (param i32 i64 f32 f64) (result i32 i64)
            local.get 0
            local.get 1)

          (func $no_param_func (result f32)
            f32.const 42.0)

          (func $void_func (param i32))

          (global $const_global i32 (i32.const 123))
          (global $mut_global (mut f64) (f64.const 3.14159))

          (memory $main_memory 1 5)
          (table $func_table 3 10 funcref)

          (export "simple_add" (func $simple_add))
          (export "multi_param" (func $multi_param))
          (export "no_param_func" (func $no_param_func))
          (export "void_func" (func $void_func))
          (export "const_global" (global $const_global))
          (export "mut_global" (global $mut_global))
          (export "main_memory" (memory $main_memory))
          (export "func_table" (table $func_table))
        )
        """;

    final Module module = Module.compile(engine, wat.getBytes());
    final Instance instance = module.instantiate(store);

    // Test runtime export descriptor retrieval
    final List<ExportDescriptor> exports = instance.getExportDescriptors();
    assertEquals(8, exports.size(), "Expected 8 exports");

    // Verify all expected exports are present
    final String[] expectedExports = {
      "simple_add", "multi_param", "no_param_func", "void_func",
      "const_global", "mut_global", "main_memory", "func_table"
    };

    for (final String exportName : expectedExports) {
      assertTrue(instance.hasExport(exportName), "Missing export: " + exportName);
    }

    // Test individual export descriptor retrieval
    final Optional<ExportDescriptor> addDescOpt = instance.getExportDescriptor("simple_add");
    assertTrue(addDescOpt.isPresent(), "simple_add export descriptor should be present");

    final ExportDescriptor addDesc = addDescOpt.get();
    assertEquals("simple_add", addDesc.getName());
    assertTrue(addDesc.isFunction());
    assertFalse(addDesc.isGlobal());
    assertFalse(addDesc.isMemory());
    assertFalse(addDesc.isTable());

    final FuncType addFuncType = addDesc.asFunctionType();
    assertEquals(List.of(WasmValueType.I32, WasmValueType.I32), addFuncType.getParams());
    assertEquals(List.of(WasmValueType.I32), addFuncType.getResults());
    assertEquals(2, addFuncType.getParamCount());
    assertEquals(1, addFuncType.getResultCount());

    // Test complex function type
    final Optional<FuncType> multiFuncTypeOpt = instance.getFunctionType("multi_param");
    assertTrue(multiFuncTypeOpt.isPresent());

    final FuncType multiFuncType = multiFuncTypeOpt.get();
    assertEquals(
        List.of(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64),
        multiFuncType.getParams());
    assertEquals(List.of(WasmValueType.I32, WasmValueType.I64), multiFuncType.getResults());
    assertEquals(4, multiFuncType.getParamCount());
    assertEquals(2, multiFuncType.getResultCount());

    // Test function with no parameters
    final Optional<FuncType> noParamTypeOpt = instance.getFunctionType("no_param_func");
    assertTrue(noParamTypeOpt.isPresent());

    final FuncType noParamType = noParamTypeOpt.get();
    assertEquals(List.of(), noParamType.getParams());
    assertEquals(List.of(WasmValueType.F32), noParamType.getResults());
    assertEquals(0, noParamType.getParamCount());
    assertEquals(1, noParamType.getResultCount());

    // Test function with no return value
    final Optional<FuncType> voidFuncTypeOpt = instance.getFunctionType("void_func");
    assertTrue(voidFuncTypeOpt.isPresent());

    final FuncType voidFuncType = voidFuncTypeOpt.get();
    assertEquals(List.of(WasmValueType.I32), voidFuncType.getParams());
    assertEquals(List.of(), voidFuncType.getResults());
    assertEquals(1, voidFuncType.getParamCount());
    assertEquals(0, voidFuncType.getResultCount());

    // Test global types
    final Optional<GlobalType> constGlobalOpt = instance.getGlobalType("const_global");
    assertTrue(constGlobalOpt.isPresent());

    final GlobalType constGlobal = constGlobalOpt.get();
    assertEquals(WasmValueType.I32, constGlobal.getValueType());
    assertFalse(constGlobal.isMutable());

    final Optional<GlobalType> mutGlobalOpt = instance.getGlobalType("mut_global");
    assertTrue(mutGlobalOpt.isPresent());

    final GlobalType mutGlobal = mutGlobalOpt.get();
    assertEquals(WasmValueType.F64, mutGlobal.getValueType());
    assertTrue(mutGlobal.isMutable());

    // Test memory type
    final Optional<MemoryType> memoryTypeOpt = instance.getMemoryType("main_memory");
    assertTrue(memoryTypeOpt.isPresent());

    final MemoryType memoryType = memoryTypeOpt.get();
    assertEquals(1, memoryType.getMinimum());
    assertEquals(Optional.of(5L), memoryType.getMaximum());
    assertFalse(memoryType.is64Bit());
    assertFalse(memoryType.isShared());

    // Test table type
    final Optional<TableType> tableTypeOpt = instance.getTableType("func_table");
    assertTrue(tableTypeOpt.isPresent());

    final TableType tableType = tableTypeOpt.get();
    assertEquals(WasmValueType.FUNCREF, tableType.getElementType());
    assertEquals(3, tableType.getMinimum());
    assertEquals(Optional.of(10L), tableType.getMaximum());

    // Test non-existent exports
    assertFalse(instance.hasExport("nonexistent"));
    assertEquals(Optional.empty(), instance.getExportDescriptor("nonexistent"));
    assertEquals(Optional.empty(), instance.getFunctionType("nonexistent"));
    assertEquals(Optional.empty(), instance.getGlobalType("nonexistent"));
    assertEquals(Optional.empty(), instance.getMemoryType("nonexistent"));
    assertEquals(Optional.empty(), instance.getTableType("nonexistent"));

    // Test type casting errors
    final Optional<ExportDescriptor> globalDescOpt = instance.getExportDescriptor("const_global");
    assertTrue(globalDescOpt.isPresent());
    final ExportDescriptor globalDesc = globalDescOpt.get();

    assertTrue(globalDesc.isGlobal());
    assertFalse(globalDesc.isFunction());

    assertThrows(IllegalStateException.class, globalDesc::asFunctionType);
    assertThrows(IllegalStateException.class, globalDesc::asMemoryType);
    assertThrows(IllegalStateException.class, globalDesc::asTableType);

    // The global cast should work
    assertDoesNotThrow(globalDesc::asGlobalType);

    instance.close();
    module.close();
  }

  @Test
  @DisplayName("Instance Type Introspection with WASI")
  void testInstanceTypeIntrospectionWithWasi() throws WasmException {
    // Create a module that uses some WASI imports for a more complex scenario
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))

          (func $main (export "_start")
            ;; Just a simple function that could use WASI
            nop)

          (func $exported_func (export "test_func") (param i32) (result i32)
            local.get 0
            i32.const 1
            i32.add)

          (memory (export "memory") 1)
        )
        """;

    final Module module = Module.compile(engine, wat.getBytes());

    // Test module-level introspection first
    final List<ImportDescriptor> imports = module.getImportDescriptors();
    assertEquals(1, imports.size());

    final ImportDescriptor wasiImport = imports.get(0);
    assertEquals("wasi_snapshot_preview1", wasiImport.getModuleName());
    assertEquals("fd_write", wasiImport.getName());
    assertTrue(wasiImport.isFunction());

    final FuncType wasiFunc = wasiImport.asFunctionType();
    assertEquals(
        List.of(WasmValueType.I32, WasmValueType.I32, WasmValueType.I32, WasmValueType.I32),
        wasiFunc.getParams());
    assertEquals(List.of(WasmValueType.I32), wasiFunc.getResults());

    final List<ExportDescriptor> exports = module.getExportDescriptors();
    assertEquals(3, exports.size()); // _start, test_func, memory

    // For this test, we'll skip instantiation since WASI requires proper setup
    // But we can verify the export types from the module
    assertTrue(module.hasExport("_start"));
    assertTrue(module.hasExport("test_func"));
    assertTrue(module.hasExport("memory"));

    final Optional<FuncType> testFuncOpt = module.getFunctionType("test_func");
    assertTrue(testFuncOpt.isPresent());

    final FuncType testFunc = testFuncOpt.get();
    assertEquals(List.of(WasmValueType.I32), testFunc.getParams());
    assertEquals(List.of(WasmValueType.I32), testFunc.getResults());

    final Optional<MemoryType> memoryOpt = module.getMemoryType("memory");
    assertTrue(memoryOpt.isPresent());

    final MemoryType memory = memoryOpt.get();
    assertEquals(1, memory.getMinimum());
    assertEquals(Optional.empty(), memory.getMaximum());

    module.close();
  }

  @Test
  @DisplayName("Edge Cases and Error Conditions")
  void testEdgeCasesAndErrors() throws WasmException {
    // Test module with only imports (no exports)
    final String importOnlyWat =
        """
        (module
          (import "env" "func" (func (param i32) (result i32)))
          (import "env" "memory" (memory 1))
        )
        """;

    final Module importOnlyModule = Module.compile(engine, importOnlyWat.getBytes());

    final List<ExportDescriptor> exports = importOnlyModule.getExportDescriptors();
    assertEquals(0, exports.size(), "Module with no exports should have empty export list");

    final List<ImportDescriptor> imports = importOnlyModule.getImportDescriptors();
    assertEquals(2, imports.size(), "Expected 2 imports");

    // Test null parameter handling
    assertThrows(IllegalArgumentException.class, () -> importOnlyModule.hasExport(null));
    assertThrows(IllegalArgumentException.class, () -> importOnlyModule.hasImport(null, "func"));
    assertThrows(IllegalArgumentException.class, () -> importOnlyModule.hasImport("env", null));
    assertThrows(IllegalArgumentException.class, () -> importOnlyModule.getFunctionType(null));
    assertThrows(IllegalArgumentException.class, () -> importOnlyModule.getGlobalType(null));
    assertThrows(IllegalArgumentException.class, () -> importOnlyModule.getMemoryType(null));
    assertThrows(IllegalArgumentException.class, () -> importOnlyModule.getTableType(null));

    importOnlyModule.close();

    // Test empty module
    final String emptyWat = """
        (module)
        """;

    final Module emptyModule = Module.compile(engine, emptyWat.getBytes());

    assertEquals(0, emptyModule.getExportDescriptors().size());
    assertEquals(0, emptyModule.getImportDescriptors().size());
    assertFalse(emptyModule.hasExport("anything"));
    assertFalse(emptyModule.hasImport("env", "anything"));

    final Instance emptyInstance = emptyModule.instantiate(store);
    assertEquals(0, emptyInstance.getExportDescriptors().size());
    assertFalse(emptyInstance.hasExport("anything"));

    emptyInstance.close();
    emptyModule.close();
  }

  @Test
  @DisplayName("Complex Type Hierarchies")
  void testComplexTypeHierarchies() throws WasmException {
    // Test a module with complex nested function signatures and multiple table/memory types
    final String complexWat =
        """
        (module
          (type $complex_sig (func
            (param i32 i64 f32 f64 externref funcref)
            (result i32 i64 f32)))

          (func $complex_func (type $complex_sig)
            local.get 0
            local.get 1
            local.get 2)

          (func $callback_func (param funcref i32) (result i32)
            local.get 1)

          (global $externref_global (mut externref) (ref.null extern))
          (global $funcref_global (mut funcref) (ref.null func))

          (table $extern_table 1 externref)
          (table $func_table 5 funcref)

          (memory $memory64 1) ;; Note: actual 64-bit memory requires special Wasmtime config

          (export "complex_func" (func $complex_func))
          (export "callback_func" (func $callback_func))
          (export "externref_global" (global $externref_global))
          (export "funcref_global" (global $funcref_global))
          (export "extern_table" (table $extern_table))
          (export "func_table" (table $func_table))
          (export "memory64" (memory $memory64))
        )
        """;

    final Module complexModule = Module.compile(engine, complexWat.getBytes());
    final Instance complexInstance = complexModule.instantiate(store);

    // Test complex function signature
    final Optional<FuncType> complexFuncOpt = complexInstance.getFunctionType("complex_func");
    assertTrue(complexFuncOpt.isPresent());

    final FuncType complexFunc = complexFuncOpt.get();
    assertEquals(6, complexFunc.getParamCount());
    assertEquals(3, complexFunc.getResultCount());
    assertEquals(
        List.of(
            WasmValueType.I32,
            WasmValueType.I64,
            WasmValueType.F32,
            WasmValueType.F64,
            WasmValueType.EXTERNREF,
            WasmValueType.FUNCREF),
        complexFunc.getParams());
    assertEquals(
        List.of(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32), complexFunc.getResults());

    // Test callback function with reference types
    final Optional<FuncType> callbackFuncOpt = complexInstance.getFunctionType("callback_func");
    assertTrue(callbackFuncOpt.isPresent());

    final FuncType callbackFunc = callbackFuncOpt.get();
    assertEquals(List.of(WasmValueType.FUNCREF, WasmValueType.I32), callbackFunc.getParams());
    assertEquals(List.of(WasmValueType.I32), callbackFunc.getResults());

    // Test reference type globals
    final Optional<GlobalType> externGlobalOpt = complexInstance.getGlobalType("externref_global");
    assertTrue(externGlobalOpt.isPresent());

    final GlobalType externGlobal = externGlobalOpt.get();
    assertEquals(WasmValueType.EXTERNREF, externGlobal.getValueType());
    assertTrue(externGlobal.isMutable());

    final Optional<GlobalType> funcGlobalOpt = complexInstance.getGlobalType("funcref_global");
    assertTrue(funcGlobalOpt.isPresent());

    final GlobalType funcGlobal = funcGlobalOpt.get();
    assertEquals(WasmValueType.FUNCREF, funcGlobal.getValueType());
    assertTrue(funcGlobal.isMutable());

    // Test different table types
    final Optional<TableType> externTableOpt = complexInstance.getTableType("extern_table");
    assertTrue(externTableOpt.isPresent());

    final TableType externTable = externTableOpt.get();
    assertEquals(WasmValueType.EXTERNREF, externTable.getElementType());
    assertEquals(1, externTable.getMinimum());
    assertEquals(Optional.empty(), externTable.getMaximum());

    final Optional<TableType> funcTableOpt = complexInstance.getTableType("func_table");
    assertTrue(funcTableOpt.isPresent());

    final TableType funcTable = funcTableOpt.get();
    assertEquals(WasmValueType.FUNCREF, funcTable.getElementType());
    assertEquals(5, funcTable.getMinimum());

    complexInstance.close();
    complexModule.close();
  }
}
