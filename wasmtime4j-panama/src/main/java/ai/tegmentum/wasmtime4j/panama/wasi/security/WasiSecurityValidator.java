package ai.tegmentum.wasmtime4j.panama.wasi.security;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Security validation and path traversal protection for WASI operations in Panama FFI implementation.
 *
 * <p>This class provides comprehensive security validation for WASI operations with a focus on
 * preventing unauthorized system access. Security features include:
 *
 * <ul>
 *   <li>Path traversal attack prevention (../ sequences, symbolic links)
 *   <li>Dangerous file extension detection and blocking
 *   <li>System directory access prevention
 *   <li>Environment variable access validation
 *   <li>File name pattern validation
 *   <li>Configurable security policies
 * </ul>
 *
 * <p>Security is the highest priority - all operations are validated defensively to prevent any
 * possibility of escape from the sandbox environment.
 *
 * @since 1.0.0
 */
public final class WasiSecurityValidator {

  private static final Logger LOGGER = Logger.getLogger(WasiSecurityValidator.class.getName());

  /** Dangerous file extensions that should be blocked by default. */
  private static final Set<String> DANGEROUS_EXTENSIONS =
      Set.of(
          ".exe", ".com", ".bat", ".cmd", ".pif", ".scr", ".vbs", ".js", ".jar", ".war", ".ear",
          ".sh", ".bash", ".zsh", ".fish", ".csh", ".tcsh", ".py", ".pl", ".rb", ".php", ".jsp",
          ".asp", ".aspx", ".dll", ".so", ".dylib");

  /** System directories that should be blocked by default. */
  private static final Set<String> SYSTEM_DIRECTORIES =
      Set.of(
          "/bin", "/sbin", "/usr/bin", "/usr/sbin", "/boot", "/dev", "/proc", "/sys", "/run",
          "/lib", "/usr/lib", "/lib64", "/usr/lib64", "/etc/passwd", "/etc/shadow", "/etc/sudoers",
          "C:\\Windows", "C:\\Program Files", "C:\\Program Files (x86)", "C:\\System32",
          "/System", "/Library", "/Applications", "/Users/.Trash");

  /** Dangerous path patterns to detect and block. */
  private static final Set<Pattern> DANGEROUS_PATH_PATTERNS =
      Set.of(
          Pattern.compile("\\.\\.[\\\\/]"), // Path traversal attempts
          Pattern.compile("[\\\\/]\\.\\.[^\\\\/]"), // Hidden path traversal
          Pattern.compile("^[a-zA-Z]:[^\\\\/]"), // Windows drive without slash
          Pattern.compile("[\0\r\n]"), // Null bytes and line breaks
          Pattern.compile("[\u202E\u202D]"), // Unicode direction override attacks
          Pattern.compile(".*\\.(lnk|url)$", Pattern.CASE_INSENSITIVE)); // Windows shortcuts

  /** Blocked environment variable names for security. */
  private static final Set<String> BLOCKED_ENVIRONMENT_VARIABLES =
      Set.of(
          "PATH", "LD_LIBRARY_PATH", "DYLD_LIBRARY_PATH", "JAVA_HOME", "CLASSPATH", "HOME",
          "USER", "USERNAME", "SUDO_USER", "SHELL", "TERM", "DISPLAY", "PWD", "OLDPWD");

  /** Whether to enforce strict security policies. */
  private final boolean strictMode;

  /** Whether to allow symbolic link following. */
  private final boolean allowSymbolicLinks;

  /** Whether to allow hidden files and directories. */
  private final boolean allowHiddenFiles;

  /** Custom dangerous extensions in addition to defaults. */
  private final Set<String> customDangerousExtensions;

  /** Custom blocked directories in addition to defaults. */
  private final Set<String> customBlockedDirectories;

  /** Custom allowed environment variables that override defaults. */
  private final Set<String> customAllowedEnvironmentVariables;

  /** Maximum path length allowed. */
  private final int maxPathLength;

  /** Maximum file name length allowed. */
  private final int maxFileNameLength;

