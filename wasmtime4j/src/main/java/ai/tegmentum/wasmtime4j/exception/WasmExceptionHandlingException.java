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

package ai.tegmentum.wasmtime4j.exception;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import java.util.List;
import java.util.Objects;

/**
 * Exception for WebAssembly exception handling operations.
 *
 * <p>This exception represents errors in WebAssembly exception handling operations including
 * exception tag creation, exception throwing/catching, and exception propagation. Provides
 * integration with WebAssembly GC for exception payloads containing GC references.
 *
 * <p>Key features:
 * <ul>
 *   <li>Exception tag information and payload validation
 *   <li>WebAssembly stack trace capture and propagation
 *   <li>Cross-language exception mapping between WebAssembly and Java
 *   <li>GC-aware exception handling for GC reference payloads
 *   <li>Exception unwinding context and debugging support
 * </ul>
 *
 * @since 1.0.0
 */
public class WasmExceptionHandlingException extends WasmtimeException {

  /** Exception tag information. */
  public static final class ExceptionTag {
    private final String name;
    private final List<WasmValueType> parameterTypes;
    private final long nativeHandle;
    private final boolean isGcAware;

    /**
     * Creates a new exception tag.
     *
     * @param name the exception tag name
     * @param parameterTypes the parameter types for this exception
     * @param nativeHandle the native handle for this tag
     * @param isGcAware whether this tag handles GC references
     */
    public ExceptionTag(
        final String name,
        final List<WasmValueType> parameterTypes,
        final long nativeHandle,
        final boolean isGcAware) {
      this.name = Objects.requireNonNull(name, "Exception tag name cannot be null");
      this.parameterTypes = List.copyOf(Objects.requireNonNull(parameterTypes,
          "Parameter types cannot be null"));
      this.nativeHandle = nativeHandle;
      this.isGcAware = isGcAware;
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
     * Checks if this tag is GC-aware.
     *
     * @return true if this tag handles GC references
     */
    public boolean isGcAware() {
      return isGcAware;
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
          && isGcAware == that.isGcAware
          && Objects.equals(name, that.name)
          && Objects.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, parameterTypes, nativeHandle, isGcAware);
    }

    @Override
    public String toString() {
      return "ExceptionTag{"
          + "name='" + name + '\''
          + ", parameterTypes=" + parameterTypes
          + ", nativeHandle=" + nativeHandle
          + ", isGcAware=" + isGcAware
          + '}';
    }
  }

  /** Exception payload with GC support. */
  public static final class ExceptionPayload {
    private final ExceptionTag tag;
    private final List<WasmValue> values;
    private final List<GcValue> gcValues;

    /**
     * Creates a new exception payload.
     *
     * @param tag the exception tag
     * @param values the payload values
     * @param gcValues the GC values (nullable)
     */
    public ExceptionPayload(
        final ExceptionTag tag,
        final List<WasmValue> values,
        final List<GcValue> gcValues) {
      this.tag = Objects.requireNonNull(tag, "Exception tag cannot be null");
      this.values = List.copyOf(Objects.requireNonNull(values, "Values cannot be null"));
      this.gcValues = gcValues != null ? List.copyOf(gcValues) : List.of();
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
     * Gets the payload values.
     *
     * @return immutable list of payload values
     */
    public List<WasmValue> getValues() {
      return values;
    }

    /**
     * Gets the GC values.
     *
     * @return immutable list of GC values
     */
    public List<GcValue> getGcValues() {
      return gcValues;
    }

    /**
     * Checks if this payload contains GC values.
     *
     * @return true if GC values are present
     */
    public boolean hasGcValues() {
      return !gcValues.isEmpty();
    }
  }

  /** Exception handling error codes. */
  public enum ExceptionErrorCode {
    /** Exception tag creation failed */
    TAG_CREATION_FAILED,
    /** Exception payload validation failed */
    PAYLOAD_VALIDATION_FAILED,
    /** Exception unwinding failed */
    UNWINDING_FAILED,
    /** Exception propagation failed */
    PROPAGATION_FAILED,
    /** GC reference handling failed */
    GC_REFERENCE_FAILED,
    /** Stack trace capture failed */
    STACK_TRACE_FAILED,
    /** Exception handler registration failed */
    HANDLER_REGISTRATION_FAILED,
    /** Cross-language mapping failed */
    CROSS_LANGUAGE_MAPPING_FAILED
  }

  private final ExceptionErrorCode exceptionErrorCode;
  private final ExceptionPayload exceptionPayload;
  private final String wasmStackTrace;
  private final int unwindDepth;

  /**
   * Creates a new WebAssembly exception handling exception.
   *
   * @param message the error message
   * @param exceptionErrorCode the specific exception error code
   */
  public WasmExceptionHandlingException(
      final String message,
      final ExceptionErrorCode exceptionErrorCode) {
    this(message, null, ErrorCode.UNKNOWN, null, exceptionErrorCode, null, null, 0);
  }

  /**
   * Creates a new WebAssembly exception handling exception with payload.
   *
   * @param message the error message
   * @param exceptionErrorCode the specific exception error code
   * @param exceptionPayload the exception payload that caused the error
   */
  public WasmExceptionHandlingException(
      final String message,
      final ExceptionErrorCode exceptionErrorCode,
      final ExceptionPayload exceptionPayload) {
    this(message, null, ErrorCode.UNKNOWN, null, exceptionErrorCode, exceptionPayload, null, 0);
  }

