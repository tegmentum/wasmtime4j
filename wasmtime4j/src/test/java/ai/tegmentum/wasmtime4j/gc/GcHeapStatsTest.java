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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcHeapStats} class.
 *
 * <p>GcHeapStats provides simple heap statistics from the WebAssembly GC runtime.
 */
@DisplayName("GcHeapStats Tests")
class GcHeapStatsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(GcHeapStats.class.getModifiers()), "GcHeapStats should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(GcHeapStats.class.getModifiers()), "GcHeapStats should be public");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("default constructor should initialize fields to zero")
    void defaultConstructorShouldInitializeFieldsToZero() {
      final GcHeapStats stats = new GcHeapStats();
      assertEquals(0L, stats.getTotalAllocated(), "totalAllocated should be 0");
      assertEquals(0L, stats.getCurrentHeapSize(), "currentHeapSize should be 0");
      assertEquals(0L, stats.getMajorCollections(), "majorCollections should be 0");
    }

    @Test
    @DisplayName("default constructor should create non-null instance")
    void defaultConstructorShouldCreateNonNullInstance() {
      final GcHeapStats stats = new GcHeapStats();
      assertNotNull(stats, "Constructor should create non-null instance");
    }
  }

  @Nested
  @DisplayName("Field Access Tests")
  class FieldAccessTests {

    @Test
    @DisplayName("totalAllocated field should be publicly accessible")
    void totalAllocatedFieldShouldBePubliclyAccessible() {
      final GcHeapStats stats = new GcHeapStats();
      stats.totalAllocated = 12345L;
      assertEquals(12345L, stats.totalAllocated, "totalAllocated field should be writable");
      assertEquals(
          12345L, stats.getTotalAllocated(), "getTotalAllocated should return field value");
    }

    @Test
    @DisplayName("currentHeapSize field should be publicly accessible")
    void currentHeapSizeFieldShouldBePubliclyAccessible() {
      final GcHeapStats stats = new GcHeapStats();
      stats.currentHeapSize = 67890L;
      assertEquals(67890L, stats.currentHeapSize, "currentHeapSize field should be writable");
      assertEquals(
          67890L, stats.getCurrentHeapSize(), "getCurrentHeapSize should return field value");
    }

    @Test
    @DisplayName("majorCollections field should be publicly accessible")
    void majorCollectionsFieldShouldBePubliclyAccessible() {
      final GcHeapStats stats = new GcHeapStats();
      stats.majorCollections = 42L;
      assertEquals(42L, stats.majorCollections, "majorCollections field should be writable");
      assertEquals(
          42L, stats.getMajorCollections(), "getMajorCollections should return field value");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getTotalAllocated should return correct value")
    void getTotalAllocatedShouldReturnCorrectValue() {
      final GcHeapStats stats = new GcHeapStats();
      stats.totalAllocated = 1000000L;
      assertEquals(1000000L, stats.getTotalAllocated());
    }

    @Test
    @DisplayName("getCurrentHeapSize should return correct value")
    void getCurrentHeapSizeShouldReturnCorrectValue() {
      final GcHeapStats stats = new GcHeapStats();
      stats.currentHeapSize = 2097152L;
      assertEquals(2097152L, stats.getCurrentHeapSize());
    }

    @Test
    @DisplayName("getMajorCollections should return correct value")
    void getMajorCollectionsShouldReturnCorrectValue() {
      final GcHeapStats stats = new GcHeapStats();
      stats.majorCollections = 15L;
      assertEquals(15L, stats.getMajorCollections());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should not be null")
    void toStringShouldNotBeNull() {
      final GcHeapStats stats = new GcHeapStats();
      assertNotNull(stats.toString());
    }

    @Test
    @DisplayName("toString should contain class name")
    void toStringShouldContainClassName() {
      final GcHeapStats stats = new GcHeapStats();
      assertTrue(stats.toString().contains("GcHeapStats"));
    }

    @Test
    @DisplayName("toString should contain field values")
    void toStringShouldContainFieldValues() {
      final GcHeapStats stats = new GcHeapStats();
      stats.totalAllocated = 100L;
      stats.currentHeapSize = 200L;
      stats.majorCollections = 5L;

      final String str = stats.toString();
      assertTrue(str.contains("totalAllocated=100"));
      assertTrue(str.contains("currentHeapSize=200"));
      assertTrue(str.contains("majorCollections=5"));
    }
  }
}
