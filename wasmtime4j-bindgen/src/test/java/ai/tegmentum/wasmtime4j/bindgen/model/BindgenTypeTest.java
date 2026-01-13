/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
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

package ai.tegmentum.wasmtime4j.bindgen.model;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.bindgen.model.BindgenType.Kind;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link BindgenType}. */
@DisplayName("BindgenType Tests")
class BindgenTypeTest {

  private static final Logger LOGGER = Logger.getLogger(BindgenTypeTest.class.getName());

  @Nested
  @DisplayName("Kind Enum Tests")
  class KindEnumTests {

    @Test
    @DisplayName("should have all expected kind values")
    void shouldHaveAllExpectedKindValues() {
      LOGGER.info("Verifying all Kind enum values are present");

      Kind[] kinds = Kind.values();

      assertThat(kinds).containsExactlyInAnyOrder(
          Kind.PRIMITIVE,
          Kind.RECORD,
          Kind.VARIANT,
          Kind.ENUM,
          Kind.FLAGS,
          Kind.LIST,
          Kind.OPTION,
          Kind.RESULT,
          Kind.TUPLE,
          Kind.RESOURCE,
          Kind.REFERENCE,
          Kind.FUNCTION);
    }

    @Test
    @DisplayName("should convert kind to string correctly")
    void shouldConvertKindToStringCorrectly() {
      assertThat(Kind.PRIMITIVE.toString()).isEqualTo("PRIMITIVE");
      assertThat(Kind.RECORD.toString()).isEqualTo("RECORD");
      assertThat(Kind.VARIANT.toString()).isEqualTo("VARIANT");
    }

