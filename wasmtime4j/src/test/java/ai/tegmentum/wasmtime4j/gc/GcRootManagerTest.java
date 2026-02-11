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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
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
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(GcRootManager.class.getModifiers()), "GcRootManager should be final");
    }
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("getInstance should return non-null")
    void getInstanceShouldReturnNonNull() {
      final GcRootManager manager = GcRootManager.getInstance();
      assertNotNull(manager, "getInstance should return non-null manager");
    }

    @Test
    @DisplayName("getInstance should return same instance")
    void getInstanceShouldReturnSameInstance() {
      final GcRootManager m1 = GcRootManager.getInstance();
      final GcRootManager m2 = GcRootManager.getInstance();
      assertSame(m1, m2, "getInstance should always return the same singleton");
    }
  }

  @Nested
  @DisplayName("AddRoot Tests")
  class AddRootTests {

    @Test
    @DisplayName("addRoot with null should throw IllegalArgumentException")
    void addRootWithNullShouldThrowIae() {
      final GcRootManager manager = GcRootManager.getInstance();
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.addRoot(null),
          "addRoot(null) should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("RemoveRoot Tests")
  class RemoveRootTests {

    @Test
    @DisplayName("removeRoot with null should return false")
    void removeRootWithNullShouldReturnFalse() {
      final GcRootManager manager = GcRootManager.getInstance();
      final boolean removed = manager.removeRoot(null);
      assertFalse(removed, "removeRoot(null) should return false");
    }
  }

  @Nested
  @DisplayName("IsRoot Tests")
  class IsRootTests {

    @Test
    @DisplayName("isRoot with null should return false")
    void isRootWithNullShouldReturnFalse() {
      final GcRootManager manager = GcRootManager.getInstance();
      assertFalse(manager.isRoot(null), "isRoot(null) should return false");
    }
  }

  @Nested
  @DisplayName("CreateWeakReference Tests")
  class CreateWeakReferenceTests {

    @Test
    @DisplayName("createWeakReference with null should throw IllegalArgumentException")
    void createWeakRefWithNullShouldThrowIae() {
      final GcRootManager manager = GcRootManager.getInstance();
      assertThrows(
          IllegalArgumentException.class,
          () -> manager.createWeakReference(null),
          "createWeakReference(null) should throw IllegalArgumentException");
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
      assertTrue(cleaned >= 0, "cleanup should return non-negative count");
    }
  }

  @Nested
  @DisplayName("GetRootCount Tests")
  class GetRootCountTests {

    @Test
    @DisplayName("getRootCount should return non-negative")
    void getRootCountShouldReturnNonNegative() {
      final GcRootManager manager = GcRootManager.getInstance();
      assertTrue(manager.getRootCount() >= 0, "getRootCount should return non-negative value");
    }
  }

  @Nested
  @DisplayName("Stats Tests")
  class StatsTests {

    @Test
    @DisplayName("getStats should return non-null stats")
    void getStatsShouldReturnNonNull() {
      final GcRootManager manager = GcRootManager.getInstance();
      final GcRootManager.RootManagerStats stats = manager.getStats();
      assertNotNull(stats, "getStats should return non-null stats");
    }

    @Test
    @DisplayName("stats rootsAdded should be non-negative")
    void statsRootsAddedShouldBeNonNegative() {
      final GcRootManager.RootManagerStats stats = GcRootManager.getInstance().getStats();
      assertTrue(stats.getRootsAdded() >= 0, "rootsAdded should be non-negative");
    }

    @Test
    @DisplayName("stats rootsRemoved should be non-negative")
    void statsRootsRemovedShouldBeNonNegative() {
      final GcRootManager.RootManagerStats stats = GcRootManager.getInstance().getStats();
      assertTrue(stats.getRootsRemoved() >= 0, "rootsRemoved should be non-negative");
    }

    @Test
    @DisplayName("stats objectsFinalized should be non-negative")
    void statsObjectsFinalizedShouldBeNonNegative() {
      final GcRootManager.RootManagerStats stats = GcRootManager.getInstance().getStats();
      assertTrue(stats.getObjectsFinalized() >= 0, "objectsFinalized should be non-negative");
    }

    @Test
    @DisplayName("stats toString should contain class name")
    void statsToStringShouldContainClassName() {
      final GcRootManager.RootManagerStats stats = GcRootManager.getInstance().getStats();
      final String str = stats.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("RootManagerStats"), "toString should contain 'RootManagerStats'");
    }
  }
}
