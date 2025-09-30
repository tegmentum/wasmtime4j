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

package ai.tegmentum.wasmtime4j.jni.experimental;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * JNI implementation of WebAssembly exception handling.
 *
 * <p>This class provides JNI-specific bindings for WebAssembly exception handling, integrating with
 * the native Rust implementation while ensuring proper resource management and defensive
 * programming practices.
 *
 * @since 1.0.0
 */
public final class JniExceptionHandler {

  private static final Logger LOGGER = Logger.getLogger(JniExceptionHandler.class.getName());

  private final ConcurrentHashMap<Long, ExceptionHandler.ExceptionTag> tagCache;
  private final ConcurrentHashMap<Long, ExceptionHandlerCallback> handlerCallbacks;

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

  /** Creates a new JNI exception handler. */
  public JniExceptionHandler() {
    this.tagCache = new ConcurrentHashMap<>();
    this.handlerCallbacks = new ConcurrentHashMap<>();
  }

  /**
   * Creates a native exception handler with the given configuration.
   *
   * @param config the exception handling configuration
   * @return the native handle for the exception handler
   * @throws IllegalArgumentException if config is null
   * @throws RuntimeException if native creation fails
   */
  public long createNativeHandler(final ExceptionHandler.ExceptionHandlingConfig config) {
    JniValidation.validateNotNull(config, "Exception handling config");

    try {
      final long handle =
          nativeCreateHandler(
              config.isNestedTryCatchEnabled(),
              config.isExceptionUnwindingEnabled(),
              config.getMaxUnwindDepth(),
              config.isExceptionTypeValidationEnabled(),
              config.isStackTracesEnabled(),
              config.isExceptionPropagationEnabled());

      if (handle == 0) {
        throw new RuntimeException("Failed to create native exception handler");
      }

      LOGGER.fine("Created native exception handler with handle: " + handle);
      return handle;
    } catch (final Exception e) {
      throw JniExceptionMapper.mapToRuntimeException(
          "Failed to create native exception handler", e);
    }
  }

  /**
   * Creates a native exception tag.
   *
   * @param handlerHandle the exception handler handle
   * @param name the tag name
   * @param parameterTypes the parameter types
   * @return the native handle for the exception tag
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws RuntimeException if native creation fails
   */
  public long createNativeExceptionTag(
      final long handlerHandle, final String name, final List<WasmValueType> parameterTypes) {
    JniValidation.validateNotNull(name, "Exception tag name");
    JniValidation.validateNotNull(parameterTypes, "Parameter types");
    JniValidation.validateNotEmpty(name.trim(), "Exception tag name");

    if (handlerHandle == 0) {
      throw new IllegalArgumentException("Invalid handler handle");
    }

    try {
      // Convert parameter types to byte array for native call
      final byte[] typeBytes = new byte[parameterTypes.size()];
      for (int i = 0; i < parameterTypes.size(); i++) {
        typeBytes[i] = (byte) parameterTypes.get(i).ordinal();
      }

      final long tagHandle = nativeCreateExceptionTag(handlerHandle, name.trim(), typeBytes);

      if (tagHandle == 0) {
        throw new RuntimeException("Failed to create native exception tag: " + name);
      }

      // Cache the tag for later use
      final ExceptionHandler.ExceptionTag tag =
          new ExceptionHandler.ExceptionTag(name.trim(), parameterTypes, tagHandle, handlerHandle);
      tagCache.put(tagHandle, tag);

      LOGGER.fine("Created native exception tag '" + name + "' with handle: " + tagHandle);
      return tagHandle;
    } catch (final Exception e) {
      throw JniExceptionMapper.mapToRuntimeException("Failed to create exception tag: " + name, e);
    }
  }

  /**
   * Registers a native exception handler for a specific tag.
   *
   * @param handlerHandle the exception handler handle
   * @param tagHandle the exception tag handle
   * @param callback the exception handler callback
   * @throws IllegalArgumentException if callback is null or handles are invalid
   * @throws RuntimeException if registration fails
   */
  public void registerNativeExceptionHandler(
      final long handlerHandle, final long tagHandle, final ExceptionHandlerCallback callback) {
    JniValidation.validateNotNull(callback, "Exception handler callback");

    if (handlerHandle == 0) {
      throw new IllegalArgumentException("Invalid handler handle");
    }
    if (tagHandle == 0) {
      throw new IllegalArgumentException("Invalid tag handle");
    }

    try {
      // Store the callback for native code to invoke
      handlerCallbacks.put(tagHandle, callback);

      nativeRegisterExceptionHandler(handlerHandle, tagHandle);

      LOGGER.fine("Registered exception handler for tag handle: " + tagHandle);
    } catch (final Exception e) {
      handlerCallbacks.remove(tagHandle);
      throw JniExceptionMapper.mapToRuntimeException("Failed to register exception handler", e);
    }
  }

