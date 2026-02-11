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
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cross-runtime parity tests for Function operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * function calls with various parameter and return types.
 *
 * <p>Test coverage:
 *
 * <ul>
 *   <li>Function calls with no parameters/returns
 *   <li>Function calls with i32 parameters and returns
 *   <li>Function calls with i64 parameters and returns
 *   <li>Function calls with f32/f64 parameters and returns
 *   <li>Function calls with multiple parameters
 * </ul>
 */
@DisplayName("Function Parity Tests")
@Tag("integration")
class FunctionParityTest {

  private static final Logger LOGGER = Logger.getLogger(FunctionParityTest.class.getName());

  /** WASM module with various function signatures for testing. */
  private static final String FUNCTION_MODULE_WAT =
      "(module "
          + "  (func (export \"nop\"))"
          + "  (func (export \"const_i32\") (result i32)"
          + "    i32.const 42)"
          + "  (func (export \"const_i64\") (result i64)"
          + "    i64.const 9223372036854775807)"
          + "  (func (export \"add_i32\") (param i32 i32) (result i32)"
          + "    local.get 0"
          + "    local.get 1"
          + "    i32.add)"
          + "  (func (export \"add_i64\") (param i64 i64) (result i64)"
          + "    local.get 0"
          + "    local.get 1"
          + "    i64.add)"
          + "  (func (export \"mul_i32\") (param i32 i32) (result i32)"
          + "    local.get 0"
          + "    local.get 1"
          + "    i32.mul)"
          + "  (func (export \"factorial\") (param i32) (result i32)"
          + "    local.get 0"
          + "    i32.const 1"
          + "    i32.le_s"
          + "    if (result i32)"
          + "      i32.const 1"
          + "    else"
          + "      local.get 0"
          + "      local.get 0"
          + "      i32.const 1"
          + "      i32.sub"
          + "      call $factorial"
          + "      i32.mul"
          + "    end)"
          + "  (func $factorial (param i32) (result i32)"
          + "    local.get 0"
          + "    i32.const 1"
          + "    i32.le_s"
          + "    if (result i32)"
          + "      i32.const 1"
          + "    else"
          + "      local.get 0"
          + "      local.get 0"
          + "      i32.const 1"
          + "      i32.sub"
          + "      call $factorial"
          + "      i32.mul"
          + "    end)"
          + "  (func (export \"add_f32\") (param f32 f32) (result f32)"
          + "    local.get 0"
          + "    local.get 1"
          + "    f32.add)"
          + "  (func (export \"add_f64\") (param f64 f64) (result f64)"
          + "    local.get 0"
          + "    local.get 1"
          + "    f64.add)"
          + "  (func (export \"identity_i32\") (param i32) (result i32)"
          + "    local.get 0)"
          + "  (func (export \"identity_i64\") (param i64) (result i64)"
          + "    local.get 0)"
          + ")";

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private Store jniStore;
  private Store panamaStore;
  private Module jniModule;
  private Module panamaModule;
  private Instance jniInstance;
  private Instance panamaInstance;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for function parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up function parity test resources");

    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniStore = jniRuntime.createStore(jniEngine);
        jniModule = jniRuntime.compileModuleWat(jniEngine, FUNCTION_MODULE_WAT);
        jniInstance = jniStore.createInstance(jniModule);
        jniCreatedSuccessfully = true;
        LOGGER.info("JNI resources created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI resources: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaStore = panamaRuntime.createStore(panamaEngine);
        panamaModule = panamaRuntime.compileModuleWat(panamaEngine, FUNCTION_MODULE_WAT);
        panamaInstance = panamaStore.createInstance(panamaModule);
        panamaCreatedSuccessfully = true;
        LOGGER.info("Panama resources created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama resources: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up function parity test resources");

    closeQuietly(jniInstance, "JNI instance");
    closeQuietly(jniModule, "JNI module");
    closeQuietly(jniStore, "JNI store");
    closeQuietly(jniEngine, "JNI engine");
    closeQuietly(jniRuntime, "JNI runtime");

    closeQuietly(panamaInstance, "Panama instance");
    closeQuietly(panamaModule, "Panama module");
    closeQuietly(panamaStore, "Panama store");
    closeQuietly(panamaEngine, "Panama engine");
    closeQuietly(panamaRuntime, "Panama runtime");
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.info(name + " closed");
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
  @DisplayName("No Parameter Function Parity Tests")
  class NoParameterFunctionParityTests {

    @Test
    @DisplayName("should call void function with same behavior on both runtimes")
    void shouldCallVoidFunction() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniNop = jniInstance.getFunction("nop").orElseThrow();
      final WasmFunction panamaNop = panamaInstance.getFunction("nop").orElseThrow();

      LOGGER.info("Calling nop on both runtimes");

      // Both should succeed without exception
      jniNop.call();
      panamaNop.call();

      LOGGER.info("Both runtimes called nop successfully");
    }

    @Test
    @DisplayName("should return same i32 constant on both runtimes")
    void shouldReturnSameI32Constant() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniConst = jniInstance.getFunction("const_i32").orElseThrow();
      final WasmFunction panamaConst = panamaInstance.getFunction("const_i32").orElseThrow();

      final WasmValue[] jniResult = jniConst.call();
      final WasmValue[] panamaResult = panamaConst.call();

      LOGGER.info("JNI const_i32 result: " + jniResult[0].asInt());
      LOGGER.info("Panama const_i32 result: " + panamaResult[0].asInt());

      assertThat(jniResult).hasSize(1);
      assertThat(panamaResult).hasSize(1);
      assertThat(jniResult[0].asInt()).isEqualTo(42);
      assertThat(panamaResult[0].asInt()).isEqualTo(42);
    }

