package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WasiConfigurationException class. */
class WasiConfigurationExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Simple message constructor creates configuration exception correctly")
    void testSimpleMessageConstructor() {
      final String message = "Configuration failed";
      final WasiConfigurationException exception = new WasiConfigurationException(message);

      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
      assertEquals("configuration", exception.getOperation());
      assertEquals(WasiConfigurationException.ConfigurationArea.SYSTEM, exception.getConfigurationArea());
      assertNull(exception.getConfigurationParameter());
      assertNull(exception.getProvidedValue());
      assertNull(exception.getExpectedValue());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.CONFIGURATION, exception.getCategory());
    }

    @Test
    @DisplayName("Message with cause constructor creates exception correctly")
    void testMessageWithCauseConstructor() {
      final String message = "Configuration failed";
      final RuntimeException cause = new RuntimeException("Config error");
      final WasiConfigurationException exception = new WasiConfigurationException(message, cause);

      assertEquals(message, exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals("configuration", exception.getOperation());
      assertEquals(WasiConfigurationException.ConfigurationArea.SYSTEM, exception.getConfigurationArea());
    }

    @Test
    @DisplayName("Configuration area constructor creates exception correctly")
    void testConfigurationAreaConstructor() {
      final String message = "Environment configuration failed";
      final WasiConfigurationException.ConfigurationArea configArea = WasiConfigurationException.ConfigurationArea.ENVIRONMENT;

      final WasiConfigurationException exception = new WasiConfigurationException(message, configArea);

      assertEquals(message, exception.getMessage());
      assertEquals("environment-configuration", exception.getOperation());
      assertEquals("environment", exception.getResource());
      assertEquals(configArea, exception.getConfigurationArea());
      assertNull(exception.getConfigurationParameter());
      assertNull(exception.getProvidedValue());
      assertNull(exception.getExpectedValue());
    }

    @Test
    @DisplayName("Parameter-specific constructor creates exception correctly")
    void testParameterSpecificConstructor() {
      final String message = "Invalid parameter value";
      final WasiConfigurationException.ConfigurationArea configArea = WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS;
      final String parameter = "max_memory";
      final String providedValue = "-1";
      final String expectedValue = "positive integer";

      final WasiConfigurationException exception = new WasiConfigurationException(message, configArea, parameter, providedValue, expectedValue);

      assertTrue(exception.getMessage().contains(message));
      assertTrue(exception.getMessage().contains("[parameter: max_memory]"));
      assertTrue(exception.getMessage().contains("[provided: -1]"));
      assertTrue(exception.getMessage().contains("[expected: positive integer]"));
      assertEquals("resource-limits-configuration", exception.getOperation());
      assertEquals("resource-limits:max_memory", exception.getResource());
      assertEquals(configArea, exception.getConfigurationArea());
      assertEquals(parameter, exception.getConfigurationParameter());
      assertEquals(providedValue, exception.getProvidedValue());
      assertEquals(expectedValue, exception.getExpectedValue());
    }

    @Test
    @DisplayName("Full constructor with cause creates exception correctly")
    void testFullConstructorWithCause() {
      final String message = "Network configuration error";
      final WasiConfigurationException.ConfigurationArea configArea = WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION;
      final String parameter = "port";
      final String providedValue = "invalid";
      final String expectedValue = "1-65535";
      final RuntimeException cause = new RuntimeException("Parse error");

      final WasiConfigurationException exception = new WasiConfigurationException(message, configArea, parameter, providedValue, expectedValue, cause);

      assertTrue(exception.getMessage().contains(message));
      assertEquals(cause, exception.getCause());
      assertEquals("network-configuration", exception.getOperation());
      assertEquals("network-configuration:port", exception.getResource());
      assertEquals(configArea, exception.getConfigurationArea());
      assertEquals(parameter, exception.getConfigurationParameter());
      assertEquals(providedValue, exception.getProvidedValue());
      assertEquals(expectedValue, exception.getExpectedValue());
    }

    @Test
    @DisplayName("Constructor with null message throws IllegalArgumentException")
    void testConstructorWithNullMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiConfigurationException(null, WasiConfigurationException.ConfigurationArea.SYSTEM, "param", "value", "expected"));
    }

    @Test
    @DisplayName("Constructor with empty message throws IllegalArgumentException")
    void testConstructorWithEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiConfigurationException("", WasiConfigurationException.ConfigurationArea.SYSTEM, "param", "value", "expected"));
    }
  }

  @Nested
  @DisplayName("Configuration Area Tests")
  class ConfigurationAreaTests {

    @Test
    @DisplayName("isEnvironmentError returns true for ENVIRONMENT area")
    void testIsEnvironmentError() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);
      assertTrue(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    @DisplayName("isFileSystemPermissionsError returns true for FILE_SYSTEM_PERMISSIONS area")
    void testIsFileSystemPermissionsError() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS);
      assertFalse(exception.isEnvironmentError());
      assertTrue(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    @DisplayName("isNetworkConfigurationError returns true for NETWORK_CONFIGURATION area")
    void testIsNetworkConfigurationError() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertTrue(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    @DisplayName("isComponentInstantiationError returns true for COMPONENT_INSTANTIATION area")
    void testIsComponentInstantiationError() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.COMPONENT_INSTANTIATION);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertTrue(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    @DisplayName("isResourceLimitsError returns true for RESOURCE_LIMITS area")
    void testIsResourceLimitsError() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertTrue(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    @DisplayName("isRuntimeEngineError returns true for RUNTIME_ENGINE area")
    void testIsRuntimeEngineError() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.RUNTIME_ENGINE);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertTrue(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    @DisplayName("isSecurityPolicyError returns true for SECURITY_POLICY area")
    void testIsSecurityPolicyError() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.SECURITY_POLICY);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertTrue(exception.isSecurityPolicyError());
    }
  }

  @Nested
  @DisplayName("Configuration Guidance Tests")
  class ConfigurationGuidanceTests {

    @Test
    @DisplayName("getConfigurationGuidance provides parameter-specific guidance")
    void testGetConfigurationGuidanceWithParameter() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Error",
          WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS,
          "max_memory",
          "invalid",
          "positive integer");

      final String guidance = exception.getConfigurationGuidance();
      assertTrue(guidance.contains("Set 'max_memory' to positive integer"));
      assertTrue(guidance.contains("resource quotas and limits"));
    }

    @Test
    @DisplayName("getConfigurationGuidance provides area-specific guidance without parameters")
    void testGetConfigurationGuidanceWithoutParameter() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Error",
          WasiConfigurationException.ConfigurationArea.ENVIRONMENT);

      final String guidance = exception.getConfigurationGuidance();
      assertTrue(guidance.contains("Check configuration for environment"));
      assertTrue(guidance.contains("Environment variables"));
    }

    @Test
    @DisplayName("getConfigurationGuidance provides correct guidance for all configuration areas")
    void testGetConfigurationGuidanceForAllAreas() {
      // Test each configuration area has appropriate guidance
      final WasiConfigurationException.ConfigurationArea[] areas = WasiConfigurationException.ConfigurationArea.values();
      
      for (final WasiConfigurationException.ConfigurationArea area : areas) {
        final WasiConfigurationException exception = new WasiConfigurationException("Error", area);
        final String guidance = exception.getConfigurationGuidance();
        assertNotNull(guidance);
        assertFalse(guidance.isEmpty());
        
        // Each area should have specific guidance
        switch (area) {
          case ENVIRONMENT:
            assertTrue(guidance.contains("Environment variables"));
            break;
          case FILE_SYSTEM_PERMISSIONS:
            assertTrue(guidance.contains("file system permissions"));
            break;
          case NETWORK_CONFIGURATION:
            assertTrue(guidance.contains("network settings"));
            break;
          case COMPONENT_INSTANTIATION:
            assertTrue(guidance.contains("component parameters"));
            break;
          case RESOURCE_LIMITS:
            assertTrue(guidance.contains("resource quotas"));
            break;
          case RUNTIME_ENGINE:
            assertTrue(guidance.contains("runtime engine"));
            break;
          case SECURITY_POLICY:
            assertTrue(guidance.contains("security policies"));
            break;
          case SYSTEM:
            assertTrue(guidance.contains("system-level"));
            break;
        }
      }
    }
  }

  @Nested
  @DisplayName("Message Formatting Tests")
  class MessageFormattingTests {

    @Test
    @DisplayName("Operation formatting works correctly for all configuration areas")
    void testOperationFormatting() {
      assertEquals("environment-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT).getOperation());
      assertEquals("file-system-permissions-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS).getOperation());
      assertEquals("network-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION).getOperation());
      assertEquals("component-instantiation-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.COMPONENT_INSTANTIATION).getOperation());
      assertEquals("resource-limits-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS).getOperation());
      assertEquals("runtime-engine-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.RUNTIME_ENGINE).getOperation());
      assertEquals("security-policy-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.SECURITY_POLICY).getOperation());
      assertEquals("system-configuration", 
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.SYSTEM).getOperation());
    }

    @Test
    @DisplayName("Resource formatting works correctly with area only")
    void testResourceFormattingAreaOnly() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);
      assertEquals("environment", exception.getResource());
    }

    @Test
    @DisplayName("Resource formatting works correctly with area and parameter")
    void testResourceFormattingAreaAndParameter() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT, "PATH", "invalid", "valid path");
      assertEquals("environment:PATH", exception.getResource());
    }

    @Test
    @DisplayName("Detailed message formatting includes all parameter information")
    void testDetailedMessageFormatting() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Base error", WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS, "max_memory", "-1", "positive integer");

      final String message = exception.getMessage();
      assertTrue(message.contains("Base error"));
      assertTrue(message.contains("[parameter: max_memory]"));
      assertTrue(message.contains("[provided: -1]"));
      assertTrue(message.contains("[expected: positive integer]"));
    }

    @Test
    @DisplayName("Detailed message formatting handles null values gracefully")
    void testDetailedMessageFormattingWithNulls() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Base error", WasiConfigurationException.ConfigurationArea.SYSTEM, null, null, null);

      final String message = exception.getMessage();
      assertTrue(message.contains("Base error"));
      // Should not contain parameter information for null values
      assertFalse(message.contains("[parameter:"));
      assertFalse(message.contains("[provided:"));
      assertFalse(message.contains("[expected:"));
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("WasiConfigurationException extends WasiException")
    void testWasiConfigurationExceptionExtendsWasiException() {
      final WasiConfigurationException exception = new WasiConfigurationException("Test error");
      assertTrue(exception instanceof WasiException);
    }

    @Test
    @DisplayName("WasiConfigurationException extends WasmException")
    void testWasiConfigurationExceptionExtendsWasmException() {
      final WasiConfigurationException exception = new WasiConfigurationException("Test error");
      assertTrue(exception instanceof WasmException);
    }

    @Test
    @DisplayName("Configuration exceptions are never retryable")
    void testConfigurationExceptionsNotRetryable() {
      final WasiConfigurationException exception = new WasiConfigurationException("Test error");
      assertFalse(exception.isRetryable());
      
      // All configuration areas should be non-retryable
      for (final WasiConfigurationException.ConfigurationArea area : WasiConfigurationException.ConfigurationArea.values()) {
        final WasiConfigurationException areaException = new WasiConfigurationException("Error", area);
        assertFalse(areaException.isRetryable());
      }
    }
  }

  @Nested
  @DisplayName("Configuration Area Enum Tests")
  class ConfigurationAreaEnumTests {

    @Test
    @DisplayName("All ConfigurationArea values are properly defined")
    void testConfigurationAreaValues() {
      final WasiConfigurationException.ConfigurationArea[] areas = WasiConfigurationException.ConfigurationArea.values();
      assertEquals(8, areas.length);
      
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.ENVIRONMENT));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.COMPONENT_INSTANTIATION));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.RUNTIME_ENGINE));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.SECURITY_POLICY));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.SYSTEM));
    }

    private boolean contains(final WasiConfigurationException.ConfigurationArea[] array, final WasiConfigurationException.ConfigurationArea value) {
      for (final WasiConfigurationException.ConfigurationArea area : array) {
        if (area == value) {
          return true;
        }
      }
      return false;
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Empty parameter name handling")
    void testEmptyParameterNameHandling() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Error", WasiConfigurationException.ConfigurationArea.SYSTEM, "", "value", "expected");
      assertEquals("", exception.getConfigurationParameter());
    }

    @Test
    @DisplayName("Empty provided value handling")
    void testEmptyProvidedValueHandling() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Error", WasiConfigurationException.ConfigurationArea.SYSTEM, "param", "", "expected");
      assertEquals("", exception.getProvidedValue());
    }

    @Test
    @DisplayName("Empty expected value handling")
    void testEmptyExpectedValueHandling() {
      final WasiConfigurationException exception = new WasiConfigurationException(
          "Error", WasiConfigurationException.ConfigurationArea.SYSTEM, "param", "value", "");
      assertEquals("", exception.getExpectedValue());
    }
  }
}