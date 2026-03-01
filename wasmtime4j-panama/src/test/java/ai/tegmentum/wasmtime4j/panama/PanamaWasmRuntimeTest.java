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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Panama-specific tests for {@link PanamaWasmRuntime}.
 *
 * <p>Tests that verify Panama implementation types are correctly returned. Generic WasmRuntime API
 * tests have been migrated to {@code WasmRuntimeApiDualRuntimeTest} in the integration test module.
 */
@DisplayName("PanamaWasmRuntime Panama-Specific Tests")
class PanamaWasmRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasmRuntimeTest.class.getName());

  private final List<AutoCloseable> resources = new ArrayList<>();

  private PanamaWasmRuntime runtime;

  @BeforeEach
  void setUp() throws WasmException {
    runtime = new PanamaWasmRuntime();
    resources.add(runtime);
    LOGGER.info("Test setup: PanamaWasmRuntime created");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Panama Type Verification Tests")
  class PanamaTypeVerificationTests {

    @Test
    @DisplayName("Should create PanamaWasmRuntime via default constructor")
    void shouldCreateRuntime() throws WasmException {
      final PanamaWasmRuntime rt = new PanamaWasmRuntime();
      resources.add(rt);
      assertNotNull(rt, "Runtime should not be null");
      assertTrue(rt.isValid(), "Runtime should be valid after creation");
      LOGGER.info("Created PanamaWasmRuntime successfully");
    }

    @Test
    @DisplayName("Should implement WasmRuntime interface")
    void shouldImplementWasmRuntime() {
      assertTrue(runtime instanceof WasmRuntime, "PanamaWasmRuntime should implement WasmRuntime");
      LOGGER.info("PanamaWasmRuntime implements WasmRuntime: true");
    }

    @Test
    @DisplayName("Should implement AutoCloseable interface")
    void shouldImplementAutoCloseable() {
      assertTrue(
          runtime instanceof AutoCloseable, "PanamaWasmRuntime should implement AutoCloseable");
      LOGGER.info("PanamaWasmRuntime implements AutoCloseable: true");
    }

    @Test
    @DisplayName("Should create PanamaEngine instance")
    void shouldCreatePanamaEngine() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);
      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine instanceof PanamaEngine, "Should be PanamaEngine instance");
      LOGGER.info("Created PanamaEngine: " + engine);
    }

    @Test
    @DisplayName("Should create PanamaComponentEngine instance")
    void shouldCreatePanamaComponentEngine() throws WasmException {
      final ComponentEngine ce = runtime.createComponentEngine();
      resources.add(ce);
      assertNotNull(ce, "Component engine should not be null");
      assertTrue(ce instanceof PanamaComponentEngine, "Should be PanamaComponentEngine instance");
      LOGGER.info("Created PanamaComponentEngine: " + ce);
    }

    @Test
    @DisplayName("Should create PanamaStore instance")
    void shouldCreatePanamaStore() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Store store = runtime.createStore(engine);
      resources.add(store);
      assertNotNull(store, "Store should not be null");
      assertTrue(store instanceof PanamaStore, "Should be PanamaStore instance");
      LOGGER.info("Created PanamaStore: " + store);
    }

    @Test
    @DisplayName("Should create PanamaLinker instance")
    void shouldCreatePanamaLinker() throws WasmException {
      final Engine engine = runtime.createEngine();
      resources.add(engine);

      final Linker<?> linker = runtime.createLinker(engine);
      resources.add(linker);
      assertNotNull(linker, "Linker should not be null");
      assertTrue(linker instanceof PanamaLinker, "Should be PanamaLinker instance");
      LOGGER.info("Created PanamaLinker: " + linker);
    }

    @Test
    @DisplayName("Should create PanamaWasiContext instance")
    void shouldCreatePanamaWasiContext() throws WasmException {
      final WasiContext context = runtime.createWasiContext();
      assertNotNull(context, "WASI context should not be null");
      assertTrue(context instanceof PanamaWasiContext, "Should be PanamaWasiContext instance");
      LOGGER.info("Created PanamaWasiContext: " + context);
    }
  }
}
