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
package ai.tegmentum.wasmtime4j.experimental;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.List;
import java.util.Optional;

/**
 * Experimental exception handler interface for WebAssembly exception handling proposal.
 *
 * <p>This interface provides support for the WebAssembly exception handling proposal, enabling
 * try/catch blocks and exception throwing in WebAssembly modules.
 *
 * @since 1.0.0
 */
public interface ExceptionHandler extends AutoCloseable {

  /** Exception handling result enumeration. */
  enum HandlingResult {
    /** Exception was handled successfully. */
    HANDLED,
    /** Exception was not handled. */
    NOT_HANDLED,
    /** Exception handling failed. */
    FAILED
  }

  /**
   * Handles the given exception.
   *
   * @param exception the exception to handle
   * @return handling result
   */
  HandlingResult handle(Throwable exception);

  /**
   * Gets the handler name.
   *
   * @return handler name
   */
  String getHandlerName();

  /**
   * Checks if the handler is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Creates an exception tag with the given name and parameter types.
   *
   * @param name the tag name
   * @param parameterTypes the parameter types for exceptions with this tag
   * @return the created exception tag
   * @throws IllegalArgumentException if name is null or empty
   */
  ExceptionTag createExceptionTag(String name, List<WasmValueType> parameterTypes);

  /**
   * Gets an exception tag by name.
   *
   * @param name the tag name
   * @return the exception tag if found
   */
  Optional<ExceptionTag> getExceptionTag(String name);

  /**
   * Lists all registered exception tags.
   *
   * @return list of all exception tags
   */
  List<ExceptionTag> listExceptionTags();

  /**
   * Captures a stack trace for debugging purposes.
   *
   * @param tagHandle the exception tag handle
   * @return the stack trace string, or null if not available
   */
  String captureStackTrace(long tagHandle);

  /**
   * Performs exception unwinding.
   *
   * @param currentDepth the current unwind depth
   * @return true if unwinding should continue, false if maximum depth reached
   */
  boolean performUnwinding(int currentDepth);

  /**
   * Gets the configuration for this handler.
   *
   * @return the exception handling configuration
   */
  ExceptionHandlingConfig getConfig();

  /** Closes this exception handler and releases resources. */
  @Override
  void close();

  /** Exception tag for WebAssembly exception handling. */
  interface ExceptionTag {
    /**
     * Gets the unique identifier for this tag.
     *
     * @return the tag handle
     */
    long getTagHandle();

    /**
     * Gets the tag name.
     *
     * @return tag name
     */
    String getTagName();

    /**
     * Gets the tag type description.
     *
     * @return tag type
     */
    String getTagType();

    /**
     * Gets the parameter types for exceptions with this tag.
     *
     * @return list of parameter types
     */
    List<WasmValueType> getParameterTypes();

    /**
     * Checks if this tag supports GC references.
     *
     * @return true if GC-aware
     */
    boolean isGcAware();
  }

  /** Exception handling configuration. */
  interface ExceptionHandlingConfig {
    /**
     * Checks if nested try/catch blocks are enabled.
     *
     * @return true if nested try/catch is enabled
     */
    boolean isNestedTryCatchEnabled();

    /**
     * Checks if exception unwinding is enabled.
     *
     * @return true if exception unwinding is enabled
     */
    boolean isExceptionUnwindingEnabled();

    /**
     * Gets the maximum unwind depth.
     *
     * @return max unwind depth
     */
    int getMaxUnwindDepth();

    /**
     * Checks if exception type validation is enabled.
     *
     * @return true if type validation is enabled
     */
    boolean isExceptionTypeValidationEnabled();

    /**
     * Checks if stack traces are enabled.
     *
     * @return true if stack traces are enabled
     */
    boolean isStackTracesEnabled();

    /**
     * Checks if exception propagation between WASM and host is enabled.
     *
     * @return true if exception propagation is enabled
     */
    boolean isExceptionPropagationEnabled();

    /**
     * Checks if GC integration is enabled for exception payloads.
     *
     * @return true if GC integration is enabled
     */
    boolean isGcIntegrationEnabled();

    /** Builder for exception handling configuration. */
    interface Builder {
      /**
       * Enables or disables nested try/catch blocks.
       *
       * @param enabled true to enable
       * @return this builder
       */
      Builder nestedTryCatch(boolean enabled);

      /**
       * Enables or disables exception unwinding.
       *
       * @param enabled true to enable
       * @return this builder
       */
      Builder exceptionUnwinding(boolean enabled);

      /**
       * Sets the maximum unwind depth.
       *
       * @param maxDepth the maximum depth
       * @return this builder
       * @throws IllegalArgumentException if maxDepth is negative
       */
      Builder maxUnwindDepth(int maxDepth);

      /**
       * Enables or disables exception type validation.
       *
       * @param enabled true to enable
       * @return this builder
       */
      Builder typeValidation(boolean enabled);

      /**
       * Enables or disables stack traces.
       *
       * @param enabled true to enable
       * @return this builder
       */
      Builder stackTraces(boolean enabled);

      /**
       * Enables or disables exception propagation.
       *
       * @param enabled true to enable
       * @return this builder
       */
      Builder exceptionPropagation(boolean enabled);

      /**
       * Enables or disables GC integration.
       *
       * @param enabled true to enable
       * @return this builder
       */
      Builder gcIntegration(boolean enabled);

      /**
       * Builds the configuration.
       *
       * @return the built configuration
       */
      ExceptionHandlingConfig build();
    }
  }
}
