/*
 * Copyright 2024 Tegmentum AI
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared base class for {@link WasiHttpConfigBuilder} implementations.
 *
 * <p>This class holds all builder state and provides all setter implementations with eager
 * validation. Subclasses need only implement {@link #build()} to construct the runtime-specific
 * {@link WasiHttpConfig} instance.
 *
 * @since 1.0.0
 */
public abstract class AbstractWasiHttpConfigBuilder implements WasiHttpConfigBuilder {

  /** Allowed host patterns. */
  protected final Set<String> allowedHosts = new HashSet<>();

  /** Blocked host patterns. */
  protected final Set<String> blockedHosts = new HashSet<>();

  /** Allowed HTTP methods. */
  protected final List<String> allowedMethods = new ArrayList<>();

  /** Connection timeout. */
  protected Duration connectTimeout;

  /** Read timeout. */
  protected Duration readTimeout;

  /** Write timeout. */
  protected Duration writeTimeout;

  /** Maximum connections. */
  protected Integer maxConnections;

  /** Maximum connections per host. */
  protected Integer maxConnectionsPerHost;

  /** Maximum request body size. */
  protected Long maxRequestBodySize;

  /** Maximum response body size. */
  protected Long maxResponseBodySize;

  /** Whether HTTPS is required. */
  protected boolean httpsRequired = false;

  /** Whether certificate validation is enabled. */
  protected boolean certificateValidationEnabled = true;

  /** Whether HTTP/2 is enabled. */
  protected boolean http2Enabled = true;

  /** Whether connection pooling is enabled. */
  protected boolean connectionPoolingEnabled = true;

  /** Whether to follow redirects. */
  protected boolean followRedirects = true;

  /** Maximum redirects. */
  protected Integer maxRedirects;

  /** User agent string. */
  protected String userAgent;

  @Override
  public WasiHttpConfigBuilder allowHost(final String hostPattern) {
    if (hostPattern == null || hostPattern.isEmpty()) {
      throw new IllegalArgumentException("hostPattern cannot be null or empty");
    }
    allowedHosts.add(hostPattern);
    return this;
  }

  @Override
  public WasiHttpConfigBuilder allowHosts(final Collection<String> hostPatterns) {
    Objects.requireNonNull(hostPatterns, "hostPatterns cannot be null");
    for (final String pattern : hostPatterns) {
      if (pattern != null && !pattern.isEmpty()) {
        allowedHosts.add(pattern);
      }
    }
    return this;
  }

  @Override
  public WasiHttpConfigBuilder allowAllHosts() {
    allowedHosts.add("*");
    return this;
  }

  @Override
  public WasiHttpConfigBuilder blockHost(final String hostPattern) {
    if (hostPattern == null || hostPattern.isEmpty()) {
      throw new IllegalArgumentException("hostPattern cannot be null or empty");
    }
    blockedHosts.add(hostPattern);
    return this;
  }

  @Override
  public WasiHttpConfigBuilder blockHosts(final Collection<String> hostPatterns) {
    Objects.requireNonNull(hostPatterns, "hostPatterns cannot be null");
    for (final String pattern : hostPatterns) {
      if (pattern != null && !pattern.isEmpty()) {
        blockedHosts.add(pattern);
      }
    }
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withConnectTimeout(final Duration timeout) {
    Objects.requireNonNull(timeout, "timeout cannot be null");
    if (timeout.isNegative()) {
      throw new IllegalArgumentException("timeout cannot be negative");
    }
    this.connectTimeout = timeout;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withReadTimeout(final Duration timeout) {
    Objects.requireNonNull(timeout, "timeout cannot be null");
    if (timeout.isNegative()) {
      throw new IllegalArgumentException("timeout cannot be negative");
    }
    this.readTimeout = timeout;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withWriteTimeout(final Duration timeout) {
    Objects.requireNonNull(timeout, "timeout cannot be null");
    if (timeout.isNegative()) {
      throw new IllegalArgumentException("timeout cannot be negative");
    }
    this.writeTimeout = timeout;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withMaxConnections(final int maxConnections) {
    if (maxConnections < 1) {
      throw new IllegalArgumentException("maxConnections must be positive");
    }
    this.maxConnections = maxConnections;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withMaxConnectionsPerHost(final int maxConnectionsPerHost) {
    if (maxConnectionsPerHost < 1) {
      throw new IllegalArgumentException("maxConnectionsPerHost must be positive");
    }
    this.maxConnectionsPerHost = maxConnectionsPerHost;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withMaxRequestBodySize(final long maxSize) {
    if (maxSize < 1) {
      throw new IllegalArgumentException("maxSize must be positive");
    }
    this.maxRequestBodySize = maxSize;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withMaxResponseBodySize(final long maxSize) {
    if (maxSize < 1) {
      throw new IllegalArgumentException("maxSize must be positive");
    }
    this.maxResponseBodySize = maxSize;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder allowMethods(final String... methods) {
    Objects.requireNonNull(methods, "methods cannot be null");
    allowedMethods.clear();
    allowedMethods.addAll(Arrays.asList(methods));
    return this;
  }

  @Override
  public WasiHttpConfigBuilder requireHttps(final boolean required) {
    this.httpsRequired = required;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withCertificateValidation(final boolean enabled) {
    this.certificateValidationEnabled = enabled;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withHttp2(final boolean enabled) {
    this.http2Enabled = enabled;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withConnectionPooling(final boolean enabled) {
    this.connectionPoolingEnabled = enabled;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder followRedirects(final boolean follow) {
    this.followRedirects = follow;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withMaxRedirects(final int maxRedirects) {
    if (maxRedirects < 0) {
      throw new IllegalArgumentException("maxRedirects cannot be negative");
    }
    this.maxRedirects = maxRedirects;
    return this;
  }

  @Override
  public WasiHttpConfigBuilder withUserAgent(final String userAgent) {
    this.userAgent = userAgent;
    return this;
  }
}
