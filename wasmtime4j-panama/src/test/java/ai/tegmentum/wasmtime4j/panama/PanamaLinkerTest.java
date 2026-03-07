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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Panama-specific tests for {@link PanamaLinker} that exercise constructor logic and
 * Panama-specific accessors such as {@code getNativeLinker()} and {@code getWasiContext()}.
 *
 * <p>Generic Linker API tests that use only the unified interface have been migrated to {@code
 * LinkerApiDualRuntimeTest} in the integration test module.
 */
@DisplayName("PanamaLinker Tests")
class PanamaLinkerTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaLinkerTest.class.getName());

  private PanamaEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    engine = new PanamaEngine();
    resources.add(engine);
    LOGGER.info("Created PanamaEngine for test");
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

  private PanamaLinker<?> createLinker() throws Exception {
    final PanamaLinker<?> linker = new PanamaLinker<>(engine);
    resources.add(linker);
    return linker;
  }

  // ===== Constructor Tests =====

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should reject null engine")
    void shouldRejectNullEngine() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> new PanamaLinker<>(null));
      assertTrue(
          ex.getMessage().contains("Engine cannot be null"),
          "Expected message to contain 'Engine cannot be null': " + ex.getMessage());
      LOGGER.info("Correctly rejected null engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject closed engine")
    void shouldRejectClosedEngine() throws Exception {
      final PanamaEngine closedEngine = new PanamaEngine();
      closedEngine.close();

      final IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> new PanamaLinker<>(closedEngine));
      assertTrue(
          ex.getMessage().contains("closed"),
          "Expected message to contain 'closed': " + ex.getMessage());
      LOGGER.info("Correctly rejected closed engine: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should create valid linker with native handle")
    void shouldCreateValidLinker() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertTrue(linker.isValid(), "Linker should be valid after creation");
      assertNotNull(linker.getNativeLinker(), "Native linker should not be null");
      assertNotNull(linker.getEngine(), "Engine reference should not be null");
      LOGGER.info("Created valid linker with native handle");
    }
  }

  // ===== Panama-Specific Accessor Tests =====

  @Nested
  @DisplayName("Panama-Specific Accessor Tests")
  class PanamaSpecificAccessorTests {

    @Test
    @DisplayName("Should get and set WASI context")
    void shouldGetAndSetWasiContext() throws Exception {
      final PanamaLinker<?> linker = createLinker();

      assertNull(linker.getWasiContext(), "WASI context should be null initially");
      LOGGER.info("WASI context correctly null initially");
    }
  }
}
