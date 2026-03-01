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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration interface for WASI HTTP context creation.
 *
 * <p>WasiHttpConfig encapsulates all configuration options needed to create a WASI HTTP context,
 * including allowed hosts, connection limits, timeouts, and security settings.
 *
 * <p>Configurations are immutable once created and can be reused across multiple context creations.
 * Use {@link WasiHttpConfigBuilder} to create configured instances.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiHttpConfig config = WasiHttpConfig.builder()
 *     .allowHost("api.example.com")
 *     .allowHost("*.trusted.org")
 *     .withConnectTimeout(Duration.ofSeconds(30))
 *     .withMaxConnections(100)
 *     .build();
 *
 * WasiHttpContext httpContext = WasiHttpFactory.createContext(config);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiHttpConfig {

  /**
   * Creates a new configuration builder.
   *
   * @return a new WasiHttpConfigBuilder instance
   */
  static WasiHttpConfigBuilder builder() {
    try {
      final Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.wasi.http.PanamaWasiHttpConfigBuilder");
      return (WasiHttpConfigBuilder) builderClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      try {
        final Class<?> builderClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.http.JniWasiHttpConfigBuilder");
        return (WasiHttpConfigBuilder) builderClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        throw new RuntimeException(
            "No WasiHttpConfigBuilder implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create WASI HTTP config builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create WASI HTTP config builder", e);
    }
  }

  /**
   * Creates a default configuration that blocks all outbound HTTP requests.
   *
   * @return a default WasiHttpConfig instance with no allowed hosts
   */
  static WasiHttpConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Gets the set of allowed host patterns for outbound HTTP requests.
   *
   * <p>Host patterns can include wildcards (e.g., "*.example.com"). If empty, all outbound requests
   * are blocked.
   *
   * @return immutable set of allowed host patterns
   */
  Set<String> getAllowedHosts();

  /**
   * Gets the set of blocked host patterns.
   *
   * <p>Blocked patterns take precedence over allowed patterns.
   *
   * @return immutable set of blocked host patterns
   */
  Set<String> getBlockedHosts();

  /**
   * Gets the connection timeout for HTTP requests.
   *
   * @return the connection timeout, or empty if not specified
   */
  Optional<Duration> getConnectTimeout();

  /**
   * Gets the read timeout for HTTP requests.
   *
   * @return the read timeout, or empty if not specified
   */
  Optional<Duration> getReadTimeout();

  /**
   * Gets the write timeout for HTTP requests.
   *
   * @return the write timeout, or empty if not specified
   */
  Optional<Duration> getWriteTimeout();

  /**
   * Gets the maximum number of concurrent connections.
   *
   * @return the maximum connections, or empty if not specified
   */
  Optional<Integer> getMaxConnections();

  /**
   * Gets the maximum number of connections per host.
   *
   * @return the maximum connections per host, or empty if not specified
   */
  Optional<Integer> getMaxConnectionsPerHost();

  /**
   * Gets the maximum request body size in bytes.
   *
   * @return the maximum request body size, or empty if not specified
   */
  Optional<Long> getMaxRequestBodySize();

  /**
   * Gets the maximum response body size in bytes.
   *
   * @return the maximum response body size, or empty if not specified
   */
  Optional<Long> getMaxResponseBodySize();

  /**
   * Gets the list of allowed HTTP methods.
   *
   * <p>If empty, all standard HTTP methods are allowed.
   *
   * @return immutable list of allowed HTTP methods
   */
  List<String> getAllowedMethods();

  /**
   * Gets whether HTTPS is required for all requests.
   *
   * @return true if HTTPS is required, false otherwise
   */
  boolean isHttpsRequired();

  /**
   * Gets whether certificate validation is enabled.
   *
   * @return true if certificate validation is enabled, false otherwise
   */
  boolean isCertificateValidationEnabled();

  /**
   * Gets whether HTTP/2 is enabled.
   *
   * @return true if HTTP/2 is enabled, false otherwise
   */
  boolean isHttp2Enabled();

  /**
   * Gets whether connection pooling is enabled.
   *
   * @return true if connection pooling is enabled, false otherwise
   */
  boolean isConnectionPoolingEnabled();

  /**
   * Gets whether redirect following is enabled.
   *
   * @return true if redirects are followed, false otherwise
   */
  boolean isFollowRedirects();

  /**
   * Gets the maximum number of redirects to follow.
   *
   * @return the maximum redirects, or empty if not specified
   */
  Optional<Integer> getMaxRedirects();

  /**
   * Gets the user agent string to use for requests.
   *
   * @return the user agent string, or empty if not specified
   */
  Optional<String> getUserAgent();

  /**
   * Creates a new configuration builder based on this configuration.
   *
   * @return a new WasiHttpConfigBuilder initialized with this configuration's values
   */
  WasiHttpConfigBuilder toBuilder();

  /**
   * Validates this configuration for consistency and completeness.
   *
   * @throws IllegalArgumentException if the configuration is invalid
   */
  void validate();
}
