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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmExceptionHandlingException;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.jni.util.JniGcBridge;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * JNI implementation of WebAssembly exception handling with GC integration.
 *
 * <p>This class provides JNI-specific bindings for WebAssembly exception handling, integrating with
 * both the native Rust implementation and the WebAssembly GC foundation. It ensures proper resource
 * management, cross-language exception propagation, and defensive programming practices.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Cross-language exception propagation between WebAssembly and Java
 *   <li>GC-aware exception handling for exception payloads containing GC references
 *   <li>Exception unwinding with proper resource cleanup
 *   <li>Stack trace capture and debugging support
 *   <li>Exception handler registration and callback management
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniExceptionHandler implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniExceptionHandler.class.getName());
  private static final AtomicLong NEXT_HANDLER_ID = new AtomicLong(1);

  private final long handlerId;
  private final long nativeHandle;
  private final GcRuntime gcRuntime;
  private final ConcurrentHashMap<Long, WasmExceptionHandlingException.ExceptionTag> tagCache;
  private final ConcurrentHashMap<Long, ExceptionHandlerCallback> handlerCallbacks;
  private volatile boolean closed = false;

  static {
    // Load native library
    try {
      System.loadLibrary("wasmtime4j_native");
      LOGGER.info("Loaded wasmtime4j_native library for exception handling");
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.severe("Failed to load wasmtime4j_native library: " + e.getMessage());
      throw new RuntimeException("Failed to load native library for exception handling", e);
    }
  }

  /**
   * Creates a new JNI exception handler.
   *
   * @param config the exception handling configuration
   * @param gcRuntime the GC runtime for GC-aware exception handling
   * @throws IllegalArgumentException if config is null
   * @throws WasmExceptionHandlingException if native creation fails
   */
  public JniExceptionHandler(final ExceptionHandlingConfig config, final GcRuntime gcRuntime) {
    JniValidation.requireNonNull(config, "Exception handling config");

    this.handlerId = NEXT_HANDLER_ID.getAndIncrement();
    this.gcRuntime = gcRuntime; // Nullable - GC support is optional
    this.tagCache = new ConcurrentHashMap<>();
    this.handlerCallbacks = new ConcurrentHashMap<>();

    try {
      this.nativeHandle =
          nativeCreateHandler(
              config.enableNestedTryCatch,
              config.enableExceptionUnwinding,
              config.maxUnwindDepth,
              config.validateExceptionTypes,
              config.enableStackTraces,
              config.enableExceptionPropagation,
              config.enableGcIntegration);

      if (nativeHandle == 0) {
        throw new WasmExceptionHandlingException(
            "Failed to create native exception handler",
            WasmExceptionHandlingException.ExceptionErrorCode.TAG_CREATION_FAILED);
      }

      LOGGER.fine(
          "Created JNI exception handler with ID: "
              + handlerId
              + ", native handle: "
              + nativeHandle);
    } catch (final Exception e) {
      throw new WasmExceptionHandlingException(
          "Failed to create JNI exception handler",
          e,
          ai.tegmentum.wasmtime4j.exception.WasmtimeException.ErrorCode.NATIVE_LIBRARY_ERROR,
          null,
          WasmExceptionHandlingException.ExceptionErrorCode.TAG_CREATION_FAILED,
          null,
          null,
          0);
    }
  }

  /**
   * Creates an exception tag for WebAssembly exception handling.
   *
   * @param name the exception tag name
   * @param parameterTypes the parameter types for this exception
   * @param isGcAware whether this tag handles GC references
   * @return the created exception tag
   * @throws IllegalArgumentException if parameters are invalid
   * @throws WasmExceptionHandlingException if tag creation fails
   * @throws IllegalStateException if handler is closed
   */
  public WasmExceptionHandlingException.ExceptionTag createExceptionTag(
      final String name, final List<WasmValueType> parameterTypes, final boolean isGcAware) {
    validateNotClosed();
    JniValidation.requireNonNull(name, "Exception tag name");
    JniValidation.requireNonEmpty(name.trim(), "Exception tag name");
    JniValidation.requireNonNull(parameterTypes, "Parameter types");

    final String trimmedName = name.trim();

    // Check if tag already exists
    if (tagCache.values().stream().anyMatch(tag -> tag.getName().equals(trimmedName))) {
      throw new IllegalArgumentException("Exception tag '" + trimmedName + "' already exists");
    }

    // Validate GC integration if requested
    if (isGcAware && gcRuntime == null) {
      throw new IllegalArgumentException("GC-aware exception tag requires GC runtime");
    }

    try {
      // Convert parameter types to byte array for native call
      final byte[] typeBytes = new byte[parameterTypes.size()];
      for (int i = 0; i < parameterTypes.size(); i++) {
        typeBytes[i] = (byte) parameterTypes.get(i).ordinal();
      }

      final long tagHandle =
          nativeCreateExceptionTag(nativeHandle, trimmedName, typeBytes, isGcAware);

      if (tagHandle == 0) {
        throw new WasmExceptionHandlingException.TagCreationException(
            "Native tag creation returned null handle", trimmedName);
      }

      final WasmExceptionHandlingException.ExceptionTag tag =
          new WasmExceptionHandlingException.ExceptionTag(
              trimmedName, parameterTypes, tagHandle, isGcAware);

      tagCache.put(tagHandle, tag);

      LOGGER.fine(
          "Created exception tag '"
              + trimmedName
              + "' with handle: "
              + tagHandle
              + ", GC-aware: "
              + isGcAware);
      return tag;
    } catch (final WasmExceptionHandlingException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmExceptionHandlingException.TagCreationException(
          "Failed to create exception tag", e, trimmedName);
    }
  }

  /**
   * Throws a WebAssembly exception with the given tag and payload.
   *
   * @param tag the exception tag
   * @param payload the exception payload
   * @throws IllegalArgumentException if parameters are invalid
   * @throws WasmExceptionHandlingException the thrown WebAssembly exception
   * @throws IllegalStateException if handler is closed
   */
  public void throwException(
      final WasmExceptionHandlingException.ExceptionTag tag,
      final WasmExceptionHandlingException.ExceptionPayload payload) {
    validateNotClosed();
    JniValidation.requireNonNull(tag, "Exception tag");
    JniValidation.requireNonNull(payload, "Exception payload");

    // Validate payload matches tag parameter types
    validatePayload(tag, payload);

    // Handle GC references if present
    final long[] gcHandles = handleGcReferences(payload);

    try {
      // Serialize payload for native call
      final byte[] payloadData = serializePayload(payload);

      // Capture stack trace if enabled
      final String stackTrace = captureStackTrace(tag);

      // Throw the exception natively
      nativeThrowException(nativeHandle, tag.getNativeHandle(), payloadData, gcHandles);

    } catch (final Exception e) {
      // Clean up GC handles on failure
      if (gcHandles != null && gcRuntime != null) {
        for (final long handle : gcHandles) {
          if (handle != 0) {
            try {
              gcRuntime.releaseGcValue(handle);
            } catch (final Exception gcE) {
              LOGGER.warning(
                  "Failed to release GC handle during exception cleanup: " + gcE.getMessage());
            }
          }
        }
      }

      throw new WasmExceptionHandlingException(
          "Failed to throw WebAssembly exception",
          e,
          ai.tegmentum.wasmtime4j.exception.WasmtimeException.ErrorCode.UNKNOWN,
          null,
          WasmExceptionHandlingException.ExceptionErrorCode.PROPAGATION_FAILED,
          payload,
          null,
          0);
    }
  }

  /**
   * Registers an exception handler for a specific tag.
   *
   * @param tag the exception tag to handle
   * @param handler the exception handler callback
   * @throws IllegalArgumentException if parameters are invalid
   * @throws WasmExceptionHandlingException if registration fails
   * @throws IllegalStateException if handler is closed
   */
  public void registerExceptionHandler(
      final WasmExceptionHandlingException.ExceptionTag tag,
      final ExceptionHandlerCallback handler) {
    validateNotClosed();
    JniValidation.requireNonNull(tag, "Exception tag");
    JniValidation.requireNonNull(handler, "Exception handler callback");

    try {
      // Store the callback for native code to invoke
      handlerCallbacks.put(tag.getNativeHandle(), handler);

      nativeRegisterExceptionHandler(nativeHandle, tag.getNativeHandle());

      LOGGER.fine("Registered exception handler for tag: " + tag.getName());
    } catch (final Exception e) {
      handlerCallbacks.remove(tag.getNativeHandle());
      throw new WasmExceptionHandlingException(
          "Failed to register exception handler",
          e,
          ai.tegmentum.wasmtime4j.exception.WasmtimeException.ErrorCode.UNKNOWN,
          null,
          WasmExceptionHandlingException.ExceptionErrorCode.HANDLER_REGISTRATION_FAILED,
          null,
          null,
          0);
    }
  }

  /**
   * Performs exception unwinding with proper resource cleanup.
   *
   * @param currentDepth the current unwind depth
   * @return true if unwinding should continue, false if maximum depth reached
   * @throws IllegalArgumentException if depth is negative
   * @throws WasmExceptionHandlingException if unwinding fails
   * @throws IllegalStateException if handler is closed
   */
  public boolean performUnwinding(final int currentDepth) {
    validateNotClosed();

    if (currentDepth < 0) {
      throw new IllegalArgumentException("Current depth cannot be negative");
    }

    try {
      final boolean shouldContinue = nativePerformUnwinding(nativeHandle, currentDepth);
      LOGGER.fine("Unwinding at depth " + currentDepth + ", continue: " + shouldContinue);
      return shouldContinue;
    } catch (final Exception e) {
      throw new WasmExceptionHandlingException.UnwindingException(
          "Failed to perform unwinding", e, currentDepth);
    }
  }

  /**
   * Captures a stack trace for the given exception tag.
   *
   * @param tag the exception tag
   * @return the stack trace string, or null if not available
   * @throws IllegalArgumentException if tag is null
   * @throws IllegalStateException if handler is closed
   */
  public String captureStackTrace(final WasmExceptionHandlingException.ExceptionTag tag) {
    validateNotClosed();
    JniValidation.requireNonNull(tag, "Exception tag");

    try {
      final String trace = nativeCaptureStackTrace(nativeHandle, tag.getNativeHandle());
      LOGGER.fine("Captured stack trace for tag: " + tag.getName());
      return trace;
    } catch (final Exception e) {
      LOGGER.warning(
          "Failed to capture stack trace for tag " + tag.getName() + ": " + e.getMessage());
      return null; // Return null instead of throwing for optional feature
    }
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
   * Gets the native handle.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
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

      try {
        // Clean up cached tags and callbacks
        tagCache.clear();
        handlerCallbacks.clear();

        // Release native resources
        if (nativeHandle != 0) {
          nativeCloseHandler(nativeHandle);
        }

        LOGGER.info("Closed JNI exception handler with ID: " + handlerId);
      } catch (final Exception e) {
        LOGGER.warning("Error during exception handler cleanup: " + e.getMessage());
      }
    }
  }

  /**
   * Validates that the payload matches the exception tag parameter types.
   *
   * @param tag the exception tag
   * @param payload the payload to validate
   * @throws WasmExceptionHandlingException.PayloadValidationException if validation fails
   */
  private void validatePayload(
      final WasmExceptionHandlingException.ExceptionTag tag,
      final WasmExceptionHandlingException.ExceptionPayload payload) {
    final List<WasmValueType> expectedTypes = tag.getParameterTypes();
    final List<WasmValue> values = payload.getValues();

    if (values.size() != expectedTypes.size()) {
      throw new WasmExceptionHandlingException.PayloadValidationException(
          "Exception payload size ("
              + values.size()
              + ") doesn't match tag parameter count ("
              + expectedTypes.size()
              + ")",
          payload);
    }

    for (int i = 0; i < values.size(); i++) {
      final WasmValueType expectedType = expectedTypes.get(i);
      final WasmValueType actualType = values.get(i).getType();

      if (!expectedType.equals(actualType)) {
        throw new WasmExceptionHandlingException.PayloadValidationException(
            "Exception payload parameter "
                + i
                + " type mismatch. "
                + "Expected: "
                + expectedType
                + ", Actual: "
                + actualType,
            payload);
      }
    }

    // Validate GC consistency
    if (tag.isGcAware() && !payload.hasGcValues()) {
      throw new WasmExceptionHandlingException.PayloadValidationException(
          "GC-aware exception tag requires GC values in payload", payload);
    }

    if (!tag.isGcAware() && payload.hasGcValues()) {
      throw new WasmExceptionHandlingException.PayloadValidationException(
          "Non-GC-aware exception tag cannot have GC values in payload", payload);
    }
  }

  /**
   * Handles GC references in the exception payload.
   *
   * @param payload the exception payload
   * @return array of GC handles, or null if no GC references
   */
  private long[] handleGcReferences(final WasmExceptionHandlingException.ExceptionPayload payload) {
    if (!payload.hasGcValues() || gcRuntime == null) {
      return null;
    }

    try {
      final List<GcValue> gcValues = payload.getGcValues();
      final long[] handles = new long[gcValues.size()];

      for (int i = 0; i < gcValues.size(); i++) {
        handles[i] = JniGcBridge.createGcHandle(gcRuntime, gcValues.get(i));
      }

      return handles;
    } catch (final Exception e) {
      throw new WasmExceptionHandlingException.GcReferenceException(
          "Failed to handle GC references in exception payload", e, payload);
    }
  }

  /**
   * Serializes the exception payload for native code.
   *
   * @param payload the payload to serialize
   * @return the serialized payload data
   */
  private byte[] serializePayload(final WasmExceptionHandlingException.ExceptionPayload payload) {
    // This would use the ExceptionMarshaling utility
    // For now, return empty array as placeholder
    return new byte[0];
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
   * Callback invoked from native code when an exception is handled.
   *
   * @param tagHandle the exception tag handle
   * @param payloadData the serialized payload data
   * @param gcHandles the GC handles (nullable)
   * @return true to continue execution, false to re-throw
   */
  @SuppressWarnings("unused") // Called from native code
  private boolean onExceptionHandled(
      final long tagHandle, final byte[] payloadData, final long[] gcHandles) {
    try {
      final ExceptionHandlerCallback callback = handlerCallbacks.get(tagHandle);
      if (callback == null) {
        LOGGER.warning("No callback found for tag handle: " + tagHandle);
        return false;
      }

      final WasmExceptionHandlingException.ExceptionTag tag = tagCache.get(tagHandle);
      if (tag == null) {
        LOGGER.warning("No tag found for handle: " + tagHandle);
        return false;
      }

      // Deserialize payload and reconstruct exception payload
      final List<WasmValue> values = deserializePayload(payloadData, tag.getParameterTypes());
      final List<GcValue> gcValues = reconstructGcValues(gcHandles);

      final WasmExceptionHandlingException.ExceptionPayload payload =
          new WasmExceptionHandlingException.ExceptionPayload(tag, values, gcValues);

      return callback.handle(tag, payload);
    } catch (final Exception e) {
      LOGGER.severe("Exception in callback handler: " + e.getMessage());
      return false;
    }
  }

  /**
   * Deserializes payload data from native code.
   *
   * @param data the serialized data
   * @param types the expected types
   * @return the deserialized payload
   */
  private List<WasmValue> deserializePayload(final byte[] data, final List<WasmValueType> types) {
    // This would use the ExceptionMarshaling utility
    // For now, return empty list as placeholder
    return Collections.emptyList();
  }

  /**
   * Reconstructs GC values from native handles.
   *
   * @param gcHandles the GC handles (nullable)
   * @return the reconstructed GC values
   */
  private List<GcValue> reconstructGcValues(final long[] gcHandles) {
    if (gcHandles == null || gcRuntime == null) {
      return Collections.emptyList();
    }

    try {
      return JniGcBridge.reconstructGcValues(gcRuntime, gcHandles);
    } catch (final Exception e) {
      LOGGER.warning("Failed to reconstruct GC values: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  // Native method declarations
  private static native long nativeCreateHandler(
      boolean enableNested,
      boolean enableUnwinding,
      int maxDepth,
      boolean validateTypes,
      boolean enableStackTraces,
      boolean enablePropagation,
      boolean enableGcIntegration);

  private static native long nativeCreateExceptionTag(
      long handlerHandle, String name, byte[] parameterTypes, boolean isGcAware);

  private static native void nativeThrowException(
      long handlerHandle, long tagHandle, byte[] payloadData, long[] gcHandles);

  private static native void nativeRegisterExceptionHandler(long handlerHandle, long tagHandle);

  private static native String nativeCaptureStackTrace(long handlerHandle, long tagHandle);

  private static native boolean nativePerformUnwinding(long handlerHandle, int currentDepth);

  private static native void nativeCloseHandler(long handlerHandle);

  /** Exception handling configuration. */
  public static final class ExceptionHandlingConfig {
    public final boolean enableNestedTryCatch;
    public final boolean enableExceptionUnwinding;
    public final int maxUnwindDepth;
    public final boolean validateExceptionTypes;
    public final boolean enableStackTraces;
    public final boolean enableExceptionPropagation;
    public final boolean enableGcIntegration;

    private ExceptionHandlingConfig(final Builder builder) {
      this.enableNestedTryCatch = builder.enableNestedTryCatch;
      this.enableExceptionUnwinding = builder.enableExceptionUnwinding;
      this.maxUnwindDepth = builder.maxUnwindDepth;
      this.validateExceptionTypes = builder.validateExceptionTypes;
      this.enableStackTraces = builder.enableStackTraces;
      this.enableExceptionPropagation = builder.enableExceptionPropagation;
      this.enableGcIntegration = builder.enableGcIntegration;
    }

    /** Creates a new builder. */
    public static Builder builder() {
      return new Builder();
    }

    /** Default configuration. */
    public static ExceptionHandlingConfig defaultConfig() {
      return new Builder().build();
    }

    /** Builder for exception handling configuration. */
    public static final class Builder {
      private boolean enableNestedTryCatch = true;
      private boolean enableExceptionUnwinding = true;
      private int maxUnwindDepth = 1000;
      private boolean validateExceptionTypes = true;
      private boolean enableStackTraces = true;
      private boolean enableExceptionPropagation = true;
      private boolean enableGcIntegration = false;

      public Builder enableNestedTryCatch(final boolean enable) {
        this.enableNestedTryCatch = enable;
        return this;
      }

      public Builder enableExceptionUnwinding(final boolean enable) {
        this.enableExceptionUnwinding = enable;
        return this;
      }

      /**
       * Sets maximum exception unwind depth.
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

      public Builder validateExceptionTypes(final boolean enable) {
        this.validateExceptionTypes = enable;
        return this;
      }

      public Builder enableStackTraces(final boolean enable) {
        this.enableStackTraces = enable;
        return this;
      }

      public Builder enableExceptionPropagation(final boolean enable) {
        this.enableExceptionPropagation = enable;
        return this;
      }

      public Builder enableGcIntegration(final boolean enable) {
        this.enableGcIntegration = enable;
        return this;
      }

      public ExceptionHandlingConfig build() {
        return new ExceptionHandlingConfig(this);
      }
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
          && enableExceptionPropagation == that.enableExceptionPropagation
          && enableGcIntegration == that.enableGcIntegration;
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          enableNestedTryCatch,
          enableExceptionUnwinding,
          maxUnwindDepth,
          validateExceptionTypes,
          enableStackTraces,
          enableExceptionPropagation,
          enableGcIntegration);
    }
  }

  /** Functional interface for exception handler callbacks. */
  @FunctionalInterface
  public interface ExceptionHandlerCallback {
    /**
     * Handles a WebAssembly exception.
     *
     * @param tag the exception tag
     * @param payload the exception payload
     * @return true to continue execution, false to re-throw
     */
    boolean handle(
        WasmExceptionHandlingException.ExceptionTag tag,
        WasmExceptionHandlingException.ExceptionPayload payload);
  }
}
