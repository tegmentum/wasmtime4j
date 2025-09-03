package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WASI configuration operations fail.
 *
 * <p>This exception is thrown when WASI configuration operations fail, including:
 *
 * <ul>
 *   <li>Invalid configuration parameters
 *   <li>Configuration validation errors
 *   <li>Environment setup failures
 *   <li>Runtime configuration conflicts
 *   <li>Permission configuration errors
 * </ul>
 *
 * <p>Configuration exceptions provide detailed information about configuration failures, including
 * the specific configuration parameter, expected values, and remediation guidance.
 *
 * @since 1.0.0
 */
public class WasiConfigurationException extends WasiException {

  private static final long serialVersionUID = 1L;

  /** The configuration parameter that caused the error. */
  private final String configurationParameter;

  /** The invalid value that was provided. */
  private final String providedValue;

  /** The expected value or format description. */
  private final String expectedValue;

  /** The configuration area that failed. */
  private final ConfigurationArea configurationArea;

  /** Configuration areas for better error categorization and handling. */
  public enum ConfigurationArea {
    /** Environment variables and system environment setup. */
    ENVIRONMENT,
    /** File system permissions and path configuration. */
    FILE_SYSTEM_PERMISSIONS,
    /** Network configuration and socket permissions. */
    NETWORK_CONFIGURATION,
    /** Component instantiation parameters. */
    COMPONENT_INSTANTIATION,
    /** Resource limits and quotas. */
    RESOURCE_LIMITS,
    /** Runtime engine configuration. */
    RUNTIME_ENGINE,
    /** Security policy configuration. */
    SECURITY_POLICY,
    /** General system configuration. */
    SYSTEM
  }

  /**
   * Creates a new configuration exception with the specified message.
   *
   * @param message the error message
   */
  public WasiConfigurationException(final String message) {
    super(message, "configuration", null, false, ErrorCategory.CONFIGURATION);
    this.configurationParameter = null;
    this.providedValue = null;
    this.expectedValue = null;
    this.configurationArea = ConfigurationArea.SYSTEM;
  }

  /**
   * Creates a new configuration exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasiConfigurationException(final String message, final Throwable cause) {
    super(message, "configuration", null, false, ErrorCategory.CONFIGURATION, cause);
    this.configurationParameter = null;
    this.providedValue = null;
    this.expectedValue = null;
    this.configurationArea = ConfigurationArea.SYSTEM;
  }

  /**
   * Creates a new configuration exception with configuration area context.
   *
   * @param message the error message
   * @param configurationArea the configuration area that failed
   */
  public WasiConfigurationException(
      final String message, final ConfigurationArea configurationArea) {
    super(
        message,
        formatOperation(configurationArea),
        formatResourceIdentifier(configurationArea, null),
        false,
        ErrorCategory.CONFIGURATION);
    this.configurationParameter = null;
    this.providedValue = null;
    this.expectedValue = null;
    this.configurationArea = configurationArea;
  }

  /**
   * Creates a new configuration exception with parameter-specific details.
   *
   * @param message the error message
   * @param configurationArea the configuration area that failed
   * @param configurationParameter the specific parameter that caused the error
   * @param providedValue the invalid value that was provided
   * @param expectedValue the expected value or format description
   */
  public WasiConfigurationException(
      final String message,
      final ConfigurationArea configurationArea,
      final String configurationParameter,
      final String providedValue,
      final String expectedValue) {
    super(
        formatDetailedMessage(message, configurationParameter, providedValue, expectedValue),
        formatOperation(configurationArea),
        formatResourceIdentifier(configurationArea, configurationParameter),
        false,
        ErrorCategory.CONFIGURATION);
    this.configurationParameter = configurationParameter;
    this.providedValue = providedValue;
    this.expectedValue = expectedValue;
    this.configurationArea = configurationArea;
  }

  /**
   * Creates a new configuration exception with full details and cause.
   *
   * @param message the error message
   * @param configurationArea the configuration area that failed
   * @param configurationParameter the specific parameter that caused the error
   * @param providedValue the invalid value that was provided
   * @param expectedValue the expected value or format description
   * @param cause the underlying cause
   */
  public WasiConfigurationException(
      final String message,
      final ConfigurationArea configurationArea,
      final String configurationParameter,
      final String providedValue,
      final String expectedValue,
      final Throwable cause) {
    super(
        formatDetailedMessage(message, configurationParameter, providedValue, expectedValue),
        formatOperation(configurationArea),
        formatResourceIdentifier(configurationArea, configurationParameter),
        false,
        ErrorCategory.CONFIGURATION,
        cause);
    this.configurationParameter = configurationParameter;
    this.providedValue = providedValue;
    this.expectedValue = expectedValue;
    this.configurationArea = configurationArea;
  }

  /**
   * Gets the configuration parameter that caused the error.
   *
   * @return the configuration parameter name, or null if not specified
   */
  public String getConfigurationParameter() {
    return configurationParameter;
  }

  /**
   * Gets the invalid value that was provided.
   *
   * @return the provided value, or null if not specified
   */
  public String getProvidedValue() {
    return providedValue;
  }

  /**
   * Gets the expected value or format description.
   *
   * @return the expected value description, or null if not specified
   */
  public String getExpectedValue() {
    return expectedValue;
  }

  /**
   * Gets the configuration area that failed.
   *
   * @return the configuration area
   */
  public ConfigurationArea getConfigurationArea() {
    return configurationArea;
  }

