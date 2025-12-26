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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
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
 * <p>ComponentRegistryStatistics provides comprehensive statistics about a component registry's
 * state, including component counts, memory usage, performance metrics, and operational statistics.
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
      assertFalse(
          ComponentRegistryStatistics.class.isInterface(),
          "ComponentRegistryStatistics should not be an interface");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          ComponentRegistryStatistics.Builder.class,
          method.getReturnType(),
          "builder should return Builder");
    }

    @Test
    @DisplayName("should have Builder inner class")
    void shouldHaveBuilderInnerClass() {
      final Class<?>[] declaredClasses = ComponentRegistryStatistics.class.getDeclaredClasses();
      boolean hasBuilder = false;
      for (final Class<?> innerClass : declaredClasses) {
        if ("Builder".equals(innerClass.getSimpleName())) {
          hasBuilder = true;
          assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Builder should be public");
          assertTrue(Modifier.isFinal(innerClass.getModifiers()), "Builder should be final");
          assertTrue(Modifier.isStatic(innerClass.getModifiers()), "Builder should be static");
          break;
        }
      }
      assertTrue(hasBuilder, "Should have Builder inner class");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getTotalComponents method")
    void shouldHaveGetTotalComponentsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getTotalComponents");
      assertNotNull(method, "getTotalComponents method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getActiveComponents method")
    void shouldHaveGetActiveComponentsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getActiveComponents");
      assertNotNull(method, "getActiveComponents method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getInactiveComponents method")
    void shouldHaveGetInactiveComponentsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getInactiveComponents");
      assertNotNull(method, "getInactiveComponents method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getTotalMemoryUsage method")
    void shouldHaveGetTotalMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getTotalMemoryUsage");
      assertNotNull(method, "getTotalMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAverageComponentSize method")
    void shouldHaveGetAverageComponentSizeMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getAverageComponentSize");
      assertNotNull(method, "getAverageComponentSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAvailableInterfaces method")
    void shouldHaveGetAvailableInterfacesMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getAvailableInterfaces");
      assertNotNull(method, "getAvailableInterfaces method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getComponentsByAuthor method")
    void shouldHaveGetComponentsByAuthorMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getComponentsByAuthor");
      assertNotNull(method, "getComponentsByAuthor method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getComponentsByTag method")
    void shouldHaveGetComponentsByTagMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getComponentsByTag");
      assertNotNull(method, "getComponentsByTag method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getLastRegistration method")
    void shouldHaveGetLastRegistrationMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getLastRegistration");
      assertNotNull(method, "getLastRegistration method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getLastUnregistration method")
    void shouldHaveGetLastUnregistrationMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getLastUnregistration");
      assertNotNull(method, "getLastUnregistration method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getTotalRegistrations method")
    void shouldHaveGetTotalRegistrationsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getTotalRegistrations");
      assertNotNull(method, "getTotalRegistrations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTotalUnregistrations method")
    void shouldHaveGetTotalUnregistrationsMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getTotalUnregistrations");
      assertNotNull(method, "getTotalUnregistrations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAverageQueryTime method")
    void shouldHaveGetAverageQueryTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getAverageQueryTime");
      assertNotNull(method, "getAverageQueryTime method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have getTotalQueries method")
    void shouldHaveGetTotalQueriesMethod() throws NoSuchMethodException {
      final Method method = ComponentRegistryStatistics.class.getMethod("getTotalQueries");
      assertNotNull(method, "getTotalQueries method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create statistics with defaults")
    void builderShouldCreateStatisticsWithDefaults() {
      final ComponentRegistryStatistics stats = ComponentRegistryStatistics.builder().build();

      assertNotNull(stats, "Builder should create statistics");
      assertEquals(0, stats.getTotalComponents(), "Total components should default to 0");
      assertEquals(0, stats.getActiveComponents(), "Active components should default to 0");
      assertEquals(0, stats.getInactiveComponents(), "Inactive components should default to 0");
      assertEquals(0L, stats.getTotalMemoryUsage(), "Total memory usage should default to 0");
      assertEquals(
          0L, stats.getAverageComponentSize(), "Average component size should default to 0");
      assertTrue(stats.getAvailableInterfaces().isEmpty(), "Available interfaces should be empty");
      assertTrue(stats.getComponentsByAuthor().isEmpty(), "Components by author should be empty");
      assertTrue(stats.getComponentsByTag().isEmpty(), "Components by tag should be empty");
      assertNull(stats.getLastRegistration(), "Last registration should be null");
      assertNull(stats.getLastUnregistration(), "Last unregistration should be null");
      assertEquals(0L, stats.getTotalRegistrations(), "Total registrations should default to 0");
      assertEquals(
          0L, stats.getTotalUnregistrations(), "Total unregistrations should default to 0");
      assertEquals(
          0.0, stats.getAverageQueryTime(), 0.001, "Average query time should default to 0");
      assertEquals(0L, stats.getTotalQueries(), "Total queries should default to 0");
    }

    @Test
    @DisplayName("builder should set totalComponents")
    void builderShouldSetTotalComponents() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().totalComponents(100).build();

      assertEquals(100, stats.getTotalComponents(), "Total components should be 100");
    }

    @Test
    @DisplayName("builder should set activeComponents")
    void builderShouldSetActiveComponents() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().activeComponents(50).build();

      assertEquals(50, stats.getActiveComponents(), "Active components should be 50");
    }

    @Test
    @DisplayName("builder should set inactiveComponents")
    void builderShouldSetInactiveComponents() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().inactiveComponents(25).build();

      assertEquals(25, stats.getInactiveComponents(), "Inactive components should be 25");
    }

    @Test
    @DisplayName("builder should set totalMemoryUsage")
    void builderShouldSetTotalMemoryUsage() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().totalMemoryUsage(1024 * 1024L).build();

      assertEquals(1024 * 1024L, stats.getTotalMemoryUsage(), "Total memory should be 1MB");
    }

    @Test
    @DisplayName("builder should set averageComponentSize")
    void builderShouldSetAverageComponentSize() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().averageComponentSize(10240L).build();

      assertEquals(10240L, stats.getAverageComponentSize(), "Average size should be 10KB");
    }

    @Test
    @DisplayName("builder should set availableInterfaces")
    void builderShouldSetAvailableInterfaces() {
      final Set<String> interfaces = Set.of("wasi:http", "wasi:filesystem");
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().availableInterfaces(interfaces).build();

      assertEquals(2, stats.getAvailableInterfaces().size(), "Should have 2 interfaces");
      assertTrue(stats.getAvailableInterfaces().contains("wasi:http"), "Should contain wasi:http");
    }

    @Test
    @DisplayName("builder should set componentsByAuthor")
    void builderShouldSetComponentsByAuthor() {
      final Map<String, Integer> byAuthor = Map.of("author1", 5, "author2", 3);
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().componentsByAuthor(byAuthor).build();

      assertEquals(2, stats.getComponentsByAuthor().size(), "Should have 2 authors");
      assertEquals(5, stats.getComponentsByAuthor().get("author1"), "author1 should have 5");
    }

    @Test
    @DisplayName("builder should set componentsByTag")
    void builderShouldSetComponentsByTag() {
      final Map<String, Integer> byTag = Map.of("http", 10, "filesystem", 7);
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().componentsByTag(byTag).build();

      assertEquals(2, stats.getComponentsByTag().size(), "Should have 2 tags");
      assertEquals(10, stats.getComponentsByTag().get("http"), "http should have 10");
    }

    @Test
    @DisplayName("builder should set lastRegistration")
    void builderShouldSetLastRegistration() {
      final Instant now = Instant.now();
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().lastRegistration(now).build();

      assertEquals(now, stats.getLastRegistration(), "Last registration should match");
    }

    @Test
    @DisplayName("builder should set lastUnregistration")
    void builderShouldSetLastUnregistration() {
      final Instant now = Instant.now();
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().lastUnregistration(now).build();

      assertEquals(now, stats.getLastUnregistration(), "Last unregistration should match");
    }

    @Test
    @DisplayName("builder should set totalRegistrations")
    void builderShouldSetTotalRegistrations() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().totalRegistrations(1000L).build();

      assertEquals(1000L, stats.getTotalRegistrations(), "Total registrations should be 1000");
    }

    @Test
    @DisplayName("builder should set totalUnregistrations")
    void builderShouldSetTotalUnregistrations() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().totalUnregistrations(500L).build();

      assertEquals(500L, stats.getTotalUnregistrations(), "Total unregistrations should be 500");
    }

    @Test
    @DisplayName("builder should set averageQueryTime")
    void builderShouldSetAverageQueryTime() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().averageQueryTime(1.5).build();

      assertEquals(1.5, stats.getAverageQueryTime(), 0.001, "Average query time should be 1.5");
    }

    @Test
    @DisplayName("builder should set totalQueries")
    void builderShouldSetTotalQueries() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().totalQueries(10000L).build();

      assertEquals(10000L, stats.getTotalQueries(), "Total queries should be 10000");
    }

    @Test
    @DisplayName("builder should chain methods correctly")
    void builderShouldChainMethodsCorrectly() {
      final Instant now = Instant.now();
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder()
              .totalComponents(100)
              .activeComponents(75)
              .inactiveComponents(25)
              .totalMemoryUsage(1024 * 1024 * 100L)
              .averageComponentSize(1024 * 1024L)
              .availableInterfaces(Set.of("wasi:http", "wasi:filesystem"))
              .componentsByAuthor(Map.of("author1", 50, "author2", 50))
              .componentsByTag(Map.of("http", 30, "fs", 70))
              .lastRegistration(now)
              .lastUnregistration(now)
              .totalRegistrations(500L)
              .totalUnregistrations(100L)
              .averageQueryTime(2.5)
              .totalQueries(50000L)
              .build();

      assertEquals(100, stats.getTotalComponents(), "Total components should be 100");
      assertEquals(75, stats.getActiveComponents(), "Active components should be 75");
      assertEquals(25, stats.getInactiveComponents(), "Inactive components should be 25");
      assertEquals(1024 * 1024 * 100L, stats.getTotalMemoryUsage(), "Memory should be 100MB");
      assertEquals(1024 * 1024L, stats.getAverageComponentSize(), "Avg size should be 1MB");
      assertEquals(2, stats.getAvailableInterfaces().size(), "Should have 2 interfaces");
      assertEquals(2, stats.getComponentsByAuthor().size(), "Should have 2 authors");
      assertEquals(2, stats.getComponentsByTag().size(), "Should have 2 tags");
      assertEquals(now, stats.getLastRegistration(), "Last registration should match");
      assertEquals(now, stats.getLastUnregistration(), "Last unregistration should match");
      assertEquals(500L, stats.getTotalRegistrations(), "Total registrations should be 500");
      assertEquals(100L, stats.getTotalUnregistrations(), "Total unregistrations should be 100");
      assertEquals(2.5, stats.getAverageQueryTime(), 0.001, "Average query time should be 2.5");
      assertEquals(50000L, stats.getTotalQueries(), "Total queries should be 50000");
    }
  }

  @Nested
  @DisplayName("Value Tests")
  class ValueTests {

    @Test
    @DisplayName("should handle zero values")
    void shouldHandleZeroValues() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder()
              .totalComponents(0)
              .activeComponents(0)
              .inactiveComponents(0)
              .totalMemoryUsage(0L)
              .averageComponentSize(0L)
              .totalRegistrations(0L)
              .totalUnregistrations(0L)
              .averageQueryTime(0.0)
              .totalQueries(0L)
              .build();

      assertEquals(0, stats.getTotalComponents(), "Total should be 0");
      assertEquals(0, stats.getActiveComponents(), "Active should be 0");
      assertEquals(0, stats.getInactiveComponents(), "Inactive should be 0");
      assertEquals(0L, stats.getTotalMemoryUsage(), "Memory should be 0");
      assertEquals(0L, stats.getAverageComponentSize(), "Avg size should be 0");
      assertEquals(0L, stats.getTotalRegistrations(), "Registrations should be 0");
      assertEquals(0L, stats.getTotalUnregistrations(), "Unregistrations should be 0");
      assertEquals(0.0, stats.getAverageQueryTime(), 0.001, "Query time should be 0");
      assertEquals(0L, stats.getTotalQueries(), "Queries should be 0");
    }

    @Test
    @DisplayName("should handle max int values")
    void shouldHandleMaxIntValues() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder()
              .totalComponents(Integer.MAX_VALUE)
              .activeComponents(Integer.MAX_VALUE)
              .inactiveComponents(Integer.MAX_VALUE)
              .build();

      assertEquals(Integer.MAX_VALUE, stats.getTotalComponents(), "Should handle max int");
      assertEquals(Integer.MAX_VALUE, stats.getActiveComponents(), "Should handle max int");
      assertEquals(Integer.MAX_VALUE, stats.getInactiveComponents(), "Should handle max int");
    }

    @Test
    @DisplayName("should handle max long values")
    void shouldHandleMaxLongValues() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder()
              .totalMemoryUsage(Long.MAX_VALUE)
              .averageComponentSize(Long.MAX_VALUE)
              .totalRegistrations(Long.MAX_VALUE)
              .totalUnregistrations(Long.MAX_VALUE)
              .totalQueries(Long.MAX_VALUE)
              .build();

      assertEquals(Long.MAX_VALUE, stats.getTotalMemoryUsage(), "Should handle max long");
      assertEquals(Long.MAX_VALUE, stats.getAverageComponentSize(), "Should handle max long");
      assertEquals(Long.MAX_VALUE, stats.getTotalRegistrations(), "Should handle max long");
      assertEquals(Long.MAX_VALUE, stats.getTotalUnregistrations(), "Should handle max long");
      assertEquals(Long.MAX_VALUE, stats.getTotalQueries(), "Should handle max long");
    }

    @Test
    @DisplayName("should handle null collections gracefully")
    void shouldHandleNullCollectionsGracefully() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder()
              .availableInterfaces(null)
              .componentsByAuthor(null)
              .componentsByTag(null)
              .build();

      assertNotNull(stats.getAvailableInterfaces(), "Interfaces should not be null");
      assertNotNull(stats.getComponentsByAuthor(), "By author should not be null");
      assertNotNull(stats.getComponentsByTag(), "By tag should not be null");
      assertTrue(stats.getAvailableInterfaces().isEmpty(), "Interfaces should be empty");
      assertTrue(stats.getComponentsByAuthor().isEmpty(), "By author should be empty");
      assertTrue(stats.getComponentsByTag().isEmpty(), "By tag should be empty");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("availableInterfaces should be immutable")
    void availableInterfacesShouldBeImmutable() {
      final Set<String> interfaces = new java.util.HashSet<>();
      interfaces.add("wasi:http");
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().availableInterfaces(interfaces).build();

      // Modify original set
      interfaces.add("wasi:filesystem");

      // Stats should not be affected
      assertEquals(1, stats.getAvailableInterfaces().size(), "Should have original size");
      assertFalse(
          stats.getAvailableInterfaces().contains("wasi:filesystem"),
          "Should not contain added element");
    }

    @Test
    @DisplayName("componentsByAuthor should be immutable")
    void componentsByAuthorShouldBeImmutable() {
      final Map<String, Integer> byAuthor = new java.util.HashMap<>();
      byAuthor.put("author1", 5);
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().componentsByAuthor(byAuthor).build();

      // Modify original map
      byAuthor.put("author2", 10);

      // Stats should not be affected
      assertEquals(1, stats.getComponentsByAuthor().size(), "Should have original size");
      assertFalse(
          stats.getComponentsByAuthor().containsKey("author2"), "Should not contain added entry");
    }

    @Test
    @DisplayName("componentsByTag should be immutable")
    void componentsByTagShouldBeImmutable() {
      final Map<String, Integer> byTag = new java.util.HashMap<>();
      byTag.put("http", 10);
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder().componentsByTag(byTag).build();

      // Modify original map
      byTag.put("fs", 20);

      // Stats should not be affected
      assertEquals(1, stats.getComponentsByTag().size(), "Should have original size");
      assertFalse(stats.getComponentsByTag().containsKey("fs"), "Should not contain added entry");
    }
  }

  @Nested
  @DisplayName("Consistency Tests")
  class ConsistencyTests {

    @Test
    @DisplayName("active plus inactive should equal total in valid state")
    void activePlusInactiveShouldEqualTotalInValidState() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder()
              .totalComponents(100)
              .activeComponents(75)
              .inactiveComponents(25)
              .build();

      assertEquals(
          stats.getTotalComponents(),
          stats.getActiveComponents() + stats.getInactiveComponents(),
          "Active + inactive should equal total");
    }

    @Test
    @DisplayName("registrations minus unregistrations should match total")
    void registrationsMinusUnregistrationsShouldMatchTotal() {
      final ComponentRegistryStatistics stats =
          ComponentRegistryStatistics.builder()
              .totalComponents(400)
              .totalRegistrations(500L)
              .totalUnregistrations(100L)
              .build();

      assertEquals(
          stats.getTotalComponents(),
          stats.getTotalRegistrations() - stats.getTotalUnregistrations(),
          "Registrations - unregistrations should equal total components");
    }
  }
}
