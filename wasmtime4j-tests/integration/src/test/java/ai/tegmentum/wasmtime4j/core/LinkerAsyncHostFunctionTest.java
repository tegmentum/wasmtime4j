package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.HostFunctionAsync;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Linker#defineHostFunctionAsync} with native {@code Func::new_async()} support.
 *
 * <p>Verifies that async host functions are properly registered and invoked through the Wasmtime
 * async executor, enabling cooperative scheduling for host functions that perform I/O or other
 * async operations.
 *
 * @since 1.1.0
 */
@DisplayName("Linker Async Host Function Tests")
public class LinkerAsyncHostFunctionTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(LinkerAsyncHostFunctionTest.class.getName());

  /** WAT module that imports a host function and calls it from an exported function. */
  private static final String WAT_IMPORT_AND_CALL =
      """
      (module
        (import "env" "host_add" (func $host_add (param i32 i32) (result i32)))
        (func (export "compute") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          call $host_add
        )
      )
      """;

  /** WAT module that imports a host function with no return value. */
  private static final String WAT_IMPORT_VOID =
      """
      (module
        (import "env" "host_log" (func $host_log (param i32)))
        (func (export "do_log") (param i32)
          local.get 0
          call $host_log
        )
      )
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("async host function returns correct result via Func::new_async()")
  void asyncHostFunctionReturnsCorrectResult(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing async host function with result");

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Linker<Void> linker = Linker.create(engine)) {

      // Define an async host function that adds two i32 values
      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunctionAsync asyncAdd =
          (caller, params) -> {
            LOGGER.fine(
                "Async host_add called with " + params[0].asInt() + " + " + params[1].asInt());
            final int result = params[0].asInt() + params[1].asInt();
            return CompletableFuture.completedFuture(new WasmValue[] {WasmValue.i32(result)});
          };

      linker.defineHostFunctionAsync("env", "host_add", addType, asyncAdd);
      LOGGER.info("[" + runtime + "] Async host function defined successfully");

      // Compile and instantiate the module
      final Module module = engine.compileWat(WAT_IMPORT_AND_CALL);
      final Instance instance = linker.instantiate(store, module);
      LOGGER.info("[" + runtime + "] Module instantiated with async host function");

      // Call the exported function that invokes the async host function
      final WasmValue[] results =
          instance.callFunction("compute", WasmValue.i32(17), WasmValue.i32(25));
      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should return exactly one value");
      assertEquals(42, results[0].asInt(), "17 + 25 should equal 42");
      LOGGER.info(
          "[" + runtime + "] Async host function returned correct result: " + results[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("async host function with void return completes successfully")
  void asyncHostFunctionVoidReturn(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing async host function with void return");

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Linker<Void> linker = Linker.create(engine)) {

      // Define an async host function that takes an i32 and returns nothing
      final FunctionType logType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});

      final java.util.concurrent.atomic.AtomicInteger loggedValue =
          new java.util.concurrent.atomic.AtomicInteger(-1);

      final HostFunctionAsync asyncLog =
          (caller, params) -> {
            final int value = params[0].asInt();
            LOGGER.fine("Async host_log called with value: " + value);
            loggedValue.set(value);
            return CompletableFuture.completedFuture(new WasmValue[0]);
          };

      linker.defineHostFunctionAsync("env", "host_log", logType, asyncLog);

      final Module module = engine.compileWat(WAT_IMPORT_VOID);
      final Instance instance = linker.instantiate(store, module);

      // Call the function that invokes the async host function
      instance.callFunction("do_log", WasmValue.i32(99));

      assertEquals(99, loggedValue.get(), "Async host function should have been called with 99");
      LOGGER.info("[" + runtime + "] Async void host function logged value: " + loggedValue.get());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("async host function with CompletableFuture that completes later")
  void asyncHostFunctionWithDelayedFuture(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing async host function with delayed future");

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Linker<Void> linker = Linker.create(engine)) {

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      // Define an async host function that completes on a separate thread
      final HostFunctionAsync asyncAdd =
          (caller, params) -> {
            return CompletableFuture.supplyAsync(
                () -> {
                  final int result = params[0].asInt() + params[1].asInt();
                  LOGGER.fine("Async host_add completed on background thread: " + result);
                  return new WasmValue[] {WasmValue.i32(result)};
                });
          };

      linker.defineHostFunctionAsync("env", "host_add", addType, asyncAdd);

      final Module module = engine.compileWat(WAT_IMPORT_AND_CALL);
      final Instance instance = linker.instantiate(store, module);

      final WasmValue[] results =
          instance.callFunction("compute", WasmValue.i32(100), WasmValue.i32(200));
      assertEquals(300, results[0].asInt(), "100 + 200 should equal 300");
      LOGGER.info("[" + runtime + "] Delayed async host function returned: " + results[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("async host function works on non-async engine (sync wrapper)")
  void asyncHostFunctionWorksOnNonAsyncEngine(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing async host function on non-async engine");

    // Create engine WITHOUT async support — async host functions use sync Func::new()
    // under the hood, so they should work on any engine.
    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Linker<Void> linker = Linker.create(engine)) {

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunctionAsync asyncAdd =
          (caller, params) ->
              CompletableFuture.completedFuture(
                  new WasmValue[] {WasmValue.i32(params[0].asInt() + params[1].asInt())});

      linker.defineHostFunctionAsync("env", "host_add", addType, asyncAdd);
      LOGGER.info("[" + runtime + "] defineHostFunctionAsync succeeded");

      final Module module = engine.compileWat(WAT_IMPORT_AND_CALL);
      final Instance instance = linker.instantiate(store, module);

      // Call the function — async host function should execute via sync wrapper
      final WasmValue[] results =
          instance.callFunction("compute", WasmValue.i32(10), WasmValue.i32(20));
      assertEquals(30, results[0].asInt(), "10 + 20 should equal 30");
      LOGGER.info(
          "[" + runtime + "] Async host function on non-async engine: " + results[0].asInt());

      instance.close();
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("async host function exception propagates correctly")
  void asyncHostFunctionExceptionPropagates(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing async host function exception propagation");

    try (final Engine engine = Engine.create();
        final Store store = engine.createStore();
        final Linker<Void> linker = Linker.create(engine)) {

      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      // Define an async host function that fails
      final HostFunctionAsync asyncFailing =
          (caller, params) -> {
            final CompletableFuture<WasmValue[]> future = new CompletableFuture<>();
            future.completeExceptionally(new WasmException("Intentional async failure"));
            return future;
          };

      linker.defineHostFunctionAsync("env", "host_add", addType, asyncFailing);

      final Module module = engine.compileWat(WAT_IMPORT_AND_CALL);
      final Instance instance = linker.instantiate(store, module);

      // Calling the function should propagate the async exception
      final Exception ex =
          assertThrows(
              Exception.class,
              () -> instance.callFunction("compute", WasmValue.i32(1), WasmValue.i32(2)),
              "Call should fail with async exception");

      LOGGER.info("[" + runtime + "] Async exception propagated correctly: " + ex.getMessage());
      assertNotNull(ex.getMessage(), "Exception should have a descriptive message");

      instance.close();
      module.close();
    }
  }
}
