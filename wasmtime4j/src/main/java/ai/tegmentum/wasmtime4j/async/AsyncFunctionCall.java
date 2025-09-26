package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Represents an asynchronous WebAssembly function call.
 *
 * <p>This class encapsulates all the information needed to execute a function
 * asynchronously, including function name, arguments, timeout, and execution options.
 *
 * @since 1.0.0
 */
public final class AsyncFunctionCall {

  private final String functionName;
  private final WasmValue[] args;
  private final Duration timeout;
  private final Executor executor;
  private final boolean enableCaching;
  private final String callId;
  private final int priority;

  /**
   * Creates a new async function call.
   *
   * @param functionName name of the function to call
   * @param args function arguments
   * @param timeout optional timeout for the call
   * @param executor optional custom executor
   * @param enableCaching whether to enable result caching
   * @param callId optional call identifier
   * @param priority call priority (higher values = higher priority)
   */
  public AsyncFunctionCall(
      final String functionName,
      final WasmValue[] args,
      final Duration timeout,
      final Executor executor,
      final boolean enableCaching,
      final String callId,
      final int priority) {
    this.functionName = functionName;
    this.args = args != null ? args.clone() : new WasmValue[0];
    this.timeout = timeout;
    this.executor = executor;
    this.enableCaching = enableCaching;
    this.callId = callId != null ? callId : generateCallId();
    this.priority = priority;
  }

  /**
   * Creates a simple async function call.
   *
   * @param functionName name of the function to call
   * @param args function arguments
   * @return new async function call
   */
  public static AsyncFunctionCall of(final String functionName, final WasmValue... args) {
    return new AsyncFunctionCall(
        functionName, args, null, null, false, null, 0);
  }

  /**
   * Creates an async function call with timeout.
   *
   * @param functionName name of the function to call
   * @param timeout timeout for the call
   * @param args function arguments
   * @return new async function call
   */
  public static AsyncFunctionCall withTimeout(
      final String functionName, final Duration timeout, final WasmValue... args) {
    return new AsyncFunctionCall(
        functionName, args, timeout, null, false, null, 0);
  }

  /**
   * Creates an async function call with custom executor.
   *
   * @param functionName name of the function to call
   * @param executor custom executor
   * @param args function arguments
   * @return new async function call
   */
  public static AsyncFunctionCall withExecutor(
      final String functionName, final Executor executor, final WasmValue... args) {
    return new AsyncFunctionCall(
        functionName, args, null, executor, false, null, 0);
  }

  /**
   * Creates an async function call with caching enabled.
   *
   * @param functionName name of the function to call
   * @param args function arguments
   * @return new async function call with caching
   */
  public static AsyncFunctionCall withCaching(
      final String functionName, final WasmValue... args) {
    return new AsyncFunctionCall(
        functionName, args, null, null, true, null, 0);
  }

  /**
   * Creates a high-priority async function call.
   *
   * @param functionName name of the function to call
   * @param priority call priority
   * @param args function arguments
   * @return new high-priority async function call
   */
  public static AsyncFunctionCall withPriority(
      final String functionName, final int priority, final WasmValue... args) {
    return new AsyncFunctionCall(
        functionName, args, null, null, false, null, priority);
  }

  /**
   * Gets the function name.
   *
   * @return function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the function arguments.
   *
   * @return function arguments array (defensive copy)
   */
  public WasmValue[] getArgs() {
    return args.clone();
  }

  /**
   * Gets the call timeout.
   *
   * @return timeout duration, or null if no timeout
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Gets the custom executor.
   *
   * @return executor, or null if using default
   */
  public Executor getExecutor() {
    return executor;
  }

  /**
   * Checks if result caching is enabled.
   *
   * @return true if caching is enabled
   */
  public boolean isCachingEnabled() {
    return enableCaching;
  }

  /**
   * Gets the call identifier.
   *
   * @return call ID
   */
  public String getCallId() {
    return callId;
  }

  /**
   * Gets the call priority.
   *
   * @return call priority
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Checks if the call has a timeout.
   *
   * @return true if a timeout is set
   */
  public boolean hasTimeout() {
    return timeout != null;
  }

  /**
   * Checks if the call uses a custom executor.
   *
   * @return true if a custom executor is set
   */
  public boolean hasCustomExecutor() {
    return executor != null;
  }

  /**
   * Creates a builder for this async function call.
   *
   * @return new builder
   */
  public Builder toBuilder() {
    return new Builder(functionName)
        .args(args)
        .timeout(timeout)
        .executor(executor)
        .caching(enableCaching)
        .callId(callId)
        .priority(priority);
  }

  private static String generateCallId() {
    return "call-" + System.nanoTime();
  }

  @Override
  public String toString() {
    return String.format(
        "AsyncFunctionCall{function='%s', args=%d, timeout=%s, priority=%d, caching=%s}",
        functionName, args.length, timeout, priority, enableCaching);
  }

  /**
   * Builder for AsyncFunctionCall instances.
   */
  public static final class Builder {
    private final String functionName;
    private WasmValue[] args = new WasmValue[0];
    private Duration timeout;
    private Executor executor;
    private boolean enableCaching = false;
    private String callId;
    private int priority = 0;

    public Builder(final String functionName) {
      this.functionName = functionName;
    }

    public Builder args(final WasmValue... args) {
      this.args = args != null ? args.clone() : new WasmValue[0];
      return this;
    }

    public Builder timeout(final Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder executor(final Executor executor) {
      this.executor = executor;
      return this;
    }

    public Builder caching(final boolean enableCaching) {
      this.enableCaching = enableCaching;
      return this;
    }

    public Builder callId(final String callId) {
      this.callId = callId;
      return this;
    }

    public Builder priority(final int priority) {
      this.priority = priority;
      return this;
    }

    public AsyncFunctionCall build() {
      return new AsyncFunctionCall(
          functionName, args, timeout, executor, enableCaching, callId, priority);
    }
  }
}