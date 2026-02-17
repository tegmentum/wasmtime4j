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

import ai.tegmentum.wasmtime4j.wasi.http.AbstractWasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * JNI implementation of {@link WasiHttpConfig}.
 *
 * <p>This class provides an immutable configuration for WASI HTTP contexts. Instances are created
 * via the {@link JniWasiHttpConfigBuilder}.
 *
 * @since 1.0.0
 */
public final class JniWasiHttpConfig extends AbstractWasiHttpConfig {

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
    super(
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

  @Override
  protected WasiHttpConfigBuilder createBuilder() {
    return new JniWasiHttpConfigBuilder();
  }
}