  /**
   * Captures a stack trace for an exception.
   *
   * @param handlerHandle the exception handler handle
   * @param tagHandle the exception tag handle
   * @return the stack trace string, or null if not available
   * @throws IllegalArgumentException if handles are invalid
   * @throws RuntimeException if capture fails
   */
  public String captureStackTrace(final long handlerHandle, final long tagHandle) {
    if (handlerHandle == 0) {
      throw new IllegalArgumentException("Invalid handler handle");
    }
    if (tagHandle == 0) {
      throw new IllegalArgumentException("Invalid tag handle");
    }

    try {
      final String trace = nativeCaptureStackTrace(handlerHandle, tagHandle);
      LOGGER.fine("Captured stack trace for tag handle: " + tagHandle);
      return trace;
    } catch (final Exception e) {
      LOGGER.warning("Failed to capture stack trace: " + e.getMessage());
      return null; // Return null instead of throwing for optional feature
    }
  }

  /**
   * Performs native exception unwinding.
   *
   * @param handlerHandle the exception handler handle
   * @param currentDepth the current unwind depth
   * @return true if unwinding should continue, false if maximum depth reached
   * @throws IllegalArgumentException if handle is invalid
   * @throws RuntimeException if unwinding fails
   */
  public boolean performNativeUnwinding(final long handlerHandle, final int currentDepth) {
    if (handlerHandle == 0) {
      throw new IllegalArgumentException("Invalid handler handle");
    }

    if (currentDepth < 0) {
      throw new IllegalArgumentException("Current depth cannot be negative");
    }

    try {
      final boolean shouldContinue = nativePerformUnwinding(handlerHandle, currentDepth);
      LOGGER.fine("Unwinding at depth " + currentDepth + ", continue: " + shouldContinue);
      return shouldContinue;
    } catch (final Exception e) {
      throw JniExceptionMapper.mapToRuntimeException("Failed to perform unwinding", e);
    }
  }

  /**
   * Closes a native exception handler and releases resources.
   *
   * @param handlerHandle the exception handler handle
   */
  public void closeNativeHandler(final long handlerHandle) {
    if (handlerHandle == 0) {
      return; // Already closed or invalid
    }

    try {
      // Remove cached tags and handlers for this handler
      tagCache
          .entrySet()
          .removeIf(
              entry -> {
                final ExceptionHandler.ExceptionTag tag = entry.getValue();
                return tag.getHandlerId() == handlerHandle;
              });

      handlerCallbacks
          .entrySet()
          .removeIf(
              entry -> {
                final ExceptionHandler.ExceptionTag tag = tagCache.get(entry.getKey());
                return tag != null && tag.getHandlerId() == handlerHandle;
              });

      nativeCloseHandler(handlerHandle);

      LOGGER.fine("Closed native exception handler with handle: " + handlerHandle);
    } catch (final Exception e) {
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

  /**
   * Callback invoked from native code when an exception is handled.
   *
   * @param tagHandle the exception tag handle
   * @param payloadData the serialized payload data
   * @return true to continue execution, false to re-throw
   */
  @SuppressWarnings("unused") // Called from native code
  private boolean onExceptionHandled(final long tagHandle, final byte[] payloadData) {
    try {
      final ExceptionHandlerCallback callback = handlerCallbacks.get(tagHandle);
      if (callback == null) {
        LOGGER.warning("No callback found for tag handle: " + tagHandle);
        return false;
      }

      final ExceptionHandler.ExceptionTag tag = tagCache.get(tagHandle);
      if (tag == null) {
        LOGGER.warning("No tag found for handle: " + tagHandle);
        return false;
      }

      // Deserialize payload data
      final List<WasmValue> payload = deserializePayload(payloadData, tag.getParameterTypes());

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

  // Native method declarations
  private static native long nativeCreateHandler(
      boolean enableNested,
      boolean enableUnwinding,
      int maxDepth,
      boolean validateTypes,
      boolean enableStackTraces,
      boolean enablePropagation);

  private static native long nativeCreateExceptionTag(
      long handlerHandle, String name, byte[] parameterTypes);

  private static native void nativeRegisterExceptionHandler(long handlerHandle, long tagHandle);

  private static native String nativeCaptureStackTrace(long handlerHandle, long tagHandle);

  private static native boolean nativePerformUnwinding(long handlerHandle, int currentDepth);

  private static native void nativeCloseHandler(long handlerHandle);

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
    boolean handle(ExceptionHandler.ExceptionTag tag, List<WasmValue> payload);
  }
}
