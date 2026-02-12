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

import ai.tegmentum.wasmtime4j.wasi.permission.WasiResourceLimits;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for WASI Resource Management classes.
 *
 * <p>This test class covers:
 *
 * <ul>
 *   <li>{@link WasiResourceLeakDetector} - Resource leak detection with phantom references
 *   <li>{@link WasiResourceUsageTracker} - Resource usage tracking with limit enforcement
 *   <li>{@link WasiContextIsolationValidator} - Context isolation validation
 * </ul>
 */
@DisplayName("WASI Resource Management Tests")
class WasiResourceManagementTest {

  @Nested
  @DisplayName("WasiResourceLeakDetector Tests")
  class WasiResourceLeakDetectorTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("should be a final class")
      void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(WasiResourceLeakDetector.class.getModifiers()))
            .as("WasiResourceLeakDetector should be final")
            .isTrue();
      }

      @Test
      @DisplayName("should be public")
      void shouldBePublic() {
        assertThat(Modifier.isPublic(WasiResourceLeakDetector.class.getModifiers()))
            .as("WasiResourceLeakDetector should be public")
            .isTrue();
      }

      @Test
      @DisplayName("should implement AutoCloseable")
      void shouldImplementAutoCloseable() {
        assertThat(AutoCloseable.class.isAssignableFrom(WasiResourceLeakDetector.class))
            .as("WasiResourceLeakDetector should implement AutoCloseable")
            .isTrue();
      }

      @Test
      @DisplayName("should have private LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        final Field field = WasiResourceLeakDetector.class.getDeclaredField("LOGGER");
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("should have default constructor")
      void shouldHaveDefaultConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor = WasiResourceLeakDetector.class.getConstructor();
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have constructor with configuration parameters")
      void shouldHaveConfigurationConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiResourceLeakDetector.class.getConstructor(int.class, Duration.class, int.class);
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Tracking Method Tests")
    class TrackingMethodTests {

      @Test
      @DisplayName("should have trackWasiContext method")
      void shouldHaveTrackWasiContextMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceLeakDetector.class.getMethod(
                "trackWasiContext", String.class, WasiContext.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have trackFileHandle method")
      void shouldHaveTrackFileHandleMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceLeakDetector.class.getMethod(
                "trackFileHandle", int.class, WasiFileHandle.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have trackMemorySegment method")
      void shouldHaveTrackMemorySegmentMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceLeakDetector.class.getMethod(
                "trackMemorySegment", long.class, Object.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have untrackWasiContext method")
      void shouldHaveUntrackWasiContextMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceLeakDetector.class.getMethod("untrackWasiContext", String.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have untrackFileHandle method")
      void shouldHaveUntrackFileHandleMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceLeakDetector.class.getMethod("untrackFileHandle", int.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have untrackMemorySegment method")
      void shouldHaveUntrackMemorySegmentMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceLeakDetector.class.getMethod("untrackMemorySegment", long.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Detection Method Tests")
    class DetectionMethodTests {

      @Test
      @DisplayName("should have performLeakDetection method")
      void shouldHavePerformLeakDetectionMethod() throws NoSuchMethodException {
        final Method method = WasiResourceLeakDetector.class.getMethod("performLeakDetection");
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Statistics Method Tests")
    class StatisticsMethodTests {

      @Test
      @DisplayName("should have getStatistics method")
      void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
        final Method method = WasiResourceLeakDetector.class.getMethod("getStatistics");
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getTrackedContextCount method")
      void shouldHaveGetTrackedContextCountMethod() throws NoSuchMethodException {
        final Method method = WasiResourceLeakDetector.class.getMethod("getTrackedContextCount");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getTrackedFileHandleCount method")
      void shouldHaveGetTrackedFileHandleCountMethod() throws NoSuchMethodException {
        final Method method = WasiResourceLeakDetector.class.getMethod("getTrackedFileHandleCount");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getTrackedMemorySegmentCount method")
      void shouldHaveGetTrackedMemorySegmentCountMethod() throws NoSuchMethodException {
        final Method method =
            WasiResourceLeakDetector.class.getMethod("getTrackedMemorySegmentCount");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Lifecycle Method Tests")
    class LifecycleMethodTests {

      @Test
      @DisplayName("should have close method")
      void shouldHaveCloseMethod() throws NoSuchMethodException {
        final Method method = WasiResourceLeakDetector.class.getMethod("close");
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Inner Class Tests")
    class InnerClassTests {

      @Test
      @DisplayName("should have ResourceStatistics inner class")
      void shouldHaveResourceStatisticsInnerClass() {
        Class<?>[] declaredClasses = WasiResourceLeakDetector.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("ResourceStatistics")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("ResourceStatistics inner class should exist").isTrue();
      }

      @Test
      @DisplayName("should have LeakDetectionResults inner class")
      void shouldHaveLeakDetectionResultsInnerClass() {
        Class<?>[] declaredClasses = WasiResourceLeakDetector.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("LeakDetectionResults")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("LeakDetectionResults inner class should exist").isTrue();
      }
    }

    @Nested
    @DisplayName("ResourceStatistics Method Tests")
    class ResourceStatisticsMethodTests {

      private Class<?> getResourceStatisticsClass() {
        for (Class<?> declaredClass : WasiResourceLeakDetector.class.getDeclaredClasses()) {
          if (declaredClass.getSimpleName().equals("ResourceStatistics")) {
            return declaredClass;
          }
        }
        return null;
      }

      @Test
      @DisplayName("ResourceStatistics should have getContextsCreated method")
      void shouldHaveGetContextsCreatedMethod() throws NoSuchMethodException {
        Class<?> statsClass = getResourceStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getContextsCreated");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("ResourceStatistics should have getFileHandlesCreated method")
      void shouldHaveGetFileHandlesCreatedMethod() throws NoSuchMethodException {
        Class<?> statsClass = getResourceStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getFileHandlesCreated");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("ResourceStatistics should have getActiveContexts method")
      void shouldHaveGetActiveContextsMethod() throws NoSuchMethodException {
        Class<?> statsClass = getResourceStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getActiveContexts");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("LeakDetectionResults Method Tests")
    class LeakDetectionResultsMethodTests {

      private Class<?> getLeakDetectionResultsClass() {
        for (Class<?> declaredClass : WasiResourceLeakDetector.class.getDeclaredClasses()) {
          if (declaredClass.getSimpleName().equals("LeakDetectionResults")) {
            return declaredClass;
          }
        }
        return null;
      }

      @Test
      @DisplayName("LeakDetectionResults should have getLeakedContexts method")
      void shouldHaveGetLeakedContextsMethod() throws NoSuchMethodException {
        Class<?> resultsClass = getLeakDetectionResultsClass();
        assertThat(resultsClass).isNotNull();

        final Method method = resultsClass.getMethod("getLeakedContexts");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("LeakDetectionResults should have getTotalLeaked method")
      void shouldHaveGetTotalLeakedMethod() throws NoSuchMethodException {
        Class<?> resultsClass = getLeakDetectionResultsClass();
        assertThat(resultsClass).isNotNull();

        final Method method = resultsClass.getMethod("getTotalLeaked");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("LeakDetectionResults should have hasLeaks method")
      void shouldHaveHasLeaksMethod() throws NoSuchMethodException {
        Class<?> resultsClass = getLeakDetectionResultsClass();
        assertThat(resultsClass).isNotNull();

        final Method method = resultsClass.getMethod("hasLeaks");
        assertThat(method.getReturnType()).isEqualTo(boolean.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }
  }

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
  @DisplayName("WasiContextIsolationValidator Tests")
  class WasiContextIsolationValidatorTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("should be a final class")
      void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(WasiContextIsolationValidator.class.getModifiers()))
            .as("WasiContextIsolationValidator should be final")
            .isTrue();
      }

      @Test
      @DisplayName("should be public")
      void shouldBePublic() {
        assertThat(Modifier.isPublic(WasiContextIsolationValidator.class.getModifiers()))
            .as("WasiContextIsolationValidator should be public")
            .isTrue();
      }

      @Test
      @DisplayName("should have private LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        final Field field = WasiContextIsolationValidator.class.getDeclaredField("LOGGER");
        assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("should have default constructor")
      void shouldHaveDefaultConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor = WasiContextIsolationValidator.class.getConstructor();
        assertThat(constructor).isNotNull();
        assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have constructor with strictIsolationMode parameter")
      void shouldHaveStrictIsolationModeConstructor() throws NoSuchMethodException {
        final Constructor<?> constructor =
            WasiContextIsolationValidator.class.getConstructor(boolean.class);
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
        Class<?> isolationLevelClass = null;
        for (Class<?> declaredClass : WasiContextIsolationValidator.class.getDeclaredClasses()) {
          if (declaredClass.getSimpleName().equals("IsolationLevel")) {
            isolationLevelClass = declaredClass;
            break;
          }
        }
        assertThat(isolationLevelClass).isNotNull();

        final Method method =
            WasiContextIsolationValidator.class.getMethod(
                "registerContext", String.class, WasiContext.class, isolationLevelClass);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have unregisterContext method")
      void shouldHaveUnregisterContextMethod() throws NoSuchMethodException {
        final Method method =
            WasiContextIsolationValidator.class.getMethod("unregisterContext", String.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Validation Method Tests")
    class ValidationMethodTests {

      @Test
      @DisplayName("should have validatePathAccess method")
      void shouldHaveValidatePathAccessMethod() throws NoSuchMethodException {
        final Method method =
            WasiContextIsolationValidator.class.getMethod(
                "validatePathAccess", String.class, Path.class, WasiFileOperation.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have validateResourceAccess method")
      void shouldHaveValidateResourceAccessMethod() throws NoSuchMethodException {
        final Method method =
            WasiContextIsolationValidator.class.getMethod(
                "validateResourceAccess", String.class, String.class, String.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have validateMemoryAccess method")
      void shouldHaveValidateMemoryAccessMethod() throws NoSuchMethodException {
        final Method method =
            WasiContextIsolationValidator.class.getMethod(
                "validateMemoryAccess", String.class, long.class, long.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have validateCrossContextCommunication method")
      void shouldHaveValidateCrossContextCommunicationMethod() throws NoSuchMethodException {
        final Method method =
            WasiContextIsolationValidator.class.getMethod(
                "validateCrossContextCommunication", String.class, String.class, String.class);
        assertThat(method.getReturnType()).isEqualTo(void.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Statistics and State Method Tests")
    class StatisticsAndStateMethodTests {

      @Test
      @DisplayName("should have getStatistics method")
      void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
        final Method method = WasiContextIsolationValidator.class.getMethod("getStatistics");
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have getActiveContextCount method")
      void shouldHaveGetActiveContextCountMethod() throws NoSuchMethodException {
        final Method method =
            WasiContextIsolationValidator.class.getMethod("getActiveContextCount");
        assertThat(method.getReturnType()).isEqualTo(int.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("should have isStrictIsolationMode method")
      void shouldHaveIsStrictIsolationModeMethod() throws NoSuchMethodException {
        final Method method =
            WasiContextIsolationValidator.class.getMethod("isStrictIsolationMode");
        assertThat(method.getReturnType()).isEqualTo(boolean.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }

    @Nested
    @DisplayName("Inner Type Tests")
    class InnerTypeTests {

      @Test
      @DisplayName("should have IsolationLevel enum")
      void shouldHaveIsolationLevelEnum() {
        Class<?>[] declaredClasses = WasiContextIsolationValidator.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("IsolationLevel")) {
            found = true;
            assertThat(declaredClass.isEnum()).isTrue();
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("IsolationLevel enum should exist").isTrue();
      }

      @Test
      @DisplayName("IsolationLevel should have expected values")
      void isolationLevelShouldHaveExpectedValues() {
        Class<?> isolationLevelClass = null;
        for (Class<?> declaredClass : WasiContextIsolationValidator.class.getDeclaredClasses()) {
          if (declaredClass.getSimpleName().equals("IsolationLevel")) {
            isolationLevelClass = declaredClass;
            break;
          }
        }
        assertThat(isolationLevelClass).isNotNull();

        Object[] enumConstants = isolationLevelClass.getEnumConstants();
        assertThat(enumConstants).isNotNull();
        assertThat(enumConstants.length).isEqualTo(3);

        boolean hasPermissive = false;
        boolean hasStandard = false;
        boolean hasStrict = false;

        for (Object constant : enumConstants) {
          String name = constant.toString();
          if ("PERMISSIVE".equals(name)) {
            hasPermissive = true;
          }
          if ("STANDARD".equals(name)) {
            hasStandard = true;
          }
          if ("STRICT".equals(name)) {
            hasStrict = true;
          }
        }

        assertThat(hasPermissive).as("Should have PERMISSIVE level").isTrue();
        assertThat(hasStandard).as("Should have STANDARD level").isTrue();
        assertThat(hasStrict).as("Should have STRICT level").isTrue();
      }

      @Test
      @DisplayName("should have IsolationStatistics inner class")
      void shouldHaveIsolationStatisticsInnerClass() {
        Class<?>[] declaredClasses = WasiContextIsolationValidator.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> declaredClass : declaredClasses) {
          if (declaredClass.getSimpleName().equals("IsolationStatistics")) {
            found = true;
            assertThat(Modifier.isPublic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isStatic(declaredClass.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(declaredClass.getModifiers())).isTrue();
            break;
          }
        }
        assertThat(found).as("IsolationStatistics inner class should exist").isTrue();
      }
    }

    @Nested
    @DisplayName("IsolationStatistics Method Tests")
    class IsolationStatisticsMethodTests {

      private Class<?> getIsolationStatisticsClass() {
        for (Class<?> declaredClass : WasiContextIsolationValidator.class.getDeclaredClasses()) {
          if (declaredClass.getSimpleName().equals("IsolationStatistics")) {
            return declaredClass;
          }
        }
        return null;
      }

      @Test
      @DisplayName("IsolationStatistics should have getContextsRegistered method")
      void shouldHaveGetContextsRegisteredMethod() throws NoSuchMethodException {
        Class<?> statsClass = getIsolationStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getContextsRegistered");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("IsolationStatistics should have getPathIsolationViolations method")
      void shouldHaveGetPathIsolationViolationsMethod() throws NoSuchMethodException {
        Class<?> statsClass = getIsolationStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getPathIsolationViolations");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("IsolationStatistics should have getResourceIsolationViolations method")
      void shouldHaveGetResourceIsolationViolationsMethod() throws NoSuchMethodException {
        Class<?> statsClass = getIsolationStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getResourceIsolationViolations");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("IsolationStatistics should have getMemoryIsolationViolations method")
      void shouldHaveGetMemoryIsolationViolationsMethod() throws NoSuchMethodException {
        Class<?> statsClass = getIsolationStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getMemoryIsolationViolations");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }

      @Test
      @DisplayName("IsolationStatistics should have getTotalViolations method")
      void shouldHaveGetTotalViolationsMethod() throws NoSuchMethodException {
        Class<?> statsClass = getIsolationStatisticsClass();
        assertThat(statsClass).isNotNull();

        final Method method = statsClass.getMethod("getTotalViolations");
        assertThat(method.getReturnType()).isEqualTo(long.class);
        assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiResourceLeakDetector should have expected number of public methods")
    void leakDetectorShouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiResourceLeakDetector.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected: trackWasiContext, trackFileHandle, trackMemorySegment,
      // untrackWasiContext, untrackFileHandle, untrackMemorySegment,
      // performLeakDetection, getStatistics,
      // getTrackedContextCount, getTrackedFileHandleCount, getTrackedMemorySegmentCount, close
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(12);
    }

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

    @Test
    @DisplayName("WasiContextIsolationValidator should have expected number of public methods")
    void isolationValidatorShouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiContextIsolationValidator.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected: registerContext, unregisterContext, validatePathAccess, validateResourceAccess,
      // validateMemoryAccess, validateCrossContextCommunication, getStatistics,
      // getActiveContextCount, isStrictIsolationMode
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(9);
    }
  }
}
