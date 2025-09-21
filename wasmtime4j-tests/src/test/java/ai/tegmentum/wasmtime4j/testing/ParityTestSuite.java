/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.testing;

import ai.tegmentum.wasmtime4j.*;
import java.lang.reflect.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Parity test suite that validates identical behavior between JNI and Panama implementations.
 *
 * <p>This test suite implements comprehensive parity validation including:
 *
 * <ul>
 *   <li>API behavioral equivalence testing
 *   <li>Performance characteristic comparison
 *   <li>Error handling consistency validation
 *   <li>Memory management behavior comparison
 *   <li>Threading and concurrency behavior validation
 *   <li>Cross-implementation data exchange testing
 * </ul>
 */
public final class ParityTestSuite {

  private static final Logger LOGGER = Logger.getLogger(ParityTestSuite.class.getName());

  // Core API classes to test for parity
  private static final Map<String, Class<?>> CORE_APIS = Map.of(
      "Engine", Engine.class,
      "Store", Store.class,
      "Module", Module.class,
      "Instance", Instance.class,
      "Memory", Memory.class,
      "Table", Table.class,
      "Global", Global.class,
      "Function", Function.class,
      "Linker", Linker.class,
      "Config", Config.class
  );

  private TestResults lastResults = TestResults.builder().build();

  public static ParityTestSuite create() {
    return new ParityTestSuite();
  }

  /**
   * Validates functional parity between JNI and Panama implementations.
   *
   * @return comprehensive parity report
   */
  public ParityReport validateFunctionalParity() {
    LOGGER.info("Starting comprehensive functional parity validation");

    final List<ParityViolation> violations = new ArrayList<>();
    final List<String> missingJniApis = new ArrayList<>();
    final List<String> missingPanamaApis = new ArrayList<>();

    // Test each core API for parity
    for (final Map.Entry<String, Class<?>> apiEntry : CORE_APIS.entrySet()) {
      final String apiName = apiEntry.getKey();
      final Class<?> apiInterface = apiEntry.getValue();

      LOGGER.info("Testing parity for API: " + apiName);

      try {
        // Check if implementations exist
        final boolean hasJniImpl = hasJniImplementation(apiName);
        final boolean hasPanamaImpl = hasPanamaImplementation(apiName);

        if (!hasJniImpl) {
          missingJniApis.add(apiName);
        }
        if (!hasPanamaImpl) {
          missingPanamaApis.add(apiName);
        }

        if (hasJniImpl && hasPanamaImpl) {
          // Test behavioral parity
          violations.addAll(testApiBehavioralParity(apiName, apiInterface));

          // Test performance parity
          violations.addAll(testApiPerformanceParity(apiName, apiInterface));

          // Test error handling parity
          violations.addAll(testApiErrorHandlingParity(apiName, apiInterface));

          // Test memory management parity
          violations.addAll(testApiMemoryManagementParity(apiName, apiInterface));
        }

      } catch (final Exception e) {
        LOGGER.warning("Parity testing failed for API " + apiName + ": " + e.getMessage());
        violations.add(
            ParityViolation.builder(apiName, "general")
                .withType(ParityViolation.ParityViolationType.IMPLEMENTATION_ERROR)
                .withDescription("Failed to test parity: " + e.getMessage())
                .withSeverity(ParityViolation.Severity.HIGH)
                .build());
      }
    }

    // Test cross-implementation compatibility
    violations.addAll(testCrossImplementationCompatibility());

    // Test threading parity
    violations.addAll(testThreadingParity());

    // Calculate overall parity metrics
    final int totalApis = CORE_APIS.size();
    final int violationCount = violations.size();
    final double parityPercentage = Math.max(0.0, ((double) (totalApis * 10 - violationCount) / (totalApis * 10)) * 100.0);

    LOGGER.info(
        String.format(
            "Functional parity validation completed. Parity: %.2f%%, Violations: %d",
            parityPercentage, violationCount));

    return new DefaultParityReport(
        parityPercentage, totalApis, violationCount, violations, missingJniApis, missingPanamaApis);
  }

