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
package ai.tegmentum.wasmtime4j.tests.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Comprehensive tests for basic WASM types (i32, i64, f32, f64). */
public class BasicTypesTest extends DualRuntimeTest {

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("i32 parameter and return")
  public void testI32(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "add_i32") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              i32.add
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results =
          instance.callFunction("add_i32", WasmValue.i32(10), WasmValue.i32(32));

      assertEquals(1, results.length);
      assertEquals(42, results[0].asInt());

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("i64 parameter and return")
  public void testI64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "multiply_i64") (param i64 i64) (result i64)
              local.get 0
              local.get 1
              i64.mul
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results =
          instance.callFunction("multiply_i64", WasmValue.i64(7L), WasmValue.i64(6L));

      assertEquals(1, results.length);
      assertEquals(42L, results[0].asLong());

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("f32 parameter and return")
  public void testF32(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "divide_f32") (param f32 f32) (result f32)
              local.get 0
              local.get 1
              f32.div
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results =
          instance.callFunction("divide_f32", WasmValue.f32(84.0f), WasmValue.f32(2.0f));

      assertEquals(1, results.length);
      assertEquals(42.0f, results[0].asFloat(), 0.001f);

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("f64 parameter and return")
  public void testF64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "sqrt_f64") (param f64) (result f64)
              local.get 0
              f64.sqrt
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results = instance.callFunction("sqrt_f64", WasmValue.f64(1764.0));

      assertEquals(1, results.length);
      assertEquals(42.0, results[0].asDouble(), 0.001);

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple parameters of different types")
  public void testMixedParameters(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "mixed") (param i32 i64 f32 f64) (result i32)
              ;; Just return the i32 parameter for simplicity
              local.get 0
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results =
          instance.callFunction(
              "mixed",
              WasmValue.i32(42),
              WasmValue.i64(100L),
              WasmValue.f32(3.14f),
              WasmValue.f64(2.718));

      assertEquals(1, results.length);
      assertEquals(42, results[0].asInt());

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple return values")
  public void testMultipleReturns(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "multi_return") (param i32 i32) (result i32 i32 i32)
              local.get 0
              local.get 1
              local.get 0
              local.get 1
              i32.add
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results =
          instance.callFunction("multi_return", WasmValue.i32(10), WasmValue.i32(32));

      assertEquals(3, results.length);
      assertEquals(10, results[0].asInt());
      assertEquals(32, results[1].asInt());
      assertEquals(42, results[2].asInt());

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("No parameters, single return")
  public void testNoParams(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "get_constant") (result i32)
              i32.const 42
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results = instance.callFunction("get_constant");

      assertEquals(1, results.length);
      assertEquals(42, results[0].asInt());

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("No parameters, no return")
  public void testNoParamsNoReturn(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "noop"))
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results = instance.callFunction("noop");

      assertEquals(0, results.length);

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Maximum parameter count")
  public void testManyParameters(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      // Test with 10 parameters
      final String wat =
          """
          (module
            (func (export "sum_ten")
              (param i32 i32 i32 i32 i32 i32 i32 i32 i32 i32)
              (result i32)
              local.get 0
              local.get 1
              i32.add
              local.get 2
              i32.add
              local.get 3
              i32.add
              local.get 4
              i32.add
              local.get 5
              i32.add
              local.get 6
              i32.add
              local.get 7
              i32.add
              local.get 8
              i32.add
              local.get 9
              i32.add
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results =
          instance.callFunction(
              "sum_ten",
              WasmValue.i32(1),
              WasmValue.i32(2),
              WasmValue.i32(3),
              WasmValue.i32(4),
              WasmValue.i32(5),
              WasmValue.i32(6),
              WasmValue.i32(7),
              WasmValue.i32(8),
              WasmValue.i32(9),
              WasmValue.i32(3));

      assertEquals(1, results.length);
      assertEquals(48, results[0].asInt()); // 1+2+3+4+5+6+7+8+9+3=48

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Negative numbers")
  public void testNegativeNumbers(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    try {
      final String wat =
          """
          (module
            (func (export "negate") (param i32) (result i32)
              i32.const 0
              local.get 0
              i32.sub
            )
          )
          """;

      final Module module = engine.compileWat(wat);
      final Instance instance = module.instantiate(store);

      final WasmValue[] results = instance.callFunction("negate", WasmValue.i32(42));

      assertEquals(1, results.length);
      assertEquals(-42, results[0].asInt());

      instance.close();
    } finally {
      store.close();
      engine.close();
    }
  }
}
