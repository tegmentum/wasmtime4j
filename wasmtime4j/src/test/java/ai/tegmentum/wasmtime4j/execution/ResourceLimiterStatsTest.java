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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ResourceLimiterStats class.
 *
 * <p>Verifies the constructor, getter methods, and computed statistics for resource limiter
 * metrics.
 */
@DisplayName("ResourceLimiterStats Tests")
class ResourceLimiterStatsTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create stats with all zero values")
    void shouldCreateStatsWithAllZeroValues() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 0, 0);

      assertNotNull(stats, "Stats should not be null");
      assertEquals(0, stats.getTotalMemoryBytes(), "totalMemoryBytes should be 0");
      assertEquals(0, stats.getTotalTableElements(), "totalTableElements should be 0");
      assertEquals(0, stats.getMemoryGrowRequests(), "memoryGrowRequests should be 0");
      assertEquals(0, stats.getMemoryGrowDenials(), "memoryGrowDenials should be 0");
      assertEquals(0, stats.getTableGrowRequests(), "tableGrowRequests should be 0");
      assertEquals(0, stats.getTableGrowDenials(), "tableGrowDenials should be 0");
    }

    @Test
    @DisplayName("should create stats with typical values")
    void shouldCreateStatsWithTypicalValues() {
      ResourceLimiterStats stats =
          new ResourceLimiterStats(
              1024 * 1024, // 1 MB memory
              500, // 500 table elements
              10, // 10 memory grow requests
              2, // 2 memory grow denials
              5, // 5 table grow requests
              1 // 1 table grow denial
              );

      assertEquals(1024 * 1024, stats.getTotalMemoryBytes(), "totalMemoryBytes should be 1 MB");
      assertEquals(500, stats.getTotalTableElements(), "totalTableElements should be 500");
      assertEquals(10, stats.getMemoryGrowRequests(), "memoryGrowRequests should be 10");
      assertEquals(2, stats.getMemoryGrowDenials(), "memoryGrowDenials should be 2");
      assertEquals(5, stats.getTableGrowRequests(), "tableGrowRequests should be 5");
      assertEquals(1, stats.getTableGrowDenials(), "tableGrowDenials should be 1");
    }

    @Test
    @DisplayName("should create stats with large values")
    void shouldCreateStatsWithLargeValues() {
      long largeMemory = 10L * 1024L * 1024L * 1024L; // 10 GB
      long largeElements = 1_000_000L;
      long largeRequests = 100_000L;

      ResourceLimiterStats stats =
          new ResourceLimiterStats(largeMemory, largeElements, largeRequests, 1000, 50000, 500);

      assertEquals(largeMemory, stats.getTotalMemoryBytes(), "totalMemoryBytes should be 10 GB");
      assertEquals(largeElements, stats.getTotalTableElements(), "totalTableElements should match");
      assertEquals(largeRequests, stats.getMemoryGrowRequests(), "memoryGrowRequests should match");
    }
  }

  @Nested
  @DisplayName("TotalMemoryBytes Tests")
  class TotalMemoryBytesTests {

    @Test
    @DisplayName("should return correct totalMemoryBytes")
    void shouldReturnCorrectTotalMemoryBytes() {
      long expectedBytes = 5 * 1024 * 1024; // 5 MB
      ResourceLimiterStats stats = new ResourceLimiterStats(expectedBytes, 0, 0, 0, 0, 0);

      assertEquals(
          expectedBytes, stats.getTotalMemoryBytes(), "getTotalMemoryBytes should return 5 MB");
    }

    @Test
    @DisplayName("should handle Long.MAX_VALUE for totalMemoryBytes")
    void shouldHandleLongMaxValueForTotalMemoryBytes() {
      ResourceLimiterStats stats = new ResourceLimiterStats(Long.MAX_VALUE, 0, 0, 0, 0, 0);

      assertEquals(
          Long.MAX_VALUE,
          stats.getTotalMemoryBytes(),
          "getTotalMemoryBytes should return Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("TotalTableElements Tests")
  class TotalTableElementsTests {

    @Test
    @DisplayName("should return correct totalTableElements")
    void shouldReturnCorrectTotalTableElements() {
      long expectedElements = 10000;
      ResourceLimiterStats stats = new ResourceLimiterStats(0, expectedElements, 0, 0, 0, 0);

      assertEquals(
          expectedElements,
          stats.getTotalTableElements(),
          "getTotalTableElements should return 10000");
    }
  }

  @Nested
  @DisplayName("Memory Grow Request/Denial Tests")
  class MemoryGrowTests {

    @Test
    @DisplayName("should return correct memoryGrowRequests")
    void shouldReturnCorrectMemoryGrowRequests() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 25, 0, 0, 0);

      assertEquals(25, stats.getMemoryGrowRequests(), "getMemoryGrowRequests should return 25");
    }

    @Test
    @DisplayName("should return correct memoryGrowDenials")
    void shouldReturnCorrectMemoryGrowDenials() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 5, 0, 0);

      assertEquals(5, stats.getMemoryGrowDenials(), "getMemoryGrowDenials should return 5");
    }
  }

  @Nested
  @DisplayName("Table Grow Request/Denial Tests")
  class TableGrowTests {

    @Test
    @DisplayName("should return correct tableGrowRequests")
    void shouldReturnCorrectTableGrowRequests() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 15, 0);

      assertEquals(15, stats.getTableGrowRequests(), "getTableGrowRequests should return 15");
    }

    @Test
    @DisplayName("should return correct tableGrowDenials")
    void shouldReturnCorrectTableGrowDenials() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 0, 3);

      assertEquals(3, stats.getTableGrowDenials(), "getTableGrowDenials should return 3");
    }
  }

  @Nested
  @DisplayName("Memory Denial Rate Tests")
  class MemoryDenialRateTests {

    @Test
    @DisplayName("should return 0.0 when no memory grow requests")
    void shouldReturnZeroWhenNoMemoryGrowRequests() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 5, 0, 0);

      assertEquals(
          0.0, stats.getMemoryDenialRate(), 0.001, "Denial rate should be 0.0 with no requests");
    }

    @Test
    @DisplayName("should return correct denial rate for typical scenario")
    void shouldReturnCorrectDenialRateForTypicalScenario() {
      // 100 requests, 20 denials = 20% denial rate
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 100, 20, 0, 0);

      assertEquals(
          0.2, stats.getMemoryDenialRate(), 0.001, "Denial rate should be 0.2 (20%) with 20/100");
    }

    @Test
    @DisplayName("should return 0.0 when all requests are approved")
    void shouldReturnZeroWhenAllRequestsApproved() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 50, 0, 0, 0);

      assertEquals(
          0.0, stats.getMemoryDenialRate(), 0.001, "Denial rate should be 0.0 when no denials");
    }

    @Test
    @DisplayName("should return 1.0 when all requests are denied")
    void shouldReturnOneWhenAllRequestsDenied() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 30, 30, 0, 0);

      assertEquals(
          1.0, stats.getMemoryDenialRate(), 0.001, "Denial rate should be 1.0 when all denied");
    }

    @Test
    @DisplayName("should handle fractional denial rates")
    void shouldHandleFractionalDenialRates() {
      // 3 requests, 1 denial = 33.33% denial rate
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 3, 1, 0, 0);

      double expectedRate = 1.0 / 3.0;
      assertEquals(
          expectedRate, stats.getMemoryDenialRate(), 0.001, "Denial rate should be ~0.333 (1/3)");
    }
  }

  @Nested
  @DisplayName("Table Denial Rate Tests")
  class TableDenialRateTests {

    @Test
    @DisplayName("should return 0.0 when no table grow requests")
    void shouldReturnZeroWhenNoTableGrowRequests() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 0, 5);

      assertEquals(
          0.0, stats.getTableDenialRate(), 0.001, "Denial rate should be 0.0 with no requests");
    }

    @Test
    @DisplayName("should return correct denial rate for typical scenario")
    void shouldReturnCorrectDenialRateForTypicalScenario() {
      // 50 requests, 10 denials = 20% denial rate
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 50, 10);

      assertEquals(
          0.2, stats.getTableDenialRate(), 0.001, "Denial rate should be 0.2 (20%) with 10/50");
    }

    @Test
    @DisplayName("should return 0.0 when all table requests are approved")
    void shouldReturnZeroWhenAllTableRequestsApproved() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 25, 0);

      assertEquals(0.0, stats.getTableDenialRate(), 0.001, "Denial rate should be 0.0");
    }

    @Test
    @DisplayName("should return 1.0 when all table requests are denied")
    void shouldReturnOneWhenAllTableRequestsDenied() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 15, 15);

      assertEquals(
          1.0, stats.getTableDenialRate(), 0.001, "Denial rate should be 1.0 when all denied");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 0, 0);

      assertNotNull(stats.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 0, 0, 0, 0);

      assertTrue(
          stats.toString().contains("ResourceLimiterStats"), "toString should contain class name");
    }

    @Test
    @DisplayName("should include field names in toString")
    void shouldIncludeFieldNamesInToString() {
      ResourceLimiterStats stats = new ResourceLimiterStats(1024, 500, 10, 2, 5, 1);

      String result = stats.toString();
      assertTrue(result.contains("totalMemoryBytes"), "toString should contain totalMemoryBytes");
      assertTrue(
          result.contains("totalTableElements"), "toString should contain totalTableElements");
      assertTrue(
          result.contains("memoryGrowRequests"), "toString should contain memoryGrowRequests");
      assertTrue(result.contains("memoryGrowDenials"), "toString should contain memoryGrowDenials");
      assertTrue(result.contains("tableGrowRequests"), "toString should contain tableGrowRequests");
      assertTrue(result.contains("tableGrowDenials"), "toString should contain tableGrowDenials");
    }

    @Test
    @DisplayName("should include values in toString")
    void shouldIncludeValuesInToString() {
      ResourceLimiterStats stats = new ResourceLimiterStats(1024, 500, 10, 2, 5, 1);

      String result = stats.toString();
      assertTrue(result.contains("1024"), "toString should contain memory bytes value");
      assertTrue(result.contains("500"), "toString should contain table elements value");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent typical usage scenario")
    void shouldRepresentTypicalUsageScenario() {
      // Simulate a module that has run for a while with some denials
      ResourceLimiterStats stats =
          new ResourceLimiterStats(
              64 * 1024 * 1024, // 64 MB total memory
              10000, // 10000 table elements
              150, // 150 memory grow requests
              15, // 15 memory grow denials (10%)
              80, // 80 table grow requests
              4 // 4 table grow denials (5%)
              );

      assertEquals(64 * 1024 * 1024, stats.getTotalMemoryBytes());
      assertEquals(10000, stats.getTotalTableElements());
      assertEquals(0.1, stats.getMemoryDenialRate(), 0.001, "Memory denial rate should be 10%");
      assertEquals(0.05, stats.getTableDenialRate(), 0.001, "Table denial rate should be 5%");
    }

    @Test
    @DisplayName("should handle edge case with more denials than requests")
    void shouldHandleEdgeCaseWithMoreDenialsThanRequests() {
      // This shouldn't happen in practice but the class should handle it
      ResourceLimiterStats stats = new ResourceLimiterStats(0, 0, 5, 10, 3, 7);

      // Denial rate can exceed 1.0 in malformed stats
      assertTrue(
          stats.getMemoryDenialRate() > 1.0, "Memory denial rate exceeds 1.0 with malformed data");
      assertTrue(
          stats.getTableDenialRate() > 1.0, "Table denial rate exceeds 1.0 with malformed data");
    }

    @Test
    @DisplayName("should handle maximum scale values")
    void shouldHandleMaximumScaleValues() {
      // Test with very large realistic values
      ResourceLimiterStats stats =
          new ResourceLimiterStats(
              Long.MAX_VALUE / 2, // Very large memory
              Long.MAX_VALUE / 2, // Very large table elements
              Long.MAX_VALUE / 4, // Many requests
              Long.MAX_VALUE / 8, // Some denials
              Long.MAX_VALUE / 4,
              Long.MAX_VALUE / 8);

      assertNotNull(stats.toString(), "Should handle large values in toString");
      assertTrue(stats.getMemoryDenialRate() > 0, "Should calculate denial rate with large values");
      assertTrue(stats.getTableDenialRate() > 0, "Should calculate denial rate with large values");
    }
  }
}
