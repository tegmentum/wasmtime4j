package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiPermissionException;
import ai.tegmentum.wasmtime4j.jni.wasi.permission.WasiPermissionManager;
import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Comprehensive file system security validation orchestrator for WASI operations.
 *
 * <p>This class coordinates all security validation layers to provide comprehensive protection
 * against unauthorized file system access. It integrates multiple security components:
 *
 * <ul>
 *   <li>Path traversal attack prevention and validation
 *   <li>Permission-based access control with fine-grained rules
 *   <li>Directory-level access controls with inheritance
 *   <li>Real-time security policy enforcement
 *   <li>Comprehensive audit logging and monitoring
 *   <li>Defense-in-depth security architecture
 * </ul>
 *
 * <p>Security validation follows a strict deny-by-default policy where all operations must pass
 * multiple validation layers before being allowed to proceed.
 *
 * @since 1.0.0
 */
public final class WasiFileSystemSecurityOrchestrator {

  private static final Logger LOGGER =
      Logger.getLogger(WasiFileSystemSecurityOrchestrator.class.getName());

  /** The security validator for path traversal protection. */
  private final WasiSecurityValidator securityValidator;

  /** The permission manager for access control. */
  private final WasiPermissionManager permissionManager;

  /** The directory access control system. */
  private final WasiDirectoryAccessControl directoryAccessControl;

  /** Cache of previously validated paths to improve performance. */
  private final Set<String> validatedPathCache = ConcurrentHashMap.newKeySet();

  /** Whether to enable path validation caching. */
  private final boolean enablePathCaching;

  /** Whether to enable comprehensive audit logging. */
  private final boolean enableAuditLogging;

  /** Whether to use strict validation mode. */
  private final boolean strictValidationMode;

  /**
   * Creates a new security orchestrator with the specified components.
   *
   * @param securityValidator the security validator
   * @param permissionManager the permission manager
   * @param directoryAccessControl the directory access control system
   * @param enablePathCaching whether to enable path validation caching
   * @param enableAuditLogging whether to enable audit logging
   * @param strictValidationMode whether to use strict validation mode
   */
  public WasiFileSystemSecurityOrchestrator(
      final WasiSecurityValidator securityValidator,
      final WasiPermissionManager permissionManager,
      final WasiDirectoryAccessControl directoryAccessControl,
      final boolean enablePathCaching,
      final boolean enableAuditLogging,
      final boolean strictValidationMode) {
    JniValidation.requireNonNull(securityValidator, "securityValidator");
    JniValidation.requireNonNull(permissionManager, "permissionManager");
    JniValidation.requireNonNull(directoryAccessControl, "directoryAccessControl");

    this.securityValidator = securityValidator;
    this.permissionManager = permissionManager;
    this.directoryAccessControl = directoryAccessControl;
    this.enablePathCaching = enablePathCaching;
    this.enableAuditLogging = enableAuditLogging;
    this.strictValidationMode = strictValidationMode;

    LOGGER.info(
        String.format(
            "Created security orchestrator: caching=%s, audit=%s, strict=%s",
            enablePathCaching, enableAuditLogging, strictValidationMode));
  }

