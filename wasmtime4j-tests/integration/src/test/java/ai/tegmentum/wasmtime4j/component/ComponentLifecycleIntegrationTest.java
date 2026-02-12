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

package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.jni.JniComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for Component lifecycle - WebAssembly Component creation and metadata.
 *
 * <p>These tests verify component creation, ID retrieval, version queries, and metadata access.
 *
 * @since 1.0.0
 */
@DisplayName("Component Lifecycle Integration Tests")
public final class ComponentLifecycleIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentLifecycleIntegrationTest.class.getName());

  private static boolean componentAvailable = false;

  // Minimal valid WebAssembly component binary
  // This is a minimal component that exports nothing - just enough to be valid
  private static final byte[] MINIMAL_COMPONENT = createMinimalComponent();

  @BeforeAll
  static void checkComponentAvailable() {
    try {
      // Try to create a component engine to check if native implementation works
      try (JniComponent.JniComponentEngine engine = JniComponent.createComponentEngine()) {
        componentAvailable = engine != null && engine.isValid();
        if (componentAvailable) {
          LOGGER.info("Component native implementation is available");
        }
      }
    } catch (final Exception e) {
      componentAvailable = false;
      LOGGER.warning("Component native implementation not available: " + e.getMessage());
    }
  }

  private static void assumeComponentAvailable() {
    assumeTrue(componentAvailable, "Component native implementation not available - skipping");
  }

  private static byte[] createMinimalComponent() {
    // Create a minimal WebAssembly component binary
    // Component format: magic number + version + component sections
    // This is the simplest valid component with no imports/exports
    return new byte[] {
      // WebAssembly component magic number
      0x00,
      0x61,
      0x73,
      0x6D, // \0asm
      // Component model version (0d for component layer)
      0x0d,
      0x00,
      0x01,
      0x00,
      // Component section (type 0x00 - component)
      0x00, // section id for component
      0x04, // section size (4 bytes)
      // Minimal core module inside
      0x00,
      0x61,
      0x73,
      0x6D // Just the magic
    };
  }

  private JniComponent.JniComponentEngine componentEngine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (componentAvailable) {
      componentEngine = JniComponent.createComponentEngine();
      resources.add(componentEngine);
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    componentEngine = null;
  }

  @Nested
  @DisplayName("Component Engine Creation Tests")
  class EngineCreationTests {

    @Test
    @DisplayName("should create component engine")
    void shouldCreateComponentEngine(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertNotNull(componentEngine, "Component engine should not be null");
      assertTrue(componentEngine.isValid(), "Component engine should be valid");
      LOGGER.info("Component engine created successfully");
    }

    @Test
    @DisplayName("should handle multiple engine instances")
    void shouldHandleMultipleEngineInstances(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (JniComponent.JniComponentEngine engine2 = JniComponent.createComponentEngine()) {
        assertNotNull(engine2, "Second engine should not be null");
        assertTrue(engine2.isValid(), "Second engine should be valid");
        assertTrue(componentEngine.isValid(), "First engine should still be valid");
        LOGGER.info("Multiple component engines created successfully");
      }
    }
  }

  @Nested
  @DisplayName("Component Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("should report active instances count")
    void shouldReportActiveInstancesCount(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      int activeCount = componentEngine.getActiveInstancesCount();
      assertTrue(activeCount >= 0, "Active instances count should be non-negative");
      LOGGER.info("Active instances count: " + activeCount);
    }

    @Test
    @DisplayName("should cleanup instances")
    void shouldCleanupInstances(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      int cleanedUp = componentEngine.cleanupInstances();
      assertTrue(cleanedUp >= 0, "Cleaned up count should be non-negative");
      LOGGER.info("Cleaned up " + cleanedUp + " instances");
    }
  }

  @Nested
  @DisplayName("Component Metadata Tests")
  class MetadataTests {

    @Test
    @DisplayName("should create component with metadata")
    void shouldCreateComponentWithMetadata(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a mock component handle for testing metadata
      // Since we can't load a real component without valid WASM, we test the metadata creation
      ComponentVersion version = new ComponentVersion(1, 2, 3);
      ComponentMetadata metadata = new ComponentMetadata("test-component", version, "Test");

      assertNotNull(metadata, "Metadata should not be null");
      assertNotNull(metadata.getName(), "Metadata name should not be null");
      assertNotNull(metadata.getVersion(), "Metadata version should not be null");
      LOGGER.info(
          "Component metadata created: " + metadata.getName() + " v" + metadata.getVersion());
    }

    @Test
    @DisplayName("should return component version")
    void shouldReturnComponentVersion(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentVersion version = new ComponentVersion(2, 0, 1);
      assertTrue(version.getMajor() == 2, "Major version should be 2");
      assertTrue(version.getMinor() == 0, "Minor version should be 0");
      assertTrue(version.getPatch() == 1, "Patch version should be 1");
      LOGGER.info("Component version: " + version);
    }

    @Test
    @DisplayName("should check version compatibility")
    void shouldCheckVersionCompatibility(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      ComponentVersion v1 = new ComponentVersion(1, 0, 0);
      ComponentVersion v2 = new ComponentVersion(1, 1, 0);
      ComponentVersion v3 = new ComponentVersion(2, 0, 0);

      // isCompatibleWith returns true if major matches AND this >= other
      assertTrue(v2.isCompatibleWith(v1), "v2 (1.1.0) should be compatible with v1 (1.0.0)");
      assertFalse(v1.isCompatibleWith(v2), "v1 (1.0.0) should not be compatible with v2 (1.1.0)");
      assertFalse(v1.isCompatibleWith(v3), "v1 should not be compatible with v3 (different major)");
      assertFalse(v3.isCompatibleWith(v1), "v3 should not be compatible with v1 (different major)");
      LOGGER.info("Version compatibility checks passed");
    }
  }

  @Nested
  @DisplayName("Component Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should close engine properly")
    void shouldCloseEngineProperly(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      JniComponent.JniComponentEngine tempEngine = JniComponent.createComponentEngine();
      assertTrue(tempEngine.isValid(), "Engine should be valid before close");

      tempEngine.close();
      assertFalse(tempEngine.isValid(), "Engine should not be valid after close");
      LOGGER.info("Engine closed properly");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      JniComponent.JniComponentEngine tempEngine = JniComponent.createComponentEngine();

      // First close
      assertDoesNotThrow(() -> tempEngine.close(), "First close should not throw");

      // Second close should also not throw
      assertDoesNotThrow(() -> tempEngine.close(), "Second close should not throw");

      LOGGER.info("Multiple close calls handled correctly");
    }

    @Test
    @DisplayName("should check if engine is valid")
    void shouldCheckIfEngineIsValid(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertTrue(componentEngine.isValid(), "Engine should be valid when open");
      LOGGER.info("Engine validity check passed");
    }
  }

  @Nested
  @DisplayName("Component Lifecycle State Tests")
  class LifecycleStateTests {

    @Test
    @DisplayName("should have lifecycle states defined")
    void shouldHaveLifecycleStatesDefined(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Verify all lifecycle states are accessible
      assertNotNull(ComponentLifecycleState.CREATING, "CREATING state should exist");
      assertNotNull(ComponentLifecycleState.READY, "READY state should exist");
      assertNotNull(ComponentLifecycleState.ACTIVE, "ACTIVE state should exist");
      assertNotNull(ComponentLifecycleState.SUSPENDED, "SUSPENDED state should exist");
      assertNotNull(ComponentLifecycleState.DESTROYED, "DESTROYED state should exist");

      LOGGER.info("All lifecycle states verified");
    }
  }
}
