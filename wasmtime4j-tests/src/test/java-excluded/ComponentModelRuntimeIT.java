package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentEngine;
import ai.tegmentum.wasmtime4j.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentMetadata;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.jni.JniComponentEngine;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for Component Model runtime functionality.
 *
 * <p>These tests validate the core Component Model implementation including component loading,
 * instantiation, and basic lifecycle management.
 */
@DisplayName("Component Model Runtime Integration Tests")
class ComponentModelRuntimeIT {

  private static final Logger LOGGER = Logger.getLogger(ComponentModelRuntimeIT.class.getName());

  private ComponentEngine componentEngine;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());

    // Create component engine with basic configuration
    final ComponentEngineConfig config =
        ComponentEngineConfig.builder()
            .enableAdvancedOrchestration(false)
            .enableDistributedSupport(false)
            .enableEnterpriseManagement(false)
            .enableWitInterfaceEnhancement(false)
            .build();

    componentEngine = new JniComponentEngine(config);
    assertNotNull(componentEngine, "Component engine should be created");
    assertTrue(componentEngine.isValid(), "Component engine should be valid");

    LOGGER.info("Test setup completed for: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) throws Exception {
    LOGGER.info("Tearing down test: " + testInfo.getDisplayName());

    if (componentEngine != null && componentEngine.isValid()) {
      componentEngine.close();
    }

    LOGGER.info("Test teardown completed for: " + testInfo.getDisplayName());
  }

  @Test
  @DisplayName("Should create component engine with configuration")
  void shouldCreateComponentEngineWithConfiguration() {
    // Verify engine properties
    assertNotNull(componentEngine.getId());
    assertNotNull(componentEngine.getConfig());
    assertTrue(componentEngine.isValid());

    // Verify resource usage
    final var resourceUsage = componentEngine.getResourceUsage();
    assertNotNull(resourceUsage);

    LOGGER.info("Component engine created successfully with ID: " + componentEngine.getId());
  }

  @Test
  @DisplayName("Should load component from bytes")
  void shouldLoadComponentFromBytes() throws Exception {
    // Create minimal WASM bytes (empty module)
    final byte[] minimalWasm = createMinimalWasmBytes();

    // Create metadata
    final ComponentMetadata metadata =
        new ComponentMetadata(
            "test-component",
            new ComponentVersion(1, 0, 0),
            "Test component for runtime validation");

    // Load component
    final Component component = componentEngine.loadComponentFromBytes(minimalWasm, metadata);

    assertNotNull(component, "Component should be loaded");
    assertTrue(component.isValid(), "Component should be valid");
    assertNotNull(component.getId(), "Component should have an ID");
    assertNotNull(component.getVersion(), "Component should have a version");

    LOGGER.info("Component loaded successfully: " + component.getId());

    component.close();
  }

  @Test
  @DisplayName("Should instantiate component")
  void shouldInstantiateComponent() throws Exception {
    // Create minimal WASM bytes
    final byte[] minimalWasm = createMinimalWasmBytes();

    // Load component
    final Component component = componentEngine.loadComponentFromBytes(minimalWasm);

    // Instantiate component
    final ComponentInstance instance = component.instantiate();

    assertNotNull(instance, "Component instance should be created");
    assertTrue(instance.isValid(), "Component instance should be valid");
    assertNotNull(instance.getId(), "Component instance should have an ID");

    LOGGER.info("Component instantiated successfully: " + instance.getId());

    instance.close();
    component.close();
  }

  @Test
  @DisplayName("Should get component metadata")
  void shouldGetComponentMetadata() throws Exception {
    // Create minimal WASM bytes
    final byte[] minimalWasm = createMinimalWasmBytes();

    // Create metadata
    final ComponentMetadata metadata =
        new ComponentMetadata(
            "metadata-test-component",
            new ComponentVersion(2, 1, 0),
            "Component for metadata testing");

    // Load component
    final Component component = componentEngine.loadComponentFromBytes(minimalWasm, metadata);

    // Verify metadata
    final ComponentMetadata retrievedMetadata = component.getMetadata();
    assertNotNull(retrievedMetadata);
    assertTrue(retrievedMetadata.getName().contains("metadata-test"));
    assertNotNull(retrievedMetadata.getVersion());

    LOGGER.info("Component metadata validated: " + retrievedMetadata);

    component.close();
  }

  @Test
  @DisplayName("Should get component size")
  void shouldGetComponentSize() throws Exception {
    // Create minimal WASM bytes
    final byte[] minimalWasm = createMinimalWasmBytes();

    // Load component
    final Component component = componentEngine.loadComponentFromBytes(minimalWasm);

    // Get size
    final long size = component.getSize();
    assertTrue(size > 0, "Component size should be positive");

    LOGGER.info("Component size: " + size + " bytes");

    component.close();
  }

  /**
   * Creates minimal valid WebAssembly bytes for testing. This is a minimal WASM module that does
   * nothing but is structurally valid.
   */
  private byte[] createMinimalWasmBytes() {
    // WebAssembly module magic number (0x6d736100) + version (0x01000000)
    return new byte[] {
      0x00, 0x61, 0x73, 0x6D, // Magic number "\0asm"
      0x01, 0x00, 0x00, 0x00 // Version 1
    };
  }
}
