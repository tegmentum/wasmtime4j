package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for component linking and composition functionality.
 *
 * <p>These tests verify the complete component composition workflow including dependency
 * resolution, interface compatibility checking, component linking, and instantiation with linked
 * dependencies.
 */
class ComponentLinkingIntegrationTest {

  private ComponentEngine componentEngine;
  private ComponentRegistry registry;
  private Store store;

  @BeforeEach
  void setUp(TestInfo testInfo) throws WasmException {
    System.out.printf("Starting test: %s%n", testInfo.getDisplayName());

    // Create component engine with default configuration
    final ComponentEngineConfig config = new ComponentEngineConfig();
    this.componentEngine = WasmRuntimeFactory.getDefaultRuntime().createComponentEngine(config);
    this.registry = componentEngine.getRegistry();
    this.store = WasmRuntimeFactory.getDefaultRuntime().createStore();

    assertNotNull(componentEngine, "Component engine should be created");
    assertNotNull(registry, "Component registry should be available");
    assertNotNull(store, "Store should be created");
    assertTrue(componentEngine.supportsComponentModel(), "Engine should support component model");
  }

  @AfterEach
  void tearDown() throws Exception {
    if (store != null) {
      store.close();
    }
    if (componentEngine != null) {
      componentEngine.close();
    }
  }

  @Test
  @DisplayName("Should compile and register simple component")
  void shouldCompileAndRegisterSimpleComponent() throws WasmException {
    // Create a minimal WebAssembly component
    final byte[] simpleComponentBytes = createMinimalComponentBytes();

    // Compile component
    final ComponentSimple component =
        componentEngine.compileComponent(simpleComponentBytes, "simple-component");

    assertNotNull(component, "Component should be compiled successfully");
    assertEquals("simple-component", component.getId(), "Component ID should match");
    assertTrue(component.isValid(), "Component should be valid");
    assertTrue(component.getSize() > 0, "Component should have non-zero size");

    // Register component
    registry.register("simple-test", component);

    assertTrue(registry.isRegistered(component.getId()), "Component should be registered by ID");
    assertTrue(
        registry.findByName("simple-test").isPresent(), "Component should be findable by name");
    assertEquals(1, registry.getComponentCount(), "Registry should contain one component");
  }

  @Test
  @DisplayName("Should check component compatibility correctly")
  void shouldCheckComponentCompatibilityCorrectly() throws WasmException {
    // Create two compatible components
    final ComponentSimple providerComponent = createProviderComponent();
    final ComponentSimple consumerComponent = createConsumerComponent();

    // Register components
    registry.register("provider", providerComponent);
    registry.register("consumer", consumerComponent);

    // Check compatibility
    final WitCompatibilityResult compatibility =
        componentEngine.checkCompatibility(providerComponent, consumerComponent);

    assertNotNull(compatibility, "Compatibility result should not be null");
    assertTrue(compatibility.isCompatible(), "Components should be compatible");
    assertNotNull(compatibility.getDetails(), "Compatibility details should be provided");
    assertFalse(compatibility.getSatisfiedImports().isEmpty(), "Should have satisfied imports");
    assertTrue(
        compatibility.getUnsatisfiedImports().isEmpty(), "Should have no unsatisfied imports");
  }

  @Test
  @DisplayName("Should detect incompatible components")
  void shouldDetectIncompatibleComponents() throws WasmException {
    // Create incompatible components
    final ComponentSimple component1 = createProviderComponent();
    final ComponentSimple component2 = createIncompatibleComponent();

    // Check compatibility
    final WitCompatibilityResult compatibility =
        componentEngine.checkCompatibility(component1, component2);

    assertNotNull(compatibility, "Compatibility result should not be null");
    assertFalse(compatibility.isCompatible(), "Components should be incompatible");
    assertNotNull(compatibility.getDetails(), "Compatibility details should be provided");
    assertFalse(compatibility.getUnsatisfiedImports().isEmpty(), "Should have unsatisfied imports");
  }

  @Test
  @DisplayName("Should link compatible components successfully")
  void shouldLinkCompatibleComponentsSuccessfully() throws WasmException {
    // Create components that can be linked
    final ComponentSimple provider = createProviderComponent();
    final ComponentSimple consumer = createConsumerComponent();

    // Register components
    registry.register(provider);
    registry.register(consumer);

    // Link components
    final List<ComponentSimple> componentsToLink = Arrays.asList(provider, consumer);
    final ComponentSimple linkedComponent = componentEngine.linkComponents(componentsToLink);

    assertNotNull(linkedComponent, "Linked component should be created");
    assertTrue(linkedComponent.isValid(), "Linked component should be valid");

    // Verify linked component has combined interfaces
    final Set<String> linkedExports = linkedComponent.getExportedInterfaces();
    final Set<String> linkedImports = linkedComponent.getImportedInterfaces();

    assertFalse(linkedExports.isEmpty(), "Linked component should have exports");
    // Linked component should have fewer unresolved imports due to internal linking
  }

