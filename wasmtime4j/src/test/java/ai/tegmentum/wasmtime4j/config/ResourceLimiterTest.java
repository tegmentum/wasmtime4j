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

package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ResourceLimiter interface.
 *
 * <p>Validates that the interface defines the correct API surface for dynamic callback-based
 * resource limiting.
 */
@DisplayName("ResourceLimiter Interface Tests")
class ResourceLimiterTest {

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ResourceLimiter.class.isInterface(), "ResourceLimiter should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ResourceLimiter.class.getModifiers()),
          "ResourceLimiter should be public");
    }
  }

  @Nested
  @DisplayName("Method Definition Tests")
  class MethodDefinitionTests {

    @Test
    @DisplayName("should have memoryGrowing method with correct signature")
    void shouldHaveMemoryGrowingMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiter.class.getMethod("memoryGrowing", long.class, long.class, long.class);
      assertNotNull(method, "memoryGrowing method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertNotNull(method, "Should have 3 long parameters");
    }

    @Test
    @DisplayName("should have tableGrowing method with correct signature")
    void shouldHaveTableGrowingMethod() throws NoSuchMethodException {
      Method method =
          ResourceLimiter.class.getMethod("tableGrowing", int.class, int.class, int.class);
      assertNotNull(method, "tableGrowing method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have memoryGrowFailed default method")
    void shouldHaveMemoryGrowFailedDefaultMethod() throws NoSuchMethodException {
      Method method = ResourceLimiter.class.getMethod("memoryGrowFailed", String.class);
      assertNotNull(method, "memoryGrowFailed method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(method.isDefault(), "Should be a default method");
    }

    @Test
    @DisplayName("should have tableGrowFailed default method")
    void shouldHaveTableGrowFailedDefaultMethod() throws NoSuchMethodException {
      Method method = ResourceLimiter.class.getMethod("tableGrowFailed", String.class);
      assertNotNull(method, "tableGrowFailed method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(method.isDefault(), "Should be a default method");
    }

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactly4DeclaredMethods() {
      long methodCount =
          Arrays.stream(ResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(4, methodCount, "Should have exactly 4 declared methods");
    }

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "memoryGrowing", "tableGrowing", "memoryGrowFailed", "tableGrowFailed"));

      Set<String> actualMethods =
          Arrays.stream(ResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 2 abstract methods")
    void shouldHaveExactly2AbstractMethods() {
      long abstractCount =
          Arrays.stream(ResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .count();
      assertEquals(2, abstractCount, "Should have exactly 2 abstract methods");
    }

    @Test
    @DisplayName("should have exactly 2 default methods")
    void shouldHaveExactly2DefaultMethods() {
      long defaultCount =
          Arrays.stream(ResourceLimiter.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(2, defaultCount, "Should have exactly 2 default methods");
    }
  }

  @Nested
  @DisplayName("Default Method Behavior Tests")
  class DefaultMethodBehaviorTests {

    @Test
    @DisplayName("memoryGrowFailed default should not throw")
    void memoryGrowFailedDefaultShouldNotThrow() {
      ResourceLimiter limiter =
          new ResourceLimiter() {
            @Override
            public boolean memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return true;
            }

            @Override
            public boolean tableGrowing(
                final int currentElements,
                final int desiredElements,
                final int maximumElements) {
              return true;
            }
          };

      // Should not throw - default implementation is no-op
      limiter.memoryGrowFailed("test error");
      limiter.tableGrowFailed("test error");
    }
  }
}
