package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive tests for WebAssembly GC operations.
 *
 * <p>Tests all GC features including struct creation, array manipulation, reference type
 * conversions, and heap management across both JNI and Panama implementations.
 */
public class GcComprehensiveTest {

  private static final Logger LOGGER = Logger.getLogger(GcComprehensiveTest.class.getName());

  private WasmRuntime runtime;
  private GcRuntime gcRuntime;

  @BeforeEach
  void setUp(TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());

    // Use the factory to get the appropriate runtime implementation
    runtime = WasmRuntimeFactory.createRuntime();

    // Get the GC runtime - this will be JNI or Panama based on Java version
    if (runtime instanceof ai.tegmentum.wasmtime4j.jni.JniWasmRuntime) {
      gcRuntime = ((ai.tegmentum.wasmtime4j.jni.JniWasmRuntime) runtime).getGcRuntime();
    } else if (runtime instanceof ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime) {
      gcRuntime = ((ai.tegmentum.wasmtime4j.panama.PanamaWasmRuntime) runtime).getGcRuntime();
    } else {
      fail("Unsupported runtime type: " + runtime.getClass());
    }

    assertNotNull(gcRuntime, "GC runtime should be available");
  }

  @AfterEach
  void tearDown(TestInfo testInfo) throws Exception {
    LOGGER.info("Tearing down test: " + testInfo.getDisplayName());

    if (runtime != null) {
      try {
        runtime.dispose();
      } catch (Exception e) {
        LOGGER.warning("Failed to dispose runtime: " + e.getMessage());
      }
    }
  }

  @Test
  void testI31ReferenceOperations() {
    LOGGER.info("Testing I31 reference operations");

    // Test creating I31 references with various values
    I31Instance i31Zero = gcRuntime.createI31(0);
    I31Instance i31Positive = gcRuntime.createI31(42);
    I31Instance i31Negative = gcRuntime.createI31(-100);
    I31Instance i31Max = gcRuntime.createI31(Integer.MAX_VALUE >> 1); // 31-bit max
    I31Instance i31Min = gcRuntime.createI31(Integer.MIN_VALUE >> 1); // 31-bit min

    // Test value retrieval
    assertEquals(0, i31Zero.getValue());
    assertEquals(42, i31Positive.getValue());
    assertEquals(-100, i31Negative.getValue());

    // Test signed/unsigned value access
    assertEquals(42, i31Positive.getSignedValue());
    assertEquals(42, i31Positive.getUnsignedValue());
    assertEquals(-100, i31Negative.getSignedValue());
    assertTrue(i31Negative.getUnsignedValue() >= 0); // Unsigned should be positive

    // Test reference type checking
    assertTrue(gcRuntime.refTest(i31Positive, GcReferenceType.I31_REF));
    assertTrue(gcRuntime.refTest(i31Positive, GcReferenceType.EQ_REF));
    assertTrue(gcRuntime.refTest(i31Positive, GcReferenceType.ANY_REF));

    // Test reference equality
    assertTrue(gcRuntime.refEquals(i31Positive, i31Positive));
    assertFalse(gcRuntime.refEquals(i31Positive, i31Negative));
    assertFalse(gcRuntime.refEquals(i31Positive, null));
    assertTrue(gcRuntime.refEquals(null, null));

    // Test null checking
    assertFalse(gcRuntime.isNull(i31Positive));
    assertTrue(gcRuntime.isNull(null));

    LOGGER.info("I31 reference operations test completed successfully");
  }

  @Test
  void testStructOperations() {
    LOGGER.info("Testing struct operations");

    // Create a struct type
    StructType pointType =
        StructType.builder("Point")
            .addField("x", FieldType.i32(), true)
            .addField("y", FieldType.i32(), true)
            .addField("z", FieldType.f64(), false) // immutable field
            .build();

    // Register the struct type
    int typeId = gcRuntime.registerStructType(pointType);
    assertTrue(typeId > 0, "Type ID should be positive");

    // Create struct with initial values
    List<GcValue> initialValues =
        Arrays.asList(GcValue.i32(10), GcValue.i32(20), GcValue.f64(3.14));
    StructInstance point = gcRuntime.createStruct(pointType, initialValues);
    assertNotNull(point);

    // Test field access
    assertEquals(GcValue.Type.I32, gcRuntime.getStructField(point, 0).getType());
    assertEquals(10, gcRuntime.getStructField(point, 0).asI32());
    assertEquals(20, gcRuntime.getStructField(point, 1).asI32());
    assertEquals(3.14, gcRuntime.getStructField(point, 2).asF64(), 0.001);

    // Test field modification
    gcRuntime.setStructField(point, 0, GcValue.i32(100));
    gcRuntime.setStructField(point, 1, GcValue.i32(200));

    assertEquals(100, gcRuntime.getStructField(point, 0).asI32());
    assertEquals(200, gcRuntime.getStructField(point, 1).asI32());

    // Test struct with default values
    StructInstance defaultPoint = gcRuntime.createStruct(pointType);
    assertNotNull(defaultPoint);
    assertEquals(0, gcRuntime.getStructField(defaultPoint, 0).asI32());
    assertEquals(0, gcRuntime.getStructField(defaultPoint, 1).asI32());
    assertEquals(0.0, gcRuntime.getStructField(defaultPoint, 2).asF64());

    // Test reference type checking
    assertTrue(gcRuntime.refTest(point, GcReferenceType.STRUCT_REF));
    assertTrue(gcRuntime.refTest(point, GcReferenceType.EQ_REF));
    assertTrue(gcRuntime.refTest(point, GcReferenceType.ANY_REF));

    LOGGER.info("Struct operations test completed successfully");
  }

  @Test
  void testArrayOperations() {
    LOGGER.info("Testing array operations");

    // Create an array type
    ArrayType intArrayType =
        ArrayType.builder("IntArray").elementType(FieldType.i32()).mutable(true).build();

    // Register the array type
    int typeId = gcRuntime.registerArrayType(intArrayType);
    assertTrue(typeId > 0, "Type ID should be positive");

    // Create array with initial elements
    List<GcValue> initialElements =
        Arrays.asList(
            GcValue.i32(1), GcValue.i32(2), GcValue.i32(3), GcValue.i32(4), GcValue.i32(5));
    ArrayInstance intArray = gcRuntime.createArray(intArrayType, initialElements);
    assertNotNull(intArray);

    // Test array length
    assertEquals(5, gcRuntime.getArrayLength(intArray));

    // Test element access
    for (int i = 0; i < 5; i++) {
      GcValue element = gcRuntime.getArrayElement(intArray, i);
      assertEquals(GcValue.Type.I32, element.getType());
      assertEquals(i + 1, element.asI32());
    }

    // Test element modification
    gcRuntime.setArrayElement(intArray, 2, GcValue.i32(99));
    assertEquals(99, gcRuntime.getArrayElement(intArray, 2).asI32());

    // Test array with default values
    ArrayInstance defaultArray = gcRuntime.createArray(intArrayType, 10);
    assertNotNull(defaultArray);
    assertEquals(10, gcRuntime.getArrayLength(defaultArray));
    assertEquals(0, gcRuntime.getArrayElement(defaultArray, 0).asI32());
    assertEquals(0, gcRuntime.getArrayElement(defaultArray, 9).asI32());

    // Test bounds checking
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> {
          gcRuntime.getArrayElement(intArray, 5);
        });

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> {
          gcRuntime.setArrayElement(intArray, -1, GcValue.i32(0));
        });

    // Test reference type checking
    assertTrue(gcRuntime.refTest(intArray, GcReferenceType.ARRAY_REF));
    assertTrue(gcRuntime.refTest(intArray, GcReferenceType.EQ_REF));
    assertTrue(gcRuntime.refTest(intArray, GcReferenceType.ANY_REF));

    LOGGER.info("Array operations test completed successfully");
  }

  @Test
  void testReferenceCastingAndTypeChecking() {
    LOGGER.info("Testing reference casting and type checking");

    // Create various objects
    I31Instance i31 = gcRuntime.createI31(42);

    StructType structType =
        StructType.builder("TestStruct").addField("value", FieldType.i32(), true).build();
    gcRuntime.registerStructType(structType);
    final StructInstance struct =
        gcRuntime.createStruct(structType, Arrays.asList(GcValue.i32(123)));

    ArrayType arrayType =
        ArrayType.builder("TestArray").elementType(FieldType.i32()).mutable(true).build();
    gcRuntime.registerArrayType(arrayType);
    final ArrayInstance array = gcRuntime.createArray(arrayType, Arrays.asList(GcValue.i32(456)));

    // Test type hierarchy relationships
    assertTrue(gcRuntime.refTest(i31, GcReferenceType.I31_REF));
    assertTrue(gcRuntime.refTest(i31, GcReferenceType.EQ_REF));
    assertTrue(gcRuntime.refTest(i31, GcReferenceType.ANY_REF));
    assertFalse(gcRuntime.refTest(i31, GcReferenceType.STRUCT_REF));
    assertFalse(gcRuntime.refTest(i31, GcReferenceType.ARRAY_REF));

    assertTrue(gcRuntime.refTest(struct, GcReferenceType.STRUCT_REF));
    assertTrue(gcRuntime.refTest(struct, GcReferenceType.EQ_REF));
    assertTrue(gcRuntime.refTest(struct, GcReferenceType.ANY_REF));
    assertFalse(gcRuntime.refTest(struct, GcReferenceType.I31_REF));
    assertFalse(gcRuntime.refTest(struct, GcReferenceType.ARRAY_REF));

    assertTrue(gcRuntime.refTest(array, GcReferenceType.ARRAY_REF));
    assertTrue(gcRuntime.refTest(array, GcReferenceType.EQ_REF));
    assertTrue(gcRuntime.refTest(array, GcReferenceType.ANY_REF));
    assertFalse(gcRuntime.refTest(array, GcReferenceType.I31_REF));
    assertFalse(gcRuntime.refTest(array, GcReferenceType.STRUCT_REF));

    // Test upcasting (should succeed)
    GcObject anyRef1 = gcRuntime.refCast(i31, GcReferenceType.ANY_REF);
    GcObject anyRef2 = gcRuntime.refCast(struct, GcReferenceType.ANY_REF);
    GcObject eqRef = gcRuntime.refCast(i31, GcReferenceType.EQ_REF);
    assertNotNull(anyRef1);
    assertNotNull(anyRef2);
    assertNotNull(eqRef);

    // Test invalid downcasting (should fail)
    assertThrows(
        ClassCastException.class,
        () -> {
          gcRuntime.refCast(struct, GcReferenceType.I31_REF);
        });

    assertThrows(
        ClassCastException.class,
        () -> {
          gcRuntime.refCast(array, GcReferenceType.STRUCT_REF);
        });

    LOGGER.info("Reference casting and type checking test completed successfully");
  }

  @Test
  void testGarbageCollection() {
    LOGGER.info("Testing garbage collection");

    // Get initial GC stats
    GcStats initialStats = gcRuntime.getGcStats();
    assertNotNull(initialStats);

    long initialAllocated = initialStats.getTotalAllocated();
    final long initialCollections = initialStats.getMajorCollections();

    // Allocate some objects
    for (int i = 0; i < 100; i++) {
      I31Instance i31 = gcRuntime.createI31(i);
      assertNotNull(i31);
    }

    // Get stats after allocation
    GcStats afterAllocationStats = gcRuntime.getGcStats();
    assertTrue(afterAllocationStats.getTotalAllocated() > initialAllocated);

    // Trigger garbage collection
    GcStats collectionStats = gcRuntime.collectGarbage();
    assertNotNull(collectionStats);

    // Get final stats
    GcStats finalStats = gcRuntime.getGcStats();
    assertTrue(finalStats.getMajorCollections() > initialCollections);

    // Verify stats consistency
    assertTrue(finalStats.getTotalAllocated() >= initialStats.getTotalAllocated());
    assertTrue(finalStats.getBytesAllocated() >= 0);
    assertTrue(finalStats.getCurrentHeapSize() >= 0);

    LOGGER.info("Garbage collection test completed successfully");
  }

  @Test
  void testComplexNestedStructures() {
    LOGGER.info("Testing complex nested structures");

    // Create a nested struct type (struct containing arrays and other structs)
    StructType innerStructType =
        StructType.builder("InnerStruct")
            .addField("id", FieldType.i32(), false)
            .addField("name_length", FieldType.i32(), false)
            .build();
    gcRuntime.registerStructType(innerStructType);

    ArrayType stringArrayType =
        ArrayType.builder("ByteArray").elementType(FieldType.packedI8()).mutable(false).build();
    gcRuntime.registerArrayType(stringArrayType);

    StructType outerStructType =
        StructType.builder("OuterStruct")
            .addField("inner", FieldType.reference(GcReferenceType.STRUCT_REF), true)
            .addField("data", FieldType.reference(GcReferenceType.ARRAY_REF), true)
            .addField("counter", FieldType.i64(), true)
            .build();
    gcRuntime.registerStructType(outerStructType);

    // Create inner objects
    StructInstance innerStruct =
        gcRuntime.createStruct(innerStructType, Arrays.asList(GcValue.i32(1001), GcValue.i32(10)));

    ArrayInstance byteArray =
        gcRuntime.createArray(
            stringArrayType,
            Arrays.asList(
                GcValue.i32(72), // 'H'
                GcValue.i32(101), // 'e'
                GcValue.i32(108), // 'l'
                GcValue.i32(108), // 'l'
                GcValue.i32(111) // 'o'
                ));

    // Create outer struct
    StructInstance outerStruct =
        gcRuntime.createStruct(
            outerStructType,
            Arrays.asList(
                GcValue.reference(innerStruct), GcValue.reference(byteArray), GcValue.i64(12345L)));

    // Verify nested access
    assertNotNull(outerStruct);
    assertEquals(12345L, gcRuntime.getStructField(outerStruct, 2).asI64());

    // Get nested references and verify their contents
    GcValue innerRef = gcRuntime.getStructField(outerStruct, 0);
    assertNotNull(innerRef.asReference());

    GcValue arrayRef = gcRuntime.getStructField(outerStruct, 1);
    assertNotNull(arrayRef.asReference());

    LOGGER.info("Complex nested structures test completed successfully");
  }

  @Test
  void testErrorHandlingAndEdgeCases() {
    LOGGER.info("Testing error handling and edge cases");

    // Test null parameter validation
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          gcRuntime.createStruct(null, Arrays.asList(GcValue.i32(1)));
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          gcRuntime.createArray(null, Arrays.asList(GcValue.i32(1)));
        });

    // Test invalid field/element indices
    I31Instance i31 = gcRuntime.createI31(42);

    StructType structType =
        StructType.builder("TestStruct").addField("value", FieldType.i32(), true).build();
    gcRuntime.registerStructType(structType);
    StructInstance struct = gcRuntime.createStruct(structType, Arrays.asList(GcValue.i32(123)));

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          gcRuntime.getStructField(struct, -1);
        });

    // Test I31 value range validation
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          gcRuntime.createI31(Integer.MAX_VALUE); // Too large for 31-bit
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          gcRuntime.createI31(Integer.MIN_VALUE); // Too small for 31-bit
        });

    // Test array bounds
    ArrayType arrayType =
        ArrayType.builder("TestArray").elementType(FieldType.i32()).mutable(true).build();
    gcRuntime.registerArrayType(arrayType);
    ArrayInstance array = gcRuntime.createArray(arrayType, 5);

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> {
          gcRuntime.getArrayElement(array, 5); // Out of bounds
        });

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> {
          gcRuntime.setArrayElement(array, -1, GcValue.i32(0)); // Negative index
        });

    // Test negative array length
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          gcRuntime.createArray(arrayType, -1);
        });

    LOGGER.info("Error handling and edge cases test completed successfully");
  }

  @Test
  void testReferenceTypeHierarchy() {
    LOGGER.info("Testing reference type hierarchy compliance");

    // Test GcReferenceType subtype relationships
    assertTrue(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.ANY_REF));
    assertTrue(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.EQ_REF));
    assertTrue(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.I31_REF));
    assertFalse(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.STRUCT_REF));
    assertFalse(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.ARRAY_REF));

    assertTrue(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ANY_REF));
    assertTrue(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.EQ_REF));
    assertFalse(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.I31_REF));
    assertFalse(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ARRAY_REF));

    assertTrue(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.ANY_REF));
    assertTrue(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.EQ_REF));
    assertFalse(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.I31_REF));
    assertFalse(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.STRUCT_REF));

    assertTrue(GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.ANY_REF));
    assertFalse(GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.I31_REF));

    assertTrue(GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.ANY_REF));
    assertFalse(GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.EQ_REF));

    // Test equality support
    assertTrue(GcReferenceType.I31_REF.supportsEquality());
    assertTrue(GcReferenceType.STRUCT_REF.supportsEquality());
    assertTrue(GcReferenceType.ARRAY_REF.supportsEquality());
    assertTrue(GcReferenceType.EQ_REF.supportsEquality());
    assertFalse(
        GcReferenceType.ANY_REF.supportsEquality()); // ANY_REF doesn't necessarily support equality

    LOGGER.info("Reference type hierarchy compliance test completed successfully");
  }
}
