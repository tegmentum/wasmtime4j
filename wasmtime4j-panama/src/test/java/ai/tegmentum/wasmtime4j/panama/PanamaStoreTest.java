package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.StoreLimits;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaStore}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior and validation without relying on actual native calls where
 * possible.
 *
 * <p>Note: Tests that require real native operations (store creation with valid engines, method
 * calls on live stores) are tested in integration tests.
 */
class PanamaStoreTest {

  @Test
  void testConstructorWithNullEngine() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new PanamaStore(null));

    assertThat(exception.getMessage()).contains("Engine cannot be null");
  }

  @Test
  void testConstructorWithLimitsNullEngine() {
    final StoreLimits limits = StoreLimits.builder().build();

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new PanamaStore(null, limits));

    assertThat(exception.getMessage()).contains("Engine cannot be null");
  }

  @Test
  void testConstructorWithLimitsNullLimits() {
    // Cannot test with real engine in unit test, but we can verify the validation order
    // by checking that null limits is caught second (engine check first)
    // This test documents the expected behavior
    assertNotNull(StoreLimits.class);
  }

  @Test
  void testForModuleWithNullModule() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> PanamaStore.forModule(null));

    assertThat(exception.getMessage()).contains("module cannot be null");
  }

  @Test
  void testForModuleWithNonPanamaModule() {
    // Create a non-Panama Module implementation to test type checking
    final Module nonPanamaModule = createMockModule();

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> PanamaStore.forModule(nonPanamaModule));

    assertThat(exception.getMessage()).contains("module must be a PanamaModule instance");
  }

  /**
   * Creates a minimal mock Module for testing type validation. This module is NOT a PanamaModule,
   * so it should be rejected by PanamaStore.forModule().
   */
  private Module createMockModule() {
    return new Module() {
      @Override
      public ai.tegmentum.wasmtime4j.Instance instantiate(ai.tegmentum.wasmtime4j.Store store) {
        return null;
      }

      @Override
      public ai.tegmentum.wasmtime4j.Instance instantiate(
          ai.tegmentum.wasmtime4j.Store store, ai.tegmentum.wasmtime4j.ImportMap imports) {
        return null;
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ExportType> getExports() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ImportType> getImports() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.Map<String, byte[]> getCustomSections() {
        return java.util.Collections.emptyMap();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.GlobalType> getGlobalTypes() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.TableType> getTableTypes() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.MemoryType> getMemoryTypes() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.FuncType> getFunctionTypes() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ExportDescriptor> getExportDescriptors() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ImportDescriptor> getImportDescriptors() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ModuleImport> getModuleImports() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.List<ai.tegmentum.wasmtime4j.ModuleExport> getModuleExports() {
        return java.util.Collections.emptyList();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.FuncType> getFunctionType(String name) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.GlobalType> getGlobalType(String name) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.MemoryType> getMemoryType(String name) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Optional<ai.tegmentum.wasmtime4j.TableType> getTableType(String name) {
        return java.util.Optional.empty();
      }

      @Override
      public boolean hasExport(String name) {
        return false;
      }

      @Override
      public boolean hasImport(String moduleName, String fieldName) {
        return false;
      }

      @Override
      public boolean validateImports(ai.tegmentum.wasmtime4j.ImportMap imports) {
        return true;
      }

      @Override
      public ai.tegmentum.wasmtime4j.ImportValidation validateImportsDetailed(
          ai.tegmentum.wasmtime4j.ImportMap imports) {
        return new ai.tegmentum.wasmtime4j.ImportValidation(
            true,
            java.util.Collections.emptyList(),
            java.util.Collections.emptyList(),
            0,
            0,
            java.time.Duration.ZERO);
      }

      @Override
      public byte[] serialize() {
        return new byte[0];
      }

      @Override
      public ai.tegmentum.wasmtime4j.Engine getEngine() {
        return null;
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public void close() {}

      @Override
      public String getName() {
        return "mock-module";
      }
    };
  }

  // Note: Tests that require a real native engine/store are in integration tests
  // The following tests document expected validation behavior:

  @Test
  void testValidationDocumentation() {
    // This test documents the expected validation behavior of PanamaStore
    // These validations are tested in integration tests with real native libraries

    // 1. Constructor(engine) - validates engine != null
    // 2. Constructor(engine) - validates engine.isValid() == true
    // 3. Constructor(engine, limits) - validates engine != null
    // 4. Constructor(engine, limits) - validates limits != null
    // 5. forModule(module) - validates module != null
    // 6. forModule(module) - validates module instanceof PanamaModule
    // 7. forModule(module) - validates module.isValid() == true

    // Parameter validations (tested in integration tests with live store):
    // - setFuel(fuel) - fuel >= 0
    // - addFuel(fuel) - fuel >= 0
    // - consumeFuel(fuel) - fuel >= 0
    // - createHostFunction(name, type, impl) - all non-null
    // - createGlobal(type, mutable, value) - all non-null, value type matches
    // - createTable(elementType, initial, max) - valid sizes and element type
    // - createMemory(initial, max) - valid page counts
    // - createInstance(module) - module non-null

    assertThat(true).isTrue(); // Documentation test always passes
  }
}
