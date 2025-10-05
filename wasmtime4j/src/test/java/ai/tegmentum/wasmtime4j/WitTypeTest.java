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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for WIT type system. */
class WitTypeTest {

  @Test
  @DisplayName("Test primitive type creation and properties")
  void testPrimitiveTypes() {
    // Test boolean type
    final WitType boolType = WitType.primitive(WitPrimitiveType.BOOL);
    assertEquals("bool", boolType.getName());
    assertTrue(boolType.isPrimitive());
    assertFalse(boolType.isComposite());
    assertFalse(boolType.isResource());
    assertEquals(Optional.of(1), boolType.getSizeBytes());

    // Test integer types
    final WitType s32Type = WitType.primitive(WitPrimitiveType.S32);
    assertEquals("s32", s32Type.getName());
    assertTrue(s32Type.isPrimitive());
    assertEquals(Optional.of(4), s32Type.getSizeBytes());

    // Test floating point types
    final WitType f64Type = WitType.primitive(WitPrimitiveType.FLOAT64);
    assertEquals("float64", f64Type.getName());
    assertTrue(f64Type.isPrimitive());
    assertEquals(Optional.of(8), f64Type.getSizeBytes());

    // Test string type (variable size)
    final WitType stringType = WitType.primitive(WitPrimitiveType.STRING);
    assertEquals("string", stringType.getName());
    assertTrue(stringType.isPrimitive());
    assertEquals(Optional.empty(), stringType.getSizeBytes());
  }

  @Test
  @DisplayName("Test record type creation and properties")
  void testRecordTypes() {
    final Map<String, WitType> fields =
        Map.of(
            "name", WitType.primitive(WitPrimitiveType.STRING),
            "age", WitType.primitive(WitPrimitiveType.U32),
            "active", WitType.primitive(WitPrimitiveType.BOOL));

    final WitType recordType = WitType.record("Person", fields);

    assertEquals("Person", recordType.getName());
    assertFalse(recordType.isPrimitive());
    assertTrue(recordType.isComposite());
    assertFalse(recordType.isResource());
    assertEquals(Optional.empty(), recordType.getSizeBytes()); // Variable size composite
    assertEquals(WitTypeCategory.RECORD, recordType.getKind().getCategory());
    assertEquals(3, recordType.getMetadata().get("fieldCount"));
  }

  @Test
  @DisplayName("Test variant type creation and properties")
  void testVariantTypes() {
    final Map<String, Optional<WitType>> cases =
        Map.of(
            "none", Optional.empty(),
            "some", Optional.of(WitType.primitive(WitPrimitiveType.S32)),
            "error", Optional.of(WitType.primitive(WitPrimitiveType.STRING)));

    final WitType variantType = WitType.variant("Result", cases);

    assertEquals("Result", variantType.getName());
    assertFalse(variantType.isPrimitive());
    assertTrue(variantType.isComposite());
    assertFalse(variantType.isResource());
    assertEquals(WitTypeCategory.VARIANT, variantType.getKind().getCategory());
    assertEquals(3, variantType.getMetadata().get("caseCount"));
  }

  @Test
  @DisplayName("Test enum type creation and properties")
  void testEnumTypes() {
    final List<String> values = List.of("red", "green", "blue");
    final WitType enumType = WitType.enumType("Color", values);

    assertEquals("Color", enumType.getName());
    assertFalse(enumType.isPrimitive());
    assertTrue(enumType.isComposite());
    assertFalse(enumType.isResource());
    assertEquals(WitTypeCategory.ENUM, enumType.getKind().getCategory());
    assertEquals(3, enumType.getMetadata().get("valueCount"));
  }

  @Test
  @DisplayName("Test flags type creation and properties")
  void testFlagsTypes() {
    final List<String> flags = List.of("read", "write", "execute");
    final WitType flagsType = WitType.flags("Permissions", flags);

    assertEquals("Permissions", flagsType.getName());
    assertFalse(flagsType.isPrimitive());
    assertTrue(flagsType.isComposite());
    assertFalse(flagsType.isResource());
    assertEquals(WitTypeCategory.FLAGS, flagsType.getKind().getCategory());
    assertEquals(3, flagsType.getMetadata().get("flagCount"));
  }

  @Test
  @DisplayName("Test list type creation and properties")
  void testListTypes() {
    final WitType elementType = WitType.primitive(WitPrimitiveType.STRING);
    final WitType listType = WitType.list(elementType);

    assertEquals("list<string>", listType.getName());
    assertFalse(listType.isPrimitive());
    assertTrue(listType.isComposite());
    assertFalse(listType.isResource());
    assertEquals(WitTypeCategory.LIST, listType.getKind().getCategory());
    assertEquals("string", listType.getMetadata().get("elementType"));
  }

