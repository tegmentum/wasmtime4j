package ai.tegmentum.wasmtime4j.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
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
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Comprehensive tests for malformed WebAssembly modules. Tests various types of malformed modules,
 * validation errors, and error handling scenarios.
 */
@DisplayName("Module Malformed Scenarios Tests")
class ModuleMalformedIT extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleMalformedIT.class.getName());

  private WasmTestDataManager testDataManager;
  private ExecutorService executorService;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("module.malformed");

    try {
      testDataManager = WasmTestDataManager.getInstance();
      testDataManager.ensureTestDataAvailable();
      executorService = Executors.newFixedThreadPool(4);
    } catch (final Exception e) {
      LOGGER.warning("Failed to setup test environment: " + e.getMessage());
      skipIfNot(false, "Test environment setup failed: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDownExecutor() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        executorService.shutdownNow();
      }
    }
  }

  @Test
  @DisplayName("Should reject modules with corrupted magic numbers")
  void shouldRejectModulesWithCorruptedMagicNumbers() {
    final List<byte[]> corruptedMagicModules = createCorruptedMagicModules();

    for (int i = 0; i < corruptedMagicModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] corruptedModule = corruptedMagicModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-magic-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(corruptedModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Corrupted magic module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine("Corrupted magic " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should reject modules with invalid versions")
  void shouldRejectModulesWithInvalidVersions() {
    final List<byte[]> invalidVersionModules = createInvalidVersionModules();

    for (int i = 0; i < invalidVersionModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] invalidModule = invalidVersionModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-version-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(invalidModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Invalid version module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine("Invalid version " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should reject truncated modules")
  void shouldRejectTruncatedModules() {
    final List<byte[]> truncatedModules = createTruncatedModules();

    for (int i = 0; i < truncatedModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] truncatedModule = truncatedModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-truncated-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(truncatedModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Truncated module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine("Truncated module " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should reject modules with malformed sections")
  void shouldRejectModulesWithMalformedSections() {
    final List<byte[]> malformedSectionModules = createMalformedSectionModules();

    for (int i = 0; i < malformedSectionModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] malformedModule = malformedSectionModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-section-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(malformedModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Malformed section module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine("Malformed section " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should reject modules with invalid type sections")
  void shouldRejectModulesWithInvalidTypeSections() {
    final List<byte[]> invalidTypeModules = createInvalidTypeSectionModules();

    for (int i = 0; i < invalidTypeModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] invalidModule = invalidTypeModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-type-section-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(invalidModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Invalid type section module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine(
          "Invalid type section " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should reject modules with invalid function sections")
  void shouldRejectModulesWithInvalidFunctionSections() {
    final List<byte[]> invalidFunctionModules = createInvalidFunctionSectionModules();

    for (int i = 0; i < invalidFunctionModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] invalidModule = invalidFunctionModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-function-section-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(invalidModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Invalid function section module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine(
          "Invalid function section " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should reject modules with invalid code sections")
  void shouldRejectModulesWithInvalidCodeSections() {
    final List<byte[]> invalidCodeModules = createInvalidCodeSectionModules();

    for (int i = 0; i < invalidCodeModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] invalidModule = invalidCodeModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-code-section-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(invalidModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Invalid code section module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine(
          "Invalid code section " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should reject modules with invalid memory sections")
  void shouldRejectModulesWithInvalidMemorySections() {
    final List<byte[]> invalidMemoryModules = createInvalidMemorySectionModules();

    for (int i = 0; i < invalidMemoryModules.size(); i++) {
      final int moduleIndex = i;
      final byte[] invalidModule = invalidMemoryModules.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-memory-section-" + moduleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(invalidModule))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Invalid memory section module " + moduleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine(
          "Invalid memory section " + moduleIndex + " validation: " + validation.getSummary());
    }
  }

  @Test
  @DisplayName("Should handle completely random data")
  void shouldHandleCompletelyRandomData() {
    final List<byte[]> randomDataSamples = createRandomDataSamples();

    for (int i = 0; i < randomDataSamples.size(); i++) {
      final int sampleIndex = i;
      final byte[] randomData = randomDataSamples.get(i);

      final CrossRuntimeValidationResult validation =
          CrossRuntimeTestRunner.validateConsistency(
              "malformed-random-" + sampleIndex,
              runtime -> {
                try (final Engine engine = runtime.createEngine()) {
                  // When & Then
                  assertThatThrownBy(() -> engine.compileModule(randomData))
                      .isInstanceOfAny(
                          WasmException.class,
                          CompilationException.class,
                          ValidationException.class);

                  return "Random data " + sampleIndex + " correctly rejected";
                }
              },
              comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

      assertThat(validation.isConsistent()).isTrue();
      LOGGER.fine("Random data " + sampleIndex + " validation: " + validation.getSummary());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(CrossRuntimeTestRunner.RuntimeArgumentsProvider.class)
  @DisplayName("Should handle malformed module stress test")
  void shouldHandleMalformedModuleStressTest(final RuntimeType runtimeType) {
    // skipIfCategoryNotEnabled("stress");

    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "malformed-stress-" + runtimeType,
            runtime -> {
              final int stressTestCount = 100;
              final AtomicInteger rejectedCount = new AtomicInteger(0);
              final AtomicInteger processedCount = new AtomicInteger(0);

              try (final Engine engine = runtime.createEngine()) {
                // Generate various malformed modules
                final List<byte[]> malformedModules = generateMalformedModules(stressTestCount);

                for (final byte[] malformedModule : malformedModules) {
                  try {
                    engine.compileModule(malformedModule);
                    // If it doesn't throw, that's unexpected but log it
                    LOGGER.fine("Malformed module was unexpectedly accepted");
                  } catch (final WasmException e) {
                    rejectedCount.incrementAndGet();
                  } catch (final Exception e) {
                    // Other exceptions are also acceptable for malformed data
                    rejectedCount.incrementAndGet();
                  }
                  processedCount.incrementAndGet();
                }
              }

              // Most should be rejected
              final int rejected = rejectedCount.get();
              final int processed = processedCount.get();
              assertThat(rejected).isGreaterThan(processed * 3 / 4); // At least 75% rejected

              return "Stress test: " + rejected + "/" + processed + " malformed modules rejected";
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Malformed stress test for " + runtimeType + ": " + validation.getSummary());
  }

  @Test
  @DisplayName("Should handle concurrent malformed module validation")
  void shouldHandleConcurrentMalformedModuleValidation() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "malformed-concurrent",
            runtime -> {
              final int threadCount = 4;
              final int modulesPerThread = 10;
              final CountDownLatch startLatch = new CountDownLatch(1);
              final CountDownLatch completionLatch = new CountDownLatch(threadCount);
              final AtomicInteger totalRejected = new AtomicInteger(0);

              try (final Engine engine = runtime.createEngine()) {
                // When - Validate malformed modules concurrently
                for (int i = 0; i < threadCount; i++) {
                  final int threadId = i;
                  executorService.submit(
                      () -> {
                        int threadRejected = 0;
                        try {
                          startLatch.await();

                          for (int j = 0; j < modulesPerThread; j++) {
                            final byte[] malformedModule =
                                generateMalformedModule(threadId * modulesPerThread + j);

                            try {
                              engine.compileModule(malformedModule);
                              // If it doesn't throw, that's unexpected
                            } catch (final WasmException e) {
                              threadRejected++;
                            } catch (final Exception e) {
                              // Other exceptions are also acceptable
                              threadRejected++;
                            }
                          }
                        } catch (final InterruptedException e) {
                          Thread.currentThread().interrupt();
                        } finally {
                          totalRejected.addAndGet(threadRejected);
                          completionLatch.countDown();
                        }
                      });
                }

                // Start all threads
                startLatch.countDown();

                // Wait for completion
                final boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
                assertThat(completed).isTrue();

                // Then - Most should have been rejected
                final int totalModules = threadCount * modulesPerThread;
                final int rejected = totalRejected.get();
                assertThat(rejected).isGreaterThan(totalModules / 2);

                return "Concurrent validation: "
                    + rejected
                    + "/"
                    + totalModules
                    + " malformed modules rejected";
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Concurrent malformed validation: " + validation.getSummary());
  }

  @Test
  @DisplayName("Should provide meaningful error messages for malformed modules")
  void shouldProvideMeaningfulErrorMessagesForMalformedModules() {
    final CrossRuntimeValidationResult validation =
        CrossRuntimeTestRunner.validateConsistency(
            "malformed-error-messages",
            runtime -> {
              // Given - Various malformed modules with known issues
              final List<MalformedTestCase> testCases = createMalformedTestCases();

              try (final Engine engine = runtime.createEngine()) {
                int meaningfulErrors = 0;

                // When & Then
                for (final MalformedTestCase testCase : testCases) {
                  try {
                    engine.compileModule(testCase.moduleBytes);
                    // Should not reach here
                  } catch (final WasmException e) {
                    // Verify error message is not empty and somewhat meaningful
                    final String message = e.getMessage();
                    if (message != null && !message.trim().isEmpty() && message.length() > 5) {
                      meaningfulErrors++;
                    }
                    LOGGER.fine("Error for " + testCase.description + ": " + message);
                  }
                }

                // Most errors should have meaningful messages
                assertThat(meaningfulErrors).isGreaterThan(testCases.size() / 2);

                return "Meaningful errors: " + meaningfulErrors + "/" + testCases.size();
              }
            },
            comparison -> comparison.getJniExecution().equals(comparison.getPanamaExecution()));

    assertThat(validation.isConsistent()).isTrue();
    LOGGER.info("Error message validation: " + validation.getSummary());
  }

  /** Creates modules with corrupted magic numbers. */
  private List<byte[]> createCorruptedMagicModules() {
    final List<byte[]> modules = new ArrayList<>();

    // Various magic number corruptions
    modules.add(new byte[] {0x01, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00}); // Wrong first byte
    modules.add(new byte[] {0x00, 0x62, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00}); // Wrong second byte
    modules.add(new byte[] {0x00, 0x61, 0x74, 0x6d, 0x01, 0x00, 0x00, 0x00}); // Wrong third byte
    modules.add(new byte[] {0x00, 0x61, 0x73, 0x6e, 0x01, 0x00, 0x00, 0x00}); // Wrong fourth byte
    modules.add(
        new byte[] {
          (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x01, 0x00, 0x00, 0x00
        }); // All wrong

    return modules;
  }

  /** Creates modules with invalid versions. */
  private List<byte[]> createInvalidVersionModules() {
    final List<byte[]> modules = new ArrayList<>();

    // Various version corruptions
    modules.add(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x02, 0x00, 0x00, 0x00}); // Version 2
    modules.add(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x00, 0x00, 0x00, 0x00}); // Version 0
    modules.add(new byte[] {0x00, 0x61, 0x73, 0x6d, (byte) 0xFF, 0x00, 0x00, 0x00}); // High version
    modules.add(new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x01, 0x00, 0x00}); // Wrong minor version

    return modules;
  }

  /** Creates truncated modules. */
  private List<byte[]> createTruncatedModules() {
    final List<byte[]> modules = new ArrayList<>();
    final byte[] validStart = TestUtils.createSimpleWasmModule();

    // Truncate at various points
    modules.add(new byte[0]); // Empty
    modules.add(Arrays.copyOf(validStart, 1)); // Only one byte
    modules.add(Arrays.copyOf(validStart, 4)); // Only magic
    modules.add(Arrays.copyOf(validStart, 7)); // Incomplete version
    modules.add(Arrays.copyOf(validStart, validStart.length / 2)); // Half module

    return modules;
  }

  /** Creates modules with malformed sections. */
  private List<byte[]> createMalformedSectionModules() {
    final List<byte[]> modules = new ArrayList<>();

    // Section with invalid size
    modules.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d,
          0x01,
          0x00,
          0x00,
          0x00, // Header
          0x01,
          (byte) 0xFF,
          (byte) 0xFF,
          (byte) 0xFF,
          (byte) 0xFF // Invalid section size
        });

    // Section with size larger than remaining bytes
    modules.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d,
          0x01,
          0x00,
          0x00,
          0x00, // Header
          0x01,
          0x10,
          0x00,
          0x01 // Size 16 but only 2 bytes follow
        });

    return modules;
  }

  /** Creates modules with invalid type sections. */
  private List<byte[]> createInvalidTypeSectionModules() {
    final List<byte[]> modules = new ArrayList<>();

    // Invalid function type
    modules.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d,
          0x01,
          0x00,
          0x00,
          0x00, // Header
          0x01,
          0x05,
          0x01, // Type section
          (byte) 0xFF,
          0x00,
          0x00 // Invalid type indicator
        });

    return modules;
  }

  /** Creates modules with invalid function sections. */
  private List<byte[]> createInvalidFunctionSectionModules() {
    final List<byte[]> modules = new ArrayList<>();

    // Function section referencing non-existent type
    modules.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d,
          0x01,
          0x00,
          0x00,
          0x00, // Header
          0x01,
          0x04,
          0x01,
          0x60,
          0x00,
          0x00, // Valid type section
          0x03,
          0x02,
          0x01,
          (byte) 0xFF // Function section with invalid type index
        });

    return modules;
  }

  /** Creates modules with invalid code sections. */
  private List<byte[]> createInvalidCodeSectionModules() {
    final List<byte[]> modules = new ArrayList<>();

    // Code section with invalid instruction
    modules.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d,
          0x01,
          0x00,
          0x00,
          0x00, // Header
          0x01,
          0x04,
          0x01,
          0x60,
          0x00,
          0x00, // Type section
          0x03,
          0x02,
          0x01,
          0x00, // Function section
          0x0a,
          0x06,
          0x01,
          0x04,
          0x00,
          (byte) 0xFF,
          (byte) 0xFF,
          0x0b // Invalid instruction
        });

    return modules;
  }

  /** Creates modules with invalid memory sections. */
  private List<byte[]> createInvalidMemorySectionModules() {
    final List<byte[]> modules = new ArrayList<>();

    // Memory with invalid limits
    modules.add(
        new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d,
          0x01,
          0x00,
          0x00,
          0x00, // Header
          0x05,
          0x05,
          0x01,
          0x01, // Memory section
          (byte) 0x80,
          (byte) 0x80,
          (byte) 0x80,
          (byte) 0x80,
          0x10 // Invalid min size (too large)
        });

    return modules;
  }

  /** Creates random data samples. */
  private List<byte[]> createRandomDataSamples() {
    final List<byte[]> samples = new ArrayList<>();
    final Random random = new Random(12345); // Fixed seed for reproducibility

    for (int i = 0; i < 10; i++) {
      final byte[] randomData = new byte[16 + random.nextInt(48)];
      random.nextBytes(randomData);
      samples.add(randomData);
    }

    return samples;
  }

  /** Generates malformed modules for stress testing. */
  private List<byte[]> generateMalformedModules(final int count) {
    final List<byte[]> modules = new ArrayList<>();

    // Combine all types of malformed modules
    modules.addAll(createCorruptedMagicModules());
    modules.addAll(createInvalidVersionModules());
    modules.addAll(createTruncatedModules());
    modules.addAll(createMalformedSectionModules());
    modules.addAll(createRandomDataSamples());

    // Fill remaining with generated malformed modules
    final Random random = new Random(54321);
    while (modules.size() < count) {
      modules.add(generateMalformedModule(random.nextInt()));
    }

    return modules.subList(0, Math.min(count, modules.size()));
  }

  /** Generates a single malformed module based on seed. */
  private byte[] generateMalformedModule(final int seed) {
    final Random random = new Random(seed);

    switch (random.nextInt(5)) {
      case 0:
        // Corrupted header
        final byte[] header = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};
        header[random.nextInt(8)] = (byte) random.nextInt(256);
        return header;

      case 1:
        // Random short data
        final byte[] shortData = new byte[4 + random.nextInt(12)];
        random.nextBytes(shortData);
        return shortData;

      case 2:
        // Invalid section
        return new byte[] {
          0x00,
          0x61,
          0x73,
          0x6d,
          0x01,
          0x00,
          0x00,
          0x00, // Header
          (byte) (0x80 + random.nextInt(127)), // Invalid section ID
          (byte) random.nextInt(10),
          (byte) random.nextInt(256)
        };

      case 3:
        // Truncated valid module
        final byte[] validModule = TestUtils.createSimpleWasmModule();
        final int truncateAt = 8 + random.nextInt(Math.max(1, validModule.length - 8));
        return Arrays.copyOf(validModule, truncateAt);

      default:
        // Completely random
        final byte[] randomData = new byte[8 + random.nextInt(24)];
        random.nextBytes(randomData);
        return randomData;
    }
  }

  /** Creates malformed test cases with descriptions. */
  private List<MalformedTestCase> createMalformedTestCases() {
    final List<MalformedTestCase> testCases = new ArrayList<>();

    testCases.add(
        new MalformedTestCase(
            "Invalid magic number", new byte[] {0x01, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00}));

    testCases.add(
        new MalformedTestCase(
            "Invalid version", new byte[] {0x00, 0x61, 0x73, 0x6d, 0x02, 0x00, 0x00, 0x00}));

    testCases.add(new MalformedTestCase("Truncated module", new byte[] {0x00, 0x61, 0x73, 0x6d}));

    testCases.add(
        new MalformedTestCase(
            "Invalid section size",
            new byte[] {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, (byte) 0xFF}));

    return testCases;
  }

  /** Test case for malformed modules. */
  private static class MalformedTestCase {
    final String description;
    final byte[] moduleBytes;

    MalformedTestCase(final String description, final byte[] moduleBytes) {
      this.description = description;
      this.moduleBytes = moduleBytes.clone();
    }
  }
}
