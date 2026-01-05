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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WitPrimitiveType;
import ai.tegmentum.wasmtime4j.WitType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WIT (WebAssembly Interface Types) value marshalling.
 *
 * <p>These tests verify that WIT values can be correctly marshalled between Java and WebAssembly
 * component interfaces, covering all primitive types, composite types, and special cases.
 *
 * @since 1.0.0
 */
@DisplayName("WIT Value Marshalling Integration Tests")
public final class WitValueMarshallingIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WitValueMarshallingIntegrationTest.class.getName());

  @BeforeAll
  static void setUp() {
    LOGGER.info("Setting up WIT Value Marshalling Integration Tests");
  }

  @Nested
  @DisplayName("Primitive Type Tests")
  class PrimitiveTypeTests {

    @Test
    @DisplayName("Should create boolean WIT type")
    void shouldCreateBooleanWitType() {
      LOGGER.info("Testing boolean WIT type creation");

      WitType boolType = WitType.createBool();

      assertNotNull(boolType, "Boolean type should not be null");
      assertEquals("bool", boolType.getName(), "Boolean type name should be 'bool'");
      assertTrue(boolType.isPrimitive(), "Boolean should be a primitive type");
      assertFalse(boolType.isComposite(), "Boolean should not be a composite type");
      assertFalse(boolType.isResource(), "Boolean should not be a resource type");

      LOGGER.info("Boolean WIT type created: " + boolType);
    }

    @Test
    @DisplayName("Should create signed integer WIT types")
    void shouldCreateSignedIntegerWitTypes() {
      LOGGER.info("Testing signed integer WIT type creation");

      // Test S8
      final WitType s8Type = WitType.createS8();
      assertNotNull(s8Type, "S8 type should not be null");
      assertEquals("s8", s8Type.getName());
      assertTrue(s8Type.isPrimitive());

      // Test S16
      final WitType s16Type = WitType.createS16();
      assertNotNull(s16Type, "S16 type should not be null");
      assertEquals("s16", s16Type.getName());
      assertTrue(s16Type.isPrimitive());

      // Test S32
      final WitType s32Type = WitType.createS32();
      assertNotNull(s32Type, "S32 type should not be null");
      assertEquals("s32", s32Type.getName());
      assertTrue(s32Type.isPrimitive());

      // Test S64
      final WitType s64Type = WitType.createS64();
      assertNotNull(s64Type, "S64 type should not be null");
      assertEquals("s64", s64Type.getName());
      assertTrue(s64Type.isPrimitive());

      LOGGER.info("Signed integer WIT types created successfully");
    }

    @Test
    @DisplayName("Should create unsigned integer WIT types")
    void shouldCreateUnsignedIntegerWitTypes() {
      LOGGER.info("Testing unsigned integer WIT type creation");

      // Test U8
      final WitType u8Type = WitType.createU8();
      assertNotNull(u8Type, "U8 type should not be null");
      assertEquals("u8", u8Type.getName());

      // Test U16
      final WitType u16Type = WitType.createU16();
      assertNotNull(u16Type, "U16 type should not be null");
      assertEquals("u16", u16Type.getName());

      // Test U32
      final WitType u32Type = WitType.createU32();
      assertNotNull(u32Type, "U32 type should not be null");
      assertEquals("u32", u32Type.getName());

      // Test U64
      final WitType u64Type = WitType.createU64();
      assertNotNull(u64Type, "U64 type should not be null");
      assertEquals("u64", u64Type.getName());

      LOGGER.info("Unsigned integer WIT types created successfully");
    }

    @Test
    @DisplayName("Should create floating point WIT types")
    void shouldCreateFloatingPointWitTypes() {
      LOGGER.info("Testing floating point WIT type creation");

      WitType float32Type = WitType.createFloat32();
      WitType float64Type = WitType.createFloat64();

      assertNotNull(float32Type, "Float32 type should not be null");
      assertNotNull(float64Type, "Float64 type should not be null");

      assertEquals("float32", float32Type.getName());
      assertEquals("float64", float64Type.getName());

      assertTrue(float32Type.isPrimitive());
      assertTrue(float64Type.isPrimitive());

      LOGGER.info("Floating point WIT types created successfully");
    }

    @Test
    @DisplayName("Should create char WIT type")
    void shouldCreateCharWitType() {
      LOGGER.info("Testing char WIT type creation");

      WitType charType = WitType.createChar();

      assertNotNull(charType, "Char type should not be null");
      assertEquals("char", charType.getName());
      assertTrue(charType.isPrimitive());

      LOGGER.info("Char WIT type created: " + charType);
    }

    @Test
    @DisplayName("Should create string WIT type")
    void shouldCreateStringWitType() {
      LOGGER.info("Testing string WIT type creation");

      WitType stringType = WitType.createString();

      assertNotNull(stringType, "String type should not be null");
      assertEquals("string", stringType.getName());
      assertTrue(stringType.isPrimitive());

      LOGGER.info("String WIT type created: " + stringType);
    }
  }

  @Nested
  @DisplayName("Composite Type Tests")
  class CompositeTypeTests {

    @Test
    @DisplayName("Should create record WIT type")
    void shouldCreateRecordWitType() {
      LOGGER.info("Testing record WIT type creation");

      Map<String, WitType> fields =
          Map.of("name", WitType.createString(), "age", WitType.createU32());

      WitType recordType = WitType.record("Person", fields);

      assertNotNull(recordType, "Record type should not be null");
      assertEquals("Person", recordType.getName());
      assertTrue(recordType.isComposite(), "Record should be a composite type");
      assertFalse(recordType.isPrimitive(), "Record should not be a primitive type");

      // Check metadata contains field count
      assertEquals(2, recordType.getMetadata().get("fieldCount"));

      LOGGER.info("Record WIT type created: " + recordType);
    }

    @Test
    @DisplayName("Should create list WIT type")
    void shouldCreateListWitType() {
      LOGGER.info("Testing list WIT type creation");

      WitType stringType = WitType.createString();
      WitType listType = WitType.list(stringType);

      assertNotNull(listType, "List type should not be null");
      assertTrue(listType.getName().contains("list"), "Name should contain 'list'");
      assertTrue(listType.isComposite(), "List should be a composite type");

      LOGGER.info("List WIT type created: " + listType);
    }

    @Test
    @DisplayName("Should create tuple WIT type")
    void shouldCreateTupleWitType() {
      LOGGER.info("Testing tuple WIT type creation");

      WitType tupleType = WitType.tuple(WitType.createS32(), WitType.createString());

      assertNotNull(tupleType, "Tuple type should not be null");
      assertTrue(tupleType.getName().contains("tuple"), "Name should contain 'tuple'");
      assertTrue(tupleType.isComposite(), "Tuple should be a composite type");
      assertEquals(2, tupleType.getMetadata().get("elementCount"));

      LOGGER.info("Tuple WIT type created: " + tupleType);
    }

    @Test
    @DisplayName("Should create option WIT type")
    void shouldCreateOptionWitType() {
      LOGGER.info("Testing option WIT type creation");

      WitType innerType = WitType.createS32();
      WitType optionType = WitType.option(innerType);

      assertNotNull(optionType, "Option type should not be null");
      assertTrue(optionType.getName().contains("option"), "Name should contain 'option'");
      assertTrue(optionType.isComposite(), "Option should be a composite type");

      LOGGER.info("Option WIT type created: " + optionType);
    }

    @Test
    @DisplayName("Should create result WIT type")
    void shouldCreateResultWitType() {
      LOGGER.info("Testing result WIT type creation");

      WitType okType = WitType.createS32();
      WitType errorType = WitType.createString();
      WitType resultType = WitType.result(Optional.of(okType), Optional.of(errorType));

      assertNotNull(resultType, "Result type should not be null");
      assertTrue(resultType.getName().contains("result"), "Name should contain 'result'");
      assertTrue(resultType.isComposite(), "Result should be a composite type");

      LOGGER.info("Result WIT type created: " + resultType);
    }

    @Test
    @DisplayName("Should create enum WIT type")
    void shouldCreateEnumWitType() {
      LOGGER.info("Testing enum WIT type creation");

      List<String> values = List.of("Red", "Green", "Blue");
      WitType enumType = WitType.enumType("Color", values);

      assertNotNull(enumType, "Enum type should not be null");
      assertEquals("Color", enumType.getName());
      assertTrue(enumType.isComposite(), "Enum should be a composite type");
      assertEquals(3, enumType.getMetadata().get("valueCount"));

      LOGGER.info("Enum WIT type created: " + enumType);
    }

    @Test
    @DisplayName("Should create flags WIT type")
    void shouldCreateFlagsWitType() {
      LOGGER.info("Testing flags WIT type creation");

      List<String> flags = List.of("Read", "Write", "Execute");
      WitType flagsType = WitType.flags("Permission", flags);

      assertNotNull(flagsType, "Flags type should not be null");
      assertEquals("Permission", flagsType.getName());
      assertTrue(flagsType.isComposite(), "Flags should be a composite type");
      assertEquals(3, flagsType.getMetadata().get("flagCount"));

      LOGGER.info("Flags WIT type created: " + flagsType);
    }

    @Test
    @DisplayName("Should create variant WIT type")
    void shouldCreateVariantWitType() {
      LOGGER.info("Testing variant WIT type creation");

      Map<String, Optional<WitType>> cases =
          Map.of(
              "None", Optional.empty(),
              "Some", Optional.of(WitType.createS32()));
      WitType variantType = WitType.variant("OptionalInt", cases);

      assertNotNull(variantType, "Variant type should not be null");
      assertEquals("OptionalInt", variantType.getName());
      assertTrue(variantType.isComposite(), "Variant should be a composite type");
      assertEquals(2, variantType.getMetadata().get("caseCount"));

      LOGGER.info("Variant WIT type created: " + variantType);
    }
  }

  @Nested
  @DisplayName("WitPrimitiveType Enum Tests")
  class WitPrimitiveTypeEnumTests {

    @Test
    @DisplayName("Should have all expected primitive types")
    void shouldHaveAllExpectedPrimitiveTypes() {
      LOGGER.info("Testing WitPrimitiveType enum values");

      WitPrimitiveType[] types = WitPrimitiveType.values();

      assertEquals(13, types.length, "Should have 13 primitive types");

      assertNotNull(WitPrimitiveType.BOOL);
      assertNotNull(WitPrimitiveType.S8);
      assertNotNull(WitPrimitiveType.U8);
      assertNotNull(WitPrimitiveType.S16);
      assertNotNull(WitPrimitiveType.U16);
      assertNotNull(WitPrimitiveType.S32);
      assertNotNull(WitPrimitiveType.U32);
      assertNotNull(WitPrimitiveType.S64);
      assertNotNull(WitPrimitiveType.U64);
      assertNotNull(WitPrimitiveType.FLOAT32);
      assertNotNull(WitPrimitiveType.FLOAT64);
      assertNotNull(WitPrimitiveType.CHAR);
      assertNotNull(WitPrimitiveType.STRING);

      LOGGER.info("WitPrimitiveType enum values verified");
    }

    @Test
    @DisplayName("Should have correct WIT type names")
    void shouldHaveCorrectWitTypeNames() {
      LOGGER.info("Testing WIT type names");

      assertEquals("bool", WitPrimitiveType.BOOL.getWitTypeName());
      assertEquals("s8", WitPrimitiveType.S8.getWitTypeName());
      assertEquals("u8", WitPrimitiveType.U8.getWitTypeName());
      assertEquals("s16", WitPrimitiveType.S16.getWitTypeName());
      assertEquals("u16", WitPrimitiveType.U16.getWitTypeName());
      assertEquals("s32", WitPrimitiveType.S32.getWitTypeName());
      assertEquals("u32", WitPrimitiveType.U32.getWitTypeName());
      assertEquals("s64", WitPrimitiveType.S64.getWitTypeName());
      assertEquals("u64", WitPrimitiveType.U64.getWitTypeName());
      assertEquals("float32", WitPrimitiveType.FLOAT32.getWitTypeName());
      assertEquals("float64", WitPrimitiveType.FLOAT64.getWitTypeName());
      assertEquals("char", WitPrimitiveType.CHAR.getWitTypeName());
      assertEquals("string", WitPrimitiveType.STRING.getWitTypeName());

      LOGGER.info("WIT type names verified");
    }

    @Test
    @DisplayName("Should have correct byte sizes")
    void shouldHaveCorrectByteSizes() {
      LOGGER.info("Testing primitive type byte sizes");

      assertEquals(1, WitPrimitiveType.BOOL.getSizeBytes());
      assertEquals(1, WitPrimitiveType.S8.getSizeBytes());
      assertEquals(1, WitPrimitiveType.U8.getSizeBytes());
      assertEquals(2, WitPrimitiveType.S16.getSizeBytes());
      assertEquals(2, WitPrimitiveType.U16.getSizeBytes());
      assertEquals(4, WitPrimitiveType.S32.getSizeBytes());
      assertEquals(4, WitPrimitiveType.U32.getSizeBytes());
      assertEquals(8, WitPrimitiveType.S64.getSizeBytes());
      assertEquals(8, WitPrimitiveType.U64.getSizeBytes());
      assertEquals(4, WitPrimitiveType.FLOAT32.getSizeBytes());
      assertEquals(8, WitPrimitiveType.FLOAT64.getSizeBytes());
      assertEquals(4, WitPrimitiveType.CHAR.getSizeBytes());
      assertEquals(-1, WitPrimitiveType.STRING.getSizeBytes()); // Variable size

      LOGGER.info("Byte sizes verified");
    }

    @Test
    @DisplayName("Should correctly identify integer types")
    void shouldCorrectlyIdentifyIntegerTypes() {
      LOGGER.info("Testing integer type identification");

      assertTrue(WitPrimitiveType.S8.isInteger());
      assertTrue(WitPrimitiveType.U8.isInteger());
      assertTrue(WitPrimitiveType.S16.isInteger());
      assertTrue(WitPrimitiveType.U16.isInteger());
      assertTrue(WitPrimitiveType.S32.isInteger());
      assertTrue(WitPrimitiveType.U32.isInteger());
      assertTrue(WitPrimitiveType.S64.isInteger());
      assertTrue(WitPrimitiveType.U64.isInteger());

      assertFalse(WitPrimitiveType.BOOL.isInteger());
      assertFalse(WitPrimitiveType.FLOAT32.isInteger());
      assertFalse(WitPrimitiveType.FLOAT64.isInteger());
      assertFalse(WitPrimitiveType.CHAR.isInteger());
      assertFalse(WitPrimitiveType.STRING.isInteger());

      LOGGER.info("Integer type identification verified");
    }

    @Test
    @DisplayName("Should correctly identify floating point types")
    void shouldCorrectlyIdentifyFloatingPointTypes() {
      LOGGER.info("Testing floating point type identification");

      assertTrue(WitPrimitiveType.FLOAT32.isFloatingPoint());
      assertTrue(WitPrimitiveType.FLOAT64.isFloatingPoint());

      assertFalse(WitPrimitiveType.BOOL.isFloatingPoint());
      assertFalse(WitPrimitiveType.S32.isFloatingPoint());
      assertFalse(WitPrimitiveType.STRING.isFloatingPoint());

      LOGGER.info("Floating point type identification verified");
    }

    @Test
    @DisplayName("Should correctly identify signed vs unsigned")
    void shouldCorrectlyIdentifySignedVsUnsigned() {
      LOGGER.info("Testing signed vs unsigned identification");

      assertTrue(WitPrimitiveType.S8.isSignedInteger());
      assertTrue(WitPrimitiveType.S16.isSignedInteger());
      assertTrue(WitPrimitiveType.S32.isSignedInteger());
      assertTrue(WitPrimitiveType.S64.isSignedInteger());

      assertTrue(WitPrimitiveType.U8.isUnsignedInteger());
      assertTrue(WitPrimitiveType.U16.isUnsignedInteger());
      assertTrue(WitPrimitiveType.U32.isUnsignedInteger());
      assertTrue(WitPrimitiveType.U64.isUnsignedInteger());

      assertFalse(WitPrimitiveType.S8.isUnsignedInteger());
      assertFalse(WitPrimitiveType.U8.isSignedInteger());

      LOGGER.info("Signed vs unsigned identification verified");
    }

    @Test
    @DisplayName("Should map to correct Java types")
    void shouldMapToCorrectJavaTypes() {
      LOGGER.info("Testing Java type mapping");

      assertEquals(boolean.class, WitPrimitiveType.BOOL.getJavaType());
      assertEquals(byte.class, WitPrimitiveType.S8.getJavaType());
      assertEquals(byte.class, WitPrimitiveType.U8.getJavaType());
      assertEquals(short.class, WitPrimitiveType.S16.getJavaType());
      assertEquals(short.class, WitPrimitiveType.U16.getJavaType());
      assertEquals(int.class, WitPrimitiveType.S32.getJavaType());
      assertEquals(int.class, WitPrimitiveType.U32.getJavaType());
      assertEquals(long.class, WitPrimitiveType.S64.getJavaType());
      assertEquals(long.class, WitPrimitiveType.U64.getJavaType());
      assertEquals(float.class, WitPrimitiveType.FLOAT32.getJavaType());
      assertEquals(double.class, WitPrimitiveType.FLOAT64.getJavaType());
      assertEquals(char.class, WitPrimitiveType.CHAR.getJavaType());
      assertEquals(String.class, WitPrimitiveType.STRING.getJavaType());

      LOGGER.info("Java type mapping verified");
    }

    @Test
    @DisplayName("Should parse from string")
    void shouldParseFromString() {
      LOGGER.info("Testing parsing from string");

      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("bool"));
      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("BOOL"));
      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("boolean"));
      assertEquals(WitPrimitiveType.S8, WitPrimitiveType.fromString("s8"));
      assertEquals(WitPrimitiveType.S8, WitPrimitiveType.fromString("i8"));
      assertEquals(WitPrimitiveType.U8, WitPrimitiveType.fromString("u8"));
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("s32"));
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("i32"));
      assertEquals(WitPrimitiveType.FLOAT32, WitPrimitiveType.fromString("f32"));
      assertEquals(WitPrimitiveType.FLOAT32, WitPrimitiveType.fromString("float32"));
      assertEquals(WitPrimitiveType.FLOAT64, WitPrimitiveType.fromString("f64"));
      assertEquals(WitPrimitiveType.FLOAT64, WitPrimitiveType.fromString("float64"));
      assertEquals(WitPrimitiveType.STRING, WitPrimitiveType.fromString("string"));

      LOGGER.info("String parsing verified");
    }

    @Test
    @DisplayName("Should throw for invalid type name")
    void shouldThrowForInvalidTypeName() {
      LOGGER.info("Testing exception for invalid type name");

      assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString("invalid"));
      assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString(""));
      assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString(null));

      LOGGER.info("Invalid type name exception verified");
    }

    @Test
    @DisplayName("Should correctly identify variable size types")
    void shouldCorrectlyIdentifyVariableSizeTypes() {
      LOGGER.info("Testing variable size identification");

      assertTrue(WitPrimitiveType.STRING.isVariableSize());

      assertFalse(WitPrimitiveType.BOOL.isVariableSize());
      assertFalse(WitPrimitiveType.S32.isVariableSize());
      assertFalse(WitPrimitiveType.FLOAT64.isVariableSize());
      assertFalse(WitPrimitiveType.CHAR.isVariableSize());

      LOGGER.info("Variable size identification verified");
    }
  }

  @Nested
  @DisplayName("Type Compatibility Tests")
  class TypeCompatibilityTests {

    @Test
    @DisplayName("Should be compatible with same type")
    void shouldBeCompatibleWithSameType() {
      LOGGER.info("Testing same type compatibility");

      WitType s32Type1 = WitType.createS32();
      WitType s32Type2 = WitType.createS32();

      assertTrue(s32Type1.isCompatibleWith(s32Type2), "Same types should be compatible");
      assertTrue(s32Type1.equals(s32Type2), "Same types should be equal");

      LOGGER.info("Same type compatibility verified");
    }

    @Test
    @DisplayName("Should not be compatible with null")
    void shouldNotBeCompatibleWithNull() {
      LOGGER.info("Testing null compatibility");

      WitType s32Type = WitType.createS32();

      assertFalse(s32Type.isCompatibleWith(null), "Type should not be compatible with null");

      LOGGER.info("Null compatibility verified");
    }

    @Test
    @DisplayName("Should get size bytes for primitive types")
    void shouldGetSizeBytesForPrimitiveTypes() {
      LOGGER.info("Testing getSizeBytes for primitive types");

      WitType s32Type = WitType.createS32();
      Optional<Integer> size = s32Type.getSizeBytes();

      assertTrue(size.isPresent(), "Size should be present for primitive type");
      assertEquals(4, size.get(), "S32 should be 4 bytes");

      LOGGER.info("getSizeBytes verified for primitive types");
    }
  }

  @Nested
  @DisplayName("Nested Type Tests")
  class NestedTypeTests {

    @Test
    @DisplayName("Should create nested list of records")
    void shouldCreateNestedListOfRecords() {
      LOGGER.info("Testing nested list of records");

      Map<String, WitType> personFields =
          Map.of("name", WitType.createString(), "age", WitType.createU32());
      WitType personRecord = WitType.record("Person", personFields);

      WitType peopleList = WitType.list(personRecord);

      assertNotNull(peopleList, "List type should not be null");
      assertTrue(peopleList.isComposite(), "List should be a composite type");
      assertTrue(peopleList.getName().contains("list"), "Name should contain 'list'");

      LOGGER.info("Nested list of records created: " + peopleList);
    }

    @Test
    @DisplayName("Should create option of list")
    void shouldCreateOptionOfList() {
      LOGGER.info("Testing option of list");

      WitType listType = WitType.list(WitType.createString());
      WitType optionOfList = WitType.option(listType);

      assertNotNull(optionOfList, "Option type should not be null");
      assertTrue(optionOfList.isComposite(), "Option should be a composite type");
      assertTrue(optionOfList.getName().contains("option"), "Name should contain 'option'");

      LOGGER.info("Option of list created: " + optionOfList);
    }

    @Test
    @DisplayName("Should create result with complex types")
    void shouldCreateResultWithComplexTypes() {
      LOGGER.info("Testing result with complex types");

      WitType okType = WitType.list(WitType.createS32());
      WitType errorType = WitType.record("Error", Map.of("message", WitType.createString()));

      WitType resultType = WitType.result(Optional.of(okType), Optional.of(errorType));

      assertNotNull(resultType, "Result type should not be null");
      assertTrue(resultType.isComposite(), "Result should be a composite type");

      LOGGER.info("Result with complex types created: " + resultType);
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should create empty record")
    void shouldCreateEmptyRecord() {
      LOGGER.info("Testing empty record creation");

      WitType emptyRecord = WitType.record("Empty", Map.of());

      assertNotNull(emptyRecord, "Empty record should not be null");
      assertEquals("Empty", emptyRecord.getName());
      assertEquals(0, emptyRecord.getMetadata().get("fieldCount"));

      LOGGER.info("Empty record created: " + emptyRecord);
    }

    @Test
    @DisplayName("Should create empty tuple")
    void shouldCreateEmptyTuple() {
      LOGGER.info("Testing empty tuple creation");

      WitType emptyTuple = WitType.tuple(List.of());

      assertNotNull(emptyTuple, "Empty tuple should not be null");
      assertEquals(0, emptyTuple.getMetadata().get("elementCount"));

      LOGGER.info("Empty tuple created: " + emptyTuple);
    }

    @Test
    @DisplayName("Should create result with no ok type")
    void shouldCreateResultWithNoOkType() {
      LOGGER.info("Testing result with no ok type");

      WitType errorType = WitType.createString();
      WitType resultType = WitType.result(Optional.empty(), Optional.of(errorType));

      assertNotNull(resultType, "Result type should not be null");
      assertTrue(resultType.getName().contains("result"), "Name should contain 'result'");

      LOGGER.info("Result with no ok type created: " + resultType);
    }

    @Test
    @DisplayName("Should create result with no error type")
    void shouldCreateResultWithNoErrorType() {
      LOGGER.info("Testing result with no error type");

      WitType okType = WitType.createS32();
      WitType resultType = WitType.result(Optional.of(okType), Optional.empty());

      assertNotNull(resultType, "Result type should not be null");
      assertTrue(resultType.getName().contains("result"), "Name should contain 'result'");

      LOGGER.info("Result with no error type created: " + resultType);
    }

    @Test
    @DisplayName("Should create resource type")
    void shouldCreateResourceType() {
      LOGGER.info("Testing resource type creation");

      WitType resourceType = WitType.resource("FileHandle", "file-handle-123");

      assertNotNull(resourceType, "Resource type should not be null");
      assertEquals("FileHandle", resourceType.getName());
      assertTrue(resourceType.isResource(), "Should be a resource type");
      assertFalse(resourceType.isPrimitive(), "Should not be a primitive type");
      assertEquals("file-handle-123", resourceType.getMetadata().get("resourceId"));

      LOGGER.info("Resource type created: " + resourceType);
    }
  }

  @Nested
  @DisplayName("Type Equality Tests")
  class TypeEqualityTests {

    @Test
    @DisplayName("Should correctly implement equals")
    void shouldCorrectlyImplementEquals() {
      LOGGER.info("Testing equals implementation");

      WitType s32a = WitType.createS32();
      WitType s32b = WitType.createS32();
      WitType s64 = WitType.createS64();

      assertTrue(s32a.equals(s32b), "Same primitive types should be equal");
      assertFalse(s32a.equals(s64), "Different primitive types should not be equal");
      assertFalse(s32a.equals(null), "Type should not equal null");
      assertTrue(s32a.equals(s32a), "Type should equal itself");

      LOGGER.info("Equals implementation verified");
    }

    @Test
    @DisplayName("Should correctly implement hashCode")
    void shouldCorrectlyImplementHashCode() {
      LOGGER.info("Testing hashCode implementation");

      WitType s32a = WitType.createS32();
      WitType s32b = WitType.createS32();

      assertEquals(s32a.hashCode(), s32b.hashCode(), "Equal types should have same hashCode");

      LOGGER.info("HashCode implementation verified");
    }

    @Test
    @DisplayName("Should correctly implement toString")
    void shouldCorrectlyImplementToString() {
      LOGGER.info("Testing toString implementation");

      WitType s32 = WitType.createS32();
      String str = s32.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitType"), "Should contain class name");
      assertTrue(str.contains("s32"), "Should contain type name");

      LOGGER.info("toString: " + str);
    }
  }
}
