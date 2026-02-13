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

package ai.tegmentum.wasmtime4j.panama.wasi.threads;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeExecutionBindings;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of {@link WasiThreadsContext} for WASI-Threads support.
 *
 * <p>This class provides thread spawning capabilities for WebAssembly modules that use the
 * wasi-threads proposal. It uses Panama Foreign Function API for native interop.
 *
 * <p><strong>Important limitations:</strong>
 *
 * <ul>
 *   <li>A trap or WASI exit in one thread will exit the entire process
 *   <li>Not suitable for multi-tenant embeddings
 *   <li>Requires WASI Preview 1 (not compatible with WASI 0.2)
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaWasiThreadsContext implements WasiThreadsContext {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiThreadsContext.class.getName());

  private static final NativeExecutionBindings NATIVE_BINDINGS =
      NativeExecutionBindings.getInstance();

  /** Maximum valid thread ID as per WASI-Threads specification (0x1FFFFFFF). */
  private static final int MAX_THREAD_ID = 0x1FFFFFFF;

  /** The native memory segment for the WASI-Threads context. */
  private final MemorySegment nativeContext;

  /** Arena for memory management. */
  private final Arena arena;

  /** Counter for tracking the maximum assigned thread ID. */
  private final AtomicInteger maxThreadId = new AtomicInteger(0);

  /** Counter for tracking the current number of active threads. */
  private final AtomicInteger threadCount = new AtomicInteger(1); // Main thread starts at 1

  /** Flag indicating if WASI-Threads support is enabled. */
  private final boolean enabled;

  /** Resource lifecycle handle. */
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama WASI-Threads context.
   *
   * @param nativeContext the native memory segment for the WASI-Threads context
   * @param arena the arena used for memory management
   * @param enabled whether WASI-Threads support is enabled
   */
  PanamaWasiThreadsContext(
      final MemorySegment nativeContext, final Arena arena, final boolean enabled) {
    if (nativeContext == null || nativeContext.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native context cannot be null");
    }
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }

    this.nativeContext = nativeContext;
    this.arena = arena;
    this.enabled = enabled;

    // Capture values for safety net (must not capture 'this')
    final MemorySegment safetyNativeCtx = nativeContext;
    final Arena safetyArena = arena;
    final AtomicInteger safetyThreadCount = this.threadCount;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiThreadsContext",
            () -> {
              final int remaining = safetyThreadCount.get();
              if (remaining > 1) {
                LOGGER.warning(
                    String.format(
                        "Closing WASI-Threads context with %d threads still active",
                        remaining - 1));
              }
              try {
                NATIVE_BINDINGS.wasiThreadsContextClose(safetyNativeCtx);
              } catch (final Throwable t) {
                throw new Exception("Error closing WASI-Threads context", t);
              } finally {
                safetyArena.close();
              }
            },
            this,
            () -> {
              try {
                NATIVE_BINDINGS.wasiThreadsContextClose(safetyNativeCtx);
              } catch (final Throwable t) {
                LOGGER.warning("Safety net failed to close WASI-Threads context: " + t);
              } finally {
                safetyArena.close();
              }
            });

    LOGGER.info(
        String.format(
            "Created Panama WASI-Threads context: %s, enabled: %s", nativeContext, enabled));
  }

  @Override
  public int spawn(final int threadStartArg) throws WasmException {
    ensureNotClosed();

    if (!enabled) {
      throw new WasmException("WASI-Threads is not enabled for this context");
    }

    try {
      final int threadId = NATIVE_BINDINGS.wasiThreadsSpawn(nativeContext, threadStartArg);

      if (threadId > 0) {
        // Update tracking counters
        threadCount.incrementAndGet();

        // Update max thread ID if this is higher
        int currentMax;
        do {
          currentMax = maxThreadId.get();
          if (threadId <= currentMax) {
            break;
          }
        } while (!maxThreadId.compareAndSet(currentMax, threadId));

        LOGGER.fine(String.format("Spawned thread with ID: %d, arg: %d", threadId, threadStartArg));
      } else {
        LOGGER.warning(
            String.format(
                "Failed to spawn thread with arg: %d, returned: %d", threadStartArg, threadId));
      }

      return threadId;
    } catch (final Exception e) {
      throw new WasmException("Failed to spawn thread: " + e.getMessage(), e);
    }
  }

  @Override
  public int getThreadCount() {
    ensureNotClosed();
    return threadCount.get();
  }

  @Override
  public boolean isEnabled() {
    return enabled && !resourceHandle.isClosed();
  }

  @Override
  public int getMaxThreadId() {
    ensureNotClosed();
    return maxThreadId.get();
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed() && enabled;
  }

  /**
   * Gets the native memory segment for this context.
   *
   * @return the native context memory segment
   */
  public MemorySegment getNativeContext() {
    ensureNotClosed();
    return nativeContext;
  }

  /**
   * Decrements the thread count when a thread completes.
   *
   * @param threadId the ID of the thread that completed
   */
  void onThreadCompleted(final int threadId) {
    if (threadId <= 0) {
      throw new IllegalArgumentException("Thread ID must be positive");
    }

    final int remaining = threadCount.decrementAndGet();
    LOGGER.fine(String.format("Thread %d completed, remaining threads: %d", threadId, remaining));
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Ensures that this context has not been closed.
   *
   * @throws IllegalStateException if the context has been closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }
}
