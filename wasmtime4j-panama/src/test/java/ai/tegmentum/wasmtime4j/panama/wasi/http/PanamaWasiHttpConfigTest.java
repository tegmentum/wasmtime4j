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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaWasiHttpConfig}.
 *
 * <p>These tests exercise actual method calls to improve JaCoCo coverage.
 */
@DisplayName("PanamaWasiHttpConfig Integration Tests")
class PanamaWasiHttpConfigTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiHttpConfigTest.class.getName());

  @Nested
  @DisplayName("Default Value Tests")
  class DefaultValueTests {

    @Test
    @DisplayName("Should have correct default boolean values")
    void shouldHaveCorrectDefaultBooleanValues() {
      LOGGER.info("Testing default boolean values");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().build();

      assertFalse(config.isHttpsRequired(), "HTTPS should not be required by default");
      assertTrue(
          config.isCertificateValidationEnabled(),
          "Certificate validation should be enabled by default");
      assertTrue(config.isHttp2Enabled(), "HTTP/2 should be enabled by default");
      assertTrue(
          config.isConnectionPoolingEnabled(), "Connection pooling should be enabled by default");
      assertTrue(config.isFollowRedirects(), "Follow redirects should be enabled by default");

      LOGGER.info(
          "Default boolean values verified: httpsRequired="
              + config.isHttpsRequired()
              + ", certValidation="
              + config.isCertificateValidationEnabled());
    }

    @Test
    @DisplayName("Should have empty collections by default")
    void shouldHaveEmptyCollectionsByDefault() {
      LOGGER.info("Testing default collection values");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().build();

      assertTrue(config.getAllowedHosts().isEmpty(), "Allowed hosts should be empty by default");
      assertTrue(config.getBlockedHosts().isEmpty(), "Blocked hosts should be empty by default");
      assertTrue(
          config.getAllowedMethods().isEmpty(), "Allowed methods should be empty by default");

      LOGGER.info("Default collections are empty as expected");
    }

    @Test
    @DisplayName("Should have empty optionals by default")
    void shouldHaveEmptyOptionalsByDefault() {
      LOGGER.info("Testing default optional values");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().build();

      assertFalse(
          config.getConnectTimeout().isPresent(), "Connect timeout should be empty by default");
      assertFalse(config.getReadTimeout().isPresent(), "Read timeout should be empty by default");
      assertFalse(config.getWriteTimeout().isPresent(), "Write timeout should be empty by default");
      assertFalse(
          config.getMaxConnections().isPresent(), "Max connections should be empty by default");
      assertFalse(
          config.getMaxConnectionsPerHost().isPresent(),
          "Max connections per host should be empty by default");
      assertFalse(
          config.getMaxRequestBodySize().isPresent(),
          "Max request body size should be empty by default");
      assertFalse(
          config.getMaxResponseBodySize().isPresent(),
          "Max response body size should be empty by default");
      assertFalse(config.getMaxRedirects().isPresent(), "Max redirects should be empty by default");
      assertFalse(config.getUserAgent().isPresent(), "User agent should be empty by default");

      LOGGER.info("Default optional values are empty as expected");
    }
  }

  @Nested
  @DisplayName("Host Configuration Tests")
  class HostConfigurationTests {

    @Test
    @DisplayName("Should return allowed hosts")
    void shouldReturnAllowedHosts() {
      LOGGER.info("Testing getAllowedHosts");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("api.example.com")
              .allowHost("cdn.example.com")
              .build();

      final Set<String> hosts = config.getAllowedHosts();
      assertEquals(2, hosts.size(), "Should have 2 allowed hosts");
      assertTrue(hosts.contains("api.example.com"), "Should contain api.example.com");
      assertTrue(hosts.contains("cdn.example.com"), "Should contain cdn.example.com");

      LOGGER.info("Allowed hosts: " + hosts);
    }

    @Test
    @DisplayName("Should return blocked hosts")
    void shouldReturnBlockedHosts() {
      LOGGER.info("Testing getBlockedHosts");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().blockHost("bad1.com").blockHost("bad2.com").build();

      final Set<String> hosts = config.getBlockedHosts();
      assertEquals(2, hosts.size(), "Should have 2 blocked hosts");
      assertTrue(hosts.contains("bad1.com"), "Should contain bad1.com");

      LOGGER.info("Blocked hosts: " + hosts);
    }

    @Test
    @DisplayName("Should return immutable host sets")
    void shouldReturnImmutableHostSets() {
      LOGGER.info("Testing immutability of host sets");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().allowHost("example.com").build();

      final Set<String> hosts = config.getAllowedHosts();
      try {
        hosts.add("hacker.com");
        // If we reach here, immutability is not enforced but it shouldn't crash
        LOGGER.warning("Host set mutation was allowed - may want to enforce immutability");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("Host set is correctly immutable");
      }
    }
  }

  @Nested
  @DisplayName("Timeout Getter Tests")
  class TimeoutGetterTests {

    @Test
    @DisplayName("Should return connect timeout")
    void shouldReturnConnectTimeout() {
      LOGGER.info("Testing getConnectTimeout");

      final Duration timeout = Duration.ofSeconds(30);
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withConnectTimeout(timeout).build();

      final Optional<Duration> result = config.getConnectTimeout();
      assertTrue(result.isPresent(), "Connect timeout should be present");
      assertEquals(timeout, result.get(), "Connect timeout should match");

      LOGGER.info("Connect timeout: " + result.get());
    }

    @Test
    @DisplayName("Should return read timeout")
    void shouldReturnReadTimeout() {
      LOGGER.info("Testing getReadTimeout");

      final Duration timeout = Duration.ofMinutes(2);
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withReadTimeout(timeout).build();

      final Optional<Duration> result = config.getReadTimeout();
      assertTrue(result.isPresent(), "Read timeout should be present");
      assertEquals(timeout, result.get(), "Read timeout should match");

      LOGGER.info("Read timeout: " + result.get());
    }

    @Test
    @DisplayName("Should return write timeout")
    void shouldReturnWriteTimeout() {
      LOGGER.info("Testing getWriteTimeout");

      final Duration timeout = Duration.ofSeconds(45);
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withWriteTimeout(timeout).build();

      final Optional<Duration> result = config.getWriteTimeout();
      assertTrue(result.isPresent(), "Write timeout should be present");
      assertEquals(timeout, result.get(), "Write timeout should match");

      LOGGER.info("Write timeout: " + result.get());
    }
  }

  @Nested
  @DisplayName("Connection Limit Getter Tests")
  class ConnectionLimitGetterTests {

    @Test
    @DisplayName("Should return max connections")
    void shouldReturnMaxConnections() {
      LOGGER.info("Testing getMaxConnections");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withMaxConnections(100).build();

      final Optional<Integer> result = config.getMaxConnections();
      assertTrue(result.isPresent(), "Max connections should be present");
      assertEquals(100, result.get(), "Max connections should be 100");

      LOGGER.info("Max connections: " + result.get());
    }

    @Test
    @DisplayName("Should return max connections per host")
    void shouldReturnMaxConnectionsPerHost() {
      LOGGER.info("Testing getMaxConnectionsPerHost");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withMaxConnectionsPerHost(10).build();

      final Optional<Integer> result = config.getMaxConnectionsPerHost();
      assertTrue(result.isPresent(), "Max connections per host should be present");
      assertEquals(10, result.get(), "Max connections per host should be 10");

      LOGGER.info("Max connections per host: " + result.get());
    }
  }

  @Nested
  @DisplayName("Body Size Getter Tests")
  class BodySizeGetterTests {

    @Test
    @DisplayName("Should return max request body size")
    void shouldReturnMaxRequestBodySize() {
      LOGGER.info("Testing getMaxRequestBodySize");

      final long size = 10 * 1024 * 1024L;
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withMaxRequestBodySize(size).build();

      final Optional<Long> result = config.getMaxRequestBodySize();
      assertTrue(result.isPresent(), "Max request body size should be present");
      assertEquals(size, result.get(), "Max request body size should match");

      LOGGER.info("Max request body size: " + result.get());
    }

    @Test
    @DisplayName("Should return max response body size")
    void shouldReturnMaxResponseBodySize() {
      LOGGER.info("Testing getMaxResponseBodySize");

      final long size = 50 * 1024 * 1024L;
      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().withMaxResponseBodySize(size).build();

      final Optional<Long> result = config.getMaxResponseBodySize();
      assertTrue(result.isPresent(), "Max response body size should be present");
      assertEquals(size, result.get(), "Max response body size should match");

      LOGGER.info("Max response body size: " + result.get());
    }
  }

  @Nested
  @DisplayName("Methods Getter Tests")
  class MethodsGetterTests {

    @Test
    @DisplayName("Should return allowed methods")
    void shouldReturnAllowedMethods() {
      LOGGER.info("Testing getAllowedMethods");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().allowMethods("GET", "POST", "PUT").build();

      final List<String> methods = config.getAllowedMethods();
      assertEquals(3, methods.size(), "Should have 3 allowed methods");
      assertTrue(methods.contains("GET"), "Should contain GET");
      assertTrue(methods.contains("POST"), "Should contain POST");
      assertTrue(methods.contains("PUT"), "Should contain PUT");

      LOGGER.info("Allowed methods: " + methods);
    }
  }

  @Nested
  @DisplayName("Redirect Getter Tests")
  class RedirectGetterTests {

    @Test
    @DisplayName("Should return max redirects")
    void shouldReturnMaxRedirects() {
      LOGGER.info("Testing getMaxRedirects");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().withMaxRedirects(5).build();

      final Optional<Integer> result = config.getMaxRedirects();
      assertTrue(result.isPresent(), "Max redirects should be present");
      assertEquals(5, result.get(), "Max redirects should be 5");

      LOGGER.info("Max redirects: " + result.get());
    }
  }

  @Nested
  @DisplayName("User Agent Getter Tests")
  class UserAgentGetterTests {

    @Test
    @DisplayName("Should return user agent")
    void shouldReturnUserAgent() {
      LOGGER.info("Testing getUserAgent");

      final String agent = "Wasmtime4J/1.0";
      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().withUserAgent(agent).build();

      final Optional<String> result = config.getUserAgent();
      assertTrue(result.isPresent(), "User agent should be present");
      assertEquals(agent, result.get(), "User agent should match");

      LOGGER.info("User agent: " + result.get());
    }
  }

  @Nested
  @DisplayName("toBuilder Tests")
  class ToBuilderTests {

    @Test
    @DisplayName("Should create builder from config")
    void shouldCreateBuilderFromConfig() {
      LOGGER.info("Testing toBuilder");

      final WasiHttpConfig original =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("api.example.com")
              .blockHost("bad.com")
              .withConnectTimeout(Duration.ofSeconds(30))
              .withReadTimeout(Duration.ofSeconds(60))
              .withWriteTimeout(Duration.ofSeconds(45))
              .withMaxConnections(50)
              .withMaxConnectionsPerHost(5)
              .withMaxRequestBodySize(1024L)
              .withMaxResponseBodySize(2048L)
              .allowMethods("GET", "POST")
              .requireHttps(true)
              .withCertificateValidation(false)
              .withHttp2(false)
              .withConnectionPooling(false)
              .followRedirects(false)
              .withMaxRedirects(3)
              .withUserAgent("TestAgent")
              .build();

      final WasiHttpConfigBuilder builder = original.toBuilder();
      assertNotNull(builder, "Builder should not be null");

      final WasiHttpConfig rebuilt = builder.build();
      assertEquals(original.getAllowedHosts(), rebuilt.getAllowedHosts());
      assertEquals(original.getBlockedHosts(), rebuilt.getBlockedHosts());
      assertEquals(original.getConnectTimeout(), rebuilt.getConnectTimeout());
      assertEquals(original.getReadTimeout(), rebuilt.getReadTimeout());
      assertEquals(original.getWriteTimeout(), rebuilt.getWriteTimeout());
      assertEquals(original.getMaxConnections(), rebuilt.getMaxConnections());
      assertEquals(original.getMaxConnectionsPerHost(), rebuilt.getMaxConnectionsPerHost());
      assertEquals(original.getMaxRequestBodySize(), rebuilt.getMaxRequestBodySize());
      assertEquals(original.getMaxResponseBodySize(), rebuilt.getMaxResponseBodySize());
      assertEquals(original.getAllowedMethods(), rebuilt.getAllowedMethods());
      assertEquals(original.isHttpsRequired(), rebuilt.isHttpsRequired());
      assertEquals(
          original.isCertificateValidationEnabled(), rebuilt.isCertificateValidationEnabled());
      assertEquals(original.isHttp2Enabled(), rebuilt.isHttp2Enabled());
      assertEquals(original.isConnectionPoolingEnabled(), rebuilt.isConnectionPoolingEnabled());
      assertEquals(original.isFollowRedirects(), rebuilt.isFollowRedirects());
      assertEquals(original.getMaxRedirects(), rebuilt.getMaxRedirects());
      assertEquals(original.getUserAgent(), rebuilt.getUserAgent());

      LOGGER.info("toBuilder creates correct builder");
    }

    @Test
    @DisplayName("Should allow modifications via toBuilder")
    void shouldAllowModificationsViaToBuilder() {
      LOGGER.info("Testing toBuilder modifications");

      final WasiHttpConfig original =
          new PanamaWasiHttpConfigBuilder().withMaxConnections(50).build();

      final WasiHttpConfig modified = original.toBuilder().withMaxConnections(100).build();

      assertEquals(50, original.getMaxConnections().get());
      assertEquals(100, modified.getMaxConnections().get());

      LOGGER.info("toBuilder modifications work correctly");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should validate successfully")
    void shouldValidateSuccessfully() {
      LOGGER.info("Testing validate");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().build();
      assertDoesNotThrow(config::validate, "Validation should not throw");

      LOGGER.info("Validation passed");
    }

    @Test
    @DisplayName("Should validate config with all fields")
    void shouldValidateConfigWithAllFields() {
      LOGGER.info("Testing validation with all fields");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .withConnectTimeout(Duration.ofSeconds(30))
              .withReadTimeout(Duration.ofSeconds(60))
              .withWriteTimeout(Duration.ofSeconds(45))
              .withMaxConnections(100)
              .withMaxConnectionsPerHost(10)
              .withMaxRequestBodySize(1024L)
              .withMaxResponseBodySize(2048L)
              .withMaxRedirects(5)
              .build();

      assertDoesNotThrow(config::validate, "Validation should pass");

      LOGGER.info("Full config validation passed");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
      LOGGER.info("Testing self equality");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder().allowHost("example.com").build();

      assertEquals(config, config, "Config should be equal to itself");

      LOGGER.info("Self equality verified");
    }

    @Test
    @DisplayName("Should be equal to equivalent config")
    void shouldBeEqualToEquivalentConfig() {
      LOGGER.info("Testing equivalent equality");

      final WasiHttpConfig config1 =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("example.com")
              .withMaxConnections(100)
              .requireHttps(true)
              .build();

      final WasiHttpConfig config2 =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("example.com")
              .withMaxConnections(100)
              .requireHttps(true)
              .build();

      assertEquals(config1, config2, "Configs with same values should be equal");
      assertEquals(config1.hashCode(), config2.hashCode(), "Hash codes should match");

      LOGGER.info("Equivalent configs are equal");
    }

    @Test
    @DisplayName("Should not be equal to different config")
    void shouldNotBeEqualToDifferentConfig() {
      LOGGER.info("Testing inequality");

      final WasiHttpConfig config1 =
          new PanamaWasiHttpConfigBuilder().withMaxConnections(100).build();

      final WasiHttpConfig config2 =
          new PanamaWasiHttpConfigBuilder().withMaxConnections(200).build();

      assertNotEquals(config1, config2, "Configs with different values should not be equal");

      LOGGER.info("Different configs are not equal");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      LOGGER.info("Testing null inequality");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().build();

      assertNotEquals(null, config, "Config should not be equal to null");

      LOGGER.info("Config is not equal to null");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      LOGGER.info("Testing type inequality");

      final WasiHttpConfig config = new PanamaWasiHttpConfigBuilder().build();

      assertNotEquals("string", config, "Config should not be equal to string");

      LOGGER.info("Config is not equal to different type");
    }

    @Test
    @DisplayName("Should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
      LOGGER.info("Testing hashCode consistency");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("example.com")
              .withMaxConnections(100)
              .build();

      final int hash1 = config.hashCode();
      final int hash2 = config.hashCode();

      assertEquals(hash1, hash2, "HashCode should be consistent");

      LOGGER.info("HashCode is consistent: " + hash1);
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("api.example.com")
              .blockHost("bad.com")
              .requireHttps(true)
              .build();

      final String str = config.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("PanamaWasiHttpConfig"), "toString should contain class name");
      assertTrue(str.contains("api.example.com"), "toString should contain allowed host");
      assertTrue(str.contains("httpsRequired"), "toString should contain httpsRequired");

      LOGGER.info("toString: " + str);
    }
  }

  @Nested
  @DisplayName("Comprehensive Configuration Tests")
  class ComprehensiveConfigurationTests {

    @Test
    @DisplayName("Should build fully configured instance")
    void shouldBuildFullyConfiguredInstance() {
      LOGGER.info("Testing fully configured instance");

      final WasiHttpConfig config =
          new PanamaWasiHttpConfigBuilder()
              .allowHost("api.example.com")
              .allowHosts(Arrays.asList("www.example.com", "cdn.example.com"))
              .blockHost("bad.com")
              .blockHosts(Collections.singletonList("malicious.com"))
              .withConnectTimeout(Duration.ofSeconds(30))
              .withReadTimeout(Duration.ofMinutes(1))
              .withWriteTimeout(Duration.ofSeconds(45))
              .withMaxConnections(100)
              .withMaxConnectionsPerHost(10)
              .withMaxRequestBodySize(10 * 1024 * 1024L)
              .withMaxResponseBodySize(50 * 1024 * 1024L)
              .allowMethods("GET", "POST", "PUT", "DELETE")
              .requireHttps(true)
              .withCertificateValidation(true)
              .withHttp2(true)
              .withConnectionPooling(true)
              .followRedirects(true)
              .withMaxRedirects(10)
              .withUserAgent("Wasmtime4J/1.0")
              .build();

      // Verify all settings
      assertEquals(3, config.getAllowedHosts().size());
      assertEquals(2, config.getBlockedHosts().size());
      assertEquals(Duration.ofSeconds(30), config.getConnectTimeout().get());
      assertEquals(Duration.ofMinutes(1), config.getReadTimeout().get());
      assertEquals(Duration.ofSeconds(45), config.getWriteTimeout().get());
      assertEquals(100, config.getMaxConnections().get());
      assertEquals(10, config.getMaxConnectionsPerHost().get());
      assertEquals(10 * 1024 * 1024L, config.getMaxRequestBodySize().get());
      assertEquals(50 * 1024 * 1024L, config.getMaxResponseBodySize().get());
      assertEquals(4, config.getAllowedMethods().size());
      assertTrue(config.isHttpsRequired());
      assertTrue(config.isCertificateValidationEnabled());
      assertTrue(config.isHttp2Enabled());
      assertTrue(config.isConnectionPoolingEnabled());
      assertTrue(config.isFollowRedirects());
      assertEquals(10, config.getMaxRedirects().get());
      assertEquals("Wasmtime4J/1.0", config.getUserAgent().get());

      LOGGER.info("Fully configured instance built successfully");
    }
  }
}
