package ai.tegmentum.wasmtime4j.security;

/**
 * Types of security events that can be monitored and audited.
 *
 * @since 1.0.0
 */
public enum SecurityEventType {

  // === Authentication and Authorization Events ===
  AUTHENTICATION_SUCCESS("auth.success", "Authentication successful"),
  AUTHENTICATION_FAILURE("auth.failure", "Authentication failed"),
  AUTHORIZATION_GRANTED("authz.granted", "Authorization granted"),
  AUTHORIZATION_DENIED("authz.denied", "Authorization denied"),
  CAPABILITY_GRANTED("capability.granted", "Capability granted"),
  CAPABILITY_REVOKED("capability.revoked", "Capability revoked"),
  CAPABILITY_VIOLATION("capability.violation", "Capability violation attempted"),

  // === Module and Instance Events ===
  MODULE_LOADED("module.loaded", "WebAssembly module loaded"),
  MODULE_LOAD_FAILED("module.load.failed", "Module loading failed"),
  MODULE_VALIDATION_FAILED("module.validation.failed", "Module validation failed"),
  MODULE_HASH_MISMATCH("module.hash.mismatch", "Module hash verification failed"),
  INSTANCE_CREATED("instance.created", "WebAssembly instance created"),
  INSTANCE_CREATION_FAILED("instance.creation.failed", "Instance creation failed"),
  INSTANCE_TERMINATED("instance.terminated", "Instance terminated"),

  // === Execution Events ===
  EXECUTION_STARTED("execution.started", "WebAssembly execution started"),
  EXECUTION_COMPLETED("execution.completed", "WebAssembly execution completed"),
  EXECUTION_FAILED("execution.failed", "WebAssembly execution failed"),
  EXECUTION_TIMEOUT("execution.timeout", "Execution timeout exceeded"),
  EXECUTION_INTERRUPTED("execution.interrupted", "Execution interrupted"),
  TRAP_OCCURRED("trap.occurred", "WebAssembly trap occurred"),

  // === Resource Events ===
  RESOURCE_LIMIT_EXCEEDED("resource.limit.exceeded", "Resource limit exceeded"),
  MEMORY_ALLOCATION_FAILED("memory.allocation.failed", "Memory allocation failed"),
  MEMORY_GROWTH_ATTEMPTED("memory.growth.attempted", "Memory growth attempted"),
  MEMORY_LIMIT_REACHED("memory.limit.reached", "Memory limit reached"),
  INSTRUCTION_LIMIT_REACHED("instruction.limit.reached", "Instruction limit reached"),
  TIME_LIMIT_EXCEEDED("time.limit.exceeded", "Time limit exceeded"),

  // === Host Function Events ===
  HOST_FUNCTION_CALLED("host.function.called", "Host function called"),
  HOST_FUNCTION_FAILED("host.function.failed", "Host function call failed"),
  HOST_FUNCTION_BLOCKED("host.function.blocked", "Host function call blocked"),
  NATIVE_ACCESS_ATTEMPTED("native.access.attempted", "Native system access attempted"),
  NATIVE_ACCESS_DENIED("native.access.denied", "Native system access denied"),

  // === File System Events ===
  FILE_ACCESS_ATTEMPTED("file.access.attempted", "File access attempted"),
  FILE_ACCESS_DENIED("file.access.denied", "File access denied"),
  FILE_READ("file.read", "File read operation"),
  FILE_WRITE("file.write", "File write operation"),
  FILE_DELETE("file.delete", "File delete operation"),
  DIRECTORY_ACCESS("directory.access", "Directory access"),
  PATH_TRAVERSAL_ATTEMPTED("path.traversal.attempted", "Path traversal attempted"),

  // === Network Events ===
  NETWORK_CONNECTION_ATTEMPTED("network.connection.attempted", "Network connection attempted"),
  NETWORK_CONNECTION_DENIED("network.connection.denied", "Network connection denied"),
  NETWORK_CONNECTION_ESTABLISHED(
      "network.connection.established", "Network connection established"),
  NETWORK_DATA_SENT("network.data.sent", "Network data sent"),
  NETWORK_DATA_RECEIVED("network.data.received", "Network data received"),
  DNS_LOOKUP_ATTEMPTED("dns.lookup.attempted", "DNS lookup attempted"),

  // === Process Events ===
  PROCESS_SPAWN_ATTEMPTED("process.spawn.attempted", "Process spawn attempted"),
  PROCESS_SPAWN_DENIED("process.spawn.denied", "Process spawn denied"),
  PROCESS_SIGNAL_SENT("process.signal.sent", "Process signal sent"),
  ENVIRONMENT_ACCESS("environment.access", "Environment variable access"),

  // === Security Violations ===
  SECURITY_POLICY_VIOLATION("security.policy.violation", "Security policy violation"),
  INTRUSION_DETECTED("intrusion.detected", "Intrusion detected"),
  ANOMALOUS_BEHAVIOR("anomalous.behavior", "Anomalous behavior detected"),
  SUSPICIOUS_ACTIVITY("suspicious.activity", "Suspicious activity detected"),
  ATTACK_PATTERN_DETECTED("attack.pattern.detected", "Attack pattern detected"),
  BUFFER_OVERFLOW_ATTEMPTED("buffer.overflow.attempted", "Buffer overflow attempted"),
  CODE_INJECTION_ATTEMPTED("code.injection.attempted", "Code injection attempted"),

