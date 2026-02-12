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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.wasi.permission.WasiResourceLimits;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiResourceUsageTracker} class.
 *
 * <p>WasiResourceUsageTracker provides comprehensive resource usage tracking and statistics
 * collection for WASI operations using Panama FFI.
 */
@DisplayName("WasiResourceUsageTracker Tests")
class WasiResourceUsageTrackerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiResourceUsageTracker.class.getModifiers()),
          "WasiResourceUsageTracker should be public");
      assertTrue(
          Modifier.isFinal(WasiResourceUsageTracker.class.getModifiers()),
          "WasiResourceUsageTracker should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with resource limits")
    void shouldHaveResourceLimitsConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiResourceUsageTracker.class.getConstructor(WasiResourceLimits.class);
      assertNotNull(constructor, "Constructor with WasiResourceLimits should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with resource limits and detailed tracking flag")
    void shouldHaveFullConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiResourceUsageTracker.class.getConstructor(WasiResourceLimits.class, boolean.class);
      assertNotNull(constructor, "Constructor with limits and boolean should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Context Registration Method Tests")
  class ContextRegistrationMethodTests {

    @Test
    @DisplayName("should have registerContext method")
    void shouldHaveRegisterContextMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod("registerContext", String.class);
      assertNotNull(method, "registerContext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have unregisterContext method")
    void shouldHaveUnregisterContextMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod("unregisterContext", String.class);
      assertNotNull(method, "unregisterContext method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Memory Tracking Method Tests")
  class MemoryTrackingMethodTests {

    @Test
    @DisplayName("should have recordMemoryAllocation method")
    void shouldHaveRecordMemoryAllocationMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod(
              "recordMemoryAllocation", String.class, long.class);
      assertNotNull(method, "recordMemoryAllocation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have recordMemoryDeallocation method")
    void shouldHaveRecordMemoryDeallocationMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod(
              "recordMemoryDeallocation", String.class, long.class);
      assertNotNull(method, "recordMemoryDeallocation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("File System Tracking Method Tests")
  class FileSystemTrackingMethodTests {

    @Test
    @DisplayName("should have recordFileSystemOperation method")
    void shouldHaveRecordFileSystemOperationMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod(
              "recordFileSystemOperation",
              String.class,
              WasiFileOperation.class,
              long.class,
              long.class);
      assertNotNull(method, "recordFileSystemOperation method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Execution Tracking Method Tests")
  class ExecutionTrackingMethodTests {

    @Test
    @DisplayName("should have recordCpuTime method")
    void shouldHaveRecordCpuTimeMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod("recordCpuTime", String.class, long.class);
      assertNotNull(method, "recordCpuTime method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have recordExecutionTime method")
    void shouldHaveRecordExecutionTimeMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod("recordExecutionTime", String.class, long.class);
      assertNotNull(method, "recordExecutionTime method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getContextUsageSnapshot method")
    void shouldHaveGetContextUsageSnapshotMethod() throws NoSuchMethodException {
      final Method method =
          WasiResourceUsageTracker.class.getMethod("getContextUsageSnapshot", String.class);
      assertNotNull(method, "getContextUsageSnapshot method should exist");
      assertEquals(
          WasiResourceUsageTracker.ContextResourceUsageSnapshot.class,
          method.getReturnType(),
          "Should return ContextResourceUsageSnapshot");
    }

    @Test
    @DisplayName("should have getGlobalUsage method")
    void shouldHaveGetGlobalUsageMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageTracker.class.getMethod("getGlobalUsage");
      assertNotNull(method, "getGlobalUsage method should exist");
      assertEquals(
          WasiResourceUsageTracker.GlobalResourceUsageSnapshot.class,
          method.getReturnType(),
          "Should return GlobalResourceUsageSnapshot");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageTracker.class.getMethod("getResourceLimits");
      assertNotNull(method, "getResourceLimits method should exist");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }

    @Test
    @DisplayName("should have isDetailedTrackingEnabled method")
    void shouldHaveIsDetailedTrackingEnabledMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageTracker.class.getMethod("isDetailedTrackingEnabled");
      assertNotNull(method, "isDetailedTrackingEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getTrackedContextCount method")
    void shouldHaveGetTrackedContextCountMethod() throws NoSuchMethodException {
      final Method method = WasiResourceUsageTracker.class.getMethod("getTrackedContextCount");
      assertNotNull(method, "getTrackedContextCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }
}
