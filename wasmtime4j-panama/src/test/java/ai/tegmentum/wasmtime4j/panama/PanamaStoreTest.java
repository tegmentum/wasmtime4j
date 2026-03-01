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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaStore} that exercise Panama-specific constructors, factory methods,
 * internal accessors, and callback registry type verification.
 */
@DisplayName("PanamaStore Tests")
class PanamaStoreTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaStoreTest.class.getName());

  private PanamaEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    resources.add(engine);
    LOGGER.info("Created PanamaEngine for store tests");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  private PanamaStore createStore() throws WasmException {
    final PanamaStore store = new PanamaStore(engine);
    resources.add(store);
    return store;
  }

  private PanamaModule compileWat(final String wat) throws WasmException {
    final PanamaModule module = (PanamaModule) engine.compileWat(wat);
    resources.add(module);
    return module;
  }

  private static final String FUNCTION_MODULE_WAT =
      """
      (module
        (func (export "get42") (result i32)
          i32.const 42
        )
      )
      """;

  // ===== Constructor Tests =====

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should reject null engine")
    void shouldRejectNullEngine() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaStore(null));
      assertThat(ex.getMessage()).contains("Engine cannot be null");
      LOGGER.info("Correctly rejected null engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject closed engine")
    void shouldRejectClosedEngine() throws Exception {
      final PanamaEngine closedEngine = new PanamaEngine();
      closedEngine.close();

      assertThrows(IllegalStateException.class, () -> new PanamaStore(closedEngine));
      LOGGER.info("Correctly rejected closed engine");
    }

    @Test
    @DisplayName("Should create valid store from engine")
    void shouldCreateValidStore() throws Exception {
      final PanamaStore store = createStore();

      assertTrue(store.isValid(), "Store should be valid after creation");
      assertNotNull(store.getNativeStore(), "Native store pointer should not be null");
      assertThat(store.getEngine()).isSameAs(engine);
      LOGGER.info("Successfully created valid store");
    }

    @Test
    @DisplayName("Should reject null engine with limits")
    void shouldRejectNullEngineWithLimits() throws Exception {
      final StoreLimits limits = StoreLimits.builder().build();

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaStore(null, limits));
      assertThat(ex.getMessage()).contains("Engine cannot be null");
      LOGGER.info("Correctly rejected null engine with limits");
    }

    @Test
    @DisplayName("Should reject null limits")
    void shouldRejectNullLimits() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaStore(engine, null));
      assertThat(ex.getMessage()).contains("Limits cannot be null");
      LOGGER.info("Correctly rejected null limits");
    }

    @Test
    @DisplayName("Should create store with limits")
    void shouldCreateStoreWithLimits() throws Exception {
      final StoreLimits limits =
          StoreLimits.builder().memorySize(1024 * 1024).instances(10).tableElements(100).build();
      final PanamaStore store = new PanamaStore(engine, limits);
      resources.add(store);

      assertTrue(store.isValid(), "Store with limits should be valid");
      LOGGER.info("Created store with limits successfully");
    }
  }

  // ===== forModule Factory Tests =====

  @Nested
  @DisplayName("forModule Factory Tests")
  class ForModuleTests {

    @Test
    @DisplayName("Should reject null module")
    void shouldRejectNullModule() throws Exception {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> PanamaStore.forModule(null));
      assertThat(ex.getMessage()).contains("module cannot be null");
      LOGGER.info("Correctly rejected null module");
    }

    @Test
    @DisplayName("forModule should create a store from a module")
    void shouldCreateStoreFromModule() throws Exception {
      final PanamaModule module = compileWat(FUNCTION_MODULE_WAT);

      final PanamaStore moduleStore = PanamaStore.forModule(module);
      assertNotNull(moduleStore, "Store created from module should not be null");
      assertNotNull(moduleStore.getEngine(), "Store engine should not be null");
      moduleStore.close();
      LOGGER.info("forModule successfully created store from module");
    }

    @Test
    @DisplayName("Should reject closed module")
    void shouldRejectClosedModule() throws Exception {
      final PanamaModule module = (PanamaModule) engine.compileWat(FUNCTION_MODULE_WAT);
      module.close();

      assertThrows(IllegalStateException.class, () -> PanamaStore.forModule(module));
      LOGGER.info("Correctly rejected closed module");
    }
  }

  // ===== Callback Registry Tests =====

  @Nested
  @DisplayName("Callback Registry Tests")
  class CallbackRegistryTests {

    @Test
    @DisplayName("Should lazily create callback registry")
    void shouldLazilyCreateCallbackRegistry() throws Exception {
      final PanamaStore store = createStore();

      final CallbackRegistry registry = store.getCallbackRegistry();
      assertNotNull(registry, "Callback registry should not be null");
      assertThat(registry).isInstanceOf(PanamaCallbackRegistry.class);
      LOGGER.info("Callback registry created lazily");
    }

    @Test
    @DisplayName("Should return same callback registry on subsequent calls")
    void shouldReturnSameCallbackRegistry() throws Exception {
      final PanamaStore store = createStore();

      final CallbackRegistry registry1 = store.getCallbackRegistry();
      final CallbackRegistry registry2 = store.getCallbackRegistry();
      assertThat(registry1).isSameAs(registry2);
      LOGGER.info("Callback registry is singleton per store");
    }
  }

  // ===== Lifecycle Tests =====

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("Should return engine reference")
    void shouldReturnEngineReference() throws Exception {
      final PanamaStore store = createStore();

      assertThat(store.getEngine()).isSameAs(engine);
      LOGGER.info("Store returns correct engine reference");
    }

    @Test
    @DisplayName("Should provide resource manager")
    void shouldProvideResourceManager() throws Exception {
      final PanamaStore store = createStore();

      assertNotNull(store.getResourceManager(), "Resource manager should not be null");
      LOGGER.info("Resource manager is available");
    }
  }
}
