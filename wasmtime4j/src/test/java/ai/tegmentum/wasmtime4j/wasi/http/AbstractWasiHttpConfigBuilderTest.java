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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AbstractWasiHttpConfigBuilder} validation logic. */
@DisplayName("AbstractWasiHttpConfigBuilder Tests")
class AbstractWasiHttpConfigBuilderTest {

  /** Concrete test subclass that builds a test config. */
  private static final class TestHttpConfigBuilder extends AbstractWasiHttpConfigBuilder {
    @Override
    public WasiHttpConfig build() {
      return new TestHttpConfig(this);
    }
  }

  /** Concrete test subclass of AbstractWasiHttpConfig. */
  private static final class TestHttpConfig extends AbstractWasiHttpConfig {
    TestHttpConfig(TestHttpConfigBuilder builder) {
      super(
          builder.allowedHosts,
          builder.blockedHosts,
          builder.connectTimeout,
          builder.readTimeout,
          builder.writeTimeout,
          builder.maxConnections,
          builder.maxConnectionsPerHost,
          builder.maxRequestBodySize,
          builder.maxResponseBodySize,
          builder.allowedMethods,
          builder.httpsRequired,
          builder.certificateValidationEnabled,
          builder.http2Enabled,
          builder.connectionPoolingEnabled,
          builder.followRedirects,
          builder.maxRedirects,
          builder.userAgent);
    }

    @Override
    protected WasiHttpConfigBuilder createBuilder() {
      return new TestHttpConfigBuilder();
    }
  }

  private TestHttpConfigBuilder builder() {
    return new TestHttpConfigBuilder();
  }

  @Nested
  @DisplayName("Host Pattern Validation")
  class HostPatternValidation {

    @Test
    @DisplayName("should throw when allowHost pattern is null")
    void shouldThrowWhenAllowHostPatternIsNull() {
      assertThrows(IllegalArgumentException.class, () -> builder().allowHost(null));
    }

    @Test
    @DisplayName("should throw when allowHost pattern is empty")
    void shouldThrowWhenAllowHostPatternIsEmpty() {
      assertThrows(IllegalArgumentException.class, () -> builder().allowHost(""));
    }

    @Test
    @DisplayName("should throw when blockHost pattern is null")
    void shouldThrowWhenBlockHostPatternIsNull() {
      assertThrows(IllegalArgumentException.class, () -> builder().blockHost(null));
    }

    @Test
    @DisplayName("should throw when blockHost pattern is empty")
    void shouldThrowWhenBlockHostPatternIsEmpty() {
      assertThrows(IllegalArgumentException.class, () -> builder().blockHost(""));
    }

    @Test
    @DisplayName("should throw when allowHosts collection is null")
    void shouldThrowWhenAllowHostsCollectionIsNull() {
      assertThrows(NullPointerException.class, () -> builder().allowHosts(null));
    }

    @Test
    @DisplayName("should throw when blockHosts collection is null")
    void shouldThrowWhenBlockHostsCollectionIsNull() {
      assertThrows(NullPointerException.class, () -> builder().blockHosts(null));
    }

    @Test
    @DisplayName("allowAllHosts should add wildcard")
    void allowAllHostsShouldAddWildcard() {
      WasiHttpConfig config = builder().allowAllHosts().build();
      assertTrue(config.getAllowedHosts().contains("*"));
    }
  }

  @Nested
  @DisplayName("Timeout Validation")
  class TimeoutValidation {

    @Test
    @DisplayName("should throw when connect timeout is null")
    void shouldThrowWhenConnectTimeoutIsNull() {
      assertThrows(NullPointerException.class, () -> builder().withConnectTimeout(null));
    }

