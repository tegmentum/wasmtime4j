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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.GuestProfiler.ProfileFormat;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GuestProfiler} interface.
 *
 * <p>GuestProfiler provides profiling capabilities for WebAssembly execution.
 */
@DisplayName("GuestProfiler Tests")
class GuestProfilerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(GuestProfiler.class.isInterface(), "GuestProfiler should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(GuestProfiler.class),
          "GuestProfiler should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      final Method method = GuestProfiler.class.getMethod("start");
      assertNotNull(method, "start method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = GuestProfiler.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isProfiling method")
    void shouldHaveIsProfilingMethod() throws NoSuchMethodException {
      final Method method = GuestProfiler.class.getMethod("isProfiling");
      assertNotNull(method, "isProfiling method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getProfileData method")
    void shouldHaveGetProfileDataMethod() throws NoSuchMethodException {
      final Method method = GuestProfiler.class.getMethod("getProfileData");
      assertNotNull(method, "getProfileData method should exist");
      assertEquals(ProfileData.class, method.getReturnType(), "Should return ProfileData");
    }

    @Test
    @DisplayName("should have exportTo with Path method")
    void shouldHaveExportToPathMethod() throws NoSuchMethodException {
      final Method method =
          GuestProfiler.class.getMethod("exportTo", Path.class, ProfileFormat.class);
      assertNotNull(method, "exportTo(Path) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have exportTo with OutputStream method")
    void shouldHaveExportToOutputStreamMethod() throws NoSuchMethodException {
      final Method method =
          GuestProfiler.class.getMethod("exportTo", OutputStream.class, ProfileFormat.class);
      assertNotNull(method, "exportTo(OutputStream) method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = GuestProfiler.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = GuestProfiler.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("ProfileFormat Enum Tests")
  class ProfileFormatEnumTests {

    @Test
    @DisplayName("should have JSON value")
    void shouldHaveJsonValue() {
      assertNotNull(ProfileFormat.valueOf("JSON"), "JSON should exist");
    }

    @Test
    @DisplayName("should have FLAMEGRAPH value")
    void shouldHaveFlamegraphValue() {
      assertNotNull(ProfileFormat.valueOf("FLAMEGRAPH"), "FLAMEGRAPH should exist");
    }

    @Test
    @DisplayName("should have CHROME_TRACE value")
    void shouldHaveChromeTraceValue() {
      assertNotNull(ProfileFormat.valueOf("CHROME_TRACE"), "CHROME_TRACE should exist");
    }

    @Test
    @DisplayName("should have PPROF value")
    void shouldHavePprofValue() {
      assertNotNull(ProfileFormat.valueOf("PPROF"), "PPROF should exist");
    }

    @Test
    @DisplayName("should have exactly four values")
    void shouldHaveExactlyFourValues() {
      assertEquals(4, ProfileFormat.values().length, "Should have exactly 4 ProfileFormat values");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create(Store) static method")
    void shouldHaveCreateStoreMethod() throws NoSuchMethodException {
      final Method method =
          GuestProfiler.class.getMethod("create", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "create(Store) method should exist");
      assertEquals(GuestProfiler.class, method.getReturnType(), "Should return GuestProfiler");
    }

    @Test
    @DisplayName("should have create(Store, ProfilerConfig) static method")
    void shouldHaveCreateStoreConfigMethod() throws NoSuchMethodException {
      final Method method =
          GuestProfiler.class.getMethod(
              "create", ai.tegmentum.wasmtime4j.Store.class, ProfilerConfig.class);
      assertNotNull(method, "create(Store, ProfilerConfig) method should exist");
      assertEquals(GuestProfiler.class, method.getReturnType(), "Should return GuestProfiler");
    }
  }

  @Nested
  @DisplayName("Static Factory Behavior Tests")
  class StaticFactoryBehaviorTests {

    @Test
    @DisplayName("create with null store should throw exception")
    void createWithNullStoreShouldThrowException() {
      assertThrows(
          Exception.class,
          () -> GuestProfiler.create(null),
          "create with null store should throw exception");
    }

    @Test
    @DisplayName("create with null store and config should throw exception")
    void createWithNullStoreAndConfigShouldThrowException() {
      assertThrows(
          Exception.class,
          () -> GuestProfiler.create(null, null),
          "create with null store and config should throw exception");
    }
  }
}
