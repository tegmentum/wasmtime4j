package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution context interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionContext {

  /**
   * Gets the context ID.
   *
   * @return context ID
   */
  String getContextId();

  /**
   * Gets the execution environment.
   *
   * @return execution environment
   */
  ExecutionEnvironment getEnvironment();

  /**
   * Gets execution parameters.
   *
   * @return execution parameters
   */
  java.util.Map<String, Object> getParameters();

  /**
   * Sets an execution parameter.
   *
   * @param key parameter key
   * @param value parameter value
   */
  void setParameter(String key, Object value);

  /**
   * Gets execution configuration.
   *
   * @return execution configuration
   */
  ExecutionContextConfig getConfig();

  /**
   * Gets the security context as a generic object.
   *
   * @return security context or null
   */
  Object getSecurityContext();

  /**
   * Gets execution metadata.
   *
   * @return metadata
   */
  ExecutionMetadata getMetadata();

  /**
   * Checks if the context is valid.
   *
   * @return true if valid
   */
  boolean isValid();

  /** Execution environment interface. */
  interface ExecutionEnvironment {
    /**
     * Gets environment variables.
     *
     * @return environment variables
     */
    java.util.Map<String, String> getEnvironmentVariables();

    /**
     * Gets the working directory.
     *
     * @return working directory path
     */
    String getWorkingDirectory();

    /**
     * Gets resource limits.
     *
     * @return resource limits
     */
    ResourceLimits getResourceLimits();
  }

  /** Resource limits interface. */
  interface ResourceLimits {
    /**
     * Gets the maximum memory limit in bytes.
     *
     * @return memory limit
     */
    long getMaxMemory();

    /**
     * Gets the maximum execution time in milliseconds.
     *
     * @return execution time limit
     */
    long getMaxExecutionTime();

    /**
     * Gets the maximum CPU time in milliseconds.
     *
     * @return CPU time limit
     */
    long getMaxCpuTime();
  }

  /** Execution metadata interface. */
  interface ExecutionMetadata {
    /**
     * Gets the creation timestamp.
     *
     * @return creation time
     */
    long getCreationTime();

    /**
     * Gets the last access timestamp.
     *
     * @return last access time
     */
    long getLastAccessTime();

    /**
     * Gets the creator information.
     *
     * @return creator info
     */
    String getCreator();

    /**
     * Gets metadata tags.
     *
     * @return metadata tags
     */
    java.util.Set<String> getTags();
  }
}
