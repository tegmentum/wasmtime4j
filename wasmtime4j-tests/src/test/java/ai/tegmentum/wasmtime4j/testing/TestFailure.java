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

import java.time.Duration;

/**
 * Represents detailed information about a test failure.
 *
 * <p>This class captures comprehensive failure information including the test name, error message,
 * exception details, and execution context for debugging.
 */
public final class TestFailure {

  private final String testName;
  private final String errorMessage;
  private final Throwable exception;
  private final Duration executionTime;
  private final String testClass;
  private final String testMethod;

  private TestFailure(final Builder builder) {
    this.testName = builder.testName;
    this.errorMessage = builder.errorMessage;
    this.exception = builder.exception;
    this.executionTime = builder.executionTime;
    this.testClass = builder.testClass;
    this.testMethod = builder.testMethod;
  }

  /**
   * Creates a new test failure builder.
   *
   * @param testName the name of the failed test
   * @return a new builder instance
   */
  public static Builder builder(final String testName) {
    return new Builder(testName);
  }

  /**
   * Gets the name of the failed test.
   *
   * @return test name
   */
  public String getTestName() {
    return testName;
  }

  /**
   * Gets the error message describing the failure.
   *
   * @return error message
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Gets the exception that caused the failure (if any).
   *
   * @return exception or null if no exception was thrown
   */
  public Throwable getException() {
    return exception;
  }

  /**
   * Gets the execution time before failure occurred.
   *
   * @return execution time
   */
  public Duration getExecutionTime() {
    return executionTime;
  }

  /**
   * Gets the test class name.
   *
   * @return test class name
   */
  public String getTestClass() {
    return testClass;
  }

  /**
   * Gets the test method name.
   *
   * @return test method name
   */
  public String getTestMethod() {
    return testMethod;
  }

  /**
   * Gets the stack trace as a string (if exception is available).
   *
   * @return stack trace or empty string if no exception
   */
  public String getStackTrace() {
    if (exception == null) {
      return "";
    }
    final java.io.StringWriter sw = new java.io.StringWriter();
    final java.io.PrintWriter pw = new java.io.PrintWriter(sw);
    exception.printStackTrace(pw);
    return sw.toString();
  }

  @Override
  public String toString() {
    return String.format(
        "TestFailure{testName='%s', errorMessage='%s', executionTime=%s}",
        testName, errorMessage, executionTime);
  }

  /** Builder for creating TestFailure instances. */
  public static final class Builder {

    private final String testName;
    private String errorMessage;
    private Throwable exception;
    private Duration executionTime = Duration.ZERO;
    private String testClass;
    private String testMethod;

    private Builder(final String testName) {
      this.testName = testName;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage the error message
     * @return this builder
     */
    public Builder withErrorMessage(final String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    /**
     * Sets the exception that caused the failure.
     *
     * @param exception the exception
     * @return this builder
     */
    public Builder withException(final Throwable exception) {
      this.exception = exception;
      if (errorMessage == null && exception != null) {
        this.errorMessage = exception.getMessage();
      }
      return this;
    }

    /**
     * Sets the execution time.
     *
     * @param executionTime the execution time
     * @return this builder
     */
    public Builder withExecutionTime(final Duration executionTime) {
      this.executionTime = executionTime;
      return this;
    }

    /**
     * Sets the test class name.
     *
     * @param testClass the test class name
     * @return this builder
     */
    public Builder withTestClass(final String testClass) {
      this.testClass = testClass;
      return this;
    }

    /**
     * Sets the test method name.
     *
     * @param testMethod the test method name
     * @return this builder
     */
    public Builder withTestMethod(final String testMethod) {
      this.testMethod = testMethod;
      return this;
    }

    /**
     * Builds the TestFailure instance.
     *
     * @return the test failure
     */
    public TestFailure build() {
      return new TestFailure(this);
    }
  }
}
