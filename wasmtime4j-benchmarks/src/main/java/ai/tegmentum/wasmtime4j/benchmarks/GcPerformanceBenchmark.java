package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.gc.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH benchmarks for WebAssembly GC operations.
 *
 * <p>Measures performance of GC object creation, field/element access, type checking, reference
 * operations, and garbage collection across both JNI and Panama implementations.
 *
 * <p>Run with: {@code mvn exec:java
 * -Dexec.mainClass="ai.tegmentum.wasmtime4j.benchmarks.GcPerformanceBenchmark"}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class GcPerformanceBenchmark {

  private static final Logger LOGGER = Logger.getLogger(GcPerformanceBenchmark.class.getName());

  private WasmRuntime runtime;
  private GcRuntime gcRuntime;

  // Pre-created types for benchmarks
  private StructType pointStructType;
  private StructType complexStructType;
  private ArrayType intArrayType;
  private ArrayType structArrayType;

  // Pre-created objects for benchmarks
  private StructInstance pointStruct;
  private ArrayInstance intArray;
  private I31Instance[] i31Objects;
  private StructInstance[] structObjects;

  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    LOGGER.info("Setting up GC performance benchmark trial");

    runtime = WasmRuntimeFactory.createRuntime();

    if (runtime instanceof ai.tegmentum.wasmtime4j.jni.JniWasmRuntime) {
      gcRuntime = ((ai.tegmentum.wasmtime4j.jni.JniWasmRuntime) runtime).getGcRuntime();
      LOGGER.info("Using JNI GC runtime for benchmarks");
    } else if (runtime instanceof ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime) {
      gcRuntime = ((ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime) runtime).getGcRuntime();
      LOGGER.info("Using Panama GC runtime for benchmarks");
    } else {
      throw new RuntimeException("Unsupported runtime type: " + runtime.getClass());
    }

    setupTypes();
    setupObjects();
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() throws Exception {
    LOGGER.info("Tearing down GC performance benchmark trial");

    if (runtime != null) {
      try {
        runtime.dispose();
      } catch (Exception e) {
        LOGGER.warning("Failed to dispose runtime: " + e.getMessage());
      }
    }
  }

  private void setupTypes() {
    // Point struct type
    pointStructType =
        StructType.builder("Point")
            .addField("x", FieldType.i32(), true)
            .addField("y", FieldType.i32(), true)
            .build();
    gcRuntime.registerStructType(pointStructType);

    // Complex struct type
    complexStructType =
        StructType.builder("ComplexStruct")
            .addField("id", FieldType.i64(), false)
            .addField("value", FieldType.f64(), true)
            .addField("flags", FieldType.i32(), true)
            .addField("data", FieldType.reference(GcReferenceType.ARRAY_REF), true)
            .build();
    gcRuntime.registerStructType(complexStructType);

    // Integer array type
    intArrayType = ArrayType.builder("IntArray").elementType(FieldType.i32()).mutable(true).build();
    gcRuntime.registerArrayType(intArrayType);

    // Struct array type
    structArrayType =
        ArrayType.builder("StructArray")
            .elementType(FieldType.reference(GcReferenceType.STRUCT_REF))
            .mutable(true)
            .build();
    gcRuntime.registerArrayType(structArrayType);
  }

  private void setupObjects() {
    // Create point struct
    pointStruct =
        gcRuntime.createStruct(pointStructType, Arrays.asList(GcValue.i32(100), GcValue.i32(200)));

    // Create integer array
    intArray = gcRuntime.createArray(intArrayType, 1000);

    // Create I31 objects
    i31Objects = new I31Instance[100];
    for (int i = 0; i < i31Objects.length; i++) {
      i31Objects[i] = gcRuntime.createI31(i * 1000);
    }

    // Create struct objects
    structObjects = new StructInstance[100];
    for (int i = 0; i < structObjects.length; i++) {
      structObjects[i] =
          gcRuntime.createStruct(
              pointStructType, Arrays.asList(GcValue.i32(i), GcValue.i32(i * 2)));
    }
  }

  // I31 Creation Benchmarks

  @Benchmark
  public I31Instance benchmarkI31Creation(Blackhole bh) {
    I31Instance i31 = gcRuntime.createI31(42);
    bh.consume(i31);
    return i31;
  }

  @Benchmark
  public void benchmarkI31CreationBatch(Blackhole bh) {
    for (int i = 0; i < 100; i++) {
      I31Instance i31 = gcRuntime.createI31(i);
      bh.consume(i31);
    }
  }

  @Benchmark
  public int benchmarkI31ValueAccess() {
    return i31Objects[0].getValue();
  }

  // Struct Creation Benchmarks

  @Benchmark
  public StructInstance benchmarkStructCreation(Blackhole bh) {
    List<GcValue> values = Arrays.asList(GcValue.i32(10), GcValue.i32(20));
    StructInstance struct = gcRuntime.createStruct(pointStructType, values);
    bh.consume(struct);
    return struct;
  }

  @Benchmark
  public StructInstance benchmarkStructCreationDefault(Blackhole bh) {
    StructInstance struct = gcRuntime.createStruct(pointStructType);
    bh.consume(struct);
    return struct;
  }

  @Benchmark
  public void benchmarkStructCreationBatch(Blackhole bh) {
    for (int i = 0; i < 50; i++) {
      List<GcValue> values = Arrays.asList(GcValue.i32(i), GcValue.i32(i * 2));
      StructInstance struct = gcRuntime.createStruct(pointStructType, values);
      bh.consume(struct);
    }
  }

  // Struct Field Access Benchmarks

  @Benchmark
  public GcValue benchmarkStructFieldGet() {
    return gcRuntime.getStructField(pointStruct, 0);
  }

  @Benchmark
  public void benchmarkStructFieldSet(Blackhole bh) {
    gcRuntime.setStructField(pointStruct, 0, GcValue.i32(999));
    bh.consume(pointStruct);
  }

  @Benchmark
  public void benchmarkStructFieldAccessBatch(Blackhole bh) {
    for (int i = 0; i < 50; i++) {
      GcValue value = gcRuntime.getStructField(structObjects[i % structObjects.length], 0);
      bh.consume(value);
    }
  }

  // Array Creation Benchmarks

  @Benchmark
  public ArrayInstance benchmarkArrayCreation(Blackhole bh) {
    List<GcValue> elements =
        Arrays.asList(
            GcValue.i32(1), GcValue.i32(2), GcValue.i32(3), GcValue.i32(4), GcValue.i32(5));
    ArrayInstance array = gcRuntime.createArray(intArrayType, elements);
    bh.consume(array);
    return array;
  }

  @Benchmark
  public ArrayInstance benchmarkArrayCreationDefault(Blackhole bh) {
    ArrayInstance array = gcRuntime.createArray(intArrayType, 100);
    bh.consume(array);
    return array;
  }

  @Benchmark
  public ArrayInstance benchmarkLargeArrayCreation(Blackhole bh) {
    ArrayInstance array = gcRuntime.createArray(intArrayType, 10000);
    bh.consume(array);
    return array;
  }

  // Array Element Access Benchmarks

  @Benchmark
  public GcValue benchmarkArrayElementGet() {
    return gcRuntime.getArrayElement(intArray, 500);
  }

  @Benchmark
  public void benchmarkArrayElementSet(Blackhole bh) {
    gcRuntime.setArrayElement(intArray, 500, GcValue.i32(999));
    bh.consume(intArray);
  }

  @Benchmark
  public int benchmarkArrayLength() {
    return gcRuntime.getArrayLength(intArray);
  }

  @Benchmark
  public void benchmarkArraySequentialAccess(Blackhole bh) {
    for (int i = 0; i < 100; i++) {
      GcValue value = gcRuntime.getArrayElement(intArray, i);
      bh.consume(value);
    }
  }

  @Benchmark
  public void benchmarkArrayRandomAccess(Blackhole bh) {
    for (int i = 0; i < 100; i++) {
      int index = (i * 7) % 1000; // Pseudo-random access pattern
      GcValue value = gcRuntime.getArrayElement(intArray, index);
      bh.consume(value);
    }
  }

  // Reference Operation Benchmarks

  @Benchmark
  public boolean benchmarkRefTest() {
    return gcRuntime.refTest(pointStruct, GcReferenceType.STRUCT_REF);
  }

  @Benchmark
  public boolean benchmarkRefEquals() {
    return gcRuntime.refEquals(i31Objects[0], i31Objects[1]);
  }

  @Benchmark
  public boolean benchmarkIsNull() {
    return gcRuntime.isNull(pointStruct);
  }

  @Benchmark
  public void benchmarkRefTestBatch(Blackhole bh) {
    for (int i = 0; i < 50; i++) {
      boolean result =
          gcRuntime.refTest(structObjects[i % structObjects.length], GcReferenceType.STRUCT_REF);
      bh.consume(result);
    }
  }

  // Reference Casting Benchmarks

  @Benchmark
  public GcObject benchmarkRefCastUpcast(Blackhole bh) {
    GcObject anyRef = gcRuntime.refCast(pointStruct, GcReferenceType.ANY_REF);
    bh.consume(anyRef);
    return anyRef;
  }

  @Benchmark
  public void benchmarkRefCastBatch(Blackhole bh) {
    for (int i = 0; i < 25; i++) {
      GcObject anyRef =
          gcRuntime.refCast(i31Objects[i % i31Objects.length], GcReferenceType.ANY_REF);
      bh.consume(anyRef);
    }
  }

  // Mixed Workload Benchmarks

  @Benchmark
  public void benchmarkMixedWorkloadSmall(Blackhole bh) {
    // Simulate typical mixed GC operations
    I31Instance i31 = gcRuntime.createI31(123);
    bh.consume(i31);

    StructInstance struct = gcRuntime.createStruct(pointStructType);
    bh.consume(struct);

    GcValue field = gcRuntime.getStructField(struct, 0);
    bh.consume(field);

    gcRuntime.setStructField(struct, 1, GcValue.i32(456));

    ArrayInstance array = gcRuntime.createArray(intArrayType, 10);
    bh.consume(array);

    boolean isStruct = gcRuntime.refTest(struct, GcReferenceType.STRUCT_REF);
    bh.consume(isStruct);
  }

  @Benchmark
  public void benchmarkMixedWorkloadLarge(Blackhole bh) {
    // Larger mixed workload simulation
    for (int i = 0; i < 20; i++) {
      I31Instance i31 = gcRuntime.createI31(i);
      bh.consume(i31);

      StructInstance struct =
          gcRuntime.createStruct(
              pointStructType, Arrays.asList(GcValue.i32(i), GcValue.i32(i * 2)));
      bh.consume(struct);

      GcValue x = gcRuntime.getStructField(struct, 0);
      GcValue y = gcRuntime.getStructField(struct, 1);
      bh.consume(x);
      bh.consume(y);

      if (i % 5 == 0) {
        ArrayInstance array = gcRuntime.createArray(intArrayType, i + 1);
        bh.consume(array);

        for (int j = 0; j < i + 1; j++) {
          gcRuntime.setArrayElement(array, j, GcValue.i32(j));
        }
      }

      boolean typeCheck = gcRuntime.refTest(struct, GcReferenceType.EQ_REF);
      bh.consume(typeCheck);
    }
  }

  // Garbage Collection Benchmarks

  @Benchmark
  public GcStats benchmarkCollectGarbage() {
    return gcRuntime.collectGarbage();
  }

  @Benchmark
  public GcStats benchmarkGetGcStats() {
    return gcRuntime.getGcStats();
  }

  @Benchmark
  public void benchmarkAllocationPressure(Blackhole bh) {
    // Create allocation pressure to trigger GC
    for (int i = 0; i < 1000; i++) {
      I31Instance i31 = gcRuntime.createI31(i);
      bh.consume(i31);

      if (i % 100 == 0) {
        ArrayInstance array = gcRuntime.createArray(intArrayType, 50);
        bh.consume(array);
      }

      if (i % 50 == 0) {
        StructInstance struct = gcRuntime.createStruct(pointStructType);
        bh.consume(struct);
      }
    }
  }

  // Memory Usage Benchmarks

  @Benchmark
  public void benchmarkMemoryIntensiveWorkload(Blackhole bh) {
    // Create objects with varying sizes to test memory management
    ArrayInstance smallArray = gcRuntime.createArray(intArrayType, 10);
    ArrayInstance mediumArray = gcRuntime.createArray(intArrayType, 100);
    ArrayInstance largeArray = gcRuntime.createArray(intArrayType, 1000);
    bh.consume(smallArray);
    bh.consume(mediumArray);
    bh.consume(largeArray);

    // Create many small objects
    for (int i = 0; i < 500; i++) {
      I31Instance i31 = gcRuntime.createI31(i);
      bh.consume(i31);
    }

    // Create complex nested structures
    for (int i = 0; i < 10; i++) {
      ArrayInstance dataArray = gcRuntime.createArray(intArrayType, 20);
      StructInstance complexStruct =
          gcRuntime.createStruct(
              complexStructType,
              Arrays.asList(
                  GcValue.i64(i),
                  GcValue.f64(i * 3.14),
                  GcValue.i32(i % 256),
                  GcValue.reference(dataArray)));
      bh.consume(complexStruct);
    }
  }

  // Performance Regression Detection

  /**
   * Baseline performance test for I31 operations. This serves as a reference for performance
   * regression detection.
   */
  @Benchmark
  public void benchmarkI31BaselinePerformance(Blackhole bh) {
    for (int i = 0; i < 1000; i++) {
      I31Instance i31 = gcRuntime.createI31(i % 1000000);
      int value = i31.getValue();
      bh.consume(value);
    }
  }

  /**
   * Baseline performance test for struct operations. This serves as a reference for performance
   * regression detection.
   */
  @Benchmark
  public void benchmarkStructBaselinePerformance(Blackhole bh) {
    for (int i = 0; i < 100; i++) {
      StructInstance struct =
          gcRuntime.createStruct(
              pointStructType, Arrays.asList(GcValue.i32(i), GcValue.i32(i * 2)));

      GcValue x = gcRuntime.getStructField(struct, 0);
      GcValue y = gcRuntime.getStructField(struct, 1);

      gcRuntime.setStructField(struct, 0, GcValue.i32(x.asI32() + 1));
      gcRuntime.setStructField(struct, 1, GcValue.i32(y.asI32() + 1));

      bh.consume(struct);
    }
  }

  /**
   * Baseline performance test for array operations. This serves as a reference for performance
   * regression detection.
   */
  @Benchmark
  public void benchmarkArrayBaselinePerformance(Blackhole bh) {
    for (int i = 0; i < 50; i++) {
      ArrayInstance array = gcRuntime.createArray(intArrayType, 100);

      // Fill array
      for (int j = 0; j < 100; j++) {
        gcRuntime.setArrayElement(array, j, GcValue.i32(j));
      }

      // Sum array elements
      int sum = 0;
      for (int j = 0; j < 100; j++) {
        GcValue value = gcRuntime.getArrayElement(array, j);
        sum += value.asI32();
      }

      bh.consume(sum);
    }
  }

  public static void main(String[] args) throws RunnerException {
    Options opt =
        new OptionsBuilder()
            .include(GcPerformanceBenchmark.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC")
            .build();

    new Runner(opt).run();
  }
}
