package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution request interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionRequest {

  /**
   * Gets the request ID.
   *
   * @return request ID
   */
  String getId();

  /**
   * Gets the function name to execute.
   *
   * @return function name
   */
  String getFunctionName();

  /**
   * Gets the function arguments.
   *
   * @return function arguments
   */
  Object[] getArguments();

  /**
   * Gets the execution context.
   *
   * @return execution context
   */
  ExecutionContext getContext();

  /**
   * Gets the execution quotas.
   *
   * @return execution quotas
   */
  ExecutionQuotas getQuotas();

  /**
   * Gets the execution policy.
   *
   * @return execution policy
   */
  ExecutionPolicy getPolicy();

  /**
   * Gets the request priority.
   *
   * @return request priority
   */
  RequestPriority getPriority();

  /**
   * Gets the request timeout in milliseconds.
   *
   * @return timeout
   */
  long getTimeout();

  /**
   * Gets request metadata.
   *
   * @return metadata map
   */
  java.util.Map<String, Object> getMetadata();

  /**
   * Gets the request timestamp.
   *
   * @return timestamp
   */
  long getTimestamp();

  /**
   * Gets the caller information.
   *
   * @return caller info
   */
  CallerInfo getCallerInfo();

  /** Request priority enumeration. */
  enum RequestPriority {
    /** Low priority. */
    LOW,
    /** Normal priority. */
    NORMAL,
    /** High priority. */
    HIGH,
    /** Critical priority. */
    CRITICAL
  }

  /** Caller information interface. */
  interface CallerInfo {
    /**
     * Gets the caller ID.
     *
     * @return caller ID
     */
    String getId();

    /**
     * Gets the caller type.
     *
     * @return caller type
     */
    String getType();

    /**
     * Gets the caller permissions.
     *
     * @return permissions
     */
    java.util.Set<String> getPermissions();
  }
}
