/*
 * Copyright 2024 Tegmentum AI
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

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for Component Model foundation validation.
 *
 * <p>This test suite validates the foundational Component Model support implemented
 * as part of Task #304, including WIT interface handling, component linking,
 * and component registry functionality.
 *
 * @since 1.0.0
 */
@DisplayName("Component Model Foundation")
class ComponentModelFoundationTest {

  @Nested
  @DisplayName("Component Registry Tests")
  class ComponentRegistryTests {

    private ComponentRegistry registry;
    private ComponentSimple testComponent;

    @BeforeEach
    void setUp() {
      registry = new BasicComponentRegistry();
      testComponent = createTestComponent("test-component-1", new ComponentVersion(1, 0, 0));
    }

    @Test
    @DisplayName("Should register and find component by ID")
    void shouldRegisterAndFindComponentById() throws WasmException {
      // Given a component
      final String componentId = testComponent.getId();

      // When registering the component
      registry.register(testComponent);

      // Then the component should be findable by ID
      assertTrue(registry.isRegistered(componentId));
      assertEquals(1, registry.getComponentCount());

      final var foundComponent = registry.findById(componentId);
      assertTrue(foundComponent.isPresent());
      assertEquals(testComponent, foundComponent.get());
    }

    @Test
    @DisplayName("Should register and find component by name")
    void shouldRegisterAndFindComponentByName() throws WasmException {
      // Given a component name
      final String componentName = "my-test-component";

      // When registering the component with a name
      registry.register(componentName, testComponent);

      // Then the component should be findable by name
      final var foundComponent = registry.findByName(componentName);
      assertTrue(foundComponent.isPresent());
      assertEquals(testComponent, foundComponent.get());

      // And also findable by ID
      final var foundById = registry.findById(testComponent.getId());
      assertTrue(foundById.isPresent());
      assertEquals(testComponent, foundById.get());
    }

    @Test
    @DisplayName("Should find components by version")
    void shouldFindComponentsByVersion() throws WasmException {
      // Given multiple components with different versions
      final ComponentVersion version1 = new ComponentVersion(1, 0, 0);
      final ComponentVersion version2 = new ComponentVersion(2, 0, 0);

      final ComponentSimple comp1 = createTestComponent("comp1", version1);
      final ComponentSimple comp2 = createTestComponent("comp2", version1);
      final ComponentSimple comp3 = createTestComponent("comp3", version2);

      // When registering the components
      registry.register(comp1);
      registry.register(comp2);
      registry.register(comp3);

      // Then should find components by version
      final List<ComponentSimple> v1Components = registry.findByVersion(version1);
      assertEquals(2, v1Components.size());
      assertTrue(v1Components.contains(comp1));
      assertTrue(v1Components.contains(comp2));

      final List<ComponentSimple> v2Components = registry.findByVersion(version2);
      assertEquals(1, v2Components.size());
      assertTrue(v2Components.contains(comp3));
    }

    @Test
    @DisplayName("Should handle component unregistration")
    void shouldHandleComponentUnregistration() throws WasmException {
      // Given a registered component
      registry.register("test-name", testComponent);
      assertTrue(registry.isRegistered(testComponent.getId()));

      // When unregistering the component
      registry.unregister(testComponent.getId());

      // Then the component should no longer be registered
      assertFalse(registry.isRegistered(testComponent.getId()));
      assertEquals(0, registry.getComponentCount());
      assertTrue(registry.findById(testComponent.getId()).isEmpty());
      assertTrue(registry.findByName("test-name").isEmpty());
    }

    @Test
    @DisplayName("Should provide registry statistics")
    void shouldProvideRegistryStatistics() throws WasmException {
      // Given multiple registered components
      final ComponentSimple comp1 = createTestComponent("comp1", new ComponentVersion(1, 0, 0));
      final ComponentSimple comp2 = createTestComponent("comp2", new ComponentVersion(1, 0, 0));
      final ComponentSimple comp3 = createTestComponent("comp3", new ComponentVersion(2, 0, 0));

      registry.register("named-comp1", comp1);
      registry.register(comp2); // Anonymous
      registry.register("named-comp3", comp3);

      // When getting statistics
      final ComponentRegistryStatistics stats = registry.getStatistics();

      // Then statistics should be correct
      assertEquals(3, stats.getTotalComponents());
      assertEquals(2, stats.getNamedComponents());
      assertEquals(1, stats.getAnonymousComponents());
      assertEquals(2, stats.getUniqueVersions());
    }

