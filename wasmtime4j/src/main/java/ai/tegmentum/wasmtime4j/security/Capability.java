package ai.tegmentum.wasmtime4j.security;

/**
 * Enumeration of security capabilities for WebAssembly runtime operations.
 *
 * <p>Capabilities define specific permissions that can be granted or denied in a capability-based
 * security model. Each capability represents a distinct operation or resource access that requires
 * explicit authorization.
 *
 * @since 1.0.0
 */
public enum Capability {

  // === Module Operations ===
  /** Load WebAssembly modules from any source. */
  MODULE_LOAD("module.load", "Load WebAssembly modules"),

  /** Load WebAssembly modules only from trusted sources. */
  MODULE_LOAD_TRUSTED("module.load.trusted", "Load modules from trusted sources only"),

  /** Compile WebAssembly modules. */
  MODULE_COMPILE("module.compile", "Compile WebAssembly modules"),

  /** Serialize and deserialize WebAssembly modules. */
  MODULE_SERIALIZE("module.serialize", "Serialize/deserialize modules"),

  // === Instance Operations ===
  /** Create WebAssembly instances. */
  INSTANCE_CREATE("instance.create", "Create WebAssembly instances"),

  /** Execute WebAssembly functions. */
  INSTANCE_EXECUTE("instance.execute", "Execute WebAssembly functions"),

  /** Access WebAssembly instance exports. */
  INSTANCE_EXPORT_ACCESS("instance.export.access", "Access instance exports"),

  // === Memory Operations ===
  /** Read from WebAssembly linear memory. */
  MEMORY_READ("memory.read", "Read from WebAssembly memory"),

  /** Write to WebAssembly linear memory. */
  MEMORY_WRITE("memory.write", "Write to WebAssembly memory"),

  /** Grow WebAssembly linear memory. */
  MEMORY_GROW("memory.grow", "Grow WebAssembly memory"),

  /** Access memory outside allocated bounds (unsafe). */
  MEMORY_UNSAFE_ACCESS("memory.unsafe", "Unsafe memory access"),

  // === Host Function Operations ===
  /** Call host functions from WebAssembly. */
  HOST_FUNCTION_CALL("host.function.call", "Call host functions"),

  /** Define new host functions. */
  HOST_FUNCTION_DEFINE("host.function.define", "Define host functions"),

  /** Access native system APIs through host functions. */
  HOST_NATIVE_ACCESS("host.native.access", "Access native system APIs"),

  // === File System Operations ===
  /** Read files through WASI. */
  FILESYSTEM_READ("filesystem.read", "Read files"),

  /** Write files through WASI. */
  FILESYSTEM_WRITE("filesystem.write", "Write files"),

  /** Create directories and files through WASI. */
  FILESYSTEM_CREATE("filesystem.create", "Create files and directories"),

  /** Delete files and directories through WASI. */
  FILESYSTEM_DELETE("filesystem.delete", "Delete files and directories"),

  /** Access filesystem metadata. */
  FILESYSTEM_METADATA("filesystem.metadata", "Access filesystem metadata"),

  // === Network Operations ===
  /** Establish network connections. */
  NETWORK_CONNECT("network.connect", "Establish network connections"),

  /** Bind to network ports (server operations). */
  NETWORK_BIND("network.bind", "Bind to network ports"),

  /** Perform DNS lookups. */
  NETWORK_DNS("network.dns", "Perform DNS lookups"),

  /** Access raw sockets (privileged). */
  NETWORK_RAW_SOCKET("network.raw", "Access raw sockets"),

  // === Process Operations ===
  /** Spawn child processes. */
  PROCESS_SPAWN("process.spawn", "Spawn child processes"),

  /** Send signals to processes. */
  PROCESS_SIGNAL("process.signal", "Send process signals"),

  /** Access process information. */
  PROCESS_INFO("process.info", "Access process information"),

  // === Environment Operations ===
  /** Read environment variables. */
  ENVIRONMENT_READ("environment.read", "Read environment variables"),

  /** Modify environment variables. */
  ENVIRONMENT_WRITE("environment.write", "Modify environment variables"),

  /** Access system time and clocks. */
  TIME_ACCESS("time.access", "Access system time"),

  /** Generate random numbers. */
  RANDOM_GENERATE("random.generate", "Generate random numbers"),

  // === Threading Operations ===
  /** Create threads. */
  THREAD_CREATE("thread.create", "Create threads"),

