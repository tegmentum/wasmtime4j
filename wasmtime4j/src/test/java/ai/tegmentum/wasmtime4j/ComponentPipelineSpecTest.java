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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentPipelineSpec} class.
 *
 * <p>ComponentPipelineSpec defines the stages and connections in a component processing pipeline.
 */
@DisplayName("ComponentPipelineSpec Tests")
class ComponentPipelineSpecTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentPipelineSpec.class.getModifiers()),
          "ComponentPipelineSpec should be public");
      assertTrue(
          Modifier.isFinal(ComponentPipelineSpec.class.getModifiers()),
          "ComponentPipelineSpec should be final");
      assertFalse(
          ComponentPipelineSpec.class.isInterface(),
          "ComponentPipelineSpec should not be an interface");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("constructor should set all fields")
    void constructorShouldSetAllFields() {
      final List<String> stageNames = List.of("stage1", "stage2", "stage3");
      final ComponentPipelineConfig config = new ComponentPipelineConfig();
      final ComponentPipelineSpec spec =
          new ComponentPipelineSpec("test-pipeline", stageNames, config);

      assertEquals("test-pipeline", spec.getPipelineName(), "Pipeline name should match");
      assertEquals(3, spec.getStageNames().size(), "Should have 3 stages");
      assertEquals("stage1", spec.getStageNames().get(0), "First stage should match");
      assertEquals("stage2", spec.getStageNames().get(1), "Second stage should match");
      assertEquals("stage3", spec.getStageNames().get(2), "Third stage should match");
      assertNotNull(spec.getConfig(), "Config should not be null");
    }

    @Test
    @DisplayName("constructor should reject null pipeline name")
    void constructorShouldRejectNullPipelineName() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentPipelineSpec(null, List.of("stage1"), new ComponentPipelineConfig()),
          "Should throw for null pipelineName");
    }

    @Test
    @DisplayName("constructor should reject null stage names")
    void constructorShouldRejectNullStageNames() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentPipelineSpec("pipeline", null, new ComponentPipelineConfig()),
          "Should throw for null stageNames");
    }

    @Test
    @DisplayName("constructor should use default config when null provided")
    void constructorShouldUseDefaultConfigWhenNullProvided() {
      final ComponentPipelineSpec spec =
          new ComponentPipelineSpec("pipeline", List.of("stage1"), null);

      assertNotNull(spec.getConfig(), "Config should not be null");
      assertEquals(10, spec.getConfig().getMaxStages(), "Should use default maxStages");
    }

    @Test
    @DisplayName("constructor should create empty stage list")
    void constructorShouldCreateEmptyStageList() {
      final ComponentPipelineSpec spec =
          new ComponentPipelineSpec("pipeline", List.of(), new ComponentPipelineConfig());

      assertTrue(spec.getStageNames().isEmpty(), "Stage names should be empty");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getPipelineName method")
    void shouldHaveGetPipelineNameMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineSpec.class.getMethod("getPipelineName");
      assertNotNull(method, "getPipelineName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getStageNames method")
    void shouldHaveGetStageNamesMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineSpec.class.getMethod("getStageNames");
      assertNotNull(method, "getStageNames method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentPipelineSpec.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          ComponentPipelineConfig.class,
          method.getReturnType(),
          "Should return ComponentPipelineConfig");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("stageNames should be immutable")
    void stageNamesShouldBeImmutable() {
      final List<String> stageNames = new java.util.ArrayList<>();
      stageNames.add("stage1");
      stageNames.add("stage2");

      final ComponentPipelineSpec spec =
          new ComponentPipelineSpec("pipeline", stageNames, new ComponentPipelineConfig());

      // Modify original list
      stageNames.add("stage3");

      // Verify spec was not affected
      assertEquals(
          2,
          spec.getStageNames().size(),
          "Spec should not be affected by original list modification");

      // Verify returned list is immutable
      final List<String> returnedList = spec.getStageNames();
      assertThrows(
          UnsupportedOperationException.class,
          () -> returnedList.add("newStage"),
          "Returned list should be immutable");
    }

    @Test
    @DisplayName("defensive copy should be made on construction")
    void defensiveCopyShouldBeMadeOnConstruction() {
      final List<String> originalStages = new java.util.ArrayList<>();
      originalStages.add("original");

      final ComponentPipelineSpec spec =
          new ComponentPipelineSpec("pipeline", originalStages, new ComponentPipelineConfig());

      originalStages.clear();
      originalStages.add("modified");

      assertEquals(1, spec.getStageNames().size(), "Should have 1 stage");
      assertEquals("original", spec.getStageNames().get(0), "Should have original value");
    }
  }

  @Nested
  @DisplayName("Behavior Tests")
  class BehaviorTests {

    @Test
    @DisplayName("should preserve stage order")
    void shouldPreserveStageOrder() {
      final List<String> stageNames = List.of("first", "second", "third", "fourth");
      final ComponentPipelineSpec spec =
          new ComponentPipelineSpec("pipeline", stageNames, new ComponentPipelineConfig());

      assertEquals("first", spec.getStageNames().get(0), "First stage should be preserved");
      assertEquals("second", spec.getStageNames().get(1), "Second stage should be preserved");
      assertEquals("third", spec.getStageNames().get(2), "Third stage should be preserved");
      assertEquals("fourth", spec.getStageNames().get(3), "Fourth stage should be preserved");
    }

    @Test
    @DisplayName("should allow duplicate stage names")
    void shouldAllowDuplicateStageNames() {
      final List<String> stageNames = List.of("stage", "stage", "stage");
      final ComponentPipelineSpec spec =
          new ComponentPipelineSpec("pipeline", stageNames, new ComponentPipelineConfig());

      assertEquals(3, spec.getStageNames().size(), "Should have 3 stages");
      assertEquals("stage", spec.getStageNames().get(0), "All stages should be 'stage'");
      assertEquals("stage", spec.getStageNames().get(1), "All stages should be 'stage'");
      assertEquals("stage", spec.getStageNames().get(2), "All stages should be 'stage'");
    }
  }
}
