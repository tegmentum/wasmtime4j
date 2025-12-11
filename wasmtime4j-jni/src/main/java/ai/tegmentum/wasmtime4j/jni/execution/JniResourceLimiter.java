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

package ai.tegmentum.wasmtime4j.jni.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterConfig;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterStats;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.util.logging.Logger;

/**
 * JNI implementation of the ResourceLimiter interface.
 *
 * <p>This class provides resource limiting functionality through JNI calls to the native Wasmtime
 * library.
 *
 * @since 1.0.0
 */
public final class JniResourceLimiter extends JniResource implements ResourceLimiter {

  private static final Logger LOGGER = Logger.getLogger(JniResourceLimiter.class.getName());

  private final ResourceLimiterConfig config;

  /**
   * Creates a new JNI resource limiter with the specified configuration.
   *
   * @param config the limiter configuration
   * @return a new resource limiter
   * @throws WasmException if limiter creation fails
   */
  public static JniResourceLimiter create(final ResourceLimiterConfig config)
      throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    final long maxMemoryBytes =
        config.getMaxMemoryBytes().isPresent() ? config.getMaxMemoryBytes().getAsLong() : -1L;
    final long maxMemoryPages =
        config.getMaxMemoryPages().isPresent() ? config.getMaxMemoryPages().getAsLong() : -1L;
    final long maxTableElements =
        config.getMaxTableElements().isPresent() ? config.getMaxTableElements().getAsLong() : -1L;
    final int maxInstances =
        config.getMaxInstances().isPresent() ? config.getMaxInstances().getAsInt() : -1;
    final int maxTables =
        config.getMaxTables().isPresent() ? config.getMaxTables().getAsInt() : -1;
    final int maxMemories =
        config.getMaxMemories().isPresent() ? config.getMaxMemories().getAsInt() : -1;

    final long limiterId =
        nativeCreate(
            maxMemoryBytes, maxMemoryPages, maxTableElements, maxInstances, maxTables, maxMemories);

    if (limiterId < 0) {
      throw new WasmException("Failed to create resource limiter");
    }

