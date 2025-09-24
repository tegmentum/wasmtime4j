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

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Support for WebAssembly exception handling instructions.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API provides support for WebAssembly exception handling
 * instructions including try, catch, throw, and rethrow.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Try block execution with exception catching
 *   <li>Exception throwing with proper payload marshaling
 *   <li>Exception rethrowing and propagation
 *   <li>Nested exception handling support
 *   <li>Integration with WebAssembly instance execution
 * </ul>
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING)
public final class ExceptionInstructions {

  private static final Logger LOGGER = Logger.getLogger(ExceptionInstructions.class.getName());

  // Prevent instantiation
  private ExceptionInstructions() {}

  /** Represents a try block execution context. */
  public static final class TryBlock {
    private final Instance instance;
    private final ExceptionHandler handler;
    private final String functionName;
    private final List<WasmValue> arguments;

    /**
     * Creates a new try block.
     *
     * @param instance the WebAssembly instance
     * @param handler the exception handler
     * @param functionName the function to execute
     * @param arguments the function arguments
     * @throws IllegalArgumentException if any parameter is null
     */
    public TryBlock(
        final Instance instance,
        final ExceptionHandler handler,
        final String functionName,
        final List<WasmValue> arguments) {
      if (instance == null) {
        throw new IllegalArgumentException("Instance cannot be null");
      }
      if (handler == null) {
        throw new IllegalArgumentException("Exception handler cannot be null");
      }
      if (functionName == null || functionName.trim().isEmpty()) {
        throw new IllegalArgumentException("Function name cannot be null or empty");
      }
      if (arguments == null) {
        throw new IllegalArgumentException("Arguments cannot be null");
      }

      this.instance = instance;
      this.handler = handler;
      this.functionName = functionName.trim();
      this.arguments = List.copyOf(arguments);
    }

    /**
     * Executes the try block and returns the result.
     *
     * @return the function execution result
     * @throws ExceptionHandler.WasmException if an exception is thrown and not caught
     * @throws RuntimeException if execution fails
     */
    public List<WasmValue> execute() {
      try {
        LOGGER.fine(
            "Executing try block: " + functionName + " with " + arguments.size() + " arguments");
        return instance.call(functionName, arguments);
      } catch (final ExceptionHandler.WasmException e) {
        LOGGER.fine("WebAssembly exception caught in try block: " + e.getMessage());
        throw e;
      } catch (final Exception e) {
        LOGGER.warning("Unexpected exception in try block: " + e.getMessage());
        throw new RuntimeException("Try block execution failed", e);
      }
    }

    /**
     * Adds a catch block for a specific exception tag.
     *
     * @param tag the exception tag to catch
     * @param catchHandler the handler function for this exception
     * @return a new catch block builder
     * @throws IllegalArgumentException if tag or handler is null
     */
    public CatchBlock catchException(
        final ExceptionHandler.ExceptionTag tag, final ExceptionCatchHandler catchHandler) {
      if (tag == null) {
        throw new IllegalArgumentException("Exception tag cannot be null");
      }
      if (catchHandler == null) {
        throw new IllegalArgumentException("Catch handler cannot be null");
      }

      return new CatchBlock(this, tag, catchHandler);
    }

    /**
     * Gets the WebAssembly instance.
     *
     * @return the instance
     */
    public Instance getInstance() {
      return instance;
    }

    /**
     * Gets the exception handler.
     *
     * @return the exception handler
     */
    public ExceptionHandler getHandler() {
      return handler;
    }

    /**
     * Gets the function name.
     *
     * @return the function name
     */
    public String getFunctionName() {
      return functionName;
    }

    /**
     * Gets the function arguments.
     *
     * @return immutable list of arguments
     */
    public List<WasmValue> getArguments() {
      return arguments;
    }
  }

  /** Represents a catch block for handling specific exceptions. */
  public static final class CatchBlock {
    private final TryBlock tryBlock;
    private final ExceptionHandler.ExceptionTag tag;
    private final ExceptionCatchHandler catchHandler;

    private CatchBlock(
        final TryBlock tryBlock,
        final ExceptionHandler.ExceptionTag tag,
        final ExceptionCatchHandler catchHandler) {
      this.tryBlock = tryBlock;
      this.tag = tag;
      this.catchHandler = catchHandler;
    }

    /**
     * Executes the try-catch block.
     *
     * @return the execution result
     * @throws ExceptionHandler.WasmException if an unhandled exception occurs
     * @throws RuntimeException if execution fails
     */
    public List<WasmValue> execute() {
      try {
        return tryBlock.execute();
      } catch (final ExceptionHandler.WasmException e) {
        if (e.getTag().equals(tag)) {
          LOGGER.fine("Handling exception: " + tag.getName());
          return catchHandler.handle(e.getTag(), e.getPayload());
        } else {
          // Exception doesn't match this catch block, rethrow
          LOGGER.fine("Exception doesn't match catch block, rethrowing: " + e.getTag().getName());
          throw e;
        }
      }
    }

