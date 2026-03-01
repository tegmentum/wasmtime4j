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
package ai.tegmentum.wasmtime4j.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitPrimitiveType;
import ai.tegmentum.wasmtime4j.wit.WitType;
import ai.tegmentum.wasmtime4j.wit.WitTypeCategory;
import ai.tegmentum.wasmtime4j.wit.WitTypeKind;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the WIT (WebAssembly Interface Types) type system.
 *
 * <p>This test class validates WitType, WitTypeKind, WitPrimitiveType, and WitTypeCategory classes
 * for WebAssembly component model type handling.
 */
@DisplayName("WIT Type System Integration Tests")
public class WitTypeSystemTest {

  private static final Logger LOGGER = Logger.getLogger(WitTypeSystemTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WIT Type System Integration Tests");
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

      assertNotNull(WitPrimitiveType.BOOL, "BOOL should exist");
      assertNotNull(WitPrimitiveType.S8, "S8 should exist");
      assertNotNull(WitPrimitiveType.U8, "U8 should exist");
      assertNotNull(WitPrimitiveType.S16, "S16 should exist");
      assertNotNull(WitPrimitiveType.U16, "U16 should exist");
      assertNotNull(WitPrimitiveType.S32, "S32 should exist");
      assertNotNull(WitPrimitiveType.U32, "U32 should exist");
      assertNotNull(WitPrimitiveType.S64, "S64 should exist");
      assertNotNull(WitPrimitiveType.U64, "U64 should exist");
      assertNotNull(WitPrimitiveType.FLOAT32, "FLOAT32 should exist");
      assertNotNull(WitPrimitiveType.FLOAT64, "FLOAT64 should exist");
      assertNotNull(WitPrimitiveType.CHAR, "CHAR should exist");
      assertNotNull(WitPrimitiveType.STRING, "STRING should exist");

      LOGGER.info("WitPrimitiveType enum values verified: " + types.length);
    }

    @Test
    @DisplayName("Should have correct size bytes for primitive types")
    void shouldHaveCorrectSizeBytesForPrimitiveTypes() {
      LOGGER.info("Testing WitPrimitiveType size bytes");

      assertEquals(1, WitPrimitiveType.BOOL.getSizeBytes(), "BOOL should be 1 byte");
      assertEquals(1, WitPrimitiveType.S8.getSizeBytes(), "S8 should be 1 byte");
      assertEquals(1, WitPrimitiveType.U8.getSizeBytes(), "U8 should be 1 byte");
      assertEquals(2, WitPrimitiveType.S16.getSizeBytes(), "S16 should be 2 bytes");
      assertEquals(2, WitPrimitiveType.U16.getSizeBytes(), "U16 should be 2 bytes");
      assertEquals(4, WitPrimitiveType.S32.getSizeBytes(), "S32 should be 4 bytes");
      assertEquals(4, WitPrimitiveType.U32.getSizeBytes(), "U32 should be 4 bytes");
      assertEquals(8, WitPrimitiveType.S64.getSizeBytes(), "S64 should be 8 bytes");
      assertEquals(8, WitPrimitiveType.U64.getSizeBytes(), "U64 should be 8 bytes");
      assertEquals(4, WitPrimitiveType.FLOAT32.getSizeBytes(), "FLOAT32 should be 4 bytes");
      assertEquals(8, WitPrimitiveType.FLOAT64.getSizeBytes(), "FLOAT64 should be 8 bytes");
      assertEquals(4, WitPrimitiveType.CHAR.getSizeBytes(), "CHAR should be 4 bytes");
      assertEquals(-1, WitPrimitiveType.STRING.getSizeBytes(), "STRING should be variable size");

      LOGGER.info("WitPrimitiveType size bytes verified");
    }

    @Test
    @DisplayName("Should correctly identify variable size types")
    void shouldCorrectlyIdentifyVariableSizeTypes() {
      LOGGER.info("Testing variable size type identification");

      assertTrue(WitPrimitiveType.STRING.isVariableSize(), "STRING should be variable size");

      assertFalse(WitPrimitiveType.BOOL.isVariableSize(), "BOOL should not be variable size");
      assertFalse(WitPrimitiveType.S32.isVariableSize(), "S32 should not be variable size");
      assertFalse(WitPrimitiveType.FLOAT64.isVariableSize(), "FLOAT64 should not be variable size");

      LOGGER.info("Variable size type identification verified");
    }

