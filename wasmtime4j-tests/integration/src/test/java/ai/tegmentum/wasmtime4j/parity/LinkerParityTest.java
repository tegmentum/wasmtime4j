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
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
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
 * Cross-runtime parity tests for Linker operations.
 *
 * <p>These tests verify that JNI and Panama runtime implementations produce identical results for
 * linker creation, host function definition, module instantiation with imports, shadowing behavior,
 * and hasImport checks.
 */
@DisplayName("Linker Parity Tests")
@Tag("integration")
class LinkerParityTest {

  private static final Logger LOGGER = Logger.getLogger(LinkerParityTest.class.getName());

  /** WASM module that imports a function from the host environment. */
  private static final byte[] IMPORT_MODULE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // magic + version
        0x01,
        0x07,
        0x01,
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type section
        0x02,
        0x0b,
        0x01,
        0x03,
        0x65,
        0x6e,
        0x76,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // import
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x0c,
        0x01,
        0x08,
        0x63,
        0x61,
        0x6c,
        0x6c,
        0x5f,
        0x61,
        0x64,
        0x64,
        0x00,
        0x01, // export
        0x0a,
        0x0a,
        0x01,
        0x08,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x10,
        0x00,
        0x0b // code section
      };

  /** Simple WASM module with no imports for basic tests. */
  private static final byte[] SIMPLE_MODULE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d,
        0x01,
        0x00,
        0x00,
        0x00, // magic + version
        0x01,
        0x05,
        0x01,
        0x60,
        0x00,
        0x01,
        0x7f, // type section: () -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x0a,
        0x01,
        0x06,
        0x67,
        0x65,
        0x74,
        0x5f,
        0x34,
        0x32,
        0x00,
        0x00, // export "get_42"
        0x0a,
        0x06,
        0x01,
        0x04,
        0x00,
        0x41,
        0x2a,
        0x0b // code section: i32.const 42
      };

  private static boolean jniAvailable;
  private static boolean panamaAvailable;

  private WasmRuntime jniRuntime;
  private WasmRuntime panamaRuntime;
  private Engine jniEngine;
  private Engine panamaEngine;
  private boolean jniCreatedSuccessfully;
  private boolean panamaCreatedSuccessfully;

  @BeforeAll
  static void checkRuntimeAvailability() {
    LOGGER.info("Checking runtime availability for linker parity tests");

    jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
    panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

    LOGGER.info("JNI runtime available: " + jniAvailable);
    LOGGER.info("Panama runtime available: " + panamaAvailable);
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up linker parity test resources");

    jniCreatedSuccessfully = false;
    panamaCreatedSuccessfully = false;

    if (jniAvailable) {
      try {
        jniRuntime = WasmRuntimeFactory.create(RuntimeType.JNI);
        jniEngine = jniRuntime.createEngine();
        jniCreatedSuccessfully = true;
        LOGGER.info("JNI runtime and engine created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create JNI resources: " + e.getMessage());
      }
    }

    if (panamaAvailable) {
      try {
        panamaRuntime = WasmRuntimeFactory.create(RuntimeType.PANAMA);
        panamaEngine = panamaRuntime.createEngine();
        panamaCreatedSuccessfully = true;
        LOGGER.info("Panama runtime and engine created successfully");
      } catch (final Exception e) {
        LOGGER.warning("Failed to create Panama resources: " + e.getMessage());
      }
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up linker parity test resources");

    closeQuietly(jniEngine, "JNI engine");
    closeQuietly(jniRuntime, "JNI runtime");
    closeQuietly(panamaEngine, "Panama engine");
    closeQuietly(panamaRuntime, "Panama runtime");
  }

  private void closeQuietly(final AutoCloseable resource, final String name) {
    if (resource != null) {
      try {
        resource.close();
        LOGGER.fine(name + " closed");
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
  @DisplayName("Linker Creation Parity Tests")
  class LinkerCreationParityTests {

    @Test
    @DisplayName("should create linkers with default config on both runtimes")
    void shouldCreateDefaultLinkers() throws Exception {
      requireBothRuntimes();

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        LOGGER.info("Created JNI linker: " + jniLinker);
        LOGGER.info("Created Panama linker: " + panamaLinker);

        assertThat(jniLinker).isNotNull();
        assertThat(panamaLinker).isNotNull();
        assertThat(jniLinker.isValid()).isTrue();
        assertThat(panamaLinker.isValid()).isTrue();
      }
    }

    @Test
    @DisplayName("should get engine from linker on both runtimes")
    void shouldGetEngineFromLinker() throws Exception {
      requireBothRuntimes();

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        final Engine jniLinkerEngine = jniLinker.getEngine();
        final Engine panamaLinkerEngine = panamaLinker.getEngine();

        LOGGER.info("JNI linker engine: " + jniLinkerEngine);
        LOGGER.info("Panama linker engine: " + panamaLinkerEngine);

        assertThat(jniLinkerEngine).isNotNull();
        assertThat(panamaLinkerEngine).isNotNull();
      }
    }
  }

  @Nested
  @DisplayName("Host Function Definition Parity Tests")
  class HostFunctionDefinitionParityTests {

    @Test
    @DisplayName("should define host function on both runtimes")
    void shouldDefineHostFunction() throws Exception {
      requireBothRuntimes();

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction addImpl =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        jniLinker.defineHostFunction("env", "add", addType, addImpl);
        panamaLinker.defineHostFunction("env", "add", addType, addImpl);

        LOGGER.info("Defined 'env::add' on JNI linker");
        LOGGER.info("Defined 'env::add' on Panama linker");

        assertThat(jniLinker.hasImport("env", "add")).isTrue();
        assertThat(panamaLinker.hasImport("env", "add")).isTrue();
      }
    }

    @Test
    @DisplayName("should define void host function on both runtimes")
    void shouldDefineVoidHostFunction() throws Exception {
      requireBothRuntimes();

      final FunctionType logType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

      final int[] capturedValue = {0};
      final HostFunction logImpl =
          HostFunction.voidFunction(
              (params) -> {
                capturedValue[0] = params[0].asInt();
                LOGGER.info("Logged value: " + capturedValue[0]);
              });

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        jniLinker.defineHostFunction("env", "log", logType, logImpl);
        panamaLinker.defineHostFunction("env", "log", logType, logImpl);

        LOGGER.info("Defined void 'env::log' on both linkers");

        assertThat(jniLinker.hasImport("env", "log")).isTrue();
        assertThat(panamaLinker.hasImport("env", "log")).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Module Instantiation with Imports Parity Tests")
  class ModuleInstantiationParityTests {

    @Test
    @DisplayName("should instantiate module with import on both runtimes")
    void shouldInstantiateModuleWithImport() throws Exception {
      requireBothRuntimes();

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction addImpl =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine);
          Store jniStore = jniEngine.createStore();
          Store panamaStore = panamaEngine.createStore();
          Module jniModule = jniEngine.compileModule(IMPORT_MODULE_WASM);
          Module panamaModule = panamaEngine.compileModule(IMPORT_MODULE_WASM)) {

        jniLinker.defineHostFunction("env", "add", addType, addImpl);
        panamaLinker.defineHostFunction("env", "add", addType, addImpl);

        try (Instance jniInstance = jniLinker.instantiate(jniStore, jniModule);
            Instance panamaInstance = panamaLinker.instantiate(panamaStore, panamaModule)) {

          LOGGER.info("Instantiated module with import on JNI: " + jniInstance);
          LOGGER.info("Instantiated module with import on Panama: " + panamaInstance);

          assertThat(jniInstance).isNotNull();
          assertThat(panamaInstance).isNotNull();

          final WasmFunction jniCallAdd = jniInstance.getFunction("call_add").orElseThrow();
          final WasmFunction panamaCallAdd = panamaInstance.getFunction("call_add").orElseThrow();

          final WasmValue[] jniResult = jniCallAdd.call(WasmValue.i32(10), WasmValue.i32(20));
          final WasmValue[] panamaResult = panamaCallAdd.call(WasmValue.i32(10), WasmValue.i32(20));

          LOGGER.info("JNI call_add(10, 20) = " + jniResult[0].asInt());
          LOGGER.info("Panama call_add(10, 20) = " + panamaResult[0].asInt());

          assertThat(jniResult[0].asInt()).isEqualTo(30);
          assertThat(panamaResult[0].asInt()).isEqualTo(30);
        }
      }
    }

    @Test
    @DisplayName("should instantiate simple module without imports on both runtimes")
    void shouldInstantiateSimpleModule() throws Exception {
      requireBothRuntimes();

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine);
          Store jniStore = jniEngine.createStore();
          Store panamaStore = panamaEngine.createStore();
          Module jniModule = jniEngine.compileModule(SIMPLE_MODULE_WASM);
          Module panamaModule = panamaEngine.compileModule(SIMPLE_MODULE_WASM)) {

        try (Instance jniInstance = jniLinker.instantiate(jniStore, jniModule);
            Instance panamaInstance = panamaLinker.instantiate(panamaStore, panamaModule)) {

          final WasmFunction jniGet42 = jniInstance.getFunction("get_42").orElseThrow();
          final WasmFunction panamaGet42 = panamaInstance.getFunction("get_42").orElseThrow();

          final WasmValue[] jniResult = jniGet42.call();
          final WasmValue[] panamaResult = panamaGet42.call();

          LOGGER.info("JNI get_42() = " + jniResult[0].asInt());
          LOGGER.info("Panama get_42() = " + panamaResult[0].asInt());

          assertThat(jniResult[0].asInt()).isEqualTo(42);
          assertThat(panamaResult[0].asInt()).isEqualTo(42);
        }
      }
    }
  }

  @Nested
  @DisplayName("Shadowing Behavior Parity Tests")
  class ShadowingBehaviorParityTests {

    @Test
    @DisplayName("should allow shadowing when enabled on both runtimes")
    void shouldAllowShadowingWhenEnabled() throws Exception {
      requireBothRuntimes();

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction addImplV1 =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};

      final HostFunction addImplV2 =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() * params[1].asInt())};

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        jniLinker.allowShadowing(true);
        panamaLinker.allowShadowing(true);

        jniLinker.defineHostFunction("env", "add", addType, addImplV1);
        panamaLinker.defineHostFunction("env", "add", addType, addImplV1);

        // Re-define with v2 - should succeed with shadowing enabled
        jniLinker.defineHostFunction("env", "add", addType, addImplV2);
        panamaLinker.defineHostFunction("env", "add", addType, addImplV2);

        LOGGER.info("Successfully shadowed 'env::add' on both linkers");

        assertThat(jniLinker.hasImport("env", "add")).isTrue();
        assertThat(panamaLinker.hasImport("env", "add")).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("hasImport Check Parity Tests")
  class HasImportParityTests {

    @Test
    @DisplayName("should return false for undefined imports on both runtimes")
    void shouldReturnFalseForUndefinedImports() throws Exception {
      requireBothRuntimes();

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        final boolean jniHasUndefined = jniLinker.hasImport("nonexistent", "function");
        final boolean panamaHasUndefined = panamaLinker.hasImport("nonexistent", "function");

        LOGGER.info("JNI hasImport('nonexistent', 'function'): " + jniHasUndefined);
        LOGGER.info("Panama hasImport('nonexistent', 'function'): " + panamaHasUndefined);

        assertThat(jniHasUndefined).isFalse();
        assertThat(panamaHasUndefined).isFalse();
      }
    }

    @Test
    @DisplayName("should return true for defined imports on both runtimes")
    void shouldReturnTrueForDefinedImports() throws Exception {
      requireBothRuntimes();

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction addImpl =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        assertThat(jniLinker.hasImport("env", "add")).isFalse();
        assertThat(panamaLinker.hasImport("env", "add")).isFalse();

        jniLinker.defineHostFunction("env", "add", addType, addImpl);
        panamaLinker.defineHostFunction("env", "add", addType, addImpl);

        final boolean jniHasAdd = jniLinker.hasImport("env", "add");
        final boolean panamaHasAdd = panamaLinker.hasImport("env", "add");

        LOGGER.info("JNI hasImport('env', 'add') after definition: " + jniHasAdd);
        LOGGER.info("Panama hasImport('env', 'add') after definition: " + panamaHasAdd);

        assertThat(jniHasAdd).isTrue();
        assertThat(panamaHasAdd).isTrue();
      }
    }

    @Test
    @DisplayName("should distinguish between modules and names on both runtimes")
    void shouldDistinguishBetweenModulesAndNames() throws Exception {
      requireBothRuntimes();

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction addImpl =
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())};

      try (Linker<?> jniLinker = Linker.create(jniEngine);
          Linker<?> panamaLinker = Linker.create(panamaEngine)) {

        jniLinker.defineHostFunction("env", "add", addType, addImpl);
        panamaLinker.defineHostFunction("env", "add", addType, addImpl);

        // Should find the defined import
        assertThat(jniLinker.hasImport("env", "add")).isTrue();
        assertThat(panamaLinker.hasImport("env", "add")).isTrue();

        // Different module name should not find it
        assertThat(jniLinker.hasImport("other", "add")).isFalse();
        assertThat(panamaLinker.hasImport("other", "add")).isFalse();

        // Different function name should not find it
        assertThat(jniLinker.hasImport("env", "sub")).isFalse();
        assertThat(panamaLinker.hasImport("env", "sub")).isFalse();

        LOGGER.info("Both runtimes correctly distinguish between modules and names");
      }
    }
  }

  @Nested
  @DisplayName("Linker Lifecycle Parity Tests")
  class LinkerLifecycleParityTests {

    @Test
    @DisplayName("should close linkers cleanly on both runtimes")
    void shouldCloseLinkers() throws Exception {
      requireBothRuntimes();

      final Linker<?> jniLinker = Linker.create(jniEngine);
      final Linker<?> panamaLinker = Linker.create(panamaEngine);

      assertThat(jniLinker.isValid()).isTrue();
      assertThat(panamaLinker.isValid()).isTrue();

      jniLinker.close();
      panamaLinker.close();

      LOGGER.info("Both linkers closed successfully");
    }

    @Test
    @DisplayName("should handle multiple linker creations on both runtimes")
    void shouldHandleMultipleLinkerCreations() throws Exception {
      requireBothRuntimes();

      for (int i = 0; i < 5; i++) {
        try (Linker<?> jniLinker = Linker.create(jniEngine);
            Linker<?> panamaLinker = Linker.create(panamaEngine)) {

          LOGGER.fine("Created linker pair " + (i + 1));

          assertThat(jniLinker).isNotNull();
          assertThat(panamaLinker).isNotNull();
          assertThat(jniLinker.isValid()).isTrue();
          assertThat(panamaLinker.isValid()).isTrue();
        }
      }

      LOGGER.info("Successfully created and closed 5 linker pairs");
    }
  }
}
