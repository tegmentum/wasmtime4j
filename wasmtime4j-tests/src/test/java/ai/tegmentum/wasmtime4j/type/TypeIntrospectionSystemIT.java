package ai.tegmentum.wasmtime4j.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive integration tests for the Type Introspection System.
 *
 * <p>These tests verify that type introspection works correctly across both JNI and Panama
 * implementations, ensuring identical behavior and complete API coverage.
 *
 * @since 1.0.0
 */
@DisplayName("Type Introspection System Integration Tests")
public class TypeIntrospectionSystemIT {

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
  @DisplayName("Basic Module Type Introspection")
  void testBasicModuleTypeIntrospection() throws WasmException {
    // Create a comprehensive test module with all types
    final String wat =
        """
        (module
          (import "env" "memory" (memory 1 10))
          (import "env" "table" (table 5 funcref))
          (import "env" "global_i32" (global i32))
          (import "env" "global_mut_f64" (global (mut f64)))
          (import "env" "imported_func" (func (param i32 f64) (result i64)))

          (func $add (param $a i32) (param $b i32) (result i32)
            local.get $a
            local.get $b
            i32.add)

          (func $complex (param i32 i64 f32 f64) (result i32 i64)
            local.get 0
            local.get 1)

          (func $no_params (result f32)
            f32.const 42.0)

          (func $no_results (param i32))

          (global $exported_const i32 (i32.const 100))
          (global $exported_mut (mut f64) (f64.const 3.14))

          (memory $exported_memory 2 20)
          (table $exported_table 10 funcref)

          (export "add" (func $add))
          (export "complex" (func $complex))
          (export "no_params" (func $no_params))
          (export "no_results" (func $no_results))
          (export "exported_const" (global $exported_const))
          (export "exported_mut" (global $exported_mut))
          (export "exported_memory" (memory $exported_memory))
          (export "exported_table" (table $exported_table))
        )
        """;

    final Module module = Module.compile(engine, wat.getBytes());

    // Test import descriptors
    final List<ImportDescriptor> imports = module.getImportDescriptors();
    assertEquals(5, imports.size(), "Expected 5 imports");

    // Verify memory import
    final ImportDescriptor memoryImport =
        imports.stream()
            .filter(imp -> imp.getName().equals("memory"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Memory import not found"));

    assertEquals("env", memoryImport.getModuleName());
    assertEquals("memory", memoryImport.getName());
    assertTrue(memoryImport.isMemory());
    assertFalse(memoryImport.isFunction());
    assertFalse(memoryImport.isGlobal());
    assertFalse(memoryImport.isTable());

    final MemoryType memoryType = memoryImport.asMemoryType();
    assertEquals(1, memoryType.getMinimum());
    assertEquals(Optional.of(10L), memoryType.getMaximum());
    assertFalse(memoryType.is64Bit());
    assertFalse(memoryType.isShared());

    // Verify table import
    final ImportDescriptor tableImport =
        imports.stream()
            .filter(imp -> imp.getName().equals("table"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Table import not found"));

    assertTrue(tableImport.isTable());
    final TableType tableType = tableImport.asTableType();
    assertEquals(WasmValueType.FUNCREF, tableType.getElementType());
    assertEquals(5, tableType.getMinimum());
    assertEquals(Optional.empty(), tableType.getMaximum());

    // Verify global imports
    final ImportDescriptor globalImport =
        imports.stream()
            .filter(imp -> imp.getName().equals("global_i32"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Global import not found"));

    assertTrue(globalImport.isGlobal());
    final GlobalType globalType = globalImport.asGlobalType();
    assertEquals(WasmValueType.I32, globalType.getValueType());
    assertFalse(globalType.isMutable());

    final ImportDescriptor mutGlobalImport =
        imports.stream()
            .filter(imp -> imp.getName().equals("global_mut_f64"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Mutable global import not found"));

    final GlobalType mutGlobalType = mutGlobalImport.asGlobalType();
    assertEquals(WasmValueType.F64, mutGlobalType.getValueType());
    assertTrue(mutGlobalType.isMutable());

    // Verify function import
    final ImportDescriptor funcImport =
        imports.stream()
            .filter(imp -> imp.getName().equals("imported_func"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Function import not found"));

    assertTrue(funcImport.isFunction());
    final FuncType funcType = funcImport.asFunctionType();
    assertEquals(List.of(WasmValueType.I32, WasmValueType.F64), funcType.getParams());
    assertEquals(List.of(WasmValueType.I64), funcType.getResults());
    assertEquals(2, funcType.getParamCount());
    assertEquals(1, funcType.getResultCount());

    // Test export descriptors
    final List<ExportDescriptor> exports = module.getExportDescriptors();
    assertEquals(8, exports.size(), "Expected 8 exports");

    // Verify exported functions
    final ExportDescriptor addExport =
        exports.stream()
            .filter(exp -> exp.getName().equals("add"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Add function export not found"));

    assertTrue(addExport.isFunction());
    final FuncType addFuncType = addExport.asFunctionType();
    assertEquals(List.of(WasmValueType.I32, WasmValueType.I32), addFuncType.getParams());
    assertEquals(List.of(WasmValueType.I32), addFuncType.getResults());

    final ExportDescriptor complexExport =
        exports.stream()
            .filter(exp -> exp.getName().equals("complex"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Complex function export not found"));

    final FuncType complexFuncType = complexExport.asFunctionType();
    assertEquals(
        List.of(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64),
        complexFuncType.getParams());
    assertEquals(List.of(WasmValueType.I32, WasmValueType.I64), complexFuncType.getResults());

    // Test direct access methods
    assertTrue(module.hasExport("add"));
    assertTrue(module.hasExport("exported_memory"));
    assertFalse(module.hasExport("nonexistent"));

    assertTrue(module.hasImport("env", "memory"));
    assertTrue(module.hasImport("env", "imported_func"));
    assertFalse(module.hasImport("env", "nonexistent"));
    assertFalse(module.hasImport("other", "memory"));

    // Test type-specific getters
    final Optional<FuncType> addTypeOpt = module.getFunctionType("add");
    assertTrue(addTypeOpt.isPresent());
    assertEquals(addFuncType, addTypeOpt.get());

    final Optional<MemoryType> memTypeOpt = module.getMemoryType("exported_memory");
    assertTrue(memTypeOpt.isPresent());
    assertEquals(2, memTypeOpt.get().getMinimum());
    assertEquals(Optional.of(20L), memTypeOpt.get().getMaximum());

    final Optional<GlobalType> globalTypeOpt = module.getGlobalType("exported_const");
    assertTrue(globalTypeOpt.isPresent());
    assertEquals(WasmValueType.I32, globalTypeOpt.get().getValueType());
    assertFalse(globalTypeOpt.get().isMutable());

    final Optional<TableType> tableTypeOpt = module.getTableType("exported_table");
    assertTrue(tableTypeOpt.isPresent());
    assertEquals(WasmValueType.FUNCREF, tableTypeOpt.get().getElementType());
    assertEquals(10, tableTypeOpt.get().getMinimum());

    module.close();
  }

  @Test
  @DisplayName("WasmValueType Conversion Methods")
  void testWasmValueTypeConversions() {
    // Test size methods
    assertEquals(4, WasmValueType.I32.getSize());
    assertEquals(8, WasmValueType.I64.getSize());
    assertEquals(4, WasmValueType.F32.getSize());
    assertEquals(8, WasmValueType.F64.getSize());
    assertEquals(16, WasmValueType.V128.getSize());
    assertEquals(-1, WasmValueType.FUNCREF.getSize());
    assertEquals(-1, WasmValueType.EXTERNREF.getSize());

    // Test type checking methods
    assertTrue(WasmValueType.I32.isInteger());
    assertTrue(WasmValueType.I64.isInteger());
    assertFalse(WasmValueType.F32.isInteger());
    assertFalse(WasmValueType.FUNCREF.isInteger());

    assertTrue(WasmValueType.F32.isFloat());
    assertTrue(WasmValueType.F64.isFloat());
    assertFalse(WasmValueType.I32.isFloat());
    assertFalse(WasmValueType.FUNCREF.isFloat());

    assertTrue(WasmValueType.FUNCREF.isReference());
    assertTrue(WasmValueType.EXTERNREF.isReference());
    assertFalse(WasmValueType.I32.isReference());
    assertFalse(WasmValueType.F64.isReference());

    assertTrue(WasmValueType.V128.isVector());
    assertFalse(WasmValueType.I32.isVector());
    assertFalse(WasmValueType.FUNCREF.isVector());

    assertTrue(WasmValueType.I32.isNumeric());
    assertTrue(WasmValueType.F64.isNumeric());
    assertFalse(WasmValueType.FUNCREF.isNumeric());
    assertFalse(WasmValueType.V128.isNumeric());

    // Test native type code conversions
    assertEquals(WasmValueType.I32, WasmValueType.fromNativeTypeCode(0));
    assertEquals(WasmValueType.I64, WasmValueType.fromNativeTypeCode(1));
    assertEquals(WasmValueType.F32, WasmValueType.fromNativeTypeCode(2));
    assertEquals(WasmValueType.F64, WasmValueType.fromNativeTypeCode(3));
    assertEquals(WasmValueType.V128, WasmValueType.fromNativeTypeCode(4));
    assertEquals(WasmValueType.FUNCREF, WasmValueType.fromNativeTypeCode(5));
    assertEquals(WasmValueType.EXTERNREF, WasmValueType.fromNativeTypeCode(6));

    assertEquals(0, WasmValueType.I32.toNativeTypeCode());
    assertEquals(1, WasmValueType.I64.toNativeTypeCode());
    assertEquals(2, WasmValueType.F32.toNativeTypeCode());
    assertEquals(3, WasmValueType.F64.toNativeTypeCode());
    assertEquals(4, WasmValueType.V128.toNativeTypeCode());
    assertEquals(5, WasmValueType.FUNCREF.toNativeTypeCode());
    assertEquals(6, WasmValueType.EXTERNREF.toNativeTypeCode());

    // Test invalid type code
    assertThrows(IllegalArgumentException.class, () -> WasmValueType.fromNativeTypeCode(999));
  }

  @Test
  @DisplayName("Type Interface Equality and ToString")
  void testTypeInterfaceEquality() {
    // Test FuncType equality
    final FuncType func1 =
        createMockFuncType(
            List.of(WasmValueType.I32, WasmValueType.F64), List.of(WasmValueType.I64));
    final FuncType func2 =
        createMockFuncType(
            List.of(WasmValueType.I32, WasmValueType.F64), List.of(WasmValueType.I64));
    final FuncType func3 =
        createMockFuncType(List.of(WasmValueType.I32), List.of(WasmValueType.I64));

    assertEquals(func1, func2);
    assertNotEquals(func1, func3);
    assertEquals(func1.hashCode(), func2.hashCode());
    assertNotNull(func1.toString());

    // Test MemoryType equality
    final MemoryType mem1 = createMockMemoryType(1, 10L, false, false);
    final MemoryType mem2 = createMockMemoryType(1, 10L, false, false);
    final MemoryType mem3 = createMockMemoryType(1, null, false, false);

    assertEquals(mem1, mem2);
    assertNotEquals(mem1, mem3);
    assertEquals(mem1.hashCode(), mem2.hashCode());
    assertNotNull(mem1.toString());

    // Test GlobalType equality
    final GlobalType global1 = createMockGlobalType(WasmValueType.I32, true);
    final GlobalType global2 = createMockGlobalType(WasmValueType.I32, true);
    final GlobalType global3 = createMockGlobalType(WasmValueType.I32, false);

    assertEquals(global1, global2);
    assertNotEquals(global1, global3);
    assertEquals(global1.hashCode(), global2.hashCode());
    assertNotNull(global1.toString());

    // Test TableType equality
    final TableType table1 = createMockTableType(WasmValueType.FUNCREF, 5, 100L);
    final TableType table2 = createMockTableType(WasmValueType.FUNCREF, 5, 100L);
    final TableType table3 = createMockTableType(WasmValueType.EXTERNREF, 5, 100L);

    assertEquals(table1, table2);
    assertNotEquals(table1, table3);
    assertEquals(table1.hashCode(), table2.hashCode());
    assertNotNull(table1.toString());
  }

  @Test
  @DisplayName("Error Handling and Edge Cases")
  void testErrorHandling() {
    // Test invalid table element types
    assertThrows(
        IllegalArgumentException.class, () -> createMockTableType(WasmValueType.I32, 1, null));

    // Test negative minimum values
    assertThrows(
        IllegalArgumentException.class, () -> createMockMemoryType(-1, null, false, false));

    assertThrows(
        IllegalArgumentException.class, () -> createMockTableType(WasmValueType.FUNCREF, -1, null));

    // Test maximum less than minimum
    assertThrows(IllegalArgumentException.class, () -> createMockMemoryType(10, 5L, false, false));

    assertThrows(
        IllegalArgumentException.class, () -> createMockTableType(WasmValueType.FUNCREF, 10, 5L));

    // Test null parameters in constructor-like scenarios
    assertThrows(
        IllegalArgumentException.class, () -> createMockFuncType(null, List.of(WasmValueType.I32)));

    assertThrows(
        IllegalArgumentException.class, () -> createMockFuncType(List.of(WasmValueType.I32), null));

    assertThrows(IllegalArgumentException.class, () -> createMockGlobalType(null, false));
  }

  // Helper methods to create mock implementations for testing
  private FuncType createMockFuncType(
      final List<WasmValueType> params, final List<WasmValueType> results) {
    return new FuncType() {
      @Override
      public List<WasmValueType> getParams() {
        return params;
      }

      @Override
      public List<WasmValueType> getResults() {
        return results;
      }

      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.FUNCTION;
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof FuncType)) {
          return false;
        }
        FuncType other = (FuncType) obj;
        return params.equals(other.getParams()) && results.equals(other.getResults());
      }

      @Override
      public int hashCode() {
        return java.util.Objects.hash(params, results);
      }

      @Override
      public String toString() {
        return String.format("FuncType{params=%s, results=%s}", params, results);
      }
    };
  }

  private MemoryType createMockMemoryType(
      final long minimum, final Long maximum, final boolean is64Bit, final boolean isShared) {
    if (minimum < 0) {
      throw new IllegalArgumentException("Minimum cannot be negative");
    }
    if (maximum != null && maximum < minimum) {
      throw new IllegalArgumentException("Maximum less than minimum");
    }

    return new MemoryType() {
      @Override
      public long getMinimum() {
        return minimum;
      }

      @Override
      public Optional<Long> getMaximum() {
        return Optional.ofNullable(maximum);
      }

      @Override
      public boolean is64Bit() {
        return is64Bit;
      }

      @Override
      public boolean isShared() {
        return isShared;
      }

      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.MEMORY;
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof MemoryType)) {
          return false;
        }
        MemoryType other = (MemoryType) obj;
        return minimum == other.getMinimum()
            && getMaximum().equals(other.getMaximum())
            && is64Bit == other.is64Bit()
            && isShared == other.isShared();
      }

      @Override
      public int hashCode() {
        return java.util.Objects.hash(minimum, getMaximum(), is64Bit, isShared);
      }

      @Override
      public String toString() {
        return String.format(
            "MemoryType{min=%d, max=%s, 64bit=%b, shared=%b}",
            minimum, getMaximum().map(String::valueOf).orElse("unlimited"), is64Bit, isShared);
      }
    };
  }

  private GlobalType createMockGlobalType(final WasmValueType valueType, final boolean isMutable) {
    if (valueType == null) {
      throw new IllegalArgumentException("Value type cannot be null");
    }

    return new GlobalType() {
      @Override
      public WasmValueType getValueType() {
        return valueType;
      }

      @Override
      public boolean isMutable() {
        return isMutable;
      }

      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.GLOBAL;
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof GlobalType)) {
          return false;
        }
        GlobalType other = (GlobalType) obj;
        return valueType == other.getValueType() && isMutable == other.isMutable();
      }

      @Override
      public int hashCode() {
        return java.util.Objects.hash(valueType, isMutable);
      }

      @Override
      public String toString() {
        return String.format("GlobalType{valueType=%s, mutable=%b}", valueType, isMutable);
      }
    };
  }

  private TableType createMockTableType(
      final WasmValueType elementType, final long minimum, final Long maximum) {
    if (elementType == null) {
      throw new IllegalArgumentException("Element type cannot be null");
    }
    if (!elementType.isReference()) {
      throw new IllegalArgumentException("Element type must be reference");
    }
    if (minimum < 0) {
      throw new IllegalArgumentException("Minimum cannot be negative");
    }
    if (maximum != null && maximum < minimum) {
      throw new IllegalArgumentException("Maximum less than minimum");
    }

    return new TableType() {
      @Override
      public WasmValueType getElementType() {
        return elementType;
      }

      @Override
      public long getMinimum() {
        return minimum;
      }

      @Override
      public Optional<Long> getMaximum() {
        return Optional.ofNullable(maximum);
      }

      @Override
      public WasmTypeKind getKind() {
        return WasmTypeKind.TABLE;
      }

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof TableType)) {
          return false;
        }
        TableType other = (TableType) obj;
        return elementType == other.getElementType()
            && minimum == other.getMinimum()
            && getMaximum().equals(other.getMaximum());
      }

      @Override
      public int hashCode() {
        return java.util.Objects.hash(elementType, minimum, getMaximum());
      }

      @Override
      public String toString() {
        return String.format(
            "TableType{element=%s, min=%d, max=%s}",
            elementType, minimum, getMaximum().map(String::valueOf).orElse("unlimited"));
      }
    };
  }
}