    @Test
    @DisplayName("Should validate registry consistency")
    void shouldValidateRegistryConsistency() throws WasmException {
      // Given a properly configured registry
      registry.register("test-component", testComponent);

      // When validating the registry
      assertDoesNotThrow(() -> registry.validateRegistry());

      // Then validation should pass
      assertTrue(registry.isRegistered(testComponent.getId()));
    }

    @Test
    @DisplayName("Should prevent duplicate registration")
    void shouldPreventDuplicateRegistration() throws WasmException {
      // Given a registered component
      registry.register(testComponent);

      // When attempting to register the same component again
      // Then should throw an exception
      assertThrows(WasmException.class, () -> registry.register(testComponent));
    }
  }

  @Nested
  @DisplayName("Component Linking Tests")
  class ComponentLinkingTests {

    private ComponentLinker linker;
    private ComponentSimple component1;
    private ComponentSimple component2;

    @BeforeEach
    void setUp() {
      linker = new BasicComponentLinker();
      component1 = createTestComponent("comp1", new ComponentVersion(1, 0, 0));
      component2 = createTestComponent("comp2", new ComponentVersion(1, 0, 0));
    }

    @Test
    @DisplayName("Should link components successfully")
    void shouldLinkComponentsSuccessfully() throws WasmException {
      // Given components to link
      final List<ComponentSimple> components = List.of(component1, component2);
      final ComponentLinkingConfig config = new ComponentLinkingConfig();

      // When linking components
      final ComponentSimple linkedComponent = linker.linkComponents(components, config);

      // Then should return a valid linked component
      assertNotNull(linkedComponent);
      assertTrue(linkedComponent.isValid());
      assertNotNull(linkedComponent.getId());
    }

    @Test
    @DisplayName("Should check component compatibility")
    void shouldCheckComponentCompatibility() {
      // Given two components
      final ComponentSimple source = createTestComponent("source", new ComponentVersion(1, 0, 0));
      final ComponentSimple target = createTestComponent("target", new ComponentVersion(1, 0, 0));

      // When checking compatibility
      final ComponentCompatibilityResult result = linker.checkCompatibility(source, target);

      // Then should return compatibility result
      assertNotNull(result);
      assertTrue(result.isCompatible()); // Same versions should be compatible
      assertNotNull(result.getDetails());
    }

    @Test
    @DisplayName("Should validate linking configuration")
    void shouldValidateLinkingConfiguration() {
      // Given a linking configuration
      final ComponentLinkingConfig config = new ComponentLinkingConfig();

      // When validating the configuration
      final LinkingValidationResult result = linker.validateLinking(config);

      // Then should return validation result
      assertNotNull(result);
      assertTrue(result.isValid());
      assertNotNull(result.getDetails());
    }

    @Test
    @DisplayName("Should provide linking statistics")
    void shouldProvideLinkingStatistics() throws WasmException {
      // Given some linking operations
      final List<ComponentSimple> components = List.of(component1, component2);
      final ComponentLinkingConfig config = new ComponentLinkingConfig();
      linker.linkComponents(components, config);

      // When getting statistics
      final ComponentLinkingStatistics stats = linker.getStatistics();

      // Then should provide accurate statistics
      assertNotNull(stats);
      assertEquals(1, stats.getTotalLinksCreated());
      assertEquals(0, stats.getTotalSwapsPerformed());
      assertTrue(stats.getActiveLinksCount() >= 0);
    }

    @Test
    @DisplayName("Should reject null components for linking")
    void shouldRejectNullComponentsForLinking() {
      // Given null components
      final ComponentLinkingConfig config = new ComponentLinkingConfig();

      // When attempting to link null components
      // Then should throw an exception
      assertThrows(WasmException.class, () -> linker.linkComponents(null, config));
    }

    @Test
    @DisplayName("Should reject invalid linking configuration")
    void shouldRejectInvalidLinkingConfiguration() {
      // Given components but null configuration
      final List<ComponentSimple> components = List.of(component1, component2);

      // When attempting to link with null configuration
      // Then should throw an exception
      assertThrows(WasmException.class, () -> linker.linkComponents(components, null));
    }
  }

  @Nested
  @DisplayName("WIT Interface Tests")
  class WitInterfaceTests {

    @Test
    @DisplayName("Should validate WIT interface definition")
    void shouldValidateWitInterfaceDefinition() throws WasmException {
      // Given a test component with WIT interface
      final ComponentSimple component = createTestComponent("wit-test", new ComponentVersion(1, 0, 0));

      // When getting the WIT interface
      final WitInterfaceDefinition witInterface = component.getWitInterface();

      // Then should have valid WIT interface
      assertNotNull(witInterface);
      assertNotNull(witInterface.getName());
      assertNotNull(witInterface.getVersion());
      assertNotNull(witInterface.getPackageName());
      assertNotNull(witInterface.getFunctionNames());
      assertNotNull(witInterface.getTypeNames());
    }

