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
package ai.tegmentum.wasmtime4j.panama.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaWasiHttpConfigBuilder}.
 *
 * <p>These tests exercise actual method calls to improve JaCoCo coverage.
 */
@DisplayName("PanamaWasiHttpConfigBuilder Integration Tests")
class PanamaWasiHttpConfigBuilderTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaWasiHttpConfigBuilderTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create builder with default constructor")
    void shouldCreateBuilderWithDefaultConstructor() {
      LOGGER.info("Testing default constructor");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      assertNotNull(builder, "Builder should not be null");

      LOGGER.info("Builder created successfully");
    }

    @Test
    @DisplayName("Should build default config")
    void shouldBuildDefaultConfig() {
      LOGGER.info("Testing default build");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().build();
      assertNotNull(config, "Config should not be null");

      LOGGER.info("Default config built: " + config);
    }
  }

  @Nested
  @DisplayName("Host Configuration Tests")
  class HostConfigurationTests {

    @Test
    @DisplayName("Should allow single host")
    void shouldAllowSingleHost() {
      LOGGER.info("Testing allowHost");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.allowHost("example.com");

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertTrue(config.getAllowedHosts().contains("example.com"));

      LOGGER.info("Single host allowed: " + config.getAllowedHosts());
    }

    @Test
    @DisplayName("Should allow multiple hosts via collection")
    void shouldAllowMultipleHostsViaCollection() {
      LOGGER.info("Testing allowHosts collection");

      final List<String> hosts = Arrays.asList("api.example.com", "www.example.com");
      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().allowHosts(hosts).build();

      final Set<String> allowed = config.getAllowedHosts();
      assertEquals(2, allowed.size());
      assertTrue(allowed.contains("api.example.com"));
      assertTrue(allowed.contains("www.example.com"));

      LOGGER.info("Multiple hosts allowed: " + allowed);
    }

    @Test
    @DisplayName("Should allow all hosts with wildcard")
    void shouldAllowAllHostsWithWildcard() {
      LOGGER.info("Testing allowAllHosts");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().allowAllHosts().build();

      assertTrue(config.getAllowedHosts().contains("*"));

      LOGGER.info("Wildcard allowed");
    }

    @Test
    @DisplayName("Should block single host")
    void shouldBlockSingleHost() {
      LOGGER.info("Testing blockHost");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.blockHost("malicious.com");

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertTrue(config.getBlockedHosts().contains("malicious.com"));

      LOGGER.info("Single host blocked: " + config.getBlockedHosts());
    }

    @Test
    @DisplayName("Should block multiple hosts via collection")
    void shouldBlockMultipleHostsViaCollection() {
      LOGGER.info("Testing blockHosts collection");

      final List<String> hosts = Arrays.asList("bad1.com", "bad2.com");
      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().blockHosts(hosts).build();

      final Set<String> blocked = config.getBlockedHosts();
      assertEquals(2, blocked.size());

      LOGGER.info("Multiple hosts blocked: " + blocked);
    }

    @Test
    @DisplayName("Should reject null host pattern")
    void shouldRejectNullHostPattern() {
      LOGGER.info("Testing null host rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(IllegalArgumentException.class, () -> builder.allowHost(null));
      assertThrows(IllegalArgumentException.class, () -> builder.blockHost(null));

      LOGGER.info("Null host patterns rejected");
    }

    @Test
    @DisplayName("Should reject empty host pattern")
    void shouldRejectEmptyHostPattern() {
      LOGGER.info("Testing empty host rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(IllegalArgumentException.class, () -> builder.allowHost(""));
      assertThrows(IllegalArgumentException.class, () -> builder.blockHost(""));

      LOGGER.info("Empty host patterns rejected");
    }

    @Test
    @DisplayName("Should reject null host collection")
    void shouldRejectNullHostCollection() {
      LOGGER.info("Testing null collection rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(NullPointerException.class, () -> builder.allowHosts(null));
      assertThrows(NullPointerException.class, () -> builder.blockHosts(null));

      LOGGER.info("Null collections rejected");
    }

    @Test
    @DisplayName("Should skip null and empty patterns in collection")
    void shouldSkipNullAndEmptyPatternsInCollection() {
      LOGGER.info("Testing filtering in collections");

      final List<String> hosts = Arrays.asList("valid.com", null, "", "another.com");
      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().allowHosts(hosts).build();

      final Set<String> allowed = config.getAllowedHosts();
      assertEquals(2, allowed.size());
      assertFalse(allowed.contains(null));
      assertFalse(allowed.contains(""));

      LOGGER.info("Null/empty patterns filtered: " + allowed);
    }
  }

  @Nested
  @DisplayName("Timeout Configuration Tests")
  class TimeoutConfigurationTests {

    @Test
    @DisplayName("Should set connect timeout")
    void shouldSetConnectTimeout() {
      LOGGER.info("Testing withConnectTimeout");

      final Duration timeout = Duration.ofSeconds(30);
      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.withConnectTimeout(timeout);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertEquals(timeout, config.getConnectTimeout().get());

      LOGGER.info("Connect timeout set: " + timeout);
    }

    @Test
    @DisplayName("Should set read timeout")
    void shouldSetReadTimeout() {
      LOGGER.info("Testing withReadTimeout");

      final Duration timeout = Duration.ofMinutes(2);
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withReadTimeout(timeout).build();

      assertEquals(timeout, config.getReadTimeout().get());

      LOGGER.info("Read timeout set: " + timeout);
    }

    @Test
    @DisplayName("Should set write timeout")
    void shouldSetWriteTimeout() {
      LOGGER.info("Testing withWriteTimeout");

      final Duration timeout = Duration.ofSeconds(60);
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withWriteTimeout(timeout).build();

      assertEquals(timeout, config.getWriteTimeout().get());

      LOGGER.info("Write timeout set: " + timeout);
    }

    @Test
    @DisplayName("Should reject null timeout")
    void shouldRejectNullTimeout() {
      LOGGER.info("Testing null timeout rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(NullPointerException.class, () -> builder.withConnectTimeout(null));
      assertThrows(NullPointerException.class, () -> builder.withReadTimeout(null));
      assertThrows(NullPointerException.class, () -> builder.withWriteTimeout(null));

      LOGGER.info("Null timeouts rejected");
    }

    @Test
    @DisplayName("Should reject negative timeout")
    void shouldRejectNegativeTimeout() {
      LOGGER.info("Testing negative timeout rejection");

      final Duration negativeTimeout = Duration.ofSeconds(-1);
      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(
          IllegalArgumentException.class, () -> builder.withConnectTimeout(negativeTimeout));
      assertThrows(IllegalArgumentException.class, () -> builder.withReadTimeout(negativeTimeout));
      assertThrows(IllegalArgumentException.class, () -> builder.withWriteTimeout(negativeTimeout));

      LOGGER.info("Negative timeouts rejected");
    }

    @Test
    @DisplayName("Should allow zero timeout")
    void shouldAllowZeroTimeout() {
      LOGGER.info("Testing zero timeout acceptance");

      final Duration zeroTimeout = Duration.ZERO;
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .withConnectTimeout(zeroTimeout)
              .withReadTimeout(zeroTimeout)
              .withWriteTimeout(zeroTimeout)
              .build();

      assertEquals(Duration.ZERO, config.getConnectTimeout().get());
      assertEquals(Duration.ZERO, config.getReadTimeout().get());
      assertEquals(Duration.ZERO, config.getWriteTimeout().get());

      LOGGER.info("Zero timeouts accepted");
    }
  }

  @Nested
  @DisplayName("Connection Limit Tests")
  class ConnectionLimitTests {

    @Test
    @DisplayName("Should set max connections")
    void shouldSetMaxConnections() {
      LOGGER.info("Testing withMaxConnections");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.withMaxConnections(100);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertEquals(100, config.getMaxConnections().get());

      LOGGER.info("Max connections set: 100");
    }

    @Test
    @DisplayName("Should set max connections per host")
    void shouldSetMaxConnectionsPerHost() {
      LOGGER.info("Testing withMaxConnectionsPerHost");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withMaxConnectionsPerHost(10).build();

      assertEquals(10, config.getMaxConnectionsPerHost().get());

      LOGGER.info("Max connections per host set: 10");
    }

    @Test
    @DisplayName("Should reject zero max connections")
    void shouldRejectZeroMaxConnections() {
      LOGGER.info("Testing zero max connections rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> builder.withMaxConnections(0));
      assertTrue(ex.getMessage().contains("positive"));

      LOGGER.info("Zero max connections rejected: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject negative max connections")
    void shouldRejectNegativeMaxConnections() {
      LOGGER.info("Testing negative max connections rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(IllegalArgumentException.class, () -> builder.withMaxConnections(-1));
      assertThrows(IllegalArgumentException.class, () -> builder.withMaxConnectionsPerHost(-5));

      LOGGER.info("Negative max connections rejected");
    }

    @Test
    @DisplayName("Should accept minimum valid connections")
    void shouldAcceptMinimumValidConnections() {
      LOGGER.info("Testing minimum valid connections");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .withMaxConnections(1)
              .withMaxConnectionsPerHost(1)
              .build();

      assertEquals(1, config.getMaxConnections().get());
      assertEquals(1, config.getMaxConnectionsPerHost().get());

      LOGGER.info("Minimum valid connections accepted");
    }
  }

  @Nested
  @DisplayName("Body Size Limit Tests")
  class BodySizeLimitTests {

    @Test
    @DisplayName("Should set max request body size")
    void shouldSetMaxRequestBodySize() {
      LOGGER.info("Testing withMaxRequestBodySize");

      final long size = 10 * 1024 * 1024L;
      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.withMaxRequestBodySize(size);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertEquals(size, config.getMaxRequestBodySize().get());

      LOGGER.info("Max request body size set: " + size);
    }

    @Test
    @DisplayName("Should set max response body size")
    void shouldSetMaxResponseBodySize() {
      LOGGER.info("Testing withMaxResponseBodySize");

      final long size = 50 * 1024 * 1024L;
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withMaxResponseBodySize(size).build();

      assertEquals(size, config.getMaxResponseBodySize().get());

      LOGGER.info("Max response body size set: " + size);
    }

    @Test
    @DisplayName("Should reject zero body size")
    void shouldRejectZeroBodySize() {
      LOGGER.info("Testing zero body size rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(IllegalArgumentException.class, () -> builder.withMaxRequestBodySize(0));
      assertThrows(IllegalArgumentException.class, () -> builder.withMaxResponseBodySize(0));

      LOGGER.info("Zero body sizes rejected");
    }

    @Test
    @DisplayName("Should reject negative body size")
    void shouldRejectNegativeBodySize() {
      LOGGER.info("Testing negative body size rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(IllegalArgumentException.class, () -> builder.withMaxRequestBodySize(-1));
      assertThrows(IllegalArgumentException.class, () -> builder.withMaxResponseBodySize(-100));

      LOGGER.info("Negative body sizes rejected");
    }
  }

  @Nested
  @DisplayName("HTTP Methods Tests")
  class HttpMethodsTests {

    @Test
    @DisplayName("Should set allowed methods")
    void shouldSetAllowedMethods() {
      LOGGER.info("Testing allowMethods");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.allowMethods("GET", "POST", "PUT", "DELETE");

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      final List<String> methods = config.getAllowedMethods();
      assertEquals(4, methods.size());
      assertTrue(methods.contains("GET"));
      assertTrue(methods.contains("POST"));

      LOGGER.info("Allowed methods set: " + methods);
    }

    @Test
    @DisplayName("Should replace methods on second call")
    void shouldReplaceMethodsOnSecondCall() {
      LOGGER.info("Testing method replacement");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .allowMethods("GET", "POST")
              .allowMethods("HEAD", "OPTIONS")
              .build();

      final List<String> methods = config.getAllowedMethods();
      assertEquals(2, methods.size());
      assertTrue(methods.contains("HEAD"));
      assertTrue(methods.contains("OPTIONS"));
      assertFalse(methods.contains("GET"));

      LOGGER.info("Methods replaced: " + methods);
    }

    @Test
    @DisplayName("Should reject null methods array")
    void shouldRejectNullMethodsArray() {
      LOGGER.info("Testing null methods rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      assertThrows(NullPointerException.class, () -> builder.allowMethods((String[]) null));

      LOGGER.info("Null methods array rejected");
    }
  }

  @Nested
  @DisplayName("Boolean Configuration Tests")
  class BooleanConfigurationTests {

    @Test
    @DisplayName("Should set HTTPS requirement")
    void shouldSetHttpsRequirement() {
      LOGGER.info("Testing requireHttps");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.requireHttps(true);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertTrue(config.isHttpsRequired());

      LOGGER.info("HTTPS required: true");
    }

    @Test
    @DisplayName("Should set certificate validation")
    void shouldSetCertificateValidation() {
      LOGGER.info("Testing withCertificateValidation");

      final WasiHttpConfig configEnabled =
          new PanamaWasiHttpConfigBuilder().withCertificateValidation(true).build();
      assertTrue(configEnabled.isCertificateValidationEnabled());

      final WasiHttpConfig configDisabled =
          new PanamaWasiHttpConfigBuilder().withCertificateValidation(false).build();
      assertFalse(configDisabled.isCertificateValidationEnabled());

      LOGGER.info("Certificate validation configured");
    }

    @Test
    @DisplayName("Should set HTTP/2")
    void shouldSetHttp2() {
      LOGGER.info("Testing withHttp2");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.withHttp2(false);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertFalse(config.isHttp2Enabled());

      LOGGER.info("HTTP/2 configured: false");
    }

    @Test
    @DisplayName("Should set connection pooling")
    void shouldSetConnectionPooling() {
      LOGGER.info("Testing withConnectionPooling");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withConnectionPooling(false).build();

      assertFalse(config.isConnectionPoolingEnabled());

      LOGGER.info("Connection pooling configured: false");
    }

    @Test
    @DisplayName("Should set follow redirects")
    void shouldSetFollowRedirects() {
      LOGGER.info("Testing followRedirects");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.followRedirects(false);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertFalse(config.isFollowRedirects());

      LOGGER.info("Follow redirects configured: false");
    }
  }

  @Nested
  @DisplayName("Redirect Limit Tests")
  class RedirectLimitTests {

    @Test
    @DisplayName("Should set max redirects")
    void shouldSetMaxRedirects() {
      LOGGER.info("Testing withMaxRedirects");

      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.withMaxRedirects(5);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertEquals(5, config.getMaxRedirects().get());

      LOGGER.info("Max redirects set: 5");
    }

    @Test
    @DisplayName("Should allow zero max redirects")
    void shouldAllowZeroMaxRedirects() {
      LOGGER.info("Testing zero max redirects");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().withMaxRedirects(0).build();

      assertEquals(0, config.getMaxRedirects().get());

      LOGGER.info("Zero max redirects accepted");
    }

    @Test
    @DisplayName("Should reject negative max redirects")
    void shouldRejectNegativeMaxRedirects() {
      LOGGER.info("Testing negative max redirects rejection");

      final PanamaWasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> builder.withMaxRedirects(-1));
      assertTrue(ex.getMessage().contains("negative"));

      LOGGER.info("Negative max redirects rejected: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("User Agent Tests")
  class UserAgentTests {

    @Test
    @DisplayName("Should set user agent")
    void shouldSetUserAgent() {
      LOGGER.info("Testing withUserAgent");

      final String agent = "Wasmtime4J/1.0";
      final WasiHttpConfigBuilder builder = new PanamaWasiHttpConfigBuilder();
      final WasiHttpConfigBuilder result = builder.withUserAgent(agent);

      assertSame(builder, result, "Should return same builder for chaining");

      final WasiHttpConfig config = builder.build();
      assertEquals(agent, config.getUserAgent().get());

      LOGGER.info("User agent set: " + agent);
    }

    @Test
    @DisplayName("Should allow null user agent")
    void shouldAllowNullUserAgent() {
      LOGGER.info("Testing null user agent");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().withUserAgent(null).build();

      assertFalse(config.getUserAgent().isPresent());

      LOGGER.info("Null user agent accepted");
    }
  }

  @Nested
  @DisplayName("Method Chaining Tests")
  class MethodChainingTests {

    @Test
    @DisplayName("Should support full method chaining")
    void shouldSupportFullMethodChaining() {
      LOGGER.info("Testing full method chaining");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("api.example.com")
              .allowHosts(Collections.singletonList("www.example.com"))
              .allowAllHosts()
              .blockHost("bad.com")
              .blockHosts(Collections.singletonList("malicious.com"))
              .withConnectTimeout(Duration.ofSeconds(30))
              .withReadTimeout(Duration.ofSeconds(60))
              .withWriteTimeout(Duration.ofSeconds(45))
              .withMaxConnections(100)
              .withMaxConnectionsPerHost(10)
              .withMaxRequestBodySize(1024L)
              .withMaxResponseBodySize(2048L)
              .allowMethods("GET", "POST")
              .requireHttps(true)
              .withCertificateValidation(true)
              .withHttp2(true)
              .withConnectionPooling(true)
              .followRedirects(true)
              .withMaxRedirects(5)
              .withUserAgent("TestAgent")
              .build();

      assertNotNull(config);
      assertTrue(config.isHttpsRequired());
      assertEquals(100, config.getMaxConnections().get());

      LOGGER.info("Full method chaining works correctly");
    }
  }
}
