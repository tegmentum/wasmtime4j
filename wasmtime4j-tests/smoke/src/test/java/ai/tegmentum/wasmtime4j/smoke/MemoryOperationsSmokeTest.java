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
package ai.tegmentum.wasmtime4j.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Smoke test for WebAssembly memory operations.
 *
 * <p>Validates that memory can be exported, written to via a WASM function, and read back.
 */
@DisplayName("Memory Operations Smoke Test")
public final class MemoryOperationsSmokeTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(MemoryOperationsSmokeTest.class.getName());

  private static final String MEMORY_WAT =
      """
      (module
        (memory (export "memory") 1)
        (func (export "store_value") (param i32 i32)
          local.get 0
          local.get 1
          i32.store
        )
        (func (export "load_value") (param i32) (result i32)
          local.get 0
          i32.load
        )
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest(name = "memory roundtrip for {0}")
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory store and load should roundtrip correctly")
  void memoryStoreAndLoadShouldRoundtrip(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Module module = engine.compileWat(MEMORY_WAT);
        final Instance instance = module.instantiate(store)) {

      final int testValue = 0xDEAD_BEEF;
      instance.callFunction("store_value", WasmValue.i32(0), WasmValue.i32(testValue));

      final WasmValue[] results = instance.callFunction("load_value", WasmValue.i32(0));
      assertEquals(testValue, results[0].asInt(), "Loaded value should match stored value");
      LOGGER.info("Memory roundtrip: 0x" + Integer.toHexString(results[0].asInt()));
    }
  }
}
