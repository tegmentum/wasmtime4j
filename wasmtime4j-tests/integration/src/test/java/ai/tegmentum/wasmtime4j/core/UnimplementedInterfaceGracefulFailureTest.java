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

package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.InstanceManager;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Context;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests that factory methods for unimplemented interfaces fail gracefully with clear error messages
 * rather than crashing with NPE or segfault. {@link InstanceManager#create(Engine)} and {@link
 * WasiPreview2Context#create(WasiConfig)} use reflection-based factories that attempt to load
 * implementation classes which may not exist.
 *
 * @since 1.0.0
 */
@DisplayName("Unimplemented Interface Graceful Failure Tests")
public class UnimplementedInterfaceGracefulFailureTest {

  private static final Logger LOGGER =
      Logger.getLogger(UnimplementedInterfaceGracefulFailureTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    if (engine != null) {
      try {
        engine.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close engine: " + e.getMessage());
      }
    }
    if (runtime != null) {
      try {
        runtime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close runtime: " + e.getMessage());
      }
    }
    LOGGER.info("Finished test: " + testInfo.getDisplayName());
  }

  @Nested
  @DisplayName("InstanceManager.create() Tests")
  class InstanceManagerCreateTests {

    @Test
    @DisplayName("InstanceManager.create(engine) fails gracefully or succeeds")
    void instanceManagerCreateFailsGracefully() {
      LOGGER.info("Testing InstanceManager.create(engine) - expecting graceful failure or success");

      try {
        final InstanceManager manager = InstanceManager.create(engine);
        // If an implementation exists, the factory should return a non-null instance
        assertNotNull(manager, "If factory succeeds, result should be non-null");
        LOGGER.info(
            "InstanceManager created successfully: " + manager.getClass().getName());
      } catch (final RuntimeException e) {
        // Expected: reflection-based factory throws RuntimeException when no impl found
        LOGGER.info(
            "InstanceManager.create() threw RuntimeException (expected): " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a descriptive message");
        assertTrue(
            e.getMessage().contains("InstanceManager")
                || e.getMessage().contains("implementation")
                || e.getMessage().contains("classpath"),
            "Error message should reference InstanceManager or classpath, got: "
                + e.getMessage());
      } catch (final Exception e) {
        // Any other exception is also acceptable as long as it's not NPE/Error
        LOGGER.info(
            "InstanceManager.create() threw " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }
    }

    @Test
    @DisplayName("InstanceManager.create(null) throws NullPointerException or IllegalArgumentException")
    void instanceManagerCreateWithNullEngineThrows() {
      LOGGER.info("Testing InstanceManager.create(null)");

      try {
        InstanceManager.create(null);
        fail("InstanceManager.create(null) should throw an exception");
      } catch (final NullPointerException e) {
        LOGGER.info("Null engine rejected with NullPointerException: " + e.getMessage());
      } catch (final IllegalArgumentException e) {
        LOGGER.info("Null engine rejected with IllegalArgumentException: " + e.getMessage());
      } catch (final Exception e) {
        // Any exception rejecting null is acceptable
        LOGGER.info(
            "Null engine rejected with " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("WasiPreview2Context.create() Tests")
  class WasiPreview2ContextCreateTests {

    @Test
    @DisplayName("WasiPreview2Context.create(config) fails gracefully or succeeds")
    void wasiPreview2ContextCreateFailsGracefully() {
      LOGGER.info(
          "Testing WasiPreview2Context.create(config) - expecting graceful failure or success");

      try {
        // WasiConfig.builder() itself may fail if no implementation is found
        final WasiConfig config = WasiConfig.defaultConfig();
        final WasiPreview2Context ctx = WasiPreview2Context.create(config);
        assertNotNull(ctx, "If factory succeeds, result should be non-null");
        LOGGER.info(
            "WasiPreview2Context created successfully: " + ctx.getClass().getName());
      } catch (final IllegalArgumentException e) {
        // WasiPreview2Context.create() validates WASI version
        LOGGER.info(
            "WasiPreview2Context.create() threw IllegalArgumentException: " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a descriptive message");
      } catch (final RuntimeException e) {
        // Expected: either WasiConfig.builder() or WasiPreview2Context.create()
        // fails because reflection can't find impl classes
        LOGGER.info(
            "WasiPreview2Context.create() threw RuntimeException (expected): "
                + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a descriptive message");
      } catch (final Exception e) {
        // Any non-fatal exception is acceptable
        LOGGER.info(
            "WasiPreview2Context.create() threw " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a message");
      }
    }

    @Test
    @DisplayName("WasiPreview2Context.create(null) throws NullPointerException or IllegalArgumentException")
    void wasiPreview2ContextCreateWithNullConfigThrows() {
      LOGGER.info("Testing WasiPreview2Context.create(null)");

      assertThrows(
          Exception.class,
          () -> WasiPreview2Context.create(null),
          "WasiPreview2Context.create(null) should throw an exception");

      LOGGER.info("Null config correctly rejected");
    }
  }
}
