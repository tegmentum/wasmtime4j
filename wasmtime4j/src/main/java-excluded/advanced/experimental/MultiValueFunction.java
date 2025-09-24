/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.experimental;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import java.util.List;
import java.util.Objects;

/**
 * Support for WebAssembly multi-value proposal functions.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API is experimental and subject to change. It implements
 * the WebAssembly multi-value proposal, allowing functions to return multiple values directly
 * rather than through memory or global variables.
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.MULTI_VALUE)
public final class MultiValueFunction {

  /** Multi-value function signature. */
  public static final class MultiValueSignature {
    private final List<WasmValueType> parameterTypes;
    private final List<WasmValueType> returnTypes;
    private final String name;

    /**
     * Creates a new multi-value function signature.
     *
     * @param name the function name
     * @param parameterTypes the parameter types
     * @param returnTypes the return types (may be multiple)
     * @throws IllegalArgumentException if any parameter is null or name is empty
     */
    public MultiValueSignature(
        final String name,
        final List<WasmValueType> parameterTypes,
        final List<WasmValueType> returnTypes) {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Function name cannot be null or empty");
      }
      if (parameterTypes == null) {
        throw new IllegalArgumentException("Parameter types cannot be null");
      }
      if (returnTypes == null) {
        throw new IllegalArgumentException("Return types cannot be null");
      }

      this.name = name.trim();
      this.parameterTypes = List.copyOf(parameterTypes);
      this.returnTypes = List.copyOf(returnTypes);
    }

    /**
     * Gets the function name.
     *
     * @return the function name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the parameter types.
     *
     * @return immutable list of parameter types
     */
    public List<WasmValueType> getParameterTypes() {
      return parameterTypes;
    }

    /**
     * Gets the return types.
     *
     * @return immutable list of return types
     */
    public List<WasmValueType> getReturnTypes() {
      return returnTypes;
    }

    /**
     * Checks if this function returns multiple values.
     *
     * @return true if function returns multiple values
     */
    public boolean isMultiValue() {
      return returnTypes.size() > 1;
    }

    /**
     * Gets the number of return values.
     *
     * @return the number of return values
     */
    public int getReturnCount() {
      return returnTypes.size();
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final MultiValueSignature that = (MultiValueSignature) obj;
      return Objects.equals(name, that.name)
          && Objects.equals(parameterTypes, that.parameterTypes)
          && Objects.equals(returnTypes, that.returnTypes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, parameterTypes, returnTypes);
    }

    @Override
    public String toString() {
      return "MultiValueSignature{"
          + "name='"
          + name
          + '\''
          + ", parameterTypes="
          + parameterTypes
          + ", returnTypes="
          + returnTypes
          + '}';
    }
  }

  /** Multi-value function result. */
  public static final class MultiValueResult {
    private final List<WasmValue> values;

    /**
     * Creates a new multi-value result.
     *
     * @param values the return values
     * @throws IllegalArgumentException if values is null
     */
    public MultiValueResult(final List<WasmValue> values) {
      if (values == null) {
        throw new IllegalArgumentException("Return values cannot be null");
      }
      this.values = List.copyOf(values);
    }

    /**
     * Gets all return values.
     *
     * @return immutable list of return values
     */
    public List<WasmValue> getValues() {
      return values;
    }

    /**
     * Gets a specific return value by index.
     *
     * @param index the value index
     * @return the value at the specified index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public WasmValue getValue(final int index) {
      return values.get(index);
    }

    /**
     * Gets the first return value.
     *
     * @return the first return value
     * @throws RuntimeException if no values are present
     */
    public WasmValue getFirstValue() {
      if (values.isEmpty()) {
        throw new RuntimeException("No return values available");
      }
      return values.get(0);
    }

    /**
     * Gets the number of return values.
     *
     * @return the number of return values
     */
    public int getValueCount() {
      return values.size();
    }

    /**
     * Checks if multiple values are present.
     *
     * @return true if multiple values are present
     */
    public boolean hasMultipleValues() {
      return values.size() > 1;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final MultiValueResult that = (MultiValueResult) obj;
      return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(values);
    }

    @Override
    public String toString() {
      return "MultiValueResult{values=" + values + '}';
    }
  }

  /** Multi-value host function interface. */
  @FunctionalInterface
  public interface MultiValueHostFunction {
    /**
     * Invokes the host function with the given parameters.
     *
     * @param parameters the input parameters
     * @return the multi-value result
     * @throws RuntimeException if function execution fails
     */
    MultiValueResult invoke(List<WasmValue> parameters);
  }

  /** Multi-value function configuration. */
  public static final class MultiValueConfig {
    private final boolean validateReturnTypes;
    private final boolean enableParameterValidation;
    private final int maxReturnValues;
    private final boolean allowEmptyReturns;

    private MultiValueConfig(final Builder builder) {
      this.validateReturnTypes = builder.validateReturnTypes;
      this.enableParameterValidation = builder.enableParameterValidation;
      this.maxReturnValues = builder.maxReturnValues;
      this.allowEmptyReturns = builder.allowEmptyReturns;
    }

    /**
     * Checks if return type validation is enabled.
     *
     * @return true if return type validation is enabled
     */
    public boolean isReturnTypeValidationEnabled() {
      return validateReturnTypes;
    }

    /**
     * Checks if parameter validation is enabled.
     *
     * @return true if parameter validation is enabled
     */
    public boolean isParameterValidationEnabled() {
      return enableParameterValidation;
    }

    /**
     * Gets the maximum number of return values allowed.
     *
     * @return the maximum number of return values
     */
    public int getMaxReturnValues() {
      return maxReturnValues;
    }

    /**
     * Checks if empty returns are allowed.
     *
     * @return true if empty returns are allowed
     */
    public boolean isEmptyReturnsAllowed() {
      return allowEmptyReturns;
    }

    /**
     * Creates a new builder for multi-value configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
      return new Builder();
    }

    /** Builder for multi-value configuration. */
    public static final class Builder {
      private boolean validateReturnTypes = true;
      private boolean enableParameterValidation = true;
      private int maxReturnValues = 16;
      private boolean allowEmptyReturns = false;

      private Builder() {}

      /**
       * Enables or disables return type validation.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder validateReturnTypes(final boolean enable) {
        this.validateReturnTypes = enable;
        return this;
      }

      /**
       * Enables or disables parameter validation.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableParameterValidation(final boolean enable) {
        this.enableParameterValidation = enable;
        return this;
      }

      /**
       * Sets the maximum number of return values.
       *
       * @param max the maximum number of return values (must be positive)
       * @return this builder
       * @throws IllegalArgumentException if max is not positive
       */
      public Builder maxReturnValues(final int max) {
        if (max <= 0) {
          throw new IllegalArgumentException("Max return values must be positive");
        }
        this.maxReturnValues = max;
        return this;
      }

      /**
       * Enables or disables empty returns.
       *
       * @param allow true to allow, false to disallow
       * @return this builder
       */
      public Builder allowEmptyReturns(final boolean allow) {
        this.allowEmptyReturns = allow;
        return this;
      }

      /**
       * Builds the multi-value configuration.
       *
       * @return the configuration
       */
      public MultiValueConfig build() {
        return new MultiValueConfig(this);
      }
    }
  }

  private final MultiValueConfig config;
  private final long nativeHandle;

  /**
   * Creates a new multi-value function handler.
   *
   * @param config the multi-value configuration
   * @throws IllegalArgumentException if config is null
   * @throws UnsupportedOperationException if multi-value feature is not enabled
   */
  public MultiValueFunction(final MultiValueConfig config) {
    ExperimentalFeatures.validateFeatureSupport(ExperimentalFeatures.Feature.MULTI_VALUE);

    if (config == null) {
      throw new IllegalArgumentException("Multi-value config cannot be null");
    }

    this.config = config;
    this.nativeHandle = createNativeMultiValueHandler(config);
  }

  /**
   * Calls a multi-value WebAssembly function.
   *
   * @param functionHandle the native handle of the WebAssembly function
   * @param signature the function signature
   * @param parameters the input parameters
   * @return the multi-value result
   * @throws IllegalArgumentException if any parameter is null
   * @throws RuntimeException if function call fails
   */
  public MultiValueResult callFunction(
      final long functionHandle,
      final MultiValueSignature signature,
      final List<WasmValue> parameters) {
    if (signature == null) {
      throw new IllegalArgumentException("Function signature cannot be null");
    }
    if (parameters == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }

    // Validate parameters if enabled
    if (config.isParameterValidationEnabled()) {
      validateParameters(signature, parameters);
    }

    final List<WasmValue> result =
        callNativeMultiValueFunction(
            nativeHandle, functionHandle,
            signature, parameters);

    // Validate return values if enabled
    if (config.isReturnTypeValidationEnabled()) {
      validateReturnValues(signature, result);
    }

    return new MultiValueResult(result);
  }

  /**
   * Creates a multi-value host function.
   *
   * @param signature the function signature
   * @param implementation the host function implementation
   * @return the native function handle
   * @throws IllegalArgumentException if any parameter is null
   */
  public long createHostFunction(
      final MultiValueSignature signature, final MultiValueHostFunction implementation) {
    if (signature == null) {
      throw new IllegalArgumentException("Function signature cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Function implementation cannot be null");
    }

    return createNativeHostFunction(nativeHandle, signature, implementation);
  }

  /**
   * Validates function parameters against the signature.
   *
   * @param signature the function signature
   * @param parameters the parameters to validate
   * @throws IllegalArgumentException if validation fails
   */
  private void validateParameters(
      final MultiValueSignature signature, final List<WasmValue> parameters) {
    final List<WasmValueType> expectedTypes = signature.getParameterTypes();

    if (parameters.size() != expectedTypes.size()) {
      throw new IllegalArgumentException(
          "Parameter count mismatch. Expected: "
              + expectedTypes.size()
              + ", Actual: "
              + parameters.size());
    }

    for (int i = 0; i < parameters.size(); i++) {
      final WasmValueType expectedType = expectedTypes.get(i);
      final WasmValueType actualType = parameters.get(i).getType();

      if (!expectedType.equals(actualType)) {
        throw new IllegalArgumentException(
            "Parameter "
                + i
                + " type mismatch. Expected: "
                + expectedType
                + ", Actual: "
                + actualType);
      }
    }
  }

  /**
   * Validates return values against the signature.
   *
   * @param signature the function signature
   * @param returnValues the return values to validate
   * @throws IllegalArgumentException if validation fails
   */
  private void validateReturnValues(
      final MultiValueSignature signature, final List<WasmValue> returnValues) {
    final List<WasmValueType> expectedTypes = signature.getReturnTypes();

    if (!config.isEmptyReturnsAllowed() && returnValues.isEmpty() && !expectedTypes.isEmpty()) {
      throw new IllegalArgumentException(
          "Function returned no values but signature expects return values");
    }

    if (returnValues.size() > config.getMaxReturnValues()) {
      throw new IllegalArgumentException(
          "Too many return values: "
              + returnValues.size()
              + " (max allowed: "
              + config.getMaxReturnValues()
              + ")");
    }

    if (returnValues.size() != expectedTypes.size()) {
      throw new IllegalArgumentException(
          "Return value count mismatch. Expected: "
              + expectedTypes.size()
              + ", Actual: "
              + returnValues.size());
    }

    for (int i = 0; i < returnValues.size(); i++) {
      final WasmValueType expectedType = expectedTypes.get(i);
      final WasmValueType actualType = returnValues.get(i).getType();

      if (!expectedType.equals(actualType)) {
        throw new IllegalArgumentException(
            "Return value "
                + i
                + " type mismatch. Expected: "
                + expectedType
                + ", Actual: "
                + actualType);
      }
    }
  }

  /**
   * Gets the multi-value configuration.
   *
   * @return the configuration
   */
  public MultiValueConfig getConfig() {
    return config;
  }

  /**
   * Gets the native handle for this multi-value function handler.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /** Closes this multi-value function handler and releases native resources. */
  public void close() {
    if (nativeHandle != 0) {
      closeNativeMultiValueHandler(nativeHandle);
    }
  }

  // Native method declarations - implementations in wasmtime4j-native
  private static native long createNativeMultiValueHandler(MultiValueConfig config);

  private static native List<WasmValue> callNativeMultiValueFunction(
      long handlerHandle,
      long functionHandle,
      MultiValueSignature signature,
      List<WasmValue> parameters);

  private static native long createNativeHostFunction(
      long handlerHandle, MultiValueSignature signature, MultiValueHostFunction implementation);

  private static native void closeNativeMultiValueHandler(long handle);

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}
