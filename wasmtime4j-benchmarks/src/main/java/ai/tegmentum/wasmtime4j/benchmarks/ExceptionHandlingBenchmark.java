/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.experimental.ExceptionHandler;
import ai.tegmentum.wasmtime4j.experimental.ExceptionInstructions;
import ai.tegmentum.wasmtime4j.experimental.ExceptionMarshaling;
import ai.tegmentum.wasmtime4j.experimental.ExperimentalFeatures;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for WebAssembly exception handling performance.
 *
 * <p>This benchmark suite measures the performance overhead of various exception handling
 * operations to ensure they meet production performance requirements.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class ExceptionHandlingBenchmark {

  @Param({"1", "5", "10", "20"})
  private int payloadSize;

  @Param({"true", "false"})
  private boolean enableValidation;

  @Param({"true", "false"})
  private boolean enableStackTraces;

  private ExceptionHandler handler;
  private ExceptionHandler.ExceptionTag smallTag;
  private ExceptionHandler.ExceptionTag mediumTag;
  private ExceptionHandler.ExceptionTag largeTag;
  private List<WasmValue> smallPayload;
  private List<WasmValue> mediumPayload;
  private List<WasmValue> largePayload;
  private List<Object> javaObjects;
  private List<WasmValueType> types;

  @Setup(Level.Trial)
  public void setupTrial() {
    // Enable exception handling feature
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);

    // Create exception handler with configuration
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(true)
            .enableExceptionUnwinding(true)
            .maxUnwindDepth(1000)
            .validateExceptionTypes(enableValidation)
            .enableStackTraces(enableStackTraces)
            .enableExceptionPropagation(true)
            .build();

    handler = new ExceptionHandler(config);

    // Create exception tags with different payload sizes
    smallTag = handler.createExceptionTag("small", List.of(WasmValueType.I32));
    mediumTag =
        handler.createExceptionTag(
            "medium",
            Arrays.asList(
                WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64));
    largeTag =
        handler.createExceptionTag(
            "large",
            Arrays.asList(
                WasmValueType.I32,
                WasmValueType.I64,
                WasmValueType.F32,
                WasmValueType.F64,
                WasmValueType.I32,
                WasmValueType.I64,
                WasmValueType.F32,
                WasmValueType.F64,
                WasmValueType.I32,
                WasmValueType.I64,
                WasmValueType.F32,
                WasmValueType.F64,
                WasmValueType.I32,
                WasmValueType.I64,
                WasmValueType.F32,
                WasmValueType.F64,
                WasmValueType.I32,
                WasmValueType.I64,
                WasmValueType.F32,
                WasmValueType.F64));

    // Create payloads
    smallPayload = List.of(WasmValue.i32(42));
    mediumPayload =
        Arrays.asList(
            WasmValue.i32(42), WasmValue.i64(123L), WasmValue.f32(3.14f), WasmValue.f64(2.718));
    largePayload =
        Arrays.asList(
            WasmValue.i32(1),
            WasmValue.i64(2L),
            WasmValue.f32(3.0f),
            WasmValue.f64(4.0),
            WasmValue.i32(5),
            WasmValue.i64(6L),
            WasmValue.f32(7.0f),
            WasmValue.f64(8.0),
            WasmValue.i32(9),
            WasmValue.i64(10L),
            WasmValue.f32(11.0f),
            WasmValue.f64(12.0),
            WasmValue.i32(13),
            WasmValue.i64(14L),
            WasmValue.f32(15.0f),
            WasmValue.f64(16.0),
            WasmValue.i32(17),
            WasmValue.i64(18L),
            WasmValue.f32(19.0f),
            WasmValue.f64(20.0));

    // Create Java objects for marshaling
    javaObjects = Arrays.asList(42, 123L, 3.14f, 2.718);
    types =
        Arrays.asList(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    if (handler != null) {
      handler.close();
    }
    ExperimentalFeatures.reset();
  }

  @Benchmark
  public void benchmarkExceptionTagCreation(final Blackhole bh) {
    final List<WasmValueType> paramTypes = createParameterTypes(payloadSize);
    final ExceptionHandler.ExceptionTag tag =
        handler.createExceptionTag("benchmark_tag_" + System.nanoTime(), paramTypes);
    bh.consume(tag);
  }

  @Benchmark
  public void benchmarkSmallExceptionThrowing(final Blackhole bh) {
    try {
      handler.throwException(smallTag, smallPayload);
    } catch (final ExceptionHandler.WasmException e) {
      bh.consume(e);
    }
  }

  @Benchmark
  public void benchmarkMediumExceptionThrowing(final Blackhole bh) {
    try {
      handler.throwException(mediumTag, mediumPayload);
    } catch (final ExceptionHandler.WasmException e) {
      bh.consume(e);
    }
  }

  @Benchmark
  public void benchmarkLargeExceptionThrowing(final Blackhole bh) {
    try {
      handler.throwException(largeTag, largePayload);
    } catch (final ExceptionHandler.WasmException e) {
      bh.consume(e);
    }
  }

  @Benchmark
  public void benchmarkExceptionCatching(final Blackhole bh) {
    try {
      handler.throwException(mediumTag, mediumPayload);
    } catch (final ExceptionHandler.WasmException e) {
      final List<WasmValue> payload = handler.catchException(e, mediumTag);
      bh.consume(payload);
    }
  }

  @Benchmark
  public void benchmarkExceptionHandlerRegistration(final Blackhole bh) {
    final ExceptionHandler.ExceptionHandlerFunction handlerFunc =
        (tag, payload) -> {
          bh.consume(tag);
          bh.consume(payload);
          return true;
        };

    handler.registerExceptionHandler(mediumTag, handlerFunc);
    bh.consume(handlerFunc);
  }

  @Benchmark
  public void benchmarkExceptionUnwinding(final Blackhole bh) {
    final boolean result = handler.performUnwinding(payloadSize);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkPayloadMarshaling(final Blackhole bh) {
    final List<WasmValue> values = ExceptionMarshaling.marshalPayload(javaObjects, types);
    bh.consume(values);
  }

  @Benchmark
  public void benchmarkPayloadUnmarshaling(final Blackhole bh) {
    final List<Object> objects = ExceptionMarshaling.unmarshalPayload(mediumPayload);
    bh.consume(objects);
  }

  @Benchmark
  public void benchmarkPayloadSerialization(final Blackhole bh) {
    final byte[] data = ExceptionMarshaling.serializePayload(mediumPayload);
    bh.consume(data);
  }

  @Benchmark
  public void benchmarkPayloadDeserialization(final Blackhole bh) {
    final byte[] data = ExceptionMarshaling.serializePayload(mediumPayload);
    final List<WasmValue> payload = ExceptionMarshaling.deserializePayload(data);
    bh.consume(payload);
  }

  @Benchmark
  public void benchmarkPayloadValidation(final Blackhole bh) {
    try {
      ExceptionMarshaling.validatePayload(mediumPayload, mediumTag.getParameterTypes());
      bh.consume(true);
    } catch (final Exception e) {
      bh.consume(false);
    }
  }

  @Benchmark
  public void benchmarkTryBlockCreation(final Blackhole bh) {
    final ExceptionInstructions.SupplierTryBlock tryBlock =
        ExceptionInstructions.tryExecution(handler, () -> mediumPayload);
    bh.consume(tryBlock);
  }

  @Benchmark
  public void benchmarkTryBlockExecution(final Blackhole bh) {
    final ExceptionInstructions.SupplierTryBlock tryBlock =
        ExceptionInstructions.tryExecution(
            handler,
            () -> {
              // Simulate some work
              return Arrays.asList(WasmValue.i32(42), WasmValue.f64(3.14));
            });

    final List<WasmValue> result = tryBlock.execute();
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkTryCatchExecution(final Blackhole bh) {
    final ExceptionInstructions.SupplierTryBlock tryBlock =
        ExceptionInstructions.tryExecution(
            handler,
            () -> {
              // Throw an exception
              handler.throwException(mediumTag, mediumPayload);
              return mediumPayload; // Never reached
            });

    try {
      final List<WasmValue> result =
          tryBlock
              .catchException(
                  mediumTag,
                  (tag, payload) -> {
                    bh.consume(tag);
                    return payload; // Return the caught payload
                  })
              .execute();
      bh.consume(result);
    } catch (final Exception e) {
      bh.consume(e);
    }
  }

  @Benchmark
  public void benchmarkMultiCatchExecution(final Blackhole bh) {
    final ExceptionInstructions.SupplierTryBlock tryBlock =
        ExceptionInstructions.tryExecution(
            handler,
            () -> {
              // Randomly throw one of several exceptions
              final int choice = (int) (System.nanoTime() % 3);
              switch (choice) {
                case 0:
                  handler.throwException(smallTag, smallPayload);
                  break;
                case 1:
                  handler.throwException(mediumTag, mediumPayload);
                  break;
                default:
                  return Arrays.asList(WasmValue.i32(999)); // Success case
              }
              return null; // Never reached
            });

    try {
      final List<WasmValue> result =
          tryBlock
              .catchException(smallTag, (tag, payload) -> payload)
              .catchException(mediumTag, (tag, payload) -> payload)
              .execute();
      bh.consume(result);
    } catch (final Exception e) {
      bh.consume(e);
    }
  }

  @Benchmark
  public void benchmarkExceptionWithStackTrace(final Blackhole bh) {
    if (!enableStackTraces) {
      bh.consume(null);
      return;
    }

    try {
      handler.throwException(mediumTag, mediumPayload);
    } catch (final ExceptionHandler.WasmException e) {
      final String stackTrace = e.getWasmStackTrace();
      bh.consume(stackTrace);
    }
  }

  @Benchmark
  public void benchmarkConcurrentExceptionOperations(final Blackhole bh)
      throws InterruptedException {
    final int threadCount = 4;
    final Thread[] threads = new Thread[threadCount];
    final boolean[] results = new boolean[threadCount];

    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  // Each thread creates a unique tag and throws an exception
                  final ExceptionHandler.ExceptionTag threadTag =
                      handler.createExceptionTag(
                          "thread_" + threadIndex, List.of(WasmValueType.I32));
                  handler.throwException(threadTag, List.of(WasmValue.i32(threadIndex)));
                  results[threadIndex] = false; // Should not reach here
                } catch (final ExceptionHandler.WasmException e) {
                  results[threadIndex] = true;
                } catch (final Exception e) {
                  results[threadIndex] = false;
                }
              });
    }

    // Start all threads
    for (final Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      thread.join();
    }

    bh.consume(results);
  }

  @Benchmark
  public void benchmarkExceptionHandlerLifecycle(final Blackhole bh) {
    // Benchmark complete lifecycle: create handler, create tag, throw exception, close handler
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(false)
            .enableExceptionUnwinding(false)
            .validateExceptionTypes(false)
            .enableStackTraces(false)
            .build();

    try (final ExceptionHandler tempHandler = new ExceptionHandler(config)) {
      final ExceptionHandler.ExceptionTag tag =
          tempHandler.createExceptionTag("lifecycle_test", List.of(WasmValueType.I32));

      try {
        tempHandler.throwException(tag, List.of(WasmValue.i32(123)));
      } catch (final ExceptionHandler.WasmException e) {
        bh.consume(e);
      }
    }
  }

  /**
   * Creates parameter types list of the specified size.
   *
   * @param size the number of parameters
   * @return list of parameter types
   */
  private List<WasmValueType> createParameterTypes(final int size) {
    final WasmValueType[] types = {
      WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
    };
    final WasmValueType[] result = new WasmValueType[size];

    for (int i = 0; i < size; i++) {
      result[i] = types[i % types.length];
    }

    return Arrays.asList(result);
  }
}
