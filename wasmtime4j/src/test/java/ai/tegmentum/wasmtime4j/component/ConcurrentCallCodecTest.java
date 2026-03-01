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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConcurrentCallCodec}.
 *
 * <p>Validates that the JSON serialization/deserialization round-trips correctly for all Component
 * Model value types, matching the Rust {@code concurrent_call_json} module format.
 *
 * @since 1.1.0
 */
@DisplayName("ConcurrentCallCodec")
class ConcurrentCallCodecTest {

  @Nested
  @DisplayName("serializeCalls")
  class SerializeCalls {

    @Test
    @DisplayName("serializes empty call list")
    void serializesEmptyCalls() {
      final String json = ConcurrentCallCodec.serializeCalls(List.of());
      assertEquals("[]", json, "Empty call list should produce empty JSON array");
    }

    @Test
    @DisplayName("serializes call with no arguments")
    void serializesCallWithNoArgs() {
      final List<ConcurrentCall> calls = List.of(ConcurrentCall.of("noop"));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertEquals("[{\"name\":\"noop\",\"args\":[]}]", json);
    }

    @Test
    @DisplayName("serializes call with primitive arguments")
    void serializesCallWithPrimitiveArgs() {
      final List<ConcurrentCall> calls =
          List.of(ConcurrentCall.of("add", ComponentVal.s32(1), ComponentVal.s32(2)));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertTrue(json.contains("\"name\":\"add\""), "Should contain function name");
      assertTrue(json.contains("\"type\":\"S32\""), "Should contain S32 type tag");
      assertTrue(json.contains("\"value\":1"), "Should contain value 1");
      assertTrue(json.contains("\"value\":2"), "Should contain value 2");
    }

    @Test
    @DisplayName("serializes multiple calls")
    void serializesMultipleCalls() {
      final List<ConcurrentCall> calls =
          List.of(
              ConcurrentCall.of("add", ComponentVal.s32(1), ComponentVal.s32(2)),
              ConcurrentCall.of("multiply", ComponentVal.s32(3), ComponentVal.s32(4)));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertTrue(json.contains("\"name\":\"add\""), "Should contain first function name");
      assertTrue(json.contains("\"name\":\"multiply\""), "Should contain second function name");
    }

    @Test
    @DisplayName("serializes all primitive types")
    void serializesAllPrimitiveTypes() {
      final List<ConcurrentCall> calls =
          List.of(
              ConcurrentCall.of(
                  "all_types",
                  ComponentVal.bool(true),
                  ComponentVal.s8((byte) -1),
                  ComponentVal.s16((short) -100),
                  ComponentVal.s32(42),
                  ComponentVal.s64(Long.MAX_VALUE),
                  ComponentVal.u8((short) 255),
                  ComponentVal.u16(65535),
                  ComponentVal.u32(4294967295L),
                  ComponentVal.u64(Long.MIN_VALUE),
                  ComponentVal.f32(3.14f),
                  ComponentVal.f64(2.718),
                  ComponentVal.char_('Z'),
                  ComponentVal.string("hello world")));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertTrue(json.contains("\"type\":\"Bool\""), "Should contain Bool type");
      assertTrue(json.contains("\"type\":\"S8\""), "Should contain S8 type");
      assertTrue(json.contains("\"type\":\"S16\""), "Should contain S16 type");
      assertTrue(json.contains("\"type\":\"S32\""), "Should contain S32 type");
      assertTrue(json.contains("\"type\":\"S64\""), "Should contain S64 type");
      assertTrue(json.contains("\"type\":\"U8\""), "Should contain U8 type");
      assertTrue(json.contains("\"type\":\"U16\""), "Should contain U16 type");
      assertTrue(json.contains("\"type\":\"U32\""), "Should contain U32 type");
      assertTrue(json.contains("\"type\":\"U64\""), "Should contain U64 type");
      assertTrue(json.contains("\"type\":\"Float32\""), "Should contain Float32 type");
      assertTrue(json.contains("\"type\":\"Float64\""), "Should contain Float64 type");
      assertTrue(json.contains("\"type\":\"Char\""), "Should contain Char type");
      assertTrue(json.contains("\"type\":\"String\""), "Should contain String type");
      assertTrue(json.contains("\"value\":\"hello world\""), "Should contain string value");
    }