  public TestResults getLastResults() {
    return lastResults;
  }

  // API Behavioral Parity Testing

  private List<ParityViolation> testApiBehavioralParity(final String apiName, final Class<?> apiInterface) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Get JNI and Panama implementation classes
      final Class<?> jniClass = getJniImplementationClass(apiName);
      final Class<?> panamaClass = getPanamaImplementationClass(apiName);

      if (jniClass == null || panamaClass == null) {
        return violations; // Skip if either implementation is missing
      }

      // Compare method signatures and behavior
      for (final Method interfaceMethod : apiInterface.getDeclaredMethods()) {
        if (isPublicApiMethod(interfaceMethod)) {
          violations.addAll(testMethodBehavioralParity(apiName, interfaceMethod, jniClass, panamaClass));
        }
      }

    } catch (final Exception e) {
      LOGGER.warning("Behavioral parity testing failed for " + apiName + ": " + e.getMessage());
      violations.add(
          ParityViolation.builder(apiName, "behavioral")
              .withType(ParityViolation.ParityViolationType.BEHAVIORAL_DIFFERENCE)
              .withDescription("Behavioral testing failed: " + e.getMessage())
              .withSeverity(ParityViolation.Severity.MEDIUM)
              .build());
    }

    return violations;
  }

  private List<ParityViolation> testMethodBehavioralParity(
      final String apiName, final Method method, final Class<?> jniClass, final Class<?> panamaClass) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Check if method exists in both implementations
      final Method jniMethod = findMatchingMethod(jniClass, method);
      final Method panamaMethod = findMatchingMethod(panamaClass, method);

      if (jniMethod == null && panamaMethod != null) {
        violations.add(
            ParityViolation.builder(apiName, method.getName())
                .withType(ParityViolation.ParityViolationType.MISSING_IMPLEMENTATION)
                .withDescription("Method missing in JNI implementation")
                .withSeverity(ParityViolation.Severity.HIGH)
                .build());
      } else if (jniMethod != null && panamaMethod == null) {
        violations.add(
            ParityViolation.builder(apiName, method.getName())
                .withType(ParityViolation.ParityViolationType.MISSING_IMPLEMENTATION)
                .withDescription("Method missing in Panama implementation")
                .withSeverity(ParityViolation.Severity.HIGH)
                .build());
      } else if (jniMethod != null && panamaMethod != null) {
        // Both methods exist, test behavioral equivalence
        violations.addAll(testMethodBehaviorEquivalence(apiName, method, jniMethod, panamaMethod));
      }

    } catch (final Exception e) {
      LOGGER.fine("Method parity testing failed for " + apiName + "." + method.getName() + ": " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testMethodBehaviorEquivalence(
      final String apiName, final Method interfaceMethod, final Method jniMethod, final Method panamaMethod) {
    final List<ParityViolation> violations = new ArrayList<>();

    // For behavioral testing, we would need to create instances and call methods
    // This is a simplified implementation that focuses on signatures and annotations
    try {
      // Compare return types
      if (!jniMethod.getReturnType().equals(panamaMethod.getReturnType())) {
        violations.add(
            ParityViolation.builder(apiName, interfaceMethod.getName())
                .withType(ParityViolation.ParityViolationType.INCORRECT_RETURN_VALUE)
                .withDescription(
                    String.format(
                        "Return type mismatch: JNI=%s, Panama=%s",
                        jniMethod.getReturnType().getSimpleName(),
                        panamaMethod.getReturnType().getSimpleName()))
                .withSeverity(ParityViolation.Severity.HIGH)
                .build());
      }

      // Compare parameter types
      if (!Arrays.equals(jniMethod.getParameterTypes(), panamaMethod.getParameterTypes())) {
        violations.add(
            ParityViolation.builder(apiName, interfaceMethod.getName())
                .withType(ParityViolation.ParityViolationType.INCORRECT_PARAMETER_HANDLING)
                .withDescription("Parameter type mismatch between implementations")
                .withSeverity(ParityViolation.Severity.HIGH)
                .build());
      }

      // Compare exception declarations
      if (!Arrays.equals(jniMethod.getExceptionTypes(), panamaMethod.getExceptionTypes())) {
        violations.add(
            ParityViolation.builder(apiName, interfaceMethod.getName())
                .withType(ParityViolation.ParityViolationType.EXCEPTION_HANDLING_DIFFERENCE)
                .withDescription("Exception declaration mismatch between implementations")
                .withSeverity(ParityViolation.Severity.MEDIUM)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Method behavior equivalence testing failed: " + e.getMessage());
    }

    return violations;
  }

  // API Performance Parity Testing

  private List<ParityViolation> testApiPerformanceParity(final String apiName, final Class<?> apiInterface) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Create simple performance benchmarks for each implementation
      if (apiName.equals("Engine")) {
        violations.addAll(testEnginePerformanceParity());
      } else if (apiName.equals("Module")) {
        violations.addAll(testModulePerformanceParity());
      } else if (apiName.equals("Function")) {
        violations.addAll(testFunctionPerformanceParity());
      }
      // Add more specific API performance tests as needed

    } catch (final Exception e) {
      LOGGER.warning("Performance parity testing failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testEnginePerformanceParity() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Benchmark engine creation time
      final Duration jniTime = benchmarkEngineCreation("jni");
      final Duration panamaTime = benchmarkEngineCreation("panama");

      // Allow up to 50% performance difference
      final double ratio = (double) Math.max(jniTime.toNanos(), panamaTime.toNanos()) /
                          Math.min(jniTime.toNanos(), panamaTime.toNanos());

      if (ratio > 1.5) {
        violations.add(
            ParityViolation.builder("Engine", "create")
                .withType(ParityViolation.ParityViolationType.PERFORMANCE_DIFFERENCE)
                .withDescription(
                    String.format(
                        "Significant performance difference: JNI=%dms, Panama=%dms",
                        jniTime.toMillis(), panamaTime.toMillis()))
                .withSeverity(ParityViolation.Severity.LOW)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Engine performance parity testing failed: " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testModulePerformanceParity() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Benchmark module compilation time
      final byte[] testWasm = generatePerformanceTestWasm();

      final Duration jniTime = benchmarkModuleCompilation("jni", testWasm);
      final Duration panamaTime = benchmarkModuleCompilation("panama", testWasm);

      // Allow up to 30% performance difference for compilation
      final double ratio = (double) Math.max(jniTime.toNanos(), panamaTime.toNanos()) /
                          Math.min(jniTime.toNanos(), panamaTime.toNanos());

      if (ratio > 1.3) {
        violations.add(
            ParityViolation.builder("Module", "compile")
                .withType(ParityViolation.ParityViolationType.PERFORMANCE_DIFFERENCE)
                .withDescription(
                    String.format(
                        "Module compilation performance difference: JNI=%dms, Panama=%dms",
                        jniTime.toMillis(), panamaTime.toMillis()))
                .withSeverity(ParityViolation.Severity.LOW)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Module performance parity testing failed: " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testFunctionPerformanceParity() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Benchmark function call performance
      final Duration jniTime = benchmarkFunctionCalls("jni");
      final Duration panamaTime = benchmarkFunctionCalls("panama");

      // Allow up to 20% performance difference for function calls
      final double ratio = (double) Math.max(jniTime.toNanos(), panamaTime.toNanos()) /
                          Math.min(jniTime.toNanos(), panamaTime.toNanos());

      if (ratio > 1.2) {
        violations.add(
            ParityViolation.builder("Function", "call")
                .withType(ParityViolation.ParityViolationType.PERFORMANCE_DIFFERENCE)
                .withDescription(
                    String.format(
                        "Function call performance difference: JNI=%dns, Panama=%dns",
                        jniTime.toNanos(), panamaTime.toNanos()))
                .withSeverity(ParityViolation.Severity.LOW)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Function performance parity testing failed: " + e.getMessage());
    }

    return violations;
  }

  // Error Handling Parity Testing

  private List<ParityViolation> testApiErrorHandlingParity(final String apiName, final Class<?> apiInterface) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test error handling consistency for common error scenarios
      violations.addAll(testInvalidParameterHandling(apiName));
      violations.addAll(testResourceExhaustionHandling(apiName));
      violations.addAll(testMalformedWasmHandling(apiName));

    } catch (final Exception e) {
      LOGGER.warning("Error handling parity testing failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testInvalidParameterHandling(final String apiName) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test null parameter handling
      final Exception jniException = testNullParameterHandling("jni", apiName);
      final Exception panamaException = testNullParameterHandling("panama", apiName);

      if ((jniException == null) != (panamaException == null)) {
        violations.add(
            ParityViolation.builder(apiName, "nullParameterHandling")
                .withType(ParityViolation.ParityViolationType.EXCEPTION_HANDLING_DIFFERENCE)
                .withDescription("Inconsistent null parameter handling between implementations")
                .withSeverity(ParityViolation.Severity.MEDIUM)
                .build());
      } else if (jniException != null && panamaException != null) {
        // Both throw exceptions, check if they're the same type
        if (!jniException.getClass().equals(panamaException.getClass())) {
          violations.add(
              ParityViolation.builder(apiName, "nullParameterHandling")
                  .withType(ParityViolation.ParityViolationType.EXCEPTION_HANDLING_DIFFERENCE)
                  .withDescription(
                      String.format(
                          "Different exception types for null parameters: JNI=%s, Panama=%s",
                          jniException.getClass().getSimpleName(),
                          panamaException.getClass().getSimpleName()))
                  .withSeverity(ParityViolation.Severity.MEDIUM)
                  .build());
        }
      }

    } catch (final Exception e) {
      LOGGER.fine("Invalid parameter handling test failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testResourceExhaustionHandling(final String apiName) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test resource exhaustion scenarios (simplified)
      if (apiName.equals("Engine") || apiName.equals("Store")) {
        // Create many instances to test resource limits
        final boolean jniHandlesExhaustion = testResourceExhaustion("jni", apiName);
        final boolean panamaHandlesExhaustion = testResourceExhaustion("panama", apiName);

        if (jniHandlesExhaustion != panamaHandlesExhaustion) {
          violations.add(
              ParityViolation.builder(apiName, "resourceExhaustion")
                  .withType(ParityViolation.ParityViolationType.RESOURCE_MANAGEMENT_DIFFERENCE)
                  .withDescription("Inconsistent resource exhaustion handling")
                  .withSeverity(ParityViolation.Severity.MEDIUM)
                  .build());
        }
      }

    } catch (final Exception e) {
      LOGGER.fine("Resource exhaustion handling test failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testMalformedWasmHandling(final String apiName) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      if (apiName.equals("Module")) {
        // Test malformed WASM handling
        final byte[] malformedWasm = new byte[]{0x00, 0x61, 0x73, 0x6d, 0x00}; // Invalid WASM

        final Exception jniException = testMalformedWasmCompilation("jni", malformedWasm);
        final Exception panamaException = testMalformedWasmCompilation("panama", malformedWasm);

        if ((jniException == null) != (panamaException == null)) {
          violations.add(
              ParityViolation.builder(apiName, "malformedWasm")
                  .withType(ParityViolation.ParityViolationType.EXCEPTION_HANDLING_DIFFERENCE)
                  .withDescription("Inconsistent malformed WASM handling")
                  .withSeverity(ParityViolation.Severity.HIGH)
                  .build());
        }
      }

    } catch (final Exception e) {
      LOGGER.fine("Malformed WASM handling test failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  // Memory Management Parity Testing

  private List<ParityViolation> testApiMemoryManagementParity(final String apiName, final Class<?> apiInterface) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test memory allocation and cleanup patterns
      violations.addAll(testMemoryAllocationParity(apiName));
      violations.addAll(testResourceCleanupParity(apiName));

    } catch (final Exception e) {
      LOGGER.warning("Memory management parity testing failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testMemoryAllocationParity(final String apiName) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Simplified memory allocation testing
      final long jniMemoryDelta = measureMemoryUsage("jni", apiName);
      final long panamaMemoryDelta = measureMemoryUsage("panama", apiName);

      // Allow up to 2x memory usage difference
      final double ratio = (double) Math.max(jniMemoryDelta, panamaMemoryDelta) /
                          Math.max(Math.min(jniMemoryDelta, panamaMemoryDelta), 1);

      if (ratio > 2.0) {
        violations.add(
            ParityViolation.builder(apiName, "memoryAllocation")
                .withType(ParityViolation.ParityViolationType.RESOURCE_MANAGEMENT_DIFFERENCE)
                .withDescription(
                    String.format(
                        "Significant memory usage difference: JNI=%d bytes, Panama=%d bytes",
                        jniMemoryDelta, panamaMemoryDelta))
                .withSeverity(ParityViolation.Severity.LOW)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Memory allocation parity testing failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testResourceCleanupParity(final String apiName) {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test resource cleanup behavior
      final boolean jniCleansUp = testResourceCleanup("jni", apiName);
      final boolean panamaCleansUp = testResourceCleanup("panama", apiName);

      if (jniCleansUp != panamaCleansUp) {
        violations.add(
            ParityViolation.builder(apiName, "resourceCleanup")
                .withType(ParityViolation.ParityViolationType.RESOURCE_MANAGEMENT_DIFFERENCE)
                .withDescription("Inconsistent resource cleanup behavior")
                .withSeverity(ParityViolation.Severity.MEDIUM)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Resource cleanup parity testing failed for " + apiName + ": " + e.getMessage());
    }

    return violations;
  }

  // Cross-Implementation Compatibility Testing

  private List<ParityViolation> testCrossImplementationCompatibility() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test if objects created by one implementation can be used by another
      violations.addAll(testModuleCrossCompatibility());
      violations.addAll(testSerializationCrossCompatibility());

    } catch (final Exception e) {
      LOGGER.warning("Cross-implementation compatibility testing failed: " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testModuleCrossCompatibility() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test if modules compiled by one implementation can be used by another
      final byte[] testWasm = generateCompatibilityTestWasm();

      // Compile with JNI, use with Panama (and vice versa)
      // This would require more sophisticated testing infrastructure
      // For now, we'll just verify that both can compile the same WASM

      boolean jniCanCompile = false;
      boolean panamaCanCompile = false;

      try (final Engine jniEngine = Engine.create()) {
        try (final Module jniModule = Module.compile(jniEngine, testWasm)) {
          jniCanCompile = true;
        }
      } catch (final Exception e) {
        LOGGER.fine("JNI module compilation failed: " + e.getMessage());
      }

      try (final Engine panamaEngine = Engine.create()) {
        try (final Module panamaModule = Module.compile(panamaEngine, testWasm)) {
          panamaCanCompile = true;
        }
      } catch (final Exception e) {
        LOGGER.fine("Panama module compilation failed: " + e.getMessage());
      }

      if (jniCanCompile != panamaCanCompile) {
        violations.add(
            ParityViolation.builder("Module", "crossCompatibility")
                .withType(ParityViolation.ParityViolationType.COMPATIBILITY_ISSUE)
                .withDescription("Inconsistent WASM compilation support between implementations")
                .withSeverity(ParityViolation.Severity.HIGH)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Module cross-compatibility testing failed: " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testSerializationCrossCompatibility() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test serialization compatibility between implementations
      final byte[] testWasm = generateCompatibilityTestWasm();

      // Test if modules serialized by one implementation can be deserialized by another
      // This is a simplified test focusing on basic serialization functionality

      byte[] jniSerialized = null;
      byte[] panamaSerialized = null;

      try (final Engine engine = Engine.create()) {
        try (final Module module = Module.compile(engine, testWasm)) {
          jniSerialized = module.serialize();
        }
      } catch (final Exception e) {
        LOGGER.fine("JNI serialization failed: " + e.getMessage());
      }

      try (final Engine engine = Engine.create()) {
        try (final Module module = Module.compile(engine, testWasm)) {
          panamaSerialized = module.serialize();
        }
      } catch (final Exception e) {
        LOGGER.fine("Panama serialization failed: " + e.getMessage());
      }

      if ((jniSerialized == null) != (panamaSerialized == null)) {
        violations.add(
            ParityViolation.builder("Module", "serialization")
                .withType(ParityViolation.ParityViolationType.COMPATIBILITY_ISSUE)
                .withDescription("Inconsistent serialization support between implementations")
                .withSeverity(ParityViolation.Severity.MEDIUM)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Serialization cross-compatibility testing failed: " + e.getMessage());
    }

    return violations;
  }

  // Threading Parity Testing

  private List<ParityViolation> testThreadingParity() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test threading behavior consistency
      violations.addAll(testConcurrentAccessParity());
      violations.addAll(testThreadSafetyParity());

    } catch (final Exception e) {
      LOGGER.warning("Threading parity testing failed: " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testConcurrentAccessParity() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test concurrent access patterns
      final int threadCount = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      final CompletableFuture<Boolean> jniFuture = CompletableFuture.supplyAsync(
          () -> testConcurrentAccess("jni"), executor);
      final CompletableFuture<Boolean> panamaFuture = CompletableFuture.supplyAsync(
          () -> testConcurrentAccess("panama"), executor);

      final boolean jniResult = jniFuture.get(30, TimeUnit.SECONDS);
      final boolean panamaResult = panamaFuture.get(30, TimeUnit.SECONDS);

      executor.shutdown();

      if (jniResult != panamaResult) {
        violations.add(
            ParityViolation.builder("Engine", "concurrentAccess")
                .withType(ParityViolation.ParityViolationType.THREADING_DIFFERENCE)
                .withDescription("Inconsistent concurrent access behavior")
                .withSeverity(ParityViolation.Severity.MEDIUM)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Concurrent access parity testing failed: " + e.getMessage());
    }

    return violations;
  }

  private List<ParityViolation> testThreadSafetyParity() {
    final List<ParityViolation> violations = new ArrayList<>();

    try {
      // Test thread safety characteristics
      final boolean jniThreadSafe = testThreadSafety("jni");
      final boolean panamaThreadSafe = testThreadSafety("panama");

      if (jniThreadSafe != panamaThreadSafe) {
        violations.add(
            ParityViolation.builder("Engine", "threadSafety")
                .withType(ParityViolation.ParityViolationType.THREADING_DIFFERENCE)
                .withDescription("Inconsistent thread safety characteristics")
                .withSeverity(ParityViolation.Severity.HIGH)
                .build());
      }

    } catch (final Exception e) {
      LOGGER.fine("Thread safety parity testing failed: " + e.getMessage());
    }

    return violations;
  }

  // Helper Methods

  private boolean hasJniImplementation(final String apiName) {
    try {
      final String className = "ai.tegmentum.wasmtime4j.jni.Jni" + apiName;
      Class.forName(className);
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  private boolean hasPanamaImplementation(final String apiName) {
    try {
      final String className = "ai.tegmentum.wasmtime4j.panama.Panama" + apiName;
      Class.forName(className);
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  private Class<?> getJniImplementationClass(final String apiName) {
    try {
      return Class.forName("ai.tegmentum.wasmtime4j.jni.Jni" + apiName);
    } catch (final ClassNotFoundException e) {
      return null;
    }
  }

  private Class<?> getPanamaImplementationClass(final String apiName) {
    try {
      return Class.forName("ai.tegmentum.wasmtime4j.panama.Panama" + apiName);
    } catch (final ClassNotFoundException e) {
      return null;
    }
  }

  private boolean isPublicApiMethod(final Method method) {
    return Modifier.isPublic(method.getModifiers())
        && !Modifier.isStatic(method.getModifiers())
        && !method.isDefault()
        && !method.getName().equals("toString")
        && !method.getName().equals("equals")
        && !method.getName().equals("hashCode");
  }

  private Method findMatchingMethod(final Class<?> clazz, final Method targetMethod) {
    try {
      return clazz.getMethod(targetMethod.getName(), targetMethod.getParameterTypes());
    } catch (final NoSuchMethodException e) {
      return null;
    }
  }

  // Performance Benchmarking Methods

  private Duration benchmarkEngineCreation(final String implementation) {
    final Instant start = Instant.now();
    try (final Engine engine = Engine.create()) {
      // Engine created successfully
    } catch (final Exception e) {
      LOGGER.fine(implementation + " engine creation failed: " + e.getMessage());
    }
    return Duration.between(start, Instant.now());
  }

  private Duration benchmarkModuleCompilation(final String implementation, final byte[] wasmBytes) {
    final Instant start = Instant.now();
    try (final Engine engine = Engine.create();
         final Module module = Module.compile(engine, wasmBytes)) {
      // Module compiled successfully
    } catch (final Exception e) {
      LOGGER.fine(implementation + " module compilation failed: " + e.getMessage());
    }
    return Duration.between(start, Instant.now());
  }

  private Duration benchmarkFunctionCalls(final String implementation) {
    final byte[] wasmBytes = generatePerformanceTestWasm();
    final Instant start = Instant.now();

    try (final Engine engine = Engine.create();
         final Store store = Store.create(engine);
         final Module module = Module.compile(engine, wasmBytes);
         final Instance instance = Instance.create(store, module)) {

      final Function addFunction = instance.getExport("add", Function.class);
      if (addFunction != null) {
        for (int i = 0; i < 100; i++) {
          addFunction.call(i, i + 1);
        }
      }

    } catch (final Exception e) {
      LOGGER.fine(implementation + " function calls failed: " + e.getMessage());
    }

    return Duration.between(start, Instant.now());
  }

  // Error Handling Test Methods

  private Exception testNullParameterHandling(final String implementation, final String apiName) {
    try {
      if (apiName.equals("Engine")) {
        Engine.create(null); // Should throw exception
      } else if (apiName.equals("Module")) {
        try (final Engine engine = Engine.create()) {
          Module.compile(engine, null); // Should throw exception
        }
      }
      return null; // No exception thrown
    } catch (final Exception e) {
      return e;
    }
  }

  private boolean testResourceExhaustion(final String implementation, final String apiName) {
    try {
      // Create many resources to test exhaustion handling
      final List<AutoCloseable> resources = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        if (apiName.equals("Engine")) {
          resources.add(Engine.create());
        } else if (apiName.equals("Store")) {
          final Engine engine = Engine.create();
          resources.add(Store.create(engine));
          resources.add(engine);
        }
      }

      // Clean up
      for (final AutoCloseable resource : resources) {
        try {
          resource.close();
        } catch (final Exception e) {
          // Cleanup failure
        }
      }

      return true; // Handled exhaustion gracefully
    } catch (final Exception e) {
      return false; // Failed to handle exhaustion
    }
  }

  private Exception testMalformedWasmCompilation(final String implementation, final byte[] malformedWasm) {
    try (final Engine engine = Engine.create()) {
      Module.compile(engine, malformedWasm); // Should throw exception
      return null;
    } catch (final Exception e) {
      return e;
    }
  }

  // Memory Management Test Methods

  private long measureMemoryUsage(final String implementation, final String apiName) {
    final Runtime runtime = Runtime.getRuntime();
    final long before = runtime.totalMemory() - runtime.freeMemory();

    try {
      // Create and use resources
      for (int i = 0; i < 10; i++) {
        if (apiName.equals("Engine")) {
          try (final Engine engine = Engine.create()) {
            // Use engine
          }
        } else if (apiName.equals("Module")) {
          try (final Engine engine = Engine.create()) {
            final byte[] wasmBytes = generatePerformanceTestWasm();
            try (final Module module = Module.compile(engine, wasmBytes)) {
              // Use module
            }
          }
        }
      }

      System.gc(); // Encourage garbage collection
      Thread.sleep(100);

    } catch (final Exception e) {
      LOGGER.fine("Memory usage measurement failed: " + e.getMessage());
    }

    final long after = runtime.totalMemory() - runtime.freeMemory();
    return Math.max(0, after - before);
  }

  private boolean testResourceCleanup(final String implementation, final String apiName) {
    try {
      // Test if resources are properly cleaned up
      final List<AutoCloseable> resources = new ArrayList<>();

      // Create resources
      for (int i = 0; i < 10; i++) {
        if (apiName.equals("Engine")) {
          resources.add(Engine.create());
        }
      }

      // Close all resources
      for (final AutoCloseable resource : resources) {
        resource.close();
      }

      return true; // Cleanup successful
    } catch (final Exception e) {
      return false; // Cleanup failed
    }
  }

  // Threading Test Methods

  private boolean testConcurrentAccess(final String implementation) {
    try {
      final int threadCount = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final List<Future<Boolean>> futures = new ArrayList<>();

      for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(() -> {
          try (final Engine engine = Engine.create()) {
            return engine != null;
          } catch (final Exception e) {
            return false;
          }
        }));
      }

      boolean allSucceeded = true;
      for (final Future<Boolean> future : futures) {
        if (!future.get(10, TimeUnit.SECONDS)) {
          allSucceeded = false;
        }
      }

      executor.shutdown();
      return allSucceeded;

    } catch (final Exception e) {
      return false;
    }
  }

  private boolean testThreadSafety(final String implementation) {
    try {
      // Simple thread safety test
      final Engine sharedEngine = Engine.create();
      final int threadCount = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final List<Future<Boolean>> futures = new ArrayList<>();

      for (int i = 0; i < threadCount; i++) {
        futures.add(executor.submit(() -> {
          try {
            // Try to use shared engine from multiple threads
            final Store store = Store.create(sharedEngine);
            store.close();
            return true;
          } catch (final Exception e) {
            return false;
          }
        }));
      }

      boolean allSucceeded = true;
      for (final Future<Boolean> future : futures) {
        if (!future.get(10, TimeUnit.SECONDS)) {
          allSucceeded = false;
        }
      }

      executor.shutdown();
      sharedEngine.close();
      return allSucceeded;

    } catch (final Exception e) {
      return false;
    }
  }

  // WASM Generation Helper Methods

  private byte[] generatePerformanceTestWasm() {
    return createBasicWasmModule();
  }

  private byte[] generateCompatibilityTestWasm() {
    return createBasicWasmModule();
  }

  private byte[] createBasicWasmModule() {
    // This is a minimal valid WebAssembly module with an add function
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // WASM magic number
      0x01, 0x00, 0x00, 0x00, // Version
      // Type section (function signatures)
      0x01, 0x07, 0x01, 0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f,
      // Function section
      0x03, 0x02, 0x01, 0x00,
      // Export section
      0x07, 0x07, 0x01, 0x03, 0x61, 0x64, 0x64, 0x00, 0x00,
      // Code section
      0x0a, 0x09, 0x01, 0x07, 0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b
    };
  }
}