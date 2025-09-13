package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeTestRunner;
import ai.tegmentum.wasmtime4j.webassembly.CrossRuntimeValidationResult;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestDataManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive validation tests for Module API. Tests module validation scenarios, error handling,
 * malformed modules, and WebAssembly specification compliance.
 */
@DisplayName("Module Validation Comprehensive Tests")
class ModuleValidationIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleValidationTest.class.getName());

  private WasmTestDataManager testDataManager;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("module.validation");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test data manager: " + e.getMessage());
      skipIfNot(false, "Test data manager setup failed: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should validate WebAssembly magic number")
  void shouldValidateWebAssemblyMagicNumber() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-magic-number",
            runtime -> {
              // Given - Invalid magic number
              final byte[] invalidMagic = {0x01, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

              try (final Engine engine = runtime.createEngine()) {
                // When & Then
                assertThatThrownBy(() -> engine.compileModule(invalidMagic))
                    .isInstanceOfAny(
                        WasmException.class, CompilationException.class, ValidationException.class);

                return "Invalid magic number correctly rejected";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Magic number validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate WebAssembly version")
  void shouldValidateWebAssemblyVersion() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-version",
            runtime -> {
              // Given - Invalid version
              final byte[] invalidVersion = {0x00, 0x61, 0x73, 0x6d, 0x02, 0x00, 0x00, 0x00};

              try (final Engine engine = runtime.createEngine()) {
                // When & Then
                assertThatThrownBy(() -> engine.compileModule(invalidVersion))
                    .isInstanceOfAny(
                        WasmException.class, CompilationException.class, ValidationException.class);

                return "Invalid version correctly rejected";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Version validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle empty modules")
  void shouldHandleEmptyModules() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-empty-module",
            runtime -> {
              // Given
              final byte[] emptyModule = new byte[0];

              try (final Engine engine = runtime.createEngine()) {
                // When & Then
                assertThatThrownBy(() -> engine.compileModule(emptyModule))
                    .isInstanceOfAny(
                        WasmException.class, CompilationException.class, ValidationException.class);

                return "Empty module correctly rejected";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Empty module validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle module with only header")
  void shouldHandleModuleWithOnlyHeader() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-header-only",
            runtime -> {
              // Given - Only magic and version, no sections
              final byte[] headerOnly = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

              try (final Engine engine = runtime.createEngine()) {
                // When - This might be valid (empty module) or invalid depending on implementation
                final Module module = engine.compileModule(headerOnly);
                assertThat(module).isNotNull();
                assertThat(module.isValid()).isTrue();

                // Empty module should have no exports or imports
                assertThat(module.getExports()).isEmpty();
                assertThat(module.getImports()).isEmpty();

                module.close();
                return "Header-only module accepted as empty module";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Header-only module validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate malformed section headers")
  void shouldValidateMalformedSectionHeaders() {
    final List<byte[]> malformedModules = createMalformedSectionModules();

    for (int i = 0; i < malformedModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] malformedModule = malformedModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "validation-malformed-section-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(malformedModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Malformed section " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.info("Malformed section " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should validate invalid function signatures")
  void shouldValidateInvalidFunctionSignatures() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-invalid-function-signature",
            runtime -> {
              // Given - Module with invalid function signature
              final byte[] invalidSignature = createModuleWithInvalidFunctionSignature();

              try (final Engine engine = runtime.createEngine()) {
                // When & Then
                assertThatThrownBy(() -> engine.compileModule(invalidSignature))
                    .isInstanceOfAny(
                        WasmException.class, CompilationException.class, ValidationException.class);

                return "Invalid function signature correctly rejected";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Invalid function signature validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate invalid memory descriptors")
  void shouldValidateInvalidMemoryDescriptors() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-invalid-memory",
            runtime -> {
              // Given - Module with invalid memory descriptor
              final byte[] invalidMemory = createModuleWithInvalidMemoryDescriptor();

              try (final Engine engine = runtime.createEngine()) {
                // When & Then
                assertThatThrownBy(() -> engine.compileModule(invalidMemory))
                    .isInstanceOfAny(
                        WasmException.class, CompilationException.class, ValidationException.class);

                return "Invalid memory descriptor correctly rejected";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Invalid memory descriptor validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate modules with circular imports")
  void shouldValidateModulesWithCircularImports() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-circular-imports",
            runtime -> {
              // Given - Module with self-referential imports (which should be valid structurally)
              final byte[] circularImports = createModuleWithCircularImports();

              try (final Engine engine = runtime.createEngine()) {
                // When - This should compile successfully, but instantiation may fail
                final Module module = engine.compileModule(circularImports);
                assertThat(module).isNotNull();
                assertThat(module.isValid()).isTrue();

                // Check that imports are present
                assertThat(module.getImports()).isNotEmpty();

                module.close();
                return "Module with circular imports compiled successfully";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Circular imports validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate bytecode instruction sequences")
  void shouldValidateBytecodeInstructionSequences() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-invalid-bytecode",
            runtime -> {
              // Given - Module with invalid bytecode sequence
              final byte[] invalidBytecode = createModuleWithInvalidBytecode();

              try (final Engine engine = runtime.createEngine()) {
                // When & Then
                assertThatThrownBy(() -> engine.compileModule(invalidBytecode))
                    .isInstanceOfAny(
                        WasmException.class, CompilationException.class, ValidationException.class);

                return "Invalid bytecode correctly rejected";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Invalid bytecode validation: " + validation.getSummary());
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should handle stress test with many invalid modules")
  void shouldHandleStressTestWithManyInvalidModules(final RuntimeType runtimeType) {
    // skipIfCategoryNotEnabled("stress");

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-stress-invalid-modules",
            runtime -> {
              final List<byte[]> invalidModules = generateInvalidModules(20);
              int rejectedCount = 0;

              try (final Engine engine = runtime.createEngine()) {
                for (final byte[] invalidModule : invalidModules) {
                  try {
                    engine.compileModule(invalidModule);
                    // If it doesn't throw, that's unexpected but not necessarily wrong
                    LOGGER.warning("Expected invalid module was accepted");
                  } catch (final WasmException e) {
                    rejectedCount++;
                  }
                }
              }

              // Most should be rejected
              assertThat(rejectedCount).isGreaterThan(invalidModules.size() / 2);
              return "Rejected "
                  + rejectedCount
                  + " out of "
                  + invalidModules.size()
                  + " invalid modules";
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Stress test validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should validate valid modules pass validation")
  void shouldValidateValidModulesPassValidation() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "validation-valid-modules",
            runtime -> {
              // Given - Collection of valid modules
              final List<byte[]> validModules =
                  Arrays.asList(
                      TestUtils.createSimpleWasmModule(), TestUtils.createMemoryImportWasmModule());

              try (final Engine engine = runtime.createEngine()) {
                // When & Then - All should compile successfully
                for (int i = 0; i < validModules.size(); i++) {
                  final byte[] validModule = validModules.get(i);

                  final Module module = engine.compileModule(validModule);
                  assertThat(module).isNotNull();
                  assertThat(module.isValid()).isTrue();
                  module.close();
                }

                return "All " + validModules.size() + " valid modules compiled successfully";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Valid modules validation: " + validation.getSummary());
  }

  /** Creates a list of malformed WebAssembly modules with invalid section headers. */
  private List<byte[]> createMalformedSectionModules() {
    final List<byte[]> malformed = new ArrayList<>();

    // Malformed type section
    malformed.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d, // magic
          0x01,
          0x00,
          0x00,
          0x00, // version
          0x01,
          (byte) 0xFF, // type section with invalid length
          0x01,
          0x60
        });

    // Truncated section
    malformed.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d, // magic
          0x01,
          0x00,
          0x00,
          0x00, // version
          0x01,
          0x05, // type section length 5
          0x01,
          0x60 // but only 2 bytes follow
        });

    // Invalid section ID
    malformed.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d, // magic
          0x01,
          0x00,
          0x00,
          0x00, // version
          (byte) 0xFF,
          0x01, // invalid section ID 255
          0x60
        });

    return malformed;
  }

  /** Creates a WebAssembly module with invalid function signature. */
  private byte[] createModuleWithInvalidFunctionSignature() {
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version
      0x01,
      0x05,
      0x01, // type section
      0x60,
      (byte) 0xFF,
      0x7f, // invalid param count 255
      0x01,
      0x7f
    };
  }

  /** Creates a WebAssembly module with invalid memory descriptor. */
  private byte[] createModuleWithInvalidMemoryDescriptor() {
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version
      0x05,
      0x05,
      0x01, // memory section
      0x01, // limits type with max
      (byte) 0x80,
      (byte) 0x80,
      (byte) 0x80,
      0x08, // min = 1GB (invalid - too large)
      (byte) 0x80,
      (byte) 0x80,
      (byte) 0x80,
      0x04 // max = 512MB (less than min - invalid)
    };
  }

  /** Creates a WebAssembly module with circular imports (imports from itself). */
  private byte[] createModuleWithCircularImports() {
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version
      0x01,
      0x04,
      0x01, // type section
      0x60,
      0x00,
      0x00, // func type void -> void
      0x02,
      0x0a,
      0x01, // import section
      0x04,
      0x73,
      0x65,
      0x6c,
      0x66, // module "self"
      0x04,
      0x66,
      0x75,
      0x6e,
      0x63, // name "func"
      0x00,
      0x00 // func import type 0
    };
  }

  /** Creates a WebAssembly module with invalid bytecode. */
  private byte[] createModuleWithInvalidBytecode() {
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version
      0x01,
      0x04,
      0x01, // type section
      0x60,
      0x00,
      0x00, // func type void -> void
      0x03,
      0x02,
      0x01,
      0x00, // function section
      0x0a,
      0x06,
      0x01, // code section
      0x04,
      0x00, // func body length 4
      (byte) 0xFF,
      (byte) 0xFF, // invalid opcodes
      0x0b // end
    };
  }

  /** Generates a list of invalid WebAssembly modules for stress testing. */
  private List<byte[]> generateInvalidModules(final int count) {
    final List<byte[]> invalidModules = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      // Create various types of invalid modules
      switch (i % 5) {
        case 0:
          // Random bytes
          final byte[] random = new byte[20];
          Arrays.fill(random, (byte) (i & 0xFF));
          invalidModules.add(random);
          break;

        case 1:
          // Corrupted magic
          invalidModules.add(new byte[] {(byte) i, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00});
          break;

        case 2:
          // Corrupted version
          invalidModules.add(new byte[] {0x00, 0x61, 0x73, 0x6d, (byte) i, 0x00, 0x00, 0x00});
          break;

        case 3:
          // Truncated after header
          invalidModules.add(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01});
          break;

        default:
          // Invalid section
          invalidModules.add(
              new byte[] {
                0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, (byte) (0x80 + i), 0x01, (byte) i
              });
          break;
      }
    }

    return invalidModules;
  }
}
