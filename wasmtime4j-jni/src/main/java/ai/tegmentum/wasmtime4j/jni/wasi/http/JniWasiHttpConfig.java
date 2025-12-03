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

package ai.tegmentum.wasmtime4j.jni.wasi.http;

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JNI implementation of {@link WasiHttpConfig}.
 *
 * <p>This class provides an immutable configuration for WASI HTTP contexts. Instances are created
 * via the {@link JniWasiHttpConfigBuilder}.
 *
 * @since 1.0.0
 */
public final class JniWasiHttpConfig implements WasiHttpConfig {

  private final Set<String> allowedHosts;
  private final Set<String> blockedHosts;
  private final Duration connectTimeout;
  private final Duration readTimeout;
  private final Duration writeTimeout;
  private final Integer maxConnections;
  private final Integer maxConnectionsPerHost;
  private final Long maxRequestBodySize;
  private final Long maxResponseBodySize;
  private final List<String> allowedMethods;
  private final boolean httpsRequired;
  private final boolean certificateValidationEnabled;
  private final boolean http2Enabled;
  private final boolean connectionPoolingEnabled;
  private final boolean followRedirects;
  private final Integer maxRedirects;
  private final String userAgent;

  /**
   * Creates a new JniWasiHttpConfig with the specified settings.
   *
   * @param allowedHosts the set of allowed host patterns
   * @param blockedHosts the set of blocked host patterns
   * @param connectTimeout the connection timeout
   * @param readTimeout the read timeout
   * @param writeTimeout the write timeout
   * @param maxConnections the maximum connections
   * @param maxConnectionsPerHost the maximum connections per host
   * @param maxRequestBodySize the maximum request body size
   * @param maxResponseBodySize the maximum response body size
   * @param allowedMethods the list of allowed HTTP methods
   * @param httpsRequired whether HTTPS is required
   * @param certificateValidationEnabled whether certificate validation is enabled
   * @param http2Enabled whether HTTP/2 is enabled
   * @param connectionPoolingEnabled whether connection pooling is enabled
   * @param followRedirects whether to follow redirects
   * @param maxRedirects the maximum number of redirects
   * @param userAgent the user agent string
   */
  JniWasiHttpConfig(
      final Set<String> allowedHosts,
      final Set<String> blockedHosts,
      final Duration connectTimeout,
      final Duration readTimeout,
      final Duration writeTimeout,
      final Integer maxConnections,
      final Integer maxConnectionsPerHost,
      final Long maxRequestBodySize,
      final Long maxResponseBodySize,
      final List<String> allowedMethods,
      final boolean httpsRequired,
      final boolean certificateValidationEnabled,
      final boolean http2Enabled,
      final boolean connectionPoolingEnabled,
      final boolean followRedirects,
      final Integer maxRedirects,
      final String userAgent) {
    this.allowedHosts =
        Collections.unmodifiableSet(new HashSet<>(Objects.requireNonNull(allowedHosts)));
    this.blockedHosts =
        Collections.unmodifiableSet(new HashSet<>(Objects.requireNonNull(blockedHosts)));
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.writeTimeout = writeTimeout;
    this.maxConnections = maxConnections;
    this.maxConnectionsPerHost = maxConnectionsPerHost;
    this.maxRequestBodySize = maxRequestBodySize;
    this.maxResponseBodySize = maxResponseBodySize;
    this.allowedMethods =
        Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(allowedMethods)));
    this.httpsRequired = httpsRequired;
    this.certificateValidationEnabled = certificateValidationEnabled;
    this.http2Enabled = http2Enabled;
    this.connectionPoolingEnabled = connectionPoolingEnabled;
    this.followRedirects = followRedirects;
    this.maxRedirects = maxRedirects;
    this.userAgent = userAgent;
  }

  @Override
  public Set<String> getAllowedHosts() {
    return allowedHosts;
  }

  @Override
  public Set<String> getBlockedHosts() {
    return blockedHosts;
  }

  @Override
  public Optional<Duration> getConnectTimeout() {
    return Optional.ofNullable(connectTimeout);
  }

  @Override
  public Optional<Duration> getReadTimeout() {
    return Optional.ofNullable(readTimeout);
  }

  @Override
  public Optional<Duration> getWriteTimeout() {
    return Optional.ofNullable(writeTimeout);
  }

  @Override
  public Optional<Integer> getMaxConnections() {
    return Optional.ofNullable(maxConnections);
  }

  @Override
  public Optional<Integer> getMaxConnectionsPerHost() {
    return Optional.ofNullable(maxConnectionsPerHost);
  }

  @Override
  public Optional<Long> getMaxRequestBodySize() {
    return Optional.ofNullable(maxRequestBodySize);
  }

  @Override
  public Optional<Long> getMaxResponseBodySize() {
    return Optional.ofNullable(maxResponseBodySize);
  }

  @Override
  public List<String> getAllowedMethods() {
    return allowedMethods;
  }

  @Override
  public boolean isHttpsRequired() {
    return httpsRequired;
  }

  @Override
  public boolean isCertificateValidationEnabled() {
    return certificateValidationEnabled;
  }

  @Override
  public boolean isHttp2Enabled() {
    return http2Enabled;
  }

  @Override
  public boolean isConnectionPoolingEnabled() {
    return connectionPoolingEnabled;
  }

  @Override
  public boolean isFollowRedirects() {
    return followRedirects;
  }

  @Override
  public Optional<Integer> getMaxRedirects() {
    return Optional.ofNullable(maxRedirects);
  }

  @Override
  public Optional<String> getUserAgent() {
    return Optional.ofNullable(userAgent);
  }

  @Override
  public WasiHttpConfigBuilder toBuilder() {
    final JniWasiHttpConfigBuilder builder = new JniWasiHttpConfigBuilder();
    builder.allowHosts(allowedHosts);
    builder.blockHosts(blockedHosts);
    if (connectTimeout != null) {
      builder.withConnectTimeout(connectTimeout);
    }
    if (readTimeout != null) {
      builder.withReadTimeout(readTimeout);
    }
    if (writeTimeout != null) {
      builder.withWriteTimeout(writeTimeout);
    }
    if (maxConnections != null) {
      builder.withMaxConnections(maxConnections);
    }
    if (maxConnectionsPerHost != null) {
      builder.withMaxConnectionsPerHost(maxConnectionsPerHost);
    }
    if (maxRequestBodySize != null) {
      builder.withMaxRequestBodySize(maxRequestBodySize);
    }
    if (maxResponseBodySize != null) {
      builder.withMaxResponseBodySize(maxResponseBodySize);
    }
    if (!allowedMethods.isEmpty()) {
      builder.allowMethods(allowedMethods.toArray(new String[0]));
    }
    builder.requireHttps(httpsRequired);
    builder.withCertificateValidation(certificateValidationEnabled);
    builder.withHttp2(http2Enabled);
    builder.withConnectionPooling(connectionPoolingEnabled);
    builder.followRedirects(followRedirects);
    if (maxRedirects != null) {
      builder.withMaxRedirects(maxRedirects);
    }
    if (userAgent != null) {
      builder.withUserAgent(userAgent);
    }
    return builder;
  }

  @Override
  public void validate() {
    if (maxConnections != null && maxConnections < 1) {
      throw new IllegalArgumentException("maxConnections must be positive");
    }
    if (maxConnectionsPerHost != null && maxConnectionsPerHost < 1) {
      throw new IllegalArgumentException("maxConnectionsPerHost must be positive");
    }
    if (maxRequestBodySize != null && maxRequestBodySize < 1) {
      throw new IllegalArgumentException("maxRequestBodySize must be positive");
    }
    if (maxResponseBodySize != null && maxResponseBodySize < 1) {
      throw new IllegalArgumentException("maxResponseBodySize must be positive");
    }
    if (maxRedirects != null && maxRedirects < 0) {
      throw new IllegalArgumentException("maxRedirects cannot be negative");
    }
    if (connectTimeout != null && connectTimeout.isNegative()) {
      throw new IllegalArgumentException("connectTimeout cannot be negative");
    }
    if (readTimeout != null && readTimeout.isNegative()) {
      throw new IllegalArgumentException("readTimeout cannot be negative");
    }
    if (writeTimeout != null && writeTimeout.isNegative()) {
      throw new IllegalArgumentException("writeTimeout cannot be negative");
    }
  }

  @Override
  public String toString() {
    return "JniWasiHttpConfig{"
        + "allowedHosts="
        + allowedHosts
        + ", blockedHosts="
        + blockedHosts
        + ", httpsRequired="
        + httpsRequired
        + ", http2Enabled="
        + http2Enabled
        + '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JniWasiHttpConfig)) {
      return false;
    }
    final JniWasiHttpConfig that = (JniWasiHttpConfig) o;
    return httpsRequired == that.httpsRequired
        && certificateValidationEnabled == that.certificateValidationEnabled
        && http2Enabled == that.http2Enabled
        && connectionPoolingEnabled == that.connectionPoolingEnabled
        && followRedirects == that.followRedirects
        && Objects.equals(allowedHosts, that.allowedHosts)
        && Objects.equals(blockedHosts, that.blockedHosts)
        && Objects.equals(connectTimeout, that.connectTimeout)
        && Objects.equals(readTimeout, that.readTimeout)
        && Objects.equals(writeTimeout, that.writeTimeout)
        && Objects.equals(maxConnections, that.maxConnections)
        && Objects.equals(maxConnectionsPerHost, that.maxConnectionsPerHost)
        && Objects.equals(maxRequestBodySize, that.maxRequestBodySize)
        && Objects.equals(maxResponseBodySize, that.maxResponseBodySize)
        && Objects.equals(allowedMethods, that.allowedMethods)
        && Objects.equals(maxRedirects, that.maxRedirects)
        && Objects.equals(userAgent, that.userAgent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        allowedHosts,
        blockedHosts,
        connectTimeout,
        readTimeout,
        writeTimeout,
        maxConnections,
        maxConnectionsPerHost,
        maxRequestBodySize,
        maxResponseBodySize,
        allowedMethods,
        httpsRequired,
        certificateValidationEnabled,
        http2Enabled,
        connectionPoolingEnabled,
        followRedirects,
        maxRedirects,
        userAgent);
  }
}