  /**
   * Validates comprehensive security for a file system operation.
   *
   * <p>This method performs multi-layer security validation including:
   *
   * <ul>
   *   <li>Path normalization and traversal attack prevention
   *   <li>Permission-based access control validation
   *   <li>Directory-level access rule enforcement
   *   <li>File existence and type validation in strict mode
   *   <li>Security policy compliance checking
   * </ul>
   *
   * @param path the file path to validate
   * @param operation the file operation to validate
   * @return the validated and normalized path
   * @throws WasiFileSystemException if validation fails
   */
  public Path validateFileSystemOperation(final String path, final WasiFileOperation operation) {
    JniValidation.requireNonEmpty(path, "path");
    JniValidation.requireNonNull(operation, "operation");

    if (enableAuditLogging) {
      LOGGER.info(
          String.format("Security validation started: path=%s, operation=%s", path, operation));
    }

    final long startTime = System.nanoTime();

    try {
      // Step 1: Path normalization and basic validation
      final Path normalizedPath = normalizeAndValidatePath(path);

      // Step 2: Check path validation cache if enabled
      if (enablePathCaching) {
        final String cacheKey = normalizedPath.toString() + ":" + operation.name();
        if (validatedPathCache.contains(cacheKey)) {
          if (enableAuditLogging) {
            LOGGER.fine(
                String.format(
                    "Security validation cached: path=%s, operation=%s",
                    normalizedPath, operation));
          }
          return normalizedPath;
        }
      }

      // Step 3: Security validator - path traversal protection
      validatePathSecurity(normalizedPath);

      // Step 4: Permission manager - access control validation
      validatePermissions(normalizedPath, operation);

      // Step 5: Directory access control - directory-level rules
      validateDirectoryAccess(normalizedPath, operation);

      // Step 6: Strict validation mode checks
      if (strictValidationMode) {
        performStrictValidation(normalizedPath, operation);
      }

      // Step 7: Cache successful validation if enabled
      if (enablePathCaching) {
        final String cacheKey = normalizedPath.toString() + ":" + operation.name();
        validatedPathCache.add(cacheKey);

        // Prevent cache from growing too large
        if (validatedPathCache.size() > 10000) {
          validatedPathCache.clear();
          LOGGER.fine("Path validation cache cleared due to size limit");
        }
      }

      final long elapsedNanos = System.nanoTime() - startTime;
      if (enableAuditLogging) {
        LOGGER.info(
            String.format(
                "Security validation completed: path=%s, operation=%s, elapsed=%dus",
                normalizedPath, operation, elapsedNanos / 1000));
      }

      return normalizedPath;

    } catch (final WasiFileSystemException e) {
      final long elapsedNanos = System.nanoTime() - startTime;
      LOGGER.warning(
          String.format(
              "Security validation failed: path=%s, operation=%s, error=%s, elapsed=%dus",
              path, operation, e.getMessage(), elapsedNanos / 1000));
      throw e;

    } catch (final Exception e) {
      final long elapsedNanos = System.nanoTime() - startTime;
      LOGGER.warning(
          String.format(
              "Security validation error: path=%s, operation=%s, error=%s, elapsed=%dus",
              path, operation, e.getMessage(), elapsedNanos / 1000));
      throw new WasiFileSystemException("Security validation error: " + e.getMessage(), "EIO", e);
    }
  }

  /**
   * Validates environment variable access security.
   *
   * @param variableName the environment variable name
   * @throws WasiPermissionException if access is denied
   */
  public void validateEnvironmentVariableAccess(final String variableName) {
    JniValidation.requireNonEmpty(variableName, "variableName");

    if (enableAuditLogging) {
      LOGGER.info(String.format("Environment variable validation: name=%s", variableName));
    }

    try {
      securityValidator.validateEnvironmentAccess(variableName);

      if (enableAuditLogging) {
        LOGGER.info(String.format("Environment variable access granted: name=%s", variableName));
      }

    } catch (final Exception e) {
      LOGGER.warning(
          String.format(
              "Environment variable access denied: name=%s, error=%s",
              variableName, e.getMessage()));
      throw new WasiPermissionException("Environment variable access denied: " + e.getMessage(), e);
    }
  }

  /** Clears the path validation cache. */
  public void clearValidationCache() {
    if (enablePathCaching) {
      validatedPathCache.clear();
      LOGGER.info("Path validation cache cleared");
    }
  }

  /**
   * Gets the current size of the validation cache.
   *
   * @return the cache size
   */
  public int getValidationCacheSize() {
    return enablePathCaching ? validatedPathCache.size() : 0;
  }

  /**
   * Creates a new security orchestrator builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Normalizes and validates basic path properties. */
  private Path normalizeAndValidatePath(final String path) {
    try {
      final Path pathObj = Paths.get(path);
      return pathObj.normalize().toAbsolutePath();
    } catch (final Exception e) {
      LOGGER.warning(
          String.format("Path normalization failed: path=%s, error=%s", path, e.getMessage()));
      throw new WasiFileSystemException("Invalid path: " + path, "EINVAL", e);
    }
  }

  /** Validates path security through the security validator. */
  private void validatePathSecurity(final Path path) {
    try {
      securityValidator.validatePath(path);
    } catch (final Exception e) {
      LOGGER.warning(
          String.format(
              "Path security validation failed: path=%s, error=%s", path, e.getMessage()));
      throw new WasiFileSystemException("Path security violation: " + e.getMessage(), "EACCES", e);
    }
  }

  /** Validates permissions through the permission manager. */
  private void validatePermissions(final Path path, final WasiFileOperation operation) {
    try {
      permissionManager.validateFileSystemAccess(path, operation);
    } catch (final Exception e) {
      LOGGER.warning(
          String.format(
              "Permission validation failed: path=%s, operation=%s, error=%s",
              path, operation, e.getMessage()));
      throw new WasiPermissionException("Permission denied: " + e.getMessage(), e);
    }
  }

