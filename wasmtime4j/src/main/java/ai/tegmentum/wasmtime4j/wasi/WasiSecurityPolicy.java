package ai.tegmentum.wasmtime4j.wasi;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Security policy interface for WASI components.
 *
 * <p>Security policies define access control rules, permitted operations, and security constraints
 * for WASI component execution. Policies are enforced at runtime to ensure components operate
 * within defined security boundaries.
 *
 * <p>Policies can restrict file system access, network operations, resource usage, and other
 * potentially sensitive operations based on component identity and trust level.
 *
 * @since 1.0.0
 */
public interface WasiSecurityPolicy {

  /**
   * Creates a new security policy builder.
   *
   * @return a new WasiSecurityPolicyBuilder instance
   */
  static WasiSecurityPolicyBuilder builder() {
    // This will be implemented by the factory to return appropriate implementation
    throw new UnsupportedOperationException("Implementation provided by factory");
  }

  /**
   * Creates a permissive security policy that allows most operations.
   *
   * @return permissive security policy
   */
  static WasiSecurityPolicy permissive() {
    return builder().withPermissiveMode(true).build();
  }

  /**
   * Creates a restrictive security policy that allows minimal operations.
   *
   * @return restrictive security policy
   */
  static WasiSecurityPolicy restrictive() {
    return builder().withRestrictiveMode(true).build();
  }

  /**
   * Checks if file system access is allowed for the specified path.
   *
   * @param path the path to check
   * @param operation the type of operation (read, write, create, delete)
   * @return true if access is allowed, false otherwise
   */
  boolean isFileSystemAccessAllowed(final Path path, final String operation);

  /**
   * Checks if network access is allowed for the specified host and port.
   *
   * @param host the host to connect to
   * @param port the port number
   * @param protocol the protocol (tcp, udp)
   * @return true if access is allowed, false otherwise
   */
  boolean isNetworkAccessAllowed(final String host, final int port, final String protocol);

  /**
   * Gets the set of allowed file system operations.
   *
   * @return set of allowed operations (read, write, create, delete, list)
   */
  Set<String> getAllowedFileSystemOperations();

  /**
   * Gets the set of allowed network operations.
   *
   * @return set of allowed operations (connect, bind, listen)
   */
  Set<String> getAllowedNetworkOperations();

  /**
   * Gets the list of allowed file system paths.
   *
   * @return list of paths that can be accessed
   */
  List<Path> getAllowedPaths();

  /**
   * Gets the list of blocked file system paths.
   *
   * @return list of paths that are explicitly blocked
   */
  List<Path> getBlockedPaths();

  /**
   * Checks if environment variable access is allowed.
   *
   * @param name the environment variable name
   * @return true if access is allowed, false otherwise
   */
  boolean isEnvironmentVariableAllowed(final String name);

  /**
   * Checks if process spawning is allowed.
   *
   * @return true if process spawning is allowed, false otherwise
   */
  boolean isProcessSpawningAllowed();

  /**
   * Checks if threading is allowed.
   *
   * @return true if threading is allowed, false otherwise
   */
  boolean isThreadingAllowed();

  /**
   * Validates this security policy for completeness and consistency.
   *
   * @throws IllegalArgumentException if the policy is invalid
   */
  void validate();
}