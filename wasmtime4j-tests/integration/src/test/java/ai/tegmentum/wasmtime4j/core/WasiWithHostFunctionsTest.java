package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests that WASI modules can coexist with custom host functions in the same Linker.
 *
 * <p>This is a critical production scenario: a WASM module that imports both WASI system interface
 * functions (e.g., fd_write for stdio) and application-specific host functions in a custom
 * namespace (e.g., "env"). Both sets of imports must be resolved by the same Linker for
 * instantiation to succeed.
 */
@DisplayName("WASI With Host Functions Tests")
public class WasiWithHostFunctionsTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiWithHostFunctionsTest.class.getName());

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI module with custom host function in same Linker")
  public void testWasiModuleWithCustomHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Module imports both WASI (proc_exit) and a custom host function (env.get_value).
    // The exported "call_host" function calls our custom host function and returns its result.
    // proc_exit is imported but not called — it just needs to be resolvable.
    final String wat =
        "(module\n"
            + "  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n"
            + "  (import \"env\" \"get_value\" (func $get_value (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"call_host\") (result i32)\n"
            + "    call $get_value\n"
            + "  )\n"
            + ")";

    final AtomicInteger hostCallCount = new AtomicInteger(0);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<WasiContext> linker = Linker.create(engine)) {

      // Add WASI imports
      final WasiContext wasiCtx = WasiContext.create();
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      // Add custom host function on the same linker
      linker.defineHostFunction(
          "env",
          "get_value",
          FunctionType.of(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32}),
          (params) -> {
            hostCallCount.incrementAndGet();
            return new WasmValue[] {WasmValue.i32(42)};
          });

      final Module module = engine.compileWat(wat);
      try (Instance instance = linker.instantiate(store, module)) {
        assertNotNull(instance, "Instance should be created with both WASI and custom imports");

        final WasmValue[] results = instance.callFunction("call_host");
        assertEquals(1, results.length, "Should return exactly 1 result");
        assertEquals(42, results[0].asInt(), "Custom host function should return 42");
        assertEquals(1, hostCallCount.get(), "Host function should have been called once");

        LOGGER.info(
            "[" + runtime + "] WASI + custom host function: call_host() = " + results[0].asInt());
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI module calls both WASI and custom host functions")
  public void testWasiAndCustomHostBothCalled(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Module imports WASI environ_sizes_get (returns 0 env vars) and a custom host function.
    // The exported function calls both: gets env count via WASI, adds our custom value.
    final String wat =
        "(module\n"
            + "  (import \"wasi_snapshot_preview1\" \"environ_sizes_get\""
            + " (func $environ_sizes_get (param i32 i32) (result i32)))\n"
            + "  (import \"env\" \"add_offset\" (func $add_offset (param i32) (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"compute\") (result i32)\n"
            + "    ;; Call environ_sizes_get to write env count at offset 0 and data size at"
            + " offset 4\n"
            + "    i32.const 0\n"
            + "    i32.const 4\n"
            + "    call $environ_sizes_get\n"
            + "    drop\n"
            + "    ;; Load the env count (should be 0 with no env vars set)\n"
            + "    i32.const 0\n"
            + "    i32.load\n"
            + "    ;; Add our custom offset via host function\n"
            + "    call $add_offset\n"
            + "  )\n"
            + ")";

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<WasiContext> linker = Linker.create(engine)) {

      // WASI context with no env vars → environ_sizes_get writes 0 to env count
      final WasiContext wasiCtx = WasiContext.create();
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      // Custom host function: adds 100 to input
      linker.defineHostFunction(
          "env",
          "add_offset",
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32}),
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() + 100)});

      final Module module = engine.compileWat(wat);
      try (Instance instance = linker.instantiate(store, module)) {
        final WasmValue[] results = instance.callFunction("compute");

        // environ_sizes_get writes 0 env vars → load gives 0 → add_offset(0) = 100
        assertEquals(100, results[0].asInt(), "0 env vars + 100 offset should equal 100");

        LOGGER.info(
            "[" + runtime + "] WASI + custom host both called: compute() = " + results[0].asInt());
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI module with env vars and custom host function")
  public void testWasiWithEnvVarsAndCustomHost(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Module imports WASI environ_sizes_get and a custom host function.
    // We set 2 env vars so environ_sizes_get should report count=2.
    final String wat =
        "(module\n"
            + "  (import \"wasi_snapshot_preview1\" \"environ_sizes_get\""
            + " (func $environ_sizes_get (param i32 i32) (result i32)))\n"
            + "  (import \"env\" \"multiply\" (func $multiply (param i32 i32) (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"env_count_times_factor\") (param i32) (result i32)\n"
            + "    ;; Get env count\n"
            + "    i32.const 0\n"
            + "    i32.const 4\n"
            + "    call $environ_sizes_get\n"
            + "    drop\n"
            + "    ;; Load env count from offset 0\n"
            + "    i32.const 0\n"
            + "    i32.load\n"
            + "    ;; Multiply env count by the parameter\n"
            + "    local.get 0\n"
            + "    call $multiply\n"
            + "  )\n"
            + ")";

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<WasiContext> linker = Linker.create(engine)) {

      final WasiContext wasiCtx = WasiContext.create();
      wasiCtx.setEnv("KEY1", "value1");
      wasiCtx.setEnv("KEY2", "value2");
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      linker.defineHostFunction(
          "env",
          "multiply",
          FunctionType.of(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32}),
          (params) -> new WasmValue[] {WasmValue.i32(params[0].asInt() * params[1].asInt())});

      final Module module = engine.compileWat(wat);
      try (Instance instance = linker.instantiate(store, module)) {
        final WasmValue[] results =
            instance.callFunction("env_count_times_factor", WasmValue.i32(10));

        // 2 env vars * 10 = 20
        assertEquals(20, results[0].asInt(), "2 env vars * 10 should equal 20");

        LOGGER.info(
            "["
                + runtime
                + "] WASI with env vars + custom host: env_count_times_factor(10) = "
                + results[0].asInt());
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple custom host functions alongside WASI")
  public void testMultipleCustomHostFunctionsWithWasi(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Module imports WASI and two custom host functions in the "env" namespace.
    final String wat =
        "(module\n"
            + "  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n"
            + "  (import \"env\" \"get_a\" (func $get_a (result i32)))\n"
            + "  (import \"env\" \"get_b\" (func $get_b (result i32)))\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"sum_ab\") (result i32)\n"
            + "    call $get_a\n"
            + "    call $get_b\n"
            + "    i32.add\n"
            + "  )\n"
            + ")";

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<WasiContext> linker = Linker.create(engine)) {

      final WasiContext wasiCtx = WasiContext.create();
      WasiLinkerUtils.addToLinker(linker, wasiCtx);

      linker.defineHostFunction(
          "env",
          "get_a",
          FunctionType.of(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32}),
          (params) -> new WasmValue[] {WasmValue.i32(30)});

      linker.defineHostFunction(
          "env",
          "get_b",
          FunctionType.of(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32}),
          (params) -> new WasmValue[] {WasmValue.i32(12)});

      final Module module = engine.compileWat(wat);
      try (Instance instance = linker.instantiate(store, module)) {
        final WasmValue[] results = instance.callFunction("sum_ab");

        assertEquals(42, results[0].asInt(), "get_a(30) + get_b(12) should equal 42");

        LOGGER.info(
            "[" + runtime + "] Multiple custom hosts + WASI: sum_ab() = " + results[0].asInt());
      }
      module.close();
    }
  }
}