  @Test
  @DisplayName("Test option type creation and properties")
  void testOptionTypes() {
    final WitType innerType = WitType.primitive(WitPrimitiveType.S32);
    final WitType optionType = WitType.option(innerType);

    assertEquals("option<s32>", optionType.getName());
    assertFalse(optionType.isPrimitive());
    assertTrue(optionType.isComposite());
    assertFalse(optionType.isResource());
    assertEquals(WitTypeCategory.OPTION, optionType.getKind().getCategory());
  }

  @Test
  @DisplayName("Test result type creation and properties")
  void testResultTypes() {
    final WitType okType = WitType.primitive(WitPrimitiveType.STRING);
    final WitType errorType = WitType.primitive(WitPrimitiveType.S32);

    // Result with both ok and error types
    final WitType resultType = WitType.result(Optional.of(okType), Optional.of(errorType));
    assertEquals("result<string, s32>", resultType.getName());
    assertFalse(resultType.isPrimitive());
    assertTrue(resultType.isComposite());
    assertFalse(resultType.isResource());
    assertEquals(WitTypeCategory.RESULT, resultType.getKind().getCategory());

    // Result with only ok type
    final WitType resultOkOnly = WitType.result(Optional.of(okType), Optional.empty());
    assertEquals("result<string, _>", resultOkOnly.getName());

    // Result with only error type
    final WitType resultErrorOnly = WitType.result(Optional.empty(), Optional.of(errorType));
    assertEquals("result<_, s32>", resultErrorOnly.getName());

    // Empty result
    final WitType resultEmpty = WitType.result(Optional.empty(), Optional.empty());
    assertEquals("result", resultEmpty.getName());
  }

  @Test
  @DisplayName("Test resource type creation and properties")
  void testResourceTypes() {
    final WitType resourceType = WitType.resource("FileHandle", "file-handle-123");

    assertEquals("FileHandle", resourceType.getName());
    assertFalse(resourceType.isPrimitive());
    assertFalse(resourceType.isComposite());
    assertTrue(resourceType.isResource());
    assertEquals(Optional.of(4), resourceType.getSizeBytes()); // Resource handle size
    assertEquals(WitTypeCategory.RESOURCE, resourceType.getKind().getCategory());
    assertEquals("file-handle-123", resourceType.getMetadata().get("resourceId"));
  }

  @Test
  @DisplayName("Test type compatibility")
  void testTypeCompatibility() {
    // Same types should be compatible
    final WitType type1 = WitType.primitive(WitPrimitiveType.S32);
    final WitType type2 = WitType.primitive(WitPrimitiveType.S32);
    assertTrue(type1.isCompatibleWith(type2));

    // Different types should not be compatible
    final WitType type3 = WitType.primitive(WitPrimitiveType.STRING);
    assertFalse(type1.isCompatibleWith(type3));

    // Records with same fields should be compatible
    final Map<String, WitType> fields = Map.of("x", WitType.primitive(WitPrimitiveType.S32));
    final WitType record1 = WitType.record("Point", fields);
    final WitType record2 = WitType.record("Point", fields);
    assertTrue(record1.isCompatibleWith(record2));

    // Records with different fields should not be compatible
    final Map<String, WitType> differentFields =
        Map.of("y", WitType.primitive(WitPrimitiveType.S32));
    final WitType record3 = WitType.record("Point", differentFields);
    assertFalse(record1.isCompatibleWith(record3));
  }

  @Test
  @DisplayName("Test type equality and hash code")
  void testTypeEqualityAndHashCode() {
    final WitType type1 = WitType.primitive(WitPrimitiveType.S32);
    final WitType type2 = WitType.primitive(WitPrimitiveType.S32);
    final WitType type3 = WitType.primitive(WitPrimitiveType.STRING);

    // Equal types
    assertEquals(type1, type2);
    assertEquals(type1.hashCode(), type2.hashCode());

    // Different types
    assertNotEquals(type1, type3);
    assertNotEquals(type1.hashCode(), type3.hashCode());

    // Null safety
    assertNotEquals(type1, null);
    assertNotEquals(null, type1);
  }

  @Test
  @DisplayName("Test complex nested types")
  void testComplexNestedTypes() {
    // Create a complex type: list<option<record{ name: string, values: list<s32> }>>
    final Map<String, WitType> recordFields =
        Map.of(
            "name", WitType.primitive(WitPrimitiveType.STRING),
            "values", WitType.list(WitType.primitive(WitPrimitiveType.S32)));

    final WitType recordType = WitType.record("DataSet", recordFields);
    final WitType optionType = WitType.option(recordType);
    final WitType listType = WitType.list(optionType);

    assertEquals("list<option<DataSet>>", listType.getName());
    assertFalse(listType.isPrimitive());
    assertTrue(listType.isComposite());
    assertEquals(WitTypeCategory.LIST, listType.getKind().getCategory());
  }

