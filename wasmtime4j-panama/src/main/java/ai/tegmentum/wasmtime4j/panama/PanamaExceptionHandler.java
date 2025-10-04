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

package ai.tegmentum.wasmtime4j.panama.experimental;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Panama implementation of WebAssembly exception handling.
 *
 * <p>This class provides Panama Foreign Function API bindings for WebAssembly exception handling,
 * integrating with the native Rust implementation while ensuring proper resource management and
 * defensive programming practices.
 *
 * @since 1.0.0
 */
public final class PanamaExceptionHandler {

  private static final Logger LOGGER = Logger.getLogger(PanamaExceptionHandler.class.getName());

  private static final Linker LINKER = Linker.nativeLinker();
  private static final SymbolLookup SYMBOL_LOOKUP;
  private static final MethodHandle CREATE_HANDLER;
  private static final MethodHandle CREATE_TAG;
  private static final MethodHandle CAPTURE_STACK_TRACE;
  private static final MethodHandle PERFORM_UNWINDING;
  private static final MethodHandle CLOSE_HANDLER;
  private static final MethodHandle FREE_STRING;

  private final ConcurrentHashMap<Long, ExceptionHandler.ExceptionTag> tagCache;
  private final Arena arena;

  static {
    try {
      // Load native library
      System.loadLibrary("wasmtime4j_native");
      SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

      // Initialize method handles
      CREATE_HANDLER =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_handler_create").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_BOOLEAN, // enable_nested
                  ValueLayout.JAVA_BOOLEAN, // enable_unwinding
                  ValueLayout.JAVA_INT, // max_depth
                  ValueLayout.JAVA_BOOLEAN // validate_types
                  ));