    @Test
    @DisplayName("should throw when connect timeout is negative")
    void shouldThrowWhenConnectTimeoutIsNegative() {
      assertThrows(
          IllegalArgumentException.class,
          () -> builder().withConnectTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    @DisplayName("should throw when read timeout is null")
    void shouldThrowWhenReadTimeoutIsNull() {
      assertThrows(NullPointerException.class, () -> builder().withReadTimeout(null));
    }

    @Test
    @DisplayName("should throw when read timeout is negative")
    void shouldThrowWhenReadTimeoutIsNegative() {
      assertThrows(
          IllegalArgumentException.class, () -> builder().withReadTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    @DisplayName("should throw when write timeout is null")
    void shouldThrowWhenWriteTimeoutIsNull() {
      assertThrows(NullPointerException.class, () -> builder().withWriteTimeout(null));
    }

    @Test
    @DisplayName("should throw when write timeout is negative")
    void shouldThrowWhenWriteTimeoutIsNegative() {
      assertThrows(
          IllegalArgumentException.class, () -> builder().withWriteTimeout(Duration.ofSeconds(-1)));
    }
  }

  @Nested
  @DisplayName("Connection Limit Validation")
  class ConnectionLimitValidation {

    @Test
    @DisplayName("should throw when maxConnections is zero")
    void shouldThrowWhenMaxConnectionsIsZero() {
      assertThrows(IllegalArgumentException.class, () -> builder().withMaxConnections(0));
    }

    @Test
    @DisplayName("should throw when maxConnections is negative")
    void shouldThrowWhenMaxConnectionsIsNegative() {
      assertThrows(IllegalArgumentException.class, () -> builder().withMaxConnections(-1));
    }

    @Test
    @DisplayName("should throw when maxConnectionsPerHost is zero")
    void shouldThrowWhenMaxConnectionsPerHostIsZero() {
      assertThrows(IllegalArgumentException.class, () -> builder().withMaxConnectionsPerHost(0));
    }
  }

  @Nested
  @DisplayName("Body Size Validation")
  class BodySizeValidation {

    @Test
    @DisplayName("should throw when maxRequestBodySize is zero")
    void shouldThrowWhenMaxRequestBodySizeIsZero() {
      assertThrows(IllegalArgumentException.class, () -> builder().withMaxRequestBodySize(0));
    }

    @Test
    @DisplayName("should throw when maxResponseBodySize is zero")
    void shouldThrowWhenMaxResponseBodySizeIsZero() {
      assertThrows(IllegalArgumentException.class, () -> builder().withMaxResponseBodySize(0));
    }
  }

  @Nested
  @DisplayName("Redirect Validation")
  class RedirectValidation {

    @Test
    @DisplayName("should throw when maxRedirects is negative")
    void shouldThrowWhenMaxRedirectsIsNegative() {
      assertThrows(IllegalArgumentException.class, () -> builder().withMaxRedirects(-1));
    }

    @Test
    @DisplayName("should allow zero maxRedirects")
    void shouldAllowZeroMaxRedirects() {
      WasiHttpConfig config = builder().withMaxRedirects(0).build();
      assertTrue(config.getMaxRedirects().isPresent());
    }
  }

  @Nested
  @DisplayName("Method Validation")
  class MethodValidation {

    @Test
    @DisplayName("should throw when methods array is null")
    void shouldThrowWhenMethodsArrayIsNull() {
      assertThrows(NullPointerException.class, () -> builder().allowMethods((String[]) null));
    }
  }

  @Nested
  @DisplayName("Full Build")
  class FullBuild {

    @Test
    @DisplayName("should build config with all settings")
    void shouldBuildConfigWithAllSettings() {
      WasiHttpConfig config =
          builder()
              .allowHost("example.com")
              .allowHosts(Arrays.asList("*.test.com"))
              .blockHost("evil.com")
              .blockHosts(Collections.singleton("bad.com"))
              .withConnectTimeout(Duration.ofSeconds(30))
              .withReadTimeout(Duration.ofSeconds(60))
              .withWriteTimeout(Duration.ofSeconds(30))
              .withMaxConnections(100)
              .withMaxConnectionsPerHost(10)
              .withMaxRequestBodySize(1024 * 1024)
              .withMaxResponseBodySize(10 * 1024 * 1024)
              .allowMethods("GET", "POST")
              .requireHttps(true)
              .withCertificateValidation(true)
              .withHttp2(true)
              .withConnectionPooling(true)
              .followRedirects(true)
              .withMaxRedirects(5)
              .withUserAgent("test-agent")
              .build();

      assertNotNull(config);
      assertTrue(config.getAllowedHosts().contains("example.com"));
      assertTrue(config.getBlockedHosts().contains("evil.com"));
      assertTrue(config.isHttpsRequired());
    }
  }
}
