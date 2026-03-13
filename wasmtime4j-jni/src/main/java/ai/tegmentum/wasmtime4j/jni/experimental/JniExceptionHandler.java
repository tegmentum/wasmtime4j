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
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * JNI implementation of WebAssembly exception handling.
 *
 * <p>This class provides a pure Java implementation of the {@link ExceptionHandler} interface,
 * maintaining a thread-safe tag registry and exception handling configuration. No native calls are
 * required since this is a custom abstraction layer over WASM exception concepts.
 *
 * @since 1.0.0
 */
public final class JniExceptionHandler implements ExceptionHandler {

  private static final Logger LOGGER = Logger.getLogger(JniExceptionHandler.class.getName());

  private final ConcurrentHashMap<String, ExceptionTag> tagsByName;
  private final ConcurrentHashMap<Long, ExceptionTag> tagsByHandle;
  private final AtomicLong nextTagHandle;
  private final ExceptionHandlingConfig config;
  private final String handlerName;
  private final AtomicBoolean enabled;
  private final AtomicBoolean closed;
  private final ReadWriteLock closeLock = new ReentrantReadWriteLock();

  /**
   * Creates a new JNI exception handler with default configuration.
   *
   * @return a new exception handler
   */
  public static JniExceptionHandler create() {
    return create(DefaultExceptionHandlingConfig.getDefault());
  }

  /**
   * Creates a new JNI exception handler with the specified configuration.
   *
   * @param config the exception handling configuration
   * @return a new exception handler
   * @throws NullPointerException if config is null
   */
  public static JniExceptionHandler create(final ExceptionHandlingConfig config) {
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
  public static JniExceptionHandler create(
      final ExceptionHandlingConfig config, final String handlerName) {
    Objects.requireNonNull(config, "Configuration cannot be null");
    Objects.requireNonNull(handlerName, "Handler name cannot be null");
    return new JniExceptionHandler(config, handlerName);
  }

  private JniExceptionHandler(final ExceptionHandlingConfig config, final String handlerName) {
    this.config = config;
    this.handlerName = handlerName;
    this.tagsByName = new ConcurrentHashMap<>();
    this.tagsByHandle = new ConcurrentHashMap<>();
    this.nextTagHandle = new AtomicLong(1);
    this.enabled = new AtomicBoolean(true);
    this.closed = new AtomicBoolean(false);
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("Created JNI exception handler: " + handlerName);
    }
  }

  @Override
  public HandlingResult handle(final Throwable exception) {
    if (closed.get() || !enabled.get()) {
      return HandlingResult.NOT_HANDLED;
    }

    if (exception == null) {
      return HandlingResult.NOT_HANDLED;
    }

    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("Handling exception: " + exception.getClass().getName());
    }
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
    Validation.requireNonNull(name, "Exception tag name");
    Validation.requireNonNull(parameterTypes, "Parameter types");
    Validation.requireNonBlank(name, "Exception tag name");
    beginOperation();
    try {
      final String trimmedName = name.trim();
      if (tagsByName.containsKey(trimmedName)) {
        throw new IllegalArgumentException("Exception tag already exists: " + trimmedName);
      }

      final long tagHandle = nextTagHandle.getAndIncrement();

      final ExceptionTag tag =
          new DefaultExceptionTag(tagHandle, trimmedName, new ArrayList<>(parameterTypes), false);

      tagsByName.put(trimmedName, tag);
      tagsByHandle.put(tagHandle, tag);

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Created exception tag '" + trimmedName + "' with handle: " + tagHandle);
      }
      return tag;
    } finally {
      endOperation();
    }
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
    beginOperation();
    try {
      if (tagHandle == 0L) {
        return null;
      }

      if (!config.isStackTracesEnabled()) {
        return null;
      }

      // Generate a simulated WASM stack trace matching the Rust implementation
      final ExceptionTag tag = tagsByHandle.get(tagHandle);
      final String tagInfo;
      if (tag != null) {
        tagInfo = "exception tag '" + tag.getTagName() + "' (handle: " + tag.getTagHandle() + ")";
      } else {
        tagInfo = "unknown tag (handle: " + tagHandle + ")";
      }

      final String trace =
          "wasm function 0: <"
              + tagInfo
              + ">\n"
              + "wasm function 1: <wasm_entry>\n"
              + "wasm function 2: <wasm_start>";

      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Captured stack trace for tag handle: " + tagHandle);
      }
      return trace;
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean performUnwinding(final int currentDepth) {
    if (currentDepth < 0) {
      throw new IllegalArgumentException("Current depth cannot be negative");
    }
    beginOperation();
    try {
      if (!config.isExceptionUnwindingEnabled()) {
        return false;
      }

      final boolean shouldContinue = currentDepth < config.getMaxUnwindDepth();
      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Unwinding at depth " + currentDepth + ", continue: " + shouldContinue);
      }
      return shouldContinue;
    } finally {
      endOperation();
    }
  }

  @Override
  public ExceptionHandlingConfig getConfig() {
    return config;
  }

  @Override
  public void close() {
    closeLock.writeLock().lock();
    try {
      if (closed.compareAndSet(false, true)) {
        tagsByName.clear();
        tagsByHandle.clear();
        LOGGER.info("Closed JNI exception handler: " + handlerName);
      }
    } finally {
      closeLock.writeLock().unlock();
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
   * Checks if this handler is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return closed.get();
  }

  private void beginOperation() {
    closeLock.readLock().lock();
    if (closed.get()) {
      closeLock.readLock().unlock();
      throw new IllegalStateException("Exception handler is closed");
    }
  }

  private void endOperation() {
    closeLock.readLock().unlock();
  }
}
