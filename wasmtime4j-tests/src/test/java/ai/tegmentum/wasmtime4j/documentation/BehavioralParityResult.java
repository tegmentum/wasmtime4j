/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.List;
import java.util.Objects;

/**
 * Result of behavioral parity validation for a specific API method.
 *
 * <p>Contains detailed information about behavioral consistency between JNI and Panama
 * implementations for a single method.
 *
 * @since 1.0.0
 */
public final class BehavioralParityResult {

  private final String methodSignature;
  private final boolean isParityAchieved;
  private final List<TestCase> testCases;
  private final List<String> differences;
  private final double confidenceScore;
  private final String summary;

  /**
   * Creates a new behavioral parity result.
   *
   * @param methodSignature the method signature that was tested
   * @param isParityAchieved whether behavioral parity was achieved
   * @param testCases list of test cases executed
   * @param differences list of detected behavioral differences
   * @param confidenceScore confidence in the parity assessment (0.0 to 1.0)
   * @param summary human-readable summary of the results
   */
  public BehavioralParityResult(
      final String methodSignature,
      final boolean isParityAchieved,
      final List<TestCase> testCases,
      final List<String> differences,
      final double confidenceScore,
      final String summary) {
    this.methodSignature = Objects.requireNonNull(methodSignature, "methodSignature");
    this.isParityAchieved = isParityAchieved;
    this.testCases = List.copyOf(Objects.requireNonNull(testCases, "testCases"));
    this.differences = List.copyOf(Objects.requireNonNull(differences, "differences"));
    this.confidenceScore = confidenceScore;
    this.summary = Objects.requireNonNull(summary, "summary");
  }

  /**
   * Returns the method signature that was tested.
   *
   * @return method signature
   */
  public String getMethodSignature() {
    return methodSignature;
  }

  /**
   * Checks if behavioral parity was achieved.
   *
   * @return {@code true} if parity achieved, {@code false} otherwise
   */
  public boolean isParityAchieved() {
    return isParityAchieved;
  }

  /**
   * Returns list of test cases executed during validation.
   *
   * @return immutable list of test cases
   */
  public List<TestCase> getTestCases() {
    return testCases;
  }

  /**
   * Returns list of detected behavioral differences.
   *
   * @return immutable list of differences
   */
  public List<String> getDifferences() {
    return differences;
  }

  /**
   * Returns confidence score in the parity assessment.
   *
   * @return confidence score from 0.0 to 1.0
   */
  public double getConfidenceScore() {
    return confidenceScore;
  }

  /**
   * Returns human-readable summary of the results.
   *
   * @return result summary
   */
  public String getSummary() {
    return summary;
  }

  /**
   * Returns the number of successful test cases.
   *
   * @return count of successful test cases
   */
  public int getSuccessfulTestCases() {
    return (int) testCases.stream().filter(TestCase::isSuccessful).count();
  }

  /**
   * Returns the total number of test cases.
   *
   * @return total test case count
   */
  public int getTotalTestCases() {
    return testCases.size();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BehavioralParityResult that = (BehavioralParityResult) obj;
    return Objects.equals(methodSignature, that.methodSignature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(methodSignature);
  }

  @Override
  public String toString() {
    return "BehavioralParityResult{"
        + "method='"
        + methodSignature
        + '\''
        + ", parityAchieved="
        + isParityAchieved
        + ", testCases="
        + getTotalTestCases()
        + ", successful="
        + getSuccessfulTestCases()
        + ", confidence="
        + String.format("%.2f", confidenceScore)
        + '}';
  }

  /** Represents a single test case in behavioral parity validation. */
  public static final class TestCase {
    private final String description;
    private final String input;
    private final String jniOutput;
    private final String panamaOutput;
    private final boolean isSuccessful;

    /**
     * Creates a new test case.
     *
     * @param description test case description
     * @param input test input parameters
     * @param jniOutput JNI implementation output
     * @param panamaOutput Panama implementation output
     * @param isSuccessful whether outputs match
     */
    public TestCase(
        final String description,
        final String input,
        final String jniOutput,
        final String panamaOutput,
        final boolean isSuccessful) {
      this.description = Objects.requireNonNull(description, "description");
      this.input = Objects.requireNonNull(input, "input");
      this.jniOutput = Objects.requireNonNull(jniOutput, "jniOutput");
      this.panamaOutput = Objects.requireNonNull(panamaOutput, "panamaOutput");
      this.isSuccessful = isSuccessful;
    }

    public String getDescription() {
      return description;
    }

    public String getInput() {
      return input;
    }

    public String getJniOutput() {
      return jniOutput;
    }

    public String getPanamaOutput() {
      return panamaOutput;
    }

    public boolean isSuccessful() {
      return isSuccessful;
    }
  }
}
