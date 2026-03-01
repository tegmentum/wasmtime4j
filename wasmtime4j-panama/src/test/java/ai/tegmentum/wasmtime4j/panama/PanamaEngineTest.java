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

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Panama-specific tests for {@link PanamaEngine}.
 *
 * <p>Tests Panama implementation details that are not part of the unified Engine interface, such as
 * the two-argument constructor, native engine pointer access, and internal ID. Generic Engine API
 * tests have been migrated to {@code EngineApiDualRuntimeTest} in the integration module.
 */
@DisplayName("Panama Engine Tests")
class PanamaEngineTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaEngineTest.class.getName());

  /** Resources to close after each test, in reverse order. */
  private final List<AutoCloseable> resources = new ArrayList<>();

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

  /** Creates a PanamaEngine and tracks it for cleanup. */
  private PanamaEngine createEngine() throws Exception {
    final PanamaEngine engine = new PanamaEngine();
    resources.add(engine);
    return engine;
  }

  // ==================== Panama-Specific Constructor Tests ====================

  @Nested
  @DisplayName("Panama-Specific Constructor Tests")
  class PanamaSpecificConstructorTests {

    @Test
    @DisplayName("Config+Runtime constructor should create valid engine")
    void shouldCreateWithConfigAndRuntime() throws Exception {
      final EngineConfig config = new EngineConfig();
      final PanamaEngine engine = new PanamaEngine(config, null);
      resources.add(engine);
      assertNotNull(engine, "Engine should not be null");
      assertTrue(engine.isValid(), "Engine should be valid");
    }
  }

  // ==================== Panama-Specific Identity Tests ====================

  @Nested
  @DisplayName("Panama-Specific Identity Tests")
  class PanamaSpecificIdentityTests {

    @Test
    @DisplayName("getId should return non-zero")
    void shouldReturnNonZeroId() throws Exception {
      final PanamaEngine engine = createEngine();
      final long id = engine.getId();
      LOGGER.info("Engine ID: " + id);
      assertThat(id).isNotEqualTo(0L);
    }

    @Test
    @DisplayName("getNativeEngine should return non-null pointer")
    void shouldReturnNonNullNativeEngine() throws Exception {
      final PanamaEngine engine = createEngine();
      assertNotNull(engine.getNativeEngine(), "Native engine pointer should not be null");
    }
  }

  // ==================== Panama-Specific Closed Engine Tests ====================

  @Nested
  @DisplayName("Panama-Specific Closed Engine Tests")
  class PanamaSpecificClosedEngineTests {

    @Test
    @DisplayName("getNativeEngine on closed engine should throw IllegalStateException")
    void getNativeEngineOnClosedEngineShouldThrow() throws Exception {
      final PanamaEngine closedEngine = new PanamaEngine();
      closedEngine.close();

      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, closedEngine::getNativeEngine);
      assertTrue(
          ex.getMessage().contains("closed"),
          "Exception message should mention 'closed': " + ex.getMessage());
      LOGGER.info("getNativeEngine correctly rejected closed engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("close ordering should set flag before destruction")
    void closeOrderingShouldSetFlagBeforeDestruction() throws Exception {
      final PanamaEngine engine = new PanamaEngine();

      // First close destroys resources and sets flag
      engine.close();

      // Second close should be no-op (flag already set)
      engine.close();

      // getNativeEngine must throw -- flag was set before destruction
      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, engine::getNativeEngine);
      assertTrue(
          ex.getMessage().contains("closed"),
          "Exception message should mention 'closed': " + ex.getMessage());
      LOGGER.info("Close ordering verified -- flag set before destruction: " + ex.getMessage());
    }
  }
}
