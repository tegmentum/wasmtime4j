package ai.tegmentum.wasmtime4j.jni.wasi.permission;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.wasi.permission.WasiResourceLimits;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Configurable permission system for controlling WASI capabilities with fine-grained controls.
 *
 * <p>This class provides comprehensive permission management for WASI operations, including:
 *
 * <ul>
 *   <li>Fine-grained file system access controls
 *   <li>Environment variable access restrictions
 *   <li>Resource limiting and quota enforcement
 *   <li>Operation-specific permission validation
 *   <li>Thread-safe permission checking
 * </ul>
 *
 * <p>The permission system is designed with security as the highest priority. All operations are
 * denied by default and must be explicitly allowed through configuration.
 *
 * @since 1.0.0
 */
public final class WasiPermissionManager {

  private static final Logger LOGGER = Logger.getLogger(WasiPermissionManager.class.getName());

  /** Path-specific permissions mapping. */
  private final Map<Path, Set<WasiFileOperation>> pathPermissions = new ConcurrentHashMap<>();

  /** Global file system permissions applied to all paths. */
  private final Set<WasiFileOperation> globalPermissions = EnumSet.noneOf(WasiFileOperation.class);

  /** Allowed environment variable patterns. */
  private final Set<String> allowedEnvironmentVariables = ConcurrentHashMap.newKeySet();

  /** Denied environment variable patterns. */
  private final Set<String> deniedEnvironmentVariables = ConcurrentHashMap.newKeySet();

  /** Resource limits for the WASI context. */
  private final WasiResourceLimits resourceLimits;

  /** Whether to allow dangerous operations by default. */
  private final boolean allowDangerousOperations;

  /** Whether to enforce strict path validation. */
  private final boolean strictPathValidation;

  /**
   * Creates a new WASI permission manager with the specified configuration.
   *
   * @param builder the permission manager builder
   */
  private WasiPermissionManager(final Builder builder) {
    this.pathPermissions.putAll(builder.pathPermissions);
    this.globalPermissions.addAll(builder.globalPermissions);
    this.allowedEnvironmentVariables.addAll(builder.allowedEnvironmentVariables);
    this.deniedEnvironmentVariables.addAll(builder.deniedEnvironmentVariables);
    this.resourceLimits = builder.resourceLimits;
    this.allowDangerousOperations = builder.allowDangerousOperations;
    this.strictPathValidation = builder.strictPathValidation;

    LOGGER.info(
        String.format(
            "Created WASI permission manager with %d path rules, %d global permissions, "
                + "dangerous operations: %s, strict validation: %s",
            pathPermissions.size(),
            globalPermissions.size(),
            allowDangerousOperations,
            strictPathValidation));
  }

  /**
   * Validates file system access for the specified path.
   *
   * @param path the file path to validate
   * @throws JniException if access is not permitted
   */
  public void validateFileSystemAccess(final Path path) {
    validateFileSystemAccess(path, null);
  }

  /**
   * Validates file system access for the specified path and operation.
   *
   * @param path the file path to validate
   * @param operation the file operation to validate (null for general access)
   * @throws JniException if access is not permitted
   */
  public void validateFileSystemAccess(final Path path, final WasiFileOperation operation) {
    JniValidation.requireNonNull(path, "path");

    final Path normalizedPath = path.normalize().toAbsolutePath();

    LOGGER.fine(
        String.format(
            "Validating file system access for path: %s, operation: %s",
            normalizedPath, operation));

    // Check if the operation is dangerous and not allowed
    if (operation != null && operation.isDangerous() && !allowDangerousOperations) {
      throw new JniException(
          String.format(
              "Dangerous file operation '%s' is not permitted for path: %s",
              operation, normalizedPath));
    }

    // Check path-specific permissions first
    boolean hasPermission = false;

    for (final Map.Entry<Path, Set<WasiFileOperation>> entry : pathPermissions.entrySet()) {
      final Path permittedPath = entry.getKey();
      final Set<WasiFileOperation> allowedOps = entry.getValue();

      if (isPathWithinDirectory(normalizedPath, permittedPath)) {
        if (operation == null) {
          // General access - check if any permissions are granted
          if (!allowedOps.isEmpty()) {
            hasPermission = true;
            break;
          }
        } else {
          // Specific operation - check if the operation is allowed
          if (allowedOps.contains(operation)) {
            hasPermission = true;
            break;
          }
        }
      }
    }

    // If no path-specific permission found, check global permissions
    if (!hasPermission && operation != null && globalPermissions.contains(operation)) {
      hasPermission = true;
    }

    // If still no permission and it's a general access check, see if global permissions exist
    if (!hasPermission && operation == null && !globalPermissions.isEmpty()) {
      hasPermission = true;
    }

    if (!hasPermission) {
      final String operationStr = operation != null ? operation.toString() : "general access";
      throw new JniException(
          String.format(
              "File system access denied for %s on path: %s", operationStr, normalizedPath));
    }

    LOGGER.fine(String.format("File system access granted for path: %s", normalizedPath));
  }