    /**
     * Adds another catch block for a different exception tag.
     *
     * @param tag the exception tag to catch
     * @param catchHandler the handler function for this exception
     * @return a new multi-catch block
     * @throws IllegalArgumentException if tag or handler is null
     */
    public MultiCatchBlock catchException(
        final ExceptionHandler.ExceptionTag tag, final ExceptionCatchHandler catchHandler) {
      if (tag == null) {
        throw new IllegalArgumentException("Exception tag cannot be null");
      }
      if (catchHandler == null) {
        throw new IllegalArgumentException("Catch handler cannot be null");
      }

      return new MultiCatchBlock(tryBlock)
          .addCatch(this.tag, this.catchHandler)
          .addCatch(tag, catchHandler);
    }

    /**
     * Gets the exception tag being caught.
     *
     * @return the exception tag
     */
    public ExceptionHandler.ExceptionTag getTag() {
      return tag;
    }

    /**
     * Gets the catch handler.
     *
     * @return the catch handler
     */
    public ExceptionCatchHandler getCatchHandler() {
      return catchHandler;
    }
  }

  /** Represents a multi-catch block for handling multiple exception types. */
  public static final class MultiCatchBlock {
    private final TryBlock tryBlock;
    private final java.util.Map<ExceptionHandler.ExceptionTag, ExceptionCatchHandler> catchHandlers;

    private MultiCatchBlock(final TryBlock tryBlock) {
      this.tryBlock = tryBlock;
      this.catchHandlers = new java.util.LinkedHashMap<>();
    }

    /**
     * Adds a catch handler for a specific exception tag.
     *
     * @param tag the exception tag
     * @param catchHandler the catch handler
     * @return this multi-catch block
     */
    private MultiCatchBlock addCatch(
        final ExceptionHandler.ExceptionTag tag, final ExceptionCatchHandler catchHandler) {
      catchHandlers.put(tag, catchHandler);
      return this;
    }

    /**
     * Adds another catch block for a different exception tag.
     *
     * @param tag the exception tag to catch
     * @param catchHandler the handler function for this exception
     * @return this multi-catch block
     * @throws IllegalArgumentException if tag or handler is null
     */
    public MultiCatchBlock catchException(
        final ExceptionHandler.ExceptionTag tag, final ExceptionCatchHandler catchHandler) {
      if (tag == null) {
        throw new IllegalArgumentException("Exception tag cannot be null");
      }
      if (catchHandler == null) {
        throw new IllegalArgumentException("Catch handler cannot be null");
      }

      return addCatch(tag, catchHandler);
    }

    /**
     * Executes the try-catch block with multiple catch handlers.
     *
     * @return the execution result
     * @throws ExceptionHandler.WasmException if an unhandled exception occurs
     * @throws RuntimeException if execution fails
     */
    public List<WasmValue> execute() {
      try {
        return tryBlock.execute();
      } catch (final ExceptionHandler.WasmException e) {
        final ExceptionCatchHandler handler = catchHandlers.get(e.getTag());
        if (handler != null) {
          LOGGER.fine("Handling exception with multi-catch: " + e.getTag().getName());
          return handler.handle(e.getTag(), e.getPayload());
        } else {
          // No handler for this exception, rethrow
          LOGGER.fine("No handler for exception, rethrowing: " + e.getTag().getName());
          throw e;
        }
      }
    }

    /**
     * Gets all catch handlers.
     *
     * @return immutable map of catch handlers
     */
    public java.util.Map<ExceptionHandler.ExceptionTag, ExceptionCatchHandler> getCatchHandlers() {
      return java.util.Map.copyOf(catchHandlers);
    }
  }

  /**
   * Creates a new try block for exception handling.
   *
   * @param instance the WebAssembly instance
   * @param handler the exception handler
   * @param functionName the function to execute
   * @param arguments the function arguments
   * @return a new try block
   * @throws IllegalArgumentException if any parameter is null
   */
  public static TryBlock tryExecution(
      final Instance instance,
      final ExceptionHandler handler,
      final String functionName,
      final List<WasmValue> arguments) {
    return new TryBlock(instance, handler, functionName, arguments);
  }

  /**
   * Creates a new try block for exception handling with no arguments.
   *
   * @param instance the WebAssembly instance
   * @param handler the exception handler
   * @param functionName the function to execute
   * @return a new try block
   * @throws IllegalArgumentException if any parameter is null
   */
  public static TryBlock tryExecution(
      final Instance instance, final ExceptionHandler handler, final String functionName) {
    return tryExecution(instance, handler, functionName, List.of());
  }

  /**
   * Creates a new try block for exception handling with a supplier function.
   *
   * @param handler the exception handler
   * @param execution the execution supplier
   * @return a supplier try block
   * @throws IllegalArgumentException if any parameter is null
   */
  public static SupplierTryBlock tryExecution(
      final ExceptionHandler handler, final Supplier<List<WasmValue>> execution) {
    if (handler == null) {
      throw new IllegalArgumentException("Exception handler cannot be null");
    }
    if (execution == null) {
      throw new IllegalArgumentException("Execution supplier cannot be null");
    }

    return new SupplierTryBlock(handler, execution);
  }

