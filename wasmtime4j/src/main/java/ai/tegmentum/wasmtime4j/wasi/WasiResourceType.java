package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI resource types in Preview 2.
 *
 * <p>Defines the different types of resources that can be managed by the WASI Preview 2 system,
 * each with their own capabilities, permissions, and lifecycle management requirements.
 *
 * @since 1.0.0
 */
public enum WasiResourceType {
  /**
   * Filesystem resource for file and directory operations.
   */
  FILESYSTEM("filesystem", "File system access and operations"),

  /**
   * Network resource for socket and network operations.
   */
  NETWORK("network", "Network connectivity and socket operations"),

  /**
   * Process resource for process spawning and management.
   */
  PROCESS("process", "Process creation and management"),

  /**
   * Environment resource for environment variable access.
   */
  ENVIRONMENT("environment", "Environment variable access"),

  /**
   * Time resource for time and clock operations.
   */
  TIME("time", "Time and clock access"),

  /**
   * Random resource for random number generation.
   */
  RANDOM("random", "Random number generation"),

  /**
   * Stdio resource for standard input/output operations.
   */
  STDIO("stdio", "Standard input/output streams"),

  /**
   * Memory resource for shared memory operations.
   */
  MEMORY("memory", "Shared memory management"),

  /**
   * Thread resource for threading operations.
   */
  THREAD("thread", "Thread creation and management"),

  /**
   * Crypto resource for cryptographic operations.
   */
  CRYPTO("crypto", "Cryptographic operations"),

  /**
   * HTTP resource for HTTP client/server operations.
   */
  HTTP("http", "HTTP client and server operations"),

  /**
   * Database resource for database connectivity.
   */
  DATABASE("database", "Database connectivity and operations"),

  /**
   * Log resource for logging operations.
   */
  LOG("log", "Logging and diagnostic output"),

  /**
   * Custom resource for user-defined resource types.
   */
  CUSTOM("custom", "User-defined resource type");

  private final String name;
  private final String description;

  WasiResourceType(final String name, final String description) {
    this.name = name;
    this.description = description;
  }

  /**
   * Gets the resource type name.
   *
   * @return the resource type name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the resource type description.
   *
   * @return the resource type description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this resource type requires special permissions.
   *
   * @return true if special permissions are required, false otherwise
   */
  public boolean requiresSpecialPermissions() {
    return this == FILESYSTEM || this == NETWORK || this == PROCESS || this == CRYPTO;
  }

  /**
   * Checks if this resource type can be shared between components.
   *
   * @return true if the resource can be shared, false otherwise
   */
  public boolean isShareable() {
    return this == TIME || this == RANDOM || this == LOG || this == ENVIRONMENT;
  }

  /**
   * Checks if this resource type requires cleanup on component termination.
   *
   * @return true if cleanup is required, false otherwise
   */
  public boolean requiresCleanup() {
    return this == FILESYSTEM || this == NETWORK || this == PROCESS || this == MEMORY
        || this == THREAD || this == DATABASE || this == HTTP;
  }

  /**
   * Gets the default permissions for this resource type.
   *
   * @return the default permissions
   */
  public WasiResourcePermissions getDefaultPermissions() {
    switch (this) {
      case FILESYSTEM:
        return WasiResourcePermissions.READ_ONLY;
      case NETWORK:
        return WasiResourcePermissions.READ_WRITE;
      case PROCESS:
        return WasiResourcePermissions.EXECUTE;
      case ENVIRONMENT:
        return WasiResourcePermissions.READ_ONLY;
      case TIME:
      case RANDOM:
      case STDIO:
        return WasiResourcePermissions.READ_WRITE;
      case MEMORY:
      case THREAD:
        return WasiResourcePermissions.READ_WRITE;
      case CRYPTO:
        return WasiResourcePermissions.EXECUTE;
      case HTTP:
      case DATABASE:
        return WasiResourcePermissions.READ_WRITE;
      case LOG:
        return WasiResourcePermissions.WRITE_ONLY;
      case CUSTOM:
      default:
        return WasiResourcePermissions.NONE;
    }
  }

  /**
   * Finds a resource type by name.
   *
   * @param name the resource type name to find
   * @return the resource type, or null if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  public static WasiResourceType fromName(final String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Resource type name cannot be null or empty");
    }

    for (final WasiResourceType type : values()) {
      if (type.name.equalsIgnoreCase(name.trim())) {
        return type;
      }
    }

    return null;
  }

  /**
   * Checks if a resource type name is valid.
   *
   * @param name the resource type name to check
   * @return true if the name corresponds to a known resource type, false otherwise
   */
  public static boolean isValidName(final String name) {
    return fromName(name) != null;
  }
}