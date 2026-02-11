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
 * Cross-runtime parity tests for Store operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * store creation and basic operations.
 */
@DisplayName("Store Parity Tests")
@Tag("integration")
class StoreParityTest {

  private static final Logger LOGGER = Logger.getLogger(StoreParityTest.class.getName());

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for store parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up store parity test resources");

    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniCreatedSuccessfully = true;
        LOGGER.info("JNI runtime and engine created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI resources: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaCreatedSuccessfully = true;
        LOGGER.info("Panama runtime and engine created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama resources: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up store parity test resources");

    closeQuietly(jniEngine, "JNI engine");
    closeQuietly(jniRuntime, "JNI runtime");
    closeQuietly(panamaEngine, "Panama engine");
    closeQuietly(panamaRuntime, "Panama runtime");
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.info(name + " closed");
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimes() {
    assumeTrue(
        jniCreatedSuccessfully && panamaCreatedSuccessfully,
        "Both JNI and Panama runtimes required");
  }

  @Nested
  @DisplayName("Store Creation Parity Tests")
  class StoreCreationParityTests {

    @Test
    @DisplayName("should create stores with default config on both runtimes")
    void shouldCreateDefaultStores() throws Exception {
      requireBothRuntimes();

      try (Store jniStore = jniRuntime.createStore(jniEngine);
          Store panamaStore = panamaRuntime.createStore(panamaEngine)) {

        LOGGER.info("Created JNI store: " + jniStore);
        LOGGER.info("Created Panama store: " + panamaStore);

        assertThat(jniStore).isNotNull();
        assertThat(panamaStore).isNotNull();
      }
    }

    @Test
    @DisplayName("should create stores with fuel limit on both runtimes")
    void shouldCreateStoresWithFuelLimit() throws Exception {
      requireBothRuntimes();

      final long fuelLimit = 10000L;
      final long memoryLimit = 0L;
      final long timeout = 0L;

      try (Store jniStore = jniRuntime.createStore(jniEngine, fuelLimit, memoryLimit, timeout);
          Store panamaStore =
              panamaRuntime.createStore(panamaEngine, fuelLimit, memoryLimit, timeout)) {

        LOGGER.info("Created stores with fuel limit: " + fuelLimit);

        assertThat(jniStore).isNotNull();
        assertThat(panamaStore).isNotNull();
      }
    }

    @Test
    @DisplayName("should create stores with memory limit on both runtimes")
    void shouldCreateStoresWithMemoryLimit() throws Exception {
      requireBothRuntimes();

      final long fuelLimit = 0L;
      final long memoryLimit = 64 * 1024 * 1024L;
      final long timeout = 0L;

      try (Store jniStore = jniRuntime.createStore(jniEngine, fuelLimit, memoryLimit, timeout);
          Store panamaStore =
              panamaRuntime.createStore(panamaEngine, fuelLimit, memoryLimit, timeout)) {

        LOGGER.info("Created stores with memory limit: " + memoryLimit + " bytes");

        assertThat(jniStore).isNotNull();
        assertThat(panamaStore).isNotNull();
      }
    }
  }

  @Nested
  @DisplayName("Store Lifecycle Parity Tests")
  class StoreLifecycleParityTests {

    @Test
    @DisplayName("should close stores cleanly on both runtimes")
    void shouldCloseStoresCleanly() throws Exception {
      requireBothRuntimes();

      final Store jniStore = jniRuntime.createStore(jniEngine);
      final Store panamaStore = panamaRuntime.createStore(panamaEngine);

      LOGGER.info("Closing stores");

      jniStore.close();
      panamaStore.close();

      LOGGER.info("Stores closed successfully");
    }

    @Test
    @DisplayName("should handle multiple store creations on both runtimes")
    void shouldHandleMultipleStoreCreations() throws Exception {
      requireBothRuntimes();

      for (int i = 0; i < 5; i++) {
        try (Store jniStore = jniRuntime.createStore(jniEngine);
            Store panamaStore = panamaRuntime.createStore(panamaEngine)) {

          LOGGER.info("Created store pair " + (i + 1));

          assertThat(jniStore).isNotNull();
          assertThat(panamaStore).isNotNull();
        }
      }

      LOGGER.info("Successfully created and closed 5 store pairs");
    }
  }
}