  /**
   * Throws a WebAssembly exception with the given tag and payload.
   *
   * @param handler the exception handler
   * @param tag the exception tag
   * @param payload the exception payload
   * @throws IllegalArgumentException if any parameter is null
   * @throws ExceptionHandler.WasmException the thrown exception
   */
  public static void throwException(
      final ExceptionHandler handler,
      final ExceptionHandler.ExceptionTag tag,
      final List<WasmValue> payload) {
    if (handler == null) {
      throw new IllegalArgumentException("Exception handler cannot be null");
    }

    handler.throwException(tag, payload);
  }

  /**
   * Rethrows a caught WebAssembly exception.
   *
   * @param exception the exception to rethrow
   * @throws IllegalArgumentException if exception is null
   * @throws ExceptionHandler.WasmException the rethrown exception
   */
  public static void rethrowException(final ExceptionHandler.WasmException exception) {
    if (exception == null) {
      throw new IllegalArgumentException("Exception cannot be null");
    }

    LOGGER.fine("Rethrowing exception: " + exception.getTag().getName());
    throw exception;
  }

  /** Represents a try block with a supplier function. */
  public static final class SupplierTryBlock {
    private final ExceptionHandler handler;
    private final Supplier<List<WasmValue>> execution;

    private SupplierTryBlock(
        final ExceptionHandler handler, final Supplier<List<WasmValue>> execution) {
      this.handler = handler;
      this.execution = execution;
    }

    /**
     * Executes the try block and returns the result.
     *
     * @return the execution result
     * @throws ExceptionHandler.WasmException if an exception is thrown and not caught
     * @throws RuntimeException if execution fails
     */
    public List<WasmValue> execute() {
      try {
        LOGGER.fine("Executing supplier try block");
        return execution.get();
      } catch (final ExceptionHandler.WasmException e) {
        LOGGER.fine("WebAssembly exception caught in supplier try block: " + e.getMessage());
        throw e;
      } catch (final Exception e) {
        LOGGER.warning("Unexpected exception in supplier try block: " + e.getMessage());
        throw new RuntimeException("Supplier try block execution failed", e);
      }
    }

    /**
     * Adds a catch block for a specific exception tag.
     *
     * @param tag the exception tag to catch
     * @param catchHandler the handler function for this exception
     * @return a new supplier catch block
     * @throws IllegalArgumentException if tag or handler is null
     */
    public SupplierCatchBlock catchException(
        final ExceptionHandler.ExceptionTag tag, final ExceptionCatchHandler catchHandler) {
      if (tag == null) {
        throw new IllegalArgumentException("Exception tag cannot be null");
      }
      if (catchHandler == null) {
        throw new IllegalArgumentException("Catch handler cannot be null");
      }

      return new SupplierCatchBlock(this, tag, catchHandler);
    }

    /**
     * Gets the exception handler.
     *
     * @return the exception handler
     */
    public ExceptionHandler getHandler() {
      return handler;
    }
  }

  /** Represents a catch block for supplier try blocks. */
  public static final class SupplierCatchBlock {
    private final SupplierTryBlock tryBlock;
    private final ExceptionHandler.ExceptionTag tag;
    private final ExceptionCatchHandler catchHandler;

    private SupplierCatchBlock(
        final SupplierTryBlock tryBlock,
        final ExceptionHandler.ExceptionTag tag,
        final ExceptionCatchHandler catchHandler) {
      this.tryBlock = tryBlock;
      this.tag = tag;
      this.catchHandler = catchHandler;
    }

    /**
     * Executes the try-catch block.
     *
     * @return the execution result
     * @throws ExceptionHandler.WasmException if an unhandled exception occurs
     * @throws RuntimeException if execution fails
     */
    public List<WasmValue> execute() {
      try {
        return tryBlock.execute();
      } catch (final ExceptionHandler.WasmException e) {
        if (e.getTag().equals(tag)) {
          LOGGER.fine("Handling exception in supplier catch: " + tag.getName());
          return catchHandler.handle(e.getTag(), e.getPayload());
        } else {
          // Exception doesn't match this catch block, rethrow
          LOGGER.fine(
              "Exception doesn't match supplier catch block, rethrowing: " + e.getTag().getName());
          throw e;
        }
      }
    }
  }

  /** Functional interface for exception catch handlers. */
  @FunctionalInterface
  public interface ExceptionCatchHandler {
    /**
     * Handles a caught WebAssembly exception.
     *
     * @param tag the exception tag
     * @param payload the exception payload
     * @return the result of handling the exception
     * @throws RuntimeException if handling fails
     */
    List<WasmValue> handle(ExceptionHandler.ExceptionTag tag, List<WasmValue> payload);
  }
}
