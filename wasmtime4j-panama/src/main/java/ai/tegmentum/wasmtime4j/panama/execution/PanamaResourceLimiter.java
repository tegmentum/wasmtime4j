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

package ai.tegmentum.wasmtime4j.panama.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterConfig;
import ai.tegmentum.wasmtime4j.execution.ResourceLimiterStats;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the ResourceLimiter interface.
 *
 * <p>This class provides resource limiting functionality through Panama Foreign Function API calls
 * to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaResourceLimiter implements ResourceLimiter {

  private static final Logger LOGGER = Logger.getLogger(PanamaResourceLimiter.class.getName());

  private final long limiterId;
  private final ResourceLimiterConfig config;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama resource limiter with the specified configuration.
   *
   * @param config the limiter configuration
   * @return a new resource limiter
   * @throws WasmException if limiter creation fails
   */
  private static MethodHandle getHandle(final String name) throws WasmException {
    return NativeFunctionBindings.getInstance()
        .getMethodHandle(name)
        .orElseThrow(() -> new WasmException("Native function not found: " + name));
  }

  /**
   * Creates a new Panama resource limiter with the specified configuration.
   *
   * @param config the limiter configuration
   * @return a new resource limiter
   * @throws WasmException if limiter creation fails
   */
  public static PanamaResourceLimiter create(final ResourceLimiterConfig config)
      throws WasmException {
    final MethodHandle createHandle = getHandle("wasmtime4j_limiter_create");

    try {
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
          (long)
              createHandle.invoke(
                  maxMemoryBytes,
                  maxMemoryPages,
                  maxTableElements,
                  maxInstances,
                  maxTables,
                  maxMemories);

      if (limiterId < 0) {
        throw new WasmException("Failed to create resource limiter");
      }

      LOGGER.fine("Created resource limiter: " + limiterId);
      return new PanamaResourceLimiter(limiterId, config);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating resource limiter: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new Panama resource limiter with default (no limits) configuration.
   *
   * @return a new resource limiter with no limits
   * @throws WasmException if limiter creation fails
   */
  public static PanamaResourceLimiter createDefault() throws WasmException {
    final MethodHandle createHandle = getHandle("wasmtime4j_limiter_create_default");

    try {
      final long limiterId = (long) createHandle.invoke();

      if (limiterId < 0) {
        throw new WasmException("Failed to create default resource limiter");
      }

      LOGGER.fine("Created default resource limiter: " + limiterId);
      return new PanamaResourceLimiter(limiterId, ResourceLimiterConfig.defaults());
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error creating default resource limiter: " + e.getMessage(), e);
    }
  }

  private PanamaResourceLimiter(final long limiterId, final ResourceLimiterConfig config) {
    this.limiterId = limiterId;
    this.config = config;
  }

  @Override
  public long getId() {
    return limiterId;
  }

  @Override
  public ResourceLimiterConfig getConfig() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Limiter has been closed");
    }
    return config;
  }

  @Override
  public boolean allowMemoryGrow(final long currentPages, final long requestedPages)
      throws WasmException {
    if (closed) {
      throw new IllegalStateException("Limiter has been closed");
    }

    if (currentPages < 0) {
      throw new IllegalArgumentException("Current pages cannot be negative: " + currentPages);
    }
    if (requestedPages < 0) {
      throw new IllegalArgumentException("Requested pages cannot be negative: " + requestedPages);
    }

    final MethodHandle allowHandle = getHandle("wasmtime4j_limiter_allow_memory_grow");

    try {
      final int result = (int) allowHandle.invoke(limiterId, currentPages, requestedPages);
      return result == 1;
    } catch (final Throwable e) {
      throw new WasmException("Error checking memory grow permission: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean allowTableGrow(final long currentElements, final long requestedElements)
      throws WasmException {
    if (closed) {
      throw new IllegalStateException("Limiter has been closed");
    }

    if (currentElements < 0) {
      throw new IllegalArgumentException("Current elements cannot be negative: " + currentElements);
    }
    if (requestedElements < 0) {
      throw new IllegalArgumentException(
          "Requested elements cannot be negative: " + requestedElements);
    }

    final MethodHandle allowHandle = getHandle("wasmtime4j_limiter_allow_table_grow");

    try {
      final int result = (int) allowHandle.invoke(limiterId, currentElements, requestedElements);
      return result == 1;
    } catch (final Throwable e) {
      throw new WasmException("Error checking table grow permission: " + e.getMessage(), e);
    }
  }

  @Override
  public ResourceLimiterStats getStats() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Limiter has been closed");
    }

    final MethodHandle statsHandle = getHandle("wasmtime4j_limiter_get_stats_json");
    final MethodHandle freeHandle = getHandle("wasmtime4j_limiter_string_free");

    try {
      final MemorySegment jsonPtr = (MemorySegment) statsHandle.invoke(limiterId);

      if (jsonPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to get resource limiter stats for limiter: " + limiterId);
      }

      try {
        final String jsonStr = jsonPtr.reinterpret(Long.MAX_VALUE).getString(0);
        final JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

        return new ResourceLimiterStats(
            json.get("total_memory_bytes").getAsLong(),
            json.get("total_table_elements").getAsLong(),
            json.get("memory_grow_requests").getAsLong(),
            json.get("memory_grow_denials").getAsLong(),
            json.get("table_grow_requests").getAsLong(),
            json.get("table_grow_denials").getAsLong());
      } finally {
        freeHandle.invoke(jsonPtr);
      }
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error getting resource limiter stats: " + e.getMessage(), e);
    }
  }

  @Override
  public void resetStats() throws WasmException {
    if (closed) {
      throw new IllegalStateException("Limiter has been closed");
    }

    final MethodHandle resetHandle = getHandle("wasmtime4j_limiter_reset_stats");

    try {
      final int status = (int) resetHandle.invoke(limiterId);
      if (status != 0) {
        throw new WasmException("Failed to reset resource limiter stats for limiter: " + limiterId);
      }
      LOGGER.fine("Reset stats for resource limiter: " + limiterId);
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error resetting resource limiter stats: " + e.getMessage(), e);
    }
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }
    closed = true;

    final MethodHandle freeHandle = getHandle("wasmtime4j_limiter_free");

    try {
      final int status = (int) freeHandle.invoke(limiterId);
      if (status != 0) {
        LOGGER.warning("Failed to free resource limiter: " + limiterId);
      } else {
        LOGGER.fine("Closed resource limiter: " + limiterId);
      }
    } catch (final Throwable e) {
      throw new WasmException("Error closing resource limiter: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the total number of registered limiters.
   *
   * @return the count of registered limiters
   * @throws WasmException if the count cannot be retrieved
   */
  public static int getLimiterCount() throws WasmException {
    final MethodHandle countHandle = getHandle("wasmtime4j_limiter_get_count");

    try {
      final int count = (int) countHandle.invoke();
      if (count < 0) {
        throw new WasmException("Failed to get limiter count");
      }
      return count;
    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new WasmException("Error getting limiter count: " + e.getMessage(), e);
    }
  }
}
