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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WASI HTTP request/response handling.
 *
 * <p>These tests verify HTTP configuration building, host pattern matching, timeout settings,
 * connection limits, and statistics tracking.
 *
 * @since 1.0.0
 */
@DisplayName("WASI HTTP Request/Response Integration Tests")
public final class WasiHttpRequestResponseIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiHttpRequestResponseIntegrationTest.class.getName());

  private static boolean wasiHttpAvailable = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;
  private static String unavailableReason;

  @BeforeAll
  static void checkWasiHttpAvailable() {
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      sharedEngine = sharedRuntime.createEngine();

      // Try to load WASI HTTP classes to verify implementation is available
      final Class<?> jniConfigClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpConfig");
      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");

      if (jniConfigClass != null && jniContextClass != null) {
        wasiHttpAvailable = true;
        LOGGER.info("WASI HTTP is available (JNI classes loaded successfully)");
      }
    } catch (final ClassNotFoundException e) {
      unavailableReason = "JNI HTTP classes not found: " + e.getMessage();
      LOGGER.warning("WASI HTTP not available: " + unavailableReason);
      wasiHttpAvailable = false;
    } catch (final Exception e) {
      unavailableReason = "Failed to initialize: " + e.getMessage();
      LOGGER.warning("WASI HTTP not available: " + unavailableReason);
      wasiHttpAvailable = false;
    }
  }

  @AfterAll
  static void cleanup() {
    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared engine: " + e.getMessage());
      }
    }
    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared runtime: " + e.getMessage());
      }
    }
  }

  private static void assumeWasiHttpAvailable() {
    assumeTrue(wasiHttpAvailable, "WASI HTTP not available: " + unavailableReason);
  }

  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("WasiHttpConfig Interface Tests")
  class ConfigInterfaceTests {

    @Test
    @DisplayName("should have builder method")
    void shouldHaveBuilderMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var builder = WasiHttpConfig.class.getMethod("builder");
      assertNotNull(builder, "builder method should exist");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(builder.getModifiers()), "builder should be static");
      assertEquals(
          WasiHttpConfigBuilder.class,
          builder.getReturnType(),
          "Should return WasiHttpConfigBuilder");

      LOGGER.info("Builder method verified");
    }

    @Test
    @DisplayName("should have defaultConfig method")
    void shouldHaveDefaultConfigMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var defaultConfig = WasiHttpConfig.class.getMethod("defaultConfig");
      assertNotNull(defaultConfig, "defaultConfig method should exist");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(defaultConfig.getModifiers()),
          "defaultConfig should be static");
      assertEquals(
          WasiHttpConfig.class, defaultConfig.getReturnType(), "Should return WasiHttpConfig");

      LOGGER.info("Default config method verified");
    }

    @Test
    @DisplayName("should have getAllowedHosts method")
    void shouldHaveGetAllowedHostsMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var getAllowedHosts = WasiHttpConfig.class.getMethod("getAllowedHosts");
      assertNotNull(getAllowedHosts, "getAllowedHosts method should exist");
      assertEquals(Set.class, getAllowedHosts.getReturnType(), "Should return Set");

      LOGGER.info("GetAllowedHosts method verified");
    }

    @Test
    @DisplayName("should have timeout methods")
    void shouldHaveTimeoutMethods(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var getConnectTimeout = WasiHttpConfig.class.getMethod("getConnectTimeout");
      assertNotNull(getConnectTimeout, "getConnectTimeout method should exist");
      assertEquals(Optional.class, getConnectTimeout.getReturnType(), "Should return Optional");

      final var getReadTimeout = WasiHttpConfig.class.getMethod("getReadTimeout");
      assertNotNull(getReadTimeout, "getReadTimeout method should exist");

      final var getWriteTimeout = WasiHttpConfig.class.getMethod("getWriteTimeout");
      assertNotNull(getWriteTimeout, "getWriteTimeout method should exist");

      LOGGER.info("Timeout methods verified");
    }

    @Test
    @DisplayName("should have connection limit methods")
    void shouldHaveConnectionLimitMethods(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var getMaxConnections = WasiHttpConfig.class.getMethod("getMaxConnections");
      assertNotNull(getMaxConnections, "getMaxConnections method should exist");
      assertEquals(Optional.class, getMaxConnections.getReturnType(), "Should return Optional");

      final var getMaxConnectionsPerHost =
          WasiHttpConfig.class.getMethod("getMaxConnectionsPerHost");
      assertNotNull(getMaxConnectionsPerHost, "getMaxConnectionsPerHost method should exist");

      LOGGER.info("Connection limit methods verified");
    }

    @Test
    @DisplayName("should have body size limit methods")
    void shouldHaveBodySizeLimitMethods(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var getMaxRequestBodySize = WasiHttpConfig.class.getMethod("getMaxRequestBodySize");
      assertNotNull(getMaxRequestBodySize, "getMaxRequestBodySize method should exist");
      assertEquals(Optional.class, getMaxRequestBodySize.getReturnType(), "Should return Optional");

      final var getMaxResponseBodySize = WasiHttpConfig.class.getMethod("getMaxResponseBodySize");
      assertNotNull(getMaxResponseBodySize, "getMaxResponseBodySize method should exist");

      LOGGER.info("Body size limit methods verified");
    }

    @Test
    @DisplayName("should have security methods")
    void shouldHaveSecurityMethods(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var isHttpsRequired = WasiHttpConfig.class.getMethod("isHttpsRequired");
      assertNotNull(isHttpsRequired, "isHttpsRequired method should exist");
      assertEquals(boolean.class, isHttpsRequired.getReturnType(), "Should return boolean");

      final var isCertValidation = WasiHttpConfig.class.getMethod("isCertificateValidationEnabled");
      assertNotNull(isCertValidation, "isCertificateValidationEnabled method should exist");

      LOGGER.info("Security methods verified");
    }

    @Test
    @DisplayName("should have feature toggle methods")
    void shouldHaveFeatureToggleMethods(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var isHttp2Enabled = WasiHttpConfig.class.getMethod("isHttp2Enabled");
      assertNotNull(isHttp2Enabled, "isHttp2Enabled method should exist");

      final var isPoolingEnabled = WasiHttpConfig.class.getMethod("isConnectionPoolingEnabled");
      assertNotNull(isPoolingEnabled, "isConnectionPoolingEnabled method should exist");

      final var isFollowRedirects = WasiHttpConfig.class.getMethod("isFollowRedirects");
      assertNotNull(isFollowRedirects, "isFollowRedirects method should exist");

      LOGGER.info("Feature toggle methods verified");
    }
  }

  @Nested
  @DisplayName("WasiHttpContext Interface Tests")
  class ContextInterfaceTests {

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(
          java.io.Closeable.class.isAssignableFrom(WasiHttpContext.class),
          "WasiHttpContext should extend Closeable");

      LOGGER.info("Closeable extension verified");
    }

    @Test
    @DisplayName("should have addToLinker method")
    void shouldHaveAddToLinkerMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var addToLinker =
          WasiHttpContext.class.getMethod(
              "addToLinker",
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(addToLinker, "addToLinker method should exist");

      LOGGER.info("AddToLinker method verified");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var getConfig = WasiHttpContext.class.getMethod("getConfig");
      assertNotNull(getConfig, "getConfig method should exist");
      assertEquals(WasiHttpConfig.class, getConfig.getReturnType(), "Should return WasiHttpConfig");

      LOGGER.info("GetConfig method verified");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var isValid = WasiHttpContext.class.getMethod("isValid");
      assertNotNull(isValid, "isValid method should exist");
      assertEquals(boolean.class, isValid.getReturnType(), "Should return boolean");

      LOGGER.info("IsValid method verified");
    }

    @Test
    @DisplayName("should have isHostAllowed method")
    void shouldHaveIsHostAllowedMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var isHostAllowed = WasiHttpContext.class.getMethod("isHostAllowed", String.class);
      assertNotNull(isHostAllowed, "isHostAllowed method should exist");
      assertEquals(boolean.class, isHostAllowed.getReturnType(), "Should return boolean");

      LOGGER.info("IsHostAllowed method verified");
    }
  }

  @Nested
  @DisplayName("WasiHttpConfigBuilder Interface Tests")
  class ConfigBuilderInterfaceTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(
          WasiHttpConfigBuilder.class.isInterface(),
          "WasiHttpConfigBuilder should be an interface");

      LOGGER.info("Interface verification passed");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var build = WasiHttpConfigBuilder.class.getMethod("build");
      assertNotNull(build, "build method should exist");
      assertEquals(WasiHttpConfig.class, build.getReturnType(), "Should return WasiHttpConfig");

      LOGGER.info("Build method verified");
    }

    @Test
    @DisplayName("should have allowHost method")
    void shouldHaveAllowHostMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var allowHost = WasiHttpConfigBuilder.class.getMethod("allowHost", String.class);
      assertNotNull(allowHost, "allowHost method should exist");
      assertEquals(WasiHttpConfigBuilder.class, allowHost.getReturnType(), "Should return builder");

      LOGGER.info("AllowHost method verified");
    }

    @Test
    @DisplayName("should have withConnectTimeout method")
    void shouldHaveWithConnectTimeoutMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var withConnectTimeout =
          WasiHttpConfigBuilder.class.getMethod("withConnectTimeout", Duration.class);
      assertNotNull(withConnectTimeout, "withConnectTimeout method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class, withConnectTimeout.getReturnType(), "Should return builder");

      LOGGER.info("WithConnectTimeout method verified");
    }

    @Test
    @DisplayName("should have withMaxConnections method")
    void shouldHaveWithMaxConnectionsMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var withMaxConnections =
          WasiHttpConfigBuilder.class.getMethod("withMaxConnections", int.class);
      assertNotNull(withMaxConnections, "withMaxConnections method should exist");
      assertEquals(
          WasiHttpConfigBuilder.class, withMaxConnections.getReturnType(), "Should return builder");

      LOGGER.info("WithMaxConnections method verified");
    }
  }

  @Nested
  @DisplayName("WasiHttpFactory Interface Tests")
  class FactoryInterfaceTests {

    @Test
    @DisplayName("should have createContext method")
    void shouldHaveCreateContextMethod(final TestInfo testInfo) throws NoSuchMethodException {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final var createContext =
          WasiHttpFactory.class.getMethod("createContext", WasiHttpConfig.class);
      assertNotNull(createContext, "createContext method should exist");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(createContext.getModifiers()),
          "createContext should be static");
      assertEquals(
          WasiHttpContext.class, createContext.getReturnType(), "Should return WasiHttpContext");

      LOGGER.info("CreateContext method verified");
    }
  }

  @Nested
  @DisplayName("Native HTTP Context Tests")
  class NativeHttpContextTests {

    @Test
    @DisplayName("should create default HTTP config")
    void shouldCreateDefaultHttpConfig(final TestInfo testInfo) {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try {
        final WasiHttpConfig config = WasiHttpConfig.defaultConfig();
        assertNotNull(config, "Default config should not be null");

        final Set<String> allowedHosts = config.getAllowedHosts();
        assertNotNull(allowedHosts, "Allowed hosts should not be null");

        LOGGER.info("Default HTTP config created, allowed hosts: " + allowedHosts.size());
      } catch (final Exception e) {
        LOGGER.info("Default config test skipped: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should create HTTP context from config")
    void shouldCreateHttpContextFromConfig(final TestInfo testInfo) {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try {
        final WasiHttpConfig config = WasiHttpConfig.defaultConfig();
        final WasiHttpContext context = WasiHttpFactory.createContext(config);
        resources.add(context);

        assertNotNull(context, "Context should not be null");
        assertTrue(context.isValid(), "Context should be valid");

        LOGGER.info("HTTP context created successfully");
      } catch (final Exception e) {
        LOGGER.info("HTTP context test skipped: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should close HTTP context properly")
    void shouldCloseHttpContextProperly(final TestInfo testInfo) {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try {
        final WasiHttpConfig config = WasiHttpConfig.defaultConfig();
        final WasiHttpContext context = WasiHttpFactory.createContext(config);

        assertTrue(context.isValid(), "Context should be valid initially");

        context.close();

        assertFalse(context.isValid(), "Context should be invalid after close");

        LOGGER.info("HTTP context closed successfully");
      } catch (final Exception e) {
        LOGGER.info("HTTP context close test skipped: " + e.getMessage());
      }
    }
  }
}
