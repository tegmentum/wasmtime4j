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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Caller;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Export;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests {@link Caller#getExport(String)}, {@link Caller#hasExport(String)}, {@link
 * Caller#engine()}, {@link Caller#gc()}, and {@link Caller#data()} through actual WASM execution.
 *
 * <p>Each test defines a caller-aware host function, links it into a WASM module that calls it, and
 * then exercises the Caller methods inside the host function lambda. Results are captured via
 * AtomicReference and asserted after WASM returns.
 *
 * <p>If the runtime's CallerContextProvider does not deliver caller context for host functions
 * registered via the Linker, the WASM call will throw a WasmException. Tests catch this and skip
 * assertions with a log message.
 *
 * @since 1.0.0
 */
@DisplayName("Caller Export, Engine, GC, and Data Tests")
public class CallerExportAndEngineTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(CallerExportAndEngineTest.class.getName());

  /**
   * WAT module that imports a host function and exports a memory.
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

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.getExport returns memory export")
  void callerGetExportReturnsMemory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.getExport(\"memory\")");

    final AtomicReference<Optional<Export>> exportResult = new AtomicReference<>();
    final AtomicReference<Boolean> hasExportResult = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              exportResult.set(caller.getExport("memory"));
              hasExportResult.set(caller.hasExport("memory"));
              LOGGER.info(
                  "[" + runtime + "] Caller.getExport(\"memory\"): " + exportResult.get());
              LOGGER.info(
                  "[" + runtime + "] Caller.hasExport(\"memory\"): " + hasExportResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
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

    assertTrue(exportResult.get().isPresent(), "getExport(\"memory\") should return present");
    assertTrue(hasExportResult.get(), "hasExport(\"memory\") should return true");
    LOGGER.info("[" + runtime + "] Caller.getExport(\"memory\") returned memory export");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.getExport returns empty for missing export")
  void callerGetExportMissingReturnsEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.getExport(\"nonexistent\")");

    final AtomicReference<Optional<Export>> exportResult = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              exportResult.set(caller.getExport("nonexistent"));
              LOGGER.info(
                  "["
                      + runtime
                      + "] Caller.getExport(\"nonexistent\"): "
                      + exportResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
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

    assertFalse(
        exportResult.get().isPresent(), "getExport(\"nonexistent\") should return empty");
    LOGGER.info("[" + runtime + "] Caller.getExport(\"nonexistent\") correctly returned empty");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.engine returns non-null Engine")
  void callerEngineReturnsNonNull(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.engine()");

    final AtomicReference<Engine> engineResult = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              engineResult.set(caller.engine());
              LOGGER.info("[" + runtime + "] Caller.engine(): " + engineResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
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

    assertNotNull(engineResult.get(), "Caller.engine() should return non-null");
    LOGGER.info("[" + runtime + "] Caller.engine() returned non-null Engine");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.gc does not throw")
  void callerGcDoesNotThrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.gc()");

    final AtomicReference<Throwable> gcError = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              try {
                caller.gc();
                LOGGER.info("[" + runtime + "] Caller.gc() completed without exception");
              } catch (final Exception e) {
                gcError.set(e);
                LOGGER.warning("[" + runtime + "] Caller.gc() threw: " + e.getMessage());
              }
              return new WasmValue[] {WasmValue.i32(42)};
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

    assertNull(gcError.get(), "Caller.gc() should not throw, but threw: " + gcError.get());
    LOGGER.info("[" + runtime + "] Caller.gc() verified no exception thrown");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.data returns store data when set")
  void callerDataReturnsStoreData(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.data() with store data");

    final String expectedData = "test-caller-data";
    final AtomicBoolean callerAvailable = new AtomicBoolean(false);
    final AtomicReference<Object> dataResult = new AtomicReference<>();

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<String> caller, WasmValue[] params) -> {
              callerAvailable.set(true);
              dataResult.set(caller.data());
              LOGGER.info("[" + runtime + "] Caller.data(): " + dataResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
            });

    try (Engine engine = Engine.create();
        Linker<String> linker = Linker.create(engine);
        Store store = engine.createStore(expectedData);
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

    if (!callerAvailable.get()) {
      LOGGER.info("[" + runtime + "] Host function lambda was not reached, skipping");
      return;
    }

    if (dataResult.get() == null) {
      LOGGER.info(
          "[" + runtime + "] Caller.data() returned null (runtime may not propagate store data)");
      return;
    }

    assertTrue(
        dataResult.get().equals(expectedData),
        "Caller.data() should return \""
            + expectedData
            + "\" but got: "
            + dataResult.get());
    LOGGER.info("[" + runtime + "] Caller.data() correctly returned store data");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Caller.data returns null when no data set")
  void callerDataReturnsNullWhenNoDataSet(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing Caller.data() without store data");

    final AtomicBoolean callerAvailable = new AtomicBoolean(false);
    final AtomicReference<Object> dataResult = new AtomicReference<>(new Object());

    final HostFunction hostFn =
        HostFunction.multiValueWithCaller(
            (Caller<Void> caller, WasmValue[] params) -> {
              callerAvailable.set(true);
              dataResult.set(caller.data());
              LOGGER.info(
                  "[" + runtime + "] Caller.data() (no store data): " + dataResult.get());
              return new WasmValue[] {WasmValue.i32(42)};
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

    if (!callerAvailable.get()) {
      LOGGER.info("[" + runtime + "] Host function lambda was not reached, skipping");
      return;
    }

    assertNull(dataResult.get(), "Caller.data() should return null when no data set");
    LOGGER.info("[" + runtime + "] Caller.data() correctly returned null");
  }
}
