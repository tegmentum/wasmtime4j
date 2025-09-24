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

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for WIT value marshaler. */
class WitValueMarshalerTest {

  private WitValueMarshaler marshaler;

  @BeforeEach
  void setUp() {
    marshaler = new WitValueMarshaler();
  }

  @Test
  @DisplayName("Test primitive value marshaling to WIT")
  void testPrimitiveValueMarshalingToWit() throws WasmException {
    // Boolean
    final WitType boolType = WitType.primitive(WitPrimitiveType.BOOL);
    assertEquals(true, marshaler.marshalToWit(true, boolType));
    assertEquals(false, marshaler.marshalToWit(false, boolType));

    // Integers
    final WitType s32Type = WitType.primitive(WitPrimitiveType.S32);
    assertEquals(42, marshaler.marshalToWit(42, s32Type));
    assertEquals(42, marshaler.marshalToWit(42L, s32Type)); // Long to int conversion

    // Floating point
    final WitType f32Type = WitType.primitive(WitPrimitiveType.FLOAT32);
    assertEquals(3.14f, marshaler.marshalToWit(3.14, f32Type));

    final WitType f64Type = WitType.primitive(WitPrimitiveType.FLOAT64);
    assertEquals(2.718, marshaler.marshalToWit(2.718, f64Type));

    // Character
    final WitType charType = WitType.primitive(WitPrimitiveType.CHAR);
    assertEquals('A', marshaler.marshalToWit('A', charType));
    assertEquals('X', marshaler.marshalToWit("X", charType)); // String to char conversion

    // String
    final WitType stringType = WitType.primitive(WitPrimitiveType.STRING);
    assertEquals("hello", marshaler.marshalToWit("hello", stringType));
    assertEquals("42", marshaler.marshalToWit(42, stringType)); // toString conversion
  }

  @Test
  @DisplayName("Test primitive value marshaling to Java")
  void testPrimitiveValueMarshalingToJava() throws WasmException {
    // For primitives, WIT and Java representations are typically the same
    final WitType boolType = WitType.primitive(WitPrimitiveType.BOOL);
    assertEquals(true, marshaler.marshalToJava(true, boolType));

    final WitType s32Type = WitType.primitive(WitPrimitiveType.S32);
    assertEquals(42, marshaler.marshalToJava(42, s32Type));

    final WitType stringType = WitType.primitive(WitPrimitiveType.STRING);
    assertEquals("hello", marshaler.marshalToJava("hello", stringType));
  }

  @Test
  @DisplayName("Test record value marshaling")
  void testRecordValueMarshaling() throws WasmException {
    final Map<String, WitType> fields =
        Map.of(
            "name", WitType.primitive(WitPrimitiveType.STRING),
            "age", WitType.primitive(WitPrimitiveType.U32));

    final WitType recordType = WitType.record("Person", fields);

    // Marshal Map to WIT record
    final Map<String, Object> personData = Map.of("name", "Alice", "age", 30);

    final Object witRecord = marshaler.marshalToWit(personData, recordType);
    assertInstanceOf(WitValueMarshaler.WitRecord.class, witRecord);

    final WitValueMarshaler.WitRecord record = (WitValueMarshaler.WitRecord) witRecord;
    assertEquals("Alice", record.getFields().get("name"));
    assertEquals(30, record.getFields().get("age"));

    // Marshal back to Java
    final Object javaValue = marshaler.marshalToJava(witRecord, recordType);
    assertInstanceOf(Map.class, javaValue);

    @SuppressWarnings("unchecked")
    final Map<String, Object> javaMap = (Map<String, Object>) javaValue;
    assertEquals("Alice", javaMap.get("name"));
    assertEquals(30, javaMap.get("age"));
  }