  // === Sandbox Events ===
  SANDBOX_CREATED("sandbox.created", "Sandbox created"),
  SANDBOX_DESTROYED("sandbox.destroyed", "Sandbox destroyed"),
  SANDBOX_BREACH_ATTEMPTED("sandbox.breach.attempted", "Sandbox breach attempted"),
  ISOLATION_VIOLATION("isolation.violation", "Isolation violation"),
  ESCAPE_ATTEMPT("escape.attempt", "Sandbox escape attempt"),

  // === Configuration Events ===
  SECURITY_POLICY_CHANGED("security.policy.changed", "Security policy changed"),
  RESOURCE_LIMITS_CHANGED("resource.limits.changed", "Resource limits changed"),
  CONFIGURATION_UPDATED("configuration.updated", "Configuration updated"),
  CAPABILITY_POLICY_UPDATED("capability.policy.updated", "Capability policy updated"),

  // === Debug and Development Events ===
  DEBUG_MODE_ENABLED("debug.enabled", "Debug mode enabled"),
  DEBUG_MODE_DISABLED("debug.disabled", "Debug mode disabled"),
  BREAKPOINT_SET("breakpoint.set", "Debug breakpoint set"),
  PROFILING_STARTED("profiling.started", "Profiling started"),
  PROFILING_STOPPED("profiling.stopped", "Profiling stopped"),

  // === System Events ===
  SYSTEM_SHUTDOWN("system.shutdown", "System shutdown initiated"),
  GARBAGE_COLLECTION("gc.occurred", "Garbage collection occurred"),
  MEMORY_PRESSURE("memory.pressure", "Memory pressure detected"),
  PERFORMANCE_DEGRADATION("performance.degradation", "Performance degradation detected"),

  // === Audit Events ===
  AUDIT_LOG_ROTATION("audit.log.rotation", "Audit log rotation"),
  AUDIT_LOG_TAMPERING("audit.log.tampering", "Audit log tampering detected"),
  SECURITY_SCAN_COMPLETED("security.scan.completed", "Security scan completed"),
  COMPLIANCE_CHECK("compliance.check", "Compliance check performed");

  private final String id;
  private final String description;

  SecurityEventType(final String id, final String description) {
    this.id = id;
    this.description = description;
  }

  /**
   * Gets the unique identifier for this event type.
   *
   * @return event type identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the human-readable description of this event type.
   *
   * @return event type description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the severity level of this event type.
   *
   * @return severity level
   */
  public SecuritySeverity getSeverity() {
    switch (this) {
      case INTRUSION_DETECTED:
      case ATTACK_PATTERN_DETECTED:
      case SANDBOX_BREACH_ATTEMPTED:
      case ESCAPE_ATTEMPT:
      case BUFFER_OVERFLOW_ATTEMPTED:
      case CODE_INJECTION_ATTEMPTED:
      case AUDIT_LOG_TAMPERING:
        return SecuritySeverity.CRITICAL;

      case SECURITY_POLICY_VIOLATION:
      case CAPABILITY_VIOLATION:
      case RESOURCE_LIMIT_EXCEEDED:
      case ISOLATION_VIOLATION:
      case PATH_TRAVERSAL_ATTEMPTED:
      case AUTHENTICATION_FAILURE:
      case NATIVE_ACCESS_DENIED:
      case ANOMALOUS_BEHAVIOR:
        return SecuritySeverity.HIGH;

      case AUTHORIZATION_DENIED:
      case NETWORK_CONNECTION_DENIED:
      case FILE_ACCESS_DENIED:
      case PROCESS_SPAWN_DENIED:
      case MODULE_VALIDATION_FAILED:
      case EXECUTION_TIMEOUT:
      case SUSPICIOUS_ACTIVITY:
        return SecuritySeverity.MEDIUM;

      case AUTHENTICATION_SUCCESS:
      case AUTHORIZATION_GRANTED:
      case MODULE_LOADED:
      case INSTANCE_CREATED:
      case EXECUTION_COMPLETED:
      case AUDIT_LOG_ROTATION:
      case SECURITY_SCAN_COMPLETED:
        return SecuritySeverity.INFO;

      default:
        return SecuritySeverity.LOW;
    }
  }

  /**
   * Checks if this event type indicates a security violation.
   *
   * @return true if this is a security violation event
   */
  public boolean isSecurityViolation() {
    switch (this) {
      case SECURITY_POLICY_VIOLATION:
      case CAPABILITY_VIOLATION:
      case INTRUSION_DETECTED:
      case SANDBOX_BREACH_ATTEMPTED:
      case ESCAPE_ATTEMPT:
      case ISOLATION_VIOLATION:
      case BUFFER_OVERFLOW_ATTEMPTED:
      case CODE_INJECTION_ATTEMPTED:
      case PATH_TRAVERSAL_ATTEMPTED:
      case ATTACK_PATTERN_DETECTED:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if this event type should trigger immediate alerts.
   *
   * @return true if this event should trigger alerts
   */
  public boolean requiresImmediateAlert() {
    return getSeverity() == SecuritySeverity.CRITICAL || getSeverity() == SecuritySeverity.HIGH;
  }
}
