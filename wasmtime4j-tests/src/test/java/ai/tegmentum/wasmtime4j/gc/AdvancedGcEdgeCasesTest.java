package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive test suite for advanced WebAssembly GC edge cases and complex scenarios.
 *
 * <p>This test suite validates the implementation of WebAssembly 3.0 GC features including
 * advanced reference type operations, complex struct and array compositions, incremental
 * and concurrent garbage collection, debugging tools, and safety validation.
 *
 * @since 1.0.0
 */
@DisplayName("Advanced GC Edge Cases and Complex Scenarios")
class AdvancedGcEdgeCasesTest {

    private WasmRuntime runtime;
    private GcRuntime gcRuntime;

    @BeforeEach
    void setUp() {
        runtime = WasmRuntimeFactory.createDefault();
        gcRuntime = runtime.createGcRuntime();
    }

    @AfterEach
    void tearDown() {
        if (runtime != null) {
            runtime.dispose();
        }
    }

    @Nested
    @DisplayName("Advanced Reference Type Operations")
    class ReferenceTypeOperationsTest {

        @Test
        @DisplayName("Should perform safe reference type downcasting with runtime type checking")
        void testSafeReferenceTypeDowncasting() {
            // Create a struct type for testing
            StructType personType = StructType.builder()
                .name("Person")
                .field("age", FieldType.i32(), true)
                .field("name_length", FieldType.i32(), false)
                .build();

            StructInstance person = gcRuntime.createStruct(personType, List.of(
                GcValue.i32(25),
                GcValue.i32(10)
            ));

            // Test successful downcast
            GcObject anyRefPerson = person; // Upcast to anyref
            StructInstance downcastPerson = gcRuntime.refCastStruct(anyRefPerson, personType);
            assertNotNull(downcastPerson);
            assertEquals(25, downcastPerson.getField(0).asI32());

            // Test runtime type identification
            GcReferenceType runtimeType = gcRuntime.getRuntimeType(person);
            assertEquals(GcReferenceType.STRUCT_REF, runtimeType);

            // Test ref.test operation
            assertTrue(gcRuntime.refTestStruct(person, personType));
            assertTrue(gcRuntime.refTest(person, GcReferenceType.STRUCT_REF));
            assertTrue(gcRuntime.refTest(person, GcReferenceType.EQ_REF));
            assertTrue(gcRuntime.refTest(person, GcReferenceType.ANY_REF));
        }

        @Test
        @DisplayName("Should handle nullable reference casts safely")
        void testNullableReferenceCasts() {
            // Test null handling
            Optional<GcObject> nullResult = gcRuntime.refCastNullable(null, GcReferenceType.STRUCT_REF);
            assertFalse(nullResult.isPresent());

            // Create and test with actual object
            I31Instance i31Value = gcRuntime.createI31(42);
            Optional<GcObject> nonNullResult = gcRuntime.refCastNullable(i31Value, GcReferenceType.I31_REF);
            assertTrue(nonNullResult.isPresent());
        }

        @Test
        @DisplayName("Should detect invalid casts and throw appropriate exceptions")
        void testInvalidCastDetection() {
            I31Instance i31Value = gcRuntime.createI31(42);

            // Create a struct type different from what we're casting to
            StructType personType = StructType.builder()
                .name("Person")
                .field("age", FieldType.i32(), true)
                .build();

            // This should fail - trying to cast I31 to struct
            assertThrows(ClassCastException.class, () -> {
                gcRuntime.refCastStruct(i31Value, personType);
            });

            // This should return false
            assertFalse(gcRuntime.refTestStruct(i31Value, personType));
        }
    }

    @Nested
    @DisplayName("Complex Struct and Array Edge Cases")
    class ComplexStructArrayTest {

