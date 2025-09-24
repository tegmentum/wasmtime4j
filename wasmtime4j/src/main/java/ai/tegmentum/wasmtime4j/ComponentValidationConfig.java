/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j;

/**
 * Configuration for component validation operations.
 *
 * <p>This class provides configuration options for validating WebAssembly components, including
 * validation depth, security checks, and performance limits.
 *
 * @since 1.0.0
 */
public final class ComponentValidationConfig {

  private final boolean validateSecurity;
  private final boolean validatePerformance;
  private final boolean validateInterfaces;
  private final boolean validateDependencies;
  private final boolean strictMode;
  private final long maxValidationTimeMs;

  private ComponentValidationConfig(Builder builder) {
    this.validateSecurity = builder.validateSecurity;
    this.validatePerformance = builder.validatePerformance;
    this.validateInterfaces = builder.validateInterfaces;
    this.validateDependencies = builder.validateDependencies;
    this.strictMode = builder.strictMode;
    this.maxValidationTimeMs = builder.maxValidationTimeMs;
  }

  /**
   * Checks if security validation is enabled.
   *
   * @return true if security validation is enabled
   */
  public boolean isValidateSecurity() {
    return validateSecurity;
  }

  /**
   * Checks if performance validation is enabled.
   *
   * @return true if performance validation is enabled
   */
  public boolean isValidatePerformance() {
    return validatePerformance;
  }

  /**
   * Checks if interface validation is enabled.
   *
   * @return true if interface validation is enabled
   */
  public boolean isValidateInterfaces() {
    return validateInterfaces;
  }

  /**
   * Checks if dependency validation is enabled.
   *
   * @return true if dependency validation is enabled
   */
  public boolean isValidateDependencies() {
    return validateDependencies;
  }

  /**
   * Checks if strict mode is enabled.
   *
   * @return true if strict mode is enabled
   */
  public boolean isStrictMode() {
    return strictMode;
  }

  /**
   * Gets the maximum validation time in milliseconds.
   *
   * @return the maximum validation time
   */
  public long getMaxValidationTimeMs() {
    return maxValidationTimeMs;
  }

  /**
   * Creates a default configuration with all validations enabled.
   *
   * @return a default configuration
   */
  public static ComponentValidationConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Creates a new builder for ComponentValidationConfig.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ComponentValidationConfig. */
  public static final class Builder {
    private boolean validateSecurity = true;
    private boolean validatePerformance = true;
    private boolean validateInterfaces = true;
    private boolean validateDependencies = true;
    private boolean strictMode = false;
    private long maxValidationTimeMs = 30000; // 30 seconds default

    private Builder() {}

    /**
     * Sets whether to validate security aspects.
     *
     * @param validateSecurity true to enable security validation
     * @return this builder
     */
    public Builder validateSecurity(boolean validateSecurity) {
      this.validateSecurity = validateSecurity;
      return this;
    }

    /**
     * Sets whether to validate performance aspects.
     *
     * @param validatePerformance true to enable performance validation
     * @return this builder
     */
    public Builder validatePerformance(boolean validatePerformance) {
      this.validatePerformance = validatePerformance;
      return this;
    }

    /**
     * Sets whether to validate interfaces.
     *
     * @param validateInterfaces true to enable interface validation
     * @return this builder
     */
    public Builder validateInterfaces(boolean validateInterfaces) {
      this.validateInterfaces = validateInterfaces;
      return this;
    }

    /**
     * Sets whether to validate dependencies.
     *
     * @param validateDependencies true to enable dependency validation
     * @return this builder
     */
    public Builder validateDependencies(boolean validateDependencies) {
      this.validateDependencies = validateDependencies;
      return this;
    }

    /**
     * Sets whether to enable strict mode validation.
     *
     * @param strictMode true to enable strict mode
     * @return this builder
     */
    public Builder strictMode(boolean strictMode) {
      this.strictMode = strictMode;
      return this;
    }

    /**
     * Sets the maximum validation time.
     *
     * @param maxValidationTimeMs the maximum validation time in milliseconds
     * @return this builder
     */
    public Builder maxValidationTimeMs(long maxValidationTimeMs) {
      this.maxValidationTimeMs = maxValidationTimeMs;
      return this;
    }

    /**
     * Builds the ComponentValidationConfig.
     *
     * @return the configured ComponentValidationConfig
     */
    public ComponentValidationConfig build() {
      return new ComponentValidationConfig(this);
    }
  }
}
