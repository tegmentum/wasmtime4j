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
import java.util.List;

/**
 * Result of validating a specific API endpoint.
 *
 * <p>This class provides comprehensive validation results for individual API methods including
 * functional correctness, performance metrics, and compliance information.
 */
public final class ApiValidationResult {

  private final String apiName;
  private final String methodName;
  private final boolean isValid;
  private final boolean isImplemented;
  private final boolean hasNativeBacking;
  private final List<String> validationErrors;
  private final Duration executionTime;
  private final TestResults testResults;

  private ApiValidationResult(final Builder builder) {
    this.apiName = builder.apiName;
    this.methodName = builder.methodName;
    this.isValid = builder.isValid;
    this.isImplemented = builder.isImplemented;
    this.hasNativeBacking = builder.hasNativeBacking;
    this.validationErrors = builder.validationErrors;
    this.executionTime = builder.executionTime;
    this.testResults = builder.testResults;
  }

  /**
   * Creates a new API validation result builder.
   *
   * @param apiName the API name
   * @param methodName the method name
   * @return a new builder instance
   */
  public static Builder builder(final String apiName, final String methodName) {
    return new Builder(apiName, methodName);
  }

  /**
   * Gets the API name.
   *
   * @return API name
   */
  public String getApiName() {
    return apiName;
  }

  /**
   * Gets the method name.
   *
   * @return method name
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * Checks if the API validation passed.
   *
   * @return true if validation passed
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Checks if the API method is implemented.
   *
   * @return true if implemented
   */
  public boolean isImplemented() {
    return isImplemented;
  }

  /**
   * Checks if the API method has native backing.
   *
   * @return true if has native backing
   */
  public boolean hasNativeBacking() {
    return hasNativeBacking;
  }

  /**
   * Gets the validation errors (if any).
   *
   * @return list of validation errors
   */
  public List<String> getValidationErrors() {
    return validationErrors;
  }

  /**
   * Gets the validation execution time.
   *
   * @return execution time
   */
  public Duration getExecutionTime() {
    return executionTime;
  }

  /**
   * Gets the detailed test results for this API.
   *
   * @return test results or null if no tests were run
   */
  public TestResults getTestResults() {
    return testResults;
  }

  /**
   * Gets the full API identifier.
   *
   * @return full API identifier in format "apiName.methodName"
   */
  public String getFullApiName() {
    return apiName + "." + methodName;
  }

  @Override
  public String toString() {
    return String.format(
        "ApiValidationResult{api='%s.%s', valid=%s, implemented=%s, nativeBacking=%s, errors=%d}",
        apiName, methodName, isValid, isImplemented, hasNativeBacking, validationErrors.size());
  }

  /** Builder for creating ApiValidationResult instances. */
  public static final class Builder {

    private final String apiName;
    private final String methodName;
    private boolean isValid = false;
    private boolean isImplemented = false;
    private boolean hasNativeBacking = false;
    private List<String> validationErrors = new java.util.ArrayList<>();
    private Duration executionTime = Duration.ZERO;
    private TestResults testResults;

    private Builder(final String apiName, final String methodName) {
      this.apiName = apiName;
      this.methodName = methodName;
    }

    /**
     * Sets whether the validation passed.
     *
     * @param isValid true if validation passed
     * @return this builder
     */
    public Builder withValid(final boolean isValid) {
      this.isValid = isValid;
      return this;
    }

    /**
     * Sets whether the API is implemented.
     *
     * @param isImplemented true if implemented
     * @return this builder
     */
    public Builder withImplemented(final boolean isImplemented) {
      this.isImplemented = isImplemented;
      return this;
    }

    /**
     * Sets whether the API has native backing.
     *
     * @param hasNativeBacking true if has native backing
     * @return this builder
     */
    public Builder withNativeBacking(final boolean hasNativeBacking) {
      this.hasNativeBacking = hasNativeBacking;
      return this;
    }

    /**
     * Sets the validation errors.
     *
     * @param validationErrors list of validation errors
     * @return this builder
     */
    public Builder withValidationErrors(final List<String> validationErrors) {
      this.validationErrors = new java.util.ArrayList<>(validationErrors);
      return this;
    }

    /**
     * Adds a validation error.
     *
     * @param error the validation error
     * @return this builder
     */
    public Builder addValidationError(final String error) {
      if (this.validationErrors == null) {
        this.validationErrors = new java.util.ArrayList<>();
      }
      this.validationErrors.add(error);
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
     * Sets the test results.
     *
     * @param testResults the test results
     * @return this builder
     */
    public Builder withTestResults(final TestResults testResults) {
      this.testResults = testResults;
      return this;
    }

    /**
     * Builds the ApiValidationResult instance.
     *
     * @return the API validation result
     */
    public ApiValidationResult build() {
      return new ApiValidationResult(this);
    }
  }
}
