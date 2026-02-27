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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for WASI HTTP operations.
 *
 * <p>Provides type-safe wrappers for all WASI HTTP native functions including config builder
 * operations, config management, HTTP context lifecycle, statistics collection, and linker
 * integration.
 */
public final class NativeHttpBindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeHttpBindings.class.getName());

  /** Initialization-on-demand holder for thread-safe lazy singleton. */
  private static final class Holder {
    static final NativeHttpBindings INSTANCE = new NativeHttpBindings();
  }

  private NativeHttpBindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeHttpBindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeHttpBindings getInstance() {
    return Holder.INSTANCE;
  }

  private void initializeBindings() {
    // WASI HTTP Config Builder Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_new",
        FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_allow_host",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_block_host",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_allow_all_hosts",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_connect_timeout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_read_timeout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_write_timeout",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_connections",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_connections_per_host",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_request_body_size",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_response_body_size",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_https_required",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_certificate_validation",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_http2_enabled",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_connection_pooling",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_follow_redirects",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_redirects",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_set_user_agent",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_build",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_builder_free",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // WASI HTTP Config Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_default", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_config_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // WASI HTTP Context Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_new",
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_new_default", FunctionDescriptor.of(ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_get_id",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_is_valid",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_is_host_allowed",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_reset_stats",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    // WASI HTTP Statistics Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_total_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_successful_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_failed_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_active_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_bytes_sent",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_bytes_received",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_connection_timeouts",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_read_timeouts",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_blocked_requests",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_body_size_violations",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_active_connections",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_idle_connections",
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_avg_duration_ms",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_min_duration_ms",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_stats_max_duration_ms",
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));

    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_ctx_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    // WASI HTTP Availability Check
    addFunctionBinding(
        "wasmtime4j_panama_wasi_http_is_available", FunctionDescriptor.of(ValueLayout.JAVA_INT));
  }

  // ====================================================================================
  // WASI HTTP Config Builder Functions
  // ====================================================================================

  /**
   * Creates a new WASI HTTP config builder.
   *
   * @return pointer to the config builder, or null on failure
   */
  public MemorySegment wasiHttpConfigBuilderNew() {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_new", MemorySegment.class);
  }

  /**
   * Adds an allowed host to the config builder.
   *
   * @param builderPtr pointer to the config builder
   * @param hostPtr pointer to the host string
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderAllowHost(
      final MemorySegment builderPtr, final MemorySegment hostPtr) {
    validatePointer(builderPtr, "builderPtr");
    validatePointer(hostPtr, "hostPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_allow_host",
        Integer.class,
        builderPtr,
        hostPtr);
  }

  /**
   * Adds a blocked host to the config builder.
   *
   * @param builderPtr pointer to the config builder
   * @param hostPtr pointer to the host string
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderBlockHost(
      final MemorySegment builderPtr, final MemorySegment hostPtr) {
    validatePointer(builderPtr, "builderPtr");
    validatePointer(hostPtr, "hostPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_block_host",
        Integer.class,
        builderPtr,
        hostPtr);
  }

  /**
   * Sets whether all hosts are allowed.
   *
   * @param builderPtr pointer to the config builder
   * @param allow true to allow all hosts
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderAllowAllHosts(
      final MemorySegment builderPtr, final boolean allow) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_allow_all_hosts",
        Integer.class,
        builderPtr,
        allow ? 1 : 0);
  }

  /**
   * Sets the connect timeout in milliseconds.
   *
   * @param builderPtr pointer to the config builder
   * @param timeoutMs timeout in milliseconds
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetConnectTimeout(
      final MemorySegment builderPtr, final long timeoutMs) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_connect_timeout",
        Integer.class,
        builderPtr,
        timeoutMs);
  }

  /**
   * Sets the read timeout in milliseconds.
   *
   * @param builderPtr pointer to the config builder
   * @param timeoutMs timeout in milliseconds
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetReadTimeout(
      final MemorySegment builderPtr, final long timeoutMs) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_read_timeout",
        Integer.class,
        builderPtr,
        timeoutMs);
  }

  /**
   * Sets the write timeout in milliseconds.
   *
   * @param builderPtr pointer to the config builder
   * @param timeoutMs timeout in milliseconds
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetWriteTimeout(
      final MemorySegment builderPtr, final long timeoutMs) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_write_timeout",
        Integer.class,
        builderPtr,
        timeoutMs);
  }

  /**
   * Sets the maximum number of connections.
   *
   * @param builderPtr pointer to the config builder
   * @param maxConnections maximum connections
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxConnections(
      final MemorySegment builderPtr, final int maxConnections) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_connections",
        Integer.class,
        builderPtr,
        maxConnections);
  }

  /**
   * Sets the maximum connections per host.
   *
   * @param builderPtr pointer to the config builder
   * @param maxConnectionsPerHost maximum connections per host
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxConnectionsPerHost(
      final MemorySegment builderPtr, final int maxConnectionsPerHost) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_connections_per_host",
        Integer.class,
        builderPtr,
        maxConnectionsPerHost);
  }

  /**
   * Sets the maximum request body size.
   *
   * @param builderPtr pointer to the config builder
   * @param maxSize maximum size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxRequestBodySize(
      final MemorySegment builderPtr, final long maxSize) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_request_body_size",
        Integer.class,
        builderPtr,
        maxSize);
  }

  /**
   * Sets the maximum response body size.
   *
   * @param builderPtr pointer to the config builder
   * @param maxSize maximum size in bytes
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxResponseBodySize(
      final MemorySegment builderPtr, final long maxSize) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_response_body_size",
        Integer.class,
        builderPtr,
        maxSize);
  }

  /**
   * Sets whether HTTPS is required.
   *
   * @param builderPtr pointer to the config builder
   * @param required true to require HTTPS
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetHttpsRequired(
      final MemorySegment builderPtr, final boolean required) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_https_required",
        Integer.class,
        builderPtr,
        required ? 1 : 0);
  }

  /**
   * Sets whether certificate validation is enabled.
   *
   * @param builderPtr pointer to the config builder
   * @param enabled true to enable certificate validation
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetCertificateValidation(
      final MemorySegment builderPtr, final boolean enabled) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_certificate_validation",
        Integer.class,
        builderPtr,
        enabled ? 1 : 0);
  }

  /**
   * Sets whether HTTP/2 is enabled.
   *
   * @param builderPtr pointer to the config builder
   * @param enabled true to enable HTTP/2
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetHttp2Enabled(
      final MemorySegment builderPtr, final boolean enabled) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_http2_enabled",
        Integer.class,
        builderPtr,
        enabled ? 1 : 0);
  }

  /**
   * Sets whether connection pooling is enabled.
   *
   * @param builderPtr pointer to the config builder
   * @param enabled true to enable connection pooling
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetConnectionPooling(
      final MemorySegment builderPtr, final boolean enabled) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_connection_pooling",
        Integer.class,
        builderPtr,
        enabled ? 1 : 0);
  }

  /**
   * Sets whether to follow redirects.
   *
   * @param builderPtr pointer to the config builder
   * @param follow true to follow redirects
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetFollowRedirects(
      final MemorySegment builderPtr, final boolean follow) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_follow_redirects",
        Integer.class,
        builderPtr,
        follow ? 1 : 0);
  }

  /**
   * Sets the maximum number of redirects to follow.
   *
   * @param builderPtr pointer to the config builder
   * @param maxRedirects maximum redirects
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetMaxRedirects(
      final MemorySegment builderPtr, final int maxRedirects) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_max_redirects",
        Integer.class,
        builderPtr,
        maxRedirects);
  }

  /**
   * Sets the user agent string.
   *
   * @param builderPtr pointer to the config builder
   * @param userAgentPtr pointer to the user agent string
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpConfigBuilderSetUserAgent(
      final MemorySegment builderPtr, final MemorySegment userAgentPtr) {
    validatePointer(builderPtr, "builderPtr");
    validatePointer(userAgentPtr, "userAgentPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_set_user_agent",
        Integer.class,
        builderPtr,
        userAgentPtr);
  }

  /**
   * Builds a WASI HTTP config from the builder.
   *
   * @param builderPtr pointer to the config builder
   * @return pointer to the config, or null on failure
   */
  public MemorySegment wasiHttpConfigBuilderBuild(final MemorySegment builderPtr) {
    validatePointer(builderPtr, "builderPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_config_builder_build", MemorySegment.class, builderPtr);
  }

  /**
   * Frees a WASI HTTP config builder.
   *
   * @param builderPtr pointer to the config builder
   */
  public void wasiHttpConfigBuilderFree(final MemorySegment builderPtr) {
    if (builderPtr != null && !builderPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_http_config_builder_free", Void.class, builderPtr);
    }
  }

  // ====================================================================================
  // WASI HTTP Config Functions
  // ====================================================================================

  /**
   * Creates a default WASI HTTP config.
   *
   * @return pointer to the config, or null on failure
   */
  public MemorySegment wasiHttpConfigDefault() {
    return callNativeFunction("wasmtime4j_panama_wasi_http_config_default", MemorySegment.class);
  }

  /**
   * Frees a WASI HTTP config.
   *
   * @param configPtr pointer to the config
   */
  public void wasiHttpConfigFree(final MemorySegment configPtr) {
    if (configPtr != null && !configPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_http_config_free", Void.class, configPtr);
    }
  }

  // ====================================================================================
  // WASI HTTP Context Functions
  // ====================================================================================

  /**
   * Creates a new WASI HTTP context with the specified config.
   *
   * @param configPtr pointer to the config
   * @return pointer to the context, or null on failure
   */
  public MemorySegment wasiHttpContextNew(final MemorySegment configPtr) {
    validatePointer(configPtr, "configPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_new", MemorySegment.class, configPtr);
  }

  /**
   * Creates a new WASI HTTP context with default config.
   *
   * @return pointer to the context, or null on failure
   */
  public MemorySegment wasiHttpContextNewDefault() {
    return callNativeFunction("wasmtime4j_panama_wasi_http_ctx_new_default", MemorySegment.class);
  }

  /**
   * Gets the ID of a WASI HTTP context.
   *
   * @param contextPtr pointer to the context
   * @return the context ID
   */
  public long wasiHttpContextGetId(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction("wasmtime4j_panama_wasi_http_ctx_get_id", Long.class, contextPtr);
  }

  /**
   * Checks if a WASI HTTP context is valid.
   *
   * @param contextPtr pointer to the context
   * @return 1 if valid, 0 otherwise
   */
  public int wasiHttpContextIsValid(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_is_valid", Integer.class, contextPtr);
  }

  /**
   * Checks if a host is allowed by the context.
   *
   * @param contextPtr pointer to the context
   * @param hostPtr pointer to the host string
   * @return 1 if allowed, 0 otherwise
   */
  public int wasiHttpContextIsHostAllowed(
      final MemorySegment contextPtr, final MemorySegment hostPtr) {
    validatePointer(contextPtr, "contextPtr");
    validatePointer(hostPtr, "hostPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_is_host_allowed", Integer.class, contextPtr, hostPtr);
  }

  /**
   * Resets the statistics for a WASI HTTP context.
   *
   * @param contextPtr pointer to the context
   * @return 0 on success, negative error code on failure
   */
  public int wasiHttpContextResetStats(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_reset_stats", Integer.class, contextPtr);
  }

  // ====================================================================================
  // WASI HTTP Statistics Functions
  // ====================================================================================

  /**
   * Gets the total number of requests.
   *
   * @param contextPtr pointer to the context
   * @return total request count
   */
  public long wasiHttpContextStatsTotalRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_total_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of successful requests.
   *
   * @param contextPtr pointer to the context
   * @return successful request count
   */
  public long wasiHttpContextStatsSuccessfulRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_successful_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of failed requests.
   *
   * @param contextPtr pointer to the context
   * @return failed request count
   */
  public long wasiHttpContextStatsFailedRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_failed_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of active requests.
   *
   * @param contextPtr pointer to the context
   * @return active request count
   */
  public int wasiHttpContextStatsActiveRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_active_requests", Integer.class, contextPtr);
  }

  /**
   * Gets the total bytes sent.
   *
   * @param contextPtr pointer to the context
   * @return total bytes sent
   */
  public long wasiHttpContextStatsBytesSent(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_bytes_sent", Long.class, contextPtr);
  }

  /**
   * Gets the total bytes received.
   *
   * @param contextPtr pointer to the context
   * @return total bytes received
   */
  public long wasiHttpContextStatsBytesReceived(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_bytes_received", Long.class, contextPtr);
  }

  /**
   * Gets the number of connection timeouts.
   *
   * @param contextPtr pointer to the context
   * @return connection timeout count
   */
  public long wasiHttpContextStatsConnectionTimeouts(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_connection_timeouts", Long.class, contextPtr);
  }

  /**
   * Gets the number of read timeouts.
   *
   * @param contextPtr pointer to the context
   * @return read timeout count
   */
  public long wasiHttpContextStatsReadTimeouts(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_read_timeouts", Long.class, contextPtr);
  }

  /**
   * Gets the number of blocked requests.
   *
   * @param contextPtr pointer to the context
   * @return blocked request count
   */
  public long wasiHttpContextStatsBlockedRequests(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_blocked_requests", Long.class, contextPtr);
  }

  /**
   * Gets the number of body size violations.
   *
   * @param contextPtr pointer to the context
   * @return body size violation count
   */
  public long wasiHttpContextStatsBodySizeViolations(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_body_size_violations", Long.class, contextPtr);
  }

  /**
   * Gets the number of active connections.
   *
   * @param contextPtr pointer to the context
   * @return active connection count
   */
  public int wasiHttpContextStatsActiveConnections(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_active_connections", Integer.class, contextPtr);
  }

  /**
   * Gets the number of idle connections.
   *
   * @param contextPtr pointer to the context
   * @return idle connection count
   */
  public int wasiHttpContextStatsIdleConnections(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_idle_connections", Integer.class, contextPtr);
  }

  /**
   * Gets the average request duration in milliseconds.
   *
   * @param contextPtr pointer to the context
   * @return average duration in milliseconds
   */
  public long wasiHttpContextStatsAvgDurationMs(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_avg_duration_ms", Long.class, contextPtr);
  }

  /**
   * Gets the minimum request duration in milliseconds.
   *
   * @param contextPtr pointer to the context
   * @return minimum duration in milliseconds
   */
  public long wasiHttpContextStatsMinDurationMs(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_min_duration_ms", Long.class, contextPtr);
  }

  /**
   * Gets the maximum request duration in milliseconds.
   *
   * @param contextPtr pointer to the context
   * @return maximum duration in milliseconds
   */
  public long wasiHttpContextStatsMaxDurationMs(final MemorySegment contextPtr) {
    validatePointer(contextPtr, "contextPtr");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_http_ctx_stats_max_duration_ms", Long.class, contextPtr);
  }

  /**
   * Frees a WASI HTTP context.
   *
   * @param contextPtr pointer to the context
   */
  public void wasiHttpContextFree(final MemorySegment contextPtr) {
    if (contextPtr != null && !contextPtr.equals(MemorySegment.NULL)) {
      callNativeFunction("wasmtime4j_panama_wasi_http_ctx_free", Void.class, contextPtr);
    }
  }

  // ====================================================================================
  // WASI HTTP Availability Functions
  // ====================================================================================

  /**
   * Checks if WASI HTTP support is available.
   *
   * @return 1 if WASI HTTP is available, 0 otherwise
   */
  public int wasiHttpIsAvailable() {
    return callNativeFunction("wasmtime4j_panama_wasi_http_is_available", Integer.class);
  }
}