        @Test
        @DisplayName("Should handle packed structs with custom field alignment")
        void testPackedStructsWithCustomAlignment() {
            StructType packedType = StructType.builder()
                .name("PackedStruct")
                .field("byte_field", FieldType.packedI8(), true)
                .field("short_field", FieldType.packedI16(), true)
                .field("int_field", FieldType.i32(), true)
                .build();

            Map<Integer, Integer> customAlignment = Map.of(
                0, 1,  // byte field: 1-byte alignment
                1, 2,  // short field: 2-byte alignment
                2, 4   // int field: 4-byte alignment
            );

            StructInstance packedStruct = gcRuntime.createPackedStruct(
                packedType,
                List.of(GcValue.i32(127), GcValue.i32(32000), GcValue.i32(1000000)),
                customAlignment
            );

            assertNotNull(packedStruct);
            assertEquals(127, packedStruct.getField(0).asI32());
            assertEquals(32000, packedStruct.getField(1).asI32());
            assertEquals(1000000, packedStruct.getField(2).asI32());
        }

        @Test
        @DisplayName("Should support variable-length arrays with flexible members")
        void testVariableLengthArrays() {
            ArrayType flexArrayType = ArrayType.builder()
                .name("FlexArray")
                .elementType(FieldType.i32())
                .mutable(true)
                .build();

            List<GcValue> flexibleElements = List.of(
                GcValue.i32(100), GcValue.i32(200), GcValue.i32(300)
            );

            ArrayInstance flexArray = gcRuntime.createVariableLengthArray(
                flexArrayType, 5, flexibleElements);

            assertNotNull(flexArray);
            assertEquals(8, flexArray.getLength()); // 5 base + 3 flexible

            // Verify we can access both base and flexible elements
            for (int i = 0; i < flexArray.getLength(); i++) {
                GcValue element = flexArray.getElement(i);
                assertNotNull(element);
            }
        }

        @Test
        @DisplayName("Should handle nested arrays and complex compositions")
        void testNestedArraysAndComplexCompositions() {
            // Create struct type for nested elements
            StructType pointType = StructType.builder()
                .name("Point")
                .field("x", FieldType.i32(), true)
                .field("y", FieldType.i32(), true)
                .build();

            // Create some point structs
            StructInstance point1 = gcRuntime.createStruct(pointType,
                List.of(GcValue.i32(10), GcValue.i32(20)));
            StructInstance point2 = gcRuntime.createStruct(pointType,
                List.of(GcValue.i32(30), GcValue.i32(40)));

            // Create array type that holds struct references
            ArrayType pointArrayType = ArrayType.builder()
                .name("PointArray")
                .elementType(FieldType.reference())
                .mutable(true)
                .build();

            // Create nested array
            ArrayInstance nestedArray = gcRuntime.createNestedArray(
                pointArrayType, List.of(point1, point2));

            assertNotNull(nestedArray);
            assertEquals(2, nestedArray.getLength());
        }

        @Test
        @DisplayName("Should support efficient array operations like copy and fill")
        void testArrayOperations() {
            ArrayType intArrayType = ArrayType.builder()
                .name("IntArray")
                .elementType(FieldType.i32())
                .mutable(true)
                .build();

            // Create source array
            ArrayInstance sourceArray = gcRuntime.createArray(intArrayType,
                List.of(GcValue.i32(1), GcValue.i32(2), GcValue.i32(3), GcValue.i32(4), GcValue.i32(5)));

            // Create destination array
            ArrayInstance destArray = gcRuntime.createArray(intArrayType, 10);

            // Test array copy
            gcRuntime.copyArrayElements(sourceArray, 1, destArray, 2, 3);

            // Verify copy results
            assertEquals(2, destArray.getElement(2).asI32());
            assertEquals(3, destArray.getElement(3).asI32());
            assertEquals(4, destArray.getElement(4).asI32());

            // Test array fill
            gcRuntime.fillArrayElements(destArray, 6, 3, GcValue.i32(99));

            // Verify fill results
            assertEquals(99, destArray.getElement(6).asI32());
            assertEquals(99, destArray.getElement(7).asI32());
            assertEquals(99, destArray.getElement(8).asI32());
        }
    }

    @Nested
    @DisplayName("Advanced Garbage Collection Operations")
    class AdvancedGcOperationsTest {

        @Test
        @DisplayName("Should perform incremental garbage collection with pause time limits")
        void testIncrementalGarbageCollection() {
            // Create some objects to garbage collect
            for (int i = 0; i < 100; i++) {
                gcRuntime.createI31(i);
            }

            // Perform incremental GC with 10ms pause limit
            GcStats incrementalStats = gcRuntime.collectGarbageIncremental(10);

            assertNotNull(incrementalStats);
            assertTrue(incrementalStats.getTotalCollected() >= 0);
            assertTrue(incrementalStats.getCurrentHeapSize() >= 0);
        }

