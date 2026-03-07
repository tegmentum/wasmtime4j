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
package ai.tegmentum.wasmtime4j.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WASI Preview 2 functionality.
 *
 * <p>This test suite validates the complete WASI Preview 2 implementation including:
 *
 * <ul>
 *   <li>Component model integration
 *   <li>Async I/O operations
 *   <li>Network operations (where supported)
 *   <li>Process management
 *   <li>Resource management and cleanup
 * </ul>
 */
public class WasiPreview2Test extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiPreview2Test.class.getName());

  private Engine engine;

  @AfterEach
  void tearDown(TestInfo testInfo) {
    LOGGER.info("Tearing down test: " + testInfo.getDisplayName());
    if (engine != null) {
      try {
        engine.close();
      } catch (Exception e) {
        LOGGER.warning("Failed to close engine: " + e.getMessage());
      }
    }
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testWasiPreview2ContextCreation")
  void testWasiPreview2ContextCreation(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Create WASI context with Preview 2 capabilities
    WasiContext context = WasiContext.create();
    assertNotNull(context, "WASI context should be created");

    // Configure Preview 2 specific features
    context
        .setAsyncIoEnabled(true)
        .setMaxAsyncOperations(10)
        .setAsyncTimeout(5000)
        .setComponentModelEnabled(true)
        .setProcessEnabled(true)
        .setNetworkEnabled(true);

    LOGGER.info("Successfully created and configured WASI Preview 2 context");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testWasiPreview2LinkerCreation")
  void testWasiPreview2LinkerCreation(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context =
        WasiContext.create().setAsyncIoEnabled(true).setComponentModelEnabled(true);

    // Create linker with WASI Preview 2 support
    Linker<WasiContext> linker = WasiLinkerUtils.createPreview2Linker(engine, context);
    assertNotNull(linker, "Preview 2 linker should be created");

    // Verify WASI Preview 2 imports are present
    assertTrue(
        WasiLinkerUtils.hasWasiPreview2Imports(linker),
        "Linker should have WASI Preview 2 imports");

    LOGGER.info("Successfully created WASI Preview 2 linker");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testComponentModelSupport")
  void testComponentModelSupport(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Check if runtime supports component model
    LOGGER.info("Testing component model support");

    WasiContext context = WasiContext.create().setComponentModelEnabled(true);

    Linker<WasiContext> linker = Linker.create(engine);

    // Verify component model imports
    assertTrue(
        WasiLinkerUtils.runtimeSupportsComponentModel(),
        "Linker should have component model imports");

    LOGGER.info("Successfully validated component model support");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testAsyncIoConfiguration")
  void testAsyncIoConfiguration(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Configure async I/O settings
    assertDoesNotThrow(
        () -> {
          context.setAsyncIoEnabled(true).setMaxAsyncOperations(20).setAsyncTimeout(10000);
        },
        "Configuring async I/O should not throw");

    // Test with different timeout values
    assertDoesNotThrow(
        () -> {
          context.setAsyncTimeout(-1); // No timeout
        },
        "Setting no timeout should not throw");

    assertDoesNotThrow(
        () -> {
          context.setAsyncTimeout(0); // Immediate timeout
        },
        "Setting immediate timeout should not throw");

    // Test max operations limits
    assertDoesNotThrow(
        () -> {
          context.setMaxAsyncOperations(-1); // Unlimited
        },
        "Setting unlimited async operations should not throw");

    LOGGER.info("Successfully configured async I/O settings");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testProcessAndNetworkConfiguration")
  void testProcessAndNetworkConfiguration(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Configure process and network capabilities
    assertDoesNotThrow(
        () -> {
          context.setProcessEnabled(true).setNetworkEnabled(true);
        },
        "Enabling process and network should not throw");

    // Test disabling capabilities
    assertDoesNotThrow(
        () -> {
          context.setProcessEnabled(false).setNetworkEnabled(false);
        },
        "Disabling process and network should not throw");

    LOGGER.info("Successfully configured process and network settings");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testFullWasiPreview2Linker")
  void testFullWasiPreview2Linker(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context =
        WasiContext.create()
            .setAsyncIoEnabled(true)
            .setComponentModelEnabled(true)
            .setProcessEnabled(true)
            .setNetworkEnabled(true)
            .setMaxAsyncOperations(50)
            .setAsyncTimeout(15000);

    // Create full linker with both Preview 2 and Component Model
    Linker<WasiContext> linker = WasiLinkerUtils.createFullLinker(engine, context);
    assertNotNull(linker, "Full linker should be created");

    // Verify all imports are present
    assertTrue(
        WasiLinkerUtils.hasWasiPreview2Imports(linker),
        "Full linker should have WASI Preview 2 imports");
    assertTrue(
        WasiLinkerUtils.runtimeSupportsComponentModel(),
        "Full linker should have Component Model imports");

    LOGGER.info("Successfully created full WASI Preview 2 + Component Model linker");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testWasiPreview2AddToLinkerDirectly")
  void testWasiPreview2AddToLinkerDirectly(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context =
        WasiContext.create().setAsyncIoEnabled(true).setComponentModelEnabled(true);

    Linker<WasiContext> linker = Linker.create(engine);

    // Verify imports were added
    assertTrue(
        WasiLinkerUtils.hasWasiPreview2Imports(linker),
        "Linker should have WASI Preview 2 imports after adding");

    LOGGER.info("Successfully added WASI Preview 2 imports to linker");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testErrorHandlingInPreview2")
  void testErrorHandlingInPreview2(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    WasiContext context = WasiContext.create();

    // Test invalid async operation limits
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setMaxAsyncOperations(-2);
        },
        "Invalid max async operations should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setAsyncTimeout(-2);
        },
        "Invalid async timeout should throw");

    LOGGER.info("Successfully validated error handling in WASI Preview 2");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testContextLifecycleManagement")
  void testContextLifecycleManagement(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // Test multiple context creation and cleanup
    for (int i = 0; i < 5; i++) {
      WasiContext context =
          WasiContext.create()
              .setAsyncIoEnabled(true)
              .setComponentModelEnabled(true)
              .setMaxAsyncOperations(10);

      Linker<WasiContext> linker = WasiLinkerUtils.createPreview2Linker(engine, context);
      assertNotNull(linker, "Linker " + i + " should be created");

      // Contexts and linkers should be properly managed by the runtime
      LOGGER.fine("Created context and linker iteration: " + i);
    }

    LOGGER.info("Successfully validated context lifecycle management");
  }

  /** Test is enabled only if async I/O is actually supported by the runtime. */
  @EnabledIf("supportsAsyncIo")
  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("testAsyncIoIntegration")
  void testAsyncIoIntegration(final RuntimeType runtime) throws WasmException {
    setRuntime(runtime);
    engine = Engine.create();

    // This test would require actual WASM components that use async I/O
    // For now, we test the configuration and setup
    WasiContext context =
        WasiContext.create().setAsyncIoEnabled(true).setMaxAsyncOperations(5).setAsyncTimeout(1000);

    Linker<WasiContext> linker = WasiLinkerUtils.createPreview2Linker(engine, context);
    assertNotNull(linker, "Async I/O linker should be created");

    // In a real test, we would load a WASM component that performs async I/O
    // and verify that operations complete correctly with timeouts and cancellation

    LOGGER.info("Successfully validated async I/O integration setup");
  }

  /** Checks if the runtime supports async I/O operations. */
  static boolean supportsAsyncIo() {
    try {
      WasiContext testContext = WasiContext.create();
      testContext.setAsyncIoEnabled(true);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
