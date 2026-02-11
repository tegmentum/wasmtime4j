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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
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
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * DualRuntime tests for {@link HostFunction#streaming(HostFunction.StreamingHostFunction)}.
 *
 * <p>Existing tests in HostFunctionIntegrationTest use {@code @Nested @Test} (not DualRuntime).
 * This file adds DualRuntime coverage with linker integration, verifying that streaming host
 * functions work correctly in both JNI and Panama runtimes.
 *
 * @since 1.0.0
 */
@DisplayName("HostFunction Streaming DualRuntime Tests")
public class HostFunctionStreamingDualRuntimeTest extends DualRuntimeTest {

  private static final Logger LOGGER =
      Logger.getLogger(HostFunctionStreamingDualRuntimeTest.class.getName());

  /** WAT module that imports a stream_fn returning i32 and re-exports it via call_stream. */
  private static final String STREAM_WAT =
      """
      (module
        (import "env" "stream_fn" (func $stream_fn (result i32)))
        (func (export "call_stream") (result i32)
          call $stream_fn))
      """;

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("streaming host function through linker returns correct value")
  void streamingHostFunctionThroughLinker(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing streaming host function through linker");

    final FunctionType streamType =
        new FunctionType(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

    final HostFunction streamImpl =
        HostFunction.streaming(
            (params, context) -> {
              context.yield(WasmValue.i32(42));
            });

    try (Engine engine = Engine.create();
        Linker<Void> linker = Linker.create(engine);
        Store store = engine.createStore();
        Module module = engine.compileWat(STREAM_WAT)) {

      linker.defineHostFunction("env", "stream_fn", streamType, streamImpl);

      try (Instance instance = linker.instantiate(store, module)) {
        final Optional<WasmFunction> callStream = instance.getFunction("call_stream");
        assertTrue(callStream.isPresent(), "call_stream export must be present");

        final WasmValue[] results = callStream.get().call();
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.length, "Should have 1 result");
        assertEquals(42, results[0].asI32(), "Streaming function should return 42");
        LOGGER.info("[" + runtime + "] Streaming function through linker returned: "
            + results[0].asI32());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("streaming yields multiple values via direct execute")
  void streamingYieldsMultipleValues(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing streaming yields multiple values");

    final HostFunction streamImpl =
        HostFunction.streaming(
            (params, context) -> {
              context.yield(WasmValue.i32(10));
              context.yield(WasmValue.i32(20));
              context.yield(WasmValue.i32(30));
            });

    final WasmValue[] results = streamImpl.execute(new WasmValue[0]);

    assertNotNull(results, "Results should not be null");
    assertEquals(3, results.length, "Should have 3 results");
    assertEquals(10, results[0].asI32(), "First result should be 10");
    assertEquals(20, results[1].asI32(), "Second result should be 20");
    assertEquals(30, results[2].asI32(), "Third result should be 30");
    LOGGER.info("[" + runtime + "] Streaming yielded 3 values: ["
        + results[0].asI32() + ", " + results[1].asI32() + ", " + results[2].asI32() + "]");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("streaming context ignores null yields")
  void streamingContextNullIgnored(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing streaming context ignores null yields");

    final HostFunction streamImpl =
        HostFunction.streaming(
            (params, context) -> {
              context.yield((WasmValue) null);
              context.yield(WasmValue.i32(99));
            });

    final WasmValue[] results = streamImpl.execute(new WasmValue[0]);

    assertNotNull(results, "Results should not be null");
    assertEquals(1, results.length,
        "Should have 1 result (null yield should be skipped)");
    assertEquals(99, results[0].asI32(), "Only non-null value should be 99");
    LOGGER.info("[" + runtime + "] Streaming context correctly skipped null, kept value 99");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("streaming context clear resets results")
  void streamingContextClearResets(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing streaming context clear resets results");

    final HostFunction.StreamingContext context = new HostFunction.StreamingContext();

    context.yield(WasmValue.i32(1));
    context.yield(WasmValue.i32(2));
    context.yield(WasmValue.i32(3));
    assertEquals(3, context.getResultCount(), "Should have 3 results before clear");

    context.clear();
    assertEquals(0, context.getResultCount(), "Should have 0 results after clear");

    final WasmValue[] results = context.getResults();
    assertNotNull(results, "Results should not be null after clear");
    assertEquals(0, results.length, "Results array should be empty after clear");
    LOGGER.info("[" + runtime + "] StreamingContext.clear() reset count from 3 to 0");
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("streaming with no yields returns empty array")
  void streamingNoYieldsReturnsEmpty(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing streaming with no yields returns empty");

    final HostFunction streamImpl =
        HostFunction.streaming(
            (params, context) -> {
              // No yields
            });

    final WasmValue[] results = streamImpl.execute(new WasmValue[0]);

    assertNotNull(results, "Results should not be null");
    assertEquals(0, results.length, "No yields should return empty array");
    LOGGER.info("[" + runtime + "] Streaming with no yields returned empty array");
  }
}
