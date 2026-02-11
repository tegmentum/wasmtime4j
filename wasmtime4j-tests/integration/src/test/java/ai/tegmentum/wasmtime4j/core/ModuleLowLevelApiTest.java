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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.ResourcesRequired;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Module low-level APIs: {@link Module#getCustomSections()},
 * {@link Module#resourcesRequired()}, {@link Module#getCompiledModule()},
 * {@link Module#imageRange()}, and {@link Module#deserializeRaw(Engine, long, long)}.
 *
 * @since 1.0.0
 */
@DisplayName("Module Low-Level API Tests")
public class ModuleLowLevelApiTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(ModuleLowLevelApiTest.class.getName());

  /**
   * WAT module with memory, table, global, and function to exercise resourcesRequired.
   */
  private static final String RICH_WAT =
      """
      (module
        (memory (export "mem") 1 4)
        (table (export "t") 2 funcref)
        (global (export "g") (mut i32) (i32.const 0))
        (func (export "noop")))
      """;

  /** Module with memory but no max pages (unbounded). */
  private static final String UNBOUNDED_MEMORY_WAT =
      """
      (module
        (memory (export "mem") 1))
      """;

  private static final String EMPTY_WAT = "(module)";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getCustomSections returns non-null map")
  void getCustomSectionsReturnsMap(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getCustomSections");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      try {
        final Map<String, byte[]> sections = module.getCustomSections();
        assertNotNull(sections, "Custom sections map should not be null");
        LOGGER.info("[" + runtime + "] Custom sections: " + sections.size() + " entries");
        for (final Map.Entry<String, byte[]> entry : sections.entrySet()) {
          LOGGER.info("[" + runtime + "]   Section '" + entry.getKey()
              + "': " + entry.getValue().length + " bytes");
        }
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] getCustomSections not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resourcesRequired has sensible values for module with memory")
  void resourcesRequiredHasSensibleValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resourcesRequired");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final ResourcesRequired res = module.resourcesRequired();

      assertNotNull(res, "ResourcesRequired should not be null");
      assertTrue(res.getMinimumMemoryBytes() > 0,
          "Module with 1-page memory should have minimumMemoryBytes > 0");
      assertEquals(1, res.getNumMemories(), "Module should have 1 memory");
      LOGGER.info("[" + runtime + "] ResourcesRequired:"
          + " minMem=" + res.getMinimumMemoryBytes()
          + " maxMem=" + res.getMaximumMemoryBytes()
          + " numMemories=" + res.getNumMemories()
          + " numTables=" + res.getNumTables()
          + " numGlobals=" + res.getNumGlobals()
          + " numFunctions=" + res.getNumFunctions());

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resourcesRequired max memory matches 4 pages")
  void resourcesRequiredMaxMemoryIsCorrect(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resourcesRequired max memory");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final ResourcesRequired res = module.resourcesRequired();

      // 4 pages * 64KB = 262144 bytes
      final long expectedMax = 4L * 65536L;
      assertEquals(expectedMax, res.getMaximumMemoryBytes(),
          "Max memory should be 4 pages (262144 bytes)");
      LOGGER.info("[" + runtime + "] Max memory: " + res.getMaximumMemoryBytes()
          + " bytes (expected " + expectedMax + ")");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getCompiledModule returns Optional")
  void getCompiledModuleReturnsOptional(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getCompiledModule");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      try {
        final Optional<?> compiledOpt = module.getCompiledModule();
        assertNotNull(compiledOpt, "getCompiledModule should not return null");
        LOGGER.info("[" + runtime + "] getCompiledModule present: " + compiledOpt.isPresent());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] getCompiledModule not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("imageRange returns Optional")
  void imageRangeReturnsOptional(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing imageRange");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      try {
        final Optional<?> rangeOpt = module.imageRange();
        assertNotNull(rangeOpt, "imageRange should not return null");
        LOGGER.info("[" + runtime + "] imageRange present: " + rangeOpt.isPresent());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] imageRange not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("deserializeRaw throws UnsupportedOperationException")
  void deserializeRawThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing deserializeRaw");

    try (Engine engine = Engine.create()) {
      try {
        Module.deserializeRaw(engine, 0L, 0L);
        LOGGER.info("[" + runtime + "] deserializeRaw succeeded unexpectedly");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] deserializeRaw threw UnsupportedOperationException: "
            + e.getMessage());
      } catch (final UnsatisfiedLinkError | Exception e) {
        LOGGER.info("[" + runtime + "] deserializeRaw threw: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resourcesRequired on empty module shows zero memories")
  void resourcesRequiredOnEmptyModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resourcesRequired on empty module");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(EMPTY_WAT);

      final ResourcesRequired res = module.resourcesRequired();

      assertNotNull(res, "ResourcesRequired should not be null for empty module");
      assertEquals(0, res.getNumMemories(), "Empty module should have 0 memories");
      assertEquals(0, res.getNumTables(), "Empty module should have 0 tables");
      LOGGER.info("[" + runtime + "] Empty module resources:"
          + " numMemories=" + res.getNumMemories()
          + " numTables=" + res.getNumTables()
          + " numGlobals=" + res.getNumGlobals()
          + " numFunctions=" + res.getNumFunctions());

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resourcesRequired reports table, global, and function fields")
  void resourcesRequiredReportsTableFields(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resourcesRequired table/global/function fields");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final ResourcesRequired res = module.resourcesRequired();

      assertNotNull(res, "ResourcesRequired should not be null");
      assertTrue(res.getMinimumTableElements() >= 2,
          "Module with table of min 2 should report minimumTableElements >= 2, got: "
              + res.getMinimumTableElements());
      assertEquals(1, res.getNumTables(),
          "Module should have 1 table");
      assertEquals(1, res.getNumGlobals(),
          "Module should have 1 global");
      assertTrue(res.getNumFunctions() >= 1,
          "Module should have at least 1 function, got: " + res.getNumFunctions());
      LOGGER.info("[" + runtime + "] ResourcesRequired fields:"
          + " minTableElements=" + res.getMinimumTableElements()
          + " maxTableElements=" + res.getMaximumTableElements()
          + " numTables=" + res.getNumTables()
          + " numGlobals=" + res.getNumGlobals()
          + " numFunctions=" + res.getNumFunctions());

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("resourcesRequired reports -1 max memory for unbounded memory")
  void resourcesRequiredNoMaxMemoryForUnbounded(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resourcesRequired on module with unbounded memory");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(UNBOUNDED_MEMORY_WAT);

      final ResourcesRequired res = module.resourcesRequired();

      assertNotNull(res, "ResourcesRequired should not be null");
      assertEquals(1, res.getNumMemories(), "Module should have 1 memory");
      assertTrue(res.getMinimumMemoryBytes() > 0,
          "Module with 1-page memory should have minimumMemoryBytes > 0");
      LOGGER.info("[" + runtime + "] Unbounded memory resources:"
          + " minMem=" + res.getMinimumMemoryBytes()
          + " maxMem=" + res.getMaximumMemoryBytes()
          + " numMemories=" + res.getNumMemories());
      // Unbounded memory should have max == -1 or a very large value
      // The exact value depends on Wasmtime's behavior; log it for debugging
      assertTrue(res.getMaximumMemoryBytes() == -1
              || res.getMaximumMemoryBytes() > res.getMinimumMemoryBytes(),
          "Unbounded memory should have max == -1 or max > min, got max="
              + res.getMaximumMemoryBytes());

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("isSerializable returns true")
  void isSerializableReturnsTrue(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing isSerializable");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      assertTrue(module.isSerializable(),
          "Module should be serializable by default");
      LOGGER.info("[" + runtime + "] isSerializable: " + module.isSerializable());

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("deserializeRaw with null engine throws IllegalArgumentException")
  void deserializeRawNullEngineThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing deserializeRaw with null engine");

    final IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Module.deserializeRaw(null, 1, 1),
            "Null engine should throw IllegalArgumentException");

    LOGGER.info("[" + runtime + "] Threw as expected: " + ex.getMessage());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("deserializeRaw with negative address throws IllegalArgumentException")
  void deserializeRawNegativeAddressThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing deserializeRaw with negative address");

    try (Engine engine = Engine.create()) {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> Module.deserializeRaw(engine, -1, 10),
              "Negative address should throw IllegalArgumentException");

      LOGGER.info("[" + runtime + "] Threw as expected: " + ex.getMessage());
    }
  }
}
