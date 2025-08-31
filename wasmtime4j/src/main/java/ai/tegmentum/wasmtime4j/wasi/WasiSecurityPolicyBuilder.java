package ai.tegmentum.wasmtime4j.wasi;

import java.nio.file.Path;
import java.util.Set;

/**
 * Builder for creating WASI security policies.
 *
 * @since 1.0.0
 */
public interface WasiSecurityPolicyBuilder {

  /**
   * Enables permissive mode (allows most operations).
   *
   * @param permissive true for permissive mode
   * @return this builder for method chaining
   */
  WasiSecurityPolicyBuilder withPermissiveMode(final boolean permissive);

  /**
   * Enables restrictive mode (minimal permissions).
   *
   * @param restrictive true for restrictive mode
   * @return this builder for method chaining
   */
  WasiSecurityPolicyBuilder withRestrictiveMode(final boolean restrictive);

  /**
   * Allows access to the specified file system path.
   *
   * @param path the path to allow
   * @return this builder for method chaining
   */
  WasiSecurityPolicyBuilder withAllowedPath(final Path path);

  /**
   * Blocks access to the specified file system path.
   *
   * @param path the path to block
   * @return this builder for method chaining
   */
  WasiSecurityPolicyBuilder withBlockedPath(final Path path);

  /**
   * Sets allowed file system operations.
   *
   * @param operations the allowed operations
   * @return this builder for method chaining
   */
  WasiSecurityPolicyBuilder withAllowedFileSystemOperations(final Set<String> operations);

  /**
   * Sets allowed network operations.
   *
   * @param operations the allowed operations
   * @return this builder for method chaining
   */
  WasiSecurityPolicyBuilder withAllowedNetworkOperations(final Set<String> operations);

  /**
   * Creates the configured security policy.
   *
   * @return a configured WasiSecurityPolicy instance
   */
  WasiSecurityPolicy build();
}