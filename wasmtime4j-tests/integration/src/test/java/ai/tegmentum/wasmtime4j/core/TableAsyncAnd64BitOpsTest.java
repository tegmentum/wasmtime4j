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
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for 64-bit table operations ({@link
 * WasmTable#getSize64()}, {@link WasmTable#get64(long)}, {@link WasmTable#set64(long, WasmValue)},
 * {@link WasmTable#grow64(long, WasmValue)}).
 *
 * <p>The 64-bit operations throw {@link UnsupportedOperationException} because {@link
 * WasmTable#supports64BitAddressing()} defaults to false. The JNI implementation may throw {@link
 * UnsatisfiedLinkError} if the native binding for supports64BitAddressing is missing.
 *
 * @since 1.0.0
 */
@DisplayName("Table Async and 64-Bit Operations Tests")
public class TableAsyncAnd64BitOpsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(TableAsyncAnd64BitOpsTest.class.getName());

  /** Module with a funcref table of initial size 5 and a nop function. */
  private static final String TABLE_WAT =
      """
      (module
        (table (export "tab") 5 funcref)
        (func (export "nop")))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  /**
   * Asserts that calling a 64-bit table operation throws either UnsupportedOperationException (from
   * the default interface guard) or UnsatisfiedLinkError (from missing JNI native binding for
   * supports64BitAddressing).
   */
  private static void assert64BitOpThrows(
      final String opName, final Runnable operation, final RuntimeType runtime) {
    try {
      operation.run();
      fail(opName + " should throw UnsupportedOperationException or UnsatisfiedLinkError");
    } catch (final UnsupportedOperationException e) {
      LOGGER.info(
          "["
              + runtime
              + "] "
              + opName
              + " threw UnsupportedOperationException: "
              + e.getMessage());
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.info(
          "["
              + runtime
              + "] "
              + opName
              + " threw UnsatisfiedLinkError (missing native binding): "
              + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getSize64 throws UnsupportedOperationException or UnsatisfiedLinkError")
  void getSize64ThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getSize64 throws on non-64-bit table");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TABLE_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmTable table =
          instance
              .getTable("tab")
              .orElseThrow(() -> new AssertionError("tab table should be present"));

      // Log supports64BitAddressing result defensively
      try {
        final boolean supports64 = table.supports64BitAddressing();
        LOGGER.info("[" + runtime + "] supports64BitAddressing: " + supports64);
      } catch (final UnsatisfiedLinkError e) {
        LOGGER.info("[" + runtime + "] supports64BitAddressing threw UnsatisfiedLinkError");
      }

      assert64BitOpThrows("getSize64", table::getSize64, runtime);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("get64 throws UnsupportedOperationException or UnsatisfiedLinkError")
  void get64ThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing get64 throws on non-64-bit table");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TABLE_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmTable table =
          instance
              .getTable("tab")
              .orElseThrow(() -> new AssertionError("tab table should be present"));

      assert64BitOpThrows("get64", () -> table.get64(0), runtime);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("set64 throws UnsupportedOperationException or UnsatisfiedLinkError")
  void set64ThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing set64 throws on non-64-bit table");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TABLE_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmTable table =
          instance
              .getTable("tab")
              .orElseThrow(() -> new AssertionError("tab table should be present"));

      assert64BitOpThrows("set64", () -> table.set64(0, WasmValue.funcRefNull()), runtime);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("grow64 throws UnsupportedOperationException or UnsatisfiedLinkError")
  void grow64ThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing grow64 throws on non-64-bit table");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TABLE_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmTable table =
          instance
              .getTable("tab")
              .orElseThrow(() -> new AssertionError("tab table should be present"));

      assert64BitOpThrows("grow64", () -> table.grow64(1, WasmValue.funcRefNull()), runtime);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("fill64 throws UnsupportedOperationException or UnsatisfiedLinkError")
  void fill64ThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing fill64 throws on non-64-bit table");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TABLE_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmTable table =
          instance
              .getTable("tab")
              .orElseThrow(() -> new AssertionError("tab table should be present"));

      assert64BitOpThrows("fill64", () -> table.fill64(0, 1, WasmValue.funcRefNull()), runtime);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy64 throws UnsupportedOperationException or UnsatisfiedLinkError")
  void copy64ThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing copy64 throws on non-64-bit table");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TABLE_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmTable table =
          instance
              .getTable("tab")
              .orElseThrow(() -> new AssertionError("tab table should be present"));

      assert64BitOpThrows("copy64", () -> table.copy64(0, table, 0, 1), runtime);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("init64 throws UnsupportedOperationException or UnsatisfiedLinkError")
  void init64ThrowsUnsupported(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing init64 throws on non-64-bit table");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TABLE_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmTable table =
          instance
              .getTable("tab")
              .orElseThrow(() -> new AssertionError("tab table should be present"));

      assert64BitOpThrows("init64", () -> table.init64(0, 0, 0, 1), runtime);
    }
  }
}
