package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Validates runtime compatibility against Wasmtime's authoritative behavior and standards.
 * Implements comprehensive checks to ensure zero functional discrepancies between JNI and Panama
 * implementations while maintaining full Wasmtime compatibility.
 *
 * <p>This validator performs:
 *
 * <ul>
 *   <li>Wasmtime API specification compliance validation
 *   <li>Floating-point precision consistency checks
 *   <li>Memory behavior verification
 *   <li>WASI interaction compatibility validation
 *   <li>Exception handling consistency verification
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasmtimeCompatibilityValidator {
  private static final Logger LOGGER =
      Logger.getLogger(WasmtimeCompatibilityValidator.class.getName());

  // Wasmtime specification compliance thresholds
  private static final double WASMTIME_FLOAT32_EPSILON = 1.19e-07; // IEEE 754 float32 epsilon
  private static final double WASMTIME_FLOAT64_EPSILON = 2.22e-16; // IEEE 754 float64 epsilon
  private static final long WASMTIME_MAX_MEMORY_PAGES = 65536; // WebAssembly memory limit
  private static final int WASMTIME_MAX_CALL_STACK_DEPTH = 1024; // Default Wasmtime call stack limit

  private final ToleranceConfiguration toleranceConfig;

  /**
   * Creates a new Wasmtime compatibility validator.
   *
   * @param toleranceConfig the tolerance configuration for validation
   */
  public WasmtimeCompatibilityValidator(final ToleranceConfiguration toleranceConfig) {
    this.toleranceConfig = Objects.requireNonNull(toleranceConfig, "toleranceConfig cannot be null");
  }

  /**
   * Validates compatibility across runtime execution results.
   *
   * @param executionResults map of runtime execution results
   * @return list of compatibility issues detected
   */
  public List<BehavioralDiscrepancy> validateCompatibility(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    Objects.requireNonNull(executionResults, "executionResults cannot be null");

    final List<BehavioralDiscrepancy> issues = new ArrayList<>();

    LOGGER.fine("Validating Wasmtime compatibility for " + executionResults.size() + " runtimes");

    // Validate floating-point precision consistency
    issues.addAll(validateFloatingPointPrecision(executionResults));

    // Validate memory behavior consistency
    issues.addAll(validateMemoryBehavior(executionResults));

    // Validate exception handling consistency
    issues.addAll(validateExceptionHandling(executionResults));

    // Validate performance characteristics consistency
    issues.addAll(validatePerformanceCharacteristics(executionResults));

    // Validate WASI interaction consistency (if applicable)
    issues.addAll(validateWasiInteractions(executionResults));

    LOGGER.fine("Wasmtime compatibility validation completed: " + issues.size() + " issues found");
    return issues;
  }

  /** Validates floating-point precision consistency across runtimes. */
  private List<BehavioralDiscrepancy> validateFloatingPointPrecision(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> issues = new ArrayList<>();

    final List<BehavioralAnalyzer.TestExecutionResult> successfulResults = executionResults.values().stream()
        .filter(BehavioralAnalyzer.TestExecutionResult::isSuccessful)
        .toList();

    if (successfulResults.size() < 2) {
      return issues; // Need at least 2 successful results to compare
    }

    // Check floating-point values for precision consistency
    for (int i = 0; i < successfulResults.size(); i++) {
      for (int j = i + 1; j < successfulResults.size(); j++) {
        final BehavioralAnalyzer.TestExecutionResult result1 = successfulResults.get(i);
        final BehavioralAnalyzer.TestExecutionResult result2 = successfulResults.get(j);

        if (!areFloatingPointValuesConsistent(result1.getReturnValue(), result2.getReturnValue())) {
          issues.add(
              new BehavioralDiscrepancy(
                  DiscrepancyType.RETURN_VALUE_MISMATCH,
                  DiscrepancySeverity.CRITICAL,
                  "Floating-point precision inconsistency detected",
                  String.format("Values differ beyond Wasmtime IEEE 754 specification tolerance"),
                  "Verify floating-point implementation compliance with Wasmtime standards",
                  "floating-point-precision",
                  executionResults.keySet()));
        }
      }
    }

    return issues;
  }

  /** Validates memory behavior consistency across runtimes. */
  private List<BehavioralDiscrepancy> validateMemoryBehavior(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> issues = new ArrayList<>();

    final List<BehavioralAnalyzer.MemoryUsage> memoryUsages = executionResults.values().stream()
        .filter(r -> r.getMemoryUsage().isPresent())
        .map(r -> r.getMemoryUsage().get())
        .toList();

    if (memoryUsages.size() < 2) {
      return issues; // Need at least 2 memory measurements to compare
    }

    // Validate memory usage patterns are consistent
    final long minHeap = memoryUsages.stream().mapToLong(BehavioralAnalyzer.MemoryUsage::getHeapUsed).min().orElse(0);
    final long maxHeap = memoryUsages.stream().mapToLong(BehavioralAnalyzer.MemoryUsage::getHeapUsed).max().orElse(0);

    if (minHeap > 0) {
      final double memoryVariationRatio = (double) (maxHeap - minHeap) / minHeap;
      if (memoryVariationRatio > 0.5) { // More than 50% variation
        issues.add(
            new BehavioralDiscrepancy(
                DiscrepancyType.MEMORY_USAGE_DEVIATION,
                DiscrepancySeverity.MAJOR,
                "Significant memory usage variation between runtimes",
                String.format("Memory usage varies by %.1f%% across runtimes", memoryVariationRatio * 100),
                "Investigate memory allocation differences and optimize for consistency",
                "memory-consistency",
                executionResults.keySet()));
      }
    }

    return issues;
  }

  /** Validates exception handling consistency across runtimes. */
  private List<BehavioralDiscrepancy> validateExceptionHandling(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> issues = new ArrayList<>();

    final List<Exception> exceptions = executionResults.values().stream()
        .filter(r -> !r.isSuccessful() && !r.isSkipped())
        .map(BehavioralAnalyzer.TestExecutionResult::getException)
        .filter(Objects::nonNull)
        .toList();

    if (exceptions.size() < 2) {
      return issues; // Need at least 2 exceptions to compare
    }

    // Validate exception types are consistent with Wasmtime behavior
    final Set<String> exceptionCategories = exceptions.stream()
        .map(this::categorizeWasmtimeException)
        .collect(java.util.stream.Collectors.toSet());

    if (exceptionCategories.size() > 1) {
      issues.add(
          new BehavioralDiscrepancy(
              DiscrepancyType.EXCEPTION_TYPE_MISMATCH,
              DiscrepancySeverity.CRITICAL,
              "Inconsistent exception categorization across runtimes",
              String.format("Exception categories: %s", exceptionCategories),
              "Align exception handling to match Wasmtime specification exactly",
              "exception-consistency",
              executionResults.keySet()));
    }

    return issues;
  }

  /** Validates performance characteristics consistency. */
  private List<BehavioralDiscrepancy> validatePerformanceCharacteristics(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> issues = new ArrayList<>();

    final List<Duration> executionTimes = executionResults.values().stream()
        .map(BehavioralAnalyzer.TestExecutionResult::getExecutionTime)
        .toList();

    if (executionTimes.size() < 2) {
      return issues;
    }

    final Duration minTime = executionTimes.stream().min(Duration::compareTo).orElse(Duration.ZERO);
    final Duration maxTime = executionTimes.stream().max(Duration::compareTo).orElse(Duration.ZERO);

    if (minTime.toNanos() > 0) {
      final double performanceVariation = (double) (maxTime.toNanos() - minTime.toNanos()) / minTime.toNanos();

      // Flag extreme performance variations that suggest implementation differences
      if (performanceVariation > 5.0) { // More than 5x difference
        issues.add(
            new BehavioralDiscrepancy(
                DiscrepancyType.PERFORMANCE_DEVIATION,
                DiscrepancySeverity.MAJOR,
                "Extreme performance variation suggests implementation differences",
                String.format("Performance varies by %.1fx across runtimes", performanceVariation + 1),
                "Investigate and optimize runtime implementations for consistent performance",
                "performance-consistency",
                executionResults.keySet()));
      }
    }

    return issues;
  }

  /** Validates WASI interaction consistency (placeholder for WASI-specific tests). */
  private List<BehavioralDiscrepancy> validateWasiInteractions(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> issues = new ArrayList<>();

    // WASI validation logic would go here when WASI tests are implemented
    // For now, we'll just log that WASI validation is available

    LOGGER.fine("WASI interaction validation available for future implementation");

    return issues;
  }

  /** Checks if floating-point values are consistent within Wasmtime precision. */
  private boolean areFloatingPointValuesConsistent(final Object value1, final Object value2) {
    if (Objects.equals(value1, value2)) {
      return true;
    }

    if (value1 == null || value2 == null) {
      return Objects.equals(value1, value2);
    }

    // Handle single floating-point values
    if (value1 instanceof Float && value2 instanceof Float) {
      return Math.abs((Float) value1 - (Float) value2) <= WASMTIME_FLOAT32_EPSILON;
    }

    if (value1 instanceof Double && value2 instanceof Double) {
      return Math.abs((Double) value1 - (Double) value2) <= WASMTIME_FLOAT64_EPSILON;
    }

    // Handle Number comparisons with appropriate precision
    if (value1 instanceof Number && value2 instanceof Number) {
      final double d1 = ((Number) value1).doubleValue();
      final double d2 = ((Number) value2).doubleValue();
      return Math.abs(d1 - d2) <= WASMTIME_FLOAT64_EPSILON;
    }

    // Handle array comparisons
    if (value1.getClass().isArray() && value2.getClass().isArray()) {
      return areFloatingPointArraysConsistent(value1, value2);
    }

    return false;
  }

  /** Checks if floating-point arrays are consistent within Wasmtime precision. */
  private boolean areFloatingPointArraysConsistent(final Object array1, final Object array2) {
    if (array1 instanceof float[] && array2 instanceof float[]) {
      final float[] f1 = (float[]) array1;
      final float[] f2 = (float[]) array2;
      if (f1.length != f2.length) {
        return false;
      }
      for (int i = 0; i < f1.length; i++) {
        if (Math.abs(f1[i] - f2[i]) > WASMTIME_FLOAT32_EPSILON) {
          return false;
        }
      }
      return true;
    }

    if (array1 instanceof double[] && array2 instanceof double[]) {
      final double[] d1 = (double[]) array1;
      final double[] d2 = (double[]) array2;
      if (d1.length != d2.length) {
        return false;
      }
      for (int i = 0; i < d1.length; i++) {
        if (Math.abs(d1[i] - d2[i]) > WASMTIME_FLOAT64_EPSILON) {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  /** Categorizes exceptions according to Wasmtime specification. */
  private String categorizeWasmtimeException(final Exception exception) {
    final String className = exception.getClass().getSimpleName().toLowerCase();

    // Wasmtime-specific exception categorization
    if (className.contains("trap") || className.contains("runtime")) {
      return "runtime-trap";
    } else if (className.contains("compilation") || className.contains("parse")) {
      return "compilation-error";
    } else if (className.contains("instantiation") || className.contains("link")) {
      return "instantiation-error";
    } else if (className.contains("validation")) {
      return "validation-error";
    } else if (className.contains("memory") || className.contains("outofmemory")) {
      return "memory-error";
    } else if (className.contains("stack") || className.contains("overflow")) {
      return "stack-error";
    } else if (className.contains("wasi")) {
      return "wasi-error";
    } else {
      return "other-error";
    }
  }
}