  /**
   * Creates a new WebAssembly exception handling exception with full details.
   *
   * @param message the error message
   * @param cause the underlying cause
   * @param errorCode the Wasmtime error code
   * @param nativeStackTrace the native stack trace
   * @param exceptionErrorCode the specific exception error code
   * @param exceptionPayload the exception payload that caused the error
   * @param wasmStackTrace the WebAssembly stack trace
   * @param unwindDepth the current unwind depth
   */
  public WasmExceptionHandlingException(
      final String message,
      final Throwable cause,
      final ErrorCode errorCode,
      final String nativeStackTrace,
      final ExceptionErrorCode exceptionErrorCode,
      final ExceptionPayload exceptionPayload,
      final String wasmStackTrace,
      final int unwindDepth) {
    super(message, cause, errorCode, nativeStackTrace);
    this.exceptionErrorCode = exceptionErrorCode != null ? exceptionErrorCode
        : ExceptionErrorCode.TAG_CREATION_FAILED;
    this.exceptionPayload = exceptionPayload;
    this.wasmStackTrace = wasmStackTrace;
    this.unwindDepth = unwindDepth;
  }

  /**
   * Gets the specific exception error code.
   *
   * @return the exception error code
   */
  public ExceptionErrorCode getExceptionErrorCode() {
    return exceptionErrorCode;
  }

  /**
   * Gets the exception payload that caused the error.
   *
   * @return the exception payload, or null if not available
   */
  public ExceptionPayload getExceptionPayload() {
    return exceptionPayload;
  }

  /**
   * Gets the WebAssembly stack trace.
   *
   * @return the WebAssembly stack trace, or null if not available
   */
  public String getWasmStackTrace() {
    return wasmStackTrace;
  }

  /**
   * Gets the current unwind depth.
   *
   * @return the unwind depth
   */
  public int getUnwindDepth() {
    return unwindDepth;
  }

  /**
   * Checks if this exception has a WebAssembly stack trace.
   *
   * @return true if WebAssembly stack trace is available
   */
  public boolean hasWasmStackTrace() {
    return wasmStackTrace != null && !wasmStackTrace.isEmpty();
  }

  /**
   * Checks if this exception involves GC references.
   *
   * @return true if exception payload contains GC references
   */
  public boolean involvesGcReferences() {
    return exceptionPayload != null && exceptionPayload.hasGcValues();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[").append(getErrorCode()).append("/").append(exceptionErrorCode).append("]");
    if (getMessage() != null) {
      sb.append(": ").append(getMessage());
    }
    if (unwindDepth > 0) {
      sb.append(" (unwind depth: ").append(unwindDepth).append(")");
    }
    if (exceptionPayload != null) {
      sb.append(" (tag: ").append(exceptionPayload.getTag().getName()).append(")");
    }
    return sb.toString();
  }

  /** Exception thrown when exception tag creation fails. */
  public static class TagCreationException extends WasmExceptionHandlingException {
    public TagCreationException(final String message, final String tagName) {
      super("Failed to create exception tag '" + tagName + "': " + message,
          ExceptionErrorCode.TAG_CREATION_FAILED);
    }

    public TagCreationException(final String message, final Throwable cause, final String tagName) {
      super("Failed to create exception tag '" + tagName + "': " + message, cause,
          ErrorCode.UNKNOWN, null, ExceptionErrorCode.TAG_CREATION_FAILED, null, null, 0);
    }
  }

  /** Exception thrown when payload validation fails. */
  public static class PayloadValidationException extends WasmExceptionHandlingException {
    public PayloadValidationException(final String message, final ExceptionPayload payload) {
      super(message, ExceptionErrorCode.PAYLOAD_VALIDATION_FAILED, payload);
    }

    public PayloadValidationException(final String message, final Throwable cause,
        final ExceptionPayload payload) {
      super(message, cause, ErrorCode.VALIDATION_ERROR, null,
          ExceptionErrorCode.PAYLOAD_VALIDATION_FAILED, payload, null, 0);
    }
  }

  /** Exception thrown when exception unwinding fails. */
  public static class UnwindingException extends WasmExceptionHandlingException {
    public UnwindingException(final String message, final int unwindDepth) {
      super(message, null, ErrorCode.UNKNOWN, null, ExceptionErrorCode.UNWINDING_FAILED,
          null, null, unwindDepth);
    }

    public UnwindingException(final String message, final Throwable cause, final int unwindDepth) {
      super(message, cause, ErrorCode.UNKNOWN, null, ExceptionErrorCode.UNWINDING_FAILED,
          null, null, unwindDepth);
    }
  }

  /** Exception thrown when GC reference handling fails. */
  public static class GcReferenceException extends WasmExceptionHandlingException {
    public GcReferenceException(final String message, final ExceptionPayload payload) {
      super(message, ExceptionErrorCode.GC_REFERENCE_FAILED, payload);
    }

    public GcReferenceException(final String message, final Throwable cause,
        final ExceptionPayload payload) {
      super(message, cause, ErrorCode.UNKNOWN, null, ExceptionErrorCode.GC_REFERENCE_FAILED,
          payload, null, 0);
    }
  }

  /** Exception thrown when cross-language exception mapping fails. */
  public static class CrossLanguageMappingException extends WasmExceptionHandlingException {
    public CrossLanguageMappingException(final String message, final ExceptionPayload payload) {
      super(message, ExceptionErrorCode.CROSS_LANGUAGE_MAPPING_FAILED, payload);
    }

    public CrossLanguageMappingException(final String message, final Throwable cause,
        final ExceptionPayload payload, final String wasmStackTrace) {
      super(message, cause, ErrorCode.UNKNOWN, null,
          ExceptionErrorCode.CROSS_LANGUAGE_MAPPING_FAILED, payload, wasmStackTrace, 0);
    }
  }
}