  @Test
  @DisplayName("Test variant value marshaling")
  void testVariantValueMarshaling() throws WasmException {
    final Map<String, Optional<WitType>> cases =
        Map.of(
            "none", Optional.empty(),
            "some", Optional.of(WitType.primitive(WitPrimitiveType.S32)));

    final WitType variantType = WitType.variant("Option", cases);

    // Marshal single-entry Map to WIT variant
    final Map<String, Object> variantData = Map.of("some", 42);

    final Object witVariant = marshaler.marshalToWit(variantData, variantType);
    assertInstanceOf(WitValueMarshaler.WitVariant.class, witVariant);

    final WitValueMarshaler.WitVariant variant = (WitValueMarshaler.WitVariant) witVariant;
    assertEquals("some", variant.getCaseName());
    assertEquals(42, variant.getValue().get());

    // Marshal back to Java
    final Object javaValue = marshaler.marshalToJava(witVariant, variantType);
    assertInstanceOf(Map.class, javaValue);

    @SuppressWarnings("unchecked")
    final Map<String, Object> javaMap = (Map<String, Object>) javaValue;
    assertEquals(42, javaMap.get("some"));
  }

  @Test
  @DisplayName("Test enum value marshaling")
  void testEnumValueMarshaling() throws WasmException {
    final WitType enumType = WitType.enumType("Color", List.of("red", "green", "blue"));

    // Marshal String to WIT enum
    final Object witEnum = marshaler.marshalToWit("red", enumType);
    assertInstanceOf(WitValueMarshaler.WitEnum.class, witEnum);

    final WitValueMarshaler.WitEnum enumValue = (WitValueMarshaler.WitEnum) witEnum;
    assertEquals("red", enumValue.getValue());

    // Marshal back to Java
    final Object javaValue = marshaler.marshalToJava(witEnum, enumType);
    assertEquals("red", javaValue);

    // Test with Java enum
    enum TestColor {
      RED,
      GREEN,
      BLUE
    }
    final Object witEnumFromJava = marshaler.marshalToWit(TestColor.GREEN, enumType);
    assertInstanceOf(WitValueMarshaler.WitEnum.class, witEnumFromJava);
    assertEquals("GREEN", ((WitValueMarshaler.WitEnum) witEnumFromJava).getValue());
  }

  @Test
  @DisplayName("Test flags value marshaling")
  void testFlagsValueMarshaling() throws WasmException {
    final WitType flagsType = WitType.flags("Permissions", List.of("read", "write", "execute"));

    // Marshal List to WIT flags
    final List<String> flagsList = List.of("read", "execute");

    final Object witFlags = marshaler.marshalToWit(flagsList, flagsType);
    assertInstanceOf(WitValueMarshaler.WitFlags.class, witFlags);

    final WitValueMarshaler.WitFlags flags = (WitValueMarshaler.WitFlags) witFlags;
    assertEquals(flagsList, flags.getFlags());

    // Marshal back to Java
    final Object javaValue = marshaler.marshalToJava(witFlags, flagsType);
    assertEquals(flagsList, javaValue);

    // Test with array
    final String[] flagsArray = {"read", "write"};
    final Object witFlagsFromArray = marshaler.marshalToWit(flagsArray, flagsType);
    assertInstanceOf(WitValueMarshaler.WitFlags.class, witFlagsFromArray);
  }

  @Test
  @DisplayName("Test list value marshaling")
  void testListValueMarshaling() throws WasmException {
    final WitType listType = WitType.list(WitType.primitive(WitPrimitiveType.STRING));

    // Marshal List to WIT list
    final List<String> stringList = List.of("hello", "world", "test");

    final Object witList = marshaler.marshalToWit(stringList, listType);
    assertInstanceOf(WitValueMarshaler.WitList.class, witList);

    final WitValueMarshaler.WitList list = (WitValueMarshaler.WitList) witList;
    assertEquals(stringList, list.getElements());

    // Marshal back to Java
    final Object javaValue = marshaler.marshalToJava(witList, listType);
    assertEquals(stringList, javaValue);

    // Test with array
    final String[] stringArray = {"foo", "bar"};
    final Object witListFromArray = marshaler.marshalToWit(stringArray, listType);
    assertInstanceOf(WitValueMarshaler.WitList.class, witListFromArray);
  }

