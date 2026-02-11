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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for WASI environment variables and program arguments. Verifies that WASM modules can call
 * environ_sizes_get and args_sizes_get WASI syscalls, and that environment variables and program
 * arguments set via WasiContext are correctly propagated to the WASI runtime.
 *
 * @since 1.0.0
 */
@DisplayName("WASI Environment and Arguments Tests")
public class WasiEnvironTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiEnvironTest.class.getName());

  /**
   * WAT module that reads environment variable count via environ_sizes_get. Returns the number of
   * environment variables at memory offset 0 and the total buffer size at offset 4.
   */
  private static final String ENVIRON_COUNT_WAT =
      """
      (module
        (import "wasi_snapshot_preview1" "environ_sizes_get"
          (func $environ_sizes_get (param i32 i32) (result i32)))
        (memory (export "memory") 1)

        (func (export "get_environ_count") (result i32)
          i32.const 0      ;; count ptr
          i32.const 4      ;; buf_size ptr
          call $environ_sizes_get
        )

        (func (export "read_count") (result i32)
          i32.const 0
          i32.load
        )

        (func (export "read_buf_size") (result i32)
          i32.const 4
          i32.load
        )
      )
      """;

  /**
   * WAT module that reads argument count via args_sizes_get. Returns the number of arguments at
   * memory offset 0 and the total buffer size at offset 4.
   */
  private static final String ARGS_COUNT_WAT =
      """
      (module
        (import "wasi_snapshot_preview1" "args_sizes_get"
          (func $args_sizes_get (param i32 i32) (result i32)))
        (memory (export "memory") 1)

        (func (export "get_args_count") (result i32)
          i32.const 0      ;; count ptr
          i32.const 4      ;; buf_size ptr
          call $args_sizes_get
        )

        (func (export "read_count") (result i32)
          i32.const 0
          i32.load
        )

        (func (export "read_buf_size") (result i32)
          i32.const 4
          i32.load
        )
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("environ_sizes_get syscall returns success with configured env vars")
  void environSizesGetReturnsSuccess(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing environ_sizes_get syscall");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(ENVIRON_COUNT_WAT);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.setEnv("TEST_KEY", "test_value");
      wasiCtx.setEnv("ANOTHER_KEY", "another_value");

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);

      final int errno = instance.callFunction("get_environ_count")[0].asInt();
      assertEquals(0, errno, "environ_sizes_get should return WASI_ESUCCESS (0)");

      final int count = instance.callFunction("read_count")[0].asInt();
      LOGGER.info("[" + runtime + "] Env count returned by environ_sizes_get: " + count);
      assertEquals(2, count, "Should have 2 environment variables (TEST_KEY and ANOTHER_KEY)");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("args_sizes_get syscall returns success with configured args")
  void argsSizesGetReturnsSuccess(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing args_sizes_get syscall");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(ARGS_COUNT_WAT);
      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.setArgv(new String[] {"program", "arg1", "arg2", "arg3"});

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);

      final int errno = instance.callFunction("get_args_count")[0].asInt();
      assertEquals(0, errno, "args_sizes_get should return WASI_ESUCCESS (0)");

      final int count = instance.callFunction("read_count")[0].asInt();
      LOGGER.info("[" + runtime + "] Args count returned by args_sizes_get: " + count);
      assertEquals(4, count, "Should have 4 arguments (program, arg1, arg2, arg3)");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Empty environment returns zero count")
  void emptyEnvironment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing empty environment");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(ENVIRON_COUNT_WAT);
      final WasiContext wasiCtx = WasiContext.create();
      // No env vars set

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);

      final int errno = instance.callFunction("get_environ_count")[0].asInt();
      assertEquals(0, errno, "environ_sizes_get should succeed with empty env");

      final int count = instance.callFunction("read_count")[0].asInt();
      assertEquals(0, count, "Should have 0 environment variables with no env configured");
      LOGGER.info("[" + runtime + "] Empty env count: " + count);

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("No args configured returns minimal count")
  void noArgsConfigured(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing no args configured");

    try (Engine engine = Engine.create();
        Store store = engine.createStore()) {
      final Module module = engine.compileWat(ARGS_COUNT_WAT);
      final WasiContext wasiCtx = WasiContext.create();
      // No args set

      final Linker<WasiContext> linker = Linker.create(engine);
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      final Instance instance = linker.instantiate(store, module);

      final int errno = instance.callFunction("get_args_count")[0].asInt();
      assertEquals(0, errno, "args_sizes_get should succeed with no args");

      final int count = instance.callFunction("read_count")[0].asInt();
      LOGGER.info("[" + runtime + "] Args count with no argv: " + count);
      // Wasmtime may return 0 or 1 (with a default program name) when no args are set
      assertTrue(count >= 0 && count <= 1, "Args count should be 0 or 1 with no args configured");

      instance.close();
      linker.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WasiContext env configuration API works without errors")
  void wasiContextEnvConfigurationApi(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing WasiContext env configuration API");

    // Verify the API accepts various configurations without throwing
    final WasiContext ctx = WasiContext.create();
    assertNotNull(ctx, "WasiContext.create() should return non-null");

    // Single env var
    ctx.setEnv("KEY1", "value1");
    LOGGER.info("[" + runtime + "] setEnv single succeeded");

    // Bulk env vars
    final Map<String, String> envMap = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      envMap.put("VAR_" + i, "value_" + i);
    }
    ctx.setEnv(envMap);
    LOGGER.info("[" + runtime + "] setEnv bulk (100 vars) succeeded");

    // Special characters
    ctx.setEnv("WITH_SPACES", "hello world");
    ctx.setEnv("WITH_EQUALS", "key=value");
    ctx.setEnv("WITH_UNICODE", "café");
    LOGGER.info("[" + runtime + "] setEnv with special chars succeeded");

    // Args
    ctx.setArgv(new String[] {"program", "arg1", "arg2"});
    LOGGER.info("[" + runtime + "] setArgv succeeded");

    // Empty args
    ctx.setArgv(new String[] {});
    LOGGER.info("[" + runtime + "] setArgv empty succeeded");
  }
}
