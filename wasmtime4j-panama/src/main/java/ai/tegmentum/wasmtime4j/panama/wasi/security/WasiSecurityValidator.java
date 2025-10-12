package ai.tegmentum.wasmtime4j.panama.wasi.security;

import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiPermissionException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Panama implementation of WASI security validation with comprehensive protection against common
 * attacks.
 *
 * <p>This validator provides production-ready security validation for WASI operations, including:
 *
 * <ul>
 *   <li>Path traversal attack prevention
 *   <li>Environment variable access control
 *   <li>Resource access validation
 *   <li>Configurable security policies
 *   <li>Thread-safe validation operations
 * </ul>
 *
 * <p>All validation operations are designed to fail securely and provide comprehensive logging for
 * security auditing.
 *
 * @since 1.0.0
 */
public final class WasiSecurityValidator {

  private static final Logger LOGGER = Logger.getLogger(WasiSecurityValidator.class.getName());

  /** Default instance with standard security settings. */
  private static final WasiSecurityValidator DEFAULT_INSTANCE = new Builder().build();

  /** Pattern for detecting path traversal attempts. */
  private static final Pattern PATH_TRAVERSAL_PATTERN =
      Pattern.compile("(\\.\\.[\\\\/])|([\\\\/:][\\\\./]{2,})");

  /** Pattern for detecting potentially dangerous paths. */
  private static final Pattern DANGEROUS_PATH_PATTERN =
      Pattern.compile("(\0|[\\\\/]\\.{1,2}[\\\\/]|^[\\\\/]\\.{1,2}$)");

  /** Maximum allowed path length. */
  private final int maxPathLength;

  /** Whether to allow absolute paths. */
  private final boolean allowAbsolutePaths;

  /** Whether to allow symbolic links. */
  private final boolean allowSymbolicLinks;

  /** Set of forbidden path components. */
  private final Set<String> forbiddenPathComponents;

  /** Set of allowed environment variable name patterns. */
  private final Set<Pattern> allowedEnvironmentPatterns;

  /** Set of forbidden environment variable names. */
  private final Set<String> forbiddenEnvironmentNames;

  /**
   * Creates a new security validator with the specified configuration.
   *
   * @param builder the builder containing configuration
   */
  private WasiSecurityValidator(final Builder builder) {
    this.maxPathLength = builder.maxPathLength;
    this.allowAbsolutePaths = builder.allowAbsolutePaths;
    this.allowSymbolicLinks = builder.allowSymbolicLinks;
    this.forbiddenPathComponents = Set.copyOf(builder.forbiddenPathComponents);
    this.allowedEnvironmentPatterns = Set.copyOf(builder.allowedEnvironmentPatterns);
    this.forbiddenEnvironmentNames = Set.copyOf(builder.forbiddenEnvironmentNames);

    LOGGER.info(
        String.format(
            "Created WASI security validator: maxPathLength=%d, allowAbsolute=%s, allowSymlinks=%s",
            maxPathLength, allowAbsolutePaths, allowSymbolicLinks));
  }

  /**
   * Gets the default security validator instance.
   *
   * @return the default security validator
   */
  public static WasiSecurityValidator defaultValidator() {
    return DEFAULT_INSTANCE;
  }