    @Test
    @DisplayName("Should correctly identify integer types")
    void shouldCorrectlyIdentifyIntegerTypes() {
      LOGGER.info("Testing integer type identification");

      assertTrue(WitPrimitiveType.S8.isInteger(), "S8 should be integer");
      assertTrue(WitPrimitiveType.U8.isInteger(), "U8 should be integer");
      assertTrue(WitPrimitiveType.S16.isInteger(), "S16 should be integer");
      assertTrue(WitPrimitiveType.U16.isInteger(), "U16 should be integer");
      assertTrue(WitPrimitiveType.S32.isInteger(), "S32 should be integer");
      assertTrue(WitPrimitiveType.U32.isInteger(), "U32 should be integer");
      assertTrue(WitPrimitiveType.S64.isInteger(), "S64 should be integer");
      assertTrue(WitPrimitiveType.U64.isInteger(), "U64 should be integer");

      assertFalse(WitPrimitiveType.BOOL.isInteger(), "BOOL should not be integer");
      assertFalse(WitPrimitiveType.FLOAT32.isInteger(), "FLOAT32 should not be integer");
      assertFalse(WitPrimitiveType.FLOAT64.isInteger(), "FLOAT64 should not be integer");
      assertFalse(WitPrimitiveType.CHAR.isInteger(), "CHAR should not be integer");
      assertFalse(WitPrimitiveType.STRING.isInteger(), "STRING should not be integer");

      LOGGER.info("Integer type identification verified");
    }

    @Test
    @DisplayName("Should correctly identify floating point types")
    void shouldCorrectlyIdentifyFloatingPointTypes() {
      LOGGER.info("Testing floating point type identification");

      assertTrue(WitPrimitiveType.FLOAT32.isFloatingPoint(), "FLOAT32 should be floating point");
      assertTrue(WitPrimitiveType.FLOAT64.isFloatingPoint(), "FLOAT64 should be floating point");

      assertFalse(WitPrimitiveType.S32.isFloatingPoint(), "S32 should not be floating point");
      assertFalse(WitPrimitiveType.BOOL.isFloatingPoint(), "BOOL should not be floating point");
      assertFalse(WitPrimitiveType.STRING.isFloatingPoint(), "STRING should not be floating point");

      LOGGER.info("Floating point type identification verified");
    }

    @Test
    @DisplayName("Should correctly identify signed integer types")
    void shouldCorrectlyIdentifySignedIntegerTypes() {
      LOGGER.info("Testing signed integer type identification");

      assertTrue(WitPrimitiveType.S8.isSignedInteger(), "S8 should be signed");
      assertTrue(WitPrimitiveType.S16.isSignedInteger(), "S16 should be signed");
      assertTrue(WitPrimitiveType.S32.isSignedInteger(), "S32 should be signed");
      assertTrue(WitPrimitiveType.S64.isSignedInteger(), "S64 should be signed");

      assertFalse(WitPrimitiveType.U8.isSignedInteger(), "U8 should not be signed");
      assertFalse(WitPrimitiveType.U32.isSignedInteger(), "U32 should not be signed");

      LOGGER.info("Signed integer type identification verified");
    }

    @Test
    @DisplayName("Should correctly identify unsigned integer types")
    void shouldCorrectlyIdentifyUnsignedIntegerTypes() {
      LOGGER.info("Testing unsigned integer type identification");

      assertTrue(WitPrimitiveType.U8.isUnsignedInteger(), "U8 should be unsigned");
      assertTrue(WitPrimitiveType.U16.isUnsignedInteger(), "U16 should be unsigned");
      assertTrue(WitPrimitiveType.U32.isUnsignedInteger(), "U32 should be unsigned");
      assertTrue(WitPrimitiveType.U64.isUnsignedInteger(), "U64 should be unsigned");

      assertFalse(WitPrimitiveType.S8.isUnsignedInteger(), "S8 should not be unsigned");
      assertFalse(WitPrimitiveType.S32.isUnsignedInteger(), "S32 should not be unsigned");

      LOGGER.info("Unsigned integer type identification verified");
    }

    @Test
    @DisplayName("Should map to correct Java types")
    void shouldMapToCorrectJavaTypes() {
      LOGGER.info("Testing Java type mapping");

      assertEquals(boolean.class, WitPrimitiveType.BOOL.getJavaType(), "BOOL maps to boolean");
      assertEquals(byte.class, WitPrimitiveType.S8.getJavaType(), "S8 maps to byte");
      assertEquals(byte.class, WitPrimitiveType.U8.getJavaType(), "U8 maps to byte");
      assertEquals(short.class, WitPrimitiveType.S16.getJavaType(), "S16 maps to short");
      assertEquals(short.class, WitPrimitiveType.U16.getJavaType(), "U16 maps to short");
      assertEquals(int.class, WitPrimitiveType.S32.getJavaType(), "S32 maps to int");
      assertEquals(int.class, WitPrimitiveType.U32.getJavaType(), "U32 maps to int");
      assertEquals(long.class, WitPrimitiveType.S64.getJavaType(), "S64 maps to long");
      assertEquals(long.class, WitPrimitiveType.U64.getJavaType(), "U64 maps to long");
      assertEquals(float.class, WitPrimitiveType.FLOAT32.getJavaType(), "FLOAT32 maps to float");
      assertEquals(double.class, WitPrimitiveType.FLOAT64.getJavaType(), "FLOAT64 maps to double");
      assertEquals(char.class, WitPrimitiveType.CHAR.getJavaType(), "CHAR maps to char");
      assertEquals(String.class, WitPrimitiveType.STRING.getJavaType(), "STRING maps to String");

      LOGGER.info("Java type mapping verified");
    }

