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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Handler for WebAssembly exception handling proposal.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API is experimental and subject to change. It implements
 * the WebAssembly exception handling proposal for try/catch blocks and exception throwing within
 * WebAssembly modules.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Exception tag creation and management
 *   <li>Exception throwing and catching
 *   <li>Payload marshaling and validation
 *   <li>Stack unwinding and cleanup
 *   <li>Exception propagation between WebAssembly and host
 * </ul>
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING)
public final class ExceptionHandler implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(ExceptionHandler.class.getName());
  private static final AtomicLong NEXT_HANDLER_ID = new AtomicLong(1);

  /** WebAssembly exception tag definition. */
  public static final class ExceptionTag {
    private final String name;
    private final List<WasmValueType> parameterTypes;
    private final long nativeHandle;
    private final long handlerId;

    /**
     * Creates a new exception tag.
     *
     * @param name the tag name
     * @param parameterTypes the parameter types for this exception
     * @param nativeHandle the native handle
     * @param handlerId the handler ID that created this tag
     * @throws IllegalArgumentException if name is null or empty, or parameterTypes is null
     */
    public ExceptionTag(
        final String name,
        final List<WasmValueType> parameterTypes,
        final long nativeHandle,
        final long handlerId) {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Exception tag name cannot be null or empty");
      }
      if (parameterTypes == null) {
        throw new IllegalArgumentException("Parameter types cannot be null");
      }

      this.name = name.trim();
      this.parameterTypes = List.copyOf(parameterTypes);
      this.nativeHandle = nativeHandle;
      this.handlerId = handlerId;
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

    /**
     * Gets the handler ID that created this tag.
     *
     * @return the handler ID
     */
    public long getHandlerId() {
      return handlerId;
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
          && handlerId == that.handlerId
          && Objects.equals(name, that.name)
          && Objects.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, parameterTypes, nativeHandle, handlerId);
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
          + ", handlerId="
          + handlerId
          + '}';
    }
  }

  /** WebAssembly exception instance. */
  public static final class WasmException extends RuntimeException {
    private final ExceptionTag tag;
    private final List<WasmValue> payload;
    private final String wasmStackTrace;

    /**
     * Creates a new WebAssembly exception.
     *
     * @param tag the exception tag
     * @param payload the exception payload values
     * @param message the exception message
     * @param wasmStackTrace the WebAssembly stack trace
     * @throws IllegalArgumentException if tag or payload is null
     */
    public WasmException(
        final ExceptionTag tag,
        final List<WasmValue> payload,
        final String message,
        final String wasmStackTrace) {
      super(message);
      if (tag == null) {
        throw new IllegalArgumentException("Exception tag cannot be null");
      }
      if (payload == null) {
        throw new IllegalArgumentException("Exception payload cannot be null");
      }

      this.tag = tag;
      this.payload = List.copyOf(payload);
      this.wasmStackTrace = wasmStackTrace;
    }

    /**
     * Creates a new WebAssembly exception without stack trace.
     *
     * @param tag the exception tag
     * @param payload the exception payload values
     * @param message the exception message
     * @throws IllegalArgumentException if tag or payload is null
     */
    public WasmException(
        final ExceptionTag tag, final List<WasmValue> payload, final String message) {
      this(tag, payload, message, null);
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

    /**
     * Gets the WebAssembly stack trace.
     *
     * @return the WebAssembly stack trace, or null if not available
     */
    public String getWasmStackTrace() {
      return wasmStackTrace;
    }
  }

  /** Exception handling configuration. */
  public static final class ExceptionHandlingConfig {
    private final boolean enableNestedTryCatch;
    private final boolean enableExceptionUnwinding;
    private final int maxUnwindDepth;
    private final boolean validateExceptionTypes;
    private final boolean enableStackTraces;
    private final boolean enableExceptionPropagation;

    private ExceptionHandlingConfig(final Builder builder) {
      this.enableNestedTryCatch = builder.enableNestedTryCatch;
      this.enableExceptionUnwinding = builder.enableExceptionUnwinding;
      this.maxUnwindDepth = builder.maxUnwindDepth;
      this.validateExceptionTypes = builder.validateExceptionTypes;
      this.enableStackTraces = builder.enableStackTraces;
      this.enableExceptionPropagation = builder.enableExceptionPropagation;
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
     * Checks if stack traces are enabled.
     *
     * @return true if stack traces are enabled
     */
    public boolean isStackTracesEnabled() {
      return enableStackTraces;
    }

    /**
     * Checks if exception propagation between WebAssembly and host is enabled.
     *
     * @return true if exception propagation is enabled
     */
    public boolean isExceptionPropagationEnabled() {
      return enableExceptionPropagation;
    }

    /**
     * Creates a new builder for exception handling configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
      return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final ExceptionHandlingConfig that = (ExceptionHandlingConfig) obj;
      return enableNestedTryCatch == that.enableNestedTryCatch
          && enableExceptionUnwinding == that.enableExceptionUnwinding
          && maxUnwindDepth == that.maxUnwindDepth
          && validateExceptionTypes == that.validateExceptionTypes
          && enableStackTraces == that.enableStackTraces
          && enableExceptionPropagation == that.enableExceptionPropagation;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          enableNestedTryCatch,
          enableExceptionUnwinding,
          maxUnwindDepth,
          validateExceptionTypes,
          enableStackTraces,
          enableExceptionPropagation);
    }

    /** Builder for exception handling configuration. */
    public static final class Builder {
      private boolean enableNestedTryCatch = true;
      private boolean enableExceptionUnwinding = true;
      private int maxUnwindDepth = 1000;
      private boolean validateExceptionTypes = true;
      private boolean enableStackTraces = true;
      private boolean enableExceptionPropagation = true;

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
       * Enables or disables stack traces for exceptions.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableStackTraces(final boolean enable) {
        this.enableStackTraces = enable;
        return this;
      }

      /**
       * Enables or disables exception propagation between WebAssembly and host.
       *
       * @param enable true to enable, false to disable
       * @return this builder
       */
      public Builder enableExceptionPropagation(final boolean enable) {
        this.enableExceptionPropagation = enable;
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
  private final long handlerId;
  private final ConcurrentHashMap<String, ExceptionTag> tags;
  private volatile boolean closed = false;

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
    this.handlerId = NEXT_HANDLER_ID.getAndIncrement();
    this.tags = new ConcurrentHashMap<>();
    this.nativeHandle = createNativeHandler(config);

    LOGGER.info("Created exception handler with ID: " + handlerId);
  }

  /**
   * Creates an exception tag for use in WebAssembly modules.
   *
   * @param name the tag name
   * @param parameterTypes the parameter types for this exception
   * @return the created exception tag
   * @throws IllegalArgumentException if name is null or empty, or parameterTypes is null
   * @throws RuntimeException if tag creation fails
   * @throws IllegalStateException if handler is closed
   */
  public ExceptionTag createExceptionTag(
      final String name, final List<WasmValueType> parameterTypes) {
    validateNotClosed();

    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Exception tag name cannot be null or empty");
    }
    if (parameterTypes == null) {
      throw new IllegalArgumentException("Parameter types cannot be null");
    }

    final String trimmedName = name.trim();

    // Check if tag already exists
    final ExceptionTag existingTag = tags.get(trimmedName);
    if (existingTag != null) {
      throw new IllegalArgumentException("Exception tag '" + trimmedName + "' already exists");
    }

    final long tagHandle = createNativeExceptionTag(nativeHandle, trimmedName, parameterTypes);
    final ExceptionTag tag = new ExceptionTag(trimmedName, parameterTypes, tagHandle, handlerId);

    tags.put(trimmedName, tag);

    LOGGER.fine("Created exception tag: " + trimmedName + " with handle: " + tagHandle);
    return tag;
  }

  /**
   * Gets an existing exception tag by name.
   *
   * @param name the tag name
   * @return the exception tag, or null if not found
   * @throws IllegalArgumentException if name is null or empty
   * @throws IllegalStateException if handler is closed
   */
  public ExceptionTag getExceptionTag(final String name) {
    validateNotClosed();

    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Exception tag name cannot be null or empty");
    }

    return tags.get(name.trim());
  }

  /**
   * Lists all exception tags created by this handler.
   *
   * @return immutable list of exception tags
   * @throws IllegalStateException if handler is closed
   */
  public List<ExceptionTag> listExceptionTags() {
    validateNotClosed();
    return List.copyOf(tags.values());
  }

  /**
   * Throws a WebAssembly exception with the given tag and payload.
   *
   * @param tag the exception tag
   * @param payload the exception payload values
   * @throws IllegalArgumentException if tag or payload is null
   * @throws WasmException the thrown WebAssembly exception
   * @throws IllegalStateException if handler is closed
   */
  public void throwException(final ExceptionTag tag, final List<WasmValue> payload) {
    validateNotClosed();

    if (tag == null) {
      throw new IllegalArgumentException("Exception tag cannot be null");
    }
    if (payload == null) {
      throw new IllegalArgumentException("Exception payload cannot be null");
    }

    // Validate tag belongs to this handler
    if (tag.getHandlerId() != handlerId) {
      throw new IllegalArgumentException("Exception tag does not belong to this handler");
    }

    // Validate payload matches tag parameter types
    if (config.isExceptionTypeValidationEnabled()) {
      validatePayload(tag, payload);
    }

    final String stackTrace =
        config.isStackTracesEnabled()
            ? captureStackTrace(nativeHandle, tag.getNativeHandle())
            : null;

    final String message = "WebAssembly exception: " + tag.getName();
    throw new WasmException(tag, payload, message, stackTrace);
  }

  /**
   * Catches a WebAssembly exception and returns its payload.
   *
   * @param exception the WebAssembly exception to catch
   * @param expectedTag the expected exception tag
   * @return the exception payload if tag matches
   * @throws IllegalArgumentException if exception or tag is null
   * @throws IllegalStateException if tag doesn't match
   */
  public List<WasmValue> catchException(
      final WasmException exception, final ExceptionTag expectedTag) {
    validateNotClosed();

    if (exception == null) {
      throw new IllegalArgumentException("Exception cannot be null");
    }
    if (expectedTag == null) {
      throw new IllegalArgumentException("Expected tag cannot be null");
    }

    if (!exception.getTag().equals(expectedTag)) {
      throw new IllegalStateException("Exception tag does not match expected tag");
    }

    return exception.getPayload();
  }

  /**
   * Registers an exception handler for a specific tag.
   *
   * @param tag the exception tag to handle
   * @param handler the exception handler function
   * @throws IllegalArgumentException if tag or handler is null
   * @throws IllegalStateException if handler is closed
   */
  public void registerExceptionHandler(
      final ExceptionTag tag, final ExceptionHandlerFunction handler) {
    validateNotClosed();

    if (tag == null) {
      throw new IllegalArgumentException("Exception tag cannot be null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("Exception handler function cannot be null");
    }

    // Validate tag belongs to this handler
    if (tag.getHandlerId() != handlerId) {
      throw new IllegalArgumentException("Exception tag does not belong to this handler");
    }

    registerNativeExceptionHandler(nativeHandle, tag.getNativeHandle(), handler);

    LOGGER.fine("Registered exception handler for tag: " + tag.getName());
  }

  /**
   * Performs exception unwinding with cleanup.
   *
   * @param currentDepth the current unwind depth
   * @return true if unwinding should continue, false if maximum depth reached
   * @throws IllegalStateException if handler is closed
   */
  public boolean performUnwinding(final int currentDepth) {
    validateNotClosed();

    if (currentDepth >= config.getMaxUnwindDepth()) {
      LOGGER.warning("Maximum unwind depth reached: " + currentDepth);
      return false;
    }

    return performNativeUnwinding(nativeHandle, currentDepth);
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
   * Validates that the handler is not closed.
   *
   * @throws IllegalStateException if handler is closed
   */
  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Exception handler is closed");
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

  /**
   * Gets the handler ID.
   *
   * @return the handler ID
   */
  public long getHandlerId() {
    return handlerId;
  }

  /**
   * Checks if this handler is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  /** Closes this exception handler and releases native resources. */
  @Override
  public void close() {
    if (!closed) {
      closed = true;
      tags.clear();

      if (nativeHandle != 0) {
        closeNativeHandler(nativeHandle);
      }

      LOGGER.info("Closed exception handler with ID: " + handlerId);
    }
  }

  // Native method declarations - implementations in wasmtime4j-native
  private static native long createNativeHandler(ExceptionHandlingConfig config);

  private static native long createNativeExceptionTag(
      long handlerHandle, String name, List<WasmValueType> parameterTypes);

  private static native void registerNativeExceptionHandler(
      long handlerHandle, long tagHandle, ExceptionHandlerFunction handler);

  private static native String captureStackTrace(long handlerHandle, long tagHandle);

  private static native boolean performNativeUnwinding(long handlerHandle, int currentDepth);

  private static native void closeNativeHandler(long handle);

  @Override
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    }
  }

  /** Functional interface for exception handler callbacks. */
  @FunctionalInterface
  public interface ExceptionHandlerFunction {
    /**
     * Handles a WebAssembly exception.
     *
     * @param tag the exception tag
     * @param payload the exception payload
     * @return true to continue execution, false to re-throw
     */
    boolean handle(ExceptionTag tag, List<WasmValue> payload);
  }
}
