package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiPermissionException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Collections;
import java.util.HashSet;import java.util.Map;
import java.util.Collections;
import java.util.HashSet;import java.util.Set;
import java.util.Collections;
import java.util.HashSet;import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.HashSet;import java.util.logging.Logger;
import java.util.Collections;
import java.util.HashSet;
/**
 * Configurable directory access control system for WASI file operations.
 *
 * <p>This class provides comprehensive directory-level access controls that work in conjunction
 * with the WASI permission system to enforce fine-grained security policies. Features include:
 *
 * <ul>
 *   <li>Per-directory permission configuration with inheritance
 *   <li>Path-based access rules with wildcard support
 *   <li>Operation-specific directory permissions
 *   <li>Recursive permission inheritance from parent directories
 *   <li>Real-time permission validation with path normalization
 *   <li>Comprehensive audit logging for access attempts
 * </ul>
 *
 * <p>All access decisions are made with security as the highest priority - access is denied by
 * default and must be explicitly granted through configuration.
 *
 * @since 1.0.0
 */
public final class WasiDirectoryAccessControl {

  private static final Logger LOGGER = Logger.getLogger(WasiDirectoryAccessControl.class.getName());

  /** Maximum directory path length to prevent resource exhaustion. */
  private static final int MAX_DIRECTORY_PATH_LENGTH = 4096;

  /** Maximum number of directory rules to prevent resource exhaustion. */
  private static final int MAX_DIRECTORY_RULES = 1000;

  /** Directory-specific access rules. */
  private final Map<Path, DirectoryAccessRule> directoryRules = new ConcurrentHashMap<>();

  /** Global default permissions for directories not explicitly configured. */
  private final Set<WasiFileOperation> globalDefaultPermissions =
      EnumSet.noneOf(WasiFileOperation.class);

  /** Whether to allow access to subdirectories by inheritance. */
  private final boolean allowInheritance;

  /** Whether to perform strict path validation. */
  private final boolean strictPathValidation;

  /** Whether to log all access attempts for auditing. */
  private final boolean auditLogging;

  /**
   * Creates a new directory access control system with the specified configuration.
   *
   * @param builder the access control builder
   */
  private WasiDirectoryAccessControl(final Builder builder) {
    this.directoryRules.putAll(builder.directoryRules);
    this.globalDefaultPermissions.addAll(builder.globalDefaultPermissions);
    this.allowInheritance = builder.allowInheritance;
    this.strictPathValidation = builder.strictPathValidation;
    this.auditLogging = builder.auditLogging;

    LOGGER.info(
        String.format(
            "Created directory access control: %d rules, inheritance=%s, strict=%s, audit=%s",
            directoryRules.size(), allowInheritance, strictPathValidation, auditLogging));
  }

  /**
   * Validates that access to the specified directory path is allowed for the given operation.
   *
   * @param path the directory path to validate
   * @param operation the file operation to perform
   * @throws WasiPermissionException if access is denied
   */
  public void validateDirectoryAccess(final Path path, final WasiFileOperation operation) {
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNull(operation, "operation");

    final Path normalizedPath = normalizePath(path);

    if (auditLogging) {
      LOGGER.info(
          String.format(
              "Directory access validation: path=%s, operation=%s", normalizedPath, operation));
    }

    // Basic path validation
    validatePathBasics(normalizedPath);

    // Check for explicit directory rule
    final DirectoryAccessRule explicitRule = directoryRules.get(normalizedPath);
    if (explicitRule != null) {
      validateOperationAccess(explicitRule, operation, normalizedPath);
      return;
    }

    // Check for inherited permissions if enabled
    if (allowInheritance) {
      final DirectoryAccessRule inheritedRule = findInheritedRule(normalizedPath);
      if (inheritedRule != null) {
        validateOperationAccess(inheritedRule, operation, normalizedPath);
        return;
      }
    }

    // Check global default permissions
    if (globalDefaultPermissions.contains(operation)) {
      if (auditLogging) {
        LOGGER.info(
            String.format(
                "Directory access granted by global default: path=%s, operation=%s",
                normalizedPath, operation));
      }
      return;
    }

    // Access denied - no explicit permission found
    final String message =
        String.format(
            "Directory access denied: no permission for operation %s on path %s",
            operation, normalizedPath);
    LOGGER.warning(message);
    throw new WasiPermissionException(message);
  }

  /**
   * Grants permissions for a specific directory path.
   *
   * @param directoryPath the directory path
   * @param permissions the permissions to grant
   * @param recursive whether permissions apply recursively to subdirectories
   */
  public void grantDirectoryPermissions(
      final String directoryPath,
      final Set<WasiFileOperation> permissions,
      final boolean recursive) {
    JniValidation.requireNonEmpty(directoryPath, "directoryPath");
    JniValidation.requireNonNull(permissions, "permissions");

    if (directoryRules.size() >= MAX_DIRECTORY_RULES) {
      throw new JniException(
          "Too many directory rules - maximum " + MAX_DIRECTORY_RULES + " exceeded");
    }

    final Path normalizedPath = normalizePath(Paths.get(directoryPath));
    validatePathBasics(normalizedPath);

    final DirectoryAccessRule rule =
        new DirectoryAccessRule(EnumCollections.unmodifiableSet(new HashSet<>(permissions)), recursive, true);

    directoryRules.put(normalizedPath, rule);

    LOGGER.info(
        String.format(
            "Granted directory permissions: path=%s, permissions=%s, recursive=%s",
            normalizedPath, permissions, recursive));
  }

