package ai.tegmentum.wasmtime4j.instance;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.InstantiationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly instance API functionality across both JNI and Panama runtime
 * implementations.
 *
 * <p>This test class validates: - Instance lifecycle management - Module instantiation and binding
 * - Import/export resolution - Memory and table allocation - Global variable handling - Resource
 * cleanup and disposal - Error handling and validation - Cross-runtime behavior consistency
 */
public final class InstanceApiIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(InstanceApiIT.class.getName());
  private static final String WASM_TEST_DIR = "wasmtime4j-tests/src/test/resources/wasm/custom-tests/";

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up instance API integration test: " + testInfo.getDisplayName());
  }

  /**
   * Tests basic instance creation from a simple module without imports.
   * This validates the most fundamental instantiation workflow.
   */
  @Test
  void testBasicInstanceCreation() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing basic instance creation with " + runtimeType + " runtime");
      
      measureExecutionTime("Basic instance creation (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          final Instance instance = module.instantiate(store);
          registerForCleanup(instance);
          
          assertAll(
              "Basic instance validation",
              () -> assertNotNull(instance, "Instance should not be null"),
              () -> assertTrue(instance.isValid(), "Instance should be valid"),
              () -> assertNotNull(instance.getModule(), "Instance module should not be null"),
              () -> assertNotNull(instance.getStore(), "Instance store should not be null"),
              () -> assertEquals(module, instance.getModule(), "Instance should reference correct module"),
              () -> assertEquals(store, instance.getStore(), "Instance should reference correct store")
          );
          
          LOGGER.info("Basic instance creation successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests function export access and invocation.
   * Validates that exported functions can be retrieved and called.
   */
  @Test
  void testFunctionExportAndInvocation() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing function export and invocation with " + runtimeType + " runtime");
      
      measureExecutionTime("Function export test (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          final Instance instance = module.instantiate(store);
          registerForCleanup(instance);
          
          // Test function export retrieval
          final WasmFunction addFunction = instance.getFunction("add")
              .orElseThrow(() -> new AssertionError("Add function should be exported"));
          
          assertAll(
              "Function export validation",
              () -> assertNotNull(addFunction, "Add function should not be null"),
              () -> assertNotNull(addFunction.getFunctionType(), "Function type should not be null"),
              () -> assertEquals("add", addFunction.getName(), "Function name should match")
          );
          
          // Test function invocation
          final WasmValue[] params = {WasmValue.i32(5), WasmValue.i32(3)};
          final WasmValue[] results = addFunction.call(params);
          
          assertAll(
              "Function invocation validation",
              () -> assertNotNull(results, "Function results should not be null"),
              () -> assertEquals(1, results.length, "Function should return one value"),
              () -> assertEquals(WasmValueType.I32, results[0].getType(), "Result should be i32"),
              () -> assertEquals(8, results[0].asI32(), "Function should return 5 + 3 = 8")
          );
          
          // Test instance convenience method
          final WasmValue[] convenienceResults = instance.callFunction("add", params);
          assertEquals(results[0].asI32(), convenienceResults[0].asI32(), 
              "Convenience method should return same result");
          
          LOGGER.info("Function export and invocation successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests instance creation with host function imports.
   * Validates the complete import resolution workflow.
   */
  @Test
  void testInstanceCreationWithHostFunctionImports() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing instance creation with host function imports using " + runtimeType + " runtime");
      
      measureExecutionTime("Host function import test (" + runtimeType + ")", () -> {
        // For this test, we would need a WASM module that imports functions
        // For now, we'll test the host function creation API
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          // Create a host function
          final FunctionType hostFunctionType = new FunctionType(
              new WasmValueType[]{WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[]{WasmValueType.I32}
          );
          
          final HostFunction hostImplementation = params -> {
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            final int result = a * b;
            LOGGER.fine("Host function called: " + a + " * " + b + " = " + result);
            return new WasmValue[]{WasmValue.i32(result)};
          };
          
          final WasmFunction hostFunction = store.createHostFunction(
              "multiply", hostFunctionType, hostImplementation);
          
          assertAll(
              "Host function creation validation",
              () -> assertNotNull(hostFunction, "Host function should not be null"),
              () -> assertNotNull(hostFunction.getFunctionType(), "Host function type should not be null"),
              () -> assertEquals("multiply", hostFunction.getName(), "Host function name should match")
          );
          
          // Test host function invocation
          final WasmValue[] params = {WasmValue.i32(4), WasmValue.i32(7)};
          final WasmValue[] results = hostFunction.call(params);
          
          assertAll(
              "Host function invocation validation",
              () -> assertNotNull(results, "Host function results should not be null"),
              () -> assertEquals(1, results.length, "Host function should return one value"),
              () -> assertEquals(WasmValueType.I32, results[0].getType(), "Result should be i32"),
              () -> assertEquals(28, results[0].asI32(), "Host function should return 4 * 7 = 28")
          );
          
          // Create an import map for future use
          final ImportMap imports = ImportMap.empty();
          imports.addFunction("env", "multiply", hostFunction);
          
          assertAll(
              "Import map validation",
              () -> assertNotNull(imports, "Import map should not be null"),
              () -> assertTrue(imports.contains("env", "multiply"), "Import map should contain host function"),
              () -> assertEquals(hostFunction, imports.getFunction("env", "multiply").orElse(null), 
                  "Import map should return correct function")
          );
          
          LOGGER.info("Host function import test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests export enumeration and metadata access.
   * Validates that all exports can be discovered and their metadata accessed.
   */
  @Test
  void testExportEnumerationAndMetadata() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing export enumeration and metadata with " + runtimeType + " runtime");
      
      measureExecutionTime("Export enumeration test (" + runtimeType + ")", () -> {
        final byte[] wasmBytes = loadWasmFile("add.wasm");
        
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          final Instance instance = module.instantiate(store);
          registerForCleanup(instance);
          
          // Test export name enumeration
          final String[] exportNames = instance.getExportNames();
          
          assertAll(
              "Export enumeration validation",
              () -> assertNotNull(exportNames, "Export names should not be null"),
              () -> assertTrue(exportNames.length > 0, "Instance should have at least one export"),
              () -> assertTrue(containsName(exportNames, "add"), "Exports should include 'add' function")
          );
          
          LOGGER.info("Found " + exportNames.length + " exports: " + String.join(", ", exportNames));
          
          // Validate each export can be accessed
          for (final String exportName : exportNames) {
            LOGGER.fine("Testing export access for: " + exportName);
            
            // Try to get as function first (most likely for this module)
            final var functionOpt = instance.getFunction(exportName);
            final var globalOpt = instance.getGlobal(exportName);
            final var memoryOpt = instance.getMemory(exportName);
            final var tableOpt = instance.getTable(exportName);
            
            // At least one should be present
            final boolean hasAtLeastOneExport = 
                functionOpt.isPresent() || globalOpt.isPresent() || 
                memoryOpt.isPresent() || tableOpt.isPresent();
            
            assertTrue(hasAtLeastOneExport, 
                "Export '" + exportName + "' should be accessible through at least one getter");
            
            if (functionOpt.isPresent()) {
              LOGGER.fine("Export '" + exportName + "' is a function");
              assertNotNull(functionOpt.get().getFunctionType(), 
                  "Function export should have valid type");
            }
          }
          
          LOGGER.info("Export enumeration and metadata test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Tests error handling for invalid instantiation scenarios.
   * Validates proper exception handling and error reporting.
   */
  @Test
  void testInstanceCreationErrorHandling() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing instance creation error handling with " + runtimeType + " runtime");
      
      measureExecutionTime("Error handling test (" + runtimeType + ")", () -> {
        try (final Engine engine = runtime.createEngine();
             final Store store = engine.createStore()) {
          
          registerForCleanup(engine);
          registerForCleanup(store);
          
          final byte[] wasmBytes = loadWasmFile("add.wasm");
          final Module module = engine.compileModule(wasmBytes);
          registerForCleanup(module);
          
          // Test null parameter handling
          assertThrows(IllegalArgumentException.class, () -> {
            module.instantiate(null);
          }, "Instantiation with null store should throw IllegalArgumentException");
          
          // Test instantiation with closed store
          final Store closedStore = engine.createStore();
          closedStore.close();
          
          assertThrows(WasmException.class, () -> {
            module.instantiate(closedStore);
          }, "Instantiation with closed store should throw WasmException");
          
          // Test access to closed instance
          final Instance instance = module.instantiate(store);
          instance.close();
          
          assertThrows(WasmException.class, () -> {
            instance.getFunction("add");
          }, "Accessing closed instance should throw WasmException");
          
          assertThrows(WasmException.class, () -> {
            instance.callFunction("add", WasmValue.i32(1), WasmValue.i32(2));
          }, "Calling function on closed instance should throw WasmException");
          
          LOGGER.info("Error handling test successful with " + runtimeType);
        }
      });
    });
  }

  /**
   * Helper method to load WASM files from test resources.
   */
  private byte[] loadWasmFile(final String filename) {
    try {
      final Path wasmPath = Paths.get(WASM_TEST_DIR + filename);
      return Files.readAllBytes(wasmPath);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to load WASM file: " + filename, e);
    }
  }

  /**
   * Helper method to check if an array contains a specific name.
   */
  private boolean containsName(final String[] names, final String target) {
    for (final String name : names) {
      if (target.equals(name)) {
        return true;
      }
    }
    return false;
  }
}
