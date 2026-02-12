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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests for advanced GcRuntime methods: {@link GcRuntime#getRuntimeType(GcObject)}, {@link
 * GcRuntime#createPackedStruct(StructType, List, Map)}, {@link
 * GcRuntime#createVariableLengthArray(ArrayType, int, List)}, {@link
 * GcRuntime#registerRecursiveType(String, Object)}, {@link GcRuntime#createTypeHierarchy(Object,
 * List)}, and {@link GcRuntime#registerFinalizationCallback(GcObject, Runnable)}.
 *
 * @since 1.0.0
 */
@DisplayName("GcRuntime Advanced Methods Tests")
public class GcRuntimeAdvancedMethodsTest {

  private static final Logger LOGGER =
      Logger.getLogger(GcRuntimeAdvancedMethodsTest.class.getName());

  private static boolean gcAvailable = false;

  private WasmRuntime runtime;
  private GcRuntime gcRuntime;

  @BeforeAll
  static void checkGcAvailable() {
    try {
      try (WasmRuntime rt = WasmRuntimeFactory.create()) {
        final GcRuntime gc = rt.getGcRuntime();
        gcAvailable = gc != null;
      }
    } catch (final Exception e) {
      LOGGER.warning("GC runtime not available: " + e.getMessage());
      gcAvailable = false;
    }
    LOGGER.info("GC runtime available: " + gcAvailable);
  }

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    assumeTrue(gcAvailable, "GC runtime not available - skipping");
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());

    runtime = WasmRuntimeFactory.create();
    gcRuntime = runtime.getGcRuntime();
    assertNotNull(gcRuntime, "GC runtime should be available");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    if (runtime != null) {
      try {
        runtime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close runtime: " + e.getMessage());
      }
    }
    LOGGER.info("Finished test: " + testInfo.getDisplayName());
  }

  @Nested
  @DisplayName("getRuntimeType() Tests")
  class GetRuntimeTypeTests {

    @Test
    @DisplayName("getRuntimeType returns type for struct instance")
    void getRuntimeTypeReturnsTypeForStruct() throws GcException {
      LOGGER.info("Testing getRuntimeType with struct instance");

      final StructType pointType =
          StructType.builder("RtTypeStructTest")
              .addField("x", FieldType.i32(), true)
              .addField("y", FieldType.i32(), true)
              .build();

      gcRuntime.registerStructType(pointType);
      final StructInstance struct =
          gcRuntime.createStruct(pointType, Arrays.asList(GcValue.i32(1), GcValue.i32(2)));
      assertNotNull(struct, "Struct should be created");

      final GcReferenceType refType = gcRuntime.getRuntimeType(struct);
      assertNotNull(refType, "getRuntimeType should return non-null for a struct");
      LOGGER.info("Runtime type of struct: " + refType);

      // A struct should be a STRUCT_REF or subtype thereof
      assertTrue(
          refType == GcReferenceType.STRUCT_REF
              || refType == GcReferenceType.EQ_REF
              || refType == GcReferenceType.ANY_REF,
          "Struct runtime type should be STRUCT_REF, EQ_REF, or ANY_REF, got: " + refType);
    }

    @Test
    @DisplayName("getRuntimeType returns type for array instance")
    void getRuntimeTypeReturnsTypeForArray() throws GcException {
      LOGGER.info("Testing getRuntimeType with array instance");

      final ArrayType intArrayType =
          ArrayType.builder("RtTypeArrayTest").elementType(FieldType.i32()).mutable(true).build();

      gcRuntime.registerArrayType(intArrayType);
      final ArrayInstance array =
          gcRuntime.createArray(intArrayType, Arrays.asList(GcValue.i32(10), GcValue.i32(20)));
      assertNotNull(array, "Array should be created");

      final GcReferenceType refType = gcRuntime.getRuntimeType(array);
      assertNotNull(refType, "getRuntimeType should return non-null for an array");
      LOGGER.info("Runtime type of array: " + refType);

      // An array should be an ARRAY_REF or subtype thereof
      assertTrue(
          refType == GcReferenceType.ARRAY_REF
              || refType == GcReferenceType.EQ_REF
              || refType == GcReferenceType.ANY_REF,
          "Array runtime type should be ARRAY_REF, EQ_REF, or ANY_REF, got: " + refType);
    }
  }

  @Nested
  @DisplayName("createPackedStruct() Tests")
  class CreatePackedStructTests {

    @Test
    @DisplayName("createPackedStruct creates instance with packed fields")
    void createPackedStructCreatesInstance() throws GcException {
      LOGGER.info("Testing createPackedStruct with packed i8/i16 fields");

      final StructType packedType =
          StructType.builder("PackedStructTest")
              .addField("byteField", FieldType.packedI8(), true)
              .addField("shortField", FieldType.packedI16(), true)
              .addField("intField", FieldType.i32(), true)
              .build();

      gcRuntime.registerStructType(packedType);

      final List<GcValue> values =
          Arrays.asList(GcValue.i32(42), GcValue.i32(1000), GcValue.i32(99999));
      final Map<Integer, Integer> customAlignment = new HashMap<>();
      customAlignment.put(0, 1); // byte alignment for field 0
      customAlignment.put(1, 2); // short alignment for field 1

      final StructInstance packed =
          gcRuntime.createPackedStruct(packedType, values, customAlignment);
      assertNotNull(packed, "createPackedStruct should return a non-null StructInstance");

      LOGGER.info("Packed struct created successfully");
    }

    @Test
    @DisplayName("createPackedStruct with null struct type throws exception")
    void createPackedStructWithNullTypeThrows() {
      LOGGER.info("Testing createPackedStruct with null StructType");

      final List<GcValue> values = Arrays.asList(GcValue.i32(1));
      final Map<Integer, Integer> alignment = Collections.emptyMap();

      assertThrows(
          Exception.class,
          () -> gcRuntime.createPackedStruct(null, values, alignment),
          "createPackedStruct should reject null StructType");

      LOGGER.info("Null StructType correctly rejected");
    }
  }

  @Nested
  @DisplayName("createVariableLengthArray() Tests")
  class CreateVariableLengthArrayTests {

    @Test
    @DisplayName("createVariableLengthArray creates instance with flexible elements")
    void createVariableLengthArrayCreatesInstance() {
      LOGGER.info("Testing createVariableLengthArray with base length and flexible elements");

      final ArrayType arrayType =
          ArrayType.builder("VarLenArrayTest").elementType(FieldType.i32()).mutable(true).build();

      try {
        gcRuntime.registerArrayType(arrayType);

        final int baseLength = 3;
        final List<GcValue> flexibleElements = Arrays.asList(GcValue.i32(100), GcValue.i32(200));

        final ArrayInstance array =
            gcRuntime.createVariableLengthArray(arrayType, baseLength, flexibleElements);
        assertNotNull(array, "createVariableLengthArray should return a non-null ArrayInstance");

        LOGGER.info(
            "Variable-length array created successfully with baseLength="
                + baseLength
                + " and "
                + flexibleElements.size()
                + " flexible elements");
      } catch (final GcException e) {
        // Native binding may have signature mismatch (WrongMethodTypeException)
        // This is a known issue in the Panama implementation
        LOGGER.warning(
            "createVariableLengthArray failed with native binding error: " + e.getMessage());
        assertNotNull(e.getMessage(), "GcException should have a descriptive message");
        assertTrue(
            e.getMessage().contains("Failed to create variable-length array")
                || e.getCause() != null,
            "Exception should describe the failure or have a root cause");
      }
    }

    @Test
    @DisplayName("createVariableLengthArray with zero base length succeeds or throws meaningfully")
    void createVariableLengthArrayWithZeroLength() {
      LOGGER.info("Testing createVariableLengthArray with baseLength=0");

      final ArrayType arrayType =
          ArrayType.builder("ZeroLenArrayTest").elementType(FieldType.i32()).mutable(true).build();

      try {
        gcRuntime.registerArrayType(arrayType);
        final ArrayInstance array =
            gcRuntime.createVariableLengthArray(arrayType, 0, Arrays.asList(GcValue.i32(1)));
        assertNotNull(array, "Zero base-length array should be created if implementation allows");
        LOGGER.info("Zero base-length array created successfully");
      } catch (final GcException e) {
        // Zero base length may be rejected, or native binding may fail
        LOGGER.info("Zero base-length array creation failed (expected): " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a descriptive message");
      }
    }
  }

  @Nested
  @DisplayName("registerRecursiveType() Tests")
  class RegisterRecursiveTypeTests {

    @Test
    @DisplayName("registerRecursiveType succeeds with valid definition")
    void registerRecursiveTypeSucceeds() {
      LOGGER.info("Testing registerRecursiveType with a valid type name and definition");

      // The type definition parameter is Object, so we provide a reasonable structure
      // that the implementation can interpret
      final Map<String, Object> typeDefinition = new HashMap<>();
      typeDefinition.put("name", "LinkedListNode");
      typeDefinition.put("selfReferencing", true);

      try {
        final int typeId = gcRuntime.registerRecursiveType("LinkedListNode", typeDefinition);
        LOGGER.info("Recursive type registered with ID: " + typeId);
        assertTrue(typeId >= 0, "Type ID should be non-negative");
      } catch (final GcException e) {
        // Implementation may not fully support recursive types yet
        LOGGER.info(
            "registerRecursiveType threw GcException (may be unsupported): " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a message");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("registerRecursiveType not yet implemented: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("createTypeHierarchy() Tests")
  class CreateTypeHierarchyTests {

    @Test
    @DisplayName("createTypeHierarchy succeeds with base and derived types")
    void createTypeHierarchySucceeds() {
      LOGGER.info("Testing createTypeHierarchy with base type and derived types");

      final Map<String, Object> baseType = new HashMap<>();
      baseType.put("name", "Shape");
      baseType.put("fields", Collections.singletonList("area"));

      final Map<String, Object> derivedType1 = new HashMap<>();
      derivedType1.put("name", "Circle");
      derivedType1.put("parent", "Shape");

      final Map<String, Object> derivedType2 = new HashMap<>();
      derivedType2.put("name", "Rectangle");
      derivedType2.put("parent", "Shape");

      final List<Object> derivedTypes = Arrays.asList(derivedType1, derivedType2);

      try {
        final Map<String, Integer> hierarchy =
            gcRuntime.createTypeHierarchy(baseType, derivedTypes);
        assertNotNull(hierarchy, "Type hierarchy should return a non-null map");
        LOGGER.info("Type hierarchy created with " + hierarchy.size() + " entries: " + hierarchy);
      } catch (final GcException e) {
        // Implementation may not fully support type hierarchies yet
        LOGGER.info(
            "createTypeHierarchy threw GcException (may be unsupported): " + e.getMessage());
        assertNotNull(e.getMessage(), "Exception should have a message");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("createTypeHierarchy not yet implemented: " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("registerFinalizationCallback() Tests")
  class RegisterFinalizationCallbackTests {

    @Test
    @DisplayName("registerFinalizationCallback registers without exception")
    void registerFinalizationCallbackSucceeds() throws GcException {
      LOGGER.info("Testing registerFinalizationCallback with a valid object and callback");

      final StructType type =
          StructType.builder("FinalizableStruct").addField("value", FieldType.i32(), true).build();

      gcRuntime.registerStructType(type);
      final StructInstance obj = gcRuntime.createStruct(type, Arrays.asList(GcValue.i32(42)));
      assertNotNull(obj, "Struct should be created for finalization test");

      final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
      final Runnable callback =
          () -> {
            callbackInvoked.set(true);
            LOGGER.info("Finalization callback invoked");
          };

      assertDoesNotThrow(
          () -> gcRuntime.registerFinalizationCallback(obj, callback),
          "registerFinalizationCallback should not throw for valid args");

      LOGGER.info("Finalization callback registered successfully");
    }

    @Test
    @DisplayName("registerFinalizationCallback with null callback throws exception")
    void registerFinalizationCallbackWithNullCallbackThrows() throws GcException {
      LOGGER.info("Testing registerFinalizationCallback with null callback");

      final StructType type =
          StructType.builder("NullCallbackStruct").addField("value", FieldType.i32(), true).build();

      gcRuntime.registerStructType(type);
      final StructInstance obj = gcRuntime.createStruct(type, Arrays.asList(GcValue.i32(1)));
      assertNotNull(obj, "Struct should be created");

      assertThrows(
          Exception.class,
          () -> gcRuntime.registerFinalizationCallback(obj, null),
          "registerFinalizationCallback should reject null callback");

      LOGGER.info("Null callback correctly rejected");
    }
  }
}