    @Test
    @DisplayName("Should check WIT compatibility between components")
    void shouldCheckWitCompatibilityBetweenComponents() throws WasmException {
      // Given two components
      final ComponentSimple comp1 = createTestComponent("comp1", new ComponentVersion(1, 0, 0));
      final ComponentSimple comp2 = createTestComponent("comp2", new ComponentVersion(1, 0, 0));

      // When checking WIT compatibility
      final WitCompatibilityResult result = comp1.checkWitCompatibility(comp2);

      // Then should return compatibility result
      assertNotNull(result);
      // Note: Basic implementation may not be fully compatible, but should not throw
    }

    @Test
    @DisplayName("Should generate WIT text representation")
    void shouldGenerateWitTextRepresentation() throws WasmException {
      // Given a component with WIT interface
      final ComponentSimple component = createTestComponent("wit-text-test", new ComponentVersion(1, 0, 0));
      final WitInterfaceDefinition witInterface = component.getWitInterface();

      // When getting WIT text representation
      final String witText = witInterface.getWitText();

      // Then should have valid WIT text
      assertNotNull(witText);
      assertFalse(witText.trim().isEmpty());
      assertTrue(witText.contains("interface"));
    }

    @Test
    @DisplayName("Should handle WIT interface dependencies")
    void shouldHandleWitInterfaceDependencies() throws WasmException {
      // Given a component with WIT interface
      final ComponentSimple component = createTestComponent("dependency-test", new ComponentVersion(1, 0, 0));
      final WitInterfaceDefinition witInterface = component.getWitInterface();

      // When getting dependencies
      final Set<String> dependencies = witInterface.getDependencies();

      // Then should return dependency set (may be empty for basic implementation)
      assertNotNull(dependencies);
    }
  }

  @Nested
  @DisplayName("Component Resource Management Tests")
  class ComponentResourceManagementTests {

    private ComponentResourceManager resourceManager;

    @BeforeEach
    void setUp() {
      resourceManager = new BasicComponentResourceManager();
    }

    @Test
    @DisplayName("Should allocate resources for component")
    void shouldAllocateResourcesForComponent() throws WasmException {
      // Given a component and resource specification
      final String componentId = "resource-test-component";
      final ResourceSpecification resourceSpec = new ResourceSpecification(512, 2);

      // When allocating resources
      final ResourceAllocationResult result = resourceManager.allocateResources(componentId, resourceSpec);

      // Then should successfully allocate resources
      assertNotNull(result);
      assertTrue(result.isSuccessful());
      assertNotNull(result.getMessage());
    }

    @Test
    @DisplayName("Should track resource usage")
    void shouldTrackResourceUsage() throws WasmException {
      // Given a component with allocated resources
      final String componentId = "usage-test-component";
      final ResourceSpecification resourceSpec = new ResourceSpecification(256, 1);
      resourceManager.allocateResources(componentId, resourceSpec);

      // When getting resource usage
      final ComponentResourceUsage usage = resourceManager.getResourceUsage(componentId);

      // Then should provide usage information
      assertNotNull(usage);
      assertEquals(componentId, usage.getComponentId());
    }

    @Test
    @DisplayName("Should deallocate resources")
    void shouldDeallocateResources() throws WasmException {
      // Given a component with allocated resources
      final String componentId = "deallocation-test-component";
      final ResourceSpecification resourceSpec = new ResourceSpecification(128, 1);
      resourceManager.allocateResources(componentId, resourceSpec);

      // When deallocating resources
      assertDoesNotThrow(() -> resourceManager.deallocateResources(componentId));

      // Then deallocation should succeed
      // (Basic implementation doesn't throw on non-existent components)
    }

    @Test
    @DisplayName("Should create shared resources")
    void shouldCreateSharedResources() throws WasmException {
      // Given shared resource parameters
      final String resourceId = "shared-resource-1";
      final SharedResourceType resourceType = SharedResourceType.MEMORY;
      final SharedResourceConfig config = new SharedResourceConfig();

      // When creating shared resource
      final SharedResourceHandle handle = resourceManager.createSharedResource(
          resourceId, resourceType, config);

      // Then should create valid shared resource
      assertNotNull(handle);
      assertEquals(resourceId, handle.getResourceId());
      assertEquals(resourceType, handle.getResourceType());
      assertTrue(handle.isValid());
    }

