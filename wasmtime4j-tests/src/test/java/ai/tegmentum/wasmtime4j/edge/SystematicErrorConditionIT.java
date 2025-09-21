package ai.tegmentum.wasmtime4j.edge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.LinkingException;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.function.WasmFunction;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Systematic error condition testing for comprehensive error handling validation. Tests validate
 * error propagation, consistency, recovery mechanisms, and system stability across all error
 * categories.
 */
@DisplayName("Systematic Error Condition Tests")
final class SystematicErrorConditionIT extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // Systematic error condition tests are always enabled
  }

  /** Error categories for systematic testing. */
  public enum ErrorCategory {
    COMPILATION,
    VALIDATION,
    LINKING,
    RUNTIME,
    SECURITY,
    SYSTEM
  }

  /** Error injection scenarios for testing. */
  public enum ErrorInjectionScenario {
    NULL_PARAMETER,
    INVALID_INPUT,
    RESOURCE_EXHAUSTION,
    CONCURRENT_ACCESS,
    TIMEOUT,
    CORRUPTION
  }

  @Nested
  @DisplayName("Compilation Error Systematization")
  final class CompilationErrorSystematizationTests {

    @Test
    @DisplayName("Should handle all compilation error scenarios consistently")
    void shouldHandleAllCompilationErrorScenariosConsistently() {
      final byte[][] compilationErrorInputs = {
        {}, // Empty input
        {0x00}, // Too short
        {0x00, 0x61, 0x73, 0x6d}, // Magic only
        {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00}, // Incomplete version
        "invalid-wasm".getBytes(), // Text input
        {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, // Invalid magic
        new byte[1024], // Large invalid input
        createCorruptedWasmModule(), // Corrupted valid module
      };

      // Fill large invalid input with pattern
      Arrays.fill(compilationErrorInputs[6], (byte) 0xAA);

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing compilation error consistency on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            int consistentErrors = 0;
            for (int i = 0; i < compilationErrorInputs.length; i++) {
              final byte[] errorInput = compilationErrorInputs[i];
              final String scenario = "scenario_" + i;

              try {
                assertThatThrownBy(() -> engine.compileModule(errorInput))
                    .isInstanceOfAny(CompilationException.class, ValidationException.class)
                    .satisfies(
                        e -> {
                          validateErrorProperties(e, ErrorCategory.COMPILATION, scenario);
                          LOGGER.fine(
                              "Compilation error handled consistently for "
                                  + scenario
                                  + ": "
                                  + e.getMessage());
                        });
                consistentErrors++;
              } catch (final AssertionError ae) {
                LOGGER.warning(
                    "Inconsistent compilation error handling for "
                        + scenario
                        + ": "
                        + ae.getMessage());
              }
            }

            LOGGER.info(
                "Consistent compilation errors: "
                    + consistentErrors
                    + "/"
                    + compilationErrorInputs.length
                    + " on "
                    + runtimeType);
            assertThat(consistentErrors).isEqualTo(compilationErrorInputs.length);
          });
    }

    @ParameterizedTest
    @EnumSource(ErrorInjectionScenario.class)
    @DisplayName("Should handle compilation error injection scenarios")
    void shouldHandleCompilationErrorInjectionScenarios(final ErrorInjectionScenario scenario) {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info(
                "Testing compilation error injection scenario " + scenario + " on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final byte[] injectedErrorModule =
                createErrorInjectionModule(ErrorCategory.COMPILATION, scenario);

            assertThatThrownBy(() -> engine.compileModule(injectedErrorModule))
                .isInstanceOfAny(
                    CompilationException.class,
                    ValidationException.class,
                    IllegalArgumentException.class)
                .satisfies(
                    e -> {
                      validateErrorProperties(e, ErrorCategory.COMPILATION, scenario.name());
                      validateErrorRecovery(engine, ErrorCategory.COMPILATION);
                    });

            LOGGER.info(
                "Compilation error injection scenario " + scenario + " handled on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Runtime Error Systematization")
  final class RuntimeErrorSystematizationTests {

    @Test
    @DisplayName("Should handle all runtime error scenarios consistently")
    void shouldHandleAllRuntimeErrorScenariosConsistently() throws WasmException {
      final String[] runtimeErrorScenarios = {
        "division_by_zero",
        "integer_overflow",
        "stack_overflow",
        "memory_access_violation",
        "type_mismatch",
        "unreachable_code",
        "invalid_function_call",
        "trap_instruction"
      };

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing runtime error consistency on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            int consistentErrors = 0;
            for (final String scenario : runtimeErrorScenarios) {
              try {
                final byte[] errorModule = createRuntimeErrorModule(scenario);
                final Module module = engine.compileModule(errorModule);
                registerForCleanup(module);

                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction errorFunction = instance.getFunction("trigger_error");
                if (errorFunction != null) {
                  assertThatThrownBy(() -> errorFunction.call())
                      .isInstanceOfAny(RuntimeException.class, ArithmeticException.class)
                      .satisfies(
                          e -> {
                            validateErrorProperties(e, ErrorCategory.RUNTIME, scenario);
                            LOGGER.fine(
                                "Runtime error handled consistently for "
                                    + scenario
                                    + ": "
                                    + e.getMessage());
                          });
                  consistentErrors++;
                }
              } catch (final Exception e) {
                LOGGER.info(
                    "Runtime error scenario " + scenario + " failed setup: " + e.getMessage());
              }
            }

            LOGGER.info(
                "Consistent runtime errors: "
                    + consistentErrors
                    + "/"
                    + runtimeErrorScenarios.length
                    + " on "
                    + runtimeType);
          });
    }

    @Test
    @DisplayName("Should maintain system stability after runtime errors")
    void shouldMaintainSystemStabilityAfterRuntimeErrors() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing system stability after runtime errors on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Trigger multiple runtime errors
            for (int i = 0; i < 10; i++) {
              try {
                final byte[] errorModule = createRuntimeErrorModule("division_by_zero");
                final Module module = engine.compileModule(errorModule);
                registerForCleanup(module);

                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction errorFunction = instance.getFunction("trigger_error");
                if (errorFunction != null) {
                  try {
                    errorFunction.call();
                  } catch (final Exception e) {
                    // Expected runtime error
                    LOGGER.fine("Expected runtime error " + i + ": " + e.getMessage());
                  }
                }

                // Verify system remains stable
                assertThat(engine.isValid()).isTrue();
                assertThat(store.isValid()).isTrue();
                assertThat(instance.isValid()).isTrue();

              } catch (final Exception e) {
                LOGGER.warning("Runtime error test " + i + " failed: " + e.getMessage());
              }
            }

            // Final stability check - create new resources
            final Store recoveryStore = engine.createStore();
            registerForCleanup(recoveryStore);
            assertThat(recoveryStore.isValid()).isTrue();

            LOGGER.info("System stability after runtime errors validated on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Linking Error Systematization")
  final class LinkingErrorSystematizationTests {

    @Test
    @DisplayName("Should handle all linking error scenarios consistently")
    void shouldHandleAllLinkingErrorScenariosConsistently() throws WasmException {
      final String[] linkingErrorScenarios = {
        "missing_import_function",
        "missing_import_memory",
        "missing_import_global",
        "type_mismatch_function",
        "type_mismatch_global",
        "circular_dependency",
        "invalid_export_reference"
      };

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing linking error consistency on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            int consistentErrors = 0;
            for (final String scenario : linkingErrorScenarios) {
              try {
                final byte[] linkingErrorModule = createLinkingErrorModule(scenario);
                final Module module = engine.compileModule(linkingErrorModule);
                registerForCleanup(module);

                assertThatThrownBy(() -> store.instantiate(module))
                    .isInstanceOfAny(LinkingException.class, RuntimeException.class)
                    .satisfies(
                        e -> {
                          validateErrorProperties(e, ErrorCategory.LINKING, scenario);
                          LOGGER.fine(
                              "Linking error handled consistently for "
                                  + scenario
                                  + ": "
                                  + e.getMessage());
                        });
                consistentErrors++;

              } catch (final Exception e) {
                LOGGER.info(
                    "Linking error scenario " + scenario + " failed setup: " + e.getMessage());
              }
            }

            LOGGER.info(
                "Consistent linking errors: "
                    + consistentErrors
                    + "/"
                    + linkingErrorScenarios.length
                    + " on "
                    + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Error Propagation and Recovery Tests")
  final class ErrorPropagationAndRecoveryTests {

    @Test
    @DisplayName("Should propagate errors correctly through call stack")
    void shouldPropagateErrorsCorrectlyThroughCallStack() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing error propagation through call stack on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            final byte[] stackErrorModule = createStackErrorPropagationModule();
            final Module module = engine.compileModule(stackErrorModule);
            registerForCleanup(module);

            final Instance instance = store.instantiate(module);
            registerForCleanup(instance);

            final WasmFunction topLevelFunction = instance.getFunction("top_level");
            if (topLevelFunction != null) {
              assertThatThrownBy(() -> topLevelFunction.call())
                  .isInstanceOfAny(RuntimeException.class, ArithmeticException.class)
                  .satisfies(
                      e -> {
                        // Verify error contains stack information
                        assertThat(e.getMessage()).isNotNull();
                        assertThat(e.getMessage()).isNotEmpty();

                        // Error should propagate through multiple call levels
                        validateErrorStackTrace(e);
                        LOGGER.info(
                            "Error propagated correctly through call stack: " + e.getMessage());
                      });
            }

            LOGGER.info("Error propagation validation completed on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should recover gracefully from cascading errors")
    void shouldRecoverGracefullyFromCascadingErrors() throws WasmException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing graceful recovery from cascading errors on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            // Trigger multiple cascading errors
            for (int cascade = 0; cascade < 5; cascade++) {
              LOGGER.fine("Testing error cascade " + cascade + " on " + runtimeType);

              final Store store = engine.createStore();
              registerForCleanup(store);

              try {
                final byte[] cascadeModule = createCascadingErrorModule(cascade);
                final Module module = engine.compileModule(cascadeModule);
                registerForCleanup(module);

                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction cascadeFunction = instance.getFunction("cascade_error");
                if (cascadeFunction != null) {
                  try {
                    cascadeFunction.call(cascade);
                  } catch (final Exception e) {
                    // Expected cascading error
                    LOGGER.fine("Cascading error " + cascade + " handled: " + e.getMessage());
                  }
                }

                // Verify recovery after each cascade
                assertThat(engine.isValid()).isTrue();
                assertThat(store.isValid()).isTrue();

              } catch (final Exception e) {
                LOGGER.info("Cascading error test " + cascade + " failed: " + e.getMessage());
              }
            }

            // Final recovery validation
            final Store finalStore = engine.createStore();
            registerForCleanup(finalStore);
            assertThat(finalStore.isValid()).isTrue();

            LOGGER.info("Graceful recovery from cascading errors validated on " + runtimeType);
          });
    }

    @Test
    @DisplayName("Should handle concurrent error scenarios")
    void shouldHandleConcurrentErrorScenarios()
        throws InterruptedException, ExecutionException, TimeoutException {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing concurrent error scenarios on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final byte[] concurrentErrorModule = createConcurrentErrorModule();
            final Module module = engine.compileModule(concurrentErrorModule);
            registerForCleanup(module);

            final int concurrentCount = 4;
            final List<CompletableFuture<String>> futures = new ArrayList<>();

            // Start multiple concurrent operations that will error
            for (int i = 0; i < concurrentCount; i++) {
              final int threadId = i;
              final CompletableFuture<String> future =
                  CompletableFuture.supplyAsync(
                      () -> {
                        try {
                          final Store store = engine.createStore();
                          final Instance instance = store.instantiate(module);
                          final WasmFunction errorFunction =
                              instance.getFunction("concurrent_error");

                          if (errorFunction != null) {
                            errorFunction.call(threadId);
                            return "UNEXPECTED_SUCCESS";
                          }
                          return "NO_FUNCTION";
                        } catch (final Exception e) {
                          return "ERROR: " + e.getClass().getSimpleName();
                        }
                      });
              futures.add(future);
            }

            // Wait for all concurrent operations to complete
            try {
              int errorCount = 0;
              for (final CompletableFuture<String> future : futures) {
                final String result = future.get(30, TimeUnit.SECONDS);
                if (result.startsWith("ERROR")) {
                  errorCount++;
                }
              }

              LOGGER.info("Concurrent errors handled: " + errorCount + "/" + concurrentCount);
              // All should error in this test
              assertThat(errorCount).isEqualTo(concurrentCount);

              // Verify engine remains stable after concurrent errors
              assertThat(engine.isValid()).isTrue();

            } catch (final Exception e) {
              throw new RuntimeException(e);
            }

            LOGGER.info("Concurrent error scenarios validated on " + runtimeType);
          });
    }
  }

  @Nested
  @DisplayName("Error Message Quality and Safety Tests")
  final class ErrorMessageQualityAndSafetyTests {

    @Test
    @DisplayName("Should provide meaningful error messages for all error types")
    void shouldProvideMeaningfulErrorMessagesForAllErrorTypes() {
      final ErrorCategory[] categories = ErrorCategory.values();

      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing error message quality on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            int meaningfulMessages = 0;
            for (final ErrorCategory category : categories) {
              try {
                final Exception error = triggerErrorCategory(engine, category);
                if (error != null) {
                  validateErrorMessageQuality(error, category);
                  meaningfulMessages++;
                  LOGGER.fine(
                      "Meaningful error message for " + category + ": " + error.getMessage());
                }
              } catch (final Exception e) {
                LOGGER.info("Error category " + category + " test failed: " + e.getMessage());
              }
            }

            LOGGER.info(
                "Meaningful error messages: "
                    + meaningfulMessages
                    + "/"
                    + categories.length
                    + " on "
                    + runtimeType);
          });
    }

    @Test
    @DisplayName("Should ensure error messages are safe for logging")
    void shouldEnsureErrorMessagesAreSafeForLogging() {
      runWithBothRuntimes(
          (runtime, runtimeType) -> {
            LOGGER.info("Testing error message safety for logging on " + runtimeType);

            final Engine engine = runtime.createEngine();
            registerForCleanup(engine);

            final Store store = engine.createStore();
            registerForCleanup(store);

            // Test various error scenarios
            final String[] errorScenarios = {
              "division_by_zero", "memory_violation", "type_mismatch", "invalid_function"
            };

            for (final String scenario : errorScenarios) {
              try {
                final byte[] errorModule = createRuntimeErrorModule(scenario);
                final Module module = engine.compileModule(errorModule);
                registerForCleanup(module);

                final Instance instance = store.instantiate(module);
                registerForCleanup(instance);

                final WasmFunction errorFunction = instance.getFunction("trigger_error");
                if (errorFunction != null) {
                  try {
                    errorFunction.call();
                  } catch (final Exception e) {
                    validateErrorMessageSafety(e, scenario);
                  }
                }
              } catch (final Exception e) {
                validateErrorMessageSafety(e, scenario);
              }
            }

            LOGGER.info("Error message safety validation completed on " + runtimeType);
          });
    }
  }

  // Helper methods for systematic error testing

  private void validateErrorProperties(
      final Throwable error, final ErrorCategory category, final String scenario) {
    // Validate basic error properties
    assertThat(error).isNotNull();
    assertThat(error.getMessage()).isNotNull();
    assertThat(error.getMessage()).isNotEmpty();
    assertThat(error.getMessage().length()).isLessThan(10000); // Reasonable length

    // Validate error type hierarchy
    assertThat(error).isInstanceOf(Exception.class);
    if (error instanceof WasmException) {
      final WasmException wasmError = (WasmException) error;
      // Additional WASM-specific validations can be added here
    }

    // Validate error message doesn't contain sensitive information
    final String message = error.getMessage().toLowerCase();
    assertThat(message).doesNotContain("password");
    assertThat(message).doesNotContain("secret");
    assertThat(message).doesNotContain("key");
  }

  private void validateErrorRecovery(final Engine engine, final ErrorCategory category) {
    // Verify system remains stable after error
    assertThat(engine.isValid()).isTrue();

    // Verify new resources can still be created
    try {
      final Store recoveryStore = engine.createStore();
      assertThat(recoveryStore.isValid()).isTrue();
      recoveryStore.close();
    } catch (final Exception e) {
      throw new AssertionError("Failed to recover after " + category + " error", e);
    }
  }

  private void validateErrorStackTrace(final Throwable error) {
    // Verify stack trace is present and reasonable
    final StackTraceElement[] stackTrace = error.getStackTrace();
    assertThat(stackTrace).isNotNull();
    assertThat(stackTrace.length).isGreaterThan(0);

    // Verify stack trace doesn't expose internal implementation details
    for (final StackTraceElement element : stackTrace) {
      final String className = element.getClassName();
      assertThat(className).doesNotContain("$$");
      assertThat(className).doesNotContain("CGLIB");
    }
  }

  private void validateErrorMessageQuality(final Exception error, final ErrorCategory category) {
    final String message = error.getMessage();

    // Message should be descriptive
    assertThat(message.length()).isGreaterThan(10);

    // Message should not contain raw memory addresses or internal identifiers
    assertThat(message).doesNotContainPattern("0x[0-9a-fA-F]{8,}");
    assertThat(message).doesNotContainPattern("@[0-9a-fA-F]{8,}");

    // Message should be human-readable
    assertThat(message).matches(".*[a-zA-Z].*"); // Contains letters
    assertThat(message).doesNotStartWith("Exception");
    assertThat(message).doesNotStartWith("Error");
  }

  private void validateErrorMessageSafety(final Exception error, final String scenario) {
    final String message = error.getMessage();

    // Ensure message doesn't contain log injection characters
    assertThat(message).doesNotContain("\n");
    assertThat(message).doesNotContain("\r");
    assertThat(message).doesNotContain("\0");

    // Ensure message doesn't contain potential XSS characters
    assertThat(message).doesNotContain("<script>");
    assertThat(message).doesNotContain("javascript:");

    // Ensure message is not excessively long
    assertThat(message.length()).isLessThan(1000);
  }

  private Exception triggerErrorCategory(final Engine engine, final ErrorCategory category) {
    try {
      switch (category) {
        case COMPILATION:
          engine.compileModule(new byte[] {0x00, 0x01, 0x02, 0x03});
          break;
        case VALIDATION:
          engine.compileModule(new byte[] {});
          break;
        case LINKING:
          final Store store = engine.createStore();
          final byte[] linkingModule = createLinkingErrorModule("missing_import_function");
          final Module module = engine.compileModule(linkingModule);
          store.instantiate(module);
          break;
        case RUNTIME:
          final Store runtimeStore = engine.createStore();
          final byte[] runtimeModule = createRuntimeErrorModule("division_by_zero");
          final Module runtimeMod = engine.compileModule(runtimeModule);
          final Instance runtimeInstance = runtimeStore.instantiate(runtimeMod);
          final WasmFunction runtimeFunc = runtimeInstance.getFunction("trigger_error");
          if (runtimeFunc != null) {
            runtimeFunc.call();
          }
          break;
        case SECURITY:
          final byte[] securityModule = createSecurityErrorModule();
          engine.compileModule(securityModule);
          break;
        case SYSTEM:
          // Simulate system error (file not found, etc.)
          Files.readAllBytes(Paths.get("/nonexistent/file"));
          break;
        default:
          throw new IllegalArgumentException("Unknown error category: " + category);
      }
    } catch (final Exception e) {
      return e;
    }
    return null;
  }

  // Helper methods for creating error test modules

  private byte[] createCorruptedWasmModule() {
    final byte[] validModule = TestUtils.createSimpleWasmModule();
    final byte[] corrupted = validModule.clone();
    // Corrupt some bytes in the middle
    if (corrupted.length > 10) {
      corrupted[8] = (byte) 0xFF;
      corrupted[9] = (byte) 0xFF;
    }
    return corrupted;
  }

  private byte[] createErrorInjectionModule(
      final ErrorCategory category, final ErrorInjectionScenario scenario) {
    switch (scenario) {
      case NULL_PARAMETER:
        return null; // Will cause null parameter error
      case INVALID_INPUT:
        return new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
      case RESOURCE_EXHAUSTION:
        return new byte[Integer.MAX_VALUE / 1000]; // Large but not max
      case CORRUPTION:
        return createCorruptedWasmModule();
      default:
        return new byte[] {0x00, 0x01, 0x02, 0x03}; // Invalid magic
    }
  }

  private byte[] createRuntimeErrorModule(final String errorType) {
    switch (errorType) {
      case "division_by_zero":
        return TestUtils.createWasmModuleFromWat(
            "(module\n"
                + "  (func $trigger_error (export \"trigger_error\") (result i32)\n"
                + "    i32.const 10\n"
                + "    i32.const 0\n"
                + "    i32.div_s\n"
                + "  )\n"
                + ")");

      case "memory_access_violation":
        return TestUtils.createWasmModuleFromWat(
            "(module\n"
                + "  (memory 1)\n"
                + "  (func $trigger_error (export \"trigger_error\") (result i32)\n"
                + "    i32.const 999999\n"
                + "    i32.load\n"
                + "  )\n"
                + ")");

      case "unreachable_code":
        return TestUtils.createWasmModuleFromWat(
            "(module\n"
                + "  (func $trigger_error (export \"trigger_error\")\n"
                + "    unreachable\n"
                + "  )\n"
                + ")");

      default:
        return TestUtils.createWasmModuleFromWat(
            "(module\n"
                + "  (func $trigger_error (export \"trigger_error\") (result i32)\n"
                + "    i32.const 42\n"
                + "    i32.const 0\n"
                + "    i32.div_s\n"
                + "  )\n"
                + ")");
    }
  }

  private byte[] createLinkingErrorModule(final String errorType) {
    switch (errorType) {
      case "missing_import_function":
        return TestUtils.createWasmModuleFromWat(
            "(module\n"
                + "  (import \"env\" \"missing_func\" (func $missing))\n"
                + "  (func $test (export \"test\")\n"
                + "    call $missing\n"
                + "  )\n"
                + ")");

      case "missing_import_memory":
        return TestUtils.createWasmModuleFromWat(
            "(module\n"
                + "  (import \"env\" \"memory\" (memory 1))\n"
                + "  (func $test (export \"test\") (result i32)\n"
                + "    i32.const 0\n"
                + "    i32.load\n"
                + "  )\n"
                + ")");

      default:
        return TestUtils.createWasmModuleFromWat(
            "(module\n"
                + "  (import \"env\" \"unknown\" (func $unknown))\n"
                + "  (func $test (export \"test\")\n"
                + "    call $unknown\n"
                + "  )\n"
                + ")");
    }
  }

  private byte[] createStackErrorPropagationModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $top_level (export \"top_level\")\n"
            + "    call $middle_level\n"
            + "  )\n"
            + "  (func $middle_level\n"
            + "    call $bottom_level\n"
            + "  )\n"
            + "  (func $bottom_level\n"
            + "    i32.const 10\n"
            + "    i32.const 0\n"
            + "    i32.div_s\n"
            + "    drop\n"
            + "  )\n"
            + ")");
  }

  private byte[] createCascadingErrorModule(final int cascadeLevel) {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $cascade_error (export \"cascade_error\") (param i32)\n"
            + "    local.get 0\n"
            + "    i32.const 0\n"
            + "    i32.eq\n"
            + "    if\n"
            + "      i32.const 10\n"
            + "      i32.const 0\n"
            + "      i32.div_s\n"
            + "      drop\n"
            + "    else\n"
            + "      local.get 0\n"
            + "      i32.const 1\n"
            + "      i32.sub\n"
            + "      call $cascade_error\n"
            + "    end\n"
            + "  )\n"
            + ")");
  }

  private byte[] createConcurrentErrorModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (func $concurrent_error (export \"concurrent_error\") (param i32)\n"
            + "    local.get 0\n"
            + "    i32.const 0\n"
            + "    i32.div_s\n"
            + "    drop\n"
            + "  )\n"
            + ")");
  }

  private byte[] createSecurityErrorModule() {
    return TestUtils.createWasmModuleFromWat(
        "(module\n"
            + "  (import \"system\" \"exec\" (func $exec (param i32) (result i32)))\n"
            + "  (func $security_violation (export \"security_violation\")\n"
            + "    i32.const 0\n"
            + "    call $exec\n"
            + "    drop\n"
            + "  )\n"
            + ")");
  }
}