  @Test
  @DisplayName("Test type metadata handling")
  void testTypeMetadata() {
    final Map<String, Object> customMetadata =
        Map.of("version", "1.0", "deprecated", false, "maxSize", 1024);

    final WitType typeWithMetadata =
        new WitType(
            "CustomType",
            WitTypeKind.primitive(WitPrimitiveType.STRING),
            customMetadata,
            Optional.of("Custom type documentation"));

    assertEquals("CustomType", typeWithMetadata.getName());
    assertEquals(customMetadata, typeWithMetadata.getMetadata());
    assertEquals("Custom type documentation", typeWithMetadata.getDocumentation().get());
    assertTrue(typeWithMetadata.getDocumentation().isPresent());
  }

  @Test
  @DisplayName("Test WitPrimitiveType functionality")
  void testWitPrimitiveType() {
    // Test type properties
    assertTrue(WitPrimitiveType.BOOL.getSizeBytes() == 1);
    assertTrue(WitPrimitiveType.S32.isInteger());
    assertTrue(WitPrimitiveType.S32.isSignedInteger());
    assertFalse(WitPrimitiveType.U32.isSignedInteger());
    assertTrue(WitPrimitiveType.U32.isUnsignedInteger());
    assertTrue(WitPrimitiveType.FLOAT32.isFloatingPoint());
    assertTrue(WitPrimitiveType.STRING.isVariableSize());
    assertFalse(WitPrimitiveType.S32.isVariableSize());

    // Test Java type mapping
    assertEquals(boolean.class, WitPrimitiveType.BOOL.getJavaType());
    assertEquals(int.class, WitPrimitiveType.S32.getJavaType());
    assertEquals(long.class, WitPrimitiveType.S64.getJavaType());
    assertEquals(float.class, WitPrimitiveType.FLOAT32.getJavaType());
    assertEquals(double.class, WitPrimitiveType.FLOAT64.getJavaType());
    assertEquals(String.class, WitPrimitiveType.STRING.getJavaType());

    // Test string parsing
    assertEquals(WitPrimitiveType.BOOL, WitPrimitiveType.fromString("bool"));
    assertEquals(WitPrimitiveType.S32, WitPrimitiveType.fromString("s32"));
    assertEquals(WitPrimitiveType.STRING, WitPrimitiveType.fromString("string"));

    // Test invalid string parsing
    assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString("invalid"));
    assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString(""));
    assertThrows(IllegalArgumentException.class, () -> WitPrimitiveType.fromString(null));

    // Test WIT type name generation
    assertEquals("bool", WitPrimitiveType.BOOL.getWitTypeName());
    assertEquals("s32", WitPrimitiveType.S32.getWitTypeName());
    assertEquals("float32", WitPrimitiveType.FLOAT32.getWitTypeName());
  }

  @Test
  @DisplayName("Test WitTypeCategory values")
  void testWitTypeCategory() {
    assertEquals(9, WitTypeCategory.values().length);

    // Verify all expected categories exist
    assertNotNull(WitTypeCategory.PRIMITIVE);
    assertNotNull(WitTypeCategory.RECORD);
    assertNotNull(WitTypeCategory.VARIANT);
    assertNotNull(WitTypeCategory.ENUM);
    assertNotNull(WitTypeCategory.FLAGS);
    assertNotNull(WitTypeCategory.LIST);
    assertNotNull(WitTypeCategory.OPTION);
    assertNotNull(WitTypeCategory.RESULT);
    assertNotNull(WitTypeCategory.RESOURCE);
  }

  @Test
  @DisplayName("Test type kind size calculations")
  void testTypeKindSizes() {
    // Primitive types should have sizes
    final WitType boolType = WitType.primitive(WitPrimitiveType.BOOL);
    assertEquals(Optional.of(1), boolType.getKind().getSizeBytes());

    // Flags should have calculated size based on flag count
    final WitType flagsType = WitType.flags("TestFlags", List.of("flag1", "flag2", "flag3"));
    assertTrue(flagsType.getKind().getSizeBytes().isPresent());
    assertTrue(flagsType.getKind().getSizeBytes().get() >= 1);

    // Variable size types should return empty
    final WitType listType = WitType.list(WitType.primitive(WitPrimitiveType.S32));
    assertEquals(Optional.empty(), listType.getKind().getSizeBytes());

    final WitType recordType =
        WitType.record("Test", Map.of("field", WitType.primitive(WitPrimitiveType.S32)));
    assertEquals(Optional.empty(), recordType.getKind().getSizeBytes());
  }
}
