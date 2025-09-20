package ai.tegmentum.wasmtime4j.security;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * High-security sandbox for executing untrusted WebAssembly code.
 *
 * <p>Provides complete isolation of WebAssembly execution with strict resource limits,
 * capability-based security, and comprehensive monitoring. Designed for executing untrusted or
 * potentially malicious WebAssembly modules safely.
 *
 * <p>Key security features:
 *
 * <ul>
 *   <li>Process-level isolation (optional)
 *   <li>Memory isolation and bounds checking
 *   <li>Execution time limits with preemption
 *   <li>Instruction count limits
 *   <li>Network and filesystem isolation
 *   <li>Capability-based access control
 *   <li>Real-time monitoring and intrusion detection
 * </ul>
 *
 * @since 1.0.0
 */
public interface Sandbox extends Closeable {

  /**
   * Creates a new sandbox builder with secure default settings.
   *
   * @return a new SandboxBuilder instance
   */
  static SandboxBuilder builder() {
    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.security.PanamaSandboxBuilder");
      return (SandboxBuilder) builderClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> builderClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.security.JniSandboxBuilder");
        return (SandboxBuilder) builderClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        throw new UnsupportedOperationException(
            "No SandboxBuilder implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create sandbox builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create sandbox builder", e);
    }
  }

  /**
   * Creates a minimal security sandbox for basic isolation.
   *
   * <p>Suitable for trusted code that needs basic resource limits.
   *
   * @return minimal sandbox configuration
   */
  static Sandbox minimal() {
    return builder()
        .withResourceLimits(ResourceLimits.minimal())
        .withSecurityPolicy(SecurityPolicy.restrictive())
        .withProcessIsolation(false)
        .build();
  }

  /**
   * Creates a standard security sandbox for general use.
   *
   * <p>Provides good balance between security and functionality.
   *
   * @return standard sandbox configuration
   */
  static Sandbox standard() {
    return builder()
        .withResourceLimits(ResourceLimits.standard())
        .withSecurityPolicy(SecurityPolicy.restrictive())
        .withProcessIsolation(false)
        .withMonitoring(true)
        .build();
  }

  /**
   * Creates a maximum security sandbox for untrusted code.
   *
   * <p>Provides maximum isolation and security for executing completely untrusted WebAssembly
   * modules.
   *
   * @return maximum security sandbox configuration
   */
  static Sandbox maxSecurity() {
    return builder()
        .withResourceLimits(ResourceLimits.minimal())
        .withSecurityPolicy(SecurityPolicy.sandbox())
        .withProcessIsolation(true)
        .withMonitoring(true)
        .withIntrusionDetection(true)
        .withTimeouts(Duration.ofSeconds(30))
        .build();
  }

  /**
   * Gets the current sandbox state.
   *
   * @return sandbox state
   */
  SandboxState getState();

  /**
   * Gets the security policy enforced by this sandbox.
   *
   * @return security policy
   */
  SecurityPolicy getSecurityPolicy();

  /**
   * Gets the resource limits enforced by this sandbox.
   *
   * @return resource limits
   */
  ResourceLimits getResourceLimits();

  /**
   * Gets the sandbox configuration.
   *
   * @return sandbox configuration
   */
  SandboxConfig getConfig();

  /**
   * Loads a WebAssembly module into the sandbox.
   *
   * <p>The module is validated against the security policy before loading. Only modules that pass
   * all security checks are allowed.
   *
   * @param moduleBytes the WebAssembly module bytes
   * @param moduleSource optional source identifier for auditing
   * @return loaded module within sandbox context
   * @throws SandboxException if loading fails or violates security policy
   */
  SandboxModule loadModule(final byte[] moduleBytes, final String moduleSource)
      throws SandboxException;

  /**
   * Creates a WebAssembly instance from a loaded module.
   *
   * <p>Instance creation is subject to resource limits and security policy.
   *
   * @param module the module to instantiate
   * @return instance within sandbox context
   * @throws SandboxException if instantiation fails or violates limits
   */
  SandboxInstance createInstance(final SandboxModule module) throws SandboxException;

  /**
   * Executes a function within the sandbox with timeout and monitoring.
   *
   * @param instance the instance containing the function
   * @param functionName the name of the function to execute
   * @param parameters the function parameters
   * @param timeout maximum execution time
   * @return execution result
   * @throws SandboxException if execution fails, times out, or violates policy
   */
  SandboxExecutionResult execute(
      final SandboxInstance instance,
      final String functionName,
      final Object[] parameters,
      final Duration timeout)
      throws SandboxException;

  /**
   * Executes a function asynchronously with cancellation support.
   *
   * @param instance the instance containing the function
   * @param functionName the name of the function to execute
   * @param parameters the function parameters
   * @param timeout maximum execution time
   * @return future representing the execution result
   */
  CompletableFuture<SandboxExecutionResult> executeAsync(
      final SandboxInstance instance,
      final String functionName,
      final Object[] parameters,
      final Duration timeout);

  /**
   * Gets real-time resource usage statistics.
   *
   * @return current resource usage
   */
  ResourceUsage getResourceUsage();

  /**
   * Gets security events and audit trail.
   *
   * @return security events since last call
   */
  SecurityAuditLog getSecurityEvents();

  /**
   * Checks if the sandbox has detected any security violations.
   *
   * @return true if violations detected, false otherwise
   */
  boolean hasSecurityViolations();

  /**
   * Pauses execution within the sandbox.
   *
   * <p>All running instances are paused until resume() is called.
   *
   * @throws SandboxException if pause operation fails
   */
  void pause() throws SandboxException;

  /**
   * Resumes execution within the sandbox.
   *
   * @throws SandboxException if resume operation fails
   */
  void resume() throws SandboxException;

  /**
   * Immediately terminates all execution within the sandbox.
   *
   * <p>This is a forceful operation that stops all running instances and cleans up resources.
   */
  void terminate();

  /**
   * Resets the sandbox to initial state.
   *
   * <p>Clears all loaded modules, instances, and resets resource counters. Security policy and
   * configuration remain unchanged.
   *
   * @throws SandboxException if reset operation fails
   */
  void reset() throws SandboxException;

  /**
   * Validates the sandbox integrity and security state.
   *
   * <p>Performs comprehensive validation including:
   *
   * <ul>
   *   <li>Resource limit compliance
   *   <li>Security policy enforcement
   *   <li>Memory integrity checks
   *   <li>Process isolation validation
   * </ul>
   *
   * @return validation result
   */
  SandboxValidationResult validate();

  /**
   * Gets the sandbox performance metrics.
   *
   * @return performance metrics
   */
  SandboxMetrics getMetrics();

  /**
   * Closes the sandbox and releases all resources.
   *
   * <p>After closing, the sandbox cannot be used for further operations. All running instances are
   * terminated gracefully.
   */
  @Override
  void close();
}
