package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.gc.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for advanced WebAssembly GC operations and edge cases.
 *
 * <p>This benchmark suite measures the performance of complex GC operations including
 * advanced reference type operations, struct and array manipulations, garbage collection
 * strategies, and debugging tools.
 *
 * @since 1.0.0
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.OPERATIONS_PER_SECOND)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class GcAdvancedOperationsBenchmark {

    private WasmRuntime runtime;
    private GcRuntime gcRuntime;
    private StructType personType;
    private ArrayType intArrayType;
    private StructInstance[] testStructs;
    private ArrayInstance[] testArrays;

    @Setup(Level.Trial)
    public void setupTrial() {
        runtime = WasmRuntimeFactory.createDefault();
        gcRuntime = runtime.createGcRuntime();

        // Create reusable types
        personType = StructType.builder()
            .name("Person")
            .field("age", FieldType.i32(), true)
            .field("height", FieldType.f32(), true)
            .field("name_length", FieldType.i32(), false)
            .build();

        intArrayType = ArrayType.builder()
            .name("IntArray")
            .elementType(FieldType.i32())
            .mutable(true)
            .build();

        // Pre-create test objects for benchmarks that need them
        testStructs = new StructInstance[1000];
        testArrays = new ArrayInstance[1000];

        for (int i = 0; i < 1000; i++) {
            testStructs[i] = gcRuntime.createStruct(personType, List.of(
                GcValue.i32(20 + (i % 60)),
                GcValue.f32(150.0f + (i % 50)),
                GcValue.i32(5 + (i % 20))
            ));

            testArrays[i] = gcRuntime.createArray(intArrayType,
                List.of(GcValue.i32(i), GcValue.i32(i * 2), GcValue.i32(i * 3)));
        }
    }

    @TearDown(Level.Trial)
    public void tearDownTrial() {
        if (runtime != null) {
            runtime.dispose();
        }
    }

    // ========== Basic Object Creation Benchmarks ==========

    @Benchmark
    public StructInstance benchmarkStructCreation(Blackhole bh) {
        StructInstance struct = gcRuntime.createStruct(personType, List.of(
            GcValue.i32(25),
            GcValue.f32(175.5f),
            GcValue.i32(10)
        ));
        bh.consume(struct);
        return struct;
    }

    @Benchmark
    public ArrayInstance benchmarkArrayCreation(Blackhole bh) {
        ArrayInstance array = gcRuntime.createArray(intArrayType,
            List.of(GcValue.i32(1), GcValue.i32(2), GcValue.i32(3), GcValue.i32(4), GcValue.i32(5)));
        bh.consume(array);
        return array;
    }

    @Benchmark
    public I31Instance benchmarkI31Creation(Blackhole bh) {
        I31Instance i31 = gcRuntime.createI31(42);
        bh.consume(i31);
        return i31;
    }

    // ========== Advanced Reference Type Operations Benchmarks ==========

    @Benchmark
    public boolean benchmarkReferenceTypeTest(Blackhole bh) {
        StructInstance struct = testStructs[0];
        boolean result = gcRuntime.refTest(struct, GcReferenceType.STRUCT_REF);
        bh.consume(result);
        return result;
    }

    @Benchmark
    public StructInstance benchmarkStructTypeCast(Blackhole bh) {
        GcObject anyRef = testStructs[0];
        StructInstance cast = gcRuntime.refCastStruct(anyRef, personType);
        bh.consume(cast);
        return cast;
    }

    @Benchmark
    public GcReferenceType benchmarkRuntimeTypeInspection(Blackhole bh) {
        GcReferenceType type = gcRuntime.getRuntimeType(testStructs[0]);
        bh.consume(type);
        return type;
    }

    @Benchmark
    public boolean benchmarkReferenceEquality(Blackhole bh) {
        boolean result = gcRuntime.refEquals(testStructs[0], testStructs[0]);
        bh.consume(result);
        return result;
    }

    // ========== Complex Type Operations Benchmarks ==========

    @Benchmark
    public StructInstance benchmarkPackedStructCreation(Blackhole bh) {
        StructType packedType = StructType.builder()
            .name("PackedStruct")
            .field("byte_field", FieldType.packedI8(), true)
            .field("int_field", FieldType.i32(), true)
            .build();

        Map<Integer, Integer> alignment = Map.of(0, 1, 1, 4);

        StructInstance packed = gcRuntime.createPackedStruct(
            packedType,
            List.of(GcValue.i32(127), GcValue.i32(1000)),
            alignment
        );
        bh.consume(packed);
        return packed;
    }

    @Benchmark
    public ArrayInstance benchmarkVariableLengthArrayCreation(Blackhole bh) {
        ArrayInstance varArray = gcRuntime.createVariableLengthArray(
            intArrayType, 3, List.of(GcValue.i32(100), GcValue.i32(200)));
        bh.consume(varArray);
        return varArray;
    }

    @Benchmark
    public void benchmarkArrayElementsCopy(Blackhole bh) {
        ArrayInstance source = testArrays[0];
        ArrayInstance dest = gcRuntime.createArray(intArrayType, 10);

        gcRuntime.copyArrayElements(source, 0, dest, 0, 3);
        bh.consume(dest);
    }

    @Benchmark
    public void benchmarkArrayElementsFill(Blackhole bh) {
        ArrayInstance array = gcRuntime.createArray(intArrayType, 10);
        gcRuntime.fillArrayElements(array, 0, 10, GcValue.i32(99));
        bh.consume(array);
    }

    // ========== Field and Element Access Benchmarks ==========

    @Benchmark
    public GcValue benchmarkStructFieldAccess(Blackhole bh) {
        GcValue value = testStructs[0].getField(0);
        bh.consume(value);
        return value;
    }

    @Benchmark
    public void benchmarkStructFieldUpdate(Blackhole bh) {
        testStructs[0].setField(0, GcValue.i32(30));
        bh.consume(testStructs[0]);
    }

    @Benchmark
    public GcValue benchmarkArrayElementAccess(Blackhole bh) {
        GcValue value = testArrays[0].getElement(0);
        bh.consume(value);
        return value;
    }

    @Benchmark
    public void benchmarkArrayElementUpdate(Blackhole bh) {
        testArrays[0].setElement(0, GcValue.i32(999));
        bh.consume(testArrays[0]);
    }

    // ========== Garbage Collection Benchmarks ==========

    @Benchmark
    public GcStats benchmarkBasicGarbageCollection(Blackhole bh) {
        GcStats stats = gcRuntime.collectGarbage();
        bh.consume(stats);
        return stats;
    }

    @Benchmark
    public GcStats benchmarkIncrementalGarbageCollection(Blackhole bh) {
        GcStats stats = gcRuntime.collectGarbageIncremental(10); // 10ms pause limit
        bh.consume(stats);
        return stats;
    }

    @Benchmark
    public GcStats benchmarkConcurrentGarbageCollection(Blackhole bh) {
        GcStats stats = gcRuntime.collectGarbageConcurrent();
        bh.consume(stats);
        return stats;
    }

    @Benchmark
    public boolean benchmarkGcPressureMonitoring(Blackhole bh) {
        boolean triggered = gcRuntime.monitorGcPressure(0.8);
        bh.consume(triggered);
        return triggered;
    }

    // ========== Type System Operations Benchmarks ==========

    @Benchmark
    public int benchmarkStructTypeRegistration(Blackhole bh) {
        StructType tempType = StructType.builder()
            .name("TempStruct" + System.nanoTime())
            .field("temp_field", FieldType.i32(), true)
            .build();

        int typeId = gcRuntime.registerStructType(tempType);
        bh.consume(typeId);
        return typeId;
    }

    @Benchmark
    public int benchmarkArrayTypeRegistration(Blackhole bh) {
        ArrayType tempType = ArrayType.builder()
            .name("TempArray" + System.nanoTime())
            .elementType(FieldType.f64())
            .mutable(true)
            .build();

        int typeId = gcRuntime.registerArrayType(tempType);
        bh.consume(typeId);
        return typeId;
    }

    // ========== Memory Management Benchmarks ==========

    @Benchmark
    public WeakGcReference benchmarkWeakReferenceCreation(Blackhole bh) {
        I31Instance obj = gcRuntime.createI31(123);
        WeakGcReference weakRef = gcRuntime.createWeakReference(obj, () -> {});
        bh.consume(weakRef);
        return weakRef;
    }

    @Benchmark
    public void benchmarkFinalizationCallbackRegistration(Blackhole bh) {
        I31Instance obj = gcRuntime.createI31(456);
        gcRuntime.registerFinalizationCallback(obj, () -> {});
        bh.consume(obj);
    }

    // ========== Host Integration Benchmarks ==========

    @Benchmark
    public GcObject benchmarkHostObjectIntegration(Blackhole bh) {
        String hostString = "Benchmark string " + System.nanoTime();
        GcObject wrapper = gcRuntime.integrateHostObject(hostString, GcReferenceType.ANY_REF);
        bh.consume(wrapper);
        return wrapper;
    }

    @Benchmark
    public Object benchmarkHostObjectExtraction(Blackhole bh) {
        String hostString = "Extract me";
        GcObject wrapper = gcRuntime.integrateHostObject(hostString, GcReferenceType.ANY_REF);
        Object extracted = gcRuntime.extractHostObject(wrapper);
        bh.consume(extracted);
        return extracted;
    }

    // ========== Debugging and Analysis Benchmarks ==========

    @Benchmark
    public GcHeapInspection benchmarkHeapInspection(Blackhole bh) {
        GcHeapInspection inspection = gcRuntime.inspectHeap();
        bh.consume(inspection);
        return inspection;
    }

    @Benchmark
    public MemoryLeakAnalysis benchmarkMemoryLeakDetection(Blackhole bh) {
        MemoryLeakAnalysis analysis = gcRuntime.detectMemoryLeaks();
        bh.consume(analysis);
        return analysis;
    }

    @Benchmark
    public ReferenceSafetyResult benchmarkReferenceSafetyValidation(Blackhole bh) {
        ReferenceSafetyResult result = gcRuntime.validateReferenceSafety(
            List.of(testStructs[0], testStructs[1]));
        bh.consume(result);
        return result;
    }

    @Benchmark
    public MemoryCorruptionAnalysis benchmarkMemoryCorruptionDetection(Blackhole bh) {
        MemoryCorruptionAnalysis analysis = gcRuntime.detectMemoryCorruption();
        bh.consume(analysis);
        return analysis;
    }

    @Benchmark
    public GcInvariantValidation benchmarkInvariantValidation(Blackhole bh) {
        GcInvariantValidation validation = gcRuntime.validateInvariants();
        bh.consume(validation);
        return validation;
    }

    // ========== Stress Test Benchmarks ==========

    @Benchmark
    public void benchmarkMixedOperationsStress(Blackhole bh) {
        // Create objects
        StructInstance struct = gcRuntime.createStruct(personType, List.of(
            GcValue.i32(25), GcValue.f32(170.0f), GcValue.i32(8)));
        ArrayInstance array = gcRuntime.createArray(intArrayType, 5);
        I31Instance i31 = gcRuntime.createI31(789);

        // Perform operations
        struct.getField(0);
        array.setElement(2, GcValue.i32(100));
        boolean isStruct = gcRuntime.refTest(struct, GcReferenceType.STRUCT_REF);

        // Type operations
        gcRuntime.getRuntimeType(i31);

        bh.consume(struct);
        bh.consume(array);
        bh.consume(i31);
        bh.consume(isStruct);
    }

    @Benchmark
    public void benchmarkObjectLifecycleStress(Blackhole bh) {
        // Create many short-lived objects
        for (int i = 0; i < 100; i++) {
            I31Instance temp = gcRuntime.createI31(i);
            bh.consume(temp);
        }

        // Trigger GC
        GcStats stats = gcRuntime.collectGarbage();
        bh.consume(stats);
    }

    // ========== Nested Benchmark Methods ==========

    /**
     * Nested benchmark class for testing different GC strategies.
     */
    @State(Scope.Benchmark)
    public static class GcStrategyBenchmark {

        private WasmRuntime runtime;
        private GcRuntime gcRuntime;

        @Setup(Level.Trial)
        public void setup() {
            runtime = WasmRuntimeFactory.createDefault();
            gcRuntime = runtime.createGcRuntime();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (runtime != null) {
                runtime.dispose();
            }
        }

        @Benchmark
        public void benchmarkGenerationalGcStrategy(Blackhole bh) {
            Map<String, Object> params = Map.of(
                "young_generation_size", 512 * 1024,
                "old_generation_size", 2 * 1024 * 1024
            );

            gcRuntime.configureGcStrategy("generational", params);

            // Create young objects
            for (int i = 0; i < 50; i++) {
                I31Instance obj = gcRuntime.createI31(i);
                bh.consume(obj);
            }

            GcStats stats = gcRuntime.collectGarbage();
            bh.consume(stats);
        }

        @Benchmark
        public void benchmarkConcurrentMarkSweepStrategy(Blackhole bh) {
            Map<String, Object> params = Map.of(
                "concurrent_threads", 2,
                "incremental_step_size", 50
            );

            gcRuntime.configureGcStrategy("concurrent_mark_sweep", params);

            // Create objects
            for (int i = 0; i < 50; i++) {
                I31Instance obj = gcRuntime.createI31(i);
                bh.consume(obj);
            }

            GcStats stats = gcRuntime.collectGarbageConcurrent();
            bh.consume(stats);
        }
    }
}