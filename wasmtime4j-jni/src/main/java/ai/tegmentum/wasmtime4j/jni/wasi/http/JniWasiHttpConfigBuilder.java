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

import ai.tegmentum.wasmtime4j.wasi.http.AbstractWasiHttpConfigBuilder;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;

/**
 * JNI implementation of {@link WasiHttpConfigBuilder}.
 *
 * <p>This builder provides a fluent API for constructing {@link JniWasiHttpConfig} instances with
 * various security and performance settings.
 *
 * @since 1.0.0
 */
public final class JniWasiHttpConfigBuilder extends AbstractWasiHttpConfigBuilder {

  /** Creates a new JniWasiHttpConfigBuilder with default settings. */
  public JniWasiHttpConfigBuilder() {
    // Default constructor with default values
  }

  @Override
  public WasiHttpConfig build() {
    final JniWasiHttpConfig config =
        new JniWasiHttpConfig(
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
    config.validate();
    return config;
  }
}