    @Test
    @DisplayName("should get kind by name")
    void shouldGetKindByName() {
      assertThat(Kind.valueOf("PRIMITIVE")).isEqualTo(Kind.PRIMITIVE);
      assertThat(Kind.valueOf("LIST")).isEqualTo(Kind.LIST);
      assertThat(Kind.valueOf("OPTION")).isEqualTo(Kind.OPTION);
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create primitive type with primitive() factory")
    void shouldCreatePrimitiveTypeWithFactory() {
      LOGGER.info("Testing primitive() factory method");

      BindgenType type = BindgenType.primitive("i32");

      assertThat(type.getName()).isEqualTo("i32");
      assertThat(type.getKind()).isEqualTo(Kind.PRIMITIVE);
      assertThat(type.isPrimitive()).isTrue();
      assertThat(type.isRecord()).isFalse();
      assertThat(type.isVariant()).isFalse();
      assertThat(type.isEnum()).isFalse();
    }

    @Test
    @DisplayName("should create reference type with reference() factory")
    void shouldCreateReferenceTypeWithFactory() {
      LOGGER.info("Testing reference() factory method");

      BindgenType type = BindgenType.reference("MyRecord");

      assertThat(type.getName()).isEqualTo("MyRecord");
      assertThat(type.getKind()).isEqualTo(Kind.REFERENCE);
      assertThat(type.getReferencedTypeName()).hasValue("MyRecord");
    }

    @Test
    @DisplayName("should create list type with list() factory")
    void shouldCreateListTypeWithFactory() {
      LOGGER.info("Testing list() factory method");

      BindgenType elementType = BindgenType.primitive("string");
      BindgenType listType = BindgenType.list(elementType);

      assertThat(listType.getName()).isEqualTo("list<string>");
      assertThat(listType.getKind()).isEqualTo(Kind.LIST);
      assertThat(listType.getElementType()).hasValue(elementType);
    }

    @Test
    @DisplayName("should create option type with option() factory")
    void shouldCreateOptionTypeWithFactory() {
      LOGGER.info("Testing option() factory method");

      BindgenType innerType = BindgenType.primitive("u32");
      BindgenType optionType = BindgenType.option(innerType);

      assertThat(optionType.getName()).isEqualTo("option<u32>");
      assertThat(optionType.getKind()).isEqualTo(Kind.OPTION);
      assertThat(optionType.getElementType()).hasValue(innerType);
    }

    @Test
    @DisplayName("should create result type with both ok and error types")
    void shouldCreateResultTypeWithBothTypes() {
      LOGGER.info("Testing result() factory method with both types");

      BindgenType okType = BindgenType.primitive("string");
      BindgenType errorType = BindgenType.primitive("u32");
      BindgenType resultType = BindgenType.result(okType, errorType);

      assertThat(resultType.getName()).isEqualTo("result<string, u32>");
      assertThat(resultType.getKind()).isEqualTo(Kind.RESULT);
      assertThat(resultType.getOkType()).hasValue(okType);
      assertThat(resultType.getErrorType()).hasValue(errorType);
    }

    @Test
    @DisplayName("should create result type with null ok type")
    void shouldCreateResultTypeWithNullOkType() {
      LOGGER.info("Testing result() factory method with null ok type");

      BindgenType errorType = BindgenType.primitive("u32");
      BindgenType resultType = BindgenType.result(null, errorType);

      assertThat(resultType.getName()).isEqualTo("result<_, u32>");
      assertThat(resultType.getOkType()).isEmpty();
      assertThat(resultType.getErrorType()).hasValue(errorType);
    }

    @Test
    @DisplayName("should create result type with null error type")
    void shouldCreateResultTypeWithNullErrorType() {
      LOGGER.info("Testing result() factory method with null error type");

      BindgenType okType = BindgenType.primitive("string");
      BindgenType resultType = BindgenType.result(okType, null);

      assertThat(resultType.getName()).isEqualTo("result<string, _>");
      assertThat(resultType.getOkType()).hasValue(okType);
      assertThat(resultType.getErrorType()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create type with builder and default values")
    void shouldCreateTypeWithDefaultValues() {
      LOGGER.info("Testing builder with default values");

      BindgenType type = BindgenType.builder()
          .name("test")
          .build();

      assertThat(type.getName()).isEqualTo("test");
      assertThat(type.getKind()).isEqualTo(Kind.REFERENCE);
      assertThat(type.getDocumentation()).isEmpty();
      assertThat(type.getFields()).isEmpty();
      assertThat(type.getCases()).isEmpty();
      assertThat(type.getEnumValues()).isEmpty();
      assertThat(type.getTupleElements()).isEmpty();
    }

    @Test
    @DisplayName("should create record type with fields")
    void shouldCreateRecordTypeWithFields() {
      LOGGER.info("Testing builder for record type");

      BindgenField field1 = new BindgenField("name", BindgenType.primitive("string"));
      BindgenField field2 = new BindgenField("age", BindgenType.primitive("u32"));

      BindgenType recordType = BindgenType.builder()
          .name("Person")
          .kind(Kind.RECORD)
          .addField(field1)
          .addField(field2)
          .documentation("A person record")
          .build();

      assertThat(recordType.getName()).isEqualTo("Person");
      assertThat(recordType.getKind()).isEqualTo(Kind.RECORD);
      assertThat(recordType.isRecord()).isTrue();
      assertThat(recordType.getFields()).hasSize(2);
      assertThat(recordType.getFields()).containsExactly(field1, field2);
      assertThat(recordType.getDocumentation()).hasValue("A person record");
    }

    @Test
    @DisplayName("should create record type with fields() method")
    void shouldCreateRecordTypeWithFieldsMethod() {
      LOGGER.info("Testing builder with fields() list method");

      List<BindgenField> fields = Arrays.asList(
          new BindgenField("x", BindgenType.primitive("i32")),
          new BindgenField("y", BindgenType.primitive("i32")));

      BindgenType recordType = BindgenType.builder()
          .name("Point")
          .kind(Kind.RECORD)
          .fields(fields)
          .build();

      assertThat(recordType.getFields()).hasSize(2);
    }

    @Test
    @DisplayName("should create variant type with cases")
    void shouldCreateVariantTypeWithCases() {
      LOGGER.info("Testing builder for variant type");

      BindgenVariantCase case1 = new BindgenVariantCase("none");
      BindgenVariantCase case2 = new BindgenVariantCase("some", BindgenType.primitive("i32"));

      BindgenType variantType = BindgenType.builder()
          .name("MyOption")
          .kind(Kind.VARIANT)
          .addCase(case1)
          .addCase(case2)
          .build();

      assertThat(variantType.getName()).isEqualTo("MyOption");
      assertThat(variantType.getKind()).isEqualTo(Kind.VARIANT);
      assertThat(variantType.isVariant()).isTrue();
      assertThat(variantType.getCases()).hasSize(2);
      assertThat(variantType.getCases()).containsExactly(case1, case2);
    }

    @Test
    @DisplayName("should create variant type with cases() method")
    void shouldCreateVariantTypeWithCasesMethod() {
      LOGGER.info("Testing builder with cases() list method");

      List<BindgenVariantCase> cases = Arrays.asList(
          new BindgenVariantCase("left", BindgenType.primitive("i32")),
          new BindgenVariantCase("right", BindgenType.primitive("string")));

      BindgenType variantType = BindgenType.builder()
          .name("Either")
          .kind(Kind.VARIANT)
          .cases(cases)
          .build();

      assertThat(variantType.getCases()).hasSize(2);
    }

    @Test
    @DisplayName("should create enum type with values")
    void shouldCreateEnumTypeWithValues() {
      LOGGER.info("Testing builder for enum type");

      BindgenType enumType = BindgenType.builder()
          .name("Color")
          .kind(Kind.ENUM)
          .addEnumValue("red")
          .addEnumValue("green")
          .addEnumValue("blue")
          .build();

      assertThat(enumType.getName()).isEqualTo("Color");
      assertThat(enumType.getKind()).isEqualTo(Kind.ENUM);
      assertThat(enumType.isEnum()).isTrue();
      assertThat(enumType.getEnumValues()).hasSize(3);
      assertThat(enumType.getEnumValues()).containsExactly("red", "green", "blue");
    }

    @Test
    @DisplayName("should create enum type with enumValues() method")
    void shouldCreateEnumTypeWithEnumValuesMethod() {
      LOGGER.info("Testing builder with enumValues() list method");

      List<String> values = Arrays.asList("monday", "tuesday", "wednesday");

      BindgenType enumType = BindgenType.builder()
          .name("Day")
          .kind(Kind.ENUM)
          .enumValues(values)
          .build();

      assertThat(enumType.getEnumValues()).hasSize(3);
      assertThat(enumType.getEnumValues()).containsExactly("monday", "tuesday", "wednesday");
    }

    @Test
    @DisplayName("should create tuple type with elements")
    void shouldCreateTupleTypeWithElements() {
      LOGGER.info("Testing builder for tuple type");

      BindgenType tupleType = BindgenType.builder()
          .name("tuple<i32, string>")
          .kind(Kind.TUPLE)
          .tupleElements(Arrays.asList(
              BindgenType.primitive("i32"),
              BindgenType.primitive("string")))
          .build();

      assertThat(tupleType.getKind()).isEqualTo(Kind.TUPLE);
      assertThat(tupleType.getTupleElements()).hasSize(2);
    }

    @Test
    @DisplayName("should set element type for list types")
    void shouldSetElementTypeForListTypes() {
      LOGGER.info("Testing builder with elementType()");

      BindgenType elementType = BindgenType.primitive("u8");
      BindgenType listType = BindgenType.builder()
          .name("list<u8>")
          .kind(Kind.LIST)
          .elementType(elementType)
          .build();

      assertThat(listType.getElementType()).hasValue(elementType);
    }

    @Test
    @DisplayName("should set ok and error types for result types")
    void shouldSetOkAndErrorTypes() {
      LOGGER.info("Testing builder with okType() and errorType()");

      BindgenType okType = BindgenType.primitive("string");
      BindgenType errorType = BindgenType.primitive("i32");

      BindgenType resultType = BindgenType.builder()
          .name("result<string, i32>")
          .kind(Kind.RESULT)
          .okType(okType)
          .errorType(errorType)
          .build();

      assertThat(resultType.getOkType()).hasValue(okType);
      assertThat(resultType.getErrorType()).hasValue(errorType);
    }

    @Test
    @DisplayName("should set referenced type name")
    void shouldSetReferencedTypeName() {
      LOGGER.info("Testing builder with referencedTypeName()");

      BindgenType refType = BindgenType.builder()
          .name("MyType")
          .kind(Kind.REFERENCE)
          .referencedTypeName("MyType")
          .build();

      assertThat(refType.getReferencedTypeName()).hasValue("MyType");
    }
  }

  @Nested
  @DisplayName("Type Check Method Tests")
  class TypeCheckMethodTests {

    @Test
    @DisplayName("isPrimitive() should return true only for primitive types")
    void isPrimitiveShouldReturnTrueOnlyForPrimitiveTypes() {
      BindgenType primitive = BindgenType.primitive("i32");
      BindgenType record = BindgenType.builder().name("R").kind(Kind.RECORD).build();

      assertThat(primitive.isPrimitive()).isTrue();
      assertThat(record.isPrimitive()).isFalse();
    }

    @Test
    @DisplayName("isRecord() should return true only for record types")
    void isRecordShouldReturnTrueOnlyForRecordTypes() {
      BindgenType record = BindgenType.builder().name("R").kind(Kind.RECORD).build();
      BindgenType primitive = BindgenType.primitive("i32");

      assertThat(record.isRecord()).isTrue();
      assertThat(primitive.isRecord()).isFalse();
    }

    @Test
    @DisplayName("isVariant() should return true only for variant types")
    void isVariantShouldReturnTrueOnlyForVariantTypes() {
      BindgenType variant = BindgenType.builder().name("V").kind(Kind.VARIANT).build();
      BindgenType primitive = BindgenType.primitive("i32");

      assertThat(variant.isVariant()).isTrue();
      assertThat(primitive.isVariant()).isFalse();
    }

    @Test
    @DisplayName("isEnum() should return true only for enum types")
    void isEnumShouldReturnTrueOnlyForEnumTypes() {
      BindgenType enumType = BindgenType.builder().name("E").kind(Kind.ENUM).build();
      BindgenType primitive = BindgenType.primitive("i32");

      assertThat(enumType.isEnum()).isTrue();
      assertThat(primitive.isEnum()).isFalse();
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should be equal when name and kind match")
    void shouldBeEqualWhenNameAndKindMatch() {
      LOGGER.info("Testing equals() for matching types");

      BindgenType type1 = BindgenType.primitive("i32");
      BindgenType type2 = BindgenType.primitive("i32");

      assertThat(type1).isEqualTo(type2);
      assertThat(type1.hashCode()).isEqualTo(type2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when names differ")
    void shouldNotBeEqualWhenNamesDiffer() {
      LOGGER.info("Testing equals() for different names");

      BindgenType type1 = BindgenType.primitive("i32");
      BindgenType type2 = BindgenType.primitive("i64");

      assertThat(type1).isNotEqualTo(type2);
    }

    @Test
    @DisplayName("should not be equal when kinds differ")
    void shouldNotBeEqualWhenKindsDiffer() {
      LOGGER.info("Testing equals() for different kinds");

      BindgenType type1 = BindgenType.builder().name("MyType").kind(Kind.RECORD).build();
      BindgenType type2 = BindgenType.builder().name("MyType").kind(Kind.VARIANT).build();

      assertThat(type1).isNotEqualTo(type2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      BindgenType type = BindgenType.primitive("i32");

      assertThat(type).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      BindgenType type = BindgenType.primitive("i32");

      assertThat(type).isNotEqualTo("i32");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      BindgenType type = BindgenType.primitive("i32");

      assertThat(type).isEqualTo(type);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include name and kind in toString()")
    void shouldIncludeNameAndKindInToString() {
      LOGGER.info("Testing toString() output");

      BindgenType type = BindgenType.primitive("string");
      String toString = type.toString();

      assertThat(toString).contains("name='string'");
      assertThat(toString).contains("kind=PRIMITIVE");
      assertThat(toString).startsWith("BindgenType{");
      assertThat(toString).endsWith("}");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("fields list should be immutable")
    void fieldsListShouldBeImmutable() {
      LOGGER.info("Testing that fields list is immutable");

      BindgenType recordType = BindgenType.builder()
          .name("Record")
          .kind(Kind.RECORD)
          .addField(new BindgenField("f", BindgenType.primitive("i32")))
          .build();

      List<BindgenField> fields = recordType.getFields();

      org.assertj.core.api.Assertions.assertThatThrownBy(() -> fields.add(
          new BindgenField("f2", BindgenType.primitive("i32"))))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("cases list should be immutable")
    void casesListShouldBeImmutable() {
      LOGGER.info("Testing that cases list is immutable");

      BindgenType variantType = BindgenType.builder()
          .name("Variant")
          .kind(Kind.VARIANT)
          .addCase(new BindgenVariantCase("a"))
          .build();

      List<BindgenVariantCase> cases = variantType.getCases();

      org.assertj.core.api.Assertions.assertThatThrownBy(() -> cases.add(
          new BindgenVariantCase("b")))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("enumValues list should be immutable")
    void enumValuesListShouldBeImmutable() {
      LOGGER.info("Testing that enumValues list is immutable");

      BindgenType enumType = BindgenType.builder()
          .name("Enum")
          .kind(Kind.ENUM)
          .addEnumValue("a")
          .build();

      List<String> values = enumType.getEnumValues();

      org.assertj.core.api.Assertions.assertThatThrownBy(() -> values.add("b"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("tupleElements list should be immutable")
    void tupleElementsListShouldBeImmutable() {
      LOGGER.info("Testing that tupleElements list is immutable");

      BindgenType tupleType = BindgenType.builder()
          .name("Tuple")
          .kind(Kind.TUPLE)
          .tupleElements(Arrays.asList(BindgenType.primitive("i32")))
          .build();

      List<BindgenType> elements = tupleType.getTupleElements();

      org.assertj.core.api.Assertions.assertThatThrownBy(() -> elements.add(
          BindgenType.primitive("i64")))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
