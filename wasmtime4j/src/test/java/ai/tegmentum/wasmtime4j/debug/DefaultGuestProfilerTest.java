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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultGuestProfiler} - package-private default GuestProfiler implementation.
 *
 * <p>Tests are in the same package to access the package-private class. Uses a minimal Store stub.
 */
@DisplayName("DefaultGuestProfiler Tests")
class DefaultGuestProfilerTest {

  private Store stubStore;

  @BeforeEach
  void setUp() {
    stubStore = createStubStore();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create profiler with Store")
    void shouldCreateProfilerWithStore() {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      assertNotNull(profiler, "Profiler should be created successfully");
    }

    @Test
    @DisplayName("should create profiler with Store and config")
    void shouldCreateProfilerWithStoreAndConfig() {
      final ProfilerConfig config = ProfilerConfig.defaults();
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore, config);
      assertNotNull(profiler, "Profiler should be created with custom config");
    }

    @Test
    @DisplayName("should reject null store")
    void shouldRejectNullStore() {
      assertThrows(
          NullPointerException.class,
          () -> new DefaultGuestProfiler(null),
          "Constructor should reject null store");
    }

    @Test
    @DisplayName("should reject null config")
    void shouldRejectNullConfig() {
      assertThrows(
          NullPointerException.class,
          () -> new DefaultGuestProfiler(stubStore, null),
          "Constructor should reject null config");
    }
  }

  @Nested
  @DisplayName("Start and Stop Tests")
  class StartStopTests {

    @Test
    @DisplayName("start should begin profiling")
    void startShouldBeginProfiling() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      assertTrue(profiler.isProfiling(), "isProfiling should return true after start()");
    }

    @Test
    @DisplayName("start should throw if already profiling")
    void startShouldThrowIfAlreadyProfiling() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      assertThrows(
          IllegalStateException.class,
          profiler::start,
          "start() should throw if already profiling");
    }

    @Test
    @DisplayName("stop should end profiling")
    void stopShouldEndProfiling() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      profiler.stop();
      assertFalse(profiler.isProfiling(), "isProfiling should return false after stop()");
    }

    @Test
    @DisplayName("stop should throw if not profiling")
    void stopShouldThrowIfNotProfiling() {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      assertThrows(
          IllegalStateException.class, profiler::stop, "stop() should throw if not profiling");
    }
  }

  @Nested
  @DisplayName("IsProfiling Tests")
  class IsProfilingTests {

    @Test
    @DisplayName("isProfiling should return false initially")
    void isProfilingShouldReturnFalseInitially() {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      assertFalse(profiler.isProfiling(), "isProfiling should return false before start()");
    }
  }

  @Nested
  @DisplayName("GetProfileData Tests")
  class GetProfileDataTests {

    @Test
    @DisplayName("getProfileData should return data after stop")
    void getProfileDataShouldReturnDataAfterStop() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      profiler.stop();
      final ProfileData data = profiler.getProfileData();
      assertNotNull(data, "Profile data should not be null after stop");
      assertNotNull(data.getTotalDuration(), "Total duration should not be null");
      assertEquals(0, data.getTotalFunctionCalls(), "Function call count should be 0 initially");
      assertEquals(0, data.getTotalInstructions(), "Instruction count should be 0 initially");
      assertEquals(0, data.getMaxStackDepth(), "Max stack depth should be 0 initially");
      assertNotNull(data.getFunctionProfiles(), "Function profiles should not be null");
      assertTrue(data.getFunctionProfiles().isEmpty(), "Function profiles should be empty");
      assertNotNull(data.getCustomMetrics(), "Custom metrics should not be null");
      assertTrue(data.getCustomMetrics().isEmpty(), "Custom metrics should be empty");
    }

    @Test
    @DisplayName("getProfileData should throw if still profiling")
    void getProfileDataShouldThrowIfStillProfiling() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      assertThrows(
          IllegalStateException.class,
          profiler::getProfileData,
          "getProfileData should throw if profiling is still active");
    }

    @Test
    @DisplayName("getProfileData should have non-negative duration after start-stop")
    void getProfileDataShouldHaveNonNegativeDuration() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      profiler.stop();
      final ProfileData data = profiler.getProfileData();
      assertTrue(data.getTotalDuration().toNanos() >= 0, "Duration should be non-negative");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear profiling state")
    void resetShouldClearProfilingState() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      profiler.reset();
      assertFalse(profiler.isProfiling(), "isProfiling should be false after reset");
    }

    @Test
    @DisplayName("reset should allow restarting profiling")
    void resetShouldAllowRestartingProfiling() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      profiler.stop();
      profiler.reset();
      assertDoesNotThrow(profiler::start, "Should be able to start profiling after reset");
      assertTrue(profiler.isProfiling(), "isProfiling should be true after restarting");
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should stop profiling if active")
    void closeShouldStopProfilingIfActive() throws WasmException {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      profiler.start();
      profiler.close();
      assertFalse(profiler.isProfiling(), "isProfiling should be false after close");
    }

    @Test
    @DisplayName("close should not throw if not profiling")
    void closeShouldNotThrowIfNotProfiling() {
      final DefaultGuestProfiler profiler = new DefaultGuestProfiler(stubStore);
      assertDoesNotThrow(profiler::close, "close() should not throw if not profiling");
    }
  }

  @Nested
  @DisplayName("GuestProfiler Interface Factory Tests")
  class FactoryTests {

    @Test
    @DisplayName("GuestProfiler.create should return DefaultGuestProfiler")
    void guestProfilerCreateShouldReturnDefaultGuestProfiler() throws WasmException {
      final GuestProfiler profiler = GuestProfiler.create(stubStore);
      assertNotNull(profiler, "GuestProfiler.create should return non-null profiler");
      assertTrue(
          profiler instanceof DefaultGuestProfiler,
          "GuestProfiler.create should return a DefaultGuestProfiler instance");
    }

    @Test
    @DisplayName("GuestProfiler.create with config should return DefaultGuestProfiler")
    void guestProfilerCreateWithConfigShouldReturnDefaultGuestProfiler() throws WasmException {
      final ProfilerConfig config =
          ProfilerConfig.builder().trackFunctionCalls(true).trackStackDepth(false).build();
      final GuestProfiler profiler = GuestProfiler.create(stubStore, config);
      assertNotNull(
          profiler, "GuestProfiler.create(store, config) should return non-null profiler");
    }
  }

  /** Creates a minimal Store proxy for testing DefaultGuestProfiler without native runtime. */
  private static Store createStubStore() {
    return (Store)
        java.lang.reflect.Proxy.newProxyInstance(
            Store.class.getClassLoader(),
            new Class<?>[] {Store.class},
            (final Object proxy, final java.lang.reflect.Method method, final Object[] args) -> {
              final String name = method.getName();
              if ("close".equals(name)) {
                return null;
              }
              if ("toString".equals(name)) {
                return "StubStore";
              }
              if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
              }
              if ("equals".equals(name)) {
                return proxy == args[0];
              }
              return null;
            });
  }
}