    LOGGER.fine("Created resource limiter: " + limiterId);
    return new JniResourceLimiter(limiterId, config);
  }

  /**
   * Creates a new JNI resource limiter with default (no limits) configuration.
   *
   * @return a new resource limiter with no limits
   * @throws WasmException if limiter creation fails
   */
  public static JniResourceLimiter createDefault() throws WasmException {
    final long limiterId = nativeCreateDefault();

    if (limiterId < 0) {
      throw new WasmException("Failed to create default resource limiter");
    }

    LOGGER.fine("Created default resource limiter: " + limiterId);
    return new JniResourceLimiter(limiterId, ResourceLimiterConfig.defaults());
  }

  private JniResourceLimiter(final long nativeHandle, final ResourceLimiterConfig config) {
    super(nativeHandle);
    this.config = config;
  }

  @Override
  public long getId() {
    return getNativeHandle();
  }

  @Override
  public ResourceLimiterConfig getConfig() throws WasmException {
    ensureNotClosed();
    return config;
  }

  @Override
  public boolean allowMemoryGrow(final long currentPages, final long requestedPages)
      throws WasmException {
    ensureNotClosed();

    if (currentPages < 0) {
      throw new IllegalArgumentException("Current pages cannot be negative: " + currentPages);
    }
    if (requestedPages < 0) {
      throw new IllegalArgumentException("Requested pages cannot be negative: " + requestedPages);
    }

    final int result = nativeAllowMemoryGrow(getNativeHandle(), currentPages, requestedPages);
    return result == 1;
  }

  @Override
  public boolean allowTableGrow(final long currentElements, final long requestedElements)
      throws WasmException {
    ensureNotClosed();

    if (currentElements < 0) {
      throw new IllegalArgumentException("Current elements cannot be negative: " + currentElements);
    }
    if (requestedElements < 0) {
      throw new IllegalArgumentException(
          "Requested elements cannot be negative: " + requestedElements);
    }

    final int result = nativeAllowTableGrow(getNativeHandle(), currentElements, requestedElements);
    return result == 1;
  }

  @Override
  public ResourceLimiterStats getStats() throws WasmException {
    ensureNotClosed();

    final String jsonStr = nativeGetStatsJson(getNativeHandle());
    if (jsonStr == null || jsonStr.isEmpty()) {
      throw new WasmException("Failed to get resource limiter stats for limiter: " + getId());
    }

    return parseStatsJson(jsonStr);
  }

  @Override
  public void resetStats() throws WasmException {
    ensureNotClosed();

    final int status = nativeResetStats(getNativeHandle());
    if (status != 0) {
      throw new WasmException("Failed to reset resource limiter stats for limiter: " + getId());
    }
    LOGGER.fine("Reset stats for resource limiter: " + getId());
  }

  @Override
  protected void doClose() throws Exception {
    final int status = nativeFree(nativeHandle);
    if (status != 0) {
      LOGGER.warning("Failed to free resource limiter: " + nativeHandle);
    } else {
      LOGGER.fine("Closed resource limiter: " + nativeHandle);
    }
  }

  @Override
  protected String getResourceType() {
    return "ResourceLimiter";
  }

  /**
   * Gets the total number of registered limiters.
   *
   * @return the count of registered limiters
   * @throws WasmException if the count cannot be retrieved
   */
  public static int getLimiterCount() throws WasmException {
    final int count = nativeGetCount();
    if (count < 0) {
      throw new WasmException("Failed to get limiter count");
    }
    return count;
  }

  /**
   * Parses the stats JSON string into a ResourceLimiterStats object.
   *
   * @param jsonStr the JSON string from native code
   * @return the parsed stats
   * @throws WasmException if parsing fails
   */
  private ResourceLimiterStats parseStatsJson(final String jsonStr) throws WasmException {
    try {
      // Simple JSON parsing without external dependencies
      long totalMemoryBytes = extractLongValue(jsonStr, "total_memory_bytes");
      long totalTableElements = extractLongValue(jsonStr, "total_table_elements");
      long memoryGrowRequests = extractLongValue(jsonStr, "memory_grow_requests");
      long memoryGrowDenials = extractLongValue(jsonStr, "memory_grow_denials");
      long tableGrowRequests = extractLongValue(jsonStr, "table_grow_requests");
      long tableGrowDenials = extractLongValue(jsonStr, "table_grow_denials");

      return new ResourceLimiterStats(
          totalMemoryBytes,
          totalTableElements,
          memoryGrowRequests,
          memoryGrowDenials,
          tableGrowRequests,
          tableGrowDenials);
    } catch (final Exception e) {
      throw new WasmException("Failed to parse stats JSON: " + e.getMessage(), e);
    }
  }

  /**
   * Extracts a long value from a JSON string by key name.
   *
   * @param json the JSON string
   * @param key the key to extract
   * @return the long value
   */
  private long extractLongValue(final String json, final String key) {
    final String pattern = "\"" + key + "\":";
    final int keyStart = json.indexOf(pattern);
    if (keyStart < 0) {
      return 0L;
    }
    final int valueStart = keyStart + pattern.length();
    int valueEnd = valueStart;
    while (valueEnd < json.length()) {
      final char c = json.charAt(valueEnd);
      if (c == ',' || c == '}' || c == ' ') {
        break;
      }
      valueEnd++;
    }
    final String valueStr = json.substring(valueStart, valueEnd).trim();
    return Long.parseLong(valueStr);
  }

  // Native methods
  private static native long nativeCreate(
      long maxMemoryBytes,
      long maxMemoryPages,
      long maxTableElements,
      int maxInstances,
      int maxTables,
      int maxMemories);

  private static native long nativeCreateDefault();

  private static native int nativeFree(long limiterId);

  private static native int nativeAllowMemoryGrow(
      long limiterId, long currentPages, long requestedPages);

  private static native int nativeAllowTableGrow(
      long limiterId, long currentElements, long requestedElements);

  private static native String nativeGetStatsJson(long limiterId);

  private static native int nativeResetStats(long limiterId);

  private static native int nativeGetCount();
}
