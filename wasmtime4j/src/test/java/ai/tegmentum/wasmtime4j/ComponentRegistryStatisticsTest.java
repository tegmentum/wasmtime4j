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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentRegistryStatistics} class.
 *
 * <p>ComponentRegistryStatistics provides comprehensive statistics about a component registry
 * including component counts, memory usage, available interfaces, author/tag breakdown, timestamps,
 * and query metrics.
 */
@DisplayName("ComponentRegistryStatistics Tests")
class ComponentRegistryStatisticsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentRegistryStatistics.class.getModifiers()),
          "ComponentRegistryStatistics should be public");
      assertTrue(
          Modifier.isFinal(ComponentRegistryStatistics.class.getModifiers()),
          "ComponentRegistryStatistics should be final");
    }

    @Test
    @DisplayName("should have static builder() factory method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final var method = ComponentRegistryStatistics.class.getMethod("builder");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder() should be static");
    }
  }

  @Nested
  @DisplayName("Builder Default Values Tests")
  class BuilderDefaultValuesTests {

    @Test
    @DisplayName("should build with all numeric defaults as zero")
    void shouldBuildWithDefaultsAsZero() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder().build();

      assertNotNull(stats, "Stats should not be null");
      assertEquals(0, stats.getTotalComponents(), "Default totalComponents should be 0");
      assertEquals(0, stats.getActiveComponents(), "Default activeComponents should be 0");
      assertEquals(0, stats.getInactiveComponents(), "Default inactiveComponents should be 0");
      assertEquals(0L, stats.getTotalMemoryUsage(), "Default totalMemoryUsage should be 0");
      assertEquals(0L, stats.getAverageComponentSize(), "Default averageComponentSize should be 0");
      assertEquals(0L, stats.getTotalRegistrations(), "Default totalRegistrations should be 0");
      assertEquals(
          0L, stats.getTotalUnregistrations(),
          "Default totalUnregistrations should be 0");
      assertEquals(0.0, stats.getAverageQueryTime(), 0.001, "Default averageQueryTime should be 0");
      assertEquals(0L, stats.getTotalQueries(), "Default totalQueries should be 0");
    }

    @Test
    @DisplayName("should build with empty collections by default")
    void shouldBuildWithEmptyCollections() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder().build();

      assertTrue(
          stats.getAvailableInterfaces().isEmpty(),
          "Default availableInterfaces should be empty");
      assertTrue(
          stats.getComponentsByAuthor().isEmpty(),
          "Default componentsByAuthor should be empty");
      assertTrue(
          stats.getComponentsByTag().isEmpty(),
          "Default componentsByTag should be empty");
    }

    @Test
    @DisplayName("should build with null timestamps by default")
    void shouldBuildWithNullTimestamps() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder().build();

      assertNull(stats.getLastRegistration(), "Default lastRegistration should be null");
      assertNull(stats.getLastUnregistration(), "Default lastUnregistration should be null");
    }
  }

  @Nested
  @DisplayName("Builder Fluent API Tests")
  class BuilderFluentApiTests {

    @Test
    @DisplayName("should set totalComponents")
    void shouldSetTotalComponents() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .totalComponents(15)
          .build();

      assertEquals(15, stats.getTotalComponents(), "totalComponents should be 15");
    }

    @Test
    @DisplayName("should set activeComponents and inactiveComponents")
    void shouldSetActiveAndInactiveComponents() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .activeComponents(10)
          .inactiveComponents(5)
          .build();

      assertEquals(10, stats.getActiveComponents(), "activeComponents should be 10");
      assertEquals(5, stats.getInactiveComponents(), "inactiveComponents should be 5");
    }

    @Test
    @DisplayName("should set totalMemoryUsage and averageComponentSize")
    void shouldSetMemoryMetrics() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .totalMemoryUsage(1024 * 1024L)
          .averageComponentSize(10240L)
          .build();

      assertEquals(
          1024 * 1024L, stats.getTotalMemoryUsage(),
          "totalMemoryUsage should be 1 MB");
      assertEquals(
          10240L, stats.getAverageComponentSize(),
          "averageComponentSize should be 10240");
    }

    @Test
    @DisplayName("should set availableInterfaces")
    void shouldSetAvailableInterfaces() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .availableInterfaces(Set.of("wasi:http", "wasi:filesystem"))
          .build();

      assertEquals(
          2, stats.getAvailableInterfaces().size(),
          "Should have 2 available interfaces");
      assertTrue(
          stats.getAvailableInterfaces().contains("wasi:http"),
          "Should contain 'wasi:http'");
    }

    @Test
    @DisplayName("should set componentsByAuthor and componentsByTag")
    void shouldSetBreakdownMaps() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .componentsByAuthor(Map.of("alice", 3, "bob", 2))
          .componentsByTag(Map.of("networking", 4, "storage", 1))
          .build();

      assertEquals(
          2, stats.getComponentsByAuthor().size(),
          "componentsByAuthor should have 2 entries");
      assertEquals(
          3, stats.getComponentsByAuthor().get("alice"),
          "Alice should have 3 components");
      assertEquals(
          2, stats.getComponentsByTag().size(),
          "componentsByTag should have 2 entries");
    }

    @Test
    @DisplayName("should set timestamps")
    void shouldSetTimestamps() {
      final Instant regTime = Instant.parse("2025-01-15T10:00:00Z");
      final Instant unregTime = Instant.parse("2025-01-15T11:00:00Z");

      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .lastRegistration(regTime)
          .lastUnregistration(unregTime)
          .build();

      assertEquals(regTime, stats.getLastRegistration(), "lastRegistration should match");
      assertEquals(unregTime, stats.getLastUnregistration(), "lastUnregistration should match");
    }

    @Test
    @DisplayName("should set registration and unregistration counts")
    void shouldSetRegistrationCounts() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .totalRegistrations(100L)
          .totalUnregistrations(20L)
          .build();

      assertEquals(100L, stats.getTotalRegistrations(), "totalRegistrations should be 100");
      assertEquals(20L, stats.getTotalUnregistrations(), "totalUnregistrations should be 20");
    }

    @Test
    @DisplayName("should set query metrics")
    void shouldSetQueryMetrics() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .averageQueryTime(5.5)
          .totalQueries(1000L)
          .build();

      assertEquals(
          5.5, stats.getAverageQueryTime(), 0.001,
          "averageQueryTime should be 5.5");
      assertEquals(1000L, stats.getTotalQueries(), "totalQueries should be 1000");
    }
  }

  @Nested
  @DisplayName("Full Builder Chain Tests")
  class FullBuilderChainTests {

    @Test
    @DisplayName("should support full method chaining")
    void shouldSupportFullChaining() {
      final Instant now = Instant.now();
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .totalComponents(20)
          .activeComponents(15)
          .inactiveComponents(5)
          .totalMemoryUsage(2048L)
          .averageComponentSize(102L)
          .availableInterfaces(Set.of("iface1"))
          .componentsByAuthor(Map.of("author1", 10))
          .componentsByTag(Map.of("tag1", 5))
          .lastRegistration(now)
          .lastUnregistration(now)
          .totalRegistrations(50L)
          .totalUnregistrations(10L)
          .averageQueryTime(2.3)
          .totalQueries(500L)
          .build();

      assertEquals(20, stats.getTotalComponents(), "totalComponents should be 20");
      assertEquals(15, stats.getActiveComponents(), "activeComponents should be 15");
      assertEquals(5, stats.getInactiveComponents(), "inactiveComponents should be 5");
      assertEquals(2048L, stats.getTotalMemoryUsage(), "totalMemoryUsage should be 2048");
      assertEquals(500L, stats.getTotalQueries(), "totalQueries should be 500");
    }
  }

  @Nested
  @DisplayName("Null Handling Tests")
  class NullHandlingTests {

    @Test
    @DisplayName("should handle null availableInterfaces as empty set")
    void shouldHandleNullAvailableInterfaces() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .availableInterfaces(null)
          .build();

      assertTrue(
          stats.getAvailableInterfaces().isEmpty(),
          "Null availableInterfaces should become empty set");
    }

    @Test
    @DisplayName("should handle null componentsByAuthor as empty map")
    void shouldHandleNullComponentsByAuthor() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .componentsByAuthor(null)
          .build();

      assertTrue(
          stats.getComponentsByAuthor().isEmpty(),
          "Null componentsByAuthor should become empty map");
    }

    @Test
    @DisplayName("should handle null componentsByTag as empty map")
    void shouldHandleNullComponentsByTag() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder()
          .componentsByTag(null)
          .build();

      assertTrue(
          stats.getComponentsByTag().isEmpty(),
          "Null componentsByTag should become empty map");
    }
  }
}
