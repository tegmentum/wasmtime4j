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
package ai.tegmentum.wasmtime4j.panama.experimental;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.experimental.DefaultExceptionHandlingConfig;
import ai.tegmentum.wasmtime4j.experimental.DefaultExceptionTag;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
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
public final class PanamaExceptionHandler implements ExceptionHandler {

  private static final Logger LOGGER = Logger.getLogger(PanamaExceptionHandler.class.getName());

  private static final Linker LINKER = Linker.nativeLinker();
  private static final SymbolLookup SYMBOL_LOOKUP;
  private static final MethodHandle CREATE_HANDLER;
  private static final MethodHandle CREATE_TAG;
  private static final MethodHandle CAPTURE_STACK_TRACE;
  private static final MethodHandle PERFORM_UNWINDING;
  private static final MethodHandle CLOSE_HANDLER;
  private static final MethodHandle FREE_STRING;

  private final ConcurrentHashMap<String, ExceptionTag> tagsByName;
  private final ConcurrentHashMap<Long, ExceptionTag> tagsByHandle;
  private final Arena arena;
  private final MemorySegment nativeHandle;
  private final ExceptionHandlingConfig config;
  private final String handlerName;
  private final AtomicBoolean enabled;
  private final NativeResourceHandle resourceHandle;

  static {
    try {
      // Use NativeLibraryLoader to properly load the library from JAR resources
      SYMBOL_LOOKUP = NativeLibraryLoader.getInstance().getSymbolLookup();

      CREATE_HANDLER =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_handler_create").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_BOOLEAN));

      CREATE_TAG =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_tag_create").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      CAPTURE_STACK_TRACE =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_capture_stack_trace").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      PERFORM_UNWINDING =
          LINKER.downcallHandle(
              SYMBOL_LOOKUP.find("wasmtime4j_exception_perform_unwinding").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

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

  /**
   * Creates a new Panama exception handler with default configuration.
   *
   * @return a new exception handler
   */
  public static PanamaExceptionHandler create() {
    return create(DefaultExceptionHandlingConfig.getDefault());
  }

  /**
   * Creates a new Panama exception handler with the specified configuration.
   *
   * @param config the exception handling configuration
   * @return a new exception handler
   * @throws NullPointerException if config is null
   */
  public static PanamaExceptionHandler create(final ExceptionHandlingConfig config) {
    return create(config, "PanamaExceptionHandler");
  }

  /**
   * Creates a new Panama exception handler with the specified configuration and name.
   *
   * @param config the exception handling configuration
   * @param handlerName the handler name
   * @return a new exception handler
   * @throws NullPointerException if config or handlerName is null
   */
  public static PanamaExceptionHandler create(
      final ExceptionHandlingConfig config, final String handlerName) {
    Objects.requireNonNull(config, "Configuration cannot be null");
    Objects.requireNonNull(handlerName, "Handler name cannot be null");
    return new PanamaExceptionHandler(config, handlerName);
  }

  private PanamaExceptionHandler(final ExceptionHandlingConfig config, final String handlerName) {
    this.config = config;
    this.handlerName = handlerName;
    this.tagsByName = new ConcurrentHashMap<>();
    this.tagsByHandle = new ConcurrentHashMap<>();
    this.arena = Arena.ofShared();
    this.enabled = new AtomicBoolean(true);

    try {
      this.nativeHandle =
          (MemorySegment)
              CREATE_HANDLER.invoke(
                  config.isNestedTryCatchEnabled(),
                  config.isExceptionUnwindingEnabled(),
                  config.getMaxUnwindDepth(),
                  config.isExceptionTypeValidationEnabled());

      if (nativeHandle == null || nativeHandle.address() == 0L) {
        throw new RuntimeException("Failed to create native exception handler");
      }

      // Capture local references for safety net (must not capture 'this')
      final MemorySegment handleForCleanup = this.nativeHandle;
      final Arena arenaForCleanup = this.arena;

      this.resourceHandle =
          new NativeResourceHandle(
              "PanamaExceptionHandler",
              () -> {
                tagsByName.clear();
                tagsByHandle.clear();

                if (nativeHandle != null && nativeHandle.address() != 0L) {
                  try {
                    CLOSE_HANDLER.invoke(nativeHandle);
                  } catch (final Throwable t) {
                    throw new Exception("Failed to close native exception handler", t);
                  }
                }

                arena.close();
                LOGGER.info("Closed Panama exception handler: " + handlerName);
              },
              this,
              () -> {
                if (handleForCleanup != null && handleForCleanup.address() != 0L) {
                  try {
                    CLOSE_HANDLER.invoke(handleForCleanup);
                  } catch (final Throwable t) {
                    LOGGER.log(Level.WARNING, "Safety net cleanup failed", t);
                  }
                }
                arenaForCleanup.close();
              });

      LOGGER.fine("Created Panama exception handler: " + handlerName);
    } catch (final Throwable e) {
      arena.close();
      throw new RuntimeException("Failed to create native exception handler", e);
    }
  }

  @Override
  public HandlingResult handle(final Throwable exception) {
    if (!resourceHandle.tryBeginOperation()) {
      return HandlingResult.NOT_HANDLED;
    }
    try {
      if (!enabled.get()) {
        return HandlingResult.NOT_HANDLED;
      }

      if (exception == null) {
        return HandlingResult.NOT_HANDLED;
      }

      LOGGER.fine("Handling exception: " + exception.getClass().getName());
      return HandlingResult.HANDLED;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public String getHandlerName() {
    return handlerName;
  }

  @Override
  public boolean isEnabled() {
    return enabled.get() && !resourceHandle.isClosed();
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
    resourceHandle.beginOperation();
    try {

      if (tagsByName.containsKey(name)) {
        throw new IllegalArgumentException("Exception tag already exists: " + name);
      }

      try {
        final MemorySegment nameSegment = arena.allocateFrom(name.trim());

        final byte[] typesArray = new byte[parameterTypes.size()];
        for (int i = 0; i < parameterTypes.size(); i++) {
          typesArray[i] = (byte) parameterTypes.get(i).ordinal();
        }
        final MemorySegment typesSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, typesArray);

        final long tagHandle =
            (long)
                CREATE_TAG.invoke(
                    nativeHandle, nameSegment, typesSegment, (long) parameterTypes.size());

        if (tagHandle == 0L) {
          throw new RuntimeException("Failed to create native exception tag: " + name);
        }

        final ExceptionTag tag =
            new DefaultExceptionTag(tagHandle, name, new ArrayList<>(parameterTypes), false);

        tagsByName.put(name, tag);
        tagsByHandle.put(tagHandle, tag);

        LOGGER.fine("Created exception tag '" + name + "' with handle: " + tagHandle);
        return tag;
      } catch (final Throwable e) {
        throw new RuntimeException("Failed to create exception tag: " + name, e);
      }
    } finally {
      resourceHandle.endOperation();
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
    resourceHandle.beginOperation();
    try {

      if (tagHandle == 0L) {
        return null;
      }

      if (!config.isStackTracesEnabled()) {
        return null;
      }

      try {
        final MemorySegment traceSegment =
            (MemorySegment) CAPTURE_STACK_TRACE.invoke(nativeHandle, tagHandle);

        if (traceSegment == null || traceSegment.address() == 0L) {
          return null;
        }

        final String trace = traceSegment.reinterpret(Long.MAX_VALUE).getString(0);
        FREE_STRING.invoke(traceSegment);

        LOGGER.fine("Captured stack trace for tag handle: " + tagHandle);
        return trace;
      } catch (final Throwable e) {
        LOGGER.warning("Failed to capture stack trace: " + e.getMessage());
        return null;
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public boolean performUnwinding(final int currentDepth) {
    resourceHandle.beginOperation();
    try {

      if (currentDepth < 0) {
        throw new IllegalArgumentException("Current depth cannot be negative");
      }

      if (!config.isExceptionUnwindingEnabled()) {
        return false;
      }

      try {
        final boolean shouldContinue =
            (boolean) PERFORM_UNWINDING.invoke(nativeHandle, currentDepth);
        LOGGER.fine("Unwinding at depth " + currentDepth + ", continue: " + shouldContinue);
        return shouldContinue;
      } catch (final Throwable e) {
        LOGGER.warning("Failed to perform unwinding: " + e.getMessage());
        return false;
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ExceptionHandlingConfig getConfig() {
    return config;
  }

  @Override
  public void close() {
    resourceHandle.close();
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
   * Gets the native memory segment handle.
   *
   * @return the native handle
   */
  public MemorySegment getNativeHandle() {
    resourceHandle.beginOperation();
    try {
      return nativeHandle;
    } finally {
      resourceHandle.endOperation();
    }
  }

  /**
   * Checks if this handler is closed.
   *
   * @return true if closed
   */
  public boolean isClosed() {
    return resourceHandle.isClosed();
  }
}