  /**
   * Creates a new WASI security validator with the specified configuration.
   *
   * @param builder the security validator builder
   */
  private WasiSecurityValidator(final Builder builder) {
    this.strictMode = builder.strictMode;
    this.allowSymbolicLinks = builder.allowSymbolicLinks;
    this.allowHiddenFiles = builder.allowHiddenFiles;
    this.customDangerousExtensions = Set.copyOf(builder.customDangerousExtensions);
    this.customBlockedDirectories = Set.copyOf(builder.customBlockedDirectories);
    this.customAllowedEnvironmentVariables = Set.copyOf(builder.customAllowedEnvironmentVariables);
    this.maxPathLength = builder.maxPathLength;
    this.maxFileNameLength = builder.maxFileNameLength;

    LOGGER.info(
        String.format(
            "Created Panama WASI security validator: strict=%s, symlinks=%s, hidden=%s, "
                + "maxPathLength=%d, maxFileNameLength=%d",
            strictMode, allowSymbolicLinks, allowHiddenFiles, maxPathLength, maxFileNameLength));
  }

  /**
   * Validates that a file path is secure and does not pose security risks.
   *
   * <p>This method performs comprehensive security validation including:
   *
   * <ul>
   *   <li>Path traversal attack detection
   *   <li>Dangerous file extension checking
   *   <li>System directory access prevention
   *   <li>Path length validation
   *   <li>File name pattern validation
   * </ul>
   *
   * @param path the file path to validate
   * @throws RuntimeException if the path poses security risks
   */
  public void validatePath(final Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    final String pathString = path.toString();

    LOGGER.fine(String.format("Validating path security: %s", pathString));

    // Basic path validation
    validatePathLength(pathString);
    validateFileNameLength(path);

    // Path traversal validation
    validatePathTraversal(pathString);

    // Dangerous pattern validation
    validateDangerousPatterns(pathString);

    // System directory validation
    validateSystemDirectoryAccess(path);

    // File extension validation
    validateFileExtension(pathString);

    // Hidden file validation
    validateHiddenFile(path);

    // Symbolic link validation
    if (!allowSymbolicLinks) {
      validateSymbolicLinks(path);
    }

    LOGGER.fine(String.format("Path validation completed successfully: %s", pathString));
  }

  /**
   * Validates environment variable access for security.
   *
   * @param variableName the environment variable name to validate
   * @throws RuntimeException if access to the variable poses security risks
   */
  public void validateEnvironmentAccess(final String variableName) {
    if (variableName == null || variableName.isEmpty()) {
      throw new IllegalArgumentException("Variable name cannot be null or empty");
    }

    LOGGER.fine(String.format("Validating environment variable access: %s", variableName));

    // Check if explicitly allowed
    if (customAllowedEnvironmentVariables.contains(variableName)) {
      LOGGER.fine(String.format("Environment variable explicitly allowed: %s", variableName));
      return;
    }

    // Check if blocked for security
    if (BLOCKED_ENVIRONMENT_VARIABLES.contains(variableName)) {
      throw new RuntimeException(
          String.format(
              "Environment variable access denied for security reasons: %s", variableName));
    }

    // In strict mode, validate variable name patterns
    if (strictMode) {
      validateEnvironmentVariableName(variableName);
    }

    LOGGER.fine(String.format("Environment variable access validated: %s", variableName));
  }

  /**
   * Creates a default security validator with balanced security settings.
   *
   * @return a default security validator
   */
  public static WasiSecurityValidator defaultValidator() {
    return builder()
        .withStrictMode(false)
        .withSymbolicLinks(false)
        .withHiddenFiles(false)
        .withMaxPathLength(4096)
        .withMaxFileNameLength(255)
        .build();
  }

  /**
   * Creates a strict security validator with maximum security settings.
   *
   * @return a strict security validator
   */
  public static WasiSecurityValidator strictValidator() {
    return builder()
        .withStrictMode(true)
        .withSymbolicLinks(false)
        .withHiddenFiles(false)
        .withMaxPathLength(1024)
        .withMaxFileNameLength(100)
        .build();
  }

  /**
   * Creates a permissive security validator with relaxed security settings.
   *
   * @return a permissive security validator
   */
  public static WasiSecurityValidator permissiveValidator() {
    return builder()
        .withStrictMode(false)
        .withSymbolicLinks(true)
        .withHiddenFiles(true)
        .withMaxPathLength(8192)
        .withMaxFileNameLength(512)
        .build();
  }

