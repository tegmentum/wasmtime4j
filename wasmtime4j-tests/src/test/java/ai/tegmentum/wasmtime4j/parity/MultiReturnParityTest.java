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

package ai.tegmentum.wasmtime4j.parity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cross-runtime parity tests for multi-value return functions. Verifies that JNI and Panama produce
 * identical results for functions returning 0, 1, 2, and 3+ values of mixed types.
 *
 * @since 1.0.0
 */
@DisplayName("Multi-Return Parity Tests")
@Tag("integration")
class MultiReturnParityTest {

  private static final Logger LOGGER = Logger.getLogger(MultiReturnParityTest.class.getName());

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private Store jniStore;
  private Store panamaStore;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
    LOGGER.info("JNI available: " + jniAvailable + ", Panama available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniStore = jniRuntime.createStore(jniEngine);
        jniCreatedSuccessfully = true;
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI resources: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaStore = panamaRuntime.createStore(panamaEngine);
        panamaCreatedSuccessfully = true;
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama resources: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    closeQuietly(jniStore, "JNI store");
    closeQuietly(jniEngine, "JNI engine");
    closeQuietly(jniRuntime, "JNI runtime");
    closeQuietly(panamaStore, "Panama store");
    closeQuietly(panamaEngine, "Panama engine");
    closeQuietly(panamaRuntime, "Panama runtime");
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + name + ": " + e.getMessage());
      }
    }
  }

  private void requireBothRuntimes() {
    assumeTrue(
        jniCreatedSuccessfully && panamaCreatedSuccessfully,
        "Both JNI and Panama runtimes required");
  }

  @Nested
  @DisplayName("Multi-Return Value Parity")
  class MultiReturnValueParityTests {

    @Test
    @DisplayName("Function returning 0 values (void)")
    void voidReturnParity() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (func (export "void_func")
              nop))
          """;

      final Module jniModule = jniRuntime.compileModuleWat(jniEngine, wat);
      final Module panamaModule = panamaRuntime.compileModuleWat(panamaEngine, wat);
      final Instance jniInstance = jniModule.instantiate(jniStore);
      final Instance panamaInstance = panamaModule.instantiate(panamaStore);

      final WasmValue[] jniResult = jniInstance.callFunction("void_func");
      final WasmValue[] panamaResult = panamaInstance.callFunction("void_func");

      assertThat(jniResult).hasSize(0).as("JNI void should return 0 values");
      assertThat(panamaResult).hasSize(0).as("Panama void should return 0 values");
      LOGGER.info("Void return parity verified");

      jniInstance.close();
      panamaInstance.close();
      jniModule.close();
      panamaModule.close();
    }

    @Test
    @DisplayName("Function returning 1 value")
    void singleReturnParity() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (func (export "single") (result i32)
              i32.const 42))
          """;

      final Module jniModule = jniRuntime.compileModuleWat(jniEngine, wat);
      final Module panamaModule = panamaRuntime.compileModuleWat(panamaEngine, wat);
      final Instance jniInstance = jniModule.instantiate(jniStore);
      final Instance panamaInstance = panamaModule.instantiate(panamaStore);

      final WasmValue[] jniResult = jniInstance.callFunction("single");
      final WasmValue[] panamaResult = panamaInstance.callFunction("single");

      assertThat(jniResult).hasSize(1);
      assertThat(panamaResult).hasSize(1);
      assertThat(jniResult[0].asInt()).isEqualTo(42);
      assertThat(panamaResult[0].asInt()).isEqualTo(42);
      LOGGER.info("Single return parity verified: " + jniResult[0].asInt());

      jniInstance.close();
      panamaInstance.close();
      jniModule.close();
      panamaModule.close();
    }

    @Test
    @DisplayName("Function returning 2 values (i32, i64)")
    void dualReturnParity() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (func (export "dual") (result i32 i64)
              i32.const 42
              i64.const 999999999999))
          """;

      final Module jniModule = jniRuntime.compileModuleWat(jniEngine, wat);
      final Module panamaModule = panamaRuntime.compileModuleWat(panamaEngine, wat);
      final Instance jniInstance = jniModule.instantiate(jniStore);
      final Instance panamaInstance = panamaModule.instantiate(panamaStore);

      final WasmValue[] jniResult = jniInstance.callFunction("dual");
      final WasmValue[] panamaResult = panamaInstance.callFunction("dual");

      assertThat(jniResult).hasSize(2);
      assertThat(panamaResult).hasSize(2);
      assertThat(jniResult[0].asInt()).isEqualTo(panamaResult[0].asInt());
      assertThat(jniResult[1].asLong()).isEqualTo(panamaResult[1].asLong());
      LOGGER.info(
          "Dual return parity verified: ("
              + jniResult[0].asInt()
              + ", "
              + jniResult[1].asLong()
              + ")");

      jniInstance.close();
      panamaInstance.close();
      jniModule.close();
      panamaModule.close();
    }

    @Test
    @DisplayName("Function returning 3+ values (mixed types)")
    void tripleReturnParity() throws Exception {
      requireBothRuntimes();

      final String wat =
          """
          (module
            (func (export "triple") (result i32 f64 i64)
              i32.const 7
              f64.const 3.14
              i64.const 100))
          """;

      final Module jniModule = jniRuntime.compileModuleWat(jniEngine, wat);
      final Module panamaModule = panamaRuntime.compileModuleWat(panamaEngine, wat);
      final Instance jniInstance = jniModule.instantiate(jniStore);
      final Instance panamaInstance = panamaModule.instantiate(panamaStore);

      final WasmValue[] jniResult = jniInstance.callFunction("triple");
      final WasmValue[] panamaResult = panamaInstance.callFunction("triple");

      assertThat(jniResult).hasSize(3);
      assertThat(panamaResult).hasSize(3);
      assertThat(jniResult[0].asInt()).isEqualTo(panamaResult[0].asInt());
      assertThat(jniResult[1].asDouble()).isEqualTo(panamaResult[1].asDouble());
      assertThat(jniResult[2].asLong()).isEqualTo(panamaResult[2].asLong());
      LOGGER.info(
          "Triple return parity verified: ("
              + jniResult[0].asInt()
              + ", "
              + jniResult[1].asDouble()
              + ", "
              + jniResult[2].asLong()
              + ")");

      jniInstance.close();
      panamaInstance.close();
      jniModule.close();
      panamaModule.close();
    }
  }
}
