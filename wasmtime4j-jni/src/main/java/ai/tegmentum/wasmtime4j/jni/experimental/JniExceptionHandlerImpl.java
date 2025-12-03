/*
 * Copyright 2025 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.experimental.DefaultExceptionHandlingConfig;
import ai.tegmentum.wasmtime4j.experimental.DefaultExceptionTag;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of WebAssembly exception handling.
 *
 * <p>This class provides JNI bindings for WebAssembly exception handling, integrating with the
 * native Rust implementation while ensuring proper resource management and defensive programming
 * practices.
 *
 * @since 1.0.0
 */
public final class JniExceptionHandlerImpl implements ExceptionHandler {

  private static final Logger LOGGER = Logger.getLogger(JniExceptionHandlerImpl.class.getName());

  private final ConcurrentHashMap<String, ExceptionTag> tagsByName;
  private final ConcurrentHashMap<Long, ExceptionTag> tagsByHandle;
  private final long nativeHandle;
  private final ExceptionHandlingConfig config;
  private final String handlerName;
  private final AtomicBoolean enabled;
  private final AtomicBoolean closed;

  static {
    NativeLibraryLoader.loadLibrary();
  }

  /**
   * Creates a new JNI exception handler with default configuration.
   *
   * @return a new exception handler
   */
  public static JniExceptionHandlerImpl create() {
    return create(DefaultExceptionHandlingConfig.getDefault());
  }

  /**
   * Creates a new JNI exception handler with the specified configuration.
   *
   * @param config the exception handling configuration
   * @return a new exception handler
   * @throws NullPointerException if config is null
   */
  public static JniExceptionHandlerImpl create(final ExceptionHandlingConfig config) {
    return create(config, "JniExceptionHandler");
  }

  /**
   * Creates a new JNI exception handler with the specified configuration and name.
   *
   * @param config the exception handling configuration
   * @param handlerName the handler name
   * @return a new exception handler
   * @throws NullPointerException if config or handlerName is null
   */
  public static JniExceptionHandlerImpl create(
      final ExceptionHandlingConfig config, final String handlerName) {
    Objects.requireNonNull(config, "Configuration cannot be null");
    Objects.requireNonNull(handlerName, "Handler name cannot be null");
    return new JniExceptionHandlerImpl(config, handlerName);
  }

  private JniExceptionHandlerImpl(final ExceptionHandlingConfig config, final String handlerName) {
    this.config = config;
    this.handlerName = handlerName;
    this.tagsByName = new ConcurrentHashMap<>();
    this.tagsByHandle = new ConcurrentHashMap<>();
    this.enabled = new AtomicBoolean(true);
    this.closed = new AtomicBoolean(false);

    this.nativeHandle = createNativeHandler(config);

    if (nativeHandle == 0L) {
      throw new RuntimeException("Failed to create native exception handler");
    }

    LOGGER.fine("Created JNI exception handler: " + handlerName);
  }

  @Override
  public HandlingResult handle(final Throwable exception) {
    if (closed.get() || !enabled.get()) {
      return HandlingResult.NOT_HANDLED;
    }

    if (exception == null) {
      return HandlingResult.NOT_HANDLED;
    }

    LOGGER.fine("Handling exception: " + exception.getClass().getName());
    return HandlingResult.HANDLED;
  }

  @Override
  public String getHandlerName() {
    return handlerName;
  }

  @Override
  public boolean isEnabled() {
    return enabled.get() && !closed.get();
  }

  /**
   * Enables or disables this exception handler.
   *
   * @param enabled true to enable, false to disable
   */
  public void setEnabled(final boolean enabled) {
    this.enabled.set(enabled);
  }

