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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
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
 * Tests that try-with-resources properly cleans up nested WebAssembly resources. Verifies that
 * resources are released even when exceptions occur during instantiation or execution.
 *
 * @since 1.0.0
 */
@DisplayName("Try-With-Resources Nesting Tests")
public class TryWithResourcesTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(TryWithResourcesTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Standard nesting: Engine > Store > Module > Instance")
  void standardNesting(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing standard try-with-resources nesting");

    try (Engine engine = Engine.create()) {
      LOGGER.info("[" + runtime + "] Engine created");
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "add") (param i32 i32) (result i32)
              local.get 0 local.get 1 i32.add))
          """);
      LOGGER.info("[" + runtime + "] Module compiled");

      try (Store store = engine.createStore()) {
        LOGGER.info("[" + runtime + "] Store created");
        final Instance instance = module.instantiate(store);
        LOGGER.info("[" + runtime + "] Instance created");

        final WasmValue[] result =
            instance.callFunction("add", WasmValue.i32(10), WasmValue.i32(32));
        assertEquals(42, result[0].asInt(), "add(10, 32) should return 42");
        LOGGER.info("[" + runtime + "] Function call successful: " + result[0].asInt());

        instance.close();
      }
      module.close();
    }
    LOGGER.info("[" + runtime + "] All resources cleaned up via try-with-resources");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Exception during instantiation cleans up prior resources")
  void exceptionDuringInstantiation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cleanup after instantiation exception");

    try (Engine engine = Engine.create()) {
      // Module that requires an import it won't get
      final Module module =
          engine.compileWat(
              """
          (module
            (import "env" "missing_func" (func (result i32)))
            (func (export "call_it") (result i32)
              call 0))
          """);

      try (Store store = engine.createStore()) {
        LOGGER.info("[" + runtime + "] Attempting to instantiate module with missing import");

        // Instantiation should fail because the import is not provided
        assertThrows(
            Exception.class,
            () -> module.instantiate(store),
            "Instantiation without required imports should throw");
        LOGGER.info("[" + runtime + "] Expected exception thrown for missing import");
      }
      LOGGER.info("[" + runtime + "] Store cleaned up via try-with-resources");
      module.close();
    }
    LOGGER.info("[" + runtime + "] Engine cleaned up via try-with-resources");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Exception during function call cleans up instance and store")
  void exceptionDuringFunctionCall(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cleanup after function call trap");

    try (Engine engine = Engine.create()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "trap")
              unreachable))
          """);

      try (Store store = engine.createStore()) {
        final Instance instance = module.instantiate(store);
        LOGGER.info("[" + runtime + "] Calling function that traps");

        try {
          instance.callFunction("trap");
          fail("Function should have trapped");
        } catch (final Exception e) {
          LOGGER.info(
              "[" + runtime + "] Expected trap: " + e.getClass().getSimpleName()
                  + " - " + e.getMessage());
        }

        // Instance should still be closeable after trap
        assertDoesNotThrow(instance::close, "Instance close after trap should not crash");
        LOGGER.info("[" + runtime + "] Instance closed after trap");
      }
      LOGGER.info("[" + runtime + "] Store cleaned up after trap");
      module.close();
    }
    LOGGER.info("[" + runtime + "] All resources cleaned up after function call trap");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple instances from same module close individually")
  void multipleInstancesFromSameModule(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple instances from same module");

    try (Engine engine = Engine.create()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "get_val") (result i32)
              i32.const 99))
          """);

      try (Store store1 = engine.createStore();
          Store store2 = engine.createStore();
          Store store3 = engine.createStore()) {

        final Instance inst1 = module.instantiate(store1);
        final Instance inst2 = module.instantiate(store2);
        final Instance inst3 = module.instantiate(store3);

        LOGGER.info("[" + runtime + "] Created 3 instances from same module");

        // Verify all work
        assertEquals(99, inst1.callFunction("get_val")[0].asInt());
        assertEquals(99, inst2.callFunction("get_val")[0].asInt());
        assertEquals(99, inst3.callFunction("get_val")[0].asInt());
        LOGGER.info("[" + runtime + "] All 3 instances returned correct values");

        // Close in different order
        inst2.close();
        LOGGER.info("[" + runtime + "] Closed instance 2");

        // Remaining instances should still work
        assertEquals(99, inst1.callFunction("get_val")[0].asInt());
        assertEquals(99, inst3.callFunction("get_val")[0].asInt());
        LOGGER.info("[" + runtime + "] Remaining instances still work after closing inst2");

        inst1.close();
        inst3.close();
        LOGGER.info("[" + runtime + "] All instances closed");
      }
      module.close();
    }
    LOGGER.info("[" + runtime + "] All resources cleaned up");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Linker in try-with-resources scope")
  void linkerInTryWithResources(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Linker in try-with-resources");

    try (Engine engine = Engine.create()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (import "env" "get_val" (func (result i32)))
            (func (export "call_host") (result i32)
              call 0))
          """);

      try (Linker<Void> linker = Linker.create(engine)) {
        linker.defineHostFunction(
            "env",
            "get_val",
            FunctionType.of(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32}),
            HostFunction.singleValue(params -> WasmValue.i32(77)));
        LOGGER.info("[" + runtime + "] Host function defined in linker");

        try (Store store = engine.createStore()) {
          final Instance instance = linker.instantiate(store, module);
          final WasmValue[] result = instance.callFunction("call_host");
          assertEquals(77, result[0].asInt(), "Host function should return 77");
          LOGGER.info("[" + runtime + "] Host function call returned: " + result[0].asInt());
          instance.close();
        }
      }
      LOGGER.info("[" + runtime + "] Linker closed via try-with-resources");
      module.close();
    }
    LOGGER.info("[" + runtime + "] All resources cleaned up");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Nested try-with-resources with early return")
  void nestedTryWithResourcesEarlyReturn(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing nested try-with-resources with early return path");

    boolean earlyReturnTriggered = false;

    try (Engine engine = Engine.create()) {
      final Module module =
          engine.compileWat(
              """
          (module
            (func (export "check") (param i32) (result i32)
              local.get 0))
          """);

      try (Store store = engine.createStore()) {
        final Instance instance = module.instantiate(store);
        final WasmValue[] result = instance.callFunction("check", WasmValue.i32(1));

        if (result[0].asInt() == 1) {
          LOGGER.info("[" + runtime + "] Early return condition met, exiting inner scope");
          earlyReturnTriggered = true;
          instance.close();
          // Early exit from inner try-with-resources - store should still be cleaned up
        }
      }
      // Store is cleaned up even though we broke early
      module.close();
    }

    assertTrue(earlyReturnTriggered, "Early return path should have been triggered");
    LOGGER.info("[" + runtime + "] All resources cleaned up after early return");
  }
}
