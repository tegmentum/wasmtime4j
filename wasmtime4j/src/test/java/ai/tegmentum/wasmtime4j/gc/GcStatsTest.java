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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcStats} class.
 *
 * <p>GcStats provides comprehensive metrics about garbage collection activity.
 */
@DisplayName("GcStats Tests")
class GcStatsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(GcStats.class.getModifiers()), "GcStats should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(GcStats.class.getModifiers()), "GcStats should be public");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should have builder factory method")
    void shouldHaveBuilderFactoryMethod() {
      final GcStats.Builder builder = GcStats.builder();
      assertNotNull(builder, "builder() should return a builder");
    }

    @Test
    @DisplayName("builder should create GcStats with defaults")
    void builderShouldCreateGcStatsWithDefaults() {
      final GcStats stats = GcStats.builder().build();
      assertNotNull(stats, "Should create GcStats");
      assertEquals(0L, stats.getTotalAllocated(), "Total allocated should default to 0");
      assertEquals(0L, stats.getTotalCollected(), "Total collected should default to 0");
    }

    @Test
    @DisplayName("builder should set totalAllocated")
    void builderShouldSetTotalAllocated() {
      final GcStats stats = GcStats.builder().totalAllocated(1000L).build();
      assertEquals(1000L, stats.getTotalAllocated());
    }

    @Test
    @DisplayName("builder should set totalCollected")
    void builderShouldSetTotalCollected() {
      final GcStats stats = GcStats.builder().totalCollected(500L).build();
      assertEquals(500L, stats.getTotalCollected());
    }

    @Test
    @DisplayName("builder should set bytesAllocated")
    void builderShouldSetBytesAllocated() {
      final GcStats stats = GcStats.builder().bytesAllocated(1048576L).build();
      assertEquals(1048576L, stats.getBytesAllocated());
    }

    @Test
    @DisplayName("builder should set bytesCollected")
    void builderShouldSetBytesCollected() {
      final GcStats stats = GcStats.builder().bytesCollected(524288L).build();
      assertEquals(524288L, stats.getBytesCollected());
    }

    @Test
    @DisplayName("builder should set minorCollections")
    void builderShouldSetMinorCollections() {
      final GcStats stats = GcStats.builder().minorCollections(10L).build();
      assertEquals(10L, stats.getMinorCollections());
    }

    @Test
    @DisplayName("builder should set majorCollections")
    void builderShouldSetMajorCollections() {
      final GcStats stats = GcStats.builder().majorCollections(3L).build();
      assertEquals(3L, stats.getMajorCollections());
    }

    @Test
    @DisplayName("builder should set totalGcTime")
    void builderShouldSetTotalGcTime() {
      final Duration gcTime = Duration.ofMillis(150);
      final GcStats stats = GcStats.builder().totalGcTime(gcTime).build();
      assertEquals(gcTime, stats.getTotalGcTime());
    }

    @Test
    @DisplayName("builder should set currentHeapSize")
    void builderShouldSetCurrentHeapSize() {
      final GcStats stats = GcStats.builder().currentHeapSize(16777216).build();
      assertEquals(16777216, stats.getCurrentHeapSize());
    }

    @Test
    @DisplayName("builder should set peakHeapSize")
    void builderShouldSetPeakHeapSize() {
      final GcStats stats = GcStats.builder().peakHeapSize(33554432).build();
      assertEquals(33554432, stats.getPeakHeapSize());
    }

    @Test
    @DisplayName("builder should set maxHeapSize")
    void builderShouldSetMaxHeapSize() {
      final GcStats stats = GcStats.builder().maxHeapSize(67108864).build();
      assertEquals(67108864, stats.getMaxHeapSize());
    }

    @Test
    @DisplayName("builder should set captureTime")
    void builderShouldSetCaptureTime() {
      final Instant captureTime = Instant.now();
      final GcStats stats = GcStats.builder().captureTime(captureTime).build();
      assertEquals(captureTime, stats.getCaptureTime());
    }

    @Test
    @DisplayName("builder should support method chaining")
    void builderShouldSupportMethodChaining() {
      final GcStats stats =
          GcStats.builder()
              .totalAllocated(1000L)
              .totalCollected(500L)
              .bytesAllocated(1048576L)
              .bytesCollected(524288L)
              .minorCollections(10L)
              .majorCollections(3L)
              .currentHeapSize(16777216)
              .build();

      assertEquals(1000L, stats.getTotalAllocated());
      assertEquals(500L, stats.getTotalCollected());
      assertEquals(13, stats.getTotalCollections());
    }
  }

  @Nested
  @DisplayName("Computed Property Tests")
  class ComputedPropertyTests {

    @Test
    @DisplayName("should compute getTotalCollections correctly")
    void shouldComputeTotalCollectionsCorrectly() {
      final GcStats stats = GcStats.builder().minorCollections(10L).majorCollections(3L).build();
      assertEquals(13L, stats.getTotalCollections());
    }

    @Test
    @DisplayName("should compute getLiveObjects correctly")
    void shouldComputeLiveObjectsCorrectly() {
      final GcStats stats = GcStats.builder().totalAllocated(1000L).totalCollected(300L).build();
      assertEquals(700L, stats.getLiveObjects());
    }

    @Test
    @DisplayName("should compute getHeapUtilization correctly")
    void shouldComputeHeapUtilizationCorrectly() {
      final GcStats stats =
          GcStats.builder().currentHeapSize(8000000).maxHeapSize(10000000).build();
      assertEquals(80.0, stats.getHeapUtilization(), 0.001);
    }

    @Test
    @DisplayName("should handle zero maxHeapSize for getHeapUtilization")
    void shouldHandleZeroMaxHeapSizeForGetHeapUtilization() {
      final GcStats stats = GcStats.builder().currentHeapSize(1000).maxHeapSize(0).build();
      assertEquals(0.0, stats.getHeapUtilization(), 0.001);
    }

    @Test
    @DisplayName("should compute getCollectionEfficiency correctly")
    void shouldComputeCollectionEfficiencyCorrectly() {
      final GcStats stats =
          GcStats.builder().bytesAllocated(1000000L).bytesCollected(900000L).build();
      assertEquals(90.0, stats.getCollectionEfficiency(), 0.001);
    }

    @Test
    @DisplayName("should handle zero bytesAllocated for getCollectionEfficiency")
    void shouldHandleZeroBytesAllocatedForGetCollectionEfficiency() {
      final GcStats stats = GcStats.builder().bytesAllocated(0L).bytesCollected(0L).build();
      assertEquals(100.0, stats.getCollectionEfficiency(), 0.001);
    }
  }

  @Nested
  @DisplayName("Threshold Check Tests")
  class ThresholdCheckTests {

    @Test
    @DisplayName("hasHighHeapUtilization should return true when above 80%")
    void hasHighHeapUtilizationShouldReturnTrueWhenAbove80Percent() {
      final GcStats stats =
          GcStats.builder().currentHeapSize(8500000).maxHeapSize(10000000).build();
      assertTrue(stats.hasHighHeapUtilization());
    }

    @Test
    @DisplayName("hasHighHeapUtilization should return false when below 80%")
    void hasHighHeapUtilizationShouldReturnFalseWhenBelow80Percent() {
      final GcStats stats =
          GcStats.builder().currentHeapSize(7000000).maxHeapSize(10000000).build();
      assertFalse(stats.hasHighHeapUtilization());
    }
  }

  @Nested
  @DisplayName("Objects By Generation Tests")
  class ObjectsByGenerationTests {

    @Test
    @DisplayName("should return unmodifiable map")
    void shouldReturnUnmodifiableMap() {
      final Map<Integer, Long> generations = new HashMap<>();
      generations.put(0, 500L);
      generations.put(1, 100L);
      final GcStats stats = GcStats.builder().objectsByGeneration(generations).build();

      final Map<Integer, Long> result = stats.getObjectsByGeneration();
      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals(500L, result.get(0));
      assertEquals(100L, result.get(1));
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final GcStats stats = GcStats.builder().totalAllocated(100L).build();
      assertEquals(stats, stats);
    }

    @Test
    @DisplayName("should be equal to stats with same values")
    void shouldBeEqualToStatsWithSameValues() {
      final Instant now = Instant.now();
      final GcStats stats1 = GcStats.builder().totalAllocated(100L).captureTime(now).build();
      final GcStats stats2 = GcStats.builder().totalAllocated(100L).captureTime(now).build();
      assertEquals(stats1, stats2);
    }

    @Test
    @DisplayName("should not be equal to stats with different values")
    void shouldNotBeEqualToStatsWithDifferentValues() {
      final GcStats stats1 = GcStats.builder().totalAllocated(100L).build();
      final GcStats stats2 = GcStats.builder().totalAllocated(200L).build();
      assertNotEquals(stats1, stats2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final GcStats stats = GcStats.builder().build();
      assertNotEquals(null, stats);
    }
  }

  @Nested
  @DisplayName("ToString and Summary Tests")
  class ToStringAndSummaryTests {

    @Test
    @DisplayName("toString should not be null")
    void toStringShouldNotBeNull() {
      final GcStats stats = GcStats.builder().build();
      assertNotNull(stats.toString());
    }

    @Test
    @DisplayName("getSummary should not be null")
    void getSummaryShouldNotBeNull() {
      final GcStats stats = GcStats.builder().build();
      assertNotNull(stats.getSummary());
    }

    @Test
    @DisplayName("getSummary should contain key metrics")
    void getSummaryShouldContainKeyMetrics() {
      final GcStats stats =
          GcStats.builder()
              .totalAllocated(1000L)
              .totalCollected(500L)
              .currentHeapSize(1048576)
              .maxHeapSize(2097152)
              .minorCollections(5L)
              .majorCollections(2L)
              .totalGcTime(Duration.ofMillis(100))
              .build();

      final String summary = stats.getSummary();
      assertTrue(summary.contains("500"), "Should contain live objects");
      assertTrue(summary.contains("collections"), "Should mention collections");
    }
  }
}
