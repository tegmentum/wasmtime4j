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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentGarbageCollectionResult} interface.
 *
 * <p>ComponentGarbageCollectionResult represents the result of a garbage collection operation.
 */
@DisplayName("ComponentGarbageCollectionResult Tests")
class ComponentGarbageCollectionResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentGarbageCollectionResult.class.getModifiers()),
          "ComponentGarbageCollectionResult should be public");
      assertTrue(
          ComponentGarbageCollectionResult.class.isInterface(),
          "ComponentGarbageCollectionResult should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionResult.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getMemoryReclaimed method")
    void shouldHaveGetMemoryReclaimedMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionResult.class.getMethod("getMemoryReclaimed");
      assertNotNull(method, "getMemoryReclaimed method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionResult.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getObjectsCollected method")
    void shouldHaveGetObjectsCollectedMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionResult.class.getMethod("getObjectsCollected");
      assertNotNull(method, "getObjectsCollected method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface contract. */
    private static class StubComponentGarbageCollectionResult
        implements ComponentGarbageCollectionResult {
      private final String status;
      private final long memoryReclaimed;
      private final long duration;
      private final long objectsCollected;

      StubComponentGarbageCollectionResult(
          final String status,
          final long memoryReclaimed,
          final long duration,
          final long objectsCollected) {
        this.status = status;
        this.memoryReclaimed = memoryReclaimed;
        this.duration = duration;
        this.objectsCollected = objectsCollected;
      }

      @Override
      public String getStatus() {
        return status;
      }

      @Override
      public long getMemoryReclaimed() {
        return memoryReclaimed;
      }

      @Override
      public long getDuration() {
        return duration;
      }

      @Override
      public long getObjectsCollected() {
        return objectsCollected;
      }
    }

    @Test
    @DisplayName("stub should return correct values")
    void stubShouldReturnCorrectValues() {
      final ComponentGarbageCollectionResult result =
          new StubComponentGarbageCollectionResult("SUCCESS", 1024 * 1024L, 50L, 10L);

      assertEquals("SUCCESS", result.getStatus(), "Status should be SUCCESS");
      assertEquals(1024 * 1024L, result.getMemoryReclaimed(), "Should have 1MB reclaimed");
      assertEquals(50L, result.getDuration(), "Duration should be 50ms");
      assertEquals(10L, result.getObjectsCollected(), "Should have 10 objects collected");
    }

    @Test
    @DisplayName("stub should handle zero values")
    void stubShouldHandleZeroValues() {
      final ComponentGarbageCollectionResult result =
          new StubComponentGarbageCollectionResult("SUCCESS", 0L, 0L, 0L);

      assertEquals("SUCCESS", result.getStatus(), "Status should be SUCCESS");
      assertEquals(0L, result.getMemoryReclaimed(), "Memory reclaimed should be 0");
      assertEquals(0L, result.getDuration(), "Duration should be 0");
      assertEquals(0L, result.getObjectsCollected(), "Objects collected should be 0");
    }

    @Test
    @DisplayName("stub should handle max values")
    void stubShouldHandleMaxValues() {
      final ComponentGarbageCollectionResult result =
          new StubComponentGarbageCollectionResult(
              "SUCCESS", Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

      assertEquals("SUCCESS", result.getStatus(), "Status should be SUCCESS");
      assertEquals(Long.MAX_VALUE, result.getMemoryReclaimed(), "Should handle max long");
      assertEquals(Long.MAX_VALUE, result.getDuration(), "Should handle max long");
      assertEquals(Long.MAX_VALUE, result.getObjectsCollected(), "Should handle max long");
    }

    @Test
    @DisplayName("stub should handle large byte values")
    void stubShouldHandleLargeByteValues() {
      final long oneTerabyte = 1024L * 1024L * 1024L * 1024L;
      final ComponentGarbageCollectionResult result =
          new StubComponentGarbageCollectionResult("PARTIAL", oneTerabyte, 10000L, 1000L);

      assertEquals("PARTIAL", result.getStatus(), "Status should be PARTIAL");
      assertEquals(oneTerabyte, result.getMemoryReclaimed(), "Should have 1TB reclaimed");
      assertEquals(10000L, result.getDuration(), "Duration should be 10 seconds");
      assertEquals(1000L, result.getObjectsCollected(), "Should have 1000 objects");
    }

    @Test
    @DisplayName("stub should handle failed status")
    void stubShouldHandleFailedStatus() {
      final ComponentGarbageCollectionResult result =
          new StubComponentGarbageCollectionResult("FAILED", 0L, 100L, 0L);

      assertEquals("FAILED", result.getStatus(), "Status should be FAILED");
      assertEquals(0L, result.getMemoryReclaimed(), "Memory reclaimed should be 0 on failure");
      assertEquals(100L, result.getDuration(), "Duration should reflect time spent");
      assertEquals(0L, result.getObjectsCollected(), "No objects collected on failure");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactlyFourDeclaredMethods() {
      final var methods = ComponentGarbageCollectionResult.class.getDeclaredMethods();
      assertEquals(4, methods.length, "Should have exactly 4 declared methods");
    }

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() {
      final var methods = ComponentGarbageCollectionResult.class.getMethods();
      final var methodNames =
          java.util.Arrays.stream(methods)
              .map(Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getStatus"), "Should have getStatus");
      assertTrue(methodNames.contains("getMemoryReclaimed"), "Should have getMemoryReclaimed");
      assertTrue(methodNames.contains("getDuration"), "Should have getDuration");
      assertTrue(methodNames.contains("getObjectsCollected"), "Should have getObjectsCollected");
    }
  }
}
