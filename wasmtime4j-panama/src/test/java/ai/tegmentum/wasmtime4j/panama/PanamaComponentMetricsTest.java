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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentMetrics;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaComponentMetrics} class.
 *
 * <p>PanamaComponentMetrics provides metrics for WebAssembly component execution.
 */
@DisplayName("PanamaComponentMetrics Tests")
class PanamaComponentMetricsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      // Not public - package-private
      assertTrue(
          !Modifier.isPublic(PanamaComponentMetrics.class.getModifiers()),
          "PanamaComponentMetrics should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaComponentMetrics.class.getModifiers()),
          "PanamaComponentMetrics should be final");
    }

    @Test
    @DisplayName("should implement ComponentMetrics interface")
    void shouldImplementComponentMetricsInterface() {
      assertTrue(
          ComponentMetrics.class.isAssignableFrom(PanamaComponentMetrics.class),
          "PanamaComponentMetrics should implement ComponentMetrics");
    }
  }

  @Nested
  @DisplayName("ComponentMetrics Method Tests")
  class ComponentMetricsMethodTests {

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getExecutionMetrics method")
    void shouldHaveGetExecutionMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getExecutionMetrics");
      assertNotNull(method, "getExecutionMetrics method should exist");
      assertEquals(
          ComponentMetrics.ExecutionMetrics.class,
          method.getReturnType(),
          "Should return ExecutionMetrics");
    }

    @Test
    @DisplayName("should have getMemoryMetrics method")
    void shouldHaveGetMemoryMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getMemoryMetrics");
      assertNotNull(method, "getMemoryMetrics method should exist");
      assertEquals(
          ComponentMetrics.MemoryMetrics.class,
          method.getReturnType(),
          "Should return MemoryMetrics");
    }

    @Test
    @DisplayName("should have getPerformanceMetrics method")
    void shouldHaveGetPerformanceMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getPerformanceMetrics");
      assertNotNull(method, "getPerformanceMetrics method should exist");
      assertEquals(
          ComponentMetrics.PerformanceMetrics.class,
          method.getReturnType(),
          "Should return PerformanceMetrics");
    }

    @Test
    @DisplayName("should have getResourceMetrics method")
    void shouldHaveGetResourceMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getResourceMetrics");
      assertNotNull(method, "getResourceMetrics method should exist");
      assertEquals(
          ComponentMetrics.ResourceMetrics.class,
          method.getReturnType(),
          "Should return ResourceMetrics");
    }

    @Test
    @DisplayName("should have getErrorMetrics method")
    void shouldHaveGetErrorMetricsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getErrorMetrics");
      assertNotNull(method, "getErrorMetrics method should exist");
      assertEquals(
          ComponentMetrics.ErrorMetrics.class,
          method.getReturnType(),
          "Should return ErrorMetrics");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getEndTime method")
    void shouldHaveGetEndTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("getEndTime");
      assertNotNull(method, "getEndTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have reset method")
    void shouldHaveResetMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("reset");
      assertNotNull(method, "reset method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have snapshot method")
    void shouldHaveSnapshotMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentMetrics.class.getMethod("snapshot");
      assertNotNull(method, "snapshot method should exist");
      assertEquals(
          ComponentMetrics.MetricsSnapshot.class,
          method.getReturnType(),
          "Should return MetricsSnapshot");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with String and MemorySegment")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaComponentMetrics.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 2
            && constructor.getParameterTypes()[0] == String.class
            && constructor.getParameterTypes()[1] == MemorySegment.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(
          hasExpectedConstructor,
          "Should have constructor with String and MemorySegment parameters");
    }
  }
}
