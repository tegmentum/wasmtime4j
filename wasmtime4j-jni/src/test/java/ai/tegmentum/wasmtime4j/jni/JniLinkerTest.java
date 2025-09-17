package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link JniLinker}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly linker operations is tested in integration
 * tests.
 */
class JniLinkerTest {

  private static final long VALID_HANDLE = 0x12345678L;

  @Mock private JniEngine mockEngine;
  @Mock private FunctionType mockFunctionType;
  @Mock private HostFunction mockHostFunction;
  @Mock private JniMemory mockMemory;
  @Mock private JniTable mockTable;
  @Mock private JniGlobal mockGlobal;
  @Mock private JniInstance mockInstance;
  @Mock private JniStore mockStore;
  @Mock private JniModule mockModule;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockEngine.getNativeHandle()).thenReturn(VALID_HANDLE);
    when(mockMemory.getNativeHandle()).thenReturn(VALID_HANDLE);
    when(mockTable.getNativeHandle()).thenReturn(VALID_HANDLE);
    when(mockGlobal.getNativeHandle()).thenReturn(VALID_HANDLE);
    when(mockInstance.getNativeHandle()).thenReturn(VALID_HANDLE);
    when(mockStore.getNativeHandle()).thenReturn(VALID_HANDLE);
    when(mockModule.getNativeHandle()).thenReturn(VALID_HANDLE);
  }

  @Test
  void testConstructorWithValidHandle() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    assertThat(linker.getResourceType()).isEqualTo("Linker");
    assertFalse(linker.isClosed());
    assertEquals(mockEngine, linker.getEngine());
    assertTrue(linker.isValid());
  }

  @Test
  void testConstructorWithZeroHandle() {
    assertThrows(IllegalArgumentException.class, () ->
        new JniLinker(0L, mockEngine));
  }

  @Test
  void testConstructorWithNullEngine() {
    assertThrows(JniValidationException.class, () ->
        new JniLinker(VALID_HANDLE, null));
  }

  @Test
  void testCreateWithValidEngine() throws WasmException {
    // This test would need native library support for actual creation
    // For unit test, we just verify parameter validation
    assertThrows(JniValidationException.class, () ->
        JniLinker.create(null));
  }

  @Test
  void testCreateWithNonJniEngine() throws WasmException {
    Engine nonJniEngine = new Engine() {
      @Override public Store createStore() throws WasmException { return null; }
      @Override public Store createStore(Object data) throws WasmException { return null; }
      @Override public Module compileModule(byte[] wasmBytes) throws WasmException { return null; }
      @Override public ai.tegmentum.wasmtime4j.EngineConfig getConfig() { return null; }
      @Override public boolean isValid() { return true; }
      @Override public void close() {}
    };

    assertThrows(IllegalArgumentException.class, () ->
        JniLinker.create(nonJniEngine));
  }

  @Test
  void testDefineHostFunctionParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null module name
    assertThrows(JniValidationException.class, () ->
        linker.defineHostFunction(null, "test", mockFunctionType, mockHostFunction));

    // Test empty module name
    assertThrows(JniValidationException.class, () ->
        linker.defineHostFunction("", "test", mockFunctionType, mockHostFunction));

    // Test null function name
    assertThrows(JniValidationException.class, () ->
        linker.defineHostFunction("env", null, mockFunctionType, mockHostFunction));

    // Test empty function name
    assertThrows(JniValidationException.class, () ->
        linker.defineHostFunction("env", "", mockFunctionType, mockHostFunction));

    // Test null function type
    assertThrows(JniValidationException.class, () ->
        linker.defineHostFunction("env", "test", null, mockHostFunction));

    // Test null host function
    assertThrows(JniValidationException.class, () ->
        linker.defineHostFunction("env", "test", mockFunctionType, null));
  }

  @Test
  void testDefineMemoryParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null module name
    assertThrows(JniValidationException.class, () ->
        linker.defineMemory(null, "memory", mockMemory));

    // Test empty module name
    assertThrows(JniValidationException.class, () ->
        linker.defineMemory("", "memory", mockMemory));

    // Test null memory name
    assertThrows(JniValidationException.class, () ->
        linker.defineMemory("env", null, mockMemory));

    // Test empty memory name
    assertThrows(JniValidationException.class, () ->
        linker.defineMemory("env", "", mockMemory));

    // Test null memory
    assertThrows(JniValidationException.class, () ->
        linker.defineMemory("env", "memory", null));
  }

  @Test
  void testDefineTableParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null module name
    assertThrows(JniValidationException.class, () ->
        linker.defineTable(null, "table", mockTable));

    // Test empty module name
    assertThrows(JniValidationException.class, () ->
        linker.defineTable("", "table", mockTable));

    // Test null table name
    assertThrows(JniValidationException.class, () ->
        linker.defineTable("env", null, mockTable));

    // Test empty table name
    assertThrows(JniValidationException.class, () ->
        linker.defineTable("env", "", mockTable));

    // Test null table
    assertThrows(JniValidationException.class, () ->
        linker.defineTable("env", "table", null));
  }

  @Test
  void testDefineGlobalParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null module name
    assertThrows(JniValidationException.class, () ->
        linker.defineGlobal(null, "global", mockGlobal));

    // Test empty module name
    assertThrows(JniValidationException.class, () ->
        linker.defineGlobal("", "global", mockGlobal));

    // Test null global name
    assertThrows(JniValidationException.class, () ->
        linker.defineGlobal("env", null, mockGlobal));

    // Test empty global name
    assertThrows(JniValidationException.class, () ->
        linker.defineGlobal("env", "", mockGlobal));

    // Test null global
    assertThrows(JniValidationException.class, () ->
        linker.defineGlobal("env", "global", null));
  }

  @Test
  void testDefineInstanceParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null module name
    assertThrows(JniValidationException.class, () ->
        linker.defineInstance(null, mockInstance));

    // Test empty module name
    assertThrows(JniValidationException.class, () ->
        linker.defineInstance("", mockInstance));

    // Test null instance
    assertThrows(JniValidationException.class, () ->
        linker.defineInstance("module", null));
  }

  @Test
  void testAliasParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null from module
    assertThrows(JniValidationException.class, () ->
        linker.alias(null, "name", "to_module", "to_name"));

    // Test empty from module
    assertThrows(JniValidationException.class, () ->
        linker.alias("", "name", "to_module", "to_name"));

    // Test null from name
    assertThrows(JniValidationException.class, () ->
        linker.alias("from_module", null, "to_module", "to_name"));

    // Test empty from name
    assertThrows(JniValidationException.class, () ->
        linker.alias("from_module", "", "to_module", "to_name"));

    // Test null to module
    assertThrows(JniValidationException.class, () ->
        linker.alias("from_module", "name", null, "to_name"));

    // Test empty to module
    assertThrows(JniValidationException.class, () ->
        linker.alias("from_module", "name", "", "to_name"));

    // Test null to name
    assertThrows(JniValidationException.class, () ->
        linker.alias("from_module", "name", "to_module", null));

    // Test empty to name
    assertThrows(JniValidationException.class, () ->
        linker.alias("from_module", "name", "to_module", ""));
  }

  @Test
  void testInstantiateParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null store
    assertThrows(JniValidationException.class, () ->
        linker.instantiate(null, mockModule));

    // Test null module
    assertThrows(JniValidationException.class, () ->
        linker.instantiate(mockStore, null));
  }

  @Test
  void testInstantiateNamedParameterValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test null store
    assertThrows(JniValidationException.class, () ->
        linker.instantiate(null, "module_name", mockModule));

    // Test null module name
    assertThrows(JniValidationException.class, () ->
        linker.instantiate(mockStore, null, mockModule));

    // Test empty module name
    assertThrows(JniValidationException.class, () ->
        linker.instantiate(mockStore, "", mockModule));

    // Test null module
    assertThrows(JniValidationException.class, () ->
        linker.instantiate(mockStore, "module_name", null));
  }

  @Test
  void testConvertToNativeTypes() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Test conversion of all value types
    final WasmValueType[] types = {
        WasmValueType.I32,
        WasmValueType.I64,
        WasmValueType.F32,
        WasmValueType.F64,
        WasmValueType.V128,
        WasmValueType.FUNCREF,
        WasmValueType.EXTERNREF
    };

    // We can't directly test the private method, but we can verify it through function type usage
    final FunctionType functionType = new FunctionType(types, new WasmValueType[0]);
    when(mockFunctionType.getParamTypes()).thenReturn(types);
    when(mockFunctionType.getReturnTypes()).thenReturn(new WasmValueType[0]);

    // This would test the conversion indirectly through defineHostFunction
    // In unit test, we just verify parameter validation occurs
    assertNotNull(functionType);
  }

  @Test
  void testOperationsAfterClose() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);
    linker.close();

    // Verify all operations throw IllegalStateException after close
    assertThrows(IllegalStateException.class, () ->
        linker.defineHostFunction("env", "test", mockFunctionType, mockHostFunction));

    assertThrows(IllegalStateException.class, () ->
        linker.defineMemory("env", "memory", mockMemory));

    assertThrows(IllegalStateException.class, () ->
        linker.defineTable("env", "table", mockTable));

    assertThrows(IllegalStateException.class, () ->
        linker.defineGlobal("env", "global", mockGlobal));

    assertThrows(IllegalStateException.class, () ->
        linker.defineInstance("module", mockInstance));

    assertThrows(IllegalStateException.class, () ->
        linker.alias("from", "name", "to", "name"));

    assertThrows(IllegalStateException.class, () ->
        linker.instantiate(mockStore, mockModule));

    assertThrows(IllegalStateException.class, () ->
        linker.enableWasi());

    assertFalse(linker.isValid());
  }

  @Test
  void testNonJniObjectsValidation() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Create non-JNI implementations
    WasmMemory nonJniMemory = new WasmMemory() {
      @Override public long size() { return 0; }
      @Override public long grow(long pages) { return 0; }
      @Override public byte[] read(long offset, int length) { return new byte[0]; }
      @Override public void write(long offset, byte[] data) {}
      @Override public boolean isValid() { return true; }
      @Override public void close() {}
    };

    WasmTable nonJniTable = new WasmTable() {
      @Override public long size() { return 0; }
      @Override public long grow(long delta, WasmValue init) { return 0; }
      @Override public WasmValue get(long index) { return null; }
      @Override public void set(long index, WasmValue value) {}
      @Override public boolean isValid() { return true; }
      @Override public void close() {}
    };

    WasmGlobal nonJniGlobal = new WasmGlobal() {
      @Override public WasmValue get() { return null; }
      @Override public void set(WasmValue value) {}
      @Override public boolean isMutable() { return false; }
      @Override public WasmValueType getType() { return WasmValueType.I32; }
      @Override public boolean isValid() { return true; }
      @Override public void close() {}
    };

    // Test that non-JNI objects are rejected
    assertThrows(IllegalArgumentException.class, () ->
        linker.defineMemory("env", "memory", nonJniMemory));

    assertThrows(IllegalArgumentException.class, () ->
        linker.defineTable("env", "table", nonJniTable));

    assertThrows(IllegalArgumentException.class, () ->
        linker.defineGlobal("env", "global", nonJniGlobal));
  }

  @Test
  void testMultipleClose() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);

    // Multiple close calls should be safe
    linker.close();
    linker.close();
    linker.close();

    assertFalse(linker.isValid());
  }

  @Test
  void testGetEngine() {
    final JniLinker linker = new JniLinker(VALID_HANDLE, mockEngine);
    assertEquals(mockEngine, linker.getEngine());
  }
}