  /**
   * Revokes permissions for a specific directory path.
   *
   * @param directoryPath the directory path
   * @param permissions the permissions to revoke
   */
  public void revokeDirectoryPermissions(
      final String directoryPath, final Set<WasiFileOperation> permissions) {
    JniValidation.requireNonEmpty(directoryPath, "directoryPath");
    JniValidation.requireNonNull(permissions, "permissions");

    final Path normalizedPath = normalizePath(Paths.get(directoryPath));

    final DirectoryAccessRule existingRule = directoryRules.get(normalizedPath);
    if (existingRule != null) {
      final Set<WasiFileOperation> remainingPermissions = EnumCollections.unmodifiableSet(new HashSet<>(existingRule.permissions));
      remainingPermissions.removeAll(permissions);

      if (remainingPermissions.isEmpty()) {
        directoryRules.remove(normalizedPath);
        LOGGER.info(String.format("Removed all directory permissions: path=%s", normalizedPath));
      } else {
        final DirectoryAccessRule updatedRule =
            new DirectoryAccessRule(
                remainingPermissions, existingRule.recursive, existingRule.enabled);
        directoryRules.put(normalizedPath, updatedRule);
        LOGGER.info(
            String.format(
                "Revoked directory permissions: path=%s, revoked=%s, remaining=%s",
                normalizedPath, permissions, remainingPermissions));
      }
    }
  }

  /**
   * Enables or disables permissions for a specific directory path.
   *
   * @param directoryPath the directory path
   * @param enabled whether to enable or disable the rule
   */
  public void setDirectoryRuleEnabled(final String directoryPath, final boolean enabled) {
    JniValidation.requireNonEmpty(directoryPath, "directoryPath");

    final Path normalizedPath = normalizePath(Paths.get(directoryPath));

    final DirectoryAccessRule existingRule = directoryRules.get(normalizedPath);
    if (existingRule != null) {
      final DirectoryAccessRule updatedRule =
          new DirectoryAccessRule(existingRule.permissions, existingRule.recursive, enabled);
      directoryRules.put(normalizedPath, updatedRule);

      LOGGER.info(
          String.format(
              "Directory rule %s: path=%s", enabled ? "enabled" : "disabled", normalizedPath));
    }
  }

  /**
   * Gets the permissions for a specific directory path.
   *
   * @param directoryPath the directory path
   * @return the permissions for the directory, or empty set if no permissions
   */
  public Set<WasiFileOperation> getDirectoryPermissions(final String directoryPath) {
    JniValidation.requireNonEmpty(directoryPath, "directoryPath");

    final Path normalizedPath = normalizePath(Paths.get(directoryPath));

    // Check for explicit rule
    final DirectoryAccessRule explicitRule = directoryRules.get(normalizedPath);
    if (explicitRule != null && explicitRule.enabled) {
      return EnumCollections.unmodifiableSet(new HashSet<>(explicitRule.permissions));
    }

    // Check for inherited rule
    if (allowInheritance) {
      final DirectoryAccessRule inheritedRule = findInheritedRule(normalizedPath);
      if (inheritedRule != null) {
        return EnumCollections.unmodifiableSet(new HashSet<>(inheritedRule.permissions));
      }
    }

    // Return global defaults
    return EnumCollections.unmodifiableSet(new HashSet<>(globalDefaultPermissions));
  }

  /**
   * Lists all configured directory rules.
   *
   * @return a map of directory paths to their access rules
   */
  public Map<String, Set<WasiFileOperation>> listDirectoryRules() {
    final Map<String, Set<WasiFileOperation>> result = new ConcurrentHashMap<>();

    for (final Map.Entry<Path, DirectoryAccessRule> entry : directoryRules.entrySet()) {
      if (entry.getValue().enabled) {
        result.put(entry.getKey().toString(), EnumCollections.unmodifiableSet(new HashSet<>(entry.getValue()).permissions));
      }
    }

    return result;
  }

  /** Clears all directory access rules. */
  public void clearAllRules() {
    directoryRules.clear();
    LOGGER.info("Cleared all directory access rules");
  }

  /**
   * Gets the number of configured directory rules.
   *
   * @return the number of directory rules
   */
  public int getRuleCount() {
    return directoryRules.size();
  }

  /**
   * Creates a new directory access control builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Normalizes a path for consistent comparison. */
  private Path normalizePath(final Path path) {
    try {
      return path.toAbsolutePath().normalize();
    } catch (final Exception e) {
      LOGGER.warning(String.format("Failed to normalize path: %s", e.getMessage()));
      throw new JniException("Invalid path: " + path, e);
    }
  }