  /**
   * Checks if this is an environment configuration error.
   *
   * @return true if this is an environment error, false otherwise
   */
  public boolean isEnvironmentError() {
    return configurationArea == ConfigurationArea.ENVIRONMENT;
  }

  /**
   * Checks if this is a file system permissions configuration error.
   *
   * @return true if this is a file system permissions error, false otherwise
   */
  public boolean isFileSystemPermissionsError() {
    return configurationArea == ConfigurationArea.FILE_SYSTEM_PERMISSIONS;
  }

  /**
   * Checks if this is a network configuration error.
   *
   * @return true if this is a network configuration error, false otherwise
   */
  public boolean isNetworkConfigurationError() {
    return configurationArea == ConfigurationArea.NETWORK_CONFIGURATION;
  }

  /**
   * Checks if this is a component instantiation configuration error.
   *
   * @return true if this is a component instantiation error, false otherwise
   */
  public boolean isComponentInstantiationError() {
    return configurationArea == ConfigurationArea.COMPONENT_INSTANTIATION;
  }

  /**
   * Checks if this is a resource limits configuration error.
   *
   * @return true if this is a resource limits error, false otherwise
   */
  public boolean isResourceLimitsError() {
    return configurationArea == ConfigurationArea.RESOURCE_LIMITS;
  }

  /**
   * Checks if this is a runtime engine configuration error.
   *
   * @return true if this is a runtime engine error, false otherwise
   */
  public boolean isRuntimeEngineError() {
    return configurationArea == ConfigurationArea.RUNTIME_ENGINE;
  }

  /**
   * Checks if this is a security policy configuration error.
   *
   * @return true if this is a security policy error, false otherwise
   */
  public boolean isSecurityPolicyError() {
    return configurationArea == ConfigurationArea.SECURITY_POLICY;
  }

  /**
   * Creates a helpful configuration guidance message.
   *
   * @return guidance message for fixing the configuration issue
   */
  public String getConfigurationGuidance() {
    final StringBuilder guidance = new StringBuilder();

    if (configurationParameter != null && expectedValue != null) {
      guidance.append("Set '").append(configurationParameter).append("' to ").append(expectedValue);
    } else {
      guidance
          .append("Check configuration for ")
          .append(configurationArea.name().toLowerCase().replace('_', ' '));
    }

    switch (configurationArea) {
      case ENVIRONMENT:
        guidance.append(". Environment variables may need to be set or updated.");
        break;
      case FILE_SYSTEM_PERMISSIONS:
        guidance.append(". Verify file system permissions and path accessibility.");
        break;
      case NETWORK_CONFIGURATION:
        guidance.append(". Check network settings and socket permissions.");
        break;
      case COMPONENT_INSTANTIATION:
        guidance.append(". Review component parameters and dependencies.");
        break;
      case RESOURCE_LIMITS:
        guidance.append(". Adjust resource quotas and limits as needed.");
        break;
      case RUNTIME_ENGINE:
        guidance.append(". Verify runtime engine settings and capabilities.");
        break;
      case SECURITY_POLICY:
        guidance.append(". Review security policies and permissions.");
        break;
      case SYSTEM:
        guidance.append(". Check system-level configuration settings.");
        break;
      default:
        guidance.append(". Review configuration settings.");
        break;
    }

    return guidance.toString();
  }

  /**
   * Formats the operation for display.
   *
   * @param configurationArea the configuration area
   * @return formatted operation string
   */
  private static String formatOperation(final ConfigurationArea configurationArea) {
    if (configurationArea == null) {
      return "configuration";
    }

    final String areaName = configurationArea.name().toLowerCase().replace('_', '-');
    
    // Avoid redundant "-configuration" suffix for areas that already include "configuration"
    if (areaName.endsWith("-configuration")) {
      return areaName;
    }
    
    return areaName + "-configuration";
  }

  /**
   * Formats the resource identifier combining area and parameter.
   *
   * @param configurationArea the configuration area
   * @param configurationParameter the configuration parameter
   * @return formatted resource string
   */
  private static String formatResourceIdentifier(
      final ConfigurationArea configurationArea, final String configurationParameter) {
    if (configurationArea == null && configurationParameter == null) {
      return null;
    }

    if (configurationParameter == null) {
      return configurationArea != null
          ? configurationArea.name().toLowerCase().replace('_', '-')
          : null;
    }

    if (configurationArea == null) {
      return configurationParameter;
    }

    return configurationArea.name().toLowerCase().replace('_', '-') + ":" + configurationParameter;
  }

  /**
   * Formats a detailed message with parameter information.
   *
   * @param baseMessage the base message
   * @param parameter the configuration parameter
   * @param providedValue the provided value
   * @param expectedValue the expected value
   * @return formatted detailed message
   */
  private static String formatDetailedMessage(
      final String baseMessage,
      final String parameter,
      final String providedValue,
      final String expectedValue) {
    if (baseMessage == null || baseMessage.isEmpty()) {
      throw new IllegalArgumentException("Base message cannot be null or empty");
    }

    final StringBuilder message = new StringBuilder(baseMessage);

    if (parameter != null) {
      message.append(" [parameter: ").append(parameter).append("]");
    }

    if (providedValue != null) {
      message.append(" [provided: ").append(providedValue).append("]");
    }

    if (expectedValue != null) {
      message.append(" [expected: ").append(expectedValue).append("]");
    }

    return message.toString();
  }
}
