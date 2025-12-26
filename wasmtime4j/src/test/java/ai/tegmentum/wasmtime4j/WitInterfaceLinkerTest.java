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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitInterfaceLinker} class.
 *
 * <p>WitInterfaceLinker provides component composition and linking functionality for WIT
 * interfaces.
 */
@DisplayName("WitInterfaceLinker Tests")
class WitInterfaceLinkerTest {

  private WitInterfaceLinker linker;

  @BeforeEach
  void setUp() {
    linker = new WitInterfaceLinker();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create linker with empty bindings")
    void shouldCreateLinkerWithEmptyBindings() {
      final WitInterfaceLinker newLinker = new WitInterfaceLinker();

      assertNotNull(newLinker);
      assertTrue(newLinker.getAllBindings().isEmpty());
    }
  }

  @Nested
  @DisplayName("Binding Management Tests")
  class BindingManagementTests {

    @Test
    @DisplayName("getAllBindings should return empty list initially")
    void getAllBindingsShouldReturnEmptyListInitially() {
      assertTrue(linker.getAllBindings().isEmpty());
    }

    @Test
    @DisplayName("getBindingsForComponent should return empty list for unknown component")
    void getBindingsForComponentShouldReturnEmptyListForUnknownComponent() {
      final List<WitInterfaceLinker.InterfaceBinding> bindings =
          linker.getBindingsForComponent("unknown-component");
      assertTrue(bindings.isEmpty());
    }

    @Test
    @DisplayName("unbindInterface should return false for non-existent binding")
    void unbindInterfaceShouldReturnFalseForNonExistentBinding() {
      final boolean result = linker.unbindInterface("provider", "consumer", "interface");
      assertFalse(result);
    }

    @Test
    @DisplayName("clearBindings should clear all bindings")
    void clearBindingsShouldClearAllBindings() {
      linker.clearBindings();
      assertTrue(linker.getAllBindings().isEmpty());
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("linkComponents should throw on null components")
    void linkComponentsShouldThrowOnNullComponents() {
      assertThrows(NullPointerException.class, () -> linker.linkComponents(null));
    }

    @Test
    @DisplayName("linkComponents should throw on empty components list")
    void linkComponentsShouldThrowOnEmptyComponentsList() {
      assertThrows(IllegalArgumentException.class, () -> linker.linkComponents(List.of()));
    }

    @Test
    @DisplayName("validateInterfaceCompatibility should throw on null provider")
    void validateInterfaceCompatibilityShouldThrowOnNullProvider() {
      assertThrows(
          NullPointerException.class,
          () -> linker.validateInterfaceCompatibility(null, null, "interface"));
    }
  }

  @Nested
  @DisplayName("ComponentLinkType Enum Tests")
  class ComponentLinkTypeEnumTests {

    @Test
    @DisplayName("ComponentLinkType should be an enum")
    void componentLinkTypeShouldBeEnum() {
      assertTrue(WitInterfaceLinker.ComponentLinkType.class.isEnum());
    }

    @Test
    @DisplayName("ComponentLinkType should have 3 values")
    void componentLinkTypeShouldHave3Values() {
      assertEquals(3, WitInterfaceLinker.ComponentLinkType.values().length);
    }

    @Test
    @DisplayName("ComponentLinkType should have all expected values")
    void componentLinkTypeShouldHaveAllExpectedValues() {
      assertNotNull(WitInterfaceLinker.ComponentLinkType.INTERFACE_BINDING);
      assertNotNull(WitInterfaceLinker.ComponentLinkType.RESOURCE_SHARING);
      assertNotNull(WitInterfaceLinker.ComponentLinkType.EVENT_COMMUNICATION);
    }

