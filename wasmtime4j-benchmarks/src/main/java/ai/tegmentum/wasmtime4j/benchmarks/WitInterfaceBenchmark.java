/*
 * Copyright 2024 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.WitInterfaceLinker;
import ai.tegmentum.wasmtime4j.WitInterfaceParser;
import ai.tegmentum.wasmtime4j.WitPrimitiveType;
import ai.tegmentum.wasmtime4j.WitResourceManager;
import ai.tegmentum.wasmtime4j.WitType;
import ai.tegmentum.wasmtime4j.WitTypeValidator;
import ai.tegmentum.wasmtime4j.WitValueMarshaler;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for WIT interface operations.
 *
 * <p>These benchmarks measure the performance of core WIT interface operations including parsing,
 * validation, marshaling, and resource management.
 *
 * @since 1.0.0
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class WitInterfaceBenchmark {

  private WitInterfaceParser parser;
  private WitTypeValidator validator;
  private WitValueMarshaler marshaler;
  private WitResourceManager resourceManager;
  private WitInterfaceLinker linker;

  // Test data
  private String simpleInterfaceText;
  private String complexInterfaceText;
  private WitInterfaceDefinition testInterface;
  private WitType primitiveType;
  private WitType recordType;
  private WitType listType;
  private Map<String, Object> testRecord;

  @Setup(Level.Trial)
  public void setupTrial() {
    parser = new WitInterfaceParser();
    validator = new WitTypeValidator();
    marshaler = new WitValueMarshaler();
    resourceManager = new WitResourceManager();
    linker = new WitInterfaceLinker();

    // Initialize test data
    initializeTestData();
  }

  @TearDown(Level.Trial)
  public void teardownTrial() {
    if (resourceManager != null) {
      resourceManager.close();
    }
  }

  private void initializeTestData() {
    // Simple interface for basic parsing benchmarks
    simpleInterfaceText =
        """
        interface calculator {
            add: func(a: s32, b: s32) -> s32;
            subtract: func(a: s32, b: s32) -> s32;
            multiply: func(a: s32, b: s32) -> s32;
            divide: func(a: s32, b: s32) -> s32;
        }
        """;

    // Complex interface for comprehensive parsing benchmarks
    complexInterfaceText =
        """
        interface complex {
            type point = record {
                x: f64,
                y: f64,
                z: f64
            };

            type color = enum {
                red,
                green,
                blue,
                alpha
            };

            type permissions = flags {
                read,
                write,
                execute,
                admin
            };

            type result-data = result<point, string>;
            type optional-color = option<color>;
            type point-list = list<point>;

            create-point: func(x: f64, y: f64, z: f64) -> point;
            distance: func(p1: point, p2: point) -> f64;
            colorize: func(p: point, c: optional-color) -> result-data;
            batch-process: func(points: point-list, perms: permissions) -> list<result-data>;
        }
        """;

    try {
      testInterface = parser.parseInterface(simpleInterfaceText, "test");
    } catch (WasmException e) {
      throw new RuntimeException("Failed to parse test interface", e);
    }

    // Create test types
    primitiveType = WitType.primitive(WitPrimitiveType.S32);

    final Map<String, WitType> recordFields =
        Map.of(
            "name", WitType.primitive(WitPrimitiveType.STRING),
            "age", WitType.primitive(WitPrimitiveType.U32),
            "score", WitType.primitive(WitPrimitiveType.FLOAT64));

    recordType = WitType.record("Person", recordFields);
    listType = WitType.list(WitType.primitive(WitPrimitiveType.S32));

    testRecord = Map.of("name", "John Doe", "age", 30, "score", 95.5);
  }

  @Benchmark
  public void benchmarkSimpleInterfaceParsing(final Blackhole bh) throws WasmException {
    final WitInterfaceDefinition result = parser.parseInterface(simpleInterfaceText, "benchmark");
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkComplexInterfaceParsing(final Blackhole bh) throws WasmException {
    final WitInterfaceDefinition result = parser.parseInterface(complexInterfaceText, "benchmark");
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkPrimitiveTypeValidation(final Blackhole bh) throws WasmException {
    final WitTypeValidator.WitTypeValidationResult result = validator.validateType(primitiveType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkRecordTypeValidation(final Blackhole bh) throws WasmException {
    final WitTypeValidator.WitTypeValidationResult result = validator.validateType(recordType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkInterfaceValidation(final Blackhole bh) throws WasmException {
    final WitTypeValidator.WitInterfaceValidationResult result =
        validator.validateInterface(testInterface);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkPrimitiveValueMarshalingToWit(final Blackhole bh) throws WasmException {
    final Object result = marshaler.marshalToWit(42, primitiveType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkPrimitiveValueMarshalingToJava(final Blackhole bh) throws WasmException {
    final Object result = marshaler.marshalToJava(42, primitiveType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkRecordValueMarshalingToWit(final Blackhole bh) throws WasmException {
    final Object result = marshaler.marshalToWit(testRecord, recordType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkRecordValueMarshalingToJava(final Blackhole bh) throws WasmException {
    final WitValueMarshaler.WitRecord witRecord = new WitValueMarshaler.WitRecord(testRecord);
    final Object result = marshaler.marshalToJava(witRecord, recordType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkListValueMarshalingToWit(final Blackhole bh) throws WasmException {
    final List<Integer> testList = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    final Object result = marshaler.marshalToWit(testList, listType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkListValueMarshalingToJava(final Blackhole bh) throws WasmException {
    final List<Integer> testList = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    final WitValueMarshaler.WitList witList = new WitValueMarshaler.WitList(testList);
    final Object result = marshaler.marshalToJava(witList, listType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkResourceCreation(final Blackhole bh) throws WasmException {
    final String testResource = "test-resource-" + System.nanoTime();
    final int handle = resourceManager.createResource("string", testResource);
    bh.consume(handle);

    // Cleanup to avoid resource buildup during benchmark
    resourceManager.destroyResource(handle);
  }

  @Benchmark
  public void benchmarkResourceAccess(final Blackhole bh) throws WasmException {
    // Create a resource first
    final String testResource = "test-resource-access";
    final int handle = resourceManager.createResource("string", testResource);

    try {
      // Benchmark the access
      final WitResourceManager.ManagedResource result = resourceManager.getResource(handle);
      bh.consume(result);
    } finally {
      // Cleanup
      resourceManager.destroyResource(handle);
    }
  }

  @Benchmark
  public void benchmarkResourceReferenceCount(final Blackhole bh) throws WasmException {
    // Create a resource first
    final String testResource = "test-resource-refcount";
    final int handle = resourceManager.createResource("string", testResource);

    try {
      // Benchmark reference counting operations
      resourceManager.incrementRefCount(handle);
      bh.consume(handle);
      resourceManager.decrementRefCount(handle);
    } finally {
      // Cleanup
      resourceManager.destroyResource(handle);
    }
  }

  @Benchmark
  public void benchmarkTypeCompatibilityCheck(final Blackhole bh) {
    final WitType type1 = WitType.primitive(WitPrimitiveType.S32);
    final WitType type2 = WitType.primitive(WitPrimitiveType.S32);

    final boolean result = type1.isCompatibleWith(type2);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkComplexTypeCompatibilityCheck(final Blackhole bh) {
    final WitTypeValidator.WitTypeCompatibilityResult result =
        validator.validateTypeCompatibility(recordType, recordType);
    bh.consume(result);
  }

  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentResourceAccess(final Blackhole bh) throws WasmException {
    // Create a resource first
    final String testResource = "concurrent-test-" + Thread.currentThread().threadId();
    final int handle = resourceManager.createResource("string", testResource);

    try {
      // Concurrent access benchmark
      final WitResourceManager.ManagedResource result = resourceManager.getResource(handle);
      bh.consume(result);
    } finally {
      // Cleanup
      resourceManager.destroyResource(handle);
    }
  }

  @Benchmark
  public void benchmarkEnumTypeCreation(final Blackhole bh) {
    final List<String> enumValues = List.of("red", "green", "blue", "yellow", "purple");
    final WitType result = WitType.enumType("Color", enumValues);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkVariantTypeCreation(final Blackhole bh) {
    final Map<String, Optional<WitType>> variantCases =
        Map.of(
            "none", Optional.empty(),
            "some", Optional.of(WitType.primitive(WitPrimitiveType.S32)),
            "error", Optional.of(WitType.primitive(WitPrimitiveType.STRING)));

    final WitType result = WitType.variant("Result", variantCases);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkFlagsTypeCreation(final Blackhole bh) {
    final List<String> flags = List.of("read", "write", "execute", "admin", "guest");
    final WitType result = WitType.flags("Permissions", flags);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkOptionValueMarshaling(final Blackhole bh) throws WasmException {
    final WitType optionType = WitType.option(WitType.primitive(WitPrimitiveType.S32));
    final Optional<Integer> testValue = Optional.of(42);

    final Object result = marshaler.marshalToWit(testValue, optionType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkResultValueMarshaling(final Blackhole bh) throws WasmException {
    final WitType resultType =
        WitType.result(
            Optional.of(WitType.primitive(WitPrimitiveType.STRING)),
            Optional.of(WitType.primitive(WitPrimitiveType.S32)));

    final String successValue = "success";

    final Object result = marshaler.marshalToWit(successValue, resultType);
    bh.consume(result);
  }

  @Benchmark
  public void benchmarkNestedTypeCreation(final Blackhole bh) {
    // Create nested type: list<option<record{name: string, values: list<s32>}>>
    final Map<String, WitType> innerRecordFields =
        Map.of(
            "name", WitType.primitive(WitPrimitiveType.STRING),
            "values", WitType.list(WitType.primitive(WitPrimitiveType.S32)));

    final WitType innerRecord = WitType.record("DataSet", innerRecordFields);
    final WitType optionRecord = WitType.option(innerRecord);
    final WitType result = WitType.list(optionRecord);

    bh.consume(result);
  }

  /** Benchmark method that measures interface compatibility validation performance. */
  @Benchmark
  public void benchmarkInterfaceCompatibilityCheck(final Blackhole bh) throws WasmException {
    final WitInterfaceDefinition interface1 = parser.parseInterface(simpleInterfaceText, "test1");
    final WitInterfaceDefinition interface2 = parser.parseInterface(simpleInterfaceText, "test2");

    final WitCompatibilityResult result = interface1.isCompatibleWith(interface2);
    bh.consume(result);
  }

  /** Benchmark comprehensive type creation and validation pipeline. */
  @Benchmark
  public void benchmarkCompleteTypePipeline(final Blackhole bh) throws WasmException {
    // Create complex type
    final Map<String, WitType> fields =
        Map.of(
            "id", WitType.primitive(WitPrimitiveType.U64),
            "name", WitType.primitive(WitPrimitiveType.STRING),
            "tags", WitType.list(WitType.primitive(WitPrimitiveType.STRING)),
            "metadata", WitType.option(WitType.primitive(WitPrimitiveType.STRING)));

    final WitType complexType = WitType.record("ComplexRecord", fields);

    // Validate type
    final WitTypeValidator.WitTypeValidationResult validation = validator.validateType(complexType);

    // Create test data and marshal
    final Map<String, Object> testData =
        Map.of(
            "id",
            123L,
            "name",
            "test",
            "tags",
            List.of("tag1", "tag2"),
            "metadata",
            Optional.of("meta"));

    final Object marshaledValue = marshaler.marshalToWit(testData, complexType);

    bh.consume(validation);
    bh.consume(marshaledValue);
  }
}
