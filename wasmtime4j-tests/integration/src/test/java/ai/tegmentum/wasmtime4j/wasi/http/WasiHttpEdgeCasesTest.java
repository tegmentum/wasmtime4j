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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Edge case integration tests for WASI HTTP functionality.
 *
 * <p>This test class focuses on boundary conditions and error handling scenarios:
 *
 * <ul>
 *   <li>Connection pool exhaustion
 *   <li>Redirect loop handling
 *   <li>Large response body handling
 *   <li>Timeout edge cases
 *   <li>Configuration boundary values
 *   <li>Concurrent access patterns
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("WASI HTTP Edge Cases Integration Tests")
public class WasiHttpEdgeCasesTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiHttpEdgeCasesTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();

  private static boolean checkWasiHttpAvailable() {
    try {
      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass
                  .getConstructor(WasiHttpConfig.class)
                  .newInstance(WasiHttpConfig.defaultConfig());
      if (context != null) {
        context.close();
        return true;
      }
    } catch (final Exception e) {
      LOGGER.warning("WASI HTTP not available: " + e.getMessage());
    }
    return false;
  }

  private static void assumeWasiHttpAvailable() {
    assumeTrue(
        checkWasiHttpAvailable(), "WASI HTTP native implementation not available - skipping");
  }

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
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Connection Pool Edge Cases")
  class ConnectionPoolEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should reject zero max connections")
    void shouldRejectZeroMaxConnections(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing zero max connections configuration");

      // Zero connections should be rejected - API requires >= 1
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiHttpConfig.builder().withMaxConnections(0).build(),
          "Zero max connections should be rejected");

      LOGGER.info("Zero max connections correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle max int connections")
    void shouldHandleMaxIntConnections(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing max int connections configuration");

      final WasiHttpConfig config =
          WasiHttpConfig.builder().withMaxConnections(Integer.MAX_VALUE).build();

      final Optional<Integer> maxConnections = config.getMaxConnections();
      assertTrue(maxConnections.isPresent(), "Max connections should be present");
      assertEquals(Integer.MAX_VALUE, maxConnections.get(), "Should store max int value");

      LOGGER.info("Max int connections handled: " + maxConnections.get());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle connections per host limit")
    void shouldHandleConnectionsPerHostLimit(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing connections per host limit");

      // Test that per-host limit can be higher than total (unusual but valid)
      final WasiHttpConfig config =
          WasiHttpConfig.builder().withMaxConnections(10).withMaxConnectionsPerHost(20).build();

      assertEquals(10, config.getMaxConnections().get(), "Total max should be 10");
      assertEquals(20, config.getMaxConnectionsPerHost().get(), "Per-host max should be 20");

      LOGGER.info("Per-host limit configuration handled");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @Timeout(30)
    @DisplayName("should handle concurrent context creation")
    void shouldHandleConcurrentContextCreation(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing concurrent HTTP context creation");

      final int threadCount = 10;
      final int contextsPerThread = 5;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                startLatch.await();
                for (int i = 0; i < contextsPerThread; i++) {
                  WasiHttpContext context = null;
                  try {
                    final WasiHttpConfig config =
                        WasiHttpConfig.builder()
                            .allowHost("thread" + threadId + "-" + i + ".example.com")
                            .withMaxConnections(threadId + 1)
                            .build();
                    context =
                        (WasiHttpContext)
                            jniContextClass
                                .getConstructor(WasiHttpConfig.class)
                                .newInstance(config);
                    if (context != null && context.isValid()) {
                      successCount.incrementAndGet();
                    }
                  } catch (final Exception e) {
                    errorCount.incrementAndGet();
                    LOGGER.warning("Context creation failed: " + e.getMessage());
                  } finally {
                    if (context != null) {
                      try {
                        context.close();
                      } catch (final Exception e) {
                        // Ignore close errors
                      }
                    }
                  }
                }
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                completionLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(
          completionLatch.await(25, TimeUnit.SECONDS),
          "All threads should complete within timeout");

      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");

      LOGGER.info(
          "Concurrent context creation: "
              + successCount.get()
              + " successes, "
              + errorCount.get()
              + " errors");

      final int expectedTotal = threadCount * contextsPerThread;
      assertEquals(
          expectedTotal, successCount.get(), "All contexts should be created successfully");
      assertEquals(0, errorCount.get(), "No errors should occur");
    }
  }

  @Nested
  @DisplayName("Redirect Handling Edge Cases")
  class RedirectEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle zero max redirects")
    void shouldHandleZeroMaxRedirects(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing zero max redirects configuration");

      final WasiHttpConfig config =
          WasiHttpConfig.builder().followRedirects(true).withMaxRedirects(0).build();

      assertTrue(config.isFollowRedirects(), "Follow redirects should be enabled");
      assertEquals(0, config.getMaxRedirects().get(), "Max redirects should be 0");

      LOGGER.info("Zero max redirects handled");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle large max redirects value")
    void shouldHandleLargeMaxRedirects(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing large max redirects configuration");

      final int largeRedirectCount = 1000;
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .followRedirects(true)
              .withMaxRedirects(largeRedirectCount)
              .build();

      assertEquals(largeRedirectCount, config.getMaxRedirects().get(), "Should store large value");

      LOGGER.info("Large max redirects handled: " + largeRedirectCount);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should not set max redirects when follow redirects disabled")
    void shouldIgnoreMaxRedirectsWhenDisabled(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing max redirects when follow redirects disabled");

      final WasiHttpConfig config =
          WasiHttpConfig.builder().followRedirects(false).withMaxRedirects(10).build();

      assertFalse(config.isFollowRedirects(), "Follow redirects should be disabled");
      // Max redirects value should still be stored even if not used
      assertTrue(config.getMaxRedirects().isPresent(), "Max redirects should still be present");

      LOGGER.info("Max redirects with disabled follow redirects handled");
    }
  }

  @Nested
  @DisplayName("Large Response Body Edge Cases")
  class LargeResponseBodyEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should reject zero body size limits")
    void shouldRejectZeroBodySizeLimits(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing zero body size limits");

      // Zero body size should be rejected - API requires >= 1
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiHttpConfig.builder().withMaxRequestBodySize(0L).build(),
          "Zero max request body size should be rejected");

      assertThrows(
          IllegalArgumentException.class,
          () -> WasiHttpConfig.builder().withMaxResponseBodySize(0L).build(),
          "Zero max response body size should be rejected");

      LOGGER.info("Zero body size limits correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle very large body size limits")
    void shouldHandleVeryLargeBodySizeLimits(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing very large body size limits");

      final long oneGB = 1024L * 1024L * 1024L;
      final long tenGB = 10 * oneGB;

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .withMaxRequestBodySize(oneGB)
              .withMaxResponseBodySize(tenGB)
              .build();

      assertEquals(oneGB, config.getMaxRequestBodySize().get(), "Request size should be 1GB");
      assertEquals(tenGB, config.getMaxResponseBodySize().get(), "Response size should be 10GB");

      LOGGER.info("Large body size limits handled: request=" + oneGB + ", response=" + tenGB);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 1024L, 65536L, 1048576L, 104857600L})
    @DisplayName("should handle various body size limits")
    void shouldHandleVariousBodySizeLimits(final long sizeLimit) {
      assumeWasiHttpAvailable();
      LOGGER.info("Testing body size limit: " + sizeLimit);

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .withMaxRequestBodySize(sizeLimit)
              .withMaxResponseBodySize(sizeLimit)
              .build();

      assertEquals(sizeLimit, config.getMaxRequestBodySize().get(), "Request size should match");
      assertEquals(sizeLimit, config.getMaxResponseBodySize().get(), "Response size should match");

      LOGGER.info("Body size limit " + sizeLimit + " handled correctly");
    }
  }

  @Nested
  @DisplayName("Timeout Edge Cases")
  class TimeoutEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle zero duration timeouts")
    void shouldHandleZeroDurationTimeouts(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing zero duration timeouts");

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .withConnectTimeout(Duration.ZERO)
              .withReadTimeout(Duration.ZERO)
              .withWriteTimeout(Duration.ZERO)
              .build();

      assertEquals(Duration.ZERO, config.getConnectTimeout().get(), "Connect timeout should be 0");
      assertEquals(Duration.ZERO, config.getReadTimeout().get(), "Read timeout should be 0");
      assertEquals(Duration.ZERO, config.getWriteTimeout().get(), "Write timeout should be 0");

      LOGGER.info("Zero duration timeouts handled");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle very long timeouts")
    void shouldHandleVeryLongTimeouts(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing very long timeouts");

      final Duration oneHour = Duration.ofHours(1);
      final Duration oneDay = Duration.ofDays(1);

      final WasiHttpConfig config =
          WasiHttpConfig.builder().withConnectTimeout(oneHour).withReadTimeout(oneDay).build();

      assertEquals(oneHour, config.getConnectTimeout().get(), "Connect timeout should be 1 hour");
      assertEquals(oneDay, config.getReadTimeout().get(), "Read timeout should be 1 day");

      LOGGER.info("Long timeouts handled: connect=1h, read=1d");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle millisecond precision timeouts")
    void shouldHandleMillisecondPrecisionTimeouts(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing millisecond precision timeouts");

      final Duration preciseTimeout = Duration.ofMillis(1234);

      final WasiHttpConfig config =
          WasiHttpConfig.builder().withConnectTimeout(preciseTimeout).build();

      assertEquals(
          preciseTimeout, config.getConnectTimeout().get(), "Should preserve ms precision");
      assertEquals(1234, config.getConnectTimeout().get().toMillis(), "Should be 1234ms");

      LOGGER.info("Millisecond precision timeout handled: " + preciseTimeout.toMillis() + "ms");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle nano precision timeouts")
    void shouldHandleNanoPrecisionTimeouts(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing nano precision timeouts");

      final Duration nanoTimeout = Duration.ofNanos(123456789);

      final WasiHttpConfig config =
          WasiHttpConfig.builder().withConnectTimeout(nanoTimeout).build();

      assertEquals(nanoTimeout, config.getConnectTimeout().get(), "Should preserve nano precision");

      LOGGER.info("Nano precision timeout handled: " + nanoTimeout.toNanos() + "ns");
    }
  }

  @Nested
  @DisplayName("Host Pattern Edge Cases")
  class HostPatternEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should reject empty host pattern")
    void shouldRejectEmptyHostPattern(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing empty host pattern");

      // Empty host pattern should be rejected - API requires non-empty string
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiHttpConfig.builder().allowHost("").build(),
          "Empty host pattern should be rejected");

      LOGGER.info("Empty host pattern correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle very long host pattern")
    void shouldHandleVeryLongHostPattern(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing very long host pattern");

      // Maximum DNS label is 63 characters, domain is 253
      final String longHost = "a".repeat(50) + "." + "b".repeat(50) + ".example.com";

      final WasiHttpConfig config = WasiHttpConfig.builder().allowHost(longHost).build();

      assertTrue(config.getAllowedHosts().contains(longHost), "Should contain long host");

      LOGGER.info("Long host pattern handled: " + longHost.length() + " chars");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle multiple wildcard levels")
    void shouldHandleMultipleWildcardLevels(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing multiple wildcard levels");

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("*.example.com")
              .allowHost("*.*.trusted.org")
              .allowHost("*")
              .build();

      final Set<String> hosts = config.getAllowedHosts();
      assertTrue(hosts.contains("*.example.com"), "Should contain single wildcard");
      assertTrue(hosts.contains("*.*.trusted.org"), "Should contain double wildcard");
      assertTrue(hosts.contains("*"), "Should contain universal wildcard");

      LOGGER.info("Multiple wildcard levels handled: " + hosts.size() + " patterns");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle case sensitivity in hosts")
    void shouldHandleCaseSensitivityInHosts(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing case sensitivity in host patterns");

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("API.EXAMPLE.COM")
              .allowHost("api.example.com")
              .build();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      // DNS is case-insensitive, so both should match either pattern
      assertTrue(context.isHostAllowed("api.example.com"), "Lowercase should be allowed");
      assertTrue(context.isHostAllowed("API.EXAMPLE.COM"), "Uppercase should be allowed");
      assertTrue(context.isHostAllowed("Api.Example.Com"), "Mixed case should be allowed");

      LOGGER.info("Case sensitivity handled correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle numeric IP addresses as hosts")
    void shouldHandleNumericIpAddressesAsHosts(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing numeric IP addresses as hosts");

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("192.168.1.1")
              .allowHost("10.0.0.0")
              .allowHost("127.0.0.1")
              .build();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      assertTrue(context.isHostAllowed("192.168.1.1"), "Should allow specific IP");
      assertTrue(context.isHostAllowed("127.0.0.1"), "Should allow localhost IP");
      assertFalse(context.isHostAllowed("192.168.1.2"), "Should not allow different IP");

      LOGGER.info("IP address hosts handled correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle IPv6 addresses as hosts")
    void shouldHandleIpv6AddressesAsHosts(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing IPv6 addresses as hosts");

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("::1")
              .allowHost("2001:db8::1")
              .allowHost("[::1]")
              .build();

      final Set<String> hosts = config.getAllowedHosts();
      assertTrue(hosts.contains("::1"), "Should contain IPv6 loopback");
      assertTrue(hosts.contains("2001:db8::1"), "Should contain IPv6 address");

      LOGGER.info("IPv6 address hosts handled correctly");
    }
  }

  @Nested
  @DisplayName("Configuration Validation Edge Cases")
  class ConfigurationValidationEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create empty config")
    void shouldCreateEmptyConfig(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing empty config creation");

      final WasiHttpConfig config = WasiHttpConfig.builder().build();

      assertNotNull(config, "Empty config should not be null");
      assertTrue(config.getAllowedHosts().isEmpty(), "Should have no allowed hosts");
      assertTrue(config.getBlockedHosts().isEmpty(), "Should have no blocked hosts");

      LOGGER.info("Empty config created successfully");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle duplicate host patterns")
    void shouldHandleDuplicateHostPatterns(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing duplicate host patterns");

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("api.example.com")
              .allowHost("api.example.com")
              .allowHost("api.example.com")
              .build();

      final Set<String> hosts = config.getAllowedHosts();
      // Set should deduplicate
      assertEquals(1, hosts.size(), "Should have only 1 host after deduplication");
      assertTrue(hosts.contains("api.example.com"), "Should contain the host");

      LOGGER.info("Duplicate hosts deduplicated correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle same host in allow and block lists")
    void shouldHandleSameHostInAllowAndBlockLists(final RuntimeType runtime) throws Exception {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing same host in allow and block lists");

      // Block should take precedence
      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("api.example.com")
              .blockHost("api.example.com")
              .build();

      final Class<?> jniContextClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpContext");
      final WasiHttpContext context =
          (WasiHttpContext)
              jniContextClass.getConstructor(WasiHttpConfig.class).newInstance(config);
      resources.add(context);

      // Block should take precedence
      assertFalse(
          context.isHostAllowed("api.example.com"),
          "Blocked host should take precedence over allowed");

      LOGGER.info("Block list priority verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should preserve config immutability")
    void shouldPreserveConfigImmutability(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing config immutability");

      final WasiHttpConfig original =
          WasiHttpConfig.builder().allowHost("api.example.com").withMaxConnections(10).build();

      // Get the sets
      final Set<String> hosts = original.getAllowedHosts();

      // Attempt to modify should fail or have no effect on original
      assertThrows(
          UnsupportedOperationException.class,
          () -> hosts.add("hacked.com"),
          "Allowed hosts should be immutable");

      // Verify original is unchanged
      assertEquals(1, original.getAllowedHosts().size(), "Original should be unchanged");
      assertTrue(
          original.getAllowedHosts().contains("api.example.com"), "Original host should remain");

      LOGGER.info("Config immutability verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should validate config after build")
    void shouldValidateConfigAfterBuild(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing config validation");

      final WasiHttpConfig config =
          WasiHttpConfig.builder()
              .allowHost("valid.example.com")
              .withMaxConnections(100)
              .withConnectTimeout(Duration.ofSeconds(30))
              .build();

      // Should not throw
      assertDoesNotThrow(() -> config.validate(), "Valid config should pass validation");

      LOGGER.info("Config validation passed");
    }
  }

  @Nested
  @DisplayName("User Agent Edge Cases")
  class UserAgentEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle empty user agent")
    void shouldHandleEmptyUserAgent(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing empty user agent");

      final WasiHttpConfig config = WasiHttpConfig.builder().withUserAgent("").build();

      assertTrue(config.getUserAgent().isPresent(), "User agent should be present");
      assertEquals("", config.getUserAgent().get(), "User agent should be empty string");

      LOGGER.info("Empty user agent handled");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle very long user agent")
    void shouldHandleVeryLongUserAgent(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing very long user agent");

      final String longUserAgent = "LongAgent/1.0 " + "x".repeat(1000);

      final WasiHttpConfig config = WasiHttpConfig.builder().withUserAgent(longUserAgent).build();

      assertEquals(longUserAgent, config.getUserAgent().get(), "Should preserve long user agent");

      LOGGER.info("Long user agent handled: " + longUserAgent.length() + " chars");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle special characters in user agent")
    void shouldHandleSpecialCharactersInUserAgent(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing special characters in user agent");

      final String specialUserAgent = "Agent/1.0 (Linux; U; \u4E2D\u6587; en-US)";

      final WasiHttpConfig config =
          WasiHttpConfig.builder().withUserAgent(specialUserAgent).build();

      assertEquals(specialUserAgent, config.getUserAgent().get(), "Should preserve special chars");

      LOGGER.info("Special characters in user agent handled");
    }
  }

  @Nested
  @DisplayName("HTTP Methods Edge Cases")
  class HttpMethodsEdgeCases {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle empty methods list")
    void shouldHandleEmptyMethodsList(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing empty methods list");

      final WasiHttpConfig config = WasiHttpConfig.builder().build();

      // No methods configured - should return empty or default list
      final List<String> methods = config.getAllowedMethods();
      assertNotNull(methods, "Methods list should not be null");

      LOGGER.info("Empty methods list handled, size: " + methods.size());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle custom HTTP methods")
    void shouldHandleCustomHttpMethods(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing custom HTTP methods");

      final WasiHttpConfig config =
          WasiHttpConfig.builder().allowMethods("GET", "POST", "CUSTOM", "X-SPECIAL").build();

      final List<String> methods = config.getAllowedMethods();
      assertTrue(methods.contains("GET"), "Should contain GET");
      assertTrue(methods.contains("POST"), "Should contain POST");
      assertTrue(methods.contains("CUSTOM"), "Should contain custom method");
      assertTrue(methods.contains("X-SPECIAL"), "Should contain X-prefixed method");

      LOGGER.info("Custom HTTP methods handled: " + methods.size() + " methods");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should handle duplicate methods")
    void shouldHandleDuplicateMethods(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiHttpAvailable();
      LOGGER.info("Testing duplicate HTTP methods");

      final WasiHttpConfig config =
          WasiHttpConfig.builder().allowMethods("GET", "GET", "POST", "GET").build();

      final List<String> methods = config.getAllowedMethods();
      // May or may not deduplicate depending on implementation
      assertTrue(methods.contains("GET"), "Should contain GET");
      assertTrue(methods.contains("POST"), "Should contain POST");

      LOGGER.info("Duplicate methods handled, result size: " + methods.size());
    }
  }
}
