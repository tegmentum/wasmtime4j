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
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link WasmFunction#getNativeHandle()}.
 *
 * <p>The default implementation returns 0L. JNI implementations may return actual native handles.
 * Verifies non-negative values and consistency across calls.
 *
 * @since 1.0.0
 */
@DisplayName("Function Native Handle Tests")
public class FunctionNativeHandleTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(FunctionNativeHandleTest.class.getName());

  /** Module with two exported functions: add (with params) and nop (no params, no results). */
  private static final String TWO_FUNCS_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0 local.get 1 i32.add)
        (func (export "nop")))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getNativeHandle returns non-negative value")
  void getNativeHandleReturnsNonNegative(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getNativeHandle returns non-negative");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_FUNCS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmFunction addFunc =
          instance
              .getFunction("add")
              .orElseThrow(() -> new AssertionError("add function should be present"));

      final long handle = addFunc.getNativeHandle();

      LOGGER.info("[" + runtime + "] getNativeHandle for 'add': " + handle);
      assertTrue(handle >= 0, "Native handle should be non-negative, was: " + handle);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getNativeHandle is consistent for same function")
  void getNativeHandleConsistentForSameFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getNativeHandle consistency");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_FUNCS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmFunction addFunc =
          instance
              .getFunction("add")
              .orElseThrow(() -> new AssertionError("add function should be present"));

      final long firstCall = addFunc.getNativeHandle();
      final long secondCall = addFunc.getNativeHandle();

      LOGGER.info("[" + runtime + "] firstCall: " + firstCall);
      LOGGER.info("[" + runtime + "] secondCall: " + secondCall);

      assertEquals(firstCall, secondCall, "getNativeHandle should return same value across calls");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("getNativeHandle for different functions logged for diagnostics")
  void getNativeHandleDifferentFunctionsMayDiffer(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing getNativeHandle for different functions");

    try (Engine engine = Engine.create();
        Module module = engine.compileWat(TWO_FUNCS_WAT);
        Store store = engine.createStore();
        Instance instance = module.instantiate(store)) {

      final WasmFunction addFunc =
          instance
              .getFunction("add")
              .orElseThrow(() -> new AssertionError("add function should be present"));
      final WasmFunction nopFunc =
          instance
              .getFunction("nop")
              .orElseThrow(() -> new AssertionError("nop function should be present"));

      final long addHandle = addFunc.getNativeHandle();
      final long nopHandle = nopFunc.getNativeHandle();

      LOGGER.info("[" + runtime + "] 'add' handle: " + addHandle);
      LOGGER.info("[" + runtime + "] 'nop' handle: " + nopHandle);
      LOGGER.info("[" + runtime + "] handles equal: " + (addHandle == nopHandle));

      // Both should be non-negative
      assertTrue(addHandle >= 0, "add handle should be non-negative");
      assertTrue(nopHandle >= 0, "nop handle should be non-negative");

      // Handles may or may not differ depending on implementation
      // We just log for diagnostic purposes
    }
  }
}