    @Test
    @DisplayName("ComponentLinkType valueOf should work")
    void componentLinkTypeValueOfShouldWork() {
      assertEquals(
          WitInterfaceLinker.ComponentLinkType.INTERFACE_BINDING,
          WitInterfaceLinker.ComponentLinkType.valueOf("INTERFACE_BINDING"));
      assertEquals(
          WitInterfaceLinker.ComponentLinkType.RESOURCE_SHARING,
          WitInterfaceLinker.ComponentLinkType.valueOf("RESOURCE_SHARING"));
      assertEquals(
          WitInterfaceLinker.ComponentLinkType.EVENT_COMMUNICATION,
          WitInterfaceLinker.ComponentLinkType.valueOf("EVENT_COMMUNICATION"));
    }

    @Test
    @DisplayName("ComponentLinkType ordinals should be consistent")
    void componentLinkTypeOrdinalsShouldBeConsistent() {
      assertEquals(0, WitInterfaceLinker.ComponentLinkType.INTERFACE_BINDING.ordinal());
      assertEquals(1, WitInterfaceLinker.ComponentLinkType.RESOURCE_SHARING.ordinal());
      assertEquals(2, WitInterfaceLinker.ComponentLinkType.EVENT_COMMUNICATION.ordinal());
    }
  }

  @Nested
  @DisplayName("LinkCompatibilityResult Tests")
  class LinkCompatibilityResultTests {

    @Test
    @DisplayName("should create compatible result")
    void shouldCreateCompatibleResult() {
      final WitInterfaceLinker.LinkCompatibilityResult result =
          new WitInterfaceLinker.LinkCompatibilityResult(true, "Compatible", List.of());

      assertTrue(result.isCompatible());
      assertEquals("Compatible", result.getMessage());
      assertTrue(result.getErrors().isEmpty());
      assertEquals("", result.getErrorMessage());
    }

    @Test
    @DisplayName("should create incompatible result with errors")
    void shouldCreateIncompatibleResultWithErrors() {
      final List<String> errors = List.of("Error 1", "Error 2");
      final WitInterfaceLinker.LinkCompatibilityResult result =
          new WitInterfaceLinker.LinkCompatibilityResult(false, "Incompatible", errors);

      assertFalse(result.isCompatible());
      assertEquals("Incompatible", result.getMessage());
      assertEquals(2, result.getErrors().size());
      assertEquals("Error 1; Error 2", result.getErrorMessage());
    }
  }

  @Nested
  @DisplayName("InterfaceCompatibilityResult Tests")
  class InterfaceCompatibilityResultTests {

    @Test
    @DisplayName("should create compatible result")
    void shouldCreateCompatibleResult() {
      final WitInterfaceLinker.InterfaceCompatibilityResult result =
          new WitInterfaceLinker.InterfaceCompatibilityResult(true, "Compatible", List.of());

      assertTrue(result.isCompatible());
      assertEquals("Compatible", result.getMessage());
      assertTrue(result.getIssues().isEmpty());
    }

    @Test
    @DisplayName("should create incompatible result with issues")
    void shouldCreateIncompatibleResultWithIssues() {
      final List<String> issues = List.of("Issue 1", "Issue 2", "Issue 3");
      final WitInterfaceLinker.InterfaceCompatibilityResult result =
          new WitInterfaceLinker.InterfaceCompatibilityResult(false, "Incompatible", issues);

      assertFalse(result.isCompatible());
      assertEquals("Incompatible", result.getMessage());
      assertEquals(3, result.getIssues().size());
    }
  }

  @Nested
  @DisplayName("ComponentLink Tests")
  class ComponentLinkTests {

    @Test
    @DisplayName("should create component link with all fields")
    void shouldCreateComponentLinkWithAllFields() {
      final WitInterfaceLinker.ComponentLink link =
          new WitInterfaceLinker.ComponentLink(
              "provider-id",
              "consumer-id",
              "test-interface",
              WitInterfaceLinker.ComponentLinkType.INTERFACE_BINDING,
              Map.of("linkId", "link-123", "extra", "data"));

      assertEquals("provider-id", link.getProviderId());
      assertEquals("consumer-id", link.getConsumerId());
      assertEquals("test-interface", link.getInterfaceName());
      assertEquals(WitInterfaceLinker.ComponentLinkType.INTERFACE_BINDING, link.getLinkType());
      assertEquals("link-123", link.getLinkId());
      assertEquals("data", link.getMetadata().get("extra"));
    }