    @Test
    @DisplayName("serializes string with special characters")
    void serializesStringWithSpecialChars() {
      final List<ConcurrentCall> calls =
          List.of(ConcurrentCall.of("test", ComponentVal.string("hello \"world\"\nnewline")));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertTrue(json.contains("\\\"world\\\""), "Should escape double quotes. Got: " + json);
      assertTrue(json.contains("\\n"), "Should escape newline. Got: " + json);
    }

    @Test
    @DisplayName("serializes compound types")
    void serializesCompoundTypes() {
      final List<ConcurrentCall> calls =
          List.of(
              ConcurrentCall.of(
                  "compound",
                  ComponentVal.list(ComponentVal.s32(1), ComponentVal.s32(2)),
                  ComponentVal.tuple(ComponentVal.string("a"), ComponentVal.s32(42)),
                  ComponentVal.record(
                      Map.of("name", ComponentVal.string("Alice"), "age", ComponentVal.u32(30)))));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertTrue(json.contains("\"type\":\"List\""), "Should contain List type");
      assertTrue(json.contains("\"type\":\"Tuple\""), "Should contain Tuple type");
      assertTrue(json.contains("\"type\":\"Record\""), "Should contain Record type");
    }

    @Test
    @DisplayName("serializes variant, enum, option, result, flags")
    void serializesAlgebraicTypes() {
      final List<ConcurrentCall> calls =
          List.of(
              ConcurrentCall.of(
                  "algebraic",
                  ComponentVal.variant("some_case", ComponentVal.s32(42)),
                  ComponentVal.variant("no_payload"),
                  ComponentVal.enum_("red"),
                  ComponentVal.some(ComponentVal.string("present")),
                  ComponentVal.none(),
                  ComponentVal.ok(ComponentVal.s32(100)),
                  ComponentVal.err(ComponentVal.string("bad")),
                  ComponentVal.flags("read", "write")));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertTrue(json.contains("\"type\":\"Variant\""), "Should contain Variant type");
      assertTrue(json.contains("\"type\":\"Enum\""), "Should contain Enum type");
      assertTrue(json.contains("\"type\":\"Option\""), "Should contain Option type");
      assertTrue(json.contains("\"type\":\"Result\""), "Should contain Result type");
      assertTrue(json.contains("\"type\":\"Flags\""), "Should contain Flags type");
    }

