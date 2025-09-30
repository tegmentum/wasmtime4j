package ai.tegmentum.wasmtime4j.evolution;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitEvolutionChange;
import ai.tegmentum.wasmtime4j.WitEvolutionMetrics;
import ai.tegmentum.wasmtime4j.WitEvolutionOperation;
import ai.tegmentum.wasmtime4j.WitEvolutionResult;
import ai.tegmentum.wasmtime4j.WitEvolutionValidation;
import ai.tegmentum.wasmtime4j.WitInterfaceBindings;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.WitInterfaceEvolution;
import ai.tegmentum.wasmtime4j.WitTypeAdapter;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive tests for WIT interface evolution functionality.
 *
 * <p>These tests validate the complete interface evolution system including:
 *
 * <ul>
 *   <li>Backward compatibility analysis
 *   <li>Type adaptation and conversion
 *   <li>Interface migration scenarios
 *   <li>Version compatibility checking
 *   <li>Error handling and edge cases
 * </ul>
 *
 * @since 1.0.0
 */
@Execution(ExecutionMode.CONCURRENT)
class WitInterfaceEvolutionTest {

  private WasmRuntime runtime;
  private WitInterfaceEvolution evolution;
  private TestInterfaceBuilder interfaceBuilder;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.createRuntime();
    evolution = runtime.createWitInterfaceEvolution();
    interfaceBuilder = new TestInterfaceBuilder();
  }

  @AfterEach
  void tearDown() {
    if (runtime != null) {
      runtime.close();
    }
  }

  @Nested
  @DisplayName("Interface Evolution Analysis")
  class InterfaceEvolutionAnalysisTests {

    @Test
    @DisplayName("Should analyze evolution between compatible versions")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldAnalyzeEvolutionBetweenCompatibleVersions() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("calculator")
              .withVersion("1.0.0")
              .withFunction("add", List.of("i32", "i32"), "i32")
              .withFunction("subtract", List.of("i32", "i32"), "i32")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("calculator")
              .withVersion("1.1.0")
              .withFunction("add", List.of("i32", "i32"), "i32")
              .withFunction("subtract", List.of("i32", "i32"), "i32")
              .withFunction("multiply", List.of("i32", "i32"), "i32") // New function added
              .build();

      // Act
      final WitEvolutionResult result = evolution.evolveInterface(sourceInterface, targetInterface);

      // Assert
      assertNotNull(result, "Evolution result should not be null");
      assertTrue(result.isSuccessful(), "Evolution should be successful");
      assertFalse(result.hasBreakingChanges(), "Should not have breaking changes");

      final List<WitEvolutionChange> changes = result.getChanges();
      assertEquals(1, changes.size(), "Should have exactly one change");

      final WitEvolutionChange change = changes.get(0);
      assertEquals(WitEvolutionChange.ChangeType.FUNCTION_ADDED, change.getType());
      assertFalse(change.isBreaking(), "Adding function should not be breaking");
      assertTrue(
          change.getDescription().contains("multiply"), "Change should mention the new function");

      final WitEvolutionMetrics metrics = result.getMetrics();
      assertNotNull(metrics, "Metrics should not be null");
      assertTrue(metrics.isSuccessful(), "Metrics should indicate success");
      assertTrue(metrics.getCompatibilityScore() > 0.8, "Compatibility score should be high");
    }

    @Test
    @DisplayName("Should detect breaking changes in evolution")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldDetectBreakingChangesInEvolution() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("calculator")
              .withVersion("1.0.0")
              .withFunction("add", List.of("i32", "i32"), "i32")
              .withFunction("divide", List.of("i32", "i32"), "i32")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("calculator")
              .withVersion("2.0.0")
              .withFunction("add", List.of("f64", "f64"), "f64") // Changed signature
              // Function "divide" removed
              .build();

      // Act
      final WitEvolutionResult result = evolution.evolveInterface(sourceInterface, targetInterface);

      // Assert
      assertNotNull(result, "Evolution result should not be null");
      assertTrue(result.isSuccessful(), "Evolution analysis should complete successfully");
      assertTrue(result.hasBreakingChanges(), "Should have breaking changes");

      final List<WitEvolutionChange> breakingChanges = result.getBreakingChanges();
      assertTrue(breakingChanges.size() >= 2, "Should have at least 2 breaking changes");

      final boolean hasSignatureChange =
          breakingChanges.stream()
              .anyMatch(
                  change ->
                      change.getType() == WitEvolutionChange.ChangeType.FUNCTION_SIGNATURE_CHANGED);
      assertTrue(hasSignatureChange, "Should detect function signature change");

      final boolean hasFunctionRemoval =
          breakingChanges.stream()
              .anyMatch(
                  change -> change.getType() == WitEvolutionChange.ChangeType.FUNCTION_REMOVED);
      assertTrue(hasFunctionRemoval, "Should detect function removal");

      final WitEvolutionMetrics metrics = result.getMetrics();
      assertTrue(metrics.getCompatibilityScore() < 0.5, "Compatibility score should be low");
    }

    @Test
    @DisplayName("Should handle type evolution")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleTypeEvolution() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("data-processor")
              .withVersion("1.0.0")
              .withType("user-record", "record", Map.of("name", "string", "age", "u32"))
              .withFunction("process-user", List.of("user-record"), "string")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("data-processor")
              .withVersion("1.1.0")
              .withType(
                  "user-record",
                  "record",
                  Map.of(
                      "name", "string",
                      "age", "u32",
                      "email", "string" // New field added
                      ))
              .withFunction("process-user", List.of("user-record"), "string")
              .build();

      // Act
      final WitEvolutionResult result = evolution.evolveInterface(sourceInterface, targetInterface);

      // Assert
      assertNotNull(result, "Evolution result should not be null");
      assertTrue(result.isSuccessful(), "Evolution should be successful");

      final List<WitEvolutionChange> changes = result.getChanges();
      final boolean hasTypeModification =
          changes.stream()
              .anyMatch(change -> change.getType() == WitEvolutionChange.ChangeType.TYPE_MODIFIED);
      assertTrue(hasTypeModification, "Should detect type modification");

      // Check if type adapters were created
      final Map<String, WitTypeAdapter> adapters = result.getTypeAdapters();
      assertTrue(adapters.containsKey("user-record"), "Should create adapter for modified type");

      final WitTypeAdapter adapter = adapters.get("user-record");
      assertNotNull(adapter, "Type adapter should not be null");
      assertEquals("user-record", adapter.getSourceTypeName());
      assertEquals("user-record", adapter.getTargetTypeName());
    }
  }

  @Nested
  @DisplayName("Compatibility Checking")
  class CompatibilityCheckingTests {

    @Test
    @DisplayName("Should validate backward compatibility")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldValidateBackwardCompatibility() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("api")
              .withVersion("1.0.0")
              .withFunction("get-data", List.of(), "string")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("api")
              .withVersion("1.1.0")
              .withFunction("get-data", List.of(), "string")
              .withFunction("get-metadata", List.of(), "string") // New function
              .build();

      // Act
      final WitCompatibilityResult result =
          evolution.checkEvolutionCompatibility(sourceInterface, targetInterface);

      // Assert
      assertNotNull(result, "Compatibility result should not be null");
      assertTrue(result.isCompatible(), "Interfaces should be compatible");
      assertTrue(
          result.getSatisfiedImports().contains("get-data"), "Should satisfy existing imports");
      assertTrue(result.getUnsatisfiedImports().isEmpty(), "Should not have unsatisfied imports");
    }

    @Test
    @DisplayName("Should detect incompatible changes")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldDetectIncompatibleChanges() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("api")
              .withVersion("1.0.0")
              .withFunction("process", List.of("string", "i32"), "string")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("api")
              .withVersion("2.0.0")
              .withFunction("process", List.of("string"), "string") // Removed parameter
              .build();

      // Act
      final WitCompatibilityResult result =
          evolution.checkEvolutionCompatibility(sourceInterface, targetInterface);

      // Assert
      assertNotNull(result, "Compatibility result should not be null");
      assertFalse(result.isCompatible(), "Interfaces should be incompatible");
      assertTrue(result.hasUnsatisfiedImports(), "Should have unsatisfied imports");
    }
  }

  @Nested
  @DisplayName("Type Adaptation")
  class TypeAdaptationTests {

    @Test
    @DisplayName("Should create type adapters for compatible types")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldCreateTypeAdaptersForCompatibleTypes() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("converter")
              .withVersion("1.0.0")
              .withType("point", "record", Map.of("x", "i32", "y", "i32"))
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("converter")
              .withVersion("1.1.0")
              .withType("point", "record", Map.of("x", "f64", "y", "f64")) // Changed precision
              .build();

      // Act
      final Map<String, WitTypeAdapter> adapters =
          evolution.createTypeAdapters(sourceInterface, targetInterface);

      // Assert
      assertNotNull(adapters, "Adapters map should not be null");
      assertFalse(adapters.isEmpty(), "Should create at least one adapter");
      assertTrue(adapters.containsKey("point"), "Should create adapter for point type");

      final WitTypeAdapter pointAdapter = adapters.get("point");
      assertNotNull(pointAdapter, "Point adapter should not be null");
      assertTrue(pointAdapter.supportsForwardConversion(), "Should support forward conversion");
      assertTrue(pointAdapter.supportsReverseConversion(), "Should support reverse conversion");
      assertEquals(WitTypeAdapter.AdapterType.STRUCTURAL_ADAPTATION, pointAdapter.getAdapterType());

      // Test actual conversion
      final WasmValue sourcePoint =
          WasmValue.record(
              Map.of(
                  "x", WasmValue.i32(10),
                  "y", WasmValue.i32(20)));

      final WasmValue convertedPoint = pointAdapter.convertForward(sourcePoint);
      assertNotNull(convertedPoint, "Converted point should not be null");
      assertTrue(convertedPoint.isRecord(), "Converted value should be a record");

      // Validate statistics
      final WitTypeAdapter.AdapterStatistics stats = pointAdapter.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.getForwardConversions() > 0, "Should have recorded forward conversions");
    }

    @Test
    @DisplayName("Should validate conversion compatibility")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldValidateConversionCompatibility() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("validator")
              .withVersion("1.0.0")
              .withType("number", "u32")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("validator")
              .withVersion("1.1.0")
              .withType("number", "i64") // Compatible widening conversion
              .build();

      // Act
      final Map<String, WitTypeAdapter> adapters =
          evolution.createTypeAdapters(sourceInterface, targetInterface);
      final WitTypeAdapter numberAdapter = adapters.get("number");

      // Assert
      assertNotNull(numberAdapter, "Number adapter should be created");

      final WasmValue sourceNumber = WasmValue.u32(42);
      final WitTypeAdapter.AdapterValidationResult validation =
          numberAdapter.validateForwardConversion(sourceNumber);

      assertTrue(validation.isValid(), "Conversion should be valid");
      assertTrue(validation.getErrors().isEmpty(), "Should not have errors");
      assertFalse(numberAdapter.isLossless(), "U32 to I64 conversion should be lossless");
    }

    @Test
    @DisplayName("Should handle incompatible type conversions")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleIncompatibleTypeConversions() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("incompatible")
              .withVersion("1.0.0")
              .withType("data", "string")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("incompatible")
              .withVersion("2.0.0")
              .withType("data", "i32") // Incompatible type change
              .build();

      // Act & Assert
      assertThrows(
          WasmRuntimeException.class,
          () -> {
            evolution.createTypeAdapters(sourceInterface, targetInterface);
          },
          "Should throw exception for incompatible type conversion");
    }
  }

  @Nested
  @DisplayName("Interface Binding Generation")
  class InterfaceBindingGenerationTests {

    @Test
    @DisplayName("Should generate bindings for evolved interface")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldGenerateBindingsForEvolvedInterface() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("service")
              .withVersion("1.0.0")
              .withFunction("hello", List.of("string"), "string")
              .withType("config", "record", Map.of("timeout", "u32"))
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("service")
              .withVersion("1.1.0")
              .withFunction("hello", List.of("string"), "string")
              .withFunction("goodbye", List.of("string"), "string") // New function
              .withType(
                  "config",
                  "record",
                  Map.of(
                      "timeout", "u32",
                      "retry_count", "u32" // New field
                      ))
              .build();

      // Act
      final WitEvolutionResult evolutionResult =
          evolution.evolveInterface(sourceInterface, targetInterface);
      final WitInterfaceBindings bindings = evolution.createEvolutionBindings(evolutionResult);

      // Assert
      assertNotNull(bindings, "Bindings should not be null");
      assertEquals(sourceInterface, bindings.getSourceInterface());
      assertEquals(targetInterface, bindings.getTargetInterface());

      // Check function bindings
      final Map<String, WitInterfaceBindings.FunctionBinding> functionBindings =
          bindings.getFunctionBindings();
      assertTrue(functionBindings.containsKey("hello"), "Should bind existing function");
      assertTrue(bindings.isFunctionBound("hello"), "Hello function should be bound");

      final WitInterfaceBindings.FunctionBinding helloBinding =
          bindings.getFunctionBinding("hello").orElse(null);
      assertNotNull(helloBinding, "Hello binding should exist");
      assertTrue(helloBinding.isDirect(), "Hello binding should be direct");

      // Check type bindings
      final Map<String, WitInterfaceBindings.TypeBinding> typeBindings = bindings.getTypeBindings();
      assertTrue(typeBindings.containsKey("config"), "Should bind config type");
      assertTrue(bindings.isTypeBound("config"), "Config type should be bound");

      final WitInterfaceBindings.TypeBinding configBinding =
          bindings.getTypeBinding("config").orElse(null);
      assertNotNull(configBinding, "Config binding should exist");
      assertFalse(
          configBinding.isDirect(), "Config binding should not be direct (structure changed)");

      // Check unbound items
      final Set<String> unboundFunctions = bindings.getUnboundFunctions();
      assertTrue(
          unboundFunctions.contains("goodbye"),
          "Goodbye function should be unbound (new function)");

      // Validate bindings
      final WitInterfaceBindings.BindingValidationResult validation = bindings.validateBindings();
      assertTrue(validation.isValid(), "Bindings should be valid");
      assertTrue(validation.getCoverage() > 0.5, "Binding coverage should be reasonable");

      // Check statistics
      final WitInterfaceBindings.BindingStatistics stats = bindings.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.getTotalBindings() > 0, "Should have created bindings");
      assertTrue(stats.getSuccessRate() > 0.5, "Success rate should be reasonable");
    }

    @Test
    @DisplayName("Should invoke bound functions with adaptation")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldInvokeBoundFunctionsWithAdaptation() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("math")
              .withVersion("1.0.0")
              .withFunction("square", List.of("i32"), "i32")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("math")
              .withVersion("1.1.0")
              .withFunction("square", List.of("f64"), "f64") // Changed to floating point
              .build();

      final WitEvolutionResult evolutionResult =
          evolution.evolveInterface(sourceInterface, targetInterface);
      final WitInterfaceBindings bindings = evolution.createEvolutionBindings(evolutionResult);

      // Act
      final WasmValue result = bindings.invoke("square", WasmValue.i32(5));

      // Assert
      assertNotNull(result, "Result should not be null");
      // The result should be adapted back to the source interface type
      // This would depend on the specific implementation details
    }
  }

  @Nested
  @DisplayName("Validation and Error Handling")
  class ValidationAndErrorHandlingTests {

    @Test
    @DisplayName("Should validate evolution constraints")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldValidateEvolutionConstraints() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("constrained")
              .withVersion("1.0.0")
              .withFunction("critical-function", List.of("string"), "string")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("constrained")
              .withVersion("2.0.0")
              // Critical function removed - should violate constraints
              .withFunction("new-function", List.of("string"), "string")
              .build();

      // Act
      final WitEvolutionValidation validation =
          evolution.validateEvolutionConstraints(sourceInterface, targetInterface);

      // Assert
      assertNotNull(validation, "Validation result should not be null");
      assertFalse(validation.isValid(), "Evolution should be invalid");
      assertTrue(validation.hasViolations(), "Should have constraint violations");
      assertTrue(validation.hasIssues(), "Should have compatibility issues");

      final List<WitEvolutionValidation.ConstraintViolation> violations =
          validation.getViolations();
      assertFalse(violations.isEmpty(), "Should have violations");

      final boolean hasRemovalViolation =
          violations.stream()
              .anyMatch(
                  v ->
                      v.getType()
                          == WitEvolutionValidation.ViolationType.BACKWARD_COMPATIBILITY_VIOLATION);
      assertTrue(hasRemovalViolation, "Should have backward compatibility violation");

      assertTrue(
          validation.getRiskAssessment() == WitEvolutionValidation.EvolutionRisk.HIGH,
          "Risk assessment should be high");
    }

    @Test
    @DisplayName("Should handle invalid interface definitions")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleInvalidInterfaceDefinitions() {
      // Arrange
      final WitInterfaceDefinition invalidInterface =
          interfaceBuilder
              .withName("") // Invalid empty name
              .withVersion("1.0.0")
              .build();

      final WitInterfaceDefinition validInterface =
          interfaceBuilder
              .withName("valid")
              .withVersion("1.0.0")
              .withFunction("test", List.of(), "string")
              .build();

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            evolution.evolveInterface(invalidInterface, validInterface);
          },
          "Should throw exception for invalid interface");
    }

    @Test
    @DisplayName("Should handle unsupported evolution operations")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldHandleUnsupportedEvolutionOperations() {
      // Arrange
      final Set<WitEvolutionOperation> supportedOps = evolution.getSupportedOperations();

      // Act & Assert
      assertNotNull(supportedOps, "Supported operations should not be null");
      assertFalse(supportedOps.isEmpty(), "Should have some supported operations");

      // Verify common operations are supported
      assertTrue(
          supportedOps.contains(WitEvolutionOperation.ADD_FUNCTION),
          "Should support adding functions");
      assertTrue(
          supportedOps.contains(WitEvolutionOperation.ADD_TYPE), "Should support adding types");
      assertTrue(
          supportedOps.contains(WitEvolutionOperation.MODIFY_TYPE),
          "Should support modifying types");
    }
  }

  @Nested
  @DisplayName("Performance and Metrics")
  class PerformanceAndMetricsTests {

    @Test
    @DisplayName("Should collect evolution metrics")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldCollectEvolutionMetrics() {
      // Arrange
      final WitInterfaceDefinition sourceInterface =
          interfaceBuilder
              .withName("metrics-test")
              .withVersion("1.0.0")
              .withFunction("func1", List.of("i32"), "i32")
              .withFunction("func2", List.of("string"), "string")
              .withType("type1", "i32")
              .withType("type2", "string")
              .build();

      final WitInterfaceDefinition targetInterface =
          interfaceBuilder
              .withName("metrics-test")
              .withVersion("1.1.0")
              .withFunction("func1", List.of("i32"), "i32")
              .withFunction("func2", List.of("string"), "string")
              .withFunction("func3", List.of("f64"), "f64")
              .withType("type1", "i32")
              .withType("type2", "string")
              .withType("type3", "f64")
              .build();

      final Instant startTime = Instant.now();

      // Act
      final WitEvolutionResult result = evolution.evolveInterface(sourceInterface, targetInterface);

      // Assert
      final WitEvolutionMetrics metrics = result.getMetrics();
      assertNotNull(metrics, "Metrics should not be null");

      assertTrue(metrics.isSuccessful(), "Metrics should indicate success");
      assertEquals(3, metrics.getTypesAnalyzed(), "Should analyze 3 types");
      assertEquals(3, metrics.getFunctionsAnalyzed(), "Should analyze 3 functions");
      assertTrue(metrics.getAdaptersCreated() >= 0, "Adapters created should be non-negative");
      assertTrue(
          metrics.getCompatibilityScore() >= 0.0 && metrics.getCompatibilityScore() <= 1.0,
          "Compatibility score should be between 0 and 1");

      final Duration evolutionDuration = metrics.getEvolutionDuration();
      assertNotNull(evolutionDuration, "Evolution duration should not be null");
      assertTrue(
          evolutionDuration.compareTo(Duration.ZERO) >= 0, "Duration should be non-negative");

      assertTrue(metrics.getMemoryUsed() > 0, "Should use some memory");
      assertTrue(metrics.getEvolutionThroughput() >= 0.0, "Throughput should be non-negative");
    }

    @Test
    @DisplayName("Should handle large interface evolution efficiently")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void shouldHandleLargeInterfaceEvolutionEfficiently() {
      // Arrange
      final TestInterfaceBuilder sourceBuilder =
          interfaceBuilder.withName("large-interface").withVersion("1.0.0");

      final TestInterfaceBuilder targetBuilder =
          interfaceBuilder.withName("large-interface").withVersion("1.1.0");

      // Add many functions and types to test performance
      for (int i = 0; i < 100; i++) {
        sourceBuilder.withFunction("func" + i, List.of("i32"), "i32");
        sourceBuilder.withType("type" + i, "i32");

        targetBuilder.withFunction("func" + i, List.of("i32"), "i32");
        targetBuilder.withType("type" + i, "i32");
      }

      // Add some new functions and types to the target
      for (int i = 100; i < 120; i++) {
        targetBuilder.withFunction("func" + i, List.of("i32"), "i32");
        targetBuilder.withType("type" + i, "i32");
      }

      final WitInterfaceDefinition sourceInterface = sourceBuilder.build();
      final WitInterfaceDefinition targetInterface = targetBuilder.build();

      final Instant startTime = Instant.now();

      // Act
      final WitEvolutionResult result = evolution.evolveInterface(sourceInterface, targetInterface);

      // Assert
      final Duration actualDuration = Duration.between(startTime, Instant.now());
      assertTrue(
          actualDuration.compareTo(Duration.ofSeconds(5)) < 0,
          "Large interface evolution should complete within 5 seconds");

      assertTrue(result.isSuccessful(), "Large evolution should be successful");
      assertFalse(result.hasBreakingChanges(), "Should not have breaking changes");

      final WitEvolutionMetrics metrics = result.getMetrics();
      assertEquals(120, metrics.getTypesAnalyzed(), "Should analyze all types");
      assertEquals(120, metrics.getFunctionsAnalyzed(), "Should analyze all functions");
      assertTrue(metrics.getEvolutionThroughput() > 10.0, "Should have reasonable throughput");
    }
  }

  /** Helper class for building test interface definitions. */
  private static class TestInterfaceBuilder {
    private String name = "test-interface";
    private String version = "1.0.0";
    private String packageName = "test-package";
    private final java.util.Map<String, FunctionSpec> functions = new java.util.HashMap<>();
    private final java.util.Map<String, TypeSpec> types = new java.util.HashMap<>();

    TestInterfaceBuilder withName(final String name) {
      this.name = name;
      return this;
    }

    TestInterfaceBuilder withVersion(final String version) {
      this.version = version;
      return this;
    }

    TestInterfaceBuilder withPackage(final String packageName) {
      this.packageName = packageName;
      return this;
    }

    TestInterfaceBuilder withFunction(
        final String name, final List<String> params, final String returnType) {
      functions.put(name, new FunctionSpec(name, params, returnType));
      return this;
    }

    TestInterfaceBuilder withType(final String name, final String baseType) {
      types.put(name, new TypeSpec(name, baseType, Map.of()));
      return this;
    }

    TestInterfaceBuilder withType(
        final String name, final String baseType, final Map<String, String> fields) {
      types.put(name, new TypeSpec(name, baseType, fields));
      return this;
    }

    WitInterfaceDefinition build() {
      return new TestWitInterfaceDefinition(name, version, packageName, functions, types);
    }

    private record FunctionSpec(String name, List<String> parameters, String returnType) {}

    private record TypeSpec(String name, String baseType, Map<String, String> fields) {}
  }

  /** Test implementation of WitInterfaceDefinition. */
  private static class TestWitInterfaceDefinition implements WitInterfaceDefinition {
    private final String name;
    private final String version;
    private final String packageName;
    private final Map<String, TestInterfaceBuilder.FunctionSpec> functions;
    private final Map<String, TestInterfaceBuilder.TypeSpec> types;

    TestWitInterfaceDefinition(
        final String name,
        final String version,
        final String packageName,
        final Map<String, TestInterfaceBuilder.FunctionSpec> functions,
        final Map<String, TestInterfaceBuilder.TypeSpec> types) {
      this.name = name;
      this.version = version;
      this.packageName = packageName;
      this.functions = Map.copyOf(functions);
      this.types = Map.copyOf(types);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getVersion() {
      return version;
    }

    @Override
    public String getPackageName() {
      return packageName;
    }

    @Override
    public List<String> getFunctionNames() {
      return List.copyOf(functions.keySet());
    }

    @Override
    public List<String> getTypeNames() {
      return List.copyOf(types.keySet());
    }

    @Override
    public Set<String> getDependencies() {
      return Set.of();
    }

    @Override
    public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
      // Simplified compatibility check
      return WitCompatibilityResult.compatible("Compatible", Set.of());
    }

    @Override
    public String getWitText() {
      return "// Generated WIT text for " + name;
    }

    @Override
    public List<String> getImportNames() {
      return List.of();
    }

    @Override
    public List<String> getExportNames() {
      return getFunctionNames();
    }
  }
}
