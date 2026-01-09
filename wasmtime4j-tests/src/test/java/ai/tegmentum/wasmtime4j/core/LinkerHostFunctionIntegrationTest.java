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

import ai.tegmentum.wasmtime4j.CodeBuilder;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WebAssembly Linker host function operations.
 *
 * <p>Split from LinkerIntegrationTest to avoid native resource exhaustion issues when running many
 * nested test classes together.
 *
 * @since 1.0.0
 */
@DisplayName("Linker Host Function Integration Tests")
public final class LinkerHostFunctionIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerHostFunctionIntegrationTest.class.getName());

  /** Helper method to create a FuncType without a factory method. */
  private static FuncType funcType(
      final List<WasmValueType> params, final List<WasmValueType> results) {
    return new FuncType() {
      @Override
      public List<WasmValueType> getParams() {
        return Collections.unmodifiableList(params);
      }

      @Override
      public List<WasmValueType> getResults() {
        return Collections.unmodifiableList(results);
      }
    };
  }

  /**
   * Creates a module that imports "env" "add" function and exports "call_add".
   *
   * <pre>
   * (module
   *   (import "env" "add" (func $add (param i32 i32) (result i32)))
   *   (func (export "call_add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     call $add))
   * </pre>
   */
  private static byte[] createImportAddModule() throws WasmException {
    return new CodeBuilder()
        .addType(
            funcType(List.of(WasmValueType.I32, WasmValueType.I32), List.of(WasmValueType.I32)))
        .addFunctionImport("env", "add", 0)
        .addFunction(
            0, List.of(), new byte[] {0x20, 0x00, 0x20, 0x01, 0x10, 0x00}) // local.get 0, local.get
        // 1, call 0
        .addExport("call_add", CodeBuilder.ExportKind.FUNCTION, 1)
        .build();
  }

  @Test
  @DisplayName("should define host function and call from module")
  void shouldDefineHostFunctionAndCallFromModule() throws Exception {
    LOGGER.info("Testing host function definition");

    try (final Engine engine = Engine.create();
        final Linker<Void> linker = Linker.create(engine);
        final Store store = engine.createStore()) {

      // Create host function
      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction addImpl =
          (params) -> {
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.i32(a + b)};
          };

      linker.defineHostFunction("env", "add", addType, addImpl);
      LOGGER.info("Defined host function in linker");

      // Compile and instantiate
      final Module module = engine.compileModule(createImportAddModule());
      try (final Instance instance = linker.instantiate(store, module)) {
        // Call the exported function that uses our host function
        final Optional<WasmFunction> callAdd = instance.getFunction("call_add");
        assertTrue(callAdd.isPresent());

        final WasmValue[] result = callAdd.get().call(WasmValue.i32(15), WasmValue.i32(27));
        assertEquals(42, result[0].asInt(), "15 + 27 should equal 42");

        LOGGER.info("Host function definition verified");
      }
    }
  }
}
