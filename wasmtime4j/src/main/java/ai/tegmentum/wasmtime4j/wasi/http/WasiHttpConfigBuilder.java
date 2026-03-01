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
import java.util.Collection;

/**
 * Builder interface for creating {@link WasiHttpConfig} instances.
 *
 * <p>This builder provides a fluent API for constructing WASI HTTP configurations with various
 * security and performance settings.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiHttpConfig config = WasiHttpConfig.builder()
 *     .allowHost("api.example.com")
 *     .allowHost("cdn.example.com")
 *     .blockHost("internal.example.com")
 *     .withConnectTimeout(Duration.ofSeconds(30))
 *     .withMaxConnections(100)
 *     .requireHttps(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiHttpConfigBuilder {

  /**
   * Allows outbound HTTP requests to the specified host.
   *
   * <p>Host patterns can include wildcards (e.g., "*.example.com").
   *
   * @param hostPattern the host pattern to allow
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hostPattern is null or empty
   */
  WasiHttpConfigBuilder allowHost(String hostPattern);

  /**
   * Allows outbound HTTP requests to the specified hosts.
   *
   * @param hostPatterns the host patterns to allow
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hostPatterns is null
   */
  WasiHttpConfigBuilder allowHosts(Collection<String> hostPatterns);

  /**
   * Allows all outbound HTTP requests.
   *
   * <p><b>WARNING:</b> This is a security risk and should only be used in controlled environments.
   *
   * @return this builder for method chaining
   */
  WasiHttpConfigBuilder allowAllHosts();

  /**
   * Blocks outbound HTTP requests to the specified host.
   *
   * <p>Blocked patterns take precedence over allowed patterns.
   *
   * @param hostPattern the host pattern to block
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hostPattern is null or empty
   */
  WasiHttpConfigBuilder blockHost(String hostPattern);

  /**
   * Blocks outbound HTTP requests to the specified hosts.
   *
   * @param hostPatterns the host patterns to block
   * @return this builder for method chaining
   * @throws IllegalArgumentException if hostPatterns is null
   */
  WasiHttpConfigBuilder blockHosts(Collection<String> hostPatterns);

  /**
   * Sets the connection timeout for HTTP requests.
   *
   * @param timeout the connection timeout
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiHttpConfigBuilder withConnectTimeout(Duration timeout);

  /**
   * Sets the read timeout for HTTP requests.
   *
   * @param timeout the read timeout
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiHttpConfigBuilder withReadTimeout(Duration timeout);

  /**
   * Sets the write timeout for HTTP requests.
   *
   * @param timeout the write timeout
   * @return this builder for method chaining
   * @throws IllegalArgumentException if timeout is null or negative
   */
  WasiHttpConfigBuilder withWriteTimeout(Duration timeout);

  /**
   * Sets the maximum number of concurrent connections.
   *
   * @param maxConnections the maximum number of connections
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxConnections is not positive
   */
  WasiHttpConfigBuilder withMaxConnections(int maxConnections);

  /**
   * Sets the maximum number of connections per host.
   *
   * @param maxConnectionsPerHost the maximum connections per host
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxConnectionsPerHost is not positive
   */
  WasiHttpConfigBuilder withMaxConnectionsPerHost(int maxConnectionsPerHost);

  /**
   * Sets the maximum request body size in bytes.
   *
   * @param maxSize the maximum request body size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxSize is not positive
   */
  WasiHttpConfigBuilder withMaxRequestBodySize(long maxSize);

  /**
   * Sets the maximum response body size in bytes.
   *
   * @param maxSize the maximum response body size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxSize is not positive
   */
  WasiHttpConfigBuilder withMaxResponseBodySize(long maxSize);

  /**
   * Allows only the specified HTTP methods.
   *
   * @param methods the allowed HTTP methods (e.g., "GET", "POST")
   * @return this builder for method chaining
   * @throws IllegalArgumentException if methods is null
   */
  WasiHttpConfigBuilder allowMethods(String... methods);

  /**
   * Requires HTTPS for all outbound requests.
   *
   * @param required true to require HTTPS, false to allow HTTP
   * @return this builder for method chaining
   */
  WasiHttpConfigBuilder requireHttps(boolean required);

  /**
   * Enables or disables certificate validation.
   *
   * <p><b>WARNING:</b> Disabling certificate validation is a security risk.
   *
   * @param enabled true to enable certificate validation
   * @return this builder for method chaining
   */
  WasiHttpConfigBuilder withCertificateValidation(boolean enabled);

  /**
   * Enables or disables HTTP/2 support.
   *
   * @param enabled true to enable HTTP/2
   * @return this builder for method chaining
   */
  WasiHttpConfigBuilder withHttp2(boolean enabled);

  /**
   * Enables or disables connection pooling.
   *
   * @param enabled true to enable connection pooling
   * @return this builder for method chaining
   */
  WasiHttpConfigBuilder withConnectionPooling(boolean enabled);

  /**
   * Enables or disables redirect following.
   *
   * @param follow true to follow redirects
   * @return this builder for method chaining
   */
  WasiHttpConfigBuilder followRedirects(boolean follow);

  /**
   * Sets the maximum number of redirects to follow.
   *
   * @param maxRedirects the maximum number of redirects
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxRedirects is negative
   */
  WasiHttpConfigBuilder withMaxRedirects(int maxRedirects);

  /**
   * Sets the user agent string for HTTP requests.
   *
   * @param userAgent the user agent string
   * @return this builder for method chaining
   */
  WasiHttpConfigBuilder withUserAgent(String userAgent);

  /**
   * Builds the configuration with the specified settings.
   *
   * @return a new WasiHttpConfig instance
   * @throws IllegalStateException if the configuration is invalid
   */
  WasiHttpConfig build();
}