  /** Use synchronization primitives. */
  THREAD_SYNC("thread.sync", "Use thread synchronization"),

  /** Access shared memory between threads. */
  THREAD_SHARED_MEMORY("thread.shared_memory", "Access shared memory"),

  // === Debugging and Profiling ===
  /** Enable debugging features. */
  DEBUG_ENABLE("debug.enable", "Enable debugging features"),

  /** Access profiling and performance data. */
  PROFILING_ACCESS("profiling.access", "Access profiling data"),

  /** Inject debug breakpoints. */
  DEBUG_BREAKPOINT("debug.breakpoint", "Inject debug breakpoints"),

  // === System and Security ===
  /** Modify security policies at runtime. */
  SECURITY_POLICY_MODIFY("security.policy.modify", "Modify security policies"),

  /** Access security audit logs. */
  SECURITY_AUDIT_ACCESS("security.audit.access", "Access security audit logs"),

  /** Bypass certain security restrictions (administrative). */
  SECURITY_BYPASS("security.bypass", "Bypass security restrictions"),

  /** Access system administration functions. */
  SYSTEM_ADMIN("system.admin", "System administration"),

  // === Resource Management ===
  /** Exceed default resource limits. */
  RESOURCE_LIMIT_OVERRIDE("resource.limit.override", "Override resource limits"),

  /** Access unlimited memory allocation. */
  RESOURCE_UNLIMITED_MEMORY("resource.unlimited.memory", "Unlimited memory access"),

  /** Access unlimited execution time. */
  RESOURCE_UNLIMITED_TIME("resource.unlimited.time", "Unlimited execution time");

  private final String id;
  private final String description;

  Capability(final String id, final String description) {
    this.id = id;
    this.description = description;
  }

  /**
   * Gets the unique identifier for this capability.
   *
   * @return capability identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the human-readable description of this capability.
   *
   * @return capability description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this capability implies the given capability.
   *
   * <p>Some capabilities include others. For example, SECURITY_BYPASS implies most other
   * capabilities.
   *
   * @param other the capability to check
   * @return true if this capability implies the other
   */
  public boolean implies(final Capability other) {
    if (this == other) {
      return true;
    }

    switch (this) {
      case SECURITY_BYPASS:
        // Security bypass implies most capabilities except admin
        return other != SYSTEM_ADMIN;

      case SYSTEM_ADMIN:
        // System admin implies all capabilities
        return true;

      case HOST_NATIVE_ACCESS:
        // Native access implies most host capabilities
        return other == HOST_FUNCTION_CALL || other == HOST_FUNCTION_DEFINE;

      case RESOURCE_UNLIMITED_MEMORY:
        // Unlimited memory implies memory operations
        return other == MEMORY_READ
            || other == MEMORY_WRITE
            || other == MEMORY_GROW
            || other == MEMORY_UNSAFE_ACCESS;

      case RESOURCE_UNLIMITED_TIME:
        // Unlimited time implies execution capabilities
        return other == INSTANCE_EXECUTE;

      case FILESYSTEM_WRITE:
        // Write access implies create capability
        return other == FILESYSTEM_CREATE;

      case FILESYSTEM_DELETE:
        // Delete implies write access
        return other == FILESYSTEM_WRITE || other == FILESYSTEM_CREATE;

      default:
        return false;
    }
  }

  /**
   * Gets the security risk level of this capability.
   *
   * @return risk level (LOW, MEDIUM, HIGH, CRITICAL)
   */
  public RiskLevel getRiskLevel() {
    switch (this) {
      case SECURITY_BYPASS:
      case SYSTEM_ADMIN:
      case MEMORY_UNSAFE_ACCESS:
      case SECURITY_POLICY_MODIFY:
        return RiskLevel.CRITICAL;

      case HOST_NATIVE_ACCESS:
      case PROCESS_SPAWN:
      case NETWORK_RAW_SOCKET:
      case FILESYSTEM_DELETE:
      case RESOURCE_UNLIMITED_MEMORY:
      case RESOURCE_UNLIMITED_TIME:
        return RiskLevel.HIGH;

      case FILESYSTEM_WRITE:
      case NETWORK_CONNECT:
      case THREAD_CREATE:
      case ENVIRONMENT_WRITE:
      case DEBUG_ENABLE:
        return RiskLevel.MEDIUM;

      default:
        return RiskLevel.LOW;
    }
  }

  /** Risk levels for security capabilities. */
  public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }
}
