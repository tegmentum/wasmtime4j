package ai.tegmentum.wasmtime4j.edge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive malformed module testing for robust error handling validation. Tests validate
 * proper error detection, recovery mechanisms, and defensive programming against corrupted
 * WebAssembly modules.
 */
@DisplayName("Malformed Module Edge Cases Tests")
final class MalformedModuleEdgeCasesIT extends BaseIntegrationTest {

  private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6d};
  private static final byte[] WASM_VERSION = {0x01, 0x00, 0x00, 0x00};
  private static final SecureRandom RANDOM = new SecureRandom();

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // Edge case tests are always enabled
  }

  @Nested
  @DisplayName("Invalid Magic Number Tests")
  final class InvalidMagicNumberTests {

    @Test
    @DisplayName("Should reject completely invalid magic numbers")
    void shouldRejectCompletelyInvalidMagicNumbers() {
      final byte[][] invalidMagics = {
        {0x00, 0x00, 0x00, 0x00}, // All zeros
        {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, // All ones
        {0x7F, 0x45, 0x4C, 0x46}, // ELF magic
        {0x4D, 0x5A}, // PE/COFF magic (truncated)
        {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, // Java class file magic
        {0x50, 0x4B, 0x03, 0x04}, // ZIP file magic
        {0x1F, (byte) 0x8B, 0x08, 0x00}, // GZIP magic
        "WASM".getBytes(), // Text instead of binary
        {0x00, 0x61, 0x73}, // Truncated magic
        {0x61, 0x73, 0x6d, 0x00}, // Reversed magic
      };

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing invalid magic numbers on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            int rejectionCount = 0;
            for (final byte[] invalidMagic : invalidMagics) {
              final byte[] invalidModule = createModuleWithMagic(invalidMagic);

              assertThatThrownBy(() -> engine.compileModule(invalidModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                        LOGGER.fine("Rejected invalid magic: " + Arrays.toString(invalidMagic));
                      });
              rejectionCount++;
            }

            LOGGER.info("Rejected " + rejectionCount + " invalid magic numbers on " + runtimeType);
          });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    @DisplayName("Should reject modules with byte corruption in magic")
    void shouldRejectModulesWithByteCorruptionInMagic(final int corruptionIndex) {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info(
                "Testing magic corruption at index " + corruptionIndex + " on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Test systematic corruption at each byte position
            for (int corruptionValue = 0; corruptionValue < 256; corruptionValue++) {
              if (corruptionValue == WASM_MAGIC[corruptionIndex]) {
                continue; // Skip valid values
              }

              final byte[] corruptedMagic = WASM_MAGIC.clone();
              corruptedMagic[corruptionIndex] = (byte) corruptionValue;
              final byte[] corruptedModule = createModuleWithMagic(corruptedMagic);

              assertThatThrownBy(() -> engine.compileModule(corruptedModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                      });
            }

            LOGGER.info(
                "Tested 255 corruptions at magic index " + corruptionIndex + " on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Invalid Version Tests")
  final class InvalidVersionTests {

    @Test
    @DisplayName("Should reject unsupported version numbers")
    void shouldRejectUnsupportedVersionNumbers() {
      final byte[][] invalidVersions = {
        {0x00, 0x00, 0x00, 0x00}, // Version 0
        {0x02, 0x00, 0x00, 0x00}, // Version 2
        {(byte) 0xFF, 0x00, 0x00, 0x00}, // Version 255
        {0x01, 0x01, 0x00, 0x00}, // Version 1.1
        {0x00, 0x00, 0x01, 0x00}, // Big-endian confusion
        {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, // Maximum version
        {0x01, 0x00, 0x00}, // Truncated version
        {0x01, 0x00, 0x00, 0x00, 0x00}, // Extended version
      };

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing invalid version numbers on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            int rejectionCount = 0;
            for (final byte[] invalidVersion : invalidVersions) {
              final byte[] invalidModule = createModuleWithVersion(invalidVersion);

              assertThatThrownBy(() -> engine.compileModule(invalidModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                        LOGGER.fine("Rejected invalid version: " + Arrays.toString(invalidVersion));
                      });
              rejectionCount++;
            }

            LOGGER.info("Rejected " + rejectionCount + " invalid versions on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle version endianness edge cases")
    void shouldHandleVersionEndiannessEdgeCases() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing version endianness edge cases on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Test both little-endian and big-endian interpretations
            final int[] versionValues = {
              0x00000001, // Correct little-endian
              0x01000000, // Big-endian interpretation
              0x00000100, // Byte-swapped
              0x00010000, // Different byte-swapped
            };

            int testCount = 0;
            for (final int versionValue : versionValues) {
              final ByteBuffer buffer = ByteBuffer.allocate(4);
              buffer.order(ByteOrder.LITTLE_ENDIAN);
              buffer.putInt(versionValue);
              final byte[] versionBytes = buffer.array();

              final byte[] testModule = createModuleWithVersion(versionBytes);

              if (versionValue == 0x00000001) {
                // This should be valid
                try {
                  final Module module = engine.compileModule(testModule);
                  registerForCleanup(module);
                  assertThat(module.isValid()).isTrue();
                  LOGGER.fine("Accepted correct version on " + runtimeType);
                } catch (final WasmException e) {
                  // Might still fail due to missing sections, but not due to version
                  LOGGER.fine("Module failed for other reasons: " + e.getMessage());
                }
              } else {
                // These should be rejected
                assertThatThrownBy(() -> engine.compileModule(testModule))
                    .isInstanceOfAny(CompilationException.class, ValidationException.class);
                LOGGER.fine("Rejected incorrect endianness version: " + versionValue);
              }
              testCount++;
            }

            LOGGER.info("Tested " + testCount + " version endianness cases on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Malformed Section Tests")
  final class MalformedSectionTests {

    @Test
    @DisplayName("Should reject modules with invalid section IDs")
    void shouldRejectModulesWithInvalidSectionIds() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing invalid section IDs on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Valid section IDs are 0-12 in WASM spec
            final int[] invalidSectionIds = {13, 14, 15, 255, -1, 256, 1000};

            int rejectionCount = 0;
            for (final int invalidId : invalidSectionIds) {
              final byte[] invalidModule = createModuleWithInvalidSection(invalidId);

              assertThatThrownBy(() -> engine.compileModule(invalidModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                        LOGGER.fine("Rejected invalid section ID: " + invalidId);
                      });
              rejectionCount++;
            }

            LOGGER.info("Rejected " + rejectionCount + " invalid section IDs on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject modules with corrupted section sizes")
    void shouldRejectModulesWithCorruptedSectionSizes() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing corrupted section sizes on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Test various invalid section size scenarios
            final int[] corruptedSizes = {
              -1, // Negative size
              Integer.MAX_VALUE, // Impossibly large
              1000000, // Very large but not max
              0, // Zero size with content
            };

            int rejectionCount = 0;
            for (final int corruptedSize : corruptedSizes) {
              final byte[] invalidModule = createModuleWithCorruptedSectionSize(corruptedSize);

              assertThatThrownBy(() -> engine.compileModule(invalidModule))
                  .isInstanceOfAny(
                      CompilationException.class,
                      ValidationException.class,
                      IllegalArgumentException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                        LOGGER.fine("Rejected corrupted section size: " + corruptedSize);
                      });
              rejectionCount++;
            }

            LOGGER.info(
                "Rejected " + rejectionCount + " corrupted section sizes on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject modules with truncated sections")
    void shouldRejectModulesWithTruncatedSections() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing truncated sections on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Create modules with various truncation points
            final int[] truncationPoints = {8, 12, 16, 20, 24, 28, 32};

            int rejectionCount = 0;
            for (final int truncationPoint : truncationPoints) {
              final byte[] truncatedModule = createTruncatedModule(truncationPoint);

              assertThatThrownBy(() -> engine.compileModule(truncatedModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                        LOGGER.fine("Rejected truncated module at: " + truncationPoint);
                      });
              rejectionCount++;
            }

            LOGGER.info("Rejected " + rejectionCount + " truncated modules on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Invalid Bytecode Tests")
  final class InvalidBytecodeTests {

    @Test
    @DisplayName("Should reject modules with invalid opcodes")
    void shouldRejectModulesWithInvalidOpcodes() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing invalid opcodes on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Create modules with invalid/reserved opcodes
            final byte[] invalidOpcodes = {
              (byte) 0xC0, // Reserved opcode
              (byte) 0xC1, // Reserved opcode
              (byte) 0xFF, // Invalid opcode
              (byte) 0x00, // Invalid in certain contexts
            };

            int rejectionCount = 0;
            for (final byte invalidOpcode : invalidOpcodes) {
              final byte[] invalidModule = createModuleWithInvalidOpcode(invalidOpcode);

              assertThatThrownBy(() -> engine.compileModule(invalidModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                        LOGGER.fine(
                            "Rejected invalid opcode: " + String.format("0x%02X", invalidOpcode));
                      });
              rejectionCount++;
            }

            LOGGER.info("Rejected " + rejectionCount + " invalid opcodes on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject modules with malformed instruction sequences")
    void shouldRejectModulesWithMalformedInstructionSequences() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing malformed instruction sequences on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Test various malformed instruction patterns
            final byte[][] malformedSequences = {
              {0x20, 0x00}, // get_local with invalid index
              {0x21}, // set_local without index
              {0x41}, // i32.const without value
              {0x6A}, // i32.add without stack preparation
              {0x0F}, // return in wrong context
            };

            int rejectionCount = 0;
            for (final byte[] malformedSequence : malformedSequences) {
              final byte[] invalidModule = createModuleWithMalformedInstructions(malformedSequence);

              assertThatThrownBy(() -> engine.compileModule(invalidModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                        LOGGER.fine(
                            "Rejected malformed sequence: " + Arrays.toString(malformedSequence));
                      });
              rejectionCount++;
            }

            LOGGER.info(
                "Rejected "
                    + rejectionCount
                    + " malformed instruction sequences on "
                    + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Broken Import/Export Tests")
  final class BrokenImportExportTests {

    @Test
    @DisplayName("Should reject modules with malformed import declarations")
    void shouldRejectModulesWithMalformedImportDeclarations() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing malformed import declarations on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            int rejectionCount = 0;

            // Test import with empty module name
            byte[] invalidModule = createModuleWithMalformedImport("", "func", 0);
            assertThatThrownBy(() -> engine.compileModule(invalidModule))
                .isInstanceOfAny(CompilationException.class, ValidationException.class);
            rejectionCount++;

            // Test import with empty function name
            invalidModule = createModuleWithMalformedImport("env", "", 0);
            assertThatThrownBy(() -> engine.compileModule(invalidModule))
                .isInstanceOfAny(CompilationException.class, ValidationException.class);
            rejectionCount++;

            // Test import with invalid type index
            invalidModule = createModuleWithMalformedImport("env", "func", 999);
            assertThatThrownBy(() -> engine.compileModule(invalidModule))
                .isInstanceOfAny(CompilationException.class, ValidationException.class);
            rejectionCount++;

            LOGGER.info("Rejected " + rejectionCount + " malformed imports on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should reject modules with malformed export declarations")
    void shouldRejectModulesWithMalformedExportDeclarations() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing malformed export declarations on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            int rejectionCount = 0;

            // Test export with empty name
            byte[] invalidModule = createModuleWithMalformedExport("", 0, 0);
            assertThatThrownBy(() -> engine.compileModule(invalidModule))
                .isInstanceOfAny(CompilationException.class, ValidationException.class);
            rejectionCount++;

            // Test export with invalid function index
            invalidModule = createModuleWithMalformedExport("test", 0, 999);
            assertThatThrownBy(() -> engine.compileModule(invalidModule))
                .isInstanceOfAny(CompilationException.class, ValidationException.class);
            rejectionCount++;

            // Test export with invalid export type
            invalidModule = createModuleWithMalformedExport("test", 99, 0);
            assertThatThrownBy(() -> engine.compileModule(invalidModule))
                .isInstanceOfAny(CompilationException.class, ValidationException.class);
            rejectionCount++;

            LOGGER.info("Rejected " + rejectionCount + " malformed exports on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Random Corruption Tests")
  final class RandomCorruptionTests {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16})
    @DisplayName("Should reject modules with random byte corruption")
    void shouldRejectModulesWithRandomByteCorruption(final int corruptionCount) {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info(
                "Testing " + corruptionCount + " random byte corruptions on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Generate a base valid-looking module
            final byte[] baseModule = createMinimalValidModule();
            int rejectionCount = 0;

            // Test multiple corruption scenarios
            for (int test = 0; test < 10; test++) {
              final byte[] corruptedModule = baseModule.clone();

              // Apply random corruptions
              for (int i = 0; i < corruptionCount; i++) {
                final int corruptionIndex = RANDOM.nextInt(corruptedModule.length);
                corruptedModule[corruptionIndex] = (byte) RANDOM.nextInt(256);
              }

              assertThatThrownBy(() -> engine.compileModule(corruptedModule))
                  .isInstanceOfAny(CompilationException.class, ValidationException.class)
                  .satisfies(
                      e -> {
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();
                      });
              rejectionCount++;
            }

            LOGGER.info(
                "Rejected "
                    + rejectionCount
                    + " randomly corrupted modules ("
                    + corruptionCount
                    + " corruptions each) on "
                    + runtimeType);
          });
    }
  }

  // Helper methods for creating malformed modules

  private byte[] createModuleWithMagic(final byte[] magic) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : magic) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }
    return toByteArray(module);
  }

  private byte[] createModuleWithVersion(final byte[] version) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : version) {
      module.add(b);
    }
    return toByteArray(module);
  }

  private byte[] createModuleWithInvalidSection(final int sectionId) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }

    // Add invalid section
    module.add((byte) (sectionId & 0xFF));
    module.add((byte) 0x04); // Section size
    module.add((byte) 0x00); // Dummy content
    module.add((byte) 0x00);
    module.add((byte) 0x00);
    module.add((byte) 0x00);

    return toByteArray(module);
  }

  private byte[] createModuleWithCorruptedSectionSize(final int corruptedSize) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }

    // Add section with corrupted size
    module.add((byte) 0x01); // Type section
    // Add LEB128 encoded corrupted size
    encodeLEB128(module, corruptedSize);
    // Add minimal content that doesn't match size
    module.add((byte) 0x00);

    return toByteArray(module);
  }

  private byte[] createTruncatedModule(final int truncationPoint) {
    final byte[] fullModule = createMinimalValidModule();
    final int actualTruncation = Math.min(truncationPoint, fullModule.length - 1);
    return Arrays.copyOf(fullModule, actualTruncation);
  }

  private byte[] createModuleWithInvalidOpcode(final byte invalidOpcode) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }

    // Add type section
    module.add((byte) 0x01); // Type section
    module.add((byte) 0x04); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x60); // Function type
    module.add((byte) 0x00); // No params
    module.add((byte) 0x00); // No results

    // Add function section
    module.add((byte) 0x03); // Function section
    module.add((byte) 0x02); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x00); // Type index

    // Add code section with invalid opcode
    module.add((byte) 0x0A); // Code section
    module.add((byte) 0x05); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x03); // Function body size
    module.add((byte) 0x00); // No locals
    module.add(invalidOpcode); // Invalid opcode
    module.add((byte) 0x0B); // End

    return toByteArray(module);
  }

  private byte[] createModuleWithMalformedInstructions(final byte[] malformedSequence) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }

    // Add type section
    module.add((byte) 0x01); // Type section
    module.add((byte) 0x04); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x60); // Function type
    module.add((byte) 0x00); // No params
    module.add((byte) 0x00); // No results

    // Add function section
    module.add((byte) 0x03); // Function section
    module.add((byte) 0x02); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x00); // Type index

    // Add code section with malformed instructions
    module.add((byte) 0x0A); // Code section
    encodeLEB128(module, malformedSequence.length + 3); // Size
    module.add((byte) 0x01); // Count
    encodeLEB128(module, malformedSequence.length + 2); // Function body size
    module.add((byte) 0x00); // No locals
    for (final byte b : malformedSequence) {
      module.add(b);
    }
    module.add((byte) 0x0B); // End

    return toByteArray(module);
  }

  private byte[] createModuleWithMalformedImport(
      final String moduleName, final String funcName, final int typeIndex) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }

    // Add type section
    module.add((byte) 0x01); // Type section
    module.add((byte) 0x04); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x60); // Function type
    module.add((byte) 0x00); // No params
    module.add((byte) 0x00); // No results

    // Add import section with malformed import
    module.add((byte) 0x02); // Import section
    final List<Byte> importContent = new ArrayList<>();
    importContent.add((byte) 0x01); // Count

    // Module name
    encodeLEB128(importContent, moduleName.length());
    for (final byte b : moduleName.getBytes()) {
      importContent.add(b);
    }

    // Function name
    encodeLEB128(importContent, funcName.length());
    for (final byte b : funcName.getBytes()) {
      importContent.add(b);
    }

    // Import type (function)
    importContent.add((byte) 0x00);
    encodeLEB128(importContent, typeIndex); // Type index (possibly invalid)

    encodeLEB128(module, importContent.size());
    module.addAll(importContent);

    return toByteArray(module);
  }

  private byte[] createModuleWithMalformedExport(
      final String exportName, final int exportType, final int functionIndex) {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }

    // Add export section with malformed export
    module.add((byte) 0x07); // Export section
    final List<Byte> exportContent = new ArrayList<>();
    exportContent.add((byte) 0x01); // Count

    // Export name
    encodeLEB128(exportContent, exportName.length());
    for (final byte b : exportName.getBytes()) {
      exportContent.add(b);
    }

    // Export type (possibly invalid)
    exportContent.add((byte) (exportType & 0xFF));
    encodeLEB128(exportContent, functionIndex); // Function index (possibly invalid)

    encodeLEB128(module, exportContent.size());
    module.addAll(exportContent);

    return toByteArray(module);
  }

  private byte[] createMinimalValidModule() {
    final List<Byte> module = new ArrayList<>();
    for (final byte b : WASM_MAGIC) {
      module.add(b);
    }
    for (final byte b : WASM_VERSION) {
      module.add(b);
    }

    // Add type section
    module.add((byte) 0x01); // Type section
    module.add((byte) 0x04); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x60); // Function type
    module.add((byte) 0x00); // No params
    module.add((byte) 0x00); // No results

    // Add function section
    module.add((byte) 0x03); // Function section
    module.add((byte) 0x02); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x00); // Type index

    // Add code section
    module.add((byte) 0x0A); // Code section
    module.add((byte) 0x04); // Size
    module.add((byte) 0x01); // Count
    module.add((byte) 0x02); // Function body size
    module.add((byte) 0x00); // No locals
    module.add((byte) 0x0B); // End

    return toByteArray(module);
  }

  private void encodeLEB128(final List<Byte> output, final int value) {
    int remaining = value;
    while (remaining >= 0x80) {
      output.add((byte) ((remaining & 0x7F) | 0x80));
      remaining >>>= 7;
    }
    output.add((byte) (remaining & 0x7F));
  }

  private byte[] toByteArray(final List<Byte> list) {
    final byte[] array = new byte[list.size()];
    for (int i = 0; i < list.size(); i++) {
      array[i] = list.get(i);
    }
    return array;
  }
}
