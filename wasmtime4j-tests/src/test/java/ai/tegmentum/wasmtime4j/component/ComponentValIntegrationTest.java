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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentResult;
import ai.tegmentum.wasmtime4j.ComponentVal;
import ai.tegmentum.wasmtime4j.ComponentVariant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for ComponentVal - Component Model value types.
 *
 * <p>These tests verify the creation, type checking, and value extraction of all Component Model
 * value types including primitives, compound types, and handle types.
 *
 * @since 1.0.0
 */
@DisplayName("ComponentVal Integration Tests")
public final class ComponentValIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ComponentValIntegrationTest.class.getName());

  @Nested
  @DisplayName("Boolean Value Tests")
  class BooleanValueTests {

    @Test
    @DisplayName("should create and extract boolean true value")
    void shouldCreateAndExtractBooleanTrueValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.bool(true);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isBool(), "Value should be a boolean");
      assertTrue(val.asBool(), "Value should be true");

      // Verify other type checks return false
      assertFalse(val.isS32(), "Should not be s32");
      assertFalse(val.isString(), "Should not be string");

      LOGGER.info("Boolean true value test passed");
    }

    @Test
    @DisplayName("should create and extract boolean false value")
    void shouldCreateAndExtractBooleanFalseValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.bool(false);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isBool(), "Value should be a boolean");
      assertFalse(val.asBool(), "Value should be false");

      LOGGER.info("Boolean false value test passed");
    }

    @Test
    @DisplayName("should throw when extracting boolean from non-boolean value")
    void shouldThrowWhenExtractingBooleanFromNonBooleanValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.s32(42);

      assertThrows(
          IllegalStateException.class,
          () -> val.asBool(),
          "Should throw when extracting bool from s32");

      LOGGER.info("Boolean extraction error test passed");
    }
  }

  @Nested
  @DisplayName("Signed Integer Value Tests")
  class SignedIntegerValueTests {

    @Test
    @DisplayName("should create and extract s8 value")
    void shouldCreateAndExtractS8Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final byte testValue = (byte) -128;
      final ComponentVal val = ComponentVal.s8(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isS8(), "Value should be s8");
      assertEquals(testValue, val.asS8(), "Value should match");

      // Test max value
      final ComponentVal maxVal = ComponentVal.s8((byte) 127);
      assertEquals((byte) 127, maxVal.asS8(), "Max s8 value should match");

      LOGGER.info("s8 value test passed");
    }

    @Test
    @DisplayName("should create and extract s16 value")
    void shouldCreateAndExtractS16Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final short testValue = (short) -32768;
      final ComponentVal val = ComponentVal.s16(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isS16(), "Value should be s16");
      assertEquals(testValue, val.asS16(), "Value should match");

      // Test max value
      final ComponentVal maxVal = ComponentVal.s16((short) 32767);
      assertEquals((short) 32767, maxVal.asS16(), "Max s16 value should match");

      LOGGER.info("s16 value test passed");
    }

    @Test
    @DisplayName("should create and extract s32 value")
    void shouldCreateAndExtractS32Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int testValue = -2147483648;
      final ComponentVal val = ComponentVal.s32(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isS32(), "Value should be s32");
      assertEquals(testValue, val.asS32(), "Value should match");

      // Test max and zero
      assertEquals(Integer.MAX_VALUE, ComponentVal.s32(Integer.MAX_VALUE).asS32(), "Max s32");
      assertEquals(0, ComponentVal.s32(0).asS32(), "Zero s32");

      LOGGER.info("s32 value test passed");
    }

    @Test
    @DisplayName("should create and extract s64 value")
    void shouldCreateAndExtractS64Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final long testValue = Long.MIN_VALUE;
      final ComponentVal val = ComponentVal.s64(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isS64(), "Value should be s64");
      assertEquals(testValue, val.asS64(), "Value should match");

      // Test max value
      assertEquals(Long.MAX_VALUE, ComponentVal.s64(Long.MAX_VALUE).asS64(), "Max s64");

      LOGGER.info("s64 value test passed");
    }
  }

  @Nested
  @DisplayName("Unsigned Integer Value Tests")
  class UnsignedIntegerValueTests {

    @Test
    @DisplayName("should create and extract u8 value")
    void shouldCreateAndExtractU8Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final short testValue = (short) 255;
      final ComponentVal val = ComponentVal.u8(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isU8(), "Value should be u8");
      assertEquals(testValue, val.asU8(), "Value should match");

      // Test zero
      assertEquals((short) 0, ComponentVal.u8((short) 0).asU8(), "Zero u8");

      LOGGER.info("u8 value test passed");
    }

    @Test
    @DisplayName("should create and extract u16 value")
    void shouldCreateAndExtractU16Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int testValue = 65535;
      final ComponentVal val = ComponentVal.u16(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isU16(), "Value should be u16");
      assertEquals(testValue, val.asU16(), "Value should match");

      LOGGER.info("u16 value test passed");
    }

    @Test
    @DisplayName("should create and extract u32 value")
    void shouldCreateAndExtractU32Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final long testValue = 4294967295L;
      final ComponentVal val = ComponentVal.u32(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isU32(), "Value should be u32");
      assertEquals(testValue, val.asU32(), "Value should match");

      LOGGER.info("u32 value test passed");
    }

    @Test
    @DisplayName("should create and extract u64 value")
    void shouldCreateAndExtractU64Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final long testValue = -1L; // All bits set = max unsigned value
      final ComponentVal val = ComponentVal.u64(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isU64(), "Value should be u64");
      assertEquals(testValue, val.asU64(), "Value should match");

      LOGGER.info("u64 value test passed");
    }
  }

  @Nested
  @DisplayName("Floating Point Value Tests")
  class FloatingPointValueTests {

    @Test
    @DisplayName("should create and extract f32 value")
    void shouldCreateAndExtractF32Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final float testValue = 3.14159f;
      final ComponentVal val = ComponentVal.f32(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isF32(), "Value should be f32");
      assertEquals(testValue, val.asF32(), 0.00001f, "Value should match");

      // Test special values
      assertTrue(Float.isNaN(ComponentVal.f32(Float.NaN).asF32()), "NaN f32");
      assertEquals(Float.POSITIVE_INFINITY, ComponentVal.f32(Float.POSITIVE_INFINITY).asF32());
      assertEquals(Float.NEGATIVE_INFINITY, ComponentVal.f32(Float.NEGATIVE_INFINITY).asF32());

      LOGGER.info("f32 value test passed");
    }

    @Test
    @DisplayName("should create and extract f64 value")
    void shouldCreateAndExtractF64Value(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final double testValue = 3.141592653589793;
      final ComponentVal val = ComponentVal.f64(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isF64(), "Value should be f64");
      assertEquals(testValue, val.asF64(), 0.0000000001, "Value should match");

      // Test special values
      assertTrue(Double.isNaN(ComponentVal.f64(Double.NaN).asF64()), "NaN f64");

      LOGGER.info("f64 value test passed");
    }
  }

  @Nested
  @DisplayName("Character and String Value Tests")
  class CharacterAndStringValueTests {

    @Test
    @DisplayName("should create and extract char value")
    void shouldCreateAndExtractCharValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final char testValue = 'A';
      final ComponentVal val = ComponentVal.char_(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isChar(), "Value should be char");
      assertEquals(testValue, val.asChar(), "Value should match");

      // Test Unicode character - Chinese character for "middle"
      final ComponentVal unicodeVal = ComponentVal.char_('中');
      assertEquals('中', unicodeVal.asChar(), "Unicode char should match");

      LOGGER.info("char value test passed");
    }

    @Test
    @DisplayName("should create and extract string value")
    void shouldCreateAndExtractStringValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final String testValue = "Hello, WebAssembly!";
      final ComponentVal val = ComponentVal.string(testValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isString(), "Value should be string");
      assertEquals(testValue, val.asString(), "Value should match");

      // Test empty string
      assertEquals("", ComponentVal.string("").asString(), "Empty string");

      // Test Unicode string - "Hello 世界" (Hello World in Chinese)
      final String unicode = "Hello 世界";
      assertEquals(unicode, ComponentVal.string(unicode).asString(), "Unicode string");

      LOGGER.info("string value test passed");
    }
  }

  @Nested
  @DisplayName("List Value Tests")
  class ListValueTests {

    @Test
    @DisplayName("should create and extract list value with varargs")
    void shouldCreateAndExtractListValueWithVarargs(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val =
          ComponentVal.list(ComponentVal.s32(1), ComponentVal.s32(2), ComponentVal.s32(3));

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isList(), "Value should be list");

      final List<ComponentVal> list = val.asList();
      assertEquals(3, list.size(), "List should have 3 elements");
      assertEquals(1, list.get(0).asS32(), "First element should be 1");
      assertEquals(2, list.get(1).asS32(), "Second element should be 2");
      assertEquals(3, list.get(2).asS32(), "Third element should be 3");

      LOGGER.info("list value varargs test passed");
    }

    @Test
    @DisplayName("should create and extract empty list")
    void shouldCreateAndExtractEmptyList(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.list();

      assertTrue(val.isList(), "Value should be list");
      assertTrue(val.asList().isEmpty(), "List should be empty");

      LOGGER.info("empty list test passed");
    }

    @Test
    @DisplayName("should create nested lists")
    void shouldCreateNestedLists(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal innerList = ComponentVal.list(ComponentVal.s32(1), ComponentVal.s32(2));
      final ComponentVal outerList = ComponentVal.list(innerList, ComponentVal.s32(3));

      assertTrue(outerList.isList(), "Outer should be list");
      assertEquals(2, outerList.asList().size(), "Outer list should have 2 elements");
      assertTrue(outerList.asList().get(0).isList(), "First element should be list");

      LOGGER.info("nested list test passed");
    }
  }

  @Nested
  @DisplayName("Record Value Tests")
  class RecordValueTests {

    @Test
    @DisplayName("should create and extract record value")
    void shouldCreateAndExtractRecordValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Map<String, ComponentVal> fields = new HashMap<>();
      fields.put("name", ComponentVal.string("Alice"));
      fields.put("age", ComponentVal.s32(30));
      fields.put("active", ComponentVal.bool(true));

      final ComponentVal val = ComponentVal.record(fields);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isRecord(), "Value should be record");

      final Map<String, ComponentVal> record = val.asRecord();
      assertEquals(3, record.size(), "Record should have 3 fields");
      assertEquals("Alice", record.get("name").asString(), "Name field should match");
      assertEquals(30, record.get("age").asS32(), "Age field should match");
      assertTrue(record.get("active").asBool(), "Active field should be true");

      LOGGER.info("record value test passed");
    }

    @Test
    @DisplayName("should create empty record")
    void shouldCreateEmptyRecord(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.record(new HashMap<>());

      assertTrue(val.isRecord(), "Value should be record");
      assertTrue(val.asRecord().isEmpty(), "Record should be empty");

      LOGGER.info("empty record test passed");
    }
  }

  @Nested
  @DisplayName("Tuple Value Tests")
  class TupleValueTests {

    @Test
    @DisplayName("should create and extract tuple value")
    void shouldCreateAndExtractTupleValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val =
          ComponentVal.tuple(
              ComponentVal.string("test"), ComponentVal.s32(42), ComponentVal.bool(true));

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isTuple(), "Value should be tuple");

      final List<ComponentVal> tuple = val.asTuple();
      assertEquals(3, tuple.size(), "Tuple should have 3 elements");
      assertEquals("test", tuple.get(0).asString(), "First element should be 'test'");
      assertEquals(42, tuple.get(1).asS32(), "Second element should be 42");
      assertTrue(tuple.get(2).asBool(), "Third element should be true");

      LOGGER.info("tuple value test passed");
    }

    @Test
    @DisplayName("should create empty tuple")
    void shouldCreateEmptyTuple(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.tuple();

      assertTrue(val.isTuple(), "Value should be tuple");
      assertTrue(val.asTuple().isEmpty(), "Tuple should be empty");

      LOGGER.info("empty tuple test passed");
    }
  }

  @Nested
  @DisplayName("Variant Value Tests")
  class VariantValueTests {

    @Test
    @DisplayName("should create variant with payload")
    void shouldCreateVariantWithPayload(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal payload = ComponentVal.s32(100);
      final ComponentVal val = ComponentVal.variant("success", payload);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isVariant(), "Value should be variant");

      final ComponentVariant variant = val.asVariant();
      assertEquals("success", variant.getCaseName(), "Case name should match");
      assertTrue(variant.getPayload().isPresent(), "Payload should be present");
      assertEquals(100, variant.getPayload().get().asS32(), "Payload value should match");

      LOGGER.info("variant with payload test passed");
    }

    @Test
    @DisplayName("should create variant without payload")
    void shouldCreateVariantWithoutPayload(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.variant("none");

      assertTrue(val.isVariant(), "Value should be variant");

      final ComponentVariant variant = val.asVariant();
      assertEquals("none", variant.getCaseName(), "Case name should match");
      assertFalse(variant.getPayload().isPresent(), "Payload should not be present");

      LOGGER.info("variant without payload test passed");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should create and extract enum value")
    void shouldCreateAndExtractEnumValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.enum_("red");

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isEnum(), "Value should be enum");
      assertEquals("red", val.asEnum(), "Enum case should match");

      LOGGER.info("enum value test passed");
    }

    @Test
    @DisplayName("should create various enum values")
    void shouldCreateVariousEnumValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertEquals("pending", ComponentVal.enum_("pending").asEnum());
      assertEquals("completed", ComponentVal.enum_("completed").asEnum());
      assertEquals("failed", ComponentVal.enum_("failed").asEnum());

      LOGGER.info("various enum values test passed");
    }
  }

  @Nested
  @DisplayName("Option Value Tests")
  class OptionValueTests {

    @Test
    @DisplayName("should create some option with value")
    void shouldCreateSomeOptionWithValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal inner = ComponentVal.s32(42);
      final ComponentVal val = ComponentVal.some(inner);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isOption(), "Value should be option");

      final Optional<ComponentVal> option = val.asSome();
      assertTrue(option.isPresent(), "Option should be present");
      assertEquals(42, option.get().asS32(), "Inner value should match");

      LOGGER.info("some option test passed");
    }

    @Test
    @DisplayName("should create none option")
    void shouldCreateNoneOption(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.none();

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isOption(), "Value should be option");

      final Optional<ComponentVal> option = val.asSome();
      assertFalse(option.isPresent(), "Option should not be present");

      LOGGER.info("none option test passed");
    }

    @Test
    @DisplayName("should create nested option")
    void shouldCreateNestedOption(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal innerOption = ComponentVal.some(ComponentVal.s32(100));
      final ComponentVal outerOption = ComponentVal.some(innerOption);

      assertTrue(outerOption.isOption(), "Outer should be option");
      assertTrue(outerOption.asSome().isPresent(), "Outer should be some");
      assertTrue(outerOption.asSome().get().isOption(), "Inner should be option");

      LOGGER.info("nested option test passed");
    }
  }

  @Nested
  @DisplayName("Result Value Tests")
  class ResultValueTests {

    @Test
    @DisplayName("should create ok result with value")
    void shouldCreateOkResultWithValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal okValue = ComponentVal.s32(200);
      final ComponentVal val = ComponentVal.ok(okValue);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isResult(), "Value should be result");

      final ComponentResult result = val.asResult();
      assertTrue(result.isOk(), "Result should be ok");
      assertFalse(result.isErr(), "Result should not be err");
      assertTrue(result.getOk().isPresent(), "Ok value should be present");
      assertEquals(200, result.getOk().get().asS32(), "Ok value should match");

      LOGGER.info("ok result test passed");
    }

    @Test
    @DisplayName("should create ok result without value")
    void shouldCreateOkResultWithoutValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.ok();

      assertTrue(val.isResult(), "Value should be result");

      final ComponentResult result = val.asResult();
      assertTrue(result.isOk(), "Result should be ok");
      assertFalse(result.getOk().isPresent(), "Ok value should not be present");

      LOGGER.info("ok result without value test passed");
    }

    @Test
    @DisplayName("should create err result with value")
    void shouldCreateErrResultWithValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal errValue = ComponentVal.string("Something went wrong");
      final ComponentVal val = ComponentVal.err(errValue);

      assertTrue(val.isResult(), "Value should be result");

      final ComponentResult result = val.asResult();
      assertFalse(result.isOk(), "Result should not be ok");
      assertTrue(result.isErr(), "Result should be err");
      assertTrue(result.getErr().isPresent(), "Err value should be present");
      assertEquals("Something went wrong", result.getErr().get().asString(), "Err value match");

      LOGGER.info("err result test passed");
    }

    @Test
    @DisplayName("should create err result without value")
    void shouldCreateErrResultWithoutValue(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.err();

      assertTrue(val.isResult(), "Value should be result");

      final ComponentResult result = val.asResult();
      assertTrue(result.isErr(), "Result should be err");
      assertFalse(result.getErr().isPresent(), "Err value should not be present");

      LOGGER.info("err result without value test passed");
    }
  }

  @Nested
  @DisplayName("Flags Value Tests")
  class FlagsValueTests {

    @Test
    @DisplayName("should create flags with set")
    void shouldCreateFlagsWithSet(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Set<String> enabledFlags = new HashSet<>();
      enabledFlags.add("read");
      enabledFlags.add("write");

      final ComponentVal val = ComponentVal.flags(enabledFlags);

      assertNotNull(val, "ComponentVal should not be null");
      assertTrue(val.isFlags(), "Value should be flags");

      final Set<String> flags = val.asFlags();
      assertEquals(2, flags.size(), "Should have 2 flags");
      assertTrue(flags.contains("read"), "Should contain 'read'");
      assertTrue(flags.contains("write"), "Should contain 'write'");

      LOGGER.info("flags with set test passed");
    }

    @Test
    @DisplayName("should create flags with varargs")
    void shouldCreateFlagsWithVarargs(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.flags("execute", "admin", "sudo");

      assertTrue(val.isFlags(), "Value should be flags");

      final Set<String> flags = val.asFlags();
      assertEquals(3, flags.size(), "Should have 3 flags");
      assertTrue(flags.contains("execute"), "Should contain 'execute'");
      assertTrue(flags.contains("admin"), "Should contain 'admin'");
      assertTrue(flags.contains("sudo"), "Should contain 'sudo'");

      LOGGER.info("flags with varargs test passed");
    }

    @Test
    @DisplayName("should create empty flags")
    void shouldCreateEmptyFlags(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.flags();

      assertTrue(val.isFlags(), "Value should be flags");
      assertTrue(val.asFlags().isEmpty(), "Flags should be empty");

      LOGGER.info("empty flags test passed");
    }
  }

  @Nested
  @DisplayName("Type Checking Consistency Tests")
  class TypeCheckingConsistencyTests {

    @Test
    @DisplayName("should return false for all other type checks on s32")
    void shouldReturnFalseForAllOtherTypeChecksOnS32(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal val = ComponentVal.s32(42);

      assertTrue(val.isS32(), "Should be s32");
      assertFalse(val.isBool(), "Should not be bool");
      assertFalse(val.isS8(), "Should not be s8");
      assertFalse(val.isS16(), "Should not be s16");
      assertFalse(val.isS64(), "Should not be s64");
      assertFalse(val.isU8(), "Should not be u8");
      assertFalse(val.isU16(), "Should not be u16");
      assertFalse(val.isU32(), "Should not be u32");
      assertFalse(val.isU64(), "Should not be u64");
      assertFalse(val.isF32(), "Should not be f32");
      assertFalse(val.isF64(), "Should not be f64");
      assertFalse(val.isChar(), "Should not be char");
      assertFalse(val.isString(), "Should not be string");
      assertFalse(val.isList(), "Should not be list");
      assertFalse(val.isRecord(), "Should not be record");
      assertFalse(val.isTuple(), "Should not be tuple");
      assertFalse(val.isVariant(), "Should not be variant");
      assertFalse(val.isEnum(), "Should not be enum");
      assertFalse(val.isOption(), "Should not be option");
      assertFalse(val.isResult(), "Should not be result");
      assertFalse(val.isFlags(), "Should not be flags");
      assertFalse(val.isResource(), "Should not be resource");

      LOGGER.info("type checking consistency test passed");
    }

    @Test
    @DisplayName("should return correct type for each value type")
    void shouldReturnCorrectTypeForEachValueType(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create one of each type and verify its type check returns true
      assertTrue(ComponentVal.bool(true).isBool(), "bool type check");
      assertTrue(ComponentVal.s8((byte) 1).isS8(), "s8 type check");
      assertTrue(ComponentVal.s16((short) 1).isS16(), "s16 type check");
      assertTrue(ComponentVal.s32(1).isS32(), "s32 type check");
      assertTrue(ComponentVal.s64(1L).isS64(), "s64 type check");
      assertTrue(ComponentVal.u8((short) 1).isU8(), "u8 type check");
      assertTrue(ComponentVal.u16(1).isU16(), "u16 type check");
      assertTrue(ComponentVal.u32(1L).isU32(), "u32 type check");
      assertTrue(ComponentVal.u64(1L).isU64(), "u64 type check");
      assertTrue(ComponentVal.f32(1.0f).isF32(), "f32 type check");
      assertTrue(ComponentVal.f64(1.0).isF64(), "f64 type check");
      assertTrue(ComponentVal.char_('a').isChar(), "char type check");
      assertTrue(ComponentVal.string("").isString(), "string type check");
      assertTrue(ComponentVal.list().isList(), "list type check");
      assertTrue(ComponentVal.record(new HashMap<>()).isRecord(), "record type check");
      assertTrue(ComponentVal.tuple().isTuple(), "tuple type check");
      assertTrue(ComponentVal.variant("case").isVariant(), "variant type check");
      assertTrue(ComponentVal.enum_("case").isEnum(), "enum type check");
      assertTrue(ComponentVal.some(ComponentVal.s32(1)).isOption(), "option type check");
      assertTrue(ComponentVal.none().isOption(), "none option type check");
      assertTrue(ComponentVal.ok().isResult(), "ok result type check");
      assertTrue(ComponentVal.err().isResult(), "err result type check");
      assertTrue(ComponentVal.flags().isFlags(), "flags type check");

      LOGGER.info("correct type for each value test passed");
    }
  }

  @Nested
  @DisplayName("Type Extraction Error Tests")
  class TypeExtractionErrorTests {

    @Test
    @DisplayName("should throw when extracting wrong primitive type")
    void shouldThrowWhenExtractingWrongPrimitiveType(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal stringVal = ComponentVal.string("test");

      assertThrows(IllegalStateException.class, () -> stringVal.asS32(), "s32 from string");
      assertThrows(IllegalStateException.class, () -> stringVal.asS64(), "s64 from string");
      assertThrows(IllegalStateException.class, () -> stringVal.asF32(), "f32 from string");
      assertThrows(IllegalStateException.class, () -> stringVal.asF64(), "f64 from string");
      assertThrows(IllegalStateException.class, () -> stringVal.asBool(), "bool from string");
      assertThrows(IllegalStateException.class, () -> stringVal.asChar(), "char from string");

      LOGGER.info("wrong primitive type extraction error test passed");
    }

    @Test
    @DisplayName("should throw when extracting wrong compound type")
    void shouldThrowWhenExtractingWrongCompoundType(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentVal listVal = ComponentVal.list(ComponentVal.s32(1));

      assertThrows(IllegalStateException.class, () -> listVal.asRecord(), "record from list");
      assertThrows(IllegalStateException.class, () -> listVal.asTuple(), "tuple from list");
      assertThrows(IllegalStateException.class, () -> listVal.asVariant(), "variant from list");
      assertThrows(IllegalStateException.class, () -> listVal.asEnum(), "enum from list");
      assertThrows(IllegalStateException.class, () -> listVal.asSome(), "option from list");
      assertThrows(IllegalStateException.class, () -> listVal.asResult(), "result from list");
      assertThrows(IllegalStateException.class, () -> listVal.asFlags(), "flags from list");

      LOGGER.info("wrong compound type extraction error test passed");
    }
  }

  @Nested
  @DisplayName("Component Type Tests")
  class ComponentTypeTests {

    @Test
    @DisplayName("should get type for primitive values")
    void shouldGetTypeForPrimitiveValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertNotNull(ComponentVal.bool(true).getType(), "bool type");
      assertNotNull(ComponentVal.s32(42).getType(), "s32 type");
      assertNotNull(ComponentVal.string("test").getType(), "string type");

      LOGGER.info("type retrieval test passed");
    }
  }

  @Nested
  @DisplayName("Complex Composition Tests")
  class ComplexCompositionTests {

    @Test
    @DisplayName("should create complex nested structure")
    void shouldCreateComplexNestedStructure(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Build: record { user: record { name: string, age: s32 }, roles: list<string> }
      final Map<String, ComponentVal> userFields = new HashMap<>();
      userFields.put("name", ComponentVal.string("Admin"));
      userFields.put("age", ComponentVal.s32(35));
      final ComponentVal userRecord = ComponentVal.record(userFields);

      final ComponentVal rolesList =
          ComponentVal.list(ComponentVal.string("admin"), ComponentVal.string("moderator"));

      final Map<String, ComponentVal> outerFields = new HashMap<>();
      outerFields.put("user", userRecord);
      outerFields.put("roles", rolesList);
      final ComponentVal complexVal = ComponentVal.record(outerFields);

      assertTrue(complexVal.isRecord(), "Outer should be record");
      final Map<String, ComponentVal> outer = complexVal.asRecord();

      assertTrue(outer.get("user").isRecord(), "user field should be record");
      assertTrue(outer.get("roles").isList(), "roles field should be list");

      final Map<String, ComponentVal> user = outer.get("user").asRecord();
      assertEquals("Admin", user.get("name").asString(), "User name");
      assertEquals(35, user.get("age").asS32(), "User age");

      final List<ComponentVal> roles = outer.get("roles").asList();
      assertEquals(2, roles.size(), "Should have 2 roles");
      assertEquals("admin", roles.get(0).asString(), "First role");

      LOGGER.info("complex nested structure test passed");
    }

    @Test
    @DisplayName("should create result containing record")
    void shouldCreateResultContainingRecord(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Map<String, ComponentVal> successData = new HashMap<>();
      successData.put("id", ComponentVal.s32(123));
      successData.put("status", ComponentVal.string("created"));

      final ComponentVal result = ComponentVal.ok(ComponentVal.record(successData));

      assertTrue(result.isResult(), "Should be result");
      assertTrue(result.asResult().isOk(), "Should be ok");
      assertTrue(result.asResult().getOk().get().isRecord(), "Ok value should be record");

      final Map<String, ComponentVal> data = result.asResult().getOk().get().asRecord();
      assertEquals(123, data.get("id").asS32(), "Id should match");
      assertEquals("created", data.get("status").asString(), "Status should match");

      LOGGER.info("result containing record test passed");
    }
  }
}
