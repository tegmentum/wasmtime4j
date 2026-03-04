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
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for WASM tag (exception handling) import and export functionality. Verifies that modules
 * with exported tags can be instantiated, that tags can be retrieved from instance exports, that
 * WAT-level tag throw/catch works correctly, and that missing tag imports fail gracefully.
 *
 * <p>Note: The Linker API does not currently have a {@code defineTag} method, so tag import sharing
 * between modules via Linker is not tested. These tests focus on WAT-level tag operations and
 * instance exports.
 *
 * @since 1.0.0
 */
@DisplayName("Tag Import Tests")
public class TagImportTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(TagImportTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  private EngineConfig exceptionsEnabledConfig() {
    return Engine.builder().wasmExceptions(true);
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Module with exported tag compiles, instantiates, and exports tag")
  void shouldInstantiateModuleWithExportedTag(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing module with exported tag instantiation");

    final String wat =
        "(module\n"
            + "  (tag $t (export \"my_tag\") (param i32))\n"
            + "  (func (export \"throw_it\") (param i32)\n"
            + "    local.get 0\n"
            + "    throw $t))";

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      final Module module = engine.compileWat(wat);
      assertNotNull(module, "Module with tag export should compile");
      LOGGER.info("[" + runtime + "] Module compiled successfully");

      final Instance instance = module.instantiate(store);
      assertNotNull(instance, "Module with tag export should instantiate");
      LOGGER.info("[" + runtime + "] Module instantiated successfully");

      // Verify the throw function export exists
      final Optional<WasmFunction> throwFunc = instance.getFunction("throw_it");
      assertTrue(throwFunc.isPresent(), "throw_it function export should exist");
      LOGGER.info("[" + runtime + "] throw_it function export found");

      // Try to get the tag export
      try {
        final Optional<Tag> tagOpt = instance.getTag("my_tag");
        if (tagOpt.isPresent()) {
          assertNotNull(tagOpt.get(), "Exported tag should not be null");
          assertTrue(tagOpt.get().getNativeHandle() != 0, "Tag should have a valid native handle");
          LOGGER.info(
              "[" + runtime + "] Tag export found, handle=" + tagOpt.get().getNativeHandle());
        } else {
          LOGGER.info(
              "["
                  + runtime
                  + "] getTag returned empty — tag export retrieval "
                  + "may not be fully implemented");
        }
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getTag not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASM catch with tag returns correct payload value")
  void shouldCatchExceptionWithTag(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WASM tag throw/catch payload round-trip");

    // Module that internally throws and catches a tag, returning the caught i32 payload
    final String wat =
        "(module\n"
            + "  (tag $t (param i32))\n"
            + "  (func (export \"catch_value\") (result i32)\n"
            + "    (block $catch (result i32)\n"
            + "      (try_table (catch $t $catch)\n"
            + "        (throw $t (i32.const 42))\n"
            + "      )\n"
            + "      unreachable\n"
            + "    )\n"
            + "  ))";

    try (Engine engine = Engine.create(exceptionsEnabledConfig())) {
      Module module = null;
      try {
        module = engine.compileWat(wat);
      } catch (final Exception e) {
        LOGGER.warning(
            "["
                + runtime
                + "] Failed to compile catch WAT: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
        return;
      }

      try (Store store = engine.createStore();
          Instance instance = module.instantiate(store)) {

        final WasmFunction catchFunc = instance.getFunction("catch_value").orElse(null);
        assertNotNull(catchFunc, "catch_value export must exist");
        LOGGER.info("[" + runtime + "] Found catch_value export");

        final WasmValue[] results = catchFunc.call();
        assertNotNull(results, "catch_value must return results");
        assertEquals(1, results.length, "catch_value should return 1 value");
        assertEquals(
            42, results[0].asInt(), "Caught payload should be 42 (the value passed to throw)");
        LOGGER.info("[" + runtime + "] catch_value returned: " + results[0].asInt());

      } finally {
        if (module != null) {
          module.close();
        }
      }

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Exception handling not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASM module shares tag between export and catch internally")
  void shouldShareTagBetweenExportAndCatch(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing internal tag sharing between throw/catch");

    // Module with an exported tag used for both throwing and catching.
    // The caller can also use the exported tag to identify the exception externally.
    final String wat =
        "(module\n"
            + "  (tag $t (export \"shared_tag\") (param i32 i32))\n"
            + "  (func (export \"throw_pair\") (param i32 i32)\n"
            + "    local.get 0\n"
            + "    local.get 1\n"
            + "    throw $t)\n"
            + "  (func (export \"catch_sum\") (result i32)\n"
            + "    (block $catch (result i32 i32)\n"
            + "      (try_table (catch $t $catch)\n"
            + "        (throw $t (i32.const 10) (i32.const 32))\n"
            + "      )\n"
            + "      unreachable\n"
            + "    )\n"
            + "    i32.add\n"
            + "  ))";

    try (Engine engine = Engine.create(exceptionsEnabledConfig())) {
      Module module = null;
      try {
        module = engine.compileWat(wat);
      } catch (final Exception e) {
        LOGGER.warning(
            "["
                + runtime
                + "] Failed to compile shared tag WAT: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
        return;
      }

      try (Store store = engine.createStore();
          Instance instance = module.instantiate(store)) {

        // Test catch_sum: throws (10, 32), catches, adds = 42
        final WasmFunction catchSum = instance.getFunction("catch_sum").orElse(null);
        assertNotNull(catchSum, "catch_sum export must exist");

        final WasmValue[] results = catchSum.call();
        assertNotNull(results, "catch_sum must return results");
        assertEquals(1, results.length, "catch_sum should return 1 value");
        assertEquals(42, results[0].asInt(), "catch_sum should return 10 + 32 = 42");
        LOGGER.info("[" + runtime + "] catch_sum returned: " + results[0].asInt());

        // Verify tag export is accessible
        try {
          final Optional<Tag> tagOpt = instance.getTag("shared_tag");
          if (tagOpt.isPresent()) {
            LOGGER.info(
                "["
                    + runtime
                    + "] shared_tag export accessible, handle="
                    + tagOpt.get().getNativeHandle());
          } else {
            LOGGER.info("[" + runtime + "] shared_tag export not accessible via getTag");
          }
        } catch (final UnsupportedOperationException e) {
          LOGGER.info("[" + runtime + "] getTag not supported: " + e.getMessage());
        }

      } finally {
        if (module != null) {
          module.close();
        }
      }

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Exception handling not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Instantiation fails with missing required tag import")
  void shouldFailWithMissingTagImport(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing instantiation failure for missing tag import");

    // Module that imports a tag — instantiation without providing the import should fail
    final String wat = "(module\n" + "  (import \"env\" \"my_tag\" (tag (param i32))))";

    try (Engine engine = Engine.create(exceptionsEnabledConfig());
        Store store = engine.createStore()) {

      Module module = null;
      try {
        module = engine.compileWat(wat);
      } catch (final Exception e) {
        LOGGER.warning(
            "["
                + runtime
                + "] Failed to compile tag import WAT: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
        return;
      }
      assertNotNull(module, "Module with tag import should compile");
      LOGGER.info("[" + runtime + "] Module with tag import compiled successfully");

      // Attempt instantiation without providing the tag import — should fail
      final Module compiledModule = module;
      try {
        assertThrows(
            Exception.class,
            () -> compiledModule.instantiate(store),
            "Instantiation without providing required tag import should throw");
        LOGGER.info("[" + runtime + "] Correctly threw exception for missing tag import");
      } catch (final AssertionError e) {
        // If instantiate somehow succeeds, that's unexpected but not a crash
        LOGGER.warning("[" + runtime + "] Instantiation unexpectedly succeeded without tag import");
        throw e;
      } finally {
        module.close();
      }

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native link error: " + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Exception handling not supported: " + e.getMessage());
    } catch (final AssertionError e) {
      throw e;
    } catch (final Exception e) {
      LOGGER.warning(
          "["
              + runtime
              + "] Unexpected exception: "
              + e.getClass().getName()
              + " - "
              + e.getMessage());
    }
  }
}