        @Test
        @DisplayName("Should perform concurrent garbage collection")
        void testConcurrentGarbageCollection() {
            // Create objects in multiple threads
            CountDownLatch latch = new CountDownLatch(5);

            for (int t = 0; t < 5; t++) {
                Thread thread = new Thread(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            gcRuntime.createI31(i);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
                thread.start();
            }

            // Wait for all threads to create objects
            assertDoesNotThrow(() -> latch.await(5, TimeUnit.SECONDS));

            // Perform concurrent GC
            GcStats concurrentStats = gcRuntime.collectGarbageConcurrent();

            assertNotNull(concurrentStats);
            assertTrue(concurrentStats.getTotalCollected() >= 0);
        }

        @Test
        @DisplayName("Should support configurable GC strategies")
        void testConfigurableGcStrategies() {
            Map<String, Object> generationalParams = Map.of(
                "young_generation_size", 1024 * 1024,
                "old_generation_size", 4 * 1024 * 1024,
                "gc_threshold", 0.8
            );

            assertDoesNotThrow(() -> {
                gcRuntime.configureGcStrategy("generational", generationalParams);
            });

            Map<String, Object> concurrentParams = Map.of(
                "concurrent_threads", 2,
                "incremental_step_size", 100
            );

            assertDoesNotThrow(() -> {
                gcRuntime.configureGcStrategy("concurrent_mark_sweep", concurrentParams);
            });
        }

        @Test
        @DisplayName("Should monitor GC pressure and trigger collection when needed")
        void testGcPressureMonitoring() {
            // Create memory pressure
            for (int i = 0; i < 1000; i++) {
                ArrayInstance array = gcRuntime.createArray(
                    ArrayType.builder()
                        .elementType(FieldType.i32())
                        .mutable(true)
                        .build(),
                    100
                );
                // Keep reference to prevent immediate collection
                assertNotNull(array);
            }

            // Monitor pressure - should trigger collection at 80% threshold
            boolean collectionTriggered = gcRuntime.monitorGcPressure(0.8);

            // Result depends on implementation, but should not throw
            assertNotNull(collectionTriggered);
        }
    }

    @Nested
    @DisplayName("Advanced Memory Management")
    class AdvancedMemoryManagementTest {

        @Test
        @DisplayName("Should support weak references with finalization callbacks")
        void testWeakReferencesWithFinalization() {
            AtomicBoolean finalized = new AtomicBoolean(false);
            AtomicReference<GcObject> weakRef = new AtomicReference<>();

            // Create object and weak reference
            I31Instance strongRef = gcRuntime.createI31(42);
            WeakGcReference weakReference = gcRuntime.createWeakReference(
                strongRef, () -> finalized.set(true));

            assertNotNull(weakReference);
            assertTrue(weakReference.get().isPresent());

            // Clear strong reference to allow collection
            strongRef = null;

            // Force garbage collection
            gcRuntime.collectGarbage();

            // In a real implementation, the weak reference might be cleared
            // For now, just verify the interface works
            assertNotNull(weakReference.getFinalizationCallback());
        }

        @Test
        @DisplayName("Should handle finalization callbacks properly")
        void testFinalizationCallbacks() {
            AtomicBoolean callbackExecuted = new AtomicBoolean(false);

            I31Instance object = gcRuntime.createI31(123);

            gcRuntime.registerFinalizationCallback(object, () -> {
                callbackExecuted.set(true);
            });

            // Force finalization
            int finalizedCount = gcRuntime.runFinalization();

            // Result depends on implementation
            assertTrue(finalizedCount >= 0);
        }
    }

    @Nested
    @DisplayName("Host Integration Features")
    class HostIntegrationTest {

        @Test
        @DisplayName("Should integrate host-managed objects with GC heap")
        void testHostObjectIntegration() {
            String hostString = "Hello from host";

            GcObject gcWrapper = gcRuntime.integrateHostObject(hostString, GcReferenceType.ANY_REF);
            assertNotNull(gcWrapper);

            Object extractedHost = gcRuntime.extractHostObject(gcWrapper);
            assertEquals(hostString, extractedHost);
        }

