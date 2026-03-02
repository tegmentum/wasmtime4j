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
package ai.tegmentum.wasmtime4j;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitTypeKind} class.
 *
 * <p>WitTypeKind represents the kind of a WIT type, defining its structure and behavior.
 */
@DisplayName("WitTypeKind Tests")
class WitTypeKindTest {

  @Nested
  @DisplayName("Primitive Type Kind Tests")
  class PrimitiveTypeKindTests {

    @Test
    @DisplayName("primitive should create primitive type kind")
    void primitiveShouldCreatePrimitiveTypeKind() {
      final WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertTrue(kind.isPrimitive());
      assertFalse(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.PRIMITIVE, kind.getCategory());
    }

    @Test
    @DisplayName("primitive should return correct size")
    void primitiveShouldReturnCorrectSize() {
      assertEquals(Optional.of(4), WitTypeKind.primitive(WitPrimitiveType.S32).getSizeBytes());
      assertEquals(Optional.of(8), WitTypeKind.primitive(WitPrimitiveType.S64).getSizeBytes());
      assertEquals(Optional.of(1), WitTypeKind.primitive(WitPrimitiveType.BOOL).getSizeBytes());
      assertEquals(Optional.empty(), WitTypeKind.primitive(WitPrimitiveType.STRING).getSizeBytes());
    }

    @Test
    @DisplayName("primitive should return primitive type")
    void primitiveShouldReturnPrimitiveType() {
      final WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.U64);

      assertTrue(kind.getPrimitiveType().isPresent());
      assertEquals(WitPrimitiveType.U64, kind.getPrimitiveType().get());
    }

