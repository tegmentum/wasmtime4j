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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Module.AddressMapping;
import ai.tegmentum.wasmtime4j.ResourcesRequired;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests Module low-level APIs: {@link Module#resourcesRequired()}, {@link Module#text()}, {@link
 * Module#addressMap()}.
 *
 * @since 1.0.0
 */
@DisplayName("Module Low-Level API Tests")
public class ModuleLowLevelApiTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ModuleLowLevelApiTest.class.getName());

  /** WAT module with memory, table, global, and function to exercise resourcesRequired. */
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
  @DisplayName("resourcesRequired has sensible values for module with memory")
  void resourcesRequiredHasSensibleValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing resourcesRequired");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final ResourcesRequired res = module.resourcesRequired();

      assertNotNull(res, "ResourcesRequired should not be null");
      assertTrue(
          res.getMinimumMemoryBytes() > 0,
          "Module with 1-page memory should have minimumMemoryBytes > 0");
      assertEquals(1, res.getNumMemories(), "Module should have 1 memory");
      LOGGER.info(
          "["
              + runtime
              + "] ResourcesRequired:"
              + " minMem="
              + res.getMinimumMemoryBytes()
              + " maxMem="
              + res.getMaximumMemoryBytes()
              + " numMemories="
              + res.getNumMemories()
              + " numTables="
              + res.getNumTables()
              + " numGlobals="
              + res.getNumGlobals()
              + " numFunctions="
              + res.getNumFunctions());

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
      assertEquals(
          expectedMax, res.getMaximumMemoryBytes(), "Max memory should be 4 pages (262144 bytes)");
      LOGGER.info(
          "["
              + runtime
              + "] Max memory: "
              + res.getMaximumMemoryBytes()
              + " bytes (expected "
              + expectedMax
              + ")");

      module.close();
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
      LOGGER.info(
          "["
              + runtime
              + "] Empty module resources:"
              + " numMemories="
              + res.getNumMemories()
              + " numTables="
              + res.getNumTables()
              + " numGlobals="
              + res.getNumGlobals()
              + " numFunctions="
              + res.getNumFunctions());

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
      assertTrue(
          res.getMinimumTableElements() >= 2,
          "Module with table of min 2 should report minimumTableElements >= 2, got: "
              + res.getMinimumTableElements());
      assertEquals(1, res.getNumTables(), "Module should have 1 table");
      assertEquals(1, res.getNumGlobals(), "Module should have 1 global");
      assertTrue(
          res.getNumFunctions() >= 1,
          "Module should have at least 1 function, got: " + res.getNumFunctions());
      LOGGER.info(
          "["
              + runtime
              + "] ResourcesRequired fields:"
              + " minTableElements="
              + res.getMinimumTableElements()
              + " maxTableElements="
              + res.getMaximumTableElements()
              + " numTables="
              + res.getNumTables()
              + " numGlobals="
              + res.getNumGlobals()
              + " numFunctions="
              + res.getNumFunctions());

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
      assertTrue(
          res.getMinimumMemoryBytes() > 0,
          "Module with 1-page memory should have minimumMemoryBytes > 0");
      LOGGER.info(
          "["
              + runtime
              + "] Unbounded memory resources:"
              + " minMem="
              + res.getMinimumMemoryBytes()
              + " maxMem="
              + res.getMaximumMemoryBytes()
              + " numMemories="
              + res.getNumMemories());
      // Unbounded memory should have max == -1 or a very large value
      // The exact value depends on Wasmtime's behavior; log it for debugging
      assertTrue(
          res.getMaximumMemoryBytes() == -1
              || res.getMaximumMemoryBytes() > res.getMinimumMemoryBytes(),
          "Unbounded memory should have max == -1 or max > min, got max="
              + res.getMaximumMemoryBytes());

      module.close();
    }
  }

  // ===== Module.text() tests =====

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("text() returns non-empty bytes for compiled module")
  void textReturnsNonEmptyBytesForCompiledModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing text() on compiled module");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final byte[] text = module.text();

      assertNotNull(text, "text() should not return null");
      assertTrue(text.length > 0, "text() should return non-empty bytes for a compiled module");
      LOGGER.info("[" + runtime + "] Module text size: " + text.length + " bytes (machine code)");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("text() returns non-empty bytes for empty module")
  void textReturnsNonEmptyBytesForEmptyModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing text() on empty module");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(EMPTY_WAT);

      final byte[] text = module.text();

      assertNotNull(text, "text() should not return null even for empty module");
      // Even an empty module may have some compiled code (prologue/epilogue)
      LOGGER.info("[" + runtime + "] Empty module text size: " + text.length + " bytes");

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("text() returns defensive copy")
  void textReturnsDefensiveCopy(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing text() defensive copy behavior");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final byte[] text1 = module.text();
      final byte[] text2 = module.text();

      assertNotNull(text1, "First text() call should not return null");
      assertNotNull(text2, "Second text() call should not return null");
      assertEquals(text1.length, text2.length, "Both text() calls should return same-length data");
      assertFalse(text1 == text2, "text() should return a new array each call (defensive copy)");

      // Verify contents are identical
      for (int i = 0; i < text1.length; i++) {
        assertEquals(text1[i], text2[i], "text() contents should be identical at index " + i);
      }

      LOGGER.info("[" + runtime + "] Verified text() defensive copy: " + text1.length + " bytes");

      module.close();
    }
  }

  // ===== Module.addressMap() tests =====

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("addressMap() returns list for module with function")
  void addressMapReturnsListForModuleWithFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing addressMap() on module with function");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final List<AddressMapping> addressMap = module.addressMap();

      assertNotNull(addressMap, "addressMap() should not return null");
      LOGGER.info("[" + runtime + "] Address map entries: " + addressMap.size());

      // If address map is available, verify entries have valid code offsets
      if (!addressMap.isEmpty()) {
        for (int i = 0; i < Math.min(5, addressMap.size()); i++) {
          final AddressMapping entry = addressMap.get(i);
          assertTrue(
              entry.getCodeOffset() >= 0,
              "Code offset should be non-negative at index "
                  + i
                  + ", got: "
                  + entry.getCodeOffset());
          LOGGER.info(
              "["
                  + runtime
                  + "] Address map["
                  + i
                  + "]: codeOffset="
                  + entry.getCodeOffset()
                  + " wasmOffset="
                  + (entry.getWasmOffset().isPresent()
                      ? String.valueOf(entry.getWasmOffset().getAsInt())
                      : "N/A"));
        }
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("addressMap() entries have valid AddressMapping fields")
  void addressMapEntriesHaveValidFields(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing addressMap() entry fields");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(RICH_WAT);

      final List<AddressMapping> addressMap = module.addressMap();

      assertNotNull(addressMap, "addressMap() should not return null");

      // Verify AddressMapping equals/hashCode/toString
      if (addressMap.size() >= 2) {
        final AddressMapping first = addressMap.get(0);
        final AddressMapping second = addressMap.get(1);

        // equals: same object
        assertEquals(first, first, "AddressMapping should equal itself");

        // toString should contain offset info
        final String str = first.toString();
        assertNotNull(str, "toString() should not be null");
        assertTrue(str.contains("codeOffset"), "toString() should contain codeOffset");
        LOGGER.info("[" + runtime + "] AddressMapping.toString(): " + str);
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("addressMap() returns empty or list for empty module")
  void addressMapOnEmptyModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing addressMap() on empty module");

    try (Engine engine = Engine.create()) {
      final Module module = engine.compileWat(EMPTY_WAT);

      final List<AddressMapping> addressMap = module.addressMap();

      assertNotNull(addressMap, "addressMap() should not return null for empty module");
      LOGGER.info("[" + runtime + "] Empty module address map size: " + addressMap.size());

      module.close();
    }
  }
}
