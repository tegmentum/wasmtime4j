package ai.tegmentum.wasmtime4j.async.reactive;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * Represents an event in the WebAssembly function execution process.
 *
 * <p>ExecutionEvent provides detailed information about function calls, their parameters,
 * results, and execution characteristics. These events are designed to work with reactive
 * streams for real-time monitoring of WebAssembly function execution.
 *
 * <p>Events are immutable and contain all relevant information about the execution state
 * at the time the event was created.
 *
 * @since 1.0.0
 */
public interface ExecutionEvent {

  /**
   * Gets the unique identifier for this execution.
   *
   * @return the execution identifier
   */
  String getExecutionId();

  /**
   * Gets the name of the function being executed.
   *
   * @return the function name
   */
  String getFunctionName();

  /**
   * Gets the execution phase.
   *
   * @return the execution phase
   */
  ExecutionPhase getPhase();

  /**
   * Gets the parameters passed to the function.
   *
   * @return array of function parameters, or empty array if not available
   */
  WasmValue[] getParameters();

  /**
   * Gets the results returned by the function.
   *
   * @return array of function results, or empty array if not available
   */
  WasmValue[] getResults();

  /**
   * Gets the error that occurred during execution, if any.
   *
   * @return the error, or empty if no error occurred
   */
  Optional<Exception> getError();

  /**
   * Gets the execution time for the function call.
   *
   * @return execution duration
   */
  Duration getExecutionTime();

  /**
   * Gets the timestamp when this event was created.
   *
   * @return the event timestamp
   */
  Instant getTimestamp();

  /**
   * Gets the number of instructions executed.
   *
   * @return instruction count, or -1 if not available
   */
  long getInstructionCount();

  /**
   * Gets the memory usage during execution.
   *
   * @return memory usage in bytes, or -1 if not available
   */
  long getMemoryUsage();

  /**
   * Gets the stack depth during execution.
   *
   * @return stack depth, or -1 if not available
   */
  int getStackDepth();

  /**
   * Gets additional execution context information.
   *
   * @return execution context, or empty if not available
   */
  Optional<String> getExecutionContext();

  /**
   * Checks if this event represents a successful execution.
   *
   * @return true if execution completed successfully
   */
  boolean isSuccessful();

  /**
   * Checks if this event represents a failed execution.
   *
   * @return true if execution failed
   */
  boolean isFailed();

  /**
   * Checks if this event represents the start of execution.
   *
   * @return true if this is an execution start event
   */
  boolean isExecutionStart();

  /**
   * Checks if this event represents the end of execution.
   *
   * @return true if this is an execution end event
   */
  boolean isExecutionEnd();

  // Factory methods for creating events

  /**
   * Creates an execution start event.
   *
   * @param executionId the execution identifier
   * @param functionName the function name
   * @param parameters the function parameters
   * @return a new execution start event
   */
  static ExecutionEvent executionStart(
      final String executionId, final String functionName, final WasmValue[] parameters) {
    return new ExecutionEventImpl(
        executionId, functionName, ExecutionPhase.STARTING, parameters, new WasmValue[0], null,
        Duration.ZERO, Instant.now(), -1, -1, -1, null, false, false, true, false);
  }

  /**
   * Creates a successful execution completion event.
   *
   * @param executionId the execution identifier
   * @param functionName the function name
   * @param results the function results
   * @param executionTime the execution duration
   * @return a new successful execution event
   */
  static ExecutionEvent executionSuccess(
      final String executionId,
      final String functionName,
      final WasmValue[] results,
      final Duration executionTime) {
    return new ExecutionEventImpl(
        executionId, functionName, ExecutionPhase.COMPLETED, new WasmValue[0], results, null,
        executionTime, Instant.now(), -1, -1, -1, null, true, false, false, true);
  }

  /**
   * Creates a failed execution event.
   *
   * @param executionId the execution identifier
   * @param functionName the function name
   * @param error the error that caused the failure
   * @param executionTime the execution duration before failure
   * @return a new failed execution event
   */
  static ExecutionEvent executionFailure(
      final String executionId,
      final String functionName,
      final Exception error,
      final Duration executionTime) {
    return new ExecutionEventImpl(
        executionId, functionName, ExecutionPhase.FAILED, new WasmValue[0], new WasmValue[0], error,
        executionTime, Instant.now(), -1, -1, -1, null, false, true, false, true);
  }

