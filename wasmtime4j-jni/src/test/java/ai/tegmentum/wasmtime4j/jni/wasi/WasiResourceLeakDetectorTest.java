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
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for WasiResourceLeakDetector defensive programming and validation logic.
 *
 * <p>These tests focus on parameter validation and resource tracking logic without requiring native
 * library loading.
 */
class WasiResourceLeakDetectorTest {
  private WasiResourceLeakDetector detector;

  @BeforeEach
  void setUp() {
    detector = new WasiResourceLeakDetector();
  }

  @AfterEach
  void tearDown() {
    if (detector != null) {
      detector.close();
    }
  }

  // Constructor tests

  @Test
  void testDefaultConstructor() {
    final WasiResourceLeakDetector defaultDetector = new WasiResourceLeakDetector();

    assertThat(defaultDetector).isNotNull();
    assertThat(defaultDetector.getTrackedContextCount()).isZero();
    assertThat(defaultDetector.getTrackedFileHandleCount()).isZero();
    assertThat(defaultDetector.getTrackedMemorySegmentCount()).isZero();

    defaultDetector.close();
  }

  @Test
  void testParameterizedConstructorWithValidValues() {
    final WasiResourceLeakDetector customDetector =
        new WasiResourceLeakDetector(500, Duration.ofMinutes(15), 60);

    assertThat(customDetector).isNotNull();
    assertThat(customDetector.getTrackedContextCount()).isZero();

    customDetector.close();
  }