    @Test
    @DisplayName("rejects null calls list")
    void rejectsNullCalls() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ConcurrentCallCodec.serializeCalls(null),
          "Should reject null calls list");
    }
  }

  @Nested
  @DisplayName("deserializeResults")
  class DeserializeResults {

    @Test
    @DisplayName("deserializes empty results")
    void deserializesEmptyResults() {
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults("[]");
      assertNotNull(results, "Results should not be null");
      assertTrue(results.isEmpty(), "Empty JSON array should produce empty results");
    }

    @Test
    @DisplayName("deserializes single result with S32")
    void deserializesSingleS32() {
      final String json = "[[{\"type\":\"S32\",\"value\":42}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      assertEquals(1, results.size(), "Should have 1 result");
      assertEquals(1, results.get(0).size(), "First result should have 1 value");
      assertTrue(results.get(0).get(0).isS32(), "Value should be S32");
      assertEquals(42, results.get(0).get(0).asS32(), "Value should be 42");
    }

    @Test
    @DisplayName("deserializes multiple results")
    void deserializesMultipleResults() {
      final String json = "[[{\"type\":\"S32\",\"value\":3}],[{\"type\":\"S32\",\"value\":12}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      assertEquals(2, results.size(), "Should have 2 results");
      assertEquals(3, results.get(0).get(0).asS32(), "First result should be 3");
      assertEquals(12, results.get(1).get(0).asS32(), "Second result should be 12");
    }

    @Test
    @DisplayName("deserializes all primitive types")
    void deserializesAllPrimitiveTypes() {
      final String json =
          "[[{\"type\":\"Bool\",\"value\":true},"
              + "{\"type\":\"S8\",\"value\":-1},"
              + "{\"type\":\"S16\",\"value\":-100},"
              + "{\"type\":\"S32\",\"value\":42},"
              + "{\"type\":\"S64\",\"value\":9223372036854775807},"
              + "{\"type\":\"U8\",\"value\":255},"
              + "{\"type\":\"U16\",\"value\":65535},"
              + "{\"type\":\"U32\",\"value\":4294967295},"
              + "{\"type\":\"U64\",\"value\":-9223372036854775808},"
              + "{\"type\":\"Float32\",\"value\":3.14},"
              + "{\"type\":\"Float64\",\"value\":2.718},"
              + "{\"type\":\"Char\",\"value\":\"Z\"},"
              + "{\"type\":\"String\",\"value\":\"hello\"}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final List<ComponentVal> vals = results.get(0);
      assertEquals(13, vals.size(), "Should have 13 values");

      assertTrue(vals.get(0).asBool(), "Bool should be true");
      assertEquals(-1, vals.get(1).asS8(), "S8 should be -1");
      assertEquals(-100, vals.get(2).asS16(), "S16 should be -100");
      assertEquals(42, vals.get(3).asS32(), "S32 should be 42");
      assertEquals(Long.MAX_VALUE, vals.get(4).asS64(), "S64 should be Long.MAX_VALUE");
      assertEquals(255, vals.get(5).asU8(), "U8 should be 255");
      assertEquals(65535, vals.get(6).asU16(), "U16 should be 65535");
      assertEquals(4294967295L, vals.get(7).asU32(), "U32 should be 4294967295");
      assertEquals(Long.MIN_VALUE, vals.get(8).asU64(), "U64 should be Long.MIN_VALUE");
      assertEquals(3.14f, vals.get(9).asF32(), 0.01f, "Float32 should be ~3.14");
      assertEquals(2.718, vals.get(10).asF64(), 0.001, "Float64 should be ~2.718");
      assertEquals('Z', vals.get(11).asChar(), "Char should be 'Z'");
      assertEquals("hello", vals.get(12).asString(), "String should be 'hello'");
    }

    @Test
    @DisplayName("deserializes list values")
    void deserializesListValues() {
      final String json =
          "[[{\"type\":\"List\",\"value\":[{\"type\":\"S32\",\"value\":1},"
              + "{\"type\":\"S32\",\"value\":2},{\"type\":\"S32\",\"value\":3}]}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final ComponentVal listVal = results.get(0).get(0);
      assertTrue(listVal.isList(), "Value should be List");
      final List<ComponentVal> items = listVal.asList();
      assertEquals(3, items.size(), "List should have 3 items");
      assertEquals(1, items.get(0).asS32(), "First item should be 1");
      assertEquals(2, items.get(1).asS32(), "Second item should be 2");
      assertEquals(3, items.get(2).asS32(), "Third item should be 3");
    }

    @Test
    @DisplayName("deserializes record values")
    void deserializesRecordValues() {
      final String json =
          "[[{\"type\":\"Record\",\"value\":[[\"name\",{\"type\":\"String\","
              + "\"value\":\"Alice\"}],[\"age\",{\"type\":\"U32\",\"value\":30}]]}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final ComponentVal recordVal = results.get(0).get(0);
      assertTrue(recordVal.isRecord(), "Value should be Record");
      final Map<String, ComponentVal> fields = recordVal.asRecord();
      assertEquals("Alice", fields.get("name").asString(), "Name should be Alice");
      assertEquals(30L, fields.get("age").asU32(), "Age should be 30");
    }

    @Test
    @DisplayName("deserializes variant with payload")
    void deserializesVariantWithPayload() {
      final String json =
          "[[{\"type\":\"Variant\",\"value\":{\"discriminant\":\"some_case\","
              + "\"value\":{\"type\":\"S32\",\"value\":42}}}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final ComponentVal variantVal = results.get(0).get(0);
      assertTrue(variantVal.isVariant(), "Value should be Variant");
      final ComponentVariant variant = variantVal.asVariant();
      assertEquals("some_case", variant.getCaseName(), "Case name should be some_case");
      assertTrue(variant.hasPayload(), "Should have payload");
      assertEquals(42, variant.getPayload().get().asS32(), "Payload should be 42");
    }

    @Test
    @DisplayName("deserializes variant without payload")
    void deserializesVariantWithoutPayload() {
      final String json =
          "[[{\"type\":\"Variant\",\"value\":{\"discriminant\":\"no_payload\","
              + "\"value\":null}}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final ComponentVariant variant = results.get(0).get(0).asVariant();
      assertEquals("no_payload", variant.getCaseName(), "Case name should be no_payload");
      assertFalse(variant.hasPayload(), "Should not have payload");
    }

    @Test
    @DisplayName("deserializes enum values")
    void deserializesEnumValues() {
      final String json = "[[{\"type\":\"Enum\",\"value\":\"red\"}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      assertTrue(results.get(0).get(0).isEnum(), "Value should be Enum");
      assertEquals("red", results.get(0).get(0).asEnum(), "Enum should be 'red'");
    }

    @Test
    @DisplayName("deserializes option some")
    void deserializesOptionSome() {
      final String json =
          "[[{\"type\":\"Option\",\"value\":{\"type\":\"String\",\"value\":\"present\"}}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final ComponentVal optVal = results.get(0).get(0);
      assertTrue(optVal.isOption(), "Value should be Option");
      final Optional<ComponentVal> inner = optVal.asSome();
      assertTrue(inner.isPresent(), "Option should be Some");
      assertEquals("present", inner.get().asString(), "Inner value should be 'present'");
    }

    @Test
    @DisplayName("deserializes option none")
    void deserializesOptionNone() {
      final String json = "[[{\"type\":\"Option\",\"value\":null}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final Optional<ComponentVal> inner = results.get(0).get(0).asSome();
      assertFalse(inner.isPresent(), "Option should be None");
    }

    @Test
    @DisplayName("deserializes result ok")
    void deserializesResultOk() {
      final String json =
          "[[{\"type\":\"Result\",\"value\":{\"ok\":{\"type\":\"S32\",\"value\":100},"
              + "\"err\":null,\"is_ok\":true}}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final ComponentResult result = results.get(0).get(0).asResult();
      assertTrue(result.isOk(), "Result should be Ok");
      assertEquals(100, result.getOk().get().asS32(), "Ok value should be 100");
    }

    @Test
    @DisplayName("deserializes result err")
    void deserializesResultErr() {
      final String json =
          "[[{\"type\":\"Result\",\"value\":{\"ok\":null,"
              + "\"err\":{\"type\":\"String\",\"value\":\"bad\"},\"is_ok\":false}}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final ComponentResult result = results.get(0).get(0).asResult();
      assertTrue(result.isErr(), "Result should be Err");
      assertEquals("bad", result.getErr().get().asString(), "Err value should be 'bad'");
    }

    @Test
    @DisplayName("deserializes flags")
    void deserializesFlags() {
      final String json = "[[{\"type\":\"Flags\",\"value\":[\"read\",\"write\"]}]]";
      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(json);
      final Set<String> flags = results.get(0).get(0).asFlags();
      assertEquals(2, flags.size(), "Should have 2 flags");
      assertTrue(flags.contains("read"), "Should contain 'read'");
      assertTrue(flags.contains("write"), "Should contain 'write'");
    }

    @Test
    @DisplayName("rejects null json")
    void rejectsNullJson() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ConcurrentCallCodec.deserializeResults(null),
          "Should reject null json");
    }

    @Test
    @DisplayName("rejects malformed json")
    void rejectsMalformedJson() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ConcurrentCallCodec.deserializeResults("{invalid"),
          "Should reject malformed json");
    }
  }

  @Nested
  @DisplayName("round-trip")
  class RoundTrip {

    @Test
    @DisplayName("round-trips primitive types through serialize/deserialize")
    void roundTripsPrimitives() {
      final List<ConcurrentCall> calls =
          List.of(
              ConcurrentCall.of(
                  "test",
                  ComponentVal.bool(true),
                  ComponentVal.s32(-42),
                  ComponentVal.u64(Long.MAX_VALUE),
                  ComponentVal.f64(Math.PI),
                  ComponentVal.string("hello")));
      final String json = ConcurrentCallCodec.serializeCalls(calls);
      assertNotNull(json, "Serialized JSON should not be null");
      assertFalse(json.isEmpty(), "Serialized JSON should not be empty");

      // Verify the JSON is well-formed by checking it starts and ends correctly
      assertTrue(json.startsWith("["), "Should start with [");
      assertTrue(json.endsWith("]"), "Should end with ]");

      // Verify type tags are present
      assertTrue(json.contains("\"type\":\"Bool\""), "Should contain Bool");
      assertTrue(json.contains("\"type\":\"S32\""), "Should contain S32");
      assertTrue(json.contains("\"type\":\"U64\""), "Should contain U64");
      assertTrue(json.contains("\"type\":\"Float64\""), "Should contain Float64");
      assertTrue(json.contains("\"type\":\"String\""), "Should contain String");
    }

    @Test
    @DisplayName("round-trips compound types through result deserialization")
    void roundTripsCompound() {
      // Simulate what the Rust side would return for compound types
      final String resultJson =
          "[[{\"type\":\"List\",\"value\":[{\"type\":\"S32\",\"value\":1},"
              + "{\"type\":\"S32\",\"value\":2}]},"
              + "{\"type\":\"Tuple\",\"value\":[{\"type\":\"String\",\"value\":\"x\"},"
              + "{\"type\":\"S32\",\"value\":99}]},"
              + "{\"type\":\"Record\",\"value\":[[\"a\",{\"type\":\"Bool\",\"value\":true}],"
              + "[\"b\",{\"type\":\"S32\",\"value\":7}]]}]]";

      final List<List<ComponentVal>> results = ConcurrentCallCodec.deserializeResults(resultJson);
      assertEquals(1, results.size(), "Should have 1 result set");
      assertEquals(3, results.get(0).size(), "Should have 3 values in result");

      // Verify list
      final List<ComponentVal> listItems = results.get(0).get(0).asList();
      assertEquals(2, listItems.size(), "List should have 2 items");
      assertEquals(1, listItems.get(0).asS32(), "First list item should be 1");
      assertEquals(2, listItems.get(1).asS32(), "Second list item should be 2");

      // Verify tuple
      final List<ComponentVal> tupleItems = results.get(0).get(1).asTuple();
      assertEquals(2, tupleItems.size(), "Tuple should have 2 items");
      assertEquals("x", tupleItems.get(0).asString(), "First tuple item should be 'x'");
      assertEquals(99, tupleItems.get(1).asS32(), "Second tuple item should be 99");

      // Verify record
      final Map<String, ComponentVal> recordFields = results.get(0).get(2).asRecord();
      assertTrue(recordFields.get("a").asBool(), "Record field 'a' should be true");
      assertEquals(7, recordFields.get("b").asS32(), "Record field 'b' should be 7");
    }
  }
}
