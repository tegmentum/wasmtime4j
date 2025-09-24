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
 * Handler for WebAssembly exception handling proposal.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API is experimental and subject to change. It implements
 * the WebAssembly exception handling proposal for try/catch blocks and exception throwing within
 * WebAssembly modules.
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING)
public final class ExceptionHandler {

  /** WebAssembly exception tag definition. */
  public static final class ExceptionTag {
    private final String name;
    private final List<WasmValueType> parameterTypes;
    private final long nativeHandle;

    /**
     * Creates a new exception tag.
     *
     * @param name the tag name
     * @param parameterTypes the parameter types for this exception
     * @param nativeHandle the native handle
     * @throws IllegalArgumentException if name is null or empty, or parameterTypes is null
     */
    public ExceptionTag(
        final String name, final List<WasmValueType> parameterTypes, final long nativeHandle) {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Exception tag name cannot be null or empty");
      }
      if (parameterTypes == null) {
        throw new IllegalArgumentException("Parameter types cannot be null");
      }

      this.name = name.trim();
      this.parameterTypes = List.copyOf(parameterTypes);
      this.nativeHandle = nativeHandle;
    }

    /**
     * Gets the exception tag name.
     *
     * @return the tag name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the parameter types for this exception.
     *
     * @return immutable list of parameter types
     */
    public List<WasmValueType> getParameterTypes() {
      return parameterTypes;
    }