  @Test
  @DisplayName("Test option value marshaling")
  void testOptionValueMarshaling() throws WasmException {
    final WitType optionType = WitType.option(WitType.primitive(WitPrimitiveType.S32));

    // Marshal Optional to WIT option
    final Optional<Integer> someValue = Optional.of(42);
    final Object witOptionSome = marshaler.marshalToWit(someValue, optionType);
    assertInstanceOf(WitValueMarshaler.WitOption.class, witOptionSome);

    final WitValueMarshaler.WitOption optionSome = (WitValueMarshaler.WitOption) witOptionSome;
    assertEquals(42, optionSome.getValue().get());

    // Marshal empty Optional
    final Optional<Integer> emptyValue = Optional.empty();
    final Object witOptionEmpty = marshaler.marshalToWit(emptyValue, optionType);
    assertInstanceOf(WitValueMarshaler.WitOption.class, witOptionEmpty);

    final WitValueMarshaler.WitOption optionEmpty = (WitValueMarshaler.WitOption) witOptionEmpty;
    assertTrue(optionEmpty.getValue().isEmpty());

    // Marshal direct value (should be wrapped in Optional)
    final Object witOptionDirect = marshaler.marshalToWit(123, optionType);
    assertInstanceOf(WitValueMarshaler.WitOption.class, witOptionDirect);
    assertEquals(123, ((WitValueMarshaler.WitOption) witOptionDirect).getValue().get());

    // Marshal null (should create empty option)
    final Object witOptionNull = marshaler.marshalToWit(null, optionType);
    assertInstanceOf(WitValueMarshaler.WitOption.class, witOptionNull);
    assertTrue(((WitValueMarshaler.WitOption) witOptionNull).getValue().isEmpty());
  }

  @Test
  @DisplayName("Test result value marshaling")
  void testResultValueMarshaling() throws WasmException {
    final WitType resultType =
        WitType.result(
            Optional.of(WitType.primitive(WitPrimitiveType.STRING)),
            Optional.of(WitType.primitive(WitPrimitiveType.S32)));

    // Marshal success value
    final Object witResultOk = marshaler.marshalToWit("success", resultType);
    assertInstanceOf(WitValueMarshaler.WitResult.class, witResultOk);

    final WitValueMarshaler.WitResult resultOk = (WitValueMarshaler.WitResult) witResultOk;
    assertTrue(resultOk.isOk());
    assertEquals("success", resultOk.getOkValue());

    // Marshal WitResult directly
    final WitValueMarshaler.WitResult directResult = WitValueMarshaler.WitResult.error(404);
    final Object witResultError = marshaler.marshalToWit(directResult, resultType);
    assertSame(directResult, witResultError);

    // Marshal back to Java
    final Object javaValueOk = marshaler.marshalToJava(resultOk, resultType);
    assertEquals("success", javaValueOk);
  }

  @Test
  @DisplayName("Test resource value marshaling")
  void testResourceValueMarshaling() throws WasmException {
    final WitType resourceType = WitType.resource("FileHandle", "file-123");

    // Marshal arbitrary object to WIT resource
    final String fileData = "file content";
    final Object witResource = marshaler.marshalToWit(fileData, resourceType);
    assertInstanceOf(WitValueMarshaler.WitResource.class, witResource);

    final WitValueMarshaler.WitResource resource = (WitValueMarshaler.WitResource) witResource;
    assertEquals(fileData.hashCode(), resource.getHandle());
    assertEquals(fileData, resource.getValue());

    // Marshal back to Java
    final Object javaValue = marshaler.marshalToJava(witResource, resourceType);
    assertEquals(fileData, javaValue);

    // Test with WitResource directly
    final WitValueMarshaler.WitResource directResource =
        new WitValueMarshaler.WitResource(999, "test");
    final Object witResourceDirect = marshaler.marshalToWit(directResource, resourceType);
    assertSame(directResource, witResourceDirect);
  }

  @Test
  @DisplayName("Test custom value converter registration")
  void testCustomValueConverterRegistration() throws WasmException {
    final WitType customType = WitType.primitive(WitPrimitiveType.STRING);

    // Register custom converter
    marshaler.registerConverter(
        customType,
        new WitValueMarshaler.ValueConverter() {
          @Override
          public Object toWit(final Object value) {
            return "custom:" + value;
          }

          @Override
          public Object fromWit(final Object value) {
            return value.toString().substring(7); // Remove "custom:" prefix
          }
        });

    // Test custom conversion
    final Object witValue = marshaler.marshalToWit("test", customType);
    assertEquals("custom:test", witValue);

    final Object javaValue = marshaler.marshalToJava("custom:hello", customType);
    assertEquals("hello", javaValue);
  }

