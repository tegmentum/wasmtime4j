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
 * Fuzz tests for module operations via Linker and fuzzed bytecode compilation.
 *
 * <p>This fuzzer tests the robustness of:
 *
 * <ul>
 *   <li>Module compilation from arbitrary byte sequences
 *   <li>Linker host function definition with fuzzed names and signatures
 *   <li>Module instantiation via Linker with defined host functions
 *   <li>Export and import inspection of fuzzed modules
 * </ul>
 *
 * <p>Note: Full Component Model (WIT components) is not yet exposed via the Java API. These tests
 * exercise core module operations through the Linker, which is the primary module composition
 * mechanism available.
 *
 * @since 1.0.0
 */
public class ComponentModelFuzzer {

  /**
   * Fuzz test for module compilation from arbitrary byte sequences.
   *
   * <p>This test feeds arbitrary byte sequences to the module compiler. The compiler should handle
   * all inputs gracefully without crashing. Unlike ModuleLoadFuzzer, this also attempts
   * instantiation via a Linker when compilation succeeds.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzComponentInstantiation(final FuzzedDataProvider data) {
    final byte[] moduleBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Try to compile arbitrary bytes as a module
      try (Module module = engine.compileModule(moduleBytes)) {
        // If compilation succeeded, try instantiation via Linker
        // This may fail if the module has unsatisfied imports
        try (Instance instance = linker.instantiate(store, module)) {
          instance.getExportNames();
        }
      }
    } catch (WasmException e) {
      // Expected for invalid module bytes or unsatisfied imports
    } catch (IllegalArgumentException e) {
      // Expected for null/empty input
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for Linker host function definition with fuzzed names and signatures.
   *
   * <p>This test defines host functions in a Linker using fuzzed module names, function names, and
   * type signatures, then attempts to instantiate a module that imports a function. The Linker
   * should handle all valid and invalid inputs gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzComponentLinking(final FuzzedDataProvider data) {
    final String moduleName = data.consumeString(100);
    final String funcName = data.consumeString(100);
    final int paramCount = data.consumeInt(0, 7);
    final int resultCount = data.consumeInt(0, 4);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Build fuzzed parameter types
      final WasmValueType[] params = new WasmValueType[paramCount];
      for (int i = 0; i < paramCount; i++) {
        params[i] = pickRandomType(data);
      }

      // Build fuzzed result types
      final WasmValueType[] results = new WasmValueType[resultCount];
      for (int i = 0; i < resultCount; i++) {
        results[i] = pickRandomType(data);
      }

      // Define a host function with fuzzed signature
      final FunctionType funcType = FunctionType.of(params, results);
      linker.defineHostFunction(
          moduleName,
          funcName,
          funcType,
          (args) -> {
            final WasmValue[] returnValues = new WasmValue[results.length];
            for (int i = 0; i < results.length; i++) {
              returnValues[i] = defaultValueForType(results[i]);
            }
            return returnValues;
          });

      // Also define a known "env"/"host_func" so we can test instantiation
      final FunctionType envFuncType =
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      linker.defineHostFunction(
          "env",
          "host_func",
          envFuncType,
          (args) -> new WasmValue[] {WasmValue.i32(args[0].asInt())});

      // Attempt instantiation with a module that imports "env"/"host_func"
      final String importModuleWat =
          """
          (module
              (import "env" "host_func" (func $hf (param i32) (result i32)))
              (func (export "call_host") (param i32) (result i32)
                  local.get 0
                  call $hf)
          )
          """;

      try (Module module = engine.compileWat(importModuleWat);
          Instance instance = linker.instantiate(store, module)) {
        final WasmValue[] callResults = instance.callFunction("call_host", WasmValue.i32(42));
        if (callResults != null && callResults.length > 0) {
          final int result = callResults[0].asInt();
          if (result != 42) {
            throw new AssertionError("Host function identity expected 42 but got " + result);
          }
        }
      }

    } catch (WasmException e) {
      // Expected for invalid module/function names or linking errors
    } catch (IllegalArgumentException e) {
      // Expected for null/empty inputs
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for compiling fuzzed byte sequences and inspecting exports/imports.
   *
   * <p>This test attempts to compile fuzzed byte sequences as WASM modules and inspect their
   * exports and imports. This exercises the module metadata extraction path with arbitrary input.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzComponentExports(final FuzzedDataProvider data) {
    final byte[] moduleBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create()) {
      // Attempt to compile fuzzed bytes
      try (Module module = engine.compileModule(moduleBytes)) {
        // Inspect exports
        final var exports = module.getExports();
        if (exports != null) {
          for (var export : exports) {
            // Access all export properties to exercise the metadata path
            export.getName();
            export.getType();
          }
        }

        // Inspect imports
        final var imports = module.getImports();
        if (imports != null) {
          for (var imp : imports) {
            // Access all import properties
            imp.getModuleName();
            imp.getName();
            imp.getType();
          }
        }
      }
    } catch (WasmException e) {
      // Expected for invalid module bytes
    } catch (IllegalArgumentException e) {
      // Expected for null/empty input
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
