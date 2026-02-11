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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link Linker#defineUnknownImportsAsTraps(Store, Module)} and
 * {@link Linker#defineUnknownImportsAsDefaultValues(Store, Module)}.
 *
 * <p>These methods allow instantiation of modules with unsatisfied imports by either trapping on
 * call (as traps) or returning default values (as defaults).
 *
 * @since 1.0.0
 */
@DisplayName("Linker Unknown Imports Tests")
public class LinkerUnknownImportsTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerUnknownImportsTest.class.getName());

  /**
   * WAT module importing two functions: one we define, one we leave undefined. Both have wrappers
   * so we can call each independently.
   */
  private static final String WAT =
      """
      (module
        (import "env" "undefined_func" (func $undef (param i32) (result i32)))
        (import "env" "defined_func" (func $def (result i32)))
        (func (export "call_undefined") (param i32) (result i32)
          local.get 0 call $undef)
        (func (export "call_defined") (result i32)
          call $def))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("defineUnknownImportsAsTraps allows instantiation with missing imports")
  void defineUnknownImportsAsTrapsInstantiatesSuccessfully(
      final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing defineUnknownImportsAsTraps instantiation");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final FunctionType defType = FunctionType.of(
          new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "defined_func", defType,
          params -> new WasmValue[]{WasmValue.i32(99)});

      final Module module = engine.compileWat(WAT);

      linker.defineUnknownImportsAsTraps(store, module);

      final Instance instance =
          assertDoesNotThrow(() -> linker.instantiate(store, module),
              "Instantiation should succeed after defineUnknownImportsAsTraps");
      assertNotNull(instance, "Instance should not be null");
      LOGGER.info("[" + runtime + "] Instantiation succeeded with traps for unknowns");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Trapped function throws WasmException on call")
  void trappedFunctionThrowsOnCall(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing trapped function throws on call");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final FunctionType defType = FunctionType.of(
          new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "defined_func", defType,
          params -> new WasmValue[]{WasmValue.i32(99)});

      final Module module = engine.compileWat(WAT);
      linker.defineUnknownImportsAsTraps(store, module);
      final Instance instance = linker.instantiate(store, module);

      final Exception ex = assertThrows(Exception.class,
          () -> instance.callFunction("call_undefined", WasmValue.i32(5)),
          "Calling trapped function should throw");
      LOGGER.info("[" + runtime + "] Trapped function threw: " + ex.getClass().getName()
          + " - " + ex.getMessage());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Defined function still works after defineUnknownImportsAsTraps")
  void definedFunctionStillWorksAfterTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing defined function works after traps");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final FunctionType defType = FunctionType.of(
          new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "defined_func", defType,
          params -> new WasmValue[]{WasmValue.i32(99)});

      final Module module = engine.compileWat(WAT);
      linker.defineUnknownImportsAsTraps(store, module);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result = instance.callFunction("call_defined");

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should have exactly 1 result");
      assertEquals(99, result[0].asInt(), "Defined function should return 99");
      LOGGER.info("[" + runtime + "] Defined function returned: " + result[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("defineUnknownImportsAsDefaultValues allows instantiation")
  void defineUnknownImportsAsDefaultValuesInstantiates(
      final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing defineUnknownImportsAsDefaultValues instantiation");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final FunctionType defType = FunctionType.of(
          new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "defined_func", defType,
          params -> new WasmValue[]{WasmValue.i32(99)});

      final Module module = engine.compileWat(WAT);

      linker.defineUnknownImportsAsDefaultValues(store, module);

      final Instance instance =
          assertDoesNotThrow(() -> linker.instantiate(store, module),
              "Instantiation should succeed after defineUnknownImportsAsDefaultValues");
      assertNotNull(instance, "Instance should not be null");
      LOGGER.info("[" + runtime + "] Instantiation succeeded with defaults for unknowns");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Default value function returns zero (default i32)")
  void defaultValueFunctionReturnsDefault(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing default value function returns zero");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final FunctionType defType = FunctionType.of(
          new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "defined_func", defType,
          params -> new WasmValue[]{WasmValue.i32(99)});

      final Module module = engine.compileWat(WAT);
      linker.defineUnknownImportsAsDefaultValues(store, module);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result =
          instance.callFunction("call_undefined", WasmValue.i32(5));

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should have exactly 1 result");
      assertEquals(0, result[0].asInt(),
          "Default value for i32 should be 0");
      LOGGER.info("[" + runtime + "] Default value function returned: " + result[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Defined function still works after defineUnknownImportsAsDefaultValues")
  void definedFunctionStillWorksAfterDefaults(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing defined function works after defaults");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      final FunctionType defType = FunctionType.of(
          new WasmValueType[]{}, new WasmValueType[]{WasmValueType.I32});
      linker.defineHostFunction("env", "defined_func", defType,
          params -> new WasmValue[]{WasmValue.i32(99)});

      final Module module = engine.compileWat(WAT);
      linker.defineUnknownImportsAsDefaultValues(store, module);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] result = instance.callFunction("call_defined");

      assertNotNull(result, "Result should not be null");
      assertEquals(1, result.length, "Should have exactly 1 result");
      assertEquals(99, result[0].asInt(), "Defined function should return 99");
      LOGGER.info("[" + runtime + "] Defined function returned: " + result[0].asInt());

      instance.close();
      module.close();
    }
  }
}