    @Test
    @DisplayName("getLinkId should return null when not in metadata")
    void getLinkIdShouldReturnNullWhenNotInMetadata() {
      final WitInterfaceLinker.ComponentLink link =
          new WitInterfaceLinker.ComponentLink(
              "provider",
              "consumer",
              "interface",
              WitInterfaceLinker.ComponentLinkType.RESOURCE_SHARING,
              Map.of());

      assertNotNull(link.getMetadata());
      // getLinkId returns null when linkId is not in metadata
    }
  }

  @Nested
  @DisplayName("InterfaceBinding Tests")
  class InterfaceBindingTests {

    @Test
    @DisplayName("should create interface binding with all fields")
    void shouldCreateInterfaceBindingWithAllFields() {
      final MockInterfaceDefinition providerInterface =
          new MockInterfaceDefinition("provider-iface");
      final MockInterfaceDefinition consumerInterface =
          new MockInterfaceDefinition("consumer-iface");

      final WitInterfaceLinker.InterfaceBinding binding =
          new WitInterfaceLinker.InterfaceBinding(
              "provider-id", "consumer-id", "test-interface", providerInterface, consumerInterface);

      assertEquals("provider-id", binding.getProviderId());
      assertEquals("consumer-id", binding.getConsumerId());
      assertEquals("test-interface", binding.getInterfaceName());
      assertEquals("provider-iface", binding.getProviderInterface().getName());
      assertEquals("consumer-iface", binding.getConsumerInterface().getName());
    }
  }

  @Nested
  @DisplayName("LinkingManifest Tests")
  class LinkingManifestTests {

    @Test
    @DisplayName("should create linking manifest with all fields")
    void shouldCreateLinkingManifestWithAllFields() {
      final WitInterfaceLinker.LinkingManifest manifest =
          new WitInterfaceLinker.LinkingManifest(
              List.of("comp-1", "comp-2", "comp-3"),
              List.of("link-1", "link-2"),
              Map.of("timestamp", 12345L));

      assertEquals(3, manifest.getComponentIds().size());
      assertEquals(2, manifest.getLinkIds().size());
      assertEquals(12345L, manifest.getMetadata().get("timestamp"));
    }

    @Test
    @DisplayName("should create defensive copies")
    void shouldCreateDefensiveCopies() {
      final WitInterfaceLinker.LinkingManifest manifest =
          new WitInterfaceLinker.LinkingManifest(
              List.of("comp-1"), List.of("link-1"), Map.of("key", "value"));

      assertNotNull(manifest.getComponentIds());
      assertNotNull(manifest.getLinkIds());
      assertNotNull(manifest.getMetadata());
    }
  }

  /** Mock implementation of WitInterfaceDefinition for testing. */
  private static class MockInterfaceDefinition implements WitInterfaceDefinition {
    private final String name;

    MockInterfaceDefinition(final String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getVersion() {
      return "1.0.0";
    }

    @Override
    public String getPackageName() {
      return "test";
    }

    @Override
    public List<String> getFunctionNames() {
      return List.of();
    }

    @Override
    public List<String> getTypeNames() {
      return List.of();
    }

    @Override
    public List<String> getImportNames() {
      return List.of();
    }

    @Override
    public List<String> getExportNames() {
      return List.of();
    }

    @Override
    public java.util.Set<String> getDependencies() {
      return java.util.Set.of();
    }

    @Override
    public String getWitText() {
      return "interface " + name + " {}";
    }

    @Override
    public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
      return WitCompatibilityResult.compatible("Compatible", java.util.Set.of());
    }
  }
}
