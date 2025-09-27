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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmExceptionHandlingException;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.panama.util.PanamaArena;
import ai.tegmentum.wasmtime4j.panama.util.PanamaExceptionMapper;
import ai.tegmentum.wasmtime4j.panama.util.PanamaGcBridge;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama Foreign Function API implementation of WebAssembly exception handling with GC integration.
 *
 * <p>This class provides Panama FFI-specific bindings for WebAssembly exception handling, using
 * Project Panama's Foreign Function and Memory API for type-safe native interoperability. It
 * integrates with both the native Rust implementation and the WebAssembly GC foundation.
 *
 * <p>Key features:
 * <ul>
 *   <li>Type-safe exception handling using Panama FFI
 *   <li>Memory-safe arena-based resource management
 *   <li>GC-aware exception handling for exception payloads containing GC references
 *   <li>Cross-language exception propagation between WebAssembly and Java
 *   <li>Exception unwinding with proper resource cleanup
 *   <li>Stack trace capture and debugging support
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaExceptionHandler implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PanamaExceptionHandler.class.getName());
  private static final AtomicLong NEXT_HANDLER_ID = new AtomicLong(1);

  // Native function handles
  private static final MethodHandle CREATE_HANDLER;
  private static final MethodHandle CREATE_EXCEPTION_TAG;
  private static final MethodHandle THROW_EXCEPTION;
  private static final MethodHandle REGISTER_EXCEPTION_HANDLER;
  private static final MethodHandle CAPTURE_STACK_TRACE;
  private static final MethodHandle PERFORM_UNWINDING;
  private static final MethodHandle CLOSE_HANDLER;
  private static final MethodHandle FREE_STRING;

  static {
    try {
      final SymbolLookup lookup = SymbolLookup.loaderLookup();
      final Linker linker = Linker.nativeLinker();

      CREATE_HANDLER = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_handler_create").orElseThrow(),
          FunctionDescriptor.of(
              ValueLayout.ADDRESS,
              ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT,
              ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN,
              ValueLayout.JAVA_BOOLEAN));

      CREATE_EXCEPTION_TAG = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_tag_create").orElseThrow(),
          FunctionDescriptor.of(
              ValueLayout.JAVA_LONG,
              ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN));

      THROW_EXCEPTION = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_throw").orElseThrow(),
          FunctionDescriptor.ofVoid(
              ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
              ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

      REGISTER_EXCEPTION_HANDLER = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_register_handler").orElseThrow(),
          FunctionDescriptor.ofVoid(
              ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

      CAPTURE_STACK_TRACE = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_capture_stack_trace").orElseThrow(),
          FunctionDescriptor.of(
              ValueLayout.ADDRESS,
              ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      PERFORM_UNWINDING = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_perform_unwinding").orElseThrow(),
          FunctionDescriptor.of(
              ValueLayout.JAVA_BOOLEAN,
              ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

      CLOSE_HANDLER = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_handler_close").orElseThrow(),
          FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      FREE_STRING = linker.downcallHandle(
          lookup.find("wasmtime4j_exception_free_string").orElseThrow(),
          FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      LOGGER.info("Loaded Panama FFI bindings for exception handling");
    } catch (final Exception e) {
      LOGGER.severe("Failed to load Panama FFI bindings for exception handling: " + e.getMessage());
      throw new RuntimeException("Failed to initialize Panama exception handler", e);
    }
  }

  private final long handlerId;
  private final MemorySegment nativeHandle;
  private final GcRuntime gcRuntime;
  private final Arena arena;
  private final ConcurrentHashMap<Long, WasmExceptionHandlingException.ExceptionTag> tagCache;
  private final ConcurrentHashMap<Long, ExceptionHandlerCallback> handlerCallbacks;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama exception handler.
   *
   * @param config the exception handling configuration
   * @param gcRuntime the GC runtime for GC-aware exception handling
   * @throws IllegalArgumentException if config is null
   * @throws WasmExceptionHandlingException if native creation fails
   */
  public PanamaExceptionHandler(final ExceptionHandlingConfig config, final GcRuntime gcRuntime) {
    PanamaValidation.validateNotNull(config, "Exception handling config");

    this.handlerId = NEXT_HANDLER_ID.getAndIncrement();
    this.gcRuntime = gcRuntime; // Nullable - GC support is optional
    this.arena = PanamaArena.createSharedArena();
    this.tagCache = new ConcurrentHashMap<>();
    this.handlerCallbacks = new ConcurrentHashMap<>();

    try {
      this.nativeHandle = (MemorySegment) CREATE_HANDLER.invokeExact(
          config.enableNestedTryCatch,
          config.enableExceptionUnwinding,
          config.maxUnwindDepth,
          config.validateExceptionTypes,
          config.enableStackTraces,
          config.enableExceptionPropagation,
          config.enableGcIntegration);

      if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
        throw new WasmExceptionHandlingException("Failed to create native exception handler",
            WasmExceptionHandlingException.ExceptionErrorCode.TAG_CREATION_FAILED);
      }

      LOGGER.fine("Created Panama exception handler with ID: " + handlerId
          + ", native handle: " + nativeHandle.address());
    } catch (final WasmExceptionHandlingException e) {
      arena.close();
      throw e;
    } catch (final Throwable t) {
      arena.close();
      throw new WasmExceptionHandlingException("Failed to create Panama exception handler",
          t, ai.tegmentum.wasmtime4j.exception.WasmtimeException.ErrorCode.NATIVE_LIBRARY_ERROR,
          null, WasmExceptionHandlingException.ExceptionErrorCode.TAG_CREATION_FAILED,
          null, null, 0);
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
      final String name,
      final List<WasmValueType> parameterTypes,
      final boolean isGcAware) {
    validateNotClosed();
    PanamaValidation.validateNotNull(name, "Exception tag name");
    PanamaValidation.validateNotEmpty(name.trim(), "Exception tag name");
    PanamaValidation.validateNotNull(parameterTypes, "Parameter types");

    final String trimmedName = name.trim();

    // Check if tag already exists
    if (tagCache.values().stream().anyMatch(tag -> tag.getName().equals(trimmedName))) {
      throw new IllegalArgumentException("Exception tag '" + trimmedName + "' already exists");
    }

    // Validate GC integration if requested
    if (isGcAware && gcRuntime == null) {
      throw new IllegalArgumentException("GC-aware exception tag requires GC runtime");
    }

    try (final Arena tempArena = Arena.ofConfined()) {
      // Allocate memory for name and parameter types
      final MemorySegment nameSegment = tempArena.allocateFrom(trimmedName);
      final MemorySegment typesSegment = tempArena.allocateArray(ValueLayout.JAVA_BYTE,
          parameterTypes.size());

      // Copy parameter types to native memory
      for (int i = 0; i < parameterTypes.size(); i++) {
        typesSegment.setAtIndex(ValueLayout.JAVA_BYTE, i, (byte) parameterTypes.get(i).ordinal());
      }

      final long tagHandle = (long) CREATE_EXCEPTION_TAG.invokeExact(
          nativeHandle,
          nameSegment,
          typesSegment,
          (long) parameterTypes.size(),
          isGcAware);

      if (tagHandle == 0) {
        throw new WasmExceptionHandlingException.TagCreationException(
            "Native tag creation returned null handle", trimmedName);
      }

      final WasmExceptionHandlingException.ExceptionTag tag =
          new WasmExceptionHandlingException.ExceptionTag(trimmedName, parameterTypes,
              tagHandle, isGcAware);

      tagCache.put(tagHandle, tag);

      LOGGER.fine("Created exception tag '" + trimmedName + "' with handle: " + tagHandle
          + ", GC-aware: " + isGcAware);
      return tag;
    } catch (final WasmExceptionHandlingException e) {
      throw e;
    } catch (final Throwable t) {
      throw new WasmExceptionHandlingException.TagCreationException(
          "Failed to create exception tag", t, trimmedName);
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
    PanamaValidation.validateNotNull(tag, "Exception tag");
    PanamaValidation.validateNotNull(payload, "Exception payload");

    // Validate payload matches tag parameter types
    validatePayload(tag, payload);

    // Handle GC references if present
    final MemorySegment gcHandlesSegment = handleGcReferences(payload);

    try (final Arena tempArena = Arena.ofConfined()) {
      // Serialize payload for native call
      final MemorySegment payloadSegment = serializePayload(tempArena, payload);

      // Throw the exception natively
      THROW_EXCEPTION.invokeExact(
          nativeHandle,
          tag.getNativeHandle(),
          payloadSegment,
          payloadSegment.byteSize(),
          gcHandlesSegment != null ? gcHandlesSegment : MemorySegment.NULL);

    } catch (final Throwable t) {
      // Clean up GC handles on failure
      if (gcHandlesSegment != null && gcRuntime != null) {
        try {
          PanamaGcBridge.releaseGcHandles(gcRuntime, gcHandlesSegment);
        } catch (final Exception gcE) {
          LOGGER.warning("Failed to release GC handles during exception cleanup: "
              + gcE.getMessage());
        }
      }

      throw new WasmExceptionHandlingException("Failed to throw WebAssembly exception",
          t, ai.tegmentum.wasmtime4j.exception.WasmtimeException.ErrorCode.UNKNOWN, null,
          WasmExceptionHandlingException.ExceptionErrorCode.PROPAGATION_FAILED,
          payload, null, 0);
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
    PanamaValidation.validateNotNull(tag, "Exception tag");
    PanamaValidation.validateNotNull(handler, "Exception handler callback");

    try {
      // Store the callback for native code to invoke
      handlerCallbacks.put(tag.getNativeHandle(), handler);

      // Create a native callback stub using Panama
      final MemorySegment callbackStub = createCallbackStub(tag.getNativeHandle());

      REGISTER_EXCEPTION_HANDLER.invokeExact(nativeHandle, tag.getNativeHandle(), callbackStub);

      LOGGER.fine("Registered exception handler for tag: " + tag.getName());
    } catch (final Throwable t) {
      handlerCallbacks.remove(tag.getNativeHandle());
      throw new WasmExceptionHandlingException("Failed to register exception handler",
          t, ai.tegmentum.wasmtime4j.exception.WasmtimeException.ErrorCode.UNKNOWN, null,
          WasmExceptionHandlingException.ExceptionErrorCode.HANDLER_REGISTRATION_FAILED,
          null, null, 0);
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
      final boolean shouldContinue = (boolean) PERFORM_UNWINDING.invokeExact(nativeHandle,
          currentDepth);
      LOGGER.fine("Unwinding at depth " + currentDepth + ", continue: " + shouldContinue);
      return shouldContinue;
    } catch (final Throwable t) {
      throw new WasmExceptionHandlingException.UnwindingException(
          "Failed to perform unwinding", t, currentDepth);
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
    PanamaValidation.validateNotNull(tag, "Exception tag");

    try {
      final MemorySegment traceSegment = (MemorySegment) CAPTURE_STACK_TRACE.invokeExact(
          nativeHandle, tag.getNativeHandle());

      if (traceSegment == null || traceSegment.equals(MemorySegment.NULL)) {
        return null;
      }

      try {
        final String trace = traceSegment.getString(0);
        LOGGER.fine("Captured stack trace for tag: " + tag.getName());
        return trace;
      } finally {
        // Free the native string
        FREE_STRING.invokeExact(traceSegment);
      }
    } catch (final Throwable t) {
      LOGGER.warning("Failed to capture stack trace for tag " + tag.getName()
          + ": " + t.getMessage());
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
   * Gets the native handle address.
   *
   * @return the native handle address
   */
  public long getNativeHandle() {
    return nativeHandle.address();
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
        if (nativeHandle != null && !nativeHandle.equals(MemorySegment.NULL)) {
          CLOSE_HANDLER.invokeExact(nativeHandle);
        }

        // Close the arena to release all allocated memory
        arena.close();

        LOGGER.info("Closed Panama exception handler with ID: " + handlerId);
      } catch (final Throwable t) {
        LOGGER.warning("Error during exception handler cleanup: " + t.getMessage());
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
          "Exception payload size (" + values.size()
              + ") doesn't match tag parameter count (" + expectedTypes.size() + ")",
          payload);
    }

    for (int i = 0; i < values.size(); i++) {
      final WasmValueType expectedType = expectedTypes.get(i);
      final WasmValueType actualType = values.get(i).getType();

      if (!expectedType.equals(actualType)) {
        throw new WasmExceptionHandlingException.PayloadValidationException(
            "Exception payload parameter " + i + " type mismatch. "
                + "Expected: " + expectedType + ", Actual: " + actualType,
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
   * @return memory segment containing GC handles, or null if no GC references
   */
  private MemorySegment handleGcReferences(
      final WasmExceptionHandlingException.ExceptionPayload payload) {
    if (!payload.hasGcValues() || gcRuntime == null) {
      return null;
    }

    try {
      return PanamaGcBridge.createGcHandles(arena, gcRuntime, payload.getGcValues());
    } catch (final Exception e) {
      throw new WasmExceptionHandlingException.GcReferenceException(
          "Failed to handle GC references in exception payload", e, payload);
    }
  }

  /**
   * Serializes the exception payload for native code.
   *
   * @param tempArena the temporary arena for allocation
   * @param payload the payload to serialize
   * @return the serialized payload data
   */
  private MemorySegment serializePayload(final Arena tempArena,
      final WasmExceptionHandlingException.ExceptionPayload payload) {
    // This would use the ExceptionMarshaling utility
    // For now, return empty segment as placeholder
    return tempArena.allocateArray(ValueLayout.JAVA_BYTE, 0);
  }

  /**
   * Creates a native callback stub for exception handling.
   *
   * @param tagHandle the exception tag handle
   * @return the callback stub memory segment
   */
  private MemorySegment createCallbackStub(final long tagHandle) {
    // This would create a proper Panama callback using the Linker.upcallStub API
    // For now, return null as placeholder - would need proper callback implementation
    return MemorySegment.NULL;
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
      return Objects.hash(enableNestedTryCatch, enableExceptionUnwinding, maxUnwindDepth,
          validateExceptionTypes, enableStackTraces, enableExceptionPropagation,
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
    boolean handle(WasmExceptionHandlingException.ExceptionTag tag,
        WasmExceptionHandlingException.ExceptionPayload payload);
  }
}