      CREATE_TAG =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_tag_create").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS, // handler
                  ValueLayout.ADDRESS, // name
                  ValueLayout.ADDRESS, // param_types
                  ValueLayout.JAVA_LONG // param_count
                  ));

      CAPTURE_STACK_TRACE =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_capture_stack_trace").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS, // handler
                  ValueLayout.JAVA_LONG // tag_handle
                  ));

      PERFORM_UNWINDING =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_perform_unwinding").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS, // handler
                  ValueLayout.JAVA_INT // current_depth
                  ));

      CLOSE_HANDLER =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_handler_close").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      FREE_STRING =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_free_string").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      LOGGER.info("Initialized Panama exception handling bindings");
    } catch (final Exception e) {
      LOGGER.severe("Failed to initialize Panama exception handling: " + e.getMessage());
      throw new RuntimeException("Failed to initialize Panama exception handling", e);
    }
  }

  /** Creates a new Panama exception handler. */
  public PanamaExceptionHandler() {
    this.tagCache = new ConcurrentHashMap<>();
    this.arena = Arena.ofShared();
  }

  /**
   * Creates a native exception handler with the given configuration.
   *
   * @param config the exception handling configuration
   * @return the native memory segment for the exception handler
   * @throws IllegalArgumentException if config is null
   * @throws RuntimeException if native creation fails
   */
  public MemorySegment createNativeHandler(final ExceptionHandler.ExceptionHandlingConfig config) {
    PanamaValidation.requireNonNull(config, "Exception handling config");
    // TODO: Implement when ExceptionHandlingConfig interface is fully defined
    throw new UnsupportedOperationException(
        "Exception handler creation not yet implemented - config methods not available");
  }

  /**
   * Creates a native exception tag.
   *
   * @param handlerSegment the exception handler memory segment
   * @param name the tag name
   * @param parameterTypes the parameter types
   * @return the native handle for the exception tag
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws RuntimeException if native creation fails
   */
  public long createNativeExceptionTag(
      final MemorySegment handlerSegment,
      final String name,
      final List<WasmValueType> parameterTypes) {
    PanamaValidation.requireNonNull(handlerSegment, "Handler segment");
    PanamaValidation.requireNonNull(name, "Exception tag name");
    PanamaValidation.requireNonNull(parameterTypes, "Parameter types");
    PanamaValidation.requireNonBlank(name, "Exception tag name");

    if (handlerSegment.address() == 0L) {
      throw new IllegalArgumentException("Invalid handler segment");
    }

    try {
      // Allocate memory for the name
      final MemorySegment nameSegment = arena.allocateFrom(name.trim());

      // Convert parameter types to byte array
      final byte[] typesArray = new byte[parameterTypes.size()];
      for (int i = 0; i < parameterTypes.size(); i++) {
        typesArray[i] = (byte) parameterTypes.get(i).ordinal();
      }
      final MemorySegment typesSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, typesArray);

      final long tagHandle =
          (long)
              CREATE_TAG.invoke(
                  handlerSegment, nameSegment, typesSegment, (long) parameterTypes.size());

      if (tagHandle == 0L) {
        throw new RuntimeException("Failed to create native exception tag: " + name);
      }

      // TODO: Cache the tag when ExceptionTag implementation is available
      // ExceptionTag is abstract and cannot be instantiated directly
      // tagCache.put(tagHandle, tag);

      LOGGER.fine("Created native exception tag '" + name + "' with handle: " + tagHandle);
      return tagHandle;
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to create exception tag: " + name, e);
    }
  }

  /**
   * Captures a stack trace for an exception.
   *
   * @param handlerSegment the exception handler memory segment
   * @param tagHandle the exception tag handle
   * @return the stack trace string, or null if not available
   * @throws IllegalArgumentException if handler segment is invalid
   * @throws RuntimeException if capture fails
   */
  public String captureStackTrace(final MemorySegment handlerSegment, final long tagHandle) {
    PanamaValidation.requireNonNull(handlerSegment, "Handler segment");

    if (handlerSegment.address() == 0L) {
      throw new IllegalArgumentException("Invalid handler segment");
    }
    if (tagHandle == 0L) {
      throw new IllegalArgumentException("Invalid tag handle");
    }

    try {
      final MemorySegment traceSegment =
          (MemorySegment) CAPTURE_STACK_TRACE.invoke(handlerSegment, tagHandle);

      if (traceSegment.address() == 0L) {
        return null; // No stack trace available
      }

      // Convert C string to Java string
      final String trace = traceSegment.reinterpret(Long.MAX_VALUE).getString(0);

      // Free the native string
      FREE_STRING.invoke(traceSegment);

      LOGGER.fine("Captured stack trace for tag handle: " + tagHandle);
      return trace;
    } catch (final Throwable e) {
      LOGGER.warning("Failed to capture stack trace: " + e.getMessage());
      return null; // Return null instead of throwing for optional feature
    }
  }

  /**
   * Performs native exception unwinding.
   *
   * @param handlerSegment the exception handler memory segment
   * @param currentDepth the current unwind depth
   * @return true if unwinding should continue, false if maximum depth reached
   * @throws IllegalArgumentException if handler segment is invalid
   * @throws RuntimeException if unwinding fails
   */
  public boolean performNativeUnwinding(
      final MemorySegment handlerSegment, final int currentDepth) {
    PanamaValidation.requireNonNull(handlerSegment, "Handler segment");

    if (handlerSegment.address() == 0L) {
      throw new IllegalArgumentException("Invalid handler segment");
    }

    if (currentDepth < 0) {
      throw new IllegalArgumentException("Current depth cannot be negative");
    }

    try {
      final boolean shouldContinue =
          (boolean) PERFORM_UNWINDING.invoke(handlerSegment, currentDepth);
      LOGGER.fine("Unwinding at depth " + currentDepth + ", continue: " + shouldContinue);
      return shouldContinue;
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to perform unwinding", e);
    }
  }

  /**
   * Closes a native exception handler and releases resources.
   *
   * @param handlerSegment the exception handler memory segment
   */
  public void closeNativeHandler(final MemorySegment handlerSegment) {
    if (handlerSegment == null || handlerSegment.address() == 0L) {
      return; // Already closed or invalid
    }

    try {
      // Clear all cached tags when closing handler
      // Note: We cannot reliably determine which tags belong to this specific handler
      // without additional metadata tracking, so we clear all tags for safety
      tagCache.clear();

      final long handlerAddress = handlerSegment.address();
      CLOSE_HANDLER.invoke(handlerSegment);

      LOGGER.fine("Closed native exception handler at address: " + handlerAddress);
    } catch (final Throwable e) {
      LOGGER.warning("Failed to close native exception handler: " + e.getMessage());
    }
  }

  /**
   * Gets a cached exception tag by handle.
   *
   * @param tagHandle the tag handle
   * @return the exception tag, or null if not found
   */
  public ExceptionHandler.ExceptionTag getCachedTag(final long tagHandle) {
    return tagCache.get(tagHandle);
  }

  /** Closes this Panama exception handler and releases all resources. */
  public void close() {
    try {
      tagCache.clear();
      arena.close();
      LOGGER.info("Closed Panama exception handler");
    } catch (final Exception e) {
      LOGGER.warning("Failed to close Panama exception handler: " + e.getMessage());
    }
  }

  /**
   * Converts WebAssembly value types to byte array for native calls.
   *
   * @param types the value types
   * @return byte array representation
   */
  private byte[] convertTypesToBytes(final List<WasmValueType> types) {
    final byte[] bytes = new byte[types.size()];
    for (int i = 0; i < types.size(); i++) {
      bytes[i] = (byte) types.get(i).ordinal();
    }
    return bytes;
  }

  /**
   * Validates that a memory segment is valid and not null.
   *
   * @param segment the memory segment
   * @param name the parameter name for error messages
   * @throws IllegalArgumentException if segment is invalid
   */
  private void validateMemorySegment(final MemorySegment segment, final String name) {
    if (segment == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
    if (segment.address() == 0L) {
      throw new IllegalArgumentException(name + " has invalid address");
    }
  }

  /**
   * Creates a scoped arena for temporary allocations.
   *
   * @return a new confined arena
   */
  public Arena createScopedArena() {
    return Arena.ofConfined();
  }

  /**
   * Gets the shared arena for this handler.
   *
   * @return the shared arena
   */
  public Arena getSharedArena() {
    return arena;
  }

  /**
   * Checks if the Panama exception handler is properly initialized.
   *
   * @return true if initialized, false otherwise
   */
  public boolean isInitialized() {
    return arena.scope().isAlive();
  }
}
