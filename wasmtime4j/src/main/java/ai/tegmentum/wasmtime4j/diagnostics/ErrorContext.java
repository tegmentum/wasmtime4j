package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Map;
import java.util.Optional;

/**
 * Contextual information about where and when an error occurred.
 *
 * <p>This interface provides detailed context about the execution environment when an error
 * occurred, including module, function, and instruction-level information.
 *
 * @since 1.0.0
 */
public interface ErrorContext {

  /**
   * Gets the module name where the error occurred.
   *
   * @return the module name, or empty if not available
   */
  Optional<String> getModuleName();

  /**
   * Gets the function name where the error occurred.
   *
   * @return the function name, or empty if not available
   */
  Optional<String> getFunctionName();

  /**
   * Gets the function index where the error occurred.
   *
   * @return the function index, or empty if not available
   */
  Optional<Integer> getFunctionIndex();

  /**
   * Gets the instruction offset where the error occurred.
   *
   * @return the instruction offset, or empty if not available
   */
  Optional<Long> getInstructionOffset();

  /**
   * Gets the source location if debug information is available.
   *
   * @return the source location, or empty if not available
   */
  Optional<SourceLocation> getSourceLocation();

  /**
   * Gets the execution thread information.
   *
   * @return the thread information, or empty if not available
   */
  Optional<ThreadInfo> getThreadInfo();

  /**
   * Gets the memory state at the time of error.
   *
   * @return the memory state, or empty if not available
   */
  Optional<MemoryState> getMemoryState();

  /**
   * Gets the call stack at the time of error.
   *
   * @return the call stack, or empty if not available
   */
  Optional<WasmStackTrace> getCallStack();

  /**
   * Gets additional context properties.
   *
   * @return map of context properties
   */
  Map<String, Object> getProperties();

  /**
   * Gets the runtime environment information.
   *
   * @return the runtime environment info
   */
  RuntimeEnvironment getRuntimeEnvironment();

  /**
   * Creates a builder for constructing ErrorContext instances.
   *
   * @return a new context builder
   */
  static ErrorContextBuilder builder() {
    return new ErrorContextBuilder();
  }
}
