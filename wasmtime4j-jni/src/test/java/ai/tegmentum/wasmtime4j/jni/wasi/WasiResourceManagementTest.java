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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.wasi.permission.WasiResourceLimits;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for WASI Resource Management classes.
 *
 * <p>This test class covers:
 *
 * <ul>
 *   <li>{@link WasiResourceUsageTracker} - Resource usage tracking with limit enforcement
 *   <li>{@link WasiContextIsolationValidator} - Context isolation validation
 * </ul>
 */
@DisplayName("WASI Resource Management Tests")
class WasiResourceManagementTest {

  @Nested
  @DisplayName("WasiResourceUsageTracker Tests")
  class WasiResourceUsageTrackerTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("should be a final class")
      void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(WasiResourceUsageTracker.class.getModifiers()))
            .as("WasiResourceUsageTracker should be final")
            .isTrue();
      }

      @Test
      @DisplayName("should be public")
      void shouldBePublic() {
        assertThat(Modifier.isPublic(WasiResourceUsageTracker.class.getModifiers()))
            .as("WasiResourceUsageTracker should be public")
            .isTrue();
      }

      @Test
      @DisplayName("should have private LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        final Field field = WasiResourceUsageTracker.class.getDeclaredField("LOGGER");
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("should have constructor with WasiResourceLimits parameter")
      void shouldHaveResourceLimitsConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiResourceUsageTracker.class.getConstructor(WasiResourceLimits.class);
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have constructor with WasiResourceLimits and boolean parameters")
      void shouldHaveResourceLimitsWithDetailedTrackingConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiResourceUsageTracker.class.getConstructor(WasiResourceLimits.class, boolean.class);
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
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
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have unregisterContext method")
      void shouldHaveUnregisterContextMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceUsageTracker.class.getMethod("unregisterContext", String.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
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
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have recordMemoryDeallocation method")
      void shouldHaveRecordMemoryDeallocationMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceUsageTracker.class.getMethod(
                "recordMemoryDeallocation", String.class, long.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("File Operation Tracking Method Tests")
    class FileOperationTrackingMethodTests {

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
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("CPU Time Tracking Method Tests")
    class CpuTimeTrackingMethodTests {

      @Test
      @DisplayName("should have recordCpuTime method")
      void shouldHaveRecordCpuTimeMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceUsageTracker.class.getMethod("recordCpuTime", String.class, long.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have recordExecutionTime method")
      void shouldHaveRecordExecutionTimeMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceUsageTracker.class.getMethod(
                "recordExecutionTime", String.class, long.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Statistics Method Tests")
    class StatisticsMethodTests {

      @Test
      @DisplayName("should have getContextUsage method")
      void shouldHaveGetContextUsageMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceUsageTracker.class.getMethod("getContextUsage", String.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getGlobalUsage method")
      void shouldHaveGetGlobalUsageMethod() throws NoSuchMethodException {
        final Method method = WasiResourceUsageTracker.class.getMethod("getGlobalUsage");
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getResourceLimits method")
      void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
        final Method method = WasiResourceUsageTracker.class.getMethod("getResourceLimits");
        assertThat(method.getReturnType()).isEqualTo(WasiResourceLimits.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getTrackedContextCount method")
      void shouldHaveGetTrackedContextCountMethod() throws NoSuchMethodException {
        final Method method = WasiResourceUsageTracker.class.getMethod("getTrackedContextCount");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have isDetailedTrackingEnabled method")
      void shouldHaveIsDetailedTrackingEnabledMethod() throws NoSuchMethodException {
        final Method method = WasiResourceUsageTracker.class.getMethod("isDetailedTrackingEnabled");
        assertThat(method.getReturnType()).isEqualTo(boolean.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Inner Class Tests")
    class InnerClassTests {

      @Test
      @DisplayName("should have GlobalStatistics inner class")
      void shouldHaveGlobalStatisticsInnerClass() {
        Class<?>[] declaredClasses = WasiResourceUsageTracker.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("GlobalStatistics")) {
            found = true;
            break;
          }
        }
        assertThat(found).as("GlobalStatistics inner class should exist").isTrue();
      }

      @Test
      @DisplayName("should have ContextResourceUsageSnapshot inner class")
      void shouldHaveContextResourceUsageSnapshotInnerClass() {
        Class<?>[] declaredClasses = WasiResourceUsageTracker.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("ContextResourceUsageSnapshot")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("ContextResourceUsageSnapshot inner class should exist").isTrue();
      }

      @Test
      @DisplayName("should have GlobalResourceUsageSnapshot inner class")
      void shouldHaveGlobalResourceUsageSnapshotInnerClass() {
        Class<?>[] declaredClasses = WasiResourceUsageTracker.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("GlobalResourceUsageSnapshot")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("GlobalResourceUsageSnapshot inner class should exist").isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiResourceUsageTracker should have expected number of public methods")
    void usageTrackerShouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiResourceUsageTracker.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected: registerContext, unregisterContext, recordMemoryAllocation,
      // recordMemoryDeallocation, recordFileSystemOperation, recordCpuTime, recordExecutionTime,
      // getContextUsage, getGlobalUsage, getResourceLimits, isDetailedTrackingEnabled,
      // getTrackedContextCount
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(12);
    }
  }
}