  /**
   * Creates a detailed execution event with performance metrics.
   *
   * @param executionId the execution identifier
   * @param functionName the function name
   * @param phase the execution phase
   * @param parameters the function parameters
   * @param results the function results
   * @param executionTime the execution duration
   * @param instructionCount the number of instructions executed
   * @param memoryUsage the memory usage during execution
   * @param stackDepth the maximum stack depth
   * @return a new detailed execution event
   */
  static ExecutionEvent detailedExecution(
      final String executionId,
      final String functionName,
      final ExecutionPhase phase,
      final WasmValue[] parameters,
      final WasmValue[] results,
      final Duration executionTime,
      final long instructionCount,
      final long memoryUsage,
      final int stackDepth) {
    return new ExecutionEventImpl(
        executionId, functionName, phase, parameters, results, null, executionTime, Instant.now(),
        instructionCount, memoryUsage, stackDepth, null, phase == ExecutionPhase.COMPLETED,
        phase == ExecutionPhase.FAILED, false, false);
  }

  /** Default implementation of ExecutionEvent. */
  final class ExecutionEventImpl implements ExecutionEvent {
    private final String executionId;
    private final String functionName;
    private final ExecutionPhase phase;
    private final WasmValue[] parameters;
    private final WasmValue[] results;
    private final Exception error;
    private final Duration executionTime;
    private final Instant timestamp;
    private final long instructionCount;
    private final long memoryUsage;
    private final int stackDepth;
    private final String executionContext;
    private final boolean successful;
    private final boolean failed;
    private final boolean executionStart;
    private final boolean executionEnd;

    ExecutionEventImpl(
        final String executionId,
        final String functionName,
        final ExecutionPhase phase,
        final WasmValue[] parameters,
        final WasmValue[] results,
        final Exception error,
        final Duration executionTime,
        final Instant timestamp,
        final long instructionCount,
        final long memoryUsage,
        final int stackDepth,
        final String executionContext,
        final boolean successful,
        final boolean failed,
        final boolean executionStart,
        final boolean executionEnd) {
      this.executionId = executionId;
      this.functionName = functionName;
      this.phase = phase;
      this.parameters = parameters != null ? Arrays.copyOf(parameters, parameters.length) : new WasmValue[0];
      this.results = results != null ? Arrays.copyOf(results, results.length) : new WasmValue[0];
      this.error = error;
      this.executionTime = executionTime;
      this.timestamp = timestamp;
      this.instructionCount = instructionCount;
      this.memoryUsage = memoryUsage;
      this.stackDepth = stackDepth;
      this.executionContext = executionContext;
      this.successful = successful;
      this.failed = failed;
      this.executionStart = executionStart;
      this.executionEnd = executionEnd;
    }

    @Override
    public String getExecutionId() {
      return executionId;
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }

    @Override
    public ExecutionPhase getPhase() {
      return phase;
    }

    @Override
    public WasmValue[] getParameters() {
      return Arrays.copyOf(parameters, parameters.length);
    }

    @Override
    public WasmValue[] getResults() {
      return Arrays.copyOf(results, results.length);
    }

    @Override
    public Optional<Exception> getError() {
      return Optional.ofNullable(error);
    }

    @Override
    public Duration getExecutionTime() {
      return executionTime;
    }

    @Override
    public Instant getTimestamp() {
      return timestamp;
    }

    @Override
    public long getInstructionCount() {
      return instructionCount;
    }

    @Override
    public long getMemoryUsage() {
      return memoryUsage;
    }

    @Override
    public int getStackDepth() {
      return stackDepth;
    }

    @Override
    public Optional<String> getExecutionContext() {
      return Optional.ofNullable(executionContext);
    }

    @Override
    public boolean isSuccessful() {
      return successful;
    }

    @Override
    public boolean isFailed() {
      return failed;
    }

    @Override
    public boolean isExecutionStart() {
      return executionStart;
    }

    @Override
    public boolean isExecutionEnd() {
      return executionEnd;
    }

    @Override
    public String toString() {
      return String.format(
          "ExecutionEvent{executionId='%s', functionName='%s', phase=%s, executionTime=%s, successful=%s, failed=%s}",
          executionId, functionName, phase, executionTime, successful, failed);
    }
  }
}