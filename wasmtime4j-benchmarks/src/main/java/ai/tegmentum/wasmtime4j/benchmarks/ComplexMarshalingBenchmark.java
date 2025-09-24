package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.ComplexMarshalingService;
import ai.tegmentum.wasmtime4j.MarshalingConfiguration;
import ai.tegmentum.wasmtime4j.WasmComplexValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.ComplexJniTypeConverter;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.util.ComplexPanamaTypeConverter;
import java.io.Serializable;
import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
 * JMH benchmarks for complex parameter marshaling performance.
 *
 * <p>This benchmark suite measures the performance characteristics of the complex marshaling system
 * across different data types, sizes, and strategies. It provides comprehensive metrics for
 * optimization and regression detection.
 *
 * <p>Benchmark categories:
 *
 * <ul>
 *   <li>Multi-dimensional array marshaling performance
 *   <li>Collection marshaling overhead
 *   <li>Custom POJO serialization efficiency
 *   <li>Memory-based vs value-based marshaling comparison
 *   <li>JNI vs Panama runtime performance
 *   <li>Strategy selection overhead
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Benchmark)
public class ComplexMarshalingBenchmark {

  @Param({"100", "1000", "10000"})
  private int arraySize;

  @Param({"50", "500", "5000"})
  private int collectionSize;

  @Param({"default", "performance", "safety"})
  private String configurationPreset;

  private ComplexMarshalingService marshalingService;
  private ComplexJniTypeConverter jniConverter;
  private ComplexPanamaTypeConverter panamaConverter;
  private ArenaResourceManager arenaManager;

  // Test data structures
  private int[][] testArray2D;
  private int[][][] testArray3D;
  private List<String> testList;
  private Map<String, Integer> testMap;
  private TestComplexObject testObject;
  private byte[] testBinaryData;

  @Setup(Level.Trial)
  public void setupTrial() throws Exception {
    // Setup configurations
    final MarshalingConfiguration config =
        switch (configurationPreset) {
          case "performance" -> MarshalingConfiguration.performanceOptimized();
          case "safety" -> MarshalingConfiguration.safetyOptimized();
          default -> MarshalingConfiguration.defaultConfiguration();
        };

    // Initialize services
    marshalingService = new ComplexMarshalingService(config);
    jniConverter = new ComplexJniTypeConverter(config);

    // Setup Panama resources
    arenaManager = new ArenaResourceManager(Arena.ofShared());
    panamaConverter = new ComplexPanamaTypeConverter(config, arenaManager);

    // Generate test data
    generateTestData();
  }

