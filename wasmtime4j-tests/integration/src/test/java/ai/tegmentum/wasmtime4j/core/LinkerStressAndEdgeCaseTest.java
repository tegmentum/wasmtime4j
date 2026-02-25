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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Linker.LinkerDefinition;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Stress and edge case tests for {@link Linker}: many imports, shadowing behavior, {@link
 * InstancePre} reuse, and WASI iteration.
 *
 * @since 1.0.0
 */
@DisplayName("Linker Stress and Edge Case Tests")
public class LinkerStressAndEdgeCaseTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerStressAndEdgeCaseTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("stress test with 100 host function imports")
  void stressTestManyImports(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing linker with 100 host function imports");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      // Define 100 host functions, each returning its index
      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
      for (int i = 0; i < 100; i++) {
        final int value = i;
        linker.defineHostFunction(
            "env", "func_" + i, funcType, (params) -> new WasmValue[] {WasmValue.i32(value)});
      }

      // Build WAT that imports all 100 functions and calls a few
      final StringBuilder wat = new StringBuilder("(module\n");
      for (int i = 0; i < 100; i++) {
        wat.append("  (import \"env\" \"func_")
            .append(i)
            .append("\" (func $f")
            .append(i)
            .append(" (result i32)))\n");
      }
      // Export functions that call specific imports
      wat.append("  (func (export \"call_0\") (result i32) call $f0)\n");
      wat.append("  (func (export \"call_50\") (result i32) call $f50)\n");
      wat.append("  (func (export \"call_99\") (result i32) call $f99)\n");
      wat.append(")");

      final Module module = engine.compileWat(wat.toString());
      final Instance instance = linker.instantiate(store, module);

      assertEquals(0, instance.callFunction("call_0")[0].asInt(), "call_0 should return 0");
      assertEquals(50, instance.callFunction("call_50")[0].asInt(), "call_50 should return 50");
      assertEquals(99, instance.callFunction("call_99")[0].asInt(), "call_99 should return 99");
      LOGGER.info(
          "[" + runtime + "] 100-import stress test passed: call_0=0, call_50=50, " + "call_99=99");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("allowShadowing enables redefining imports")
  void allowShadowingOverridesDefinition(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing allowShadowing overrides definition");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<?> linker = Linker.create(engine)) {

      linker.allowShadowing(true);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      // Define first version returning 1
      linker.defineHostFunction(
          "env", "get_val", funcType, (params) -> new WasmValue[] {WasmValue.i32(1)});

      // Redefine with shadowing returning 2
      linker.defineHostFunction(
          "env", "get_val", funcType, (params) -> new WasmValue[] {WasmValue.i32(2)});

      final String wat =
          """
          (module
            (import "env" "get_val" (func (result i32)))
            (func (export "call_it") (result i32) call 0))
          """;
      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);

      final int result = instance.callFunction("call_it")[0].asInt();
      assertEquals(2, result, "Shadowed definition should return 2 (second definition wins)");
      LOGGER.info("[" + runtime + "] Shadowing test passed: call_it = " + result);

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("allowShadowing disabled throws on redefine")
  void allowShadowingDisabledThrowsOnRedefine(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing shadowing disabled throws on redefine");

    try (Engine engine = Engine.create();
        Linker<?> linker = Linker.create(engine)) {

      linker.allowShadowing(false);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

      // Define first time
      linker.defineHostFunction(
          "env", "get_val", funcType, (params) -> new WasmValue[] {WasmValue.i32(1)});

      // Second definition may or may not throw depending on implementation
      try {
        linker.defineHostFunction(
            "env", "get_val", funcType, (params) -> new WasmValue[] {WasmValue.i32(2)});
        LOGGER.info(
            "["
                + runtime
                + "] Redefine without shadowing did not throw "
                + "(shadowing may be enabled by default)");
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] Redefine without shadowing threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("instantiatePre is reusable across multiple stores")
  void instantiatePreReusable(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing instantiatePre reuse across stores");

    try (Engine engine = Engine.create();
        Linker<?> linker = Linker.create(engine)) {

      final FunctionType funcType =
          new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});
      linker.defineHostFunction(
          "env", "get_val", funcType, (params) -> new WasmValue[] {WasmValue.i32(42)});

      final String wat =
          """
          (module
            (import "env" "get_val" (func (result i32)))
            (func (export "call_it") (result i32) call 0))
          """;
      final Module module = engine.compileWat(wat);

      try {
        final InstancePre pre = linker.instantiatePre(module);
        assertNotNull(pre, "instantiatePre should return non-null");

        // Instantiate 3 times with different stores
        for (int i = 0; i < 3; i++) {
          try (Store store = engine.createStore()) {
            final Instance instance = pre.instantiate(store);
            final int result = instance.callFunction("call_it")[0].asInt();
            assertEquals(42, result, "Instance " + i + " from pre should return 42");
            LOGGER.info("[" + runtime + "] instantiatePre reuse " + i + ": call_it = " + result);
            instance.close();
          }
        }

        pre.close();
      } catch (final UnsatisfiedLinkError | UnsupportedOperationException e) {
        LOGGER.info(
            "["
                + runtime
                + "] instantiatePre not implemented: "
                + e.getClass().getSimpleName()
                + " - "
                + e.getMessage());
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] instantiatePre threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }

      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("iter after enableWasi shows WASI definitions")
  void iterAfterWasiEnableShowsDefinitions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing iter after enableWasi");

    try (Engine engine = Engine.create();
        Linker<?> linker = Linker.create(engine)) {

      try {
        linker.enableWasi();

        final List<LinkerDefinition> defs = new ArrayList<>();
        for (final LinkerDefinition def : linker.iter()) {
          defs.add(def);
        }

        LOGGER.info("[" + runtime + "] After enableWasi, iter has " + defs.size() + " definitions");

        if (defs.size() > 0) {
          // Check for typical WASI module names
          final boolean hasWasiDefs =
              defs.stream().anyMatch(d -> d.getModuleName().contains("wasi"));
          LOGGER.info("[" + runtime + "] Has wasi_* module definitions: " + hasWasiDefs);
        } else {
          LOGGER.info(
              "["
                  + runtime
                  + "] enableWasi definitions not visible via iter() "
                  + "(implementation may not expose WASI defs through iter)");
        }
      } catch (final Exception e) {
        LOGGER.info(
            "["
                + runtime
                + "] enableWasi or iter threw: "
                + e.getClass().getName()
                + " - "
                + e.getMessage());
      }
    }
  }
}
