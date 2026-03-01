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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcCollectionResult} class.
 *
 * <p>GcCollectionResult contains statistics about a garbage collection cycle, including the number
 * of objects and bytes collected.
 */
@DisplayName("GcCollectionResult Tests")
class GcCollectionResultTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("default constructor should initialize fields to zero")
    void defaultConstructorShouldInitializeFieldsToZero() {
      final GcCollectionResult result = new GcCollectionResult();

      assertEquals(0, result.getObjectsCollected(), "Objects collected should be 0");
      assertEquals(0, result.getBytesCollected(), "Bytes collected should be 0");
    }

    @Test
    @DisplayName("should create non-null instance")
    void shouldCreateNonNullInstance() {
      final GcCollectionResult result = new GcCollectionResult();
      assertNotNull(result, "Should create non-null instance");
    }
  }

  @Nested
  @DisplayName("Field Access Tests")
  class FieldAccessTests {

    @Test
    @DisplayName("should allow setting objectsCollected")
    void shouldAllowSettingObjectsCollected() {
      final GcCollectionResult result = new GcCollectionResult();
      result.objectsCollected = 100;

      assertEquals(100, result.objectsCollected, "Should allow direct field access");
      assertEquals(100, result.getObjectsCollected(), "Getter should return same value");
    }

    @Test
    @DisplayName("should allow setting bytesCollected")
    void shouldAllowSettingBytesCollected() {
      final GcCollectionResult result = new GcCollectionResult();
      result.bytesCollected = 1024 * 1024;

      assertEquals(1024 * 1024, result.bytesCollected, "Should allow direct field access");
      assertEquals(1024 * 1024, result.getBytesCollected(), "Getter should return same value");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getObjectsCollected should return objectsCollected")
    void getObjectsCollectedShouldReturnObjectsCollected() {
      final GcCollectionResult result = new GcCollectionResult();
      result.objectsCollected = 500;

      assertEquals(500, result.getObjectsCollected(), "Should return objects collected");
    }

    @Test
    @DisplayName("getBytesCollected should return bytesCollected")
    void getBytesCollectedShouldReturnBytesCollected() {
      final GcCollectionResult result = new GcCollectionResult();
      result.bytesCollected = 4096;

      assertEquals(4096, result.getBytesCollected(), "Should return bytes collected");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final GcCollectionResult result = new GcCollectionResult();
      result.objectsCollected = 10;
      result.bytesCollected = 2048;

      final String str = result.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("10"), "Should contain objectsCollected value");
      assertTrue(str.contains("2048"), "Should contain bytesCollected value");
      assertTrue(str.contains("GcCollectionResult"), "Should contain class name");
    }

    @Test
    @DisplayName("toString should handle zero values")
    void toStringShouldHandleZeroValues() {
      final GcCollectionResult result = new GcCollectionResult();
      final String str = result.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("0"), "Should contain zero values");
    }
  }

  @Nested
  @DisplayName("Large Value Tests")
  class LargeValueTests {

    @Test
    @DisplayName("should handle large objectsCollected values")
    void shouldHandleLargeObjectsCollectedValues() {
      final GcCollectionResult result = new GcCollectionResult();
      result.objectsCollected = Long.MAX_VALUE;

      assertEquals(Long.MAX_VALUE, result.getObjectsCollected(), "Should handle Long.MAX_VALUE");
    }

    @Test
    @DisplayName("should handle large bytesCollected values")
    void shouldHandleLargeBytesCollectedValues() {
      final GcCollectionResult result = new GcCollectionResult();
      result.bytesCollected = Long.MAX_VALUE;

      assertEquals(Long.MAX_VALUE, result.getBytesCollected(), "Should handle Long.MAX_VALUE");
    }

    @Test
    @DisplayName("should handle typical GC collection sizes")
    void shouldHandleTypicalGcCollectionSizes() {
      final GcCollectionResult result = new GcCollectionResult();

      // Typical GC: collected 1000 objects, 10MB of memory
      result.objectsCollected = 1000;
      result.bytesCollected = 10 * 1024 * 1024;

      assertEquals(1000, result.getObjectsCollected(), "Should handle typical object count");
      assertEquals(
          10 * 1024 * 1024, result.getBytesCollected(), "Should handle typical byte count");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support typical GC result pattern")
    void shouldSupportTypicalGcResultPattern() {
      // Pattern: GC runtime populates result after collection
      final GcCollectionResult result = new GcCollectionResult();

      // Simulate GC populating results
      result.objectsCollected = 250;
      result.bytesCollected = 512 * 1024; // 512KB

      // Verify results can be read
      assertTrue(result.getObjectsCollected() > 0, "Should have collected objects");
      assertTrue(result.getBytesCollected() > 0, "Should have collected bytes");
    }

    @Test
    @DisplayName("should support result reporting pattern")
    void shouldSupportResultReportingPattern() {
      final GcCollectionResult result = new GcCollectionResult();
      result.objectsCollected = 100;
      result.bytesCollected = 1024;

      // Pattern: report GC results
      final String report =
          String.format(
              "GC collected %d objects (%d bytes)",
              result.getObjectsCollected(), result.getBytesCollected());

      assertTrue(report.contains("100"), "Report should contain object count");
      assertTrue(report.contains("1024"), "Report should contain byte count");
    }
  }
}
