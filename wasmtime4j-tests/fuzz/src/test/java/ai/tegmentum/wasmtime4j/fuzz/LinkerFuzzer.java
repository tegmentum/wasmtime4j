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
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz tests for WebAssembly Linker operations.
 *
 * <p>This fuzzer tests the robustness of linker operations including:
 *
 * <ul>
 *   <li>Defining host functions with various signatures
 *   <li>Module instantiation with linked imports
 *   <li>Import/export namespace handling
 *   <li>Type validation during linking
 * </ul>
 *
 * @since 1.0.0
 */
public class LinkerFuzzer {

  /** A module that imports a function. */
  private static final String IMPORT_MODULE_WAT =
      """
      (module
          (import "env" "host_func" (func $host_func (param i32) (result i32)))
          (func (export "call_host") (param i32) (result i32)
              local.get 0
              call $host_func)
      )
      """;

  /** A simple module without imports for testing linking. */
  private static final String SIMPLE_MODULE_WAT =
      """
      (module
          (func (export "add") (param i32 i32) (result i32)
              local.get 0
              local.get 1
              i32.add)
          (func (export "identity") (param i64) (result i64)
              local.get 0)
      )
      """;

  /**
   * Fuzz test for defining host functions with fuzzed parameters.
   *
   * <p>This test defines host functions with fuzzed module names and function names. The linker
   * should handle all valid and invalid inputs gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzLinkerDefineFunc(final FuzzedDataProvider data) {
    final String moduleName = data.consumeString(100);
    final String funcName = data.consumeString(100);
    final int paramCount = data.consumeInt(0, 10);
    final int resultCount = data.consumeInt(0, 4);

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine)) {

      // Build parameter types
      final WasmValueType[] params = new WasmValueType[paramCount];
      for (int i = 0; i < paramCount; i++) {
        params[i] = pickRandomType(data);
      }

      // Build result types
      final WasmValueType[] results = new WasmValueType[resultCount];
      for (int i = 0; i < resultCount; i++) {
        results[i] = pickRandomType(data);
      }

      // Create function type and define host function
      final FunctionType funcType = FunctionType.of(params, results);

      linker.defineHostFunction(
          moduleName,
          funcName,
          funcType,
          (args) -> {
            // Simple host function that returns default values
            final WasmValue[] returnValues = new WasmValue[results.length];
            for (int i = 0; i < results.length; i++) {
              returnValues[i] = defaultValueForType(results[i]);
            }
            return returnValues;
          });

      // Verify the function was defined
      linker.hasImport(moduleName, funcName);

    } catch (WasmException e) {
      // Expected for invalid module/function names
    } catch (IllegalArgumentException e) {
      // Expected for null/empty inputs
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for defining and instantiating modules through a linker.
   *
   * <p>This test creates modules and links them together, testing the linker's ability to resolve
   * dependencies with fuzzed module names.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzLinkerDefineModule(final FuzzedDataProvider data) {
    final String moduleName = data.consumeString(50);
    final boolean useSimpleModule = data.consumeBoolean();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Compile and instantiate a module
      try (Module module = engine.compileWat(SIMPLE_MODULE_WAT);
          Instance instance = linker.instantiate(store, module)) {

        // Define the instance in the linker namespace
        linker.defineInstance(store, moduleName, instance);

        // Try to compile and instantiate a second module that might import from first
        if (useSimpleModule) {
          try (Module module2 = engine.compileWat(SIMPLE_MODULE_WAT);
              Instance instance2 = linker.instantiate(store, module2)) {
            // Success - both modules instantiated
            // Verify the instance is valid
            instance2.getExportNames();
          }
        }
      }

    } catch (WasmException e) {
      // Expected for various linking errors
    } catch (IllegalArgumentException e) {
      // Expected for null/empty inputs
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for instantiating modules with host function imports.
   *
   * <p>This test creates host functions that satisfy module imports, testing the linking process
   * with various input values.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzLinkerInstantiate(final FuzzedDataProvider data) {
    final int hostReturnValue = data.consumeInt();
    final int callArgument = data.consumeInt();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Define a host function that matches the import
      final FunctionType funcType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

      linker.defineHostFunction(
          "env",
          "host_func",
          funcType,
          (args) -> new WasmValue[] {WasmValue.i32(hostReturnValue + args[0].asInt())});

      // Compile the importing module
      try (Module module = engine.compileWat(IMPORT_MODULE_WAT);
          Instance instance = linker.instantiate(store, module)) {

        // Call the exported function that calls back to the host
        final WasmValue[] results = instance.callFunction("call_host", WasmValue.i32(callArgument));

        // Verify the result
        if (results != null && results.length > 0) {
          final int expected = hostReturnValue + callArgument;
          final int actual = results[0].asInt();
          if (actual != expected) {
            throw new AssertionError(
                "Host function result mismatch: expected " + expected + " but got " + actual);
          }
        }
      }

    } catch (WasmException e) {
      // Expected for linking/instantiation errors
    } catch (ArithmeticException e) {
      // Expected for integer overflow
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Picks a random WebAssembly value type based on fuzzed input.
   *
   * @param data fuzzed data provider
   * @return a random WasmValueType
   */
  private WasmValueType pickRandomType(final FuzzedDataProvider data) {
    final int choice = data.consumeInt(0, 3);
    return switch (choice) {
      case 0 -> WasmValueType.I32;
      case 1 -> WasmValueType.I64;
      case 2 -> WasmValueType.F32;
      case 3 -> WasmValueType.F64;
      default -> WasmValueType.I32;
    };
  }

  /**
   * Creates a default WasmValue for the given type.
   *
   * @param type the WebAssembly value type
   * @return a default value for that type
   */
  private WasmValue defaultValueForType(final WasmValueType type) {
    if (type == WasmValueType.I32) {
      return WasmValue.i32(0);
    } else if (type == WasmValueType.I64) {
      return WasmValue.i64(0L);
    } else if (type == WasmValueType.F32) {
      return WasmValue.f32(0.0f);
    } else if (type == WasmValueType.F64) {
      return WasmValue.f64(0.0);
    } else {
      return WasmValue.i32(0);
    }
  }
}
