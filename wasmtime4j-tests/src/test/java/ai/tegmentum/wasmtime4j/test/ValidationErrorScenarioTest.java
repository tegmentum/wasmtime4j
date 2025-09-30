package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive test suite for WebAssembly validation error scenarios.
 *
 * <p>This test class verifies proper error handling for various WebAssembly validation failures,
 * including type mismatches, invalid function signatures, and module structure violations.
 * Validation errors occur when WebAssembly modules are structurally correct but violate WebAssembly
 * semantic rules.
 */
@DisplayName("Validation Error Scenario Test Suite")
class ValidationErrorScenarioTest {

  /** Valid WebAssembly module header. */
  private static final byte[] WASM_HEADER = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Invalid function type throws ValidationException")
  void testInvalidFunctionType(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with invalid function type (invalid value type)
      byte[] invalidFunctionType = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x07, // Section size
        0x01, // 1 type
        0x60, // Function type
        0x01,
        (byte) 0xFF, // 1 parameter with invalid type 0xFF
        0x01,
        0x7F // 1 return value of type i32
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, invalidFunctionType),
              "Invalid function type should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("type")
              || exception.getMessage().toLowerCase().contains("invalid")
              || exception.getMessage().toLowerCase().contains("validation"),
          "Exception message should mention type/invalid/validation: " + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Duplicate section types throw ValidationException")
  void testDuplicateSectionTypes() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with duplicate type sections
      byte[] duplicateTypeSections = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00, // Function type: () -> ()
        0x01, // Type section again (duplicate)
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00 // Function type: () -> ()
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, duplicateTypeSections),
              "Duplicate sections should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("duplicate")
              || exception.getMessage().toLowerCase().contains("section")
              || exception.getMessage().toLowerCase().contains("multiple"),
          "Exception message should mention duplicate/section/multiple: " + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Invalid section ordering throws ValidationException")
  void testInvalidSectionOrdering() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with sections in wrong order (function before type)
      byte[] invalidSectionOrder = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x03, // Function section (should come after type section)
        0x02, // Section size
        0x01,
        0x00, // 1 function with type index 0
        0x01, // Type section (should come before function section)
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00 // Function type: () -> ()
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, invalidSectionOrder),
              "Invalid section order should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("order")
              || exception.getMessage().toLowerCase().contains("section")
              || exception.getMessage().toLowerCase().contains("unexpected")
              || exception.getMessage().toLowerCase().contains("sequence"),
          "Exception message should mention order/section/unexpected/sequence: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Type index out of bounds throws ValidationException")
  void testTypeIndexOutOfBounds() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with function referencing non-existent type
      byte[] typeIndexOutOfBounds = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00, // Function type: () -> ()
        0x03, // Function section
        0x02, // Section size
        0x01,
        0x05, // 1 function with type index 5 (out of bounds)
        0x0A, // Code section
        0x04, // Section size
        0x01, // 1 function body
        0x02, // Body size
        0x00,
        0x0B // No locals, end instruction
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, typeIndexOutOfBounds),
              "Type index out of bounds should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("index")
              || exception.getMessage().toLowerCase().contains("bounds")
              || exception.getMessage().toLowerCase().contains("type")
              || exception.getMessage().toLowerCase().contains("invalid"),
          "Exception message should mention index/bounds/type/invalid: " + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Function count mismatch throws ValidationException")
  void testFunctionCountMismatch() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with mismatched function and code counts
      byte[] functionCountMismatch = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00, // Function type: () -> ()
        0x03, // Function section
        0x03, // Section size
        0x02,
        0x00,
        0x00, // 2 functions with type index 0
        0x0A, // Code section
        0x04, // Section size
        0x01, // Only 1 function body (should be 2)
        0x02, // Body size
        0x00,
        0x0B // No locals, end instruction
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, functionCountMismatch),
              "Function count mismatch should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("function")
              || exception.getMessage().toLowerCase().contains("count")
              || exception.getMessage().toLowerCase().contains("mismatch")
              || exception.getMessage().toLowerCase().contains("code"),
          "Exception message should mention function/count/mismatch/code: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Invalid instruction sequence throws ValidationException")
  void testInvalidInstructionSequence() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with invalid instruction (reserved opcode)
      byte[] invalidInstruction = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00, // Function type: () -> ()
        0x03, // Function section
        0x02, // Section size
        0x01,
        0x00, // 1 function with type index 0
        0x0A, // Code section
        0x05, // Section size
        0x01, // 1 function body
        0x03, // Body size
        0x00, // No locals
        (byte) 0xFF, // Invalid/reserved instruction
        0x0B // End instruction
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, invalidInstruction),
              "Invalid instruction should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("instruction")
              || exception.getMessage().toLowerCase().contains("opcode")
              || exception.getMessage().toLowerCase().contains("invalid")
              || exception.getMessage().toLowerCase().contains("unknown"),
          "Exception message should mention instruction/opcode/invalid/unknown: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Stack type mismatch in function throws ValidationException")
  void testStackTypeMismatch() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with function that has stack type mismatch
      byte[] stackTypeMismatch = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x05, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x01,
        0x7F, // Function type: () -> i32
        0x03, // Function section
        0x02, // Section size
        0x01,
        0x00, // 1 function with type index 0
        0x0A, // Code section
        0x06, // Section size
        0x01, // 1 function body
        0x04, // Body size
        0x00, // No locals
        0x42,
        0x01, // i64.const 1 (wrong type, should be i32)
        0x0B // End instruction
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, stackTypeMismatch),
              "Stack type mismatch should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("type")
              || exception.getMessage().toLowerCase().contains("stack")
              || exception.getMessage().toLowerCase().contains("mismatch")
              || exception.getMessage().toLowerCase().contains("expected"),
          "Exception message should mention type/stack/mismatch/expected: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Memory index out of bounds throws ValidationException")
  void testMemoryIndexOutOfBounds() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with memory instruction referencing non-existent memory
      byte[] memoryIndexOutOfBounds = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00, // Function type: () -> ()
        0x03, // Function section
        0x02, // Section size
        0x01,
        0x00, // 1 function with type index 0
        0x0A, // Code section
        0x08, // Section size
        0x01, // 1 function body
        0x06, // Body size
        0x00, // No locals
        0x41,
        0x00, // i32.const 0
        0x28,
        0x01,
        0x00, // i32.load with memory index 1 (no memory declared)
        0x0B // End instruction
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, memoryIndexOutOfBounds),
              "Memory index out of bounds should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("memory")
              || exception.getMessage().toLowerCase().contains("index")
              || exception.getMessage().toLowerCase().contains("bounds")
              || exception.getMessage().toLowerCase().contains("undefined"),
          "Exception message should mention memory/index/bounds/undefined: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Global index out of bounds throws ValidationException")
  void testGlobalIndexOutOfBounds() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with global.get referencing non-existent global
      byte[] globalIndexOutOfBounds = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x05, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x01,
        0x7F, // Function type: () -> i32
        0x03, // Function section
        0x02, // Section size
        0x01,
        0x00, // 1 function with type index 0
        0x0A, // Code section
        0x05, // Section size
        0x01, // 1 function body
        0x03, // Body size
        0x00, // No locals
        0x23,
        0x05, // global.get 5 (no global declared)
        0x0B // End instruction
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, globalIndexOutOfBounds),
              "Global index out of bounds should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("global")
              || exception.getMessage().toLowerCase().contains("index")
              || exception.getMessage().toLowerCase().contains("bounds")
              || exception.getMessage().toLowerCase().contains("undefined"),
          "Exception message should mention global/index/bounds/undefined: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Invalid export name throws ValidationException")
  void testInvalidExportName() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with export referencing non-existent function
      byte[] invalidExportName = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00, // Function type: () -> ()
        0x03, // Function section
        0x02, // Section size
        0x01,
        0x00, // 1 function with type index 0
        0x07, // Export section
        0x07, // Section size
        0x01, // 1 export
        0x04,
        't',
        'e',
        's',
        't', // Export name "test"
        0x00,
        0x05, // Function export with index 5 (out of bounds)
        0x0A, // Code section
        0x04, // Section size
        0x01, // 1 function body
        0x02, // Body size
        0x00,
        0x0B // No locals, end instruction
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, invalidExportName),
              "Invalid export should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(
          exception.getMessage().toLowerCase().contains("export")
              || exception.getMessage().toLowerCase().contains("function")
              || exception.getMessage().toLowerCase().contains("index")
              || exception.getMessage().toLowerCase().contains("bounds"),
          "Exception message should mention export/function/index/bounds: "
              + exception.getMessage());
    }
  }

  @Test
  @DisplayName("Validation errors preserve error context")
  void testValidationErrorContext() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // WebAssembly module with multiple validation issues
      byte[] multipleIssues = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60, 0x00, 0x00, // Function type: () -> ()
        0x03, // Function section
        0x02, // Section size
        0x01, 0x99, // 1 function with invalid type index 0x99
      };

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, multipleIssues),
              "Multiple validation issues should throw WasmException");

      String message = exception.getMessage();
      assertNotNull(message, "Exception should have meaningful message");

      // Should contain enough context to identify the problem
      assertTrue(message.length() > 20, "Error message should be sufficiently detailed");

      // Message should be useful for debugging
      assertTrue(
          message.toLowerCase().contains("index")
              || message.toLowerCase().contains("type")
              || message.toLowerCase().contains("invalid")
              || message.toLowerCase().contains("bounds"),
          "Error message should provide useful debugging information: " + message);
    }
  }

  @Test
  @DisplayName("Validation works correctly with valid modules")
  void testValidationWithValidModules() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Simple valid WebAssembly module
      byte[] validModule = {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60,
        0x00,
        0x00, // Function type: () -> ()
        0x03, // Function section
        0x02, // Section size
        0x01,
        0x00, // 1 function with type index 0
        0x07, // Export section
        0x07, // Section size
        0x01, // 1 export
        0x04,
        't',
        'e',
        's',
        't', // Export name "test"
        0x00,
        0x00, // Function export with index 0
        0x0A, // Code section
        0x04, // Section size
        0x01, // 1 function body
        0x02, // Body size
        0x00,
        0x0B // No locals, end instruction
      };

      // Valid module should compile and instantiate successfully
      Module module = runtime.compileModule(engine, validModule);
      assertNotNull(module, "Valid module should compile successfully");

      // Should be able to instantiate without validation errors
      runtime.instantiateModule(store, module);
    }
  }
}