  @Override
  public ExceptionTag createExceptionTag(
      final String name, final List<WasmValueType> parameterTypes) {
    JniValidation.requireNonNull(name, "Exception tag name");
    JniValidation.requireNonNull(parameterTypes, "Parameter types");
    JniValidation.requireNonBlank(name, "Exception tag name");
    ensureNotClosed();

    if (tagsByName.containsKey(name)) {
      throw new IllegalArgumentException("Exception tag already exists: " + name);
    }

    final long tagHandle =
        createNativeExceptionTag(nativeHandle, name.trim(), new ArrayList<>(parameterTypes));

    if (tagHandle == 0L) {
      throw new RuntimeException("Failed to create native exception tag: " + name);
    }

    final ExceptionTag tag =
        new DefaultExceptionTag(tagHandle, name, new ArrayList<>(parameterTypes), false);

    tagsByName.put(name, tag);
    tagsByHandle.put(tagHandle, tag);

    LOGGER.fine("Created exception tag '" + name + "' with handle: " + tagHandle);
    return tag;
  }

  @Override
  public Optional<ExceptionTag> getExceptionTag(final String name) {
    if (name == null || name.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(tagsByName.get(name.trim()));
  }

  @Override
  public List<ExceptionTag> listExceptionTags() {
    return Collections.unmodifiableList(new ArrayList<>(tagsByName.values()));
  }

  @Override
  public String captureStackTrace(final long tagHandle) {
    ensureNotClosed();

    if (tagHandle == 0L) {
      return null;
    }

    if (!config.isStackTracesEnabled()) {
      return null;
    }

    final String trace = captureNativeStackTrace(nativeHandle, tagHandle);
    if (trace != null) {
      LOGGER.fine("Captured stack trace for tag handle: " + tagHandle);
    }
    return trace;
  }

  @Override
  public boolean performUnwinding(final int currentDepth) {
    ensureNotClosed();

    if (currentDepth < 0) {
      throw new IllegalArgumentException("Current depth cannot be negative");
    }

    if (!config.isExceptionUnwindingEnabled()) {
      return false;
    }

    final boolean shouldContinue = performNativeUnwinding(nativeHandle, currentDepth);
    LOGGER.fine("Unwinding at depth " + currentDepth + ", continue: " + shouldContinue);
    return shouldContinue;
  }

  @Override
  public ExceptionHandlingConfig getConfig() {
    return config;
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        tagsByName.clear();
        tagsByHandle.clear();

        if (nativeHandle != 0L) {
          closeNativeHandler(nativeHandle);
        }

        LOGGER.info("Closed JNI exception handler: " + handlerName);
      } catch (final Exception e) {
        LOGGER.warning("Failed to close JNI exception handler: " + e.getMessage());
      }
    }
  }

  /**
   * Gets a cached exception tag by handle.
   *
   * @param tagHandle the tag handle
   * @return the exception tag, or null if not found
   */
  public ExceptionTag getTagByHandle(final long tagHandle) {
    return tagsByHandle.get(tagHandle);
  }

  /**
   * Gets the native handle.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    ensureNotClosed();
    return nativeHandle;
  }

  /**
   * Checks if this handler is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed.get();
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Exception handler is closed");
    }
  }

  // Native methods - match the Rust JNI bindings

  /**
   * Creates a native exception handler.
   *
   * @param config the configuration
   * @return the native handle, or 0 on failure
   */
  private static native long createNativeHandler(ExceptionHandlingConfig config);

  /**
   * Creates a native exception tag.
   *
   * @param handlerHandle the handler handle
   * @param name the tag name
   * @param parameterTypes the parameter types
   * @return the tag handle, or 0 on failure
   */
  private static native long createNativeExceptionTag(
      long handlerHandle, String name, List<WasmValueType> parameterTypes);

  /**
   * Captures a stack trace for an exception.
   *
   * @param handlerHandle the handler handle
   * @param tagHandle the tag handle
   * @return the stack trace, or null if not available
   */
  private static native String captureNativeStackTrace(long handlerHandle, long tagHandle);

  /**
   * Performs native unwinding.
   *
   * @param handlerHandle the handler handle
   * @param currentDepth the current depth
   * @return true if unwinding should continue
   */
  private static native boolean performNativeUnwinding(long handlerHandle, int currentDepth);

  /**
   * Closes a native exception handler.
   *
   * @param handle the native handle
   */
  private static native void closeNativeHandler(long handle);
}