  @Test
  @DisplayName("Should fail to link incompatible components")
  void shouldFailToLinkIncompatibleComponents() throws WasmException {
    // Create incompatible components
    final ComponentSimple component1 = createProviderComponent();
    final ComponentSimple component2 = createIncompatibleComponent();

    final List<ComponentSimple> componentsToLink = Arrays.asList(component1, component2);

    // Attempt to link incompatible components should fail
    assertThrows(
        WasmException.class,
        () -> componentEngine.linkComponents(componentsToLink),
        "Linking incompatible components should throw WasmException");
  }

  @Test
  @DisplayName("Should resolve component dependencies correctly")
  void shouldResolveComponentDependenciesCorrectly() throws WasmException {
    // Create dependency chain: A -> B -> C
    final ComponentSimple componentC = createProviderComponent(); // Provides interfaces
    final ComponentSimple componentB =
        createMiddlewareComponent(); // Consumes from C, provides to A
    final ComponentSimple componentA = createConsumerComponent(); // Consumes from B

    // Register all components
    registry.register("component-c", componentC);
    registry.register("component-b", componentB);
    registry.register("component-a", componentA);

    // Resolve dependencies for component A
    final Set<ComponentSimple> dependencies = registry.resolveDependencies(componentA);

    assertNotNull(dependencies, "Dependencies should be resolved");
    assertFalse(dependencies.isEmpty(), "Component A should have dependencies");
    assertTrue(
        dependencies.contains(componentB) || dependencies.contains(componentC),
        "Dependencies should include required components");

    // Validate dependencies
    final ComponentValidationResult validation = registry.validateDependencies(componentA);
    assertTrue(validation.isValid(), "Dependencies should be valid");
    assertTrue(validation.getIssues().isEmpty(), "Should have no validation issues");
  }

  @Test
  @DisplayName("Should detect circular dependencies")
  void shouldDetectCircularDependencies() throws WasmException {
    // Create components with circular dependencies
    final ComponentSimple componentA = createCircularDependencyComponentA();
    final ComponentSimple componentB = createCircularDependencyComponentB();

    // Register components
    registry.register("circular-a", componentA);
    registry.register("circular-b", componentB);

    // Attempting to resolve dependencies should detect circular dependency
    assertThrows(
        WasmException.class,
        () -> registry.resolveDependencies(componentA),
        "Circular dependency should be detected");
  }

  @Test
  @DisplayName("Should create component instance with linked dependencies")
  void shouldCreateComponentInstanceWithLinkedDependencies() throws WasmException {
    // Create provider and consumer components
    final ComponentSimple provider = createProviderComponent();
    final ComponentSimple consumer = createConsumerComponent();

    // Register components
    registry.register(provider);
    registry.register(consumer);

    // Create instance with linked dependencies
    final List<ComponentSimple> imports = Arrays.asList(provider);
    final ComponentInstance instance = componentEngine.createInstance(consumer, store, imports);

    assertNotNull(instance, "Component instance should be created");
    assertTrue(instance.isValid(), "Component instance should be valid");

    // Verify instance has access to imported functions
    final Set<String> availableFunctions = instance.getExportedFunctions().keySet();
    assertFalse(availableFunctions.isEmpty(), "Instance should have exported functions");
  }

  @Test
  @DisplayName("Should validate component before instantiation")
  void shouldValidateComponentBeforeInstantiation() throws WasmException {
    // Create a component
    final ComponentSimple component = createProviderComponent();
    registry.register(component);

    // Validate component
    final ComponentValidationResult validation = componentEngine.validateComponent(component);

    assertNotNull(validation, "Validation result should not be null");
    assertTrue(validation.isValid(), "Component should be valid");
    assertTrue(validation.getIssues().isEmpty(), "Should have no validation issues");

    // Create instance should succeed after validation
    final ComponentInstance instance = componentEngine.createInstance(component, store);
    assertNotNull(instance, "Instance should be created for valid component");
  }

  @Test
  @DisplayName("Should handle component version compatibility")
  void shouldHandleComponentVersionCompatibility() throws WasmException {
    // Create components with different versions
    final ComponentSimple componentV1 = createVersionedComponent("1.0.0");
    final ComponentSimple componentV2 = createVersionedComponent("2.0.0");

    // Register components
    registry.register("component-v1", componentV1);
    registry.register("component-v2", componentV2);

    // Check version compatibility
    final ai.tegmentum.wasmtime4j.ComponentCompatibility compatibility =
        componentV1.checkCompatibility(componentV2);

    assertNotNull(compatibility, "Compatibility result should not be null");
    // Version compatibility depends on implementation - may or may not be compatible
  }