  @Test
  @DisplayName("Test error handling")
  void testErrorHandling() {
    // Test incompatible type marshaling
    final WitType boolType = WitType.primitive(WitPrimitiveType.BOOL);

    assertThrows(WasmException.class, () -> marshaler.marshalToWit("not a boolean", boolType));

    // Test null WIT type
    assertThrows(NullPointerException.class, () -> marshaler.marshalToWit("value", null));

    assertThrows(NullPointerException.class, () -> marshaler.marshalToJava("value", null));
  }

  @Test
  @DisplayName("Test complex nested marshaling")
  void testComplexNestedMarshaling() throws WasmException {
    // Create complex nested type: record { data: list<option<s32>> }
    final WitType optionS32 = WitType.option(WitType.primitive(WitPrimitiveType.S32));
    final WitType listOption = WitType.list(optionS32);
    final WitType recordType = WitType.record("ComplexData", Map.of("data", listOption));

    // Create nested data structure
    final Map<String, Object> recordData =
        Map.of("data", List.of(Optional.of(42), Optional.empty(), Optional.of(123)));

    // Marshal to WIT
    final Object witValue = marshaler.marshalToWit(recordData, recordType);
    assertInstanceOf(WitValueMarshaler.WitRecord.class, witValue);

    final WitValueMarshaler.WitRecord record = (WitValueMarshaler.WitRecord) witValue;
    final Object dataField = record.getFields().get("data");
    assertInstanceOf(WitValueMarshaler.WitList.class, dataField);

    // Marshal back to Java
    final Object javaValue = marshaler.marshalToJava(witValue, recordType);
    assertInstanceOf(Map.class, javaValue);
  }

  @Test
  @DisplayName("Test object to record marshaling using reflection")
  void testObjectToRecordMarshaling() throws WasmException {
    // Create a test object
    final TestPerson person = new TestPerson("John", 25, true);

    final Map<String, WitType> fields =
        Map.of(
            "name", WitType.primitive(WitPrimitiveType.STRING),
            "age", WitType.primitive(WitPrimitiveType.U32),
            "active", WitType.primitive(WitPrimitiveType.BOOL));

    final WitType recordType = WitType.record("Person", fields);

    // Marshal object to WIT record
    final Object witRecord = marshaler.marshalToWit(person, recordType);
    assertInstanceOf(WitValueMarshaler.WitRecord.class, witRecord);

    final WitValueMarshaler.WitRecord record = (WitValueMarshaler.WitRecord) witRecord;
    final Map<String, Object> recordFields = record.getFields();

    assertTrue(recordFields.containsKey("name"));
    assertTrue(recordFields.containsKey("age"));
    assertTrue(recordFields.containsKey("active"));
  }

  @Test
  @DisplayName("Test result error marshaling")
  void testResultErrorMarshaling() throws WasmException {
    final WitType resultType =
        WitType.result(
            Optional.of(WitType.primitive(WitPrimitiveType.STRING)),
            Optional.of(WitType.primitive(WitPrimitiveType.S32)));

    // Create error result
    final WitValueMarshaler.WitResult errorResult = WitValueMarshaler.WitResult.error(500);

    // Marshal error result
    final Object witValue = marshaler.marshalToWit(errorResult, resultType);
    assertSame(errorResult, witValue);

    // Try to marshal back to Java (should throw exception)
    assertThrows(WasmException.class, () -> marshaler.marshalToJava(errorResult, resultType));
  }

  /** Test class for object marshaling. */
  public static class TestPerson {
    public String name;
    public int age;
    private boolean active;

    public TestPerson(final String name, final int age, final boolean active) {
      this.name = name;
      this.age = age;
      this.active = active;
    }

    public String getName() {
      return name;
    }

    public int getAge() {
      return age;
    }

    public boolean isActive() {
      return active;
    }
  }
}