    @Test
    @DisplayName("Should parse type names from string")
    void shouldParseTypeNamesFromString() {
      LOGGER.info("Testing string to enum conversion");

      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("bool"), "bool -> BOOL");
      assertEquals(
          WitPrimitiveType.BOOL, WitPrimitiveType.fromString("boolean"), "boolean -> BOOL");
      assertEquals(WitPrimitiveType.S8, WitPrimitiveType.fromString("s8"), "s8 -> S8");
      assertEquals(WitPrimitiveType.S8, WitPrimitiveType.fromString("i8"), "i8 -> S8");
      assertEquals(WitPrimitiveType.U8, WitPrimitiveType.fromString("u8"), "u8 -> U8");
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("s32"), "s32 -> S32");
      assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("i32"), "i32 -> S32");
      assertEquals(WitPrimitiveType.FLOAT32, WitPrimitiveType.fromString("f32"), "f32 -> FLOAT32");
      assertEquals(
          WitPrimitiveType.FLOAT32, WitPrimitiveType.fromString("float32"), "float32 -> FLOAT32");
      assertEquals(WitPrimitiveType.CHAR, WitPrimitiveType.fromString("char"), "char -> CHAR");
      assertEquals(
          WitPrimitiveType.STRING, WitPrimitiveType.fromString("string"), "string -> STRING");

      // Case insensitive
      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("BOOL"), "BOOL -> BOOL");
      assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("Bool"), "Bool -> BOOL");

      LOGGER.info("String to enum conversion verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid type names")
    void shouldThrowExceptionForInvalidTypeNames() {
      LOGGER.info("Testing invalid type name handling");

      assertThrows(
          IllegalArgumentException.class,
          () -> WitPrimitiveType.fromString(null),
          "Should throw for null");
      assertThrows(
          IllegalArgumentException.class,
          () -> WitPrimitiveType.fromString(""),
          "Should throw for empty string");
      assertThrows(
          IllegalArgumentException.class,
          () -> WitPrimitiveType.fromString("invalid"),
          "Should throw for unknown type");

      LOGGER.info("Invalid type name handling verified");
    }

    @Test
    @DisplayName("Should have correct WIT type names")
    void shouldHaveCorrectWitTypeNames() {
      LOGGER.info("Testing WIT type names");

      assertEquals("bool", WitPrimitiveType.BOOL.getWitTypeName(), "BOOL WIT name");
      assertEquals("s8", WitPrimitiveType.S8.getWitTypeName(), "S8 WIT name");
      assertEquals("u8", WitPrimitiveType.U8.getWitTypeName(), "U8 WIT name");
      assertEquals("s32", WitPrimitiveType.S32.getWitTypeName(), "S32 WIT name");
      assertEquals("float32", WitPrimitiveType.FLOAT32.getWitTypeName(), "FLOAT32 WIT name");
      assertEquals("string", WitPrimitiveType.STRING.getWitTypeName(), "STRING WIT name");

      LOGGER.info("WIT type names verified");
    }
  }

  @Nested
  @DisplayName("WitTypeCategory Enum Tests")
  class WitTypeCategoryEnumTests {

    @Test
    @DisplayName("Should have all expected type categories")
    void shouldHaveAllExpectedTypeCategories() {
      LOGGER.info("Testing WitTypeCategory enum values");

      WitTypeCategory[] categories = WitTypeCategory.values();
      assertEquals(10, categories.length, "Should have 10 type categories");

      assertNotNull(WitTypeCategory.PRIMITIVE, "PRIMITIVE should exist");
      assertNotNull(WitTypeCategory.RECORD, "RECORD should exist");
      assertNotNull(WitTypeCategory.VARIANT, "VARIANT should exist");
      assertNotNull(WitTypeCategory.ENUM, "ENUM should exist");
      assertNotNull(WitTypeCategory.FLAGS, "FLAGS should exist");
      assertNotNull(WitTypeCategory.LIST, "LIST should exist");
      assertNotNull(WitTypeCategory.OPTION, "OPTION should exist");
      assertNotNull(WitTypeCategory.RESULT, "RESULT should exist");
      assertNotNull(WitTypeCategory.TUPLE, "TUPLE should exist");
      assertNotNull(WitTypeCategory.RESOURCE, "RESOURCE should exist");

      LOGGER.info("WitTypeCategory enum values verified: " + categories.length);
    }

    @Test
    @DisplayName("Should convert to and from string")
    void shouldConvertToAndFromString() {
      LOGGER.info("Testing WitTypeCategory string conversion");

      for (WitTypeCategory category : WitTypeCategory.values()) {
        String name = category.name();
        WitTypeCategory fromName = WitTypeCategory.valueOf(name);
        assertEquals(category, fromName, "valueOf should return same category");
      }

      LOGGER.info("WitTypeCategory string conversion verified");
    }
  }

  @Nested
  @DisplayName("WitTypeKind Abstract Class Tests")
  class WitTypeKindAbstractClassTests {

    @Test
    @DisplayName("Should verify WitTypeKind is abstract")
    void shouldVerifyWitTypeKindIsAbstract() {
      LOGGER.info("Testing WitTypeKind class structure");

      assertTrue(
          Modifier.isAbstract(WitTypeKind.class.getModifiers()), "WitTypeKind should be abstract");
      assertFalse(WitTypeKind.class.isInterface(), "WitTypeKind should not be an interface");

      LOGGER.info("WitTypeKind class structure verified");
    }

    @Test
    @DisplayName("Should have abstract methods")
    void shouldHaveAbstractMethods() throws Exception {
      LOGGER.info("Testing WitTypeKind abstract methods");

      Method isCompatibleWith = WitTypeKind.class.getMethod("isCompatibleWith", WitTypeKind.class);
      assertNotNull(isCompatibleWith, "isCompatibleWith method should exist");

      Method getSizeBytes = WitTypeKind.class.getMethod("getSizeBytes");
      assertNotNull(getSizeBytes, "getSizeBytes method should exist");

      Method isPrimitive = WitTypeKind.class.getMethod("isPrimitive");
      assertNotNull(isPrimitive, "isPrimitive method should exist");

      Method isComposite = WitTypeKind.class.getMethod("isComposite");
      assertNotNull(isComposite, "isComposite method should exist");

      Method isResource = WitTypeKind.class.getMethod("isResource");
      assertNotNull(isResource, "isResource method should exist");

      Method getCategory = WitTypeKind.class.getMethod("getCategory");
      assertNotNull(getCategory, "getCategory method should exist");

      LOGGER.info("WitTypeKind abstract methods verified");
    }

    @Test
    @DisplayName("Should create primitive type kind")
    void shouldCreatePrimitiveTypeKind() {
      LOGGER.info("Testing WitTypeKind.primitive()");

      WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.S32);
      assertNotNull(kind, "Primitive kind should not be null");
      assertTrue(kind.isPrimitive(), "Should be primitive");
      assertFalse(kind.isComposite(), "Should not be composite");
      assertFalse(kind.isResource(), "Should not be resource");
      assertEquals(WitTypeCategory.PRIMITIVE, kind.getCategory(), "Should be PRIMITIVE category");

      Optional<Integer> sizeBytes = kind.getSizeBytes();
      assertTrue(sizeBytes.isPresent(), "Size should be present");
      assertEquals(4, sizeBytes.get(), "S32 should be 4 bytes");

      LOGGER.info("WitTypeKind.primitive() verified");
    }

    @Test
    @DisplayName("Should create record type kind")
    void shouldCreateRecordTypeKind() {
      LOGGER.info("Testing WitTypeKind.record()");

      WitType fieldType = WitType.createS32();
      WitTypeKind kind = WitTypeKind.record(Map.of("x", fieldType, "y", fieldType));

      assertNotNull(kind, "Record kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertFalse(kind.isResource(), "Should not be resource");
      assertEquals(WitTypeCategory.RECORD, kind.getCategory(), "Should be RECORD category");

      LOGGER.info("WitTypeKind.record() verified");
    }

    @Test
    @DisplayName("Should create variant type kind")
    void shouldCreateVariantTypeKind() {
      LOGGER.info("Testing WitTypeKind.variant()");

      WitType intType = WitType.createS32();
      WitTypeKind kind =
          WitTypeKind.variant(Map.of("some", Optional.of(intType), "none", Optional.empty()));

      assertNotNull(kind, "Variant kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertEquals(WitTypeCategory.VARIANT, kind.getCategory(), "Should be VARIANT category");

      LOGGER.info("WitTypeKind.variant() verified");
    }

    @Test
    @DisplayName("Should create enum type kind")
    void shouldCreateEnumTypeKind() {
      LOGGER.info("Testing WitTypeKind.enumType()");

      WitTypeKind kind = WitTypeKind.enumType(List.of("red", "green", "blue"));

      assertNotNull(kind, "Enum kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertEquals(WitTypeCategory.ENUM, kind.getCategory(), "Should be ENUM category");

      LOGGER.info("WitTypeKind.enumType() verified");
    }

    @Test
    @DisplayName("Should create flags type kind")
    void shouldCreateFlagsTypeKind() {
      LOGGER.info("Testing WitTypeKind.flags()");

      WitTypeKind kind = WitTypeKind.flags(List.of("read", "write", "execute"));

      assertNotNull(kind, "Flags kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertEquals(WitTypeCategory.FLAGS, kind.getCategory(), "Should be FLAGS category");

      LOGGER.info("WitTypeKind.flags() verified");
    }

    @Test
    @DisplayName("Should create list type kind")
    void shouldCreateListTypeKind() {
      LOGGER.info("Testing WitTypeKind.list()");

      WitType elementType = WitType.createU8();
      WitTypeKind kind = WitTypeKind.list(elementType);

      assertNotNull(kind, "List kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertEquals(WitTypeCategory.LIST, kind.getCategory(), "Should be LIST category");

      LOGGER.info("WitTypeKind.list() verified");
    }

    @Test
    @DisplayName("Should create option type kind")
    void shouldCreateOptionTypeKind() {
      LOGGER.info("Testing WitTypeKind.option()");

      WitType innerType = WitType.createString();
      WitTypeKind kind = WitTypeKind.option(innerType);

      assertNotNull(kind, "Option kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertEquals(WitTypeCategory.OPTION, kind.getCategory(), "Should be OPTION category");

      LOGGER.info("WitTypeKind.option() verified");
    }

    @Test
    @DisplayName("Should create result type kind")
    void shouldCreateResultTypeKind() {
      LOGGER.info("Testing WitTypeKind.result()");

      WitType okType = WitType.createS32();
      WitType errType = WitType.createString();
      WitTypeKind kind = WitTypeKind.result(Optional.of(okType), Optional.of(errType));

      assertNotNull(kind, "Result kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertEquals(WitTypeCategory.RESULT, kind.getCategory(), "Should be RESULT category");

      LOGGER.info("WitTypeKind.result() verified");
    }

    @Test
    @DisplayName("Should create tuple type kind")
    void shouldCreateTupleTypeKind() {
      LOGGER.info("Testing WitTypeKind.tuple()");

      WitTypeKind kind = WitTypeKind.tuple(List.of(WitType.createS32(), WitType.createString()));

      assertNotNull(kind, "Tuple kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertTrue(kind.isComposite(), "Should be composite");
      assertEquals(WitTypeCategory.TUPLE, kind.getCategory(), "Should be TUPLE category");

      LOGGER.info("WitTypeKind.tuple() verified");
    }

    @Test
    @DisplayName("Should create resource type kind")
    void shouldCreateResourceTypeKind() {
      LOGGER.info("Testing WitTypeKind.resource()");

      WitTypeKind kind = WitTypeKind.resource("file-handle");

      assertNotNull(kind, "Resource kind should not be null");
      assertFalse(kind.isPrimitive(), "Should not be primitive");
      assertFalse(kind.isComposite(), "Should not be composite");
      assertTrue(kind.isResource(), "Should be resource");
      assertEquals(WitTypeCategory.RESOURCE, kind.getCategory(), "Should be RESOURCE category");

      LOGGER.info("WitTypeKind.resource() verified");
    }
  }

  @Nested
  @DisplayName("WitType Class Tests")
  class WitTypeClassTests {

    @Test
    @DisplayName("Should verify WitType is a final class")
    void shouldVerifyWitTypeIsFinalClass() {
      LOGGER.info("Testing WitType class structure");

      assertTrue(Modifier.isFinal(WitType.class.getModifiers()), "WitType should be final");
      assertFalse(WitType.class.isInterface(), "WitType should not be an interface");

      LOGGER.info("WitType class structure verified");
    }

    @Test
    @DisplayName("Should create primitive types via factory methods")
    void shouldCreatePrimitiveTypesViaFactoryMethods() {
      LOGGER.info("Testing WitType primitive factory methods");

      assertNotNull(WitType.createBool(), "createBool should work");
      assertTrue(WitType.createBool().isPrimitive(), "Bool should be primitive");

      assertNotNull(WitType.createS8(), "createS8 should work");
      assertNotNull(WitType.createU8(), "createU8 should work");
      assertNotNull(WitType.createS16(), "createS16 should work");
      assertNotNull(WitType.createU16(), "createU16 should work");
      assertNotNull(WitType.createS32(), "createS32 should work");
      assertNotNull(WitType.createU32(), "createU32 should work");
      assertNotNull(WitType.createS64(), "createS64 should work");
      assertNotNull(WitType.createU64(), "createU64 should work");
      assertNotNull(WitType.createFloat32(), "createFloat32 should work");
      assertNotNull(WitType.createFloat64(), "createFloat64 should work");
      assertNotNull(WitType.createChar(), "createChar should work");
      assertNotNull(WitType.createString(), "createString should work");

      LOGGER.info("WitType primitive factory methods verified");
    }

    @Test
    @DisplayName("Should create primitive type from WitPrimitiveType")
    void shouldCreatePrimitiveTypeFromWitPrimitiveType() {
      LOGGER.info("Testing WitType.primitive()");

      for (WitPrimitiveType primitive : WitPrimitiveType.values()) {
        WitType type = WitType.primitive(primitive);
        assertNotNull(type, "Type for " + primitive + " should not be null");
        assertTrue(type.isPrimitive(), primitive + " should be primitive");
        assertEquals(
            primitive.name().toLowerCase(),
            type.getName(),
            "Name should be lowercase primitive name");
      }

      LOGGER.info("WitType.primitive() verified for all primitives");
    }

    @Test
    @DisplayName("Should create record type")
    void shouldCreateRecordType() {
      LOGGER.info("Testing WitType.record()");

      WitType pointType =
          WitType.record(
              "point", Map.of("x", WitType.createFloat32(), "y", WitType.createFloat32()));

      assertNotNull(pointType, "Record type should not be null");
      assertEquals("point", pointType.getName(), "Name should be 'point'");
      assertFalse(pointType.isPrimitive(), "Record should not be primitive");
      assertTrue(pointType.isComposite(), "Record should be composite");
      assertFalse(pointType.isResource(), "Record should not be resource");

      Map<String, Object> metadata = pointType.getMetadata();
      assertEquals(2, metadata.get("fieldCount"), "Should have 2 fields");

      LOGGER.info("WitType.record() verified");
    }

    @Test
    @DisplayName("Should create variant type")
    void shouldCreateVariantType() {
      LOGGER.info("Testing WitType.variant()");

      WitType optionalInt =
          WitType.variant(
              "optional-int",
              Map.of("some", Optional.of(WitType.createS32()), "none", Optional.empty()));

      assertNotNull(optionalInt, "Variant type should not be null");
      assertEquals("optional-int", optionalInt.getName(), "Name should be 'optional-int'");
      assertFalse(optionalInt.isPrimitive(), "Variant should not be primitive");
      assertTrue(optionalInt.isComposite(), "Variant should be composite");

      Map<String, Object> metadata = optionalInt.getMetadata();
      assertEquals(2, metadata.get("caseCount"), "Should have 2 cases");

      LOGGER.info("WitType.variant() verified");
    }

    @Test
    @DisplayName("Should create enum type")
    void shouldCreateEnumType() {
      LOGGER.info("Testing WitType.enumType()");

      WitType colorEnum = WitType.enumType("color", List.of("red", "green", "blue"));

      assertNotNull(colorEnum, "Enum type should not be null");
      assertEquals("color", colorEnum.getName(), "Name should be 'color'");
      assertFalse(colorEnum.isPrimitive(), "Enum should not be primitive");
      assertTrue(colorEnum.isComposite(), "Enum should be composite");

      Map<String, Object> metadata = colorEnum.getMetadata();
      assertEquals(3, metadata.get("valueCount"), "Should have 3 values");

      LOGGER.info("WitType.enumType() verified");
    }

    @Test
    @DisplayName("Should create flags type")
    void shouldCreateFlagsType() {
      LOGGER.info("Testing WitType.flags()");

      WitType permissions = WitType.flags("permissions", List.of("read", "write", "execute"));

      assertNotNull(permissions, "Flags type should not be null");
      assertEquals("permissions", permissions.getName(), "Name should be 'permissions'");
      assertFalse(permissions.isPrimitive(), "Flags should not be primitive");
      assertTrue(permissions.isComposite(), "Flags should be composite");

      Map<String, Object> metadata = permissions.getMetadata();
      assertEquals(3, metadata.get("flagCount"), "Should have 3 flags");

      LOGGER.info("WitType.flags() verified");
    }

    @Test
    @DisplayName("Should create list type")
    void shouldCreateListType() {
      LOGGER.info("Testing WitType.list()");

      WitType byteList = WitType.list(WitType.createU8());

      assertNotNull(byteList, "List type should not be null");
      assertEquals("list<u8>", byteList.getName(), "Name should be 'list<u8>'");
      assertFalse(byteList.isPrimitive(), "List should not be primitive");
      assertTrue(byteList.isComposite(), "List should be composite");

      Map<String, Object> metadata = byteList.getMetadata();
      assertEquals("u8", metadata.get("elementType"), "Element type should be u8");

      LOGGER.info("WitType.list() verified");
    }

    @Test
    @DisplayName("Should create option type")
    void shouldCreateOptionType() {
      LOGGER.info("Testing WitType.option()");

      WitType optionalString = WitType.option(WitType.createString());

      assertNotNull(optionalString, "Option type should not be null");
      assertEquals("option<string>", optionalString.getName(), "Name should be 'option<string>'");
      assertFalse(optionalString.isPrimitive(), "Option should not be primitive");
      assertTrue(optionalString.isComposite(), "Option should be composite");

      Map<String, Object> metadata = optionalString.getMetadata();
      assertEquals("string", metadata.get("innerType"), "Inner type should be string");

      LOGGER.info("WitType.option() verified");
    }

    @Test
    @DisplayName("Should create result type")
    void shouldCreateResultType() {
      LOGGER.info("Testing WitType.result()");

      WitType result =
          WitType.result(Optional.of(WitType.createS32()), Optional.of(WitType.createString()));

      assertNotNull(result, "Result type should not be null");
      assertTrue(result.getName().startsWith("result<"), "Name should start with 'result<'");
      assertFalse(result.isPrimitive(), "Result should not be primitive");
      assertTrue(result.isComposite(), "Result should be composite");

      // Test with empty ok type
      WitType voidResult = WitType.result(Optional.empty(), Optional.of(WitType.createString()));
      assertNotNull(voidResult, "Void result type should not be null");

      // Test with empty error type
      WitType infallibleResult = WitType.result(Optional.of(WitType.createS32()), Optional.empty());
      assertNotNull(infallibleResult, "Infallible result type should not be null");

      LOGGER.info("WitType.result() verified");
    }

    @Test
    @DisplayName("Should create tuple type")
    void shouldCreateTupleType() {
      LOGGER.info("Testing WitType.tuple()");

      WitType tuple =
          WitType.tuple(List.of(WitType.createS32(), WitType.createString(), WitType.createBool()));

      assertNotNull(tuple, "Tuple type should not be null");
      assertTrue(tuple.getName().startsWith("tuple<"), "Name should start with 'tuple<'");
      assertFalse(tuple.isPrimitive(), "Tuple should not be primitive");
      assertTrue(tuple.isComposite(), "Tuple should be composite");

      Map<String, Object> metadata = tuple.getMetadata();
      assertEquals(3, metadata.get("elementCount"), "Should have 3 elements");

      // Test varargs version
      WitType pairTuple = WitType.tuple(WitType.createS32(), WitType.createS32());
      assertNotNull(pairTuple, "Pair tuple should not be null");

      LOGGER.info("WitType.tuple() verified");
    }

    @Test
    @DisplayName("Should create resource type")
    void shouldCreateResourceType() {
      LOGGER.info("Testing WitType.resource()");

      WitType fileHandle = WitType.resource("file-handle", "wasi:filesystem/file");

      assertNotNull(fileHandle, "Resource type should not be null");
      assertEquals("file-handle", fileHandle.getName(), "Name should be 'file-handle'");
      assertFalse(fileHandle.isPrimitive(), "Resource should not be primitive");
      assertFalse(fileHandle.isComposite(), "Resource should not be composite");
      assertTrue(fileHandle.isResource(), "Resource should be resource");

      Map<String, Object> metadata = fileHandle.getMetadata();
      assertEquals("wasi:filesystem/file", metadata.get("resourceId"), "Resource ID should match");

      LOGGER.info("WitType.resource() verified");
    }

    @Test
    @DisplayName("Should get size bytes for primitive types")
    void shouldGetSizeBytesForPrimitiveTypes() {
      LOGGER.info("Testing WitType.getSizeBytes()");

      assertEquals(Optional.of(1), WitType.createBool().getSizeBytes(), "Bool should be 1 byte");
      assertEquals(Optional.of(1), WitType.createS8().getSizeBytes(), "S8 should be 1 byte");
      assertEquals(Optional.of(4), WitType.createS32().getSizeBytes(), "S32 should be 4 bytes");
      assertEquals(Optional.of(8), WitType.createS64().getSizeBytes(), "S64 should be 8 bytes");
      assertEquals(
          Optional.of(4), WitType.createFloat32().getSizeBytes(), "Float32 should be 4 bytes");
      assertEquals(
          Optional.of(8), WitType.createFloat64().getSizeBytes(), "Float64 should be 8 bytes");

      // String is variable size
      Optional<Integer> stringSize = WitType.createString().getSizeBytes();
      assertTrue(
          stringSize.isEmpty() || stringSize.get() < 0, "String should be variable size or empty");

      LOGGER.info("WitType.getSizeBytes() verified");
    }

    @Test
    @DisplayName("Should check type compatibility")
    void shouldCheckTypeCompatibility() {
      LOGGER.info("Testing WitType.isCompatibleWith()");

      WitType s32a = WitType.createS32();
      WitType s32b = WitType.createS32();
      WitType s64 = WitType.createS64();

      assertTrue(s32a.isCompatibleWith(s32a), "Type should be compatible with itself");
      assertTrue(s32a.isCompatibleWith(s32b), "Same types should be compatible");
      assertFalse(s32a.isCompatibleWith(s64), "Different types should not be compatible");
      assertFalse(s32a.isCompatibleWith(null), "Should handle null");

      LOGGER.info("WitType.isCompatibleWith() verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing WitType equals implementation");

      WitType s32a = WitType.createS32();
      WitType s32b = WitType.createS32();
      WitType s64 = WitType.createS64();

      assertEquals(s32a, s32a, "Type should equal itself");
      assertEquals(s32a, s32b, "Same types should be equal");
      assertNotEquals(s32a, s64, "Different types should not be equal");
      assertNotEquals(s32a, null, "Type should not equal null");
      assertNotEquals(s32a, "s32", "Type should not equal string");

      LOGGER.info("WitType equals implementation verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing WitType hashCode implementation");

      WitType s32a = WitType.createS32();
      WitType s32b = WitType.createS32();

      assertEquals(s32a.hashCode(), s32b.hashCode(), "Equal types should have same hashCode");

      LOGGER.info("WitType hashCode implementation verified");
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      LOGGER.info("Testing WitType toString implementation");

      WitType s32 = WitType.createS32();
      String str = s32.toString();

      assertNotNull(str, "toString should not return null");
      assertFalse(str.isEmpty(), "toString should not return empty string");
      assertTrue(str.contains("s32"), "toString should contain type name");

      LOGGER.info("WitType toString: " + str);
      LOGGER.info("WitType toString implementation verified");
    }

    @Test
    @DisplayName("Should get documentation")
    void shouldGetDocumentation() {
      LOGGER.info("Testing WitType documentation");

      WitType s32 = WitType.createS32();
      Optional<String> doc = s32.getDocumentation();

      assertNotNull(doc, "Documentation should not be null");
      // Primitive types typically don't have documentation
      assertFalse(doc.isPresent(), "Primitive types typically have no documentation");

      LOGGER.info("WitType documentation verified");
    }
  }

  @Nested
  @DisplayName("WitType Nested Types Tests")
  class WitTypeNestedTypesTests {

    @Test
    @DisplayName("Should create nested list of lists")
    void shouldCreateNestedListOfLists() {
      LOGGER.info("Testing nested list type");

      WitType innerList = WitType.list(WitType.createU8());
      WitType outerList = WitType.list(innerList);

      assertNotNull(outerList, "Nested list should not be null");
      assertEquals("list<list<u8>>", outerList.getName(), "Name should reflect nesting");
      assertTrue(outerList.isComposite(), "Should be composite");

      LOGGER.info("Nested list type verified");
    }

    @Test
    @DisplayName("Should create option of option")
    void shouldCreateOptionOfOption() {
      LOGGER.info("Testing option of option type");

      WitType innerOption = WitType.option(WitType.createS32());
      WitType outerOption = WitType.option(innerOption);

      assertNotNull(outerOption, "Nested option should not be null");
      assertEquals("option<option<s32>>", outerOption.getName(), "Name should reflect nesting");

      LOGGER.info("Option of option type verified");
    }

    @Test
    @DisplayName("Should create complex record with nested types")
    void shouldCreateComplexRecordWithNestedTypes() {
      LOGGER.info("Testing complex record type");

      WitType point =
          WitType.record("point", Map.of("x", WitType.createS32(), "y", WitType.createS32()));

      WitType line =
          WitType.record(
              "line",
              Map.of(
                  "start", point,
                  "end", point,
                  "color", WitType.option(WitType.createString())));

      assertNotNull(line, "Complex record should not be null");
      assertEquals("line", line.getName(), "Name should be 'line'");
      assertTrue(line.isComposite(), "Should be composite");

      LOGGER.info("Complex record type verified");
    }

    @Test
    @DisplayName("Should create tuple with mixed types")
    void shouldCreateTupleWithMixedTypes() {
      LOGGER.info("Testing tuple with mixed types");

      WitType mixed =
          WitType.tuple(
              WitType.createS32(),
              WitType.createString(),
              WitType.option(WitType.createBool()),
              WitType.list(WitType.createU8()));

      assertNotNull(mixed, "Mixed tuple should not be null");
      assertTrue(mixed.isComposite(), "Should be composite");
      assertEquals(4, mixed.getMetadata().get("elementCount"), "Should have 4 elements");

      LOGGER.info("Tuple with mixed types verified");
    }

    @Test
    @DisplayName("Should create result with complex types")
    void shouldCreateResultWithComplexTypes() {
      LOGGER.info("Testing result with complex types");

      WitType okType =
          WitType.record(
              "success", Map.of("value", WitType.createS32(), "message", WitType.createString()));

      WitType errType =
          WitType.variant(
              "error",
              Map.of(
                  "not-found", Optional.empty(),
                  "permission-denied", Optional.of(WitType.createString()),
                  "io-error", Optional.of(WitType.createS32())));

      WitType complexResult = WitType.result(Optional.of(okType), Optional.of(errType));

      assertNotNull(complexResult, "Complex result should not be null");
      assertTrue(complexResult.isComposite(), "Should be composite");

      LOGGER.info("Result with complex types verified");
    }
  }
}
