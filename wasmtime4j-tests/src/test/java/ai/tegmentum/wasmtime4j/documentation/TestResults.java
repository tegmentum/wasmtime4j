/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.time.Duration;
import java.util.List;

/**
 * Results of test execution with comprehensive metrics and analysis.
 *
 * <p>Provides detailed information about test execution including pass/fail statistics, coverage
 * metrics, and failure analysis.
 *
 * @since 1.0.0
 */
public interface TestResults {

  /**
   * Returns the total number of tests executed.
   *
   * @return total test count
   */
  int getTotalTests();

  /**
   * Returns the number of tests that passed.
   *
   * @return passed test count
   */
  int getPassedTests();

  /**
   * Returns the number of tests that failed.
   *
   * @return failed test count
   */
  int getFailedTests();

  /**
   * Returns the number of tests that were skipped.
   *
   * @return skipped test count
   */
  int getSkippedTests();

  /**
   * Returns list of test failures with detailed information.
   *
   * @return immutable list of test failures
   */
  List<TestFailure> getFailures();

  /**
   * Returns code coverage report for the test execution.
   *
   * @return coverage analysis report
   */
  CoverageReport getCoverageReport();

  /**
   * Returns the total execution time for all tests.
   *
   * @return test execution duration
   */
  Duration getExecutionTime();

  /**
   * Returns the success rate as a percentage.
   *
   * @return success rate from 0.0 to 100.0
   */
  double getSuccessRate();

  /**
   * Checks if all tests passed successfully.
   *
   * @return {@code true} if all tests passed, {@code false} otherwise
   */
  boolean isAllTestsPassed();

  /**
   * Returns performance metrics collected during test execution.
   *
   * @return performance measurement data
   */
  PerformanceMetrics getPerformanceMetrics();

  /**
   * Returns human-readable summary of test results.
   *
   * @return formatted test summary
   */
  String getSummary();

  /**
   * Returns detailed test execution report.
   *
   * @return comprehensive test report
   */
  String getDetailedReport();
}
