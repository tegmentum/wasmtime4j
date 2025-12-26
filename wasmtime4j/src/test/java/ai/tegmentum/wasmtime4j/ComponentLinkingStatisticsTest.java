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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLinkingStatistics} class.
 *
 * <p>ComponentLinkingStatistics provides statistics and metrics for component linking operations.
 */
@DisplayName("ComponentLinkingStatistics Tests")
class ComponentLinkingStatisticsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentLinkingStatistics.class.getModifiers()),
          "ComponentLinkingStatistics should be public");
      assertTrue(
          Modifier.isFinal(ComponentLinkingStatistics.class.getModifiers()),
          "ComponentLinkingStatistics should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final var stats = new ComponentLinkingStatistics(100, 50, 25);

      assertEquals(100, stats.getTotalLinksCreated(), "Total links created should be 100");
      assertEquals(50, stats.getTotalSwapsPerformed(), "Total swaps performed should be 50");
      assertEquals(25, stats.getActiveLinksCount(), "Active links count should be 25");
    }

    @Test
    @DisplayName("should handle zero values")
    void shouldHandleZeroValues() {
      final var stats = new ComponentLinkingStatistics(0, 0, 0);

      assertEquals(0, stats.getTotalLinksCreated(), "Total links created should be 0");
      assertEquals(0, stats.getTotalSwapsPerformed(), "Total swaps performed should be 0");
      assertEquals(0, stats.getActiveLinksCount(), "Active links count should be 0");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getTotalLinksCreated should return correct value")
    void getTotalLinksCreatedShouldReturnCorrectValue() {
      final var stats = new ComponentLinkingStatistics(1000, 0, 0);

      assertEquals(1000, stats.getTotalLinksCreated(), "Should return 1000");
    }

    @Test
    @DisplayName("getTotalSwapsPerformed should return correct value")
    void getTotalSwapsPerformedShouldReturnCorrectValue() {
      final var stats = new ComponentLinkingStatistics(0, 500, 0);

      assertEquals(500, stats.getTotalSwapsPerformed(), "Should return 500");
    }

    @Test
    @DisplayName("getActiveLinksCount should return correct value")
    void getActiveLinksCountShouldReturnCorrectValue() {
      final var stats = new ComponentLinkingStatistics(0, 0, 75);

      assertEquals(75, stats.getActiveLinksCount(), "Should return 75");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle max int values")
    void shouldHandleMaxIntValues() {
      final var stats =
          new ComponentLinkingStatistics(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

      assertEquals(
          Integer.MAX_VALUE, stats.getTotalLinksCreated(), "Should handle max int for links");
      assertEquals(
          Integer.MAX_VALUE, stats.getTotalSwapsPerformed(), "Should handle max int for swaps");
      assertEquals(
          Integer.MAX_VALUE, stats.getActiveLinksCount(), "Should handle max int for active");
    }

    @Test
    @DisplayName("should handle negative values")
    void shouldHandleNegativeValues() {
      final var stats = new ComponentLinkingStatistics(-1, -1, -1);

      assertEquals(-1, stats.getTotalLinksCreated(), "Should handle negative links");
      assertEquals(-1, stats.getTotalSwapsPerformed(), "Should handle negative swaps");
      assertEquals(-1, stats.getActiveLinksCount(), "Should handle negative active");
    }

    @Test
    @DisplayName("should handle active count greater than created")
    void shouldHandleActiveCountGreaterThanCreated() {
      // Edge case - active count can be greater than created if counting differently
      final var stats = new ComponentLinkingStatistics(10, 5, 20);

      assertEquals(10, stats.getTotalLinksCreated(), "Links created should be 10");
      assertEquals(20, stats.getActiveLinksCount(), "Active count should be 20");
    }
  }

  @Nested
  @DisplayName("Relationship Tests")
  class RelationshipTests {

    @Test
    @DisplayName("active links should be less than or equal to total in typical case")
    void activeLinksTypicallyLessOrEqualToTotal() {
      final var stats = new ComponentLinkingStatistics(100, 30, 50);

      assertTrue(
          stats.getActiveLinksCount() <= stats.getTotalLinksCreated(),
          "Active links typically <= total created");
    }

    @Test
    @DisplayName("swaps can exceed link count")
    void swapsCanExceedLinkCount() {
      // A link can be swapped multiple times
      final var stats = new ComponentLinkingStatistics(10, 100, 10);

      assertTrue(
          stats.getTotalSwapsPerformed() > stats.getTotalLinksCreated(),
          "Swaps can exceed link count");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var stats = new ComponentLinkingStatistics(100, 50, 25);

      final String result = stats.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("100"), "Should contain total links");
      assertTrue(result.contains("50"), "Should contain swaps count");
      assertTrue(result.contains("25"), "Should contain active count");
    }

    @Test
    @DisplayName("toString should contain all field values")
    void toStringShouldContainAllFieldValues() {
      final var stats = new ComponentLinkingStatistics(123, 456, 789);

      final String result = stats.toString();

      assertTrue(result.contains("123"), "Should contain 123");
      assertTrue(result.contains("456"), "Should contain 456");
      assertTrue(result.contains("789"), "Should contain 789");
    }
  }
}
