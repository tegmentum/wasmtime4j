/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Comprehensive test suite for API coverage and validation.
 *
 * <p>This test suite provides complete validation of the wasmtime4j API, including functional
 * testing, parity validation, and performance verification.
 *
 * @since 1.0.0
 */
public interface ComprehensiveTestSuite {

  /**
   * Runs all available tests for complete API validation.
   *
   * <p>Includes:
   *
   * <ul>
   *   <li>Unit tests for all public methods
   *   <li>Integration tests for complex scenarios
   *   <li>Parity tests between implementations
   *   <li>Performance and compatibility tests
   * </ul>
   *
   * @return comprehensive test results with all metrics
   * @throws TestExecutionException if test execution fails
   */
  TestResults runAllTests();

  /**
   * Runs parity tests between JNI and Panama implementations.
   *
   * <p>Validates:
   *
   * <ul>
   *   <li>Functional equivalence of all methods
   *   <li>Consistent behavior across implementations
   *   <li>Exception handling compatibility
   *   <li>Resource management consistency
   * </ul>
   *
   * @return parity test results
   * @throws TestExecutionException if test execution fails
   */
  TestResults runParityTests();

  /**
   * Runs performance tests and benchmarks.
   *
   * <p>Measures:
   *
   * <ul>
   *   <li>Execution time for key operations
   *   <li>Memory usage patterns
   *   <li>Throughput characteristics
   *   <li>Resource allocation efficiency
   * </ul>
   *
   * @return performance test results
   * @throws TestExecutionException if test execution fails
   */
  TestResults runPerformanceTests();

  /**
   * Runs compatibility tests across platforms and Java versions.
   *
   * <p>Validates:
   *
   * <ul>
   *   <li>Cross-platform functionality
   *   <li>Java version compatibility
   *   <li>Native library loading
   *   <li>Runtime environment compatibility
   * </ul>
   *
   * @return compatibility test results
   * @throws TestExecutionException if test execution fails
   */
  TestResults runCompatibilityTests();

  /**
   * Runs focused tests for specific API components.
   *
   * @param componentName the name of the component to test
   * @return component-specific test results
   * @throws IllegalArgumentException if component name is invalid
   * @throws TestExecutionException if test execution fails
   */
  TestResults runComponentTests(final String componentName);

  /**
   * Runs stress tests to validate system behavior under load.
   *
   * <p>Tests:
   *
   * <ul>
   *   <li>High-concurrency scenarios
   *   <li>Memory pressure situations
   *   <li>Long-running operations
   *   <li>Resource exhaustion handling
   * </ul>
   *
   * @return stress test results
   * @throws TestExecutionException if test execution fails
   */
  TestResults runStressTests();
}