  @Test
  void testParameterizedConstructorWithZeroLeakThreshold() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new WasiResourceLeakDetector(0, Duration.ofMinutes(15), 60));

    assertThat(exception.getMessage()).contains("leakThreshold");
  }

  @Test
  void testParameterizedConstructorWithNegativeLeakThreshold() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new WasiResourceLeakDetector(-1, Duration.ofMinutes(15), 60));

    assertThat(exception.getMessage()).contains("leakThreshold");
  }

  @Test
  void testParameterizedConstructorWithNullResourceAgeThreshold() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> new WasiResourceLeakDetector(500, null, 60));

    assertThat(exception.getMessage()).contains("resourceAgeThreshold");
  }

  @Test
  void testParameterizedConstructorWithZeroMonitoringInterval() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new WasiResourceLeakDetector(500, Duration.ofMinutes(15), 0));

    assertThat(exception.getMessage()).contains("monitoringIntervalSeconds");
  }

  @Test
  void testParameterizedConstructorWithNegativeMonitoringInterval() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> new WasiResourceLeakDetector(500, Duration.ofMinutes(15), -1));

    assertThat(exception.getMessage()).contains("monitoringIntervalSeconds");
  }

  // trackWasiContext tests

  @Test
  void testTrackWasiContextWithNullContextId() {
    final WasiContext context = createFakeWasiContext();

    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> detector.trackWasiContext(null, context));

    assertThat(exception.getMessage()).contains("contextId");
  }

  @Test
  void testTrackWasiContextWithEmptyContextId() {
    final WasiContext context = createFakeWasiContext();

    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> detector.trackWasiContext("", context));

    assertThat(exception.getMessage()).contains("contextId");
  }

  @Test
  void testTrackWasiContextWithNullContext() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> detector.trackWasiContext("ctx1", null));

    assertThat(exception.getMessage()).contains("context");
  }

  @Test
  void testTrackWasiContextWithValidParameters() {
    final WasiContext context = createFakeWasiContext();

    assertDoesNotThrow(() -> detector.trackWasiContext("ctx1", context));

    assertThat(detector.getTrackedContextCount()).isEqualTo(1);
  }

  @Test
  void testTrackWasiContextAfterShutdown() {
    final WasiContext context = createFakeWasiContext();
    detector.close();

    // Should not throw but also should not track
    assertDoesNotThrow(() -> detector.trackWasiContext("ctx1", context));

    assertThat(detector.getTrackedContextCount()).isZero();
  }

  @Test
  void testTrackMultipleWasiContexts() {
    final WasiContext context1 = createFakeWasiContext();
    final WasiContext context2 = createFakeWasiContext();

    detector.trackWasiContext("ctx1", context1);
    detector.trackWasiContext("ctx2", context2);

    assertThat(detector.getTrackedContextCount()).isEqualTo(2);
  }

  // untrackWasiContext tests

  @Test
  void testUntrackWasiContextWithNullContextId() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> detector.untrackWasiContext(null));

    assertThat(exception.getMessage()).contains("contextId");
  }

  @Test
  void testUntrackWasiContextWithEmptyContextId() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> detector.untrackWasiContext(""));

    assertThat(exception.getMessage()).contains("contextId");
  }

  @Test
  void testUntrackWasiContextWithValidContextId() {
    final WasiContext context = createFakeWasiContext();
    detector.trackWasiContext("ctx1", context);

    assertDoesNotThrow(() -> detector.untrackWasiContext("ctx1"));

    assertThat(detector.getTrackedContextCount()).isZero();
  }

  @Test
  void testUntrackWasiContextThatWasNeverTracked() {
    // Should not throw
    assertDoesNotThrow(() -> detector.untrackWasiContext("non-existent"));

    assertThat(detector.getTrackedContextCount()).isZero();
  }

  // trackFileHandle tests

  @Test
  void testTrackFileHandleWithNullHandle() {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> detector.trackFileHandle(1, null));

    assertThat(exception.getMessage()).contains("handle");
  }

  @Test
  void testTrackFileHandleWithValidParameters() {
    final WasiFileHandle handle = createFakeFileHandle(1);

    assertDoesNotThrow(() -> detector.trackFileHandle(1, handle));

    assertThat(detector.getTrackedFileHandleCount()).isEqualTo(1);
  }

  @Test
  void testTrackFileHandleAfterShutdown() {
    final WasiFileHandle handle = createFakeFileHandle(1);
    detector.close();

    // Should not throw but also should not track
    assertDoesNotThrow(() -> detector.trackFileHandle(1, handle));

    assertThat(detector.getTrackedFileHandleCount()).isZero();
  }

  @Test
  void testTrackMultipleFileHandles() {
    final WasiFileHandle handle1 = createFakeFileHandle(1);
    final WasiFileHandle handle2 = createFakeFileHandle(2);

    detector.trackFileHandle(1, handle1);
    detector.trackFileHandle(2, handle2);

    assertThat(detector.getTrackedFileHandleCount()).isEqualTo(2);
  }

  // untrackFileHandle tests

  @Test
  void testUntrackFileHandleWithValidDescriptor() {
    final WasiFileHandle handle = createFakeFileHandle(1);
    detector.trackFileHandle(1, handle);

    assertDoesNotThrow(() -> detector.untrackFileHandle(1));

    assertThat(detector.getTrackedFileHandleCount()).isZero();
  }

  @Test
  void testUntrackFileHandleThatWasNeverTracked() {
    // Should not throw
    assertDoesNotThrow(() -> detector.untrackFileHandle(999));

    assertThat(detector.getTrackedFileHandleCount()).isZero();
  }

  // trackMemorySegment tests

  @Test
  void testTrackMemorySegmentWithNullSegment() {
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> detector.trackMemorySegment(0x1000L, null));

    assertThat(exception.getMessage()).contains("segment");
  }

  @Test
  void testTrackMemorySegmentWithValidParameters() {
    final Object segment = new Object();

    assertDoesNotThrow(() -> detector.trackMemorySegment(0x1000L, segment));

    assertThat(detector.getTrackedMemorySegmentCount()).isEqualTo(1);
  }

  @Test
  void testTrackMemorySegmentAfterShutdown() {
    final Object segment = new Object();
    detector.close();

    // Should not throw but also should not track
    assertDoesNotThrow(() -> detector.trackMemorySegment(0x1000L, segment));

    assertThat(detector.getTrackedMemorySegmentCount()).isZero();
  }

  @Test
  void testTrackMultipleMemorySegments() {
    final Object segment1 = new Object();
    final Object segment2 = new Object();

    detector.trackMemorySegment(0x1000L, segment1);
    detector.trackMemorySegment(0x2000L, segment2);

    assertThat(detector.getTrackedMemorySegmentCount()).isEqualTo(2);
  }

  // untrackMemorySegment tests

  @Test
  void testUntrackMemorySegmentWithValidAddress() {
    final Object segment = new Object();
    detector.trackMemorySegment(0x1000L, segment);

    assertDoesNotThrow(() -> detector.untrackMemorySegment(0x1000L));

    assertThat(detector.getTrackedMemorySegmentCount()).isZero();
  }

  @Test
  void testUntrackMemorySegmentThatWasNeverTracked() {
    // Should not throw
    assertDoesNotThrow(() -> detector.untrackMemorySegment(0x9999L));

    assertThat(detector.getTrackedMemorySegmentCount()).isZero();
  }

  // Statistics tests

  @Test
  void testGetStatisticsReturnsNonNull() {
    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();

    assertThat(stats).isNotNull();
  }

  @Test
  void testGetStatisticsReturnsDefensiveCopy() {
    final WasiResourceLeakDetector.ResourceStatistics stats1 = detector.getStatistics();
    final WasiResourceLeakDetector.ResourceStatistics stats2 = detector.getStatistics();

    // Should be different instances (defensive copies)
    assertThat(stats1).isNotSameAs(stats2);
  }

  @Test
  void testStatisticsTracksContextCreation() {
    final WasiContext context = createFakeWasiContext();
    detector.trackWasiContext("ctx1", context);

    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();

    assertThat(stats.getContextsCreated()).isEqualTo(1);
    assertThat(stats.getContextsDestroyed()).isZero();
    assertThat(stats.getActiveContexts()).isEqualTo(1);
  }

  @Test
  void testStatisticsTracksContextDestruction() {
    final WasiContext context = createFakeWasiContext();
    detector.trackWasiContext("ctx1", context);
    detector.untrackWasiContext("ctx1");

    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();

    assertThat(stats.getContextsCreated()).isEqualTo(1);
    assertThat(stats.getContextsDestroyed()).isEqualTo(1);
    assertThat(stats.getActiveContexts()).isZero();
  }

  @Test
  void testStatisticsTracksFileHandleCreation() {
    final WasiFileHandle handle = createFakeFileHandle(1);
    detector.trackFileHandle(1, handle);

    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();

    assertThat(stats.getFileHandlesCreated()).isEqualTo(1);
    assertThat(stats.getFileHandlesDestroyed()).isZero();
    assertThat(stats.getActiveFileHandles()).isEqualTo(1);
  }

  @Test
  void testStatisticsTracksFileHandleDestruction() {
    final WasiFileHandle handle = createFakeFileHandle(1);
    detector.trackFileHandle(1, handle);
    detector.untrackFileHandle(1);

    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();

    assertThat(stats.getFileHandlesCreated()).isEqualTo(1);
    assertThat(stats.getFileHandlesDestroyed()).isEqualTo(1);
    assertThat(stats.getActiveFileHandles()).isZero();
  }

  @Test
  void testStatisticsTracksMemorySegmentCreation() {
    final Object segment = new Object();
    detector.trackMemorySegment(0x1000L, segment);

    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();

    assertThat(stats.getMemorySegmentsCreated()).isEqualTo(1);
    assertThat(stats.getMemorySegmentsDestroyed()).isZero();
    assertThat(stats.getActiveMemorySegments()).isEqualTo(1);
  }

  @Test
  void testStatisticsTracksMemorySegmentDestruction() {
    final Object segment = new Object();
    detector.trackMemorySegment(0x1000L, segment);
    detector.untrackMemorySegment(0x1000L);

    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();

    assertThat(stats.getMemorySegmentsCreated()).isEqualTo(1);
    assertThat(stats.getMemorySegmentsDestroyed()).isEqualTo(1);
    assertThat(stats.getActiveMemorySegments()).isZero();
  }

  @Test
  void testStatisticsToStringContainsInformation() {
    final WasiResourceLeakDetector.ResourceStatistics stats = detector.getStatistics();
    final String statsString = stats.toString();

    assertThat(statsString).contains("ResourceStatistics");
    assertThat(statsString).contains("contexts=");
    assertThat(statsString).contains("handles=");
    assertThat(statsString).contains("segments=");
  }

  // Leak detection tests

  @Test
  void testPerformLeakDetectionReturnsResults() {
    final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();

    assertThat(results).isNotNull();
  }

  @Test
  void testPerformLeakDetectionAfterShutdown() {
    detector.close();

    final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();

    assertThat(results).isNotNull();
    assertThat(results.getTotalLeaked()).isZero();
    assertThat(results.getCleanedUpResources()).isZero();
  }

  @Test
  void testLeakDetectionResultsGetters() {
    final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();

    assertThat(results.getLeakedContexts()).isGreaterThanOrEqualTo(0);
    assertThat(results.getLeakedFileHandles()).isGreaterThanOrEqualTo(0);
    assertThat(results.getLeakedMemorySegments()).isGreaterThanOrEqualTo(0);
    assertThat(results.getCleanedUpResources()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void testLeakDetectionResultsHasLeaks() {
    final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();

    // Should not have leaks initially
    assertFalse(results.hasLeaks());
  }

  @Test
  void testLeakDetectionResultsToString() {
    final WasiResourceLeakDetector.LeakDetectionResults results = detector.performLeakDetection();
    final String resultsString = results.toString();

    assertThat(resultsString).contains("LeakDetectionResults");
    assertThat(resultsString).contains("contexts=");
    assertThat(resultsString).contains("handles=");
    assertThat(resultsString).contains("segments=");
    assertThat(resultsString).contains("cleaned=");
  }

  // Close tests

  @Test
  void testCloseIsIdempotent() {
    detector.close();
    detector.close();
    detector.close();

    // Should not throw
    assertDoesNotThrow(() -> detector.close());
  }

  @Test
  void testCloseReleasesResources() {
    final WasiContext context = createFakeWasiContext();
    detector.trackWasiContext("ctx1", context);

    detector.close();

    // After close, tracked counts should be zero
    assertThat(detector.getTrackedContextCount()).isZero();
  }

  @Test
  void testTrackingAfterCloseDoesNothing() {
    detector.close();

    final WasiContext context = createFakeWasiContext();
    detector.trackWasiContext("ctx1", context);

    assertThat(detector.getTrackedContextCount()).isZero();
  }

  // Helper methods

  private WasiContext createFakeWasiContext() {
    // Create a minimal WasiContext instance for testing
    // We can't instantiate WasiContext directly without native library,
    // but we need an object for weak/phantom reference tracking
    final WasiContextBuilder fakeBuilder = new WasiContextBuilder();
    return new WasiContext(1L, fakeBuilder);
  }

  private WasiFileHandle createFakeFileHandle(final int fileDescriptor) {
    // Create a minimal WasiFileHandle for testing
    // Using a simple anonymous SeekableByteChannel implementation
    final Path fakePath = Paths.get("/tmp/test.txt");
    final java.nio.channels.SeekableByteChannel fakeChannel =
        new java.nio.channels.SeekableByteChannel() {
          @Override
          public int read(final java.nio.ByteBuffer dst) {
            return 0;
          }

          @Override
          public int write(final java.nio.ByteBuffer src) {
            return 0;
          }

          @Override
          public long position() {
            return 0;
          }

          @Override
          public java.nio.channels.SeekableByteChannel position(final long newPosition) {
            return this;
          }

          @Override
          public long size() {
            return 0;
          }

          @Override
          public java.nio.channels.SeekableByteChannel truncate(final long size) {
            return this;
          }

          @Override
          public boolean isOpen() {
            return true;
          }

          @Override
          public void close() {}
        };

    return new WasiFileHandle(fileDescriptor, fakePath, fakeChannel, null, WasiFileOperation.READ);
  }
}