    @Test
    @DisplayName("should return same i64 constant on both runtimes")
    void shouldReturnSameI64Constant() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniConst = jniInstance.getFunction("const_i64").orElseThrow();
      final WasmFunction panamaConst = panamaInstance.getFunction("const_i64").orElseThrow();

      final WasmValue[] jniResult = jniConst.call();
      final WasmValue[] panamaResult = panamaConst.call();

      LOGGER.info("JNI const_i64 result: " + jniResult[0].asLong());
      LOGGER.info("Panama const_i64 result: " + panamaResult[0].asLong());

      assertThat(jniResult).hasSize(1);
      assertThat(panamaResult).hasSize(1);
      assertThat(jniResult[0].asLong()).isEqualTo(Long.MAX_VALUE);
      assertThat(panamaResult[0].asLong()).isEqualTo(Long.MAX_VALUE);
    }
  }

  @Nested
  @DisplayName("Integer Function Parity Tests")
  class IntegerFunctionParityTests {

    @Test
    @DisplayName("should add i32 with same results on both runtimes")
    void shouldAddI32WithSameResults() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniAdd = jniInstance.getFunction("add_i32").orElseThrow();
      final WasmFunction panamaAdd = panamaInstance.getFunction("add_i32").orElseThrow();

      final int a = 100;
      final int b = 200;

      final WasmValue[] jniResult = jniAdd.call(WasmValue.i32(a), WasmValue.i32(b));
      final WasmValue[] panamaResult = panamaAdd.call(WasmValue.i32(a), WasmValue.i32(b));

      LOGGER.info("JNI add_i32(" + a + ", " + b + ") = " + jniResult[0].asInt());
      LOGGER.info("Panama add_i32(" + a + ", " + b + ") = " + panamaResult[0].asInt());

      assertThat(jniResult[0].asInt()).isEqualTo(300);
      assertThat(panamaResult[0].asInt()).isEqualTo(300);
    }

    @Test
    @DisplayName("should add i64 with same results on both runtimes")
    void shouldAddI64WithSameResults() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniAdd = jniInstance.getFunction("add_i64").orElseThrow();
      final WasmFunction panamaAdd = panamaInstance.getFunction("add_i64").orElseThrow();

      final long a = 5000000000L;
      final long b = 3000000000L;

      final WasmValue[] jniResult = jniAdd.call(WasmValue.i64(a), WasmValue.i64(b));
      final WasmValue[] panamaResult = panamaAdd.call(WasmValue.i64(a), WasmValue.i64(b));

      LOGGER.info("JNI add_i64(" + a + ", " + b + ") = " + jniResult[0].asLong());
      LOGGER.info("Panama add_i64(" + a + ", " + b + ") = " + panamaResult[0].asLong());

      assertThat(jniResult[0].asLong()).isEqualTo(8000000000L);
      assertThat(panamaResult[0].asLong()).isEqualTo(8000000000L);
    }

    @Test
    @DisplayName("should multiply i32 with same results on both runtimes")
    void shouldMultiplyI32WithSameResults() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniMul = jniInstance.getFunction("mul_i32").orElseThrow();
      final WasmFunction panamaMul = panamaInstance.getFunction("mul_i32").orElseThrow();

      final int a = 7;
      final int b = 6;

      final WasmValue[] jniResult = jniMul.call(WasmValue.i32(a), WasmValue.i32(b));
      final WasmValue[] panamaResult = panamaMul.call(WasmValue.i32(a), WasmValue.i32(b));

      LOGGER.info("JNI mul_i32(" + a + ", " + b + ") = " + jniResult[0].asInt());
      LOGGER.info("Panama mul_i32(" + a + ", " + b + ") = " + panamaResult[0].asInt());

      assertThat(jniResult[0].asInt()).isEqualTo(42);
      assertThat(panamaResult[0].asInt()).isEqualTo(42);
    }

    @Test
    @DisplayName("should handle i32 overflow identically on both runtimes")
    void shouldHandleI32OverflowIdentically() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniAdd = jniInstance.getFunction("add_i32").orElseThrow();
      final WasmFunction panamaAdd = panamaInstance.getFunction("add_i32").orElseThrow();

      final int a = Integer.MAX_VALUE;
      final int b = 1;

      final WasmValue[] jniResult = jniAdd.call(WasmValue.i32(a), WasmValue.i32(b));
      final WasmValue[] panamaResult = panamaAdd.call(WasmValue.i32(a), WasmValue.i32(b));

      LOGGER.info("JNI overflow result: " + jniResult[0].asInt());
      LOGGER.info("Panama overflow result: " + panamaResult[0].asInt());

      // Both should overflow to MIN_VALUE
      assertThat(jniResult[0].asInt()).isEqualTo(Integer.MIN_VALUE);
      assertThat(panamaResult[0].asInt()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @DisplayName("should compute factorial with same results on both runtimes")
    void shouldComputeFactorialWithSameResults() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniFact = jniInstance.getFunction("factorial").orElseThrow();
      final WasmFunction panamaFact = panamaInstance.getFunction("factorial").orElseThrow();

      final int n = 10;

      final WasmValue[] jniResult = jniFact.call(WasmValue.i32(n));
      final WasmValue[] panamaResult = panamaFact.call(WasmValue.i32(n));

      LOGGER.info("JNI factorial(" + n + ") = " + jniResult[0].asInt());
      LOGGER.info("Panama factorial(" + n + ") = " + panamaResult[0].asInt());

      // 10! = 3628800
      assertThat(jniResult[0].asInt()).isEqualTo(3628800);
      assertThat(panamaResult[0].asInt()).isEqualTo(3628800);
    }
  }

  @Nested
  @DisplayName("Float Function Parity Tests")
  class FloatFunctionParityTests {

    @Test
    @DisplayName("should add f32 with same results on both runtimes")
    void shouldAddF32WithSameResults() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniAdd = jniInstance.getFunction("add_f32").orElseThrow();
      final WasmFunction panamaAdd = panamaInstance.getFunction("add_f32").orElseThrow();

      final float a = 1.5f;
      final float b = 2.5f;

      final WasmValue[] jniResult = jniAdd.call(WasmValue.f32(a), WasmValue.f32(b));
      final WasmValue[] panamaResult = panamaAdd.call(WasmValue.f32(a), WasmValue.f32(b));

      LOGGER.info("JNI add_f32(" + a + ", " + b + ") = " + jniResult[0].asFloat());
      LOGGER.info("Panama add_f32(" + a + ", " + b + ") = " + panamaResult[0].asFloat());

      assertThat(jniResult[0].asFloat()).isEqualTo(4.0f);
      assertThat(panamaResult[0].asFloat()).isEqualTo(4.0f);
    }

    @Test
    @DisplayName("should add f64 with same results on both runtimes")
    void shouldAddF64WithSameResults() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniAdd = jniInstance.getFunction("add_f64").orElseThrow();
      final WasmFunction panamaAdd = panamaInstance.getFunction("add_f64").orElseThrow();

      final double a = 3.14159265358979;
      final double b = 2.71828182845904;

      final WasmValue[] jniResult = jniAdd.call(WasmValue.f64(a), WasmValue.f64(b));
      final WasmValue[] panamaResult = panamaAdd.call(WasmValue.f64(a), WasmValue.f64(b));

      LOGGER.info("JNI add_f64(" + a + ", " + b + ") = " + jniResult[0].asDouble());
      LOGGER.info("Panama add_f64(" + a + ", " + b + ") = " + panamaResult[0].asDouble());

      final double expected = a + b;
      final Offset<Double> tolerance = Offset.offset(0.0000001);
      assertThat(jniResult[0].asDouble()).isCloseTo(expected, tolerance);
      assertThat(panamaResult[0].asDouble()).isCloseTo(expected, tolerance);
    }
  }

  @Nested
  @DisplayName("Identity Function Parity Tests")
  class IdentityFunctionParityTests {

    @Test
    @DisplayName("should pass through i32 boundary values identically on both runtimes")
    void shouldPassThroughI32BoundaryValues() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniId = jniInstance.getFunction("identity_i32").orElseThrow();
      final WasmFunction panamaId = panamaInstance.getFunction("identity_i32").orElseThrow();

      final int[] testValues = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 42, -42};

      for (final int value : testValues) {
        final WasmValue[] jniResult = jniId.call(WasmValue.i32(value));
        final WasmValue[] panamaResult = panamaId.call(WasmValue.i32(value));

        LOGGER.info(
            "Identity i32("
                + value
                + "): JNI="
                + jniResult[0].asInt()
                + ", Panama="
                + panamaResult[0].asInt());

        assertThat(jniResult[0].asInt()).isEqualTo(value);
        assertThat(panamaResult[0].asInt()).isEqualTo(value);
      }
    }

    @Test
    @DisplayName("should pass through i64 boundary values identically on both runtimes")
    void shouldPassThroughI64BoundaryValues() throws Exception {
      requireBothRuntimes();

      final WasmFunction jniId = jniInstance.getFunction("identity_i64").orElseThrow();
      final WasmFunction panamaId = panamaInstance.getFunction("identity_i64").orElseThrow();

      final long[] testValues = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 42L, -42L};

      for (final long value : testValues) {
        final WasmValue[] jniResult = jniId.call(WasmValue.i64(value));
        final WasmValue[] panamaResult = panamaId.call(WasmValue.i64(value));

        LOGGER.info(
            "Identity i64("
                + value
                + "): JNI="
                + jniResult[0].asLong()
                + ", Panama="
                + panamaResult[0].asLong());

        assertThat(jniResult[0].asLong()).isEqualTo(value);
        assertThat(panamaResult[0].asLong()).isEqualTo(value);
      }
    }
  }
}
