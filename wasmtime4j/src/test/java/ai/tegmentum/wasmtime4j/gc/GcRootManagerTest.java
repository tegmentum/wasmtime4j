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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcRootManager} class.
 *
 * <p>GcRootManager manages root references to prevent premature collection of WebAssembly GC
 * objects and coordinates finalization with the Java garbage collector.
 */
@DisplayName("GcRootManager Tests")
class GcRootManagerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(GcRootManager.class.getModifiers()), "GcRootManager should be final");
    }
  }

  @Nested
  @DisplayName("Singleton Pattern Tests")
  class SingletonPatternTests {

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("getInstance");
      assertEquals(GcRootManager.class, method.getReturnType(), "Should return GcRootManager");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("getInstance should return non-null instance")
    void getInstanceShouldReturnNonNullInstance() {
      final GcRootManager instance = GcRootManager.getInstance();
      assertNotNull(instance, "Should return non-null instance");
    }

    @Test
    @DisplayName("getInstance should return same instance")
    void getInstanceShouldReturnSameInstance() {
      final GcRootManager instance1 = GcRootManager.getInstance();
      final GcRootManager instance2 = GcRootManager.getInstance();
      assertTrue(instance1 == instance2, "Should return same instance");
    }
  }

  @Nested
  @DisplayName("Root Management Method Tests")
  class RootManagementMethodTests {

    @Test
    @DisplayName("should have addRoot method")
    void shouldHaveAddRootMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("addRoot", GcObject.class);
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeRoot method")
    void shouldHaveRemoveRootMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("removeRoot", GcObject.class);
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isRoot method")
    void shouldHaveIsRootMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("isRoot", GcObject.class);
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRootCount method")
    void shouldHaveGetRootCountMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("getRootCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Weak Reference Method Tests")
  class WeakReferenceMethodTests {

    @Test
    @DisplayName("should have createWeakReference method")
    void shouldHaveCreateWeakReferenceMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("createWeakReference", GcObject.class);
      assertEquals(WeakReference.class, method.getReturnType(), "Should return WeakReference");
    }
  }

  @Nested
  @DisplayName("Cleanup Method Tests")
  class CleanupMethodTests {

    @Test
    @DisplayName("should have cleanup method")
    void shouldHaveCleanupMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("cleanup");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("shutdown");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = GcRootManager.class.getMethod("getStats");
      assertEquals(
          GcRootManager.RootManagerStats.class,
          method.getReturnType(),
          "Should return RootManagerStats");
    }
  }

  @Nested
  @DisplayName("RootManagerStats Tests")
  class RootManagerStatsTests {

    @Test
    @DisplayName("should be a nested final class")
    void shouldBeNestedFinalClass() {
      assertTrue(
          Modifier.isFinal(GcRootManager.RootManagerStats.class.getModifiers()),
          "RootManagerStats should be final");
      assertTrue(
          Modifier.isStatic(GcRootManager.RootManagerStats.class.getModifiers()),
          "RootManagerStats should be static");
    }

    @Test
    @DisplayName("should have all statistics getters")
    void shouldHaveAllStatisticsGetters() {
      final String[] expectedMethods = {
        "getRootsAdded",
        "getRootsRemoved",
        "getObjectsFinalized",
        "getCurrentRootCount",
        "getCurrentWeakRefCount",
        "getCurrentPhantomRefCount",
        "toString"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(GcRootManager.RootManagerStats.class, methodName),
            "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("getRootsAdded should return long")
    void getRootsAddedShouldReturnLong() throws NoSuchMethodException {
      final Method method = GcRootManager.RootManagerStats.class.getMethod("getRootsAdded");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("getCurrentRootCount should return int")
    void getCurrentRootCountShouldReturnInt() throws NoSuchMethodException {
      final Method method = GcRootManager.RootManagerStats.class.getMethod("getCurrentRootCount");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Parameter Validation Tests")
  class ParameterValidationTests {

    @Test
    @DisplayName("addRoot should throw on null")
    void addRootShouldThrowOnNull() {
      final GcRootManager manager = GcRootManager.getInstance();
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.addRoot(null),
          "Should throw IllegalArgumentException for null");
    }

    @Test
    @DisplayName("createWeakReference should throw on null")
    void createWeakReferenceShouldThrowOnNull() {
      final GcRootManager manager = GcRootManager.getInstance();
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.createWeakReference(null),
          "Should throw IllegalArgumentException for null");
    }

    @Test
    @DisplayName("removeRoot should handle null gracefully")
    void removeRootShouldHandleNullGracefully() {
      final GcRootManager manager = GcRootManager.getInstance();
      final boolean result = manager.removeRoot(null);
      assertFalse(result, "Should return false for null");
    }

    @Test
    @DisplayName("isRoot should handle null gracefully")
    void isRootShouldHandleNullGracefully() {
      final GcRootManager manager = GcRootManager.getInstance();
      final boolean result = manager.isRoot(null);
      assertFalse(result, "Should return false for null");
    }
  }

  @Nested
  @DisplayName("Statistics Retrieval Tests")
  class StatisticsRetrievalTests {

    @Test
    @DisplayName("getStats should return non-null stats")
    void getStatsShouldReturnNonNullStats() {
      final GcRootManager manager = GcRootManager.getInstance();
      final GcRootManager.RootManagerStats stats = manager.getStats();
      assertNotNull(stats, "Should return non-null stats");
    }

    @Test
    @DisplayName("stats toString should return non-null")
    void statsToStringShouldReturnNonNull() {
      final GcRootManager manager = GcRootManager.getInstance();
      final GcRootManager.RootManagerStats stats = manager.getStats();
      final String str = stats.toString();
      assertNotNull(str, "toString should return non-null");
      assertTrue(str.contains("RootManagerStats"), "Should contain class name");
    }
  }

  @Nested
  @DisplayName("Root Count Tests")
  class RootCountTests {

    @Test
    @DisplayName("getRootCount should return non-negative value")
    void getRootCountShouldReturnNonNegativeValue() {
      final GcRootManager manager = GcRootManager.getInstance();
      final int count = manager.getRootCount();
      assertTrue(count >= 0, "Should return non-negative value");
    }
  }

  @Nested
  @DisplayName("Cleanup Tests")
  class CleanupTests {

    @Test
    @DisplayName("cleanup should return non-negative count")
    void cleanupShouldReturnNonNegativeCount() {
      final GcRootManager manager = GcRootManager.getInstance();
      final int cleaned = manager.cleanup();
      assertTrue(cleaned >= 0, "Should return non-negative count");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support root registration pattern")
    void shouldSupportRootRegistrationPattern() {
      // Documents usage:
      // GcRootManager manager = GcRootManager.getInstance();
      // manager.addRoot(gcObject);
      // // ... use object ...
      // manager.removeRoot(gcObject);
      assertTrue(hasMethod(GcRootManager.class, "getInstance"), "Need getInstance method");
      assertTrue(hasMethod(GcRootManager.class, "addRoot"), "Need addRoot method");
      assertTrue(hasMethod(GcRootManager.class, "removeRoot"), "Need removeRoot method");
    }

    @Test
    @DisplayName("should support root checking pattern")
    void shouldSupportRootCheckingPattern() {
      // Documents usage:
      // if (manager.isRoot(gcObject)) {
      //   // Object is rooted
      // }
      assertTrue(hasMethod(GcRootManager.class, "isRoot"), "Need isRoot method");
    }

    @Test
    @DisplayName("should support weak reference pattern")
    void shouldSupportWeakReferencePattern() {
      // Documents usage:
      // WeakReference<GcObject> weakRef = manager.createWeakReference(gcObject);
      // GcObject obj = weakRef.get();
      assertTrue(
          hasMethod(GcRootManager.class, "createWeakReference"), "Need createWeakReference method");
    }

    @Test
    @DisplayName("should support statistics monitoring pattern")
    void shouldSupportStatisticsMonitoringPattern() {
      // Documents usage:
      // RootManagerStats stats = manager.getStats();
      // long added = stats.getRootsAdded();
      // long removed = stats.getRootsRemoved();
      assertTrue(hasMethod(GcRootManager.class, "getStats"), "Need getStats method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Thread Safety Documentation Tests")
  class ThreadSafetyDocumentationTests {

    @Test
    @DisplayName("should document thread-safe operations")
    void shouldDocumentThreadSafeOperations() {
      // GcRootManager uses ConcurrentHashMap.newKeySet() for roots
      // and AtomicLong for counters, making it thread-safe
      final GcRootManager manager = GcRootManager.getInstance();
      assertNotNull(manager, "Manager should be accessible from any thread");
    }
  }
}
