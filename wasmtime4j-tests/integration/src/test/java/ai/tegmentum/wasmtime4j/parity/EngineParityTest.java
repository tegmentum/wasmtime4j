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
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cross-runtime parity tests for Engine operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * engine creation, configuration, and basic operations.
 */
@DisplayName("Engine Parity Tests")
@Tag("integration")
class EngineParityTest {

  private static final Logger LOGGER = Logger.getLogger(EngineParityTest.class.getName());

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up parity test runtimes");

    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniCreatedSuccessfully = true;
        LOGGER.info("JNI runtime created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI runtime: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaCreatedSuccessfully = true;
        LOGGER.info("Panama runtime created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama runtime: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up parity test runtimes");

    if (jniRuntime != null) {
      try {
        jniRuntime.close();
        LOGGER.info("JNI runtime closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing JNI runtime: " + e.getMessage());
      }
    }

    if (panamaRuntime != null) {
      try {
        panamaRuntime.close();
        LOGGER.info("Panama runtime closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing Panama runtime: " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimes() {
    assumeTrue(
        jniCreatedSuccessfully && panamaCreatedSuccessfully,
        "Both JNI and Panama runtimes required");
  }

  @Nested
  @DisplayName("Engine Creation Parity Tests")
  class EngineCreationParityTests {

    @Test
    @DisplayName("should create engines with default config on both runtimes")
    void shouldCreateDefaultEngines() throws Exception {
      requireBothRuntimes();

      try (Engine jniEngine = jniRuntime.createEngine();
          Engine panamaEngine = panamaRuntime.createEngine()) {

        LOGGER.info("Created JNI engine: " + jniEngine);
        LOGGER.info("Created Panama engine: " + panamaEngine);

        assertThat(jniEngine).isNotNull();
        assertThat(panamaEngine).isNotNull();
      }
    }

    @Test
    @DisplayName("should create engines with speed optimization config on both runtimes")
    void shouldCreateEnginesWithSpeedOptimization() throws Exception {
      requireBothRuntimes();

      final EngineConfig config = EngineConfig.forSpeed();

      try (Engine jniEngine = jniRuntime.createEngine(config);
          Engine panamaEngine = panamaRuntime.createEngine(config)) {

        LOGGER.info("Created JNI engine with SPEED optimization");
        LOGGER.info("Created Panama engine with SPEED optimization");

        assertThat(jniEngine).isNotNull();
        assertThat(panamaEngine).isNotNull();
      }
    }

    @Test
    @DisplayName("should create engines with size optimization config on both runtimes")
    void shouldCreateEnginesWithSizeOptimization() throws Exception {
      requireBothRuntimes();

      final EngineConfig config = EngineConfig.forSize();

      try (Engine jniEngine = jniRuntime.createEngine(config);
          Engine panamaEngine = panamaRuntime.createEngine(config)) {

        LOGGER.info("Created JNI engine with SIZE optimization");
        LOGGER.info("Created Panama engine with SIZE optimization");

        assertThat(jniEngine).isNotNull();
        assertThat(panamaEngine).isNotNull();
      }
    }

    @Test
    @DisplayName("should create engines with debug config on both runtimes")
    void shouldCreateEnginesWithDebugConfig() throws Exception {
      requireBothRuntimes();

      final EngineConfig config = EngineConfig.forDebug();

      try (Engine jniEngine = jniRuntime.createEngine(config);
          Engine panamaEngine = panamaRuntime.createEngine(config)) {

        LOGGER.info("Created JNI engine with DEBUG config");
        LOGGER.info("Created Panama engine with DEBUG config");

        assertThat(jniEngine).isNotNull();
        assertThat(panamaEngine).isNotNull();
      }
    }
  }

  @Nested
  @DisplayName("Engine Lifecycle Parity Tests")
  class EngineLifecycleParityTests {

    @Test
    @DisplayName("should close engines cleanly on both runtimes")
    void shouldCloseEnginesCleanly() throws Exception {
      requireBothRuntimes();

      final Engine jniEngine = jniRuntime.createEngine();
      final Engine panamaEngine = panamaRuntime.createEngine();

      LOGGER.info("Closing engines");

      jniEngine.close();
      panamaEngine.close();

      LOGGER.info("Engines closed successfully");
    }

    @Test
    @DisplayName("should handle multiple engine creations on both runtimes")
    void shouldHandleMultipleEngineCreations() throws Exception {
      requireBothRuntimes();

      for (int i = 0; i < 5; i++) {
        try (Engine jniEngine = jniRuntime.createEngine();
            Engine panamaEngine = panamaRuntime.createEngine()) {

          LOGGER.info("Created engine pair " + (i + 1));

          assertThat(jniEngine).isNotNull();
          assertThat(panamaEngine).isNotNull();
        }
      }

      LOGGER.info("Successfully created and closed 5 engine pairs");
    }
  }
}