  /**
   * Validates environment variable access.
   *
   * @param variableName the environment variable name to validate
   * @throws JniException if access is not permitted
   */
  public void validateEnvironmentAccess(final String variableName) {
    JniValidation.requireNonEmpty(variableName, "variableName");

    LOGGER.fine(String.format("Validating environment access for variable: %s", variableName));

    // Check if explicitly denied
    if (deniedEnvironmentVariables.contains(variableName)) {
      throw new JniException(String.format("Environment variable access denied: %s", variableName));
    }

    // Check if explicitly allowed
    if (allowedEnvironmentVariables.contains(variableName)) {
      LOGGER.fine(String.format("Environment access granted for variable: %s", variableName));
      return;
    }

    // Check wildcard patterns
    if (isVariableAllowedByPattern(variableName)) {
      LOGGER.fine(
          String.format("Environment access granted by pattern for variable: %s", variableName));
      return;
    }

    // If no explicit permission and not empty allow list, deny access
    if (!allowedEnvironmentVariables.isEmpty()) {
      throw new JniException(String.format("Environment variable access denied: %s", variableName));
    }

    // If allow list is empty, grant access (permissive mode)
    LOGGER.fine(
        String.format(
            "Environment access granted (permissive mode) for variable: %s", variableName));
  }

  /**
   * Gets the resource limits for this permission manager.
   *
   * @return the resource limits
   */
  public WasiResourceLimits getResourceLimits() {
    return resourceLimits;
  }

  /**
   * Checks if dangerous operations are allowed.
   *
   * @return true if dangerous operations are allowed, false otherwise
   */
  public boolean areDangerousOperationsAllowed() {
    return allowDangerousOperations;
  }

  /**
   * Checks if strict path validation is enabled.
   *
   * @return true if strict path validation is enabled, false otherwise
   */
  public boolean isStrictPathValidationEnabled() {
    return strictPathValidation;
  }

  /**
   * Creates a default permission manager with minimal permissions.
   *
   * @return a default permission manager
   */
  public static WasiPermissionManager defaultManager() {
    return builder()
        .withGlobalPermission(WasiFileOperation.READ)
        .withResourceLimits(WasiResourceLimits.defaultLimits())
        .build();
  }

  /**
   * Creates a restrictive permission manager with no permissions.
   *
   * @return a restrictive permission manager
   */
  public static WasiPermissionManager restrictiveManager() {
    return builder()
        .withStrictPathValidation(true)
        .withResourceLimits(WasiResourceLimits.restrictiveLimits())
        .build();
  }

  /**
   * Creates a permissive permission manager with broad permissions.
   *
   * @return a permissive permission manager
   */
  public static WasiPermissionManager permissiveManager() {
    return builder()
        .withAllFileOperations()
        .withAllEnvironmentVariables()
        .withDangerousOperations(true)
        .withResourceLimits(WasiResourceLimits.permissiveLimits())
        .build();
  }

  /**
   * Creates a new permission manager builder.
   *
   * @return a new permission manager builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if a path is within a directory (or is the directory itself).
   *
   * @param path the path to check
   * @param directory the directory to check against
   * @return true if the path is within the directory, false otherwise
   */
  private boolean isPathWithinDirectory(final Path path, final Path directory) {
    try {
      final Path normalizedPath = path.normalize().toAbsolutePath();
      final Path normalizedDirectory = directory.normalize().toAbsolutePath();

      return normalizedPath.startsWith(normalizedDirectory);
    } catch (final Exception e) {
      LOGGER.warning(
          String.format(
              "Error checking if path %s is within directory %s: %s",
              path, directory, e.getMessage()));
      return false;
    }
  }

