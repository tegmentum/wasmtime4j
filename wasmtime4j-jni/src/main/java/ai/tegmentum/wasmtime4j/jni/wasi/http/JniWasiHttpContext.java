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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpContext;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpStats;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link WasiHttpContext}.
 *
 * <p>This class wraps the native WASI HTTP context and provides HTTP capabilities to WebAssembly
 * modules. It manages the lifecycle of the native context and ensures proper resource cleanup.
 *
 * @since 1.0.0
 */
public final class JniWasiHttpContext implements WasiHttpContext {

  private static final Logger LOGGER = Logger.getLogger(JniWasiHttpContext.class.getName());

  private final WasiHttpConfig config;
  private final long contextHandle;
  private final long contextId;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JniWasiHttpContext with the specified configuration.
   *
   * @param config the HTTP configuration
   * @throws WasmException if the context cannot be created
   * @throws IllegalArgumentException if config is null
   */
  public JniWasiHttpContext(final WasiHttpConfig config) throws WasmException {
    this.config = Objects.requireNonNull(config, "config cannot be null");

    // Ensure native library is loaded
    NativeLibraryLoader.loadLibrary();

    try {
      this.contextHandle =
          nativeCreate(
              config.getAllowedHosts().toArray(new String[0]),
              config.getBlockedHosts().toArray(new String[0]),
              config.getConnectTimeout().map(Duration::toMillis).orElse(-1L),
              config.getReadTimeout().map(Duration::toMillis).orElse(-1L),
              config.getWriteTimeout().map(Duration::toMillis).orElse(-1L),
              config.getMaxConnections().orElse(-1),
              config.getMaxConnectionsPerHost().orElse(-1),
              config.getMaxRequestBodySize().orElse(-1L),
              config.getMaxResponseBodySize().orElse(-1L),
              config.isHttpsRequired(),
              config.isCertificateValidationEnabled(),
              config.isHttp2Enabled(),
              config.isConnectionPoolingEnabled(),
              config.isFollowRedirects(),
              config.getMaxRedirects().orElse(-1),
              config.getUserAgent().orElse(null));
      if (this.contextHandle == 0) {
        throw new WasmException("Failed to create native WASI HTTP context");
      }
      this.contextId = nativeGetContextId(contextHandle);
      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Created WASI HTTP context: " + contextId);
      }
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Failed to create WASI HTTP context: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiHttpConfig getConfig() {
    return config;
  }

  @Override
  public boolean isValid() {
    if (closed.get()) {
      return false;
    }
    return nativeIsValid(contextHandle);
  }

  @Override
  public boolean isHostAllowed(final String host) {
    if (host == null) {
      throw new IllegalArgumentException("host cannot be null");
    }
    if (closed.get()) {
      return false;
    }
    return nativeIsHostAllowed(contextHandle, host);
  }

  @Override
  public WasiHttpStats getStats() {
    if (closed.get()) {
      throw new IllegalStateException("WASI HTTP context has been closed");
    }
    return new JniWasiHttpStats(contextHandle);
  }

  @Override
  public void resetStats() {
    if (closed.get()) {
      throw new IllegalStateException("WASI HTTP context has been closed");
    }
    nativeResetStats(contextHandle);
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      nativeFree(contextHandle);
      if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
        LOGGER.fine("Closed WASI HTTP context: " + contextId);
      }
    }
  }

  /**
   * Returns the native handle for this context.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return contextHandle;
  }

  /**
   * Returns the context ID.
   *
   * @return the context ID
   */
  public long getContextId() {
    return contextId;
  }

  @Override
  public String toString() {
    return "JniWasiHttpContext{"
        + "id="
        + contextId
        + ", valid="
        + isValid()
        + ", closed="
        + closed.get()
        + '}';
  }

  // Native methods

  @SuppressWarnings("checkstyle:ParameterNumber")
  private static native long nativeCreate(
      String[] allowedHosts,
      String[] blockedHosts,
      long connectTimeoutMs,
      long readTimeoutMs,
      long writeTimeoutMs,
      int maxConnections,
      int maxConnectionsPerHost,
      long maxRequestBodySize,
      long maxResponseBodySize,
      boolean httpsRequired,
      boolean certificateValidation,
      boolean http2Enabled,
      boolean connectionPooling,
      boolean followRedirects,
      int maxRedirects,
      String userAgent);

  private static native boolean nativeIsValid(long contextHandle);

  private static native boolean nativeIsHostAllowed(long contextHandle, String host);

  private static native long nativeGetContextId(long contextHandle);

  private static native void nativeResetStats(long contextHandle);

  static native long nativeStatsTotalRequests(long contextHandle);

  static native long nativeStatsSuccessfulRequests(long contextHandle);

  static native long nativeStatsFailedRequests(long contextHandle);

  static native int nativeStatsActiveRequests(long contextHandle);

  static native long nativeStatsBytesSent(long contextHandle);

  static native long nativeStatsBytesReceived(long contextHandle);

  static native long nativeStatsConnectionTimeouts(long contextHandle);

  static native long nativeStatsReadTimeouts(long contextHandle);

  static native long nativeStatsBlockedRequests(long contextHandle);

  static native long nativeStatsBodySizeViolations(long contextHandle);

  static native int nativeStatsActiveConnections(long contextHandle);

  static native int nativeStatsIdleConnections(long contextHandle);

  static native long nativeStatsAvgDurationMs(long contextHandle);

  static native long nativeStatsMinDurationMs(long contextHandle);

  static native long nativeStatsMaxDurationMs(long contextHandle);

  private static native void nativeFree(long contextHandle);
}
