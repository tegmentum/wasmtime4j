package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;

/**
 * Resource limits for sandboxed execution.
 *
 * @since 1.0.0
 */
public interface ResourceLimits {

  /**
   * Gets maximum memory in bytes.
   *
   * @return max memory
   */
  long getMaxMemory();

  /**
   * Gets maximum CPU time.
   *
   * @return max CPU time
   */
  Duration getMaxCpuTime();

  /**
   * Gets maximum execution duration.
   *
   * @return max duration
   */
  Duration getMaxDuration();

  /**
   * Gets maximum instructions.
   *
   * @return max instructions
   */
  long getMaxInstructions();

  /**
   * Gets maximum system calls.
   *
   * @return max syscalls
   */
  long getMaxSyscalls();

  /**
   * Gets maximum file operations.
   *
   * @return max file ops
   */
  long getMaxFileOps();

  /**
   * Gets maximum network operations.
   *
   * @return max network ops
   */
  long getMaxNetworkOps();
}
