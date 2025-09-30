package ai.tegmentum.wasmtime4j;

/**
 * Component security policy interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentSecurityPolicy {

  /**
   * Gets the policy name.
   *
   * @return policy name
   */
  String getName();

  /**
   * Gets the policy version.
   *
   * @return policy version
   */
  String getVersion();

  /**
   * Gets access control rules.
   *
   * @return access control rules
   */
  java.util.List<AccessRule> getAccessRules();

  /**
   * Gets resource permissions.
   *
   * @return resource permissions
   */
  ResourcePermissions getResourcePermissions();

  /**
   * Gets execution restrictions.
   *
   * @return execution restrictions
   */
  ExecutionRestrictions getExecutionRestrictions();

  /**
   * Gets security constraints.
   *
   * @return security constraints
   */
  SecurityConstraints getSecurityConstraints();

  /**
   * Validates a security request.
   *
   * @param request security request
   * @return validation result
   */
  ValidationResult validate(SecurityRequest request);

  /**
   * Checks if an operation is allowed.
   *
   * @param operation operation to check
   * @return true if allowed
   */
  boolean isAllowed(SecurityOperation operation);

  /**
   * Gets audit configuration.
   *
   * @return audit configuration
   */
  AuditConfig getAuditConfig();

  /** Access rule interface. */
  interface AccessRule {
    /**
     * Gets rule name.
     *
     * @return rule name
     */
    String getName();

    /**
     * Gets rule type.
     *
     * @return rule type
     */
    RuleType getType();

    /**
     * Gets rule action.
     *
     * @return rule action
     */
    RuleAction getAction();

    /**
     * Gets rule conditions.
     *
     * @return rule conditions
     */
    java.util.List<RuleCondition> getConditions();

    /**
     * Gets rule priority.
     *
     * @return rule priority
     */
    int getPriority();

    /**
     * Checks if rule matches operation.
     *
     * @param operation operation to check
     * @return true if matches
     */
    boolean matches(SecurityOperation operation);
  }

  /** Resource permissions interface. */
  interface ResourcePermissions {
    /**
     * Gets memory permissions.
     *
     * @return memory permissions
     */
    MemoryPermissions getMemoryPermissions();

    /**
     * Gets file system permissions.
     *
     * @return file system permissions
     */
    FileSystemPermissions getFileSystemPermissions();

    /**
     * Gets network permissions.
     *
     * @return network permissions
     */
    NetworkPermissions getNetworkPermissions();

    /**
     * Gets environment permissions.
     *
     * @return environment permissions
     */
    EnvironmentPermissions getEnvironmentPermissions();
  }

  /** Memory permissions interface. */
  interface MemoryPermissions {
    /**
     * Gets maximum memory size.
     *
     * @return max memory in bytes
     */
    long getMaxMemorySize();

    /**
     * Checks if memory growth is allowed.
     *
     * @return true if allowed
     */
    boolean isGrowthAllowed();

    /**
     * Gets allowed memory regions.
     *
     * @return memory regions
     */
    java.util.List<MemoryRegion> getAllowedRegions();
  }

  /** File system permissions interface. */
  interface FileSystemPermissions {
    /**
     * Gets read permissions.
     *
     * @return read permissions
     */
    java.util.Set<String> getReadPermissions();

    /**
     * Gets write permissions.
     *
     * @return write permissions
     */
    java.util.Set<String> getWritePermissions();

    /**
     * Gets execute permissions.
     *
     * @return execute permissions
     */
    java.util.Set<String> getExecutePermissions();

    /**
     * Checks if path is allowed.
     *
     * @param path file path
     * @param action requested action
     * @return true if allowed
     */
    boolean isPathAllowed(String path, FileAction action);
  }

  /** Network permissions interface. */
  interface NetworkPermissions {
    /**
     * Gets allowed outbound hosts.
     *
     * @return outbound hosts
     */
    java.util.Set<String> getOutboundHosts();

    /**
     * Gets allowed inbound ports.
     *
     * @return inbound ports
     */
    java.util.Set<Integer> getInboundPorts();

    /**
     * Gets protocol restrictions.
     *
     * @return protocol restrictions
     */
    java.util.Set<String> getAllowedProtocols();

    /**
     * Checks if connection is allowed.
     *
     * @param host target host
     * @param port target port
     * @param protocol protocol
     * @return true if allowed
     */
    boolean isConnectionAllowed(String host, int port, String protocol);
  }

  /** Environment permissions interface. */
  interface EnvironmentPermissions {
    /**
     * Gets readable environment variables.
     *
     * @return readable variables
     */
    java.util.Set<String> getReadableVariables();

    /**
     * Gets writable environment variables.
     *
     * @return writable variables
     */
    java.util.Set<String> getWritableVariables();

    /**
     * Checks if variable access is allowed.
     *
     * @param variable variable name
     * @param action requested action
     * @return true if allowed
     */
    boolean isVariableAllowed(String variable, EnvironmentAction action);
  }

  /** Execution restrictions interface. */
  interface ExecutionRestrictions {
    /**
     * Gets maximum execution time.
     *
     * @return max time in milliseconds
     */
    long getMaxExecutionTime();

    /**
     * Gets maximum fuel consumption.
     *
     * @return max fuel
     */
    long getMaxFuelConsumption();

    /**
     * Gets maximum instruction count.
     *
     * @return max instructions
     */
    long getMaxInstructionCount();

    /**
     * Gets allowed function calls.
     *
     * @return allowed functions
     */
    java.util.Set<String> getAllowedFunctions();

    /**
     * Gets forbidden function calls.
     *
     * @return forbidden functions
     */
    java.util.Set<String> getForbiddenFunctions();
  }

  /** Security constraints interface. */
  interface SecurityConstraints {
    /**
     * Checks if sandboxing is required.
     *
     * @return true if required
     */
    boolean isSandboxingRequired();

    /**
     * Checks if code signing is required.
     *
     * @return true if required
     */
    boolean isCodeSigningRequired();

    /**
     * Gets required security level.
     *
     * @return security level
     */
    SecurityLevel getRequiredSecurityLevel();

    /**
     * Gets encryption requirements.
     *
     * @return encryption requirements
     */
    EncryptionRequirements getEncryptionRequirements();
  }

  /** Security request interface. */
  interface SecurityRequest {
    /**
     * Gets request operation.
     *
     * @return operation
     */
    SecurityOperation getOperation();

    /**
     * Gets request context.
     *
     * @return context
     */
    SecurityContext getContext();

    /**
     * Gets request timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();
  }

  /** Security operation interface. */
  interface SecurityOperation {
    /**
     * Gets operation type.
     *
     * @return operation type
     */
    OperationType getType();

    /**
     * Gets operation target.
     *
     * @return target resource
     */
    String getTarget();

    /**
     * Gets operation parameters.
     *
     * @return parameters
     */
    java.util.Map<String, Object> getParameters();
  }

  /** Security context interface. */
  interface SecurityContext {
    /**
     * Gets user principal.
     *
     * @return user principal
     */
    String getPrincipal();

    /**
     * Gets user roles.
     *
     * @return user roles
     */
    java.util.Set<String> getRoles();

    /**
     * Gets context attributes.
     *
     * @return attributes
     */
    java.util.Map<String, Object> getAttributes();
  }

  /** Validation result interface. */
  interface ValidationResult {
    /**
     * Checks if validation passed.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Gets validation errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets applied rules.
     *
     * @return list of applied rules
     */
    java.util.List<AccessRule> getAppliedRules();
  }

  /** Audit configuration interface. */
  interface AuditConfig {
    /**
     * Checks if auditing is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets audit level.
     *
     * @return audit level
     */
    AuditLevel getLevel();

    /**
     * Gets audit destinations.
     *
     * @return audit destinations
     */
    java.util.List<String> getDestinations();
  }

  /** Memory region interface. */
  interface MemoryRegion {
    /**
     * Gets region start address.
     *
     * @return start address
     */
    long getStartAddress();

    /**
     * Gets region size.
     *
     * @return region size
     */
    long getSize();

    /**
     * Gets region permissions.
     *
     * @return permissions
     */
    java.util.Set<MemoryPermission> getPermissions();
  }

  /** Encryption requirements interface. */
  interface EncryptionRequirements {
    /**
     * Checks if data encryption is required.
     *
     * @return true if required
     */
    boolean isDataEncryptionRequired();

    /**
     * Checks if transport encryption is required.
     *
     * @return true if required
     */
    boolean isTransportEncryptionRequired();

    /**
     * Gets minimum key length.
     *
     * @return minimum key length
     */
    int getMinKeyLength();

    /**
     * Gets allowed algorithms.
     *
     * @return allowed algorithms
     */
    java.util.Set<String> getAllowedAlgorithms();
  }

  /** Rule condition interface. */
  interface RuleCondition {
    /**
     * Gets condition field.
     *
     * @return field name
     */
    String getField();

    /**
     * Gets condition operator.
     *
     * @return operator
     */
    ConditionOperator getOperator();

    /**
     * Gets condition value.
     *
     * @return condition value
     */
    Object getValue();

    /**
     * Tests condition against operation.
     *
     * @param operation operation to test
     * @return true if condition matches
     */
    boolean test(SecurityOperation operation);
  }

  /** Rule type enumeration. */
  enum RuleType {
    /** Allow rule. */
    ALLOW,
    /** Deny rule. */
    DENY,
    /** Audit rule. */
    AUDIT,
    /** Rate limit rule. */
    RATE_LIMIT
  }

  /** Rule action enumeration. */
  enum RuleAction {
    /** Permit action. */
    PERMIT,
    /** Deny action. */
    DENY,
    /** Log action. */
    LOG,
    /** Alert action. */
    ALERT,
    /** Block action. */
    BLOCK
  }

  /** Operation type enumeration. */
  enum OperationType {
    /** Memory operation. */
    MEMORY,
    /** File system operation. */
    FILE_SYSTEM,
    /** Network operation. */
    NETWORK,
    /** Function call. */
    FUNCTION_CALL,
    /** Resource access. */
    RESOURCE_ACCESS
  }

  /** File action enumeration. */
  enum FileAction {
    /** Read action. */
    READ,
    /** Write action. */
    WRITE,
    /** Execute action. */
    EXECUTE,
    /** Delete action. */
    DELETE,
    /** Create action. */
    CREATE
  }

  /** Environment action enumeration. */
  enum EnvironmentAction {
    /** Read action. */
    READ,
    /** Write action. */
    WRITE
  }

  /** Security level enumeration. */
  enum SecurityLevel {
    /** Low security. */
    LOW,
    /** Medium security. */
    MEDIUM,
    /** High security. */
    HIGH,
    /** Maximum security. */
    MAXIMUM
  }

  /** Memory permission enumeration. */
  enum MemoryPermission {
    /** Read permission. */
    READ,
    /** Write permission. */
    WRITE,
    /** Execute permission. */
    EXECUTE
  }

  /** Audit level enumeration. */
  enum AuditLevel {
    /** No auditing. */
    NONE,
    /** Error auditing. */
    ERROR,
    /** Warning auditing. */
    WARNING,
    /** Info auditing. */
    INFO,
    /** Debug auditing. */
    DEBUG
  }

  /** Condition operator enumeration. */
  enum ConditionOperator {
    /** Equals. */
    EQUALS,
    /** Not equals. */
    NOT_EQUALS,
    /** Contains. */
    CONTAINS,
    /** Starts with. */
    STARTS_WITH,
    /** Ends with. */
    ENDS_WITH,
    /** Matches regex. */
    MATCHES,
    /** In list. */
    IN,
    /** Not in list. */
    NOT_IN
  }
}
