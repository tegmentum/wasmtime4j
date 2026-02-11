package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.validation.ModuleValidationResult;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Module#validate(Engine, byte[])} static method.
 *
 * <p>Validates that the static validation API correctly identifies valid and invalid WASM bytecode
 * without performing full compilation, and handles null arguments defensively.
 */
@DisplayName("Module.validate() Tests")
public class ModuleValidationTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleValidationTest.class.getName());

  /** Minimal valid WASM module: magic number + version header (empty module). */
  private static final byte[] VALID_EMPTY_MODULE = new byte[] {
      0x00, 0x61, 0x73, 0x6D, // magic: \0asm
      0x01, 0x00, 0x00, 0x00  // version: 1
  };

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validate valid WASM bytes returns success")
  void validateValidWasmBytesReturnsSuccess(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.validate with valid WASM bytes");

    try (Engine engine = Engine.create()) {
      final ModuleValidationResult result = Module.validate(engine, VALID_EMPTY_MODULE);

      assertNotNull(result, "Validation result must not be null");
      assertTrue(result.isValid(),
          "Valid WASM bytes should produce isValid()=true, but got errors: " + result.getErrors());
      assertFalse(result.hasErrors(),
          "Valid WASM bytes should have no errors, but got: " + result.getErrors());
      LOGGER.info("[" + runtime + "] validate returned isValid=" + result.isValid()
          + ", errors=" + result.getErrors());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validate invalid WASM bytes returns failure")
  void validateInvalidWasmBytesReturnsFailure(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.validate with garbage bytes");

    try (Engine engine = Engine.create()) {
      final byte[] garbage = new byte[] {0x7F, 0x45, 0x4C, 0x46, 0x00, 0x00, 0x00, 0x00};
      final ModuleValidationResult result = Module.validate(engine, garbage);

      assertNotNull(result, "Validation result must not be null even for invalid bytes");
      assertFalse(result.isValid(),
          "Garbage bytes should produce isValid()=false");
      assertTrue(result.hasErrors(),
          "Garbage bytes should produce errors");
      assertFalse(result.getErrors().isEmpty(),
          "Error list should be non-empty for invalid bytes");
      LOGGER.info("[" + runtime + "] validate returned isValid=" + result.isValid()
          + ", errors=" + result.getErrors());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validate empty bytes returns failure")
  void validateEmptyBytesReturnsFailure(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.validate with empty byte array");

    try (Engine engine = Engine.create()) {
      final ModuleValidationResult result = Module.validate(engine, new byte[0]);

      assertNotNull(result, "Validation result must not be null for empty bytes");
      assertFalse(result.isValid(),
          "Empty bytes should produce isValid()=false");
      LOGGER.info("[" + runtime + "] validate(empty) returned isValid=" + result.isValid()
          + ", errors=" + result.getErrors());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validate with null engine throws IllegalArgumentException")
  void validateNullEngineThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.validate with null engine");

    assertThrows(IllegalArgumentException.class,
        () -> Module.validate(null, VALID_EMPTY_MODULE),
        "Module.validate(null, bytes) must throw IllegalArgumentException");
    LOGGER.info("[" + runtime + "] Correctly threw IllegalArgumentException for null engine");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("validate with null bytes throws IllegalArgumentException")
  void validateNullBytesThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Module.validate with null bytes");

    try (Engine engine = Engine.create()) {
      assertThrows(IllegalArgumentException.class,
          () -> Module.validate(engine, null),
          "Module.validate(engine, null) must throw IllegalArgumentException");
      LOGGER.info("[" + runtime + "] Correctly threw IllegalArgumentException for null bytes");
    }
  }
}