  @Setup(Level.Iteration)
  public void setupIteration() {
    // Reset any iteration-level state if needed
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() throws Exception {
    if (arenaManager != null) {
      arenaManager.close();
    }
  }

  /** Generates test data for benchmarks based on current parameters. */
  private void generateTestData() {
    final Random random = new Random(42); // Fixed seed for reproducibility

    // Generate 2D array
    testArray2D = new int[arraySize / 10][10];
    for (int i = 0; i < testArray2D.length; i++) {
      for (int j = 0; j < testArray2D[i].length; j++) {
        testArray2D[i][j] = random.nextInt(1000);
      }
    }

    // Generate 3D array (smaller for memory efficiency)
    final int size3D = Math.min(arraySize / 50, 20);
    testArray3D = new int[size3D][size3D][size3D];
    for (int i = 0; i < testArray3D.length; i++) {
      for (int j = 0; j < testArray3D[i].length; j++) {
        for (int k = 0; k < testArray3D[i][j].length; k++) {
          testArray3D[i][j][k] = random.nextInt(1000);
        }
      }
    }

    // Generate list
    testList = new ArrayList<>(collectionSize);
    for (int i = 0; i < collectionSize; i++) {
      testList.add("Item_" + i + "_" + random.nextInt(1000));
    }

    // Generate map
    testMap = new HashMap<>(collectionSize);
    for (int i = 0; i < collectionSize; i++) {
      testMap.put("Key_" + i, random.nextInt(10000));
    }

    // Generate complex object
    testObject = new TestComplexObject();
    testObject.setId(random.nextLong());
    testObject.setName("TestObject_" + random.nextInt(1000));
    testObject.setValues(new ArrayList<>());
    for (int i = 0; i < Math.min(collectionSize, 100); i++) {
      testObject.getValues().add(random.nextDouble());
    }
    testObject.setMetadata(new HashMap<>());
    for (int i = 0; i < 10; i++) {
      testObject.getMetadata().put("meta_" + i, "value_" + random.nextInt(100));
    }

    // Generate binary data
    testBinaryData = new byte[Math.min(arraySize * 4, 100000)];
    random.nextBytes(testBinaryData);
  }

  // Multi-dimensional Array Benchmarks

  @Benchmark
  public void marshal2DArrayBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testArray2D);
    bh.consume(result);
  }

  @Benchmark
  public void unmarshal2DArrayBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData =
        marshalingService.marshal(testArray2D);
    final int[][] result = marshalingService.unmarshal(marshaledData, int[][].class);
    bh.consume(result);
  }

  @Benchmark
  public void marshal3DArrayBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testArray3D);
    bh.consume(result);
  }

  @Benchmark
  public void marshal2DArrayJni(final Blackhole bh) throws WasmException {
    final Object result = jniConverter.convertComplexObjectToWasm(testArray2D);
    bh.consume(result);
  }

  @Benchmark
  public void marshal2DArrayPanama(final Blackhole bh) throws WasmException {
    final Object result = panamaConverter.convertComplexObjectToPanamaMemory(testArray2D);
    bh.consume(result);
  }

  // Collection Benchmarks

  @Benchmark
  public void marshalListBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testList);
    bh.consume(result);
  }

  @Benchmark
  public void unmarshalListBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData =
        marshalingService.marshal(testList);
    final List<?> result = marshalingService.unmarshal(marshaledData, List.class);
    bh.consume(result);
  }

  @Benchmark
  public void marshalMapBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testMap);
    bh.consume(result);
  }

  @Benchmark
  public void unmarshalMapBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData = marshalingService.marshal(testMap);
    final Map<?, ?> result = marshalingService.unmarshal(marshaledData, Map.class);
    bh.consume(result);
  }

  @Benchmark
  public void marshalListJni(final Blackhole bh) throws WasmException {
    final Object result = jniConverter.convertComplexObjectToWasm(testList);
    bh.consume(result);
  }

  @Benchmark
  public void marshalListPanama(final Blackhole bh) throws WasmException {
    final Object result = panamaConverter.convertComplexObjectToPanamaMemory(testList);
    bh.consume(result);
  }

  // Custom Object Benchmarks

  @Benchmark
  public void marshalComplexObjectBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testObject);
    bh.consume(result);
  }

  @Benchmark
  public void unmarshalComplexObjectBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData =
        marshalingService.marshal(testObject);
    final TestComplexObject result =
        marshalingService.unmarshal(marshaledData, TestComplexObject.class);
    bh.consume(result);
  }

  @Benchmark
  public void marshalComplexObjectJni(final Blackhole bh) throws WasmException {
    final Object result = jniConverter.convertComplexObjectToWasm(testObject);
    bh.consume(result);
  }

  @Benchmark
  public void marshalComplexObjectPanama(final Blackhole bh) throws WasmException {
    final Object result = panamaConverter.convertComplexObjectToPanamaMemory(testObject);
    bh.consume(result);
  }

  // Binary Data Benchmarks

  @Benchmark
  public void marshalBinaryDataBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testBinaryData);
    bh.consume(result);
  }

  @Benchmark
  public void unmarshalBinaryDataBaseline(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData =
        marshalingService.marshal(testBinaryData);
    final byte[] result = marshalingService.unmarshal(marshaledData, byte[].class);
    bh.consume(result);
  }

  // Strategy Selection Benchmarks

  @Benchmark
  public void estimateObjectSize(final Blackhole bh) {
    final long result = marshalingService.estimateSerializedSize(testObject);
    bh.consume(result);
  }

  @Benchmark
  public void createComplexValue(final Blackhole bh) throws WasmException {
    final WasmComplexValue result = marshalingService.createComplexValue(testList);
    bh.consume(result);
  }

  // Round-trip Benchmarks

  @Benchmark
  public void roundTripArray2D(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData =
        marshalingService.marshal(testArray2D);
    final int[][] result = marshalingService.unmarshal(marshaledData, int[][].class);
    bh.consume(result);
  }

  @Benchmark
  public void roundTripList(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData =
        marshalingService.marshal(testList);
    final List<?> result = marshalingService.unmarshal(marshaledData, List.class);
    bh.consume(result);
  }

  @Benchmark
  public void roundTripComplexObject(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData marshaledData =
        marshalingService.marshal(testObject);
    final TestComplexObject result =
        marshalingService.unmarshal(marshaledData, TestComplexObject.class);
    bh.consume(result);
  }

  // Memory Overhead Benchmarks

  @Benchmark
  public void memoryOverheadSmallObject(final Blackhole bh) throws WasmException {
    final String smallString = "Hello, World!";
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(smallString);
    bh.consume(result);
  }

  @Benchmark
  public void memoryOverheadLargeObject(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testBinaryData);
    bh.consume(result);
  }

  // Concurrent Access Benchmarks

  @Benchmark
  public void concurrentMarshalArray(final Blackhole bh) throws WasmException {
    // Test thread safety of marshaling service
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testArray2D);
    bh.consume(result);
  }

  @Benchmark
  public void concurrentMarshalList(final Blackhole bh) throws WasmException {
    // Test thread safety of marshaling service
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(testList);
    bh.consume(result);
  }

  // Configuration Comparison Benchmarks

  @Benchmark
  public void marshalWithDefaultConfig(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService defaultService =
        new ComplexMarshalingService(MarshalingConfiguration.defaultConfiguration());
    final ComplexMarshalingService.MarshaledData result = defaultService.marshal(testObject);
    bh.consume(result);
  }

  @Benchmark
  public void marshalWithPerformanceConfig(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService perfService =
        new ComplexMarshalingService(MarshalingConfiguration.performanceOptimized());
    final ComplexMarshalingService.MarshaledData result = perfService.marshal(testObject);
    bh.consume(result);
  }

  @Benchmark
  public void marshalWithSafetyConfig(final Blackhole bh) throws WasmException {
    final ComplexMarshalingService safetyService =
        new ComplexMarshalingService(MarshalingConfiguration.safetyOptimized());
    final ComplexMarshalingService.MarshaledData result = safetyService.marshal(testObject);
    bh.consume(result);
  }

  // Test data classes

  public static class TestComplexObject implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private List<Double> values;
    private Map<String, String> metadata;

    public TestComplexObject() {}

    public long getId() {
      return id;
    }

    public void setId(final long id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public List<Double> getValues() {
      return values;
    }

    public void setValues(final List<Double> values) {
      this.values = values;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }

    public void setMetadata(final Map<String, String> metadata) {
      this.metadata = metadata;
    }

    @Override
    public String toString() {
      return String.format(
          "TestComplexObject{id=%d, name='%s', values=%d items, metadata=%d items}",
          id, name, values != null ? values.size() : 0, metadata != null ? metadata.size() : 0);
    }
  }

  /** Warmup benchmark to ensure JIT compilation. */
  @Benchmark
  public void warmupBenchmark(final Blackhole bh) throws WasmException {
    final String warmupData = "warmup";
    final ComplexMarshalingService.MarshaledData result = marshalingService.marshal(warmupData);
    final String unmarshaled = marshalingService.unmarshal(result, String.class);
    bh.consume(unmarshaled);
  }
}
