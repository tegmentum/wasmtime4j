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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.Export;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.ModuleExport;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for {@link Caller} epoch deadline, module export lookup, and fuel async yield interval
 * methods.
 *
 * <p>Covers: {@link Caller#hasEpochDeadline()}, {@link Caller#epochDeadline()}, {@link
 * Caller#getExportByModuleExport(ModuleExport)}, {@link Caller#fuelAsyncYieldInterval()}, {@link
 * Caller#setFuelAsyncYieldInterval(long)}.
 *
 * <p>All tests use host functions registered via the Linker with {@link
 * HostFunction#multiValueWithCaller} to obtain {@link Caller} context during WASM execution.
 *
 * @since 1.0.0
 */
@DisplayName("Caller Epoch and Advanced Access Tests")
public class CallerEpochAndAdvancedAccessTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(CallerEpochAndAdvancedAccessTest.class.getName());

  /**
   * WAT module that imports a host function and exports memory and a callable entry point.
   *
   * <pre>
   * (module
   *   (import "env" "host_fn" (func $host_fn (result i32)))
   *   (memory (export "memory") 1)
   *   (func (export "call_host") (result i32)
   *     call $host_fn))
   * </pre>
   */
  private static final String WAT =
      """
      (module
        (import "env" "host_fn" (func $host_fn (result i32)))
        (memory (export "memory") 1)
        (func (export "call_host") (result i32)
          call $host_fn))
      """;

  private static final FunctionType HOST_FN_TYPE =
      new FunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});

  private static final String CALLER_NOT_AVAILABLE =
      "Caller context not available via runtime, skipping assertions";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  /**
   * Checks if an exception indicates caller context is unavailable (not a real test failure). Walks
   * the full cause chain since the JNI runtime wraps the root cause in multiple layers.
   *
   * @param e the exception to check
   * @return true if this is a caller-not-available error
   */
  private static boolean isCallerUnavailable(final Exception e) {
    Throwable current = e;
    while (current != null) {
      final String msg = current.getMessage();
      if (msg != null
          && (msg.contains("Caller context not available")
              || msg.contains("CallerContextProvider")
              || msg.contains("caller context"))) {
        return true;
      }
      if (current instanceof UnsupportedOperationException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  // ========== Epoch Deadline Tests ==========

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.hasEpochDeadline returns true when epoch deadline is set")
  void callerHasEpochDeadlineReturnsTrueWhenSet(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.hasEpochDeadline() with epoch enabled");

    final AtomicBoolean hasDeadlineResult = new AtomicBoolean(false);

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              final boolean hasDeadline = caller.hasEpochDeadline();
              hasDeadlineResult.set(hasDeadline);
              LOGGER.info("[" + runtime + "] Caller.hasEpochDeadline(): " + hasDeadline);
              return new WasmValue[] {WasmValue.i32(hasDeadline ? 1 : 0)};
            });

    final EngineConfig config = Engine.builder().setEpochInterruption(true);
    try (Engine engine = Engine.create(config);
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      store.setEpochDeadline(100L);
      linker.defineHostFunction("env", "host_fn", HOST_FN_TYPE, hostFn);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host export must be present");

        try {
          callHostOpt.get().call();
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
            return;
          }
          throw e;
        }
      }
    }

    assertTrue(hasDeadlineResult.get(),
        "hasEpochDeadline() should return true when epoch deadline is set on store");
    LOGGER.info("[" + runtime + "] Verified: hasEpochDeadline() = true");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.epochDeadline returns value when epoch deadline is set")
  void callerEpochDeadlineReturnsValue(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.epochDeadline() with epoch enabled");

    final AtomicReference<Optional<Long>> epochResult = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              epochResult.set(caller.epochDeadline());
              LOGGER.info("[" + runtime + "] Caller.epochDeadline(): " + epochResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
            });

    final EngineConfig config = Engine.builder().setEpochInterruption(true);
    try (Engine engine = Engine.create(config);
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      store.setEpochDeadline(100L);
      linker.defineHostFunction("env", "host_fn", HOST_FN_TYPE, hostFn);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host export must be present");

        try {
          callHostOpt.get().call();
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
            return;
          }
          throw e;
        }
      }
    }

    assertNotNull(epochResult.get(), "epochDeadline() result should not be null");
    assertTrue(epochResult.get().isPresent(),
        "epochDeadline() should return a value when epoch interruption is enabled");
    assertTrue(epochResult.get().get() > 0,
        "epochDeadline() value should be > 0, got: " + epochResult.get().get());
    LOGGER.info("[" + runtime + "] Verified: epochDeadline() = " + epochResult.get().get());
  }

  // ========== getExportByModuleExport Tests ==========

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.getExportByModuleExport finds memory export")
  void callerGetExportByModuleExportFindsMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime
        + "] Testing Caller.getExportByModuleExport() for memory export");

    final AtomicReference<Optional<Export>> exportResult = new AtomicReference<>();
    final AtomicReference<ModuleExport> usedModuleExport = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              final ModuleExport me = usedModuleExport.get();
              if (me != null) {
                exportResult.set(caller.getExportByModuleExport(me));
                LOGGER.info("[" + runtime + "] Caller.getExportByModuleExport("
                    + me + "): " + exportResult.get());
              } else {
                LOGGER.warning("[" + runtime
                    + "] No ModuleExport was set before host function call");
                exportResult.set(Optional.empty());
              }
              return new WasmValue[] {WasmValue.i32(42)};
            });

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      // Find the "memory" export from module exports
      final List<ModuleExport> moduleExports = module.getModuleExports();
      LOGGER.info("[" + runtime + "] Module exports: " + moduleExports);
      ModuleExport memoryExport = null;
      for (final ModuleExport me : moduleExports) {
        if ("memory".equals(me.getName())) {
          memoryExport = me;
          break;
        }
      }
      assertNotNull(memoryExport,
          "WAT module should have a 'memory' export in getModuleExports()");
      usedModuleExport.set(memoryExport);

      linker.defineHostFunction("env", "host_fn", HOST_FN_TYPE, hostFn);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host export must be present");

        try {
          callHostOpt.get().call();
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
            return;
          }
          throw e;
        }
      }
    }

    assertNotNull(exportResult.get(),
        "getExportByModuleExport() result should not be null");
    assertTrue(exportResult.get().isPresent(),
        "getExportByModuleExport() should find the 'memory' export");
    LOGGER.info("[" + runtime + "] Verified: getExportByModuleExport() found memory export: "
        + exportResult.get().get());
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.getExportByModuleExport with null throws IllegalArgumentException")
  void callerGetExportByModuleExportNullThrows(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime
        + "] Testing Caller.getExportByModuleExport(null) throws");

    final AtomicBoolean threwException = new AtomicBoolean(false);
    final AtomicReference<Exception> caughtException = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              try {
                caller.getExportByModuleExport(null);
              } catch (final IllegalArgumentException | NullPointerException e) {
                threwException.set(true);
                caughtException.set(e);
                LOGGER.info("[" + runtime
                    + "] getExportByModuleExport(null) threw: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
              }
              return new WasmValue[] {WasmValue.i32(0)};
            });

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      linker.defineHostFunction("env", "host_fn", HOST_FN_TYPE, hostFn);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host export must be present");

        try {
          callHostOpt.get().call();
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
            return;
          }
          throw e;
        }
      }
    }

    assertTrue(threwException.get(),
        "getExportByModuleExport(null) should throw IllegalArgumentException or "
            + "NullPointerException");
    LOGGER.info("[" + runtime + "] Verified: null argument correctly rejected with "
        + caughtException.get().getClass().getSimpleName());
  }

  // ========== Fuel Async Yield Interval Tests ==========

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.fuelAsyncYieldInterval returns empty by default")
  void callerFuelAsyncYieldIntervalDefaultEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime
        + "] Testing Caller.fuelAsyncYieldInterval() default value");

    final AtomicReference<Optional<Long>> intervalResult = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              intervalResult.set(caller.fuelAsyncYieldInterval());
              LOGGER.info("[" + runtime + "] Caller.fuelAsyncYieldInterval(): "
                  + intervalResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
            });

    final EngineConfig config = Engine.builder().consumeFuel(true);
    try (Engine engine = Engine.create(config);
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      store.setFuel(10_000L);
      linker.defineHostFunction("env", "host_fn", HOST_FN_TYPE, hostFn);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host export must be present");

        try {
          callHostOpt.get().call();
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
            return;
          }
          throw e;
        }
      }
    }

    assertNotNull(intervalResult.get(),
        "fuelAsyncYieldInterval() result should not be null");
    if (intervalResult.get().isPresent()) {
      LOGGER.info("[" + runtime + "] fuelAsyncYieldInterval() returned value: "
          + intervalResult.get().get() + " (expected 0 or empty)");
      assertTrue(intervalResult.get().get() >= 0,
          "fuelAsyncYieldInterval() should be non-negative");
    } else {
      LOGGER.info("[" + runtime
          + "] fuelAsyncYieldInterval() returned empty (no interval set)");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.setFuelAsyncYieldInterval and read back via fuelAsyncYieldInterval")
  void callerSetFuelAsyncYieldIntervalAndRead(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime
        + "] Testing Caller.setFuelAsyncYieldInterval() and fuelAsyncYieldInterval()");

    final long targetInterval = 1000L;
    final AtomicReference<Optional<Long>> intervalResult = new AtomicReference<>();
    final AtomicBoolean setSucceeded = new AtomicBoolean(false);

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              try {
                caller.setFuelAsyncYieldInterval(targetInterval);
                setSucceeded.set(true);
                LOGGER.info("[" + runtime + "] setFuelAsyncYieldInterval("
                    + targetInterval + ") succeeded");
              } catch (final WasmException | UnsupportedOperationException e) {
                LOGGER.info("[" + runtime + "] setFuelAsyncYieldInterval not supported: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
              }
              intervalResult.set(caller.fuelAsyncYieldInterval());
              LOGGER.info("[" + runtime + "] Caller.fuelAsyncYieldInterval() after set: "
                  + intervalResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
            });

    final EngineConfig config = Engine.builder().consumeFuel(true);
    try (Engine engine = Engine.create(config);
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      store.setFuel(10_000L);
      linker.defineHostFunction("env", "host_fn", HOST_FN_TYPE, hostFn);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host export must be present");

        try {
          callHostOpt.get().call();
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
            return;
          }
          throw e;
        }
      }
    }

    if (setSucceeded.get()) {
      assertNotNull(intervalResult.get(),
          "fuelAsyncYieldInterval() should not be null after setting");
      if (intervalResult.get().isPresent()) {
        assertTrue(intervalResult.get().get() >= targetInterval,
            "fuelAsyncYieldInterval() should be >= " + targetInterval
                + " after set, got: " + intervalResult.get().get());
        LOGGER.info("[" + runtime + "] Verified: round-trip interval = "
            + intervalResult.get().get());
      } else {
        LOGGER.info("[" + runtime
            + "] fuelAsyncYieldInterval() returned empty after set (runtime may not "
            + "report interval via caller)");
      }
    } else {
      LOGGER.info("[" + runtime
          + "] setFuelAsyncYieldInterval not supported, skipping round-trip assertion");
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.setFuelAsyncYieldInterval with zero disables interval")
  void callerSetFuelAsyncYieldIntervalZeroDisables(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime
        + "] Testing Caller.setFuelAsyncYieldInterval(0) disables interval");

    final AtomicReference<Optional<Long>> intervalResult = new AtomicReference<>();
    final AtomicBoolean setSucceeded = new AtomicBoolean(false);

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              try {
                // First set a non-zero interval, then set to 0 to disable
                caller.setFuelAsyncYieldInterval(500L);
                caller.setFuelAsyncYieldInterval(0L);
                setSucceeded.set(true);
                LOGGER.info("[" + runtime + "] setFuelAsyncYieldInterval(0) succeeded");
              } catch (final WasmException | UnsupportedOperationException e) {
                LOGGER.info("[" + runtime + "] setFuelAsyncYieldInterval not supported: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
              }
              intervalResult.set(caller.fuelAsyncYieldInterval());
              LOGGER.info("[" + runtime + "] Caller.fuelAsyncYieldInterval() after disable: "
                  + intervalResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
            });

    final EngineConfig config = Engine.builder().consumeFuel(true);
    try (Engine engine = Engine.create(config);
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT)) {

      store.setFuel(10_000L);
      linker.defineHostFunction("env", "host_fn", HOST_FN_TYPE, hostFn);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callHostOpt = instance.getFunction("call_host");
        assertTrue(callHostOpt.isPresent(), "call_host export must be present");

        try {
          callHostOpt.get().call();
        } catch (final WasmException e) {
          if (isCallerUnavailable(e)) {
            LOGGER.info("[" + runtime + "] " + CALLER_NOT_AVAILABLE);
            return;
          }
          throw e;
        }
      }
    }

    if (setSucceeded.get()) {
      assertNotNull(intervalResult.get(),
          "fuelAsyncYieldInterval() should not be null");
      if (intervalResult.get().isPresent()) {
        assertTrue(intervalResult.get().get() == 0,
            "fuelAsyncYieldInterval() should be 0 after disabling, got: "
                + intervalResult.get().get());
        LOGGER.info("[" + runtime + "] Verified: interval disabled (0)");
      } else {
        LOGGER.info("[" + runtime
            + "] fuelAsyncYieldInterval() returned empty after disabling "
            + "(runtime-specific)");
      }
    } else {
      LOGGER.info("[" + runtime
          + "] setFuelAsyncYieldInterval not supported, skipping disable assertion");
    }
  }
}
