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

package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.wit.WitType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WitType with native library loaded.
 *
 * <p>These tests verify that WitType creation and property methods work correctly when the native
 * library is loaded and active. This ensures that native operations don't interfere with Java-side
 * WIT type handling.
 *
 * @since 1.0.0
 */
@DisplayName("WitValue Native Integration Tests")
class WitValueNativeIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WitValueNativeIntegrationTest.class.getName());

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WitValue integration tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @Nested
  @DisplayName("WitType Primitive Creation Tests")
  class WitTypePrimitiveCreationTests {

    @Test
    @DisplayName("should create boolean type")
    void shouldCreateBooleanType() {
      LOGGER.info("Testing boolean WitType creation");

      final WitType boolType = WitType.createBool();

      assertNotNull(boolType, "Type should not be null");
      assertEquals("bool", boolType.getName(), "Name should be 'bool'");
      assertTrue(boolType.isPrimitive(), "Should be primitive");
      assertFalse(boolType.isComposite(), "Should not be composite");
      assertFalse(boolType.isResource(), "Should not be resource");

      LOGGER.info("Boolean type created: " + boolType);
    }

    @Test
    @DisplayName("should create signed integer types")
    void shouldCreateSignedIntegerTypes() {
      LOGGER.info("Testing signed integer WitType creation");

      final WitType s8Type = WitType.createS8();
      final WitType s16Type = WitType.createS16();
      final WitType s32Type = WitType.createS32();
      final WitType s64Type = WitType.createS64();

      assertNotNull(s8Type, "S8 should not be null");
      assertNotNull(s16Type, "S16 should not be null");
      assertNotNull(s32Type, "S32 should not be null");
      assertNotNull(s64Type, "S64 should not be null");

      assertEquals("s8", s8Type.getName());
      assertEquals("s16", s16Type.getName());
      assertEquals("s32", s32Type.getName());
      assertEquals("s64", s64Type.getName());

      LOGGER.info("Signed integer types created successfully");
    }

    @Test
    @DisplayName("should create unsigned integer types")
    void shouldCreateUnsignedIntegerTypes() {
      LOGGER.info("Testing unsigned integer WitType creation");

      final WitType u8Type = WitType.createU8();
      final WitType u16Type = WitType.createU16();
      final WitType u32Type = WitType.createU32();
      final WitType u64Type = WitType.createU64();

      assertNotNull(u8Type, "U8 should not be null");
      assertNotNull(u16Type, "U16 should not be null");
      assertNotNull(u32Type, "U32 should not be null");
      assertNotNull(u64Type, "U64 should not be null");

      assertEquals("u8", u8Type.getName());
      assertEquals("u16", u16Type.getName());
      assertEquals("u32", u32Type.getName());
      assertEquals("u64", u64Type.getName());

      LOGGER.info("Unsigned integer types created successfully");
    }

    @Test
    @DisplayName("should create floating point types")
    void shouldCreateFloatingPointTypes() {
      LOGGER.info("Testing floating point WitType creation");

      final WitType float32Type = WitType.createFloat32();
      final WitType float64Type = WitType.createFloat64();

      assertNotNull(float32Type, "Float32 should not be null");
      assertNotNull(float64Type, "Float64 should not be null");

      assertEquals("float32", float32Type.getName());
      assertEquals("float64", float64Type.getName());

      assertTrue(float32Type.isPrimitive(), "Float32 should be primitive");
      assertTrue(float64Type.isPrimitive(), "Float64 should be primitive");

      LOGGER.info("Floating point types created successfully");
    }

    @Test
    @DisplayName("should create char and string types")
    void shouldCreateCharAndStringTypes() {
      LOGGER.info("Testing char and string WitType creation");

      final WitType charType = WitType.createChar();
      final WitType stringType = WitType.createString();

      assertNotNull(charType, "Char should not be null");
      assertNotNull(stringType, "String should not be null");

      assertEquals("char", charType.getName());
      assertEquals("string", stringType.getName());

      LOGGER.info("Char and string types created successfully");
    }
  }

  @Nested
  @DisplayName("WitType Composite Creation Tests")
  class WitTypeCompositeCreationTests {

    @Test
    @DisplayName("should create list type")
    void shouldCreateListType() {
      LOGGER.info("Testing list WitType creation");

      final WitType s32Type = WitType.createS32();
      final WitType listType = WitType.list(s32Type);

      assertNotNull(listType, "List type should not be null");
      assertTrue(listType.isComposite(), "List should be composite");
      assertFalse(listType.isPrimitive(), "List should not be primitive");

      LOGGER.info("List type created successfully");
    }

    @Test
    @DisplayName("should create option type")
    void shouldCreateOptionType() {
      LOGGER.info("Testing option WitType creation");

      final WitType stringType = WitType.createString();
      final WitType optionType = WitType.option(stringType);

      assertNotNull(optionType, "Option type should not be null");
      assertTrue(optionType.isComposite(), "Option should be composite");

      LOGGER.info("Option type created successfully");
    }

    @Test
    @DisplayName("should create result type")
    void shouldCreateResultType() {
      LOGGER.info("Testing result WitType creation");

      final WitType s32Type = WitType.createS32();
      final WitType stringType = WitType.createString();
      final WitType resultType = WitType.result(Optional.of(s32Type), Optional.of(stringType));

      assertNotNull(resultType, "Result type should not be null");
      assertTrue(resultType.isComposite(), "Result should be composite");

      LOGGER.info("Result type created successfully");
    }

    @Test
    @DisplayName("should create tuple type")
    void shouldCreateTupleType() {
      LOGGER.info("Testing tuple WitType creation");

      final WitType s32Type = WitType.createS32();
      final WitType stringType = WitType.createString();
      final WitType tupleType = WitType.tuple(Arrays.asList(s32Type, stringType));

      assertNotNull(tupleType, "Tuple type should not be null");
      assertTrue(tupleType.isComposite(), "Tuple should be composite");

      LOGGER.info("Tuple type created successfully");
    }

    @Test
    @DisplayName("should create tuple type with varargs")
    void shouldCreateTupleTypeWithVarargs() {
      LOGGER.info("Testing tuple WitType creation with varargs");

      final WitType boolType = WitType.createBool();
      final WitType s32Type = WitType.createS32();
      final WitType stringType = WitType.createString();
      final WitType tupleType = WitType.tuple(boolType, s32Type, stringType);

      assertNotNull(tupleType, "Tuple type should not be null");
      assertTrue(tupleType.isComposite(), "Tuple should be composite");

      LOGGER.info("Tuple type with varargs created successfully");
    }
  }

  @Nested
  @DisplayName("WitType Named Type Creation Tests")
  class WitTypeNamedTypeCreationTests {

    @Test
    @DisplayName("should create record type")
    void shouldCreateRecordType() {
      LOGGER.info("Testing record WitType creation");

      final Map<String, WitType> fields = new LinkedHashMap<>();
      fields.put("id", WitType.createS32());
      fields.put("name", WitType.createString());

      final WitType recordType = WitType.record("Person", fields);

      assertNotNull(recordType, "Record type should not be null");
      assertTrue(recordType.isComposite(), "Record should be composite");

      LOGGER.info("Record type created successfully");
    }

    @Test
    @DisplayName("should create enum type")
    void shouldCreateEnumType() {
      LOGGER.info("Testing enum WitType creation");

      final List<String> values = Arrays.asList("RED", "GREEN", "BLUE");
      final WitType enumType = WitType.enumType("Color", values);

      assertNotNull(enumType, "Enum type should not be null");
      assertTrue(enumType.isComposite(), "Enum should be composite");

      LOGGER.info("Enum type created successfully");
    }

    @Test
    @DisplayName("should create variant type")
    void shouldCreateVariantType() {
      LOGGER.info("Testing variant WitType creation");

      final Map<String, Optional<WitType>> cases = new LinkedHashMap<>();
      cases.put("Some", Optional.of(WitType.createS32()));
      cases.put("None", Optional.empty());

      final WitType variantType = WitType.variant("MaybeInt", cases);

      assertNotNull(variantType, "Variant type should not be null");
      assertTrue(variantType.isComposite(), "Variant should be composite");

      LOGGER.info("Variant type created successfully");
    }

    @Test
    @DisplayName("should create flags type")
    void shouldCreateFlagsType() {
      LOGGER.info("Testing flags WitType creation");

      final List<String> flagNames = Arrays.asList("READ", "WRITE", "EXECUTE");
      final WitType flagsType = WitType.flags("Permissions", flagNames);

      assertNotNull(flagsType, "Flags type should not be null");
      assertTrue(flagsType.isComposite(), "Flags should be composite");

      LOGGER.info("Flags type created successfully");
    }

    @Test
    @DisplayName("should create resource type")
    void shouldCreateResourceType() {
      LOGGER.info("Testing resource WitType creation");

      final WitType resourceType = WitType.resource("FileHandle", "file-handle-resource");

      assertNotNull(resourceType, "Resource type should not be null");
      assertTrue(resourceType.isResource(), "Should be resource");
      assertFalse(resourceType.isPrimitive(), "Resource should not be primitive");
      assertFalse(resourceType.isComposite(), "Resource should not be composite");

      LOGGER.info("Resource type created successfully");
    }
  }

  @Nested
  @DisplayName("WitType Nested Type Tests")
  class WitTypeNestedTypeTests {

    @Test
    @DisplayName("should create nested list type")
    void shouldCreateNestedListType() {
      LOGGER.info("Testing nested list WitType creation");

      final WitType s32Type = WitType.createS32();
      final WitType innerList = WitType.list(s32Type);
      final WitType outerList = WitType.list(innerList);

      assertNotNull(outerList, "Nested list should not be null");
      assertTrue(outerList.isComposite(), "Nested list should be composite");

      LOGGER.info("Nested list type created successfully");
    }

    @Test
    @DisplayName("should create option of list type")
    void shouldCreateOptionOfListType() {
      LOGGER.info("Testing option of list WitType creation");

      final WitType stringType = WitType.createString();
      final WitType listType = WitType.list(stringType);
      final WitType optionType = WitType.option(listType);

      assertNotNull(optionType, "Option of list should not be null");
      assertTrue(optionType.isComposite(), "Option of list should be composite");

      LOGGER.info("Option of list type created successfully");
    }

    @Test
    @DisplayName("should create result with complex types")
    void shouldCreateResultWithComplexTypes() {
      LOGGER.info("Testing result with complex types");

      final WitType listType = WitType.list(WitType.createS32());
      final WitType stringType = WitType.createString();
      final WitType resultType = WitType.result(Optional.of(listType), Optional.of(stringType));

      assertNotNull(resultType, "Result with complex types should not be null");
      assertTrue(resultType.isComposite(), "Result should be composite");

      LOGGER.info("Result with complex types created successfully");
    }
  }

  @Nested
  @DisplayName("WitType Equality Tests")
  class WitTypeEqualityTests {

    @Test
    @DisplayName("should compare equal primitive types")
    void shouldCompareEqualPrimitiveTypes() {
      LOGGER.info("Testing primitive type equality");

      final WitType s32Type1 = WitType.createS32();
      final WitType s32Type2 = WitType.createS32();

      assertEquals(s32Type1, s32Type2, "Same primitive types should be equal");
      assertEquals(s32Type1.hashCode(), s32Type2.hashCode(), "Hash codes should match");

      LOGGER.info("Primitive type equality verified");
    }

    @Test
    @DisplayName("should have consistent toString")
    void shouldHaveConsistentToString() {
      LOGGER.info("Testing toString consistency");

      final WitType boolType = WitType.createBool();

      final String str1 = boolType.toString();
      final String str2 = boolType.toString();

      assertNotNull(str1, "toString should not return null");
      assertEquals(str1, str2, "toString should be consistent");

      LOGGER.info("toString is consistent: " + str1);
    }
  }
}