  /** Validates directory access through directory access control. */
  private void validateDirectoryAccess(final Path path, final WasiFileOperation operation) {
    try {
      // For file operations, validate against the parent directory
      final Path directoryPath = Files.isDirectory(path) ? path : path.getParent();
      if (directoryPath != null) {
        directoryAccessControl.validateDirectoryAccess(directoryPath, operation);
      }
    } catch (final Exception e) {
      LOGGER.warning(
          String.format(
              "Directory access validation failed: path=%s, operation=%s, error=%s",
              path, operation, e.getMessage()));
      throw new WasiPermissionException("Directory access denied: " + e.getMessage(), e);
    }
  }

  /** Performs additional strict mode validation. */
  private void performStrictValidation(final Path path, final WasiFileOperation operation) {
    try {
      // Validate file existence for read operations
      if (operation.requiresReadAccess() && !Files.exists(path)) {
        throw new WasiFileSystemException("File does not exist", "ENOENT");
      }

      // Validate file type consistency
      if (Files.exists(path)) {
        if (operation == WasiFileOperation.LIST_DIRECTORY && !Files.isDirectory(path)) {
          throw new WasiFileSystemException("Path is not a directory", "ENOTDIR");
        }

        if (operation.requiresWriteAccess() && !Files.isWritable(path.getParent())) {
          throw new WasiFileSystemException("Parent directory not writable", "EACCES");
        }
      }

      // Additional security checks for dangerous operations
      if (operation.isDangerous()) {
        LOGGER.warning(
            String.format("Dangerous operation attempted: path=%s, operation=%s", path, operation));

        // Extra validation for dangerous operations
        if (operation == WasiFileOperation.EXECUTE) {
          if (!Files.isExecutable(path)) {
            throw new WasiFileSystemException("File is not executable", "EACCES");
          }
        }
      }

    } catch (final IOException e) {
      LOGGER.warning(
          String.format("Strict validation I/O error: path=%s, error=%s", path, e.getMessage()));
      throw new WasiFileSystemException("Strict validation failed: " + e.getMessage(), "EIO", e);
    }
  }

  /** Builder for creating security orchestrator configurations. */
  public static final class Builder {
    private WasiSecurityValidator securityValidator = WasiSecurityValidator.defaultValidator();
    private WasiPermissionManager permissionManager;
    private WasiDirectoryAccessControl directoryAccessControl =
        WasiDirectoryAccessControl.builder().build();
    private boolean enablePathCaching = true;
    private boolean enableAuditLogging = false;
    private boolean strictValidationMode = true;

    private Builder() {}

    /**
     * Sets the security validator.
     *
     * @param securityValidator the security validator
     * @return this builder for method chaining
     */
    public Builder withSecurityValidator(final WasiSecurityValidator securityValidator) {
      JniValidation.requireNonNull(securityValidator, "securityValidator");
      this.securityValidator = securityValidator;
      return this;
    }

    /**
     * Sets the permission manager.
     *
     * @param permissionManager the permission manager
     * @return this builder for method chaining
     */
    public Builder withPermissionManager(final WasiPermissionManager permissionManager) {
      JniValidation.requireNonNull(permissionManager, "permissionManager");
      this.permissionManager = permissionManager;
      return this;
    }

    /**
     * Sets the directory access control system.
     *
     * @param directoryAccessControl the directory access control
     * @return this builder for method chaining
     */
    public Builder withDirectoryAccessControl(
        final WasiDirectoryAccessControl directoryAccessControl) {
      JniValidation.requireNonNull(directoryAccessControl, "directoryAccessControl");
      this.directoryAccessControl = directoryAccessControl;
      return this;
    }

    /**
     * Sets whether to enable path validation caching.
     *
     * @param enablePathCaching whether to enable caching
     * @return this builder for method chaining
     */
    public Builder withPathCaching(final boolean enablePathCaching) {
      this.enablePathCaching = enablePathCaching;
      return this;
    }

    /**
     * Sets whether to enable audit logging.
     *
     * @param enableAuditLogging whether to enable audit logging
     * @return this builder for method chaining
     */
    public Builder withAuditLogging(final boolean enableAuditLogging) {
      this.enableAuditLogging = enableAuditLogging;
      return this;
    }

    /**
     * Sets whether to use strict validation mode.
     *
     * @param strictValidationMode whether to use strict mode
     * @return this builder for method chaining
     */
    public Builder withStrictValidationMode(final boolean strictValidationMode) {
      this.strictValidationMode = strictValidationMode;
      return this;
    }

    /**
     * Builds the security orchestrator.
     *
     * @return the configured security orchestrator
     */
    public WasiFileSystemSecurityOrchestrator build() {
      JniValidation.requireNonNull(permissionManager, "permissionManager must be set");

      return new WasiFileSystemSecurityOrchestrator(
          securityValidator,
          permissionManager,
          directoryAccessControl,
          enablePathCaching,
          enableAuditLogging,
          strictValidationMode);
    }
  }
}
