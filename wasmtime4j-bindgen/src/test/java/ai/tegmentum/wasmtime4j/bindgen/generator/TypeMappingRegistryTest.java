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

package ai.tegmentum.wasmtime4j.bindgen.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.bindgen.CodeStyle;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link TypeMappingRegistry}. */
@DisplayName("TypeMappingRegistry Tests")
class TypeMappingRegistryTest {

  private static final Logger LOGGER = Logger.getLogger(TypeMappingRegistryTest.class.getName());
  private static final String TEST_PACKAGE = "com.example.test";

  private TypeMappingRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new TypeMappingRegistry(CodeStyle.MODERN, TEST_PACKAGE);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create registry with valid parameters")
    void shouldCreateRegistryWithValidParameters() {
      LOGGER.info("Testing constructor with valid parameters");

      TypeMappingRegistry reg = new TypeMappingRegistry(CodeStyle.LEGACY, "com.example");

      assertThat(reg.getCodeStyle()).isEqualTo(CodeStyle.LEGACY);
      assertThat(reg.getBasePackage()).isEqualTo("com.example");
    }

    @Test
    @DisplayName("should throw NullPointerException when codeStyle is null")
    void shouldThrowWhenCodeStyleIsNull() {
      LOGGER.info("Testing constructor with null codeStyle");

      assertThatThrownBy(() -> new TypeMappingRegistry(null, TEST_PACKAGE))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("codeStyle");
    }