  /**
   * Creates a new builder for configuring a security validator.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Validates that a path is safe for use in WASI operations.
   *
   * <p>This method performs comprehensive validation to prevent path traversal attacks and other
   * security vulnerabilities.
   *
   * @param path the path to validate
   * @throws WasiPermissionException if the path is not safe
   * @throws IllegalArgumentException if path is null
   */
  public void validatePath(final Path path) throws WasiPermissionException {
    PanamaValidation.requireNonNull(path, "path");

    final String pathString = path.toString();

    LOGGER.fine(String.format("Validating path: %s", pathString));

    // Check path length
    if (pathString.length() > maxPathLength) {
      LOGGER.warning(String.format("Path too long: %d > %d", pathString.length(), maxPathLength));
      throw new WasiPermissionException(
          String.format("Path exceeds maximum length: %d", maxPathLength));
    }

    // Check for path traversal patterns
    if (PATH_TRAVERSAL_PATTERN.matcher(pathString).find()) {
      LOGGER.warning(String.format("Path traversal attempt detected: %s", pathString));
      throw new WasiPermissionException("Path traversal attempt detected");
    }

    // Check for dangerous path patterns
    if (DANGEROUS_PATH_PATTERN.matcher(pathString).find()) {
      LOGGER.warning(String.format("Dangerous path pattern detected: %s", pathString));
      throw new WasiPermissionException("Dangerous path pattern detected");
    }

    // Check absolute path policy
    if (path.isAbsolute() && !allowAbsolutePaths) {
      LOGGER.warning(String.format("Absolute path not allowed: %s", pathString));
      throw new WasiPermissionException("Absolute paths are not allowed");
    }

    // Check individual path components
    for (final Path component : path) {
      final String componentString = component.toString();
      if (forbiddenPathComponents.contains(componentString)) {
        LOGGER.warning(String.format("Forbidden path component detected: %s", componentString));
        throw new WasiPermissionException(
            String.format("Forbidden path component: %s", componentString));
      }
    }

    LOGGER.fine(String.format("Path validation passed: %s", pathString));
  }

  /**
   * Validates that an environment variable name is safe for access.
   *
   * @param name the environment variable name to validate
   * @throws WasiPermissionException if the environment variable is not accessible
   * @throws IllegalArgumentException if name is null or empty
   */
  public void validateEnvironmentAccess(final String name) throws WasiPermissionException {
    PanamaValidation.requireNonEmpty(name, "name");

    LOGGER.fine(String.format("Validating environment variable access: %s", name));

    // Check forbidden names
    if (forbiddenEnvironmentNames.contains(name)) {
      LOGGER.warning(String.format("Access to forbidden environment variable: %s", name));
      throw new WasiPermissionException(
          String.format("Access to environment variable '%s' is forbidden", name));
    }

    // Check allowed patterns
    if (!allowedEnvironmentPatterns.isEmpty()) {
      boolean allowed = false;
      for (final Pattern pattern : allowedEnvironmentPatterns) {
        if (pattern.matcher(name).matches()) {
          allowed = true;
          break;
        }
      }

      if (!allowed) {
        LOGGER.warning(
            String.format("Environment variable does not match allowed patterns: %s", name));
        throw new WasiPermissionException(
            String.format("Environment variable '%s' does not match allowed patterns", name));
      }
    }

    LOGGER.fine(String.format("Environment variable access validated: %s", name));
  }

  /**
   * Validates that a resource identifier is safe for use.
   *
   * @param resourceId the resource identifier to validate
   * @throws WasiPermissionException if the resource identifier is not safe
   */
  public void validateResourceAccess(final String resourceId) throws WasiPermissionException {
    PanamaValidation.requireNonEmpty(resourceId, "resourceId");

    LOGGER.fine(String.format("Validating resource access: %s", resourceId));

    // Check for null bytes and control characters
    if (resourceId.contains("\0") || resourceId.chars().anyMatch(Character::isISOControl)) {
      LOGGER.warning(String.format("Invalid characters in resource identifier: %s", resourceId));
      throw new WasiPermissionException("Resource identifier contains invalid characters");
    }

    // Check length
    if (resourceId.length() > 255) {
      LOGGER.warning(String.format("Resource identifier too long: %d", resourceId.length()));
      throw new WasiPermissionException("Resource identifier exceeds maximum length");
    }

    LOGGER.fine(String.format("Resource access validated: %s", resourceId));
  }

  /** Builder for creating WasiSecurityValidator instances with custom configuration. */
  public static final class Builder {

    /** Default maximum path length. */
    private static final int DEFAULT_MAX_PATH_LENGTH = 4096;

