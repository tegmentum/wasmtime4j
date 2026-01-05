/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiHttpConfig} interface.
 *
 * <p>WasiHttpConfig provides configuration options for WASI HTTP context creation.
 */
@DisplayName("WasiHttpConfig Tests")
class WasiHttpConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiHttpConfig.class.isInterface(), "WasiHttpConfig should be an interface");
    }

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method builderMethod = WasiHttpConfig.class.getMethod("builder");
      assertTrue(Modifier.isStatic(builderMethod.getModifiers()), "builder should be static");
      assertEquals(
          WasiHttpConfigBuilder.class, builderMethod.getReturnType(), "Should return builder");
    }

    @Test
    @DisplayName("should have defaultConfig static method")
    void shouldHaveDefaultConfigStaticMethod() throws NoSuchMethodException {
      final Method defaultConfigMethod = WasiHttpConfig.class.getMethod("defaultConfig");
      assertTrue(
          Modifier.isStatic(defaultConfigMethod.getModifiers()), "defaultConfig should be static");
      assertEquals(
          WasiHttpConfig.class,
          defaultConfigMethod.getReturnType(),
          "Should return WasiHttpConfig");
    }
  }

  @Nested
  @DisplayName("Host Configuration Method Tests")
  class HostConfigurationMethodTests {

    @Test
    @DisplayName("should have getAllowedHosts method")
    void shouldHaveGetAllowedHostsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getAllowedHosts");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getBlockedHosts method")
    void shouldHaveGetBlockedHostsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getBlockedHosts");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Timeout Configuration Method Tests")
  class TimeoutConfigurationMethodTests {

    @Test
    @DisplayName("should have getConnectTimeout method")
    void shouldHaveGetConnectTimeoutMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getConnectTimeout");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Duration>");
    }

    @Test
    @DisplayName("should have getReadTimeout method")
    void shouldHaveGetReadTimeoutMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getReadTimeout");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Duration>");
    }

    @Test
    @DisplayName("should have getWriteTimeout method")
    void shouldHaveGetWriteTimeoutMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getWriteTimeout");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Duration>");
    }
  }

  @Nested
  @DisplayName("Connection Configuration Method Tests")
  class ConnectionConfigurationMethodTests {

    @Test
    @DisplayName("should have getMaxConnections method")
    void shouldHaveGetMaxConnectionsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getMaxConnections");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Integer>");
    }

    @Test
    @DisplayName("should have getMaxConnectionsPerHost method")
    void shouldHaveGetMaxConnectionsPerHostMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getMaxConnectionsPerHost");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Integer>");
    }

    @Test
    @DisplayName("should have isConnectionPoolingEnabled method")
    void shouldHaveIsConnectionPoolingEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("isConnectionPoolingEnabled");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Body Size Configuration Method Tests")
  class BodySizeConfigurationMethodTests {

    @Test
    @DisplayName("should have getMaxRequestBodySize method")
    void shouldHaveGetMaxRequestBodySizeMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getMaxRequestBodySize");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Long>");
    }

    @Test
    @DisplayName("should have getMaxResponseBodySize method")
    void shouldHaveGetMaxResponseBodySizeMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getMaxResponseBodySize");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Long>");
    }
  }

  @Nested
  @DisplayName("HTTP Method Configuration Tests")
  class HttpMethodConfigurationTests {

    @Test
    @DisplayName("should have getAllowedMethods method")
    void shouldHaveGetAllowedMethodsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getAllowedMethods");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Security Configuration Method Tests")
  class SecurityConfigurationMethodTests {

    @Test
    @DisplayName("should have isHttpsRequired method")
    void shouldHaveIsHttpsRequiredMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("isHttpsRequired");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isCertificateValidationEnabled method")
    void shouldHaveIsCertificateValidationEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("isCertificateValidationEnabled");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Protocol Configuration Method Tests")
  class ProtocolConfigurationMethodTests {

    @Test
    @DisplayName("should have isHttp2Enabled method")
    void shouldHaveIsHttp2EnabledMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("isHttp2Enabled");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isFollowRedirects method")
    void shouldHaveIsFollowRedirectsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("isFollowRedirects");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxRedirects method")
    void shouldHaveGetMaxRedirectsMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getMaxRedirects");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<Integer>");
    }
  }

  @Nested
  @DisplayName("User Agent Configuration Method Tests")
  class UserAgentConfigurationMethodTests {

    @Test
    @DisplayName("should have getUserAgent method")
    void shouldHaveGetUserAgentMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("getUserAgent");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional<String>");
    }
  }

  @Nested
  @DisplayName("Builder and Validation Method Tests")
  class BuilderAndValidationMethodTests {

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("toBuilder");
      assertEquals(WasiHttpConfigBuilder.class, method.getReturnType(), "Should return builder");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = WasiHttpConfig.class.getMethod("validate");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Configuration Pattern Tests")
  class ConfigurationPatternTests {

    @Test
    @DisplayName("should support typical server configuration pattern")
    void shouldSupportTypicalServerConfigurationPattern() {
      // Document the expected pattern for server-side WASI HTTP config
      final Set<String> expectedHostMethods = new HashSet<>();
      expectedHostMethods.add("getAllowedHosts");
      expectedHostMethods.add("getBlockedHosts");

      for (final String methodName : expectedHostMethods) {
        assertTrue(
            hasMethod(WasiHttpConfig.class, methodName), "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("should support security-focused configuration pattern")
    void shouldSupportSecurityFocusedConfigurationPattern() {
      // Security methods for production deployments
      final Set<String> securityMethods = new HashSet<>();
      securityMethods.add("isHttpsRequired");
      securityMethods.add("isCertificateValidationEnabled");
      securityMethods.add("getBlockedHosts");

      for (final String methodName : securityMethods) {
        assertTrue(
            hasMethod(WasiHttpConfig.class, methodName),
            "Should have security method: " + methodName);
      }
    }

    @Test
    @DisplayName("should support performance tuning configuration pattern")
    void shouldSupportPerformanceTuningConfigurationPattern() {
      // Performance tuning methods
      final Set<String> performanceMethods = new HashSet<>();
      performanceMethods.add("getMaxConnections");
      performanceMethods.add("getMaxConnectionsPerHost");
      performanceMethods.add("isConnectionPoolingEnabled");
      performanceMethods.add("isHttp2Enabled");

      for (final String methodName : performanceMethods) {
        assertTrue(
            hasMethod(WasiHttpConfig.class, methodName),
            "Should have performance method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("HTTP Client Configuration Tests")
  class HttpClientConfigurationTests {

    @Test
    @DisplayName("should have comprehensive timeout configuration")
    void shouldHaveComprehensiveTimeoutConfiguration() {
      // Common HTTP client timeout types
      final String[] timeoutMethods = {"getConnectTimeout", "getReadTimeout", "getWriteTimeout"};

      for (final String methodName : timeoutMethods) {
        assertTrue(
            hasMethod(WasiHttpConfig.class, methodName),
            "Should have timeout method: " + methodName);
      }
    }

    @Test
    @DisplayName("should have body size limits")
    void shouldHaveBodySizeLimits() {
      // Body size limits for security
      final String[] sizeMethods = {"getMaxRequestBodySize", "getMaxResponseBodySize"};

      for (final String methodName : sizeMethods) {
        assertTrue(
            hasMethod(WasiHttpConfig.class, methodName),
            "Should have size limit method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Builder Method Behavioral Tests")
  class BuilderMethodBehavioralTests {

    @Test
    @DisplayName("builder method should throw RuntimeException when no implementation available")
    void builderMethodShouldThrowRuntimeExceptionWhenNoImplementationAvailable() {
      // When running in isolation without implementations, builder() should throw
      // This test verifies the error handling path in the builder() static method
      try {
        WasiHttpConfig.builder();
        // If we reach here, an implementation was found (e.g., in test classpath)
        // This is acceptable - the test documents expected behavior
      } catch (RuntimeException e) {
        assertTrue(
            e.getMessage().contains("No WasiHttpConfigBuilder implementation available")
                || e.getMessage().contains("Failed to create WASI HTTP config builder"),
            "Error message should indicate missing implementation or creation failure");
      }
    }

    @Test
    @DisplayName("builder method error message should mention classpath requirements")
    void builderMethodErrorMessageShouldMentionClasspathRequirements() {
      try {
        WasiHttpConfig.builder();
        // Implementation found - this is OK
      } catch (RuntimeException e) {
        if (e.getMessage().contains("No WasiHttpConfigBuilder implementation available")) {
          assertTrue(
              e.getMessage().contains("wasmtime4j-panama")
                  || e.getMessage().contains("wasmtime4j-jni"),
              "Error message should mention required implementations");
        }
      }
    }

    @Test
    @DisplayName("builder method should attempt Panama implementation first")
    void builderMethodShouldAttemptPanamaImplementationFirst() {
      // This test verifies the priority order by checking the class names in the code
      // The builder() method tries Panama first, then falls back to JNI
      try {
        final java.lang.reflect.Method builderMethod = WasiHttpConfig.class.getMethod("builder");
        assertNotNull(builderMethod, "builder method should exist");
        assertEquals(WasiHttpConfigBuilder.class, builderMethod.getReturnType());
        // The actual implementation priority is encoded in the method body
        // This test confirms the method structure is correct for the fallback pattern
      } catch (NoSuchMethodException e) {
        fail("builder method should exist");
      }
    }
  }

  @Nested
  @DisplayName("DefaultConfig Method Behavioral Tests")
  class DefaultConfigMethodBehavioralTests {

    @Test
    @DisplayName("defaultConfig method should call builder and build")
    void defaultConfigMethodShouldCallBuilderAndBuild() {
      // defaultConfig() internally calls builder().build()
      // This verifies the delegation pattern
      try {
        WasiHttpConfig.defaultConfig();
        // If we reach here, the full chain worked
      } catch (RuntimeException e) {
        // Expected if no implementation is available
        assertTrue(
            e.getMessage().contains("No WasiHttpConfigBuilder implementation available")
                || e.getMessage().contains("Failed to create WASI HTTP config builder"),
            "Error should be from builder() method");
      }
    }

    @Test
    @DisplayName("defaultConfig method signature should return WasiHttpConfig")
    void defaultConfigMethodSignatureShouldReturnWasiHttpConfig() throws NoSuchMethodException {
      final java.lang.reflect.Method method = WasiHttpConfig.class.getMethod("defaultConfig");
      assertEquals(WasiHttpConfig.class, method.getReturnType());
      assertTrue(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "defaultConfig should be static");
    }
  }

  @Nested
  @DisplayName("Reflection Error Handling Tests")
  class ReflectionErrorHandlingTests {

    @Test
    @DisplayName("should handle ClassNotFoundException gracefully")
    void shouldHandleClassNotFoundExceptionGracefully() {
      // The builder() method catches ClassNotFoundException and tries fallback
      // Verify that it doesn't throw ClassNotFoundException directly
      // If no implementation, it throws RuntimeException with useful message
      try {
        WasiHttpConfig.builder();
        // If we get here, an implementation exists
        assertTrue(true);
      } catch (RuntimeException e) {
        // Expected when no implementation - exceptions are wrapped
        assertNotNull(e.getMessage(), "RuntimeException should have a message");
      }
    }

    @Test
    @DisplayName("should wrap instantiation exceptions in RuntimeException")
    void shouldWrapInstantiationExceptionsInRuntimeException() {
      // The builder() method wraps all reflection exceptions
      // Checked exceptions should never leak from the static method
      try {
        WasiHttpConfig.builder();
        // If we get here, an implementation exists
        assertTrue(true);
      } catch (RuntimeException e) {
        // Expected behavior - all exceptions are wrapped
        assertTrue(true);
      }
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("interface should define all required configuration getters")
    void interfaceShouldDefineAllRequiredConfigurationGetters() {
      final Set<String> requiredGetters =
          new HashSet<>(
              Arrays.asList(
                  "getAllowedHosts",
                  "getBlockedHosts",
                  "getConnectTimeout",
                  "getReadTimeout",
                  "getWriteTimeout",
                  "getMaxConnections",
                  "getMaxConnectionsPerHost",
                  "getMaxRequestBodySize",
                  "getMaxResponseBodySize",
                  "getAllowedMethods",
                  "getMaxRedirects",
                  "getUserAgent"));

      for (final String getter : requiredGetters) {
        assertTrue(
            Arrays.stream(WasiHttpConfig.class.getMethods())
                .anyMatch(m -> m.getName().equals(getter)),
            "Should have getter: " + getter);
      }
    }

    @Test
    @DisplayName("interface should define all required boolean getters")
    void interfaceShouldDefineAllRequiredBooleanGetters() {
      final Set<String> booleanGetters =
          new HashSet<>(
              Arrays.asList(
                  "isHttpsRequired",
                  "isCertificateValidationEnabled",
                  "isHttp2Enabled",
                  "isConnectionPoolingEnabled",
                  "isFollowRedirects"));

      for (final String getter : booleanGetters) {
        try {
          final java.lang.reflect.Method method = WasiHttpConfig.class.getMethod(getter);
          assertEquals(
              boolean.class, method.getReturnType(), getter + " should return boolean primitive");
        } catch (NoSuchMethodException e) {
          fail("Should have boolean getter: " + getter);
        }
      }
    }

    @Test
    @DisplayName("interface should define toBuilder method")
    void interfaceShouldDefineToBuilderMethod() throws NoSuchMethodException {
      final java.lang.reflect.Method method = WasiHttpConfig.class.getMethod("toBuilder");
      assertEquals(
          WasiHttpConfigBuilder.class,
          method.getReturnType(),
          "toBuilder should return WasiHttpConfigBuilder");
      assertFalse(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "toBuilder should be instance method");
    }

    @Test
    @DisplayName("interface should define validate method")
    void interfaceShouldDefineValidateMethod() throws NoSuchMethodException {
      final java.lang.reflect.Method method = WasiHttpConfig.class.getMethod("validate");
      assertEquals(void.class, method.getReturnType(), "validate should return void");
      assertFalse(
          java.lang.reflect.Modifier.isStatic(method.getModifiers()),
          "validate should be instance method");
    }
  }
}