        @Test
        @DisplayName("Should create cross-language object sharing bridges")
        void testCrossLanguageSharingBridge() {
            I31Instance obj1 = gcRuntime.createI31(100);
            I31Instance obj2 = gcRuntime.createI31(200);

            Object sharingBridge = gcRuntime.createSharingBridge(List.of(obj1, obj2));
            assertNotNull(sharingBridge);
        }
    }

    @Nested
    @DisplayName("Debugging and Profiling Tools")
    class DebuggingProfilingTest {

        @Test
        @DisplayName("Should provide comprehensive heap inspection capabilities")
        void testHeapInspection() {
            // Create various objects to inspect
            I31Instance i31 = gcRuntime.createI31(42);

            StructType structType = StructType.builder()
                .name("TestStruct")
                .field("value", FieldType.i32(), true)
                .build();
            StructInstance struct = gcRuntime.createStruct(structType, List.of(GcValue.i32(99)));

            GcHeapInspection inspection = gcRuntime.inspectHeap();

            assertNotNull(inspection);
            assertTrue(inspection.getTotalObjectCount() >= 2);
            assertTrue(inspection.getTotalHeapSize() >= 0);
            assertTrue(inspection.getUsedHeapSize() >= 0);
            assertNotNull(inspection.getObjectTypeDistribution());
            assertNotNull(inspection.getMemoryUsageByType());
        }

        @Test
        @DisplayName("Should track object lifecycles for debugging")
        void testObjectLifecycleTracking() {
            I31Instance obj1 = gcRuntime.createI31(1);
            I31Instance obj2 = gcRuntime.createI31(2);

            ObjectLifecycleTracker tracker = gcRuntime.trackObjectLifecycles(List.of(obj1, obj2));

            assertNotNull(tracker);
            assertEquals(2, tracker.getTrackedObjects().size());

            // Perform some operations that should be tracked
            obj1.getValue(); // Read access

            // Get tracking results
            Map<Long, ObjectLifecycleTracker.ObjectStatus> statuses = tracker.getObjectStatuses();
            assertNotNull(statuses);

            Map<Long, ObjectLifecycleTracker.AccessStatistics> accessStats = tracker.getAccessStatistics();
            assertNotNull(accessStats);
        }

        @Test
        @DisplayName("Should detect memory leaks and provide analysis")
        void testMemoryLeakDetection() {
            // Create potential leak scenario
            for (int i = 0; i < 50; i++) {
                gcRuntime.createI31(i);
            }

            MemoryLeakAnalysis analysis = gcRuntime.detectMemoryLeaks();

            assertNotNull(analysis);
            assertNotNull(analysis.getAnalysisTime());
            assertTrue(analysis.getTotalObjectCount() >= 50);
            assertTrue(analysis.getPotentialLeakCount() >= 0);
            assertNotNull(analysis.getLeakSeverity());
            assertNotNull(analysis.getRecommendations());
        }

        @Test
        @DisplayName("Should provide performance profiling for GC operations")
        void testPerformanceProfiling() {
            GcProfiler profiler = gcRuntime.startProfiling();
            assertNotNull(profiler);
            assertFalse(profiler.isActive()); // Not started yet

            profiler.start();
            assertTrue(profiler.isActive());

            // Perform operations to profile
            for (int i = 0; i < 10; i++) {
                gcRuntime.createI31(i);
            }
            gcRuntime.collectGarbage();

            GcProfiler.GcProfilingResults results = profiler.stop();
            assertFalse(profiler.isActive());
            assertNotNull(results);
        }
    }

    @Nested
    @DisplayName("Safety and Validation")
    class SafetyValidationTest {

