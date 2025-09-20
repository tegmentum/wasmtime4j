/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.Map;

/**
 * Code coverage report for test execution.
 *
 * <p>Provides detailed metrics about code coverage achieved during test execution, including line,
 * branch, and method coverage.
 *
 * @since 1.0.0
 */
public interface CoverageReport {

  /**
   * Returns overall line coverage percentage.
   *
   * @return line coverage from 0.0 to 100.0
   */
  double getLineCoverage();

  /**
   * Returns overall branch coverage percentage.
   *
   * @return branch coverage from 0.0 to 100.0
   */
  double getBranchCoverage();

  /**
   * Returns overall method coverage percentage.
   *
   * @return method coverage from 0.0 to 100.0
   */
  double getMethodCoverage();

  /**
   * Returns coverage percentages by package.
   *
   * @return immutable map of package names to coverage percentages
   */
  Map<String, Double> getCoverageByPackage();

  /**
   * Returns coverage percentages by class.
   *
   * @return immutable map of class names to coverage percentages
   */
  Map<String, Double> getCoverageByClass();

  /**
   * Returns the total number of lines analyzed.
   *
   * @return total line count
   */
  int getTotalLines();

  /**
   * Returns the number of lines covered by tests.
   *
   * @return covered line count
   */
  int getCoveredLines();

  /**
   * Returns the total number of branches analyzed.
   *
   * @return total branch count
   */
  int getTotalBranches();

  /**
   * Returns the number of branches covered by tests.
   *
   * @return covered branch count
   */
  int getCoveredBranches();

  /**
   * Returns the total number of methods analyzed.
   *
   * @return total method count
   */
  int getTotalMethods();

  /**
   * Returns the number of methods covered by tests.
   *
   * @return covered method count
   */
  int getCoveredMethods();

  /**
   * Checks if coverage meets the specified threshold.
   *
   * @param threshold the minimum coverage threshold
   * @return {@code true} if coverage meets threshold, {@code false} otherwise
   */
  boolean meetsThreshold(final double threshold);

  /**
   * Returns human-readable coverage summary.
   *
   * @return formatted coverage summary
   */
  String getSummary();
}