    @Test
    @DisplayName("primitive should throw on null")
    void primitiveShouldThrowOnNull() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.primitive(null));
    }

    @Test
    @DisplayName("same primitives should be compatible")
    void samePrimitivesShouldBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.primitive(WitPrimitiveType.S32);
      final WitTypeKind kind2 = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different primitives should not be compatible")
    void differentPrimitivesShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.primitive(WitPrimitiveType.S32);
      final WitTypeKind kind2 = WitTypeKind.primitive(WitPrimitiveType.U32);

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("primitive equals and hashCode should work")
    void primitiveEqualsAndHashCodeShouldWork() {
      final WitTypeKind kind1 = WitTypeKind.primitive(WitPrimitiveType.S32);
      final WitTypeKind kind2 = WitTypeKind.primitive(WitPrimitiveType.S32);
      final WitTypeKind kind3 = WitTypeKind.primitive(WitPrimitiveType.U32);

      assertEquals(kind1, kind2);
      assertEquals(kind1.hashCode(), kind2.hashCode());
      assertNotEquals(kind1, kind3);
    }

    @Test
    @DisplayName("primitive toString should contain type info")
    void primitiveToStringShouldContainTypeInfo() {
      final WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertTrue(kind.toString().contains("Primitive"));
      assertTrue(kind.toString().contains("S32"));
    }
  }

  @Nested
  @DisplayName("Record Type Kind Tests")
  class RecordTypeKindTests {

    @Test
    @DisplayName("record should create record type kind")
    void recordShouldCreateRecordTypeKind() {
      final Map<String, WitType> fields = Map.of("id", WitType.createU32());
      final WitTypeKind kind = WitTypeKind.record(fields);

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.RECORD, kind.getCategory());
    }

    @Test
    @DisplayName("record should return empty size")
    void recordShouldReturnEmptySize() {
      final Map<String, WitType> fields = Map.of("id", WitType.createU32());
      final WitTypeKind kind = WitTypeKind.record(fields);

      assertEquals(Optional.empty(), kind.getSizeBytes());
    }

    @Test
    @DisplayName("record should return fields")
    void recordShouldReturnFields() {
      final Map<String, WitType> fields =
          Map.of(
              "id", WitType.createU32(),
              "name", WitType.createString());
      final WitTypeKind kind = WitTypeKind.record(fields);

      assertEquals(2, kind.getRecordFields().size());
      assertTrue(kind.getRecordFields().containsKey("id"));
      assertTrue(kind.getRecordFields().containsKey("name"));
    }

    @Test
    @DisplayName("record should throw on null fields")
    void recordShouldThrowOnNullFields() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.record(null));
    }

    @Test
    @DisplayName("same records should be compatible")
    void sameRecordsShouldBeCompatible() {
      final Map<String, WitType> fields = Map.of("id", WitType.createU32());
      final WitTypeKind kind1 = WitTypeKind.record(fields);
      final WitTypeKind kind2 = WitTypeKind.record(fields);

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different records should not be compatible")
    void differentRecordsShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.record(Map.of("id", WitType.createU32()));
      final WitTypeKind kind2 = WitTypeKind.record(Map.of("name", WitType.createString()));

      assertFalse(kind1.isCompatibleWith(kind2));
    }
  }

  @Nested
  @DisplayName("Variant Type Kind Tests")
  class VariantTypeKindTests {

    @Test
    @DisplayName("variant should create variant type kind")
    void variantShouldCreateVariantTypeKind() {
      final Map<String, Optional<WitType>> cases =
          Map.of(
              "ok", Optional.of(WitType.createString()),
              "err", Optional.of(WitType.createU32()));
      final WitTypeKind kind = WitTypeKind.variant(cases);

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.VARIANT, kind.getCategory());
    }

    @Test
    @DisplayName("variant should return empty size")
    void variantShouldReturnEmptySize() {
      final Map<String, Optional<WitType>> cases = Map.of("ok", Optional.empty());
      final WitTypeKind kind = WitTypeKind.variant(cases);

      assertEquals(Optional.empty(), kind.getSizeBytes());
    }

    @Test
    @DisplayName("variant should return cases")
    void variantShouldReturnCases() {
      final Map<String, Optional<WitType>> cases =
          Map.of(
              "some", Optional.of(WitType.createString()),
              "none", Optional.empty());
      final WitTypeKind kind = WitTypeKind.variant(cases);

      assertEquals(2, kind.getVariantCases().size());
      assertTrue(kind.getVariantCases().containsKey("some"));
      assertTrue(kind.getVariantCases().containsKey("none"));
      assertTrue(kind.getVariantCases().get("none").isEmpty());
    }

    @Test
    @DisplayName("variant should throw on null cases")
    void variantShouldThrowOnNullCases() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.variant(null));
    }

    @Test
    @DisplayName("same variants should be compatible")
    void sameVariantsShouldBeCompatible() {
      final Map<String, Optional<WitType>> cases = Map.of("ok", Optional.empty());
      final WitTypeKind kind1 = WitTypeKind.variant(cases);
      final WitTypeKind kind2 = WitTypeKind.variant(cases);

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different variants should not be compatible")
    void differentVariantsShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.variant(Map.of("ok", Optional.empty()));
      final WitTypeKind kind2 = WitTypeKind.variant(Map.of("err", Optional.empty()));

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("variant should not be compatible with non-variant")
    void variantShouldNotBeCompatibleWithNonVariant() {
      final WitTypeKind variant = WitTypeKind.variant(Map.of("ok", Optional.empty()));
      final WitTypeKind primitive = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertFalse(variant.isCompatibleWith(primitive));
    }
  }

  @Nested
  @DisplayName("Enum Type Kind Tests")
  class EnumTypeKindTests {

    @Test
    @DisplayName("enumType should create enum type kind")
    void enumTypeShouldCreateEnumTypeKind() {
      final List<String> values = List.of("red", "green", "blue");
      final WitTypeKind kind = WitTypeKind.enumType(values);

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.ENUM, kind.getCategory());
    }

    @Test
    @DisplayName("enumType should return size 4")
    void enumTypeShouldReturnSize4() {
      final List<String> values = List.of("a", "b", "c");
      final WitTypeKind kind = WitTypeKind.enumType(values);

      assertEquals(Optional.of(4), kind.getSizeBytes());
    }

    @Test
    @DisplayName("enumType should throw on null values")
    void enumTypeShouldThrowOnNullValues() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.enumType(null));
    }

    @Test
    @DisplayName("same enums should be compatible")
    void sameEnumsShouldBeCompatible() {
      final List<String> values = List.of("a", "b");
      final WitTypeKind kind1 = WitTypeKind.enumType(values);
      final WitTypeKind kind2 = WitTypeKind.enumType(values);

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different enums should not be compatible")
    void differentEnumsShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.enumType(List.of("a", "b"));
      final WitTypeKind kind2 = WitTypeKind.enumType(List.of("a", "c"));

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("enum should return values via getEnumValues")
    void enumShouldReturnValues() {
      final List<String> values = List.of("red", "green");
      final WitTypeKind kind = WitTypeKind.enumType(values);

      assertEquals(values, kind.getEnumValues());
    }
  }

  @Nested
  @DisplayName("Flags Type Kind Tests")
  class FlagsTypeKindTests {

    @Test
    @DisplayName("flags should create flags type kind")
    void flagsShouldCreateFlagsTypeKind() {
      final List<String> flags = List.of("read", "write", "execute");
      final WitTypeKind kind = WitTypeKind.flags(flags);

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.FLAGS, kind.getCategory());
    }

    @Test
    @DisplayName("flags should return size based on count")
    void flagsShouldReturnSizeBasedOnCount() {
      assertEquals(Optional.of(1), WitTypeKind.flags(List.of("a", "b", "c")).getSizeBytes());
      assertEquals(
          Optional.of(2),
          WitTypeKind.flags(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9")).getSizeBytes());
    }

    @Test
    @DisplayName("flags should return flags list")
    void flagsShouldReturnFlagsList() {
      final List<String> flags = List.of("read", "write");
      final WitTypeKind kind = WitTypeKind.flags(flags);

      assertEquals(2, kind.getFlags().size());
    }

    @Test
    @DisplayName("flags should throw on null")
    void flagsShouldThrowOnNull() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.flags(null));
    }

    @Test
    @DisplayName("8 flags should return 1 byte")
    void eightFlagsShouldReturn1Byte() {
      final List<String> flags = List.of("a", "b", "c", "d", "e", "f", "g", "h");
      assertEquals(Optional.of(1), WitTypeKind.flags(flags).getSizeBytes());
    }

    @Test
    @DisplayName("9 flags should return 2 bytes")
    void nineFlagsShouldReturn2Bytes() {
      final List<String> flags = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
      assertEquals(Optional.of(2), WitTypeKind.flags(flags).getSizeBytes());
    }

    @Test
    @DisplayName("16 flags should return 2 bytes")
    void sixteenFlagsShouldReturn2Bytes() {
      final List<String> flags =
          List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p");
      assertEquals(Optional.of(2), WitTypeKind.flags(flags).getSizeBytes());
    }

    @Test
    @DisplayName("17 flags should return 4 bytes")
    void seventeenFlagsShouldReturn4Bytes() {
      final List<String> flags =
          List.of(
              "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q");
      assertEquals(Optional.of(4), WitTypeKind.flags(flags).getSizeBytes());
    }

    @Test
    @DisplayName("32 flags should return 4 bytes")
    void thirtyTwoFlagsShouldReturn4Bytes() {
      final List<String> flags =
          List.of(
              "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
              "r", "s", "t", "u", "v", "w", "x", "y", "z", "aa", "ab", "ac", "ad", "ae", "af");
      assertEquals(Optional.of(4), WitTypeKind.flags(flags).getSizeBytes());
    }

    @Test
    @DisplayName("33 flags should return 8 bytes")
    void thirtyThreeFlagsShouldReturn8Bytes() {
      final List<String> flags =
          List.of(
              "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q",
              "r", "s", "t", "u", "v", "w", "x", "y", "z", "aa", "ab", "ac", "ad", "ae", "af",
              "ag");
      assertEquals(Optional.of(8), WitTypeKind.flags(flags).getSizeBytes());
    }

    @Test
    @DisplayName("same flags should be compatible")
    void sameFlagsShouldBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.flags(List.of("r", "w"));
      final WitTypeKind kind2 = WitTypeKind.flags(List.of("r", "w"));

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different flags should not be compatible")
    void differentFlagsShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.flags(List.of("r", "w"));
      final WitTypeKind kind2 = WitTypeKind.flags(List.of("r", "x"));

      assertFalse(kind1.isCompatibleWith(kind2));
    }
  }

  @Nested
  @DisplayName("List Type Kind Tests")
  class ListTypeKindTests {

    @Test
    @DisplayName("list should create list type kind")
    void listShouldCreateListTypeKind() {
      final WitType elementType = WitType.createU8();
      final WitTypeKind kind = WitTypeKind.list(elementType);

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.LIST, kind.getCategory());
    }

    @Test
    @DisplayName("list should return empty size")
    void listShouldReturnEmptySize() {
      final WitTypeKind kind = WitTypeKind.list(WitType.createU8());

      assertEquals(Optional.empty(), kind.getSizeBytes());
    }

    @Test
    @DisplayName("list should throw on null element type")
    void listShouldThrowOnNullElementType() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.list(null));
    }

    @Test
    @DisplayName("same lists should be compatible")
    void sameListsShouldBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.list(WitType.createU8());
      final WitTypeKind kind2 = WitTypeKind.list(WitType.createU8());

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different element type lists should not be compatible")
    void differentListsShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.list(WitType.createU8());
      final WitTypeKind kind2 = WitTypeKind.list(WitType.createS32());

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("list should return inner type via getInnerType")
    void listShouldReturnInnerType() {
      final WitType elementType = WitType.createU8();
      final WitTypeKind kind = WitTypeKind.list(elementType);

      assertTrue(kind.getInnerType().isPresent());
      assertEquals("u8", kind.getInnerType().get().getName());
    }
  }

  @Nested
  @DisplayName("Option Type Kind Tests")
  class OptionTypeKindTests {

    @Test
    @DisplayName("option should create option type kind")
    void optionShouldCreateOptionTypeKind() {
      final WitType innerType = WitType.createString();
      final WitTypeKind kind = WitTypeKind.option(innerType);

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.OPTION, kind.getCategory());
    }

    @Test
    @DisplayName("option should return empty size")
    void optionShouldReturnEmptySize() {
      final WitTypeKind kind = WitTypeKind.option(WitType.createString());

      assertEquals(Optional.empty(), kind.getSizeBytes());
    }

    @Test
    @DisplayName("option should return inner type")
    void optionShouldReturnInnerType() {
      final WitType innerType = WitType.createString();
      final WitTypeKind kind = WitTypeKind.option(innerType);

      assertTrue(kind.getInnerType().isPresent());
      assertEquals("string", kind.getInnerType().get().getName());
    }

    @Test
    @DisplayName("option should throw on null inner type")
    void optionShouldThrowOnNullInnerType() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.option(null));
    }

    @Test
    @DisplayName("same options should be compatible")
    void sameOptionsShouldBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.option(WitType.createString());
      final WitTypeKind kind2 = WitTypeKind.option(WitType.createString());

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different options should not be compatible")
    void differentOptionsShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.option(WitType.createString());
      final WitTypeKind kind2 = WitTypeKind.option(WitType.createS32());

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("option should not be compatible with non-option")
    void optionShouldNotBeCompatibleWithNonOption() {
      final WitTypeKind option = WitTypeKind.option(WitType.createString());
      final WitTypeKind list = WitTypeKind.list(WitType.createString());

      assertFalse(option.isCompatibleWith(list));
    }
  }

  @Nested
  @DisplayName("Result Type Kind Tests")
  class ResultTypeKindTests {

    @Test
    @DisplayName("result should create result type kind")
    void resultShouldCreateResultTypeKind() {
      final WitTypeKind kind =
          WitTypeKind.result(Optional.of(WitType.createString()), Optional.of(WitType.createU32()));

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.RESULT, kind.getCategory());
    }

    @Test
    @DisplayName("result should return empty size")
    void resultShouldReturnEmptySize() {
      final WitTypeKind kind = WitTypeKind.result(Optional.empty(), Optional.empty());

      assertEquals(Optional.empty(), kind.getSizeBytes());
    }

    @Test
    @DisplayName("result should handle empty types")
    void resultShouldHandleEmptyTypes() {
      final WitTypeKind kind = WitTypeKind.result(Optional.empty(), Optional.empty());

      assertNotNull(kind);
      assertEquals(WitTypeCategory.RESULT, kind.getCategory());
    }

    @Test
    @DisplayName("same results should be compatible")
    void sameResultsShouldBeCompatible() {
      final WitTypeKind kind1 =
          WitTypeKind.result(Optional.of(WitType.createString()), Optional.empty());
      final WitTypeKind kind2 =
          WitTypeKind.result(Optional.of(WitType.createString()), Optional.empty());

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("results with different error types should not be compatible")
    void differentResultErrorTypeShouldNotBeCompatible() {
      final WitTypeKind kind1 =
          WitTypeKind.result(Optional.of(WitType.createString()), Optional.of(WitType.createU8()));
      final WitTypeKind kind2 =
          WitTypeKind.result(Optional.of(WitType.createString()), Optional.of(WitType.createS32()));

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("result should return ok and error types")
    void resultShouldReturnOkAndErrorTypes() {
      final WitTypeKind kind =
          WitTypeKind.result(Optional.of(WitType.createString()), Optional.of(WitType.createU32()));

      assertTrue(kind.getOkType().isPresent());
      assertEquals("string", kind.getOkType().get().getName());
      assertTrue(kind.getErrorType().isPresent());
      assertEquals("u32", kind.getErrorType().get().getName());
    }
  }

  @Nested
  @DisplayName("Tuple Type Kind Tests")
  class TupleTypeKindTests {

    @Test
    @DisplayName("tuple should create tuple type kind")
    void tupleShouldCreateTupleTypeKind() {
      final List<WitType> elements = List.of(WitType.createS32(), WitType.createString());
      final WitTypeKind kind = WitTypeKind.tuple(elements);

      assertFalse(kind.isPrimitive());
      assertTrue(kind.isComposite());
      assertFalse(kind.isResource());
      assertEquals(WitTypeCategory.TUPLE, kind.getCategory());
    }

    @Test
    @DisplayName("tuple should return empty size")
    void tupleShouldReturnEmptySize() {
      final WitTypeKind kind = WitTypeKind.tuple(List.of(WitType.createS32()));

      assertEquals(Optional.empty(), kind.getSizeBytes());
    }

    @Test
    @DisplayName("tuple should throw on null elements")
    void tupleShouldThrowOnNullElements() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.tuple(null));
    }

    @Test
    @DisplayName("same tuples should be compatible")
    void sameTuplesShouldBeCompatible() {
      final List<WitType> elements = List.of(WitType.createS32(), WitType.createString());
      final WitTypeKind kind1 = WitTypeKind.tuple(elements);
      final WitTypeKind kind2 = WitTypeKind.tuple(elements);

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different size tuples should not be compatible")
    void differentSizeTuplesShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.tuple(List.of(WitType.createS32()));
      final WitTypeKind kind2 =
          WitTypeKind.tuple(List.of(WitType.createS32(), WitType.createString()));

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("tuples with different element types should not be compatible")
    void differentElementTypesTuplesShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.tuple(List.of(WitType.createS32(), WitType.createU8()));
      final WitTypeKind kind2 =
          WitTypeKind.tuple(List.of(WitType.createS32(), WitType.createString()));

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("tuple should return element types via getTupleElements")
    void tupleShouldReturnTupleElements() {
      final List<WitType> elements = List.of(WitType.createS32(), WitType.createString());
      final WitTypeKind kind = WitTypeKind.tuple(elements);

      assertEquals(2, kind.getTupleElements().size());
      assertEquals("s32", kind.getTupleElements().get(0).getName());
      assertEquals("string", kind.getTupleElements().get(1).getName());
    }
  }

  @Nested
  @DisplayName("Resource Type Kind Tests")
  class ResourceTypeKindTests {

    @Test
    @DisplayName("resource should create resource type kind")
    void resourceShouldCreateResourceTypeKind() {
      final WitTypeKind kind = WitTypeKind.resource("wasi:filesystem/file");

      assertFalse(kind.isPrimitive());
      assertFalse(kind.isComposite());
      assertTrue(kind.isResource());
      assertEquals(WitTypeCategory.RESOURCE, kind.getCategory());
    }

    @Test
    @DisplayName("resource should return size 4")
    void resourceShouldReturnSize4() {
      final WitTypeKind kind = WitTypeKind.resource("test-resource");

      assertEquals(Optional.of(4), kind.getSizeBytes());
    }

    @Test
    @DisplayName("resource should throw on null id")
    void resourceShouldThrowOnNullId() {
      assertThrows(NullPointerException.class, () -> WitTypeKind.resource(null));
    }

    @Test
    @DisplayName("same resources should be compatible")
    void sameResourcesShouldBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.resource("test-resource");
      final WitTypeKind kind2 = WitTypeKind.resource("test-resource");

      assertTrue(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("different resources should not be compatible")
    void differentResourcesShouldNotBeCompatible() {
      final WitTypeKind kind1 = WitTypeKind.resource("resource-a");
      final WitTypeKind kind2 = WitTypeKind.resource("resource-b");

      assertFalse(kind1.isCompatibleWith(kind2));
    }

    @Test
    @DisplayName("resource should return resourceId")
    void resourceShouldReturnResourceId() {
      final WitTypeKind kind = WitTypeKind.resource("wasi:io/stream");

      assertTrue(kind.getResourceId().isPresent());
      assertEquals("wasi:io/stream", kind.getResourceId().get());
    }
  }

  @Nested
  @DisplayName("Cross-Type Compatibility Tests")
  class CrossTypeCompatibilityTests {

    @Test
    @DisplayName("primitive should not be compatible with record")
    void primitiveShouldNotBeCompatibleWithRecord() {
      final WitTypeKind primitive = WitTypeKind.primitive(WitPrimitiveType.S32);
      final WitTypeKind record = WitTypeKind.record(Map.of("id", WitType.createU32()));

      assertFalse(primitive.isCompatibleWith(record));
    }

    @Test
    @DisplayName("record should not be compatible with variant")
    void recordShouldNotBeCompatibleWithVariant() {
      final WitTypeKind record = WitTypeKind.record(Map.of("id", WitType.createU32()));
      final WitTypeKind variant = WitTypeKind.variant(Map.of("ok", Optional.empty()));

      assertFalse(record.isCompatibleWith(variant));
    }

    @Test
    @DisplayName("list should not be compatible with option")
    void listShouldNotBeCompatibleWithOption() {
      final WitTypeKind list = WitTypeKind.list(WitType.createU8());
      final WitTypeKind option = WitTypeKind.option(WitType.createU8());

      assertFalse(list.isCompatibleWith(option));
    }

    @Test
    @DisplayName("tuple should not be compatible with non-tuple")
    void tupleShouldNotBeCompatibleWithNonTuple() {
      final WitTypeKind tuple = WitTypeKind.tuple(List.of(WitType.createS32()));
      final WitTypeKind flags = WitTypeKind.flags(List.of("a"));

      assertFalse(tuple.isCompatibleWith(flags));
    }

    @Test
    @DisplayName("resource should not be compatible with non-resource")
    void resourceShouldNotBeCompatibleWithNonResource() {
      final WitTypeKind resource = WitTypeKind.resource("test");
      final WitTypeKind enumKind = WitTypeKind.enumType(List.of("a"));

      assertFalse(resource.isCompatibleWith(enumKind));
    }

    @Test
    @DisplayName("flags should not be compatible with non-flags")
    void flagsShouldNotBeCompatibleWithNonFlags() {
      final WitTypeKind flags = WitTypeKind.flags(List.of("r", "w"));
      final WitTypeKind result = WitTypeKind.result(Optional.empty(), Optional.empty());

      assertFalse(flags.isCompatibleWith(result));
    }

    @Test
    @DisplayName("result should not be compatible with non-result")
    void resultShouldNotBeCompatibleWithNonResult() {
      final WitTypeKind result = WitTypeKind.result(Optional.empty(), Optional.empty());
      final WitTypeKind tuple = WitTypeKind.tuple(List.of(WitType.createS32()));

      assertFalse(result.isCompatibleWith(tuple));
    }

    @Test
    @DisplayName("enum should not be compatible with non-enum")
    void enumShouldNotBeCompatibleWithNonEnum() {
      final WitTypeKind enumKind = WitTypeKind.enumType(List.of("a", "b"));
      final WitTypeKind variant = WitTypeKind.variant(Map.of("ok", Optional.empty()));

      assertFalse(enumKind.isCompatibleWith(variant));
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("non-primitive should return empty primitive type")
    void nonPrimitiveShouldReturnEmptyPrimitiveType() {
      final WitTypeKind kind = WitTypeKind.record(Map.of("id", WitType.createU32()));

      assertTrue(kind.getPrimitiveType().isEmpty());
    }

    @Test
    @DisplayName("non-record should return empty fields")
    void nonRecordShouldReturnEmptyFields() {
      final WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertTrue(kind.getRecordFields().isEmpty());
    }

    @Test
    @DisplayName("non-variant should return empty cases")
    void nonVariantShouldReturnEmptyCases() {
      final WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertTrue(kind.getVariantCases().isEmpty());
    }

    @Test
    @DisplayName("non-flags should return empty flags")
    void nonFlagsShouldReturnEmptyFlags() {
      final WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertTrue(kind.getFlags().isEmpty());
    }

    @Test
    @DisplayName("non-option should return empty inner type")
    void nonOptionShouldReturnEmptyInnerType() {
      final WitTypeKind kind = WitTypeKind.primitive(WitPrimitiveType.S32);

      assertTrue(kind.getInnerType().isEmpty());
    }
  }
}