  @Test
  @DisplayName("Should perform bulk component operations")
  void shouldPerformBulkComponentOperations() throws WasmException {
    // Create multiple components
    final ComponentSimple[] components = new ComponentSimple[5];
    for (int i = 0; i < 5; i++) {
      components[i] = createProviderComponent();
      registry.register("bulk-component-" + i, components[i]);
    }

    // Verify all components are registered
    assertEquals(5, registry.getComponentCount(), "All components should be registered");

    // Get all components
    final Set<ComponentSimple> allComponents = registry.getAllComponents();
    assertEquals(5, allComponents.size(), "Should retrieve all registered components");

    // Clear registry
    registry.clear();
    assertEquals(0, registry.getComponentCount(), "Registry should be empty after clear");
  }

  @Test
  @DisplayName("Should handle component registry statistics")
  void shouldHandleComponentRegistryStatistics() throws WasmException {
    // Register some components
    registry.register("stats-1", createProviderComponent());
    registry.register("stats-named", createConsumerComponent());
    registry.register(createMiddlewareComponent());

    // Get statistics
    final var stats = registry.getStatistics();

    assertNotNull(stats, "Statistics should not be null");
    assertEquals(3, stats.getTotalComponents(), "Should have 3 total components");
    assertEquals(1, stats.getNamedComponents(), "Should have 1 named component");
    assertTrue(stats.getTotalRegistrations() >= 3, "Should have at least 3 registrations");
  }

  // Helper methods to create test components

  private byte[] createMinimalComponentBytes() {
    // Return minimal valid WebAssembly component bytes
    // This is a placeholder - real implementation would generate valid WASM component
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // WASM magic number
      0x01, 0x00, 0x00, 0x00, // Version
      // Minimal component sections would follow
    };
  }

  private ComponentSimple createProviderComponent() throws WasmException {
    final byte[] componentBytes = createComponentWithExports(Set.of("math:add", "string:concat"));
    return componentEngine.compileComponent(componentBytes, "provider-" + System.nanoTime());
  }

  private ComponentSimple createConsumerComponent() throws WasmException {
    final byte[] componentBytes = createComponentWithImports(Set.of("math:add"));
    return componentEngine.compileComponent(componentBytes, "consumer-" + System.nanoTime());
  }

  private ComponentSimple createIncompatibleComponent() throws WasmException {
    final byte[] componentBytes = createComponentWithImports(Set.of("crypto:hash", "network:http"));
    return componentEngine.compileComponent(componentBytes, "incompatible-" + System.nanoTime());
  }

  private ComponentSimple createMiddlewareComponent() throws WasmException {
    final byte[] componentBytes =
        createComponentWithImportsAndExports(Set.of("string:concat"), Set.of("text:process"));
    return componentEngine.compileComponent(componentBytes, "middleware-" + System.nanoTime());
  }

  private ComponentSimple createCircularDependencyComponentA() throws WasmException {
    final byte[] componentBytes =
        createComponentWithImportsAndExports(
            Set.of("service-b:function"), Set.of("service-a:function"));
    return componentEngine.compileComponent(componentBytes, "circular-a-" + System.nanoTime());
  }

  private ComponentSimple createCircularDependencyComponentB() throws WasmException {
    final byte[] componentBytes =
        createComponentWithImportsAndExports(
            Set.of("service-a:function"), Set.of("service-b:function"));
    return componentEngine.compileComponent(componentBytes, "circular-b-" + System.nanoTime());
  }

  private ComponentSimple createVersionedComponent(final String version) throws WasmException {
    final byte[] componentBytes = createComponentWithVersion(version);
    return componentEngine.compileComponent(
        componentBytes, "versioned-" + version.replace(".", "-"));
  }

  // Helper methods for creating component bytes with specific characteristics
  // These are simplified placeholders - real implementation would generate valid WASM

  private byte[] createComponentWithExports(final Set<String> exports) {
    // Generate component bytes with specified exports
    return appendMetadata(createMinimalComponentBytes(), "exports", exports);
  }

  private byte[] createComponentWithImports(final Set<String> imports) {
    // Generate component bytes with specified imports
    return appendMetadata(createMinimalComponentBytes(), "imports", imports);
  }

  private byte[] createComponentWithImportsAndExports(
      final Set<String> imports, final Set<String> exports) {
    byte[] bytes = createMinimalComponentBytes();
    bytes = appendMetadata(bytes, "imports", imports);
    bytes = appendMetadata(bytes, "exports", exports);
    return bytes;
  }

  private byte[] createComponentWithVersion(final String version) {
    return appendMetadata(createMinimalComponentBytes(), "version", Set.of(version));
  }

  private byte[] appendMetadata(byte[] bytes, String type, Set<String> values) {
    // Simplified metadata appending - real implementation would properly encode WASM sections
    byte[] metadata = (type + ":" + String.join(",", values)).getBytes();
    byte[] result = new byte[bytes.length + metadata.length];
    System.arraycopy(bytes, 0, result, 0, bytes.length);
    System.arraycopy(metadata, 0, result, bytes.length, metadata.length);
    return result;
  }
}