  /**
   * Creates a new security validator builder.
   *
   * @return a new security validator builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Validates path length against configured limits. */
  private void validatePathLength(final String pathString) {
    if (pathString.length() > maxPathLength) {
      throw new RuntimeException(
          String.format(
              "Path too long: %d characters (maximum: %d)", pathString.length(), maxPathLength));
    }
  }

  /** Validates file name length against configured limits. */
  private void validateFileNameLength(final Path path) {
    final Path fileName = path.getFileName();
    if (fileName != null) {
      final String fileNameString = fileName.toString();
      if (fileNameString.length() > maxFileNameLength) {
        throw new RuntimeException(
            String.format(
                "File name too long: %d characters (maximum: %d)",
                fileNameString.length(), maxFileNameLength));
      }
    }
  }

  /** Validates for path traversal attacks. */
  private void validatePathTraversal(final String pathString) {
    // Normalize path to detect traversal attempts
    final Path normalizedPath = Paths.get(pathString).normalize();
    final String normalizedString = normalizedPath.toString();

    // Check for traversal patterns
    for (final Pattern pattern : DANGEROUS_PATH_PATTERNS) {
      if (pattern.matcher(pathString).find() || pattern.matcher(normalizedString).find()) {
        throw new RuntimeException(
            String.format("Path contains dangerous pattern: %s", pathString));
      }
    }

    // Additional check for parent directory references
    if (normalizedString.contains("..") || pathString.contains("..")) {
      throw new RuntimeException(
          String.format("Path contains parent directory references: %s", pathString));
    }
  }

  /** Validates for dangerous patterns in the path. */
  private void validateDangerousPatterns(final String pathString) {
    // Check for null bytes and control characters
    for (int i = 0; i < pathString.length(); i++) {
      final char c = pathString.charAt(i);
      if (c < 32 && c != '\t') { // Allow tab but block other control characters
        throw new RuntimeException(
            String.format("Path contains control character at position %d: %s", i, pathString));
      }
    }

    // Check for Unicode attacks
    if (pathString.contains("\u202E") || pathString.contains("\u202D")) {
      throw new RuntimeException(
          String.format("Path contains Unicode direction override attack: %s", pathString));
    }
  }

  /** Validates system directory access. */
  private void validateSystemDirectoryAccess(final Path path) {
    final Path absolutePath = path.toAbsolutePath().normalize();
    final String pathString = absolutePath.toString();

    for (final String systemDir : SYSTEM_DIRECTORIES) {
      if (pathString.startsWith(systemDir)) {
        throw new RuntimeException(
            String.format("Access to system directory denied: %s", pathString));
      }
    }

    for (final String customBlockedDir : customBlockedDirectories) {
      if (pathString.startsWith(customBlockedDir)) {
        throw new RuntimeException(
            String.format("Access to blocked directory denied: %s", pathString));
      }
    }
  }

  /** Validates file extensions for dangerous types. */
  private void validateFileExtension(final String pathString) {
    final String lowerPath = pathString.toLowerCase();

    for (final String extension : DANGEROUS_EXTENSIONS) {
      if (lowerPath.endsWith(extension)) {
        throw new RuntimeException(
            String.format("Dangerous file extension not allowed: %s", pathString));
      }
    }

    for (final String customExtension : customDangerousExtensions) {
      if (lowerPath.endsWith(customExtension.toLowerCase())) {
        throw new RuntimeException(
            String.format("Custom dangerous file extension not allowed: %s", pathString));
      }
    }
  }

  /** Validates hidden file access if not allowed. */
  private void validateHiddenFile(final Path path) {
    if (!allowHiddenFiles) {
      final Path fileName = path.getFileName();
      if (fileName != null && fileName.toString().startsWith(".")) {
        throw new RuntimeException(
            String.format("Hidden file access not allowed: %s", path));
      }
    }
  }

  /** Validates symbolic links if not allowed. */
  private void validateSymbolicLinks(final Path path) {
    try {
      if (java.nio.file.Files.isSymbolicLink(path)) {
        throw new RuntimeException(
            String.format("Symbolic link access not allowed: %s", path));
      }
    } catch (final SecurityException e) {
      // If we can't check due to security restrictions, be conservative
      LOGGER.warning(
          String.format("Cannot check symbolic link status for path: %s", path));
      if (strictMode) {
        throw new RuntimeException(
            String.format("Cannot verify symbolic link status: %s", path));
      }
    }
  }

