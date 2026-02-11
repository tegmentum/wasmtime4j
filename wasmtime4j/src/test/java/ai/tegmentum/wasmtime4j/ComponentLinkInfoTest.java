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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLinkInfo} class.
 *
 * <p>ComponentLinkInfo provides information about linked components.
 */
@DisplayName("ComponentLinkInfo Tests")
class ComponentLinkInfoTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentLinkInfo.class.getModifiers()),
          "ComponentLinkInfo should be public");
      assertTrue(
          Modifier.isFinal(ComponentLinkInfo.class.getModifiers()),
          "ComponentLinkInfo should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final Component linkedComponent = createMockComponent("linked-comp", true);
      final List<Component> sourceComponents =
          List.of(
              createMockComponent("source1", true),
              createMockComponent("source2", true));

      final var linkInfo =
          new ComponentLinkInfo("link-123", sourceComponents, linkedComponent, true);

      assertEquals("link-123", linkInfo.getLinkId(), "Link ID should match");
      assertEquals(2, linkInfo.getSourceComponents().size(), "Should have 2 source components");
      assertEquals(linkedComponent, linkInfo.getLinkedComponent(), "Linked component should match");
    }

    @Test
    @DisplayName("should handle null link ID")
    void shouldHandleNullLinkId() {
      final List<Component> sourceComponents = List.of();
      final var linkInfo = new ComponentLinkInfo(null, sourceComponents, null, false);

      assertEquals("unknown", linkInfo.getLinkId(), "Null link ID should default to 'unknown'");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getLinkId should return correct value")
    void getLinkIdShouldReturnCorrectValue() {
      final var linkInfo = new ComponentLinkInfo("my-link-id", List.of(), null, false);

      assertEquals("my-link-id", linkInfo.getLinkId(), "Should return link ID");
    }

    @Test
    @DisplayName("getSourceComponents should return unmodifiable list")
    void getSourceComponentsShouldReturnUnmodifiableList() {
      final List<Component> sourceComponents =
          List.of(createMockComponent("source", true));
      final var linkInfo = new ComponentLinkInfo("link", sourceComponents, null, false);

      final List<Component> result = linkInfo.getSourceComponents();
      assertNotNull(result, "Should return list");
      assertEquals(1, result.size(), "Should have 1 source component");
    }

    @Test
    @DisplayName("getLinkedComponent should return correct value")
    void getLinkedComponentShouldReturnCorrectValue() {
      final Component linkedComponent = createMockComponent("linked", true);
      final var linkInfo = new ComponentLinkInfo("link", List.of(), linkedComponent, true);

      assertEquals(
          linkedComponent, linkInfo.getLinkedComponent(), "Should return linked component");
    }

    @Test
    @DisplayName("getSourceComponentCount should return correct count")
    void getSourceComponentCountShouldReturnCorrectCount() {
      final List<Component> sourceComponents =
          List.of(
              createMockComponent("s1", true),
              createMockComponent("s2", true),
              createMockComponent("s3", true));
      final var linkInfo = new ComponentLinkInfo("link", sourceComponents, null, false);

      assertEquals(3, linkInfo.getSourceComponentCount(), "Should return 3");
    }
  }

  @Nested
  @DisplayName("isActive Tests")
  class IsActiveTests {

    @Test
    @DisplayName("should return true when active and linked component is valid")
    void shouldReturnTrueWhenActiveAndLinkedComponentIsValid() {
      final Component linkedComponent = createMockComponent("linked", true);
      final var linkInfo = new ComponentLinkInfo("link", List.of(), linkedComponent, true);

      assertTrue(linkInfo.isActive(), "Should be active");
    }

    @Test
    @DisplayName("should return false when not active")
    void shouldReturnFalseWhenNotActive() {
      final Component linkedComponent = createMockComponent("linked", true);
      final var linkInfo = new ComponentLinkInfo("link", List.of(), linkedComponent, false);

      assertFalse(linkInfo.isActive(), "Should not be active when active flag is false");
    }

    @Test
    @DisplayName("should return false when linked component is null")
    void shouldReturnFalseWhenLinkedComponentIsNull() {
      final var linkInfo = new ComponentLinkInfo("link", List.of(), null, true);

      assertFalse(linkInfo.isActive(), "Should not be active when linked component is null");
    }

    @Test
    @DisplayName("should return false when linked component is not valid")
    void shouldReturnFalseWhenLinkedComponentIsNotValid() {
      final Component linkedComponent = createMockComponent("linked", false);
      final var linkInfo = new ComponentLinkInfo("link", List.of(), linkedComponent, true);

      assertFalse(linkInfo.isActive(), "Should not be active when linked component is not valid");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty source components list")
    void shouldHandleEmptySourceComponentsList() {
      final var linkInfo = new ComponentLinkInfo("link", List.of(), null, false);

      assertEquals(0, linkInfo.getSourceComponentCount(), "Should have 0 source components");
      assertTrue(linkInfo.getSourceComponents().isEmpty(), "Source components should be empty");
    }

    @Test
    @DisplayName("should handle many source components")
    void shouldHandleManySourceComponents() {
      final List<Component> sourceComponents =
          List.of(
              createMockComponent("s1", true),
              createMockComponent("s2", true),
              createMockComponent("s3", true),
              createMockComponent("s4", true),
              createMockComponent("s5", true));
      final var linkInfo = new ComponentLinkInfo("link", sourceComponents, null, false);

      assertEquals(5, linkInfo.getSourceComponentCount(), "Should have 5 source components");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final List<Component> sourceComponents = List.of(createMockComponent("s1", true));
      final var linkInfo = new ComponentLinkInfo("my-link", sourceComponents, null, true);

      final String result = linkInfo.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("my-link"), "Should contain link ID");
      assertTrue(result.contains("1"), "Should contain source component count");
    }
  }

  /**
   * Creates a mock Component for testing.
   *
   * @param id the component ID
   * @param valid whether the component is valid
   * @return a mock Component
   */
  private Component createMockComponent(final String id, final boolean valid) {
    return new Component() {
      @Override
      public String getId() {
        return id;
      }

      @Override
      public boolean isValid() {
        return valid;
      }

      @Override
      public void close() {
        // No-op for testing
      }

      @Override
      public ComponentVersion getVersion() {
        return null;
      }

      @Override
      public long getSize() {
        return 0;
      }

      @Override
      public ComponentMetadata getMetadata() {
        return null;
      }

      @Override
      public boolean exportsInterface(final String interfaceName) {
        return false;
      }

      @Override
      public boolean importsInterface(final String interfaceName) {
        return false;
      }

      @Override
      public java.util.Set<String> getExportedInterfaces() {
        return java.util.Set.of();
      }

      @Override
      public java.util.Set<String> getImportedInterfaces() {
        return java.util.Set.of();
      }

      @Override
      public ComponentInstance instantiate() {
        return null;
      }

      @Override
      public ComponentInstance instantiate(final ComponentInstanceConfig config) {
        return null;
      }

      @Override
      public ComponentDependencyGraph getDependencyGraph() {
        return null;
      }

      @Override
      public java.util.Set<Component> resolveDependencies(final ComponentRegistry registry) {
        return java.util.Set.of();
      }

      @Override
      public ComponentCompatibility checkCompatibility(final Component other) {
        return null;
      }

      @Override
      public WitInterfaceDefinition getWitInterface() {
        return null;
      }

      @Override
      public WitCompatibilityResult checkWitCompatibility(final Component other) {
        return null;
      }

      @Override
      public ComponentResourceUsage getResourceUsage() {
        return null;
      }

      @Override
      public ComponentLifecycleState getLifecycleState() {
        return ComponentLifecycleState.READY;
      }

      @Override
      public ComponentValidationResult validate(final ComponentValidationConfig validationConfig) {
        return ComponentValidationResult.success(
            new ComponentValidationResult.ValidationContext(id, null));
      }
    };
  }
}
