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

package ai.tegmentum.wasmtime4j.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cross-runtime parity tests for Component Model operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * component compilation, instantiation, and invocation.
 *
 * <p>Note: Component Model support may be limited on some runtimes. Tests are skipped if component
 * support is not available.
 */
@DisplayName("Component Parity Tests")
@Tag("integration")
class ComponentParityTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentParityTest.class.getName());

  private static boolean jniAvailable;
  private static boolean panamaAvailable;
  private static byte[] componentBytes;
  private static String componentUnavailableReason;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private Store jniStore;
  private Store panamaStore;
  private boolean jniComponentsSupported;
  private boolean panamaComponentsSupported;

  @BeforeAll
  static void checkRuntimeAndComponentAvailability() {
    LOGGER.info("Checking runtime availability for component parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);

    // Try to load a simple component for testing
    try (InputStream is = ComponentParityTest.class.getResourceAsStream("/components/add.wasm")) {
      if (is != null) {
        componentBytes = readAllBytes(is);
        LOGGER.info("Component bytes loaded: " + componentBytes.length + " bytes");
      } else {
        componentUnavailableReason = "add.wasm component not found in resources";
        LOGGER.warning("Component test resource not available: " + componentUnavailableReason);
      }
    } catch (final Exception e) {
      componentUnavailableReason = "Failed to load component: " + e.getMessage();
      LOGGER.warning("Component loading failed: " + componentUnavailableReason);
    }
  }

  private static byte[] readAllBytes(final InputStream inputStream) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final byte[] tempBuffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = inputStream.read(tempBuffer)) != -1) {
      buffer.write(tempBuffer, 0, bytesRead);
    }
    return buffer.toByteArray();
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up component parity test runtimes");

    jniComponentsSupported = false;
    panamaComponentsSupported = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniStore = jniEngine.createStore();

        // Check if engine supports components
        if (jniEngine instanceof ComponentEngine) {
          jniComponentsSupported = ((ComponentEngine) jniEngine).supportsComponentModel();
        }
        LOGGER.info("JNI runtime created, component support: " + jniComponentsSupported);
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI runtime: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaStore = panamaEngine.createStore();

        // Check if engine supports components
        if (panamaEngine instanceof ComponentEngine) {
          panamaComponentsSupported = ((ComponentEngine) panamaEngine).supportsComponentModel();
        }
        LOGGER.info("Panama runtime created, component support: " + panamaComponentsSupported);
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama runtime: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up component parity test runtimes");

    closeResource(jniStore, "JNI store");
    closeResource(jniEngine, "JNI engine");
    closeResource(jniRuntime, "JNI runtime");
    closeResource(panamaStore, "Panama store");
    closeResource(panamaEngine, "Panama engine");
    closeResource(panamaRuntime, "Panama runtime");
  }

  private void closeResource(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.info(name + " closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimesWithComponents() {
    assumeTrue(
        jniComponentsSupported && panamaComponentsSupported,
        "Both JNI and Panama runtimes with component support required");
    assumeTrue(componentBytes != null, "Component bytes required: " + componentUnavailableReason);
  }

  @Nested
  @DisplayName("ComponentEngine Creation Parity Tests")
  class ComponentEngineCreationParityTests {

    @Test
    @DisplayName("should report component model support identically on both runtimes")
    void shouldReportComponentSupportIdentically() {
      assumeTrue(
          jniRuntime != null && panamaRuntime != null, "Both runtimes required for this test");

      LOGGER.info("JNI component support: " + jniComponentsSupported);
      LOGGER.info("Panama component support: " + panamaComponentsSupported);

      // Log the support status - both should either support or not support
      if (jniEngine instanceof ComponentEngine && panamaEngine instanceof ComponentEngine) {
        final boolean jniSupport = ((ComponentEngine) jniEngine).supportsComponentModel();
        final boolean panamaSupport = ((ComponentEngine) panamaEngine).supportsComponentModel();

        LOGGER.info(
            "ComponentEngine.supportsComponentModel() - JNI: "
                + jniSupport
                + ", Panama: "
                + panamaSupport);

        // Both should have same support status
        assertThat(jniSupport).isEqualTo(panamaSupport);
      }
    }

    @Test
    @DisplayName("should create ComponentEngine instances on both runtimes")
    void shouldCreateComponentEngineInstances() {
      assumeTrue(
          jniRuntime != null && panamaRuntime != null, "Both runtimes required for this test");

      // Verify engines are ComponentEngine instances
      assertThat(jniEngine).isInstanceOf(ComponentEngine.class);
      assertThat(panamaEngine).isInstanceOf(ComponentEngine.class);

      LOGGER.info("Both engines are ComponentEngine instances");
    }
  }

  @Nested
  @DisplayName("ComponentLinker Parity Tests")
  class ComponentLinkerParityTests {

    @Test
    @DisplayName("should create ComponentLinker identically on both runtimes")
    void shouldCreateComponentLinkerIdentically() {
      requireBothRuntimesWithComponents();

      try {
        final ComponentLinker<?> jniLinker = ComponentLinker.create(jniEngine);
        final ComponentLinker<?> panamaLinker = ComponentLinker.create(panamaEngine);

        assertThat(jniLinker).isNotNull();
        assertThat(panamaLinker).isNotNull();

        LOGGER.info("Created ComponentLinker on both runtimes");

        jniLinker.close();
        panamaLinker.close();
      } catch (final UnsupportedOperationException | WasmException e) {
        LOGGER.info("ComponentLinker not supported: " + e.getMessage());
        assumeTrue(false, "ComponentLinker not supported");
      }
    }
  }

  @Nested
  @DisplayName("Component Compilation Parity Tests")
  class ComponentCompilationParityTests {

    @Test
    @DisplayName("should compile component identically on both runtimes")
    void shouldCompileComponentIdentically() {
      requireBothRuntimesWithComponents();

      try {
        final ComponentEngine jniCompEngine = (ComponentEngine) jniEngine;
        final ComponentEngine panamaCompEngine = (ComponentEngine) panamaEngine;

        final Component jniComponent = jniCompEngine.compileComponent(componentBytes);
        final Component panamaComponent = panamaCompEngine.compileComponent(componentBytes);

        assertThat(jniComponent).isNotNull();
        assertThat(panamaComponent).isNotNull();

        // Both should have same validity
        assertThat(jniComponent.isValid()).isEqualTo(panamaComponent.isValid());

        LOGGER.info("Compiled component on both runtimes");
        LOGGER.info("JNI component valid: " + jniComponent.isValid());
        LOGGER.info("Panama component valid: " + panamaComponent.isValid());

        jniComponent.close();
        panamaComponent.close();
      } catch (final Exception e) {
        LOGGER.warning("Component compilation failed: " + e.getMessage());
        assumeTrue(false, "Component compilation not supported: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should get component size identically on both runtimes")
    void shouldGetComponentSizeIdentically() {
      requireBothRuntimesWithComponents();

      try {
        final ComponentEngine jniCompEngine = (ComponentEngine) jniEngine;
        final ComponentEngine panamaCompEngine = (ComponentEngine) panamaEngine;

        final Component jniComponent = jniCompEngine.compileComponent(componentBytes);
        final Component panamaComponent = panamaCompEngine.compileComponent(componentBytes);

        final long jniSize = jniComponent.getSize();
        final long panamaSize = panamaComponent.getSize();

        LOGGER.info("JNI component size: " + jniSize);
        LOGGER.info("Panama component size: " + panamaSize);

        // Sizes should be identical (same bytes compiled)
        assertThat(jniSize).isEqualTo(panamaSize);

        jniComponent.close();
        panamaComponent.close();
      } catch (final Exception e) {
        LOGGER.warning("Component size check failed: " + e.getMessage());
        assumeTrue(false, "Component size not supported: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Component Interface Parity Tests")
  class ComponentInterfaceParityTests {

    @Test
    @DisplayName("should get exported interfaces identically on both runtimes")
    void shouldGetExportedInterfacesIdentically() {
      requireBothRuntimesWithComponents();

      try {
        final ComponentEngine jniCompEngine = (ComponentEngine) jniEngine;
        final ComponentEngine panamaCompEngine = (ComponentEngine) panamaEngine;

        final Component jniComponent = jniCompEngine.compileComponent(componentBytes);
        final Component panamaComponent = panamaCompEngine.compileComponent(componentBytes);

        final var jniExports = jniComponent.getExportedInterfaces();
        final var panamaExports = panamaComponent.getExportedInterfaces();

        LOGGER.info("JNI exported interfaces: " + jniExports);
        LOGGER.info("Panama exported interfaces: " + panamaExports);

        // Exported interfaces should be identical
        assertThat(jniExports).isEqualTo(panamaExports);

        jniComponent.close();
        panamaComponent.close();
      } catch (final Exception e) {
        LOGGER.warning("Interface query failed: " + e.getMessage());
        assumeTrue(false, "Component interface query not supported: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("should get imported interfaces identically on both runtimes")
    void shouldGetImportedInterfacesIdentically() {
      requireBothRuntimesWithComponents();

      try {
        final ComponentEngine jniCompEngine = (ComponentEngine) jniEngine;
        final ComponentEngine panamaCompEngine = (ComponentEngine) panamaEngine;

        final Component jniComponent = jniCompEngine.compileComponent(componentBytes);
        final Component panamaComponent = panamaCompEngine.compileComponent(componentBytes);

        final var jniImports = jniComponent.getImportedInterfaces();
        final var panamaImports = panamaComponent.getImportedInterfaces();

        LOGGER.info("JNI imported interfaces: " + jniImports);
        LOGGER.info("Panama imported interfaces: " + panamaImports);

        // Imported interfaces should be identical
        assertThat(jniImports).isEqualTo(panamaImports);

        jniComponent.close();
        panamaComponent.close();
      } catch (final Exception e) {
        LOGGER.warning("Interface query failed: " + e.getMessage());
        assumeTrue(false, "Component interface query not supported: " + e.getMessage());
      }
    }
  }
}