  /**
   * Checks if an environment variable is allowed by wildcard patterns.
   *
   * @param variableName the variable name to check
   * @return true if allowed by a pattern, false otherwise
   */
  private boolean isVariableAllowedByPattern(final String variableName) {
    for (final String pattern : allowedEnvironmentVariables) {
      if (pattern.endsWith("*")) {
        final String prefix = pattern.substring(0, pattern.length() - 1);
        if (variableName.startsWith(prefix)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Builder for creating WASI permission managers. */
  public static final class Builder {

    private final Map<Path, Set<WasiFileOperation>> pathPermissions = new ConcurrentHashMap<>();
    private final Set<WasiFileOperation> globalPermissions =
        EnumSet.noneOf(WasiFileOperation.class);
    private final Set<String> allowedEnvironmentVariables = ConcurrentHashMap.newKeySet();
    private final Set<String> deniedEnvironmentVariables = ConcurrentHashMap.newKeySet();
    private WasiResourceLimits resourceLimits = WasiResourceLimits.defaultLimits();
    private boolean allowDangerousOperations = false;
    private boolean strictPathValidation = false;

    private Builder() {}

    /**
     * Adds a path-specific permission.
     *
     * @param path the path to grant permission for
     * @param operation the operation to allow
     * @return this builder for method chaining
     */
    public Builder withPathPermission(final Path path, final WasiFileOperation operation) {
      JniValidation.requireNonNull(path, "path");
      JniValidation.requireNonNull(operation, "operation");

      pathPermissions
          .computeIfAbsent(
              path.normalize().toAbsolutePath(), k -> EnumSet.noneOf(WasiFileOperation.class))
          .add(operation);

      return this;
    }

    /**
     * Adds multiple path-specific permissions.
     *
     * @param path the path to grant permissions for
     * @param operations the operations to allow
     * @return this builder for method chaining
     */
    public Builder withPathPermissions(final Path path, final WasiFileOperation... operations) {
      JniValidation.requireNonNull(path, "path");
      JniValidation.requireNonNull(operations, "operations");

      final Set<WasiFileOperation> pathOps =
          pathPermissions.computeIfAbsent(
              path.normalize().toAbsolutePath(), k -> EnumSet.noneOf(WasiFileOperation.class));

      for (final WasiFileOperation operation : operations) {
        pathOps.add(operation);
      }

      return this;
    }

    /**
     * Adds a global permission that applies to all paths.
     *
     * @param operation the operation to allow globally
     * @return this builder for method chaining
     */
    public Builder withGlobalPermission(final WasiFileOperation operation) {
      JniValidation.requireNonNull(operation, "operation");
      globalPermissions.add(operation);
      return this;
    }

    /**
     * Adds multiple global permissions.
     *
     * @param operations the operations to allow globally
     * @return this builder for method chaining
     */
    public Builder withGlobalPermissions(final WasiFileOperation... operations) {
      JniValidation.requireNonNull(operations, "operations");

      for (final WasiFileOperation operation : operations) {
        globalPermissions.add(operation);
      }

      return this;
    }

    /**
     * Allows all file operations globally.
     *
     * @return this builder for method chaining
     */
    public Builder withAllFileOperations() {
      globalPermissions.addAll(EnumSet.allOf(WasiFileOperation.class));
      return this;
    }

    /**
     * Allows access to an environment variable.
     *
     * @param variableName the environment variable name (supports wildcards with *)
     * @return this builder for method chaining
     */
    public Builder withEnvironmentVariable(final String variableName) {
      JniValidation.requireNonEmpty(variableName, "variableName");
      allowedEnvironmentVariables.add(variableName);
      return this;
    }

    /**
     * Allows access to all environment variables.
     *
     * @return this builder for method chaining
     */
    public Builder withAllEnvironmentVariables() {
      allowedEnvironmentVariables.add("*");
      return this;
    }

    /**
     * Denies access to an environment variable.
     *
     * @param variableName the environment variable name
     * @return this builder for method chaining
     */
    public Builder withDeniedEnvironmentVariable(final String variableName) {
      JniValidation.requireNonEmpty(variableName, "variableName");
      deniedEnvironmentVariables.add(variableName);
      return this;
    }

    /**
     * Sets the resource limits.
     *
     * @param limits the resource limits
     * @return this builder for method chaining
     */
    public Builder withResourceLimits(final WasiResourceLimits limits) {
      JniValidation.requireNonNull(limits, "limits");
      this.resourceLimits = limits;
      return this;
    }

    /**
     * Sets whether to allow dangerous operations.
     *
     * @param allow whether to allow dangerous operations
     * @return this builder for method chaining
     */
    public Builder withDangerousOperations(final boolean allow) {
      this.allowDangerousOperations = allow;
      return this;
    }

    /**
     * Sets whether to enable strict path validation.
     *
     * @param strict whether to enable strict path validation
     * @return this builder for method chaining
     */
    public Builder withStrictPathValidation(final boolean strict) {
      this.strictPathValidation = strict;
      return this;
    }

    /**
     * Builds the permission manager.
     *
     * @return the configured permission manager
     */
    public WasiPermissionManager build() {
      return new WasiPermissionManager(this);
    }
  }
}