  /** Validates basic path properties. */
  private void validatePathBasics(final Path path) {
    final String pathString = path.toString();

    if (pathString.length() > MAX_DIRECTORY_PATH_LENGTH) {
      throw new JniException(
          String.format(
              "Directory path too long: %d characters (maximum: %d)",
              pathString.length(), MAX_DIRECTORY_PATH_LENGTH));
    }

    if (strictPathValidation) {
      try {
        // Verify the path exists and is a directory
        if (Files.exists(path) && !Files.isDirectory(path)) {
          throw new JniException("Path is not a directory: " + path);
        }
      } catch (final SecurityException e) {
        LOGGER.warning(String.format("Cannot verify directory status: %s", e.getMessage()));
        if (strictPathValidation) {
          throw new JniException("Cannot verify directory: " + path, e);
        }
      }
    }
  }

  /** Validates that an operation is allowed by the given rule. */
  private void validateOperationAccess(
      final DirectoryAccessRule rule, final WasiFileOperation operation, final Path path) {
    if (!rule.enabled) {
      final String message =
          String.format("Directory access denied: rule disabled for path %s", path);
      LOGGER.warning(message);
      throw new WasiPermissionException(message);
    }

    if (!rule.permissions.contains(operation)) {
      final String message =
          String.format(
              "Directory access denied: operation %s not permitted on path %s", operation, path);
      LOGGER.warning(message);
      throw new WasiPermissionException(message);
    }

    if (auditLogging) {
      LOGGER.info(
          String.format("Directory access granted: path=%s, operation=%s", path, operation));
    }
  }

  /** Finds an inherited rule for the given path by checking parent directories. */
  private DirectoryAccessRule findInheritedRule(final Path path) {
    Path currentPath = path.getParent();

    while (currentPath != null) {
      final DirectoryAccessRule rule = directoryRules.get(currentPath);
      if (rule != null && rule.enabled && rule.recursive) {
        if (auditLogging) {
          LOGGER.fine(
              String.format("Found inherited rule: child=%s, parent=%s", path, currentPath));
        }
        return rule;
      }
      currentPath = currentPath.getParent();
    }

    return null;
  }

  /** Represents a directory access rule with permissions and configuration. */
  private static final class DirectoryAccessRule {
    final Set<WasiFileOperation> permissions;
    final boolean recursive;
    final boolean enabled;

    DirectoryAccessRule(
        final Set<WasiFileOperation> permissions, final boolean recursive, final boolean enabled) {
      this.permissions = EnumCollections.unmodifiableSet(new HashSet<>(permissions));
      this.recursive = recursive;
      this.enabled = enabled;
    }
  }

  /** Builder for creating directory access control configurations. */
  public static final class Builder {
    private final Map<Path, DirectoryAccessRule> directoryRules = new ConcurrentHashMap<>();
    private final Set<WasiFileOperation> globalDefaultPermissions =
        EnumSet.noneOf(WasiFileOperation.class);
    private boolean allowInheritance = true;
    private boolean strictPathValidation = true;
    private boolean auditLogging = false;

    private Builder() {}

    /**
     * Sets whether to allow permission inheritance from parent directories.
     *
     * @param allowInheritance whether to allow inheritance
     * @return this builder for method chaining
     */
    public Builder withInheritance(final boolean allowInheritance) {
      this.allowInheritance = allowInheritance;
      return this;
    }

    /**
     * Sets whether to perform strict path validation.
     *
     * @param strictPathValidation whether to perform strict validation
     * @return this builder for method chaining
     */
    public Builder withStrictPathValidation(final boolean strictPathValidation) {
      this.strictPathValidation = strictPathValidation;
      return this;
    }

    /**
     * Sets whether to enable audit logging of access attempts.
     *
     * @param auditLogging whether to enable audit logging
     * @return this builder for method chaining
     */
    public Builder withAuditLogging(final boolean auditLogging) {
      this.auditLogging = auditLogging;
      return this;
    }

    /**
     * Adds a global default permission for all directories.
     *
     * @param permission the permission to add
     * @return this builder for method chaining
     */
    public Builder withGlobalDefaultPermission(final WasiFileOperation permission) {
      JniValidation.requireNonNull(permission, "permission");
      globalDefaultPermissions.add(permission);
      return this;
    }

    /**
     * Adds permissions for a specific directory.
     *
     * @param directoryPath the directory path
     * @param permissions the permissions to grant
     * @param recursive whether permissions apply recursively
     * @return this builder for method chaining
     */
    public Builder withDirectoryPermissions(
        final String directoryPath,
        final Set<WasiFileOperation> permissions,
        final boolean recursive) {
      JniValidation.requireNonEmpty(directoryPath, "directoryPath");
      JniValidation.requireNonNull(permissions, "permissions");

      final Path path = Paths.get(directoryPath).toAbsolutePath().normalize();
      final DirectoryAccessRule rule =
          new DirectoryAccessRule(EnumCollections.unmodifiableSet(new HashSet<>(permissions)), recursive, true);
      directoryRules.put(path, rule);
      return this;
    }

    /**
     * Builds the directory access control system.
     *
     * @return the configured directory access control
     */
    public WasiDirectoryAccessControl build() {
      return new WasiDirectoryAccessControl(this);
    }
  }
}
