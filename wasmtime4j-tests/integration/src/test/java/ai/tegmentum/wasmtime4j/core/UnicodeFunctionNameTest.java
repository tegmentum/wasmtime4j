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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for Unicode function and import names. Verifies that exports and imports with Unicode names
 * (Japanese, Arabic, accented Latin) work correctly in both runtimes.
 *
 * @since 1.0.0
 */
@DisplayName("Unicode Function Name Tests")
public class UnicodeFunctionNameTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(UnicodeFunctionNameTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export with Japanese Unicode name")
  void exportWithJapaneseUnicodeName(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Japanese Unicode export name");

    final String functionName = "\u8A08\u7B97"; // 計算 (keisan = calculation)
    final String wat =
        "(module\n"
            + "  (func (export \""
            + functionName
            + "\") (param i32 i32) (result i32)\n"
            + "    local.get 0 local.get 1 i32.add))";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> func = instance.getFunction(functionName);
      assertTrue(func.isPresent(), "Function with Japanese name should be found");

      final WasmValue[] result = func.get().call(WasmValue.i32(10), WasmValue.i32(20));
      assertEquals(30, result[0].asInt(), "Function should compute correctly");
      LOGGER.info("[" + runtime + "] Japanese Unicode function '" + functionName + "' works");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export with Arabic Unicode name")
  void exportWithArabicUnicodeName(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Arabic Unicode export name");

    final String functionName = "\u062D\u0633\u0627\u0628"; // حساب (hisab = calculation)
    final String wat =
        "(module\n"
            + "  (func (export \""
            + functionName
            + "\") (result i32)\n"
            + "    i32.const 42))";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> func = instance.getFunction(functionName);
      assertTrue(func.isPresent(), "Function with Arabic name should be found");

      final WasmValue[] result = func.get().call();
      assertEquals(42, result[0].asInt(), "Function should return 42");
      LOGGER.info("[" + runtime + "] Arabic Unicode function '" + functionName + "' works");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Export with accented Latin Unicode name")
  void exportWithAccentedLatinName(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing accented Latin Unicode export name");

    final String functionName = "caf\u00e9_calcul\u00e9"; // café_calculé
    final String wat =
        "(module\n"
            + "  (func (export \""
            + functionName
            + "\") (result i32)\n"
            + "    i32.const 99))";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final Optional<WasmFunction> func = instance.getFunction(functionName);
      assertTrue(func.isPresent(), "Function with accented name should be found");

      final WasmValue[] result = func.get().call();
      assertEquals(99, result[0].asInt(), "Function should return 99");
      LOGGER.info("[" + runtime + "] Accented Latin function '" + functionName + "' works");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Call function by Unicode name via Instance.callFunction")
  void callFunctionByUnicodeName(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing callFunction with Unicode name");

    final String functionName = "\u00fcbung"; // übung (exercise)
    final String wat =
        "(module\n"
            + "  (func (export \""
            + functionName
            + "\") (param i32) (result i32)\n"
            + "    local.get 0\n"
            + "    i32.const 2\n"
            + "    i32.mul))";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] result = instance.callFunction(functionName, WasmValue.i32(21));
      assertEquals(42, result[0].asInt(), "Function should double the input");
      LOGGER.info("[" + runtime + "] callFunction with Unicode name '" + functionName + "' works");

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Get export by Unicode name via hasExport")
  void hasExportWithUnicodeName(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing hasExport with Unicode name");

    final String functionName = "\u03b1\u03c1\u03b9\u03b8\u03bc\u03cc\u03c2"; // αριθμός (number)
    final String wat =
        "(module\n"
            + "  (func (export \""
            + functionName
            + "\") (result i32)\n"
            + "    i32.const 7))";

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      assertTrue(instance.hasExport(functionName), "Should find Unicode export");
      LOGGER.info("[" + runtime + "] hasExport found Greek function name '" + functionName + "'");

      instance.close();
      module.close();
    }
  }
}
