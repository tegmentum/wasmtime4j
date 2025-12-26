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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WeakGcReference} interface.
 *
 * <p>WeakGcReference represents a weak reference to a WebAssembly GC object that does not prevent
 * garbage collection.
 */
@DisplayName("WeakGcReference Tests")
class WeakGcReferenceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WeakGcReference.class.isInterface(), "WeakGcReference should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = WeakGcReference.class.getMethod("get");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have isCleared method")
    void shouldHaveIsClearedMethod() throws NoSuchMethodException {
      final Method method = WeakGcReference.class.getMethod("isCleared");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = WeakGcReference.class.getMethod("clear");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getFinalizationCallback method")
    void shouldHaveGetFinalizationCallbackMethod() throws NoSuchMethodException {
      final Method method = WeakGcReference.class.getMethod("getFinalizationCallback");
      assertEquals(Runnable.class, method.getReturnType(), "Should return Runnable");
    }

    @Test
    @DisplayName("should have setFinalizationCallback method")
    void shouldHaveSetFinalizationCallbackMethod() throws NoSuchMethodException {
      final Method method =
          WeakGcReference.class.getMethod("setFinalizationCallback", Runnable.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("get should take no parameters")
    void getShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WeakGcReference.class.getMethod("get");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("isCleared should take no parameters")
    void isClearedShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WeakGcReference.class.getMethod("isCleared");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("clear should take no parameters")
    void clearShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WeakGcReference.class.getMethod("clear");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("setFinalizationCallback should take Runnable parameter")
    void setFinalizationCallbackShouldTakeRunnableParameter() throws NoSuchMethodException {
      final Method method =
          WeakGcReference.class.getMethod("setFinalizationCallback", Runnable.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
      assertEquals(Runnable.class, method.getParameterTypes()[0], "Parameter should be Runnable");
    }
  }

  @Nested
  @DisplayName("Weak Reference Semantics Tests")
  class WeakReferenceSemanticsTests {

    @Test
    @DisplayName("should have all weak reference methods")
    void shouldHaveAllWeakReferenceMethods() {
      final String[] expectedMethods = {
        "get", "isCleared", "clear", "getFinalizationCallback", "setFinalizationCallback"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(WeakGcReference.class, methodName), "Should have method: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support checking if reference is cleared")
    void shouldSupportCheckingIfReferenceIsCleared() {
      // Documents usage: if (weakRef.isCleared()) { ... }
      assertTrue(hasMethod(WeakGcReference.class, "isCleared"), "Need isCleared method");
    }

    @Test
    @DisplayName("should support getting referenced object")
    void shouldSupportGettingReferencedObject() {
      // Documents usage: Optional<GcObject> obj = weakRef.get();
      assertTrue(hasMethod(WeakGcReference.class, "get"), "Need get method");
    }

    @Test
    @DisplayName("should support manually clearing reference")
    void shouldSupportManuallyClearingReference() {
      // Documents usage: weakRef.clear();
      assertTrue(hasMethod(WeakGcReference.class, "clear"), "Need clear method");
    }

    @Test
    @DisplayName("should support finalization callbacks")
    void shouldSupportFinalizationCallbacks() {
      // Documents usage: weakRef.setFinalizationCallback(() -> cleanup());
      assertTrue(
          hasMethod(WeakGcReference.class, "setFinalizationCallback"),
          "Need setFinalizationCallback method");
      assertTrue(
          hasMethod(WeakGcReference.class, "getFinalizationCallback"),
          "Need getFinalizationCallback method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Cache Implementation Pattern Tests")
  class CacheImplementationPatternTests {

    @Test
    @DisplayName("should support cache entry pattern")
    void shouldSupportCacheEntryPattern() {
      // WeakGcReference is ideal for cache implementations
      // Pattern: Cache<Key, WeakGcReference> where entries can be collected
      assertTrue(hasMethod(WeakGcReference.class, "get"), "Need get for cache lookup");
      assertTrue(hasMethod(WeakGcReference.class, "isCleared"), "Need isCleared for cache cleanup");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have correct method count")
    void shouldHaveCorrectMethodCount() {
      final long methodCount =
          Arrays.stream(WeakGcReference.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(5, methodCount, "Should have exactly 5 methods");
    }
  }
}
