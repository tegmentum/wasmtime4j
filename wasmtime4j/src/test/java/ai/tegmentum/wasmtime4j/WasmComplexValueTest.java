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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmComplexValue class.
 *
 * <p>WasmComplexValue represents complex WebAssembly values including multi-dimensional arrays,
 * collections, and custom POJOs.
 */
@DisplayName("WasmComplexValue Class Tests")
class WasmComplexValueTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class, not an interface")
    void shouldBeAClass() {
      assertFalse(WasmComplexValue.class.isInterface(), "WasmComplexValue should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmComplexValue.class.getModifiers()),
          "WasmComplexValue should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasmComplexValue.class.getModifiers()),
          "WasmComplexValue should be final");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have complexType field")
    void shouldHaveComplexTypeField() throws NoSuchFieldException {
      Field field = WasmComplexValue.class.getDeclaredField("complexType");
      assertNotNull(field, "complexType field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have value field")
    void shouldHaveValueField() throws NoSuchFieldException {
      Field field = WasmComplexValue.class.getDeclaredField("value");
      assertNotNull(field, "value field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have javaType field")
    void shouldHaveJavaTypeField() throws NoSuchFieldException {
      Field field = WasmComplexValue.class.getDeclaredField("javaType");
      assertNotNull(field, "javaType field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have metadata field")
    void shouldHaveMetadataField() throws NoSuchFieldException {
      Field field = WasmComplexValue.class.getDeclaredField("metadata");
      assertNotNull(field, "metadata field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() {
      Constructor<?>[] constructors = WasmComplexValue.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }
  }

  // ========================================================================
  // Instance Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("should have getComplexType method")
    void shouldHaveGetComplexTypeMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("getComplexType");
      assertNotNull(method, "getComplexType method should exist");
      assertEquals(
          WasmComplexValue.ComplexType.class, method.getReturnType(), "Should return ComplexType");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("should have getJavaType method")
    void shouldHaveGetJavaTypeMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("getJavaType");
      assertNotNull(method, "getJavaType method should exist");
      assertEquals(Class.class, method.getReturnType(), "Should return Class");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have asMultiArray method")
    void shouldHaveAsMultiArrayMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("asMultiArray");
      assertNotNull(method, "asMultiArray method should exist");
      assertEquals(1, method.getTypeParameters().length, "Should have type parameter");
    }

    @Test
    @DisplayName("should have asList method")
    void shouldHaveAsListMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("asList");
      assertNotNull(method, "asList method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have asMap method")
    void shouldHaveAsMapMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("asMap");
      assertNotNull(method, "asMap method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have asObject method")
    void shouldHaveAsObjectMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("asObject");
      assertNotNull(method, "asObject method should exist");
      assertEquals(1, method.getTypeParameters().length, "Should have type parameter");
    }

    @Test
    @DisplayName("should have asBinaryBlob method")
    void shouldHaveAsBinaryBlobMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("asBinaryBlob");
      assertNotNull(method, "asBinaryBlob method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have asString method")
    void shouldHaveAsStringMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("asString");
      assertNotNull(method, "asString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have validateCompatibility method")
    void shouldHaveValidateCompatibilityMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("validateCompatibility", Class.class);
      assertNotNull(method, "validateCompatibility method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have multiArray factory method")
    void shouldHaveMultiArrayMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("multiArray", Object.class);
      assertNotNull(method, "multiArray method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }

    @Test
    @DisplayName("should have list factory method")
    void shouldHaveListMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("list", List.class, Class.class);
      assertNotNull(method, "list method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }

    @Test
    @DisplayName("should have map factory method")
    void shouldHaveMapMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("map", Map.class, Class.class, Class.class);
      assertNotNull(method, "map method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }

    @Test
    @DisplayName("should have object factory method")
    void shouldHaveObjectMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("object", Object.class);
      assertNotNull(method, "object method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }

    @Test
    @DisplayName("should have binaryBlob factory method")
    void shouldHaveBinaryBlobMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("binaryBlob", byte[].class);
      assertNotNull(method, "binaryBlob method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }

    @Test
    @DisplayName("should have string factory method")
    void shouldHaveStringMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("string", String.class);
      assertNotNull(method, "string method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }

    @Test
    @DisplayName("should have nullRef factory method")
    void shouldHaveNullRefMethod() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("nullRef", Class.class);
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasmComplexValue.class, method.getReturnType(), "Should return WasmComplexValue");
    }
  }

  // ========================================================================
  // ComplexType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ComplexType Enum Tests")
  class ComplexTypeEnumTests {

    @Test
    @DisplayName("should be a nested enum")
    void shouldBeNestedEnum() {
      assertTrue(WasmComplexValue.ComplexType.class.isEnum(), "ComplexType should be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmComplexValue.ComplexType.class.getModifiers()),
          "ComplexType should be public");
    }

    @Test
    @DisplayName("should have MULTI_ARRAY type")
    void shouldHaveMultiArrayType() {
      assertNotNull(
          WasmComplexValue.ComplexType.valueOf("MULTI_ARRAY"), "MULTI_ARRAY should exist");
    }

    @Test
    @DisplayName("should have LIST type")
    void shouldHaveListType() {
      assertNotNull(WasmComplexValue.ComplexType.valueOf("LIST"), "LIST should exist");
    }

    @Test
    @DisplayName("should have MAP type")
    void shouldHaveMapType() {
      assertNotNull(WasmComplexValue.ComplexType.valueOf("MAP"), "MAP should exist");
    }

    @Test
    @DisplayName("should have OBJECT type")
    void shouldHaveObjectType() {
      assertNotNull(WasmComplexValue.ComplexType.valueOf("OBJECT"), "OBJECT should exist");
    }

    @Test
    @DisplayName("should have STRUCT type")
    void shouldHaveStructType() {
      assertNotNull(WasmComplexValue.ComplexType.valueOf("STRUCT"), "STRUCT should exist");
    }

    @Test
    @DisplayName("should have UNION type")
    void shouldHaveUnionType() {
      assertNotNull(WasmComplexValue.ComplexType.valueOf("UNION"), "UNION should exist");
    }

    @Test
    @DisplayName("should have BINARY_BLOB type")
    void shouldHaveBinaryBlobType() {
      assertNotNull(
          WasmComplexValue.ComplexType.valueOf("BINARY_BLOB"), "BINARY_BLOB should exist");
    }

    @Test
    @DisplayName("should have STRING_DATA type")
    void shouldHaveStringDataType() {
      assertNotNull(
          WasmComplexValue.ComplexType.valueOf("STRING_DATA"), "STRING_DATA should exist");
    }

    @Test
    @DisplayName("should have NULL_REF type")
    void shouldHaveNullRefType() {
      assertNotNull(WasmComplexValue.ComplexType.valueOf("NULL_REF"), "NULL_REF should exist");
    }

    @Test
    @DisplayName("should have exactly 9 types")
    void shouldHaveExactly9Types() {
      assertEquals(
          9, WasmComplexValue.ComplexType.values().length, "Should have exactly 9 complex types");
    }
  }

  // ========================================================================
  // Object Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          WasmComplexValue.class,
          method.getDeclaringClass(),
          "equals should be declared in WasmComplexValue");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          WasmComplexValue.class,
          method.getDeclaringClass(),
          "hashCode should be declared in WasmComplexValue");
    }

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      Method method = WasmComplexValue.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          WasmComplexValue.class,
          method.getDeclaringClass(),
          "toString should be declared in WasmComplexValue");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have ComplexType nested enum")
    void shouldHaveComplexTypeNestedEnum() {
      Set<String> nestedClassNames =
          Arrays.stream(WasmComplexValue.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(nestedClassNames.contains("ComplexType"), "Should have ComplexType");
    }
  }
}