    private int maxPathLength = DEFAULT_MAX_PATH_LENGTH;
    private boolean allowAbsolutePaths = false;
    private boolean allowSymbolicLinks = true;
    private final Set<String> forbiddenPathComponents = ConcurrentHashMap.newKeySet();
    private final Set<Pattern> allowedEnvironmentPatterns = ConcurrentHashMap.newKeySet();
    private final Set<String> forbiddenEnvironmentNames = ConcurrentHashMap.newKeySet();

    Builder() {
      // Set default forbidden path components
      forbiddenPathComponents.add(".");
      forbiddenPathComponents.add("..");

      // Set default forbidden environment variables
      forbiddenEnvironmentNames.add("PATH");
      forbiddenEnvironmentNames.add("LD_LIBRARY_PATH");
      forbiddenEnvironmentNames.add("DYLD_LIBRARY_PATH");

      // Set default allowed environment patterns (allow most standard variables)
      allowedEnvironmentPatterns.add(Pattern.compile("^[A-Z][A-Z0-9_]*$"));
      allowedEnvironmentPatterns.add(Pattern.compile("^[a-z][a-z0-9_]*$"));
      allowedEnvironmentPatterns.add(Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$"));
    }

    /**
     * Sets the maximum allowed path length.
     *
     * @param maxPathLength the maximum path length
     * @return this builder for method chaining
     * @throws IllegalArgumentException if maxPathLength is not positive
     */
    public Builder withMaxPathLength(final int maxPathLength) {
      PanamaValidation.requirePositive(maxPathLength, "maxPathLength");
      this.maxPathLength = maxPathLength;
      return this;
    }

    /**
     * Sets whether absolute paths are allowed.
     *
     * @param allowAbsolutePaths whether to allow absolute paths
     * @return this builder for method chaining
     */
    public Builder withAllowAbsolutePaths(final boolean allowAbsolutePaths) {
      this.allowAbsolutePaths = allowAbsolutePaths;
      return this;
    }

    /**
     * Sets whether symbolic links are allowed.
     *
     * @param allowSymbolicLinks whether to allow symbolic links
     * @return this builder for method chaining
     */
    public Builder withAllowSymbolicLinks(final boolean allowSymbolicLinks) {
      this.allowSymbolicLinks = allowSymbolicLinks;
      return this;
    }

    /**
     * Adds a forbidden path component.
     *
     * @param component the path component to forbid
     * @return this builder for method chaining
     * @throws IllegalArgumentException if component is null or empty
     */
    public Builder withForbiddenPathComponent(final String component) {
      PanamaValidation.requireNonEmpty(component, "component");
      forbiddenPathComponents.add(component);
      return this;
    }

    /**
     * Adds an allowed environment variable pattern.
     *
     * @param pattern the pattern for allowed environment variable names
     * @return this builder for method chaining
     * @throws IllegalArgumentException if pattern is null
     */
    public Builder withAllowedEnvironmentPattern(final Pattern pattern) {
      PanamaValidation.requireNonNull(pattern, "pattern");
      allowedEnvironmentPatterns.add(pattern);
      return this;
    }

    /**
     * Adds an allowed environment variable pattern.
     *
     * @param pattern the pattern string for allowed environment variable names
     * @return this builder for method chaining
     * @throws IllegalArgumentException if pattern is null or empty
     */
    public Builder withAllowedEnvironmentPattern(final String pattern) {
      PanamaValidation.requireNonEmpty(pattern, "pattern");
      return withAllowedEnvironmentPattern(Pattern.compile(pattern));
    }

    /**
     * Adds a forbidden environment variable name.
     *
     * @param name the environment variable name to forbid
     * @return this builder for method chaining
     * @throws IllegalArgumentException if name is null or empty
     */
    public Builder withForbiddenEnvironmentName(final String name) {
      PanamaValidation.requireNonEmpty(name, "name");
      forbiddenEnvironmentNames.add(name);
      return this;
    }

    /**
     * Builds the security validator with the configured settings.
     *
     * @return the configured security validator
     */
    public WasiSecurityValidator build() {
      return new WasiSecurityValidator(this);
    }
  }
}
