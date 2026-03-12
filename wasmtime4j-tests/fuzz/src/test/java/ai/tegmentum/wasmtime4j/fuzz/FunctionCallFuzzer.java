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
package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fuzz tests for WebAssembly function invocation.
 *
 * <p>This fuzzer tests the robustness of function calling by:
 *
 * <ul>
 *   <li>Calling functions with type mismatched arguments
 *   <li>Calling functions with wrong argument counts
 *   <li>Testing argument value edge cases
 *   <li>Testing trap handling
 * </ul>
 *
 * @since 1.0.0
 */
public class FunctionCallFuzzer {

  /** A test module with various function signatures. */
  private static final String TEST_MODULE_WAT =
      """
      (module
          (func $identity_i32 (export "identity_i32") (param i32) (result i32)
              local.get 0)
          (func $identity_i64 (export "identity_i64") (param i64) (result i64)
              local.get 0)
          (func $identity_f32 (export "identity_f32") (param f32) (result f32)
              local.get 0)
          (func $identity_f64 (export "identity_f64") (param f64) (result f64)
              local.get 0)
          (func $add_i32 (export "add_i32") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              i32.add)
          (func $add_i64 (export "add_i64") (param i64 i64) (result i64)
              local.get 0
              local.get 1
              i64.add)
          (func $no_args (export "no_args") (result i32)
              i32.const 42)
          (func $void_func (export "void_func")
              nop)
          (func $multi_param (export "multi_param") (param i32 i64 f32 f64) (result i32)
              local.get 0)
          (func $div_i32 (export "div_i32") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              i32.div_s)
      )
      """;

  /** List of function names in the test module. */
  private static final String[] FUNCTION_NAMES = {
    "identity_i32",
    "identity_i64",
    "identity_f32",
    "identity_f64",
    "add_i32",
    "add_i64",
    "no_args",
    "void_func",
    "multi_param",
    "div_i32"
  };

  /**
   * Fuzz test for calling functions with fuzzed arguments.
   *
   * <p>This test creates fuzzed WasmValue arguments and calls various exported functions. The
   * runtime should handle all inputs gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzFunctionCall(final FuzzedDataProvider data) {
    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(TEST_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      // Pick a random function to call
      final int funcIndex = data.consumeInt(0, FUNCTION_NAMES.length - 1);
      final String funcName = FUNCTION_NAMES[funcIndex];

      // Get the function
      final Optional<WasmFunction> funcOpt = instance.getFunction(funcName);
      if (funcOpt.isEmpty()) {
        return;
      }

      final WasmFunction func = funcOpt.get();

      // Build fuzzed arguments
      final List<WasmValue> args = buildFuzzedArgs(data);

      // Call the function with potentially mismatched types/counts
      // This should throw WasmException for mismatches, never crash
      try {
        func.call(args.toArray(new WasmValue[0]));
      } catch (WasmException e) {
        // Expected for type mismatches, traps, etc.
      }

    } catch (WasmException e) {
      // Expected for various error conditions
    } catch (Exception e) {
      // Unexpected exception
      throw e;
    }
  }

  /**
   * Fuzz test for calling functions by name with fuzzed function names.
   *
   * <p>This test uses fuzzed function names to test error handling for non-existent functions.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzFunctionByName(final FuzzedDataProvider data) {
    final String funcName = data.consumeString(100);
    final int argCount = data.consumeInt(0, 10);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(TEST_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      // Try to get a function with a fuzzed name
      final Optional<WasmFunction> funcOpt = instance.getFunction(funcName);

      if (funcOpt.isPresent()) {
        // Function exists, try to call it with fuzzed args
        final List<WasmValue> args = buildFuzzedArgs(data, argCount);
        try {
          funcOpt.get().call(args.toArray(new WasmValue[0]));
        } catch (WasmException e) {
          // Expected
        }
      }
      // If function doesn't exist, that's fine - empty Optional is expected

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for division operations (trap testing).
   *
   * <p>Division by zero and integer overflow are common trap sources. This test specifically
   * targets the div_i32 function.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzDivision(final FuzzedDataProvider data) {
    final int dividend = data.consumeInt();
    final int divisor = data.consumeInt();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(TEST_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmFunction> divFunc = instance.getFunction("div_i32");
      if (divFunc.isEmpty()) {
        return;
      }

      // Call division with fuzzed values
      // This may trap on division by zero or overflow (MIN_VALUE / -1)
      try {
        divFunc.get().call(WasmValue.i32(dividend), WasmValue.i32(divisor));
      } catch (WasmException e) {
        // Expected for traps
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Build a list of fuzzed WasmValue arguments.
   *
   * @param data fuzzed data provider
   * @return list of fuzzed arguments
   */
  private List<WasmValue> buildFuzzedArgs(final FuzzedDataProvider data) {
    final int argCount = data.consumeInt(0, 10);
    return buildFuzzedArgs(data, argCount);
  }

  /**
   * Build a list of fuzzed WasmValue arguments with specified count.
   *
   * @param data fuzzed data provider
   * @param argCount number of arguments to generate
   * @return list of fuzzed arguments
   */
  private List<WasmValue> buildFuzzedArgs(final FuzzedDataProvider data, final int argCount) {
    final List<WasmValue> args = new ArrayList<>();

    for (int i = 0; i < argCount; i++) {
      final int typeChoice = data.consumeInt(0, 3);
      switch (typeChoice) {
        case 0 -> args.add(WasmValue.i32(data.consumeInt()));
        case 1 -> args.add(WasmValue.i64(data.consumeLong()));
        case 2 -> args.add(WasmValue.f32(data.consumeFloat()));
        case 3 -> args.add(WasmValue.f64(data.consumeDouble()));
        default -> args.add(WasmValue.i32(0));
      }
    }

    return args;
  }
}