  /** Validates environment variable name patterns. */
  private void validateEnvironmentVariableName(final String variableName) {
    // Check for dangerous patterns in variable names
    if (!variableName.matches("^[A-Z_][A-Z0-9_]*$")) {
      throw new RuntimeException(
          String.format("Invalid environment variable name pattern: %s", variableName));
    }

    // Check for excessively long variable names
    if (variableName.length() > 128) {
      throw new RuntimeException(
          String.format("Environment variable name too long: %s", variableName));
    }
  }

  /**
   * Builder for creating WASI security validators.
   */
  public static final class Builder {

    private boolean strictMode = false;
    private boolean allowSymbolicLinks = false;
    private boolean allowHiddenFiles = false;
    private final Set<String> customDangerousExtensions = ConcurrentHashMap.newKeySet();
    private final Set<String> customBlockedDirectories = ConcurrentHashMap.newKeySet();
    private final Set<String> customAllowedEnvironmentVariables = ConcurrentHashMap.newKeySet();
    private int maxPathLength = 4096;
    private int maxFileNameLength = 255;

    private Builder() {}

    /**
     * Sets strict mode enforcement.
     *
     * @param strictMode whether to enforce strict security policies
     * @return this builder for method chaining
     */
    public Builder withStrictMode(final boolean strictMode) {
      this.strictMode = strictMode;
      return this;
    }

    /**
     * Sets symbolic link following permission.
     *
     * @param allowSymbolicLinks whether to allow following symbolic links
     * @return this builder for method chaining
     */
    public Builder withSymbolicLinks(final boolean allowSymbolicLinks) {
      this.allowSymbolicLinks = allowSymbolicLinks;
      return this;
    }

    /**
     * Sets hidden file access permission.
     *
     * @param allowHiddenFiles whether to allow access to hidden files
     * @return this builder for method chaining
     */
    public Builder withHiddenFiles(final boolean allowHiddenFiles) {
      this.allowHiddenFiles = allowHiddenFiles;
      return this;
    }

    /**
     * Adds a custom dangerous file extension.
     *
     * @param extension the file extension to block (with leading dot)
     * @return this builder for method chaining
     */
    public Builder withDangerousExtension(final String extension) {
      if (extension == null || extension.isEmpty()) {
        throw new IllegalArgumentException("Extension cannot be null or empty");
      }
      customDangerousExtensions.add(extension);
      return this;
    }

    /**
     * Adds a custom blocked directory.
     *
     * @param directory the directory path to block
     * @return this builder for method chaining
     */
    public Builder withBlockedDirectory(final String directory) {
      if (directory == null || directory.isEmpty()) {
        throw new IllegalArgumentException("Directory cannot be null or empty");
      }
      customBlockedDirectories.add(directory);
      return this;
    }

    /**
     * Adds a custom allowed environment variable.
     *
     * @param variableName the environment variable name to allow
     * @return this builder for method chaining
     */
    public Builder withAllowedEnvironmentVariable(final String variableName) {
      if (variableName == null || variableName.isEmpty()) {
        throw new IllegalArgumentException("Variable name cannot be null or empty");
      }
      customAllowedEnvironmentVariables.add(variableName);
      return this;
    }

    /**
     * Sets the maximum path length.
     *
     * @param maxPathLength the maximum path length
     * @return this builder for method chaining
     */
    public Builder withMaxPathLength(final int maxPathLength) {
      if (maxPathLength <= 0) {
        throw new IllegalArgumentException("Max path length must be positive");
      }
      this.maxPathLength = maxPathLength;
      return this;
    }

    /**
     * Sets the maximum file name length.
     *
     * @param maxFileNameLength the maximum file name length
     * @return this builder for method chaining
     */
    public Builder withMaxFileNameLength(final int maxFileNameLength) {
      if (maxFileNameLength <= 0) {
        throw new IllegalArgumentException("Max file name length must be positive");
      }
      this.maxFileNameLength = maxFileNameLength;
      return this;
    }

    /**
     * Builds the security validator.
     *
     * @return the configured security validator
     */
    public WasiSecurityValidator build() {
      return new WasiSecurityValidator(this);
    }
  }
}