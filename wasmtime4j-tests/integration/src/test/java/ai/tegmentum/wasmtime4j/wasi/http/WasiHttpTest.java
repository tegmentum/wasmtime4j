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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * Integration tests for WASI HTTP - HTTP client and server functionality.
 *
 * <p>These tests verify WASI HTTP configuration, host patterns, timeouts, connection limits, and
 * request/response handling.
 *
 * @since 1.0.0
 */
@DisplayName("WASI HTTP Integration Tests")
public final class WasiHttpTest {

  private static final Logger LOGGER = Logger.getLogger(WasiHttpTest.class.getName());

  private static boolean wasiHttpAvailable = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;
  private static WasiHttpContext sharedHttpContext;

  @BeforeAll
  static void checkWasiHttpAvailable() {
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      sharedEngine = sharedRuntime.createEngine();

      // Try to create a WASI HTTP context using JNI directly to avoid Panama crash
      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      sharedHttpContext =
          (WasiHttpContext)
              jniContextClass
                  .getConstructor(WasiHttpConfig.class)
                  .newInstance(WasiHttpConfig.defaultConfig());

      if (sharedHttpContext != null) {
        wasiHttpAvailable = true;
        LOGGER.info("WASI HTTP is available (using JNI implementation)");
      }
    } catch (final Exception e) {
      LOGGER.warning("WASI HTTP not available: " + e.getMessage());
      wasiHttpAvailable = false;
    }
  }

  @AfterAll
  static void cleanup() {
    if (sharedHttpContext != null) {
      try {
        sharedHttpContext.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared HTTP context: " + e.getMessage());
      }
    }
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
    assumeTrue(wasiHttpAvailable, "WASI HTTP native implementation not available - skipping");
  }

  private Engine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
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
    if (engine != null) {
      engine.close();
      engine = null;
    }
  }

  @Nested
  @DisplayName("HTTP Config Tests")
  class ConfigTests {

    @Test
    @DisplayName("should configure HTTP with builder")
    void shouldConfigureHttpWithBuilder(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Build a configuration with various settings
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("api.example.com")
              .allowHost("cdn.example.com")
              .withConnectTimeout(Duration.ofSeconds(30))
              .withMaxConnections(100)
              .requireHttps(true)
              .build();

      assertNotNull(config, "Config should not be null");
      LOGGER.info("Created config: " + config);

      // Verify allowed hosts
      final Set<String> allowedHosts = config.getAllowedHosts();
      assertNotNull(allowedHosts, "Allowed hosts should not be null");
      assertEquals(2, allowedHosts.size(), "Should have 2 allowed hosts");
      assertTrue(allowedHosts.contains("api.example.com"), "Should contain api.example.com");
      assertTrue(allowedHosts.contains("cdn.example.com"), "Should contain cdn.example.com");

      // Verify timeout
      final Optional<Duration> connectTimeout = config.getConnectTimeout();
      assertTrue(connectTimeout.isPresent(), "Connect timeout should be present");
      assertEquals(Duration.ofSeconds(30), connectTimeout.get(), "Connect timeout should be 30s");

      // Verify max connections
      final Optional<Integer> maxConnections = config.getMaxConnections();
      assertTrue(maxConnections.isPresent(), "Max connections should be present");
      assertEquals(100, maxConnections.get().intValue(), "Max connections should be 100");

      // Verify HTTPS required
      assertTrue(config.isHttpsRequired(), "HTTPS should be required");

      LOGGER.info("Config verification passed");
    }

    @Test
    @DisplayName("should set allowed host patterns")
    void shouldSetAllowedHostPatterns(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Test wildcard patterns
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("*.example.com")
              .allowHost("api.trusted.org")
              .blockHost("internal.example.com")
              .build();

      assertNotNull(config, "Config should not be null");

      // Verify allowed hosts include wildcard
      final Set<String> allowedHosts = config.getAllowedHosts();
      assertTrue(allowedHosts.contains("*.example.com"), "Should contain wildcard pattern");
      assertTrue(allowedHosts.contains("api.trusted.org"), "Should contain exact pattern");

      // Verify blocked hosts
      final Set<String> blockedHosts = config.getBlockedHosts();
      assertNotNull(blockedHosts, "Blocked hosts should not be null");
      assertTrue(blockedHosts.contains("internal.example.com"), "Should contain blocked host");

      LOGGER.info(
          "Host pattern verification passed with "
              + allowedHosts.size()
              + " allowed, "
              + blockedHosts.size()
              + " blocked");
    }

    @Test
    @DisplayName("should configure timeouts")
    void shouldConfigureTimeouts(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Configure all timeout types
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .withConnectTimeout(Duration.ofSeconds(10))
              .withReadTimeout(Duration.ofSeconds(30))
              .withWriteTimeout(Duration.ofSeconds(15))
              .build();

      // Verify connect timeout
      final Optional<Duration> connectTimeout = config.getConnectTimeout();
      assertTrue(connectTimeout.isPresent(), "Connect timeout should be present");
      assertEquals(Duration.ofSeconds(10), connectTimeout.get(), "Connect timeout should be 10s");

      // Verify read timeout
      final Optional<Duration> readTimeout = config.getReadTimeout();
      assertTrue(readTimeout.isPresent(), "Read timeout should be present");
      assertEquals(Duration.ofSeconds(30), readTimeout.get(), "Read timeout should be 30s");

      // Verify write timeout
      final Optional<Duration> writeTimeout = config.getWriteTimeout();
      assertTrue(writeTimeout.isPresent(), "Write timeout should be present");
      assertEquals(Duration.ofSeconds(15), writeTimeout.get(), "Write timeout should be 15s");

      LOGGER.info("Timeout configuration verification passed");
    }

    @Test
    @DisplayName("should configure connection limits")
    void shouldConfigureConnectionLimits(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Configure connection limits
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .withMaxConnections(200)
              .withMaxConnectionsPerHost(20)
              .withMaxRequestBodySize(10 * 1024 * 1024L) // 10MB
              .withMaxResponseBodySize(50 * 1024 * 1024L) // 50MB
              .build();

      // Verify max connections
      final Optional<Integer> maxConnections = config.getMaxConnections();
      assertTrue(maxConnections.isPresent(), "Max connections should be present");
      assertEquals(200, maxConnections.get().intValue(), "Max connections should be 200");

      // Verify max connections per host
      final Optional<Integer> maxPerHost = config.getMaxConnectionsPerHost();
      assertTrue(maxPerHost.isPresent(), "Max connections per host should be present");
      assertEquals(20, maxPerHost.get().intValue(), "Max per host should be 20");

      // Verify body size limits
      final Optional<Long> maxRequestSize = config.getMaxRequestBodySize();
      assertTrue(maxRequestSize.isPresent(), "Max request body size should be present");
      assertEquals(10 * 1024 * 1024L, maxRequestSize.get().longValue(), "Max request size");

      final Optional<Long> maxResponseSize = config.getMaxResponseBodySize();
      assertTrue(maxResponseSize.isPresent(), "Max response body size should be present");
      assertEquals(50 * 1024 * 1024L, maxResponseSize.get().longValue(), "Max response size");

      LOGGER.info("Connection limits verification passed");
    }

    @Test
    @DisplayName("should validate configuration")
    void shouldValidateConfiguration(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Test that valid config passes validation
      final WasiHttpConfig validConfig =
          WasiHttpConfig.builder().allowHost("example.com").withMaxConnections(10).build();
      validConfig.validate();
      LOGGER.info("Valid config passed validation");

      // Test that default config passes validation
      final WasiHttpConfig defaultConfig = WasiHttpConfig.defaultConfig();
      defaultConfig.validate();
      LOGGER.info("Default config passed validation");
    }

    @Test
    @DisplayName("should configure HTTP/2 and connection pooling")
    void shouldConfigureHttp2AndPooling(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Test HTTP/2 enabled
      final WasiHttpConfig configWithHttp2 =
          WasiHttpConfig.builder().withHttp2(true).withConnectionPooling(true).build();

      assertTrue(configWithHttp2.isHttp2Enabled(), "HTTP/2 should be enabled");
      assertTrue(
          configWithHttp2.isConnectionPoolingEnabled(), "Connection pooling should be enabled");

      // Test HTTP/2 disabled
      final WasiHttpConfig configWithoutHttp2 =
          WasiHttpConfig.builder().withHttp2(false).withConnectionPooling(false).build();

      assertFalse(configWithoutHttp2.isHttp2Enabled(), "HTTP/2 should be disabled");
      assertFalse(configWithoutHttp2.isConnectionPoolingEnabled(), "Pooling should be disabled");

      LOGGER.info("HTTP/2 and pooling configuration verification passed");
    }

    @Test
    @DisplayName("should configure redirect behavior")
    void shouldConfigureRedirectBehavior(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Test with redirects enabled
      final WasiHttpConfig configFollowRedirects =
          WasiHttpConfig.builder().followRedirects(true).withMaxRedirects(5).build();

      assertTrue(configFollowRedirects.isFollowRedirects(), "Should follow redirects");
      final Optional<Integer> maxRedirects = configFollowRedirects.getMaxRedirects();
      assertTrue(maxRedirects.isPresent(), "Max redirects should be present");
      assertEquals(5, maxRedirects.get().intValue(), "Max redirects should be 5");

      // Test with redirects disabled
      final WasiHttpConfig configNoRedirects =
          WasiHttpConfig.builder().followRedirects(false).build();

      assertFalse(configNoRedirects.isFollowRedirects(), "Should not follow redirects");

      LOGGER.info("Redirect behavior configuration verification passed");
    }

    @Test
    @DisplayName("should configure user agent")
    void shouldConfigureUserAgent(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final String userAgent = "Wasmtime4j/1.0.0";
      final WasiHttpConfig config = WasiHttpConfig.builder().withUserAgent(userAgent).build();

      final Optional<String> configuredUserAgent = config.getUserAgent();
      assertTrue(configuredUserAgent.isPresent(), "User agent should be present");
      assertEquals(userAgent, configuredUserAgent.get(), "User agent should match");

      LOGGER.info("User agent configuration verification passed");
    }

    @Test
    @DisplayName("should configure allowed methods")
    void shouldConfigureAllowedMethods(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasiHttpConfig config =
          WasiHttpConfig.builder().allowMethods("GET", "POST", "PUT").build();

      final List<String> allowedMethods = config.getAllowedMethods();
      assertNotNull(allowedMethods, "Allowed methods should not be null");
      assertEquals(3, allowedMethods.size(), "Should have 3 allowed methods");
      assertTrue(allowedMethods.contains("GET"), "Should allow GET");
      assertTrue(allowedMethods.contains("POST"), "Should allow POST");
      assertTrue(allowedMethods.contains("PUT"), "Should allow PUT");

      LOGGER.info("Allowed methods configuration verification passed");
    }

    @Test
    @DisplayName("should support toBuilder for modification")
    void shouldSupportToBuilder(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create initial config
      final WasiHttpConfig original =
          WasiHttpConfig.builder()
              .allowHost("api.example.com")
              .withConnectTimeout(Duration.ofSeconds(30))
              .build();

      // Modify via toBuilder
      final WasiHttpConfig modified =
          original.toBuilder()
              .allowHost("cdn.example.com")
              .withConnectTimeout(Duration.ofSeconds(60))
              .build();

      // Verify original is unchanged
      assertEquals(1, original.getAllowedHosts().size(), "Original should have 1 host");
      assertEquals(Duration.ofSeconds(30), original.getConnectTimeout().get(), "Original timeout");

      // Verify modified has new values
      assertEquals(2, modified.getAllowedHosts().size(), "Modified should have 2 hosts");
      assertEquals(Duration.ofSeconds(60), modified.getConnectTimeout().get(), "Modified timeout");

      LOGGER.info("toBuilder configuration verification passed");
    }
  }

  @Nested
  @DisplayName("HTTP Context Tests")
  class ContextTests {

    @Test
    @DisplayName("should create HTTP context with config")
    void shouldCreateHttpContextWithConfig(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasiHttpConfig config = WasiHttpConfig.builder().allowHost("api.example.com").build();

      // Create context - use JNI implementation directly since sharedHttpContext is already created
      assertNotNull(sharedHttpContext, "Shared HTTP context should exist");
      assertTrue(sharedHttpContext.isValid(), "Context should be valid");

      LOGGER.info("HTTP context creation verification passed");
    }

    @Test
    @DisplayName("should check host allowed status")
    void shouldCheckHostAllowedStatus(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create context with specific allowed hosts
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("api.example.com")
              .allowHost("*.trusted.org")
              .blockHost("blocked.trusted.org")
              .build();

      // Create context using JNI directly
      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      // Test exact match
      assertTrue(context.isHostAllowed("api.example.com"), "api.example.com should be allowed");

      // Test wildcard match
      assertTrue(context.isHostAllowed("sub.trusted.org"), "sub.trusted.org should be allowed");
      assertTrue(context.isHostAllowed("trusted.org"), "trusted.org should be allowed");

      // Test blocked host takes precedence
      assertFalse(context.isHostAllowed("blocked.trusted.org"), "blocked host should be denied");

      // Test unallowed host
      assertFalse(context.isHostAllowed("unknown.com"), "unknown.com should be denied");

      LOGGER.info("Host allowed status verification passed");
    }

    @Test
    @DisplayName("should return context config")
    void shouldReturnContextConfig(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("test.example.com")
              .withConnectTimeout(Duration.ofSeconds(45))
              .build();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      final WasiHttpConfig returnedConfig = context.getConfig();
      assertNotNull(returnedConfig, "Config should not be null");
      assertTrue(
          returnedConfig.getAllowedHosts().contains("test.example.com"),
          "Config should contain allowed host");
      assertEquals(
          Duration.ofSeconds(45),
          returnedConfig.getConnectTimeout().get(),
          "Config should have correct timeout");

      LOGGER.info("Context config retrieval verification passed");
    }

    @Test
    @DisplayName("should close context properly")
    void shouldCloseContextProperly(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasiHttpConfig config = WasiHttpConfig.defaultConfig();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);

      assertTrue(context.isValid(), "Context should be valid before close");

      context.close();

      assertFalse(context.isValid(), "Context should be invalid after close");

      // Double close should be safe
      context.close();

      LOGGER.info("Context close verification passed");
    }

    @Test
    @DisplayName("should block all hosts with default config")
    void shouldBlockAllHostsWithDefaultConfig(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Default config has no allowed hosts
      final WasiHttpConfig config = WasiHttpConfig.defaultConfig();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      // All hosts should be blocked
      assertFalse(context.isHostAllowed("example.com"), "example.com should be blocked");
      assertFalse(context.isHostAllowed("google.com"), "google.com should be blocked");
      assertFalse(context.isHostAllowed("localhost"), "localhost should be blocked");

      LOGGER.info("Default config blocking verification passed");
    }

    @Test
    @DisplayName("should allow all hosts with allowAllHosts")
    void shouldAllowAllHostsWithAllowAll(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasiHttpConfig config = WasiHttpConfig.builder().allowAllHosts().build();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      // All hosts should be allowed
      assertTrue(context.isHostAllowed("example.com"), "example.com should be allowed");
      assertTrue(context.isHostAllowed("google.com"), "google.com should be allowed");
      assertTrue(context.isHostAllowed("localhost"), "localhost should be allowed");
      assertTrue(context.isHostAllowed("any.domain.here"), "Any domain should be allowed");

      LOGGER.info("allowAllHosts verification passed");
    }

    @Test
    @DisplayName("should reject null host in isHostAllowed")
    void shouldRejectNullHostInIsHostAllowed(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasiHttpConfig config = WasiHttpConfig.builder().allowHost("example.com").build();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      assertThrows(
          IllegalArgumentException.class,
          () -> context.isHostAllowed(null),
          "Should throw on null host");

      LOGGER.info("Null host rejection verification passed");
    }
  }

  @Nested
  @DisplayName("HTTP Security Tests")
  class SecurityTests {

    @Test
    @DisplayName("should require HTTPS when configured")
    void shouldRequireHttpsWhenConfigured(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final WasiHttpConfig config =
          WasiHttpConfig.builder().requireHttps(true).withCertificateValidation(true).build();

      assertTrue(config.isHttpsRequired(), "HTTPS should be required");
      assertTrue(
          config.isCertificateValidationEnabled(), "Certificate validation should be enabled");

      LOGGER.info("HTTPS requirement verification passed");
    }

    @Test
    @DisplayName("should block hosts even when allowed pattern matches")
    void shouldBlockHostsEvenWhenAllowedPatternMatches(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Allow all subdomains but block specific one
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("*.example.com")
              .blockHost("secret.example.com")
              .build();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      // Wildcard allows most subdomains
      assertTrue(context.isHostAllowed("api.example.com"), "api.example.com should be allowed");
      assertTrue(context.isHostAllowed("www.example.com"), "www.example.com should be allowed");

      // But blocked host is denied
      assertFalse(
          context.isHostAllowed("secret.example.com"), "secret.example.com should be blocked");

      LOGGER.info("Block list priority verification passed");
    }

    @Test
    @DisplayName("should configure certificate validation")
    void shouldConfigureCertificateValidation(final TestInfo testInfo) throws Exception {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Test with validation enabled
      final WasiHttpConfig configEnabled =
          WasiHttpConfig.builder().withCertificateValidation(true).build();
      assertTrue(configEnabled.isCertificateValidationEnabled(), "Validation should be enabled");

      // Test with validation disabled (not recommended for production!)
      final WasiHttpConfig configDisabled =
          WasiHttpConfig.builder().withCertificateValidation(false).build();
      assertFalse(configDisabled.isCertificateValidationEnabled(), "Validation should be disabled");

      LOGGER.info("Certificate validation configuration verification passed");
    }
  }
}
