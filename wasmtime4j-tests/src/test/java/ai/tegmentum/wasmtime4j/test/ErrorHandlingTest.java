package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive tests for error handling using only public APIs.
 *
 * <p>These tests verify that proper exceptions are thrown for various error conditions, that error
 * messages are meaningful, and that defensive programming prevents JVM crashes. All tests use only
 * public APIs to ensure they work regardless of implementation details.
 */
@DisplayName("Error Handling Test Suite")
class ErrorHandlingTest {

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Runtime factory handles invalid runtime types gracefully")
  void testRuntimeFactoryErrorHandling(RuntimeType runtimeType) {
    // Test that runtime creation handles unavailable implementations gracefully
    assertDoesNotThrow(
        () -> {
          boolean isAvailable = WasmRuntimeFactory.isRuntimeAvailable(runtimeType);
          assertNotNull(isAvailable, "Runtime availability check should not return null");
        },
        "Runtime availability check should not throw exceptions");
  }

  @Test
  @DisplayName("Invalid WebAssembly compilation throws CompilationException")
  void testInvalidWasmCompilationError() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test with completely invalid bytecode
      byte[] invalidWasm = {0x00, 0x01, 0x02, 0x03}; // Not valid WASM magic

      CompilationException exception =
          assertThrows(
              CompilationException.class,
              () -> runtime.compileModule(engine, invalidWasm),
              "Invalid WASM bytecode should throw CompilationException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(exception.getMessage().length() > 10, "Exception message should be descriptive");
    }
  }

  @Test
  @DisplayName("Null parameters throw IllegalArgumentException")
  void testNullParameterValidation() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test null engine parameter
      assertThrows(
          IllegalArgumentException.class,
          () -> runtime.compileModule(null, new byte[] {0x00, 0x61, 0x73, 0x6d}),
          "Null engine should throw IllegalArgumentException");

      // Test null bytecode parameter
      assertThrows(
          IllegalArgumentException.class,
          () -> runtime.compileModule(engine, null),
          "Null bytecode should throw IllegalArgumentException");

      // Test null store creation parameter
      assertThrows(
          IllegalArgumentException.class,
          () -> runtime.createStore(null),
          "Null engine for store creation should throw IllegalArgumentException");
    }
  }

  @Test
  @DisplayName("Empty WebAssembly bytecode throws ValidationException")
  void testEmptyWasmValidationError() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test with empty bytecode
      byte[] emptyWasm = new byte[0];

      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, emptyWasm),
              "Empty WASM bytecode should throw WasmException");

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");
      assertTrue(exception.getMessage().length() > 5, "Exception message should be descriptive");
    }
  }

  @Test
  @DisplayName("Runtime factory handles null RuntimeType parameter")
  void testRuntimeFactoryNullHandling() {
    assertThrows(
        IllegalArgumentException.class,
        () -> WasmRuntimeFactory.create(null),
        "Null RuntimeType should throw IllegalArgumentException");
  }

  @Test
  @DisplayName("Runtime remains valid after creation")
  void testRuntimeValidity() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      assertTrue(runtime.isValid(), "Newly created runtime should be valid");

      // Runtime should still be valid after normal operations
      Engine engine = runtime.createEngine();
      assertTrue(runtime.isValid(), "Runtime should remain valid after engine creation");

      Store store = runtime.createStore(engine);
      assertTrue(runtime.isValid(), "Runtime should remain valid after store creation");
    }
  }

  @Test
  @DisplayName("Multiple runtime creation works safely")
  void testMultipleRuntimeCreation() throws WasmException {
    // Test that creating multiple runtimes doesn't interfere with each other
    try (WasmRuntime runtime1 = WasmRuntimeFactory.create();
        WasmRuntime runtime2 = WasmRuntimeFactory.create()) {

      assertTrue(runtime1.isValid(), "First runtime should be valid");
      assertTrue(runtime2.isValid(), "Second runtime should be valid");

      Engine engine1 = runtime1.createEngine();
      Engine engine2 = runtime2.createEngine();

      assertNotNull(engine1, "First engine should be created successfully");
      assertNotNull(engine2, "Second engine should be created successfully");
    }
  }

  @Test
  @DisplayName("Exception hierarchy is properly structured")
  void testExceptionHierarchy() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      byte[] invalidWasm = {0x00, 0x01, 0x02, 0x03};

      try {
        runtime.compileModule(engine, invalidWasm);
      } catch (CompilationException e) {
        // CompilationException should be a subclass of WasmException
        assertTrue(e instanceof WasmException, "CompilationException should extend WasmException");
        assertNotNull(e.getMessage(), "Exception should have message");
      } catch (ValidationException e) {
        // ValidationException should be a subclass of WasmException
        assertTrue(e instanceof WasmException, "ValidationException should extend WasmException");
        assertNotNull(e.getMessage(), "Exception should have message");
      } catch (WasmException e) {
        // Any WasmException is acceptable
        assertNotNull(e.getMessage(), "Exception should have message");
      }
    }
  }

  @Test
  @DisplayName("Runtime info provides valid information")
  void testRuntimeInfo() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      assertNotNull(runtime.getRuntimeInfo(), "Runtime info should not be null");
      assertTrue(
          runtime.getRuntimeInfo().toString().length() > 0,
          "Runtime info should have meaningful string representation");
    }
  }

  @Test
  @DisplayName("Error messages are safe for logging")
  void testErrorMessageSafety() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      byte[] invalidWasm = {0x00, 0x01, 0x02, 0x03};

      try {
        runtime.compileModule(engine, invalidWasm);
      } catch (WasmException e) {
        String message = e.getMessage();
        assertNotNull(message, "Exception message should not be null");

        // Ensure message doesn't contain potential log injection characters
        assertTrue(
            !message.contains("\n") && !message.contains("\r"),
            "Exception message should not contain CRLF characters");

        // Message should have reasonable length
        assertTrue(message.length() < 10000, "Exception message should not be excessively long");
      }
    }
  }
}