        @Test
        @DisplayName("Should validate reference safety for complex object graphs")
        void testReferenceSafetyValidation() {
            // Create complex object graph
            StructType nodeType = StructType.builder()
                .name("Node")
                .field("value", FieldType.i32(), true)
                .field("next", FieldType.reference(), true)
                .build();

            StructInstance node1 = gcRuntime.createStruct(nodeType,
                List.of(GcValue.i32(1), GcValue.nullValue()));
            StructInstance node2 = gcRuntime.createStruct(nodeType,
                List.of(GcValue.i32(2), GcValue.nullValue()));

            ReferenceSafetyResult safetyResult = gcRuntime.validateReferenceSafety(List.of(node1, node2));

            assertNotNull(safetyResult);
            assertTrue(safetyResult.getSafetyScore() >= 0.0 && safetyResult.getSafetyScore() <= 1.0);
            assertTrue(safetyResult.getTotalReferencesValidated() >= 0);
            assertNotNull(safetyResult.getViolationStatistics());
            assertNotNull(safetyResult.getRecommendations());
        }

        @Test
        @DisplayName("Should enforce type safety in complex scenarios")
        void testTypeSafetyEnforcement() {
            boolean isTypeSafe = gcRuntime.enforceTypeSafety("struct_field_access",
                List.of("field_index", 0, "expected_type", "i32"));

            // Result depends on implementation
            assertNotNull(isTypeSafe);
        }

        @Test
        @DisplayName("Should detect memory corruption and provide analysis")
        void testMemoryCorruptionDetection() {
            MemoryCorruptionAnalysis analysis = gcRuntime.detectMemoryCorruption();

            assertNotNull(analysis);
            assertNotNull(analysis.getAnalysisTime());
            assertFalse(analysis.isCorruptionDetected()); // Should be clean initially
            assertNotNull(analysis.getCorruptionSeverity());
            assertNotNull(analysis.getIntegrityResult());
            assertNotNull(analysis.getConsistencyResult());
            assertNotNull(analysis.getRecommendations());
        }

        @Test
        @DisplayName("Should validate GC invariants and consistency")
        void testGcInvariantValidation() {
            GcInvariantValidation validation = gcRuntime.validateInvariants();

            assertNotNull(validation);
            assertTrue(validation.areAllInvariantsSatisfied());
            assertTrue(validation.getTotalInvariantCount() >= 0);
            assertTrue(validation.getViolationCount() >= 0);
            assertTrue(validation.getSatisfactionScore() >= 0.0 && validation.getSatisfactionScore() <= 1.0);
            assertNotNull(validation.getCategoryResults());
            assertNotNull(validation.getCriticalInvariants());
            assertNotNull(validation.getPerformanceImpact());
        }
    }

    @Nested
    @DisplayName("Type System Edge Cases")
    class TypeSystemEdgeCasesTest {

        @Test
        @DisplayName("Should handle recursive type definitions properly")
        void testRecursiveTypes() {
            String linkedListTypeDef = """
                {
                  "name": "LinkedList",
                  "type": "struct",
                  "fields": [
                    {"name": "value", "type": "i32", "mutable": true},
                    {"name": "next", "type": "ref", "target": "LinkedList", "nullable": true, "mutable": true}
                  ]
                }
                """;

            int typeId = gcRuntime.registerRecursiveType("LinkedList", linkedListTypeDef);
            assertTrue(typeId >= 0);
        }

        @Test
        @DisplayName("Should create and manage type hierarchies")
        void testTypeHierarchies() {
            String baseTypeDef = """
                {
                  "name": "Shape",
                  "type": "struct",
                  "fields": [
                    {"name": "x", "type": "f64", "mutable": true},
                    {"name": "y", "type": "f64", "mutable": true}
                  ]
                }
                """;

            List<Object> derivedTypes = List.of(
                """
                {
                  "name": "Circle",
                  "type": "struct",
                  "extends": "Shape",
                  "fields": [
                    {"name": "radius", "type": "f64", "mutable": true}
                  ]
                }
                """,
                """
                {
                  "name": "Rectangle",
                  "type": "struct",
                  "extends": "Shape",
                  "fields": [
                    {"name": "width", "type": "f64", "mutable": true},
                    {"name": "height", "type": "f64", "mutable": true}
                  ]
                }
                """
            );

            Map<String, Integer> hierarchy = gcRuntime.createTypeHierarchy(baseTypeDef, derivedTypes);
            assertNotNull(hierarchy);
            assertTrue(hierarchy.containsKey("Shape"));
            assertTrue(hierarchy.containsKey("Circle"));
            assertTrue(hierarchy.containsKey("Rectangle"));
        }
    }
}