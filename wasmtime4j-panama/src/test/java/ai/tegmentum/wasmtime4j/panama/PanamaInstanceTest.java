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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive tests for Panama Instance implementation.
 *
 * <p>This test class validates the complete Instance API functionality, including export enumeration,
 * import resolution, and proper resource management. All tests use defensive programming principles
 * with comprehensive validation and error handling.
 *
 * @since 1.0.0
 */
final class PanamaInstanceTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaInstanceTest.class.getName());

  // Test infrastructure
  private ArenaResourceManager resourceManager;
  private NativeFunctionBindings nativeFunctions;
  private PanamaEngine engine;
  private PanamaStore store;

  // Test WASM bytecode with multiple exports
  // This is a simple WASM module that exports a function, memory, global, and table
  private static final byte[] TEST_WASM_WITH_MULTIPLE_EXPORTS = {
    0x00, 0x61, 0x73, 0x6d, // WASM magic number
    0x01, 0x00, 0x00, 0x00, // version 1
    
    // Type section - function signatures
    0x01, 0x07, 0x01, 0x60, 0x01, 0x7f, 0x01, 0x7f, // (i32) -> i32
    
    // Function section
    0x03, 0x02, 0x01, 0x00, // 1 function with type index 0
    
    // Table section
    0x04, 0x04, 0x01, 0x70, 0x00, 0x00, // funcref table with initial size 0
    
    // Memory section  
    0x05, 0x03, 0x01, 0x00, 0x01, // memory with initial size 1 page
    
    // Global section
    0x06, 0x06, 0x01, 0x7f, 0x00, 0x41, 0x2a, 0x0b, // i32 global with value 42
    
    // Export section
    0x07, 0x1a, 0x04, // 4 exports
    0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export "add" function 0
    0x06, 0x6d, 0x65, 0x6d, 0x6f, 0x72, 0x79, 0x02, 0x00, // export "memory" memory 0
    0x05, 0x74, 0x61, 0x62, 0x6c, 0x65, 0x01, 0x00, // export "table" table 0  
    0x06, 0x67, 0x6c, 0x6f, 0x62, 0x61, 0x6c, 0x03, 0x00, // export "global" global 0
    
    // Code section
    0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x00, 0x6a, 0x0b // add function: local.get 0, local.get 0, i32.add
  };

  // Simple WASM with no exports
  private static final byte[] TEST_WASM_NO_EXPORTS = {
    0x00, 0x61, 0x73, 0x6d, // WASM magic number
    0x01, 0x00, 0x00, 0x00, // version 1
    
    // Type section
    0x01, 0x05, 0x01, 0x60, 0x00, 0x00, // () -> ()
    
    // Function section  
    0x03, 0x02, 0x01, 0x00, // 1 function with type index 0
    
    // Code section
    0x0a, 0x04, 0x01, 0x02, 0x00, 0x0b // empty function
  };

  @BeforeEach
  void setUp() throws WasmException {
    LOGGER.info("Setting up Panama Instance test environment");
    
    // Initialize core infrastructure 
    resourceManager = ArenaResourceManager.createGlobal();
    assertNotNull(resourceManager, "Resource manager should be created");
    
    nativeFunctions = NativeFunctionBindings.getInstance();
    assertNotNull(nativeFunctions, "Native functions should be initialized");
    assertTrue(nativeFunctions.isInitialized(), "Native functions should be ready");
    
    // Create engine and store
    engine = new PanamaEngine(resourceManager);
    assertNotNull(engine, "Engine should be created");
    assertTrue(engine.isValid(), "Engine should be valid");
    
    store = engine.createStore();
    assertNotNull(store, "Store should be created");
    assertTrue(store.isValid(), "Store should be valid");
    
    LOGGER.info("Test environment setup completed successfully");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Tearing down Panama Instance test environment");
    
    try {
      if (store != null && store.isValid()) {
        store.close();
      }
      if (engine != null && engine.isValid()) {
        engine.close();
      }
      if (resourceManager != null) {
        resourceManager.close();
      }
    } catch (Exception e) {
      LOGGER.warning("Error during test cleanup: " + e.getMessage());
    }
    
    LOGGER.info("Test environment cleanup completed");
  }

  @Test
  @Timeout(30)
  void testExportEnumerationWithMultipleExports() throws WasmException {
    LOGGER.info("Testing export enumeration with multiple exports");
    
    // Compile module with multiple exports
    PanamaModule module = engine.compileModule(TEST_WASM_WITH_MULTIPLE_EXPORTS);
    assertNotNull(module, "Module should be compiled");
    
    // Create instance
    PanamaInstance instance = (PanamaInstance) module.instantiate();
    assertNotNull(instance, "Instance should be created");
    assertTrue(instance.isValid(), "Instance should be valid");
    
    try {
      // Test getExportNames returns all expected exports
      String[] exportNames = instance.getExportNames();
      assertNotNull(exportNames, "Export names should not be null");
      
      LOGGER.info("Found " + exportNames.length + " exports: " + Arrays.toString(exportNames));
      
      // Verify we have the expected number of exports
      assertEquals(4, exportNames.length, "Should have exactly 4 exports");
      
      // Verify all expected export names are present
      Set<String> expectedExports = Set.of("add", "memory", "table", "global");
      Set<String> actualExports = new HashSet<>(Arrays.asList(exportNames));
      assertEquals(expectedExports, actualExports, "Export names should match expected set");
      
      // Test individual export retrieval
      Optional<WasmFunction> addFunction = instance.getFunction("add");
      assertTrue(addFunction.isPresent(), "Should find 'add' function export");
      
      Optional<WasmMemory> memory = instance.getMemory("memory");
      assertTrue(memory.isPresent(), "Should find 'memory' export");
      
      Optional<WasmTable> table = instance.getTable("table");
      assertTrue(table.isPresent(), "Should find 'table' export");
      
      Optional<WasmGlobal> global = instance.getGlobal("global");
      assertTrue(global.isPresent(), "Should find 'global' export");
      
      // Test non-existent export
      Optional<WasmFunction> nonExistent = instance.getFunction("nonexistent");
      assertFalse(nonExistent.isPresent(), "Should not find non-existent export");
      
      // Test getDefaultMemory
      Optional<WasmMemory> defaultMemory = instance.getDefaultMemory();
      assertTrue(defaultMemory.isPresent(), "Should find default memory");
      assertEquals(memory.get(), defaultMemory.get(), "Default memory should be the same as 'memory' export");
      
    } finally {
      instance.close();
      module.close();
    }
    
    LOGGER.info("Export enumeration test completed successfully");
  }

  @Test
  @Timeout(30)
  void testExportEnumerationWithNoExports() throws WasmException {
    LOGGER.info("Testing export enumeration with no exports");
    
    // Compile module with no exports
    PanamaModule module = engine.compileModule(TEST_WASM_NO_EXPORTS);
    assertNotNull(module, "Module should be compiled");
    
    // Create instance
    PanamaInstance instance = (PanamaInstance) module.instantiate();
    assertNotNull(instance, "Instance should be created");
    assertTrue(instance.isValid(), "Instance should be valid");
    
    try {
      // Test getExportNames returns empty array
      String[] exportNames = instance.getExportNames();
      assertNotNull(exportNames, "Export names should not be null");
      assertEquals(0, exportNames.length, "Should have no exports");
      
      LOGGER.info("Confirmed no exports found as expected");
      
      // Test individual export retrieval returns empty
      Optional<WasmFunction> function = instance.getFunction("any_name");
      assertFalse(function.isPresent(), "Should find no function exports");
      
      Optional<WasmMemory> memory = instance.getMemory("memory");
      assertFalse(memory.isPresent(), "Should find no memory exports");
      
      Optional<WasmTable> table = instance.getTable("table");
      assertFalse(table.isPresent(), "Should find no table exports");
      
      Optional<WasmGlobal> global = instance.getGlobal("global");
      assertFalse(global.isPresent(), "Should find no global exports");
      
      // Test getDefaultMemory returns empty
      Optional<WasmMemory> defaultMemory = instance.getDefaultMemory();
      assertFalse(defaultMemory.isPresent(), "Should find no default memory");
      
    } finally {
      instance.close();
      module.close();
    }
    
    LOGGER.info("No exports test completed successfully");
  }

  @Test
  @Timeout(30)
  void testFunctionCall() throws WasmException {
    LOGGER.info("Testing function call through instance");
    
    // Compile module with function export
    PanamaModule module = engine.compileModule(TEST_WASM_WITH_MULTIPLE_EXPORTS);
    assertNotNull(module, "Module should be compiled");
    
    // Create instance
    PanamaInstance instance = (PanamaInstance) module.instantiate();
    assertNotNull(instance, "Instance should be created");
    assertTrue(instance.isValid(), "Instance should be valid");
    
    try {
      // Call the add function (adds parameter to itself)
      WasmValue[] results = instance.callFunction("add", WasmValue.i32(21));
      assertNotNull(results, "Function call results should not be null");
      assertEquals(1, results.length, "Should have 1 result");
      assertEquals(42, results[0].asInt(), "Result should be 21 + 21 = 42");
      
      LOGGER.info("Function call test completed with result: " + results[0].asInt());
      
      // Test function call with non-existent function
      assertThrows(WasmException.class, () -> {
        instance.callFunction("nonexistent");
      }, "Should throw exception for non-existent function");
      
    } finally {
      instance.close();
      module.close();
    }
    
    LOGGER.info("Function call test completed successfully");
  }

  @Test
  @Timeout(30)
  void testInstanceValidation() throws WasmException {
    LOGGER.info("Testing instance validation and error handling");
    
    // Compile module
    PanamaModule module = engine.compileModule(TEST_WASM_WITH_MULTIPLE_EXPORTS);
    assertNotNull(module, "Module should be compiled");
    
    // Create instance
    PanamaInstance instance = (PanamaInstance) module.instantiate();
    assertNotNull(instance, "Instance should be created");
    assertTrue(instance.isValid(), "Instance should be valid initially");
    
    // Test that module and store are accessible
    assertEquals(module, instance.getModule(), "Module should match");
    assertNotNull(instance.getStore(), "Store should be accessible");
    
    // Close instance and verify invalid state
    instance.close();
    assertFalse(instance.isValid(), "Instance should be invalid after close");
    
    // Test that operations on closed instance throw exceptions
    assertThrows(IllegalStateException.class, () -> {
      instance.getExportNames();
    }, "Should throw exception when accessing closed instance");
    
    assertThrows(IllegalStateException.class, () -> {
      instance.getFunction("add");
    }, "Should throw exception when accessing closed instance");
    
    assertThrows(IllegalStateException.class, () -> {
      instance.callFunction("add");
    }, "Should throw exception when accessing closed instance");
    
    // Close module as well
    module.close();
    
    LOGGER.info("Instance validation test completed successfully");
  }

  @Test
  @Timeout(30)
  void testExportTypeValidation() throws WasmException {
    LOGGER.info("Testing export type validation");
    
    // Compile module with multiple export types
    PanamaModule module = engine.compileModule(TEST_WASM_WITH_MULTIPLE_EXPORTS);
    assertNotNull(module, "Module should be compiled");
    
    // Create instance
    PanamaInstance instance = (PanamaInstance) module.instantiate();
    assertNotNull(instance, "Instance should be created");
    assertTrue(instance.isValid(), "Instance should be valid");
    
    try {
      // Test that exports are of the correct type
      // Function export should not be accessible as memory/table/global
      Optional<WasmMemory> functionAsMemory = instance.getMemory("add");
      assertFalse(functionAsMemory.isPresent(), "Function should not be accessible as memory");
      
      Optional<WasmTable> functionAsTable = instance.getTable("add");
      assertFalse(functionAsTable.isPresent(), "Function should not be accessible as table");
      
      Optional<WasmGlobal> functionAsGlobal = instance.getGlobal("add");
      assertFalse(functionAsGlobal.isPresent(), "Function should not be accessible as global");
      
      // Memory export should not be accessible as function/table/global
      Optional<WasmFunction> memoryAsFunction = instance.getFunction("memory");
      assertFalse(memoryAsFunction.isPresent(), "Memory should not be accessible as function");
      
      Optional<WasmTable> memoryAsTable = instance.getTable("memory");
      assertFalse(memoryAsTable.isPresent(), "Memory should not be accessible as table");
      
      Optional<WasmGlobal> memoryAsGlobal = instance.getGlobal("memory");
      assertFalse(memoryAsGlobal.isPresent(), "Memory should not be accessible as global");
      
      LOGGER.info("Export type validation completed successfully");
      
    } finally {
      instance.close();
      module.close();
    }
  }

  @Test
  @Timeout(30)
  void testParameterValidation() throws WasmException {
    LOGGER.info("Testing parameter validation");
    
    // Compile module
    PanamaModule module = engine.compileModule(TEST_WASM_WITH_MULTIPLE_EXPORTS);
    assertNotNull(module, "Module should be compiled");
    
    // Create instance
    PanamaInstance instance = (PanamaInstance) module.instantiate();
    assertNotNull(instance, "Instance should be created");
    
    try {
      // Test null parameter validation
      assertThrows(NullPointerException.class, () -> {
        instance.getFunction(null);
      }, "Should throw exception for null function name");
      
      assertThrows(NullPointerException.class, () -> {
        instance.getMemory(null);
      }, "Should throw exception for null memory name");
      
      assertThrows(NullPointerException.class, () -> {
        instance.getTable(null);
      }, "Should throw exception for null table name");
      
      assertThrows(NullPointerException.class, () -> {
        instance.getGlobal(null);
      }, "Should throw exception for null global name");
      
      assertThrows(NullPointerException.class, () -> {
        instance.callFunction(null);
      }, "Should throw exception for null function name in call");
      
      // Test empty string validation
      Optional<WasmFunction> emptyNameFunction = instance.getFunction("");
      assertFalse(emptyNameFunction.isPresent(), "Should not find function with empty name");
      
      LOGGER.info("Parameter validation test completed successfully");
      
    } finally {
      instance.close();
      module.close();
    }
  }

  @Test
  @Timeout(30)
  void testResourceManagement() throws WasmException {
    LOGGER.info("Testing instance resource management");
    
    // Compile module
    PanamaModule module = engine.compileModule(TEST_WASM_WITH_MULTIPLE_EXPORTS);
    assertNotNull(module, "Module should be compiled");
    
    // Create and immediately close multiple instances to test cleanup
    for (int i = 0; i < 5; i++) {
      PanamaInstance instance = (PanamaInstance) module.instantiate();
      assertNotNull(instance, "Instance " + i + " should be created");
      assertTrue(instance.isValid(), "Instance " + i + " should be valid");
      
      // Use the instance briefly
      String[] exports = instance.getExportNames();
      assertEquals(4, exports.length, "Should find 4 exports");
      
      // Close and verify cleanup
      instance.close();
      assertFalse(instance.isValid(), "Instance " + i + " should be invalid after close");
    }
    
    module.close();
    
    LOGGER.info("Resource management test completed successfully");
  }
}