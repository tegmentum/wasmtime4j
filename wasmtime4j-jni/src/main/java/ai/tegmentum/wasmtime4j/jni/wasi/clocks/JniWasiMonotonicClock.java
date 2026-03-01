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
package ai.tegmentum.wasmtime4j.jni.wasi.clocks;

import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiMonotonicClock;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiMonotonicClock interface.
 *
 * <p>This class provides access to WASI Preview 2 monotonic clock operations through JNI calls to
 * the native Wasmtime library. The monotonic clock measures elapsed time with nanosecond precision
 * and is guaranteed to be non-decreasing.
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>Performance measurement and profiling
 *   <li>Timeout and deadline tracking
 *   <li>Rate limiting and throttling
 *   <li>Scheduling and timing operations
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniWasiMonotonicClock implements WasiMonotonicClock {

  private static final Logger LOGGER = Logger.getLogger(JniWasiMonotonicClock.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiMonotonicClock: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI monotonic clock with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiMonotonicClock(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI monotonic clock with context handle: " + contextHandle);
  }

  @Override
  public long now() {
    return nativeNow(contextHandle);
  }

  @Override
  public long resolution() {
    return nativeResolution(contextHandle);
  }

  @Override
  public WasiPollable subscribeInstant(final long when) {
    if (when < 0) {
      throw new IllegalArgumentException("Instant cannot be negative: " + when);
    }
    final long pollableHandle = nativeSubscribeInstant(contextHandle, when);
    if (pollableHandle <= 0) {
      throw new RuntimeException("Failed to create pollable for instant");
    }
    return new JniWasiPollable(contextHandle, pollableHandle);
  }

  @Override
  public WasiPollable subscribeDuration(final long duration) {
    if (duration < 0) {
      throw new IllegalArgumentException("Duration cannot be negative: " + duration);
    }
    final long pollableHandle = nativeSubscribeDuration(contextHandle, duration);
    if (pollableHandle <= 0) {
      throw new RuntimeException("Failed to create pollable for duration");
    }
    return new JniWasiPollable(contextHandle, pollableHandle);
  }

  /**
   * Native method to get the current monotonic clock instant.
   *
   * @param contextHandle the native context handle
   * @return current instant in nanoseconds
   */
  private static native long nativeNow(long contextHandle);

  /**
   * Native method to get the monotonic clock resolution.
   *
   * @param contextHandle the native context handle
   * @return clock resolution in nanoseconds
   */
  private static native long nativeResolution(long contextHandle);

  /**
   * Native method to subscribe to a specific instant.
   *
   * @param contextHandle the native context handle
   * @param when instant in nanoseconds
   * @return pollable handle
   */
  private static native long nativeSubscribeInstant(long contextHandle, long when);

  /**
   * Native method to subscribe to a duration.
   *
   * @param contextHandle the native context handle
   * @param duration duration in nanoseconds
   * @return pollable handle
   */
  private static native long nativeSubscribeDuration(long contextHandle, long duration);
}
