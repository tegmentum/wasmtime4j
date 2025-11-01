package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniModule}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly module operations is tested in integration
 * tests.
 */
class JniModuleTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long ZERO_HANDLE = 0L;

  private JniEngine testEngine;
  private JniStore testStore;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
  }

  @Test
  void testConstructorWithValidHandleAndEngine() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module);
    assertEquals(VALID_HANDLE, module.getNativeHandle());
    assertEquals(testEngine, module.getEngine());
    assertTrue(module.isValid());
  }

  @Test
  void testConstructorWithZeroHandle() {
    final JniModule module = new JniModule(ZERO_HANDLE, testEngine);

    assertNotNull(module);
    assertEquals(ZERO_HANDLE, module.getNativeHandle());
    assertEquals(testEngine, module.getEngine());
    assertFalse(module.isValid());
  }

  @Test
  void testConstructorWithNullEngine() {
    final JniModule module = new JniModule(VALID_HANDLE, null);

    assertNotNull(module);
    assertEquals(VALID_HANDLE, module.getNativeHandle());
    assertThat(module.getEngine()).isNull();
  }

  @Test
  void testGetNameReturnsExpectedFormat() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final String name = module.getName();

    assertThat(name).startsWith("jni-module-");
    assertThat(name).contains(String.valueOf(VALID_HANDLE));
  }

  @Test
  void testGetEngineReturnsCorrectReference() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final Engine retrievedEngine = module.getEngine();

    assertNotNull(retrievedEngine);
    assertEquals(testEngine, retrievedEngine);
  }

  @Test
  void testIsValidWithValidHandle() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertTrue(module.isValid());
  }

  @Test
  void testIsValidWithZeroHandle() {
    final JniModule module = new JniModule(ZERO_HANDLE, testEngine);

    assertFalse(module.isValid());
  }

  @Test
  void testInstantiateWithNullStore() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> module.instantiate(null));

    assertThat(exception.getMessage()).contains("store");
    assertThat(exception.getMessage()).contains("cannot be null");
  }

  @Test
  void testInstantiateWithNonJniStore() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);
    // Create a Store that is not a JniStore to test type checking
    final Store nonJniStore =
        new Store() {
          @Override
          public Engine getEngine() {
            return null;
          }

          @Override
          public Object getData() {
            return null;
          }

          @Override
          public void setData(Object data) {}

          @Override
          public void setFuel(long fuel) {}

          @Override
          public long getFuel() {
            return 0;
          }

          @Override
          public void setEpochDeadline(long ticks) {}

          @Override
          public void addFuel(long fuel) {}

          @Override
          public long consumeFuel(long fuel) {
            return 0;
          }

          @Override
          public long getRemainingFuel() {
            return 0;
          }

          public void incrementEpoch() {}

          public void setMemoryLimit(long bytes) {}

          public void setTableElementLimit(long elements) {}

          public void setInstanceLimit(int count) {}

          @Override
          public ai.tegmentum.wasmtime4j.WasmFunction createHostFunction(
              String name,
              ai.tegmentum.wasmtime4j.FunctionType functionType,
              ai.tegmentum.wasmtime4j.HostFunction implementation) {
            return null;
          }

          @Override
          public ai.tegmentum.wasmtime4j.WasmGlobal createGlobal(
              ai.tegmentum.wasmtime4j.WasmValueType valueType,
              boolean isMutable,
              ai.tegmentum.wasmtime4j.WasmValue initialValue) {
            return null;
          }

          @Override
          public ai.tegmentum.wasmtime4j.WasmTable createTable(
              ai.tegmentum.wasmtime4j.WasmValueType elementType, int initialSize, int maxSize) {
            return null;
          }

          @Override
          public ai.tegmentum.wasmtime4j.WasmMemory createMemory(int initialPages, int maxPages) {
            return null;
          }

          @Override
          public ai.tegmentum.wasmtime4j.FunctionReference createFunctionReference(
              ai.tegmentum.wasmtime4j.HostFunction implementation,
              ai.tegmentum.wasmtime4j.FunctionType functionType) {
            return null;
          }

          @Override
          public ai.tegmentum.wasmtime4j.FunctionReference createFunctionReference(
              ai.tegmentum.wasmtime4j.WasmFunction function) {
            return null;
          }

          @Override
          public ai.tegmentum.wasmtime4j.CallbackRegistry getCallbackRegistry() {
            return null;
          }

          @Override
          public ai.tegmentum.wasmtime4j.Instance createInstance(
              ai.tegmentum.wasmtime4j.Module module) {
            return null;
          }

          @Override
          public boolean isValid() {
            return false;
          }

          @Override
          public void close() {}

          @Override
          public long getExecutionCount() {
            return 0;
          }

          @Override
          public long getTotalExecutionTimeMicros() {
            return 0;
          }

          @Override
          public long getTotalFuelConsumed() {
            return 0;
          }
        };

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> module.instantiate(nonJniStore));

    assertThat(exception.getMessage()).contains("store");
    assertThat(exception.getMessage()).contains("must be a JniStore instance");
  }

  @Test
  void testInstantiateWithImportsNullStore() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // ImportMap validation happens before store null check
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> module.instantiate(null, null));

    // Could be either "store" or "imports" that fails first
    assertTrue(
        exception.getMessage().contains("store") || exception.getMessage().contains("imports"));
  }

  @Test
  void testInstantiateWithImportsNullImports() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> module.instantiate(testStore, null));

    assertThat(exception.getMessage()).contains("imports");
    assertThat(exception.getMessage()).contains("cannot be null");
  }

  @Test
  void testGetImportsReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getImports());
    assertTrue(module.getImports().isEmpty());
  }

  @Test
  void testGetExportsReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getExports());
    assertTrue(module.getExports().isEmpty());
  }

  @Test
  void testGetCustomSectionsReturnsEmptyMap() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getCustomSections());
    assertTrue(module.getCustomSections().isEmpty());
  }

  @Test
  void testGetGlobalTypesReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getGlobalTypes());
    assertTrue(module.getGlobalTypes().isEmpty());
  }

  @Test
  void testGetTableTypesReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getTableTypes());
    assertTrue(module.getTableTypes().isEmpty());
  }

  @Test
  void testGetMemoryTypesReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getMemoryTypes());
    assertTrue(module.getMemoryTypes().isEmpty());
  }

  @Test
  void testGetFunctionTypesReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getFunctionTypes());
    assertTrue(module.getFunctionTypes().isEmpty());
  }

  @Test
  void testGetExportDescriptorsReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getExportDescriptors());
    assertTrue(module.getExportDescriptors().isEmpty());
  }

  @Test
  void testGetImportDescriptorsReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getImportDescriptors());
    assertTrue(module.getImportDescriptors().isEmpty());
  }

  @Test
  void testGetModuleImportsReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getModuleImports());
    assertTrue(module.getModuleImports().isEmpty());
  }

  @Test
  void testGetModuleExportsReturnsEmptyList() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertNotNull(module.getModuleExports());
    assertTrue(module.getModuleExports().isEmpty());
  }

  @Test
  void testGetFunctionTypeReturnsEmpty() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertFalse(module.getFunctionType("test").isPresent());
  }

  @Test
  void testGetGlobalTypeReturnsEmpty() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertFalse(module.getGlobalType("test").isPresent());
  }

  @Test
  void testGetMemoryTypeReturnsEmpty() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertFalse(module.getMemoryType("test").isPresent());
  }

  @Test
  void testGetTableTypeReturnsEmpty() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertFalse(module.getTableType("test").isPresent());
  }

  @Test
  void testHasExportReturnsFalse() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertFalse(module.hasExport("test"));
  }

  @Test
  void testHasImportReturnsFalse() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertFalse(module.hasImport("module", "field"));
  }

  @Test
  void testValidateImportsWithNull() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // Default implementation returns true even for null (not implemented yet)
    assertTrue(module.validateImports(null));
  }

  @Test
  void testSerializeReturnsEmptyArray() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    final byte[] serialized = module.serialize();

    assertNotNull(serialized);
    assertEquals(0, serialized.length);
  }

  // Note: Tests that call close() are disabled in unit tests since they require native library
  // These are tested in integration tests instead

  @Test
  void testModuleLifecycleState() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // Verify initial state
    assertNotNull(module);
    assertTrue(module.isValid());
    assertEquals(VALID_HANDLE, module.getNativeHandle());
    assertEquals(testEngine, module.getEngine());

    // Note: Cannot call close() in unit test - requires native library
    // Integration tests verify close() behavior and idempotency
  }

  @Test
  void testResourceManagementState() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // Verify module is in valid state
    assertNotNull(module);
    assertTrue(module.isValid());

    // Note: Cannot test try-with-resources in unit test - requires native library
    // Integration tests verify automatic resource cleanup
  }

  @Test
  void testGetNativeHandleReturnsCorrectValue() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    assertEquals(VALID_HANDLE, module.getNativeHandle());
  }

  @Test
  void testMultipleModulesWithDifferentHandles() {
    final JniModule module1 = new JniModule(0x1111L, testEngine);
    final JniModule module2 = new JniModule(0x2222L, testEngine);

    assertEquals(0x1111L, module1.getNativeHandle());
    assertEquals(0x2222L, module2.getNativeHandle());
    assertTrue(module1.isValid());
    assertTrue(module2.isValid());
  }

  @Test
  void testModuleStateAfterConstruction() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // Verify initial state
    assertTrue(module.isValid());
    assertEquals(testEngine, module.getEngine());
    assertThat(module.getName()).startsWith("jni-module-");
    assertNotNull(module.getImports());
    assertNotNull(module.getExports());
  }

  @Test
  void testDefensiveProgrammingForTypeQueries() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // These should not throw, even with null/invalid names
    assertFalse(module.getFunctionType(null).isPresent());
    assertFalse(module.getGlobalType(null).isPresent());
    assertFalse(module.getMemoryType(null).isPresent());
    assertFalse(module.getTableType(null).isPresent());
  }

  @Test
  void testOperationsWithValidHandleDoNotThrow() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // These operations should not throw with valid handle
    assertNotNull(module);
    module.getName();
    module.getImports();
    module.getExports();
    module.getCustomSections();
    module.getGlobalTypes();
    module.getTableTypes();
    module.getMemoryTypes();
    module.getFunctionTypes();
    module.hasExport("test");
    module.hasImport("module", "field");
    module.serialize();
  }

  @Test
  void testOperationsWithZeroHandleDoNotCrash() {
    final JniModule module = new JniModule(ZERO_HANDLE, testEngine);

    // These operations should not crash with zero handle
    assertFalse(module.isValid());
    assertNotNull(module.getName());
    assertNotNull(module.getImports());
    assertNotNull(module.getExports());
    assertNotNull(module.serialize());
  }

  @Test
  void testValidationPreventsCrashOnInvalidInput() {
    final JniModule module = new JniModule(VALID_HANDLE, testEngine);

    // All these should throw exceptions, not crash the JVM
    assertThrows(IllegalArgumentException.class, () -> module.instantiate(null));
    assertThrows(IllegalArgumentException.class, () -> module.instantiate(testStore, null));
    assertThrows(IllegalArgumentException.class, () -> module.instantiate(null, null));
  }
}