    @Test
    @DisplayName("Should check resource health")
    void shouldCheckResourceHealth() {
      // When checking resource health
      final ResourceHealthStatus health = resourceManager.checkResourceHealth();

      // Then should return health status
      assertNotNull(health);
      assertTrue(health.isHealthy()); // Basic implementation reports healthy
      assertNotNull(health.getMessage());
    }
  }

  /**
   * Creates a test component for testing purposes.
   *
   * @param id the component ID
   * @param version the component version
   * @return a test component
   */
  private ComponentSimple createTestComponent(final String id, final ComponentVersion version) {
    return new TestComponentSimple(id, version);
  }

  /**
   * Simple test implementation of ComponentSimple for testing purposes.
   */
  private static class TestComponentSimple implements ComponentSimple {
    private final String id;
    private final ComponentVersion version;
    private final ComponentMetadata metadata;
    private boolean valid = true;

    TestComponentSimple(final String id, final ComponentVersion version) {
      this.id = id;
      this.version = version;
      this.metadata = new ComponentMetadata(id, version, "Test Component");
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public ComponentVersion getVersion() {
      return version;
    }

    @Override
    public long getSize() {
      return 1024; // Fixed size for testing
    }

    @Override
    public ComponentMetadata getMetadata() {
      return metadata;
    }

    @Override
    public boolean exportsInterface(final String interfaceName) {
      return "test-export".equals(interfaceName);
    }

    @Override
    public boolean importsInterface(final String interfaceName) {
      return "test-import".equals(interfaceName);
    }

    @Override
    public Set<String> getExportedInterfaces() {
      return Set.of("test-export");
    }

    @Override
    public Set<String> getImportedInterfaces() {
      return Set.of("test-import");
    }

    @Override
    public ComponentInstance instantiate() throws WasmException {
      throw new UnsupportedOperationException("Test component instantiation not implemented");
    }

    @Override
    public ComponentInstance instantiate(final ComponentInstanceConfig config) throws WasmException {
      throw new UnsupportedOperationException("Test component instantiation not implemented");
    }

    @Override
    public ComponentDependencyGraph getDependencyGraph() {
      return new ComponentDependencyGraph(this);
    }

    @Override
    public Set<ComponentSimple> resolveDependencies(final ComponentRegistry registry) {
      return Set.of(); // No dependencies for test components
    }

    @Override
    public ComponentCompatibility checkCompatibility(final ComponentSimple other) {
      final boolean compatible = this.version.isCompatibleWith(other.getVersion());
      return new ComponentCompatibility(compatible, compatible ? "Compatible" : "Incompatible");
    }

    @Override
    public WitInterfaceDefinition getWitInterface() throws WasmException {
      return new TestWitInterfaceDefinition(id);
    }

    @Override
    public WitCompatibilityResult checkWitCompatibility(final ComponentSimple other) throws WasmException {
      return getWitInterface().isCompatibleWith(other.getWitInterface());
    }

    @Override
    public ComponentResourceUsage getResourceUsage() {
      return new ComponentResourceUsage(id);
    }

    @Override
    public ComponentLifecycleState getLifecycleState() {
      return ComponentLifecycleState.ACTIVE;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public ComponentValidationResult validate(final ComponentValidationConfig validationConfig) {
      final ComponentValidationResult.ValidationContext context =
          new ComponentValidationResult.ValidationContext(id, version);
      return ComponentValidationResult.success(context);
    }

    @Override
    public void close() {
      valid = false;
    }
  }

  /**
   * Test implementation of WitInterfaceDefinition.
   */
  private static class TestWitInterfaceDefinition implements WitInterfaceDefinition {
    private final String name;

    TestWitInterfaceDefinition(final String name) {
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
      return List.of("test-function");
    }

    @Override
    public List<String> getTypeNames() {
      return List.of("test-type");
    }

    @Override
    public Set<String> getDependencies() {
      return Set.of();
    }

    @Override
    public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
      if (other == null) {
        return WitCompatibilityResult.incompatible("Other interface is null", Set.of());
      }
      final boolean compatible = this.name.equals(other.getName());
      return compatible ?
          WitCompatibilityResult.compatible("Test compatibility", Set.of()) :
          WitCompatibilityResult.incompatible("Names don't match", Set.of());
    }

    @Override
    public String getWitText() {
      return "interface " + name + " {\n  test-function() -> ();\n}";
    }

    @Override
    public List<String> getImportNames() {
      return List.of("test-import");
    }

    @Override
    public List<String> getExportNames() {
      return List.of("test-export");
    }
  }
}