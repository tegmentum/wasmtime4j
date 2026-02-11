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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.DependencyResolution;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DependencyResolution} class.
 *
 * <p>DependencyResolution contains the result of dependency analysis for WebAssembly modules
 * including instantiation order, dependency edges, circular dependency detection, counts, timing,
 * and resolution status.
 */
@DisplayName("DependencyResolution Tests")
class DependencyResolutionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(DependencyResolution.class.getModifiers()),
          "DependencyResolution should be public");
      assertTrue(
          Modifier.isFinal(DependencyResolution.class.getModifiers()),
          "DependencyResolution should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should reject null instantiationOrder")
    void shouldRejectNullInstantiationOrder() {
      assertThrows(
          NullPointerException.class,
          () ->
              new DependencyResolution(
                  null,
                  Collections.emptyList(),
                  false,
                  Collections.emptyList(),
                  0,
                  0,
                  Duration.ofMillis(10),
                  true),
          "Null instantiationOrder should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null dependencies")
    void shouldRejectNullDependencies() {
      assertThrows(
          NullPointerException.class,
          () ->
              new DependencyResolution(
                  Collections.emptyList(),
                  null,
                  false,
                  Collections.emptyList(),
                  0,
                  0,
                  Duration.ofMillis(10),
                  true),
          "Null dependencies should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null circularDependencyChains")
    void shouldRejectNullCircularChains() {
      assertThrows(
          NullPointerException.class,
          () ->
              new DependencyResolution(
                  Collections.emptyList(),
                  Collections.emptyList(),
                  false,
                  null,
                  0,
                  0,
                  Duration.ofMillis(10),
                  true),
          "Null circularDependencyChains should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null analysisTime")
    void shouldRejectNullAnalysisTime() {
      assertThrows(
          NullPointerException.class,
          () ->
              new DependencyResolution(
                  Collections.emptyList(),
                  Collections.emptyList(),
                  false,
                  Collections.emptyList(),
                  0,
                  0,
                  null,
                  true),
          "Null analysisTime should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Successful Resolution Tests")
  class SuccessfulResolutionTests {

    @Test
    @DisplayName("should create successful resolution with empty collections")
    void shouldCreateSuccessfulResolutionWithEmptyCollections() {
      final DependencyResolution resolution =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              0,
              0,
              Duration.ofMillis(5),
              true);

      assertNotNull(resolution, "Resolution should not be null");
      assertTrue(resolution.getInstantiationOrder().isEmpty(), "Order should be empty");
      assertTrue(resolution.getDependencies().isEmpty(), "Dependencies should be empty");
      assertFalse(resolution.hasCircularDependencies(), "Should have no circular deps");
      assertTrue(
          resolution.getCircularDependencyChains().isEmpty(), "Circular chains should be empty");
      assertEquals(0, resolution.getTotalModules(), "totalModules should be 0");
      assertEquals(0, resolution.getResolvedDependencies(), "resolvedDependencies should be 0");
      assertEquals(
          Duration.ofMillis(5), resolution.getAnalysisTime(), "analysisTime should be 5ms");
      assertTrue(resolution.isResolutionSuccessful(), "Resolution should be successful");
    }
  }

  @Nested
  @DisplayName("Resolution with Circular Dependencies Tests")
  class CircularDependencyTests {

    @Test
    @DisplayName("should represent circular dependencies")
    void shouldRepresentCircularDependencies() {
      final List<String> chains = List.of("A -> B -> C -> A");
      final DependencyResolution resolution =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              true,
              chains,
              3,
              1,
              Duration.ofMillis(50),
              false);

      assertTrue(resolution.hasCircularDependencies(), "Should have circular deps");
      assertEquals(
          1, resolution.getCircularDependencyChains().size(), "Should have 1 circular chain");
      assertEquals(
          "A -> B -> C -> A",
          resolution.getCircularDependencyChains().get(0),
          "Chain should match");
      assertEquals(3, resolution.getTotalModules(), "totalModules should be 3");
      assertFalse(resolution.isResolutionSuccessful(), "Resolution should not be successful");
    }
  }

  @Nested
  @DisplayName("Resolution Rate Tests")
  class ResolutionRateTests {

    @Test
    @DisplayName("should return 100% for empty dependencies")
    void shouldReturn100ForEmptyDependencies() {
      final DependencyResolution resolution =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              0,
              0,
              Duration.ofMillis(1),
              true);

      assertEquals(
          100.0, resolution.getResolutionRate(), 0.001, "Empty dependencies should give 100% rate");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("getInstantiationOrder should return unmodifiable list")
    void getInstantiationOrderShouldBeUnmodifiable() {
      final DependencyResolution resolution =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              0,
              0,
              Duration.ofMillis(1),
              true);

      assertThrows(
          UnsupportedOperationException.class,
          () -> resolution.getInstantiationOrder().add(null),
          "Instantiation order should be unmodifiable");
    }

    @Test
    @DisplayName("getDependencies should return unmodifiable list")
    void getDependenciesShouldBeUnmodifiable() {
      final DependencyResolution resolution =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              0,
              0,
              Duration.ofMillis(1),
              true);

      assertThrows(
          UnsupportedOperationException.class,
          () -> resolution.getDependencies().add(null),
          "Dependencies should be unmodifiable");
    }

    @Test
    @DisplayName("getCircularDependencyChains should return unmodifiable list")
    void getCircularChainsShouldBeUnmodifiable() {
      final DependencyResolution resolution =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              0,
              0,
              Duration.ofMillis(1),
              true);

      assertThrows(
          UnsupportedOperationException.class,
          () -> resolution.getCircularDependencyChains().add("chain"),
          "Circular chains should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal resolutions should be equal")
    void equalResolutionsShouldBeEqual() {
      final DependencyResolution res1 =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              5,
              3,
              Duration.ofMillis(10),
              true);
      final DependencyResolution res2 =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              5,
              3,
              Duration.ofMillis(10),
              true);

      assertEquals(res1, res2, "Identical resolutions should be equal");
      assertEquals(res1.hashCode(), res2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("different resolutions should not be equal")
    void differentResolutionsShouldNotBeEqual() {
      final DependencyResolution res1 =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              5,
              3,
              Duration.ofMillis(10),
              true);
      final DependencyResolution res2 =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              true,
              List.of("A -> B -> A"),
              5,
              2,
              Duration.ofMillis(10),
              false);

      assertNotEquals(res1, res2, "Different resolutions should not be equal");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key information")
    void toStringShouldContainKeyInfo() {
      final DependencyResolution resolution =
          new DependencyResolution(
              Collections.emptyList(),
              Collections.emptyList(),
              false,
              Collections.emptyList(),
              3,
              2,
              Duration.ofMillis(50),
              true);

      final String result = resolution.toString();
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("DependencyResolution"), "toString should contain class name");
      assertTrue(result.contains("3"), "toString should contain totalModules");
      assertTrue(result.contains("true"), "toString should contain successful status");
    }
  }
}