    @Test
    @DisplayName("should throw NullPointerException when basePackage is null")
    void shouldThrowWhenBasePackageIsNull() {
      LOGGER.info("Testing constructor with null basePackage");

      assertThatThrownBy(() -> new TypeMappingRegistry(CodeStyle.MODERN, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("basePackage");
    }
  }

  @Nested
  @DisplayName("WIT Primitive Mapping Tests")
  class WitPrimitiveMappingTests {

    @ParameterizedTest
    @CsvSource({
        "bool, boolean",
        "s8, byte",
        "s16, short",
        "s32, int",
        "s64, long",
        "u8, byte",
        "u16, short",
        "u32, int",
        "u64, long",
        "f32, float",
        "f64, double",
        "float32, float",
        "float64, double",
        "char, int"
    })
    @DisplayName("should map WIT primitive types to Java primitives")
    void shouldMapWitPrimitivesToJavaPrimitives(final String witType, final String expected) {
      LOGGER.info("Testing WIT primitive mapping: " + witType + " -> " + expected);

      TypeName result = registry.mapWitPrimitive(witType);

      assertThat(result.toString()).isEqualTo(expected);
    }

    @Test
    @DisplayName("should map string to java.lang.String")
    void shouldMapStringToJavaString() {
      LOGGER.info("Testing string mapping");

      TypeName result = registry.mapWitPrimitive("string");

      assertThat(result).isEqualTo(ClassName.get(String.class));
    }

    @Test
    @DisplayName("should handle case-insensitive type names")
    void shouldHandleCaseInsensitiveTypeNames() {
      LOGGER.info("Testing case-insensitivity");

      assertThat(registry.mapWitPrimitive("BOOL")).isEqualTo(TypeName.BOOLEAN);
      assertThat(registry.mapWitPrimitive("Bool")).isEqualTo(TypeName.BOOLEAN);
      assertThat(registry.mapWitPrimitive("S32")).isEqualTo(TypeName.INT);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown type")
    void shouldThrowForUnknownType() {
      assertThatThrownBy(() -> registry.mapWitPrimitive("unknown"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown WIT primitive type: unknown");
    }

    @Test
    @DisplayName("should throw NullPointerException for null type")
    void shouldThrowForNullType() {
      assertThatThrownBy(() -> registry.mapWitPrimitive(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("witPrimitive");
    }
  }

  @Nested
  @DisplayName("WASM Type Mapping Tests")
  class WasmTypeMappingTests {

    @ParameterizedTest
    @CsvSource({
        "i32, int",
        "i64, long",
        "f32, float",
        "f64, double"
    })
    @DisplayName("should map WASM numeric types to Java primitives")
    void shouldMapWasmNumericTypes(final String wasmType, final String expected) {
      LOGGER.info("Testing WASM type mapping: " + wasmType + " -> " + expected);

      TypeName result = registry.mapWasmType(wasmType);

      assertThat(result.toString()).isEqualTo(expected);
    }

    @Test
    @DisplayName("should map v128 to byte[]")
    void shouldMapV128ToByteArray() {
      TypeName result = registry.mapWasmType("v128");

      assertThat(result).isEqualTo(ArrayTypeName.of(TypeName.BYTE));
    }

    @Test
    @DisplayName("should map funcref to FunctionReference")
    void shouldMapFuncrefToFunctionReference() {
      TypeName result = registry.mapWasmType("funcref");

      assertThat(result.toString()).isEqualTo("ai.tegmentum.wasmtime4j.FunctionReference");
    }

    @Test
    @DisplayName("should map externref to Object")
    void shouldMapExternrefToObject() {
      TypeName result = registry.mapWasmType("externref");

      assertThat(result).isEqualTo(ClassName.get(Object.class));
    }

    @Test
    @DisplayName("should handle case-insensitive WASM types")
    void shouldHandleCaseInsensitiveWasmTypes() {
      assertThat(registry.mapWasmType("I32")).isEqualTo(TypeName.INT);
      assertThat(registry.mapWasmType("F64")).isEqualTo(TypeName.DOUBLE);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown WASM type")
    void shouldThrowForUnknownWasmType() {
      assertThatThrownBy(() -> registry.mapWasmType("invalid"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown WASM type: invalid");
    }

    @Test
    @DisplayName("should throw NullPointerException for null WASM type")
    void shouldThrowForNullWasmType() {
      assertThatThrownBy(() -> registry.mapWasmType(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("wasmType");
    }
  }

  @Nested
  @DisplayName("List Mapping Tests")
  class ListMappingTests {

    @Test
    @DisplayName("should create List<Integer> for int element type")
    void shouldCreateListOfIntegerForInt() {
      LOGGER.info("Testing list mapping for int");

      TypeName result = registry.mapList(TypeName.INT);

      assertThat(result).isInstanceOf(ParameterizedTypeName.class);
      assertThat(result.toString()).isEqualTo("java.util.List<java.lang.Integer>");
    }

    @Test
    @DisplayName("should create List<String> for String element type")
    void shouldCreateListOfString() {
      TypeName result = registry.mapList(ClassName.get(String.class));

      assertThat(result.toString()).isEqualTo("java.util.List<java.lang.String>");
    }

    @Test
    @DisplayName("should box primitive types in list")
    void shouldBoxPrimitiveTypesInList() {
      assertThat(registry.mapList(TypeName.BOOLEAN).toString())
          .isEqualTo("java.util.List<java.lang.Boolean>");
      assertThat(registry.mapList(TypeName.LONG).toString())
          .isEqualTo("java.util.List<java.lang.Long>");
    }

    @Test
    @DisplayName("should throw NullPointerException for null element type")
    void shouldThrowForNullElementType() {
      assertThatThrownBy(() -> registry.mapList(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("elementType");
    }
  }

  @Nested
  @DisplayName("Option Mapping Tests")
  class OptionMappingTests {

    @Test
    @DisplayName("should create Optional<Integer> for int inner type")
    void shouldCreateOptionalOfIntegerForInt() {
      LOGGER.info("Testing option mapping for int");

      TypeName result = registry.mapOption(TypeName.INT);

      assertThat(result).isInstanceOf(ParameterizedTypeName.class);
      assertThat(result.toString()).isEqualTo("java.util.Optional<java.lang.Integer>");
    }

    @Test
    @DisplayName("should create Optional<String> for String inner type")
    void shouldCreateOptionalOfString() {
      TypeName result = registry.mapOption(ClassName.get(String.class));

      assertThat(result.toString()).isEqualTo("java.util.Optional<java.lang.String>");
    }

    @Test
    @DisplayName("should throw NullPointerException for null inner type")
    void shouldThrowForNullInnerType() {
      assertThatThrownBy(() -> registry.mapOption(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("innerType");
    }
  }

  @Nested
  @DisplayName("Result Mapping Tests")
  class ResultMappingTests {

    @Test
    @DisplayName("should create WitResult with ok and error types")
    void shouldCreateResultWithBothTypes() {
      LOGGER.info("Testing result mapping with both types");

      TypeName result = registry.mapResult(TypeName.INT, ClassName.get(String.class));

      assertThat(result).isInstanceOf(ParameterizedTypeName.class);
      assertThat(result.toString())
          .isEqualTo("ai.tegmentum.wasmtime4j.wit.WitResult<java.lang.Integer, java.lang.String>");
    }

    @Test
    @DisplayName("should use Void for null ok type")
    void shouldUseVoidForNullOkType() {
      TypeName result = registry.mapResult(null, ClassName.get(String.class));

      assertThat(result.toString())
          .isEqualTo("ai.tegmentum.wasmtime4j.wit.WitResult<java.lang.Void, java.lang.String>");
    }

    @Test
    @DisplayName("should use Void for null error type")
    void shouldUseVoidForNullErrorType() {
      TypeName result = registry.mapResult(TypeName.INT, null);

      assertThat(result.toString())
          .isEqualTo("ai.tegmentum.wasmtime4j.wit.WitResult<java.lang.Integer, java.lang.Void>");
    }

    @Test
    @DisplayName("should use Void for both null types")
    void shouldUseVoidForBothNullTypes() {
      TypeName result = registry.mapResult(null, null);

      assertThat(result.toString())
          .isEqualTo("ai.tegmentum.wasmtime4j.wit.WitResult<java.lang.Void, java.lang.Void>");
    }
  }

  @Nested
  @DisplayName("Tuple Mapping Tests")
  class TupleMappingTests {

    @Test
    @DisplayName("should return Void for empty tuple")
    void shouldReturnVoidForEmptyTuple() {
      LOGGER.info("Testing tuple mapping for empty list");

      TypeName result = registry.mapTuple(List.of());

      assertThat(result).isEqualTo(ClassName.get(Void.class));
    }

    @Test
    @DisplayName("should return element type for single-element tuple")
    void shouldReturnElementTypeForSingleElement() {
      TypeName result = registry.mapTuple(List.of(TypeName.INT));

      assertThat(result).isEqualTo(TypeName.INT);
    }

    @Test
    @DisplayName("should return Tuple2 for two-element tuple")
    void shouldReturnTuple2ForTwoElements() {
      TypeName result = registry.mapTuple(Arrays.asList(TypeName.INT, TypeName.LONG));

      assertThat(result.toString()).isEqualTo(TEST_PACKAGE + ".tuple.Tuple2");
    }

    @Test
    @DisplayName("should return TupleN for N-element tuple")
    void shouldReturnTupleNForNElements() {
      TypeName result = registry.mapTuple(
          Arrays.asList(TypeName.INT, TypeName.LONG, TypeName.FLOAT, TypeName.DOUBLE));

      assertThat(result.toString()).isEqualTo(TEST_PACKAGE + ".tuple.Tuple4");
    }

    @Test
    @DisplayName("should throw NullPointerException for null list")
    void shouldThrowForNullList() {
      assertThatThrownBy(() -> registry.mapTuple(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("elementTypes");
    }
  }

  @Nested
  @DisplayName("Generated Type Mapping Tests")
  class GeneratedTypeMappingTests {

    @Test
    @DisplayName("should create ClassName for simple type name")
    void shouldCreateClassNameForSimpleType() {
      LOGGER.info("Testing generated type mapping");

      ClassName result = registry.mapGeneratedType("my-type");

      assertThat(result.packageName()).isEqualTo(TEST_PACKAGE);
      assertThat(result.simpleName()).isEqualTo("MyType");
    }

    @Test
    @DisplayName("should convert kebab-case to PascalCase")
    void shouldConvertKebabCaseToPascalCase() {
      ClassName result = registry.mapGeneratedType("my-long-type-name");

      assertThat(result.simpleName()).isEqualTo("MyLongTypeName");
    }

    @Test
    @DisplayName("should convert snake_case to PascalCase")
    void shouldConvertSnakeCaseToPascalCase() {
      ClassName result = registry.mapGeneratedType("my_long_type_name");

      assertThat(result.simpleName()).isEqualTo("MyLongTypeName");
    }

    @Test
    @DisplayName("should capitalize first letter")
    void shouldCapitalizeFirstLetter() {
      ClassName result = registry.mapGeneratedType("lowercase");

      assertThat(result.simpleName()).isEqualTo("Lowercase");
    }

    @Test
    @DisplayName("should throw NullPointerException for null type name")
    void shouldThrowForNullTypeName() {
      assertThatThrownBy(() -> registry.mapGeneratedType(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("typeName");
    }
  }

  @Nested
  @DisplayName("Interface Mapping Tests")
  class InterfaceMappingTests {

    @Test
    @DisplayName("should create ClassName for interface name")
    void shouldCreateClassNameForInterface() {
      LOGGER.info("Testing interface mapping");

      ClassName result = registry.mapInterface("my-interface");

      assertThat(result.packageName()).isEqualTo(TEST_PACKAGE);
      assertThat(result.simpleName()).isEqualTo("MyInterface");
    }

    @Test
    @DisplayName("should throw NullPointerException for null interface name")
    void shouldThrowForNullInterfaceName() {
      assertThatThrownBy(() -> registry.mapInterface(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("interfaceName");
    }
  }

  @Nested
  @DisplayName("Primitive Check Tests")
  class PrimitiveCheckTests {

    @ParameterizedTest
    @ValueSource(strings = {
        "bool", "s8", "s16", "s32", "s64",
        "u8", "u16", "u32", "u64",
        "f32", "f64", "float32", "float64",
        "char", "string"
    })
    @DisplayName("should return true for primitive types")
    void shouldReturnTrueForPrimitives(final String typeName) {
      assertThat(registry.isPrimitive(typeName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"record", "variant", "my-type", "list", "option"})
    @DisplayName("should return false for non-primitive types")
    void shouldReturnFalseForNonPrimitives(final String typeName) {
      assertThat(registry.isPrimitive(typeName)).isFalse();
    }

    @Test
    @DisplayName("should handle case-insensitive primitive check")
    void shouldHandleCaseInsensitivePrimitiveCheck() {
      assertThat(registry.isPrimitive("BOOL")).isTrue();
      assertThat(registry.isPrimitive("String")).isTrue();
      assertThat(registry.isPrimitive("S32")).isTrue();
    }

    @Test
    @DisplayName("should return false for null")
    void shouldReturnFalseForNull() {
      assertThat(registry.isPrimitive(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getCodeStyle should return configured code style")
    void getCodeStyleShouldReturnConfiguredStyle() {
      TypeMappingRegistry modernRegistry = new TypeMappingRegistry(CodeStyle.MODERN, TEST_PACKAGE);
      TypeMappingRegistry legacyRegistry = new TypeMappingRegistry(CodeStyle.LEGACY, TEST_PACKAGE);

      assertThat(modernRegistry.getCodeStyle()).isEqualTo(CodeStyle.MODERN);
      assertThat(legacyRegistry.getCodeStyle()).isEqualTo(CodeStyle.LEGACY);
    }

    @Test
    @DisplayName("getBasePackage should return configured base package")
    void getBasePackageShouldReturnConfiguredPackage() {
      TypeMappingRegistry reg1 = new TypeMappingRegistry(CodeStyle.MODERN, "com.example.one");
      TypeMappingRegistry reg2 = new TypeMappingRegistry(CodeStyle.MODERN, "org.another.pkg");

      assertThat(reg1.getBasePackage()).isEqualTo("com.example.one");
      assertThat(reg2.getBasePackage()).isEqualTo("org.another.pkg");
    }
  }
}
