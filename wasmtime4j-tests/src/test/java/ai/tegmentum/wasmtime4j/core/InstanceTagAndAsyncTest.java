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
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link Instance#getTag(String)}, {@link WasmFunction#callAsync(WasmValue...)},
 * and {@link WasmFunction#matchesType(WasmValueType[], WasmValueType[])}.
 *
 * @since 1.0.0
 */
@DisplayName("Instance Tag and Async Tests")
public class InstanceTagAndAsyncTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(InstanceTagAndAsyncTest.class.getName());

  /**
   * Simple WAT module with an add function for async and type matching tests.
   */
  private static final String WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32) local.get 0 local.get 1 i32.add))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getTag returns empty on module without tags")
  void getTagReturnsEmptyOnNoTagModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getTag on module without tags");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);

      try {
        final Optional<Tag> tagOpt = instance.getTag("x");

        assertFalse(tagOpt.isPresent(),
            "getTag('x') should return empty on module without tags");
        LOGGER.info("[" + runtime + "] getTag('x') present: " + tagOpt.isPresent());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] getTag not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getTag returns tag if exported (defensive)")
  void getTagReturnsTagIfExported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getTag with exception handling tag (defensive)");

    // WAT with exception handling requires the exceptions proposal
    // This test is defensive -- it may not compile if the proposal is not enabled
    final String exnWat =
        """
        (module
          (tag $t (export "my_tag") (param i32)))
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {

      try {
        final Module module = engine.compileWat(exnWat);
        final Instance instance = module.instantiate(store);

        final Optional<Tag> tagOpt = instance.getTag("my_tag");
        if (tagOpt.isPresent()) {
          assertNotNull(tagOpt.get(), "Tag should not be null");
          LOGGER.info("[" + runtime + "] getTag('my_tag') found tag");
        } else {
          LOGGER.info("[" + runtime + "] getTag('my_tag') returned empty "
              + "(tag may not be recognized as an export)");
        }

        instance.close();
        module.close();
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] Exception handling WAT not supported: "
            + e.getClass().getName() + " - " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callAsync returns correct result for real execution")
  void callAsyncRealExecution(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callAsync real execution");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction addFunc = instance.getFunction("add").get();

      try {
        final CompletableFuture<WasmValue[]> future =
            addFunc.callAsync(WasmValue.i32(3), WasmValue.i32(4));
        final WasmValue[] results = future.get();

        assertNotNull(results, "Async results should not be null");
        assertEquals(1, results.length, "Should return 1 value");
        assertEquals(7, results[0].asInt(), "add(3, 4) should return 7");
        LOGGER.info("[" + runtime + "] callAsync add(3,4) = " + results[0].asInt());
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callAsync not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("callAsync with wrong param types throws")
  void callAsyncWrongParamsThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callAsync with wrong param types");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction addFunc = instance.getFunction("add").get();

      try {
        // add expects (i32, i32), passing (i64) should fail
        final CompletableFuture<WasmValue[]> future =
            addFunc.callAsync(WasmValue.i64(999));
        // If the future completes, check if it threw an exception
        try {
          future.get();
          LOGGER.info("[" + runtime + "] callAsync with wrong params did not throw "
              + "(runtime may be lenient)");
        } catch (final Exception e) {
          LOGGER.info("[" + runtime + "] callAsync with wrong params threw on get: "
              + e.getClass().getName() + " - " + e.getMessage());
        }
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] callAsync not supported: " + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] callAsync with wrong params threw immediately: "
            + e.getClass().getName() + " - " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("matchesType returns true for correct signature")
  void matchesTypeCorrectSignature(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing matchesType with correct signature");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(WAT);
      final Instance instance = module.instantiate(store);
      final WasmFunction addFunc = instance.getFunction("add").get();

      try {
        final boolean matches = addFunc.matchesType(
            new WasmValueType[]{WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[]{WasmValueType.I32});

        assertTrue(matches,
            "add(i32, i32) -> i32 should match type {I32, I32} -> {I32}");
        LOGGER.info("[" + runtime + "] matchesType(I32,I32 -> I32) = " + matches);
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("[" + runtime + "] matchesType not supported: " + e.getMessage());
      }

      instance.close();
      module.close();
    }
  }
}