    /**
     * Gets the native handle for this exception tag.
     *
     * @return the native handle
     */
    public long getNativeHandle() {
      return nativeHandle;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final ExceptionTag that = (ExceptionTag) obj;
      return nativeHandle == that.nativeHandle
          && Objects.equals(name, that.name)
          && Objects.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, parameterTypes, nativeHandle);
    }

    @Override
    public String toString() {
      return "ExceptionTag{"
          + "name='"
          + name
          + '\''
          + ", parameterTypes="
          + parameterTypes
          + ", nativeHandle="
          + nativeHandle
          + '}';
    }
  }

  /** WebAssembly exception instance. */
  public static final class WasmException extends RuntimeException {
    private final ExceptionTag tag;
    private final List<WasmValue> payload;

    /**
     * Creates a new WebAssembly exception.
     *
     * @param tag the exception tag
     * @param payload the exception payload values
     * @param message the exception message
     * @throws IllegalArgumentException if tag or payload is null
     */
    public WasmException(
        final ExceptionTag tag, final List<WasmValue> payload, final String message) {
      super(message);
      if (tag == null) {
        throw new IllegalArgumentException("Exception tag cannot be null");
      }
      if (payload == null) {
        throw new IllegalArgumentException("Exception payload cannot be null");
      }

      this.tag = tag;
      this.payload = List.copyOf(payload);
    }

    /**
     * Gets the exception tag.
     *
     * @return the exception tag
     */
    public ExceptionTag getTag() {
      return tag;
    }

    /**
     * Gets the exception payload values.
     *
     * @return immutable list of payload values
     */
    public List<WasmValue> getPayload() {
      return payload;
    }
  }

  /** Exception handling configuration. */
  public static final class ExceptionHandlingConfig {
    private final boolean enableNestedTryCatch;
    private final boolean enableExceptionUnwinding;
    private final int maxUnwindDepth;
    private final boolean validateExceptionTypes;

    private ExceptionHandlingConfig(final Builder builder) {
      this.enableNestedTryCatch = builder.enableNestedTryCatch;
      this.enableExceptionUnwinding = builder.enableExceptionUnwinding;
      this.maxUnwindDepth = builder.maxUnwindDepth;
      this.validateExceptionTypes = builder.validateExceptionTypes;
    }

    /**
     * Checks if nested try/catch blocks are enabled.
     *
     * @return true if nested try/catch is enabled
     */
    public boolean isNestedTryCatchEnabled() {
      return enableNestedTryCatch;
    }

    /**
     * Checks if exception unwinding is enabled.
     *
     * @return true if exception unwinding is enabled
     */
    public boolean isExceptionUnwindingEnabled() {
      return enableExceptionUnwinding;
    }

    /**
     * Gets the maximum unwind depth.
     *
     * @return the maximum unwind depth
     */
    public int getMaxUnwindDepth() {
      return maxUnwindDepth;
    }

    /**
     * Checks if exception type validation is enabled.
     *
     * @return true if exception type validation is enabled
     */
    public boolean isExceptionTypeValidationEnabled() {
      return validateExceptionTypes;
    }

    /**
     * Creates a new builder for exception handling configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
      return new Builder();
    }

    /** Builder for exception handling configuration. */
    public static final class Builder {
      private boolean enableNestedTryCatch = true;
      private boolean enableExceptionUnwinding = true;
      private int maxUnwindDepth = 1000;
      private boolean validateExceptionTypes = true;

      private Builder() {}

      /**
       * Enables or disables nested try/catch blocks.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableNestedTryCatch(final boolean enable) {
        this.enableNestedTryCatch = enable;
        return this;
      }

      /**
       * Enables or disables exception unwinding.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableExceptionUnwinding(final boolean enable) {
        this.enableExceptionUnwinding = enable;
        return this;
      }

      /**
       * Sets the maximum unwind depth.
       *
       * @param depth the maximum unwind depth (must be positive)
       * @return this builder
       * @throws IllegalArgumentException if depth is not positive
       */
      public Builder maxUnwindDepth(final int depth) {
        if (depth <= 0) {
          throw new IllegalArgumentException("Max unwind depth must be positive");
        }
        this.maxUnwindDepth = depth;
        return this;
      }

      /**
       * Enables or disables exception type validation.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder validateExceptionTypes(final boolean enable) {
        this.validateExceptionTypes = enable;
        return this;
      }

      /**
       * Builds the exception handling configuration.
       *
       * @return the configuration
       */
      public ExceptionHandlingConfig build() {
        return new ExceptionHandlingConfig(this);
      }
    }
  }

  private final ExceptionHandlingConfig config;
  private final long nativeHandle;

  /**
   * Creates a new exception handler.
   *
   * @param config the exception handling configuration
   * @throws IllegalArgumentException if config is null
   * @throws UnsupportedOperationException if exception handling feature is not enabled
   */
  public ExceptionHandler(final ExceptionHandlingConfig config) {
    ExperimentalFeatures.validateFeatureSupport(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);

    if (config == null) {
      throw new IllegalArgumentException("Exception handling config cannot be null");
    }

    this.config = config;
    this.nativeHandle = createNativeHandler(config);
  }

  /**
   * Creates an exception tag for use in WebAssembly modules.
   *
   * @param name the tag name
   * @param parameterTypes the parameter types for this exception
   * @return the created exception tag
   * @throws IllegalArgumentException if name is null or empty, or parameterTypes is null
   * @throws RuntimeException if tag creation fails
   */
  public ExceptionTag createExceptionTag(
      final String name, final List<WasmValueType> parameterTypes) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Exception tag name cannot be null or empty");
    }
    if (parameterTypes == null) {
      throw new IllegalArgumentException("Parameter types cannot be null");
    }

    final long tagHandle = createNativeExceptionTag(nativeHandle, name.trim(), parameterTypes);
    return new ExceptionTag(name.trim(), parameterTypes, tagHandle);
  }

  /**
   * Throws a WebAssembly exception with the given tag and payload.
   *
   * @param tag the exception tag
   * @param payload the exception payload values
   * @throws IllegalArgumentException if tag or payload is null
   * @throws WasmException the thrown WebAssembly exception
   */
  public void throwException(final ExceptionTag tag, final List<WasmValue> payload) {
    if (tag == null) {
      throw new IllegalArgumentException("Exception tag cannot be null");
    }
    if (payload == null) {
      throw new IllegalArgumentException("Exception payload cannot be null");
    }

    // Validate payload matches tag parameter types
    if (config.isExceptionTypeValidationEnabled()) {
      validatePayload(tag, payload);
    }

    final String message = "WebAssembly exception: " + tag.getName();
    throw new WasmException(tag, payload, message);
  }

  /**
   * Validates that the payload matches the exception tag parameter types.
   *
   * @param tag the exception tag
   * @param payload the payload values
   * @throws IllegalArgumentException if payload doesn't match tag types
   */
  private void validatePayload(final ExceptionTag tag, final List<WasmValue> payload) {
    final List<WasmValueType> expectedTypes = tag.getParameterTypes();

    if (payload.size() != expectedTypes.size()) {
      throw new IllegalArgumentException(
          "Exception payload size ("
              + payload.size()
              + ") doesn't match tag parameter count ("
              + expectedTypes.size()
              + ")");
    }

    for (int i = 0; i < payload.size(); i++) {
      final WasmValueType expectedType = expectedTypes.get(i);
      final WasmValueType actualType = payload.get(i).getType();

      if (!expectedType.equals(actualType)) {
        throw new IllegalArgumentException(
            "Exception payload parameter "
                + i
                + " type mismatch. "
                + "Expected: "
                + expectedType
                + ", Actual: "
                + actualType);
      }
    }
  }

  /**
   * Gets the exception handling configuration.
   *
   * @return the configuration
   */
  public ExceptionHandlingConfig getConfig() {
    return config;
  }

  /**
   * Gets the native handle for this exception handler.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /** Closes this exception handler and releases native resources. */
  public void close() {
    if (nativeHandle != 0) {
      closeNativeHandler(nativeHandle);
    }
  }

  // Native method declarations - implementations in wasmtime4j-native
  private static native long createNativeHandler(ExceptionHandlingConfig config);

  private static native long createNativeExceptionTag(
      long handlerHandle, String name, List<WasmValueType> parameterTypes);

  private static native void closeNativeHandler(long handle);

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}
