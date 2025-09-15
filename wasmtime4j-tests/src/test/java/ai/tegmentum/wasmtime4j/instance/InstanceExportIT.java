package ai.tegmentum.wasmtime4j.instance;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly instance export binding and access.
 *
 * <p>This test class validates: - Function export access and invocation - Global export access and
 * manipulation - Memory export access and operations - Table export access and operations - Export
 * enumeration and metadata - Type validation for exports - Cross-runtime consistency for export
 * binding
 */
public final class InstanceExportIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(InstanceExportIT.class.getName());
  private static final String WASM_TEST_DIR =
      "wasmtime4j-tests/src/test/resources/wasm/custom-tests/";

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    LOGGER.info("Setting up instance export test: " + testInfo.getDisplayName());
  }

  /**
   * Tests function export access, validation, and invocation. Validates that exported functions can
   * be retrieved and called correctly.
   */
  @Test
  void testFunctionExportAccess() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing function export access with " + runtimeType + " runtime");

          measureExecutionTime(
              "Function export test (" + runtimeType + ")",
              () -> {
                try {
                  final byte[] wasmBytes = loadWasmFile("add.wasm");

                  try (final Engine engine = runtime.createEngine();
                      final Store store = engine.createStore()) {

                    registerForCleanup(engine);
                    registerForCleanup(store);

                    final Module module = engine.compileModule(wasmBytes);
                    registerForCleanup(module);

                    final Instance instance = module.instantiate(store);
                    registerForCleanup(instance);

                    // Test function retrieval
                    final Optional<WasmFunction> addFunctionOpt = instance.getFunction("add");
                    assertTrue(addFunctionOpt.isPresent(), "Add function should be present");

                    final WasmFunction addFunction = addFunctionOpt.get();

                    assertAll(
                        "Function export validation",
                        () -> assertNotNull(addFunction, "Function should not be null"),
                        () ->
                            assertNotNull(
                                addFunction.getFunctionType(), "Function type should not be null"),
                        () ->
                            assertEquals(
                                "add", addFunction.getName(), "Function name should match"));

                    // Test function type details
                    final var functionType = addFunction.getFunctionType();
                    final WasmValueType[] paramTypes = functionType.getParamTypes();
                    final WasmValueType[] returnTypes = functionType.getReturnTypes();

                    assertAll(
                        "Function type validation",
                        () -> assertNotNull(paramTypes, "Parameter types should not be null"),
                        () -> assertNotNull(returnTypes, "Return types should not be null"),
                        () ->
                            assertEquals(2, paramTypes.length, "Function should have 2 parameters"),
                        () ->
                            assertEquals(
                                1, returnTypes.length, "Function should have 1 return value"),
                        () ->
                            assertEquals(
                                WasmValueType.I32, paramTypes[0], "First parameter should be i32"),
                        () ->
                            assertEquals(
                                WasmValueType.I32, paramTypes[1], "Second parameter should be i32"),
                        () ->
                            assertEquals(
                                WasmValueType.I32, returnTypes[0], "Return value should be i32"));

                    // Test function invocation with various inputs
                    final int[][] testCases = {
                      {0, 0, 0},
                      {1, 2, 3},
                      {10, 20, 30},
                      {-5, 15, 10},
                      {Integer.MAX_VALUE, 0, Integer.MAX_VALUE},
                      {Integer.MIN_VALUE, 0, Integer.MIN_VALUE}
                    };

                    for (final int[] testCase : testCases) {
                      final int a = testCase[0];
                      final int b = testCase[1];
                      final int expected = testCase[2];

                      final WasmValue[] params = {WasmValue.i32(a), WasmValue.i32(b)};
                      final WasmValue[] results = addFunction.call(params);

                      assertAll(
                          "Function call validation for " + a + " + " + b,
                          () -> assertNotNull(results, "Results should not be null"),
                          () -> assertEquals(1, results.length, "Should return one result"),
                          () ->
                              assertEquals(
                                  WasmValueType.I32,
                                  results[0].getType(),
                                  "Result type should be i32"),
                          () ->
                              assertEquals(
                                  expected, results[0].asI32(), "Result value should be correct"));
                    }

                    // Test non-existent function access
                    final Optional<WasmFunction> nonExistentOpt =
                        instance.getFunction("nonexistent");
                    assertTrue(
                        nonExistentOpt.isEmpty(),
                        "Non-existent function should return empty Optional");

                    LOGGER.info("Function export access test successful with " + runtimeType);
                  }
                } catch (Exception e) {
                  throw new RuntimeException("Function export test failed for " + runtimeType, e);
                }
              });
        });
  }

  /**
   * Tests memory export access and basic operations. Note: The add.wasm module might not export
   * memory, so this test includes error handling.
   */
  @Test
  void testMemoryExportAccess() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing memory export access with " + runtimeType + " runtime");

          measureExecutionTime(
              "Memory export test (" + runtimeType + ")",
              () -> {
                try {
                  final byte[] wasmBytes = loadWasmFile("add.wasm");

                  try (final Engine engine = runtime.createEngine();
                      final Store store = engine.createStore()) {

                    registerForCleanup(engine);
                    registerForCleanup(store);

                    final Module module = engine.compileModule(wasmBytes);
                    registerForCleanup(module);

                    final Instance instance = module.instantiate(store);
                    registerForCleanup(instance);

                    // Try to access memory export (may not be present in add.wasm)
                    final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
                    if (memoryOpt.isPresent()) {
                      final WasmMemory memory = memoryOpt.get();

                      assertAll(
                          "Memory export validation",
                          () -> assertNotNull(memory, "Memory should not be null"),
                          () ->
                              assertTrue(memory.size() >= 0, "Memory size should be non-negative"));

                      LOGGER.info("Memory export found and validated with " + runtimeType);
                    } else {
                      LOGGER.info(
                          "No memory export found in test module - this is expected for add.wasm");
                    }

                    // Test default memory access
                    final Optional<WasmMemory> defaultMemoryOpt = instance.getDefaultMemory();
                    if (defaultMemoryOpt.isPresent()) {
                      final WasmMemory defaultMemory = defaultMemoryOpt.get();
                      assertNotNull(defaultMemory, "Default memory should not be null");
                      LOGGER.info("Default memory found with " + runtimeType);
                    } else {
                      LOGGER.info("No default memory found - this is expected for add.wasm");
                    }

                    // Test non-existent memory access
                    final Optional<WasmMemory> nonExistentOpt = instance.getMemory("nonexistent");
                    assertTrue(
                        nonExistentOpt.isEmpty(),
                        "Non-existent memory should return empty Optional");

                    LOGGER.info("Memory export access test completed with " + runtimeType);
                  }
                } catch (Exception e) {
                  throw new RuntimeException("Memory export test failed for " + runtimeType, e);
                }
              });
        });
  }

  /**
   * Tests global export access and value retrieval. Note: The add.wasm module might not export
   * globals, so this test includes error handling.
   */
  @Test
  void testGlobalExportAccess() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing global export access with " + runtimeType + " runtime");

          measureExecutionTime(
              "Global export test (" + runtimeType + ")",
              () -> {
                try {
                  final byte[] wasmBytes = loadWasmFile("add.wasm");

                  try (final Engine engine = runtime.createEngine();
                      final Store store = engine.createStore()) {

                    registerForCleanup(engine);
                    registerForCleanup(store);

                    final Module module = engine.compileModule(wasmBytes);
                    registerForCleanup(module);

                    final Instance instance = module.instantiate(store);
                    registerForCleanup(instance);

                    // Enumerate all exports to see if any globals are present
                    final String[] exportNames = instance.getExportNames();
                    boolean foundGlobal = false;

                    for (final String exportName : exportNames) {
                      final Optional<WasmGlobal> globalOpt = instance.getGlobal(exportName);
                      if (globalOpt.isPresent()) {
                        foundGlobal = true;
                        final WasmGlobal global = globalOpt.get();

                        assertAll(
                            "Global export validation for " + exportName,
                            () -> assertNotNull(global, "Global should not be null"),
                            () -> assertNotNull(global.getType(), "Global type should not be null"),
                            () ->
                                assertNotNull(
                                    global.getValue(), "Global value should not be null"));

                        LOGGER.info(
                            "Found global export: "
                                + exportName
                                + " with type "
                                + global.getType()
                                + " and value "
                                + global.getValue());
                      }
                    }

                    if (!foundGlobal) {
                      LOGGER.info(
                          "No global exports found in test module - this is expected for add.wasm");
                    }

                    // Test non-existent global access
                    final Optional<WasmGlobal> nonExistentOpt = instance.getGlobal("nonexistent");
                    assertTrue(
                        nonExistentOpt.isEmpty(),
                        "Non-existent global should return empty Optional");

                    LOGGER.info("Global export access test completed with " + runtimeType);
                  }
                } catch (Exception e) {
                  throw new RuntimeException("Global export test failed for " + runtimeType, e);
                }
              });
        });
  }

  /**
   * Tests table export access and basic operations. Note: The add.wasm module might not export
   * tables, so this test includes error handling.
   */
  @Test
  void testTableExportAccess() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing table export access with " + runtimeType + " runtime");

          measureExecutionTime(
              "Table export test (" + runtimeType + ")",
              () -> {
                try {
                  final byte[] wasmBytes = loadWasmFile("add.wasm");

                  try (final Engine engine = runtime.createEngine();
                      final Store store = engine.createStore()) {

                    registerForCleanup(engine);
                    registerForCleanup(store);

                    final Module module = engine.compileModule(wasmBytes);
                    registerForCleanup(module);

                    final Instance instance = module.instantiate(store);
                    registerForCleanup(instance);

                    // Enumerate all exports to see if any tables are present
                    final String[] exportNames = instance.getExportNames();
                    boolean foundTable = false;

                    for (final String exportName : exportNames) {
                      final Optional<WasmTable> tableOpt = instance.getTable(exportName);
                      if (tableOpt.isPresent()) {
                        foundTable = true;
                        final WasmTable table = tableOpt.get();

                        assertAll(
                            "Table export validation for " + exportName,
                            () -> assertNotNull(table, "Table should not be null"),
                            () -> assertNotNull(table.getType(), "Table type should not be null"),
                            () ->
                                assertTrue(table.size() >= 0, "Table size should be non-negative"));

                        LOGGER.info(
                            "Found table export: "
                                + exportName
                                + " with type "
                                + table.getType()
                                + " and size "
                                + table.size());
                      }
                    }

                    if (!foundTable) {
                      LOGGER.info(
                          "No table exports found in test module - this is expected for add.wasm");
                    }

                    // Test non-existent table access
                    final Optional<WasmTable> nonExistentOpt = instance.getTable("nonexistent");
                    assertTrue(
                        nonExistentOpt.isEmpty(),
                        "Non-existent table should return empty Optional");

                    LOGGER.info("Table export access test completed with " + runtimeType);
                  }
                } catch (Exception e) {
                  throw new RuntimeException("Table export test failed for " + runtimeType, e);
                }
              });
        });
  }

  /**
   * Tests comprehensive export enumeration and metadata access. Validates that all exports can be
   * discovered and their metadata is accessible.
   */
  @Test
  void testComprehensiveExportEnumeration() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing comprehensive export enumeration with " + runtimeType + " runtime");

          measureExecutionTime(
              "Export enumeration test (" + runtimeType + ")",
              () -> {
                try {
                  final byte[] wasmBytes = loadWasmFile("add.wasm");

                  try (final Engine engine = runtime.createEngine();
                      final Store store = engine.createStore()) {

                    registerForCleanup(engine);
                    registerForCleanup(store);

                    final Module module = engine.compileModule(wasmBytes);
                    registerForCleanup(module);

                    final Instance instance = module.instantiate(store);
                    registerForCleanup(instance);

                    // Get all export names
                    final String[] exportNames = instance.getExportNames();

                    assertAll(
                        "Export names validation",
                        () -> assertNotNull(exportNames, "Export names should not be null"),
                        () ->
                            assertTrue(exportNames.length > 0, "Should have at least one export"));

                    LOGGER.info(
                        "Found "
                            + exportNames.length
                            + " exports: "
                            + String.join(", ", exportNames));

                    // Validate each export can be categorized
                    final Set<String> processedExports = new HashSet<>();
                    int functionCount = 0;
                    int globalCount = 0;
                    int memoryCount = 0;
                    int tableCount = 0;

                    for (final String exportName : exportNames) {
                      assertTrue(
                          processedExports.add(exportName),
                          "Export names should be unique: " + exportName);

                      // Try to categorize each export
                      final Optional<WasmFunction> functionOpt = instance.getFunction(exportName);
                      final Optional<WasmGlobal> globalOpt = instance.getGlobal(exportName);
                      final Optional<WasmMemory> memoryOpt = instance.getMemory(exportName);
                      final Optional<WasmTable> tableOpt = instance.getTable(exportName);

                      // Count exports by type
                      if (functionOpt.isPresent()) {
                        functionCount++;
                        final WasmFunction function = functionOpt.get();
                        assertAll(
                            "Function export " + exportName + " validation",
                            () ->
                                assertNotNull(
                                    function.getFunctionType(), "Function type should not be null"),
                            () ->
                                assertEquals(
                                    exportName, function.getName(), "Function name should match"));
                        LOGGER.fine("Export " + exportName + " is a function");
                      }

                      if (globalOpt.isPresent()) {
                        globalCount++;
                        final WasmGlobal global = globalOpt.get();
                        assertNotNull(global.getType(), "Global type should not be null");
                        LOGGER.fine("Export " + exportName + " is a global");
                      }

                      if (memoryOpt.isPresent()) {
                        memoryCount++;
                        final WasmMemory memory = memoryOpt.get();
                        assertTrue(memory.size() >= 0, "Memory size should be non-negative");
                        LOGGER.fine("Export " + exportName + " is memory");
                      }

                      if (tableOpt.isPresent()) {
                        tableCount++;
                        final WasmTable table = tableOpt.get();
                        assertNotNull(table.getType(), "Table type should not be null");
                        LOGGER.fine("Export " + exportName + " is a table");
                      }

                      // Each export should be accessible through at least one getter
                      final boolean isAccessible =
                          functionOpt.isPresent()
                              || globalOpt.isPresent()
                              || memoryOpt.isPresent()
                              || tableOpt.isPresent();
                      assertTrue(isAccessible, "Export " + exportName + " should be accessible");
                    }

                    LOGGER.info(
                        String.format(
                            "Export summary for %s: %d functions, %d globals, %d memories, %d"
                                + " tables",
                            runtimeType, functionCount, globalCount, memoryCount, tableCount));

                    // Validate expected exports for add.wasm
                    assertTrue(functionCount > 0, "Should have at least one function export (add)");

                    LOGGER.info(
                        "Comprehensive export enumeration test successful with " + runtimeType);
                  }
                } catch (Exception e) {
                  throw new RuntimeException(
                      "Comprehensive export enumeration test failed for " + runtimeType, e);
                }
              });
        });
  }

  /**
   * Tests error handling for export access edge cases. Validates proper exception handling for
   * invalid operations.
   */
  @Test
  void testExportAccessErrorHandling() {
    runWithBothRuntimes(
        (runtime, runtimeType) -> {
          LOGGER.info("Testing export access error handling with " + runtimeType + " runtime");

          measureExecutionTime(
              "Export error handling test (" + runtimeType + ")",
              () -> {
                try {
                  final byte[] wasmBytes = loadWasmFile("add.wasm");

                  try (final Engine engine = runtime.createEngine();
                      final Store store = engine.createStore()) {

                    registerForCleanup(engine);
                    registerForCleanup(store);

                    final Module module = engine.compileModule(wasmBytes);
                    registerForCleanup(module);

                    final Instance instance = module.instantiate(store);
                    registerForCleanup(instance);

                    // Test null parameter handling
                    assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                          instance.getFunction(null);
                        },
                        "Getting function with null name should throw IllegalArgumentException");

                    assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                          instance.getGlobal(null);
                        },
                        "Getting global with null name should throw IllegalArgumentException");

                    assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                          instance.getMemory(null);
                        },
                        "Getting memory with null name should throw IllegalArgumentException");

                    assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                          instance.getTable(null);
                        },
                        "Getting table with null name should throw IllegalArgumentException");

                    assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                          instance.callFunction(null, WasmValue.i32(1));
                        },
                        "Calling function with null name should throw IllegalArgumentException");

                    // Test accessing exports on closed instance
                    instance.close();

                    assertThrows(
                        WasmException.class,
                        () -> {
                          instance.getFunction("add");
                        },
                        "Accessing function on closed instance should throw WasmException");

                    assertThrows(
                        WasmException.class,
                        () -> {
                          instance.getExportNames();
                        },
                        "Getting export names on closed instance should throw WasmException");

                    LOGGER.info("Export access error handling test successful with " + runtimeType);
                  }
                } catch (Exception e) {
                  throw new RuntimeException(
                      "Export access error handling test failed for " + runtimeType, e);
                }
              });
        });
  }

  /**
   * Tests cross-runtime consistency for export access. Ensures that both JNI and Panama runtimes
   * provide identical export information.
   */
  @Test
  void testCrossRuntimeExportConsistency() {
    skipIfPanamaNotAvailable();

    LOGGER.info("Testing cross-runtime export consistency");

    measureExecutionTime(
        "Cross-runtime consistency test",
        () -> {
          try {
            final byte[] wasmBytes = loadWasmFile("add.wasm");

            // Collect export information from both runtimes
            final String[] jniExports;
            final String[] panamaExports;

            try (final var jniRuntime = createTestRuntime(ai.tegmentum.wasmtime4j.RuntimeType.JNI);
                final Engine jniEngine = jniRuntime.createEngine();
                final Store jniStore = jniEngine.createStore()) {

              final Module jniModule = jniEngine.compileModule(wasmBytes);
              final Instance jniInstance = jniModule.instantiate(jniStore);

              jniExports = jniInstance.getExportNames();

              jniInstance.close();
              jniModule.close();
            }

            try (final var panamaRuntime =
                    createTestRuntime(ai.tegmentum.wasmtime4j.RuntimeType.PANAMA);
                final Engine panamaEngine = panamaRuntime.createEngine();
                final Store panamaStore = panamaEngine.createStore()) {

              final Module panamaModule = panamaEngine.compileModule(wasmBytes);
              final Instance panamaInstance = panamaModule.instantiate(panamaStore);

              panamaExports = panamaInstance.getExportNames();

              panamaInstance.close();
              panamaModule.close();
            }

            // Compare export lists
            assertAll(
                "Cross-runtime export consistency",
                () ->
                    assertEquals(
                        jniExports.length,
                        panamaExports.length,
                        "Both runtimes should report same number of exports"),
                () ->
                    assertExportArraysEqual(
                        jniExports,
                        panamaExports,
                        "Both runtimes should report same export names"));

            LOGGER.info("Cross-runtime export consistency test successful");
          } catch (Exception e) {
            throw new RuntimeException("Cross-runtime export consistency test failed", e);
          }
        });
  }

  /** Helper method to assert that two export name arrays are equal. */
  private void assertExportArraysEqual(
      final String[] expected, final String[] actual, final String message) {
    if (expected.length != actual.length) {
      throw new AssertionError(
          message + " - different lengths: " + expected.length + " vs " + actual.length);
    }

    final Set<String> expectedSet = Set.of(expected);
    final Set<String> actualSet = Set.of(actual);

    if (!expectedSet.equals(actualSet)) {
      throw new AssertionError(
          message + " - different contents: " + expectedSet + " vs " + actualSet);
    }
  }

  /** Helper method to load WASM files from test resources. */
  private byte[] loadWasmFile(final String filename) {
    try {
      final Path wasmPath = Paths.get(WASM_TEST_DIR + filename);
      return Files.readAllBytes(wasmPath);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to load WASM file: " + filename, e);
    }
  }
}
