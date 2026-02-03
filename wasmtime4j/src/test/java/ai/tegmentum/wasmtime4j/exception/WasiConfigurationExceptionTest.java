package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for WasiConfigurationException class. */
class WasiConfigurationExceptionTest {

  @Nested
  class ConstructorTests {

    @Test
    void testSimpleMessageConstructor() {
      final String message = "Configuration failed";
      final WasiConfigurationException exception = new WasiConfigurationException(message);

      assertEquals("Configuration failed (operation: configuration)", exception.getMessage());
      assertNull(exception.getCause());
      assertEquals("configuration", exception.getOperation());
      assertEquals(
          WasiConfigurationException.ConfigurationArea.SYSTEM, exception.getConfigurationArea());
      assertNull(exception.getConfigurationParameter());
      assertNull(exception.getProvidedValue());
      assertNull(exception.getExpectedValue());
      assertFalse(exception.isRetryable());
      assertEquals(WasiException.ErrorCategory.CONFIGURATION, exception.getCategory());
    }

    @Test
    void testMessageWithCauseConstructor() {
      final String message = "Configuration failed";
      final RuntimeException cause = new RuntimeException("Config error");
      final WasiConfigurationException exception = new WasiConfigurationException(message, cause);

      assertEquals("Configuration failed (operation: configuration)", exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals("configuration", exception.getOperation());
      assertEquals(
          WasiConfigurationException.ConfigurationArea.SYSTEM, exception.getConfigurationArea());
    }

    @Test
    void testConfigurationAreaConstructor() {
      final String message = "Environment configuration failed";
      final WasiConfigurationException.ConfigurationArea configArea =
          WasiConfigurationException.ConfigurationArea.ENVIRONMENT;

      final WasiConfigurationException exception =
          new WasiConfigurationException(message, configArea);

      assertEquals(
          "Environment configuration failed (operation: environment-configuration) (resource:"
              + " environment)",
          exception.getMessage());
      assertEquals("environment-configuration", exception.getOperation());
      assertEquals("environment", exception.getResource());
      assertEquals(configArea, exception.getConfigurationArea());
      assertNull(exception.getConfigurationParameter());
      assertNull(exception.getProvidedValue());
      assertNull(exception.getExpectedValue());
    }

    @Test
    void testParameterSpecificConstructor() {
      final String message = "Invalid parameter value";
      final WasiConfigurationException.ConfigurationArea configArea =
          WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS;
      final String parameter = "max_memory";
      final String providedValue = "-1";
      final String expectedValue = "positive integer";

      final WasiConfigurationException exception =
          new WasiConfigurationException(
              message, configArea, parameter, providedValue, expectedValue);

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
    void testFullConstructorWithCause() {
      final String message = "Network configuration error";
      final WasiConfigurationException.ConfigurationArea configArea =
          WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION;
      final String parameter = "port";
      final String providedValue = "invalid";
      final String expectedValue = "1-65535";
      final RuntimeException cause = new RuntimeException("Parse error");

      final WasiConfigurationException exception =
          new WasiConfigurationException(
              message, configArea, parameter, providedValue, expectedValue, cause);

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
    void testConstructorWithNullMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiConfigurationException(
                  null,
                  WasiConfigurationException.ConfigurationArea.SYSTEM,
                  "param",
                  "value",
                  "expected"));
    }

    @Test
    void testConstructorWithEmptyMessage() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiConfigurationException(
                  "",
                  WasiConfigurationException.ConfigurationArea.SYSTEM,
                  "param",
                  "value",
                  "expected"));
    }
  }

  @Nested
  class ConfigurationAreaTests {

    @Test
    void testIsEnvironmentError() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);
      assertTrue(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    void testIsFileSystemPermissionsError() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS);
      assertFalse(exception.isEnvironmentError());
      assertTrue(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    void testIsNetworkConfigurationError() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertTrue(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    void testIsComponentInstantiationError() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.COMPONENT_INSTANTIATION);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertTrue(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    void testIsResourceLimitsError() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertTrue(exception.isResourceLimitsError());
      assertFalse(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    void testIsRuntimeEngineError() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.RUNTIME_ENGINE);
      assertFalse(exception.isEnvironmentError());
      assertFalse(exception.isFileSystemPermissionsError());
      assertFalse(exception.isNetworkConfigurationError());
      assertFalse(exception.isComponentInstantiationError());
      assertFalse(exception.isResourceLimitsError());
      assertTrue(exception.isRuntimeEngineError());
      assertFalse(exception.isSecurityPolicyError());
    }

    @Test
    void testIsSecurityPolicyError() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.SECURITY_POLICY);
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
  class ConfigurationGuidanceTests {

    @Test
    void testGetConfigurationGuidanceWithParameter() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
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
    void testGetConfigurationGuidanceWithoutParameter() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);

      final String guidance = exception.getConfigurationGuidance();
      assertTrue(guidance.contains("Check configuration for environment"));
      assertTrue(guidance.contains("Environment variables"));
    }

    @Test
    void testGetConfigurationGuidanceForAllAreas() {
      // Test each configuration area has appropriate guidance
      final WasiConfigurationException.ConfigurationArea[] areas =
          WasiConfigurationException.ConfigurationArea.values();

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
          default:
            assertTrue(guidance.contains("configuration"));
            break;
        }
      }
    }
  }

  @Nested
  class MessageFormattingTests {

    @Test
    void testOperationFormatting() {
      assertEquals(
          "environment-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT)
              .getOperation());
      assertEquals(
          "file-system-permissions-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS)
              .getOperation());
      assertEquals(
          "network-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION)
              .getOperation());
      assertEquals(
          "component-instantiation-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.COMPONENT_INSTANTIATION)
              .getOperation());
      assertEquals(
          "resource-limits-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS)
              .getOperation());
      assertEquals(
          "runtime-engine-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.RUNTIME_ENGINE)
              .getOperation());
      assertEquals(
          "security-policy-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.SECURITY_POLICY)
              .getOperation());
      assertEquals(
          "system-configuration",
          new WasiConfigurationException(
                  "Error", WasiConfigurationException.ConfigurationArea.SYSTEM)
              .getOperation());
    }

    @Test
    void testResourceFormattingAreaOnly() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);
      assertEquals("environment", exception.getResource());
    }

    @Test
    void testResourceFormattingAreaAndParameter() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.ENVIRONMENT,
              "PATH",
              "invalid",
              "valid path");
      assertEquals("environment:PATH", exception.getResource());
    }

    @Test
    void testDetailedMessageFormatting() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Base error",
              WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS,
              "max_memory",
              "-1",
              "positive integer");

      final String message = exception.getMessage();
      assertTrue(message.contains("Base error"));
      assertTrue(message.contains("[parameter: max_memory]"));
      assertTrue(message.contains("[provided: -1]"));
      assertTrue(message.contains("[expected: positive integer]"));
    }

    @Test
    void testDetailedMessageFormattingWithNulls() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
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
  class InheritanceTests {

    @Test
    void testWasiConfigurationExceptionExtendsWasiException() {
      final WasiConfigurationException exception = new WasiConfigurationException("Test error");
      assertTrue(exception instanceof WasiException);
    }

    @Test
    void testWasiConfigurationExceptionExtendsWasmException() {
      final WasiConfigurationException exception = new WasiConfigurationException("Test error");
      assertTrue(exception instanceof WasmException);
    }

    @Test
    void testConfigurationExceptionsNotRetryable() {
      final WasiConfigurationException exception = new WasiConfigurationException("Test error");
      assertFalse(exception.isRetryable());

      // All configuration areas should be non-retryable
      for (final WasiConfigurationException.ConfigurationArea area :
          WasiConfigurationException.ConfigurationArea.values()) {
        final WasiConfigurationException areaException =
            new WasiConfigurationException("Error", area);
        assertFalse(areaException.isRetryable());
      }
    }
  }

  @Nested
  class ConfigurationAreaEnumTests {

    @Test
    void testConfigurationAreaValues() {
      final WasiConfigurationException.ConfigurationArea[] areas =
          WasiConfigurationException.ConfigurationArea.values();
      assertEquals(8, areas.length);

      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.ENVIRONMENT));
      assertTrue(
          contains(areas, WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS));
      assertTrue(
          contains(areas, WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION));
      assertTrue(
          contains(areas, WasiConfigurationException.ConfigurationArea.COMPONENT_INSTANTIATION));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.RUNTIME_ENGINE));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.SECURITY_POLICY));
      assertTrue(contains(areas, WasiConfigurationException.ConfigurationArea.SYSTEM));
    }

    private boolean contains(
        final WasiConfigurationException.ConfigurationArea[] array,
        final WasiConfigurationException.ConfigurationArea value) {
      for (final WasiConfigurationException.ConfigurationArea area : array) {
        if (area == value) {
          return true;
        }
      }
      return false;
    }
  }

  @Nested
  class EdgeCaseTests {

    @Test
    void testEmptyParameterNameHandling() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.SYSTEM,
              "",
              "value",
              "expected");
      assertEquals("", exception.getConfigurationParameter());
    }

    @Test
    void testEmptyProvidedValueHandling() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.SYSTEM,
              "param",
              "",
              "expected");
      assertEquals("", exception.getProvidedValue());
    }

    @Test
    void testEmptyExpectedValueHandling() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.SYSTEM, "param", "value", "");
      assertEquals("", exception.getExpectedValue());
    }
  }

  @Nested
  class ConstructorRetryableDefaultTests {

    @Test
    void testSimpleMessageConstructorRetryableDefaultsFalse() {
      final WasiConfigurationException exception = new WasiConfigurationException("Error");

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }

    @Test
    void testMessageCauseConstructorRetryableDefaultsFalse() {
      // Kill mutation on line 77: Substituted 0 with 1
      final RuntimeException cause = new RuntimeException("Test cause");
      final WasiConfigurationException exception = new WasiConfigurationException("Error", cause);

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }

    @Test
    void testAreaConstructorRetryableDefaultsFalse() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }

    @Test
    void testFiveArgConstructorRetryableDefaultsFalse() {
      // Kill mutation on line 122: Substituted 0 with 1
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.RESOURCE_LIMITS,
              "param",
              "provided",
              "expected");

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }

    @Test
    void testSixArgConstructorRetryableDefaultsFalse() {
      // Kill mutation on line 151: Substituted 0 with 1
      final RuntimeException cause = new RuntimeException("Test cause");
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION,
              "param",
              "provided",
              "expected",
              cause);

      assertFalse(exception.isRetryable());
      assertEquals(false, exception.isRetryable());
    }
  }

  @Nested
  class NullParameterHandlingTests {

    @Test
    void testFormatOperationWithNullArea() {
      // Test with null configurationArea - formatOperation should return "configuration"
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", (WasiConfigurationException.ConfigurationArea) null);

      assertEquals("configuration", exception.getOperation());
    }

    @Test
    void testFormatResourceIdentifierBothNull() {
      // Test formatResourceIdentifier(null, null) returns null
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", (WasiConfigurationException.ConfigurationArea) null);

      assertNull(exception.getResource());
    }

    @Test
    void testFormatResourceIdentifierNullAreaWithParameter() {
      // Test with null configurationArea but non-null parameter
      // This tests the branch at line 349
      final WasiConfigurationException exception =
          new WasiConfigurationException("Error", null, "my-param", "value", "expected");

      // formatResourceIdentifier(null, "my-param") should return "my-param"
      assertEquals("my-param", exception.getResource());
    }

    @Test
    void testFormatResourceIdentifierNullParameter() {
      // Test with non-null configurationArea but null parameter
      final WasiConfigurationException exception =
          new WasiConfigurationException("Error", WasiConfigurationException.ConfigurationArea.SYSTEM);

      // formatResourceIdentifier(SYSTEM, null) should return "system"
      assertEquals("system", exception.getResource());
    }

    @Test
    void testFiveArgConstructorWithNullArea() {
      final WasiConfigurationException exception =
          new WasiConfigurationException("Error", null, "param", "provided", "expected");

      assertEquals("configuration", exception.getOperation());
      assertEquals("param", exception.getResource());
      assertNull(exception.getConfigurationArea());
    }

    @Test
    void testSixArgConstructorWithNullArea() {
      final RuntimeException cause = new RuntimeException("Test");
      final WasiConfigurationException exception =
          new WasiConfigurationException("Error", null, "param", "provided", "expected", cause);

      assertEquals("configuration", exception.getOperation());
      assertEquals("param", exception.getResource());
      assertNull(exception.getConfigurationArea());
      assertEquals(cause, exception.getCause());
    }

    @Test
    void testFiveArgConstructorWithAllNulls() {
      final WasiConfigurationException exception =
          new WasiConfigurationException("Error", null, null, null, null);

      assertEquals("configuration", exception.getOperation());
      assertNull(exception.getResource());
      assertNull(exception.getConfigurationArea());
      assertNull(exception.getConfigurationParameter());
      assertNull(exception.getProvidedValue());
      assertNull(exception.getExpectedValue());
    }
  }

  @Nested
  class GetConfigurationGuidanceMutationTests {

    @Test
    void testGuidanceWithParameterAndExpectedValue() {
      // Kill RemoveConditional_EQUAL_IF mutations on line 268
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.ENVIRONMENT,
              "MY_VAR",
              "bad-value",
              "good-value");

      final String guidance = exception.getConfigurationGuidance();
      assertTrue(guidance.contains("Set 'MY_VAR' to good-value"));
      assertFalse(guidance.contains("Check configuration for"));
    }

    @Test
    void testGuidanceWithParameterButNullExpectedValue() {
      // When parameter is not null but expectedValue is null,
      // the condition (configurationParameter != null && expectedValue != null) is false
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.ENVIRONMENT,
              "MY_VAR",
              "bad-value",
              null);

      final String guidance = exception.getConfigurationGuidance();
      // Should use the else branch
      assertTrue(guidance.contains("Check configuration for"));
      assertFalse(guidance.contains("Set 'MY_VAR'"));
    }

    @Test
    void testGuidanceWithNullParameterAndExpectedValue() {
      // When parameter is null but expectedValue is not null,
      // the condition (configurationParameter != null && expectedValue != null) is false
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.ENVIRONMENT,
              null,
              "bad-value",
              "good-value");

      final String guidance = exception.getConfigurationGuidance();
      // Should use the else branch
      assertTrue(guidance.contains("Check configuration for"));
      assertFalse(guidance.contains("Set '"));
    }

    @Test
    void testGuidanceWithBothNullParameterAndExpectedValue() {
      // When both parameter and expectedValue are null,
      // the condition (configurationParameter != null && expectedValue != null) is false
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);

      final String guidance = exception.getConfigurationGuidance();
      // Should use the else branch
      assertTrue(guidance.contains("Check configuration for"));
      assertFalse(guidance.contains("Set '"));
    }

    @Test
    void testGuidanceUnderscoreToSpaceConversion() {
      // Kill InlineConstant mutations on line 273 (95 -> 96, 32 -> 33)
      // These are character constants for '_' (95) and ' ' (32)
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS);

      final String guidance = exception.getConfigurationGuidance();
      // Should contain "file system permissions" (underscores replaced with spaces)
      assertTrue(guidance.contains("file system permissions"));
      assertFalse(guidance.contains("file_system_permissions"));
    }
  }

  @Nested
  class FormatResourceIdentifierMutationTests {

    @Test
    void testFormatResourceIdentifierUnderscoreToDashConversion() {
      // Kill InlineConstant mutations on line 345 (95 -> 96, 45 -> 46)
      // These are character constants for '_' (95) and '-' (45)
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.FILE_SYSTEM_PERMISSIONS);

      // Should contain "file-system-permissions" (underscores replaced with dashes)
      assertEquals("file-system-permissions", exception.getResource());
      assertFalse(exception.getResource().contains("_"));
    }

    @Test
    void testFormatResourceIdentifierWithParameterAndUnderscores() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.COMPONENT_INSTANTIATION,
              "my_param",
              "value",
              "expected");

      // Area should have dashes, but parameter keeps its original format
      assertEquals("component-instantiation:my_param", exception.getResource());
    }

    @Test
    void testFormatResourceIdentifierFirstConditionBothNull() {
      // Test: configurationArea == null && configurationParameter == null
      final WasiConfigurationException bothNull =
          new WasiConfigurationException(
              "Error", (WasiConfigurationException.ConfigurationArea) null);
      assertNull(bothNull.getResource());

      // Test: configurationArea != null && configurationParameter == null
      final WasiConfigurationException onlyParamNull =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);
      assertNotNull(onlyParamNull.getResource());
      assertEquals("environment", onlyParamNull.getResource());

      // Test: configurationArea == null && configurationParameter != null
      final WasiConfigurationException onlyAreaNull =
          new WasiConfigurationException("Error", null, "param", "v", "e");
      assertNotNull(onlyAreaNull.getResource());
      assertEquals("param", onlyAreaNull.getResource());
    }

    @Test
    void testFormatResourceIdentifierSecondConditionParameterNull() {
      // Test: configurationParameter == null with area not null (line 343-346)
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.RUNTIME_ENGINE,
              null,
              null,
              null);

      assertEquals("runtime-engine", exception.getResource());
      assertFalse(exception.getResource().contains(":"));
    }

    @Test
    void testFormatResourceIdentifierThirdConditionAreaNull() {
      // Test: configurationArea == null with parameter not null (line 349-350)
      final WasiConfigurationException exception =
          new WasiConfigurationException("Error", null, "my-standalone-param", "v", "e");

      assertEquals("my-standalone-param", exception.getResource());
      assertFalse(exception.getResource().contains(":"));
    }

    @Test
    void testFormatResourceIdentifierBothPresent() {
      // Test: both configurationArea and configurationParameter are present (line 353)
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error",
              WasiConfigurationException.ConfigurationArea.SECURITY_POLICY,
              "policy_name",
              "invalid",
              "valid");

      assertEquals("security-policy:policy_name", exception.getResource());
      assertTrue(exception.getResource().contains(":"));
    }
  }

  @Nested
  class FormatOperationMutationTests {

    @Test
    void testFormatOperationNullReturnsConfiguration() {
      // Kill RemoveConditional_EQUAL_ELSE mutation on line 316
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", (WasiConfigurationException.ConfigurationArea) null);

      assertEquals("configuration", exception.getOperation());
    }

    @Test
    void testFormatOperationNonNullDoesNotReturnConfiguration() {
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.ENVIRONMENT);

      assertFalse("configuration".equals(exception.getOperation()));
      assertEquals("environment-configuration", exception.getOperation());
    }

    @Test
    void testFormatOperationNetworkConfigurationNoSuffix() {
      // NETWORK_CONFIGURATION already ends with "configuration" so no suffix added
      final WasiConfigurationException exception =
          new WasiConfigurationException(
              "Error", WasiConfigurationException.ConfigurationArea.NETWORK_CONFIGURATION);

      assertEquals("network-configuration", exception.getOperation());
      // Should NOT be "network-configuration-configuration"
      assertFalse(exception.getOperation().contains("configuration-configuration"));
    }
  }
}
